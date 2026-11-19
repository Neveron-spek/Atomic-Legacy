from pathlib import Path
import json
import re
from collections import Counter

root = Path(__file__).resolve().parent
project = root.parent
recipe_root = project / 'src/main/resources/data/reinhardtshbm/recipe'
scope = {r['id'] for r in json.loads((root / 'building-audit.json').read_text())['blocks'] if r['registered']}

def split(text):
    result, start, depth, quote = [], 0, 0, None
    for i, c in enumerate(text):
        if quote:
            if c == quote and (i == 0 or text[i - 1] != '\\'):
                quote = None
        elif c in '\"\'':
            quote = c
        elif c in '({[':
            depth += 1
        elif c in ')}]':
            depth -= 1
        elif c == ',' and depth == 0:
            result.append(text[start:i].strip())
            start = i + 1
    if text[start:].strip():
        result.append(text[start:].strip())
    return result

vanilla = {'trapdoor': 'oak_trapdoor', 'fence': 'oak_fence', 'stonebrick': 'stone_bricks', 'brick_block': 'bricks', 'nether_brick': 'nether_bricks',
           'netherbrick': 'nether_brick', 'vine': 'vine', 'redstone_lamp': 'redstone_lamp',
           'iron_ingot': 'iron_ingot', 'clay_ball': 'clay_ball', 'dye': None}
materials = {'IRON': 'iron', 'GOLD': 'gold', 'STEEL': 'steel', 'CU': 'copper', 'TI': 'titanium',
             'W': 'tungsten', 'AL': 'aluminum', 'ASBESTOS': 'asbestos', 'STAR': 'starmetal',
             'MINGRADE': 'red_copper', 'P_RED': 'phosphorus', 'S': 'sulfur', 'SA326': 'schrabidium', 'BE': 'beryllium', 'DIAMOND': 'diamond', 'REDSTONE': 'redstone'}
shapes = {'ingot': 'ingots', 'dust': 'dusts', 'plate': 'plates', 'block': 'storage_blocks',
          'bolt': 'bolts', 'wireFine': 'wires', 'pipe': 'pipes', 'gem': 'gems'}
colors = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
ext = ['machine', 'machine_stripe', 'indigo', 'purple', 'pink', 'hazard', 'sand', 'bronze']

def stack_metadata(name, meta):
    if name == 'lightstone':
        variant = meta.split('.')[-2].lower() if 'ordinal' in meta else ['unrefined', 'tile', 'bricks', 'bricks_chiseled', 'chiseled'][int(meta)]
        return {'item': 'reinhardtshbm:lightstone' + ('' if variant == 'unrefined' else '_' + variant)}
    if name == 'platemetal':
        if 'BASE' in meta or meta == '0':
            return {'item': 'reinhardtshbm:platemetal'}
        variants = ['base', 'black', 'white', 'red', 'green', 'light_gray', 'blue', 'purple', 'cyan', 'pink', 'lime', 'yellow', 'light_blue', 'magenta', 'orange']
        return {'item': 'reinhardtshbm:platemetal_' + variants[int(meta)]}
    if name in {'concrete_colored', 'concrete_colored_ext', 'vinyl_tile'}:
        if 'ordinal' in meta:
            index = ext.index(meta.split('.')[-2].lower())
        else:
            index = int(meta)
        return {'item': f'reinhardtshbm:{name}', 'components': {
            'minecraft:custom_data': {'meta': index}, 'minecraft:custom_model_data': index}}
    if name == 'steel_scaffold':
        return {'item': 'reinhardtshbm:steel_scaffold' + ['', '_red', '_white', '_yellow'][int(meta)]}
    if name == 'fence_metal' and meta == '1':
        return {'item': 'reinhardtshbm:fence_metal', 'components': {'minecraft:custom_data': {'post': True}, 'minecraft:custom_model_data': 1}}
    if name == 'fence_metal' and meta == '0':
        return {'item': 'reinhardtshbm:fence_metal'}
    if name == 'filing_cabinet' and meta == 'DecoCabinetEnum.STEEL.ordinal()':
        return {'item': 'reinhardtshbm:filing_cabinet', 'components': {'minecraft:custom_data': {'variant': 1}}}
    raise ValueError(f'metadata {name}: {meta}')

def ingredient(expr):
    expr = expr.strip()
    if expr in {'KEY_ANYGLASS', 'KEY_ANYPANE', 'KEY_SAND', 'KEY_PLANKS', 'KEY_SLAB', 'KEY_STICK'}:
        return {'tag': {'KEY_ANYGLASS': 'c:glass_blocks', 'KEY_ANYPANE': 'c:glass_panes', 'KEY_SAND': 'c:sands',
                        'KEY_PLANKS': 'minecraft:planks', 'KEY_SLAB': 'minecraft:wooden_slabs', 'KEY_STICK': 'c:rods/wooden'}[expr]}
    if expr.startswith('KEY_') and expr[4:].lower() in colors:
        return {'tag': 'c:dyes/' + expr[4:].lower()}
    if expr.startswith('"dye'):
        color = re.sub(r'(?<!^)(?=[A-Z])', '_', json.loads(expr)[3:]).lower()
        return {'tag': f'c:dyes/{color}'}
    m = re.fullmatch(r'(\w+)\.(\w+)\(\)', expr)
    if m and m[1] in materials and m[2] in shapes:
        tag = f'c:{shapes[m[2]]}/{materials[m[1]]}'
        file = project / ('src/main/resources/data/c/tags/item/' + tag[2:] + '.json')
        if not file.exists() and m[1] == 'AL':
            tag = tag.replace('aluminum', 'aluminium')
        return {'tag': tag}
    if expr == 'U238.billet()':
        return {'tag': 'c:billets/u238'}
    if expr == 'Fluids.TRITIUM.getDict(1_000)':
        return {'type': 'reinhardtshbm:legacy_fluid_container', 'fluid': 'tritium', 'amount': 1000}
    if expr == 'ANY_TAR.any()':
        return {'tag': 'c:tars'}
    m = re.fullmatch(r'DictFrame.fromOne\(ModItems.circuit, EnumCircuitType.(\w+)\)', expr)
    if m:
        return {'item': 'reinhardtshbm:circuit_' + m[1].lower()}
    m = re.fullmatch(r'(ModBlocks|ModItems|Blocks|Items)\.(\w+)', expr)
    if m:
        namespace = 'reinhardtshbm' if m[1].startswith('Mod') else 'minecraft'
        name = vanilla.get(m[2], m[2]) if namespace == 'minecraft' else {'chain': 'dungeon_chain', 'ingot_mercury': 'nugget_mercury'}.get(m[2], m[2])
        if name is None:
            raise ValueError(expr)
        return {'item': f'{namespace}:{name}'}
    if expr.startswith('new ItemStack(') and expr.endswith(')'):
        args = split(expr[14:-1])
        if len(args) == 3 and args[0].startswith('ModBlocks.'):
            if args[2] == '0' and args[0] in {'ModBlocks.vinyl_tile', 'ModBlocks.fence_metal'}:
                other = stack_metadata(args[0].split('.')[1], '1')
                return {'type': 'neoforge:difference', 'base': ingredient(args[0]), 'subtracted': {
                    'type': 'neoforge:components', 'items': other['item'], 'components': other['components']}}
            if args[2] == 'OreDictionary.WILDCARD_VALUE':
                name = args[0].split('.')[1]
                if name == 'platemetal':
                    return [{'item': 'reinhardtshbm:platemetal' + suffix} for suffix in ['', '_black', '_white', '_red', '_green', '_light_gray', '_blue', '_purple', '_cyan', '_pink', '_lime', '_yellow', '_light_blue', '_magenta', '_orange']]
                return ingredient(args[0])
            if args[2] != '0' or args[0].split('.')[1] == 'lightstone':
                value = stack_metadata(args[0].split('.')[1], args[2])
                if 'components' in value:
                    return {'type': 'neoforge:components', 'items': value['item'], 'components': value['components']}
                return value
        if len(args) == 1 or len(args) == 2 or (len(args) == 3 and args[2] == '0'):
            return ingredient(args[0])
    raise ValueError(expr)

def output(expr):
    assert expr.startswith('new ItemStack('), expr
    args = split(expr[14:-1])
    first = args[0]
    if first.startswith('Item.getItemFromBlock('):
        first = first[22:-1]
    value = ingredient(first)
    if len(args) > 2:
        value = stack_metadata(first.split('.')[1], args[2])
    result = {'id': value['item'], 'count': int(args[1]) if len(args) > 1 else 1}
    if 'components' in value:
        result['components'] = value['components']
    return result

def canonical(recipe):
    result = dict(recipe['result'])
    result.setdefault('count', 1)
    if recipe['type'] == 'minecraft:crafting_shaped':
        pattern = recipe['pattern']
        while pattern and not pattern[0].strip(): pattern = pattern[1:]
        while pattern and not pattern[-1].strip(): pattern = pattern[:-1]
        left = min(i for row in pattern for i, c in enumerate(row) if c != ' ')
        right = max(i for row in pattern for i, c in enumerate(row) if c != ' ')
        slots = [[recipe['key'].get(c) for c in row[left:right+1]] for row in pattern]
        mirror = [list(reversed(row)) for row in slots]
        grid = min(json.dumps(slots, sort_keys=True), json.dumps(mirror, sort_keys=True))
    else:
        grid = sorted(json.dumps(x, sort_keys=True) for x in recipe['ingredients'])
    return json.dumps([recipe['type'], result, grid, recipe.get('neoforge:conditions')], sort_keys=True)

existing = {}
for file in recipe_root.rglob('*.json'):
    data = json.loads(file.read_text(encoding='utf-8-sig'))
    if data.get('type') in {'minecraft:crafting_shaped', 'minecraft:crafting_shapeless'}:
        existing[canonical(data)] = file
source = root / 'original/src/main/java/com/hbm/main/CraftingManager.java'
resolved, unresolved, added, corrected = [], [], [], []
protected = set()
source_lines = []
for line_no, line in enumerate(source.read_text().splitlines(), 1):
    if line_no == 390:
        for index, color in enumerate(colors):
            source_lines.append((line_no, line.replace(', i)', f', {index})').replace('dyeName', '"dye' + ''.join(w.title() for w in color.split('_')) + '"')))
    elif line_no == 398:
        plate_colors = ['black', 'white', 'red', 'green', 'light_gray', 'blue', 'purple', 'cyan', 'pink', 'lime', 'yellow', 'light_blue', 'magenta', 'orange']
        for index, color in enumerate(plate_colors):
            source_lines.append((line_no, line.replace(', i + 1)', f', {index + 1})').replace('dyeName', '"dye' + ''.join(w.title() for w in color.split('_')) + '"')))
    else:
        source_lines.append((line_no, line))
for line_no, line in source_lines:
    if line.lstrip().startswith('//'):
        continue
    m = re.search(r'(addRecipeAuto|addShapelessAuto)\((new ItemStack\(.*?), new Object\[\] \{(.*?)\}\);', line)
    if not m:
        continue
    out_ids = re.findall(r'ModBlocks\.(\w+)', m[2])
    if not out_ids or out_ids[0] not in scope:
        continue
    try:
        result = output(m[2])
        args = split(m[3])
        if m[1] == 'addRecipeAuto':
            pattern = []
            while args and args[0].startswith('"'):
                pattern.append(json.loads(args.pop(0)))
            assert len(args) % 2 == 0
            keys = {args[i].strip("'"): ingredient(args[i + 1]) for i in range(0, len(args), 2)}
            data = {'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': pattern, 'key': keys, 'result': result}
        else:
            data = {'type': 'minecraft:crafting_shapeless', 'category': 'building', 'ingredients': [ingredient(arg) for arg in args], 'result': result}
        # Preserve all conditionals: only this unconditional source region is exported here.
        if line_no >= 944:
            raise ValueError('conditional region requires separate review')
        key = canonical(data)
        if key not in existing:
            candidates = []
            for candidate in existing.values():
                if candidate in protected:
                    continue
                previous = json.loads(candidate.read_text(encoding='utf-8-sig'))
                if previous['type'] == data['type'] and previous.get('result', {}).get('id') == result['id'] and previous.get('result', {}).get('components') == result.get('components'):
                    if previous.get('pattern') == data.get('pattern'):
                        candidates.append(candidate)
            suffix = str(result.get('components', {}).get('minecraft:custom_model_data', result['id'].split(':')[1]))
            target = candidates[0] if len(candidates) == 1 else recipe_root / 'legacy_building' / f'{out_ids[0]}_{line_no}_{suffix}.json'
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(data, indent=2) + '\n')
            existing[key] = target
            (corrected if candidates and target in candidates else added).append(str(target.relative_to(recipe_root)))
        protected.add(existing[key])
        resolved.append(dict(line=line_no, id=out_ids[0], recipe=existing[key].relative_to(recipe_root).with_suffix('').as_posix(), result=result))
    except (ValueError, AssertionError, IndexError) as error:
        unresolved.append(dict(line=line_no, id=out_ids[0], expression=str(error)))
report = dict(resolved=resolved, unresolved=unresolved, added=added, corrected=corrected)
(root / 'crafting-source-audit.json').write_text(json.dumps(report, indent=2))
(project / 'src/main/resources/legacy/reinhardtshbm/building-recipes.json').write_text(json.dumps(resolved, indent=2) + '\n')
print(json.dumps(dict(resolved=len(resolved), added=len(added), corrected=len(corrected), unresolved=unresolved)))
