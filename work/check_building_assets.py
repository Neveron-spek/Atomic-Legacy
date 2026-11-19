from pathlib import Path
import hashlib
import json

root = Path(__file__).resolve().parent
old = root / 'original/src/main/resources/assets/hbm'
new = root.parent / 'src/main/resources/assets/reinhardtshbm'
rows = json.loads((root / 'building-audit.json').read_text())['blocks']
equal, different, missing = [], [], []
for row in rows:
    name = row['texture']
    if not name:
        continue
    a = old / f'textures/blocks/{name}.png'
    b = new / f'textures/block/{name.lower()}.png'
    if not a.is_file():
        continue  # Original renderer selects a suffix or an enum texture.
    if not b.is_file():
        missing.append(name)
    elif a.read_bytes() != b.read_bytes():
        different.append(name)
    else:
        equal.append(name)
data = dict(byte_identical=len(equal), different=different, missing=missing)
(root / 'building-assets-audit.json').write_text(json.dumps(data, indent=2))
print(json.dumps(data))
