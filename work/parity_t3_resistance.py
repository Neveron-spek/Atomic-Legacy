"""T3: effective explosion resistance of every original HBM block (936c60c4).

1.7.10 Block: setResistance(r) -> blockResistance = r*3; setHardness(h) -> blockResistance = max(blockResistance, h*5);
getExplosionResistance(entity) -> blockResistance/5. BlockStairs(parent) ctor: setHardness(parent.hardness);
setResistance(parent.blockResistance/3). 1.21 uses Properties.explosionResistance directly, so the port value
must equal blockResistance/5 (float arithmetic emulated).
"""
import json, re, struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent
ORIG = Path('C:/Users/Admin/Documents/ChatGPT/NeoNTM/work/original/src/main/java')
OUT = ROOT.parent / 'src/main/resources/legacy/reinhardtshbm/parity-t3-resistance.json'

def f32(x):
    if x in (float('inf'), float('-inf')):
        return x
    return struct.unpack('f', struct.pack('f', x))[0]

def num(expr):
    if expr.strip() == 'Float.POSITIVE_INFINITY':
        return float('inf')
    e = expr.strip().replace('_', '')
    e = re.sub(r'(?<=\d)[FfDd]\b', '', e)
    if not re.fullmatch(r'[0-9eE.+\-*/ ()]+', e):
        raise ValueError(expr)
    return f32(eval(e))

src = re.sub(r'/\*.*?\*/', '', (ORIG / 'com/hbm/blocks/ModBlocks.java').read_text(encoding='utf-8'), flags=re.S)
src = re.sub(r'//[^\n]*', '', src)

# class hierarchy over the whole original source
parent = {}
class_src = {}
for p in ORIG.rglob('*.java'):
    text = p.read_text(encoding='utf-8', errors='ignore')
    for m in re.finditer(r'class\s+(\w+)(?:<[^>{]*>)?\s+extends\s+(\w+)', text):
        parent.setdefault(m.group(1), m.group(2))
        class_src.setdefault(m.group(1), text)
# [parity U3: hardness/tool] Material of every block: explicit constructor argument, else the first hard-coded
# super(...)/this(...) material of the class chain, else the vanilla 1.7.10 superclass constructor.
MAT_REF = r'(?:Material\.(\w+)|\b(thick_foliage|materialGas|fluidmud|fluidacid|fluidtoxic|fluidschrabidic|fluidcorium)\b)'
VANILLA_MAT = {
    'BlockRailBase': 'circuits', 'BlockFalling': 'sand', 'BlockBush': 'plants', 'BlockFlower': 'plants',
    'BlockLeaves': 'leaves', 'BlockLeavesBase': 'leaves', 'BlockLog': 'wood', 'BlockLadder': 'circuits',
    'BlockTorch': 'circuits', 'BlockCrops': 'plants', 'BlockTallGrass': 'vine', 'BlockDoublePlant': 'plants',
    'BlockVine': 'vine', 'BlockButton': 'circuits', 'BlockLever': 'circuits', 'BlockCarpet': 'carpet',
    'BlockAnvil': 'anvil', 'BlockHopper': 'iron', 'BlockSand': 'sand', 'BlockGravel': 'sand', 'BlockCactus': 'cactus',
    'BlockWeb': 'web', 'BlockSnow': 'snow', 'BlockOre': 'rock', 'BlockStone': 'rock', 'BlockChest': 'wood',
    'BlockBed': 'cloth', 'BlockMushroom': 'plants', 'BlockReed': 'plants', 'BlockFlowerPot': 'circuits',
    'BlockSkull': 'circuits', 'BlockRedstoneWire': 'circuits', 'BlockTNT': 'tnt', 'BlockBeacon': 'glass',
    'BlockCauldron': 'iron', 'BlockFenceGate': 'wood', 'BlockWorkbench': 'wood', 'BlockFurnace': 'rock',
    'BlockDispenser': 'rock', 'BlockJukebox': 'wood', 'BlockNote': 'wood', 'BlockSapling': 'plants',
    'BlockGrass': 'grass', 'BlockDirt': 'ground', 'BlockObsidian': 'rock', 'BlockGlowstone': 'glass',
    'BlockSign': 'wood', 'BlockCompressed': None, 'BlockRotatedPillar': None, 'BlockContainer': None,
    'Block': None, 'BlockSlab': None, 'BlockPane': None, 'BlockDoor': None, 'BlockTrapDoor': None,
    'BlockBreakable': None, 'BlockFence': None, 'BlockPressurePlate': None, 'BlockBasePressurePlate': None,
    'BlockFluidClassic': None, 'BlockFluidBase': None, 'BlockLiquid': None, 'BlockMushroomCap': None,
    'BlockHugeMushroom': None, 'BlockPistonBase': 'piston', 'BlockPortal': 'portal', 'BlockFire': 'fire',
    'BlockIce': 'ice', 'BlockPackedIce': 'packedIce', 'BlockSoulSand': 'sand', 'BlockMycelium': 'grass',
    'BlockFarmland': 'ground', 'BlockSponge': 'sponge', 'BlockGlass': 'glass', 'BlockStainedGlass': 'glass',
    'BlockWall': None, 'BlockStairs': None, 'BlockNetherrack': 'rock', 'BlockClay': 'clay', 'BlockCake': 'cake',
}
def own_material(cls):
    text = class_src.get(cls)
    if text is None:
        return None
    body = text[text.find('class ' + cls):]
    m = re.search(r'\b(?:super|this)\s*\([^;]*?' + MAT_REF, body)
    if m and m.group(0).count(';') == 0:
        return m.group(1) or m.group(2)
    return None
# constructor-level calls (grep of the original: every setHardness/setResistance outside ModBlocks)
CTOR = {
    'BlockGasBase': [('h', 0.0), ('r', 0.0)],
    'BlockBedrockOreTE': [('h', -1.0), ('r', 1_000_000.0)],
    'BlockDepth': [('h', -1.0), ('r', 10.0)],
    'BlockFissure': [('h', -1.0), ('r', 1_000_000.0)],
    'BlockPorous': [('h', 1.5), ('r', 30.0)],
    'BlockBeamBase': [('h', -1.0), ('r', 1_000_000.0)],
    'NTMAnvil': [('h', 5.0), ('r', 100.0)],
    'BlockGraphiteDrilledBase': [('h', 5.0), ('r', 10.0)],
    'RBMKBase': [('h', 3.0), ('r', 30.0)],
    'RBMKConsole': [('h', 3.0), ('r', 30.0)],
    'RBMKCraneConsole': [('h', 3.0), ('r', 30.0)],
    'RBMKMiniPanelBase': [('h', 3.0), ('r', 30.0)],
    'Spotlight': [('h', 0.5)],
}

class S:
    def __init__(self):
        self.h = 0.0
        self.res = 0.0
    def hard(self, h):
        self.h = f32(h)
        h5 = f32(self.h * 5)
        if self.res < h5:
            self.res = h5
    def resist(self, r):
        self.res = f32(f32(r) * 3)

def chain(cls):
    out = []
    while cls:
        out.append(cls)
        cls = parent.get(cls)
    return out[::-1]

def statements(text):
    for m in re.finditer(r'(?m)^\s*(\w+)\s*=\s*new\s+(\w+)\s*(<[^>]*>)?\s*\(', text):
        i, depth, quote = m.end() - 1, 0, False
        while i < len(text):
            c = text[i]
            if quote:
                if c == chr(92): i += 1
                elif c == '"': quote = False
            elif c == '"': quote = True
            elif c == '(': depth += 1
            elif c == ')': depth -= 1
            elif c == ';' and depth == 0: break
            i += 1
        yield m.group(1), m.group(2), text[m.start():i], src.count('\n', 0, m.start()) + 1

CALL = re.compile(r'\.(setHardness|setResistance|setBlockUnbreakable|setBlockName)\(((?:[^()]|\([^()]*\))*)\)')
known = {}
known_mat = {}
rows = []
for field, cls, stmt, line in statements(src):
    s = S()
    for c in chain(cls):
        if c == 'BlockStairs':
            arg = re.match(r'\s*(?:ModBlocks\.)?(\w+)', stmt[stmt.index('(', stmt.index('new')) + 1:]).group(1)
            ps = known[arg]
            s.hard(ps.h)
            s.resist(f32(ps.res / 3))
        for kind, v in CTOR.get(c, []):
            s.hard(v) if kind == 'h' else s.resist(v)
    ctor_end = 0
    name = None
    raw = None
    for m in CALL.finditer(stmt):
        meth, arg = m.groups()
        if meth == 'setBlockName':
            name = arg.strip().strip('"')
        elif meth == 'setBlockUnbreakable':
            s.hard(-1.0)
        elif meth == 'setHardness':
            s.hard(num(arg))
        else:
            raw = arg.strip()
            s.resist(num(arg))
    known[field] = s
    material = None
    m = re.search(MAT_REF, stmt[stmt.index('(', stmt.index('new')):])
    if m:
        material = m.group(1) or m.group(2)
    else:
        for c in chain(cls)[::-1]:
            if c == 'BlockStairs' or c == 'BlockWall':
                arg = re.match(r'\s*(?:ModBlocks\.)?(\w+)', stmt[stmt.index('(', stmt.index('new')) + 1:]).group(1)
                material = known_mat.get(arg)
                break
            material = own_material(c) or VANILLA_MAT.get(c)
            if material:
                break
    known_mat[field] = material
    tool = re.search(r'new\s+Rail\w*\s*\(\s*"(\w+)"\s*,\s*(\d+)', stmt)
    variants = None
    if cls in ('BlockMultiSlab', 'BlockMultiSlabMeta'):
        # BlockMultiSlab.getExplosionResistance(entity, world, ...) -> slabMaterials[(meta & 7) % n].getExplosionResistance(entity)
        args = stmt[stmt.index('(', stmt.index('new')) + 1:]
        args = args[:args.index(').setBlockName')]
        if cls == 'BlockMultiSlabMeta':
            args = re.search(r'\{([^}]*)\}', args).group(1)
            parts = [a.strip() for a in args.split(',')]
        else:
            parts = [a.strip() for a in args.split(',')][2:]
        variants = [p.replace('ModBlocks.', '') for p in parts]
    eff = s.res if s.res == float('inf') else f32(s.res / 5)
    if name is None:
        continue  # Forge Fluid objects (mud_fluid etc.), not blocks
    rows.append(dict(field=field, id=name, cls=cls, line=line, resistance_arg=raw,
                     hardness=s.h, variants=variants, material=material,
                     harvest_tool=(tool.group(1) if tool else None), expected=('Infinity' if eff == float('inf') else eff)))
# Agreed port replacements of the excluded depth ores (user decision 2026-11-16) keep the original ore's values.
by_id = {r['id']: r for r in rows}
for port_id, original in (('ore_borax', 'ore_depth_borax'), ('ore_zirconium', 'ore_depth_zirconium'),
                          ('ore_nether_neodymium', 'ore_depth_nether_neodymium')):
    rows.append(dict(by_id[original], id=port_id, field=port_id, alias_of=original))
OUT.write_text(json.dumps(rows, indent=1) + '\n', encoding='utf-8', newline='\n')
print(len(rows), 'blocks;', sum(r['resistance_arg'] is not None for r in rows), 'with setResistance')
