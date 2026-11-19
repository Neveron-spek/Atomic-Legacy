package com.atomiclegacy.entity;

import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DigammaSpearEntity extends Entity {
    private static final EntityDataAccessor<Integer> TICKS_IN_GROUND =
            SynchedEntityData.defineId(DigammaSpearEntity.class, EntityDataSerializers.INT);

    public DigammaSpearEntity(EntityType<? extends DigammaSpearEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public DigammaSpearEntity(Level level, double x, double y, double z) {
        this(HbmEntityTypes.DIGAMMA_SPEAR.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TICKS_IN_GROUND, 0);
    }

    public int ticksInGround() {
        return this.entityData.get(TICKS_IN_GROUND);
    }

    private void setTicksInGround(int ticks) {
        this.entityData.set(TICKS_IN_GROUND, ticks);
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        setDeltaMovement(0.0D, -0.2D, 0.0D);

        BlockPos pos = blockPosition();
        if (level().getBlockState(pos.below()).isAir()) {
            move(MoverType.SELF, getDeltaMovement());
            if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
                strikeNearbySurface(serverLevel);
            }
            if (level().getBlockState(pos.below(3)).isAir()) {
                setTicksInGround(0);
            }
        } else {
            int ticks = ticksInGround() + 1;
            setTicksInGround(ticks);
            if (!level().isClientSide && ticks > 100) {
                detonate();
            }
        }
    }

    private void strikeNearbySurface(ServerLevel serverLevel) {
        double ix = getX() + random.nextGaussian() * 25.0D;
        double iz = getZ() + random.nextGaussian() * 25.0D;
        BlockPos surface = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(ix, getY(), iz)).above(2);
        // [parity K2: spear] EntitySpear: ExplosionNT(7.5F, NOHURT/NOPARTICLE/NODROP/NOSOUND + DIGAMMA or DIGAMMA_CIRCUIT)
        // at the surface point; it adds no chunk radiation (the former +250/+75 RAD was invented). The ExplosionNT
        // digamma block attribute itself is not ported here.
        for (Player player : serverLevel.players()) {
            // contaminate(player, DIGAMMA, DIGAMMA, 0.05F)
            com.atomiclegacy.util.ContaminationUtil.contaminate(player, com.atomiclegacy.util.ContaminationUtil.HazardType.DIGAMMA,
                    com.atomiclegacy.util.ContaminationUtil.ContaminationType.DIGAMMA, 0.05F);
            HbmAdvancements.award(player, "digamma_kauai_moho");
        }
    }

    private void detonate() {
        Level level = level();
        // [parity K2: spear] every loaded living entity of the world: contaminate(DIGAMMA, DIGAMMA2, 10F); no damage,
        // no radiation (the 256-block box, 2 digamma damage and +100 RAD reading were invented).
        if (level instanceof ServerLevel server) {
            // [e3] snapshot: contamination can kill, which must not mutate the entity lookup under iteration
            java.util.List<Entity> snapshot = new java.util.ArrayList<>();
            server.getAllEntities().forEach(snapshot::add);
            for (Entity entity : snapshot) {
                if (entity instanceof LivingEntity living) {
                    com.atomiclegacy.util.ContaminationUtil.contaminate(living, com.atomiclegacy.util.ContaminationUtil.HazardType.DIGAMMA,
                            com.atomiclegacy.util.ContaminationUtil.ContaminationType.DIGAMMA2, 10F);
                }
            }
        }
        level.playSound(null, getX(), getY(), getZ(), HbmSoundEvents.DIGAMMA_FLASH.get(), SoundSource.PLAYERS, 25000.0F, 1.0F);
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ticks_in_ground", ticksInGround());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setTicksInGround(tag.getInt("ticks_in_ground"));
    }
}
