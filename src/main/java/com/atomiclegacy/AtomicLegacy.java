package com.atomiclegacy;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.integration.create.HbmCreateAdditionCompat;
import com.atomiclegacy.integration.create.HbmCreateMultiblockCompat;
import com.atomiclegacy.integration.createbigcannons.HbmCreateBigCannonsCompat;
import com.atomiclegacy.integration.immersiveengineering.HbmImmersiveEngineeringCompat;
import com.atomiclegacy.integration.curios.PortableCrateCuriosIntegration;
import com.atomiclegacy.items.LegacyItemComponents;
import com.atomiclegacy.handler.LegacyDispenserBehaviors;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmStats;
import com.atomiclegacy.registry.HbmArmorMaterials;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmCreativeTabs;
import com.atomiclegacy.registry.HbmDataAttachments;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmMenus;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.registry.HbmParticleTypes;
import com.atomiclegacy.network.HbmNetwork;
import com.atomiclegacy.registry.HbmConditionSerializers;
import com.atomiclegacy.registry.HbmChunkTickets;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.registry.HbmWorldgenFeatures;
import com.atomiclegacy.registry.HbmWorldgenStructures;
import com.atomiclegacy.registry.HbmBiomeModifierSerializers;
import com.atomiclegacy.registry.LegacyHbmContent;
import com.atomiclegacy.registry.PortStatus;
import com.atomiclegacy.worldgen.structure.HbmLegacyScatterStructure;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AtomicLegacy.MOD_ID)
public class AtomicLegacy {
    public static final String MOD_ID = "atomiclegacy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** [parity J1] MainRegistry.generalOverride: never assigned from a config in 1.7.10, so always 0. */
    public static final int GENERAL_OVERRIDE = 0;
    /**
     * [parity J1] MainRegistry.polaroidID: rerolled once per game session (each JVM: client and dedicated server roll
     * separately) as rand.nextInt(18) + 1, excluding 4 and 9. 11 is the "broken polaroid" easter egg.
     */
    public static final int polaroidID = rollPolaroid();

    public AtomicLegacy(IEventBus modEventBus) {
        modEventBus.addListener(HbmNetwork::register);
        modEventBus.addListener(HbmChunkTickets::register);
        modEventBus.addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    HbmLegacyScatterStructure.validateTemplates();
                    LegacyDispenserBehaviors.register();
                    HbmStats.createStats();
                    com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager.init();
                }));
        if (ModList.get().isLoaded("create")) {
            modEventBus.addListener((FMLCommonSetupEvent event) ->
                    event.enqueueWork(HbmCreateMultiblockCompat::install));
        }
        if (ModList.get().isLoaded("createaddition")) {
            modEventBus.addListener((FMLCommonSetupEvent event) ->
                    event.enqueueWork(HbmCreateAdditionCompat::applyMotorBalanceDefaults));
        }
        if (ModList.get().isLoaded("createbigcannons")) {
            modEventBus.addListener((FMLCommonSetupEvent event) ->
                    event.enqueueWork(HbmCreateBigCannonsCompat::registerMustardGasFluidShellEffects));
        }
        if (ModList.get().isLoaded("immersiveengineering")) {
            modEventBus.addListener((FMLCommonSetupEvent event) ->
                    event.enqueueWork(HbmImmersiveEngineeringCompat::applyDieselGeneratorBalanceDefaults));
        }
        if (ModList.get().isLoaded("curios")) {
            PortableCrateCuriosIntegration.register();
        }
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, HbmConfig.SPEC);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, HbmClientConfig.SPEC);

        // [parity b2: enable528] values derived from the config are rebuilt once it is actually loaded
        modEventBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading event) -> {
            if (event.getConfig().getSpec() == HbmConfig.SPEC) {
                com.atomiclegacy.radiation.HbmHazardRegistry.onConfigLoaded();
            }
        });
        modEventBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading event) -> {
            if (event.getConfig().getSpec() == HbmConfig.SPEC) {
                com.atomiclegacy.radiation.HbmHazardRegistry.onConfigLoaded();
            }
        });

        // [parity b2: RBMKDials] the dial game rules are registered once, on the main thread
        modEventBus.addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(com.atomiclegacy.tileentity.RbmkDials::bootstrap));

        HbmFluids.bootstrap();
        LegacyHbmContent.bootstrap();

        HbmArmorMaterials.register(modEventBus);
        HbmFluids.register(modEventBus);
        HbmItems.register(modEventBus);
        HbmBlocks.register(modEventBus);
        HbmBlockEntities.register(modEventBus);
        HbmMenus.register(modEventBus);
        HbmParticleTypes.register(modEventBus);
        HbmEntityTypes.register(modEventBus);
        HbmMobEffects.register(modEventBus);
        HbmRecipeTypes.register(modEventBus);
        LegacyItemComponents.register(modEventBus);
        HbmConditionSerializers.register(modEventBus);
        HbmSoundEvents.register(modEventBus);
        HbmStats.register(modEventBus);
        HbmWorldgenFeatures.register(modEventBus);
        HbmWorldgenStructures.register(modEventBus);
        HbmBiomeModifierSerializers.register(modEventBus);
        HbmCreativeTabs.register(modEventBus);
        HbmDataAttachments.register(modEventBus);

        PortStatus.logBootstrapSummary(LOGGER);
    }

    private static int rollPolaroid() {
        if (GENERAL_OVERRIDE > 0 && GENERAL_OVERRIDE < 19) {
            return GENERAL_OVERRIDE;
        }
        java.util.Random rand = new java.util.Random();
        int id = rand.nextInt(18) + 1;
        while (id == 4 || id == 9) {
            id = rand.nextInt(18) + 1;
        }
        return id;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
