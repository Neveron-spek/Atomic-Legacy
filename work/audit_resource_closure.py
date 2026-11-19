from pathlib import Path
import json
import re
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent / 'pythonlibs'))
from PIL import Image

root = Path(__file__).resolve().parent
project = root.parent
assets = project / 'src/main/resources/assets/reinhardtshbm'
source_assets = root / 'original/src/main/resources/assets/hbm'
fixture = json.loads((assets.parent.parent / 'legacy/reinhardtshbm/building-scope.json').read_text())
queue, visited, textures, missing = [], set(), set(), []
for row in fixture:
    for path in [assets / f'blockstates/{row["id"]}.json', assets / f'models/item/{row["id"]}.json']:
        if path.is_file(): queue.append(path)
        elif '/blockstates/' in path.as_posix(): missing.append(path.relative_to(assets).as_posix())

def visit(obj):
    if isinstance(obj, dict):
        for key, value in obj.items():
            if key == 'textures' and isinstance(value, dict):
                for texture in value.values():
                    if texture.startswith('reinhardtshbm:'):
                        textures.add(texture.split(':')[1])
            elif key in {'model', 'parent', 'mtl_override'} and isinstance(value, str) and value.startswith('reinhardtshbm:'):
                name = value.split(':')[1]
                file = assets / (name if name.endswith(('.obj', '.mtl')) else f'models/{name}.json')
                queue.append(file)
            visit(value)
    elif isinstance(obj, list):
        for item in obj: visit(item)

while queue:
    path = queue.pop()
    if path in visited: continue
    visited.add(path)
    if not path.is_file():
        missing.append(path.relative_to(assets).as_posix())
        continue
    text = path.read_text(encoding='utf-8-sig')
    if path.suffix == '.json':
        visit(json.loads(text))
    elif path.suffix == '.obj':
        for name in re.findall(r'^mtllib\s+(\S+)', text, re.M):
            file = path.parent / name
            if file.is_file(): queue.append(file)
    else:
        textures.update(re.findall(r'^map_Kd\s+reinhardtshbm:(\S+)', text, re.M))

differences, identical, unmapped = [], 0, []
for texture in sorted(textures):
    target = assets / f'textures/{texture}.png'
    if not target.is_file():
        missing.append(str(target.relative_to(assets)))
        continue
    relative = texture.replace('block/', 'blocks/', 1).replace('item/', 'items/', 1)
    # Registry/model names differ from the pinned source texture names.
    relative = {
        'blocks/block_meteor': 'blocks/meteor',
        'blocks/block_meteor_broken': 'blocks/meteor_crushed',
        'blocks/block_meteor_cobble': 'blocks/meteor_cobble',
        'blocks/block_meteor_molten': 'blocks/meteor_cobble_molten',
        'blocks/block_meteor_treasure': 'blocks/meteor_treasure',
        'blocks/stone_gneiss': 'blocks/stone_gneiss_var',
    }.get(relative, relative)
    source = source_assets / f'textures/{relative}.png'
    if not source.is_file():
        unmapped.append(texture)
        continue
    a, b = Image.open(source).convert('RGBA'), Image.open(target).convert('RGBA')
    if a.size == b.size and a.tobytes() == b.tobytes():
        identical += 1
    else:
        differences.append(dict(texture=texture, original=list(a.size), port=list(b.size),
                                vertical_flip=a.size == b.size and a.transpose(Image.Transpose.FLIP_TOP_BOTTOM).tobytes() == b.tobytes()))
report = dict(models=len(visited), textures=len(textures), pixel_identical=identical,
              missing=sorted(set(missing)), differences=differences, renamed_or_converted=unmapped,
              placeholder_references=[str(p.relative_to(assets)) for p in visited if p.is_file() and 'legacy_placeholder' in p.read_text(encoding='utf-8-sig')])
(root / 'resource-closure-audit.json').write_text(json.dumps(report, indent=2))
print(json.dumps(report))
