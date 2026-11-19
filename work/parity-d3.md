# Партия D3 — оставшиеся расхождения внешнего вида (2026-11-18)

Эталон: HbmMods 936c60c (`work/original`): render/tileentity, render/block, render/item, ClientProxy, ModBlocks/ModItems.
Скрипты: work/tmp-d3/particles.py (иконки частиц), gap.py (рендер блоков/предметов эталона против моделей порта → blockgap.tsv, itemgap.tsv), full3d.py (setFull3D/isFull3D против item/generated), ph3.py (оставшиеся legacy_placeholder).
Статусы: «авто» — проверено сборкой/серверной диагностикой ParityD3Diagnostics; «вручную» — вид в игре не проверялся, ожидает ручной проверки.

## Сделано

| Объект | Рендер эталона | Было | Сделано | Статус |
|---|---|---|---|---|
| machine_arc_welder | RenderArcWelder: `display` (выход найденного рецепта), EntityItem renderInFrame, сдвиг (0.15625, 1.125, 0), Y90, X−90, ×1.5 | предмет не рисовался, BE не синхронизировался | BE: `display` = выход совпавшего рецепта (как serialize), getUpdateTag/getUpdatePacket, рассылка при смене; рендер — render/LegacyFramedItemRender (цепочка RenderItem#doRender в рамке → FIXED) | синхронизация — авто; вид — вручную |
| machine_soldering_station | RenderSolderingStation: то же | то же | то же | авто / вручную |
| ore_bedrock, ore_bedrock_block | BlockBedrockOreTE + RenderBlockMultipass: проход 0 — bedrock, проход 1 — ore_random_(shape%10+1) с цветом TE; color=0 → клиент берёт цвет ресурса (getStackColor, amplify); предмет — только проход 0 | cube_all с текстурой порта ore_bedrock_block, цвет/форма не передавались клиенту | client/model/BedrockOreBlockModel (ModelData SHAPE, 10 моделей ore_bedrock_shape_N, tint 1 = цвет BE или средний цвет иконки ресурса), BE шлёт тег клиенту, цвет по умолчанию 0; предмет — куб bedrock; 10 текстур ore_random_* скопированы побайтно | данные — авто; вид — вручную |
| wand_air | RenderBlockWand: куб 1/16..15/16 снаружи и изнутри, грани к такому же блоку скрыты; предмет — плоская иконка (renderInventoryBlock пуст) | cube_all 16×16, предмет — куб | модель: куб 1..15 + 6 внутренних граней (зеркальный UV), cutout; WandAirBlock#skipRendering для соседнего wand_air; предмет item/generated | вручную |
| mush | getRenderType 1 (крест), предмет — плоская иконка блока | cube_all | block/cross (cutout), предмет item/generated | вручную |
| 252 блока с пустой моделью (blockstate → block/empty, machine_dummy, legacy_machine_empty) | частицы: Block#getIcon → blockIcon из setBlockTextureName / registerBlockIcons | legacy_placeholder | models/block/particle/<id>.json (пустая геометрия + particle эталона); deco_crt — по варианту (crt_clean/broken/blinking/bsod); RBMK-колонны — `<tex>_side` (RBMKBase); funnel — side; radio_torch_* — rtty_*_off; statue_elb_f — code; skeleton_holder — soul_sand, machine_ashpit — stone_bricks (ванильные имена 1.7.10 soul_sand/stonebrick) | авто (ParityD3Diagnostics: у зарегистрированных блоков нет placeholder вне списка исключений, текстуры существуют) |
| dummy_plate/port_compact_launcher, dummy_plate/port_launch_table | block_steel | placeholder через parent block/empty | particle block_steel | авто |
| acid_block, mud_block, rad_lava_block, schrabidic_block, sulfuric_acid_block | stillIcon жидкости | cube_all legacy_placeholder | текстура forgefluid/<x>_still (побайтно = оригинал) | авто |
| 33 предмета: wrench, wrench_flipped, wrench_archineer, screwdriver(_desh), hand_drill(_desh), blowtorch, acetylene_torch, arc_electrode(+3 варианта), arc_electrode_burnt(+3), designator_arty_range, ullapool_caber, schrabidium_hammer, bottle_opener, memespoon, titanium_pickaxe, titanium_shovel, 10 мотыг (schrabidium/titanium/steel/alloy/cmb/desh/cobalt/cobalt_decorated/starmetal) | isFull3D (setFull3D в классе либо ItemSword/ItemTool/ItemHoe) | item/generated | item/handheld | авто (parent), вид в руке — вручную |

Скопированные текстуры (побайтно; переименования — только регистр): ore_random_1..10; для частиц — fire_door, launch_pad, block_steel_machine, machine_crystallizer, cyclotron, machine_funnel_side, machine_large_turbine, machine_press, machine_reactor_small, machine_refinery, lilBoy→lilboy, custom, fleija, theGadget→thegadget, fatMan→fatman, ivyMike→ivymike, prototype, tsarBomba→tsarbomba, deco_pole_top, red_pylon_large, fluorescent_lamp(_off), flood_lamp(_off), cage_lamp(_off), code, substation, deco_tape_recorder (все в textures/block/).

## Сверено, изменений не требуется

| Объект | Итог |
|---|---|
| Ручные предметы из списка задачи (детонаторы, defuser, igniter, syringe_metal_*, wand_d, bomb_caller, crowbar, pipe_lead и т. д.) | уже item/handheld (STATE 2026-11-17); full3d.py новых расхождений по ним не нашёл |
| Предметы с IItemRenderer (296 по ClientProxy/ItemRenderLibrary/IItemRendererProvider) | все имеют рендер в порте (OBJ/builtin либо обёртка U4LegacyItemRenderSetup, LandmineItemModel, PurexItemModel, HexafluorideTankItemModel, LegacyMachineItemModel); кроме obj_tester (тестовый блок) |
| Блоки с getRenderType ≠ 0 (473 с blockstate) | кубами остались только перечисленные ниже; CT-блоки, кабели, трубы, леса — кодовые модели; газы — INVISIBLE как −1 в эталоне |
| JEI (gui_nei_press/cyclotron/centrifuge_gas/custom и раскладки) | приведено партией T4 (work/parity-t4.md); повторно: все используемые текстуры gui_nei* побайтно равны оригиналу, 6 отсутствующих (assembler, chemplant, cmb, deuterium, nuke, schrabidium) в handler/nei не используются |

## Не сделано

| Объект | Рендер эталона | В порте | Причина |
|---|---|---|---|
| charge_c4/dynamite/miner/semtex, pa_source/beamline/rfc/quadrupole/dipole/detector, floodlight_beam | textureName не задан → в 1.7.10 частицы «missing texture» | legacy_placeholder | в эталоне текстуры нет |
| dummy_plate_cargo, heavy_door_part, sliding_blast_door_2, sliding_gate_door, machine_tacz_ammo_assembler, machine_dummy | — | legacy_placeholder | блоки порта, в эталоне отсутствуют |
| machine_battery*, machine_fensu, machine_minirtg и прочие LegacyHbmContent-заглушки | свои рендеры | куб legacy_placeholder | неперенесённые машины (вне объёма) |
| wand_jigsaw/loot/tandem/logic | RenderBlockSideRotation: поворот UV по getRotationFromSide | куб с текстурами по граням, поворот blockstate | выбор текстур совпадает, повороты UV граней не сверены; технические блоки без вкладки |
| crane_router, crane_partitioner | IBlockMultiPass / RenderPartitioner | cube_all | не в объёме d3 (остаток записи о кранах в STATE) |
| machine_industrial_generator (блок) | RenderIGenerator (igen.obj, анимация) | cube_all | машина не перенесена (механика, мультиблок); предмет рисуется по ItemRenderLibrary |
| machine_reactor (блок совместимости) | — | куб | машины нет в порте |
| soyuz_capsule meta 3 | RenderCapsule: soyuz_lander_rust при meta 3; meta 3 ставит только HbmWorldGen (капсула на пляже, capsuleStructure) | текстура есть, состояния нет | генерация капсулы не перенесена — это объём мира, а не рендера; требует согласования |
| Предмет в display — блоки и предметы с собственным рендером | RenderItem#doRender (ветка блока ×1.25, 0.25) | ветка блока: FIXED ×1.25×0.5; предметы с BEWLR идут по ветке плоского предмета | эквивалент не проверен глазами |
| Средний цвет ресурса ore_bedrock | ColorUtil читает весь PNG (все кадры анимации) | берётся кадр 0 спрайта | платформа; отличие только у анимированных иконок |
