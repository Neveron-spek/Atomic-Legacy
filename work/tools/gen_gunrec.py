import json, sys
REC = 'src/main/resources/data/reinhardtshbm/recipe'
def gp(shape, mat): return {'tag': f'c:gun_parts/{shape}/{mat}'}
SCOPE = {'item': 'reinhardtshbm:weapon_mod_special_scope'}
def shaped(name, pattern, key):
    with open(f'{REC}/{name}.json', 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                   'result': {'id': f'reinhardtshbm:{name}', 'count': 1}}, f, indent=2)
        f.write('\n')
# WeaponRecipes.java (936c60c) lines 71, 78, 84, 89, 90, 94, 100
shaped('gun_am180', ['BRS', 'GMG'], {'B': gp('barrel_light', 'dura_steel'), 'R': gp('receiver_light', 'dura_steel'),
       'M': gp('mechanism', 'gunmetal'), 'G': gp('grip', 'wood'), 'S': gp('stock', 'wood')})
shaped('gun_uzi', ['BRS', ' GM'], {'B': gp('barrel_light', 'desh'), 'R': gp('receiver_light', 'desh'),
       'S': gp('stock', 'any_plastic'), 'G': gp('grip', 'any_plastic'), 'M': gp('mechanism', 'gunmetal')})
shaped('gun_g3', ['BRM', 'WGS'], {'B': gp('barrel_light', 'weaponsteel'), 'R': gp('receiver_light', 'weaponsteel'),
       'M': gp('mechanism', 'weaponsteel'), 'W': gp('grip', 'wood'), 'G': gp('grip', 'rubber'), 'S': gp('stock', 'wood')})
shaped('gun_amat', [' C ', 'BRS', ' MG'], {'G': gp('grip', 'wood'), 'B': gp('barrel_heavy', 'ferrouranium'),
       'R': gp('receiver_heavy', 'ferrouranium'), 'M': gp('mechanism', 'weaponsteel'), 'C': SCOPE, 'S': gp('stock', 'wood')})
shaped('gun_m2', ['  G', 'BRM', '  G'], {'G': gp('grip', 'wood'), 'B': gp('barrel_heavy', 'ferrouranium'),
       'R': gp('receiver_heavy', 'ferrouranium'), 'M': gp('mechanism', 'weaponsteel')})
shaped('gun_lag', ['BRM', '  G'], {'B': gp('barrel_light', 'any_resistantalloy'), 'R': gp('receiver_light', 'any_resistantalloy'),
       'M': gp('mechanism', 'weaponsteel'), 'G': gp('grip', 'any_plastic')})
shaped('gun_stg77', [' D ', 'BRS', 'GGM'], {'D': SCOPE, 'B': gp('barrel_light', 'saturnite'), 'R': gp('receiver_light', 'saturnite'),
       'S': gp('stock', 'any_hardplastic'), 'G': gp('grip', 'any_hardplastic'), 'M': gp('mechanism', 'saturnite')})
print('ok')
