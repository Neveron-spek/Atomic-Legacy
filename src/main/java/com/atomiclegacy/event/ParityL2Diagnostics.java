package com.atomiclegacy.event;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTrait;
import com.atomiclegacy.fluid.HbmFluidTraitData;
import com.atomiclegacy.fluid.HbmFluidTraitInfo;
import com.atomiclegacy.fluid.HbmThermalConversions;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.tileentity.FluidBarrelBlockEntity;
import com.atomiclegacy.tileentity.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * [parity L2: diagnostics] Fluids and fluid storage against HbmMods 1.7.10 (936c60c): the full FluidType table
 * (scripted export of Fluids.java, work/tmp-l2/extract_fluids.py), thermal trait tables, FluidType.addInfo
 * tooltips, TileEntityBarrel behaviour and transfer speeds. Pads are built far from the origin (x, z = 12000).
 */
final class ParityL2Diagnostics {
    private static final int BASE_X = 12_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 12_000;
    private static final int SPACING = 16;
    private static int site;

    /** name|id|metaOrder|color|p|f|r|symbol|temp|simple traits|corrosive|radiation|poison|flammable|grade|combustible|canister|gastank */
    private static final String[] EXPECTED = {
            "none|0|0|888888|0|0|0|NONE|20|||||0||0||",
            "water|1|3|3333FF|0|0|0|NONE|20|LIQUID,UNSIPHONABLE||||0||0||",
            "steam|2|7|E5E5E5|3|0|0|NONE|100|GASEOUS,UNSIPHONABLE||||0||0||",
            "hotsteam|3|8|E7D6D6|4|0|0|NONE|300|GASEOUS,UNSIPHONABLE||||0||0||",
            "superhotsteam|4|9|E7B7B7|4|0|0|NONE|450|GASEOUS,UNSIPHONABLE||||0||0||",
            "ultrahotsteam|5|10|E39393|4|0|0|NONE|600|GASEOUS,UNSIPHONABLE||||0||0||",
            "coolant|6|13|D8FCFF|1|0|0|NONE|20|LIQUID||||0||0||",
            "lava|7|6|FF3300|4|0|0|NOWATER|1200|LIQUID,VISCOUS||||0||0||",
            "deuterium|8|31|0000FF|3|4|0|NONE|20|GASEOUS||||5000|HIGH|10000||0000FF/FFFFFF",
            "tritium|9|32|000099|3|4|0|RADIATION|20|GASEOUS||0.001||5000|HIGH|10000||000099/E9FFAA",
            "oil|10|39|020202|2|1|0|NONE|20|LIQUID,VISCOUS||||25000||0|424242|",
            "hotoil|11|45|300900|2|3|0|NONE|350|LIQUID,VISCOUS||||0||0||",
            "heavyoil|12|49|141312|2|1|0|NONE|20|LIQUID,VISCOUS||||55000|LOW|68700|513F39|",
            "bitumen|13|60|1F2426|2|0|0|NONE|20|LIQUID,VISCOUS||||0||0|5A5877|",
            "smear|14|61|190F01|2|1|0|NONE|20|LIQUID,VISCOUS||||82500|LOW|103000|624F3B|",
            "heatingoil|15|62|211806|2|2|0|NONE|20|LIQUID,VISCOUS||||391000|LOW|489000|694235|",
            "reclaimed|16|64|332B22|2|2|0|NONE|20|LIQUID,VISCOUS||||113000|LOW|141000|F65723|",
            "petroil|17|84|44413D|1|3|0|NONE|20|LIQUID||||130000|MEDIUM|195000|2369F6|",
            "lubricant|18|65|606060|2|1|0|NONE|20|LIQUID||||0||0|F1CC05|",
            "naphtha|19|51|595744|2|1|0|NONE|20|LIQUID,VISCOUS||||110000|MEDIUM|165000|5F6D44|",
            "diesel|20|78|F2EED5|1|2|0|NONE|20|LIQUID||||550000|HIGH|1370000|FF2C2C|",
            "lightoil|21|56|8C7451|1|2|0|NONE|20|LIQUID||||1460000|MEDIUM|2200000|B46B52|",
            "kerosene|22|82|FFA5D2|1|2|0|NONE|20|LIQUID||||2560000|AERO|3850000|FF377D|",
            "gas|23|67|FFFEED|1|4|1|NONE|20|GASEOUS||||50000|GAS|75000||FF4545/FFE97F",
            "petroleum|24|69|7CB7C9|1|4|1|NONE|20|GASEOUS||||1650000|GAS|2470000||5E7CFF/FFE97F",
            "lpg|25|71|4747EA|1|3|1|NONE|20|LIQUID||||1810000|HIGH|4530000||",
            "biogas|26|92|BFD37C|1|4|1|NONE|20|GASEOUS||||62500|GAS|78100||C8FF1F/303030",
            "biofuel|27|93|EEF274|1|2|0|NONE|20|LIQUID||||500000|HIGH|1250000|9EB623|",
            "nitan|28|97|8018AD|2|4|1|NONE|20|LIQUID||||64000000|HIGH|160000000|6B238C|",
            "uf6|29|112|D1CEBE|4|0|2|RADIATION|20|GASEOUS|15|0.2||0||0||",
            "puf6|30|113|4C4C4C|4|0|4|RADIATION|20|GASEOUS|15|0.1||0||0||",
            "sas3|31|114|4FFFFC|5|0|4|RADIATION|20|LIQUID|30|1.0||0||0||",
            "schrabidic|32|111|006B6B|5|0|5|ACID|20|LIQUID|75|1.0|true:2|0||0||",
            "amat|33|139|010101|5|0|5|ANTIMATTER|20|GASEOUS,ANTI||||0||0||",
            "aschrab|34|140|B50000|5|0|5|ANTIMATTER|20|GASEOUS,ANTI||||0||0||",
            "peroxide|35|106|FFF7AA|3|0|3|OXIDIZER|20|LIQUID|40|||0||0||",
            "watz|36|117|86653E|4|0|3|ACID|20|LIQUID,VISCOUS|60|0.1||0||0||",
            "cryogel|37|18|32FFFF|2|0|0|CROYGENIC|-170|LIQUID,VISCOUS||||0||0||",
            "hydrogen|38|30|4286F4|3|4|0|CROYGENIC|-260|LIQUID,EVAP||||5000|HIGH|10000||4286F4/FFFFFF",
            "oxygen|39|35|98BDF9|3|0|0|CROYGENIC|-100|LIQUID,EVAP||||0||0||98BDF9/FFFFFF",
            "xenon|40|36|BA45E8|0|0|0|ASPHYXIANT|20|GASEOUS||||0||0||8C21FF/303030",
            "balefire|41|99|28E02E|4|4|3|RADIATION|1500|LIQUID,VISCOUS|50|||256000000|HIGH|640000000||",
            "mercury|42|38|808080|2|0|0|NONE|20|LIQUID|||false:2|0||0||",
            "pain|43|115|938541|2|0|1|ACID|300|LIQUID,VISCOUS|30||true:2|0||0||",
            "wastefluid|44|141|544400|2|0|1|RADIATION|20|LIQUID,VISCOUS,NOCON||0.5||0||0||",
            "wastegas|45|142|B8B8B8|2|0|1|RADIATION|20|GASEOUS,NOCON||0.5||0||0||",
            "gasoline|46|86|445772|1|2|0|NONE|20|LIQUID||||605000|HIGH|1510000|2F7747|",
            "coalgas|47|88|445772|1|2|0|NONE|20|LIQUID||||6350000|MEDIUM|9520000|2E155F|",
            "spentsteam|48|11|445772|2|0|0|NONE|20|GASEOUS,NOCON||||0||0||",
            "fracksol|49|133|798A6B|1|3|3|ACID|20|LIQUID,VISCOUS|15||false:0|0||0|4F887F|",
            "plasma_dt|50|146|F7AFDE|0|4|0|RADIATION|3250|PLASMA,NOCON,NOID||||0||0||",
            "plasma_hd|51|147|F0ADF4|0|4|0|RADIATION|2500|PLASMA,NOCON,NOID||||0||0||",
            "plasma_ht|52|148|D1ABF2|0|4|0|RADIATION|3000|PLASMA,NOCON,NOID||||0||0||",
            "plasma_xm|53|150|C6A5FF|0|4|1|RADIATION|4250|PLASMA,NOCON,NOID||||0||0||",
            "plasma_bf|54|151|A7F1A3|4|5|4|ANTIMATTER|8500|PLASMA,NOCON,NOID||||0||0||",
            "carbondioxide|55|12|404040|3|0|0|ASPHYXIANT|20|GASEOUS||||0||0||",
            "plasma_dh3|56|149|FF83AA|0|4|0|RADIATION|3480|PLASMA,NOCON,NOID||||0||0||",
            "helium3|57|33|FCF0C4|0|0|0|ASPHYXIANT|20|GASEOUS||||0||0||FD631F/FFFFFF",
            "death|58|116|717A88|2|0|1|ACID|300|LIQUID,VISCOUS,LEADCON|80||true:4|0||0||",
            "ethanol|59|94|E0FFFF|2|3|0|NONE|20|LIQUID||||275000|HIGH|687000|EAFFF3|",
            "heavywater|60|4|00A0B0|1|0|0|NONE|20|LIQUID||||0||0||",
            "crackoil|61|41|020202|2|1|0|NONE|20|LIQUID,VISCOUS||||31200||0|424242|",
            "coaloil|62|43|020202|2|1|0|NONE|20|LIQUID,VISCOUS||||1100000||0|424242|",
            "hotcrackoil|63|47|300900|2|3|0|NONE|350|LIQUID,VISCOUS||||0||0||",
            "naphtha_crack|64|53|595744|2|1|0|NONE|20|LIQUID,VISCOUS||||85900|MEDIUM|128000|5F6D44|",
            "lightoil_crack|65|58|8C7451|1|2|0|NONE|20|LIQUID||||916000|MEDIUM|1370000|B46B52|",
            "diesel_crack|66|80|F2EED5|1|2|0|NONE|20|LIQUID||||515000|HIGH|1280000|FF2C2C|",
            "aromatics|67|74|68A09A|1|4|1|NONE|20|LIQUID,VISCOUS||||458000||0||68A09A/EDCF27",
            "unsaturateds|68|75|628FAE|1|4|1|NONE|20|GASEOUS||||3660000||0||628FAE/EDCF27",
            "salient|69|100|457F2D|0|0|0|NONE|20|LIQUID,VISCOUS,DELICIOUS||||0||0||",
            "xpjuice|70|143|BBFF09|0|0|0|NONE|20|LIQUID,VISCOUS||||0||0||",
            "enderjuice|71|144|127766|0|0|0|NONE|20|LIQUID||||0||0||",
            "petroil_leaded|72|85|44413D|1|3|0|NONE|20|LIQUID||||195000|MEDIUM|293000|2331F6|",
            "gasoline_leaded|73|87|445772|1|2|0|NONE|20|LIQUID||||907000|HIGH|2260000|2F775A|",
            "coalgas_leaded|74|89|445772|1|2|0|NONE|20|LIQUID||||9520000|MEDIUM|14200000|1E155F|",
            "sulfuric_acid|75|107|B0AA64|3|0|2|ACID|20|LIQUID|50|||0||0||",
            "coolant_hot|76|14|99525E|1|0|0|NONE|600|LIQUID||||0||0||",
            "mug|77|19|4B2D28|0|0|0|NONE|20|LIQUID,DELICIOUS||||0||0||",
            "mug_hot|78|20|6B2A20|0|0|0|NONE|500|LIQUID,DELICIOUS||||0||0||",
            "woodoil|79|91|847D54|2|2|0|NONE|20|LIQUID,VISCOUS||||110000||0|BF7E4F|",
            "coalcreosote|80|90|51694F|3|2|0|NONE|20|LIQUID,VISCOUS||||250000||0|285A3F|",
            "seedslurry|81|101|7CC35E|0|0|0|NONE|20|LIQUID,VISCOUS||||0||0|7CC35E|",
            "nitric_acid|82|108|BB7A1E|3|0|2|OXIDIZER|20|LIQUID|60|||0||0||",
            "solvent|83|109|E4E3EF|2|3|0|NONE|20|LIQUID|30|||100000||0|E4E3EF|",
            "blood|84|21|B22424|0|0|0|NONE|20|LIQUID,VISCOUS,DELICIOUS||||0||0||",
            "blood_hot|85|22|F22419|3|0|0|NONE|666|LIQUID,VISCOUS||||0||0||",
            "syngas|86|72|131313|1|4|2|NONE|20|GASEOUS||||1650000|GAS|2060000||FFFFFF/131313",
            "oxyhydrogen|87|73|483FC1|0|4|2|NONE|20|GASEOUS||||5000|GAS|15000||",
            "radiosolvent|88|110|A4D7DD|3|3|0|NONE|20|LIQUID|50|||150000||0||",
            "chlorine|89|37|BAB572|3|0|0|OXIDIZER|20|GASEOUS|25|||0||0||BAB572/887B34",
            "heavyoil_vacuum|90|50|131214|2|1|0|NONE|20|LIQUID,VISCOUS||||187000|LOW|234000|513F39|",
            "reformate|91|55|835472|2|2|0|NONE|20|LIQUID,VISCOUS||||2400000|HIGH|6000000|D180D6|",
            "lightoil_vacuum|92|59|8C8851|1|2|0|NONE|20|LIQUID||||3000000|MEDIUM|4500000|B46B52|",
            "sourgas|93|70|C9BE0D|4|4|0|ACID|20|GASEOUS|10||false:1|250000||0||C9BE0D/303030",
            "xylene|94|76|5C4E76|2|3|0|NONE|20|LIQUID,VISCOUS||||3150000|HIGH|7870000|A380D6|",
            "heatingoil_vacuum|95|63|211D06|2|2|0|NONE|20|LIQUID,VISCOUS||||1310000|LOW|1640000|694235|",
            "diesel_reform|96|79|CDC3C6|1|2|0|NONE|20|LIQUID||||1370000|HIGH|3430000|FFC500|",
            "diesel_crack_reform|97|81|CDC3CC|1|2|0|NONE|20|LIQUID||||1280000|HIGH|3210000|FFC500|",
            "kerosene_reform|98|83|FFA5F3|1|2|0|NONE|20|LIQUID||||6400000|AERO|9600000|FF377D|",
            "reformgas|99|77|6362AE|1|4|1|NONE|20|GASEOUS||||10500000|GAS|15700000||9392FF/FFB992",
            "colloid|100|102|787878|0|0|0|NONE|20|LIQUID,VISCOUS||||0||0||",
            "phosgene|101|135|CFC4A4|4|0|1|NONE|20|GASEOUS||||0||0||CFC4A4/361414",
            "mustardgas|102|136|BAB572|4|1|1|NONE|20|GASEOUS||||0||0||BAB572/361414",
            "iongel|103|105|B8FFFF|1|0|4|NONE|20|LIQUID,VISCOUS||||0||0||",
            "oil_coker|104|44|001802|2|1|0|NONE|20|LIQUID,VISCOUS||||31200||0||",
            "naphtha_coker|105|54|495944|2|1|0|NONE|20|LIQUID,VISCOUS||||125000|MEDIUM|187000||",
            "gas_coker|106|68|DEF4CA|1|4|0|NONE|20|GASEOUS||||62500|GAS|93700||",
            "egg|107|120|D2C273|0|0|0|NONE|20|LIQUID||||0||0||",
            "cholesterol|108|121|D6D2BD|0|0|0|NONE|20|LIQUID||||0||0||",
            "estradiol|109|137|CDD5D8|0|0|0|NONE|20|LIQUID||||0||0||",
            "fishoil|110|95|4B4A45|0|1|0|NONE|20|LIQUID||||75000||0||",
            "sunfloweroil|111|96|CBAD45|0|1|0|NONE|20|LIQUID||||50000||0||",
            "nitroglycerin|112|138|92ACA6|0|4|0|NONE|20|LIQUID||||0||0||",
            "redmud|113|118|D85638|3|0|4|NONE|20|LIQUID,VISCOUS,LEADCON|60|||1000||0||",
            "chlorocalcite_solution|114|122|808080|0|0|0|NONE|20|LIQUID,NOCON|60|||0||0||",
            "chlorocalcite_mix|115|123|808080|0|0|0|NONE|20|LIQUID,NOCON|60|||0||0||",
            "chlorocalcite_cleaned|116|124|808080|0|0|0|NONE|20|LIQUID,NOCON|60|||0||0||",
            "potassium_chloride|117|125|808080|0|0|0|NONE|20|LIQUID,NOCON|60|||0||0||",
            "calcium_chloride|118|126|808080|0|0|0|NONE|20|LIQUID,NOCON|60|||0||0||",
            "calcium_solution|119|127|808080|0|0|0|NONE|20|LIQUID,NOCON|60|||0||0||",
            "smoke|120|152|808080|0|0|0|NONE|20|GASEOUS,NOCON,NOID||||0||0||",
            "smoke_leaded|121|153|808080|0|0|0|NONE|20|GASEOUS,NOCON,NOID||||0||0||",
            "smoke_poison|122|154|808080|0|0|0|NONE|20|GASEOUS,NOCON,NOID||||0||0||",
            "helium4|123|34|E54B0A|0|0|0|ASPHYXIANT|20|GASEOUS||||0||0||FD631F/FFFF00",
            "heavywater_hot|124|5|4D007B|1|0|0|NONE|600|LIQUID,VISCOUS||||0||0||",
            "sodium|125|23|CCD4D5|1|2|3|NONE|400|LIQUID,VISCOUS||||0||0||",
            "sodium_hot|126|24|E2ADC1|1|2|3|NONE|1200|LIQUID,VISCOUS||||0||0||",
            "thorium_salt|127|27|7A5542|2|0|3|NONE|800|LIQUID,VISCOUS|65|||0||0||",
            "thorium_salt_hot|128|28|3E3627|2|0|3|NONE|1600|LIQUID,VISCOUS|65|||0||0||",
            "thorium_salt_depleted|129|29|302D1C|2|0|3|NONE|800|LIQUID,VISCOUS|65|||0||0||",
            "fullerene|130|119|FF7FED|3|3|3|NONE|20|LIQUID|65|||0||0||",
            "pheromone|131|155|5FA6E8|0|0|0|NONE|20|LIQUID||||0||0||",
            "pheromone_m|132|156|48C9B0|0|0|0|NONE|20|LIQUID||||0||0||",
            "oil_ds|133|40|121212|2|1|0|NONE|20|LIQUID,VISCOUS||||50000||0|424242|",
            "hotoil_ds|134|46|3F180F|2|3|0|NONE|350|LIQUID,VISCOUS||||0||0||",
            "crackoil_ds|135|42|2A1C11|2|1|0|NONE|20|LIQUID,VISCOUS||||62500||0|424242|",
            "hotcrackoil_ds|136|48|3A1A28|2|3|0|NONE|350|LIQUID,VISCOUS||||0||0||",
            "naphtha_ds|137|52|63614E|2|1|0|NONE|20|LIQUID,VISCOUS||||220000|MEDIUM|330000|5F6D44|",
            "lightoil_ds|138|57|63543E|1|2|0|NONE|20|LIQUID||||2930000|MEDIUM|4400000|B46B52|",
            "stellar_flux|139|145|E300FF|0|4|4|ANTIMATTER|20|GASEOUS,ANTI||||0||0||",
            "vitriol|140|103|6E5222|2|0|1|NONE|20|LIQUID,VISCOUS||||0||0||",
            "slop|141|104|929D45|0|0|0|NONE|20|LIQUID,VISCOUS||||0||0||",
            "lead|142|25|666672|4|0|0|NONE|350|LIQUID,VISCOUS||||0||0||",
            "lead_hot|143|26|776563|4|0|0|NONE|1500|LIQUID,VISCOUS||||0||0||",
            "perfluoromethyl|144|15|BDC8DC|1|0|1|NONE|15|LIQUID||||0||0||",
            "perfluoromethyl_cold|145|16|99DADE|1|0|1|NONE|-150|LIQUID||||0||0||",
            "perfluoromethyl_hot|146|17|B899DE|1|0|1|NONE|250|LIQUID||||0||0||",
            "lye|147|134|FFECCC|3|0|1|ACID|20|LIQUID|40|||0||0||",
            "sodium_aluminate|148|128|FFD191|3|0|1|ACID|20|LIQUID|30|||0||0||",
            "bauxite_solution|149|129|E2560F|3|0|3|ACID|20|LIQUID,VISCOUS|40|||0||0||",
            "alumina|150|130|DDFFFF|0|0|0|NONE|20|LIQUID||||0||0||",
            "air|151|1|E7EAEB|0|0|0|NONE|20|GASEOUS||||0||0||",
            "concrete|152|132|A2A2A2|0|0|0|NONE|20|LIQUID||||0||0||",
            "dhc|153|98|D2AFFF|0|0|0|NONE|20|GASEOUS||||0||0||",
            "airblast|154|2|FFDADA|0|3|0|NONE|1200|GASEOUS||||0||0||",
            "flue|155|66|131313|1|4|1|NONE|20|GASEOUS||||25000||0||FF4545/FFE97F",
            "titanium_tetrachloride|156|131|DBF796|3|0|2|NOWATER|20|LIQUID||||0||0|E1E549|",
    };

    private ParityL2Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        fluidTable(problems, cases);
        thermal(problems, cases);
        tooltips(problems, cases);
        ServerLevel level = server.overworld();
        List<BlockPos> pads = new ArrayList<>();
        try {
            barrels(level, problems, cases, pads);
        } catch (RuntimeException exception) {
            problems.add("parity L2 barrel checks threw " + exception);
        } finally {
            for (BlockPos pos : pads) cleanup(level, pos);
        }
        return cases[0];
    }

    // ---------------------------------------------------------------- fluid table

    private static void fluidTable(List<String> problems, int[] cases) {
        List<HbmFluidDefinition> nice = HbmFluids.niceOrder();
        for (String row : EXPECTED) {
            String[] c = row.split("\\|", -1);
            String name = c[0];
            Optional<HbmFluidDefinition> found = HbmFluids.byName(name);
            if (found.isEmpty()) {
                problems.add("parity L2 fluid missing: " + name);
                continue;
            }
            HbmFluidDefinition f = found.get();
            String p = "parity L2 fluid " + name + ": ";
            eq(f.oldId(), Integer.parseInt(c[1]), problems, cases, p + "id");
            check(HbmFluids.byOldId(Integer.parseInt(c[1])).orElse(null) == f, problems, cases, p + "byOldId");
            eq(nice.indexOf(f), Integer.parseInt(c[2]), problems, cases, p + "metaOrder position");
            eq(f.color(), Integer.parseInt(c[3], 16), problems, cases, p + "color");
            eq(f.poison(), Integer.parseInt(c[4]), problems, cases, p + "poison");
            eq(f.flammability(), Integer.parseInt(c[5]), problems, cases, p + "flammability");
            eq(f.reactivity(), Integer.parseInt(c[6]), problems, cases, p + "reactivity");
            check(f.symbol().name().equals(c[7]), problems, cases, p + "symbol " + f.symbol() + " != " + c[7]);
            eq(f.temperatureCelsius(), Integer.parseInt(c[8]), problems, cases, p + "temperature");
            EnumSet<HbmFluidTrait> expected = EnumSet.noneOf(HbmFluidTrait.class);
            if (!c[9].isEmpty()) for (String t : c[9].split(",")) expected.add(simple(t));
            for (HbmFluidTrait t : List.of(HbmFluidTrait.LIQUID, HbmFluidTrait.VISCOUS, HbmFluidTrait.EVAPORATES, HbmFluidTrait.GASEOUS,
                    HbmFluidTrait.PLASMA, HbmFluidTrait.ANTIMATTER, HbmFluidTrait.LEAD_CONTAINER, HbmFluidTrait.NO_CONTAINER,
                    HbmFluidTrait.NO_IDENTIFIER, HbmFluidTrait.DELICIOUS, HbmFluidTrait.UNSIPHONABLE)) {
                check(f.hasTrait(t) == expected.contains(t), problems, cases, p + "trait " + t + " port=" + f.hasTrait(t));
            }
            eq(HbmFluidTraitInfo.intParam(f, "CORROSIVE", 1, -1), c[10].isEmpty() ? -1 : Integer.parseInt(c[10]), problems, cases, p + "FT_Corrosive");
            float rad = HbmFluidTraitInfo.floatParam(f, "RADIATION", 1, -1F);
            check(c[11].isEmpty() ? rad < 0F : rad == Float.parseFloat(c[11]), problems, cases, p + "FT_VentRadiation " + rad + " != " + c[11]);
            String[] poison = HbmFluidTraitInfo.token(f, "POISON");
            String portPoison = poison == null || poison.length < 3 ? "" : poison[1] + ":" + poison[2];
            check(portPoison.equals(c[12]), problems, cases, p + "FT_Poison " + portPoison + " != " + c[12]);
            eq(f.flammableHeatEnergy(), Long.parseLong(c[13]), problems, cases, p + "FT_Flammable");
            String grade = f.combustibleFuelGrade() == null ? "" : f.combustibleFuelGrade().name();
            check(grade.equals(c[14]), problems, cases, p + "FT_Combustible grade " + grade + " != " + c[14]);
            eq(f.combustibleFuelGrade() == null ? 0L : f.combustibleHeatEnergy(), Long.parseLong(c[15]), problems, cases, p + "FT_Combustible energy");
            eq(f.canisterColorRgbOr(-1), c[16].isEmpty() ? -1 : Integer.parseInt(c[16], 16), problems, cases, p + "CD_Canister");
            String gas = f.gasTankColors().isEmpty() ? "" : f.gasTankColors().replace("0x", "").toUpperCase(Locale.ROOT);
            check(gas.equals(c[17]), problems, cases, p + "CD_Gastank " + gas + " != " + c[17]);
        }
    }

    private static HbmFluidTrait simple(String token) {
        return switch (token) {
            case "LIQUID" -> HbmFluidTrait.LIQUID;
            case "VISCOUS" -> HbmFluidTrait.VISCOUS;
            case "EVAP" -> HbmFluidTrait.EVAPORATES;
            case "GASEOUS" -> HbmFluidTrait.GASEOUS;
            case "PLASMA" -> HbmFluidTrait.PLASMA;
            case "ANTI" -> HbmFluidTrait.ANTIMATTER;
            case "LEADCON" -> HbmFluidTrait.LEAD_CONTAINER;
            case "NOCON" -> HbmFluidTrait.NO_CONTAINER;
            case "NOID" -> HbmFluidTrait.NO_IDENTIFIER;
            case "DELICIOUS" -> HbmFluidTrait.DELICIOUS;
            case "UNSIPHONABLE" -> HbmFluidTrait.UNSIPHONABLE;
            default -> throw new IllegalArgumentException(token);
        };
    }

    // ---------------------------------------------------------------- FT_Heatable / FT_Coolable / FT_PWRModerator

    private static void thermal(List<String> problems, int[] cases) {
        for (HbmFluidDefinition f : HbmFluids.definitions()) {
            HbmFluidTraitData.Heatable h = HbmFluidTraitData.heatable(f);
            double[] eff = h == null ? new double[5] : h.efficiency();
            heating(f, h, eff[0], HbmThermalConversions.firstBoilerStep(f), "BOILER", problems, cases);
            heating(f, h, eff[1], HbmThermalConversions.firstHeatExchangerStep(f), "HEATEXCHANGER", problems, cases);
            heating(f, h, eff[2], HbmThermalConversions.firstPwrStep(f), "PWR", problems, cases);
            heating(f, h, eff[3], HbmThermalConversions.firstIcfStep(f), "ICF", problems, cases);
            HbmFluidTraitData.Coolable cl = HbmFluidTraitData.coolable(f);
            double[] ceff = cl == null ? new double[2] : cl.efficiency();
            cooling(f, cl, ceff[0], HbmThermalConversions.turbineStep(f), "TURBINE", problems, cases);
            cooling(f, cl, ceff[1], HbmThermalConversions.heatExchangerCoolingStep(f), "HEATEXCHANGER", problems, cases);
            Double moderator = HbmFluidTraitData.pwrModerator(f);
            check(HbmThermalConversions.pwrModeratorMultiplier(f) == (moderator == null ? 1.0D : moderator), problems, cases,
                    "parity L2 FT_PWRModerator " + f.name());
        }
    }

    private static void heating(HbmFluidDefinition f, HbmFluidTraitData.Heatable h, double eff,
                                Optional<HbmThermalConversions.HeatingStep> step, String type, List<String> problems, int[] cases) {
        String p = "parity L2 FT_Heatable " + f.name() + " " + type + ": ";
        if (eff <= 0) {
            check(step.isEmpty(), problems, cases, p + "port has a step the original lacks");
            return;
        }
        if (step.isEmpty()) {
            check(false, problems, cases, p + "missing step");
            return;
        }
        HbmThermalConversions.HeatingStep s = step.get();
        check(s.output().name().equals(h.produced()) && s.heatReq() == h.heatReq() && s.amountReq() == h.amountReq()
                && s.amountProduced() == h.amountProduced() && s.boilerEfficiency() == eff, problems, cases, p + s);
    }

    private static void cooling(HbmFluidDefinition f, HbmFluidTraitData.Coolable c, double eff,
                                Optional<HbmThermalConversions.CoolingStep> step, String type, List<String> problems, int[] cases) {
        String p = "parity L2 FT_Coolable " + f.name() + " " + type + ": ";
        if (eff <= 0) {
            check(step.isEmpty(), problems, cases, p + "port has a step the original lacks");
            return;
        }
        if (step.isEmpty()) {
            check(false, problems, cases, p + "missing step");
            return;
        }
        HbmThermalConversions.CoolingStep s = step.get();
        check(s.output().name().equals(c.coolsTo()) && s.amountReq() == c.amountReq() && s.amountProduced() == c.amountProduced()
                && s.heatEnergy() == c.heatEnergy() && s.turbineEfficiency() == eff, problems, cases, p + s);
    }

    // ---------------------------------------------------------------- FluidType.addInfo

    private static void tooltips(List<String> problems, int[] cases) {
        String hold = "Hold <LSHIFT> to display more info";
        tooltip("oil", false, problems, cases, "[Flammable]", "Provides 25.0kTU per bucket", "[Polluting]", hold);
        tooltip("kerosene", false, problems, cases, "[Flammable]", "Provides 2.56MTU per bucket", "[Combustible]",
                "Provides 3.85MHE per bucket", "Fuel grade: Aviation", "[Polluting]", hold);
        tooltip("water", true, problems, cases, "Thermal capacity: 200 TU per 1mB", "[Boilable] Efficiency: 100%",
                "[Heatable] Efficiency: 25%", "[Liquid]", "[Ignored by siphon]");
        tooltip("water", false, problems, cases, hold);
        tooltip("cryogel", false, problems, cases, "-170°C", hold);
        tooltip("amat", false, problems, cases, "[Antimatter]", hold);
        tooltip("mustardgas", true, problems, cases, "[Polluting]", "When spilled:", " - 5.0E-4 POISON per mB",
                "[Toxin]", "- Corrosive Fumes: 8.0 DPS", "- Corrosive Fumes (requires hazmat suit):", "   - Wither II 0:05",
                "   - Nausea 0:05", "[Gaseous]");
        tooltip("lead", true, problems, cases, "350°C", "Thermal capacity: 800 TU per 1mB", "[PWR Coolant] Efficiency: 75%",
                "[ICF Coolant] Efficiency: 400%", "[PWR Flux Multiplier]", "Core flux -25%", "[Liquid]", "[Viscous]");
        tooltip("nitric_acid", false, problems, cases, "[Strongly Corrosive]", "[Polluting]", hold);
    }

    private static void tooltip(String name, boolean shift, List<String> problems, int[] cases, String... expected) {
        HbmFluidDefinition f = HbmFluids.byName(name).orElse(HbmFluids.none());
        List<Component> lines = new ArrayList<>();
        HbmFluidTraitInfo.addInfo(lines, f, shift);
        List<String> actual = lines.stream().map(Component::getString).toList();
        check(actual.equals(List.of(expected)), problems, cases, "parity L2 tooltip " + name + (shift ? " (shift)" : "") + ": " + actual);
    }

    // ---------------------------------------------------------------- TileEntityBarrel

    private static void barrels(ServerLevel level, List<String> problems, int[] cases, List<BlockPos> pads) {
        HbmFluidDefinition water = fluid("water");
        HbmFluidDefinition steam = fluid("steam");
        HbmFluidDefinition peroxide = fluid("peroxide");
        HbmFluidDefinition nitric = fluid("nitric_acid");
        HbmFluidDefinition amat = fluid("amat");

        // capacities (BlockFluidBarrel ctor), priority, 1_000_000_000 speed, comparator
        Block[] blocks = {HbmBlocks.BARREL_PLASTIC.get(), HbmBlocks.BARREL_CORRODED.get(), HbmBlocks.BARREL_STEEL.get(),
                HbmBlocks.BARREL_TCALLOY.get(), HbmBlocks.BARREL_ANTIMATTER.get()};
        int[] caps = {12_000, 16_000, 16_000, 24_000, 16_000};
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = pad(level, pads);
            FluidBarrelBlockEntity barrel = place(level, pos, blocks[i]);
            if (barrel == null) {
                check(false, problems, cases, "parity L2 barrel BE missing for " + blocks[i]);
                continue;
            }
            eq(barrel.tank().capacity(), caps[i], problems, cases, "parity L2 barrel capacity " + blocks[i]);
            check(barrel.usesBarrelPriority(), problems, cases, "parity L2 barrel LOW priority in mode 1 " + blocks[i]);
            barrel.tank().setType(water);
            IFluidHandler handler = barrel.fluidHandler(null);
            int filled = handler == null ? -1 : handler.fill(HbmFluids.toNeoStack(water, caps[i]), IFluidHandler.FluidAction.SIMULATE);
            eq(filled, caps[i], problems, cases, "parity L2 barrel receiver speed " + blocks[i]);
            barrel.tank().setAmount(caps[i] / 2);
            int signal = level.getBlockState(pos).getAnalogOutputSignal(level, pos);
            eq(signal, (int) ((double) (caps[i] / 2) / caps[i] * 15D) + 1, problems, cases, "parity L2 barrel comparator " + blocks[i]);
        }

        // plastic + hot or corrosive fluid -> destroyed; water survives
        for (HbmFluidDefinition bad : List.of(steam, peroxide)) {
            BlockPos pos = pad(level, pads);
            FluidBarrelBlockEntity barrel = place(level, pos, HbmBlocks.BARREL_PLASTIC.get());
            fill(barrel, bad, 1_000);
            tick(level, pos, barrel);
            check(level.getBlockState(pos).isAir(), problems, cases, "parity L2 plastic barrel with " + bad.name() + " must break");
        }
        {
            BlockPos pos = pad(level, pads);
            FluidBarrelBlockEntity barrel = place(level, pos, HbmBlocks.BARREL_PLASTIC.get());
            fill(barrel, water, 1_000);
            tick(level, pos, barrel);
            check(level.getBlockState(pos).is(HbmBlocks.BARREL_PLASTIC.get()), problems, cases, "parity L2 plastic barrel with water survives");
        }
        // steel + highly corrosive -> corroded barrel with the same contents; mildly corrosive stays
        {
            BlockPos pos = pad(level, pads);
            FluidBarrelBlockEntity barrel = place(level, pos, HbmBlocks.BARREL_STEEL.get());
            fill(barrel, nitric, 5_000);
            tick(level, pos, barrel);
            BlockEntity be = level.getBlockEntity(pos);
            check(level.getBlockState(pos).is(HbmBlocks.BARREL_CORRODED.get()) && be instanceof FluidBarrelBlockEntity corroded
                            && corroded.tank().type() == nitric && corroded.tank().amount() == 5_000, problems, cases,
                    "parity L2 steel barrel with nitric acid becomes barrel_corroded with 5000 mB");
        }
        {
            BlockPos pos = pad(level, pads);
            FluidBarrelBlockEntity barrel = place(level, pos, HbmBlocks.BARREL_STEEL.get());
            fill(barrel, peroxide, 1_000);
            tick(level, pos, barrel);
            check(level.getBlockState(pos).is(HbmBlocks.BARREL_STEEL.get()), problems, cases, "parity L2 steel barrel keeps peroxide");
        }
        // tcalloy + highly corrosive: no interaction and no tank damage logic
        {
            BlockPos pos = pad(level, pads);
            FluidBarrelBlockEntity barrel = place(level, pos, HbmBlocks.BARREL_TCALLOY.get());
            fill(barrel, nitric, 1_000);
            for (int i = 0; i < 20; i++) tick(level, pos, barrel);
            check(level.getBlockState(pos).is(HbmBlocks.BARREL_TCALLOY.get()) && barrel.tank().amount() == 1_000, problems, cases,
                    "parity L2 tcalloy barrel keeps nitric acid without leaking");
        }
        // corroded: 1/3 chance per tick to spill 1 mB
        {
            BlockPos pos = pad(level, pads);
            FluidBarrelBlockEntity barrel = place(level, pos, HbmBlocks.BARREL_CORRODED.get());
            fill(barrel, water, 10_000);
            int ticks = 0;
            for (; ticks < 600 && level.getBlockEntity(pos) == barrel; ticks++) tick(level, pos, barrel);
            int lost = 10_000 - barrel.tank().amount();
            check(lost > 0 && lost <= ticks && lost > ticks / 6, problems, cases,
                    "parity L2 corroded barrel leak " + lost + " mB in " + ticks + " ticks");
        }
        // antimatter: only the antimatter barrel holds it
        {
            BlockPos pos = pad(level, pads);
            FluidBarrelBlockEntity barrel = place(level, pos, HbmBlocks.BARREL_ANTIMATTER.get());
            fill(barrel, amat, 1_000);
            tick(level, pos, barrel);
            check(level.getBlockState(pos).is(HbmBlocks.BARREL_ANTIMATTER.get()), problems, cases, "parity L2 antimatter barrel holds amat");
            BlockPos steelPos = pad(level, pads);
            FluidBarrelBlockEntity steelBarrel = place(level, steelPos, HbmBlocks.BARREL_STEEL.get());
            fill(steelBarrel, amat, 1_000);
            tick(level, steelPos, steelBarrel);
            check(!level.getBlockState(steelPos).is(HbmBlocks.BARREL_STEEL.get()), problems, cases, "parity L2 steel barrel with amat explodes");
        }
        // TileEntityMachineFluidTank: LOW priority in mode 1 (from TileEntityBarrel), receiver speed max(500, free / 100)
        {
            BlockPos pos = pad(level, pads);
            level.setBlock(pos, HbmBlocks.MACHINE_FLUIDTANK.get().defaultBlockState(), 2 | 16);
            if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
                // [parity T1: fluid net] corrected: TileEntityMachineFluidTank extends TileEntityBarrel and inherits LOW in mode 1
                tank.cycleMode();
                check(tank.fluidPriority() == com.atomiclegacy.power.PowerEndpoint.ConnectionPriority.LOW,
                        problems, cases, "parity L2 machine_fluidtank LOW priority in mode 1 (TileEntityBarrel)");
                tank.cycleMode();
                tank.cycleMode();
                tank.cycleMode();
                tank.setType(water);
                IFluidHandler handler = tank.fluidHandler(null);
                int cap = tank.tank().capacity();
                int filled = handler == null ? -1 : handler.fill(HbmFluids.toNeoStack(water, cap), IFluidHandler.FluidAction.SIMULATE);
                eq(filled, Math.max(500, cap / 100), problems, cases, "parity L2 machine_fluidtank receiver speed");
            } else {
                check(false, problems, cases, "parity L2 machine_fluidtank BE missing");
            }
        }
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElseThrow();
    }

    private static void fill(FluidBarrelBlockEntity barrel, HbmFluidDefinition fluid, int amount) {
        barrel.tank().setType(fluid);
        barrel.tank().setAmount(amount);
    }

    private static void tick(ServerLevel level, BlockPos pos, FluidBarrelBlockEntity barrel) {
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) == barrel) FluidBarrelBlockEntity.tick(level, pos, state, barrel);
    }

    private static FluidBarrelBlockEntity place(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2 | 16);
        return level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel ? barrel : null;
    }

    private static BlockPos pad(ServerLevel level, List<BlockPos> pads) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + (index % 8) * SPACING, BASE_Y, BASE_Z + (index / 8) * SPACING);
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 4, 3))) {
            level.setBlock(cursor, cursor.getY() < pos.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        pads.add(pos);
        return pos;
    }

    private static void cleanup(ServerLevel level, BlockPos pos) {
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 4, 3))) {
            if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add(message);
    }

    private static void eq(long actual, long expected, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (actual != expected) problems.add(message + ": " + actual + " != " + expected);
    }
}
