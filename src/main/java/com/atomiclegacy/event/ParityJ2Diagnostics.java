package com.atomiclegacy.event;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.atomiclegacy.blocks.LegacyBlockRenderTables;
import com.atomiclegacy.util.ShadyUtil;

import net.minecraft.server.MinecraftServer;

/**
 * [parity j2] Server-checkable part of the j2 render batch: the named cosmetics table (RenderAccessoryUtility /
 * ModEventHandlerClient of 936c60c), their textures and OBJ groups, and the wand face-rotation mapping.
 */
final class ParityJ2Diagnostics {

	private ParityJ2Diagnostics() {
	}

	/** RenderAccessoryUtility#getCloakFromPlayer of the original, UUID -> cape file (lowercased), polaroidID != 11. */
	private static final Map<String, String> CAPES = Map.ofEntries(
			Map.entry("192af5d7-ed0f-48d8-bd89-9d41af8524f8", "capehbm3"),
			Map.entry("41ebd03f-7a12-42f3-b037-0caa4d6f235b", "capedrillgon"),
			Map.entry("3af1c262-61c0-4b12-a4cb-424cc3a9c8c0", "capedafnik"),
			Map.entry("937c9804-e11f-4ad2-a5b1-42e62ac73077", "capeshield"),
			Map.entry("a41df45e-13d8-4677-9398-090d3882b74f", "capevertice_2"),
			Map.entry("e82684a7-30f1-44d2-ab37-41b342be1bbd", "capenostalgia2"),
			Map.entry("87c3960a-4332-46a0-a929-ef2a488d1cda", "capesam"),
			Map.entry("d7f29d9c-5103-4f6f-88e1-2632ff95973f", "capehoboy_mk3"),
			Map.entry("dc23a304-0f84-4e2d-b47d-84c8d3bfbcdb", "capemaster"),
			Map.entry("ac49720b-4a9a-4459-a26f-bee92160287a", "capemek"),
			Map.entry("03c20435-a229-489a-a1a1-671b803f7017", "capezippysqrl"),
			Map.entry("3a4a1944-5154-4e67-b80a-b6561e8630b7", "capeschrabbyalt"),
			Map.entry("5544aa30-b305-4362-b2c1-67349bb499d5", "capesweatyswiggs"),
			Map.entry("e4ab1199-1c22-4f82-a516-c3238bc2d0d1", "capedoctor17"),
			Map.entry("4d0477d7-58da-41a9-a945-e93df8601c5a", "capedoctor17"),
			Map.entry("061bc566-ec74-4307-9614-ac3a70d2ef38", "capeblaze"),
			Map.entry("37e5eb63-b9a2-4735-9007-1c77d703daa3", "capeleftnugget"),
			Map.entry("259785a0-20e9-4c63-9286-ac2f93ff528f", "caperightnugget"),
			Map.entry("609268ad-5b34-49c2-abba-a9d83229af03", "capetankish"),
			Map.entry("fc4cc2ee-12e8-4097-b26a-1c6cb1b96531", "capefrizzlefrazzle"),
			Map.entry("1121cb7a-8773-491f-8e2b-221290c93d81", "capevaer"),
			Map.entry("bbae7bfa-0eba-40ac-a0dd-f3b715e73e61", "capeadam"),
			Map.entry("0b399a4a-8545-45a1-be3d-ece70d7d48e9", "capealcater"),
			Map.entry("42ee978c-442a-4cd8-95b6-29e469b6df10", "capejame"),
			Map.entry("06ab7c03-55ce-43f8-9d3c-2850e3c652de", "capewiki"),
			Map.entry("5bf069bc-5b46-4179-aafe-35c0a07dee8b", "capewiki"),
			Map.entry("ccd9aa1c-26b9-4dde-8f37-b96f8d99de22", "capewiki"));

	static int run(MinecraftServer server, List<String> problems) {
		int[] cases = {0};
		String other = "00000000-0000-0000-0000-000000000000";

		for (Map.Entry<String, String> e : CAPES.entrySet()) {
			check(problems, cases, e.getValue().equals(ShadyUtil.capeFor(e.getKey(), "Someone", false)),
					"cape of " + e.getKey() + " is not " + e.getValue());
		}
		check(problems, cases, "capehbm2".equals(ShadyUtil.capeFor(ShadyUtil.HbMinecraft, "x", true)), "polaroid 11 hbm cape");
		check(problems, cases, "capeblaze2".equals(ShadyUtil.capeFor(ShadyUtil.ShimmeringBlaze, "x", true)), "polaroid 11 blaze cape");
		check(problems, cases, "capetest".equals(ShadyUtil.capeFor(other, "Player512", false)), "Player* test cape");
		check(problems, cases, ShadyUtil.capeFor(other, "Steve", false) == null, "no cape for others");
		check(problems, cases, ShadyUtil.capeFor(ShadyUtil.the_NCR, "x", false) == null, "the_NCR has no cape");
		for (String cape : ShadyUtil.CAPES) {
			resource(problems, cases, "textures/models/capes/" + cape + ".png");
		}
		check(problems, cases, ShadyUtil.CAPES.size() == 27, "27 capes expected, got " + ShadyUtil.CAPES.size());

		// onRenderArmorEvent: UUID or display name
		int[][] acc = {{2}, {3}, {10}, {11}};
		String[][] who = {{ShadyUtil.HbMinecraft, "HbMinecraft"}, {ShadyUtil.the_NCR, "the_NCR"},
				{ShadyUtil.Barnaby99_x, "pheo7"}, {ShadyUtil.LePeeperSauvage, "LePeeperSauvage"}};
		for (int i = 0; i < who.length; i++) {
			check(problems, cases, ShadyUtil.accessoryFor(who[i][0], "x") == acc[i][0], "accessory by uuid " + who[i][0]);
			check(problems, cases, ShadyUtil.accessoryFor(other, who[i][1]) == acc[i][0], "accessory by name " + who[i][1]);
		}
		check(problems, cases, ShadyUtil.accessoryFor(other, "Steve") == -1, "no accessory for others");

		for (String path : List.of("textures/armor/wings_bob.png", "textures/armor/wings_black.png", "textures/armor/wings_murk.png",
				"textures/armor/axepack.png", "textures/armor/tail_peep.png", "textures/particle/fart.png")) {
			resource(problems, cases, path);
		}
		objGroup(problems, cases, "models/obj/armor/wings_pheo.obj", "Wings");
		objGroup(problems, cases, "models/obj/armor/tail_peep.obj", "FaggyAssFuckingTailThing");
		for (String group : List.of("LeftBase", "LeftTip", "RightBase", "RightTip")) {
			objGroup(problems, cases, "models/obj/armor/murk.obj", group);
		}

		// RenderBlockSideRotation: uvRotateBottom/Top/North/South/West/East = sides 0/1/5/4/2/3, read by faces Y-/Y+/X-/X+/Z+/Z-
		int[] source = {0, 1, 3, 2, 5, 4};
		for (int face = 0; face < 6; face++) {
			check(problems, cases, LegacyBlockRenderTables.faceRotationSource(face) == source[face], "wand face source " + face);
		}
		// BlockWandJigsaw#getRotationFromSide spot values; BlockWandLoot top = meta, bottom = topToBottom(meta)
		var jig = LegacyBlockRenderTables.Wand.JIGSAW;
		check(problems, cases, LegacyBlockRenderTables.rotationFromSide(jig, 1, 2) == 3, "jigsaw meta 2 top");
		check(problems, cases, LegacyBlockRenderTables.rotationFromSide(jig, 0, 4) == 2, "jigsaw meta 4 bottom");
		check(problems, cases, LegacyBlockRenderTables.rotationFromSide(jig, 4, 1) == 3, "jigsaw meta 1 side");
		check(problems, cases, LegacyBlockRenderTables.rotationFromSide(jig, 5, 3) == 2, "jigsaw meta 3 east");
		var loot = LegacyBlockRenderTables.Wand.LOOT;
		check(problems, cases, LegacyBlockRenderTables.rotationFromSide(loot, 0, 1) == 2, "loot meta 1 bottom");
		check(problems, cases, LegacyBlockRenderTables.rotationFromSide(loot, 1, 1) == 1, "loot meta 1 top");
		return cases[0];
	}

	private static void resource(List<String> problems, int[] cases, String path) {
		check(problems, cases, ParityJ2Diagnostics.class.getResource("/assets/atomiclegacy/" + path) != null, "missing " + path);
	}

	private static void objGroup(List<String> problems, int[] cases, String path, String group) {
		boolean found = false;
		try (InputStream in = ParityJ2Diagnostics.class.getResourceAsStream("/assets/atomiclegacy/" + path)) {
			if (in != null) {
				for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
					if (line.equals("o " + group) || line.equals("g " + group)) found = true;
				}
			}
		} catch (IOException ignored) {
		}
		check(problems, cases, found, "missing group " + group + " in " + path);
	}

	private static void check(List<String> problems, int[] cases, boolean ok, String message) {
		cases[0]++;
		if (!ok) problems.add("[parity j2] " + message);
	}
}
