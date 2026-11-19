# aliases for parity_e3_recipes.py (1.7.10 names -> port ids / tags)
ITEM_ALIAS = {'wire_fine': 'wire', 'pipe_lead': 'weapon_pipe_lead', 'wand': 'wand_k', 'spawn_chopper': 'chopper', 'mp_stability_20_flat': 'mp_s_20', 'ingot_mercury': 'nugget_mercury', 'nugget_mercury': 'nugget_mercury_tiny', 'cinnebar': 'cinnabar', 'fallout': 'falloutitem',
               'chain': 'dungeon_chain', 'crystal_cinnebar': 'crystal_cinnabar', 'marshmallow#1': 'reinhardtshbm:marshmallow_roasted', 'block_fallout': 'block_fallout', 'plant_flower': 'plant', 'plant_flower#WEED': 'reinhardtshbm:plant_hemp', 'mp_chip_1': 'mp_c_1', 'mp_chip_2': 'mp_c_2', 'mp_chip_3': 'mp_c_3', 'mp_chip_4': 'mp_c_4', 'mp_chip_5': 'mp_c_5', 'oil_tar#CRUDE': 'reinhardtshbm:oil_tar', 'ingot_copper': 'minecraft:copper_ingot', 'block_copper': 'minecraft:copper_block'}
VARIANTS = {'saturn': ['saturnite'], 'bigmt': ['saturnite'], 'dura': ['dura_steel'], 'mingrade': ['red_copper'], 'aluminium': ['aluminium', 'aluminum'], 'cmb': ['combine_steel'], 'magtung': ['magnetized_tungsten'],
            'bigmt': ['saturnite'], 'alloy': ['advanced_alloy'], 'bbronze': ['bismuth_bronze'], 'abronze': ['arsenic_bronze'], 'dnt': ['dineutronium'],
            'workersalloy': ['desh'], 'carbon': ['carbon', 'graphite'], 'ferro': ['ferrouranium'], 'star': ['starmetal'], 'sbd': ['schrabidate'], 'nd': ['neodymium'],
            'nb': ['niobium'], 'pb': ['lead'], 'w': ['tungsten'], 'cu': ['copper'], 'ti': ['titanium'], 'al': ['aluminium']}
MAT_ALIAS = {'aluminium': 'aluminum', 'desh': 'workersalloy', 'redcopper': 'mingrade', 'niter': 'saltpeter', 'tantalium': 'tantalum', 'combinesteel': 'cmbsteel',
             'lanthanium': 'lanthanum', 'wooden': 'wood', 'th232': 'thorium232', 'pu238': 'plutonium238', 'pu239': 'plutonium239', 'pu240': 'plutonium240',
             'pu241': 'plutonium241', 'u233': 'uranium233', 'u235': 'uranium235', 'u238': 'uranium238', 'ra226': 'radium226', 'plastic': 'anyplastic',
             'hardplastic': 'anyhardplastic', 'resistantalloy': 'anyresistantalloy', 'bismoidbronze': 'anybismoidbronze', 'bismoid': 'anybismoid',
             'highexplosive': 'anyhighexplosive', 'plasticexplosive': 'anyplasticexplosive'}
ORE_ALIAS = {
    'paneGlass': ['c:glass_panes'], 'blockGlass': ['c:glass_blocks'], 'blockGlassColorless': ['c:glass_blocks/colorless', 'minecraft:glass'],
    'paneGlassColorless': ['c:glass_panes/colorless', 'minecraft:glass_pane'], 'plankWood': ['minecraft:planks'], 'slabWood': ['minecraft:wooden_slabs'],
    'logWood': ['minecraft:logs'], 'treeSapling': ['minecraft:saplings'], 'treeLeaves': ['minecraft:leaves'], 'sand': ['c:sands'], 'stone': ['minecraft:stone', 'c:stones'],
    'cobblestone': ['c:cobblestones'], 'slimeball': ['minecraft:slime_ball', 'c:slime_balls'], 'stickWood': ['minecraft:stick', 'c:rods/wooden'], 'dye': ['c:dyes'],
    'ingotBrick': ['minecraft:brick'], 'ingotBrickNether': ['minecraft:nether_brick'], 'ingotNetherBrick': ['minecraft:nether_brick'],
    'ingotIron': ['minecraft:iron_ingot'], 'ingotGold': ['minecraft:gold_ingot'], 'gemDiamond': ['minecraft:diamond'], 'gemEmerald': ['minecraft:emerald'],
    'dustRedstone': ['minecraft:redstone'], 'dustGlowstone': ['minecraft:glowstone_dust'], 'gemQuartz': ['minecraft:quartz'], 'gemNetherQuartz': ['minecraft:quartz', 'c:gems/quartz'],
    'gemLapis': ['minecraft:lapis_lazuli'], 'gemCoal': ['minecraft:coal'], 'blockIron': ['minecraft:iron_block'], 'blockGold': ['minecraft:gold_block'],
    'blockRedstone': ['minecraft:redstone_block'], 'blockDiamond': ['minecraft:diamond_block'], 'blockLapis': ['minecraft:lapis_block'],
    'blockEmerald': ['minecraft:emerald_block'], 'blockCoal': ['minecraft:coal_block'], 'blockQuartz': ['minecraft:quartz_block'],
    'nuggetGold': ['minecraft:gold_nugget'], 'record': ['c:music_discs'], 'anyConcrete': ['c:concretes'], 'cropCarrot': ['c:crops/carrot'], 'gemAnyCoke': ['c:gems/coke'], 'container1000lubricant': ['reinhardtshbm:container1000/lubricant'], 'ntmscrewdriver': ['c:tools/screwdrivers'], 'ntmchemistryset': ['reinhardtshbm:chemistry_set'],
}
for i, d in enumerate(['black', 'red', 'green', 'brown', 'blue', 'purple', 'cyan', 'light_gray', 'gray', 'pink', 'lime', 'yellow', 'light_blue', 'magenta', 'orange', 'white']):
    ORE_ALIAS['dye' + ''.join(w.capitalize() for w in d.split('_'))] = ['c:dyes/' + d]
V = {}
for n in ['sugar', 'paper', 'leather', 'gunpowder', 'string', 'clay_ball', 'water_bucket', 'wheat', 'apple', 'iron_ingot', 'gold_ingot', 'pumpkin_seeds', 'book',
          'carrot', 'potato', 'flint', 'glowstone_dust', 'milk_bucket', 'rotten_flesh', 'wheat_seeds', 'nether_wart', 'brick', 'stick', 'spider_eye', 'bowl',
          'glass_bottle', 'name_tag', 'ender_pearl', 'iron_sword', 'clock', 'cooked_beef', 'diamond', 'emerald', 'redstone', 'quartz', 'coal', 'bone', 'feather',
          'egg', 'bucket', 'lava_bucket', 'compass', 'blaze_powder', 'blaze_rod', 'gold_nugget', 'iron_pickaxe', 'iron_axe', 'iron_shovel', 'iron_hoe', 'bread',
          'cookie', 'golden_apple', 'golden_carrot', 'ghast_tear', 'magma_cream', 'nether_star', 'ender_eye', 'fermented_spider_eye', 'poisonous_potato',
          'baked_potato', 'pumpkin_pie', 'snowball', 'arrow', 'bow', 'flint_and_steel', 'shears', 'fishing_rod', 'saddle', 'minecart', 'cake', 'comparator',
          'repeater', 'experience_bottle', 'fire_charge', 'writable_book', 'map', 'item_frame', 'flower_pot', 'iron_door', 'diamond_sword', 'diamond_pickaxe',
          'golden_sword', 'beef', 'porkchop', 'chicken', 'cooked_chicken', 'cooked_porkchop', 'lead', 'iron_helmet', 'iron_chestplate',
          'iron_leggings', 'iron_boots', 'diamond_helmet', 'diamond_chestplate', 'diamond_leggings', 'diamond_boots', 'golden_helmet', 'stone_sword', 'wooden_sword',
          'diamond_axe', 'diamond_shovel', 'diamond_hoe', 'golden_pickaxe', 'golden_axe', 'golden_shovel', 'golden_hoe', 'stone_pickaxe', 'stone_axe', 'stone_shovel', 'stone_hoe',
          'wooden_pickaxe', 'wooden_axe', 'wooden_shovel', 'wooden_hoe', 'leather_helmet', 'leather_chestplate', 'leather_leggings', 'leather_boots', 'enchanted_book']:
    V['I.' + n] = 'I:minecraft:' + n
V.update({'I.reeds': 'I:minecraft:sugar_cane', 'I.slime_ball': 'I:minecraft:slime_ball', 'I.potionitem': 'I:minecraft:potion', 'B.sand#1': 'I:minecraft:red_sand', 'I.coal#1': 'I:minecraft:charcoal',
          'I.skull#1': 'I:minecraft:wither_skeleton_skull', 'I.skull': 'I:minecraft:skeleton_skull', 'I.netherbrick': 'I:minecraft:nether_brick', 'I.fish': 'I:minecraft:cod',
          'I.melon_seeds': 'I:minecraft:melon_seeds', 'I.dye#4': 'I:minecraft:lapis_lazuli', 'I.dye#15': 'I:minecraft:bone_meal', 'I.dye#3': 'I:minecraft:cocoa_beans',
          'I.dye': 'I:minecraft:ink_sac', 'I.speckled_melon': 'I:minecraft:glistering_melon_slice', 'I.firework_charge': 'I:minecraft:firework_star',
          'I.fireworks': 'I:minecraft:firework_rocket', 'I.cooked_fished': 'I:minecraft:cooked_cod', 'I.record_13': 'I:minecraft:music_disc_13',
          'I.melon': 'I:minecraft:melon_slice', 'I.bed': 'I:minecraft:red_bed', 'I.sign': 'I:minecraft:oak_sign'})
for n in ['iron_bars', 'redstone_torch', 'hopper', 'obsidian', 'tnt', 'ladder', 'furnace', 'cobblestone', 'crafting_table', 'torch', 'sand', 'stone', 'glass', 'glowstone',
          'redstone_lamp', 'sandstone', 'lever', 'vine', 'piston', 'dispenser', 'dropper', 'chest', 'gravel', 'dirt', 'netherrack', 'soul_sand', 'end_stone', 'ice',
          'clay', 'pumpkin', 'cactus', 'iron_block', 'gold_block', 'diamond_block', 'emerald_block', 'lapis_block', 'redstone_block', 'coal_block', 'quartz_block',
          'bookshelf', 'mossy_cobblestone', 'sticky_piston', 'rail', 'glass_pane', 'beacon', 'anvil', 'jukebox', 'bedrock', 'brewing_stand', 'cauldron', 'ender_chest',
          'enchanting_table', 'daylight_detector', 'tripwire_hook', 'stone_pressure_plate', 'stone_button', 'iron_trapdoor', 'hay_block', 'packed_ice',
          'brown_mushroom', 'red_mushroom', 'mycelium', 'cobblestone_wall', 'nether_brick_fence', 'dragon_egg', 'trapped_chest', 'iron_ore',
          'gold_ore', 'coal_ore', 'diamond_ore', 'emerald_ore', 'lapis_ore', 'redstone_ore', 'sponge', 'command_block']:
    V['B.' + n] = 'I:minecraft:' + n
V.update({'B.fence': 'I:minecraft:oak_fence', 'B.stone_slab': 'I:minecraft:smooth_stone_slab', 'B.wool': 'T:minecraft:wool', 'B.wool#*': 'T:minecraft:wool',
          'B.trapdoor': 'I:minecraft:oak_trapdoor', 'B.stonebrick': 'I:minecraft:stone_bricks', 'B.brick_block': 'I:minecraft:bricks', 'B.planks': 'T:minecraft:planks',
          'B.log': 'T:minecraft:logs', 'B.nether_brick': 'I:minecraft:nether_bricks', 'B.grass': 'I:minecraft:grass_block', 'B.web': 'I:minecraft:cobweb',
          'B.melon_block': 'I:minecraft:melon', 'B.hardened_clay': 'I:minecraft:terracotta', 'B.lit_pumpkin': 'I:minecraft:jack_o_lantern', 'B.snow': 'I:minecraft:snow_block',
          'B.quartz_ore': 'I:minecraft:nether_quartz_ore', 'B.wooden_slab': 'I:minecraft:oak_slab', 'B.yellow_flower': 'I:minecraft:dandelion', 'B.red_flower': 'I:minecraft:poppy',
          'B.fence_gate': 'I:minecraft:oak_fence_gate', 'B.wooden_door': 'I:minecraft:oak_door', 'B.noteblock': 'I:minecraft:note_block', 'B.deadbush': 'I:minecraft:dead_bush',
          'B.waterlily': 'I:minecraft:lily_pad', 'B.sapling': 'T:minecraft:saplings', 'B.leaves': 'T:minecraft:leaves', 'B.golden_rail': 'I:minecraft:powered_rail'})
for i, d in enumerate(['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']):
    V['B.wool#%d' % i] = 'I:minecraft:%s_wool' % d
    V['B.stained_glass#%d' % i] = 'I:minecraft:%s_stained_glass' % d
    V['B.stained_hardened_clay#%d' % i] = 'I:minecraft:%s_terracotta' % d
    V['B.carpet#%d' % i] = 'I:minecraft:%s_carpet' % d
WILD = {'wildSandstone': ['sandstone', 'chiseled_sandstone', 'cut_sandstone'],
        'wildStonebrick': ['stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks'],
        'wildStoneSlab': ['smooth_stone_slab', 'sandstone_slab', 'petrified_oak_slab', 'cobblestone_slab', 'brick_slab', 'stone_brick_slab', 'nether_brick_slab', 'quartz_slab'],
        'wildQuartzBlock': ['quartz_block', 'chiseled_quartz_block', 'quartz_pillar'],
        'wildDirt': ['dirt', 'coarse_dirt', 'podzol'],
        'wildCobbleWall': ['cobblestone_wall', 'mossy_cobblestone_wall'],
        'wildAnvil': ['anvil', 'chipped_anvil', 'damaged_anvil']}
V.update({'B.sand': 'O:sand', 'B.sandstone': 'O:wildSandstone', 'B.stonebrick': 'O:wildStonebrick', 'B.stone_slab': 'O:wildStoneSlab', 'B.quartz_block': 'O:wildQuartzBlock',
          'B.dirt': 'O:wildDirt', 'B.cobblestone_wall': 'O:wildCobbleWall', 'B.anvil': 'O:wildAnvil', 'B.sand#0': 'I:minecraft:sand'})
VANILLA = V
IGNORE_OUT = set()
