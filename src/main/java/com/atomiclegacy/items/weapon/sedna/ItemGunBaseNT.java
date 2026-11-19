package com.atomiclegacy.items.weapon.sedna;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.network.GunAnimationPayload;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 com.hbm.items.weapon.sedna.ItemGunBaseNT. The original kept all gun
 * state in the stack NBT; here it is kept in the CUSTOM_DATA component under
 * the same key names. Client-only parts (HUD, render, input) live in
 * {@code com.atomiclegacy.client.gun}.
 */
public class ItemGunBaseNT extends Item {

	/** Timestamp for rendering smoke nodes and muzzle flashes */
	public long[] lastShot;
	/** [0;1] randomized every shot for various rendering applications */
	public double shotRand = 0D;

	public static List<Item> secrets = new ArrayList<>();
	/** Mod items accepted by this gun, shown in the weapon table tooltip. */
	public List<Item> recognizedMods = new ArrayList<>();

	public ItemStack defaultAmmo = ItemStack.EMPTY;
	public boolean isDefaultExpensive = false;

	public static final DecimalFormatSymbols SYMBOLS_US = new DecimalFormatSymbols(Locale.US);
	public static final DecimalFormat FORMAT_DMG = new DecimalFormat("#.##", SYMBOLS_US);

	public static float recoilVertical = 0;
	public static float recoilHorizontal = 0;
	public static float recoilDecay = 0.75F;
	public static float recoilRebound = 0.25F;
	public static float offsetVertical = 0;
	public static float offsetHorizontal = 0;

	public static void setupRecoil(float vertical, float horizontal, float decay, float rebound) {
		recoilVertical += vertical;
		recoilHorizontal += horizontal;
		recoilDecay = decay;
		recoilRebound = rebound;
	}

	public static void setupRecoil(float vertical, float horizontal) {
		setupRecoil(vertical, horizontal, 0.75F, 0.25F);
	}

	public static final String O_GUNCONFIG = "O_GUNCONFIG_";

	public static final String KEY_DRAWN = "drawn";
	public static final String KEY_AIMING = "aiming";
	public static final String KEY_MODE = "mode_";
	public static final String KEY_WEAR = "wear_";
	public static final String KEY_TIMER = "timer_";
	public static final String KEY_STATE = "state_";
	public static final String KEY_PRIMARY = "mouse1_";
	public static final String KEY_SECONDARY = "mouse2_";
	public static final String KEY_TERTIARY = "mouse3_";
	public static final String KEY_RELOAD = "reload_";
	public static final String KEY_LASTANIM = "lastanim_";
	public static final String KEY_ANIMTIMER = "animtimer_";
	public static final String KEY_LOCKONTARGET = "lockontarget";
	public static final String KEY_LOCKEDON = "lockedon";
	public static final String KEY_CANCELRELOAD = "cancel";
	public static final String KEY_EQUIPPED = "eqipped";

	/** Client aiming interpolation, written only by the client tick of the local player. */
	public static float prevAimingProgress;
	public static float aimingProgress;

	/** Set by the client setup: true for the local client player (MainRegistry.proxy.me()). */
	public static Predicate<Entity> LOCAL_PLAYER = entity -> false;

	/** NEVER ACCESS DIRECTLY - USE GETTER */
	protected GunConfig[] configs_DNA;

	public Function<ItemStack, String> LAMBDA_NAME_MUTATOR;
	public WeaponQuality quality;

	public GunConfig getConfig(ItemStack stack, int index) {
		GunConfig cfg = configs_DNA[index];
		if(stack == null) return cfg;
		return XWeaponModManager.eval(cfg, stack, O_GUNCONFIG + index, this, index);
	}

	public int getConfigCount() {
		return configs_DNA.length;
	}

	public ItemGunBaseNT(Properties properties, WeaponQuality quality, GunConfig... cfg) {
		super(properties.stacksTo(1));
		this.configs_DNA = cfg;
		this.quality = quality;
		this.lastShot = new long[cfg.length];
		for(int i = 0; i < cfg.length; i++) cfg[i].index = i;
		if(quality == WeaponQuality.LEGENDARY || quality == WeaponQuality.SECRET) secrets.add(this);
	}

	public static enum WeaponQuality {
		A_SIDE,
		B_SIDE,
		LEGENDARY,
		SPECIAL,
		UTILITY,
		SECRET,
		DEBUG
	}

	public static enum GunState {
		DRAWING,	//forced delay where nothing can be done
		IDLE,		//the gun is ready to fire or reload
		COOLDOWN,	//forced delay, but with option for refire
		RELOADING,	//forced delay after which a reload action happens, may be canceled (TBI)
		JAMMED,		//forced delay due to jamming
	}

	/** The four HbmKeybinds.EnumKeybind entries handled by guns; ordinals are sent over the network. */
	public static enum GunKeybind {
		GUN_PRIMARY,
		GUN_SECONDARY,
		GUN_TERTIARY,
		RELOAD
	}

	public ItemGunBaseNT setDefaultAmmo(ItemStack ammo) {
		this.defaultAmmo = ammo;
		return this;
	}

	/** setDefaultAmmo(EnumAmmo, amount): ammo_standard of that type; resolved lazily since the gun may register before ammo_standard. */
	public com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType defaultAmmoType;
	public int defaultAmmoCount;

	public ItemGunBaseNT setDefaultAmmo(com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType ammo, int amount) {
		this.defaultAmmoType = ammo;
		this.defaultAmmoCount = amount;
		return this;
	}

	/** [weapons batch F2] setDefaultAmmoExpensive(EnumAmmo, amount): isDefaultExpensive = true (gun_fatman). */
	public ItemGunBaseNT setDefaultAmmoExpensive(com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType ammo, int amount) {
		this.isDefaultExpensive = true;
		return setDefaultAmmo(ammo, amount);
	}

	public ItemStack getDefaultAmmo() {
		if(this.defaultAmmo.isEmpty() && this.defaultAmmoType != null) {
			ItemStack stack = com.atomiclegacy.items.StandardAmmoItem.stackFor(com.atomiclegacy.registry.HbmItems.AMMO_STANDARD.get(), this.defaultAmmoType);
			stack.setCount(this.defaultAmmoCount);
			this.defaultAmmo = stack;
		}
		return this.defaultAmmo;
	}

	public ItemGunBaseNT setNameMutator(Function<ItemStack, String> lambda) {
		this.LAMBDA_NAME_MUTATOR = lambda;
		return this;
	}

	@Override
	public Component getName(ItemStack stack) {
		if(this.LAMBDA_NAME_MUTATOR != null) {
			String unloc = this.LAMBDA_NAME_MUTATOR.apply(stack);
			if(unloc != null) return Component.translatable(unloc);
		}
		return super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {

		int configs = this.configs_DNA.length;
		for(int i = 0; i < configs; i++) {
			GunConfig config = getConfig(stack, i);
			for(Receiver rec : config.getReceivers(stack)) {
				IMagazine<?> mag = rec.getMagazine(stack);
				ItemStack icon = mag.getIconForHUD(stack, null);
				list.add(Component.translatable("gui.weapon.ammo").append(": ").append(icon.isEmpty() ? Component.empty() : icon.getHoverName()).append(" " + mag.reportAmmoStateForHUD(stack, null)));
				float dmg = rec.getBaseDamage(stack);
				list.add(Component.translatable("gui.weapon.baseDamage").append(": " + FORMAT_DMG.format(dmg)));
				if(mag.getType(stack, null) instanceof BulletConfig bullet) {
					int min = (int) (bullet.projectilesMin() * rec.getSplitProjectiles(stack));
					int max = (int) (bullet.projectilesMax() * rec.getSplitProjectiles(stack));
					list.add(Component.translatable("gui.weapon.damageWithAmmo").append(": " + FORMAT_DMG.format(dmg * bullet.damageMult()) + (min > 1 ? (" x" + (min != max ? (min + "-" + max) : min)) : "")));
				}
			}

			float maxDura = config.getDurability(stack);
			if(maxDura > 0) {
				int dura = Mth.clamp((int)((maxDura - getWear(stack, i)) * 100 / maxDura), 0, 100);
				list.add(Component.translatable("gui.weapon.condition").append(": " + dura + "%"));
			}

			for(ItemStack upgrade : XWeaponModManager.getUpgradeItems(stack, i)) {
				list.add(upgrade.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
			}
		}

		switch(this.quality) {
			case A_SIDE: list.add(Component.translatable("gui.weapon.quality.aside").withStyle(ChatFormatting.YELLOW)); break;
			case B_SIDE: list.add(Component.translatable("gui.weapon.quality.bside").withStyle(ChatFormatting.GOLD)); break;
			case LEGENDARY: list.add(Component.translatable("gui.weapon.quality.legendary").withStyle(ChatFormatting.RED)); break;
			case SPECIAL: list.add(Component.translatable("gui.weapon.quality.special").withStyle(ChatFormatting.AQUA)); break;
			case UTILITY: list.add(Component.translatable("gui.weapon.quality.utility").withStyle(ChatFormatting.GREEN)); break;
			case SECRET: list.add(Component.translatable("gui.weapon.quality.secret").withStyle(blink() ? ChatFormatting.DARK_RED : ChatFormatting.RED)); break;
			case DEBUG: list.add(Component.translatable("gui.weapon.quality.debug").withStyle(blink() ? ChatFormatting.YELLOW : ChatFormatting.GOLD)); break;
		}

		if(WEAPON_TABLE_OPEN.test(null) && !this.recognizedMods.isEmpty()) {
			list.add(Component.translatable("gui.weapon.accepts").append(":").withStyle(ChatFormatting.RED));
			for(Item comp : this.recognizedMods) list.add(Component.literal("  ").append(new ItemStack(comp).getHoverName()).withStyle(ChatFormatting.RED));
		}
	}

	/** Set by the client setup: true while the weapon modification table screen is open (GUIWeaponTable check). */
	public static Predicate<Void> WEAPON_TABLE_OPEN = unused -> false;

	/** BobMathUtil.getBlink */
	private static boolean blink() {
		return System.currentTimeMillis() % 1000 < 500;
	}

	public boolean canHandleKeybind(Player player, ItemStack stack, GunKeybind keybind) {
		return true;
	}

	public void handleKeybind(Player player, ItemStack stack, GunKeybind keybind, boolean newState) {
		handleKeybind(player, player.getInventory(), stack, keybind, newState);
	}

	public void handleKeybind(LivingEntity entity, Container inventory, ItemStack stack, GunKeybind keybind, boolean newState) {
		if(!HbmConfig.gunsEnabled()) return;

		int configs = this.configs_DNA.length;

		for(int i = 0; i < configs; i++) {
			GunConfig config = getConfig(stack, i);
			LambdaContext ctx = new LambdaContext(config, entity, inventory, i);

			if(keybind == GunKeybind.GUN_PRIMARY &&		newState && !getPrimary(stack, i)) {	if(config.getPressPrimary(stack) != null)		config.getPressPrimary(stack).accept(stack, ctx);		setPrimary(stack, i, newState);	continue; }
			if(keybind == GunKeybind.GUN_PRIMARY &&		!newState && getPrimary(stack, i)) {	if(config.getReleasePrimary(stack) != null)		config.getReleasePrimary(stack).accept(stack, ctx);		setPrimary(stack, i, newState);	continue; }
			if(keybind == GunKeybind.GUN_SECONDARY &&	newState && !getSecondary(stack, i)) {	if(config.getPressSecondary(stack) != null)		config.getPressSecondary(stack).accept(stack, ctx);		setSecondary(stack, i, newState);	continue; }
			if(keybind == GunKeybind.GUN_SECONDARY &&	!newState && getSecondary(stack, i)) {	if(config.getReleaseSecondary(stack) != null)	config.getReleaseSecondary(stack).accept(stack, ctx);	setSecondary(stack, i, newState);	continue; }
			if(keybind == GunKeybind.GUN_TERTIARY &&	newState && !getTertiary(stack, i)) {	if(config.getPressTertiary(stack) != null)		config.getPressTertiary(stack).accept(stack, ctx);		setTertiary(stack, i, newState);	continue; }
			if(keybind == GunKeybind.GUN_TERTIARY &&	!newState && getTertiary(stack, i)) {	if(config.getReleaseTertiary(stack) != null)	config.getReleaseTertiary(stack).accept(stack, ctx);	setTertiary(stack, i, newState);	continue; }
			if(keybind == GunKeybind.RELOAD &&			newState && !getReloadKey(stack, i)) {	if(config.getPressReload(stack) != null)		config.getPressReload(stack).accept(stack, ctx);		setReloadKey(stack, i, newState);	continue; }
			if(keybind == GunKeybind.RELOAD &&			!newState && getReloadKey(stack, i)) {	if(config.getReleaseReload(stack) != null)		config.getReleaseReload(stack).accept(stack, ctx);		setReloadKey(stack, i, newState);	continue; }
		}
	}

	public void onEquip(Player player, ItemStack stack) {
		for(int i = 0; i < this.configs_DNA.length; i++) {
			if(getLastAnim(stack, i) == GunAnimation.EQUIP && getAnimTimer(stack, i) < 5) continue;
			playAnimation(player, stack, GunAnimation.EQUIP, i);
			setPrimary(stack, i, false);
			setSecondary(stack, i, false);
			setTertiary(stack, i, false);
			setReloadKey(stack, i, false);
		}
	}

	public static void playAnimation(Player player, ItemStack stack, GunAnimation type, int index) {
		if(player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new GunAnimationPayload(type.ordinal(), 0, index));
		}

		setLastAnim(stack, index, type);
		setAnimTimer(stack, index, 0);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isHeld) {

		if(!(entity instanceof LivingEntity living)) return;
		Player player = entity instanceof Player p ? p : null;
		int confNo = this.configs_DNA.length;
		GunConfig[] configs = new GunConfig[confNo];
		LambdaContext[] ctx = new LambdaContext[confNo];
		for(int i = 0; i < confNo; i++) {
			configs[i] = this.getConfig(stack, i);
			ctx[i] = new LambdaContext(configs[i], living, player != null ? player.getInventory() : null, i);
		}

		if(world.isClientSide) {

			if(isHeld && LOCAL_PLAYER.test(player)) {

				/// AIMING ///
				prevAimingProgress = aimingProgress;
				boolean aiming = getIsAiming(stack);
				float aimSpeed = 0.25F;
				if(aiming && aimingProgress < 1F) aimingProgress += aimSpeed;
				if(!aiming && aimingProgress > 0F) aimingProgress -= aimSpeed;
				aimingProgress = Mth.clamp(aimingProgress, 0F, 1F);

				/// SMOKE NODES ///
				for(int i = 0; i < confNo; i++) if(configs[i].getSmokeHandler(stack) != null) {
					configs[i].getSmokeHandler(stack).accept(stack, ctx[i]);
				}

				for(int i = 0; i < confNo; i++) {
					BiConsumer<ItemStack, LambdaContext> orchestra = configs[i].getOrchestra(stack);
					if(orchestra != null) orchestra.accept(stack, ctx[i]);
				}
			}
			return;
		}

		/// ON EQUIP ///
		if(player != null) {
			boolean wasHeld = getIsEquipped(stack);

			if(!wasHeld && isHeld) {
				this.onEquip(player, stack);
			}
		}

		setIsEquipped(stack, isHeld);

		/// RESET WHEN NOT EQUIPPED ///
		if(!isHeld) {
			for(int i = 0; i < confNo; i++) {
				GunState current = getState(stack, i);
				if(current != GunState.JAMMED) {
					setState(stack, i, GunState.DRAWING);
					setTimer(stack, i, configs[i].getDrawDuration(stack));
				}
				setLastAnim(stack, i, GunAnimation.CYCLE); //prevents new guns from initializing with DRAWING, 0
			}
			setIsAiming(stack, false);
			setReloadCancel(stack, false);
			return;
		}

		for(int i = 0; i < confNo; i++) for(int k = 0; k == 0 || (k < 2 && IMagazine.isTrenchMaster(player) && getState(stack, i) == GunState.RELOADING); k++) {
			BiConsumer<ItemStack, LambdaContext> orchestra = configs[i].getOrchestra(stack);
			if(orchestra != null) orchestra.accept(stack, ctx[i]);

			setAnimTimer(stack, i, getAnimTimer(stack, i) + 1);

			/// STTATE MACHINE ///
			int timer = getTimer(stack, i);
			if(timer > 0) setTimer(stack, i, timer - 1);
			if(timer <= 1) configs[i].getDecider(stack).accept(stack, ctx[i]);
		}
	}

	/** NBT changes every tick; the 1.7.10 renderer never re-equipped the gun for them. */
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || oldStack.getItem() != newStack.getItem();
	}

	@Override
	public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
		return oldStack.getItem() != newStack.getItem();
	}

	// GUN DRAWN //
	public static boolean getIsDrawn(ItemStack stack) { return getValueBool(stack, KEY_DRAWN); }
	public static void setIsDrawn(ItemStack stack, boolean value) { setValueBool(stack, KEY_DRAWN, value); }
	// GUN STATE TIMER //
	public static int getTimer(ItemStack stack, int index) { return getValueInt(stack, KEY_TIMER + index); }
	public static void setTimer(ItemStack stack, int index, int value) { setValueInt(stack, KEY_TIMER + index, value); }
	// GUN STATE //
	public static GunState getState(ItemStack stack, int index) { return grabEnumSafely(GunState.values(), getValueByte(stack, KEY_STATE + index)); }
	public static void setState(ItemStack stack, int index, GunState value) { setValueByte(stack, KEY_STATE + index, (byte) value.ordinal()); }
	// GUN MODE //
	public static int getMode(ItemStack stack, int index) { return getValueInt(stack, KEY_MODE + index); }
	public static void setMode(ItemStack stack, int index, int value) { setValueInt(stack, KEY_MODE + index, value); }
	// GUN AIMING //
	public static boolean getIsAiming(ItemStack stack) { return getValueBool(stack, KEY_AIMING); }
	public static void setIsAiming(ItemStack stack, boolean value) { setValueBool(stack, KEY_AIMING, value); }
	// GUN WEAR //
	public static float getWear(ItemStack stack, int index) { return getValueFloat(stack, KEY_WEAR + index); }
	public static void setWear(ItemStack stack, int index, float value) { setValueFloat(stack, KEY_WEAR + index, value); }
	// LOCKON //
	public static int getLockonTarget(ItemStack stack) { return getValueInt(stack, KEY_LOCKONTARGET); }
	public static void setLockonTarget(ItemStack stack, int value) { setValueInt(stack, KEY_LOCKONTARGET, value); }
	public static boolean getIsLockedOn(ItemStack stack) { return getValueBool(stack, KEY_LOCKEDON); }
	public static void setIsLockedOn(ItemStack stack, boolean value) { setValueBool(stack, KEY_LOCKEDON, value); }
	// ANIM TRACKING //
	public static GunAnimation getLastAnim(ItemStack stack, int index) { return grabEnumSafely(GunAnimation.values(), getValueInt(stack, KEY_LASTANIM + index)); }
	public static void setLastAnim(ItemStack stack, int index, GunAnimation value) { setValueInt(stack, KEY_LASTANIM + index, value.ordinal()); }
	public static int getAnimTimer(ItemStack stack, int index) { return getValueInt(stack, KEY_ANIMTIMER + index); }
	public static void setAnimTimer(ItemStack stack, int index, int value) { setValueInt(stack, KEY_ANIMTIMER + index, value); }

	// BUTTON STATES //
	public static boolean getPrimary(ItemStack stack, int index) { return getValueBool(stack, KEY_PRIMARY + index); }
	public static void setPrimary(ItemStack stack, int index, boolean value) { setValueBool(stack, KEY_PRIMARY + index, value); }
	public static boolean getSecondary(ItemStack stack, int index) { return getValueBool(stack, KEY_SECONDARY + index); }
	public static void setSecondary(ItemStack stack, int index, boolean value) { setValueBool(stack, KEY_SECONDARY + index, value); }
	public static boolean getTertiary(ItemStack stack, int index) { return getValueBool(stack, KEY_TERTIARY + index); }
	public static void setTertiary(ItemStack stack, int index, boolean value) { setValueBool(stack, KEY_TERTIARY + index, value); }
	public static boolean getReloadKey(ItemStack stack, int index) { return getValueBool(stack, KEY_RELOAD + index); }
	public static void setReloadKey(ItemStack stack, int index, boolean value) { setValueBool(stack, KEY_RELOAD + index, value); }
	// RELOAD CANCEL //
	public static boolean getReloadCancel(ItemStack stack) { return getValueBool(stack, KEY_CANCELRELOAD); }
	public static void setReloadCancel(ItemStack stack, boolean value) { setValueBool(stack, KEY_CANCELRELOAD, value); }
	// EQUIPPED //
	public static boolean getIsEquipped(ItemStack stack) { return getValueBool(stack, KEY_EQUIPPED); }
	public static void setIsEquipped(ItemStack stack, boolean value) { setValueBool(stack, KEY_EQUIPPED, value); }

	/** EnumUtil.grabEnumSafely */
	private static <E> E grabEnumSafely(E[] values, int ordinal) {
		return values[Math.abs(ordinal % values.length)];
	}

	/// UTIL ///
	/** Read-only view of the stack's custom data, null if the stack has none (1.7.10 !hasTagCompound()). */
	public static CompoundTag readTag(ItemStack stack) {
		if(stack == null) return null;
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.getUnsafe();
	}

	public static void updateTag(ItemStack stack, Consumer<CompoundTag> editor) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, editor);
	}

	public static int getValueInt(ItemStack stack, String name) { CompoundTag tag = readTag(stack); return tag != null ? tag.getInt(name) : 0; }
	public static void setValueInt(ItemStack stack, String name, int value) { updateTag(stack, tag -> tag.putInt(name, value)); }

	public static float getValueFloat(ItemStack stack, String name) { CompoundTag tag = readTag(stack); return tag != null ? tag.getFloat(name) : 0; }
	public static void setValueFloat(ItemStack stack, String name, float value) { updateTag(stack, tag -> tag.putFloat(name, value)); }

	public static byte getValueByte(ItemStack stack, String name) { CompoundTag tag = readTag(stack); return tag != null ? tag.getByte(name) : 0; }
	public static void setValueByte(ItemStack stack, String name, byte value) { updateTag(stack, tag -> tag.putByte(name, value)); }

	public static boolean getValueBool(ItemStack stack, String name) { CompoundTag tag = readTag(stack); return tag != null && tag.getBoolean(name); }
	public static void setValueBool(ItemStack stack, String name, boolean value) { updateTag(stack, tag -> tag.putBoolean(name, value)); }

	/** Wrapper for extra context used in most Consumer lambdas which are part of the guncfg */
	public static class LambdaContext {
		public final GunConfig config;
		public final LivingEntity entity;
		public final Container inventory;
		public final int configIndex;

		public LambdaContext(GunConfig config, LivingEntity player, Container inventory, int configIndex) {
			this.config = config;
			this.entity = player;
			this.inventory = inventory;
			this.configIndex = configIndex;
		}

		public Player getPlayer() {
			if(!(entity instanceof Player)) return null;
			return (Player) entity;
		}
	}

	public static class SmokeNode {

		public double forward = 0D;
		public double side = 0D;
		public double lift = 0D;
		public double alpha;
		public double width = 1D;

		public SmokeNode(double alpha) { this.alpha = alpha; }
	}
}
