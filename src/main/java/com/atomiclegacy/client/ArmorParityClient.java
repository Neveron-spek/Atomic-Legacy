package com.atomiclegacy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.gui.ArmorTableScreen;
import com.atomiclegacy.items.ArmorModItem;
import com.atomiclegacy.items.LegacyJetpackItem;
import com.atomiclegacy.items.LegacyWd40ArmorModItem;
import com.atomiclegacy.network.ArmorParityPayloads;
import com.atomiclegacy.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity I2] Client half of the armor-mod batch: HbmKeybinds toggleBack (C) / toggleHUD (V),
 * the jump key for jetpacks, ClientProxy "jetpack" / "properJolt" effects, ItemModWD40 client
 * particles and the "Mods:" section of ModEventHandlerClient's armor tooltip.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class ArmorParityClient {
    public static final KeyMapping TOGGLE_BACK = new KeyMapping("key.atomiclegacy.toggle_back",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.atomiclegacy");
    public static final KeyMapping TOGGLE_HUD = new KeyMapping("key.atomiclegacy.toggle_hud",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.atomiclegacy");

    private ArmorParityClient() {
    }

    @EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        private Keys() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_BACK);
            event.register(TOGGLE_HUD);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LegacyJetpackItem.clientJumpHeld = minecraft.screen == null && minecraft.options.keyJump.isDown();
        if (minecraft.player == null) return;
        while (TOGGLE_BACK.consumeClick()) {
            PacketDistributor.sendToServer(new ArmorParityPayloads.ToggleKey(0));
        }
        while (TOGGLE_HUD.consumeClick()) {
            PacketDistributor.sendToServer(new ArmorParityPayloads.ToggleKey(1));
        }
        if (minecraft.level != null && !minecraft.isPaused()) {
            for (Player player : minecraft.level.players()) {
                wd40Particles(player);
            }
        }
    }

    /** ItemModWD40#modUpdate client branch: one "reddust" (0.01, 0.5, 0.8) per tick while hurtTime > 0. */
    private static void wd40Particles(Player entity) {
        if (entity.hurtTime <= 0) return;
        for (ItemStack armor : entity.getArmorSlots()) {
            if (armor.isEmpty()) continue;
            for (ItemStack mod : ArmorModHandler.pryMods(armor, entity.registryAccess())) {
                if (mod.getItem() instanceof LegacyWd40ArmorModItem) {
                    var rand = entity.getRandom();
                    entity.level().addParticle(new DustParticleOptions(new Vector3f(0.01F, 0.5F, 0.8F), 1.0F),
                            entity.getX() + (rand.nextDouble() - 0.5) * entity.getBbWidth() * 2,
                            entity.getY() + rand.nextDouble() * entity.getBbHeight(),
                            entity.getZ() + (rand.nextDouble() - 0.5) * entity.getBbWidth() * 2, 0, 0, 0);
                }
            }
        }
    }

    public static void handleEffect(ArmorParityPayloads.Effect payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Level world = minecraft.level;
        if (world == null || minecraft.player == null) return;
        if (payload.kind() == ArmorParityPayloads.Effect.PROPER_JOLT) {
            minecraft.player.hurtTime = payload.a();
            minecraft.player.hurtDuration = payload.b();
            return;
        }
        if (payload.kind() != ArmorParityPayloads.Effect.JETPACK) return;
        int particleSetting = minecraft.options.particles().get() == ParticleStatus.ALL ? 0
                : minecraft.options.particles().get() == ParticleStatus.DECREASED ? 1 : 2;
        if (particleSetting == 2) return;
        Entity ent = world.getEntity(payload.entity());
        if (!(ent instanceof Player p)) return;

        float angle = (float) -Math.toRadians(p.getYHeadRot() - (p.getYHeadRot() - p.yBodyRot));
        Vec3 vec = new Vec3(0, 0, -0.25).yRot(angle);
        Vec3 offset = new Vec3(0.125, 0, 0).yRot(angle);
        double ix = p.getX() + vec.x;
        double iy = p.getY() + 0.74D; // 1.7.10: posY + eyeHeight - 1 = feet + 1.62 + 0.12 - 1
        double iz = p.getZ() + vec.z;
        double ox = offset.x;
        double oz = offset.z;
        double moX = 0, moY = 0, moZ = 0;
        if (payload.a() == 0) moY -= 0.2;
        if (payload.a() == 1) {
            Vec3 look = p.getLookAngle();
            moX -= look.x * 0.1D;
            moY -= look.y * 0.1D;
            moZ -= look.z * 0.1D;
        }
        if (particleSetting == 0) {
            Vec3 pos = new Vec3(ix, iy, iz);
            Vec3 thrust = new Vec3(moX, moY, moZ).normalize();
            Vec3 target = pos.add(thrust.x * 10, thrust.y * 10, thrust.z * 10);
            BlockHitResult mop = world.clip(new ClipContext(pos, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (mop.getType() == HitResult.Type.BLOCK && mop.getDirection() == Direction.UP) {
                BlockState state = world.getBlockState(mop.getBlockPos());
                Vec3 hit = mop.getLocation();
                double delta = new Vec3(ix - hit.x, iy - hit.y, iz - hit.z).length();
                Vec3 vel = new Vec3(0.75 - delta * 0.075, 0, 0);
                for (int i = 0; i < (10 - delta); i++) {
                    vel = vel.yRot(world.random.nextFloat() * (float) Math.PI * 2F);
                    world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), hit.x, hit.y + 0.1, hit.z, vel.x, 0.1, vel.z);
                }
            }
        }
        Vec3 motion = p.getDeltaMovement();
        double mX2 = Mth.clamp(motion.x + moX * 2, -5, 5);
        double mY2 = Mth.clamp(motion.y + moY * 2, -5, 5);
        double mZ2 = Mth.clamp(motion.z + moZ * 2, -5, 5);
        double mX3 = Mth.clamp(motion.x + moX * 2, -10, 10);
        double mY3 = Mth.clamp(motion.y + moY * 2, -10, 10);
        double mZ3 = Mth.clamp(motion.z + moZ * 2, -10, 10);
        world.addParticle(ParticleTypes.FLAME, ix + ox, iy, iz + oz, mX2, mY2, mZ2);
        world.addParticle(ParticleTypes.FLAME, ix - ox, iy, iz - oz, mX2, mY2, mZ2);
        if (particleSetting == 0) {
            world.addParticle(ParticleTypes.SMOKE, ix + ox, iy, iz + oz, mX3, mY3, mZ3);
            world.addParticle(ParticleTypes.SMOKE, ix - ox, iy, iz - oz, mX3, mY3, mZ3);
        }
    }

    /** ModEventHandlerClient#onItemTooltip "ARMOR MODS" block. */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(com.atomiclegacy.registry.HbmItems.HAT.get()) && !event.getToolTip().isEmpty()) {
            event.getToolTip().add(1, Component.literal("+2 DT").withStyle(ChatFormatting.BLUE)); // ArmorHat#addInformation
        }
        if (!(stack.getItem() instanceof ArmorItem)) return;
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains(ArmorModHandler.MOD_COMPOUND_KEY)) return;
        Minecraft minecraft = Minecraft.getInstance();
        List<Component> list = event.getToolTip();
        boolean shift = InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
        if (!shift && !(minecraft.screen instanceof ArmorTableScreen)) {
            list.add(Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                    .append(Component.literal("> to display installed armor mods").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
            return;
        }
        if (minecraft.level == null) return;
        list.add(Component.literal("Mods:").withStyle(ChatFormatting.YELLOW));
        ItemStack[] mods = ArmorModHandler.pryMods(stack.copy(), minecraft.level.registryAccess());
        List<Component> desc = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            if (mods[i].getItem() instanceof ArmorModItem mod) {
                mod.addDesc(desc, mods[i], stack);
            }
        }
        list.addAll(desc);
    }
}
