package com.atomiclegacy.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Direct 1.7.10 EntityParasiteMaggot port used by infected nuclear Glyphids. */
public final class ParasiteMaggotEntity extends Monster {
    /** [parity J1] EntityCreature old AI (path following, wander, idle look). */
    private final com.atomiclegacy.entity.ai.LegacyCreatureAi legacyAi;

    public ParasiteMaggotEntity(EntityType<? extends ParasiteMaggotEntity> type, Level level) {
        super(type, level); // [parity D1] EntityMob experienceValue 5 is inherited
        legacyAi = new com.atomiclegacy.entity.ai.LegacyCreatureAi(this,
                (com.atomiclegacy.entity.ai.LegacyPathNavigation) navigation);
        moveControl = com.atomiclegacy.entity.ai.LegacyCreatureAi.noMoveControl(this);
        lookControl = com.atomiclegacy.entity.ai.LegacyCreatureAi.headFollowsYaw(this);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new com.atomiclegacy.entity.ai.LegacyPathNavigation(this, level, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                // EntityParasiteMaggot used getClosestVulnerablePlayer(..., 16)
                // rather than EntityMob's default follow range.
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    private int attackCooldown;

    /**
     * [parity D1] EntityParasiteMaggot runs the old EntityCreature / EntityMob AI (isAIEnabled() false): targeting,
     * pathing, wandering, path following and the melee hit come from {@link #customServerAiStep()}; no goals.
     */
    @Override
    protected void registerGoals() {
    }

    /** Old AI: moveForward is the movementSpeed attribute (1.0) and getAIMoveSpeed() is the constant 0.1. */
    @Override
    public float getSpeed() {
        return 0.1F;
    }

    @Override
    public void tick() {
        yBodyRot = getYRot(); // renderYawOffset = rotationYaw
        super.tick();
    }

    /** EntityMob.attackEntityFrom: whoever damages the mob becomes entityToAttack. */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker != this) {
            setTarget(attacker);
        }
        return hurt;
    }

    /**
     * EntityCreature.updateEntityActionState with findPlayerToAttack = getClosestVulnerablePlayerToEntity(16) (no
     * line of sight) and EntityMob.attackEntity: every 20 ticks, distance < 2.0, overlapping Y ranges, target seen.
     * The target is kept until it dies or turns creative; a path is requested when there is none or 1/20 per tick.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (attackCooldown > 0) attackCooldown--;
        legacyAi.tickFleeing();
        net.minecraft.world.entity.LivingEntity target = getTarget();
        if (target == null) {
            Player player = level().getNearestPlayer(net.minecraft.world.entity.ai.targeting.TargetingConditions
                    .forCombat().range(16.0D).ignoreLineOfSight(), this);
            if (player != null) {
                setTarget(player);
                target = player;
                legacyAi.navigation().setLegacyPath(legacyAi.navigation().pathToEntity(player, 16.0F, false));
            }
        } else if (target.isAlive()) {
            if (hasLineOfSight(target) && attackCooldown <= 0 && distanceTo(target) < 2.0F
                    && target.getBoundingBox().maxY > getBoundingBox().minY
                    && target.getBoundingBox().minY < getBoundingBox().maxY) {
                attackCooldown = 20;
                doHurtTarget(target);
            }
        } else {
            setTarget(null);
            target = null;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            setTarget(null);
            target = null;
        }
        legacyAi.repath(target, false, false,
                () -> legacyAi.updateWanderPath(pos -> getWalkTargetValue(pos, level())));
        legacyAi.followPath(target, false, getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        // EntityParasiteMaggot#canTriggerWalking returned false in 1.7.10.
        return true;
    }

}
