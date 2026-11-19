"""f1: loop/handler unrolling for parity_e3_recipes.py (static values copied from 936c60c sources).
UNROLL[(file, header-substring)] -> list of replacement dicts applied to the args of every call inside that loop.
Word keys are replaced on word boundaries, other keys literally."""
import re
from pathlib import Path
ORIG = Path('C:/Users/Admin/Documents/ChatGPT/NeoNTM/work/original/src/main/java/com/hbm')
MB = (ORIG / 'blocks/ModBlocks.java').read_text(encoding='utf-8', errors='replace')

DYES = ["Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"]
PLATE_DYES = ["Black", "White", "Red", "Green", "LightGray", "Blue", "Purple", "Cyan", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange"]
PLATEMETAL = ['BASE', 'BLACK', 'WHITE', 'RED', 'GREEN', 'LIGHT_GRAY', 'BLUE', 'PURPLE', 'CYAN', 'PINK', 'LIME', 'YELLOW', 'LIGHT_BLUE', 'MAGENTA', 'ORANGE']
CHEMDYE = 'BLACK RED GREEN BROWN BLUE PURPLE CYAN SILVER GRAY PINK LIME YELLOW LIGHTBLUE MAGENTA ORANGE WHITE'.split()
CM = ['STEEL', 'ALLOY', 'DESH', 'TCALLOY']
COKE = ['COAL', 'LIGNITE', 'PETROLEUM']
# Mats.java setAutogen(... WIRE ...) / (... BOLT ...), first dict name (OreDictManager DictFrame)
WIRE_MATS = [('CARBON', 'Carbon'), ('GOLD', 'Gold'), ('SCHRABIDIUM', 'Schrabidium'), ('COPPER', 'Copper'), ('TUNGSTEN', 'Tungsten'),
             ('ALUMINIUM', 'Aluminum'), ('LEAD', 'Lead'), ('ZIRCONIUM', 'Zirconium'), ('STEEL', 'Steel'), ('MINGRADE', 'Mingrade'), ('MAGTUNG', 'MagnetizedTungsten')]
BOLT_MATS = [('TUNGSTEN', 'Tungsten'), ('LEAD', 'Lead'), ('STEEL', 'Steel'), ('DURA', 'DuraSteel')]


def _slabs():
    out = []
    for m in re.finditer(r'(\w+) = new BlockMultiSlab\(null, Material\.\w+, ([^)]*)\)', MB):
        for mat in [x.strip() for x in m.group(2).split(',')]:
            out.append({'(Block) array[2]': 'ModBlocks.' + m.group(1), '(int) array[3]': 'X.' + mat.upper(),
                        '(Block) array[0]': 'ModBlocks.' + mat, '(int) array[1]': '0'})
    for m in re.finditer(r'(\w+) = new BlockMultiSlabMeta\(null, Material\.\w+, new Block\[\] \{ ([^}]*)\}, (.*?)\)\.setBlockName', MB):
        blocks = [x.strip() for x in m.group(2).split(',')]
        metas = re.findall(r'LightstoneType\.(\w+)\.ordinal\(\)', m.group(3))
        for b, mt in zip(blocks, metas):
            out.append({'(Block) array[2]': 'ModBlocks.' + m.group(1), '(int) array[3]': 'X.' + mt,
                        '(Block) array[0]': 'ModBlocks.' + b, '(int) array[1]': 'LightstoneType.%s.ordinal()' % mt})
    return out


def _stairs():
    out = []
    for m in re.finditer(r'(\w+) = new BlockGenericStairs\((\w+), (.*?)\)\.setBlockName\("\w+"\)(.*)', MB):
        out.append({'(Block) array[2]': 'ModBlocks.' + m.group(1), '(Block) array[0]': 'ModBlocks.' + m.group(2), '(int) array[1]': m.group(3)})
    return out


UNROLL = {
    ('CraftingManager.java', 'BlockMultiSlab.recipeGen'): _slabs(),
    ('CraftingManager.java', 'BlockGenericStairs.recipeGen'): _stairs(),
    ('CraftingManager.java', 'for(Item brick : bricks)'): [{'brick': 'Items.brick'}, {'brick': 'Items.netherbrick'}],
    ('CraftingManager.java', 'craneCasing.length'): [{'casing': 'Blocks.stonebrick', 'amount': '1'}, {'casing': 'IRON.ingot()', 'amount': '2'}, {'casing': 'STEEL.ingot()', 'amount': '4'}],
    ('CraftingManager.java', 'i < 16; i++'): [{'i': str(i), 'dyeName': '"dye%s"' % DYES[15 - i]} for i in range(16)],
    ('CraftingManager.java', 'i < 14; i++'): [{'i + 1': 'PlatemetalType.%s.ordinal()' % PLATEMETAL[i + 1], 'dyeName': '"dye%s"' % PLATE_DYES[i]} for i in range(14)],
    ('CraftingManager.java', 'i < 4; i++'): [{'i': 'EnumCMMaterials.%s.ordinal()' % c} for c in CM],
    ('CraftingManager.java', 'Mats.orderedList'): [{}],
    ('CraftingManager.java', 'MaterialShapes.WIRE)) for(String name'): [{'mat.id': 'Mats.MAT_%s.id' % m, 'MaterialShapes.INGOT.name() + name': '"ingot%s"' % n} for m, n in WIRE_MATS],
    ('CraftingManager.java', 'MaterialShapes.BOLT)) for(String name'): [{'mat.id': 'Mats.MAT_%s.id' % m, 'MaterialShapes.INGOT.name() + name': '"ingot%s"' % n} for m, n in BOLT_MATS],
    ('MineralRecipes.java', 'EnumCokeType.values().length'): [{'i': 'EnumCokeType.%s.ordinal()' % c} for c in COKE],
    ('MineralRecipes.java', 'ItemWasteLong.WasteClass.values().length'): [{'i': str(i)} for i in range(5)],
    ('MineralRecipes.java', 'ItemWasteShort.WasteClass.values().length'): [{'i': str(i)} for i in range(8)],
    ('PowderRecipes.java', 'i < 16; i++'): [{'i': 'EnumChemDye.%s.ordinal()' % c} for c in CHEMDYE],
}
# meta -> port id corrections needed by the unrolled variants (port keeps these as one item + custom_data meta)
ALIAS = {'crayon#%s' % c: 'crayon#%d' % i for i, c in enumerate(CHEMDYE)}
ALIAS.update({'chemical_dye#%s' % c: 'chemical_dye_' + c.lower() for c in CHEMDYE})
ALIAS.update({'concrete_colored#0': 'concrete_colored#0', 'platemetal#BASE': 'platemetal'})
for _w in ('long', 'long_depleted', 'short', 'short_depleted'):
    for _t in ('', '_tiny'):
        ALIAS['nuclear_waste_%s%s#0' % (_w, _t)] = 'nuclear_waste_%s%s#0' % (_w, _t)


def unroll(file, header):
    h = re.sub(r'\s+', ' ', header)
    fn = file.split('/')[-1]
    for (f, key), vals in UNROLL.items():
        if f == fn and key in h:
            return vals
    return None


def apply(arg, rep):
    for k, v in sorted(rep.items(), key=lambda kv: -len(kv[0])):
        if re.fullmatch(r'\w+', k):
            arg = re.sub(r'(?<![\w.])%s(?!\w)' % re.escape(k), lambda _m: v, arg)
        else:
            arg = arg.replace(k, v)
    return arg

# ModBlocks.X whose port id differs from the same-named ModItems.X (port_item cannot tell them apart)
BLOCK_ALIAS = {'fallout': 'reinhardtshbm:fallout', 'hev_battery': 'reinhardtshbm:hev_battery_block'}
for _b in 'IRON COPPER LITHIUM SILICON LEAD TITANIUM ALUMINIUM SULFUR CALCIUM BISMUTH RADIUM TECHNETIUM POLONIUM URANIUM'.split():
    ALIAS['ore_byproduct#B_' + _b] = 'ore_byproduct_' + _b.lower()
for _n in range(1, 9):
    ALIAS['page_of_#PAGE%d' % _n] = 'page_of_page%d' % _n
# SmeltingRecipes 177/181: one recipe per meta, all metas -> same result; port uses one item ingredient (all variants)
UNROLL[('SmeltingRecipes.java', 'EnumBedrockOre.values()')] = [{'i': 'OreDictionary.WILDCARD_VALUE'}]
UNROLL[('SmeltingRecipes.java', 'i < 10; i++')] = [{'i': 'OreDictionary.WILDCARD_VALUE'}]
ALIAS.update({'scraps#MINGRADE': 'scraps_red_copper', 'scraps#MAGTUNG': 'scraps_magnetized_tungsten', 'scraps#STEEL': 'scraps_steel', 'scraps#TCALLOY': 'scraps_tcalloy'})
