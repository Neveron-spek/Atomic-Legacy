package com.atomiclegacy.pollution;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.GlyphidEntity;
import com.atomiclegacy.network.PollutionSyncPayload;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player; // [parity R1: lungs]
import net.minecraft.world.phys.Vec3; // [parity R1: lungs]
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class PollutionEvents {
    private static final net.minecraft.resources.ResourceLocation SOOT_HEALTH = AtomicLegacy.id("soot_anger_health");
    private static final net.minecraft.resources.ResourceLocation SOOT_DAMAGE = AtomicLegacy.id("soot_anger_damage");

    private PollutionEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && HbmConfig.ENABLE_POLLUTION.get()) {
            HbmPollutionWorlds.tick(level);
            handleWorldDestruction(level);
            syncPlayerPollution(level);
        }
    }

    /**
     * PollutionHandler.decorateMob received LivingSpawnEvent.CheckSpawn in
     * 1.7.10.  PositionCheck is its modern spawn-check counterpart.
     */
    @SubscribeEvent
    public static void onLegacySpawnCheck(MobSpawnEvent.PositionCheck event) {
        decorateLegacySpawn(event.getEntity(), event.getLevel());
    }

    /**
     * The old handler also received LivingSpawnEvent.SpecialSpawn from
     * SpawnerAnimals.  Direct addFreshEntity sites are deliberately excluded:
     * old World.spawnEntityInWorld did not emit a LivingSpawnEvent.
     */
    @SubscribeEvent
    public static void onLegacyNaturalSpecialSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL) {
            decorateLegacySpawn(event.getEntity(), event.getLevel());
        }
    }

    /**
     * LivingSpawnEvent.AllowDespawn was another child received by the old
     * base-class listener.  Forge 1.7.10 emitted it only when entityAge's low
     * five bits were all set, and only for non-persistent EntityLiving mobs.
     */
    @SubscribeEvent
    public static void onLegacyDespawnCheck(MobDespawnEvent event) {
        Mob mob = event.getEntity();
        if (!mob.isPersistenceRequired() && (mob.getNoActionTime() & 0x1F) == 0x1F) {
            decorateLegacySpawn(mob, event.getLevel());
        }
    }

    /** Called after the exact old BossSpawnHandler doSpecialSpawn position. */
    public static void onLegacySpecialSpawn(Mob mob, ServerLevel level) {
        decorateLegacySpawn(mob, level);
    }

    private static void decorateLegacySpawn(Mob mob, net.minecraft.world.level.ServerLevelAccessor levelAccessor) {
        if (!HbmConfig.ENABLE_POLLUTION.get()
                || !(levelAccessor instanceof ServerLevel level)
                || !(mob instanceof Enemy)
                || mob instanceof GlyphidEntity) {
            return;
        }
        double soot = HbmPollutionWorlds.get(level, mob.blockPosition(), HbmPollutionType.SOOT);
        if (soot <= HbmConfig.POLLUTION_MOB_BUFF_THRESHOLD.get()) {
            return;
        }
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null && mob.getAttribute(Attributes.MAX_HEALTH).getModifier(SOOT_HEALTH) == null) {
            mob.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(SOOT_HEALTH, 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null && mob.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(SOOT_DAMAGE) == null) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(SOOT_DAMAGE, 1.5D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        mob.heal(mob.getMaxHealth());
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity living)
                || event.getEntity().level().isClientSide || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (!living.isAlive()) {
            return;
        }
        BlockPos eyePos = BlockPos.containing(living.getX(), living.getEyeY(), living.getZ());

        handleLungDisease(level, living, eyePos);

        if (HbmConfig.ENABLE_POLLUTION.get() && living.tickCount % 60 == 0) {
            double poison = HbmPollutionWorlds.get(level, eyePos, HbmPollutionType.POISON);
            if (HbmConfig.ENABLE_POISON_EFFECT.get()
                    && poison > 10.0D) {
                boolean protectedFromPoison = HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.GAS_BLISTERING);
                if (!protectedFromPoison && poison < 25.0D) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                } else if (!protectedFromPoison && poison < 50.0D) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                } else if (!protectedFromPoison) {
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
                }
            }

            double heavyMetal = HbmPollutionWorlds.get(level, eyePos, HbmPollutionType.HEAVYMETAL);
            if (HbmConfig.ENABLE_LEAD_POISONING.get()
                    && heavyMetal > 25.0D) {
                if (!HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.PARTICLE_FINE)) {
                    int amplifier = heavyMetal < 50.0D ? 0 : 2;
                    living.addEffect(new MobEffectInstance(HbmMobEffects.LEAD_POISONING, 100, amplifier));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!HbmConfig.ENABLE_POLLUTION.get() || !HbmConfig.ENABLE_LEAD_FROM_BLOCKS.get() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        double heavyMetal = HbmPollutionWorlds.get(level, event.getPos(), HbmPollutionType.HEAVYMETAL);
        if (heavyMetal < 5.0D) {
            return;
        }
        if (HbmArmorProtection.hasHeadProtection(event.getPlayer(), HbmArmorProtection.HazardClass.PARTICLE_FINE)) {
            return;
        }
        int amplifier = heavyMetal < 10.0D ? 0 : heavyMetal < 25.0D ? 1 : 2;
        event.getPlayer().addEffect(new MobEffectInstance(HbmMobEffects.LEAD_POISONING, 100, amplifier));
    }

    private static void handleWorldDestruction(ServerLevel level) {
        HbmPollutionData data = HbmPollutionData.get(level);
        for (Map.Entry<Long, HbmPollutionData.PollutionValues> entry : data.snapshot().entrySet()) {
            double poison = entry.getValue().get(HbmPollutionType.POISON);
            if (poison < HbmPollutionConstants.POISON_DESTRUCTION_THRESHOLD) {
                continue;
            }
            int baseX = HbmPollutionData.regionX(entry.getKey()) << HbmPollutionConstants.REGION_SHIFT;
            int baseZ = HbmPollutionData.regionZ(entry.getKey()) << HbmPollutionConstants.REGION_SHIFT;
            for (int i = 0; i < HbmPollutionConstants.POISON_DESTRUCTION_COUNT; i++) {
                BlockPos pos = new BlockPos(
                        baseX + level.random.nextInt(HbmPollutionConstants.REGION_SIZE),
                        0,
                        baseZ + level.random.nextInt(HbmPollutionConstants.REGION_SIZE)
                );
                // [parity R1: pollution] provider.chunkExists, then y = getHeightValue(x, z) - rand(3) + 1 (the 1.7.10
                // height map counts every light-blocking block, leaves and fluids included).
                if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                    continue;
                }
                pos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos);
                BlockPos target = pos.atY(pos.getY() - level.random.nextInt(3) + 1);
                BlockState state = level.getBlockState(target);
                Block block = state.getBlock();
                if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT) {
                    level.setBlock(target, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                } else if (com.atomiclegacy.util.LegacyMaterials.isTallGrass(state)
                        || com.atomiclegacy.util.LegacyMaterials.isLeaves(state)
                        || com.atomiclegacy.util.LegacyMaterials.isPlants(state)) { // [parity R1: pollution] tallgrass, Material.leaves/plants
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void syncPlayerPollution(ServerLevel level) {
        HbmPollutionData data = HbmPollutionData.get(level);
        for (ServerPlayer player : level.players()) {
            HbmPollutionData.PollutionValues values = data.get(player.blockPosition());
            PacketDistributor.sendToPlayer(player, PollutionSyncPayload.scaled(
                    values.get(HbmPollutionType.SOOT),
                    values.get(HbmPollutionType.POISON),
                    values.get(HbmPollutionType.HEAVYMETAL),
                    values.get(HbmPollutionType.FALLOUT)
            ));
        }
    }

    private static void handleLungDisease(ServerLevel level, LivingEntity living, BlockPos eyePos) {
        // [parity R1: lungs] EntityEffectHandler.handleLungDisease runs for every EntityLivingBase on the server;
        // creative players only get black lung and asbestos reset (not the other HbmLivingProps values).
        if (living instanceof Player player && player.isCreative()) {
            HbmLivingHazards creative = HbmLivingHazards.get(living);
            creative.setBlackLung(living, 0);
            creative.setAsbestos(living, 0);
            HbmLivingHazards.set(living, creative);
            return;
        }

        HbmLivingHazards hazards = HbmLivingHazards.get(living);
        int blackLung = hazards.getBlackLung();
        if (blackLung > 0 && blackLung < HbmLivingHazards.MAX_BLACK_LUNG * 0.5D) {
            hazards.setBlackLung(living, blackLung - 1);
            HbmLivingHazards.set(living, hazards);
        }

        double blackLungRatio = Math.min(hazards.getBlackLung(), HbmLivingHazards.MAX_BLACK_LUNG) / (double) HbmLivingHazards.MAX_BLACK_LUNG;
        double asbestosRatio = Math.min(hazards.getAsbestos(), HbmLivingHazards.MAX_ASBESTOS) / (double) HbmLivingHazards.MAX_ASBESTOS;
        double soot = HbmConfig.ENABLE_POLLUTION.get()
                ? HbmPollutionWorlds.get(level, eyePos, HbmPollutionType.SOOT)
                : 0.0D;
        if (!(living instanceof Player)) {
            soot = 0.0D; // [parity R1: lungs]
        }
        if (soot > 0.0D && HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.PARTICLE_COARSE)) {
            soot = 0.0D;
        }

        boolean coughs = blackLungRatio > 0.25D || asbestosRatio > 0.25D || soot > 30.0D;
        if (!coughs) {
            return;
        }

        double totalHazard = 1.0D - ((1.0D - blackLungRatio) * (1.0D - asbestosRatio));
        if (totalHazard > 0.75D) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
        }
        if (totalHazard > 0.95D) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }

        double sootDelta = 1.0D - Math.min(soot / 100.0D, 1.0D);
        double total = 1.0D - ((1.0D - blackLungRatio) * (1.0D - asbestosRatio) * sootDelta);
        int frequency = Math.max((int) (1000.0D - 950.0D * total), 20);
        if (level.getGameTime() % frequency == living.getId() % frequency) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), HbmSoundEvents.PLAYER_COUGH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            // [parity R1: lungs] AuxParticlePacketNT "vomit": from feet + eye height (+1 for players, i.e. eyeY - 0.5),
            // speed along the look vector: blood (look + gauss * 0.2) * 0.2, smoke (look + gauss * 0.1) * 0.05.
            Vec3 look = living.getLookAngle();
            double vx = living.getX();
            double vy = living.getY() + living.getEyeHeight() - (living instanceof Player ? 0.5D : 0.0D);
            double vz = living.getZ();
            if (asbestosRatio > 0.75D || blackLungRatio > 0.75D) {
                for (int i = 0; i < 5; i++) {
                    level.sendParticles(HbmParticleTypes.BLOOD_VOMIT.get(), vx, vy, vz, 0,
                            (look.x + level.random.nextGaussian() * 0.2D) * 0.2D,
                            (look.y + level.random.nextGaussian() * 0.2D) * 0.2D,
                            (look.z + level.random.nextGaussian() * 0.2D) * 0.2D, 1.0D);
                }
            }
            if (blackLungRatio > 0.5D) {
                int count = blackLungRatio > 0.8D ? 50 : 10;
                for (int i = 0; i < count; i++) {
                    level.sendParticles(ParticleTypes.SMOKE, vx, vy, vz, 0,
                            (look.x + level.random.nextGaussian() * 0.1D) * 0.05D,
                            (look.y + level.random.nextGaussian() * 0.1D) * 0.05D,
                            (look.z + level.random.nextGaussian() * 0.1D) * 0.05D, 1.0D);
                }
            }
        }
    }
}
