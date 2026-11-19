"""Source inventory for the agreed HbmMods building port; no guessed registrations."""
import collections
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
OLD = ROOT / 'original/src/main'
NEW = ROOT.parent / 'src/main'
source = (OLD / 'java/com/hbm/blocks/ModBlocks.java').read_text(encoding='utf-8')
source = re.sub(r'/\*.*?\*/', '', source, flags=re.S)
target = (NEW / 'java/com/reinhardt/hbm/registry/HbmBlocks.java').read_text(encoding='utf-8')
fields = set(re.findall(r'public static final DeferredBlock<[^;=]+>\s+(\w+)\s*=', target))
rows = []
excluded = {'stone_depth', 'stone_depth_nether', 'depth_brick', 'depth_tiles', 'depth_nether_brick', 'depth_nether_tiles'}
for line in source.splitlines():
    match = re.match(r'\s*(\w+) = new (\w+)\((.*)', line)
    if not match:
        continue
    name, cls, _ = match.groups()
    # The contiguous original construction/decorative section, plus building fittings.
    start = source.index('\t\tdeco_titanium =')
    end = source.index('\t\tbroadcaster_pc =')
    in_section = line.strip() in source[start:end]
    masonry = {'stone_gneiss', 'gneiss_brick', 'gneiss_tile', 'gneiss_chiseled', 'basalt', 'basalt_smooth',
               'basalt_brick', 'basalt_polished', 'basalt_tiles', 'stone_depth', 'stone_depth_nether',
               'depth_brick', 'depth_tiles', 'depth_nether_brick', 'depth_nether_tiles'}
    fittings = name in masonry | {'chain', 'trapdoor_steel', 'boxcar', 'boat', 'fence_metal'} or name.startswith(('ladder_', 'glass_'))
    if not (in_section or fittings):
        continue
    def number(method):
        matches = re.findall(r'\.' + method + r'\(([^)]+)\)', line)
        return matches[-1] if matches else None
    texture = re.search(r'setBlockTextureName\(RefStrings.MODID \+ ":([^"]+)"', line)
    target_name = {'chain': 'DUNGEON_CHAIN'}.get(name, name.upper())
    rows.append(dict(id=name, target_field=target_name, original_class=cls, registered=target_name in fields, excluded=name in excluded,
                     hardness=number('setHardness'), resistance_argument=number('setResistance'),
                     texture=texture.group(1) if texture else None,
                     visible='setCreativeTab(MainRegistry.blockTab)' in line))
duplicates = [key for key, count in collections.Counter(re.findall(
    r'public static final DeferredBlock<[^;=]+>\s+\w+\s*=\s*\w+\("([^"]+)"', target)).items() if count > 1]
report = dict(original_commit='936c60c4c07dc10aef81f75d800ad04282f08e62',
              port_commit='b9b49ea7d24b39ddb7172f10a219b2f9fc695b3a',
              blocks=rows, excluded=sorted(excluded), missing_fields=[r['id'] for r in rows if not r['registered'] and not r['excluded']],
              duplicate_literal_ids=duplicates)
(ROOT / 'building-audit.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(dict(block_families=sum(not r['excluded'] for r in rows), excluded=len(excluded), missing=report['missing_fields'], duplicate_ids=duplicates)))
