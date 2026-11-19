"""Align source packages with the original HBM sections; leave modern asset paths intact."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
base = root / 'src/main/java/com/reinhardt/hbm'
moves = {
    'block': 'blocks', 'item': 'items', 'blockentity': 'tileentity',
    'menu': 'inventory/container', 'recipe': 'inventory/recipes',
    'client/screen': 'inventory/gui', 'client/render': 'render',
}
files = list((root / 'src').rglob('*'))
files += [root / 'build.gradle']
for directory in ['tools', 'scripts']:
    files += list((root / directory).rglob('*'))
files += list((root / 'work').glob('*.py'))
changes = []
for path in files:
    if not path.is_file() or path.suffix not in {'.java', '.json', '.gradle', '.py', '.ps1'} or path == Path(__file__).resolve():
        continue
    text = path.read_text(encoding='utf-8-sig')
    updated = text
    for source, target in moves.items():
        old = 'com.reinhardt.hbm.' + source.replace('/', '.')
        new = 'com.reinhardt.hbm.' + target.replace('/', '.')
        updated = re.sub(re.escape(old) + r'''(?=[.;"'\s]|$)''', new, updated)
        updated = updated.replace('com/reinhardt/hbm/' + source + '/', 'com/reinhardt/hbm/' + target + '/')
        updated = updated.replace('com\\reinhardt\\hbm\\' + source.replace('/', '\\') + '\\',
                                  'com\\reinhardt\\hbm\\' + target.replace('/', '\\') + '\\')
    if updated != text:
        path.write_text(updated, encoding='utf-8', newline='')
        changes.append(path.relative_to(root).as_posix())
relocated = 0
for source, target in moves.items():
    source_path, target_path = (base / source).resolve(), (base / target).resolve()
    assert source_path.is_relative_to(base.resolve()) and target_path.is_relative_to(base.resolve())
    if not source_path.exists():
        continue
    assert not target_path.exists(), target_path
    relocated += len(list(source_path.rglob('*.java')))
    target_path.parent.mkdir(parents=True, exist_ok=True)
    source_path.rename(target_path)
report = {'moves': moves, 'java_files_in_aligned_sections': sum(len(list((base / target).rglob('*.java'))) for target in moves.values())}
(root / 'work/source-layout.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps({'java_files_moved': relocated, 'updated_files': len(changes)}))
