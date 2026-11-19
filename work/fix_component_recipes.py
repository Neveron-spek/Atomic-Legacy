from pathlib import Path
import json
import copy

root = Path(__file__).resolve().parents[1]
recipes = root / 'src/main/resources/data/reinhardtshbm/recipe'
groups = {
    'coil_copper': ('red_copper', ['coil_copper', 'coil_copper_from_ingot_steel']),
    'coil_gold': ('gold', ['coil_gold']),
    'coil_tungsten': ('tungsten', ['coil_tungsten']),
    'coil_magnetized_tungsten': ('magnetized_tungsten', ['coil_magnetized_tungsten_iron', 'coil_magnetized_tungsten_steel']),
}
for output, (wire, names) in groups.items():
    for name in names:
        path = recipes / (name + '.json')
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        data['key']['W'] = {'tag': 'c:wires/' + wire}
        data['key']['I'] = {'tag': 'c:ingots/' + ('steel' if 'steel' in name else 'iron')}
        path.write_text(json.dumps(data, indent=2) + '\n')
    if output in {'coil_gold', 'coil_tungsten'}:
        data = copy.deepcopy(json.loads((recipes / (output + '.json')).read_text()))
        data['key']['I'] = {'tag': 'c:ingots/steel'}
        (recipes / (output + '_from_ingot_steel.json')).write_text(json.dumps(data, indent=2) + '\n')

p = recipes / 'motor_bismuth.json'
data = json.loads(p.read_text())
data['key']['C'] = {'tag': 'c:wires_dense/neodymium'}
data['key']['S'] = {'tag': 'c:plates/cast_steel'}
p.write_text(json.dumps(data, indent=2) + '\n')
print('Restored both original coil cores and original OreDictionary ingredient groups; polymer BB recipes preserved.')
