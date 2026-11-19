"""Generates 1.7.10 ItemAutogen gun parts (all Mats#setAutogen combos) for the port."""
import json, os, re, subprocess, sys

ROOT = os.getcwd()
S = os.path.dirname(os.path.abspath(__file__))
ORIG = 'work/original/src/main/resources/assets/hbm'
RES = 'src/main/resources'
TEX = RES + '/assets/reinhardtshbm/textures/item/material'
MOD = RES + '/assets/reinhardtshbm/models/item'
LANG = RES + '/assets/reinhardtshbm/lang'
TAGS = RES + '/data/c/tags/item/gun_parts'
REC = RES + '/data/reinhardtshbm/recipe'

# port name, dict name (hbmmat key), light, dark  (Mats.java solidColorLight / solidColorDark)
MATS = {
    'wood': ('wood', 0x896727, 0x281E0B),
    'bone': ('bone', 0xFFFEEE, 0x797870),
    'steel': ('steel', 0xAFAFAF, 0x0F0F0F),
    'dura_steel': ('durasteel', 0x82A59C, 0x06281E),
    'desh': ('workersalloy', 0xFF6D6D, 0x720000),
    'ferrouranium': ('ferrouranium', 0xB7B7C9, 0x101022),
    'tcalloy': ('tcalloy', 0xD4D6D6, 0x323D3D),
    'cdalloy': ('cdalloy', 0xF7DF8F, 0x604308),
    'bismuth_bronze': ('bismuthbronze', 0xE19A69, 0x485353),
    'arsenic_bronze': ('arsenicbronze', 0xDB9462, 0x203331),
    'gunmetal': ('gunmetal', 0xFFEF3F, 0xAD3600),
    'weaponsteel': ('weaponsteel', 0xA0A0A0, 0x000000),
    'saturnite': ('saturnite', 0x3AC4DA, 0x09282C),
    'polymer': ('polymer', 0x363636, 0x040404),
    'bakelite': ('bakelite', 0xF28086, 0x2B0608),
    'rubber': ('rubber', 0x817F75, 0x0F0D03),
    'pc': ('polycarbonate', 0xEDE7C4, 0x908A67),
    'pvc': ('pvc', 0xFCFCFC, 0x9F9F9F),
}
BL = ['steel', 'dura_steel', 'desh', 'tcalloy', 'cdalloy', 'bismuth_bronze', 'arsenic_bronze', 'gunmetal', 'weaponsteel', 'saturnite']
SHAPES = {  # item prefix -> (orig texture, lang key, tag dir, materials) ; ModItems order
    'part_barrel_light': ('part_barrel_light', 'item.part_barrel_light.name', 'barrel_light', BL),
    'part_barrel_heavy': ('part_barrel_heavy', 'item.part_barrel_heavy.name', 'barrel_heavy',
                          ['steel', 'dura_steel', 'desh', 'ferrouranium', 'tcalloy', 'cdalloy', 'gunmetal', 'weaponsteel', 'saturnite']),
    'part_receiver_light': ('part_receiver_light', 'item.part_receiver_light.name', 'receiver_light', BL),
    'part_receiver_heavy': ('part_receiver_heavy', 'item.part_receiver_heavy.name', 'receiver_heavy',
                            ['dura_steel', 'ferrouranium', 'tcalloy', 'cdalloy', 'bismuth_bronze', 'arsenic_bronze', 'gunmetal', 'weaponsteel', 'saturnite']),
    'part_mechanism': ('part_mechanism', 'item.part_mechanism.name', 'mechanism', ['gunmetal', 'weaponsteel', 'saturnite']),
    'part_stock': ('part_stock', 'item.part_stock.name', 'stock',
                   ['wood', 'desh', 'gunmetal', 'weaponsteel', 'saturnite', 'polymer', 'bakelite', 'pc', 'pvc']),
    'part_grip': ('part_grip', 'item.part_grip.name', 'grip',
                  ['wood', 'bone', 'steel', 'dura_steel', 'desh', 'gunmetal', 'weaponsteel', 'saturnite', 'polymer', 'bakelite', 'rubber', 'pc', 'pvc']),
}
# OreDictManager DictGroups with shape prefixes
GROUPS = {
    'any_plastic': ['polymer', 'bakelite'],
    'any_hardplastic': ['pc', 'pvc'],
    'any_resistantalloy': ['tcalloy', 'cdalloy'],
    'any_bismoidbronze': ['bismuth_bronze', 'arsenic_bronze'],
}

LOCALES = {'en_us': 'en_US', 'ru_ru': 'ru_RU', 'de_de': 'de_DE', 'ja_jp': 'ja_JP', 'pt_br': 'pt_BR',
           'uk_ua': 'uk_UA', 'zh_cn': 'zh_CN', 'it_it': 'it_IT', 'pl_pl': 'pl_PL', 'fr_fr': 'fr_FR'}


def read_lang(code):
    d = {}
    p = f'{ORIG}/lang/{code}.lang'
    for line in open(p, encoding='utf-8', errors='replace'):
        line = line.rstrip('\r\n')
        if '=' in line and not line.startswith('#'):
            k, v = line.split('=', 1)
            d.setdefault(k, v)
    return d


def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


items = []
remap_args = []
for prefix, (tex, lkey, tagdir, mats) in SHAPES.items():
    for m in mats:
        iid = f'{prefix}_{m}'
        items.append((iid, prefix, tex, lkey, tagdir, m))
        dst = f'{TEX}/{iid}.png'
        light, dark = MATS[m][1], MATS[m][2]
        remap_args += [f'{ORIG}/textures/items/{tex}.png', dst, f'{light:06X}', f'{dark:06X}']
        wjson(f'{MOD}/{iid}.json', {'parent': 'minecraft:item/generated',
                                     'textures': {'layer0': f'reinhardtshbm:item/material/{iid}'}})
        wjson(f'{TAGS}/{tagdir}/{m}.json', {'values': [f'reinhardtshbm:{iid}']})
    for g, members in GROUPS.items():
        vals = [f'reinhardtshbm:{prefix}_{m}' for m in members if m in mats]
        if vals:
            wjson(f'{TAGS}/{tagdir}/{g}.json', {'values': vals})

subprocess.check_call(['java', os.path.join(S, 'Remap.java')] + remap_args)

# Lang
for loc, code in LOCALES.items():
    p = f'{LANG}/{loc}.json'
    if not os.path.exists(p):
        continue
    src = read_lang(code)
    data = json.load(open(p, encoding='utf-8'))
    added = 0
    for iid, prefix, tex, lkey, tagdir, m in items:
        key = f'item.reinhardtshbm.{iid}'
        fmt = src.get(lkey)
        mat = src.get('hbmmat.' + MATS[m][0])
        if fmt is None or mat is None:
            continue
        val = fmt.replace('%s', mat, 1)
        if data.get(key) != val:
            data[key] = val
            added += 1
    # molds
    mold_fmt = src.get('item.mold.name')
    for mid, shape in (('mold_barrel_heavy', 'barrelHeavy'), ('mold_receiver_heavy', 'receiverHeavy'), ('mold_stock', 'stock')):
        sname = src.get('shape.' + shape)
        if mold_fmt and sname:
            data[f'item.reinhardtshbm.{mid}'] = f'{mold_fmt} - {sname} x1'
            added += 1
    sc = src.get('item.weapon_mod_special.scope.name')
    if sc:
        data['item.reinhardtshbm.weapon_mod_special_scope'] = sc
        added += 1
    wjson(p, data)
    print(loc, 'keys', added)

# Molds models
for mid in ('mold_barrel_heavy', 'mold_receiver_heavy', 'mold_stock'):
    wjson(f'{MOD}/{mid}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'reinhardtshbm:item/{mid}'}})

# Crafting recipes, WeaponRecipes lines 44-55
STOCK = ['WWW', '  W']
GRIP = ['W ', ' W', ' W']
def shaped(name, pattern, key, result, count=1):
    wjson(f'{REC}/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                                  'key': key, 'result': {'id': result, 'count': count}})
shaped('part_stock_wood', STOCK, {'W': {'tag': 'minecraft:planks'}}, 'reinhardtshbm:part_stock_wood')
shaped('part_grip_wood', GRIP, {'W': {'tag': 'minecraft:planks'}}, 'reinhardtshbm:part_grip_wood')
for m in ('polymer', 'bakelite', 'pc', 'pvc'):
    shaped(f'part_stock_{m}', STOCK, {'W': {'tag': f'c:ingots/{m}'}}, f'reinhardtshbm:part_stock_{m}')
    shaped(f'part_grip_{m}', GRIP, {'W': {'tag': f'c:ingots/{m}'}}, f'reinhardtshbm:part_grip_{m}')
shaped('part_grip_rubber', GRIP, {'W': {'tag': 'c:ingots/rubber'}}, 'reinhardtshbm:part_grip_rubber')
shaped('part_grip_bone', GRIP, {'W': {'item': 'minecraft:bone'}}, 'reinhardtshbm:part_grip_bone')
# WeaponRecipes line 139: scope
shaped('weapon_mod_special_scope', ['SPS', 'G G', 'SPS'],
       {'P': {'tag': 'c:ingots/plastic'}, 'S': {'tag': 'c:plates/steel'}, 'G': {'tag': 'c:glass_panes'}},
       'reinhardtshbm:weapon_mod_special_scope')
print('items', len(items))
