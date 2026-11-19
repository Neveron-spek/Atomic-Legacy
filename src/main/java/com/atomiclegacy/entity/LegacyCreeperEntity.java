package com.atomiclegacy.entity;

import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Shared 1.7.10 creeper base used by the five concrete HBM creepers. */
public class LegacyCreeperEntity extends Creeper {
    protected enum Kind { NUCLEAR, TAINTED, PHOSGENE, VOLATILE, GOLD }

    private final Kind kind;
    private int legacyFuse;
    private int legacyPreviousFuse;

    protected LegacyCreeperEntity(EntityType<? extends Creeper> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
    }

    protected static AttributeSupplier.Builder createAttributes(double health, double speed) {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // EntityCreeperNuclear.attackEntityFrom returned false after the
        // entity had already died; preserve that legacy guard before any
        // healing or resistance branch.
        if (kind == Kind.NUCLEAR && !isAlive()) {
            return false;
        }
        // [parity D1] only EntityCreeperNuclear overrides attackEntityFrom; the tainted creeper takes this damage
        if (kind == Kind.NUCLEAR && (source.is(HbmDamageTypes.RADIATION)
                || source.is(HbmDamageTypes.MUD_POISONING))) {
            if (isAlive()) {
                heal(amount);
            }
            return false;
        }
        if (kind == Kind.PHOSGENE) {
            // [parity D1] EntityCreeperPhosgene: !isDamageAbsolute() && !isUnblockable() -> amount -= 4; amount < 0 -> false
            if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS)
                    && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
                amount -= 4.0F;
            }
            if (amount < 0.0F) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    /** True only while Creeper#tick's own swell prologue runs, so the vanilla 30-tick fuse never advances. */
    private boolean inVanillaTick;

    @Override
    public int getSwellDir() {
        return inVanillaTick ? -1 : super.getSwellDir();
    }

    @Override
    public boolean isIgnited() {
        return !inVanillaTick && super.isIgnited();
    }

    @Override
    public void tick() {
        // EntityCreeperNuclear's 1.7.10 onUpdate contaminated every nearby
        // living entity before the vanilla creeper tick.
        if (kind == Kind.NUCLEAR && !level().isClientSide) {
            AABB area = getBoundingBox().inflate(5.0D);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != this)) {
                // [parity K2: creeper] EntityCreeperNuclear.onUpdate: contaminate(RADIATION, CREATIVE, 0.25F) for
                // every living entity in boundingBox.expand(5, 5, 5) (the old copy counted the reading twice).
                com.atomiclegacy.util.ContaminationUtil.contaminate(target, com.atomiclegacy.util.ContaminationUtil.HazardType.RADIATION,
                        com.atomiclegacy.util.ContaminationUtil.ContaminationType.CREATIVE, 0.25F);
            }
        }
        // [parity D1] 1.7.10 EntityCreeper.onUpdate with the subclass fuseTime: the state chosen by the swell AI
        // (kept up to 7 blocks, since the vanilla direction is no longer reset) or by flint and steel advances
        // timeSinceIgnited, "creeper.primed" (1.0, 0.5) plays when it leaves zero, and the legacy explosion fires
        // at fuseTime. The vanilla counter stays at zero because Creeper#tick reads -1 through getSwellDir().
        if (isAlive()) {
            legacyPreviousFuse = legacyFuse;
            if (isIgnited()) {
                setSwellDir(1);
            }
            int direction = getSwellDir();
            if (direction > 0 && legacyFuse == 0) {
                playSound(net.minecraft.sounds.SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            }
            legacyFuse += direction;
            if (legacyFuse < 0) {
                legacyFuse = 0;
            }
            if (legacyFuse >= legacyFuseTime()) {
                legacyFuse = legacyFuseTime();
                if (!level().isClientSide) {
                    detonateLegacy();
                    return;
                }
            }
        }
        inVanillaTick = true;
        try {
            super.tick();
        } finally {
            inVanillaTick = false;
        }
        if (!isAlive()) {
            return;
        }
        if ((kind == Kind.NUCLEAR || kind == Kind.TAINTED)
                && getHealth() < getMaxHealth() && tickCount % 10 == 0) {
            heal(1.0F);
        }
    }

    @Override
    public void aiStep() {
        // the swell AI and everything after Creeper#tick's prologue must see the real direction
        inVanillaTick = false;
        super.aiStep();
    }

    public int legacyFuseTicks() {
        return legacyFuseTime();
    }

    @Override
    public float getSwelling(float partialTick) {
        return Mth.lerp(partialTick, legacyPreviousFuse, legacyFuse)
                / (float) legacyFuseTime();
    }

    private int legacyFuseTime() {
        return kind == Kind.NUCLEAR || kind == Kind.PHOSGENE ?
                (kind == Kind.NUCLEAR ? 75 : 20) : 30;
    }

    private void detonateLegacy() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = position();
        boolean griefing = level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean powered = isPowered();
        discard();
        switch (kind) {
            case NUCLEAR -> {
                if (powered) {
                    // [parity D1] powered EntityCreeperNuclear: "muke" particle + mukeExplosion 15.0 in both branches,
                    // then a bare EntityNukeExplosionMK5.statFac(50) (no mushroom cloud entity) or dealDamage(100).
                    if (griefing) {
                        com.atomiclegacy.explosion.LegacyMukeExplosion.sendMukeEffect(level, center);
                        NukeExplosionManager.scheduleMicroNuke(level, getX(), getY(), getZ(), 50);
                    } else {
                        com.atomiclegacy.explosion.LegacyMukeExplosion.detonateNuclearDamage(
                                level, this, center.add(0.0D, 0.5D, 0.0D), 100.0D);
                    }
                } else if (griefing) {
                    com.atomiclegacy.explosion.LegacyMukeExplosion.detonateMediumMiniNuke(
                            level, this, center.add(0.0D, 0.5D, 0.0D));
                } else {
                    com.atomiclegacy.entity.LegacyProjectileUtil.detonateNuclearSafe(
                            level, this, center.add(0.0D, 0.5D, 0.0D));
                }
            }
            case TAINTED -> LegacyProjectileUtil.detonateTaintedCreeper(level, this, center, powered);
            case PHOSGENE -> LegacyProjectileUtil.detonatePhosgeneCreeper(level, this, center);
            case VOLATILE -> LegacyProjectileUtil.detonateBulkieCreeper(level, this, center, powered, false);
            case GOLD -> LegacyProjectileUtil.detonateBulkieCreeper(level, this, center, powered, true);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (kind == Kind.NUCLEAR && level() instanceof ServerLevel level) {
            HbmAdvancements.awardNearby(level, getBoundingBox().inflate(50.0D), "boss_creeper");
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       DamageSource source, boolean recentlyHit) {
        int looting = legacyLootingLevel(level);
        switch (kind) {
            case NUCLEAR -> {
                dropLegacyCreeperItem(Items.TNT, looting);
                if (random.nextInt(3) == 0) {
                    spawnLegacyItem(level, "coin_creeper");
                }
                if (source.getEntity() instanceof Skeleton
                        || source.is(DamageTypeTags.IS_PROJECTILE)
                        && source.getDirectEntity() instanceof AbstractArrow arrow
                        && arrow.getOwner() == null) {
                    spawnAtLocation(com.atomiclegacy.items.StandardAmmoItem.stackFor(
                            com.atomiclegacy.registry.HbmItems.AMMO_STANDARD.get(),
                            com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType.NUKE_STANDARD));
                }
            }
            case TAINTED -> dropLegacyCreeperItem(Items.TNT, looting);
            case PHOSGENE -> dropLegacyCreeperItem(Items.GUNPOWDER, looting);
            case VOLATILE -> {
                spawnLegacyItem(level, "sulfur", 2 + random.nextInt(3));
                spawnLegacyItem(level, "stick_tnt", 1 + random.nextInt(2));
            }
            case GOLD -> {
                int amount = recentlyHit ? 5 + random.nextInt(6 + looting * 2) : 3;
                spawnLegacyItem(level, "crystal_gold", amount);
            }
        }
        // [parity P3: inherited 1.7.10 EntityCreeper.onDeath] a skeleton kill drops one record from record_13..record_wait.
        if (source.getEntity() instanceof Skeleton) {
            spawnAtLocation(new ItemStack(LEGACY_RECORDS[random.nextInt(LEGACY_RECORDS.length)]));
        }
    }

    // [parity P3] 1.7.10 item ID order of Items.record_13 .. Items.record_wait.
    private static final Item[] LEGACY_RECORDS = {
            Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT
    };

    private void dropLegacyCreeperItem(Item item, int looting) {
        int count = random.nextInt(3) + random.nextInt(1 + looting);
        if (count > 0) {
            spawnAtLocation(new ItemStack(item, count));
        }
    }

    private int legacyLootingLevel(ServerLevel level) {
        LivingEntity killer = getLastHurtByMob();
        if (killer == null || killer.getMainHandItem().isEmpty()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.LOOTING),
                killer.getMainHandItem());
    }

    private void spawnLegacyItem(ServerLevel level, String id) {
        spawnLegacyItem(level, id, 1);
    }

    private void spawnLegacyItem(ServerLevel level, String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(com.atomiclegacy.AtomicLegacy.id(id));
        if (item != Items.AIR) {
            spawnAtLocation(new ItemStack(item, count));
        }
    }

    protected Kind kind() {
        return kind;
    }

    private static boolean isLegacyRadiationImmune(LivingEntity target) {
        return target instanceof LegacyNuclearCreeperEntity
                || target instanceof LegacyTaintedCreeperEntity
                || target instanceof LegacyCyberCrabEntity
                || target instanceof LegacyMaskManEntity
                || target instanceof LegacyRadBeastEntity
                || target instanceof LegacyUfoEntity
                || target instanceof LegacyChopperEntity
                || target instanceof LegacyWormHeadEntity
                || target instanceof LegacyWormBodyEntity
                || target instanceof MushroomCow
                || target instanceof Zombie
                || target instanceof Skeleton
                || target instanceof LegacyQuackosEntity
                || target instanceof Ocelot;
    }
}
