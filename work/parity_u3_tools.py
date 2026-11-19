"""U3: vanilla tool tags of every mod block from the original Material (936c60c4, Forge 10.13 tools).

ItemPickaxe efficient/harvests: Material rock/iron/anvil (any tier, no setHarvestLevel in ModBlocks) -> mineable/pickaxe,
no needs_*_tool. ItemAxe: wood/plants/vine -> mineable/axe. ItemSpade: vanilla blocks only -> no mod block in
mineable/shovel. ItemSword 1.5: plants/vine/coral/leaves/gourd -> sword_efficient. Rails: setHarvestLevel(tool, 0).
Blocks without an original counterpart keep their current membership.
Input: run-building-audit/parity-u3-hardness.tsv (dump of ParityU3Diagnostics).
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TAGS = ROOT / 'src/main/resources/data/minecraft/tags/block'
DUMP = ROOT / 'run-building-audit/parity-u3-hardness.tsv'
TABLE = ROOT / 'src/main/resources/legacy/reinhardtshbm/parity-t3-resistance.json'

rows = {}
for r in json.loads(TABLE.read_text(encoding='utf-8')):
    rows.setdefault(r['field'], r)
material = {}
for line in DUMP.read_text(encoding='utf-8').splitlines()[1:]:
    if line.startswith('#'):
        continue
    f = line.split('\t')
    if f[1] == '-':
        continue
    material[f[0]] = (f[2], rows[f[1]].get('harvest_tool'))

def want(tag, mat, tool):
    if tag == 'mineable/pickaxe':
        return mat in ('rock', 'iron', 'anvil') or tool == 'pickaxe'
    if tag == 'mineable/axe':
        return mat in ('wood', 'plants', 'vine') or tool == 'axe'
    if tag == 'mineable/shovel':
        return tool == 'shovel'
    if tag == 'sword_efficient':
        return mat in ('plants', 'vine', 'coral', 'leaves', 'gourd')
    return False  # needs_*_tool, mineable/hoe

changes = {}
for tag in ('mineable/pickaxe', 'mineable/axe', 'mineable/shovel', 'mineable/hoe', 'sword_efficient',
            'needs_stone_tool', 'needs_iron_tool', 'needs_diamond_tool'):
    path = TAGS / (tag + '.json')
    values = json.loads(path.read_text(encoding='utf-8-sig'))['values'] if path.exists() else []
    out = []
    for v in values:
        key = v if isinstance(v, str) else v['id']
        ident = key.split(':', 1)[1] if key.startswith('reinhardtshbm:') else None
        if ident in material and not want(tag, *material[ident]):
            changes.setdefault(tag, [0, 0])[1] += 1
            continue
        out.append(v)
    present = {(v if isinstance(v, str) else v['id']) for v in out}
    for ident, (mat, tool) in sorted(material.items()):
        if want(tag, mat, tool) and 'reinhardtshbm:' + ident not in present:
            out.append('reinhardtshbm:' + ident)
            changes.setdefault(tag, [0, 0])[0] += 1
    if out or path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({'replace': False, 'values': out}, indent=2) + '\n', encoding='utf-8', newline='\n')
for tag, (added, removed) in changes.items():
    print(f'{tag}: +{added} -{removed}')
