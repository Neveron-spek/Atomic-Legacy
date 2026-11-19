package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.mixin.client.LayerLightSectionStorageInvoker;
import com.atomiclegacy.mixin.client.LevelLightEngineAccessor;
import com.atomiclegacy.mixin.client.LightEngineAccessor;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * [parity z1] Client half of ArmorNo9#onArmorTick: while the worn helmet's "isOn" tag is set, every second world tick
 * the block hit by a 50-block look ray (no liquids, blocks without a collision box ignored) floods block light into
 * the client world (lightUpRecursively, level min(15, 25 - distance * 25 / 50)); each lit position is recomputed
 * by the light engine 5 ticks later (checkLights / updateWorldHook from ModEventHandlerClient#clientTick).
 * World#setLightValue maps to the client block-light storage; World#updateLightByType to LightEngine#checkBlock.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class No9FlashlightClient {

    private static final Set<BlockPos> breadcrumb = new HashSet<>();
    private static final Map<Level, Long> lastChecks = new WeakHashMap<>();
    private static final Map<Level, Map<BlockPos, Long>> lightCheck = new WeakHashMap<>();

    private No9FlashlightClient() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Level world = mc.level;
        if (world == null) {
            return;
        }
        updateWorldHook(world);

        Player player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack armor = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!armor.is(HbmItems.NO9.get())) {
            return;
        }
        CustomData data = armor.get(DataComponents.CUSTOM_DATA);
        if (world.getGameTime() % 2 == 0 && data != null && data.copyTag().getBoolean("isOn")) {
            checkLights(world, false);
            float range = 50F;
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getViewVector(1.0F).scale(range));
            BlockHitResult mop = world.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (mop.getType() == HitResult.Type.BLOCK) {
                Vec3 look = eye.subtract(mop.getLocation());
                Direction dir = mop.getDirection();
                int level = Math.min(15, (int) (25 - (look.length() * 25 / range)));
                BlockPos hit = mop.getBlockPos();
                lightUpRecursively(world, hit.getX() + dir.getStepX(), hit.getY() + dir.getStepY(), hit.getZ() + dir.getStepZ(), level);
                breadcrumb.clear();
            }
        }
    }

    private static void lightUpRecursively(Level world, int x, int y, int z, int light) {
        if (light <= 0) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (breadcrumb.contains(pos)) return;
        breadcrumb.add(pos);

        int existingLight = getStoredBlockLight(world, pos);
        int occupancy = world.getBlockState(pos).getLightBlock(world, pos);

        // 1.7.10: only block if it's fully blocking (opacity 255), light goes through semi-translucent blocks
        if (occupancy >= 15) return;

        int newLight = Math.min(15, Math.max(existingLight, light));
        setStoredBlockLight(world, pos, newLight);
        lightCheck.computeIfAbsent(world, w -> new HashMap<>()).put(pos, world.getGameTime() + 5);

        lightUpRecursively(world, x + 1, y, z, light - 1);
        lightUpRecursively(world, x - 1, y, z, light - 1);
        lightUpRecursively(world, x, y + 1, z, light - 1);
        lightUpRecursively(world, x, y - 1, z, light - 1);
        lightUpRecursively(world, x, y, z + 1, light - 1);
        lightUpRecursively(world, x, y, z - 1, light - 1);
    }

    private static void checkLights(Level world, boolean force) {
        Map<BlockPos, Long> checks = lightCheck.get(world);
        if (checks != null) {
            Iterator<Map.Entry<BlockPos, Long>> it = checks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Long> entry = it.next();
                if (world.getGameTime() > entry.getValue() || force) {
                    world.getLightEngine().checkBlock(entry.getKey());
                    it.remove();
                }
            }
        }
        lastChecks.put(world, world.getGameTime());
    }

    private static void updateWorldHook(Level world) {
        Long last = lastChecks.get(world);
        if (last != null && last < world.getGameTime() + 15) {
            checkLights(world, false);
        }
    }

    private static LayerLightSectionStorageInvoker storage(Level world) {
        LightEngine<?, ?> engine = ((LevelLightEngineAccessor) world.getLightEngine()).hbm$getBlockEngine();
        if (engine == null) return null;
        return (LayerLightSectionStorageInvoker) ((LightEngineAccessor) engine).hbm$getStorage();
    }

    /** World#getSavedLightValue(EnumSkyBlock.Block, ...). */
    private static int getStoredBlockLight(Level world, BlockPos pos) {
        LayerLightSectionStorageInvoker storage = storage(world);
        long section = SectionPos.asLong(pos);
        if (storage == null || world.isOutsideBuildHeight(pos) || !storage.hbm$storingLightForSection(section)) return 0;
        return storage.hbm$getStoredLevel(pos.asLong());
    }

    /** World#setLightValue(EnumSkyBlock.Block, ...): the next LevelRenderer light pass publishes it and re-meshes the section. */
    private static void setStoredBlockLight(Level world, BlockPos pos, int light) {
        LayerLightSectionStorageInvoker storage = storage(world);
        long section = SectionPos.asLong(pos);
        if (storage == null || world.isOutsideBuildHeight(pos) || !storage.hbm$storingLightForSection(section)) return;
        storage.hbm$setStoredLevel(pos.asLong(), light);
    }
}
