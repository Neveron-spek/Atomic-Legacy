"""Restore/check the 21 circuit variants against the pinned 1.7.10 source."""
from pathlib import Path
import argparse
import json
import re

root = Path(__file__).resolve().parents[1]
old = root / 'work/original/src/main'
assets = root / 'src/main/resources/assets/reinhardtshbm'
recipes = root / 'src/main/resources/data/reinhardtshbm/recipe'
apply = argparse.ArgumentParser()
apply.add_argument('--check', action='store_true')
check = apply.parse_args().check
errors, changed, verified = [], [], []

def read(path):
    return path.read_text(encoding='utf-8-sig')

def write(path, content):
    content = content if isinstance(content, bytes) else content.encode('utf-8')
    if path.exists() and path.read_bytes() == content:
        return
    changed.append(path.relative_to(root).as_posix())
    if check:
        errors.append('Different/missing: ' + str(path.relative_to(root)))
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)

def dump(data):
    return json.dumps(data, ensure_ascii=False, indent=2) + '\n'

source = read(old / 'java/com/hbm/items/machine/ItemCircuit.java')
order = [s.lower() for s in re.findall(r'list.add.*?EnumCircuitType\.(\w+)', source)]
ids = ['circuit_' + s for s in order]
assert len(set(ids)) == 21
for variant, item in zip(order, ids):
    src = old / f'resources/assets/hbm/textures/items/circuit.{variant}.png'
    dst = assets / f'textures/item/{item}.png'
    write(dst, src.read_bytes())
    meta = src.with_suffix('.png.mcmeta')
    if meta.exists():
        write(dst.with_suffix('.png.mcmeta'), meta.read_bytes())
    model = json.loads(read(assets / f'models/item/{item}.json'))
    assert model['parent'] == 'minecraft:item/generated'
    assert model['textures']['layer0'] == f'reinhardtshbm:item/{item}'
for locale in ['en_US', 'ru_RU']:
    original = dict(line.split('=', 1) for line in read(old / f'resources/assets/hbm/lang/{locale}.lang').splitlines() if '=' in line)
    path = assets / f'lang/{locale.lower()}.json'
    lang = json.loads(read(path))
    for variant, item in zip(order, ids):
        value = original[f'item.circuit.{variant}.name']
        assert '\ufffd' not in value
        lang[f'item.reinhardtshbm.{item}'] = value
    write(path, dump(lang))

# Preserve all existing registry IDs, with the source getSubItems ordering.
item_source = root / 'src/main/java/com/reinhardt/hbm/registry/HbmItems.java'
text = read(item_source)
declarations = re.findall(r'^    public static final DeferredItem<Item> CIRCUIT_\w+ = material\(MISC_MATERIALS, "(circuit_\w+)"\);$', text, re.M)
assert set(declarations) == set(ids)
pattern = r'(?m)^    public static final DeferredItem<Item> CIRCUIT_\w+ = material\(MISC_MATERIALS, "circuit_\w+"\);\n'
block = ''.join(f'    public static final DeferredItem<Item> {item.upper()} = material(MISC_MATERIALS, "{item}");\n' for item in ids)
start = re.search(pattern, text).start()
text = re.sub(pattern, '', text)
write(item_source, text[:start] + block + text[start:])

materials = {'W': 'tungsten', 'CARBON': 'carbon', 'CU': 'copper', 'AL': 'aluminium', 'NB': 'niobium', 'TA': 'tantalium', 'GOLD': 'gold', 'PB': 'lead', 'RUBBER': 'rubber', 'ANY_HARDPLASTIC': 'hard_plastic', 'ANY_PLASTIC': 'plastic'}
shapes = {'wireFine': 'wires', 'plate': 'plates', 'dust': 'dusts', 'nugget': 'nuggets', 'ingot': 'ingots'}

def ingredient(expr):
    expr = expr.strip()
    if expr == 'KEY_ANYPANE':
        return {'tag': 'c:glass_panes'}
    m = re.fullmatch(r'ModItems\.(\w+)', expr)
    if m:
        return {'item': 'reinhardtshbm:' + m[1]}
    m = re.fullmatch(r'DictFrame.fromOne\(ModItems.circuit, EnumCircuitType\.(\w+)\)', expr)
    if m:
        return {'item': 'reinhardtshbm:circuit_' + m[1].lower()}
    m = re.fullmatch(r'(\w+)\.(\w+)\(\)', expr)
    assert m, expr
    return {'tag': 'c:' + shapes[m[2]] + '/' + materials[m[1]]}

# All twelve source crafting outputs, including both original wire alternatives.
craft_source = read(old / 'java/com/hbm/main/CraftingManager.java')
names = ['circuit_vacuum_tube', 'circuit_vacuum_tube_from_carbon_wire', 'circuit_numitron',
         'circuit_capacitor', 'circuit_capacitor_from_copper_wire', 'circuit_capacitor_from_aluminium_dust',
         'circuit_capacitor_from_aluminium_dust_copper_wire', 'circuit_capacitor_tantalium_from_aluminium_wire',
         'circuit_capacitor_tantalium_from_copper_wire', 'circuit_pcb_from_plate_copper', 'circuit_pcb_from_plate_gold', 'circuit_controller_chassis']
matches = list(re.finditer(r'addRecipeAuto\(DictFrame.fromOne\(ModItems.circuit, EnumCircuitType\.(\w+)(?:, (\d+))?\), new Object\[\] \{ (.*?) \}\);', craft_source))
assert len(matches) == len(names)
expected_paths = set()
for name, m in zip(names, matches):
    args = m[3]
    pattern = [json.loads(s) for s in re.findall(r'"[^"\n]*"', args)]
    keys = {k: ingredient(v) for k, v in re.findall(r"'([^']+)', (.*?)(?=, '[^']+',|$)", args)}
    data = {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': keys,
            'result': {'id': 'reinhardtshbm:circuit_' + m[1].lower(), 'count': int(m[2] or 1)}}
    path = recipes / (name + '.json')
    expected_paths.add(path)
    write(path, dump(data))
    verified.append({'recipe': name, 'source': 'main/CraftingManager.java', 'line': craft_source.count('\n', 0, m.start()) + 1})
removed = []
for path in recipes.glob('*.json'):
    data = json.loads(read(path))
    if data.get('type', '').startswith('minecraft:crafting') and data.get('result', {}).get('id', '').removeprefix('reinhardtshbm:') in ids and path not in expected_paths:
        removed.append(path.relative_to(recipes).as_posix())
        if check:
            errors.append('Non-source crafting recipe: ' + path.name)
        else:
            path.unlink()

# Transcribe all nine soldering outputs and both branches of the six LBSM recipes.
solder_source = read(old / 'java/com/hbm/inventory/recipes/SolderingRecipes.java')
matches = list(re.finditer(r'new SolderingRecipe\(new ItemStack\(ModItems.circuit, 1, EnumCircuitType\.(\w+)\.ordinal\(\)\), (\d+), ([\d_]+),(.*?)\n\s*\)\);', solder_source, re.S))
assert len(matches) == 9
lbsm = {'type': 'reinhardtshbm:less_bullshit_mode', 'feature': 'simple_crafting'}
no528 = {'type': 'neoforge:not', 'value': {'type': 'reinhardtshbm:enable_528_mode'}}
for m in matches:
    variant = m[1].lower()
    body = m[4]
    for simple in ([False, True] if 'lbsm ?' in body else [False]):
        data = {'type': 'reinhardtshbm:soldering_station', 'group': 'soldering.computer' if variant.startswith('controller') else 'soldering.circuit',
                'duration': int(m[2]), 'consumption': int(m[3].replace('_', ''))}
        conditions = [no528] if variant.startswith('controller') else []
        if 'lbsm ?' in body:
            conditions += [lbsm if simple else {'type': 'neoforge:not', 'value': lbsm}]
        if conditions:
            data['neoforge:conditions'] = conditions
        fluid = re.search(r'new FluidStack\(Fluids\.(\w+), ([\d_]+)\)', body)
        if fluid:
            data['fluid'] = {'fluid': fluid[1].lower(), 'amount': int(fluid[2].replace('_', ''))}
        groups = re.findall(r'new AStack\[\] \{(.*?)\}', body, re.S)
        assert len(groups) == 3
        for key, group in zip(['toppings', 'pcb', 'solder'], groups):
            entries = []
            for entry in re.finditer(r'new (ComparableStack|OreDictStack)\(((?:[^()]|\([^()]*\))*)\)', group.strip(), re.S):
                args = entry[2].strip()
                if entry[1] == 'ComparableStack':
                    circuit = re.fullmatch(r'ModItems.circuit, (.*?), EnumCircuitType\.(\w+)', args)
                    if circuit:
                        ing, count = {'item': 'reinhardtshbm:circuit_' + circuit[2].lower()}, circuit[1]
                    else:
                        item, _, count = args.partition(', ')
                        ing, count = ingredient(item), count or '1'
                else:
                    item, _, count = args.partition(', ')
                    ing, count = ingredient(item), count or '1'
                ternary = re.fullmatch(r'lbsm \? (\d+) : (\d+)', count)
                count = int(ternary[1 if simple else 2] if ternary else count)
                entries.append({'ingredient': ing, 'count': count})
            assert entries, group
            data[key] = entries
        data['result'] = {'id': 'reinhardtshbm:circuit_' + variant}
        name = ('lbsm_' if simple else '') + 'circuit_' + variant
        write(recipes / f'soldering_station/{name}.json', dump(data))
        verified.append({'recipe': 'soldering_station/' + name, 'source': 'inventory/recipes/SolderingRecipes.java', 'line': solder_source.count('\n', 0, m.start()) + 1})

report = {'batch': 'components-002', 'source_commit': '936c60c4c07dc10aef81f75d800ad04282f08e62',
          'items': ids, 'new_registry_ids': [], 'recipes_checked': verified, 'removed_non_source_crafting': removed,
          'changed_files': changed, 'errors': errors,
          'technical_conversion': 'circuit metadata -> existing circuit_* IDs; textures items/circuit.* -> item/circuit_*; original animation metadata preserved'}
fixture = {'items': ids, 'crafting': [
    {'recipe': name, 'result': {'id': 'reinhardtshbm:circuit_' + m[1].lower(), 'count': int(m[2] or 1)}}
    for name, m in zip(names, re.finditer(r'addRecipeAuto\(DictFrame.fromOne\(ModItems.circuit, EnumCircuitType\.(\w+)(?:, (\d+))?\), new Object\[\] \{ (.*?) \}\);', craft_source))]}
write(root / 'src/main/resources/legacy/reinhardtshbm/circuit-batch.json', dump(fixture))
target = root / ('work/circuit-batch-check.json' if check else 'work/circuit-batch.json')
target.write_text(dump(report), encoding='utf-8')
print(json.dumps({'items': len(ids), 'source_recipes': len(verified), 'changed_files': len(changed), 'removed_non_source_crafting': len(removed), 'errors': errors}))
raise SystemExit(bool(errors))
