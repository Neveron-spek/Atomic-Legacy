package com.atomiclegacy.items.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.items.weapon.sedna.Crosshair;
import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunDrill;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.atomiclegacy.items.weapon.sedna.mods.XWeaponModManager;
import com.atomiclegacy.network.GunDrillParticleBurstPayload;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * [weapons batch F3] 1.7.10 XFactoryDrill: gun_drill (UTILITY) running on gasoline / leaded gasoline / coal gas / leaded coal gas.
 *
 * <p>Config key: 1.7.10 initConfig put GUN_DRILL_DAMAGE under the key "GUN_DOUBLE_BARREL_DAMAGE" (overwriting the double
 * barrel's wrapper in the config map); the port uses its own gunDrillDamage key with the same default 10.
 * The ENGINE_ELECTRIC mod (1_000 mB "operations" per shot, electric engine sound) is not ported.
 */
public class XFactoryDrill {

	public static final String D_REACH =	"D_REACH";
	public static final String F_DTNEG =	"F_DTNEG";
	public static final String F_PIERCE =	"F_PIERCE";
	public static final String I_AOE =		"I_AOE";
	public static final String I_HARVEST =	"I_HARVEST";

	/** HbmAnimations.getRelevantTransformation(bus) for the CYCLE animation; set by the client setup (identity on the server). */
	public static Function<String, double[]> RELEVANT_TRANSFORMATION = bus -> new double[] { 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 2 };

	public static ItemGunBaseNT createDrill(Item.Properties properties) {
		return new ItemGunDrill(properties, WeaponQuality.UTILITY, new GunConfig()
				.dura(3_000).draw(10).inspect(55).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
				.rec(new Receiver(0)
						.dmg(HbmConfig::drillDamage).delay(20).dry(30).auto(true).jam(0)
						.mag(new MagazineLiquidEngine(0, 4_000, fluid("gasoline"), fluid("gasoline_leaded"), fluid("coalgas"), fluid("coalgas_leaded")))
						.offset(1, -0.0625 * 2.5, -0.25D)
						.canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_DRILL_FIRE))
				.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
				.anim(LAMBDA_DRILL_ANIMS).orchestra(OrchestrasF3.ORCHESTRA_DRILL)
				);
	}

	private static com.atomiclegacy.fluid.HbmFluidDefinition fluid(String name) {
		return HbmFluids.byName(name).orElseThrow(() -> new IllegalStateException("XFactoryDrill: missing fluid " + name));
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_DRILL_FIRE = (stack, ctx) -> {
		doStandardFire(stack, ctx, GunAnimation.CYCLE, true);
	};

	public static void doStandardFire(ItemStack stack, LambdaContext ctx, GunAnimation anim, boolean calcWear) {
		Player player = ctx.getPlayer();
		int index = ctx.configIndex;
		if(anim != null) ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);

		Receiver primary = ctx.config.getReceivers(stack)[0];
		IMagazine<?> mag = primary.getMagazine(stack);

		HitResult mop = player == null ? null : OrchestrasC2.getMouseOver(player, getModdableReach(stack, 5.0D));
		if(mop != null) {
			if(mop instanceof EntityHitResult ehr) {
				float damage = primary.getBaseDamage(stack);
				if(ehr.getEntity() instanceof LivingEntity living) {
					// EntityDamageUtil.attackEntityFromNT(living, DamageSource.causePlayerDamage(player), damage, true, true, 0.1F, dtNeg, pierce):
					// EntityDamageSource -> CATEGORY_PHYSICAL resistances, no vanilla knockback, EntityDamageUtil.knockBack x0.1
					LegacyProjectileUtil.attackSednaPhysicalKnockback(living, player, damage, getModdableDTNegation(stack, 2F), getModdablePiercing(stack, 0.15F), 0.1D);
				} else {
					ehr.getEntity().hurt(player.damageSources().playerAttack(player), damage);
				}
			}
			if(player != null && mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {

				int aoe = player.isShiftKeyDown() ? 0 : getModdableAoE(stack, 1);
				BlockPos ref = bhr.getBlockPos();
				breakExtraBlock(player.level(), ref, player, ref);
				for(int i = -aoe; i <= aoe; i++) for(int j = -aoe; j <= aoe; j++) for(int k = -aoe; k <= aoe; k++) {
					if(i == 0 && j == 0 && k == 0) continue;
					breakExtraBlock(player.level(), ref.offset(i, j, k), player, ref);
				}

				didPlink = false;
			}
		}

		int ammoToUse = 10;
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_ENGINE_ELECTRIC)) ammoToUse = 1_000; // that's 1,000 operations // [weapon mods G3: electric engine]
		mag.useUpAmmo(stack, ctx.inventory, ammoToUse);
		if(calcWear) ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index), ctx.config.getDurability(stack)));
	}

	public static boolean didPlink = false;

	public static void breakExtraBlock(Level world, BlockPos pos, Player playerEntity, BlockPos ref) {
		BlockState state = world.getBlockState(pos);
		if(state.isAir()) return;
		if(!(playerEntity instanceof ServerPlayer player)) return;

		// Block#canHarvestBlock(player, meta), unbreakable (hardness -1 and no relative hardness) or the keyhole stone
		if(!state.canHarvestBlock(world, pos, player) || (state.getDestroySpeed(world, pos) == -1.0F && state.getDestroyProgress(player, world, pos) == 0.0F)
				|| state.is(HbmBlocks.STONE_KEYHOLE.get()) || state.is(HbmBlocks.STONE_KEYHOLE_META.get())) {
			if(!didPlink) {
				// World#playSoundAtEntity(player, NTMSounds.VANILLA_PLINK ("random.break"), 0.5F, 0.8F + rand * 0.6F)
				world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F + world.random.nextFloat() * 0.6F);
				didPlink = true;
			}
			return;
		}

		// we are serverside and tryHarvestBlock already invokes the 2001 packet for every player except the user, so manually send the break effect for the user
		boolean harvested = player.gameMode.destroyBlock(pos);
		if(harvested) {
			if(pos.equals(ref)) {
				player.connection.send(new ClientboundLevelEventPacket(2001, pos, Block.getId(state), false));
			} else {
				PacketDistributor.sendToPlayer(player, new GunDrillParticleBurstPayload(pos, Block.getId(state)));
			}
		}
	}

	// this system technically doesn't need to be part of the GunCfg or Receiver or anything, we can just do this and it works the exact same
	public static double getModdableReach(ItemStack stack, double base) {		return XWeaponModManager.eval(base, stack, D_REACH, HbmItems.GUN_DRILL.get(), 0); }
	public static float getModdableDTNegation(ItemStack stack, float base) {	return XWeaponModManager.eval(base, stack, F_DTNEG, HbmItems.GUN_DRILL.get(), 0); }
	public static float getModdablePiercing(ItemStack stack, float base) {		return XWeaponModManager.eval(base, stack, F_PIERCE, HbmItems.GUN_DRILL.get(), 0); }
	public static int getModdableAoE(ItemStack stack, int base) {				return XWeaponModManager.eval(base, stack, I_AOE, HbmItems.GUN_DRILL.get(), 0); }
	public static int getModdableHarvestLevel(ItemStack stack, int base) {		return XWeaponModManager.eval(base, stack, I_HARVEST, HbmItems.GUN_DRILL.get(), 0); }

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DRILL_ANIMS = (stack, type) -> {
		switch(type) {
		case EQUIP: return new BusAnimation()
				.addBus("EQUIP", new BusAnimationSequence().setPos(-1, 0, 0).addPos(0, 0, 0, 750, IType.SIN_DOWN));
		case CYCLE:
			double deploy = RELEVANT_TRANSFORMATION.apply("DEPLOY")[0];
			double speed = RELEVANT_TRANSFORMATION.apply("SPEED")[0];
			double spin = RELEVANT_TRANSFORMATION.apply("SPIN")[0] % 360; // seamlessly continue from the previous animation state
			return new BusAnimation()
					.addBus("DEPLOY", new BusAnimationSequence().setPos(deploy, 0, 0).addPos(1, 0, 0, (int) (500 * (1 - deploy)), IType.SIN_FULL).hold(1000).addPos(0, 0, 0, 500, IType.SIN_FULL))
					.addBus("SPIN", new BusAnimationSequence().setPos(spin, 0, 0).addPos(spin + 360 * 1.5, 0, 0, 1500).addPos(360 * 3, 0, 0, 750 + (int) (1000 * (1D - spin / 360D)), IType.SIN_DOWN))
					.addBus("SPEED", new BusAnimationSequence().setPos(speed, 0, 0).addPos(1, 0, 0, 500).hold(1000).addPos(0, 0, 0, 750 + (int) (1000 * (1D - spin / 360D)), IType.SIN_DOWN));
		case CYCLE_DRY: return new BusAnimation()
				.addBus("DEPLOY", new BusAnimationSequence().addPos(0.25, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
				.addBus("SPIN", new BusAnimationSequence().addPos(360 * 1, 0, 0, 1500, IType.SIN_DOWN))
				.addBus("SPEED", new BusAnimationSequence().addPos(0.75, 0, 0, 250).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
		case INSPECT: return new BusAnimation()
				.addBus("LIFT", new BusAnimationSequence().addPos(-45, 0, 0, 500, IType.SIN_FULL).hold(1000).addPos(0, 0, 0, 500, IType.SIN_DOWN));
		}

		return null;
	};
}
