from pathlib import Path
import hashlib
import json
import shutil

destination = Path(__file__).resolve().parents[1]
previous = Path(r'C:\Users\Admin\Documents\Codex\2026-10-30\hbm-s-ntm-minecraft-1-21\work')
source = previous / 'reinhardt-port'
assert destination == Path.cwd().resolve()
copied = []

def copy_file(a, b):
    b.parent.mkdir(parents=True, exist_ok=True)
    if b.exists() and a.read_bytes() != b.read_bytes():
        raise RuntimeError(f'Destination conflict: {b}')
    shutil.copy2(a, b)
    if hashlib.sha256(a.read_bytes()).digest() != hashlib.sha256(b.read_bytes()).digest():
        raise RuntimeError(f'Copy verification failed: {b}')
    copied.append(str(b.relative_to(destination)))

def copy_directory(a, b, ignored=()):
    for file in a.rglob('*'):
        relative = file.relative_to(a)
        if any(part in ignored for part in relative.parts) or not file.is_file():
            continue
        copy_file(file, b / relative)

for name in ['src', 'gradle', 'scripts', 'tools', 'docs', 'local-deps']:
    if (source / name).exists(): copy_directory(source / name, destination / name)
for name in ['.editorconfig', '.gitignore', 'build.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat', 'LICENSE', 'settings.gradle']:
    if (source / name).is_file(): copy_file(source / name, destination / name)
copy_directory(previous / 'original', destination / 'work/original', {'.git', '.gradle', 'build'})
for name in ['audit_building.py', 'audit_crafting_source.py', 'audit_resource_closure.py', 'check_building_assets.py',
             'building-audit.json', 'building-assets-audit.json', 'crafting-source-audit.json', 'resource-closure-audit.json',
             'asset-restoration.json', 'ctm-restoration.json', 'property-adaptation.json', 'resource-path-adaptations.json', 'STATE.md']:
    if (previous / name).is_file(): copy_file(previous / name, destination / 'work' / name)
for script in (destination / 'work').glob('*.py'):
    if script.name == 'migrate_project.py': continue
    text = script.read_text(encoding='utf-8-sig')
    text = text.replace("root / 'reinhardt-port'", 'root.parent')
    text = text.replace("ROOT / 'reinhardt-port/", "ROOT.parent / '")
    text = text.replace("root / 'reinhardt-port/", "root.parent / '")
    script.write_text(text, encoding='utf-8')
for name in ['release-check.log', 'client-check.log']:
    copy_file(previous / name, destination / 'work/prior-checks' / name)
copy_file(source / 'run-building-audit/building-audit.txt', destination / 'work/prior-checks/building-audit.txt')
(destination / 'work/migration-manifest.json').write_text(json.dumps({'source': str(source), 'files_verified_before_path_adaptation': copied}, indent=2))
print(json.dumps({'verified_files': len(copied), 'project': str(destination)}))
