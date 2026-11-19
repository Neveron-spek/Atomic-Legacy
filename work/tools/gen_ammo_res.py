"""ammo_standard / ammo_secret resources and AmmoPressRecipes / WeaponRecipes ammo recipes."""
import json, os, re, shutil
SCR = os.path.dirname(os.path.abspath(__file__))
ORIG = 'work/original/src/main/resources/assets/hbm'
RES = 'src/main/resources'
TEX = RES + '/assets/reinhardtshbm/textures/item'
MOD = RES + '/assets/reinhardtshbm/models/item'
LANG = RES + '/assets/reinhardtshbm/lang'
REC = RES + '/data/reinhardtshbm/recipe'
variants = open(os.path.join(SCR, 'new_variants.txt')).read().split()
LOCALES = {'en_us': 'en_US', 'ru_ru': 'ru_RU', 'de_de': 'de_DE', 'ja_jp': 'ja_JP', 'pt_br': 'pt_BR',
           'uk_ua': 'uk_UA', 'zh_cn': 'zh_CN', 'it_it': 'it_IT', 'pl_pl': 'pl_PL', 'fr_fr': 'fr_FR'}
SECRET = ['folly_sm', 'folly_nuke', 'm44_equestrian', 'g12_equestrian', 'bmg50_equestrian', 'p35_800', 'bmg50_black', 'p35_800_bl']

def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write('\n')

def read_lang(code):
    d = {}
    for line in open(f'{ORIG}/lang/{code}.lang', encoding='utf-8', errors='replace'):
        line = line.rstrip('\r\n')
        if '=' in line and not line.startswith('#'):
            k, v = line.split('=', 1); d.setdefault(k, v)
    return d

def variant_models(item, ids):
    for i in ids:
        src = f'{ORIG}/textures/items/{item}.{i}.png'
        assert os.path.exists(src), src
        shutil.copyfile(src, f'{TEX}/{item}_{i}.png')
        wjson(f'{MOD}/{item}_{i}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'reinhardtshbm:item/{item}_{i}'}})
    # base model: variant 0 + custom_model_data overrides (same layout as the existing file)
    lines = ['{', '  "parent": "minecraft:item/generated",', '  "textures": {', f'    "layer0": "reinhardtshbm:item/{item}_{ids[0]}"', '  },', '  "overrides": [']
    ov = [f'    {{ "predicate": {{ "custom_model_data": {n} }}, "model": "reinhardtshbm:item/{item}_{i}" }}' for n, i in enumerate(ids) if n > 0]
    lines.append(',\n'.join(ov)); lines += ['  ]', '}']
    open(f'{MOD}/{item}.json', 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')

variant_models('ammo_standard', variants)
variant_models('ammo_secret', SECRET)

for loc, code in LOCALES.items():
    p = f'{LANG}/{loc}.json'
    src = read_lang(code)
    data = json.load(open(p, encoding='utf-8'))
    n = 0
    for item, ids in (('ammo_standard', variants), ('ammo_secret', SECRET)):
        for i in ids:
            v = src.get(f'item.{item}.{i}.name')
            k = f'item.reinhardtshbm.{item}.{i}'
            if v is not None and data.get(k) != v:
                data[k] = v; n += 1
    wjson(p, data); print(loc, n)

# --- AmmoPressRecipes ---
TOK = {
    'lead': {'tag': 'c:ingots/lead'}, 'nugget': {'tag': 'c:nuggets/lead'}, 'flechette': {'tag': 'c:bolts/lead'},
    'steel': {'tag': 'c:ingots/steel'}, 'wSteel': {'tag': 'c:ingots/weaponsteel'}, 'copper': {'tag': 'c:ingots/copper'},
    'plastic': {'tag': 'c:ingots/plastic'}, 'uranium': {'tag': 'c:ingots/u238'}, 'ferro': {'tag': 'c:ingots/ferrouranium'},
    'nb': {'tag': 'c:ingots/niobium'}, 'smokeless': {'tag': 'c:dusts/smokeless'}, 'he': {'tag': 'c:ingots/high_explosive'},
    'wp': {'tag': 'c:ingots/phosphorus'}, 'rp': {'tag': 'c:dusts/phosphorus'}, 'pipe': {'tag': 'c:pipes/steel'},
    'gunmetal': {'tag': 'c:plates/gunmetal'}, 'smokeful': {'item': 'minecraft:gunpowder'}, 'rocket': {'item': 'reinhardtshbm:rocket_fuel'},
    'cSmall': {'item': 'reinhardtshbm:casing_small'}, 'cBig': {'item': 'reinhardtshbm:casing_large'},
    'sSmall': {'item': 'reinhardtshbm:casing_small_steel'}, 'sBig': {'item': 'reinhardtshbm:casing_large_steel'},
    'bpShell': {'item': 'reinhardtshbm:casing_shotshell'}, 'pShell': {'item': 'reinhardtshbm:casing_buckshot'},
    'sShell': {'item': 'reinhardtshbm:casing_buckshot_advanced'}, 'dyn': {'item': 'reinhardtshbm:ball_dynamite'},
    'coplate': {'tag': 'c:plates/copper'}, 'sPlate': {'tag': 'c:plates/steel'}, 'lPlate': {'tag': 'c:plates/lead'},
    'napalm': {'item': 'reinhardtshbm:canister_napalm'}, 'silicon': {'tag': 'c:billets/silicon'}, 'tungsten': {'tag': 'c:ingots/tungsten'},
    'shell': {'item': 'reinhardtshbm:assembly_nuke'}, 'tatb': {'item': 'reinhardtshbm:ball_tatb'}, 'plutonium': {'tag': 'c:nuggets/pu239'},
    'new ComparableStack(ModItems.egg_balefire_shard)': {'item': 'reinhardtshbm:egg_balefire_shard'},
}
def fluid(name):  # Fluids.X.getDict(1000): the port matches the full universal tank like the existing flame_diesel recipe
    return {'type': 'neoforge:components', 'items': ['reinhardtshbm:fluid_tank_full'], 'components': {'minecraft:custom_data': {'fluid': name}}}
TOK['diesel'] = fluid('diesel'); TOK['gas'] = fluid('gas'); TOK['bf'] = fluid('balefire')

src = open('work/original/src/main/java/com/hbm/inventory/recipes/AmmoPressRecipes.java', encoding='utf-8').read()
recs = re.findall(r'recipes\.add\(new AmmoPressRecipe\(DictFrame\.fromOne\(ModItems\.ammo_standard, EnumAmmo\.([A-Z0-9_]+), (\d+)\),(.*?)\)\);', src, re.S)
EXISTING_OUT = {json.load(open(os.path.join(f'{REC}/ammo_press', f))).get('result', {}).get('components', {}).get('minecraft:custom_data', {}).get('variant') for f in os.listdir(f'{REC}/ammo_press')}
written = []
seen = {}
for enum, count, ins in recs:
    vid = enum.lower()
    seen[vid] = seen.get(vid, 0) + 1
    parts = [p.strip() for p in re.split(r',(?![^()]*\))', ins.replace('\n', ' ').replace('\t', ' '))]
    assert len(parts) == 9, (enum, parts)
    if vid in EXISTING_OUT:
        continue
    slots = []
    for p in parts:
        if p == 'null':
            slots.append({}); continue
        m = re.match(r'(.*?)\.copy\((\d+)\)$', p)
        name, n = (m.group(1), int(m.group(2))) if m else (p, 1)
        ing = TOK[name]
        slot = {'ingredient': ing}
        if n != 1: slot['count'] = n
        slots.append(slot)
    idx = variants.index(vid)
    fname = vid if seen[vid] == 1 else f'{vid}_{seen[vid]}'
    out = {'type': 'reinhardtshbm:ammo_press', 'group': f'ammo_press.{vid}', 'input': slots,
           'result': {'id': 'reinhardtshbm:ammo_standard', 'count': int(count),
                      'components': {'minecraft:custom_data': {'variant': vid}, 'minecraft:custom_model_data': idx}}}
    wjson(f'{REC}/ammo_press/{fname}.json', out); written.append(fname)
print('press recipes written', len(written), written)

# billets/silicon tag (SI.billet())
if not os.path.exists(f'{RES}/data/c/tags/item/billets/silicon.json'):
    wjson(f'{RES}/data/c/tags/item/billets/silicon.json', {'replace': False, 'values': ['reinhardtshbm:billet_silicon']})

# --- WeaponRecipes lines 113-117, 167-169 ---
def ammo(v, n=1):
    return {'id': 'reinhardtshbm:ammo_standard', 'count': n, 'components': {'minecraft:custom_data': {'variant': v}, 'minecraft:custom_model_data': variants.index(v)}}
def ammo_ing(v):
    return {'type': 'neoforge:components', 'items': ['reinhardtshbm:ammo_standard'], 'components': {'minecraft:custom_data': {'variant': v}}}
def shaped(name, pattern, key, result):
    wjson(f'{REC}/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key, 'result': result})
for v, c in (('stone', {'tag': 'c:cobblestones'}), ('stone_ap', {'item': 'minecraft:flint'}), ('stone_shot', {'item': 'minecraft:gravel'}), ('stone_iron', {'tag': 'c:ingots/iron'})):
    shaped(f'ammo_standard_{v}', ['C', 'P', 'G'], {'C': c, 'P': {'item': 'minecraft:paper'}, 'G': {'item': 'minecraft:gunpowder'}}, ammo(v, 6))
def shapeless(name, ings, result):
    wjson(f'{REC}/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ings, 'result': result})
shapeless('ammo_standard_ct_mortar_charge', [ammo_ing('ct_mortar')] * 7 + [{'item': 'reinhardtshbm:ducttape'}] * 2, ammo('ct_mortar_charge', 1))
def secret(v, n):
    return {'id': 'reinhardtshbm:ammo_secret', 'count': n, 'components': {'minecraft:custom_data': {'variant': v}, 'minecraft:custom_model_data': SECRET.index(v)}}
for s, base in (('m44_equestrian', 'm44_jhp'), ('g12_equestrian', 'g12'), ('bmg50_equestrian', 'bmg50_fmj')):
    shapeless(f'ammo_secret_{s}', [ammo_ing(base), {'item': 'reinhardtshbm:item_secret_selenium_steel'}], secret(s, 6))
print('ok')
