package com.atomiclegacy.event;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyNukeDefinition;
import com.atomiclegacy.explosion.BalefireExplosionManager;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.items.LegacyDetonatorItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.LegacyNukeBlockEntity;
import com.atomiclegacy.tileentity.NukeBoyBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Opt-in ({@code -Datomiclegacy.nukeAudit=true}) dedicated-server check that every
 * nuke block, detonated by redstone with its original payload, alters terrain as the
 * 1.7.10 source does: MK5/MK3-Fleija/Balefire remove the test pad, Solinium only strips
 * grass/plants/wood. Also checks two simultaneous detonations both make progress.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class NukePortDiagnostics {

	private static final int PAD = 3;
	private static final int TIMEOUT = 12000;

	private record Case(String name, BlockPos pos, boolean expectStoneRemoved) {}

	private static final List<Case> CASES = new ArrayList<>();
	private static final List<String> REPORT = new ArrayList<>();
	private static final List<String> PROBLEMS = new ArrayList<>();
	private static int index = -1;
	private static int ticks;
	private static boolean concurrentPhase;
	/** 0 = off, 1 = waiting for the unforced pad chunk to unload, 2 = waiting for the remote blast. */
	private static int remotePhase;
	private static final BlockPos REMOTE_POS = new BlockPos(-40_000, 120, 40_000);
	private static final int REAL_RADIUS = Integer.getInteger("atomiclegacy.nukeRealRadius", 12);
	private static BlockPos realPos;
	private static int realBefore;

	private NukePortDiagnostics() {}

	@SubscribeEvent
	public static void onStarted(ServerStartedEvent event) {
		if(!Boolean.getBoolean("atomiclegacy.nukeAudit")) return;
		String real = System.getProperty("atomiclegacy.nukeRealAt", "");
		if(!real.isBlank()) {
			// Copy of a played world: Gadget on natural terrain, chunk unloaded, remote detonator.
			String[] c = real.split(",");
			ServerLevel level = event.getServer().overworld();
			int x = Integer.parseInt(c[0].trim()), z = Integer.parseInt(c[1].trim());
			level.getChunk(x >> 4, z >> 4);
			int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			realPos = new BlockPos(x, y, z);
			realBefore = countSphere(level, realPos, REAL_RADIUS);
			LegacyNukeDefinition realDef = LegacyNukeDefinition.valueOf(System.getProperty("atomiclegacy.nukeRealKind", "GADGET"));
			arm(level, realPos, realDef);
			REPORT.add("real " + realDef + " at " + realPos.toShortString() + " solidBefore=" + realBefore);
			remotePhase = 1;
			index = 0;
			ticks = 0;
			return;
		}
		int i = 0;
		for(LegacyNukeDefinition def : LegacyNukeDefinition.values()) {
			if(def.isCustom()) continue;
			CASES.add(new Case(def.blockId(), new BlockPos(20_000 + i++ * 1_400, 120, 20_000), def != LegacyNukeDefinition.SOLINIUM));
		}
		CASES.add(new Case("nuke_boy", new BlockPos(20_000 + i * 1_400, 120, 20_000), true));
		index = 0;
		ticks = 0;
		start(event.getServer().overworld(), CASES.get(0));
	}

	@SubscribeEvent
	public static void onTick(ServerTickEvent.Post event) {
		if(index < 0) return;
		MinecraftServer server = event.getServer();
		ServerLevel level = server.overworld();
		ticks++;
		if(remotePhase != 0) {
			tickRemote(server, level);
			return;
		}
		if(concurrentPhase) {
			Case a = CASES.get(CASES.size() - 2), b = CASES.get(CASES.size() - 1);
			boolean doneA = padCleared(level, a.pos), doneB = padCleared(level, b.pos);
			if(doneA && doneB) {
				REPORT.add("concurrent " + a.name + "+" + b.name + " ticks=" + ticks);
				startRemote(level);
			} else if(ticks > TIMEOUT) {
				PROBLEMS.add("Concurrent detonation stalled: " + a.name + "=" + doneA + " " + b.name + "=" + doneB);
				finish(server);
			}
			return;
		}
		Case c = CASES.get(index);
		boolean passed = c.expectStoneRemoved ? padCleared(level, c.pos) : soliniumApplied(level, c.pos);
		if(passed || ticks > TIMEOUT) {
			int remaining = countSolid(level, c.pos);
			REPORT.add(c.name + " passed=" + passed + " ticks=" + ticks + " remainingPadBlocks=" + remaining);
			if(!passed) PROBLEMS.add("No expected terrain effect from " + c.name + " (remaining " + remaining + ")" + describeRemaining(level, c.pos));
			NukeExplosionManager.cancelAllForDiagnostics(level);
			BalefireExplosionManager.cancelAllForDiagnostics(level);
			index++;
			ticks = 0;
			if(index < CASES.size()) {
				start(level, CASES.get(index));
			} else {
				// two simultaneous Gadgets at fresh positions
				Case a = new Case("nuke_gadget#a", new BlockPos(-20_000, 120, -20_000), true);
				Case b = new Case("nuke_man#b", new BlockPos(-20_000, 120, -18_000), true);
				CASES.add(a);
				CASES.add(b);
				concurrentPhase = true;
				prepare(level, a.pos);
				prepare(level, b.pos);
				arm(level, a.pos, LegacyNukeDefinition.GADGET);
				arm(level, b.pos, LegacyNukeDefinition.MAN);
				level.setBlockAndUpdate(a.pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
				level.setBlockAndUpdate(b.pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
			}
		}
	}

	/** Detonator used from far away: the warhead's chunks are not loaded (1.7.10 World#getBlock/setBlock loaded them). */
	private static void startRemote(ServerLevel level) {
		concurrentPhase = false;
		NukeExplosionManager.cancelAllForDiagnostics(level);
		prepare(level, REMOTE_POS);
		arm(level, REMOTE_POS, LegacyNukeDefinition.GADGET);
		for(int cx = -1; cx <= 1; cx++) for(int cz = -1; cz <= 1; cz++)
			level.setChunkForced((REMOTE_POS.getX() >> 4) + cx, (REMOTE_POS.getZ() >> 4) + cz, false);
		remotePhase = 1;
		ticks = 0;
	}

	private static void tickRemote(MinecraftServer server, ServerLevel level) {
		BlockPos target = realPos != null ? realPos : REMOTE_POS;
		if(remotePhase == 1) {
			boolean loaded = level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) != null;
			if(!loaded || ticks > 600) {
				REPORT.add("remote padChunkUnloaded=" + !loaded + " afterTicks=" + ticks);
				if(loaded) PROBLEMS.add("Remote case: pad chunk never unloaded, test is not meaningful");
				if(!LegacyDetonatorItem.detonate(level, target, null)) PROBLEMS.add("Remote case: detonator did not trigger the warhead");
				remotePhase = 2;
				ticks = 0;
			}
			return;
		}
		if(realPos != null) {
			if(ticks <= 20 || ((NukeExplosionManager.hasPendingTasks(level) || BalefireExplosionManager.hasPendingTasks(level)) && ticks <= TIMEOUT * 5)) return;
			int after = countSphere(level, realPos, REAL_RADIUS);
			REPORT.add("real ticks=" + ticks + " pending=" + NukeExplosionManager.hasPendingTasks(level) + " solidAfter=" + after);
			if(after * 2 > realBefore) PROBLEMS.add("Real-terrain detonation removed less than half of the blocks within " + REAL_RADIUS);
			remotePhase = 0;
			finish(server);
			return;
		}
		if(!NukeExplosionManager.hasPendingTasks(level) || ticks > TIMEOUT) {
			int remaining = countSolid(level, REMOTE_POS);
			REPORT.add("remote nuke_gadget ticks=" + ticks + " remainingPadBlocks=" + remaining);
			if(remaining != 0) PROBLEMS.add("Remote detonation left the crater pad intact (remaining " + remaining + ")");
			remotePhase = 0;
			finish(server);
		}
	}

	private static void finish(MinecraftServer server) {
		index = -1;
		// user decision 2026-11-17: the fallout places active sellafield near ground zero, which feeds chunk radiation
		try {
			ServerLevel level = server.overworld();
			BlockPos base = CASES.get(0).pos.above(20);
			Block sellafield = BuiltInRegistries.BLOCK.get(AtomicLegacy.id("sellafield"));
			double[] percents = {2, 12, 27, 40};
			int[] expected = {5, 3, 0, -1};
			StringBuilder sb = new StringBuilder("fallout stone conversion:");
			for(int i = 0; i < percents.length; i++) {
				BlockPos p = base.east(i * 2);
				level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
				com.atomiclegacy.worldgen.NuclearFalloutTerrainEffects.diagnosticConvert(level, p, percents[i]);
				BlockState state = level.getBlockState(p);
				int got = state.is(sellafield) ? state.getValue(com.atomiclegacy.blocks.SellafieldBlock.LEVEL) : -1;
				String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
				sb.append(' ').append(percents[i]).append("%=").append(id).append(got >= 0 ? ":" + got : "");
				if(got != expected[i] || (expected[i] < 0 && !id.equals("sellafield_slaked"))) PROBLEMS.add("Fallout at " + percents[i] + "% gave " + id + " level " + got);
			}
			double before = com.atomiclegacy.radiation.HbmRadiationWorlds.getRadiation(level, base);
			level.getBlockState(base).randomTick(level, base, level.random);
			double after = com.atomiclegacy.radiation.HbmRadiationWorlds.getRadiation(level, base);
			sb.append(" chunkRad ").append(before).append(" -> ").append(after);
			REPORT.add(sb.toString());
			if(after - before < 2.9) PROBLEMS.add("Active sellafield level 5 random tick did not add 3 RAD to the chunk");
		} catch(Throwable t) {
			PROBLEMS.add("crater radiation check failed: " + t);
		}
		REPORT.add("errors=" + PROBLEMS.size());
		REPORT.addAll(PROBLEMS);
		try { Files.write(Path.of("nuke-audit.txt"), REPORT, StandardCharsets.UTF_8); } catch(Exception ignored) { }
		AtomicLegacy.LOGGER.info("HBM_NUKE_AUDIT_COMPLETE errors={} {}", PROBLEMS.size(), REPORT);
		server.halt(false);
	}

	private static void start(ServerLevel level, Case c) {
		prepare(level, c.pos);
		if(c.name.equals("nuke_boy")) {
			level.setBlockAndUpdate(c.pos, HbmBlocks.NUKE_BOY.get().defaultBlockState());
			NukeBoyBlockEntity boy = (NukeBoyBlockEntity) level.getBlockEntity(c.pos);
			boy.setItem(NukeBoyBlockEntity.SLOT_SHIELDING, new ItemStack(HbmItems.BOY_SHIELDING.get()));
			boy.setItem(NukeBoyBlockEntity.SLOT_TARGET, new ItemStack(HbmItems.BOY_TARGET.get()));
			boy.setItem(NukeBoyBlockEntity.SLOT_BULLET, new ItemStack(HbmItems.BOY_BULLET.get()));
			boy.setItem(NukeBoyBlockEntity.SLOT_PROPELLANT, new ItemStack(HbmItems.BOY_PROPELLANT.get()));
			boy.setItem(NukeBoyBlockEntity.SLOT_IGNITER, new ItemStack(HbmItems.BOY_IGNITER.get()));
		} else {
			LegacyNukeDefinition def = null;
			for(LegacyNukeDefinition d : LegacyNukeDefinition.values()) if(d.blockId().equals(c.name)) def = d;
			arm(level, c.pos, def);
		}
		level.setBlockAndUpdate(c.pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		if(!level.isEmptyBlock(c.pos)) PROBLEMS.add(c.name + " did not detonate on redstone");
	}

	private static void arm(ServerLevel level, BlockPos pos, LegacyNukeDefinition def) {
		Block block = BuiltInRegistries.BLOCK.get(AtomicLegacy.id(def.blockId()));
		level.setBlockAndUpdate(pos, block.defaultBlockState());
		LegacyNukeBlockEntity nuke = (LegacyNukeBlockEntity) level.getBlockEntity(pos);
		for(int slot = 0; slot < def.slotCount(); slot++) {
			String option = def.expected(slot).split("\\|")[0];
			String[] variant = option.split(":", 2);
			ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id(variant[0])));
			if(variant.length > 1) {
				CompoundTag tag = new CompoundTag();
				tag.putString("variant", variant[1]);
				stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			}
			nuke.setItem(slot, stack);
		}
		if(!nuke.isReady()) PROBLEMS.add(def.blockId() + " not ready with original payload");
	}

	/** Forced chunks, a stone pad with a grass top and an oak log, air above. */
	private static void prepare(ServerLevel level, BlockPos center) {
		for(int cx = -1; cx <= 1; cx++) for(int cz = -1; cz <= 1; cz++)
			level.setChunkForced((center.getX() >> 4) + cx, (center.getZ() >> 4) + cz, true);
		for(int x = -PAD - 1; x <= PAD + 1; x++) for(int z = -PAD - 1; z <= PAD + 1; z++) {
			for(int y = -PAD; y <= 6; y++) {
				BlockState state = y < -1 ? Blocks.STONE.defaultBlockState() : y == -1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
				level.setBlock(center.offset(x, y, z), state, 2);
			}
		}
		level.setBlock(center.offset(2, 0, 2), Blocks.OAK_LOG.defaultBlockState(), 2);
	}

	private static int countSphere(ServerLevel level, BlockPos center, int r) {
		int n = 0;
		for(int x = -r; x <= r; x++) for(int y = -r; y <= r; y++) for(int z = -r; z <= r; z++)
			if(x * x + y * y + z * z <= r * r && !level.getBlockState(center.offset(x, y, z)).isAir()) n++;
		return n;
	}

	private static boolean padCleared(ServerLevel level, BlockPos center) {
		return countSolid(level, center) == 0;
	}

	private static int countSolid(ServerLevel level, BlockPos center) {
		int n = 0;
		for(int x = -1; x <= 1; x++) for(int z = -1; z <= 1; z++) for(int y = -PAD; y <= -1; y++)
			if(!level.getBlockState(center.offset(x, y, z)).isAir()) n++;
		return n;
	}

	private static String describeRemaining(ServerLevel level, BlockPos center) {
		StringBuilder sb = new StringBuilder();
		for(int x = -1; x <= 1; x++) for(int z = -1; z <= 1; z++) for(int y = -PAD; y <= -1; y++) {
			BlockState state = level.getBlockState(center.offset(x, y, z));
			if(!state.isAir()) sb.append(" [").append(x).append(',').append(y).append(',').append(z).append(' ').append(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()).append(']');
		}
		return sb.toString();
	}

	private static boolean soliniumApplied(ServerLevel level, BlockPos center) {
		return level.getBlockState(center.offset(0, -1, 0)).is(Blocks.DIRT)
				&& level.getBlockState(center.offset(2, 0, 2)).isAir()
				&& level.getBlockState(center.offset(0, -2, 0)).is(Blocks.STONE);
	}
}
