package com.atomiclegacy.entity;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.items.GasMaskItem;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** 1.7.10 EntityMaskMan base attributes and target behaviour. */
public final class LegacyMaskManEntity extends Monster {
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.atomiclegacy.entity_mob_mask_man"),
            net.minecraft.world.BossEvent.BossBarColor.RED,
            net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
    private float previousHealth = 1000.0F;

    public LegacyMaskManEntity(EntityType<? extends LegacyMaskManEntity> type, Level level) {
        super(type, level);
        moveControl = new com.atomiclegacy.entity.ai.LegacyMoveControl(this); // [parity J1] EntityMoveHelper
        setPersistenceRequired();
        xpReward = 100;
    }

    /** [parity J1] PathNavigate with the 1.7.10 path finder (World.getPathEntityToEntity / getEntityPathToXYZ). */
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new com.atomiclegacy.entity.ai.LegacyPathNavigation(this, level, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    // [parity A2: maskman] Priorities and mutex bits of EntityMaskMan: the approach task uses MOVE|LOOK (mutex 3);
    // EntityAIMaskmanMinigun/Lasergun have mutex 0 and therefore run alongside everything else.
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new CasualApproachGoal(this));
        goalSelector.addGoal(2, new MaskmanMinigunGoal(this));
        goalSelector.addGoal(3, new MaskmanLasergunGoal(this));
        goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ThrownEgg && random.nextInt(10) == 0) {
            xpReward = 0;
            setHealth(0.0F);
            return true;
        }
        // [parity A2] isFireDamage / isMagicDamage (magic, indirectMagic, thorns) zero the amount but still go to super
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) amount = 0.0F;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) amount = 0.0F;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) amount *= 0.5F;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;
        if (amount > 50.0F) {
            amount = 50.0F + (amount - 50.0F) * 0.5F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive() && previousHealth >= getMaxHealth() * 0.5F && getHealth() < getMaxHealth() * 0.5F) {
            previousHealth = getHealth();
            if (!level().isClientSide) {
                // createExplosion(this, x, y + 4, z, 2.5F, true): block damage, no fire
                level().explode(this, getX(), getY() + 4.0D, getZ(), 2.5F, false, ExplosionInteraction.TNT);
            }
        } else {
            previousHealth = getHealth();
        }
        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (level() instanceof net.minecraft.server.level.ServerLevel level) {
            HbmAdvancements.awardNearby(level, getBoundingBox().inflate(100.0D), "boss_maskman"); // boundingBox.expand(100, 100, 100)
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        Item maskItem = BuiltInRegistries.ITEM.get(AtomicLegacy.id("gas_mask_m65"));
        Item filterItem = BuiltInRegistries.ITEM.get(AtomicLegacy.id("gas_mask_filter_combo"));
        if (maskItem != Items.AIR && filterItem != Items.AIR) {
            ItemStack mask = new ItemStack(maskItem);
            GasMaskItem.installFilter(mask, new ItemStack(filterItem), this);
            spawnAtLocation(mask);
        }
        spawnLegacyDrop(level, "coin_maskman");
        spawnLegacyDrop(level, "bottled_cloud");
        spawnAtLocation(new ItemStack(Items.SKELETON_SKULL));
    }

    private void spawnLegacyDrop(net.minecraft.server.level.ServerLevel level, String id) {
        Item item = BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
        if (item != Items.AIR) {
            spawnAtLocation(new ItemStack(item));
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.atomiclegacy.entity_mob_mask_man");
    }

    private static final class CasualApproachGoal extends Goal {
        private final LegacyMaskManEntity owner;
        private Vec3 pathTarget;
        private int pathTimer;
        private int failedPathPenalty;
        private double lastX, lastY, lastZ;

        private CasualApproachGoal(LegacyMaskManEntity owner) {
            this.owner = owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = owner.getTarget();
            if (target == null || !target.isAlive() || !(target instanceof Player)) return false;
            if (--pathTimer <= 0) {
                pathTarget = approachPosition(target);
                owner.getNavigation().createPath(pathTarget.x, pathTarget.y, pathTarget.z, 0);
                pathTimer = 4 + owner.random.nextInt(7);
                return owner.getNavigation().getPath() != null;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = owner.getTarget();
            return target != null && target.isAlive() && !owner.getNavigation().isDone();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            if (pathTarget != null) owner.getNavigation().moveTo(pathTarget.x, pathTarget.y, pathTarget.z, 1.0D);
            pathTimer = 0;
        }

        @Override
        public void stop() {
            owner.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = owner.getTarget();
            if (target == null) return;
            owner.getLookControl().setLookAt(target, 30.0F, 30.0F);
            pathTimer--;
            double distance = owner.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            if (!owner.getSensing().hasLineOfSight(target)) {
                return;
            }
            if (pathTimer <= 0 && (lastX == 0.0D && lastY == 0.0D && lastZ == 0.0D
                    || target.distanceToSqr(lastX, lastY, lastZ) >= 1.0D
                    || owner.random.nextFloat() < 0.05F)) {
                lastX = target.getX(); lastY = target.getBoundingBox().minY; lastZ = target.getZ();
                pathTimer = failedPathPenalty + 4 + owner.random.nextInt(7);
                Path path = owner.getNavigation().getPath();
                if (path != null) {
                    Node finalNode = path.getEndNode();
                    if (finalNode != null
                            && target.distanceToSqr(finalNode.x, finalNode.y, finalNode.z) < 1.0D) {
                        failedPathPenalty = 0;
                    } else {
                        failedPathPenalty += 10;
                    }
                } else {
                    failedPathPenalty += 10;
                }
                if (distance > 1024.0D) {
                    pathTimer += 10;
                } else if (distance > 256.0D) {
                    pathTimer += 5;
                }
                Vec3 next = approachPosition(target);
                if (!owner.getNavigation().moveTo(next.x, next.y, next.z, 1.0D)) {
                    pathTimer += 15;
                }
            }
        }

        private Vec3 approachPosition(LivingEntity target) {
            Vec3 delta = owner.position().subtract(target.position());
            double range = Math.min(delta.length(), 20.0D) - 10.0D;
            delta = delta.normalize();
            return owner.position().add(delta.scale(range)).add(
                    owner.random.nextGaussian() * 2.0D,
                    delta.y - 5.0D + owner.random.nextInt(11),
                    owner.random.nextGaussian() * 2.0D);
        }
    }

    /** EntityAIMaskmanMinigun(owner, true, true, 3): r762_fmj every 3 ticks while the target is 5..10 blocks away. */
    private static final class MaskmanMinigunGoal extends Goal {
        private final LegacyMaskManEntity owner;
        private LivingEntity target;
        private int timer = MINIGUN_DELAY;

        private MaskmanMinigunGoal(LegacyMaskManEntity owner) {
            this.owner = owner;
        }

        @Override
        public boolean canUse() {
            LivingEntity entity = owner.getTarget();
            if (entity == null || !entity.isAlive()) return false;
            target = entity;
            double distance = owner.position().distanceTo(target.position());
            return distance > 5.0D && distance < 10.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || !owner.getNavigation().isDone();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            timer--;
            if (target != null) owner.getLookControl().setLookAt(target, 15.0F, 15.0F);
            if (timer <= 0) {
                timer = MINIGUN_DELAY;
                owner.fireMinigun();
            }
        }
    }

    public static final int MINIGUN_DELAY = 3;
    /** EnumLaserAttack {delay, amount}: ORB(60, 5), MISSILE(10, 10), SPLASH(40, 3). */
    public static final int[][] LASER_ATTACKS = {{60, 5}, {10, 10}, {40, 3}};

    /** EntityAIMaskmanLasergun: ORB / MISSILE / SPLASH volleys while the target is more than 10 blocks away. */
    private static final class MaskmanLasergunGoal extends Goal {
        private final LegacyMaskManEntity owner;
        private LivingEntity target;
        private int timer;
        private int attackCount;
        private int attack;

        private MaskmanLasergunGoal(LegacyMaskManEntity owner) {
            this.owner = owner;
            this.attack = owner.random.nextInt(3);
        }

        @Override
        public boolean canUse() {
            LivingEntity entity = owner.getTarget();
            if (entity == null) return false;
            target = entity;
            return owner.position().distanceTo(target.position()) > 10.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || !owner.getNavigation().isDone();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            timer--;
            if (timer <= 0 && target != null) {
                timer = LASER_ATTACKS[attack][0];
                owner.fireLaserAttack(attack, target);
                attackCount++;
                if (attackCount >= LASER_ATTACKS[attack][1]) {
                    attackCount = 0;
                    attack = nextLaserAttack(attack, owner.random.nextInt(2));
                }
            }
            owner.setYRot(owner.getYHeadRot());
        }
    }

    /** newAtk = ordinal + rand.nextInt(values().length - 1), modulo 3: the same attack or the next one. */
    public static int nextLaserAttack(int attack, int roll) {
        return (attack + roll) % 3;
    }

    /** EntityBulletBaseMK4(owner, r762_fmj, 5F, 0.075F, -1.5, -1.5, 0) + hbm:weapon.calShoot. */
    private void fireMinigun() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        float yaw = getYRot();
        float pitch = getXRot();
        Vec3 offset = new Vec3(-1.5D, -1.5D, 0.0D)
                .xRot(-pitch / 180.0F * (float) Math.PI)
                .yRot(-yaw / 180.0F * (float) Math.PI);
        Vec3 direction = new Vec3(
                -net.minecraft.util.Mth.sin(yaw / 180.0F * (float) Math.PI) * net.minecraft.util.Mth.cos(pitch / 180.0F * (float) Math.PI),
                -net.minecraft.util.Mth.sin(pitch / 180.0F * (float) Math.PI),
                net.minecraft.util.Mth.cos(yaw / 180.0F * (float) Math.PI) * net.minecraft.util.Mth.cos(pitch / 180.0F * (float) Math.PI));
        LegacyBulletEntity bullet = new LegacyBulletEntity(server,
                getX() + offset.x, getY() + getEyeHeight() + offset.y, getZ() + offset.z, direction,
                StandardAmmoItem.StandardAmmoType.R762_FMJ, 5.0F, this, 0.075F);
        server.addFreshEntity(bullet);
        playSound(HbmSoundEvents.GUN_MINIGUN_FIRE.get(), 1.0F, 1.0F);
    }

    /** EntityBulletBaseNT(world, config, owner, target, motion, deviation) origin and heading. */
    private LegacyBossProjectileEntity maskmanBullet(LivingEntity target, LegacyBossProjectileEntity.Type type, float deviation) {
        double sourceY = getY() + getEyeHeight() - 0.10000000149011612D;
        double dx = target.getX() - getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - sourceY;
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-7D) {
            // the 1.7.10 constructor leaves such a bullet at the origin without motion; nothing useful is spawned
            return null;
        }
        return new LegacyBossProjectileEntity(level(), this,
                new Vec3(getX() + dx / horizontal, sourceY, getZ() + dz / horizontal), new Vec3(dx, dy, dz), type, target, deviation);
    }

    private void fireLaserAttack(int attack, LivingEntity target) {
        if (level().isClientSide) return;
        switch (attack) {
            case 0 -> { // ORB: MASKMAN_ORB, 2.0F, motionY += 0.5
                LegacyBossProjectileEntity orb = maskmanBullet(target, LegacyBossProjectileEntity.Type.MASKMAN_ORB, 0.0F);
                if (orb != null) {
                    orb.overrideMotion(orb.getDeltaMovement().add(0.0D, 0.5D, 0.0D));
                    level().addFreshEntity(orb);
                }
                playSound(HbmSoundEvents.WEAPON_TESLA_SHOOT.get(), 1.0F, 1.0F);
            }
            case 1 -> { // MISSILE: MASKMAN_ROCKET lobbed upward
                LegacyBossProjectileEntity missile = maskmanBullet(target, LegacyBossProjectileEntity.Type.MASKMAN_ROCKET, 0.0F);
                if (missile != null) {
                    missile.overrideMotion(new Vec3((target.getX() - getX()) * 0.05D,
                            0.5D + random.nextDouble() * 0.5D, (target.getZ() - getZ()) * 0.05D));
                    level().addFreshEntity(missile);
                }
                playSound(HbmSoundEvents.GUN_UNDERBARREL_FIRE.get(), 1.0F, 1.0F);
            }
            default -> { // SPLASH: five MASKMAN_TRACER, no sound
                for (int i = 0; i < 5; i++) {
                    LegacyBossProjectileEntity tracer = maskmanBullet(target, LegacyBossProjectileEntity.Type.MASKMAN_TRACER, 0.05F);
                    if (tracer != null) level().addFreshEntity(tracer);
                }
            }
        }
    }
}
