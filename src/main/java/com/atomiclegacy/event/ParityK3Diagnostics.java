package com.atomiclegacy.event;

import java.util.List;

import net.minecraft.server.MinecraftServer;

/**
 * Parity batch K3 (basic processing machines, part 1) against the pinned 1.7.10 sources. Group A: presses and
 * furnaces; B: shredder, centrifuge, crystallizer; C: arc furnace, soldering station, ammo press; D: assembly
 * machine and chemical plant. Each group builds its own pads far from the origin.
 */
final class ParityK3Diagnostics {

	private ParityK3Diagnostics() {
	}

	static int run(MinecraftServer server, List<String> problems) {
		int cases = 0;
		cases += ParityK3ADiagnostics.run(server, problems);
		cases += ParityK3BDiagnostics.run(server, problems);
		cases += ParityK3CDiagnostics.run(server, problems);
		cases += ParityK3DDiagnostics.run(server, problems);
		return cases;
	}
}
