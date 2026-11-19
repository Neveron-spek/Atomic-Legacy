package com.atomiclegacy.render;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.AtomicLegacy;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * [parity U4: item renderers / armor models] Registration of the 1.7.10 IItemRenderers that the port drew as flat
 * icons (ClientProxy#registerItemRenderer, ItemRenderLibrary, IItemRendererProvider) and of the armor models of
 * {@link LegacyAccessoryArmorLayer}.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class U4LegacyItemRenderSetup {

    private U4LegacyItemRenderSetup() {}

    private static Map<String, LegacyIItemRenderer> renderers;

    /** Item id -> renderer, in ClientProxy order. Created on first use (client thread, after Minecraft is constructed). */
    static synchronized Map<String, LegacyIItemRenderer> renderers() {
        if (renderers != null) return renderers;
        Map<String, LegacyIItemRenderer> map = new LinkedHashMap<>();

        // ItemRenderLibrary / IItemRendererProvider
        map.put("machine_industrial_generator", new LegacyModelItemRenderers.IndustrialGenerator());
        map.put("boxcar", new LegacyModelItemRenderers.Boxcar());
        map.put("sawblade", new LegacyModelItemRenderers.Sawblade());
        map.put("ammo_himars", new LegacyModelItemRenderers.Himars());
        map.put("red_pylon", new LegacyModelItemRenderers.RedPylon());
        map.put("tesla", new Y1LegacyItemRenderers.Tesla()); // [parity l1]
        map.put("vending_machine", new Y1LegacyItemRenderers.VendingMachine()); // [parity l1]
        map.put("rbmk_console", new Y1LegacyItemRenderers.RbmkConsole()); // [parity l1]
        map.put("rbmk_crane_console", new Y1LegacyItemRenderers.RbmkCraneConsole()); // [parity l1]
        // [parity Y1: ItemRenderBase items previously drawn as baked OBJ with invented display transforms]
        map.put("soyuz_capsule", new Y1LegacyItemRenderers.SoyuzCapsule());
        map.put("machine_press", new Y1LegacyItemRenderers.Press());
        map.put("machine_epress", new Y1LegacyItemRenderers.EPress());
        map.put("machine_large_turbine", new Y1LegacyItemRenderers.LargeTurbine());
        LegacyIItemRenderer centrifuge = new Y1LegacyItemRenderers.Centrifuge();
        map.put("machine_centrifuge", centrifuge);
        map.put("machine_gascent", centrifuge);
        map.put("machine_wood_burner", new Y1LegacyItemRenderers.WoodBurner());
        map.put("machine_chemical_plant", new Y1LegacyItemRenderers.ChemicalPlant());
        map.put("machine_diesel", new Y1LegacyItemRenderers.Diesel());
        map.put("machine_intake", new Y1LegacyItemRenderers.Intake());
        map.put("fluid_pump", new Y1LegacyItemRenderers.FluidPump());

        // universal JSON translated items
        double[] rtp = new double[] {0, 180, -90};
        double[] ttp_high = new double[] {0.125, 0.625, 0};
        double[] ttp_low = new double[] {0, 0.75, 0};
        double[] stp = new double[] {1.7, 1.7, 0.85};
        double[] rfp = new double[] {0, 180, -90};
        double[] tfp = new double[] {1.13, 5.2, -0.26};
        double[] sfp = new double[] {1.36, 1.36, 0.68};
        double[] rir = new double[] {0, 0, 0};
        double[] tir = new double[] {0, 0, 0};
        double[] sir = new double[] {1, 1, 1};
        for (String sword : new String[] {"steel_sword", "titanium_sword", "alloy_sword"}) {
            map.put(sword, new LegacyIconItemRenderers.Transformer(rtp, ttp_high, stp, rfp, tfp, sfp, rir, tir, sir));
        }
        map.put("desh_sword", new LegacyIconItemRenderers.Transformer(rtp, ttp_low, stp, rfp, tfp, sfp, rir, tir, sir));
        for (String sword : new String[] {"cobalt_sword", "cobalt_decorated_sword", "starmetal_sword", "schrabidium_sword", "cmb_sword", "dnt_sword"}) {
            map.put(sword, new LegacyIconItemRenderers.Transformer(rtp, ttp_high, stp, rfp, tfp, sfp, rir, tir, sir));
        }
        double[] sfp_default = new double[] {1, 1, 1};
        double[] tfp_default = new double[] {0, 0, 0};
        double[] sir_default = new double[] {1, 1, 1};
        for (String tool : new String[] {"bismuth_pickaxe", "bismuth_axe", "volcanic_pickaxe", "volcanic_axe",
                "chlorophyte_pickaxe", "chlorophyte_axe", "mese_pickaxe", "mese_axe"}) {
            map.put(tool, new LegacyIconItemRenderers.Transformer(rtp, ttp_high, stp, rfp, tfp_default, sfp_default, rir, tir, sir_default));
        }

        // ItemRenderMissileGeneric
        LegacyModelItemRenderers.MissileType t0 = LegacyModelItemRenderers.MissileType.TYPE_TIER0;
        for (String m : new String[] {"missile_test", "missile_taint", "missile_micro", "missile_bhole", "missile_schrabidium", "missile_emp"}) {
            map.put(m, new LegacyModelItemRenderers.MissileGeneric(t0));
        }
        map.put("missile_stealth", new LegacyModelItemRenderers.MissileGeneric(LegacyModelItemRenderers.MissileType.TYPE_STEALTH));
        for (String m : new String[] {"missile_generic", "missile_incendiary", "missile_cluster", "missile_buster", "missile_decoy"}) {
            map.put(m, new LegacyModelItemRenderers.MissileGeneric(LegacyModelItemRenderers.MissileType.TYPE_TIER1));
        }
        map.put("missile_anti_ballistic", new LegacyModelItemRenderers.MissileGeneric(LegacyModelItemRenderers.MissileType.TYPE_ABM));
        for (String m : new String[] {"missile_strong", "missile_incendiary_strong", "missile_cluster_strong", "missile_buster_strong", "missile_emp_strong"}) {
            map.put(m, new LegacyModelItemRenderers.MissileGeneric(LegacyModelItemRenderers.MissileType.TYPE_TIER2));
        }
        for (String m : new String[] {"missile_burst", "missile_inferno", "missile_rain", "missile_drill"}) {
            map.put(m, new LegacyModelItemRenderers.MissileGeneric(LegacyModelItemRenderers.MissileType.TYPE_TIER3));
        }
        for (String m : new String[] {"missile_nuclear", "missile_nuclear_cluster", "missile_volcano", "missile_doomsday", "missile_doomsday_rusted"}) {
            map.put(m, new LegacyModelItemRenderers.MissileGeneric(LegacyModelItemRenderers.MissileType.TYPE_NUCLEAR));
        }
        map.put("missile_shuttle", new LegacyModelItemRenderers.MissileGeneric(LegacyModelItemRenderers.MissileType.TYPE_ROBIN));

        for (String hot : new String[] {"ingot_steel_dusted", "ingot_chainsteel", "ingot_meteorite", "ingot_meteorite_forged", "blade_meteorite"}) {
            map.put(hot, new LegacyIconItemRenderers.Hot());
        }

        map.put("meteorite_sword_seared", new LegacyIconItemRenderers.MeteorSword(1.0F, 0.5F, 0.0F));
        map.put("meteorite_sword_reforged", new LegacyIconItemRenderers.MeteorSword(0.5F, 1.0F, 1.0F));
        map.put("meteorite_sword_hardened", new LegacyIconItemRenderers.MeteorSword(0.25F, 0.25F, 0.25F));
        map.put("meteorite_sword_alloyed", new LegacyIconItemRenderers.MeteorSword(0.0F, 0.5F, 1.0F));
        map.put("meteorite_sword_machined", new LegacyIconItemRenderers.MeteorSword(1.0F, 1.0F, 0.0F));
        map.put("meteorite_sword_treated", new LegacyIconItemRenderers.MeteorSword(0.5F, 1.0F, 0.5F));
        map.put("meteorite_sword_etched", new LegacyIconItemRenderers.MeteorSword(1.0F, 1.0F, 0.5F));
        map.put("meteorite_sword_bred", new LegacyIconItemRenderers.MeteorSword(0.5F, 0.5F, 0.0F));
        map.put("meteorite_sword_irradiated", new LegacyIconItemRenderers.MeteorSword(0.75F, 1.0F, 0.0F));
        map.put("meteorite_sword_fused", new LegacyIconItemRenderers.MeteorSword(1.0F, 0.0F, 0.5F));
        map.put("meteorite_sword_baleful", new LegacyIconItemRenderers.MeteorSword(0.0F, 1.0F, 0.0F));

        map.put("redstone_sword", new LegacyModelItemRenderers.RedstoneSword());
        map.put("big_sword", new LegacyModelItemRenderers.BigSword());
        LegacyIItemRenderer shim = new LegacyModelItemRenderers.Shim();
        for (String s : new String[] {"shimmer_sledge", "shimmer_axe", "stopsign", "sopsign", "chernobylsign"}) map.put(s, shim);
        LegacyIItemRenderer gavel = new LegacyModelItemRenderers.Gavel();
        for (String s : new String[] {"wood_gavel", "lead_gavel", "diamond_gavel", "mese_gavel"}) map.put(s, gavel);
        map.put("chainsaw", new LegacyModelItemRenderers.Chainsaw());
        map.put("detonator_laser", new LegacyModelItemRenderers.DetonatorLaser());
        map.put("steel_roof", new LegacyModelItemRenderers.DecoBlock());
        map.put("missile_custom", new LegacyModelItemRenderers.Missile());
        map.put("gun_b92", new ItemRenderGunAnim()); // [parity c1] ClientProxy: registerItemRenderer(ModItems.gun_b92, new ItemRenderGunAnim())

        renderers = map;
        return map;
    }

    private static Item item(String id) {
        ResourceLocation key = AtomicLegacy.id(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            throw new IllegalStateException("Legacy item renderer target is not registered: " + key);
        }
        return BuiltInRegistries.ITEM.get(key);
    }

    private static final class Extensions implements IClientItemExtensions {
        private final LegacyIItemRenderer renderer;

        Extensions(LegacyIItemRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return this.renderer;
        }

        /** ItemRenderer#renderItemInFirstPerson when the original renderer draws EQUIPPED_FIRST_PERSON itself. */
        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                               float partialTick, float equipProcess, float swingProcess) {
            if (!this.renderer.handlesStack(itemInHand)
                    || !this.renderer.handleRenderType(itemInHand, LegacyIItemRenderer.Type.EQUIPPED_FIRST_PERSON)) {
                return false;
            }
            this.renderer.applyFirstPersonHand(poseStack, arm, itemInHand, equipProcess, swingProcess);
            return true;
        }
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (Map.Entry<String, LegacyIItemRenderer> entry : renderers().entrySet()) {
            event.registerItem(new Extensions(entry.getValue()), item(entry.getKey()));
        }
        // getArmorModel replaced the vanilla biped of these helmets; LegacyAccessoryArmorLayer draws them
        IClientItemExtensions hidden = new IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                                    EquipmentSlot equipmentSlot,
                                                                                    net.minecraft.client.model.HumanoidModel<?> original) {
                original.setAllVisible(false);
                return original;
            }
        };
        Item[] helmets = LegacyAccessoryArmorLayer.HELMET_IDS.stream().map(U4LegacyItemRenderSetup::item).toArray(Item[]::new);
        event.registerItem(hidden, helmets);
    }

    @SubscribeEvent
    public static void wrapItemModels(ModelEvent.ModifyBakingResult event) {
        for (Map.Entry<String, LegacyIItemRenderer> entry : renderers().entrySet()) {
            LegacyItemModelWrapper.wrap(event.getModels(), AtomicLegacy.id(entry.getKey()), entry.getValue());
        }
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LegacyModelItemRenderers.BIG_SWORD_LAYER, LegacyModelItemRenderers::createBigSword);
        event.registerLayerDefinition(LegacyModelItemRenderers.SWORD_LAYER, LegacyModelItemRenderers::createSword);
        event.registerLayerDefinition(LegacyModelItemRenderers.STEEL_ROOF_LAYER, LegacyModelItemRenderers::createSteelRoof);
        event.registerLayerDefinition(ItemRenderGunAnim.B92_LAYER, ItemRenderGunAnim::createB92); // [parity c1]
        event.registerLayerDefinition(LegacyAccessoryArmorLayer.GAS_MASK_LAYER, LegacyAccessoryArmorLayer::createGasMask);
        event.registerLayerDefinition(LegacyAccessoryArmorLayer.M65_LAYER, LegacyAccessoryArmorLayer::createM65);
        event.registerLayerDefinition(LegacyAccessoryArmorLayer.JETPACK_LAYER, LegacyAccessoryArmorLayer::createJetpack);
        event.registerLayerDefinition(LegacyAccessoryArmorLayer.GOGGLES_LAYER, LegacyAccessoryArmorLayer::createGoggles);
    }

    /** RenderItem#doRender knew its EntityItem; the capture renderer exposes it to the ENTITY helpers. */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ITEM, LegacyIItemRenderer.CaptureRenderer::new);
    }

    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer renderer) {
                LegacyAccessoryArmorLayer.attach(renderer);
            }
        }
        // the humanoid renderers that also receive the T-51 / FSB armor layers (HbmClientRenderers#addLegacyMobArmorLayers)
        for (EntityType<?> type : new EntityType<?>[] {EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK,
                EntityType.DROWNED, EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.PIGLIN,
                EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN, EntityType.ARMOR_STAND}) {
            if (event.getRenderer(type) instanceof LivingEntityRenderer renderer) {
                LegacyAccessoryArmorLayer.attach(renderer);
            }
        }
    }
}
