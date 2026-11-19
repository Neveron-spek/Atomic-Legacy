package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmParityV3Config;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes;
import com.atomiclegacy.network.SerializableRecipePayload;
import com.atomiclegacy.tileentity.RbmkDials;
import com.atomiclegacy.util.ContaminationUtil;
import com.atomiclegacy.worldgen.CraterBiomes;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** [parity b2] Server-side hooks of the b2 batch: RBMK dials, recipe sync on login, crater biome radiation. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class ParityB2Events {
    private ParityB2Events() {
    }

    /** MainRegistry world load: RBMKDials.createDials(world). */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        RbmkDials.createDials(event.getServer().getGameRules());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RbmkDials.clear();
    }

    /**
     * ModEventHandler#onPlayerLogin: with enableServerRecipeSync a dedicated server sends every existing recipe file
     * and then the reinitialize packet. The custom machine recipes are the port's only SerializableRecipe file.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!HbmParityV3Config.enableServerRecipeSync() || !player.server.isDedicatedServer()) return;

        AtomicLegacy.LOGGER.info("Sending recipes to client!");

        boolean hasSent = false;

        File recFile = CustomMachineRecipes.recipeFile();
        if (recFile.exists() && recFile.isFile()) {
            AtomicLegacy.LOGGER.info("Sending recipe file: " + recFile.getName());
            try {
                PacketDistributor.sendToPlayer(player, new SerializableRecipePayload(false, recFile.getName(), Files.readAllBytes(recFile.toPath())));
                hasSent = true;
            } catch (IOException ex) { }
        }

        if (hasSent) {
            PacketDistributor.sendToPlayer(player, SerializableRecipePayload.reinitialize());
        }
    }

    /** EntityEffectHandler.onUpdate: RAD/s of the crater biome under the entity, multiplied while it is wet. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide) return;

        float radiation = craterRadiation(entity.level().getBiome(entity.blockPosition()));
        if (radiation <= 0) return;

        if (entity.isInWaterOrRain()) radiation *= HbmParityV3Config.craterBiomeWaterMult();

        if (radiation > 0) {
            ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, radiation / 20F);
        }
    }

    public static float craterRadiation(Holder<Biome> biome) {
        float radiation = 0;
        if (biome.is(CraterBiomes.CRATER_OUTER)) radiation = HbmParityV3Config.craterBiomeOuterRad();
        if (biome.is(CraterBiomes.CRATER)) radiation = HbmParityV3Config.craterBiomeRad();
        if (biome.is(CraterBiomes.CRATER_INNER)) radiation = HbmParityV3Config.craterBiomeInnerRad();
        return radiation;
    }
}
