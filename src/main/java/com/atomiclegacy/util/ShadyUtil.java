package com.atomiclegacy.util;

import java.util.Set;

/**
 * [parity j2] com.hbm.util.ShadyUtil: the UUIDs of the named accessories (capes, wings, axe pack, tail; see
 * render/RenderAccessoryUtility). Only the UUID table is ported here; the hash helpers belong to other systems.
 */
public final class ShadyUtil {

	private ShadyUtil() {
	}

	public static final String HbMinecraft = "192af5d7-ed0f-48d8-bd89-9d41af8524f8";
	public static final String LPkukin = "937c9804-e11f-4ad2-a5b1-42e62ac73077";
	public static final String Dafnik = "3af1c262-61c0-4b12-a4cb-424cc3a9c8c0";
	public static final String a20 = "4729b498-a81c-42fd-8acd-20d6d9f759e0";
	public static final String LordVertice = "a41df45e-13d8-4677-9398-090d3882b74f";
	public static final String Dr_Nostalgia = "e82684a7-30f1-44d2-ab37-41b342be1bbd";
	public static final String Samino2 = "87c3960a-4332-46a0-a929-ef2a488d1cda";
	public static final String Hoboy03new = "d7f29d9c-5103-4f6f-88e1-2632ff95973f";
	public static final String Dragon59MC = "dc23a304-0f84-4e2d-b47d-84c8d3bfbcdb";
	public static final String Steelcourage = "ac49720b-4a9a-4459-a26f-bee92160287a";
	public static final String ZippySqrl = "03c20435-a229-489a-a1a1-671b803f7017";
	public static final String Schrabby = "3a4a1944-5154-4e67-b80a-b6561e8630b7";
	public static final String SweatySwiggs = "5544aa30-b305-4362-b2c1-67349bb499d5";
	public static final String Drillgon = "41ebd03f-7a12-42f3-b037-0caa4d6f235b";
	public static final String Doctor17 = "e4ab1199-1c22-4f82-a516-c3238bc2d0d1";
	public static final String Doctor17PH = "4d0477d7-58da-41a9-a945-e93df8601c5a";
	public static final String ShimmeringBlaze = "061bc566-ec74-4307-9614-ac3a70d2ef38";
	public static final String FifeMiner = "37e5eb63-b9a2-4735-9007-1c77d703daa3";
	public static final String lag_add = "259785a0-20e9-4c63-9286-ac2f93ff528f";
	public static final String Pu_238 = "c95fdfd3-bea7-4255-a44b-d21bc3df95e3";
	public static final String Tankish = "609268ad-5b34-49c2-abba-a9d83229af03";
	public static final String FrizzleFrazzle = "fc4cc2ee-12e8-4097-b26a-1c6cb1b96531";
	public static final String the_NCR = "28ae585f-4431-4491-9ce8-3def6126e3c6";
	public static final String Barnaby99_x = "b04cf173-cff0-4acd-aa19-3d835224b43d";
	public static final String Ma118 = "1121cb7a-8773-491f-8e2b-221290c93d81";
	public static final String Adam29Adam29 = "bbae7bfa-0eba-40ac-a0dd-f3b715e73e61";
	public static final String Alcater = "0b399a4a-8545-45a1-be3d-ece70d7d48e9";
	public static final String ege444 = "42ee978c-442a-4cd8-95b6-29e469b6df10";
	public static final String LePeeperSauvage = "433c2bb7-018c-4d51-acfe-27f907432b5e";

	public static final Set<String> contributors = Set.of(
			"06ab7c03-55ce-43f8-9d3c-2850e3c652de", //mustang_rudolf
			"5bf069bc-5b46-4179-aafe-35c0a07dee8b", //JMF781
			"ccd9aa1c-26b9-4dde-8f37-b96f8d99de22"); //kakseao

	/**
	 * RenderAccessoryUtility#getCloakFromPlayer as data: the cape texture name (textures/models/capes/&lt;name&gt;.png,
	 * the 1.7.10 file name lowercased) for a UUID and display name, or null. {@code polaroid11} is
	 * MainRegistry.polaroidID == 11 (HbMinecraft and ShimmeringBlaze then wear the alternative capes).
	 */
	public static String capeFor(String uuid, String name, boolean polaroid11) {
		if(uuid.equals(HbMinecraft)) return polaroid11 ? "capehbm2" : "capehbm3";
		if(uuid.equals(Drillgon)) return "capedrillgon";
		if(uuid.equals(Dafnik)) return "capedafnik";
		if(uuid.equals(LPkukin)) return "capeshield";
		if(uuid.equals(LordVertice)) return "capevertice_2";
		if(uuid.equals(Dr_Nostalgia)) return "capenostalgia2";
		if(uuid.equals(Samino2)) return "capesam";
		if(uuid.equals(Hoboy03new)) return "capehoboy_mk3";
		if(uuid.equals(Dragon59MC)) return "capemaster";
		if(uuid.equals(Steelcourage)) return "capemek";
		if(uuid.equals(ZippySqrl)) return "capezippysqrl";
		if(uuid.equals(Schrabby)) return "capeschrabbyalt";
		if(uuid.equals(SweatySwiggs)) return "capesweatyswiggs";
		if(uuid.equals(Doctor17) || uuid.equals(Doctor17PH)) return "capedoctor17";
		if(uuid.equals(ShimmeringBlaze)) return polaroid11 ? "capeblaze2" : "capeblaze";
		if(uuid.equals(FifeMiner)) return "capeleftnugget";
		if(uuid.equals(lag_add)) return "caperightnugget";
		if(uuid.equals(Tankish)) return "capetankish";
		if(uuid.equals(FrizzleFrazzle)) return "capefrizzlefrazzle";
		if(uuid.equals(Ma118)) return "capevaer";
		if(uuid.equals(Adam29Adam29)) return "capeadam";
		if(uuid.equals(Alcater)) return "capealcater";
		if(uuid.equals(ege444)) return "capejame";
		if(contributors.contains(uuid)) return "capewiki";
		if(name.startsWith("Player")) return "capetest";
		return null;
	}

	/** Every cape texture name capeFor can return. */
	public static final Set<String> CAPES = Set.of("capehbm3", "capehbm2", "capedrillgon", "capedafnik", "capeshield",
			"capevertice_2", "capenostalgia2", "capesam", "capehoboy_mk3", "capemaster", "capemek", "capezippysqrl",
			"capetest", "capeschrabbyalt", "capesweatyswiggs", "capedoctor17", "capeblaze", "capeblaze2", "capewiki",
			"capeleftnugget", "caperightnugget", "capetankish", "capefrizzlefrazzle", "capevaer", "capeadam",
			"capealcater", "capejame");

	/**
	 * ModEventHandlerClient#onRenderArmorEvent (no chestplate, no invisibility): 2 / 3 = ModelArmorWings(2 / 3)
	 * (wings_bob / wings_black), 10 = ModelArmorWingsPheo (axe pack), 11 = ModelArmorTailPeep; -1 none. The four
	 * checks are independent ifs in the original; no UUID or name matches two of them.
	 */
	public static int accessoryFor(String uuid, String name) {
		if(uuid.equals(HbMinecraft) || name.equals("HbMinecraft")) return 2;
		if(uuid.equals(the_NCR) || name.equals("the_NCR")) return 3;
		if(uuid.equals(Barnaby99_x) || name.equals("pheo7")) return 10;
		if(uuid.equals(LePeeperSauvage) || name.equals("LePeeperSauvage")) return 11;
		return -1;
	}
}
