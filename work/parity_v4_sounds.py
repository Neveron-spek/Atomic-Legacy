"""V4: step sound of every original HBM block (936c60c4).

1.7.10 Block(): stepSound = soundTypeStone (no Material default). Constructors that change it: vanilla BlockLeaves
(grass), BlockLog (wood), BlockStairs/BlockWall (copy the parent block's stepSound); HBM NTMAnvil (anvil),
BlockGraphiteDrilledBase (metal). ModBlocks chains setStepSound(...) afterwards.
Output: src/main/resources/legacy/reinhardtshbm/parity-v4-sounds.json {id: sound}; sound names map to 1.21
SoundType in blocks/LegacyStepSound (stone/piston -> STONE, metal -> METAL, cloth -> WOOL, ...).
"""
import json, re
from pathlib import Path

ROOT = Path(__file__).resolve().parent
ORIG = Path('C:/Users/Admin/Documents/ChatGPT/NeoNTM/work/original/src/main/java')
OUT = ROOT.parent / 'src/main/resources/legacy/reinhardtshbm/parity-v4-sounds.json'

src = re.sub(r'/\*.*?\*/', '', (ORIG / 'com/hbm/blocks/ModBlocks.java').read_text(encoding='utf-8'), flags=re.S)
src = re.sub(r'//[^\n]*', '', src)

parent = {}
for p in ORIG.rglob('*.java'):
    text = p.read_text(encoding='utf-8', errors='ignore')
    for m in re.finditer(r'class\s+(\w+)(?:<[^>{]*>)?\s+extends\s+(\w+)', text):
        parent.setdefault(m.group(1), m.group(2))

CTOR = {'BlockLeaves': 'grass', 'BlockLog': 'wood', 'NTMAnvil': 'anvil', 'BlockGraphiteDrilledBase': 'metal'}
COPY = ('BlockStairs', 'BlockWall')
SOUND = {
    'Block.soundTypeStone': 'stone', 'Block.soundTypePiston': 'stone', 'Block.soundTypeWood': 'wood',
    'Block.soundTypeGravel': 'gravel', 'Block.soundTypeGrass': 'grass', 'Block.soundTypeMetal': 'metal',
    'Block.soundTypeGlass': 'glass', 'Block.soundTypeCloth': 'cloth', 'Block.soundTypeSand': 'sand',
    'Block.soundTypeSnow': 'snow', 'Block.soundTypeLadder': 'ladder', 'Block.soundTypeAnvil': 'anvil',
    'ModSoundTypes.grate': 'grate', 'ModSoundTypes.pipe': 'pipe', 'ModSoundTypes.flesh': 'flesh',
    'ModSoundTypes.platemetal': 'platemetal',
}

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
        yield m.group(1), m.group(2), text[m.start():i]

known = {}
table = {}
fields = {}
explicit = 0
for field, cls, stmt in statements(src):
    sound = 'stone'
    for c in chain(cls):
        if c in COPY:
            arg = re.match(r'\s*(?:ModBlocks\.)?(\w+)', stmt[stmt.index('(', stmt.index('new')) + 1:]).group(1)
            sound = known[arg]
        if c in CTOR:
            sound = CTOR[c]
    for m in re.finditer(r'\.setStepSound\(\s*([\w.]+)\s*\)', stmt):
        sound = SOUND[m.group(1)]
        explicit += 1
    known[field] = sound
    name = re.search(r'\.setBlockName\(\s*"([^"]+)"', stmt)
    if name is None:
        continue  # Forge Fluid objects
    table.setdefault(name.group(1), sound)
    fields.setdefault(field, sound)
missing = [s for s in re.findall(r'setStepSound\(\s*([\w.]+)', src) if s not in SOUND]
assert not missing, missing
for port_id, original in (('ore_borax', 'ore_depth_borax'), ('ore_zirconium', 'ore_depth_zirconium'),
                          ('ore_nether_neodymium', 'ore_depth_nether_neodymium')):
    table[port_id] = table[original]
for field, sound in fields.items():  # LegacyBlastResistance also resolves port ids by ModBlocks field name
    table.setdefault(field, sound)
OUT.write_text(json.dumps(dict(sorted(table.items())), indent=1) + '\n', encoding='utf-8', newline='\n')
counts = {}
for s in table.values():
    counts[s] = counts.get(s, 0) + 1
print(len(table), 'blocks;', explicit, 'setStepSound;', counts)
