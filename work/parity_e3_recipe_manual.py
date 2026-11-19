"""e3: hand-written recipe corrections (sources: CraftingManager/MineralRecipes/SmeltingRecipes of 936c60c)."""
import json, re
from pathlib import Path
R = Path(__file__).resolve().parent.parent / 'src/main/resources/data/reinhardtshbm/recipe'
def load(n): return json.loads((R / n).read_text(encoding='utf-8-sig'))
def save(n, j): (R / n).write_text(json.dumps(j, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
def fluid(f, amount=1000): return {'type': 'reinhardtshbm:legacy_fluid_container', 'fluid': f, 'amount': amount}
# CraftingManager:842 canister_full(LUBRICANT) x2
j = load('canister_full.json')
j['ingredients'] = [fluid('heatingoil'), fluid('unsaturateds'), {'item': 'reinhardtshbm:canister_empty'}, {'item': 'reinhardtshbm:canister_empty'}, {'tag': 'reinhardtshbm:chemistry_set'}]
j['result'] = {'id': 'reinhardtshbm:canister_full', 'count': 2, 'components': {'minecraft:custom_data': {'fluid': 'lubricant'}}}
save('canister_full.json', j)
# CraftingManager:853 fluid_identifier_multi: vacuum tube
j = load('fluid_identifier_multi.json'); j['key']['C'] = {'item': 'reinhardtshbm:circuit_vacuum_tube'}; save('fluid_identifier_multi.json', j)
# CraftingManager:895/897 express drones: welded titanium plates + any 1000 mB kerosene container
for n in ('drone_patrol_express.json', 'drone_patrol_chunkloading_express.json'):
    j = load(n); j['key']['P'] = {'tag': 'c:plates/welded_titanium'}; j['key']['K'] = fluid('kerosene'); save(n, j)
# MineralRecipes:428-441 bedrock ore byproducts
BY = ['iron', 'copper', 'lithium', 'silicon', 'lead', 'titanium', 'aluminium', 'sulfur', 'calcium', 'bismuth', 'radium', 'technetium', 'polonium', 'uranium']
OUT = {'iron': ('powder_iron', 1), 'copper': ('powder_copper', 1), 'lithium': ('powder_lithium', 1), 'silicon': ('nugget_silicon', 3), 'lead': ('powder_lead', 1),
       'titanium': ('powder_titanium', 1), 'aluminium': ('powder_aluminium', 1), 'sulfur': ('sulfur', 1), 'calcium': ('powder_calcium', 1), 'bismuth': ('powder_bismuth', 1),
       'radium': ('powder_ra226', 1), 'technetium': ('billet_technetium', 1), 'polonium': ('billet_polonium', 1), 'uranium': ('powder_uranium', 1)}
for i, b in enumerate(BY):
    out, n = OUT[b]
    save('%s_from_ore_byproduct_%s.json' % (out, b), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['###', '###', '###'],
         'key': {'#': {'type': 'neoforge:components', 'items': ['reinhardtshbm:ore_byproduct'], 'components': {'minecraft:custom_data': {'byproduct_type': b}}}},
         'result': {'id': 'reinhardtshbm:' + out, 'count': n}})
# MineralRecipes:219 depleted mercury pellet -> 2 drops of mercury
save('nugget_mercury_from_pellet_rtg_depleted.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
     'ingredients': [{'type': 'neoforge:components', 'items': ['reinhardtshbm:pellet_rtg_depleted'], 'components': {'minecraft:custom_data': {'rtg_depleted_material': 'mercury'}}}],
     'result': {'id': 'reinhardtshbm:nugget_mercury', 'count': 2}})
# SmeltingRecipes: experience of nether/gneiss ores
XP = {'smelting_ore_nether_uranium': 12.0, 'smelting_ore_nether_uranium_scorched': 12.0, 'smelting_ore_nether_plutonium': 24.0, 'smelting_ore_nether_tungsten': 12.0,
      'smelting_ore_nether_schrabidium': 256.0, 'smelting_ore_nether_cobalt': 2.0, 'smelting_ore_gneiss_iron': 5.0, 'smelting_ore_gneiss_gold': 5.0,
      'smelting_ore_gneiss_uranium': 12.0, 'smelting_ore_gneiss_uranium_scorched': 12.0, 'smelting_ore_gneiss_lithium': 10.0, 'smelting_ore_gneiss_schrabidium': 256.0}
for n, xp in XP.items():
    j = load(n + '.json'); j['experience'] = xp; save(n + '.json', j)
# CraftingManager: reactor_sensor only without 528 mode
j = load('reactor_sensor.json')
j = {'neoforge:conditions': [{'type': 'neoforge:not', 'value': {'type': 'reinhardtshbm:enable_528_mode'}}], **{k: v for k, v in j.items() if k != 'neoforge:conditions'}}
save('reactor_sensor.json', j)
print('manual ok')
# sand_mix (BlockNTMSand BORON, LEAD, URANIUM, POLONIUM, QUARTZ): recipes must use the variant block, not the duplicate sand_<x> blocks
SV = ['boron', 'lead', 'uranium', 'polonium', 'quartz']
def sand_ing(v): return {'type': 'neoforge:components', 'items': ['reinhardtshbm:sand_mix'], 'components': {'minecraft:custom_data': {'variant': v}}}
def sand_res(v, n): return {'id': 'reinhardtshbm:sand_mix', 'count': n, 'components': {'minecraft:custom_data': {'variant': v}, 'minecraft:custom_model_data': SV.index(v)}}
for n, v in (('sand_boron.json', 'boron'), ('sand_lead.json', 'lead'), ('sand_uranium_crafting_685.json', 'uranium'), ('sand_polonium_crafting_686.json', 'polonium')):
    j = load(n); j['result'] = sand_res(v, 8); save(n, j)
for n, v in (('smelting_sand_boron_to_glass_boron.json', 'boron'), ('smelting_sand_lead_to_glass_lead.json', 'lead'), ('smelting_glass_uranium_smelting_128.json', 'uranium'),
             ('smelting_glass_polonium_smelting_129.json', 'polonium')):
    j = load(n); j['ingredient'] = sand_ing(v); save(n, j)
if (R / 'sand_quartz_crafting_689.json').exists(): (R / 'sand_quartz_crafting_689.json').unlink()
for n in ('purex/vitrify_gas.json', 'purex/vitrify_liquid.json', 'purex/vitrify_solid.json'):
    j = load(n)
    for e in j['input_items']:
        if e.get('ingredient') == {'item': 'reinhardtshbm:sand_lead'}: e['ingredient'] = sand_ing('lead')
    save(n, j)
j = load('arc_furnace/quartz_sand.json'); j['input'] = sand_ing('quartz'); save('arc_furnace/quartz_sand.json', j)
# PowderRecipes:40 bakelite powder from any 1000 mB containers
j = load('powder_bakelite.json'); j['ingredients'] = [fluid('aromatics'), fluid('petroleum'), {'tag': 'reinhardtshbm:chemistry_set'}]; save('powder_bakelite.json', j)
# CraftingManager:139 copper ingot from 9 fine copper wires
j = load('ingot_aluminium_crafting_138.json'); j['key'] = {k: {'tag': 'c:wires/copper'} for k in j['key']}; j['result'] = {'id': 'minecraft:copper_ingot', 'count': 1}; save('copper_ingot_crafting_139.json', j)
print('manual2 ok')
# ItemMetaUpgrade upgrade_stack / upgrade_ejector: meta 0..2 = LegacyMetaUpgradeItem tier 1..3 (custom_data.tier)
def tier_stack(base, t): return {'id': 'reinhardtshbm:' + base, 'count': 1, 'components': {'minecraft:custom_data': {'tier': t}, 'minecraft:custom_model_data': t - 1}}
def tier_ing(base, t): return {'type': 'neoforge:components', 'items': ['reinhardtshbm:' + base], 'components': {'minecraft:custom_data': {'tier': t}}}
for base in ('upgrade_stack', 'upgrade_ejector'):
    j = load(base + '.json'); j['result'] = tier_stack(base, 1); save(base + '.json', j)
for n, base, t in (('upgrade_stack_1_crafting_731.json', 'upgrade_stack', 2), ('upgrade_stack_2_crafting_732.json', 'upgrade_stack', 3),
                   ('upgrade_ejector_1_crafting_734.json', 'upgrade_ejector', 2), ('upgrade_ejector_2_crafting_735.json', 'upgrade_ejector', 3)):
    if not (R / n).exists(): continue
    j = load(n)
    for k, v in j['key'].items():
        if v.get('item') in ('reinhardtshbm:' + base, 'reinhardtshbm:%s_%d' % (base, t - 1)): j['key'][k] = tier_ing(base, t - 1)
    j['result'] = tier_stack(base, t); save(n, j)
# test block obj_tester is not ported (creative tab null)
for n in ('obj_tester_crafting_677.json',):
    if (R / n).exists(): (R / n).unlink()
# SmeltingRecipes:173 scrap_plastic wildcard: every scrap_plastic_<type> item of the port
import glob, os
lang = json.loads((R.parent.parent.parent / 'assets/reinhardtshbm/lang/en_us.json').read_text(encoding='utf-8'))
scraps = sorted(k.split('.')[-1] for k in lang if re.fullmatch(r'item\.reinhardtshbm\.scrap_plastic_[a-z_]+', k))
j = load('smelting_scrap_plastic_to_ingot_polymer.json'); j['ingredient'] = [{'item': 'reinhardtshbm:' + s} for s in scraps]; save('smelting_scrap_plastic_to_ingot_polymer.json', j)
print('manual3 ok', len(scraps))
