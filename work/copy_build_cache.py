from pathlib import Path
import shutil

root = Path(__file__).resolve().parent
old = Path(r'C:\Users\Admin\Documents\Codex\2026-10-30\hbm-s-ntm-minecraft-1-21\work\gradle-cache')
new = root / 'gradle-cache'
count = 0
for part in ['caches/modules-2', 'caches/neoformruntime']:
    source = old / part
    for file in source.rglob('*'):
        if not file.is_file() or file.suffix in {'.lock', '.lck'}:
            continue
        target = new / part / file.relative_to(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, target)
        count += 1
print(f'Reused {count} dependency-cache files in the new workspace; no old-cache writes.')
