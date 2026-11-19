package com.atomiclegacy.items;

import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import com.atomiclegacy.network.GunTinyExplosionPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;


/** Construction boltgun retained because legacy multiblock assembly requires it. */
public final class BoltgunItem extends Item
        implements IAnimatedItem<com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation> { // [parity m1]
    private static final ResourceLocation BOLT_SPIKE = ResourceLocation.fromNamespaceAndPath("atomiclegacy", "bolt_spike");

    public BoltgunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** [parity I3] DamageSource.causePlayerDamage(player).setDamageBypassesArmor(): death.attack.player, armour bypassed by tag. */
    private static final ResourceKey<DamageType> BOLTGUN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("atomiclegacy", "boltgun"));

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!ToolConversion.isConvertible(context.getLevel(), context.getClickedPos(), ToolConversion.Tool.BOLT)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!ToolConversion.convert(context.getLevel(), context.getClickedPos(), context.getPlayer(), ToolConversion.Tool.BOLT)) {
            return InteractionResult.PASS;
        }
        // [parity I3: ItemBoltgun.onItemUse - rivet gun sound at the player, largeexplode 0.25 blocks off the clicked face]
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    HbmSoundEvents.BOLT_GUN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.inventoryMenu.broadcastChanges();
            playAnimation(player, com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.SWING); // [parity m1]
        }
        Vec3 hit = context.getClickLocation();
        Direction dir = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        largeExplode(level, hit.x + dir.getStepX() * 0.25D, hit.y + dir.getStepY() * 0.25D, hit.z + dir.getStepZ() * 0.25D,
                pos.getX(), pos.getY(), pos.getZ());
        return InteractionResult.CONSUME;
    }

    /**
     * [parity I3] ItemBoltgun.onLeftClickEntity: replaces the normal attack (returns true) when a bolt is available.
     * Bolt priority is by type (spike, steel, tungsten, dura steel), then by slot; 10 armour-bypassing player damage
     * ignoring invulnerability frames.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (int type = 0; type < 4; type++) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack bolt = inventory.getItem(slot);
                if (bolt.isEmpty() || boltType(bolt) != type) {
                    continue;
                }
                Level level = player.level();
                if (!level.isClientSide) {
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            HbmSoundEvents.BOLT_GUN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    inventory.removeItem(slot, 1);
                    player.inventoryMenu.broadcastChanges();
                    DamageSource source = new DamageSource(
                            level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BOLTGUN_DAMAGE), player);
                    entity.invulnerableTime = 0;
                    entity.hurt(source, 10.0F);
                    if (!entity.isAlive() && entity instanceof Player victim) {
                        HbmAdvancements.award(victim, "go_fish");
                    }
                    largeExplode(level, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0D, entity.getZ(),
                            entity.getX(), entity.getY(), entity.getZ());
                    playAnimation(player, com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.SWING); // [parity m1]
                }
                return true;
            }
        }
        return false;
    }

    /** AuxParticlePacketNT {type: vanillaExt, mode: largeexplode, size: 1, count: 1} to everyone within 50 blocks of the target. */
    private static void largeExplode(Level level, double x, double y, double z, double cx, double cy, double cz) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null, cx, cy, cz, 50, new GunTinyExplosionPayload(x, y, z, 1.0F, 1));
        }
    }

    /** Order of ItemBoltgun's bolts array: bolt_spike, steel bolt, tungsten bolt, dura steel bolt; -1 for anything else. */
    public static int boltType(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (BOLT_SPIKE.equals(id)) {
            return 0;
        }
        if (pathEquals(id, "bolt_steel")) {
            return 1;
        }
        if (stack.is(HbmItems.BOLT_TUNGSTEN.get())) {
            return 2;
        }
        if (stack.is(HbmItems.BOLT_DURA_STEEL.get())) {
            return 3;
        }
        return -1;
    }

    private static boolean pathEquals(ResourceLocation id, String path) {
        return id != null && id.getNamespace().equals("atomiclegacy") && id.getPath().equals(path);
    }

    // [parity m1] item renderer: ItemRenderBoltgun via U4LegacyItemRenderSetup (M1LegacyItemRenderers.Boltgun)

    /** ItemBoltgun#getAnimation: RECOIL (1, 0, 1) over 50 ms, back to 0 over 100 ms. */
    @Override
    public com.atomiclegacy.render.anim.BusAnimation getAnimation(
            com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation type, ItemStack stack) {
        return new com.atomiclegacy.render.anim.BusAnimation()
                .addBus("RECOIL", new com.atomiclegacy.render.anim.BusAnimationSequence()
                        .addPos(1, 0, 1, 50)
                        .addPos(0, 0, 1, 100));
    }

    @Override
    public boolean shouldPlayerModelAim(ItemStack stack) {
        return false;
    }

    @Override
    public Class<com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation> getEnum() {
        return com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.class;
    }
}
