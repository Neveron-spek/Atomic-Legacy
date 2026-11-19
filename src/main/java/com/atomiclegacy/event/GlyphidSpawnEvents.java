package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.GlyphidEntity;
import com.atomiclegacy.pollution.HbmPollutionType;
import com.atomiclegacy.pollution.HbmPollutionWorlds;
import com.atomiclegacy.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.Difficulty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Natural Scout and escort spawning from HBM 1.7.10's pollution handler. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class GlyphidSpawnEvents {
    private GlyphidSpawnEvents() {
    }

    /**
     * [parity A2: glyphid] PollutionHandler.rampantTargetSetter (PlayerSleepInBedEvent): the bed coordinates of
     * every sleep attempt, recorded only while rampantGlyphidGuidance is enabled.
     */
    @SubscribeEvent
    public static void onPlayerSleep(CanPlayerSleepEvent event) {
        if (HbmConfig.glyphidGuidance()) {
            GlyphidEntity.setGuidanceTarget(event.getPos());
        }
    }

    /**
     * DamageResistanceHandler.onEntityDamaged: IResistanceProvider.onDamageDealt(source, amount) with the
     * damage left after DT/DR, which rolls EntityGlyphid.isArmorBroken / breakOffArmor.
     */
    @SubscribeEvent
    public static void onGlyphidDamageDealt(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof GlyphidEntity glyphid && !glyphid.level().isClientSide) {
            glyphid.onLegacyDamageDealt(event.getNewDamage());
        }
    }

    @SubscribeEvent
    public static void onNaturalSpawnPosition(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL) {
            EntityType<?> type = event.getEntity().getType();
            boolean legacyCreeper = type == HbmEntityTypes.PHOSGENE_CREEPER.get()
                    || type == HbmEntityTypes.VOLATILE_CREEPER.get()
                    || type == HbmEntityTypes.GOLD_CREEPER.get();
            if (legacyCreeper && (!(event.getLevel() instanceof ServerLevel level)
                    || level.dimension() != Level.OVERWORLD)) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
            if ((type == HbmEntityTypes.VOLATILE_CREEPER.get()
                    || type == HbmEntityTypes.GOLD_CREEPER.get())
                    && event.getY() > 40.0D) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
    }

    /**
     * [parity A2: glyphid] PollutionHandler.rampantScoutPopulator. The 1.7.10 handler listened to
     * WorldEvent.PotentialSpawns, which fires for every natural spawn-list lookup whether or not a mob ends up
     * spawning there; LevelEvent.PotentialSpawns is the same hook (1.21 additionally looks the list up once per
     * spawn attempt of a pack). The scout and its digger escort are placed at the integer event coordinates.
     */
    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!HbmConfig.glyphidNaturalScoutSpawn()
                || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || event.isCanceled()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!level.canSeeSky(pos)
                || level.random.nextInt(HbmConfig.GLYPHID_NATURAL_SCOUT_CHANCE.get()) != 0
                || HbmPollutionWorlds.get(level, pos, HbmPollutionType.SOOT)
                < HbmConfig.GLYPHID_NATURAL_SCOUT_THRESHOLD.get()) {
            return;
        }
        GlyphidEntity scout = new GlyphidEntity(HbmEntityTypes.GLYPHID.get(), level);
        scout.setVariant(GlyphidEntity.Variant.SCOUT);
        scout.moveTo(pos.getX(), pos.getY(), pos.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
        // EntityGlyphidScout.isValidLightLevel: skylightSubtracted is 10 during a thunderstorm
        if ((level.isThundering() ? level.getMaxLocalRawBrightness(pos, 10)
                : level.getMaxLocalRawBrightness(pos)) > 7) {
            scout.discard();
            return;
        }
        // PollutionHandler constructed the escort, then called
        // setLocationAndAngles on the scout a second time. Keep that random
        // draw and final scout yaw exactly, including the redundant call.
        GlyphidEntity digger = new GlyphidEntity(HbmEntityTypes.GLYPHID.get(), level);
        digger.setVariant(GlyphidEntity.Variant.DIGGER);
        scout.moveTo(pos.getX(), pos.getY(), pos.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
        digger.moveTo(pos.getX(), pos.getY(), pos.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
        // EntityGlyphid.getCanSpawnHere: not peaceful, no entity / block collision, no liquid
        boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL;
        if (!peaceful && scout.checkSpawnObstruction(level)) {
            level.addFreshEntity(scout);
        } else {
            scout.discard();
        }
        if (!peaceful && digger.checkSpawnObstruction(level)) {
            level.addFreshEntity(digger);
        } else {
            digger.discard();
        }
    }
}
