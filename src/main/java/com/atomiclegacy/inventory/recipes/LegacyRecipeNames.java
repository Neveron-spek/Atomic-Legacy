package com.atomiclegacy.inventory.recipes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.registry.HbmFluids;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * [parity K1] GenericRecipe internal names, registration order (GenericRecipes#recipeOrderedList), name wrappers
 * and custom localization of the pinned 1.7.10 recipe sets. Generated from work/original (AssemblyMachineRecipes,
 * PlasmaForgeRecipes, FusionRecipes, PrecAssRecipes, PUREXRecipes, RockMillRecipes, SuperComputerRecipes).
 * Port recipes whose JSON group is not the original internal name are mapped by recipe file name (PATH_NAME).
 */
public final class LegacyRecipeNames {
    public static final String ASSEMBLY = "assembly_machine";
    public static final String PLASMA_FORGE = "plasma_forge";
    public static final String FUSION = "fusion";
    public static final String PRECISION_ASSEMBLER = "precision_assembler";
    public static final String PUREX = "purex";
    public static final String ROCK_MILL = "rock_mill";
    public static final String SUPERCOMPUTER = "supercomputer";

    private static final Map<String, List<String>> ORDER = new HashMap<>();
    private static final Map<String, Map<String, Integer>> INDEX = new HashMap<>();
    private static final Map<String, String> WRAPPER = new HashMap<>();
    private static final Map<String, String> PATH_NAME = new HashMap<>();
    /** GenericRecipe#setNamed: the internal name is the translation key. */
    private static final Set<String> NAMED = Set.of(
            "ass.analogAlt", "ass.factorioChip", "ass.atomicClockAlt", "ass.alclad", "ass.titaniumconstruction",
            "ass.nitra", "fus.dd", "fus.do", "fus.dt", "fus.tcl", "fus.h3", "fus.th4", "fus.cl", "fus.dhc", "fus.bf",
            "fus.stellar", "chem.hydrogen", "chem.hydrogencoke", "chem.oxygen", "chem.xenon", "chem.xenonoxy",
            "chem.helium3", "chem.ethanol", "chem.biogas", "chem.biofuel", "chem.reoil", "chem.gasoline",
            "chem.coallube", "chem.heavylube", "chem.tarsand", "chem.aggregate", "chem.deshcracked",
            "chem.lightconstruction", "chem.carboncarbon", "chem.meatprocessing", "chem.biosolidfuel",
            "chem.biooilsolidfuel", "chem.oilelectrodes", "chem.lubeelectrodes", "chem.birkeland",
            "chem.growinertanthrax", "chem.growmutatedanthrax", "chem.mutateanthrax");

    static {
        ORDER.put("assembly_machine", List.of(
            "ass.plateiron", "ass.plategold", "ass.platetitanium", "ass.platealu", "ass.platesteel", "ass.platelead",
            "ass.platecopper", "ass.plateschrab", "ass.platecmb", "ass.plategunmetal", "ass.plateweaponsteel",
            "ass.platesaturnite", "ass.platedura", "ass.platemixed", "ass.dalekanium", "ass.platedesh",
            "ass.platebismuth", "ass.exsteelplating", "ass.exheavyframe", "ass.excircuit", "ass.exleadplating",
            "ass.exferroplating", "ass.excomputer", "ass.bronzetubes", "ass.explastic", "ass.exgold", "ass.hazcloth",
            "ass.firecloth", "ass.filtercoal", "ass.chip", "ass.chipBismoid", "ass.chipQuantum", "ass.atomicClock",
            "ass.analogAlt", "ass.factorioChip", "ass.atomicClockAlt", "ass.flashdrive", "ass.diskdrive",
            "ass.centrifugetower", "ass.reactorcore", "ass.thermoelement", "ass.thermoelementsilicon", "ass.rtgunit",
            "ass.magnetron", "ass.titaniumdrill", "ass.entanglementkit", "ass.protoreactor", "ass.pilepabe",
            "ass.pilepobe", "ass.pilezr", "ass.pilenu", "ass.pilethorium", "ass.partlith", "ass.partberyl",
            "ass.partcoal", "ass.partcop", "ass.partplut", "ass.cmbtile", "ass.cmbbrick", "ass.sealframe",
            "ass.sealcontroller", "ass.yellowbarrel", "ass.vitrifiedbarrel", "ass.vaultdoor", "ass.blastdoor",
            "ass.firedoor", "ass.seal", "ass.slidingdoor", "ass.vehicledoor", "ass.waterdoor", "ass.qedoor",
            "ass.queslidingdoor", "ass.roundairlock", "ass.secureaccess", "ass.slidingseal", "ass.silohatch",
            "ass.silohatchlarge", "ass.cargodoor", "ass.capnuka", "ass.capquantum", "ass.capsparkle", "ass.caprad",
            "ass.capfritz", "ass.capkorl", "ass.shredder", "ass.assembler", "ass.chemplant", "ass.chemplantAlt",
            "ass.purex", "ass.precass", "ass.sal9000", "ass.centrifuge", "ass.gascent", "ass.arcfurnace",
            "ass.acidizer", "ass.electrolyzer", "ass.rtg", "ass.derrick", "ass.pumpjack", "ass.fracker",
            "ass.flarestack", "ass.refinery", "ass.crackingtower", "ass.radiolysis", "ass.coker",
            "ass.vaccumrefinery", "ass.reformer", "ass.hydrotreater", "ass.pyrooven", "ass.liquefactor",
            "ass.solidifier", "ass.compressor", "ass.compactcompressor", "ass.epress", "ass.fel", "ass.silex",
            "ass.excavator", "ass.drillsteel", "ass.drillsteeldiamond", "ass.drilldura", "ass.drillduradiamond",
            "ass.drilldesh", "ass.drilldeshdiamond", "ass.drilltc", "ass.drilltcdiamond", "ass.drillferro",
            "ass.drillferrodiamond", "ass.slopper", "ass.mininglaser", "ass.teleporter", "ass.radar",
            "ass.radarlarge", "ass.forcefield", "ass.strandcaster", "ass.assemfac", "ass.chemfac", "ass.dieselgen",
            "ass.combustiongen", "ass.pistonsetsteel", "ass.pistonsetdura", "ass.pistonsetdesh", "ass.pistonsetstar",
            "ass.turbofan", "ass.gasturbine", "ass.hephaestus", "ass.iturbine", "ass.leviturbine", "ass.radgen",
            "ass.hpcondenser", "ass.capacitorgold", "ass.capacitorniobium", "ass.capacitortantalum",
            "ass.capacitorbismuth", "ass.capacitorspark", "ass.tank", "ass.bigasstank", "ass.orbus", "ass.cyclotron",
            "ass.beamline", "ass.rfc", "ass.quadrupole", "ass.dipole", "ass.source", "ass.detector", "ass.pagold",
            "ass.panbti", "ass.pabscco", "ass.pachlorophyte", "ass.exposurechamber", "ass.pileblock", "ass.cirnox",
            "ass.rbmk", "ass.rbmkautoloader", "ass.pwrfuel", "ass.pwrcontrol", "ass.pwrchannel", "ass.pwrheatex",
            "ass.pwrheatsink", "ass.pwrreflector", "ass.pwrreflectoralt", "ass.pwrcasing", "ass.pwrcontroller",
            "ass.pwrport", "ass.pwrneutronsource", "ass.fusioncore", "ass.fusionbscco", "ass.fusionblanket",
            "ass.fusionpipes", "ass.fusionklystron", "ass.fusioncollector", "ass.fusionbreeder", "ass.fusionboiler",
            "ass.fusionmhdt", "ass.fusioncoupler", "ass.fusionplasmaforge", "ass.watzrod", "ass.watzcooler",
            "ass.watzcasing", "ass.overdrive1", "ass.overdrive2", "ass.overdrive3", "ass.chopper", "ass.ballsotron",
            "ass.clusterpellets", "ass.buckshot", "ass.minenaval", "ass.gadget", "ass.littleboy", "ass.fatman",
            "ass.ivymike", "ass.tsarbomba", "ass.ninadidnothingwrong", "ass.fleija", "ass.solinium", "ass.n2mine",
            "ass.balefirebomb", "ass.customnuke", "ass.levibomb", "ass.endobomb", "ass.exobomb",
            "ass.explosivelenses1", "ass.explosivelenses2", "ass.wiring", "ass.core1", "ass.boyshield",
            "ass.boytarget", "ass.boybullet", "ass.boypropellant", "ass.boyigniter", "ass.manigniter", "ass.mancore",
            "ass.mikecore", "ass.mikedeut", "ass.mikecooler", "ass.fleijaigniter", "ass.fleijacore",
            "ass.fleijacharge", "ass.soliniumigniter", "ass.soliniumcore", "ass.soliniumcharge", "ass.turretchekhov",
            "ass.turretfriendly", "ass.turretjeremy", "ass.turrettauon", "ass.turretrichard", "ass.turrethoward",
            "ass.maxwell", "ass.fritz", "ass.arty", "ass.himars", "ass.himarssmall", "ass.himarssmallhe",
            "ass.himarssmallwp", "ass.himarssmalltb", "ass.himarssmallnuke", "ass.himarssmalllava",
            "ass.himarslarge", "ass.himarslargetb", "ass.missileassembly", "ass.warheadhe1", "ass.warheadhe2",
            "ass.warheadhe3", "ass.warheadinc1", "ass.warheadinc2", "ass.warheadinc3", "ass.warheadcl1",
            "ass.warheadcl2", "ass.warheadcl3", "ass.warheadbb1", "ass.warheadbb2", "ass.warheadbb3",
            "ass.warheadnuke", "ass.warheadthermonuke", "ass.warheadvolcano", "ass.thrusternerva",
            "ass.stealthmissile", "ass.shuttlemissile", "ass.launchpad", "ass.launchpadsilo", "ass.mpt10kero",
            "ass.mpt10solid", "ass.mpt10xenon", "ass.mpt15kero", "ass.mpt15kerodual", "ass.mpt15kerotriple",
            "ass.mpt15solid", "ass.mpt15solid16", "ass.mpt15hydro", "ass.mpt15hydrodual", "ass.mpt15bfshort",
            "ass.mpt15bf", "ass.mpt15bflarge", "ass.mpt20kero", "ass.mpt20kerodual", "ass.mpt20kerotriple",
            "ass.mpt20solid", "ass.mpt20solidmulti", "ass.mpt20solidmultier", "ass.mpf10kero", "ass.mpf10kerolong",
            "ass.mpf10solid", "ass.mpf10solidlong", "ass.mpf10xenon", "ass.mpf1015kero", "ass.mpf1015solid",
            "ass.mpf1015hydro", "ass.mpf1015bf", "ass.mpf15kero", "ass.mpf15solid", "ass.mpf15hydro",
            "ass.mpf1520kero", "ass.mpf1520solid", "ass.mpw10he", "ass.mpw10inc", "ass.mpw10bus",
            "ass.mpw10nukesmall", "ass.mpw10nukelarge", "ass.mpw10taint", "ass.mpw10cloud", "ass.mpw15he",
            "ass.mpw15inc", "ass.mpw15nuke", "ass.mpw15n2", "ass.mpw15bf", "ass.50bmgsm", "ass.50bmgbypass",
            "chem.shellchlorine", "ass.shellphosgene", "ass.shellmustard", "ass.launchpadsoyuz",
            "ass.launchpadlambda", "ass.satdock", "ass.alclad", "ass.titaniumconstruction", "ass.soyuz",
            "ass.lander", "ass.lambdarocket", "ass.spacelab", "ass.sensorrelay", "ass.0gassembler", "ass.spysat",
            "ass.scansat", "ass.radarsat", "ass.astrominer", "ass.lunarminer", "ass.orbitallaser", "ass.deathray",
            "ass.xenrelay", "ass.relaysat", "ass.detectorsat", "ass.rayscansat", "ass.satlink", "ass.nitra",
            "ass.emptypackage", "ass.package", "ass.unpackage", "ass.digimemer"));
        ORDER.put("plasma_forge", List.of(
            "plsm.plateeuphemium", "plsm.platednt", "plsm.hde", "plsm.exsteelplating", "plsm.exheavyframe",
            "plsm.excircuit", "plsm.exleadplating", "plsm.exferroplating", "plsm.excomputer", "plsm.bronzetubes",
            "plsm.explastic", "plsm.exgold", "plsm.cmbtubes", "plsm.weldiron", "plsm.weldsteel", "plsm.weldcopper",
            "plsm.weldtitanium", "plsm.weldzirconium", "plsm.weldaluminium", "plsm.weldtcalloy", "plsm.weldcdalloy",
            "plsm.weldtungsten", "plsm.weldcmb", "plsm.weldosmiridium", "plsm.fusionvessel", "plsm.icfcell",
            "plsm.icfemitter", "plsm.icfcapacitor", "plsm.icfturbo", "plsm.icfcasing", "plsm.icfport",
            "plsm.icfcontroller", "plsm.icfscaffold", "plsm.icfvessel", "plsm.icfstructural", "plsm.icfcore",
            "plsm.icfpress", "plsm.schrabhammer", "ass.fensusan", "plsm.gerald", "plsm.dfccore", "plsm.dfcemitter",
            "plsm.dfcreceiver", "plsm.dfcinjector", "plsm.dfcstabilizer"));
        ORDER.put("fusion", List.of(
            "fus.dd", "fus.do", "fus.dt", "fus.tcl", "fus.h3", "fus.th4", "fus.cl", "fus.dhc", "fus.bf",
            "fus.stellar"));
        ORDER.put("precision_assembler", List.of(
            "precass.crystalcircuit", "precass.crystalcircuit.recycle", "precass.chip", "precass.chip.recycle",
            "precass.chip_bismoid", "precass.chip_bismoid.recycle", "precass.chip_quantum",
            "precass.chip_quantum.recycle", "precass.atomic_clock", "precass.atomic_clock.recycle",
            "precass.controller", "precass.controller.recycle", "precass.controller_advanced",
            "precass.controller_advanced.recycle", "precass.controller_quantum",
            "precass.controller_quantum.recycle", "precass.upgrade_speed_ii", "precass.upgrade_speed_ii.recycle",
            "precass.upgrade_speed_iii", "precass.upgrade_speed_iii.recycle", "precass.upgrade_effect_ii",
            "precass.upgrade_effect_ii.recycle", "precass.upgrade_effect_iii", "precass.upgrade_effect_iii.recycle",
            "precass.upgrade_power_ii", "precass.upgrade_power_ii.recycle", "precass.upgrade_power_iii",
            "precass.upgrade_power_iii.recycle", "precass.upgrade_fortune_ii", "precass.upgrade_fortune_ii.recycle",
            "precass.upgrade_fortune_iii", "precass.upgrade_fortune_iii.recycle", "precass.upgrade_ab_ii",
            "precass.upgrade_ab_ii.recycle", "precass.upgrade_ab_iii", "precass.upgrade_ab_iii.recycle",
            "precass.upgrade_overdive_i", "precass.upgrade_overdive_i.recycle", "precass.upgrade_overdive_ii",
            "precass.upgrade_overdive_ii.recycle", "precass.upgrade_overdive_iii",
            "precass.upgrade_overdive_iii.recycle", "precass.blueprints", "precass.beigeprints", "precass.rpahelmet",
            "precass.rpaplate", "precass.rpalegs", "precass.rpaboots"));
        ORDER.put("purex", List.of(
            "purex.uzh", "purex.flashgold", "purex.flashlead", "purex.pilepu239", "purex.pilergp",
            "purex.pilethorium", "purex.pilewaste", "purex.zirnoxnu", "purex.zirnoxmeu", "purex.zirnoxthmeu",
            "purex.zirnoxmox", "purex.zirnoxmep", "purex.zirnoxheu233", "purex.zirnoxheu235", "purex.zirnoxles",
            "purex.zirnoxzfbmox", "purex.platemox", "purex.platepu238be", "purex.platepu239", "purex.platera226be",
            "purex.platesa326", "purex.plateu233", "purex.plateu235", "purex.pwrmeu", "purex.pwrheu233",
            "purex.pwrheu235", "purex.pwrmen", "purex.pwrhen237", "purex.pwrmox", "purex.pwrmep", "purex.pwrhep239",
            "purex.pwrhep241", "purex.pwrmea", "purex.pwrhea242", "purex.pwrhes326", "purex.pwrhes327",
            "purex.pwrbfbam", "purex.pwrbfpu241", "purex.thoriumsalt", "purex.watzschrab", "purex.watzhes",
            "purex.watzmes", "purex.watzles", "purex.watzhen", "purex.watzmeu", "purex.watzmep", "purex.watzlead",
            "purex.watzboron", "purex.watzdu", "purex.watznaqadah", "purex.watznaqadria", "purex.icf",
            "purex.vitliquid", "purex.vitgaseous", "purex.vitsolid", "purex.schraranium", "purex.schrabzirnox",
            "purex.schrabpwr", "purex.schrabmen"));
        ORDER.put("rock_mill", List.of(
            "rock.cobble", "rock.gravel", "rock.sand", "rock.netherrack", "rock.soulsand", "rock.schist",
            "rock.basalt", "rock.hematite", "rock.bauxite", "rock.swamp", "rock.sculkgrass", "rock.lightsculkgrass",
            "rock.clay", "rock.colloid"));
        ORDER.put("supercomputer", List.of(
            "com.flightcalc_water", "com.flightcalc_pfm", "com.flightcalc_helium", "com.particlecalc_water",
            "com.particlecalc_pfm", "com.particlecalc_helium", "com.processflight_water", "com.processflight_pfm",
            "com.processflight_helium", "com.processorbit_water", "com.processorbit_pfm", "com.processorbit_helium",
            "com.copyflightcalc", "com.copyparticlecalc", "com.copyfligthdata", "com.blueprints", "com.beigeprints",
            "com.klaus"));
        ORDER.put("chemical_plant", List.of(
            "chem.hydrogen", "chem.hydrogencoke", "chem.oxygen", "chem.xenon", "chem.xenonoxy", "chem.helium3",
            "chem.co2", "chem.perfluoromethyl", "chem.cccentrifuge", "chem.ticl4", "chem.ethanol", "chem.biogas",
            "chem.biofuel", "chem.reoil", "chem.gasoline", "chem.coallube", "chem.heavylube", "chem.tarsand",
            "chem.tel", "chem.deicer", "chem.cobble", "chem.stone", "chem.obsidian", "chem.aggregate",
            "chem.concrete", "chem.concreteasbestos", "chem.ducrete", "chem.liquidconk", "chem.asphalt",
            "chem.batterylead", "chem.batterylithium", "chem.batterysodium", "chem.batteryschrabidium",
            "chem.batteryquantum", "chem.desh", "chem.deshcracked", "chem.polymer", "chem.bakelite", "chem.rubber",
            "chem.hardplastic", "chem.pvc", "chem.kevlar", "chem.lightconstruction", "chem.carboncarbon",
            "chem.meth", "chem.epearl", "chem.meatprocessing", "chem.rustysteel", "chem.biosolidfuel",
            "chem.biooilsolidfuel", "chem.oilelectrodes", "chem.lubeelectrodes", "chem.peroxide",
            "chem.sulfuricacid", "chem.nitricacid", "chem.birkeland", "chem.schrabidic", "chem.schrabidate",
            "chem.coltancleaning", "chem.coltanpain", "chem.coltancrystal", "chem.cordite", "chem.rocketfuel",
            "chem.dynamite", "chem.tnt", "chem.tatb", "chem.c4", "chem.napalm", "chem.laminate", "chem.polarized",
            "chem.yellowcake", "chem.uf6", "chem.puf6", "chem.sas3", "chem.balefire", "chem.dhc", "chem.agar",
            "chem.growinertanthrax", "chem.growmutatedanthrax", "chem.mutateanthrax", "chem.osmiridiumdeath"));
        WRAPPER.put("com.copyflightcalc", "com.copy");
        WRAPPER.put("com.copyfligthdata", "com.copy");
        WRAPPER.put("com.copyparticlecalc", "com.copy");
        WRAPPER.put("precass.atomic_clock.recycle", "precass.recycle");
        WRAPPER.put("precass.chip.recycle", "precass.recycle");
        WRAPPER.put("precass.chip_bismoid.recycle", "precass.recycle");
        WRAPPER.put("precass.chip_quantum.recycle", "precass.recycle");
        WRAPPER.put("precass.controller.recycle", "precass.recycle");
        WRAPPER.put("precass.controller_advanced.recycle", "precass.recycle");
        WRAPPER.put("precass.controller_quantum.recycle", "precass.recycle");
        WRAPPER.put("precass.crystalcircuit.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_ab_ii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_ab_iii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_effect_ii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_effect_iii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_fortune_ii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_fortune_iii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_overdive_i.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_overdive_ii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_overdive_iii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_power_ii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_power_iii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_speed_ii.recycle", "precass.recycle");
        WRAPPER.put("precass.upgrade_speed_iii.recycle", "precass.recycle");
        WRAPPER.put("purex.icf", "purex.recycle");
        WRAPPER.put("purex.pilepu239", "purex.recycle");
        WRAPPER.put("purex.pilergp", "purex.recycle");
        WRAPPER.put("purex.pilethorium", "purex.recycle");
        WRAPPER.put("purex.pilewaste", "purex.recycle");
        WRAPPER.put("purex.platemox", "purex.recycle");
        WRAPPER.put("purex.platepu238be", "purex.recycle");
        WRAPPER.put("purex.platepu239", "purex.recycle");
        WRAPPER.put("purex.platera226be", "purex.recycle");
        WRAPPER.put("purex.platesa326", "purex.recycle");
        WRAPPER.put("purex.plateu233", "purex.recycle");
        WRAPPER.put("purex.plateu235", "purex.recycle");
        WRAPPER.put("purex.pwrbfbam", "purex.recycle");
        WRAPPER.put("purex.pwrbfpu241", "purex.recycle");
        WRAPPER.put("purex.pwrhea242", "purex.recycle");
        WRAPPER.put("purex.pwrhen237", "purex.recycle");
        WRAPPER.put("purex.pwrhep239", "purex.recycle");
        WRAPPER.put("purex.pwrhep241", "purex.recycle");
        WRAPPER.put("purex.pwrhes326", "purex.recycle");
        WRAPPER.put("purex.pwrhes327", "purex.recycle");
        WRAPPER.put("purex.pwrheu233", "purex.recycle");
        WRAPPER.put("purex.pwrheu235", "purex.recycle");
        WRAPPER.put("purex.pwrmea", "purex.recycle");
        WRAPPER.put("purex.pwrmen", "purex.recycle");
        WRAPPER.put("purex.pwrmep", "purex.recycle");
        WRAPPER.put("purex.pwrmeu", "purex.recycle");
        WRAPPER.put("purex.pwrmox", "purex.recycle");
        WRAPPER.put("purex.schrabmen", "purex.schrab");
        WRAPPER.put("purex.schrabpwr", "purex.schrab");
        WRAPPER.put("purex.schrabzirnox", "purex.schrab");
        WRAPPER.put("purex.schraranium", "purex.schrab");
        WRAPPER.put("purex.watzboron", "purex.recycle");
        WRAPPER.put("purex.watzdu", "purex.recycle");
        WRAPPER.put("purex.watzhen", "purex.recycle");
        WRAPPER.put("purex.watzhes", "purex.recycle");
        WRAPPER.put("purex.watzlead", "purex.recycle");
        WRAPPER.put("purex.watzles", "purex.recycle");
        WRAPPER.put("purex.watzmep", "purex.recycle");
        WRAPPER.put("purex.watzmes", "purex.recycle");
        WRAPPER.put("purex.watzmeu", "purex.recycle");
        WRAPPER.put("purex.watznaqadah", "purex.recycle");
        WRAPPER.put("purex.watznaqadria", "purex.recycle");
        WRAPPER.put("purex.watzschrab", "purex.recycle");
        WRAPPER.put("purex.zirnoxheu233", "purex.recycle");
        WRAPPER.put("purex.zirnoxheu235", "purex.recycle");
        WRAPPER.put("purex.zirnoxles", "purex.recycle");
        WRAPPER.put("purex.zirnoxmep", "purex.recycle");
        WRAPPER.put("purex.zirnoxmeu", "purex.recycle");
        WRAPPER.put("purex.zirnoxmox", "purex.recycle");
        WRAPPER.put("purex.zirnoxnu", "purex.recycle");
        WRAPPER.put("purex.zirnoxthmeu", "purex.recycle");
        WRAPPER.put("purex.zirnoxzfbmox", "purex.recycle");
        WRAPPER.put("rock.basalt", "rock.crushing");
        WRAPPER.put("rock.bauxite", "rock.crushing");
        WRAPPER.put("rock.cobble", "rock.crushing");
        WRAPPER.put("rock.gravel", "rock.crushing");
        WRAPPER.put("rock.hematite", "rock.crushing");
        WRAPPER.put("rock.lightsculkgrass", "rock.crushing");
        WRAPPER.put("rock.netherrack", "rock.crushing");
        WRAPPER.put("rock.sand", "rock.crushing");
        WRAPPER.put("rock.schist", "rock.crushing");
        WRAPPER.put("rock.sculkgrass", "rock.crushing");
        WRAPPER.put("rock.soulsand", "rock.crushing");
        WRAPPER.put("rock.swamp", "rock.crushing");
        PATH_NAME.put("precision_assembler:atomic_clock_recycle", "precass.atomic_clock.recycle");
        PATH_NAME.put("precision_assembler:chip_bismoid_recycle", "precass.chip_bismoid.recycle");
        PATH_NAME.put("precision_assembler:chip_quantum_recycle", "precass.chip_quantum.recycle");
        PATH_NAME.put("precision_assembler:chip_recycle", "precass.chip.recycle");
        PATH_NAME.put("precision_assembler:controller_advanced_recycle", "precass.controller_advanced.recycle");
        PATH_NAME.put("precision_assembler:controller_quantum_recycle", "precass.controller_quantum.recycle");
        PATH_NAME.put("precision_assembler:controller_recycle", "precass.controller.recycle");
        PATH_NAME.put("precision_assembler:orbital_assembly_crystal_circuit_recycle", "precass.crystalcircuit.recycle");
        PATH_NAME.put("precision_assembler:orbital_assembly_crystal_circuit_recycle_expensive", "precass.crystalcircuit.recycle");
        PATH_NAME.put("precision_assembler:upgrade_ab_ii_recycle", "precass.upgrade_ab_ii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_ab_iii_recycle", "precass.upgrade_ab_iii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_effect_ii_recycle", "precass.upgrade_effect_ii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_effect_iii_recycle", "precass.upgrade_effect_iii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_fortune_ii_recycle", "precass.upgrade_fortune_ii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_fortune_iii_recycle", "precass.upgrade_fortune_iii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_overdive_i_recycle", "precass.upgrade_overdive_i.recycle");
        PATH_NAME.put("precision_assembler:upgrade_overdive_ii_recycle", "precass.upgrade_overdive_ii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_overdive_iii_recycle", "precass.upgrade_overdive_iii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_power_ii_recycle", "precass.upgrade_power_ii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_power_iii_recycle", "precass.upgrade_power_iii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_speed_ii_recycle", "precass.upgrade_speed_ii.recycle");
        PATH_NAME.put("precision_assembler:upgrade_speed_iii_recycle", "precass.upgrade_speed_iii.recycle");
        PATH_NAME.put("purex:flash_gold", "purex.flashgold");
        PATH_NAME.put("purex:flash_lead", "purex.flashlead");
        PATH_NAME.put("purex:icf", "purex.icf");
        PATH_NAME.put("purex:pile_pu239", "purex.pilepu239");
        PATH_NAME.put("purex:pile_rgp", "purex.pilergp");
        PATH_NAME.put("purex:pile_thorium", "purex.pilethorium");
        PATH_NAME.put("purex:pile_waste", "purex.pilewaste");
        PATH_NAME.put("purex:plate_mox", "purex.platemox");
        PATH_NAME.put("purex:plate_pu238be", "purex.platepu238be");
        PATH_NAME.put("purex:plate_pu239", "purex.platepu239");
        PATH_NAME.put("purex:plate_ra226be", "purex.platera226be");
        PATH_NAME.put("purex:plate_sa326", "purex.platesa326");
        PATH_NAME.put("purex:plate_u233", "purex.plateu233");
        PATH_NAME.put("purex:plate_u235", "purex.plateu235");
        PATH_NAME.put("purex:pwr_bfb_am_mix", "purex.pwrbfbam");
        PATH_NAME.put("purex:pwr_bfb_pu241", "purex.pwrbfpu241");
        PATH_NAME.put("purex:pwr_hea242", "purex.pwrhea242");
        PATH_NAME.put("purex:pwr_hen237", "purex.pwrhen237");
        PATH_NAME.put("purex:pwr_hep239", "purex.pwrhep239");
        PATH_NAME.put("purex:pwr_hep241", "purex.pwrhep241");
        PATH_NAME.put("purex:pwr_hes326", "purex.pwrhes326");
        PATH_NAME.put("purex:pwr_hes327", "purex.pwrhes327");
        PATH_NAME.put("purex:pwr_heu233", "purex.pwrheu233");
        PATH_NAME.put("purex:pwr_heu235", "purex.pwrheu235");
        PATH_NAME.put("purex:pwr_mea", "purex.pwrmea");
        PATH_NAME.put("purex:pwr_men", "purex.pwrmen");
        PATH_NAME.put("purex:pwr_mep", "purex.pwrmep");
        PATH_NAME.put("purex:pwr_meu", "purex.pwrmeu");
        PATH_NAME.put("purex:pwr_mox", "purex.pwrmox");
        PATH_NAME.put("purex:schrab_pwr_men", "purex.schrabmen");
        PATH_NAME.put("purex:schrab_pwr_mep", "purex.schrabpwr");
        PATH_NAME.put("purex:schrab_zirnox", "purex.schrabzirnox");
        PATH_NAME.put("purex:schraranium", "purex.schraranium");
        PATH_NAME.put("purex:thorium_salt", "purex.thoriumsalt");
        PATH_NAME.put("purex:uzh", "purex.uzh");
        PATH_NAME.put("purex:vitrify_gas", "purex.vitgaseous");
        PATH_NAME.put("purex:vitrify_liquid", "purex.vitliquid");
        PATH_NAME.put("purex:vitrify_solid", "purex.vitsolid");
        PATH_NAME.put("purex:watz_boron", "purex.watzboron");
        PATH_NAME.put("purex:watz_du", "purex.watzdu");
        PATH_NAME.put("purex:watz_hen", "purex.watzhen");
        PATH_NAME.put("purex:watz_hes", "purex.watzhes");
        PATH_NAME.put("purex:watz_lead", "purex.watzlead");
        PATH_NAME.put("purex:watz_les", "purex.watzles");
        PATH_NAME.put("purex:watz_mep", "purex.watzmep");
        PATH_NAME.put("purex:watz_mes", "purex.watzmes");
        PATH_NAME.put("purex:watz_meu", "purex.watzmeu");
        PATH_NAME.put("purex:watz_schrabidium", "purex.watzschrab");
        PATH_NAME.put("purex:zirnox_heu233", "purex.zirnoxheu233");
        PATH_NAME.put("purex:zirnox_heu235", "purex.zirnoxheu235");
        PATH_NAME.put("purex:zirnox_les", "purex.zirnoxles");
        PATH_NAME.put("purex:zirnox_mep", "purex.zirnoxmep");
        PATH_NAME.put("purex:zirnox_meu", "purex.zirnoxmeu");
        PATH_NAME.put("purex:zirnox_mox", "purex.zirnoxmox");
        PATH_NAME.put("purex:zirnox_natural_uranium", "purex.zirnoxnu");
        PATH_NAME.put("purex:zirnox_thorium", "purex.zirnoxthmeu");
        PATH_NAME.put("purex:zirnox_zfb_mox", "purex.zirnoxzfbmox");
        PATH_NAME.put("rock_mill:basalt", "rock.basalt");
        PATH_NAME.put("rock_mill:bauxite", "rock.bauxite");
        PATH_NAME.put("rock_mill:clay", "rock.clay");
        PATH_NAME.put("rock_mill:cobblestone", "rock.cobble");
        PATH_NAME.put("rock_mill:colloid", "rock.colloid");
        PATH_NAME.put("rock_mill:gneiss", "rock.schist");
        PATH_NAME.put("rock_mill:gravel", "rock.gravel");
        PATH_NAME.put("rock_mill:hematite", "rock.hematite");
        PATH_NAME.put("rock_mill:netherrack", "rock.netherrack");
        PATH_NAME.put("rock_mill:sand", "rock.sand");
        PATH_NAME.put("rock_mill:soul_sand", "rock.soulsand");
        PATH_NAME.put("rock_mill:swamp", "rock.swamp");
        ORDER.forEach((type, names) -> {
            Map<String, Integer> index = new HashMap<>();
            for (int i = 0; i < names.size(); i++) {
                index.putIfAbsent(names.get(i), i);
            }
            INDEX.put(type, index);
        });
    }

    private LegacyRecipeNames() {
    }

    public static List<String> order(String type) {
        return ORDER.getOrDefault(type, List.of());
    }

    public static Map<String, String> pathNames() {
        return Map.copyOf(PATH_NAME);
    }

    public static boolean isNamed(String internalName) {
        return NAMED.contains(internalName);
    }

    public static String wrapper(String internalName) {
        return WRAPPER.get(internalName);
    }

    /** GenericRecipe#getInternalName of a port recipe. */
    public static String internalName(String type, RecipeHolder<?> holder, String group) {
        String path = holder.id().getPath();
        String mapped = PATH_NAME.get(type + ":" + path.substring(path.lastIndexOf('/') + 1));
        if (mapped != null) {
            return mapped;
        }
        return group == null || group.isBlank() ? path : group;
    }

    /** GenericRecipe#getLocalizedName: custom localization, otherwise the icon name; then the name wrapper. */
    public static MutableComponent localizedName(String internalName, ItemStack icon) {
        MutableComponent name = NAMED.contains(internalName) ? Component.translatable(internalName) : icon.getHoverName().copy();
        String wrapper = WRAPPER.get(internalName);
        return wrapper == null ? name : Component.translatable(wrapper, name);
    }

    /** GenericRecipes#recipeOrderedList order; recipes unknown to 1.7.10 go last; ties by recipe id. */
    public static <T extends Recipe<?>> Comparator<RecipeHolder<T>> comparator(String type, Function<T, String> group) {
        return Comparator.<RecipeHolder<T>>comparingLong(holder -> sortKey(type, internalName(type, holder, group.apply(holder.value()))))
                .thenComparing(holder -> holder.id().toString());
    }

    public static long sortKey(String type, String name) {
        Map<String, Integer> index = INDEX.getOrDefault(type, Map.of());
        Integer position = index.get(name);
        if (position != null) {
            return (long) position << 20;
        }
        // AssemblyMachineRecipes: "ass.package"/"ass.unpackage" + fluid unlocalized name, per fluid of Fluids.getInNiceOrder
        boolean pack = name.startsWith("ass.packagehbmfluid.");
        if (pack || name.startsWith("ass.unpackagehbmfluid.")) {
            Integer base = index.get("ass.package");
            String fluid = name.substring(name.indexOf("hbmfluid.") + "hbmfluid.".length());
            List<HbmFluidDefinition> nice = HbmFluids.niceOrder();
            int fluidIndex = nice.size();
            for (int i = 0; i < nice.size(); i++) {
                if (nice.get(i).name().equalsIgnoreCase(fluid)) {
                    fluidIndex = i;
                    break;
                }
            }
            if (base != null) {
                return ((long) base << 20) + fluidIndex * 2L + (pack ? 0 : 1);
            }
        }
        return Long.MAX_VALUE;
    }
}
