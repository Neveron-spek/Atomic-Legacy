package com.atomiclegacy.explosion;

import com.atomiclegacy.registry.HbmBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/**
 * Terrain part of 1.7.10 EntityNukeExplosionMK3 with waste=false: ExplosionFleija
 * (extType 0: every block of the spheroid becomes air) or ExplosionSolinium
 * (extType 1: ExplosionNukeGeneric#solinium on every block). Both walk the same
 * square spiral of columns; the entity performed {@code speed} column steps per
 * tick, speed starting at BombConfig.blastSpeed (1024) and growing by one per tick.
 *
 * <p>Adaptations: the 1.7.10 lower limit {@code posY + y > 0} (keep the bottom
 * bedrock layer) is the level's minimum build height; a per-tick time budget may
 * spread a step batch over more ticks (same final terrain).</p>
 */
final class Mk3ColumnExplosion {

	private static final int BLAST_SPEED = 1024;
	private static final long NANOS_PER_TICK = 40_000_000L;

	private final int posX, posY, posZ;
	private final int radius2;
	private final int nlimit;
	private final boolean solinium;
	private final float coefficient = 1.0F;
	private final float coefficient2 = 1.0F;
	private int n = 1;
	private int lastposX;
	private int lastposZ;
	private int speed = com.atomiclegacy.config.HbmParityV3Config.blastSpeed(); // [parity V3: BombConfig.blastSpeed]
	private int stepsLeftThisTick;
	private boolean finished;

	Mk3ColumnExplosion(double x, double y, double z, int radius, boolean solinium) {
		// (int) this.posX — truncation, as in the original constructor call
		this.posX = (int) x;
		this.posY = (int) y;
		this.posZ = (int) z;
		this.radius2 = radius * radius;
		this.nlimit = this.radius2 * 4;
		this.solinium = solinium;
	}

	/** @return true once the spiral is complete */
	boolean tick(ServerLevel level) {
		if(finished) return true;
		if(stepsLeftThisTick <= 0) {
			speed += 1;
			stepsLeftThisTick = speed;
		}
		long deadline = System.nanoTime() + NANOS_PER_TICK;
		while(stepsLeftThisTick > 0) {
			stepsLeftThisTick--;
			if(update(level)) {
				finished = true;
				return true;
			}
			if((stepsLeftThisTick & 15) == 0 && System.nanoTime() > deadline) break;
		}
		return false;
	}

	private boolean update(ServerLevel level) {
		breakColumn(level, this.lastposX, this.lastposZ);
		int shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
		int shell2 = solinium ? Math.max(shell * 2, 1) : shell * 2;
		if(shell2 == 0) return true;
		int leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
		int element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * leg - shell + 1;
		this.lastposX = leg == 0 ? shell : leg == 1 ? -element : leg == 2 ? -shell : element;
		this.lastposZ = leg == 0 ? element : leg == 1 ? shell : leg == 2 ? -element : -shell;
		this.n++;
		return this.n > this.nlimit;
	}

	private void breakColumn(ServerLevel level, int x, int z) {
		int dist = this.radius2 - (x * x + z * z);
		if(dist <= 0) return;
		dist = (int) Math.sqrt(dist);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for(int y = (int) (dist / this.coefficient2); y > -dist / this.coefficient; y--) {
			int by = this.posY + y;
			if(by >= level.getMaxBuildHeight()) continue;
			pos.set(this.posX + x, by, this.posZ + z);
			if(solinium) {
				solinium(level, pos);
			} else if(by > level.getMinBuildHeight()) {
				BlockState state = level.getBlockState(pos);
				if(!state.isAir() && !isDecoBlockAlt(state)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
	}

	/** ExplosionNukeGeneric#solinium */
	private static void solinium(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(state.isAir()) return;
		Block b = state.getBlock();
		if(b == Blocks.GRASS_BLOCK || b == Blocks.MYCELIUM || b == HbmBlocks.WASTE_EARTH.get() || b == HbmBlocks.WASTE_MYCELIUM.get()) {
			level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
			return;
		}
		if(isOrganicMaterial(state)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	/**
	 * 1.7.10 materials cactus, coral, leaves, plants, sponge, vine, gourd, wood.
	 * Modern blocks have no Material; wood is identified by the BASS note-block
	 * instrument that replaced Material.wood, the others by their vanilla tags/blocks.
	 */
	private static boolean isOrganicMaterial(BlockState state) {
		if(state.instrument() == NoteBlockInstrument.BASS && !state.liquid()) return true;
		if(state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS)
				|| state.is(BlockTags.CROPS) || state.is(BlockTags.CORALS) || state.is(BlockTags.CORAL_BLOCKS)
				|| state.is(BlockTags.WALL_CORALS) || state.is(BlockTags.CAVE_VINES)) return true;
		Block b = state.getBlock();
		return b == Blocks.CACTUS || b == Blocks.SPONGE || b == Blocks.WET_SPONGE || b == Blocks.VINE
				|| b == Blocks.PUMPKIN || b == Blocks.CARVED_PUMPKIN || b == Blocks.JACK_O_LANTERN || b == Blocks.MELON
				|| b == Blocks.SHORT_GRASS || b == Blocks.TALL_GRASS || b == Blocks.FERN || b == Blocks.LARGE_FERN
				|| b == Blocks.DEAD_BUSH || b == Blocks.SUGAR_CANE || b == Blocks.LILY_PAD
				|| b == Blocks.BROWN_MUSHROOM || b == Blocks.RED_MUSHROOM || b == Blocks.COCOA
				|| b == Blocks.PUMPKIN_STEM || b == Blocks.MELON_STEM || b == Blocks.NETHER_WART;
	}

	/** DecoBlockAlt instances of the pinned source: statue_elb, statue_elb_g, statue_elb_w, statue_elb_f. */
	private static boolean isDecoBlockAlt(BlockState state) {
		var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
		return id.getNamespace().equals(com.atomiclegacy.AtomicLegacy.MOD_ID) && id.getPath().startsWith("statue_elb");
	}
}
