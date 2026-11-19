package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.DetonatableBlock;
import com.atomiclegacy.blocks.LandmineBlock;
import com.atomiclegacy.blocks.LegacyBarrelBlock;
import com.atomiclegacy.blocks.LegacyBombs;
import com.atomiclegacy.blocks.LegacyBombs.BombReturnCode;
import com.atomiclegacy.blocks.TimedExplosiveBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyEmpEntity;
import com.atomiclegacy.entity.TimedExplosiveEntity;
import com.atomiclegacy.explosion.LegacyDeferredDetonations;
import com.atomiclegacy.items.LegacyBombCallerItem;
import com.atomiclegacy.items.LegacyDetonatorItem;
import com.atomiclegacy.items.LegacyDropItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.BombMultiBlockEntity;
import com.atomiclegacy.tileentity.FireworksBlockEntity;
import com.atomiclegacy.tileentity.LandmineBlockEntity;
import com.atomiclegacy.tileentity.WallChargeBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Dedicated-server checks for parity batch H3 (mines, charges, det blocks, bomb blocks, detonators) against the
 * pinned 1.7.10 sources. Every case builds its own stone pad far from the origin, triggers the device the way the
 * original did (entity over the mine, redstone, timer, detonator) and compares the outcome with the values of the
 * referenced original file. Not covered here: client effects, det_nuke / crashed bomb nuclear blasts, the bomber.
 */
final class ParityH3Diagnostics {

	private ParityH3Diagnostics() {
	}

	private static final int BASE_X = 6_000;
	private static final int BASE_Y = 240;
	private static final int BASE_Z = 6_000;
	private static final int SPACING = 96;
	private static int site;
	private static final List<int[]> FORCED = new ArrayList<>();

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		ServerLevel level = server.overworld();
		site = 0;
		try {
			properties(problems, cases);
			mines(level, problems, cases);
			charges(level, problems, cases);
			detBlocks(level, problems, cases);
			tntBlocks(level, problems, cases);
			bombBlocks(level, problems, cases);
			barrelsAndMisc(level, problems, cases);
			detonators(level, problems, cases);
			recipesAndTexts(server, problems, cases);
		} catch(RuntimeException e) {
			AtomicLegacy.LOGGER.error("Parity H3 diagnostics crashed", e);
			problems.add("H3 diagnostics crashed: " + e);
		} finally {
			for(int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
			FORCED.clear();
		}
		return cases[0];
	}

	/// Block properties: ModBlocks.java:1426-1427, 1717-1749 (setHardness / setResistance arguments) ///
	private static void properties(List<String> problems, int[] cases) {
		Object[][] rows = {
				{ HbmBlocks.BLOCK_SEMTEX.get(), 2.0F, 2.0F }, { HbmBlocks.BLOCK_C4.get(), 2.0F, 2.0F },
				{ HbmBlocks.BOMB_MULTI.get(), 0.0F, 200.0F }, { HbmBlocks.FLAME_WAR.get(), 5.0F, 200.0F },
				{ HbmBlocks.FLOAT_BOMB.get(), 5.0F, 200.0F }, { HbmBlocks.EMP_BOMB.get(), 5.0F, 200.0F },
				{ HbmBlocks.THERM_ENDO.get(), 5.0F, 200.0F }, { HbmBlocks.THERM_EXO.get(), 5.0F, 200.0F },
				{ HbmBlocks.DET_CORD.get(), 0.1F, 0.0F }, { HbmBlocks.DET_CHARGE.get(), 0.1F, 0.0F },
				{ HbmBlocks.DET_NUKE.get(), 0.1F, 0.0F },
				{ HbmBlocks.RED_BARREL.get(), 0.5F, 2.5F }, { HbmBlocks.PINK_BARREL.get(), 0.5F, 2.5F },
				{ HbmBlocks.YELLOW_BARREL.get(), 0.5F, 2.5F }, { HbmBlocks.VITRIFIED_BARREL.get(), 0.5F, 2.5F },
				{ HbmBlocks.LOX_BARREL.get(), 0.5F, 2.5F }, { HbmBlocks.TAINT_BARREL.get(), 0.5F, 2.5F },
				{ HbmBlocks.FIREWORKS.get(), 0.0F, 5.0F },
				{ HbmBlocks.CHARGE_DYNAMITE.get(), 0.0F, 1.0F }, { HbmBlocks.CHARGE_MINER.get(), 0.0F, 1.0F },
				{ HbmBlocks.CHARGE_C4.get(), 0.0F, 1.0F }, { HbmBlocks.CHARGE_SEMTEX.get(), 0.0F, 1.0F },
				{ HbmBlocks.MINE_AP.get(), 1.0F, 1.0F }, { HbmBlocks.MINE_HE.get(), 1.0F, 1.0F },
				{ HbmBlocks.MINE_SHRAP.get(), 1.0F, 1.0F }, { HbmBlocks.MINE_FAT.get(), 1.0F, 1.0F },
				{ HbmBlocks.MINE_NAVAL.get(), 1.0F, 1.0F },
				{ HbmBlocks.DYNAMITE.get(), 0.0F, 0.0F }, { HbmBlocks.TNT_NTM.get(), 0.0F, 0.0F },
				{ HbmBlocks.SEMTEX.get(), 0.0F, 0.0F }, { HbmBlocks.C4.get(), 0.0F, 0.0F },
				{ HbmBlocks.FISSURE_BOMB.get(), 0.0F, 0.0F },
		};
		for(Object[] row : rows) {
			Block block = (Block) row[0];
			String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
			check(block.defaultDestroyTime() == (float) row[1], problems, cases, id + " hardness " + block.defaultDestroyTime() + " expected " + row[1]);
			check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, id + " resistance " + block.getExplosionResistance() + " expected " + row[2]); // [parity T3: expected = original r*3/5]
		}
		// Landmine.java:45-49 bounds; the naval mine has no case and stays a full cube
		check(Math.abs(LandmineBlock.LandmineType.NUCLEAR.shape().max(Direction.Axis.Y) - 6.0D / 16.0D) < 1.0E-6, problems, cases, "mine_fat height");
		// ModBlocks.java:1740-1744 Landmine(material, range, height)
		double[][] ranges = { { 1.5D, 1D }, { 2D, 5D }, { 1.5D, 1D }, { 2.5D, 1D }, { 2.5D, 1D } };
		LandmineBlock.LandmineType[] types = LandmineBlock.LandmineType.values();
		for(int i = 0; i < types.length; i++) {
			check(types[i].range() == ranges[i][0] && types[i].height() == ranges[i][1], problems, cases, types[i] + " trigger range");
		}
		// BlockDynamite/BlockTNT/BlockSemtex/BlockC4.explodeEntity: createExplosion sizes 8 / 10 / 12 / 15
		check(TimedExplosiveEntity.Kind.DYNAMITE.explosionRadius() == 8F && TimedExplosiveEntity.Kind.TNT.explosionRadius() == 10F
				&& TimedExplosiveEntity.Kind.SEMTEX.explosionRadius() == 12F && TimedExplosiveEntity.Kind.C4.explosionRadius() == 15F,
				problems, cases, "TNT-like explosion sizes");
	}

	/// Landmine.java / TileEntityLandmine.java ///
	private static void mines(ServerLevel level, List<String> problems, int[] cases) {
		// Priming: the mine arms only once nothing living is inside twice its trigger box, then fires inside the box
		BlockPos pos = pad(level, 6, 3);
		level.setBlock(pos, HbmBlocks.MINE_AP.get().defaultBlockState(), 3);
		IronGolem far = golem(level, pos.getX() + 4.0D, pos.getY(), pos.getZ() + 0.5D);
		tickMine(level, pos);
		check(!primed(level, pos), problems, cases, "mine_ap primed with a living entity inside the doubled range");
		far.discard();
		tickMine(level, pos);
		check(primed(level, pos), problems, cases, "mine_ap did not prime after the area cleared");
		far = golem(level, pos.getX() + 4.0D, pos.getY(), pos.getZ() + 0.5D);
		tickMine(level, pos);
		check(level.getBlockState(pos).is(HbmBlocks.MINE_AP.get()), problems, cases, "mine_ap fired at an entity outside range 1.5");
		far.discard();
		IronGolem target = golem(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		tickMine(level, pos);
		check(level.getBlockState(pos).isAir(), problems, cases, "mine_ap did not fire at an entity on top of it");
		// EntityProcessorCrossSmooth(0.5, MINE_AP_DAMAGE = 10): fixedDamage * (1 - 0) for an entity whose box contains the centre
		checkHealth(target, 100F - HbmConfig.MINE_AP_DAMAGE.get().floatValue(), problems, cases, "mine_ap damage");
		check(removed(level, pos, 6, 3) == 0, problems, cases, "mine_ap destroyed blocks (it has no block allocator)");
		cleanup(level, pos);

		// mine_he: ExplosionVNT 4F, BlockAllocatorStandard, EntityProcessorCrossSmooth(1, MINE_HE_DAMAGE = 35); triggered by redstone
		pos = pad(level, 6, 3);
		level.setBlock(pos, HbmBlocks.MINE_HE.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		check(level.getBlockState(pos).isAir(), problems, cases, "mine_he ignored redstone");
		checkHealth(target, 100F - HbmConfig.MINE_HE_DAMAGE.get().floatValue(), problems, cases, "mine_he damage");
		check(level.getBlockState(pos.below()).isAir(), problems, cases, "mine_he left the block below intact");
		cleanup(level, pos);

		// mine_shrap: no piercing, MINE_SHRAP_DAMAGE = 7.5, 45 + 5 shrapnel entities
		pos = pad(level, 6, 3);
		level.setBlock(pos, HbmBlocks.MINE_SHRAP.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "mine_shrap return code");
		checkHealth(target, 100F - HbmConfig.MINE_SHRAP_DAMAGE.get().floatValue(), problems, cases, "mine_shrap damage");
		int shrapnel = level.getEntities((Entity) null, area(pos, 8), e -> !(e instanceof IronGolem) && !(e instanceof ItemEntity)).size();
		check(shrapnel == 50, problems, cases, "mine_shrap spawned " + shrapnel + " shrapnel entities, expected 45 + 5");
		cleanup(level, pos);

		// mine_fat: EntityProcessorCrossSmooth(2, MINE_NUKE_DAMAGE = 100).withRangeMod(1.5F)
		pos = pad(level, 6, 3);
		level.setBlock(pos, HbmBlocks.MINE_FAT.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "mine_fat return code");
		check(!target.isAlive() || target.getHealth() <= 100F - HbmConfig.MINE_NUKE_DAMAGE.get().floatValue() + 0.01F, problems, cases,
				"mine_fat damage, health " + target.getHealth());
		cleanup(level, pos);

		// mine_naval: blast centre (x + 5, y + 5, z + 5), size 25, BlockAllocatorWater(32): liquids survive, solids do not
		pos = pad(level, 4, 2);
		level.setBlock(pos, HbmBlocks.MINE_NAVAL.get().defaultBlockState(), 3);
		BlockPos water = pos.above();
		level.setBlock(water, Blocks.WATER.defaultBlockState(), 2);
		BlockPos solid = pos.offset(5, 5, 8);
		level.setBlock(solid, Blocks.STONE.defaultBlockState(), 2);
		tickMine(level, pos);
		target = golem(level, pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D);
		tickMine(level, pos);
		check(level.getBlockState(pos).is(HbmBlocks.MINE_NAVAL.get()), problems, cases, "mine_naval fired through the water above it (TileEntityLandmine needs air above)");
		Vec3 centre = new Vec3(pos.getX() + 5, pos.getY() + 5, pos.getZ() + 5);
		float expected = HbmConfig.MINE_NAVAL_DAMAGE.get().floatValue() * (float) (1.0D - boxDistance(target, centre) / 50.0D);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "mine_naval return code");
		check(level.getFluidState(water).isSource(), problems, cases, "mine_naval removed the water above it");
		check(level.getBlockState(solid).isAir(), problems, cases, "mine_naval left a stone block 3 blocks from the blast centre");
		checkHealth(target, 100F - expected, problems, cases, "mine_naval damage");
		cleanup(level, pos);

		// Defuser: block removed and dropped, no blast (Landmine.onBlockActivated)
		pos = pad(level, 4, 2);
		level.setBlock(pos, HbmBlocks.MINE_HE.get().defaultBlockState(), 3);
		FakePlayer player = testPlayer(level);
		ItemStack defuser = new ItemStack(HbmItems.DEFUSER.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, defuser);
		level.getBlockState(pos).useItemOn(defuser, level, player, InteractionHand.MAIN_HAND, hit(pos));
		check(level.getBlockState(pos).isAir() && removed(level, pos, 4, 2) == 0, problems, cases, "defused mine_he exploded or stayed");
		check(countItems(level, pos, HbmBlocks.MINE_HE.get().asItem()) == 1, problems, cases, "defused mine_he did not drop itself");
		check(defuser.getDamageValue() == 0, problems, cases, "defusing a mine cost defuser durability (onBlockActivated path does not)");
		cleanup(level, pos);

		// Losing the supporting block detonates (Landmine.onNeighborBlockChange)
		pos = pad(level, 4, 2);
		level.setBlock(pos, HbmBlocks.MINE_HE.get().defaultBlockState(), 3);
		level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
		check(level.getBlockState(pos).isAir() && removed(level, pos, 4, 2) > 1, problems, cases, "mine_he did not detonate when its support was removed");
		cleanup(level, pos);
	}

	/// BlockChargeBase.java, BlockCharge{Dynamite,Miner,C4,Semtex}.java, TileEntityCharge.java ///
	private static void charges(ServerLevel level, List<String> problems, int[] cases) {
		FakePlayer player = testPlayer(level);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(false);

		// Timer cycle 0 -> 100 -> 200 -> 300 -> 600 -> 1200 -> 3600 -> 6000 -> 0, sneak-click arms, timer runs out
		BlockPos pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.CHARGE_DYNAMITE.get().defaultBlockState(), 3);
		WallChargeBlockEntity charge = (WallChargeBlockEntity) level.getBlockEntity(pos);
		int[] cycle = { 100, 200, 300, 600, 1200, 3600, 6000, 0, 100 };
		for(int expected : cycle) {
			level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
			check(charge.timer() == expected, problems, cases, "charge timer " + charge.timer() + " expected " + expected);
		}
		player.setShiftKeyDown(true);
		level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
		player.setShiftKeyDown(false);
		check(charge.started(), problems, cases, "charge did not arm on sneak-click");
		check(!charge.saveWithoutMetadata(level.registryAccess()).contains("Timer"), problems, cases, "charge timer is saved (TileEntityCharge has no NBT)");
		// Defuser stops an armed charge and costs one durability (ItemTooling.onItemUse)
		ItemStack defuser = new ItemStack(HbmItems.DEFUSER.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, defuser);
		level.getBlockState(pos).useItemOn(defuser, level, player, InteractionHand.MAIN_HAND, hit(pos));
		check(!charge.started() && level.getBlockState(pos).is(HbmBlocks.CHARGE_DYNAMITE.get()), problems, cases, "defuser did not stop the armed charge");
		check(defuser.getDamageValue() == 1, problems, cases, "defuser durability after disarming: " + defuser.getDamageValue());
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
		player.setShiftKeyDown(false);
		IronGolem target = golem(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		for(int i = 0; i < 99; i++) WallChargeBlockEntity.tick(level, pos, level.getBlockState(pos), charge);
		check(level.getBlockState(pos).is(HbmBlocks.CHARGE_DYNAMITE.get()), problems, cases, "charge went off before its 100 tick timer");
		WallChargeBlockEntity.tick(level, pos, level.getBlockState(pos), charge);
		check(level.getBlockState(pos).isAir(), problems, cases, "charge did not go off at timer 0");
		// charge_dynamite: ExplosionNT 4F, hurts entities, default 1 / 4 drop chance
		int destroyed = removed(level, pos, 6, 4);
		check(destroyed > 0 && target.getHealth() < 100F, problems, cases, "charge_dynamite blast: destroyed " + destroyed + ", target health " + target.getHealth());
		check(countItems(level, pos, Items.COBBLESTONE) < destroyed, problems, cases, "charge_dynamite dropped every block (chance is 1 / 4)");
		cleanup(level, pos);

		// charge_miner: NOHURT + ALLDROP
		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.CHARGE_MINER.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "charge_miner return code");
		destroyed = removed(level, pos, 6, 4);
		check(target.getHealth() == 100F, problems, cases, "charge_miner hurt an entity");
		check(destroyed > 0 && countItems(level, pos, Items.COBBLESTONE) == destroyed, problems, cases,
				"charge_miner drops " + countItems(level, pos, Items.COBBLESTONE) + " of " + destroyed + " destroyed blocks");
		cleanup(level, pos);

		// charge_c4: ExplosionVNT 15F, setNoDrop, EntityProcessorStandard
		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.CHARGE_C4.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		LegacyBombs.explode(level, pos, null);
		destroyed = removed(level, pos, 6, 4);
		check(destroyed > 15 && target.getHealth() < 100F, problems, cases, "charge_c4 blast: destroyed " + destroyed + ", target health " + target.getHealth());
		check(countItems(level, pos, Items.COBBLESTONE) == 0, problems, cases, "charge_c4 dropped blocks (setNoDrop)");
		cleanup(level, pos);

		// charge_semtex: ExplosionVNT 10F, setAllDrop + setFortune(3), no entity processor
		pos = pad(level, 6, 4);
		int ores = 0;
		for(int dx = -2; dx <= 2; dx++) for(int dz = -2; dz <= 2; dz++) {
			level.setBlock(pos.offset(dx, -1, dz), Blocks.DIAMOND_ORE.defaultBlockState(), 2);
			ores++;
		}
		level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
		ores--;
		level.setBlock(pos, HbmBlocks.CHARGE_SEMTEX.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		LegacyBombs.explode(level, pos, null);
		destroyed = removed(level, pos, 6, 4);
		check(target.getHealth() == 100F, problems, cases, "charge_semtex hurt an entity");
		int cobble = countItems(level, pos, Items.COBBLESTONE);
		int diamonds = countItems(level, pos, Items.DIAMOND);
		int oresDestroyed = ores - countBlocks(level, pos, 4, Blocks.DIAMOND_ORE);
		check(cobble == destroyed - oresDestroyed, problems, cases, "charge_semtex cobblestone " + cobble + " of " + (destroyed - oresDestroyed) + " stone blocks");
		check(oresDestroyed > 10 && diamonds > oresDestroyed, problems, cases, "charge_semtex fortune III: " + diamonds + " diamonds from " + oresDestroyed + " ores");
		cleanup(level, pos);

		// Defuser on an idle charge dismantles it without a blast; support loss and other blasts detonate it
		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.CHARGE_C4.get().defaultBlockState(), 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, defuser);
		level.getBlockState(pos).useItemOn(defuser, level, player, InteractionHand.MAIN_HAND, hit(pos));
		check(level.getBlockState(pos).isAir() && removed(level, pos, 6, 4) == 0, problems, cases, "dismantled charge_c4 exploded or stayed");
		check(countItems(level, pos, HbmBlocks.CHARGE_C4.get().asItem()) == 1, problems, cases, "dismantled charge_c4 did not drop itself");
		cleanup(level, pos);

		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.CHARGE_MINER.get().defaultBlockState(), 3);
		level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
		check(level.getBlockState(pos).isAir() && removed(level, pos, 6, 4) > 1, problems, cases, "charge_miner did not detonate when its support was removed");
		cleanup(level, pos);

		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.CHARGE_DYNAMITE.get().defaultBlockState(), 3);
		level.setBlock(pos.offset(3, 0, 0), HbmBlocks.CHARGE_DYNAMITE.get().defaultBlockState(), 3);
		LegacyBombs.explode(level, pos, null);
		check(level.getBlockState(pos.offset(3, 0, 0)).isAir(), problems, cases, "charge_dynamite was not set off by a neighbouring blast");
		cleanup(level, pos);

		// Block.onBlockExploded: immediate blast through breakBlock, then the fuse-0 primed entity blast one tick later
		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.CHARGE_MINER.get().defaultBlockState(), 3);
		LegacyDeferredDetonations.drain(level);
		level.getBlockState(pos).onBlockExploded(level, pos, new net.minecraft.world.level.Explosion(level, null, pos.getX(), pos.getY(), pos.getZ(), 1F, false,
				net.minecraft.world.level.Explosion.BlockInteraction.DESTROY));
		check(level.getBlockState(pos).isAir() && removed(level, pos, 6, 4) > 0, problems, cases, "blasted charge did not explode at once");
		check(LegacyDeferredDetonations.drain(level) == 1, problems, cases, "blasted charge did not queue its second (primed entity) blast");
		cleanup(level, pos);
	}

	/// DetCord.java, ExplosiveCharge.java, BlockDetonatable.java ///
	private static void detBlocks(ServerLevel level, List<String> problems, int[] cases) {
		BlockPos pos = pad(level, 6, 4);
		// DetCord is not an IBomb; BlockDetonatable(material, 0, 0, ...) is not flammable
		level.setBlock(pos, HbmBlocks.DET_CORD.get().defaultBlockState(), 3);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.ERROR_NO_BOMB && level.getBlockState(pos).is(HbmBlocks.DET_CORD.get()),
				problems, cases, "det_cord answered a detonator (DetCord does not implement IBomb)");
		check(!level.getBlockState(pos).isFlammable(level, pos, Direction.UP), problems, cases, "det_cord is flammable");
		// No onBlockAdded: placing next to a live signal does not fire, the next neighbour update does
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(pos.south(), HbmBlocks.DET_CORD.get().defaultBlockState(), 2);
		level.setBlock(pos.south(2), HbmBlocks.DET_CORD.get().defaultBlockState(), 2);
		level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		level.setBlock(pos, HbmBlocks.DET_CORD.get().defaultBlockState(), 3);
		check(level.getBlockState(pos).is(HbmBlocks.DET_CORD.get()), problems, cases, "det_cord fired when placed next to a signal");
		level.neighborChanged(pos, Blocks.REDSTONE_BLOCK, pos.north());
		check(level.getBlockState(pos).isAir(), problems, cases, "det_cord ignored redstone");
		// Chain: blasted cord spawns a fuse-0 primed entity (BlockDetonatable.onBlockDestroyedByExplosion, popFuse 0)
		for(int round = 0; round < 6; round++) {
			for(TimedExplosiveEntity primed : level.getEntitiesOfClass(TimedExplosiveEntity.class, area(pos, 8))) primed.tick();
		}
		check(level.getBlockState(pos.south()).isAir() && level.getBlockState(pos.south(2)).isAir(), problems, cases, "det_cord chain did not propagate");
		check(countBlocks(level, pos, 8, Blocks.FIRE) == 0, problems, cases, "det_cord blast set fire (createExplosion is smoking only)");
		cleanup(level, pos);

		// det_charge: ExplosionNT 15, resolution 64
		pos = pad(level, 8, 6);
		level.setBlock(pos, HbmBlocks.DET_CHARGE.get().defaultBlockState(), 3);
		IronGolem target = golem(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "det_charge return code");
		int destroyed = removed(level, pos, 8, 6);
		check(level.getBlockState(pos).isAir() && destroyed > 15 && target.getHealth() < 100F, problems, cases,
				"det_charge blast: destroyed " + destroyed + ", target health " + target.getHealth());
		cleanup(level, pos);

		// det_nuke / det_miner are IBomb (ExplosiveCharge, DetMiner); the nuclear blast itself is not run here
		level.setBlock(pos, HbmBlocks.DET_NUKE.get().defaultBlockState(), 2);
		check(LegacyBombs.isBomb(level, pos), problems, cases, "det_nuke is not a bomb");
		level.setBlock(pos, HbmBlocks.DET_MINER.get().defaultBlockState(), 2);
		check(LegacyBombs.isBomb(level, pos), problems, cases, "det_miner is not a bomb");
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		check(DetonatableBlock.canConnectToDetCord(level, pos, Direction.UP) == false, problems, cases, "det cord connects to air");
	}

	/// BlockTNTBase.java, BlockDetonatable.java, BlockPlasticExplosive.java ///
	private static void tntBlocks(ServerLevel level, List<String> problems, int[] cases) {
		// Redstone primes with the EntityTNTPrimedBase default fuse of 80; the blast is createExplosion(entity, x, y, z, 8F, true)
		BlockPos pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.DYNAMITE.get().defaultBlockState(), 3);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.ERROR_NO_BOMB, problems, cases, "dynamite answered a detonator (BlockTNTBase is not an IBomb)");
		level.setBlock(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		List<TimedExplosiveEntity> primed = level.getEntitiesOfClass(TimedExplosiveEntity.class, area(pos, 4));
		check(level.getBlockState(pos).isAir() && primed.size() == 1 && primed.get(0).fuse() == 80, problems, cases,
				"dynamite priming: " + primed.size() + " entities" + (primed.isEmpty() ? "" : ", fuse " + primed.get(0).fuse()));
		level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
		if(!primed.isEmpty()) {
			for(int i = 0; i < 80 && primed.get(0).isAlive(); i++) primed.get(0).tick();
			check(primed.get(0).isAlive() && removed(level, pos, 6, 4) == 0, problems, cases, "dynamite exploded before 80 ticks");
			primed.get(0).tick();
			check(!primed.get(0).isAlive() && removed(level, pos, 6, 4) > 3, problems, cases, "dynamite did not explode at fuse 0");
			check(countBlocks(level, pos, 10, Blocks.FIRE) == 0, problems, cases, "dynamite blast set fire");
		}
		cleanup(level, pos);

		// A blast pops it with fuse nextInt(20) + 10; "ignite on break" blocks still drop themselves (quantityDropped = 1)
		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.C4.get().defaultBlockState(), 3);
		BlockState c4 = level.getBlockState(pos);
		check(Block.getDrops(c4.setValue(TimedExplosiveBlock.IGNITE_ON_BREAK, true), level, pos, null).size() == 1, problems, cases,
				"c4 with ignite-on-break drops nothing");
		c4.onBlockExploded(level, pos, new net.minecraft.world.level.Explosion(level, null, pos.getX(), pos.getY(), pos.getZ(), 1F, false,
				net.minecraft.world.level.Explosion.BlockInteraction.DESTROY));
		primed = level.getEntitiesOfClass(TimedExplosiveEntity.class, area(pos, 4));
		check(primed.size() == 1 && primed.get(0).fuse() >= 10 && primed.get(0).fuse() < 30, problems, cases,
				"c4 pop fuse" + (primed.isEmpty() ? " missing" : " " + primed.get(0).fuse()));
		cleanup(level, pos);

		// block_c4: ExplosionVNT 20 makeStandard + setNoDrop; its own blast removes it and queues the second blast
		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.BLOCK_C4.get().defaultBlockState(), 3);
		LegacyDeferredDetonations.drain(level);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "block_c4 return code");
		int destroyed = removed(level, pos, 6, 4);
		check(level.getBlockState(pos).isAir() && destroyed > 20, problems, cases, "block_c4 blast destroyed " + destroyed);
		check(countItems(level, pos, null) == 0, problems, cases, "block_c4 dropped items (setNoDrop)");
		check(LegacyDeferredDetonations.drain(level) == 1, problems, cases, "block_c4 did not queue its second blast");
		cleanup(level, pos);
	}

	/// BombFlameWar.java, BombFloat.java, BombThermo.java, BombMulti.java / TileEntityBombMulti.java ///
	private static void bombBlocks(ServerLevel level, List<String> problems, int[] cases) {
		// flame_war: 150 entity-only blasts in a 51 x 11 x 51 box, then ExplosionVNT 15F noDrop with CrossSmooth(2, 200)
		BlockPos pos = pad(level, 6, 4);
		BlockPos corner = pos.offset(20, -1, 20);
		for(int dx = 0; dx < 5; dx++) for(int dz = 0; dz < 5; dz++) level.setBlock(corner.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 2);
		level.setBlock(pos, HbmBlocks.FLAME_WAR.get().defaultBlockState(), 3);
		IronGolem target = golem(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "flame_war return code");
		check(level.getBlockState(pos).isAir() && removed(level, pos, 6, 4) > 15, problems, cases, "flame_war main blast");
		check(!target.isAlive() || target.getHealth() <= 0F, problems, cases, "flame_war target survived 200 damage, health " + target.getHealth());
		int cornerLeft = 0;
		for(int dx = 0; dx < 5; dx++) for(int dz = 0; dz < 5; dz++) if(level.getBlockState(corner.offset(dx, 0, dz)).is(Blocks.STONE)) cornerLeft++;
		check(cornerLeft == 25, problems, cases, "flame_war side blasts destroyed blocks 28 blocks out (they have no block allocator): " + cornerLeft + "/25 left");
		check(countItems(level, pos, Items.COBBLESTONE) == 0, problems, cases, "flame_war dropped blocks (setNoDrop)");
		cleanup(level, pos);
		for(int dx = 0; dx < 5; dx++) for(int dz = 0; dz < 5; dz++) level.setBlock(corner.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 2);

		// float_bomb: ExplosionChaos.floater(15, 50) + move(15, 0, 50, 0), triggered by redstone
		pos = pad(level, 4, 2);
		level.setBlock(pos, HbmBlocks.FLOAT_BOMB.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 2.5D, pos.getY(), pos.getZ() + 0.5D);
		level.setBlock(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		check(level.getBlockState(pos).isAir(), problems, cases, "float_bomb ignored redstone");
		check(level.getBlockState(pos.below()).isAir() && level.getBlockState(pos.below().above(50)).is(Blocks.STONE), problems, cases,
				"float_bomb did not lift the block below it by 50");
		check(Math.abs(target.getY() - (pos.getY() + 50)) < 0.01D && target.hasCustomName(), problems, cases, "float_bomb entity lift/name, y " + target.getY());
		for(int dx = -16; dx <= 16; dx++) for(int dy = 30; dy <= 66; dy++) for(int dz = -16; dz <= 16; dz++) {
			if(!level.getBlockState(pos.offset(dx, dy, dz)).isAir()) level.setBlock(pos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		cleanup(level, pos);

		// emp_bomb: empBlast(50) + EntityEMPBlast(world, 50)
		pos = pad(level, 4, 2);
		level.setBlock(pos, HbmBlocks.EMP_BOMB.get().defaultBlockState(), 3);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "emp_bomb return code");
		check(level.getBlockState(pos).isAir() && level.getEntitiesOfClass(LegacyEmpEntity.class, area(pos, 4)).size() == 1, problems, cases,
				"emp_bomb did not spawn its EMP blast entity");
		check(removed(level, pos, 4, 2) == 0, problems, cases, "emp_bomb destroyed blocks");
		cleanup(level, pos);

		// therm_endo: freeze(15) + freezer(20) + createExplosion 5.0F; therm_exo: scorch(15) + setEntitiesOnFire(20)
		pos = pad(level, 8, 2);
		level.setBlock(pos, HbmBlocks.THERM_ENDO.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 6.5D, pos.getY(), pos.getZ() + 0.5D);
		level.setBlock(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		check(level.getBlockState(pos).isAir(), problems, cases, "therm_endo ignored redstone");
		check(level.getBlockState(pos.offset(-7, -1, 0)).is(Blocks.PACKED_ICE), problems, cases, "therm_endo did not turn stone into packed ice");
		check(target.hasEffect(MobEffects.WEAKNESS) && target.getEffect(MobEffects.WEAKNESS).getAmplifier() == 4
				&& target.getEffect(MobEffects.WEAKNESS).getDuration() == 2 * 60 * 20
				&& target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && target.hasEffect(MobEffects.DIG_SLOWDOWN), problems, cases, "therm_endo entity effects");
		check(countBlocks(level, pos, 10, Blocks.FIRE) == 0, problems, cases, "therm_endo blast set fire");
		cleanup(level, pos);

		pos = pad(level, 8, 2);
		level.setBlock(pos, HbmBlocks.THERM_EXO.get().defaultBlockState(), 3);
		target = golem(level, pos.getX() + 6.5D, pos.getY(), pos.getZ() + 0.5D);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.DETONATED, problems, cases, "therm_exo return code");
		check(level.getBlockState(pos.offset(-7, -1, 0)).is(Blocks.LAVA), problems, cases, "therm_exo did not turn stone into lava");
		check(target.getRemainingFireTicks() > 0 && target.hasEffect(MobEffects.WEAKNESS)
				&& target.getEffect(MobEffects.WEAKNESS).getDuration() == 15 * 20, problems, cases, "therm_exo entity effects");
		cleanup(level, pos);

		// bomb_multi: needs TNT in slots 0, 1, 3, 4; slots 2 / 5 pick the payload (TileEntityBombMulti.return2type)
		pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.BOMB_MULTI.get().defaultBlockState(), 3);
		BombMultiBlockEntity bomb = (BombMultiBlockEntity) level.getBlockEntity(pos);
		check(LegacyBombs.explode(level, pos, null) == BombReturnCode.ERROR_MISSING_COMPONENT && level.getBlockState(pos).is(HbmBlocks.BOMB_MULTI.get()),
				problems, cases, "empty bomb_multi return code");
		Item[] payload = { Items.GUNPOWDER, Items.TNT, item("pellet_cluster"), item("powder_fire"), item("powder_poison"), item("pellet_gas") };
		for(int i = 0; i < payload.length; i++) {
			check(BombMultiBlockEntity.typeFor(new ItemStack(payload[i])) == i + 1, problems, cases, "bomb_multi payload type of " + payload[i]);
		}
		for(int slot : new int[] { 0, 1, 3, 4 }) bomb.setItem(slot, new ItemStack(Items.TNT));
		bomb.setItem(2, new ItemStack(Items.GUNPOWDER));
		bomb.setItem(5, new ItemStack(Items.TNT));
		level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		// explosionBaseValue 8 + 1 (gunpowder) + 4 (TNT) = 13, ExplosionLarge.explode -> createExplosion(..., true)
		check(level.getBlockState(pos).isAir() && removed(level, pos, 6, 4) > 10, problems, cases, "loaded bomb_multi did not explode on redstone");
		check(countItems(level, pos, Items.TNT) == 0 && countItems(level, pos, Items.GUNPOWDER) == 0, problems, cases, "bomb_multi dropped its filling");
		check(countBlocks(level, pos, 12, Blocks.FIRE) == 0, problems, cases, "bomb_multi blast set fire");
		cleanup(level, pos);
	}

	/// RedBarrel.java, BlockFireworks.java / TileEntityFireworks.java, BlockCrashedBomb.java ///
	private static void barrelsAndMisc(ServerLevel level, List<String> problems, int[] cases) {
		// red_barrel: BlockDetonatable(material, 2, 15, 100, true, true): pop fuse nextInt(100) + 50, explodes on collision
		BlockPos pos = pad(level, 6, 4);
		level.setBlock(pos, HbmBlocks.RED_BARREL.get().defaultBlockState(), 3);
		check(((LegacyBarrelBlock) HbmBlocks.RED_BARREL.get()).detonatesWhenShot() && !((LegacyBarrelBlock) HbmBlocks.LOX_BARREL.get()).detonatesWhenShot(),
				problems, cases, "barrel detonateOnShot flags");
		check(level.getBlockState(pos).isFlammable(level, pos, Direction.UP) && !HbmBlocks.LOX_BARREL.get().defaultBlockState().isFlammable(level, pos, Direction.UP),
				problems, cases, "barrel flammability");
		level.getBlockState(pos).onBlockExploded(level, pos, new net.minecraft.world.level.Explosion(level, null, pos.getX(), pos.getY(), pos.getZ(), 1F, false,
				net.minecraft.world.level.Explosion.BlockInteraction.DESTROY));
		List<TimedExplosiveEntity> primed = level.getEntitiesOfClass(TimedExplosiveEntity.class, area(pos, 4));
		check(primed.size() == 1 && primed.get(0).fuse() >= 50 && primed.get(0).fuse() < 150, problems, cases,
				"red_barrel pop fuse" + (primed.isEmpty() ? " missing" : " " + primed.get(0).fuse()));
		if(!primed.isEmpty()) {
			for(int i = 0; i < 40 && primed.get(0).isAlive(); i++) primed.get(0).tick();
			// newExplosion(entity, x, y, z, 2.5F, true, true): flaming and smoking
			check(!primed.get(0).isAlive() && (removed(level, pos, 6, 4) > 0 || countBlocks(level, pos, 6, Blocks.FIRE) > 0), problems, cases,
					"red_barrel did not explode when it landed: alive " + primed.get(0).isAlive() + ", y " + primed.get(0).getY()
							+ ", fuse " + primed.get(0).fuse() + ", removed " + removed(level, pos, 6, 4));
		}
		cleanup(level, pos);

		// fireworks: gunpowder adds 3 charges each, a powered battery fires one rocket at once, then every 30 ticks
		pos = pad(level, 3, 1);
		level.setBlock(pos, HbmBlocks.FIREWORKS.get().defaultBlockState(), 3);
		FireworksBlockEntity fireworks = (FireworksBlockEntity) level.getBlockEntity(pos);
		check(fireworks.color() == 0xff0000 && fireworks.message().equals("NUCLEAR TECH") && fireworks.charges() == 0, problems, cases, "fireworks defaults");
		FakePlayer player = testPlayer(level);
		ItemStack gunpowder = new ItemStack(Items.GUNPOWDER, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, gunpowder);
		level.getBlockState(pos).useItemOn(gunpowder, level, player, InteractionHand.MAIN_HAND, hit(pos));
		check(fireworks.charges() == 6 && gunpowder.isEmpty(), problems, cases, "fireworks charges after 2 gunpowder: " + fireworks.charges());
		level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		int before = level.getEntities((Entity) null, area(pos, 4)).size();
		FireworksBlockEntity.tick(level, pos, level.getBlockState(pos), fireworks);
		check(fireworks.charges() == 5 && level.getEntities((Entity) null, area(pos, 4)).size() == before + 1, problems, cases, "fireworks first launch");
		for(int i = 0; i < 29; i++) FireworksBlockEntity.tick(level, pos, level.getBlockState(pos), fireworks);
		check(fireworks.charges() == 5, problems, cases, "fireworks launched again before 30 ticks");
		FireworksBlockEntity.tick(level, pos, level.getBlockState(pos), fireworks);
		check(fireworks.charges() == 4, problems, cases, "fireworks second launch after 30 ticks");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		cleanup(level, pos);

		// crashed_bomb: defuser yields the BlockCrashedBomb.onBlockActivated drops (CONVENTIONAL: 16 ball_tnt), IBomb
		pos = pad(level, 3, 1);
		level.setBlock(pos, HbmBlocks.CRASHED_BOMB.get().defaultBlockState().setValue(com.atomiclegacy.blocks.CrashedBombBlock.VARIANT, 1), 3);
		check(LegacyBombs.isBomb(level, pos), problems, cases, "crashed_bomb is not a bomb");
		ItemStack defuser = new ItemStack(HbmItems.DEFUSER.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, defuser);
		level.getBlockState(pos).useItemOn(defuser, level, player, InteractionHand.MAIN_HAND, hit(pos));
		check(level.getBlockState(pos).isAir() && countItems(level, pos, HbmItems.BALL_TNT.get()) == 16, problems, cases,
				"defused conventional dud dropped " + countItems(level, pos, HbmItems.BALL_TNT.get()) + " ball_tnt");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		cleanup(level, pos);
	}

	/// ItemDetonator.java, ItemMultiDetonator.java, ItemLaserDetonator.java, ItemDrop.java, IBomb.java ///
	private static void detonators(ServerLevel level, List<String> problems, int[] cases) {
		FakePlayer player = testPlayer(level);
		// IBomb.BombReturnCode success flags and colours
		check(BombReturnCode.DETONATED.wasSuccessful() && BombReturnCode.TRIGGERED.wasSuccessful() && BombReturnCode.LAUNCHED.wasSuccessful()
				&& !BombReturnCode.ERROR_MISSING_COMPONENT.wasSuccessful() && !BombReturnCode.ERROR_INCOMPATIBLE.wasSuccessful()
				&& !BombReturnCode.ERROR_NO_BOMB.wasSuccessful() && !BombReturnCode.UNDEFINED.wasSuccessful(), problems, cases, "BombReturnCode success flags");
		Component ok = LegacyDetonatorItem.codeMessage(BombReturnCode.DETONATED);
		Component bad = LegacyDetonatorItem.codeMessage(BombReturnCode.ERROR_NO_BOMB);
		check(ok.getStyle().getColor() != null && ok.getStyle().getColor().getValue() == ChatFormatting.YELLOW.getColor()
				&& bad.getStyle().getColor().getValue() == ChatFormatting.RED.getColor()
				&& ok.getContents() instanceof TranslatableContents key && key.getKey().equals("message.atomiclegacy.bomb.detonated"),
				problems, cases, "detonator return message colour/key");

		// detonator: sneak-use on a block links it, use detonates the linked IBomb
		BlockPos pos = pad(level, 4, 2);
		level.setBlock(pos, HbmBlocks.CHARGE_MINER.get().defaultBlockState(), 3);
		ItemStack detonator = new ItemStack(HbmItems.DETONATOR.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, detonator);
		check(LegacyDetonatorItem.location(detonator) == null, problems, cases, "fresh detonator has a position");
		player.setShiftKeyDown(true);
		detonator.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
		player.setShiftKeyDown(false);
		check(pos.equals(LegacyDetonatorItem.location(detonator)), problems, cases, "detonator did not store the clicked position");
		List<Component> tip = tooltip(detonator);
		check(tip.size() == 3 && tip.get(0).getString().equals("Shift right-click to set position,") && tip.get(1).getString().equals("right-click to detonate!")
				&& tip.get(2).getString().equals("Linked to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), problems, cases, "detonator tooltip " + strings(tip));
		detonator.getItem().use(level, player, InteractionHand.MAIN_HAND);
		check(level.getBlockState(pos).isAir(), problems, cases, "detonator did not trigger the linked charge");
		cleanup(level, pos);

		// detonator_multi: counts only successful IBomb results; TNT-like blocks and air are skipped
		pos = pad(level, 6, 2);
		level.setBlock(pos, HbmBlocks.CHARGE_MINER.get().defaultBlockState(), 3);
		level.setBlock(pos.offset(8, 0, 0), HbmBlocks.TNT_NTM.get().defaultBlockState(), 2);
		ItemStack multi = new ItemStack(HbmItems.DETONATOR_MULTI.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, multi);
		player.setShiftKeyDown(true);
		multi.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
		multi.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos.offset(8, 0, 0))));
		player.setShiftKeyDown(false);
		check(LegacyDetonatorItem.locations(multi).size() == 2 && tooltip(multi).size() == 5, problems, cases, "detonator_multi positions/tooltip");
		multi.getItem().use(level, player, InteractionHand.MAIN_HAND);
		check(level.getBlockState(pos).isAir() && level.getBlockState(pos.offset(8, 0, 0)).is(HbmBlocks.TNT_NTM.get()), problems, cases,
				"detonator_multi result (charge must go off, tnt_ntm is not an IBomb)");
		player.setShiftKeyDown(true);
		multi.getItem().use(level, player, InteractionHand.MAIN_HAND);
		player.setShiftKeyDown(false);
		check(LegacyDetonatorItem.locations(multi).isEmpty(), problems, cases, "detonator_multi sneak-use did not clear positions");
		cleanup(level, pos);

		// detonator_laser: 500 block ray from the eyes
		pos = pad(level, 4, 2);
		BlockPos aimed = pos.offset(0, 1, 3);
		level.setBlock(aimed, HbmBlocks.CHARGE_MINER.get().defaultBlockState().setValue(com.atomiclegacy.blocks.WallChargeBlock.FACING, Direction.NORTH), 2);
		ItemStack laser = new ItemStack(HbmItems.DETONATOR_LASER.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, laser);
		player.moveTo(pos.getX() + 0.5D, pos.getY() + 1.5D - player.getEyeHeight(), pos.getZ() - 1.5D, 0.0F, 0.0F);
		player.setYHeadRot(0.0F);
		player.yHeadRotO = 0.0F;
		net.minecraft.world.phys.HitResult ray = player.pick(500.0D, 1.0F, false);
		laser.getItem().use(level, player, InteractionHand.MAIN_HAND);
		check(level.getBlockState(aimed).isAir(), problems, cases, "detonator_laser did not trigger the charge it was aimed at: ray " + ray.getType()
				+ " at " + ray.getLocation() + " from " + player.getEyePosition() + " looking " + player.getViewVector(1.0F) + ", target " + aimed);
		check(tooltip(laser).size() == 1 && tooltip(laser).get(0).getString().equals("Aim & click to detonate!"), problems, cases, "detonator_laser tooltip");
		player.moveTo(0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
		cleanup(level, pos);

		// detonator_deadman: bound switch triggers its IBomb (ItemDrop.onEntityItemUpdate / ModEventHandler death hook)
		pos = pad(level, 4, 2);
		level.setBlock(pos, HbmBlocks.CHARGE_MINER.get().defaultBlockState(), 3);
		ItemStack deadman = new ItemStack(HbmItems.DETONATOR_DEADMAN.get());
		check(!LegacyDropItem.detonateDeadmanTarget(level, deadman), problems, cases, "unbound dead man's detonator reported a target");
		player.setItemInHand(InteractionHand.MAIN_HAND, deadman);
		player.setShiftKeyDown(true);
		deadman.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
		player.setShiftKeyDown(false);
		check(LegacyDropItem.detonateDeadmanTarget(level, deadman) && level.getBlockState(pos).isAir(), problems, cases, "dead man's detonator did not trigger its target");
		List<Component> deadTip = tooltip(deadman);
		check(deadTip.size() == 4 && deadTip.get(1).getString().equals("drop to detonate!")
				&& deadTip.get(2).getString().equals("Set pos to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), problems, cases, "detonator_deadman tooltip " + strings(deadTip));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		cleanup(level, pos);
	}

	/// CraftingManager.java:519, ConsumableRecipes.java:33-36, AssemblyMachineRecipes.java:691, BlockCharge*.addInformation ///
	private static void recipesAndTexts(MinecraftServer server, List<String> problems, int[] cases) {
		var recipes = server.getRecipeManager();
		check(recipes.byKey(AtomicLegacy.id("flame_war")).isPresent(), problems, cases, "flame_war recipe missing");
		int[] modes = { 0, 1, 2, 4 };
		String[] ids = { "bomb_caller", "bomb_caller_napalm", "bomb_caller_chlorine", "bomb_caller_atomic" };
		for(int i = 0; i < ids.length; i++) {
			var holder = recipes.byKey(AtomicLegacy.id(ids[i]));
			check(holder.isPresent(), problems, cases, ids[i] + " recipe missing");
			if(holder.isPresent()) {
				ItemStack result = holder.get().value().getResultItem(server.registryAccess());
				check(result.is(HbmItems.BOMB_CALLER.get()) && LegacyBombCallerItem.type(result).id() == modes[i], problems, cases, ids[i] + " result type");
			}
		}
		String[][] expected = {
				{ "charge_dynamite", "3" }, { "charge_miner", "5" }, { "charge_c4", "4" }, { "charge_semtex", "7" } };
		for(String[] row : expected) {
			List<Component> tip = tooltip(new ItemStack(item(row[0])));
			check(tip.size() == Integer.parseInt(row[1]) && tip.get(0).getString().equals("Right-click to change timer.")
					&& tip.get(1).getString().equals("Sneak-click to arm.")
					&& tip.get(2).getString().equals("Can only be disarmed and removed with defuser."), problems, cases, row[0] + " tooltip " + strings(tip));
		}
		List<Component> semtex = tooltip(new ItemStack(item("charge_semtex")));
		check(semtex.size() == 7 && semtex.get(6).getString().equals("Fortune III"), problems, cases, "charge_semtex Fortune III line");
		ItemStack caller = LegacyBombCallerItem.stackFor(LegacyBombCallerItem.Type.ATOMIC);
		check(caller.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("airstrike_mode") == 4
				&& caller.getItem().isFoil(caller), problems, cases, "bomb_caller atomic variant");
	}

	/// Helpers ///
	private static void check(boolean condition, List<String> problems, int[] cases, String message) {
		cases[0]++;
		if(!condition) problems.add("H3: " + message);
	}

	private static void checkHealth(IronGolem target, float expected, List<String> problems, int[] cases, String what) {
		check(Math.abs(target.getHealth() - expected) < 0.01F, problems, cases, what + ": health " + target.getHealth() + " expected " + expected);
	}

	/** Builds a stone pad (2r+1 square, depth layers) at the next site and returns the position on top of its centre. */
	private static BlockPos pad(ServerLevel level, int radius, int depth) {
		int index = site++;
		BlockPos pos = new BlockPos(BASE_X + (index % 8) * SPACING, BASE_Y, BASE_Z + (index / 8) * SPACING);
		for(int cx = (pos.getX() - 24) >> 4; cx <= (pos.getX() + 24) >> 4; cx++) {
			for(int cz = (pos.getZ() - 24) >> 4; cz <= (pos.getZ() + 24) >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
				level.getChunk(cx, cz);
				FORCED.add(new int[] { cx, cz });
			}
		}
		// Entities are only visible to queries once the forced chunks have been promoted to entity-ticking.
		long deadline = System.currentTimeMillis() + 20_000L;
		for(int cx = (pos.getX() - 24) >> 4; cx <= (pos.getX() + 24) >> 4; cx++) {
			for(int cz = (pos.getZ() - 24) >> 4; cz <= (pos.getZ() + 24) >> 4; cz++) {
				BlockPos probe = new BlockPos((cx << 4) + 8, BASE_Y, (cz << 4) + 8);
				while(!level.isPositionEntityTicking(probe)) {
					if(System.currentTimeMillis() > deadline) throw new IllegalStateException("test chunk " + cx + "," + cz + " never became entity-ticking");
					if(!level.getChunkSource().pollTask()) Thread.onSpinWait();
				}
			}
		}
		for(int dx = -radius - 3; dx <= radius + 3; dx++) for(int dy = -depth - 2; dy <= 6; dy++) for(int dz = -radius - 3; dz <= radius + 3; dz++) {
			boolean stone = Math.abs(dx) <= radius && Math.abs(dz) <= radius && dy < 0 && dy >= -depth;
			level.setBlock(pos.offset(dx, dy, dz), stone ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		return pos;
	}

	private static int removed(ServerLevel level, BlockPos pos, int radius, int depth) {
		int removed = 0;
		for(int dx = -radius; dx <= radius; dx++) for(int dy = -depth; dy < 0; dy++) for(int dz = -radius; dz <= radius; dz++) {
			if(level.getBlockState(pos.offset(dx, dy, dz)).isAir()) removed++;
		}
		return removed;
	}

	private static int countBlocks(ServerLevel level, BlockPos pos, int radius, Block block) {
		int count = 0;
		for(BlockPos cursor : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
			if(level.getBlockState(cursor).is(block)) count++;
		}
		return count;
	}

	private static int countItems(ServerLevel level, BlockPos pos, Item item) {
		int count = 0;
		for(ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area(pos, 40))) {
			if(item == null || entity.getItem().is(item)) count += entity.getItem().getCount();
		}
		return count;
	}

	private static void cleanup(ServerLevel level, BlockPos pos) {
		for(Entity entity : level.getEntities((Entity) null, area(pos, 80))) {
			if(!(entity instanceof net.minecraft.world.entity.player.Player)) entity.discard();
		}
		for(BlockPos cursor : BlockPos.betweenClosed(pos.offset(-12, -10, -12), pos.offset(12, 10, 12))) {
			if(!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		LegacyDeferredDetonations.drain(level);
	}

	/** A dedicated fake player so the shared one used by the other diagnostics keeps its position, rotation and items. */
	private static FakePlayer testPlayer(ServerLevel level) {
		return FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(
				java.util.UUID.nameUUIDFromBytes("atomiclegacy:parity_h3".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "[ParityH3]"));
	}

	private static AABB area(BlockPos pos, int radius) {
		return new AABB(pos).inflate(radius);
	}

	private static IronGolem golem(ServerLevel level, double x, double y, double z) {
		IronGolem golem = EntityType.IRON_GOLEM.create(level);
		golem.moveTo(x, y, z, 0.0F, 0.0F);
		golem.setNoAi(true);
		level.addFreshEntity(golem);
		return golem;
	}

	private static void tickMine(ServerLevel level, BlockPos pos) {
		if(level.getBlockEntity(pos) instanceof LandmineBlockEntity mine) LandmineBlockEntity.tick(level, pos, level.getBlockState(pos), mine);
	}

	private static boolean primed(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LandmineBlockEntity mine && mine.saveWithoutMetadata(level.registryAccess()).getBoolean("primed");
	}

	/** EntityProcessorCross box distance. */
	private static double boxDistance(Entity entity, Vec3 p) {
		AABB box = entity.getBoundingBox();
		double x = box.minX <= p.x && box.maxX >= p.x ? 0 : Math.min(Math.abs(box.minX - p.x), Math.abs(box.maxX - p.x));
		double y = box.minY <= p.y && box.maxY >= p.y ? 0 : Math.min(Math.abs(box.minY - p.y), Math.abs(box.maxY - p.y));
		double z = box.minZ <= p.z && box.maxZ >= p.z ? 0 : Math.min(Math.abs(box.minZ - p.z), Math.abs(box.maxZ - p.z));
		return Math.sqrt(x * x + y * y + z * z);
	}

	private static BlockHitResult hit(BlockPos pos) {
		return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).orElseThrow(() -> new IllegalStateException("missing item " + id));
	}

	private static List<Component> tooltip(ItemStack stack) {
		List<Component> lines = new ArrayList<>();
		stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
		return lines;
	}

	private static String strings(List<Component> lines) {
		List<String> out = new ArrayList<>();
		for(Component line : lines) out.add(line.getString());
		return out.toString();
	}
}
