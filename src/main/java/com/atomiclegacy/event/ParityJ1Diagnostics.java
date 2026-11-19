package com.atomiclegacy.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.SellafieldBlock;
import com.atomiclegacy.entity.LegacyNuclearCreeperEntity;
import com.atomiclegacy.entity.LegacyTaintCrabEntity;
import com.atomiclegacy.entity.RbmkDebrisEntity;
import com.atomiclegacy.entity.ZirnoxDebrisEntity;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.radiation.HbmHazardSystem;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.radiation.HbmLivingRadiation;
import com.atomiclegacy.radiation.LegacyContaminationEffect;
import com.atomiclegacy.radiation.RadiationEvents;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.tileentity.DeconBlockEntity;
import com.atomiclegacy.util.ContaminationUtil;
import com.atomiclegacy.util.ContaminationUtil.ContaminationType;
import com.atomiclegacy.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.EffectCures;

/**
 * [parity J1] Server-side checks of the 1.7.10 living radiation mechanic and HbmPotion.radiation.
 * Expected values are quoted from work/original: potion/HbmPotion.java, util/ContaminationUtil.java,
 * handler/EntityEffectHandler.java, handler/HazmatRegistry.java, extprop/HbmLivingProps.java,
 * hazard/type/*.java and the effect sources (Balefire, BlockGasMeltdown, BlockGasRadonDense, BarbedWire,
 * BlockFallout, BlockOre, BlockSellafield, WasteEarth, EntityTaintCrab, EntityRBMKDebris, EntityZirnoxDebris,
 * ItemPill, ItemSyringe, TileEntityDecon).
 */
public final class ParityJ1Diagnostics {

	private ParityJ1Diagnostics() { }

	private static final double X = 8192.5D, Y = 300.0D, Z = 8192.5D;
	private static final float EPS = 1.0E-4F;

	public static int run(MinecraftServer server, List<String> problems) {
		int[] cases = { 0 };
		ServerLevel level = server.overworld();
		// like ParityH2Diagnostics: load the surrounding chunks so the site's entity sections become accessible
		for(int cx = ((int) X - 32) >> 4; cx <= ((int) X + 32) >> 4; cx++) for(int cz = ((int) Z - 32) >> 4; cz <= ((int) Z + 32) >> 4; cz++) level.getChunk(cx, cz);
		try {
			effect(server, level, problems, cases);
			rules(server, level, problems, cases);
			armor(server, level, problems, cases);
			sickness(level, problems, cases);
			handler(server, level, problems, cases);
			sources(server, level, problems, cases);
			removal(server, level, problems, cases);
			readouts(server, level, problems, cases);
			hazards(problems, cases);
		} catch(Throwable t) {
			StackTraceElement[] trace = t.getStackTrace();
			problems.add("parity J1: exception " + t + (trace.length > 0 ? " at " + trace[0] : "") + (trace.length > 1 ? " < " + trace[1] : "") + (trace.length > 2 ? " < " + trace[2] : ""));
		}
		cleanup(level);
		return cases[0];
	}

	/// HbmPotion.radiation ///
	private static void effect(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		MobEffect rad = HbmMobEffects.RADIATION.get();
		check(problems, cases, rad.getColor() == 0x84C128 && rad.getCategory() == MobEffectCategory.HARMFUL, "radiation: color 0x84C128, bad effect");
		check(problems, cases, rad.shouldApplyEffectTickThisTick(1, 0) && rad.shouldApplyEffectTickThisTick(37, 9), "radiation: isReady every tick");
		check(problems, cases, "effect.atomiclegacy.radiation".equals(rad.getDescriptionId()), "radiation: translation key");
		// performEffect: contaminate(RADIATION, CREATIVE, (level + 1) * 0.05F)
		int[] amps = { 0, 2, 9, 15 };
		for(int amp : amps) {
			Pig pig = pig(level, 0);
			for(int i = 0; i < 20; i++) rad.applyEffectTick(pig, amp);
			float expected = 20 * (amp + 1) * 0.05F;
			HbmLivingRadiation d = HbmLivingRadiation.get(pig);
			check(problems, cases, near(d.getRadiation(), expected) && near(d.getEnvironmentRadiation(), expected),
					"radiation amplifier " + amp + ": 20 ticks give " + expected + " RAD (got " + d.getRadiation() + ", env " + d.getEnvironmentRadiation() + ")");
			pig.discard();
		}
		// a real effect instance on a ticking mob applies once per tick and keeps milk as curative item
		Pig pig = pig(level, 0);
		ContaminationUtil.addRadiationEffect(pig, 100, 9);
		MobEffectInstance inst = pig.getEffect(HbmMobEffects.RADIATION);
		check(problems, cases, inst != null && inst.getDuration() == 100 && inst.getAmplifier() == 9 && inst.getCures().contains(EffectCures.MILK), "radiation effect: duration/amplifier kept, milk cures it");
		pig.discard();
	}

	/// ContaminationUtil.contaminate rules ///
	private static void rules(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ServerPlayer p = player(server, level);
		reset(p);
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.CREATIVE, 2F);
		check(problems, cases, near(rad(p), 2F) && near(env(p), 2F), "survival player older than 200 ticks takes CREATIVE radiation");
		reset(p);
		p.setGameMode(GameType.CREATIVE);
		boolean ok = !ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.CREATIVE, 2F);
		check(problems, cases, ok && rad(p) == 0F && near(env(p), 2F), "creative: CREATIVE radiation blocked, environment still counts");
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.NONE, 2F);
		check(problems, cases, near(rad(p), 2F), "creative: NONE radiation applies");
		reset(p);
		p.setGameMode(GameType.CREATIVE);
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 2F);
		check(problems, cases, rad(p) == 0F, "creative: RAD_BYPASS blocked");
		reset(p);
		p.setGameMode(GameType.SPECTATOR);
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.CREATIVE, 2F);
		check(problems, cases, rad(p) == 0F, "spectator treated as creative");
		reset(p);
		p.tickCount = 150;
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.NONE, 2F);
		check(problems, cases, rad(p) == 0F && near(env(p), 2F), "player younger than 200 ticks: no dose even with NONE");
		reset(p);
		p.addEffect(new MobEffectInstance(HbmMobEffects.MUTATION, 100, 0));
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.NONE, 2F);
		check(problems, cases, rad(p) == 0F && ContaminationUtil.isRadImmune(p), "mutation effect makes the player radiation immune");
		reset(p);
		p.addEffect(new MobEffectInstance(HbmMobEffects.RADX, 100, 0));
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.CREATIVE, 10F);
		float radx = rad(p);
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 10F);
		check(problems, cases, near(radx, 10F * (float) Math.pow(10, -0.2)) && near(rad(p) - radx, 10F),
				"Rad-X: CREATIVE x10^-0.2 (" + radx + "), RAD_BYPASS ignores resistance");
		// digamma
		reset(p);
		ContaminationUtil.contaminate(p, HazardType.DIGAMMA, ContaminationType.DIGAMMA, 0.5F);
		check(problems, cases, near(HbmLivingRadiation.get(p).getDigamma(), 0.5F) && env(p) == 0F, "DIGAMMA hazard raises digamma, not the radiation environment");
		reset(p);
		p.addEffect(new MobEffectInstance(HbmMobEffects.STABILITY, 100, 0));
		ContaminationUtil.contaminate(p, HazardType.DIGAMMA, ContaminationType.DIGAMMA, 0.5F);
		check(problems, cases, HbmLivingRadiation.get(p).getDigamma() == 0F, "stability blocks DIGAMMA contamination");
		reset(p);
		// immune classes (isRadImmune) and ordinary mobs
		String[] immune = { "zombie", "skeleton", "wither_skeleton", "zombified_piglin", "mooshroom", "ocelot", "cat" };
		for(String id : immune) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(id));
			Entity e = type.create(level);
			boolean immuneOk = e instanceof LivingEntity l && ContaminationUtil.isRadImmune(l)
					&& !ContaminationUtil.contaminate(l, HazardType.RADIATION, ContaminationType.NONE, 5F) && HbmLivingRadiation.get(l).getRadiation() == 0F;
			check(problems, cases, immuneOk, id + " is radiation immune");
		}
		LegacyNuclearCreeperEntity creeper = HbmEntityTypes.NUCLEAR_CREEPER.get().create(level);
		check(problems, cases, creeper != null && ContaminationUtil.isRadImmune(creeper), "nuclear creeper is radiation immune");
		Pig pig = pig(level, 0);
		check(problems, cases, !ContaminationUtil.isRadImmune(pig) && ContaminationUtil.contaminate(pig, HazardType.RADIATION, ContaminationType.CREATIVE, 5F)
				&& near(HbmLivingRadiation.get(pig).getRadiation(), 5F), "pig takes full radiation (mobs have no resistance)");
		pig.discard();
	}

	/// HazmatRegistry + calculateRadiationMod + contamination types ///
	private static void armor(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ServerPlayer p = player(server, level);
		reset(p);
		wear(p, "hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots");
		float mod = ContaminationUtil.calculateRadiationMod(p);
		check(problems, cases, near(mod, (float) Math.pow(10, -0.6)), "hazmat (yellow) set: mod 10^-0.6 (got " + mod + ")");
		check(problems, cases, near(ContaminationUtil.getResistance(p), 0.6F), "hazmat (yellow) set: resistance 0.6 (got " + ContaminationUtil.getResistance(p) + ")");
		ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.CREATIVE, 10F);
		check(problems, cases, near(rad(p), 10F * (float) Math.pow(10, -0.6)) && near(env(p), 10F), "hazmat: dose scaled, environment raw");
		reset(p);
		wear(p, "hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots");
		check(problems, cases, !ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.HAZMAT, 10F) && rad(p) == 0F, "HAZMAT contamination blocked by a hazmat set");
		check(problems, cases, ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.HAZMAT2, 10F), "HAZMAT2 contamination not blocked by the yellow hazmat set");
		reset(p);
		wear(p, "hazmat_paa_helmet", "hazmat_paa_plate", "hazmat_paa_legs", "hazmat_paa_boots");
		check(problems, cases, !ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.HAZMAT2, 10F)
				&& !ContaminationUtil.contaminate(p, HazardType.RADIATION, ContaminationType.HAZMAT, 10F), "PAA blocks HAZMAT2 and HAZMAT");
		reset(p);
	}

	/// EntityEffectHandler.handleRadiationEffect stages ///
	private static void sickness(ServerLevel level, List<String> problems, int[] cases) throws Exception {
		Method effects = RadiationEvents.class.getDeclaredMethod("applyRadiationEffects", LivingEntity.class, HbmLivingRadiation.class);
		effects.setAccessible(true);
		Holder<MobEffect>[] all = all();
		Object[][] stages = {
				// radiation, expected effects {effect, duration, amplifier}
				{ 250F, new Object[][] { { MobEffects.CONFUSION, 100, 0 }, { MobEffects.WEAKNESS, 100, 0 } } },
				{ 450F, new Object[][] { { MobEffects.CONFUSION, 150, 0 }, { MobEffects.MOVEMENT_SLOWDOWN, 100, 0 }, { MobEffects.WEAKNESS, 100, 1 } } },
				{ 650F, new Object[][] { { MobEffects.CONFUSION, 150, 0 }, { MobEffects.MOVEMENT_SLOWDOWN, 200, 2 }, { MobEffects.WEAKNESS, 200, 2 }, { MobEffects.POISON, 60, 1 } } },
				{ 850F, new Object[][] { { MobEffects.CONFUSION, 150, 0 }, { MobEffects.MOVEMENT_SLOWDOWN, 200, 2 }, { MobEffects.WEAKNESS, 200, 2 }, { MobEffects.POISON, 60, 2 }, { MobEffects.WITHER, 60, 1 } } }
		};
		for(Object[] stage : stages) {
			float r = (Float) stage[0];
			Pig pig = pig(level, 0);
			pig.setInvulnerable(true);
			HbmLivingRadiation data = HbmLivingRadiation.get(pig);
			for(int i = 0; i < 20000; i++) {
				data.setRadiation(r);
				effects.invoke(null, pig, data);
				pig.setHealth(pig.getMaxHealth());
			}
			Object[][] exp = (Object[][]) stage[1];
			StringBuilder got = new StringBuilder();
			boolean ok = true;
			int expectedCount = 0;
			for(Holder<MobEffect> h : all) {
				MobEffectInstance inst = pig.getEffect(h);
				Object[] match = null;
				for(Object[] e : exp) if(e[0] == h) match = e;
				if(match != null) expectedCount++;
				if(inst != null) got.append(h.getRegisteredName()).append('/').append(inst.getDuration()).append('/').append(inst.getAmplifier()).append(' ');
				if(match == null && inst != null) ok = false;
				if(match != null && (inst == null || inst.getDuration() != (Integer) match[1] || inst.getAmplifier() != (Integer) match[2])) ok = false;
			}
			check(problems, cases, ok && expectedCount == exp.length, "radiation sickness at " + r + " RAD: effects " + got);
			pig.discard();
		}
		// eRad < 200: nothing; eRad >= 1000: 1000 radiation damage, radiation reset, mob forced dead
		Pig calm = pig(level, 0);
		HbmLivingRadiation data = HbmLivingRadiation.get(calm);
		for(int i = 0; i < 3000; i++) { data.setRadiation(199F); effects.invoke(null, calm, data); }
		check(problems, cases, calm.getActiveEffects().isEmpty(), "below 200 RAD no sickness effects");
		data.setRadiation(1000F);
		effects.invoke(null, calm, data);
		check(problems, cases, data.getRadiation() == 0F && calm.getHealth() <= 0F && calm.isDeadOrDying(), "1000 RAD: lethal radiation damage, radiation reset to 0");
		calm.discard();
		// radiation-immune and creative entities get no sickness
		Pig immune = pig(level, 0);
		immune.addEffect(new MobEffectInstance(HbmMobEffects.MUTATION, 100, 0));
		data = HbmLivingRadiation.get(immune);
		data.setRadiation(1500F);
		effects.invoke(null, immune, data);
		check(problems, cases, immune.isAlive() && data.getRadiation() == 1500F, "radiation-immune entity: no sickness, radiation kept");
		immune.discard();
		// canVomit: everything except water creatures
		Method vomit = RadiationEvents.class.getDeclaredMethod("canVomit", LivingEntity.class);
		vomit.setAccessible(true);
		Pig v = pig(level, 0);
		Entity squid = EntityType.SQUID.create(level);
		check(problems, cases, (Boolean) vomit.invoke(null, v) && !(Boolean) vomit.invoke(null, squid), "canVomit: mobs yes, water creatures no");
		v.discard();
	}

	/// EntityEffectHandler.onUpdate pieces driven through the port's living tick ///
	private static void handler(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) throws Exception {
		Method tick = RadiationEvents.class.getDeclaredMethod("tickLiving", LivingEntity.class);
		tick.setAccessible(true);
		// handleContamination: ContaminationEffect(1F, 200, false) applies maxRad * time / maxTime per tick
		Pig pig = pig(level, 0);
		ContaminationUtil.addContamination(pig, 1F, 200, false);
		tick.invoke(null, pig);
		HbmLivingRadiation d = HbmLivingRadiation.get(pig);
		check(problems, cases, near(d.getRadiation(), 1F) && d.getContamination().size() == 1 && d.getContamination().get(0).time == 199,
				"contamination entry: first tick 1.0 RAD, time 199 (rad " + d.getRadiation() + ")");
		tick.invoke(null, pig);
		check(problems, cases, near(HbmLivingRadiation.get(pig).getRadiation(), 1F + 199F / 200F), "contamination entry decays linearly");
		LegacyContaminationEffect last = new LegacyContaminationEffect(1F, 200, false);
		last.time = 50;
		check(problems, cases, near(last.getRad(), 0.25F), "ContaminationEffect.getRad = maxRad * time / maxTime");
		pig.discard();
		// isRadImmune keeps accumulated radiation and digamma (the port used to zero them)
		Entity zombie = EntityType.ZOMBIE.create(level);
		zombie.setPos(X + 4, Y, Z);
		zombie.setNoGravity(true);
		level.addFreshEntity(zombie);
		HbmLivingRadiation zd = HbmLivingRadiation.get((LivingEntity) zombie);
		zd.setRadiation(300F);
		zd.setDigamma(1F);
		HbmLivingRadiation.set((LivingEntity) zombie, zd);
		tick.invoke(null, zombie);
		check(problems, cases, HbmLivingRadiation.get((LivingEntity) zombie).getRadiation() == 300F && HbmLivingRadiation.get((LivingEntity) zombie).getDigamma() == 1F,
				"radiation-immune zombie keeps its radiation and digamma");
		zombie.discard();
		// radBuf: every 20 ticks the environment total becomes the buffer and resets
		Pig buf = pig(level, 0);
		HbmLivingRadiation bd = HbmLivingRadiation.get(buf);
		bd.setEnvironmentRadiation(3.5F);
		HbmLivingRadiation.set(buf, bd);
		buf.tickCount = 40;
		tick.invoke(null, buf);
		bd = HbmLivingRadiation.get(buf);
		check(problems, cases, near(bd.getRadiationBuffer(), 3.5F) && bd.getEnvironmentRadiation() == 0F, "ticksExisted % 20 == 0: radBuf = radEnv, radEnv = 0");
		buf.discard();
		// RadiationConfig.hellRad: 0.1 RAD/s in the Nether -> 0.005 per tick
		ServerLevel nether = server.getLevel(Level.NETHER);
		if(nether != null) {
			BlockPos np = BlockPos.containing(1024.5, 200, 1024.5);
			for(int cx = (np.getX() >> 4) - 2; cx <= (np.getX() >> 4) + 2; cx++) for(int cz = (np.getZ() >> 4) - 2; cz <= (np.getZ() >> 4) + 2; cz++) nether.getChunk(cx, cz);
			Pig np1 = EntityType.PIG.create(nether);
			np1.setPos(np.getX() + 0.5, np.getY(), np.getZ() + 0.5);
			np1.setNoGravity(true);
			np1.setNoAi(true);
			np1.tickCount = 3;
			nether.addFreshEntity(np1);
			tick.invoke(null, np1);
			check(problems, cases, near(HbmLivingRadiation.get(np1).getRadiation(), 0.1F / 20F), "Nether ambient radiation 0.1 RAD/s (got " + HbmLivingRadiation.get(np1).getRadiation() + ")");
			np1.discard();
		}
	}

	/// effect sources: duration and amplifier ///
	private static void sources(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = BlockPos.containing(X, Y - 1, Z);
		contact(level, problems, cases, "balefire", HbmBlocks.BALEFIRE.get().defaultBlockState(), pos, 5 * 20, 9, false);
		contact(level, problems, cases, "barbed_wire_ultradeath", HbmBlocks.BARBED_WIRE_ULTRADEATH.get().defaultBlockState(), pos, 5 * 20, 9, false);
		contact(level, problems, cases, "gas_meltdown", HbmBlocks.GAS_MELTDOWN.get().defaultBlockState(), pos, 60 * 20, 2, false);
		contact(level, problems, cases, "gas_radon_dense", HbmBlocks.GAS_RADON_DENSE.get().defaultBlockState(), pos, 15 * 20, 0, false);
		contact(level, problems, cases, "fallout", HbmBlocks.FALLOUT.get().defaultBlockState(), pos, 10 * 60 * 20, 0, true);
		contact(level, problems, cases, "waste_trinitite", HbmBlocks.WASTE_TRINITITE.get().defaultBlockState(), pos, 5 * 20, 2, true);
		contact(level, problems, cases, "waste_trinitite_red", HbmBlocks.WASTE_TRINITITE_RED.get().defaultBlockState(), pos, 5 * 20, 2, true);
		contact(level, problems, cases, "brick_jungle_ooze", HbmBlocks.BRICK_JUNGLE_OOZE.get().defaultBlockState(), pos, 15 * 20, 9, true);
		contact(level, problems, cases, "waste_mycelium", HbmBlocks.WASTE_MYCELIUM.get().defaultBlockState(), pos, 30 * 20, 3, true);
		for(int meta = 0; meta < 6; meta++) {
			BlockState s = HbmBlocks.SELLAFIELD.get().defaultBlockState();
			if(meta > SellafieldBlock.LEVEL.getPossibleValues().size() - 1) break;
			contact(level, problems, cases, "sellafield level " + meta, s.setValue(SellafieldBlock.LEVEL, meta), pos, 30 * 20, meta < 5 ? meta : meta * 2, true);
		}
		// gas: 0.5 RAD contamination besides the effect
		Pig gas = pig(level, 0);
		HbmBlocks.GAS_MELTDOWN.get().defaultBlockState().entityInside(level, pos, gas);
		check(problems, cases, near(HbmLivingRadiation.get(gas).getRadiation(), 0.5F), "gas_meltdown: contaminate CREATIVE 0.5");
		gas.discard();
		// balefire block: setFire(10), no balefire burn timer
		Pig fire = pig(level, 0);
		HbmBlocks.BALEFIRE.get().defaultBlockState().entityInside(level, pos, fire);
		check(problems, cases, fire.getRemainingFireTicks() == 200 && HbmLivingHazards.get(fire).getBalefire() == 0, "balefire block: 10 s fire, no balefire timer");
		fire.discard();
		// fallout: no curative items, creative players are skipped; punching adds ContaminationEffect(1F, 200, false)
		Pig fp = pig(level, 0);
		HbmBlocks.FALLOUT.get().stepOn(level, pos, HbmBlocks.FALLOUT.get().defaultBlockState(), fp);
		MobEffectInstance fe = fp.getEffect(HbmMobEffects.RADIATION);
		check(problems, cases, fe != null && fe.getCures().isEmpty(), "fallout radiation effect has no curative items");
		// onEntityWalking cadence: not again without walking another step
		fp.removeEffect(HbmMobEffects.RADIATION);
		HbmBlocks.FALLOUT.get().stepOn(level, pos, HbmBlocks.FALLOUT.get().defaultBlockState(), fp);
		boolean noRepeat = !fp.hasEffect(HbmMobEffects.RADIATION);
		fp.moveDist += 2.0F;
		HbmBlocks.FALLOUT.get().stepOn(level, pos, HbmBlocks.FALLOUT.get().defaultBlockState(), fp);
		check(problems, cases, noRepeat && fp.hasEffect(HbmMobEffects.RADIATION), "onEntityWalking fires once per step");
		fp.discard();
		ServerPlayer p = player(server, level);
		reset(p);
		p.setGameMode(GameType.CREATIVE);
		HbmBlocks.FALLOUT.get().stepOn(level, pos, HbmBlocks.FALLOUT.get().defaultBlockState(), p);
		check(problems, cases, !p.hasEffect(HbmMobEffects.RADIATION), "fallout: creative players get no effect");
		reset(p);
		HbmBlocks.FALLOUT.get().defaultBlockState().attack(level, pos, p);
		List<LegacyContaminationEffect> cont = HbmLivingRadiation.get(p).getContamination();
		check(problems, cases, cont.size() == 1 && cont.get(0).maxRad == 1F && cont.get(0).maxTime == 200 && !cont.get(0).ignoreArmor, "fallout punch: ContaminationEffect(1F, 200, false)");
		reset(p);
		// EntityTaintCrab: radiation 10 ticks amplifier 15 within +-5
		Pig near = pig(level, 0);
		LegacyTaintCrabEntity crab = HbmEntityTypes.TAINT_CRAB.get().create(level);
		crab.setPos(X + 3, Y, Z + 2);
		crab.setNoGravity(true);
		crab.setNoAi(true);
		level.addFreshEntity(crab);
		crab.tick();
		MobEffectInstance ce = near.getEffect(HbmMobEffects.RADIATION);
		check(problems, cases, ce != null && ce.getDuration() == 10 && ce.getAmplifier() == 15, "taint crab: radiation 10 ticks, amplifier 15");
		check(problems, cases, !crab.hasEffect(HbmMobEffects.RADIATION), "taint crab skips cyber crabs (itself)");
		crab.discard();
		near.discard();
		// debris: RBMK FUEL 9 / GRAPHITE 4, ZIRNOX ELEMENT 7 / GRAPHITE 4, 60 * 20, box expanded by 2.5
		debris(level, problems, cases, new RbmkDebrisEntity(level, X + 1, Y, Z, RbmkDebrisEntity.DebrisType.FUEL), "rbmk fuel", 9);
		debris(level, problems, cases, new RbmkDebrisEntity(level, X + 1, Y, Z, RbmkDebrisEntity.DebrisType.GRAPHITE), "rbmk graphite", 4);
		debris(level, problems, cases, new ZirnoxDebrisEntity(level, X + 1, Y, Z, ZirnoxDebrisEntity.DebrisType.ELEMENT), "zirnox element", 7);
		debris(level, problems, cases, new ZirnoxDebrisEntity(level, X + 1, Y, Z, ZirnoxDebrisEntity.DebrisType.GRAPHITE), "zirnox graphite", 4);
	}

	private static void contact(ServerLevel level, List<String> problems, int[] cases, String name, BlockState state, BlockPos pos, int duration, int amp, boolean walking) {
		Pig pig = pig(level, 0);
		if(walking) state.getBlock().stepOn(level, pos, state, pig);
		else state.entityInside(level, pos, pig);
		MobEffectInstance e = pig.getEffect(HbmMobEffects.RADIATION);
		check(problems, cases, e != null && e.getDuration() == duration && e.getAmplifier() == amp,
				name + ": radiation " + duration + " ticks amplifier " + amp + " (got " + (e == null ? "none" : e.getDuration() + "/" + e.getAmplifier()) + ")");
		pig.discard();
	}

	private static void debris(ServerLevel level, List<String> problems, int[] cases, Entity debris, String name, int amp) {
		debris.setNoGravity(true);
		debris.setDeltaMovement(0, 0, 0);
		level.addFreshEntity(debris);
		Pig pig = pig(level, 0);
		debris.tick();
		MobEffectInstance e = pig.getEffect(HbmMobEffects.RADIATION);
		check(problems, cases, e != null && e.getDuration() == 60 * 20 && e.getAmplifier() == amp,
				name + " debris: radiation 1200 ticks amplifier " + amp + " (got " + (e == null ? "none" : e.getDuration() + "/" + e.getAmplifier()) + ")");
		debris.discard();
		pig.discard();
	}

	/// removePotionEffect(HbmPotion.radiation): pill_iodine, med_bag, TileEntityDecon ///
	private static void removal(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		ServerPlayer p = player(server, level);
		for(String id : new String[] { "pill_iodine", "med_bag" }) {
			reset(p);
			p.addEffect(new MobEffectInstance(HbmMobEffects.RADIATION, 1000, 3));
			use(level, p, new ItemStack(it(id)));
			check(problems, cases, !p.hasEffect(HbmMobEffects.RADIATION), id + " removes the radiation effect");
		}
		reset(p);
		BlockPos pos = BlockPos.containing(X, Y, Z);
		Pig pig = pig(level, 0);
		pig.setPos(X, Y + 0.5, Z);
		ContaminationUtil.addRadiationEffect(pig, 1000, 3);
		ContaminationUtil.addContamination(pig, 1F, 200, false);
		HbmLivingRadiation d = HbmLivingRadiation.get(pig);
		d.setRadiation(10F);
		HbmLivingRadiation.set(pig, d);
		HbmLivingHazards hz = HbmLivingHazards.get(pig);
		hz.setAsbestos(pig, 100);
		HbmLivingHazards.set(pig, hz);
		DeconBlockEntity.tick(level, pos, HbmBlocks.DECON.get().defaultBlockState(), null);
		d = HbmLivingRadiation.get(pig);
		check(problems, cases, !pig.hasEffect(HbmMobEffects.RADIATION) && d.getContamination().isEmpty() && near(d.getRadiation(), 9.5F)
				&& HbmLivingHazards.get(pig).getAsbestos() == 100, "decon: -0.5 RAD, removes the effect and contamination list, keeps asbestos");
		pig.discard();
	}

	/// ContaminationUtil.printGeigerData / printDosimeterData ///
	private static void readouts(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		List<Component> out = new ArrayList<>();
		ServerPlayer p = player(server, level, out);
		reset(p);
		HbmLivingRadiation d = HbmLivingRadiation.get(p);
		d.setRadiation(250.37F);
		d.setRadiationBuffer(3.97F);
		HbmLivingRadiation.set(p, d);
		ContaminationUtil.printGeigerData(p);
		String[] expected = {
				"===== ☢ geiger.title ☢ =====",
				"geiger.chunkRad §a0.0 RAD/s",
				"geiger.envRad §63.9 RAD/s",
				"geiger.playerRad §e250.3 RAD",
				"geiger.playerRes §f0.0% (0.0)" };
		check(problems, cases, out.size() == 5 && lines(out, expected), "geiger readout: " + flat(out));
		out.clear();
		wear(p, "hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots");
		ContaminationUtil.printGeigerData(p);
		check(problems, cases, out.size() == 5 && text(out.get(4)).startsWith("geiger.playerRes §f§a74.88% ("), "geiger resistance with hazmat: " + flat(out));
		out.clear();
		reset(p);
		d = HbmLivingRadiation.get(p);
		d.setRadiationBuffer(5F);
		HbmLivingRadiation.set(p, d);
		ContaminationUtil.printDosimeterData(p);
		check(problems, cases, out.size() == 2 && lines(out, new String[] { "===== ☢ geiger.title.dosimeter ☢ =====", "geiger.envRad §6>3.6 RAD/s" }), "dosimeter readout: " + flat(out));
		out.clear();
		d.setRadiationBuffer(0.25F);
		HbmLivingRadiation.set(p, d);
		ContaminationUtil.printDosimeterData(p);
		check(problems, cases, out.size() == 2 && "geiger.envRad §e0.2 RAD/s".equals(text(out.get(1))), "dosimeter below limit: " + flat(out));
	}

	/// HazardRegistry spot values used by the inventory pass ///
	private static void hazards(List<String> problems, int[] cases) {
		Object[][] spot = { { "ingot_u235", 1.0D }, { "nugget_u235", 0.1D }, { "billet_u235", 0.5D }, { "block_u235", 10.0D },
				{ "ingot_uranium", 0.35D }, { "ingot_u238", 0.25D }, { "ingot_pu239", 5.0D } };
		for(Object[] s : spot) {
			Item item = it((String) s[0]);
			double r = HbmHazardSystem.hazardsPerItem(new ItemStack(item)).radiation();
			check(problems, cases, Math.abs(r - (Double) s[1]) < 1.0E-5D, s[0] + ": " + s[1] + " RAD/s per item (got " + r + ")");
		}
		ItemStack pu238 = new ItemStack(it("ingot_pu238"), 16);
		check(problems, cases, HbmHazardSystem.hazardsPerItem(pu238).hot() == 3.0D, "ingot_pu238: hot 3 per item (setFire(ceil(level)) is not multiplied by the stack size)");
	}

	/// HELPERS ///
	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("parity J1: " + message);
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) <= EPS * Math.max(1F, Math.abs(b));
	}

	private static float rad(LivingEntity e) { return HbmLivingRadiation.get(e).getRadiation(); }

	private static float env(LivingEntity e) { return HbmLivingRadiation.get(e).getEnvironmentRadiation(); }

	private static Item it(String id) {
		return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
	}

	@SuppressWarnings("unchecked")
	private static Holder<MobEffect>[] all() {
		return new Holder[] { MobEffects.CONFUSION, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.POISON, MobEffects.WITHER,
				MobEffects.HUNGER, MobEffects.DIG_SLOWDOWN, MobEffects.BLINDNESS, HbmMobEffects.RADIATION };
	}

	private static void wear(Player p, String head, String chest, String legs, String feet) {
		p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(it(head)));
		p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(it(chest)));
		p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(it(legs)));
		p.setItemSlot(EquipmentSlot.FEET, new ItemStack(it(feet)));
		HbmArmorProtection.invalidateRadiationMultiplier(p);
	}

	/** Literal text with translation keys in place of translated text (independent of the server language). */
	private static String text(Component c) {
		StringBuilder b = new StringBuilder();
		if(c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) b.append(tc.getKey());
		else if(c.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents pt) b.append(pt.text());
		for(Component sibling : c.getSiblings()) b.append(text(sibling));
		return b.toString();
	}

	private static String flat(List<Component> out) {
		StringBuilder b = new StringBuilder();
		for(Component c : out) b.append('[').append(text(c)).append(']');
		return b.toString();
	}

	private static boolean lines(List<Component> out, String[] expected) {
		for(int i = 0; i < expected.length; i++) if(!expected[i].equals(text(out.get(i)))) return false;
		return true;
	}

	private static ServerPlayer player(MinecraftServer server, ServerLevel level) {
		return player(server, level, null);
	}

	/** A non-fake server player with its own profile; chat messages are captured when {@code out} is set. */
	private static ServerPlayer player(MinecraftServer server, ServerLevel level, List<Component> out) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), "parity_j1");
		ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault()) {
			@Override public void sendSystemMessage(Component component) { if(out != null) out.add(component); }
		};
		new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
			@Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
			@Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) { }
		};
		net.neoforged.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
		player.setPos(X, Y, Z);
		return player;
	}

	private static void reset(ServerPlayer p) {
		p.setGameMode(GameType.SURVIVAL);
		p.stopUsingItem();
		p.removeAllEffects();
		p.getInventory().clearContent();
		p.setHealth(p.getMaxHealth());
		p.invulnerableTime = 0;
		p.tickCount = 400;
		HbmArmorProtection.invalidateRadiationMultiplier(p);
		HbmLivingRadiation d = HbmLivingRadiation.get(p);
		d.setRadiation(0F);
		d.setDigamma(0F);
		d.setEnvironmentRadiation(0F);
		d.setRadiationBuffer(0F);
		d.getContamination().clear();
		HbmLivingRadiation.set(p, d);
	}

	private static ItemStack use(ServerLevel level, Player p, ItemStack stack) {
		p.setItemInHand(InteractionHand.MAIN_HAND, stack);
		InteractionResultHolder<ItemStack> result = stack.use(level, p, InteractionHand.MAIN_HAND);
		if(result.getObject() != stack) p.setItemInHand(InteractionHand.MAIN_HAND, result.getObject());
		if(p.isUsingItem()) {
			ItemStack using = p.getUseItem();
			ItemStack finished = using.finishUsingItem(level, p);
			if(finished != using) p.setItemInHand(InteractionHand.MAIN_HAND, finished);
			p.stopUsingItem();
		}
		return p.getMainHandItem();
	}

	private static Pig pig(ServerLevel level, int dx) {
		Pig pig = EntityType.PIG.create(level);
		pig.setPos(X + dx, Y, Z);
		pig.setNoGravity(true);
		pig.setNoAi(true);
		level.addFreshEntity(pig);
		return pig;
	}

	private static void cleanup(ServerLevel level) {
		AABB area = new AABB(X - 32, Y - 32, Z - 32, X + 32, Y + 32, Z + 32);
		for(Entity entity : level.getEntities((Entity) null, area, e -> !(e instanceof Player))) entity.discard();
		for(Entity entity : level.getEntities((Entity) null, area, e -> e instanceof ItemEntity)) entity.discard();
	}
}
