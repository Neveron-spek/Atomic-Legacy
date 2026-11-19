"""f1: per-recipe fixes after the extended e3 comparison (see work/parity-f1.md).
python parity_f1_recipe_fix.py [--dry]"""
import json, sys
from pathlib import Path

R = Path(__file__).resolve().parent.parent / 'src/main/resources/data/reinhardtshbm/recipe'
DRY = '--dry' in sys.argv

# (в) no counterpart in 936c60c (checked by substring search over the whole original src/main) or exact duplicates
DELETE = [
    # duplicates of the wildcard recipes legacy_building/concrete_smooth_392/393 (CraftingManager 392-393)
    *['concrete_smooth_from_concrete_colored_%s.json' % c for c in (
        'black', 'blue', 'brown', 'cyan', 'ext_bronze', 'ext_hazard', 'ext_indigo', 'ext_machine', 'ext_machine_stripe', 'ext_pink', 'ext_purple', 'ext_sand', 'gray', 'green', 'light_blue', 'lime', 'magenta', 'orange', 'pink', 'purple', 'red', 'silver', 'white', 'yellow')],
    # duplicates (same ingredients through a one-item tag) of legacy_building/brick_ducrete_370/371, brick_fire_929, filing_cabinet_935,
    # lightstone_422 (those ids are fixed by legacy/reinhardtshbm/building-recipes.json -> BuildingPortDiagnostics)
    'brick_ducrete_from_ducrete_smooth.json', 'brick_ducrete_from_ducrete.json', 'brick_fire.json', 'filing_cabinet_steel.json', 'lightstone.json',
    # plain smelting duplicates of the ItemHot recipes smelting_hot_* (SmeltingRecipes 167-169); vanilla furnace would pick these and skip heating
    'smelting_ingot_chainsteel_smelting_167.json', 'smelting_ingot_meteorite_smelting_168.json', 'smelting_ingot_meteorite_forged_smelting_169.json',
    'smelting_blade_meteorite_smelting_170.json',
    # stairs of blocks that have no stairs in 1.7.10 (ModBlocks has no *_stairs for them)
    'tile_lab_stairs_from_tile_lab.json', 'tile_lab_cracked_stairs_from_tile_lab_cracked.json', 'tile_lab_broken_stairs_from_tile_lab_broken.json',
    'reinforced_sand_stairs_from_reinforced_sand.json', 'cmb_brick_reinforced_stairs_from_cmb_brick_reinforced.json',
    # removed in 1.7.10 (MainRegistry.ignoreMappings) / never craftable there
    'block_advanced_alloy_from_ingots.json', 'ingot_advanced_alloy_from_block.json', 'crate_template.json', 'energy_core.json',
    'machine_controller_cdalloy.json', 'machine_controller_tcalloy.json', 'fluid_pump.json',
    'pneumatic_storage_access.json', 'pneumatic_storage_clutter.json', 'pneumatic_storage_exporter.json',
    'pneumatic_storage_importer.json', 'pneumatic_storage_mono.json',
    # americium fuel: 1.7.10 has only ingot<->nugget (MineralRecipes 92) and billet from U238+Am mix (168-170)
    'billet_americium_fuel_from_ingots.json', 'billet_americium_fuel_from_nuggets.json',
    'ingot_americium_fuel_from_billets.json', 'nugget_americium_fuel_from_billet.json',
    # port-only door variants
    'sliding_blast_door_2_from_sliding_blast_door.json', 'sliding_blast_door_from_sliding_blast_door_2.json',
    'sliding_gate_door_from_sliding_seal_door.json', 'sliding_seal_door_from_sliding_gate_door.json',
    # coltan ore is not smeltable in 1.7.10; pipe_lead.json crafted c:pipes/lead (= pipe_lead itself) into pipe_lead
    'smelting_ore_coltan.json', 'pipe_lead.json',
]

STONEBRICK = [{'item': 'minecraft:chiseled_stone_bricks'}, {'item': 'minecraft:cracked_stone_bricks'},
              {'item': 'minecraft:mossy_stone_bricks'}, {'item': 'minecraft:stone_bricks'}]


def load(name):
    return json.loads((R / name).read_text(encoding='utf-8-sig'))


def save(name, j):
    if DRY:
        print('WRITE', name); return
    (R / name).parent.mkdir(parents=True, exist_ok=True)
    (R / name).write_text(json.dumps(j, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def fixes():
    # CraftingManager 875-880: Blocks.stonebrick in a ShapedOreRecipe = any stonebrick meta (wildcard)
    for n in ('crane_extractor_stone', 'crane_extractor_stone_wand', 'crane_grabber_stone', 'crane_grabber_stone_wand',
              'crane_inserter_stone', 'crane_inserter_stone_wand'):
        j = load(n + '.json'); j['key']['C'] = STONEBRICK; save(n + '.json', j)
    # CraftingManager 93-94: BlockGenericStairs.recipeGen -> 4 stairs
    j = load('pink_stairs_from_pink_planks.json'); j['pattern'] = ['B  ', 'BB ', 'BBB']; j['result']['count'] = 4
    save('pink_stairs_from_pink_planks.json', j)
    # CraftingManager 859-860: HOLO_DIGAMMA + screwdriver(ntmscrewdriver) + duct tape + polish -> HOLO_RESTORED; damaged tape needs HOLO_RESTORED
    def holo(v, i):
        return {'minecraft:custom_data': {'variant': v}, 'minecraft:custom_model_data': i}
    save('holotape_image.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [
        {'type': 'neoforge:components', 'items': ['reinhardtshbm:holotape_image'], 'components': {'minecraft:custom_data': {'variant': 'holo_digamma'}}},
        {'tag': 'c:tools/screwdrivers'}, {'item': 'reinhardtshbm:ducttape'}, {'item': 'reinhardtshbm:armor_polish'}],
        'result': {'id': 'reinhardtshbm:holotape_image', 'components': holo('holo_restored', 1)}})
    j = load('holotape_damaged.json')
    j['ingredients'][0] = {'type': 'neoforge:components', 'items': ['reinhardtshbm:holotape_image'], 'components': {'minecraft:custom_data': {'variant': 'holo_restored'}}}
    save('holotape_damaged.json', j)
    # ToolRecipes 158-168 carts: NOT changed - 1.7.10 ShapedOreRecipe matches the cart by damage only (NBT-blind),
    # so the existing port files reproduce the original (checked by ParityS1 in checkBuildingAudit)
    # SmeltingRecipes 175-178: every EnumBedrockOre meta -> 16 cobblestone, 0.1 xp (one item ingredient covers all ore types)
    save('smelting_ore_bedrock_to_cobblestone.json', {'type': 'minecraft:smelting', 'category': 'misc', 'ingredient': {'item': 'reinhardtshbm:ore_bedrock'},
         'result': {'id': 'minecraft:cobblestone', 'count': 16}, 'experience': 0.1, 'cookingtime': 200})
    # ArmorRecipes 164/170: LBSM simple armor, addChest(SA326.ingot(), schrabidium_plate) (file name lbsm_schrabidium_plate.json is the LBSM plate_schrabidium recipe)
    save('lbsm_schrabidium_chestplate.json', {'neoforge:conditions': [{'type': 'reinhardtshbm:less_bullshit_mode', 'feature': 'simple_armor'}],
         'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['X X', 'XXX', 'XXX'], 'key': {'X': {'tag': 'c:ingots/schrabidium'}},
         'result': {'id': 'reinhardtshbm:schrabidium_plate'}})



if __name__ == '__main__':
    for n in DELETE:
        p = R / n
        if p.exists():
            print('DELETE', n)
            if not DRY: p.unlink()
        else:
            print('MISSING', n)
    fixes()
