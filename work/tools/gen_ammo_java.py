"""Adds every remaining GunFactory.EnumAmmo value to ammo_standard and StandardAmmoType."""
import re
P = 'src/main/java/com/reinhardt/hbm/items/StandardAmmoItem.java'
t = open(P, encoding='utf-8').read()

ENUM = """STONE, STONE_AP, STONE_IRON, STONE_SHOT,
M357_BP, M357_SP, M357_FMJ, M357_JHP, M357_AP, M357_EXPRESS,
M44_BP, M44_SP, M44_FMJ, M44_JHP, M44_AP, M44_EXPRESS,
P22_SP, P22_FMJ, P22_JHP, P22_AP,
P9_SP, P9_FMJ, P9_JHP, P9_AP,
R556_SP, R556_FMJ, R556_JHP, R556_AP,
R762_SP, R762_FMJ, R762_JHP, R762_AP, R762_DU,
BMG50_SP, BMG50_FMJ, BMG50_JHP, BMG50_AP, BMG50_DU,
B75, B75_INC, B75_EXP,
G12_BP, G12_BP_MAGNUM, G12_BP_SLUG, G12, G12_SLUG, G12_FLECHETTE, G12_MAGNUM, G12_EXPLOSIVE, G12_PHOSPHORUS,
G26_FLARE, G26_FLARE_SUPPLY, G26_FLARE_WEAPON,
G40_HE, G40_HEAT, G40_DEMO, G40_INC, G40_PHOSPHORUS,
ROCKET_HE, ROCKET_HEAT, ROCKET_DEMO, ROCKET_INC, ROCKET_PHOSPHORUS,
FLAME_DIESEL, FLAME_GAS, FLAME_NAPALM, FLAME_BALEFIRE,
CAPACITOR, CAPACITOR_OVERCHARGE, CAPACITOR_IR,
TAU_URANIUM,
COIL_TUNGSTEN, COIL_FERROURANIUM,
NUKE_STANDARD, NUKE_DEMO, NUKE_HIGH, NUKE_TOTS, NUKE_HIVE,
G10, G10_SHRAPNEL, G10_DU, G10_SLUG,
R762_HE, BMG50_HE, G10_EXPLOSIVE,
P45_SP, P45_FMJ, P45_JHP, P45_AP, P45_DU,
CT_HOOK, CT_MORTAR, CT_MORTAR_CHARGE,
NUKE_BALEFIRE, BMG50_SM,
ROCKET_MICRO"""
ORDER = """STONE, STONE_AP, STONE_IRON, STONE_SHOT,
M357_BP, M357_SP, M357_FMJ, M357_JHP, M357_AP, M357_EXPRESS,
M44_BP, M44_SP, M44_FMJ, M44_JHP, M44_AP, M44_EXPRESS,
P22_SP, P22_FMJ, P22_JHP, P22_AP,
P9_SP, P9_FMJ, P9_JHP, P9_AP,
P45_SP, P45_FMJ, P45_JHP, P45_AP, P45_DU,
R556_SP, R556_FMJ, R556_JHP, R556_AP,
R762_SP, R762_FMJ, R762_JHP, R762_AP, R762_DU, R762_HE,
BMG50_SP, BMG50_FMJ, BMG50_JHP, BMG50_AP, BMG50_DU, BMG50_SM, BMG50_HE,
B75, B75_INC, B75_EXP,
G12_BP, G12_BP_MAGNUM, G12_BP_SLUG, G12, G12_SLUG, G12_FLECHETTE, G12_MAGNUM, G12_EXPLOSIVE, G12_PHOSPHORUS,
G10, G10_SHRAPNEL, G10_DU, G10_SLUG, G10_EXPLOSIVE,
G26_FLARE, G26_FLARE_SUPPLY, G26_FLARE_WEAPON,
G40_HE, G40_HEAT, G40_DEMO, G40_INC, G40_PHOSPHORUS,
ROCKET_MICRO,
ROCKET_HE, ROCKET_HEAT, ROCKET_DEMO, ROCKET_INC, ROCKET_PHOSPHORUS,
FLAME_DIESEL, FLAME_GAS, FLAME_NAPALM, FLAME_BALEFIRE,
CAPACITOR, CAPACITOR_OVERCHARGE, CAPACITOR_IR,
TAU_URANIUM,
COIL_TUNGSTEN, COIL_FERROURANIUM,
NUKE_STANDARD, NUKE_DEMO, NUKE_HIGH, NUKE_TOTS, NUKE_HIVE, NUKE_BALEFIRE,
CT_HOOK, CT_MORTAR, CT_MORTAR_CHARGE"""
enum_ids = [x.strip().lower() for x in ENUM.replace('\n', ' ').split(',')]
order_ids = [x.strip().lower() for x in ORDER.replace('\n', ' ').split(',')]
assert len(enum_ids) == len(set(enum_ids)) == len(order_ids) == len(set(order_ids)), (len(enum_ids), len(order_ids))

m = re.search(r'variants\(\n(.*?)\n        \)\);', t, re.S)
existing = re.findall(r'"([a-z0-9_]+)"', m.group(1))
new_variants = [i for i in enum_ids if i not in existing]
ins = ',\n                // Remaining GunFactory.EnumAmmo values in enum order (appended).\n' + ',\n'.join(f'                "{i}"' for i in new_variants)
t = t.replace('                "p22_ap"\n        ));', '                "p22_ap"' + ins + '\n        ));', 1)

# creative order = full EnumAmmo.order
co = re.search(r'(private static final List<String> CREATIVE_ORDER = List.of\(\n)(.*?)(\n    \);)', t, re.S)
lines = []
cur = []
for i in order_ids:
    cur.append(f'"{i}"')
    if len(cur) == 6:
        lines.append('            ' + ', '.join(cur)); cur = []
if cur: lines.append('            ' + ', '.join(cur))
t = t[:co.start(2)] + ',\n'.join(lines) + t[co.end(2):]
t = t.replace('/** GunFactory.EnumAmmo.order (creative tab order of ammo_standard), limited to the ported variants. */',
              '/** GunFactory.EnumAmmo.order (creative tab order of ammo_standard). */')

# StandardAmmoType: (family, damage, mult, pen, falloff, ATN, AP, headshot) from the 1.7.10 BulletConfig of each round
F = lambda fam, mult=1.0, pen=False, fall=True, atn=0.0, ap=0.0, hs=1.25: (fam, mult, pen, fall, atn, ap, hs)
B = 'LEGACY_BULLET'
SPEC = {
    'stone_ap': F(B, 1.5, True, True, hs=1.0), 'stone_iron': F(B, 1.5, True, False, hs=1.0), 'stone_shot': F(B, 1/6, hs=1.0),
    'm357_sp': F(B), 'm357_fmj': F(B, .8, atn=2, ap=.1), 'm357_jhp': F(B, 1.5, ap=-.25, hs=1.5),
    'm357_ap': F(B, 1.25, True, False, 5, .15), 'm357_express': F(B, 1.5, True, True, 2, .1),
    'm44_sp': F(B), 'm44_fmj': F(B, .8, atn=3, ap=.1), 'm44_jhp': F(B, 1.5, ap=-.25, hs=1.5),
    'm44_ap': F(B, 1.25, True, False, 7.5, .15), 'm44_express': F(B, 1.5, True, True, 3, .1),
    'r762_jhp': F(B, 1.5, ap=-.25, hs=1.5), 'r762_ap': F(B, 1.25, True, False, 12.5, .15), 'r762_du': F(B, 1.5, True, False, 15, .25),
    'b75': F('B75'), 'b75_inc': F('B75', .8, ap=.1), 'b75_exp': F('B75', 1.5, ap=-.25),
    'g12_bp_magnum': F(B, .75/4), 'g12_bp_slug': F(B, .75), 'g12': F(B, 1/8, atn=2), 'g12_slug': F(B, 1.0, atn=4, ap=.15, hs=1.5),
    'g12_flechette': F(B, 1/8, atn=5, ap=.2), 'g12_magnum': F(B, 2/4, atn=4), 'g12_explosive': F(B, 2.5), 'g12_phosphorus': F(B, 1/8),
    'g26_flare_supply': F('FLARE'), 'g26_flare_weapon': F('FLARE'),
    'g40_he': F('G40'), 'g40_heat': F('G40', .5), 'g40_demo': F('G40', .75), 'g40_inc': F('G40', .75), 'g40_phosphorus': F('G40', .75),
    'flame_gas': F('FLAME'), 'flame_napalm': F('FLAME'), 'flame_balefire': F('FLAME'),
    'capacitor': F('ENERGY'), 'capacitor_overcharge': F('ENERGY', 1.0, True, True), 'capacitor_ir': F('ENERGY'),
    'coil_tungsten': F('COIL', 1.0, True, False), 'coil_ferrouranium': F('COIL', 1.0, True, False),
    'g10': F(B, .1, atn=5), 'g10_shrapnel': F(B, .1, atn=5), 'g10_du': F(B, .25, True, False, 10, .2), 'g10_slug': F(B, 1.0, True, True, 10, .1),
    'r762_he': F(B, 1.75), 'g10_explosive': F(B, .25),
    'p45_sp': F(B), 'p45_fmj': F(B, .8, atn=2, ap=.1), 'p45_jhp': F(B, 1.5, ap=-.25, hs=1.5), 'p45_ap': F(B, 1.25, True, False, 5, .15),
    'p45_du': F(B, 2.5, True, False, 15, .25),
    'ct_hook': F('CT', 1.0, True, False), 'ct_mortar': F('CT', 2.5), 'ct_mortar_charge': F('CT', 5.0),
    'rocket_micro': F('ROCKET_ML'),
}
existing_types = re.findall(r'^        ([A-Z0-9_]+)\("([a-z0-9_]+)"', t, re.M)
existing_ids = {i for _, i in existing_types}
missing = [i for i in enum_ids if i not in existing_ids]
assert set(missing) == set(SPEC), (set(missing) ^ set(SPEC))
def f(x):
    s = repr(round(x, 6))
    return (s if '.' in s else s + '.0') + 'F'
entries = []
for i in enum_ids:
    if i not in SPEC: continue
    fam, mult, pen, fall, atn, ap, hs = SPEC[i]
    entries.append(f'        {i.upper()}("{i}", AmmoFamily.{fam}, 0.0F, {f(mult)}, {str(pen).lower()}, {str(fall).lower()}, {f(atn)}, {f(ap)}, {f(hs)})')
old_tail = '        P22_AP("p22_ap", AmmoFamily.P22, 0.0F, 1.25F, true, false, 2.5F, 0.15F, 1.25F);'
assert old_tail in t
t = t.replace(old_tail, old_tail[:-1] + ',\n        // Remaining GunFactory.EnumAmmo rounds (appended, ordinals are synced by legacy_bullet):\n'
              '        // damage multiplier, penetration, falloff, threshold negation, armor piercing and headshot\n'
              '        // multiplier of the 1.7.10 BulletConfig of each round.\n' + ',\n'.join(entries) + ';')
# families
t = t.replace('        FLARE,\n        P22\n    }', '        FLARE,\n        P22,\n        // appended for the remaining EnumAmmo rounds\n        B75,\n        G40,\n        ENERGY,\n        COIL,\n        CT\n    }')
open(P, 'w', encoding='utf-8', newline='').write(t)
print('variants+', len(new_variants), 'types+', len(entries))
open('C:/Users/Admin/AppData/Local/Temp/claude/03873186fd96630b/cbf489fe-17f5-4dc5-af31-a190b9f110b2/scratchpad/new_variants.txt', 'w').write('\n'.join(existing + new_variants))
