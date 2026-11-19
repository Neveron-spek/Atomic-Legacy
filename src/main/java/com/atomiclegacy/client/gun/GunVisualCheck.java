package com.atomiclegacy.client.gun;

import java.util.UUID;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.WeaponTableMenu;
import com.atomiclegacy.items.StandardAmmoItem;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.network.GunKeyPayload;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Opt-in (-Datomiclegacy.visualCheck=true) scripted client run: creates a flat
 * creative world, uses the gun and the crucible through the normal input packets
 * and saves screenshots for visual review. Not active in normal play.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class GunVisualCheck {

	private static final boolean ENABLED = Boolean.getBoolean("atomiclegacy.visualCheck");
	private static boolean worldRequested;
	private static int ticks = -1;

	private GunVisualCheck() {
	}

	@SubscribeEvent
	public static void onScreen(ScreenEvent.Init.Post event) {
		if(!ENABLED || worldRequested || !(event.getScreen() instanceof TitleScreen)) return;
		worldRequested = true;
		Minecraft mc = Minecraft.getInstance();
		LevelSettings settings = new LevelSettings("visualcheck", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
		WorldOptions options = new WorldOptions(12345L, false, false);
		mc.createWorldOpenFlows().createFreshLevel("visualcheck-" + System.currentTimeMillis(), settings, options,
				registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
				event.getScreen());
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, "visual_" + name + ".png", mc.getMainRenderTarget(), message -> {});
		AtomicLegacy.LOGGER.info("HBM_VISUAL_SHOT {}", name);
	}

	private static void key(GunKeybind key, boolean state) {
		PacketDistributor.sendToServer(new GunKeyPayload(key.ordinal(), state));
	}

	private static void onServer(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
		UUID id = mc.player.getUUID();
		var server = mc.getSingleplayerServer();
		server.execute(() -> {
			ServerPlayer sp = server.getPlayerList().getPlayer(id);
			if(sp != null) action.accept(sp);
		});
	}

	/** Weapons batch C (the 10 batch A/B guns were checked in the previous session). */
	/** Weapons batch F (batches A-E were checked in the previous sessions). null ammo = special ammo, see newAmmoStack/prepareGun. */
	private static final String[] NEW_GUNS = {"gun_flamer", "gun_flamer_topaz", "gun_flamer_daybreaker", "gun_chemthrower", "gun_tau", "gun_coilgun", "gun_n_i_4_n_i", "gun_fatman", "gun_fireext", "gun_charge_thrower", "gun_drill", "gun_folly", "gun_aberrator", "gun_aberrator_eott", "gun_stinger"};
	private static final StandardAmmoItem.StandardAmmoType[] NEW_AMMO = {
			StandardAmmoItem.StandardAmmoType.FLAME_DIESEL, StandardAmmoItem.StandardAmmoType.FLAME_DIESEL, StandardAmmoItem.StandardAmmoType.FLAME_DIESEL, null, StandardAmmoItem.StandardAmmoType.TAU_URANIUM, StandardAmmoItem.StandardAmmoType.COIL_TUNGSTEN, StandardAmmoItem.StandardAmmoType.P9_SP, StandardAmmoItem.StandardAmmoType.NUKE_STANDARD, null, StandardAmmoItem.StandardAmmoType.CT_MORTAR, null, null, null, null, StandardAmmoItem.StandardAmmoType.ROCKET_HEAT};

	/** Ammo stack for one inventory slot of a new gun. */
	private static ItemStack newAmmoStack(String id, StandardAmmoItem.StandardAmmoType ammo) {
		if(ammo != null) return StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), ammo).copyWithCount(64);
		return switch(id) {
			case "gun_fireext" -> com.atomiclegacy.items.LegacyVariantItem.stackFor(HbmItems.AMMO_FIREEXT.get(), "water").copyWithCount(16);
			case "gun_folly" -> com.atomiclegacy.items.LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "folly_sm").copyWithCount(4);
			case "gun_aberrator", "gun_aberrator_eott" -> com.atomiclegacy.items.LegacyVariantItem.stackFor(HbmItems.AMMO_SECRET.get(), "p35_800").copyWithCount(64);
			default -> ItemStack.EMPTY;
		};
	}

	/** Fluid-fed guns (chemthrower, drill) are filled through their IFluidHandlerItem like a tank would. */
	private static void prepareGun(String id, ItemStack gun) {
		String fluid = id.equals("gun_chemthrower") ? "diesel" : id.equals("gun_drill") ? "gasoline" : null;
		if(fluid == null) return;
		net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(gun).ifPresent(h -> {
			var def = com.atomiclegacy.registry.HbmFluids.byName(fluid).orElseThrow();
			for(int i = 0; i < 200; i++) if(h.fill(com.atomiclegacy.registry.HbmFluids.toNeoStack(def, 1_000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) <= 0) break;
		});
	}
	private static boolean isAkimbo(String id) { return id.endsWith("akimbo") || id.equals("gun_light_revolver_dani") || id.equals("gun_aberrator_eott"); }
	private static final int GUN_SCENE = 150;

	/** Per new gun: equip, reload, fire (both hands for akimbo), aim, third person. */
	private static void gunScene(Minecraft mc, String id, StandardAmmoItem.StandardAmmoType ammo, int t) {
		switch(t) {
			case 0 -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.player.setYRot(0F);
				mc.player.setXRot(-3F);
				onServer(mc, sp -> {
					sp.closeContainer();
					sp.getInventory().clearContent();
					ItemStack gun = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AtomicLegacy.id(id)));
					prepareGun(id, gun);
					sp.getInventory().setItem(0, gun);
					for(int slot = 1; slot <= 3; slot++) sp.getInventory().setItem(slot, newAmmoStack(id, ammo));
					sp.getInventory().selected = 0;
					AtomicLegacy.LOGGER.info("HBM_VISUAL_GIVE {} -> {}", id, sp.getInventory().getItem(0));
				});
				select(mc, 0);
			}
			case 12 -> shot(mc, id + "_a_equip");
			case 30 -> {
				shot(mc, id + "_b_idle");
				AtomicLegacy.LOGGER.info("HBM_VISUAL_HELD client={} slot={}", mc.player.getMainHandItem(), mc.player.getInventory().selected);
				onServer(mc, sp -> AtomicLegacy.LOGGER.info("HBM_VISUAL_HELD server={} slot={}", sp.getMainHandItem(), sp.getInventory().selected));
			}
			case 32 -> { key(GunKeybind.RELOAD, true); key(GunKeybind.RELOAD, false); }
			case 50 -> shot(mc, id + "_c_reload");
			case 100 -> key(GunKeybind.GUN_PRIMARY, true);
			case 102 -> shot(mc, id + "_d_fire");
			case 106 -> { key(GunKeybind.GUN_PRIMARY, false); if(isAkimbo(id)) key(GunKeybind.GUN_SECONDARY, true); }
			case 108 -> { if(isAkimbo(id)) shot(mc, id + "_e_fire_right"); }
			case 112 -> { key(GunKeybind.GUN_SECONDARY, false); if(!isAkimbo(id)) { key(GunKeybind.GUN_TERTIARY, true); key(GunKeybind.GUN_TERTIARY, false); } }
			case 125 -> { if(!isAkimbo(id)) shot(mc, id + "_f_aimed"); }
			case 127 -> { if(!isAkimbo(id)) { key(GunKeybind.GUN_TERTIARY, true); key(GunKeybind.GUN_TERTIARY, false); } }
			case 135 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			case 140 -> shot(mc, id + "_g_third");
			case 142 -> { mc.options.setCameraType(CameraType.FIRST_PERSON); mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); }
			case 147 -> { shot(mc, id + "_h_inventory"); mc.setScreen(null); }
			default -> { }
		}
	}

	private static void select(Minecraft mc, int slot) {
		mc.player.getInventory().selected = slot;
		onServer(mc, sp -> sp.getInventory().selected = slot);
	}

	@SubscribeEvent
	public static void onTick(ClientTickEvent.Post event) {
		if(!ENABLED) return;
		Minecraft mc = Minecraft.getInstance();
		if(mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
		if(mc.screen != null && (ticks < 400 || (ticks > 450 && (ticks - 600) % GUN_SCENE != 145 && (ticks - 600) % GUN_SCENE != 146 && (ticks - 600) % GUN_SCENE != 147 && (ticks - 600) % GUN_SCENE != 143 && (ticks - 600) % GUN_SCENE != 144))) mc.setScreen(null);
		ticks++;

		switch(ticks) {
			case 20 -> onServer(mc, sp -> {
				ServerLevel level = sp.serverLevel();
				level.setDayTime(6000);
				level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
				BlockPos base = sp.blockPosition();
				for(int x = -4; x <= 4; x++) for(int y = 0; y <= 4; y++) level.setBlockAndUpdate(base.offset(x, y, 10), Blocks.STONE_BRICKS.defaultBlockState());
				sp.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0F, 0F);
				sp.getInventory().clearContent();
				sp.getInventory().setItem(0, new ItemStack(HbmItems.GUN_GREASEGUN.get()));
				sp.getInventory().setItem(1, StandardAmmoItem.stackFor(HbmItems.AMMO_STANDARD.get(), StandardAmmoItem.StandardAmmoType.P9_SP).copyWithCount(64));
				sp.getInventory().setItem(2, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AtomicLegacy.id("crucible"))));
				ItemStack m3 = new ItemStack(HbmItems.GUN_GREASEGUN.get());
				XWeaponModManager.install(m3, 0, new ItemStack(HbmItems.GREASEGUN_KIT.get()));
				sp.getInventory().setItem(3, m3);
				sp.getInventory().setItem(4, new ItemStack(HbmItems.GREASEGUN_KIT.get()));
				sp.getInventory().selected = 0;
			});
			case 25 -> { mc.options.setCameraType(CameraType.FIRST_PERSON); select(mc, 0); mc.player.setYRot(0F); mc.player.setXRot(0F); }
			case 32 -> shot(mc, "01_gun_equip_mid");
			case 70 -> shot(mc, "02_gun_idle");
			case 75 -> { key(GunKeybind.RELOAD, true); key(GunKeybind.RELOAD, false); }
			case 90 -> shot(mc, "03_gun_reload_mag_out");
			case 115 -> shot(mc, "04_gun_reload_turn");
			case 150 -> key(GunKeybind.GUN_PRIMARY, true);
			case 152 -> shot(mc, "05_gun_firing_a");
			case 159 -> shot(mc, "06_gun_firing_b");
			case 170 -> key(GunKeybind.GUN_PRIMARY, false);
			case 173 -> shot(mc, "07_gun_smoke_after");
			case 190 -> shot(mc, "08_gun_smoke_late");
			case 200 -> { key(GunKeybind.GUN_TERTIARY, true); key(GunKeybind.GUN_TERTIARY, false); }
			case 215 -> shot(mc, "09_gun_aimed");
			case 216 -> key(GunKeybind.GUN_PRIMARY, true);
			case 219 -> shot(mc, "10_gun_aimed_firing");
			case 222 -> key(GunKeybind.GUN_PRIMARY, false);
			case 230 -> { key(GunKeybind.GUN_TERTIARY, true); key(GunKeybind.GUN_TERTIARY, false); }
			case 240 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			case 243 -> shot(mc, "11_gun_third_person");
			case 250 -> { mc.options.setCameraType(CameraType.FIRST_PERSON); select(mc, 3); }
			case 285 -> shot(mc, "12_m3_idle");
			case 290 -> select(mc, 2);
			case 300 -> shot(mc, "13_crucible_deploy");
			case 330 -> shot(mc, "14_crucible_idle");
			case 335 -> mc.player.swing(InteractionHand.MAIN_HAND);
			case 337 -> shot(mc, "15_crucible_swing_a");
			case 341 -> shot(mc, "16_crucible_swing_b");
			case 355 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			case 358 -> shot(mc, "17_crucible_third_person");
			case 365 -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.player.setXRot(60F);
				onServer(mc, sp -> {
					ServerLevel level = sp.serverLevel();
					level.addFreshEntity(new ItemEntity(level, sp.getX() - 0.6, sp.getY() + 0.2, sp.getZ() + 2.0, new ItemStack(HbmItems.GUN_GREASEGUN.get()), 0, 0, 0));
					level.addFreshEntity(new ItemEntity(level, sp.getX() + 0.6, sp.getY() + 0.2, sp.getZ() + 2.0, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AtomicLegacy.id("crucible"))), 0, 0, 0));
				});
			}
			case 395 -> shot(mc, "18_ground_items");
			case 400 -> onServer(mc, sp -> {
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new WeaponTableMenu(id, inv), Component.translatable("container.atomiclegacy.weapons_table")));
				if(sp.containerMenu instanceof WeaponTableMenu menu) {
					menu.getSlot(7).set(sp.getInventory().getItem(3).copy());
					menu.broadcastChanges();
				}
			});
			case 420 -> shot(mc, "19_weapon_table");
			case 425 -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
			case 435 -> shot(mc, "20_inventory");
			case 440 -> {
				mc.setScreen(null);
				onServer(mc, ServerPlayer::closeContainer);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				select(mc, 0);
				mc.player.setYRot(0F);
				mc.player.setXRot(-4F);
				onServer(mc, sp -> {
					BlockPos base = sp.blockPosition();
					for(int x = -4; x <= 4; x++) for(int y = 0; y <= 4; y++) sp.serverLevel().setBlockAndUpdate(base.offset(x, y, 10), Blocks.AIR.defaultBlockState());
				});
			}
			case 445 -> { key(GunKeybind.RELOAD, true); key(GunKeybind.RELOAD, false); }
			case 520 -> key(GunKeybind.GUN_PRIMARY, true);
			case 522 -> shot(mc, "21_tracer_a");
			case 524 -> shot(mc, "22_tracer_b");
			case 527 -> shot(mc, "23_tracer_c");
			case 530 -> { key(GunKeybind.GUN_PRIMARY, false); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
			case 535 -> key(GunKeybind.GUN_PRIMARY, true);
			case 537 -> shot(mc, "24_tracer_third_a");
			case 539 -> shot(mc, "25_tracer_third_b");
			case 542 -> key(GunKeybind.GUN_PRIMARY, false);
			case 550 -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.player.setXRot(-8F);
				onServer(mc, sp -> {
					ServerLevel level = sp.serverLevel();
					net.minecraft.world.phys.Vec3 p = sp.position();
					com.atomiclegacy.entity.LegacyVortexEntity.spawn(level, p.add(0, 6, 45), 1.5F, 0F, true);
					com.atomiclegacy.entity.LegacyBlackHoleEntity.spawn(level, p.add(-32, 6, 45), 1.5F, true);
					com.atomiclegacy.entity.LegacyBlackHoleEntity.spawnRaging(level, p.add(32, 6, 45), 1.5F);
				});
			}
			case 565 -> shot(mc, "26_singularities");
			case 575 -> shot(mc, "27_singularities_b");
			case 580 -> onServer(mc, sp -> {
				for(var e : sp.serverLevel().getAllEntities()) if(e instanceof com.atomiclegacy.entity.LegacyVortexEntity || e instanceof com.atomiclegacy.entity.LegacyBlackHoleEntity) e.discard();
			});
			default -> {
				int rel = ticks - 600;
				if(rel < 0) break;
				int index = rel / GUN_SCENE;
				if(index >= NEW_GUNS.length) {
					if(rel == NEW_GUNS.length * GUN_SCENE) { AtomicLegacy.LOGGER.info("HBM_VISUAL_DONE"); mc.stop(); }
					break;
				}
				gunScene(mc, NEW_GUNS[index], NEW_AMMO[index], rel % GUN_SCENE);
			}
		}
	}
}
