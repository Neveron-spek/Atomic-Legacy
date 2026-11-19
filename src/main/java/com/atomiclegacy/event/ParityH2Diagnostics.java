package com.atomiclegacy.event;

import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletBeamEntity;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyBuoyantItemEntity;
import com.atomiclegacy.entity.LegacyDisperserCanisterEntity;
import com.atomiclegacy.entity.LegacyGrenadeEntity;
import com.atomiclegacy.entity.LegacyLingeringFireEntity;
import com.atomiclegacy.entity.LegacyMistEntity;
import com.atomiclegacy.entity.UniversalGrenadeEntity;
import com.atomiclegacy.entity.UniversalGrenadeFillings;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;
import com.atomiclegacy.items.UniversalGrenadeItem;
import com.atomiclegacy.items.UniversalGrenadeItem.Extra;
import com.atomiclegacy.items.UniversalGrenadeItem.Filling;
import com.atomiclegacy.items.UniversalGrenadeItem.Fuze;
import com.atomiclegacy.items.UniversalGrenadeItem.Shell;
import com.atomiclegacy.items.weapon.sedna.BeamBulletConfig;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * [parity H2: diagnostics] Dedicated-server checks of the thrown explosives against the pinned 1.7.10 sources:
 * stick_dynamite / stick_dynamite_fishing (ItemGrenadeDynamite, ItemGrenadeFishing, EntityGrenadeBouncyBase),
 * grenade_universal and its parts (ItemGrenadeUniversal, ItemGrenadeShell / Filling / Fuze / Extra,
 * EntityGrenadeUniversal, EntityThrowableNT) and the disperser canister (ItemDisperser, EntityDisperserCanister).
 * The test pad is far away from (0, 0) so that it does not interfere with the other diagnostics.
 */
final class ParityH2Diagnostics {

	private ParityH2Diagnostics() {
	}

	private static final int BX = 4008, BY = 200, BZ = 4008;
	/** Top face of the stone pad. */
	private static final double TOP = BY;
	private static final AABB AREA = new AABB(BX - 48, BY - 48, BZ - 48, BX + 48, BY + 60, BZ + 48);

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		for(int cx = (BX - 24) >> 4; cx <= (BX + 24) >> 4; cx++) for(int cz = (BZ - 24) >> 4; cz <= (BZ + 24) >> 4; cz++) level.getChunk(cx, cz);
		clearAll(level);

		try {
			values(problems, cases);
			recipes(server, level, problems, cases);
			dynamite(level, problems, cases);
			universalItem(level, problems, cases);
			universalFlight(level, problems, cases);
			fillings(level, problems, cases);
			disperser(level, problems, cases);
		} catch(RuntimeException ex) {
			problems.add("parity H2: exception " + ex);
		} finally {
			NukeExplosionManager.cancelAllForDiagnostics(level);
			clearAll(level);
		}
		return cases[0];
	}

	/// ItemGrenadeShell / ItemGrenadeFilling / ItemGrenadeFuze enums and the bullet configs of ItemGrenadeFilling ///
	private static void values(List<String> problems, int[] cases) {
		// EnumGrenadeShell(stackLimit, drawDuration, bounceModifier, yeetForce)
		check(problems, cases, shell(Shell.FRAG, 4, 30, 0.5D, 1D) && shell(Shell.STICK, 4, 43, 0.25D, 1.5D)
				&& shell(Shell.TECH, 2, 30, 0.5D, 1D) && shell(Shell.NUKE, 1, 43, 0.25D, 1.5D) && Shell.values().length == 4, "EnumGrenadeShell values");
		// EnumGrenadeFilling(bodyColor, labelColor, compatibleShells)
		Object[][] fillings = {
				{ Filling.POWDER, 0x424242, 0x939176, "FS" }, { Filling.HE, 0x595533, 0xA49D62, "FS" }, { Filling.DEMO, 0x595533, 0xDD4029, "FS" },
				{ Filling.INC, 0x5A5A5A, 0xFF5F21, "FS" }, { Filling.WP, 0xDCDCDC, 0xFF5F21, "FS" }, { Filling.CLUSTER, 0x5A5A5A, 0xFFC711, "FS" },
				{ Filling.EMP, 0x93A1AC, 0x00FFFF, "T" }, { Filling.PLASMA, 0x655B2C, 0x4CFF00, "T" }, { Filling.LASER, 0x493A3A, 0xFF0000, "T" },
				{ Filling.CLUSTER_HEAVY, 0x5A5A5A, 0xFF5F21, "N" }, { Filling.NUCLEAR, 0xDFD7A8, 0xA49D62, "N" },
				{ Filling.NUCLEAR_DEMO, 0xDFD7A8, 0xDD4029, "N" }, { Filling.SCHRAB, 0x00BDBD, 0x000000, "N" } };
		boolean ok = Filling.values().length == fillings.length;
		for(int i = 0; i < fillings.length && ok; i++) {
			Filling f = (Filling) fillings[i][0];
			String c = (String) fillings[i][3];
			ok = f.ordinal() == i && f.bodyColor() == (int) fillings[i][1] && f.labelColor() == (int) fillings[i][2]
					&& f.supports(Shell.FRAG) == c.contains("F") && f.supports(Shell.STICK) == c.contains("S")
					&& f.supports(Shell.TECH) == c.contains("T") && f.supports(Shell.NUKE) == c.contains("N");
		}
		check(problems, cases, ok, "EnumGrenadeFilling colors / compatible shells");
		check(problems, cases, Fuze.S3.bandColor() == 0x000000 && Fuze.S7.bandColor() == 0x404040 && Fuze.S15.bandColor() == 0x808080
				&& Fuze.IMPACT.bandColor() == 0xE36C17 && Fuze.AIRBURST.bandColor() == 0x56A137 && Fuze.values().length == 5
				&& Extra.values().length == 4 && Extra.GLUE.ordinal() == 0 && Extra.PROXY_FUZE.ordinal() == 1 && Extra.FRAG_SLEEVE.ordinal() == 2 && Extra.TRIPLEX.ordinal() == 3,
				"EnumGrenadeFuze band colors / EnumGrenadeExtra order");

		// fragmentation: setLife(3).setThresholdNegation(5F).setRicochetAngle(90).setRicochetCount(2), default velocity 10
		StandardAmmoType frag = StandardAmmoType.GRENADE_FRAGMENTATION;
		check(problems, cases, frag.projectileLifetime() == 3 && frag.armorThresholdNegation() == 5F && frag.ricochetAngle() == 90F && frag.maxRicochetCount() == 2
				&& frag.projectileSpeed() == 10D && frag.projectileGravity() == 0D && frag.damageMultiplier() == 1F && frag.onImpact() == null, "fragmentation bullet config");
		// pellets / pellets_heavy: setLife(100).setGrav(0.04).setVel(1.5F).setOnImpact(LAMBDA_TINY_EXPLODE / LAMBDA_EXPLODE)
		for(StandardAmmoType t : new StandardAmmoType[] { StandardAmmoType.GRENADE_PELLETS, StandardAmmoType.GRENADE_PELLETS_HEAVY }) {
			check(problems, cases, t.projectileLifetime() == 100 && t.projectileGravity() == 0.04D && t.projectileSpeed() == 1.5D && t.damageMultiplier() == 1F
					&& t.onImpact() == (t == StandardAmmoType.GRENADE_PELLETS ? UniversalGrenadeFillings.LAMBDA_TINY_EXPLODE : UniversalGrenadeFillings.LAMBDA_EXPLODE), t + " bullet config");
		}
		// laser: setBeam().setupDamageClass(LASER).setLife(3).setRenderRotations(false).setThresholdNegation(10F), RENDER_LASER_RED
		BeamBulletConfig laser = UniversalGrenadeFillings.laser;
		check(problems, cases, laser.dmgClass == BeamBulletConfig.DamageClass.LASER && laser.expires == 3 && !laser.renderRotations && laser.thresholdNegation() == 10F
				&& laser.armorPiercing() == 0F && laser.damageMult() == 1F && !laser.doesPenetrate() && laser.laserColor == 0x801515
				&& laser.onImpactBeam == BeamBulletConfig.LAMBDA_STANDARD_BEAM_HIT, "laser beam config");

		// getItemStackLimit: shell stack limit; ItemGrenade: maxStackSize 16
		check(problems, cases, UniversalGrenadeItem.make(Shell.FRAG, Filling.HE, Fuze.S3, null).getMaxStackSize() == 4
				&& UniversalGrenadeItem.make(Shell.TECH, Filling.EMP, Fuze.S3, null).getMaxStackSize() == 2
				&& UniversalGrenadeItem.make(Shell.NUKE, Filling.NUCLEAR, Fuze.S3, null).getMaxStackSize() == 1
				&& new ItemStack(HbmItems.STICK_DYNAMITE.get()).getMaxStackSize() == 16 && new ItemStack(HbmItems.STICK_DYNAMITE_FISHING.get()).getMaxStackSize() == 16,
				"stack limits");
		// defaults of getShell / getFilling / getFuze / getExtra without NBT
		ItemStack plain = new ItemStack(HbmItems.GRENADE_UNIVERSAL.get());
		check(problems, cases, UniversalGrenadeItem.shell(plain) == Shell.FRAG && UniversalGrenadeItem.filling(plain) == Filling.HE
				&& UniversalGrenadeItem.fuze(plain) == Fuze.S3 && UniversalGrenadeItem.extra(plain) == null, "grenade defaults without data");
	}

	private static boolean shell(Shell shell, int stack, int draw, double bounce, double force) {
		return shell.stackLimit() == stack && shell.drawDuration() == draw && shell.bounce() == bounce && shell.throwForce() == force;
	}

	/// WeaponRecipes / GrenadeCraftingHandler ///
	private static void recipes(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
		// grenade_shell FRAG x4: "B", "P", "S" with STEEL.bolt(), AL.plate() (plate_aluminium, not the cast plate), STEEL.shell()
		shaped(problems, cases, server, level, "grenade_shell_frag", HbmItems.GRENADE_SHELL_ITEMS.get("frag").get(), 4, 1, 3, List.of(item("bolt_steel"), item("plate_aluminium"), item("shell_steel")));
		notShaped(problems, cases, server, level, "grenade_shell_frag", 1, 3, List.of(item("bolt_steel"), item("plate_cast_aluminium"), item("shell_steel")));
		// grenade_filling CLUSTER_HEAVY x1: "F", "I", "F" with pellet_cluster and WEAPONSTEEL.plate() (plate_weaponsteel)
		shaped(problems, cases, server, level, "grenade_filling_cluster_heavy", HbmItems.GRENADE_FILLING_ITEMS.get("cluster_heavy").get(), 1, 1, 3, List.of(item("pellet_cluster"), item("plate_weaponsteel"), item("pellet_cluster")));
		notShaped(problems, cases, server, level, "grenade_filling_cluster_heavy", 1, 3, List.of(item("pellet_cluster"), item("plate_cast_weaponsteel"), item("pellet_cluster")));
		// grenade_filling EMP x4: " C ", "KWK" with circuit BASIC, coil_gold, WEAPONSTEEL.plate()
		shaped(problems, cases, server, level, "grenade_filling_emp", HbmItems.GRENADE_FILLING_ITEMS.get("emp").get(), 4, 3, 2,
				List.of(ItemStack.EMPTY, item("circuit_basic"), ItemStack.EMPTY, item("coil_gold"), item("plate_weaponsteel"), item("coil_gold")));
		// grenade_filling NUCLEAR x1: " T ", "CPC", " T " with ball_tatb, WEAPONSTEEL.plate(), PU239.nugget()
		shaped(problems, cases, server, level, "grenade_filling_nuclear", HbmItems.GRENADE_FILLING_ITEMS.get("nuclear").get(), 1, 3, 3,
				List.of(ItemStack.EMPTY, item("ball_tatb"), ItemStack.EMPTY, item("plate_weaponsteel"), item("nugget_pu239"), item("plate_weaponsteel"), ItemStack.EMPTY, item("ball_tatb"), ItemStack.EMPTY));
		// stick_dynamite x4: " S ", "PDP", "PDP" with safety_fuse, paper, ball_dynamite
		shaped(problems, cases, server, level, "stick_dynamite", HbmItems.STICK_DYNAMITE.get(), 4, 3, 3,
				List.of(ItemStack.EMPTY, item("safety_fuse"), ItemStack.EMPTY, new ItemStack(Items.PAPER), item("ball_dynamite"), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), item("ball_dynamite"), new ItemStack(Items.PAPER)));
		// grenade_fuze S3 x4: "S", "F" with STEEL.bolt(), safety_fuse
		shaped(problems, cases, server, level, "grenade_fuze_s3", HbmItems.GRENADE_FUZE_ITEMS.get("s3").get(), 4, 1, 2, List.of(item("bolt_steel"), item("safety_fuse")));

		// GrenadeCraftingHandler: exactly one shell, one compatible filling, one fuze and at most one extra, shapeless
		ItemStack sFrag = new ItemStack(HbmItems.GRENADE_SHELL_ITEMS.get("frag").get()), sTech = new ItemStack(HbmItems.GRENADE_SHELL_ITEMS.get("tech").get());
		ItemStack fHe = new ItemStack(HbmItems.GRENADE_FILLING_ITEMS.get("he").get()), fuS7 = new ItemStack(HbmItems.GRENADE_FUZE_ITEMS.get("s7").get());
		ItemStack eGlue = new ItemStack(HbmItems.GRENADE_EXTRA_ITEMS.get("glue").get()), eTri = new ItemStack(HbmItems.GRENADE_EXTRA_ITEMS.get("triplex").get());
		ItemStack out = craft(server, level, List.of(fuS7, sFrag, fHe));
		check(problems, cases, out.is(HbmItems.GRENADE_UNIVERSAL.get()) && out.getCount() == 1 && UniversalGrenadeItem.shell(out) == Shell.FRAG && UniversalGrenadeItem.filling(out) == Filling.HE
				&& UniversalGrenadeItem.fuze(out) == Fuze.S7 && UniversalGrenadeItem.extra(out) == null, "grenade assembly without extra");
		out = craft(server, level, List.of(sFrag, fHe, fuS7, eGlue));
		check(problems, cases, out.is(HbmItems.GRENADE_UNIVERSAL.get()) && UniversalGrenadeItem.extra(out) == Extra.GLUE, "grenade assembly with extra");
		check(problems, cases, !craft(server, level, List.of(sTech, fHe, fuS7)).is(HbmItems.GRENADE_UNIVERSAL.get()), "incompatible shell is rejected");
		check(problems, cases, !craft(server, level, List.of(sFrag, fHe, fuS7, eGlue, eTri)).is(HbmItems.GRENADE_UNIVERSAL.get()), "five parts are rejected");
		check(problems, cases, !craft(server, level, List.of(sFrag, sFrag, fHe, fuS7)).is(HbmItems.GRENADE_UNIVERSAL.get()), "two shells are rejected");
		check(problems, cases, !craft(server, level, List.of(sFrag, fHe)).is(HbmItems.GRENADE_UNIVERSAL.get()), "missing fuze is rejected");
		// getSubItems: every compatible shell / filling pair with 5 fuzes, each without extra and with the 4 extras: 19 * 5 * 5
		int[] variants = {0};
		((UniversalGrenadeItem) HbmItems.GRENADE_UNIVERSAL.get()).addCreativeVariants((stack, visibility) -> variants[0]++);
		check(problems, cases, variants[0] == 475, "creative variants: " + variants[0]);
	}

	/// ItemGrenadeDynamite / ItemGrenadeFishing / EntityGrenadeBouncyBase ///
	private static void dynamite(ServerLevel level, List<String> problems, int[] cases) {
		pad(level);
		FakePlayer player = player(level);

		// onItemRightClick: stack - 1, EntityGrenadeBouncyBase(world, living): 0.16 to the right, 0.1 below the eyes, velocity 1.5 with 0.0075 spread
		ItemStack stack = new ItemStack(HbmItems.STICK_DYNAMITE.get(), 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		stack.getItem().use(level, player, InteractionHand.MAIN_HAND);
		List<LegacyGrenadeEntity> thrown = level.getEntitiesOfClass(LegacyGrenadeEntity.class, AREA);
		check(problems, cases, thrown.size() == 1 && stack.getCount() == 2, "dynamite throw spawns one entity and consumes one stick");
		if(thrown.size() == 1) {
			LegacyGrenadeEntity g = thrown.get(0);
			Vec3 m = g.getDeltaMovement();
			check(problems, cases, Math.abs(g.getX() - (player.getX() - 0.16D)) < 1.0E-6 && Math.abs(g.getY() - (player.getEyeY() - 0.1D)) < 1.0E-6 && Math.abs(g.getZ() - player.getZ()) < 1.0E-6
					&& Math.abs(m.z - 1.5D) < 0.1D && Math.abs(m.x) < 0.1D && Math.abs(m.y) < 0.1D && g.kind() == LegacyGrenadeEntity.Kind.DYNAMITE,
					"dynamite throw position / velocity: " + g.position() + " " + m);
		}
		discardAll(level);

		// bounce: the hit axis is mirrored and the motion multiplied by getBounceMod() = 0.5, then drag 0.99 and gravity 0.03
		LegacyGrenadeEntity bounce = new LegacyGrenadeEntity(level, LegacyGrenadeEntity.Kind.DYNAMITE, new Vec3(BX + 0.5, TOP + 0.3, BZ + 0.5), new Vec3(0.2, -0.5, 0));
		level.addFreshEntity(bounce);
		bounce.tick();
		Vec3 bm = bounce.getDeltaMovement();
		check(problems, cases, Math.abs(bm.x - 0.2 * 0.5 * 0.99) < 1.0E-9 && Math.abs(bm.y - (0.5 * 0.5 * 0.99 - 0.03)) < 1.0E-9 && Math.abs(bounce.getY() - (TOP + 0.3 - 0.3 * 0.6)) < 1.0E-9,
				"dynamite bounce: motion " + bm + " y " + bounce.getY());
		bounce.discard();

		// fuse: ItemGrenade.fuse 3 -> getMaxTimer() 60; explosion: ExplosionVNT(5) with EntityProcessorCrossSmooth(1, 15) only -> no block damage
		for(LegacyGrenadeEntity.Kind kind : LegacyGrenadeEntity.Kind.values()) {
			pad(level);
			level.setBlockAndUpdate(new BlockPos(BX - 1, BY, BZ), Blocks.DIRT.defaultBlockState());
			Pig pig = pig(level, BX + 3.5, BZ + 0.5);
			LegacyGrenadeEntity g = new LegacyGrenadeEntity(level, kind, new Vec3(BX + 0.5, TOP + 0.5, BZ + 0.5), Vec3.ZERO);
			level.addFreshEntity(g);
			int ticks = 0;
			while(!g.isRemoved() && ticks < 200) { g.tick(); ticks++; }
			check(problems, cases, ticks == 60, kind + " fuse: " + ticks + " ticks");
			boolean intact = level.getBlockState(new BlockPos(BX - 1, BY, BZ)).is(Blocks.DIRT) && level.getBlockState(new BlockPos(BX, BY - 1, BZ)).is(Blocks.STONE);
			check(problems, cases, intact, kind + " explosion leaves blocks intact");
			if(kind == LegacyGrenadeEntity.Kind.DYNAMITE && pig != null) {
				float expected = 15F * (float) (1D - boxDist(pig, g.position()) / 10D);
				check(problems, cases, Math.abs((1000F - pig.getHealth()) - expected) < 0.01F, "dynamite damage " + (1000F - pig.getHealth()) + " expected " + expected);
			} else if(pig != null) {
				// world.newExplosion(null, x, y + 0.25, z, 3F, false, false): vanilla damage, no fire
				check(problems, cases, pig.getHealth() < 1000F, "fishing dynamite damages entities");
			}
			discardAll(level);
		}

		// ItemGrenadeFishing: 15 picks in the 15^3 cube around the blast, each water block yields one fish (junk / treasure chance 0), EntityItemBuoyant with motionY 1
		BlockPos c = new BlockPos(BX, BY + 20, BZ);
		for(BlockPos p : BlockPos.betweenClosed(c.offset(-7, -7, -7), c.offset(7, 7, 7))) level.setBlock(p, Blocks.WATER.defaultBlockState(), 18);
		LegacyGrenadeEntity fishing = new LegacyGrenadeEntity(level, LegacyGrenadeEntity.Kind.FISHING, Vec3.atCenterOf(c), Vec3.ZERO);
		level.addFreshEntity(fishing);
		fishing.setDeltaMovement(Vec3.ZERO);
		for(int i = 0; i < 60 && !fishing.isRemoved(); i++) { fishing.setPos(Vec3.atCenterOf(c)); fishing.setDeltaMovement(Vec3.ZERO); fishing.tick(); }
		List<LegacyBuoyantItemEntity> fish = level.getEntitiesOfClass(LegacyBuoyantItemEntity.class, AREA);
		boolean onlyFish = true, rising = true, inside = true;
		for(LegacyBuoyantItemEntity e : fish) {
			ItemStack s = e.getItem();
			onlyFish &= s.is(Items.COD) || s.is(Items.SALMON) || s.is(Items.TROPICAL_FISH) || s.is(Items.PUFFERFISH);
			rising &= e.getDeltaMovement().y == 1D;
			inside &= Math.abs(e.getX() - (Math.floor(e.getX()) + 0.5)) < 1.0E-6 && Math.abs(e.getX() - (BX + 0.5)) <= 7.01;
		}
		check(problems, cases, fishing.isRemoved() && fish.size() == 15 && onlyFish && rising && inside, "fishing dynamite loot: " + fish.size() + " items, fish only " + onlyFish + ", rising " + rising + ", in water blocks " + inside);
		for(BlockPos p : BlockPos.betweenClosed(c.offset(-7, -7, -7), c.offset(7, 7, 7))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
		discardAll(level);
	}

	/// ItemGrenadeUniversal: draw duration, throw, EntityGrenadeUniversal(world, thrower, grenade) ///
	private static void universalItem(ServerLevel level, List<String> problems, int[] cases) {
		pad(level);
		FakePlayer player = player(level);
		for(Shell shell : new Shell[] { Shell.FRAG, Shell.STICK }) {
			ItemStack stack = UniversalGrenadeItem.make(shell, Filling.POWDER, Fuze.S15, null);
			stack.setCount(2);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			UniversalGrenadeItem item = (UniversalGrenadeItem) stack.getItem();
			// onUpdate: the first held tick is onEquip (deployment 0), every further one deployment++
			item.inventoryTick(stack, level, player, 0, true);
			for(int i = 0; i < shell.drawDuration() - 1; i++) item.inventoryTick(stack, level, player, 0, true);
			item.use(level, player, InteractionHand.MAIN_HAND);
			check(problems, cases, level.getEntitiesOfClass(UniversalGrenadeEntity.class, AREA).isEmpty() && stack.getCount() == 2 && UniversalGrenadeItem.deployment(player) == shell.drawDuration() - 1,
					shell + " grenade is not thrown before the draw duration");
			item.inventoryTick(stack, level, player, 0, true);
			item.use(level, player, InteractionHand.MAIN_HAND);
			List<UniversalGrenadeEntity> thrown = level.getEntitiesOfClass(UniversalGrenadeEntity.class, AREA);
			check(problems, cases, thrown.size() == 1 && stack.getCount() == 1 && UniversalGrenadeItem.deployment(player) == 0, shell + " grenade throw: entities " + thrown.size() + ", stack " + stack.getCount());
			if(thrown.size() == 1) {
				UniversalGrenadeEntity g = thrown.get(0);
				Vec3 m = g.getDeltaMovement();
				// Vec3NT(0.25, -0.25, 0).rotateAroundYDeg(-yaw + 180) at yaw 0 -> (-0.25, -0.25, 0); velocity = getYeetForce() without spread
				check(problems, cases, Math.abs(g.getX() - (player.getX() - 0.25D)) < 1.0E-5 && Math.abs(g.getY() - (player.getEyeY() - 0.25D)) < 1.0E-6 && Math.abs(g.getZ() - player.getZ()) < 1.0E-5
						&& Math.abs(m.z - shell.throwForce()) < 1.0E-6 && Math.abs(m.x) < 1.0E-6 && Math.abs(m.y) < 1.0E-6 && g.getThrower() == player
						&& g.shell() == shell && g.filling() == Filling.POWDER && g.fuze() == Fuze.S15 && g.grenadeStack().getCount() == 1,
						shell + " grenade throw position / velocity: " + g.position() + " " + m);
			}
			discardAll(level);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	/// EntityThrowableNT flight, ItemGrenadeFuze and ItemGrenadeExtra lambdas ///
	private static void universalFlight(ServerLevel level, List<String> problems, int[] cases) {
		pad(level);

		// free flight: drag 0.99, gravity 0.03, plain displacement
		UniversalGrenadeEntity free = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.S15, null, BX + 0.5, TOP + 20, BZ + 0.5, new Vec3(0, 0, 1));
		free.tick();
		Vec3 fm = free.getDeltaMovement();
		check(problems, cases, Math.abs(free.getZ() - (BZ + 1.5)) < 1.0E-6 && Math.abs(fm.z - 0.99) < 1.0E-6 && Math.abs(fm.y + 0.03) < 1.0E-6 && free.getTimer() == 1, "free flight tick: " + free.position() + " " + fm);
		free.discard();

		// onImpact (block): position = hit + 0.05 * normal, hit axis * -getBounce(), other axes * 0.8, bounces + 1
		for(Shell shell : new Shell[] { Shell.FRAG, Shell.STICK }) {
			UniversalGrenadeEntity b = grenade(level, shell, Filling.POWDER, Fuze.S15, null, BX + 0.5, TOP + 0.3, BZ + 0.5, new Vec3(0.3, -0.5, 0));
			b.tick();
			Vec3 m = b.getDeltaMovement();
			double my = 0.5 * shell.bounce();
			check(problems, cases, b.getBounces() == 1 && Math.abs(m.x - 0.3 * 0.8 * 0.99) < 1.0E-6 && Math.abs(m.y - (my * 0.99 - 0.03)) < 1.0E-6 && Math.abs(b.getY() - (TOP + 0.05 + my)) < 1.0E-6,
					shell + " bounce: " + m + " y " + b.getY() + " bounces " + b.getBounces());
			b.discard();
		}

		// FUZE_3S / FUZE_7S / FUZE_15S: explode once getTimer() reaches 60 / 140 / 300
		int[] timers = { 60, 140, 300 };
		Fuze[] timed = { Fuze.S3, Fuze.S7, Fuze.S15 };
		for(int i = 0; i < 3; i++) {
			pad(level);
			UniversalGrenadeEntity g = grenade(level, Shell.FRAG, Filling.POWDER, timed[i], null, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
			int ticks = 0;
			while(!g.isRemoved() && ticks < 400) { g.tick(); ticks++; }
			check(problems, cases, ticks == timers[i] && level.getBlockState(new BlockPos(BX, BY - 1, BZ)).is(Blocks.STONE), timed[i] + " fuze: " + ticks + " ticks");
			discardAll(level);
		}

		// FUZE_IMPACT: bounces during the first 10 ticks (0.5 s safety), afterwards explodes at the hit vector
		UniversalGrenadeEntity early = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.IMPACT, null, BX + 0.5, TOP + 0.3, BZ + 0.5, new Vec3(0, -0.5, 0));
		early.tick();
		check(problems, cases, !early.isRemoved() && early.getBounces() == 1, "impact fuze does not trigger before 10 ticks");
		early.discard();
		UniversalGrenadeEntity impact = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.IMPACT, null, BX + 0.5, TOP + 25, BZ + 0.5, Vec3.ZERO);
		int ticks = 0;
		while(!impact.isRemoved() && ticks < 400) { impact.tick(); ticks++; }
		// the dead entity is still displaced by its motion after the explosion at the hit vector, as in EntityThrowableNT
		check(problems, cases, impact.isRemoved() && impact.getTimer() >= 10 && ticks < 100 && impact.getBounces() == 0 && impact.getY() <= TOP + 1.0E-6, "impact fuze: timer " + impact.getTimer() + " y " + impact.getY());
		discardAll(level);

		// FUZE_AIRBURST: after 30 ticks, as soon as a block is within 10 blocks below
		UniversalGrenadeEntity air = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.AIRBURST, null, BX + 0.5, TOP + 45, BZ + 0.5, Vec3.ZERO);
		ticks = 0;
		double prevHeight = 45;
		while(!air.isRemoved() && ticks < 400) { prevHeight = air.getY() - TOP; air.tick(); ticks++; }
		double height = air.getY() - TOP;
		check(problems, cases, air.isRemoved() && air.getTimer() > 30 && height > 0 && height <= 10 && prevHeight > 10, "airburst fuze: timer " + air.getTimer() + " height " + height + " previous " + prevHeight);
		discardAll(level);
		UniversalGrenadeEntity lowAir = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.AIRBURST, null, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
		ticks = 0;
		while(!lowAir.isRemoved() && ticks < 400) { lowAir.tick(); ticks++; }
		check(problems, cases, ticks == 30, "airburst fuze safety: " + ticks + " ticks");
		discardAll(level);

		// EXTRA_PROXY: from 10 ticks on, every third tick, any living entity except the thrower within 10 blocks
		Pig near = pig(level, BX + 8.5, BZ + 0.5);
		UniversalGrenadeEntity proxy = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.S15, Extra.PROXY_FUZE, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
		ticks = 0;
		while(!proxy.isRemoved() && ticks < 400) { proxy.tick(); ticks++; }
		check(problems, cases, near != null && ticks == 12, "proximity fuze: " + ticks + " ticks");
		discardAll(level);
		Pig far = pig(level, BX + 12.5, BZ + 0.5);
		UniversalGrenadeEntity proxyFar = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.S3, Extra.PROXY_FUZE, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
		ticks = 0;
		while(!proxyFar.isRemoved() && ticks < 400) { proxyFar.tick(); ticks++; }
		check(problems, cases, far != null && ticks == 60, "proximity fuze out of range: " + ticks + " ticks");
		discardAll(level);

		// EXTRA_GLUE: sticks to the block (getStuck), the timer keeps running through ticksInGround, released when the block changes
		pad(level);
		UniversalGrenadeEntity glue = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.S3, Extra.GLUE, BX + 0.5, TOP + 0.3, BZ + 0.5, new Vec3(0, -0.5, 0));
		glue.tick();
		double stuckY = glue.getY();
		for(int i = 0; i < 5; i++) glue.tick();
		check(problems, cases, glue.isStuck() && Math.abs(stuckY - (TOP + 0.05)) < 1.0E-9 && glue.getY() == stuckY && glue.getTimer() == 6 && glue.getBounces() == 1, "glue: y " + glue.getY() + " timer " + glue.getTimer());
		level.setBlockAndUpdate(new BlockPos(BX, BY - 1, BZ), Blocks.AIR.defaultBlockState());
		glue.tick();
		check(problems, cases, !glue.isStuck() && glue.getTimer() == 0, "glue release resets the timer: " + glue.getTimer());
		glue.discard();
		pad(level);
		UniversalGrenadeEntity glueTimed = grenade(level, Shell.FRAG, Filling.POWDER, Fuze.S3, Extra.GLUE, BX + 0.5, TOP + 0.3, BZ + 0.5, new Vec3(0, -0.5, 0));
		ticks = 0;
		while(!glueTimed.isRemoved() && ticks < 400) { glueTimed.tick(); ticks++; }
		check(problems, cases, ticks == 60, "glued grenade fuze: " + ticks + " ticks");
		discardAll(level);
	}

	/// EnumGrenadeFilling.explode and the onExplode extras ///
	private static void fillings(ServerLevel level, List<String> problems, int[] cases) {
		// standardExplode(range, damage): EntityProcessorCrossSmooth(1, damage), damage * (1 - distance / (2 * range)), no block damage
		// POWDER 5 / 10, HE 7.5 / 25, INC and WP 3 / 10, CLUSTER and CLUSTER_HEAVY 7.5 / 15, DEMO 5 / 10 with BlockAllocatorStandard
		// explodeStandardEnergy: EMP 15 damage / range 3, PLASMA 50 / 5; EXPLODE_NUKE 100 / 10 * 1.5, EXPLODE_NUKE_DEMO 50 / 10 * 1.5 with blocks
		Object[][] table = {
				{ Shell.STICK, Filling.POWDER, 10F, 10D, false }, { Shell.STICK, Filling.HE, 25F, 15D, false }, { Shell.STICK, Filling.DEMO, 10F, 10D, true },
				{ Shell.STICK, Filling.INC, 10F, 6D, false }, { Shell.STICK, Filling.WP, 10F, 6D, false }, { Shell.STICK, Filling.CLUSTER, 15F, 15D, false },
				{ Shell.FRAG, Filling.CLUSTER, 15F, 15D, false }, { Shell.NUKE, Filling.CLUSTER_HEAVY, 15F, 15D, false },
				{ Shell.TECH, Filling.EMP, 15F, 6D, false }, { Shell.TECH, Filling.PLASMA, 50F, 10D, false },
				{ Shell.NUKE, Filling.NUCLEAR, 100F, 30D, false }, { Shell.NUKE, Filling.NUCLEAR_DEMO, 50F, 30D, true } };
		ChunkRadiationData radiation = ChunkRadiationData.get(level);
		for(Object[] row : table) {
			Shell shell = (Shell) row[0];
			Filling filling = (Filling) row[1];
			pad(level);
			BlockPos dirt = new BlockPos(BX - 2, BY, BZ);
			level.setBlockAndUpdate(dirt, filling == Filling.INC || filling == Filling.WP ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.DIRT.defaultBlockState());
			Pig pig = pig(level, BX + 3.5, BZ + 0.5);
			BlockPos center = new BlockPos(BX, BY, BZ);
			double radBefore = radiation.getRadiation(center), radSideBefore = radiation.getRadiation(center.offset(16, 0, 0));
			UniversalGrenadeEntity g = grenade(level, shell, filling, Fuze.S3, null, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
			g.explode();
			String name = shell + "/" + filling;
			if(pig != null) {
				float expected = (float) row[2] * (float) (1D - boxDist(pig, g.position()) / (double) row[3]);
				check(problems, cases, g.isRemoved() && Math.abs((1000F - pig.getHealth()) - expected) < 0.01F, name + " damage " + (1000F - pig.getHealth()) + " expected " + expected);
			}
			boolean destroyed = !level.getBlockState(dirt).is(Blocks.DIRT) && !level.getBlockState(dirt).is(Blocks.OAK_PLANKS);
			check(problems, cases, destroyed == (boolean) row[4], name + " block damage: " + destroyed);

			if(filling == Filling.INC || filling == Filling.WP) {
				// EntityFireLingering.setArea(6, 2), 200 ticks diesel / 600 ticks phosphorus; air next to a flammable block within 2 / 3 blocks becomes fire
				List<LegacyLingeringFireEntity> fires = level.getEntitiesOfClass(LegacyLingeringFireEntity.class, AREA);
				check(problems, cases, fires.size() == 1 && Math.abs(fires.get(0).getBbWidth() - 6F) < 1.0E-4 && Math.abs(fires.get(0).getBbHeight() - 2F) < 1.0E-4
						&& level.getBlockState(dirt.above()).is(Blocks.FIRE), name + " lingering fire " + fires.size() + ", fire block " + level.getBlockState(dirt.above()));
			}
			if(filling == Filling.CLUSTER || filling == Filling.CLUSTER_HEAVY) {
				// 30 pellets with 15 damage (x1.25 = 37 for the frag shell), 15 heavy pellets with 30 damage; heading scaled by 0.5 / 0.75 (1.25) / 0.5
				StandardAmmoType type = filling == Filling.CLUSTER ? StandardAmmoType.GRENADE_PELLETS : StandardAmmoType.GRENADE_PELLETS_HEAVY;
				int count = filling == Filling.CLUSTER_HEAVY ? 15 : shell == Shell.FRAG ? 37 : 30;
				float damage = filling == Filling.CLUSTER ? 15F : 30F;
				double vy = filling == Filling.CLUSTER ? 0.75 : 1.25;
				List<LegacyBulletEntity> pellets = level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired() && b.ammoType() == type);
				boolean ok = pellets.size() == count;
				for(LegacyBulletEntity b : pellets) {
					Vec3 m = b.getDeltaMovement();
					double len = Math.sqrt(m.x * m.x / 0.25 + m.y * m.y / (vy * vy) + m.z * m.z / 0.25);
					ok &= Math.abs(b.baseDamage() - damage) < 1.0E-4 && m.y >= -1.0E-6 && Math.abs(len - 1.5) < 1.0E-3 && b.ownerEntity() == null && Math.abs(b.getY() - (TOP + 0.55)) < 1.0E-9;
				}
				check(problems, cases, ok, name + " pellets: " + pellets.size());
			}
			if(filling == Filling.NUCLEAR || filling == Filling.NUCLEAR_DEMO) {
				// XFactoryCatapult.incrementRad(mult 1 / 1.5): 50 / (|i| + |j| + 1) * mult per chunk
				double mult = filling == Filling.NUCLEAR ? 1D : 1.5D;
				double inc = radiation.getRadiation(center) - radBefore, incSide = radiation.getRadiation(center.offset(16, 0, 0)) - radSideBefore;
				check(problems, cases, Math.abs(inc - 50D * mult) < 1.0E-3 && Math.abs(incSide - 25D * mult) < 1.0E-3, name + " radiation " + inc + " / " + incSide);
				for(int i = -2; i <= 2; i++) for(int j = -2; j <= 2; j++) radiation.clearRadiation(center.offset(i * 16, 0, j * 16));
			}
			discardAll(level);
		}

		// EXPLODE_LASER: tinyExplode(2, 5) plus one 30 damage laser beam at every other living entity within 15 blocks
		pad(level);
		Pig pig = pig(level, BX + 3.5, BZ + 0.5);
		UniversalGrenadeEntity laser = grenade(level, Shell.TECH, Filling.LASER, Fuze.S3, null, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
		laser.explode();
		List<LegacyBulletBeamEntity> beams = level.getEntitiesOfClass(LegacyBulletBeamEntity.class, AREA);
		if(pig != null) {
			float expected = 5F * (float) (1D - boxDist(pig, laser.position()) / 4D) + 30F;
			check(problems, cases, beams.size() == 1 && beams.get(0).config == UniversalGrenadeFillings.laser && beams.get(0).damage == 30F
					&& Math.abs((1000F - pig.getHealth()) - expected) < 0.01F, "TECH/LASER beams " + beams.size() + ", damage " + (1000F - pig.getHealth()) + " expected " + expected);
		}
		discardAll(level);

		// EXPLODE_SCHRAB: EntityNukeExplosionMK3.statFacFleija(..., 20) + EntityCloudFleija
		pad(level);
		boolean pendingBefore = NukeExplosionManager.hasPendingTasks(level);
		UniversalGrenadeEntity schrab = grenade(level, Shell.NUKE, Filling.SCHRAB, Fuze.S3, null, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
		schrab.explode();
		check(problems, cases, !pendingBefore && NukeExplosionManager.hasPendingTasks(level), "NUKE/SCHRAB schedules the fleija explosion");
		NukeExplosionManager.cancelAllForDiagnostics(level);
		discardAll(level);

		// EXTRA_FRAG: standardFragmentation(25), x1.5 for the frag shell (38 bullets), 10 damage
		for(Shell shell : new Shell[] { Shell.STICK, Shell.FRAG }) {
			pad(level);
			UniversalGrenadeEntity g = grenade(level, shell, Filling.POWDER, Fuze.S3, Extra.FRAG_SLEEVE, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
			g.explode();
			List<LegacyBulletEntity> frags = level.getEntitiesOfClass(LegacyBulletEntity.class, AREA, b -> !b.isRetired() && b.ammoType() == StandardAmmoType.GRENADE_FRAGMENTATION);
			boolean ok = frags.size() == (shell == Shell.FRAG ? 38 : 25);
			for(LegacyBulletEntity b : frags) ok &= Math.abs(b.baseDamage() - 10F) < 1.0E-4 && Math.abs(b.getDeltaMovement().length() - 10D) < 1.0E-3;
			check(problems, cases, ok, shell + " fragmentation sleeve: " + frags.size());
			discardAll(level);
		}

		// EXTRA_TRIPLEX: three S3 grenades of the same shell / filling without extra, motion (0.25 horizontal, 0.75 up), 120 degrees apart, flame trail
		pad(level);
		FakePlayer player = player(level);
		UniversalGrenadeEntity triplex = grenade(level, Shell.STICK, Filling.POWDER, Fuze.IMPACT, Extra.TRIPLEX, BX + 0.5, TOP + 0.5, BZ + 0.5, Vec3.ZERO);
		triplex.setThrower(player);
		triplex.explode();
		List<UniversalGrenadeEntity> children = level.getEntitiesOfClass(UniversalGrenadeEntity.class, AREA, e -> e != triplex && !e.isRemoved());
		boolean ok = children.size() == 3;
		Vec3 sum = Vec3.ZERO;
		for(UniversalGrenadeEntity child : children) {
			Vec3 m = child.getDeltaMovement();
			sum = sum.add(m.x, 0, m.z);
			ok &= child.shell() == Shell.STICK && child.filling() == Filling.POWDER && child.fuze() == Fuze.S3 && child.extra() == null && child.getTrail() == UniversalGrenadeEntity.TRAIL_TRIPLET
					&& child.getThrower() == player && m.y == 0.75D && Math.abs(Math.sqrt(m.x * m.x + m.z * m.z) - 0.25D) < 1.0E-9 && child.position().equals(triplex.position());
		}
		check(problems, cases, ok && sum.length() < 1.0E-9, "triplex children: " + children.size());
		discardAll(level);
	}

	/// ItemDisperser / EntityDisperserCanister ///
	private static void disperser(ServerLevel level, List<String> problems, int[] cases) {
		pad(level);
		FakePlayer player = player(level);
		var fluid = HbmFluids.byName("pheromone").orElse(HbmFluids.none());
		if(fluid.isNone()) {
			check(problems, cases, false, "pheromone fluid missing");
			return;
		}
		// wall 6 blocks in front of the player
		for(int dx = -2; dx <= 2; dx++) for(int dy = 0; dy <= 4; dy++) level.setBlockAndUpdate(new BlockPos(BX + dx, BY + dy, BZ + 6), Blocks.STONE.defaultBlockState());
		LegacyDisperserCanisterEntity canister = new LegacyDisperserCanisterEntity(level, player, fluid, true);
		level.addFreshEntity(canister);
		Vec3 m = canister.getDeltaMovement();
		// EntityThrowable(world, thrower): 0.16 to the right, 0.1 below the eyes, velocity 1.5 with 0.0075 spread
		check(problems, cases, Math.abs(canister.getX() - (player.getX() - 0.16D)) < 1.0E-6 && Math.abs(canister.getY() - (player.getEyeY() - 0.1D)) < 1.0E-6
				&& Math.abs(canister.getZ() - player.getZ()) < 1.0E-6 && Math.abs(m.z - 1.5D) < 0.1D && Math.abs(m.x) < 0.1D && Math.abs(m.y) < 0.1D, "canister throw: " + canister.position() + " " + m);
		int ticks = 0;
		Vec3 last = canister.position();
		while(!canister.isRemoved() && ticks < 100) { last = canister.position(); canister.tick(); ticks++; }
		// explode(): EntityMist.setArea(10, 5).setDuration(80) at the canister position (in front of the wall)
		List<LegacyMistEntity> mists = level.getEntitiesOfClass(LegacyMistEntity.class, AREA);
		check(problems, cases, canister.isRemoved() && mists.size() == 1 && mists.get(0).position().distanceTo(last) < 1.0E-6 && mists.get(0).getZ() < BZ + 6
				&& Math.abs(mists.get(0).getBbWidth() - 10F) < 1.0E-4 && Math.abs(mists.get(0).getBbHeight() - 5F) < 1.0E-4,
				"canister mist: " + mists.size() + (mists.isEmpty() ? "" : " at " + mists.get(0).position() + ", canister " + last));
		discardAll(level);
	}

	/// Helpers ///

	private static FakePlayer player(ServerLevel level) {
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		player.setPos(BX + 0.5D, TOP, BZ + 0.5D);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		player.getInventory().selected = 0;
		return player;
	}

	private static UniversalGrenadeEntity grenade(ServerLevel level, Shell shell, Filling filling, Fuze fuze, Extra extra, double x, double y, double z, Vec3 motion) {
		UniversalGrenadeEntity g = new UniversalGrenadeEntity(level, UniversalGrenadeItem.make(shell, filling, fuze, extra));
		g.setPos(x, y, z);
		g.setDeltaMovement(motion);
		level.addFreshEntity(g);
		return g;
	}

	private static Pig pig(ServerLevel level, double x, double z) {
		Pig pig = EntityType.PIG.create(level);
		if(pig == null) return null;
		pig.setNoAi(true);
		pig.setNoGravity(true);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
		pig.setHealth(1000F);
		pig.setPos(x, TOP, z);
		level.addFreshEntity(pig);
		return pig;
	}

	/** EntityProcessorCross: distance from the explosion to the nearest face of the bounding box, per axis. */
	private static double boxDist(Entity entity, Vec3 p) {
		AABB box = entity.getBoundingBox();
		double xDist = (box.minX <= p.x && box.maxX >= p.x) ? 0 : Math.min(Math.abs(box.minX - p.x), Math.abs(box.maxX - p.x));
		double yDist = (box.minY <= p.y && box.maxY >= p.y) ? 0 : Math.min(Math.abs(box.minY - p.y), Math.abs(box.maxY - p.y));
		double zDist = (box.minZ <= p.z && box.maxZ >= p.z) ? 0 : Math.min(Math.abs(box.minZ - p.z), Math.abs(box.maxZ - p.z));
		return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
	}

	/** Stone pad of 31 x 31 blocks below TOP with 12 blocks of air above it. */
	private static void pad(ServerLevel level) {
		for(BlockPos p : BlockPos.betweenClosed(BX - 15, BY - 4, BZ - 15, BX + 15, BY + 12, BZ + 15)) {
			boolean stone = p.getY() == BY - 1;
			if(stone ? !level.getBlockState(p).is(Blocks.STONE) : !level.getBlockState(p).isAir()) {
				level.setBlock(p, stone ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
			}
		}
	}

	private static void discardAll(ServerLevel level) {
		for(Entity e : level.getEntities((Entity) null, AREA, e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.remove(Entity.RemovalReason.KILLED);
	}

	private static void clearAll(ServerLevel level) {
		discardAll(level);
		for(BlockPos p : BlockPos.betweenClosed(BX - 20, BY - 12, BZ - 20, BX + 20, BY + 50, BZ + 20)) {
			if(!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
		}
		discardAll(level);
	}

	private static ItemStack item(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(Item::getDefaultInstance).orElse(new ItemStack(Items.BARRIER));
	}

	private static ItemStack craft(MinecraftServer server, ServerLevel level, List<ItemStack> parts) {
		CraftingInput input = CraftingInput.of(parts.size(), 1, parts);
		return server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input, server.registryAccess())).orElse(ItemStack.EMPTY);
	}

	private static void shaped(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String id, Item result, int count, int w, int h, List<ItemStack> grid) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(r -> r.value()).orElse(null);
		CraftingInput input = CraftingInput.of(w, h, grid);
		boolean ok = recipe instanceof ShapedRecipe s && s.matches(input, level) && s.assemble(input, server.registryAccess()).is(result) && s.assemble(input, server.registryAccess()).getCount() == count;
		check(problems, cases, ok, id + " recipe");
	}

	private static void notShaped(List<String> problems, int[] cases, MinecraftServer server, ServerLevel level, String id, int w, int h, List<ItemStack> grid) {
		var recipe = server.getRecipeManager().byKey(AtomicLegacy.id(id)).map(r -> r.value()).orElse(null);
		CraftingInput input = CraftingInput.of(w, h, grid);
		check(problems, cases, recipe instanceof ShapedRecipe s && !s.matches(input, level), id + " recipe rejects the cast plate");
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if(!ok) problems.add("parity H2: " + message);
	}
}
