"""3-way merge of an agent copy (NeoNTM-wt/<cN>) into the main project, base = NeoNTM-wt/base.
Lang JSON, sounds.json and creative-tabs.properties come from the agent's work/integration fragments.
Usage: python -I merge_batch.py c1 [--dry]"""
import filecmp, json, os, shutil, subprocess, sys

WT = 'C:/Users/Admin/Documents/ChatGPT/NeoNTM-wt'
MAIN = 'C:/Users/Admin/Documents/ChatGPT/NeoNTM'
agent = sys.argv[1]
dry = '--dry' in sys.argv
A, B = f'{WT}/{agent}', f'{WT}/base'
LANG_DIR = 'src/main/resources/assets/atomiclegacy/lang/'
SKIP = {'src/main/resources/assets/atomiclegacy/sounds.json',
        'src/main/resources/com/atomiclegacy/registry/creative-tabs.properties'}
report = {'new': [], 'fast': [], 'merged': [], 'conflict': [], 'same': [], 'deleted': [], 'skipped': []}

def rel_files(root):
    out = set()
    for d, _, fs in os.walk(os.path.join(root, 'src')):
        for f in fs:
            out.add(os.path.relpath(os.path.join(d, f), root).replace('\\', '/'))
    return out

af, bf = rel_files(A), rel_files(B)
for r in sorted(bf - af):
    report['deleted'].append(r)
for r in sorted(af):
    a = f'{A}/{r}'; m = f'{MAIN}/{r}'; b = f'{B}/{r}'
    if r in SKIP or r.startswith(LANG_DIR):
        if r in bf and not filecmp.cmp(a, b, shallow=False):
            report['skipped'].append(r)
        continue
    if r not in bf:
        if not os.path.exists(m):
            report['new'].append(r)
            if not dry:
                os.makedirs(os.path.dirname(m), exist_ok=True); shutil.copyfile(a, m)
        elif filecmp.cmp(a, m, shallow=False):
            report['same'].append(r)
        else:
            report['conflict'].append(r + ' (new in agent and in main, different)')
        continue
    if filecmp.cmp(a, b, shallow=False):
        continue
    if not os.path.exists(m):
        report['conflict'].append(r + ' (deleted in main)'); continue
    if filecmp.cmp(m, b, shallow=False):
        report['fast'].append(r)
        if not dry: shutil.copyfile(a, m)
        continue
    if filecmp.cmp(m, a, shallow=False):
        report['same'].append(r); continue
    if r.endswith(('.png', '.ogg', '.obj')):
        report['conflict'].append(r + ' (binary)'); continue
    tmp = m + '.merge'
    shutil.copyfile(m, tmp)
    rc = subprocess.run(['git', 'merge-file', '-L', 'main', '-L', 'base', '-L', agent, tmp, b, a]).returncode
    if rc == 0:
        report['merged'].append(r)
        if not dry: shutil.move(tmp, m)
        else: os.remove(tmp)
    else:
        report['conflict'].append(f'{r} ({rc} conflicts, see {tmp})')
        if dry: os.remove(tmp)

# lang JSON / sounds.json / creative-tabs.properties: what the agent added relative to base (fragments only cross-checked)
def json_delta(rel):
    a, b = f'{A}/{rel}', f'{B}/{rel}'
    if not os.path.exists(a): return {}
    ad = json.load(open(a, encoding='utf-8')); bd = json.load(open(b, encoding='utf-8')) if os.path.exists(b) else {}
    return {k: v for k, v in ad.items() if bd.get(k) != v}
def apply_json(rel, add):
    dst = f'{MAIN}/{rel}'
    data = json.load(open(dst, encoding='utf-8'))
    n = 0
    for k, v in add.items():
        if k in data and data[k] != v:
            base = json.load(open(f'{B}/{rel}', encoding='utf-8')) if os.path.exists(f'{B}/{rel}') else {}
            if base.get(k) != data[k]:
                report['conflict'].append(f'{rel}: key {k} changed in main and agent'); continue
        if data.get(k) != v:
            data[k] = v; n += 1
    if n and not dry:
        with open(dst, 'w', encoding='utf-8', newline=chr(10)) as f:
            f.write(json.dumps(data, indent=2, ensure_ascii=False).replace(chr(0xc3), chr(92) + 'u00c3') + chr(10))  # validateLangFiles flags a raw A-tilde as mojibake
    return n
counts = {}
for f in sorted(os.listdir(f'{A}/{LANG_DIR}')):
    counts[f] = apply_json(LANG_DIR + f, json_delta(LANG_DIR + f))
counts['sounds.json'] = apply_json('src/main/resources/assets/atomiclegacy/sounds.json', json_delta('src/main/resources/assets/atomiclegacy/sounds.json'))
frag = f'{A}/work/integration'
if os.path.isdir(f'{frag}/lang'):
    for f in os.listdir(f'{frag}/lang'):
        miss = [k for k in json.load(open(f'{frag}/lang/{f}', encoding='utf-8')) if k not in json_delta(LANG_DIR + f)]
        if miss: report['conflict'].append(f'fragment {f}: {len(miss)} keys not in the agent lang file, e.g. {miss[:3]}')
PR = 'src/main/resources/com/atomiclegacy/registry/creative-tabs.properties'
bl = set(open(f'{B}/{PR}', encoding='utf-8').read().splitlines())
al = [l for l in open(f'{A}/{PR}', encoding='utf-8').read().splitlines() if l not in bl and '=' in l and not l.startswith('#')]
have = open(f'{MAIN}/{PR}', encoding='utf-8').read()
keys = {l.split('=')[0] for l in have.splitlines() if '=' in l and not l.startswith('#')}
new = [l for l in al if l.split('=')[0] not in keys]
counts['creative-tabs'] = len(new)
if new and not dry:
    with open(f'{MAIN}/{PR}', 'a', encoding='utf-8', newline=chr(10)) as f:
        if not have.endswith(chr(10)): f.write(chr(10))
        f.write(chr(10).join(new) + chr(10))
for k, v in report.items():
    print(k, len(v))
    if k in ('conflict', 'deleted', 'skipped', 'merged'):
        for x in v: print('   ', x)
print('fragments', counts)
