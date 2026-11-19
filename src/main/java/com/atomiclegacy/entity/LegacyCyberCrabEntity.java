package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.tileentity.TeslaCoilBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/** Shared 1.7.10 EntityCyberCrab behaviour. */
public class LegacyCyberCrabEntity extends Monster implements RangedAttackMob {
    public enum Kind { CYBER, TESLA, TAINT }
    private final Kind kind;
    private List<Vec3> targets = List.of();

    public LegacyCyberCrabEntity(EntityType<? extends LegacyCyberCrabEntity> type, Level level, Kind kind) {
        super(type, level);
        moveControl = new com.atomiclegacy.entity.ai.LegacyMoveControl(this); // [parity J1] EntityMoveHelper
        this.kind = kind;
        // EntityCyberCrab explicitly opted out of water pathing in 1.7.10.
        getNavigation().setCanFloat(false);
        // [parity D1] the crab is setDead() before the death animation could award experience
        xpReward = 0;
        // [parity D1] PathNavigate.setAvoidsWater(true)
        setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
    }

    /** [parity J1] PathNavigate with the 1.7.10 path finder (World.getPathEntityToEntity / getEntityPathToXYZ). */
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new com.atomiclegacy.entity.ai.LegacyPathNavigation(this, level, false);
    }

    public static AttributeSupplier.Builder createAttributes(double health, double speed) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed);
    }

    @Override
    protected void registerGoals() {
        if (kind != Kind.TAINT) {
            goalSelector.addGoal(0, new PanicGoal(this, 0.75D));
        }
        goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5D));
        // EntityTaintCrab replaces the base arrow goal with the old 5-tick,
        // 50-block attack cadence; the other two variants retain 60--80.
        if (kind == Kind.TAINT) {
            goalSelector.addGoal(4, new RangedAttackGoal(this, 0.5D, 5, 5, 50.0F));
        } else {
            goalSelector.addGoal(4, new RangedAttackGoal(this, 0.5D, 60, 80, 15.0F));
        }
        // [parity D1] EntityAINearestAttackableTarget(EntityPlayer, chance 0, sight) and
        // (EntityLiving [= Mob, never players or armour stands], chance 0, sight, nearbyOnly, selector)
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, 0, true, false, null));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class,
                0, true, true,
                entity -> !(entity instanceof LegacyCyberCrabEntity) && !(entity instanceof Creeper)));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // EntityCyberCrab only rejected TAU damage in 1.7.10; digamma
        // damage remained effective.  Keep this distinction explicit.
        if (source.is(HbmDamageTypes.TAU)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (isInWaterOrRain() || isOnFire())) {
            hurt(damageSources().generic(), 10.0F);
        }
        // [parity D1] EntityCyberCrab.onUpdate: at health <= 0 the crab is removed at once (no death animation,
        // hence no experience orbs; the drops were already made by onDeath) and explodes: 3.0 taint, 0.1 others.
        if (getHealth() <= 0.0F && !isRemoved()) {
            if (!level().isClientSide) {
                discard();
                level().explode(this, getX(), getY(), getZ(), kind == Kind.TAINT ? 3.0F : 0.1F, false,
                        Level.ExplosionInteraction.NONE);
            }
        }
    }

    public List<Vec3> targets() {
        return targets;
    }

    protected final void updateTeslaTargets(double emitterHeight, double range) {
        Vec3 origin = position().add(0.0D, emitterHeight, 0.0D);
        targets = level().isClientSide
                ? TeslaCoilBlockEntity.targetPoints(level(), origin, range, this)
                : TeslaCoilBlockEntity.zap(level(), origin, range, this);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        // [parity D1] new EntityBullet(world, this, target, 1.6F, 2) + setIsCritical + setTau + damage 3: starts one
        // horizontal block towards the target at eyeY - 0.1, aims at target.minY + height / 3 measured from the
        // crab's own X/Z, heading noise 0.0075 * 2; the critical tau round ignores blocks, is never stopped by an
        // entity, lives 250 ticks and deals 3 tau damage (armour bypassing) to the nearest entity it crosses.
        double dx = target.getX() - getX();
        double originY = getY() + getEyeHeight() - 0.10000000149011612D;
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - originY;
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal >= 1.0E-7D) {
            Vec3 origin = new Vec3(getX() + dx / horizontal, originY, getZ() + dz / horizontal);
            level().addFreshEntity(new LegacyBossProjectileEntity(level(), this, origin, new Vec3(dx, dy, dz),
                    LegacyBossProjectileEntity.Type.CRAB_TAU, target, 2.0F));
        }
        playSound(HbmSoundEvents.WEAPON_SAW_SHOOT.get(), 1.0F, 2.0F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HbmSoundEvents.ENTITY_CYBERCRAB.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundEvents.ENTITY_CYBERCRAB.get();
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       DamageSource source, boolean recentlyHit) {
        // EntityTaintCrab#getDropItem was reached through the old mob-loot
        // path: its common copper coil uses the inherited EntityMob count
        // formula.  EntityTeslaCrab has no common drop at all.
        // [parity P3: EntityLiving.dropFewItems runs without the recentlyHit check; only dropRareDrop needs it]
        if (kind == Kind.TAINT) {
            int count = random.nextInt(3) + random.nextInt(1 + legacyLootingLevel());
            if (count > 0) {
                spawnAtLocation(new net.minecraft.world.item.ItemStack(HbmItems.COIL_COPPER.get(), count));
            }
        }
        if (!recentlyHit) {
            return;
        }
        if (random.nextInt(200) < 5 + legacyLootingLevel()) {
            net.minecraft.world.item.Item tungsten = BuiltInRegistries.ITEM.get(
                    com.atomiclegacy.AtomicLegacy.id("coil_magnetized_tungsten"));
            if (kind == Kind.TESLA) {
                spawnAtLocation(new net.minecraft.world.item.ItemStack(HbmItems.COIL_COPPER.get()));
            } else if (kind == Kind.TAINT && tungsten != net.minecraft.world.item.Items.AIR) { // [parity D1] EntityCyberCrab has no dropRareDrop
                spawnAtLocation(new net.minecraft.world.item.ItemStack(tungsten));
            }
        }
    }

    private int legacyLootingLevel() {
        LivingEntity killer = getLastHurtByMob();
        if (killer == null || killer.getMainHandItem().isEmpty()) {
            return 0;
        }
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),
                killer.getMainHandItem());
    }

    protected Kind kind() {
        return kind;
    }
}
