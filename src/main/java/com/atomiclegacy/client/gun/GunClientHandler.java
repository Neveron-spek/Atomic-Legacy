package com.atomiclegacy.client.gun;

import java.util.EnumMap;
import java.util.function.BiFunction;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.HbmKeyMappings;
import com.atomiclegacy.client.gun.HbmAnimations.Animation;
import com.atomiclegacy.config.HbmClientConfig;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.hud.HUDComponentAmmoCounter;
import com.atomiclegacy.items.weapon.sedna.hud.HUDComponentDurabilityBar;
import com.atomiclegacy.items.weapon.sedna.hud.IHUDComponent;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.network.GunAnimationPayload;
import com.atomiclegacy.network.GunKeyPayload;
import com.atomiclegacy.network.GunMuzzleFlashPayload;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.ItemRenderGreasegun;
import com.atomiclegacy.render.ItemRenderWeaponBase;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Client side of the 1.7.10 sedna gun system: HbmKeybinds gun handling,
 * HbmAnimationPacket/MuzzleFlashPacket handlers, ModEventHandlerClient recoil and
 * FOV, ModEventHandlerRenderer hand hook and aimedBow pose, IItemHUD overlays and
 * GunFactoryClient HUD assignment.
 */
public final class GunClientHandler {

	private GunClientHandler() {
	}

	public static final HUDComponentDurabilityBar HUD_COMPONENT_DURABILITY = new HUDComponentDurabilityBar();
	public static final HUDComponentAmmoCounter HUD_COMPONENT_AMMO = new HUDComponentAmmoCounter(0);

	private static final EnumMap<GunKeybind, Boolean> KEY_STATE = new EnumMap<>(GunKeybind.class);

	private static KeyMapping mapping(GunKeybind key) {
		return switch(key) {
			case GUN_PRIMARY -> HbmKeyMappings.GUN_PRIMARY;
			case GUN_SECONDARY -> HbmKeyMappings.GUN_SECONDARY;
			case GUN_TERTIARY -> HbmKeyMappings.GUN_TERTIARY;
			case RELOAD -> HbmKeyMappings.RELOAD;
		};
	}

	private static ItemRenderWeaponBase rendererFor(ItemStack stack) {
		if(stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return null;
		BlockEntityWithoutLevelRenderer renderer = IClientItemExtensions.of(stack).getCustomRenderer();
		return renderer instanceof ItemRenderWeaponBase weapon ? weapon : null;
	}

	/* ===================== MOD BUS ===================== */

	@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class ModEvents {

		private ModEvents() {
		}

		@SubscribeEvent
		public static void clientSetup(FMLClientSetupEvent event) {
			ItemGunBaseNT.LOCAL_PLAYER = entity -> entity != null && entity == Minecraft.getInstance().player;
			ItemGunBaseNT.WEAPON_TABLE_OPEN = unused -> WeaponTableScreenAccess.isOpen();
			// GunFactoryClient
			((ItemGunBaseNT) HbmItems.GUN_GREASEGUN.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_UZI.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_G3.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LAG.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_STG77.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// LegoClient.HUD_COMPONENT_DURABILITY_MIRROR / HUD_COMPONENT_AMMO_MIRROR for the left gun
			((ItemGunBaseNT) HbmItems.GUN_UZI_AKIMBO.get()).getConfig(null, 0).hud(new HUDComponentDurabilityBar(true), new HUDComponentAmmoCounter(0).mirror());
			((ItemGunBaseNT) HbmItems.GUN_UZI_AKIMBO.get()).getConfig(null, 1).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch A: HUD]
			((ItemGunBaseNT) HbmItems.GUN_AM180.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_STAR_F.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_AMAT.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_M2.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_STAR_F_AKIMBO.get()).getConfig(null, 0).hud(new HUDComponentDurabilityBar(true), new HUDComponentAmmoCounter(0).mirror());
			((ItemGunBaseNT) HbmItems.GUN_STAR_F_AKIMBO.get()).getConfig(null, 1).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch B: HUD]
			// [weapons batch C1: HUD]
			((ItemGunBaseNT) HbmItems.GUN_PEPPERBOX.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LIGHT_REVOLVER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LIGHT_REVOLVER_ATLAS.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LIGHT_REVOLVER_DANI.get()).getConfig(null, 0).hud(new HUDComponentDurabilityBar(true), new HUDComponentAmmoCounter(0).mirror());
			((ItemGunBaseNT) HbmItems.GUN_LIGHT_REVOLVER_DANI.get()).getConfig(null, 1).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_HENRY.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_HENRY_LINCOLN.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_HEAVY_REVOLVER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_HEAVY_REVOLVER_PROTEGE.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_HANGMAN.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch C2: HUD]
			// [weapons batch C3: HUD]
			((ItemGunBaseNT) HbmItems.GUN_CARBINE.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_MINIGUN.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_MINIGUN_LACUNAE.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_MAS36.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch E2: HUD]
			com.atomiclegacy.items.weapon.sedna.factory.XFactoryEnergy.LOCAL_INVENTORY = () -> Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getInventory() : null;
			((ItemGunBaseNT) HbmItems.GUN_TESLA_CANNON.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LASER_PISTOL.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LASER_PISTOL_PEW_PEW.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LASER_PISTOL_MORNING_GLORY.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LASRIFLE.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch F1: HUD] LegoClient.HUD_COMPONENT_AMMO_NOCOUNTER for the flamethrowers
			((ItemGunBaseNT) HbmItems.GUN_FLAMER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, new HUDComponentAmmoCounter(0).noCounter());
			((ItemGunBaseNT) HbmItems.GUN_FLAMER_TOPAZ.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, new HUDComponentAmmoCounter(0).noCounter());
			((ItemGunBaseNT) HbmItems.GUN_FLAMER_DAYBREAKER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, new HUDComponentAmmoCounter(0).noCounter());
			((ItemGunBaseNT) HbmItems.GUN_CHEMTHROWER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch F3: HUD] GunFactoryClient 320-321 (gun_drill has no HUD components in 1.7.10)
			((ItemGunBaseNT) HbmItems.GUN_FIREEXT.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_CHARGE_THROWER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [parity c1] gun_pa_melee / gun_pa_ranged have no HUD components in 1.7.10; IPAWeaponsProvider.getMeleeComponentClient uses proxy.me()
			com.atomiclegacy.items.weapon.sedna.factory.XFactoryPA.LOCAL_PLAYER = () -> Minecraft.getInstance().player;
			((ItemGunBaseNT) HbmItems.GUN_AUTOSHOTGUN_HERETIC.get()).getConfig(null, 0).hud(HUD_COMPONENT_AMMO); // [parity c1] GunFactoryClient 297
			// [parity d2: HUD] GunFactoryClient 260, 331-332 (HUD_COMPONENT_AMMO_SECOND = new HUDComponentAmmoCounter(1))
			((ItemGunBaseNT) HbmItems.GUN_DEBUG.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO, new HUDComponentAmmoCounter(1));
			((ItemGunBaseNT) HbmItems.GUN_MINIGUN_DUAL.get()).getConfig(null, 0).hud(new HUDComponentDurabilityBar(true), new HUDComponentAmmoCounter(0).mirror());
			((ItemGunBaseNT) HbmItems.GUN_MINIGUN_DUAL.get()).getConfig(null, 1).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			GunBatchF3ClientEffects.clientSetup(key -> KEY_STATE.getOrDefault(key, false));
			// [weapons batch F2: HUD]
			com.atomiclegacy.items.weapon.sedna.factory.XFactoryCatapult.LOCAL_RANDOM = () -> Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getRandom() : net.minecraft.util.RandomSource.create();
			com.atomiclegacy.items.weapon.sedna.factory.OrchestrasF2.TAU_LOOP = GunBatchF2ClientEffects::tauLoop;
			((ItemGunBaseNT) HbmItems.GUN_TAU.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_FATMAN.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_COILGUN.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch C4: HUD]
			((ItemGunBaseNT) HbmItems.GUN_MARESLEG.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_MARESLEG_BROKEN.get()).getConfig(null, 0).hud(HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_LIBERATOR.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_DOUBLE_BARREL.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_MARESLEG_AKIMBO.get()).getConfig(null, 0).hud(new HUDComponentDurabilityBar(true), new HUDComponentAmmoCounter(0).mirror());
			((ItemGunBaseNT) HbmItems.GUN_MARESLEG_AKIMBO.get()).getConfig(null, 1).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch D2: HUD]
			((ItemGunBaseNT) HbmItems.GUN_G3_ZEBRA.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_AMAT_SUBTLETY.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_AMAT_PENANCE.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch E1: HUD]
			((ItemGunBaseNT) HbmItems.GUN_PANZERSCHRECK.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_QUADRO.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_FLIEGERFAUST.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_MISSILE_LAUNCHER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_BOLTER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch D3: HUD]
			((ItemGunBaseNT) HbmItems.GUN_FLAREGUN.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_CONGOLAKE.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_MK108.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch D1: HUD]
			((ItemGunBaseNT) HbmItems.GUN_SPAS12.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_AUTOSHOTGUN.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_AUTOSHOTGUN_SEXY.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			// [weapons batch F4: HUD]
			((ItemGunBaseNT) HbmItems.GUN_STINGER.get()).getConfig(null, 0).hud(HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_FOLLY.get()).getConfig(null, 0).hud(HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_ABERRATOR.get()).getConfig(null, 0).hud(HUD_COMPONENT_AMMO);
			((ItemGunBaseNT) HbmItems.GUN_ABERRATOR_EOTT.get()).getConfig(null, 0).hud(new HUDComponentAmmoCounter(0).mirror());
			((ItemGunBaseNT) HbmItems.GUN_ABERRATOR_EOTT.get()).getConfig(null, 1).hud(HUD_COMPONENT_AMMO);
		}

		@SubscribeEvent
		public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
			event.registerItem(new GunExtensions(new ItemRenderGreasegun()), HbmItems.GUN_GREASEGUN.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderLAG()), HbmItems.GUN_LAG.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderUzi()), HbmItems.GUN_UZI.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderUziAkimbo()), HbmItems.GUN_UZI_AKIMBO.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderG3(com.atomiclegacy.render.ItemRenderG3.g3_tex)), HbmItems.GUN_G3.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderSTG77()), HbmItems.GUN_STG77.get());
			// [weapons batch A: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderAm180()), HbmItems.GUN_AM180.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderStarF()), HbmItems.GUN_STAR_F.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderStarFAkimbo()), HbmItems.GUN_STAR_F_AKIMBO.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderAmat(com.atomiclegacy.render.ItemRenderAmat.amat_tex)), HbmItems.GUN_AMAT.get());
			// [weapons batch D2: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderG3(com.atomiclegacy.render.ItemRenderG3.g3_zebra_tex)), HbmItems.GUN_G3_ZEBRA.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderAmat(com.atomiclegacy.render.ItemRenderAmat.amat_subtlety_tex)), HbmItems.GUN_AMAT_SUBTLETY.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderAmat(com.atomiclegacy.render.ItemRenderAmat.amat_penance_tex)), HbmItems.GUN_AMAT_PENANCE.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderM2()), HbmItems.GUN_M2.get());
			// [weapons batch B: renderers]
			// [weapons batch C1: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderPepperbox()), HbmItems.GUN_PEPPERBOX.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderAtlas(com.atomiclegacy.render.ItemRenderAtlas.bio_revolver_tex)), HbmItems.GUN_LIGHT_REVOLVER.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderAtlas(com.atomiclegacy.render.ItemRenderAtlas.bio_revolver_atlas_tex)), HbmItems.GUN_LIGHT_REVOLVER_ATLAS.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderDANI()), HbmItems.GUN_LIGHT_REVOLVER_DANI.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderHenry(com.atomiclegacy.render.ItemRenderHenry.henry_tex)), HbmItems.GUN_HENRY.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderHenry(com.atomiclegacy.render.ItemRenderHenry.henry_lincoln_tex)), HbmItems.GUN_HENRY_LINCOLN.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderHeavyRevolver(com.atomiclegacy.render.ItemRenderHeavyRevolver.heavy_revolver_tex)), HbmItems.GUN_HEAVY_REVOLVER.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderHeavyRevolver(com.atomiclegacy.render.ItemRenderHeavyRevolver.lilmac_tex)), HbmItems.GUN_HEAVY_REVOLVER_LILMAC.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderHeavyRevolver(com.atomiclegacy.render.ItemRenderHeavyRevolver.heavy_revolver_protege_tex)), HbmItems.GUN_HEAVY_REVOLVER_PROTEGE.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderHangman()), HbmItems.GUN_HANGMAN.get());
			// [weapons batch C2: renderers]
			// [weapons batch C3: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderCarbine()), HbmItems.GUN_CARBINE.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMinigun(com.atomiclegacy.render.ItemRenderMinigun.minigun_tex)), HbmItems.GUN_MINIGUN.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMinigun(com.atomiclegacy.render.ItemRenderMinigun.minigun_lacunae_tex)), HbmItems.GUN_MINIGUN_LACUNAE.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMAS36()), HbmItems.GUN_MAS36.get());
			// [weapons batch E2: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderTeslaCannon()), HbmItems.GUN_TESLA_CANNON.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderLaserPistol(com.atomiclegacy.render.ItemRenderLaserPistol.laser_pistol_tex)), HbmItems.GUN_LASER_PISTOL.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderLaserPistol(com.atomiclegacy.render.ItemRenderLaserPistol.laser_pistol_pew_pew_tex)), HbmItems.GUN_LASER_PISTOL_PEW_PEW.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderLaserPistol(com.atomiclegacy.render.ItemRenderLaserPistol.laser_pistol_morning_glory_tex)), HbmItems.GUN_LASER_PISTOL_MORNING_GLORY.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderLasrifle()), HbmItems.GUN_LASRIFLE.get());
			// [weapons batch F1: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderFlamer(com.atomiclegacy.render.ItemRenderFlamer.flamethrower_tex)), HbmItems.GUN_FLAMER.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderFlamer(com.atomiclegacy.render.ItemRenderFlamer.flamethrower_topaz_tex)), HbmItems.GUN_FLAMER_TOPAZ.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderFlamer(com.atomiclegacy.render.ItemRenderFlamer.flamethrower_daybreaker_tex)), HbmItems.GUN_FLAMER_DAYBREAKER.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderChemthrower()), HbmItems.GUN_CHEMTHROWER.get());
			// [weapons batch F3: renderers] ClientProxy 610 (ItemRenderFireExt), GunFactoryClient 110-111
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderFireExt()), HbmItems.GUN_FIREEXT.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderChargeThrower()), HbmItems.GUN_CHARGE_THROWER.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderDrill()), HbmItems.GUN_DRILL.get());
			// [parity c1: renderers] GunFactoryClient 113 (gun_pa_ranged has no renderer: plain full 3D icon)
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderPAMelee()), HbmItems.GUN_PA_MELEE.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderSexy(com.atomiclegacy.render.ItemRenderSexy.heretic_tex)), HbmItems.GUN_AUTOSHOTGUN_HERETIC.get()); // GunFactoryClient 86
			// [parity d2: renderers] GunFactoryClient 45, 91
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderDebug()), HbmItems.GUN_DEBUG.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMinigunDual()), HbmItems.GUN_MINIGUN_DUAL.get());
			// [weapons batch F2: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderTau()), HbmItems.GUN_TAU.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderFatMan()), HbmItems.GUN_FATMAN.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderCoilgun()), HbmItems.GUN_COILGUN.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderNI4NI()), HbmItems.GUN_N_I_4_N_I.get());
			// [weapons batch C4: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMaresleg(com.atomiclegacy.render.ItemRenderMaresleg.maresleg_tex)), HbmItems.GUN_MARESLEG.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMareslegAkimbo()), HbmItems.GUN_MARESLEG_AKIMBO.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMaresleg(com.atomiclegacy.render.ItemRenderMaresleg.maresleg_broken_tex)), HbmItems.GUN_MARESLEG_BROKEN.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderLiberator()), HbmItems.GUN_LIBERATOR.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderDoubleBarrel(com.atomiclegacy.render.ItemRenderDoubleBarrel.double_barrel_tex)), HbmItems.GUN_DOUBLE_BARREL.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderDoubleBarrel(com.atomiclegacy.render.ItemRenderDoubleBarrel.double_barrel_sacred_dragon_tex)), HbmItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get());
			// [weapons batch E1: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderPanzerschreck()), HbmItems.GUN_PANZERSCHRECK.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderQuadro()), HbmItems.GUN_QUADRO.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderFliegerfaust()), HbmItems.GUN_FLIEGERFAUST.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMissileLauncher()), HbmItems.GUN_MISSILE_LAUNCHER.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderBolter()), HbmItems.GUN_BOLTER.get());
			// [weapons batch D3: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderFlaregun()), HbmItems.GUN_FLAREGUN.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderCongoLake()), HbmItems.GUN_CONGOLAKE.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderMK108()), HbmItems.GUN_MK108.get());
			// [weapons batch D1: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderSPAS12()), HbmItems.GUN_SPAS12.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderShredder(com.atomiclegacy.render.ItemRenderShredder.shredder_tex)), HbmItems.GUN_AUTOSHOTGUN.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderShredder(com.atomiclegacy.render.ItemRenderShredder.shredder_orig_tex)), HbmItems.GUN_AUTOSHOTGUN_SHREDDER.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderSexy(com.atomiclegacy.render.ItemRenderSexy.sexy_tex)), HbmItems.GUN_AUTOSHOTGUN_SEXY.get());
			// [weapons batch F4: renderers]
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderStinger()), HbmItems.GUN_STINGER.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderFolly()), HbmItems.GUN_FOLLY.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderAberrator()), HbmItems.GUN_ABERRATOR.get());
			event.registerItem(new GunExtensions(new com.atomiclegacy.render.ItemRenderEOTT()), HbmItems.GUN_ABERRATOR_EOTT.get());
		}
	}

	/** BEWLR plus the aimedBow third person pose of ModEventHandlerRenderer. */
	private static final class GunExtensions implements IClientItemExtensions {
		private final ItemRenderWeaponBase renderer;

		private GunExtensions(ItemRenderWeaponBase renderer) {
			this.renderer = renderer;
		}

		@Override
		public BlockEntityWithoutLevelRenderer getCustomRenderer() {
			return this.renderer;
		}

		@Override
		public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
			return hand == InteractionHand.MAIN_HAND ? HumanoidModel.ArmPose.BOW_AND_ARROW : null;
		}
	}

	/* ===================== PACKETS ===================== */

	/** HbmAnimationPacket#handleSedna */
	public static void handleAnimation(GunAnimationPayload m) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player == null) return;
		ItemStack stack = player.getMainHandItem();
		int slot = player.getInventory().selected;
		if(stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT gun)) return;
		if(m.animType() < 0 || m.animType() >= GunAnimation.values().length) return;
		GunAnimation type = GunAnimation.values()[m.animType()];
		int receiverIndex = m.receiverIndex();
		int gunIndex = m.itemIndex();
		if(gunIndex < 0 || gunIndex >= gun.getConfigCount()) return;

		GunConfig config = gun.getConfig(stack, gunIndex);

		if(type == GunAnimation.CYCLE) {
			if(gunIndex < gun.lastShot.length) gun.lastShot[gunIndex] = System.currentTimeMillis();
			gun.shotRand = player.level().random.nextDouble();

			Receiver[] receivers = config.getReceivers(stack);
			if(receiverIndex >= 0 && receiverIndex < receivers.length) {
				Receiver rec = receivers[receiverIndex];
				java.util.function.BiConsumer<ItemStack, LambdaContext> onRecoil = rec.getRecoil(stack);
				if(onRecoil != null) onRecoil.accept(stack, new LambdaContext(config, player, player.getInventory(), receiverIndex));
			}
		}

		BiFunction<ItemStack, GunAnimation, BusAnimation> anims = config.getAnims(stack);
		// 1.7.10 HbmAnimationPacket wraps this in catch(Exception) {}: guns without .anim() (gun_fireext) play nothing
		if(anims == null) return;
		BusAnimation animation = anims.apply(stack, type);

		if(animation == null && (type == GunAnimation.ALT_CYCLE || type == GunAnimation.CYCLE_EMPTY)) {
			animation = anims.apply(stack, GunAnimation.CYCLE);
		}

		if(animation != null) {
			boolean isReloadAnimation = type == GunAnimation.RELOAD || type == GunAnimation.RELOAD_CYCLE;
			if(isReloadAnimation && IMagazine.isTrenchMaster(player)) animation.setTimeMult(0.5D);
			HbmAnimations.hotbar[slot][gunIndex] = new Animation(stack.getItem().getDescriptionId(), System.currentTimeMillis(), animation, isReloadAnimation && config.getReloadAnimSequential(stack));
		}
	}

	/** HbmAnimationPacket#handleItem */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void handleItemAnimation(com.atomiclegacy.network.ItemAnimationPayload m) {
		LocalPlayer player = Minecraft.getInstance().player;
		if(player == null) return;
		ItemStack stack = player.getMainHandItem();
		int slot = player.getInventory().selected;
		if(stack.isEmpty() || !(stack.getItem() instanceof com.atomiclegacy.items.IAnimatedItem item)) return;
		if(m.itemIndex() < 0 || m.itemIndex() >= HbmAnimations.hotbar[0].length || slot < 0 || slot > 8) return;
		Enum[] values = (Enum[]) item.getEnum().getEnumConstants();
		Enum type = values[Math.abs(m.animType() % values.length)];
		BusAnimation animation = item.getAnimation(type, stack);
		if(animation != null) {
			HbmAnimations.hotbar[slot][m.itemIndex()] = new Animation(stack.getItem().getDescriptionId(), System.currentTimeMillis(), animation);
		}
	}

	/** MuzzleFlashPacket */
	public static void handleMuzzleFlash(GunMuzzleFlashPayload m) {
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null) return;
		Entity entity = mc.level.getEntity(m.entityId());
		if(!(entity instanceof LivingEntity living) || entity == mc.player) return; //packets are sent to the player who fired
		ItemStack stack = living.getMainHandItem();
		if(stack.getItem() instanceof ItemGunBaseNT) {
			ItemRenderWeaponBase.flashMap.put(living, System.currentTimeMillis());
		}
	}

	/* ===================== GAME BUS ===================== */

	@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
	public static final class GameEvents {

		private GameEvents() {
		}

		/** HbmKeybinds#mouseEvent -> handleProps (only processed without an open screen, like FML's MouseInputEvent). */
		@SubscribeEvent
		public static void onMouse(InputEvent.MouseButton.Post event) {
			handleProps(InputConstants.Type.MOUSE, event.getButton(), event.getAction());
		}

		/** HbmKeybinds#keyEvent -> handleProps */
		@SubscribeEvent
		public static void onKey(InputEvent.Key event) {
			handleProps(InputConstants.Type.KEYSYM, event.getKey(), event.getAction());
		}

		/** HbmKeybinds#handleOverlap: shoot in favor of attacking, scope in favor of picking. */
		@SubscribeEvent
		public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
			Minecraft mc = Minecraft.getInstance();
			Player player = mc.player;
			if(player == null || !(player.getMainHandItem().getItem() instanceof ItemGunBaseNT)) return;
			KeyMapping trigger = event.getKeyMapping();
			if(event.isAttack() && sharesKey(trigger)) {
				event.setSwingHand(false);
				event.setCanceled(true);
			}
			if(event.isPickBlock() && sharesKey(trigger)) {
				event.setSwingHand(false);
				event.setCanceled(true);
			}
		}

		/** HbmPlayerProps key states belong to the player instance; a new session starts released. */
		@SubscribeEvent
		public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
			KEY_STATE.clear();
			HbmAnimations.clear();
		}

		/** ModEventHandlerClient#onOverlayRender: HANLDE ANIMATION BUSES, finished animations are dropped. */
		@SubscribeEvent
		public static void onRenderFrame(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
			for(int i = 0; i < HbmAnimations.hotbar.length; i++) {
				for(int j = 0; j < HbmAnimations.hotbar[i].length; j++) {
					Animation animation = HbmAnimations.hotbar[i][j];
					if(animation == null || animation.holdLastFrame) continue;
					long time = System.currentTimeMillis() - animation.startMillis;
					if(time > animation.animation.getDuration()) HbmAnimations.hotbar[i][j] = null;
				}
			}
		}

		/** ModEventHandlerClient#clientTick END: visual recoil. */
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Player player = Minecraft.getInstance().player;
			if(player == null) return;

			if(HbmClientConfig.gunVisualRecoil()) {
				ItemGunBaseNT.offsetVertical += ItemGunBaseNT.recoilVertical;
				ItemGunBaseNT.offsetHorizontal += ItemGunBaseNT.recoilHorizontal;
				player.setXRot(player.getXRot() - ItemGunBaseNT.recoilVertical);
				player.setYRot(player.getYRot() - ItemGunBaseNT.recoilHorizontal);

				ItemGunBaseNT.recoilVertical *= ItemGunBaseNT.recoilDecay;
				ItemGunBaseNT.recoilHorizontal *= ItemGunBaseNT.recoilDecay;
				float dV = ItemGunBaseNT.offsetVertical * ItemGunBaseNT.recoilRebound;
				float dH = ItemGunBaseNT.offsetHorizontal * ItemGunBaseNT.recoilRebound;

				ItemGunBaseNT.offsetVertical -= dV;
				ItemGunBaseNT.offsetHorizontal -= dH;
				player.setXRot(player.getXRot() + dV);
				player.setYRot(player.getYRot() + dH);
			} else {
				ItemGunBaseNT.offsetVertical = 0;
				ItemGunBaseNT.offsetHorizontal = 0;
				ItemGunBaseNT.recoilVertical = 0;
				ItemGunBaseNT.recoilHorizontal = 0;
			}
		}

		/** ModEventHandlerClient#setupFOV (FOVUpdateEvent) */
		@SubscribeEvent
		public static void onFov(ComputeFovModifierEvent event) {
			ItemStack held = event.getPlayer().getMainHandItem();
			ItemRenderWeaponBase renderGun = rendererFor(held);
			if(renderGun == null) return;
			event.setNewFovModifier(renderGun.getViewFOV(held, event.getNewFovModifier()));
		}

		/** ModEventHandlerRenderer#onRenderHand */
		@SubscribeEvent
		public static void onRenderHand(RenderHandEvent event) {
			if(event.getHand() != InteractionHand.MAIN_HAND) return;
			ItemStack toRender = event.getItemStack();
			ItemRenderWeaponBase renderer = rendererFor(toRender);
			if(renderer == null) return;
			renderer.setPerspectiveAndRender(toRender, event.getPartialTick(), Minecraft.getInstance().renderBuffers().bufferSource(), event.getPackedLight());
			event.setCanceled(true);
		}

		@SubscribeEvent
		public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
			ItemRenderWeaponBase.renderingEntity = event.getEntity();
		}

		@SubscribeEvent
		public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
			ItemRenderWeaponBase.renderingEntity = null;
		}

		/** ItemGunBaseNT#renderHUD via ModEventHandlerClient#onOverlayRender */
		@SubscribeEvent
		public static void onOverlay(RenderGuiLayerEvent.Pre event) {
			Minecraft mc = Minecraft.getInstance();
			Player player = mc.player;
			if(player == null) return;
			ItemStack stack = player.getMainHandItem();
			if(!(stack.getItem() instanceof ItemGunBaseNT gun)) return;
			GuiGraphics graphics = event.getGuiGraphics();

			if(event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
				event.setCanceled(true);
				if(!mc.options.getCameraType().isFirstPerson()) return;
				GunConfig config = gun.getConfig(stack, 0);
				// [weapons batch F4: stinger HUD] ItemGunStinger#renderHUD: no crosshair until fully aimed, then crosshair + lock-on bar
				if(gun instanceof com.atomiclegacy.items.weapon.sedna.impl.ItemGunStinger) {
					if(ItemGunBaseNT.aimingProgress < 1F) return;
					renderCustomCrosshairs(graphics, config.getCrosshair(stack));
					GunBatchF4ClientEffects.renderStingerLockon(graphics);
					return;
				}
				if(config.getHideCrosshair(stack) && ItemGunBaseNT.aimingProgress >= 1F) return;
				renderCustomCrosshairs(graphics, config.getCrosshair(stack));
				return;
			}

			if(event.getName().equals(VanillaGuiLayers.HOTBAR)) {
				int confNo = gun.getConfigCount();
				for(int i = 0; i < confNo; i++) {
					IHUDComponent[] components = gun.getConfig(stack, i).getHUDComponents(stack);
					int bottomOffset = 0;

					if(components != null) for(IHUDComponent component : components) {
						component.renderHUDComponent(graphics, player, stack, bottomOffset, i);
						bottomOffset += component.getComponentHeight(player, stack);
					}
				}
			}
		}
	}

	private static boolean sharesKey(KeyMapping trigger) {
		for(GunKeybind key : GunKeybind.values()) {
			if(mapping(key).getKey().equals(trigger.getKey())) return true;
		}
		return false;
	}

	/** HbmKeybinds#handleProps for the four gun keybinds. */
	private static void handleProps(InputConstants.Type type, int code, int action) {
		Minecraft mc = Minecraft.getInstance();
		if(mc.player == null || mc.screen != null) return;

		for(GunKeybind key : GunKeybind.values()) {
			InputConstants.Key bound = mapping(key).getKey();
			if(bound.getType() != type || bound.getValue() != code) continue;
			boolean current = action != GLFW.GLFW_RELEASE;
			boolean last = KEY_STATE.getOrDefault(key, false);
			if(last != current) {
				KEY_STATE.put(key, current);
				PacketDistributor.sendToServer(new GunKeyPayload(key.ordinal(), current));
			}
		}
	}

	/** RenderScreenOverlay#renderCustomCrosshairs */
	public static void renderCustomCrosshairs(GuiGraphics graphics, Crosshair cross) {

		if(cross == Crosshair.NONE) return;

		int size = cross.size;

		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		graphics.blit(HUDComponentDurabilityBar.misc, graphics.guiWidth() / 2 - (size / 2), graphics.guiHeight() / 2 - (size / 2), cross.x, cross.y, size, size);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}
}
