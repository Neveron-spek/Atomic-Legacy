package com.atomiclegacy.entity;

import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.pollution.HbmArmorProtection;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityTaintCrab. */
public final class LegacyTaintCrabEntity extends LegacyCyberCrabEntity {
    public LegacyTaintCrabEntity(EntityType<? extends LegacyTaintCrabEntity> type, Level level) {
        super(type, level, Kind.TAINT);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(25.0D, 0.5D);
    }

    @Override
    public void tick() {
        updateTeslaTargets(1.25D, 10.0D);
        if (level().isClientSide) {
            super.tick();
            return;
        }
        AABB area = new AABB(getX() - 5.0D, getY() - 5.0D, getZ() - 5.0D,
                getX() + 5.0D, getY() + 5.0D, getZ() + 5.0D);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (target instanceof LegacyCyberCrabEntity) {
                continue;
            }
            // [parity J1] EntityTaintCrab.onLivingUpdate: HbmPotion.radiation 10 ticks, amplifier 15.
            com.atomiclegacy.util.ContaminationUtil.addRadiationEffect(target, 10, 15);
        }
        super.tick();
    }

    /** EntityTaintCrab uses the 7.62 mm FMJ bullet configuration, with a
     * one-block/tick projectile and base damage 10; it is not the tau round
     * used by the generic cyber crab. */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        // [parity D1] new EntityBulletBaseMK4(this, r762_fmj, 10F, 0F, 0, 0, 0): from posY + eyeHeight along the
        // entity's own yaw/pitch, unit heading advanced by BulletConfig.velocity per tick (no 1 block/tick cap),
        // plus the "vanilla"/"flame" particle with 0.3 of that heading.
        Vec3 origin = position().add(0.0D, getEyeHeight(), 0.0D);
        Vec3 direction = calculateViewVector(getXRot(), getYRot()).normalize();
        LegacyBulletEntity bullet = new LegacyBulletEntity(
                level(), origin.x, origin.y, origin.z, direction,
                StandardAmmoItem.StandardAmmoType.R762_FMJ, 10.0F, this, 0.0F);
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    origin.x, origin.y, origin.z, 0, direction.x, direction.y, direction.z, 0.3D);
        }
        level().addFreshEntity(bullet);
        playSound(HbmSoundEvents.WEAPON_SAW_SHOOT.get(), 1.0F, 0.5F);
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
