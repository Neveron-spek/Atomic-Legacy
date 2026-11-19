# m1 — позы предметов по рендерам эталона, plant_dead (2026-11-19)

Эталон 936c60c. Имя диагностики `ParityM1ItemDiagnostics` (`ParityM1Diagnostics` уже занят этапом RBMK).

## 1. Предметы с придуманными позами → рендер эталона

| Предмет | Рендер 1.7.10 | Порт теперь |
|---|---|---|
| machine_ammo_press | RenderAmmoPress#getRenderer | M1LegacyItemRenderers.AmmoPress (U4) |
| machine_blast_furnace | RenderBlastFurnace#getRenderer | .BlastFurnace |
| machine_boiler | RenderBoiler#getRenderer (meta 1 burst — в порте нет такого стека) | .Boiler |
| machine_industrial_boiler | RenderIndustrialBoiler#getRenderer | .IndustrialBoiler |
| chimney_brick / chimney_industrial | RenderChimneyBrick/Industrial#getRenderer (без cull) | .ChimneyBrick / .ChimneyIndustrial |
| machine_condenser_powered | RenderCondenser#getRenderer | .PoweredCondenser |
| machine_tower_small / _large | ItemRenderLibrary | .TowerSmall / .TowerLarge |
| machine_radar | ItemRenderLibrary (без cull, вращение тарелки) | .Radar |
| machine_radar_large | RenderRadarLarge#getRenderer | .RadarLarge |
| machine_radgen | ItemRenderLibrary (Base, Rotor, Light зелёный без текстуры; без Glass) | .Radgen |
| sat_dock | ItemRenderLibrary (cargo_pad) | .SatDock |
| boat | ItemRenderLibrary (duchessgambit) | .Boat |
| rbmk_autoloader | RenderRBMKAutoloader#getRenderer | .RbmkAutoloader |
| boltgun | ItemRenderBoltgun (+ RECOIL ствола в 1-м лице) | .Boltgun; BoltgunItem — IAnimatedItem, RECOIL (1,0,1)/50 мс → 0/100 мс, playAnimation на сервере как ItemBoltgun |
| transition_seal | BlockDummyable renderType −1, IItemRenderer нет (нет в ItemRenderLibraryDoors) | плоская иконка block/transition_seal |
| skeleton_holder | renderType −1, рендера нет, setBlockTextureName("soul_sand") | плоская иконка minecraft:block/soul_sand |
| barrel_corroded, barrel_antimatter | BlockFluidBarrel → RenderBarrel (ISBRH, блочные позы) | FluidBarrelItemRenderer (как barrel_steel) |
| rbmk_loader, rbmk_steam_inlet, rbmk_steam_outlet | RBMKLoader/Inlet/Outlet: renderType 0, один blockIcon | cube_all (и в мире — BER рисует ту же модель); придуманная форма с выступами template/rbmk_port_block удалена |

radar/radar_large/radgen фактически шли через LegacyMachineItemRenderer (записи ObjMachine были мёртвыми): там поза GUI как ItemRenderBase, но не в руке/на земле, и radgen рисовал лишнее стекло — заменено.

Ресурсы: OBJ скопированы побайтно в models/entity/legacy_{ammo_press,blast_furnace,boiler,industrial_boiler,chimney_brick,chimney_industrial,condenser,tower_small,tower_large,radar,radar_large,radgen,cargo_pad,rbmk_autoloader,boltgun}.obj; текстуры tower_small.png и cargo_pad.png (textures/models/machines) — побайтно; остальные текстуры — уже имевшиеся побайтные копии.

Код: из ObjMachineItemRenderer удалены 19 профилей, ветки и методы поз; из RbmkComponentItemRenderer — 4 метода; из LegacyMachineItemRenderer — радары/radgen, из LegacyMachineBlockEntityRenderer — renderItemRadar/LargeRadar/RadiationGenerator, из LegacyMachineItemModel — 3 id. ObjMachineBlockItem.LEGACY_RENDERED_IDS — opt-out OBJ-рендера (без дублей IClientItemExtensions); HbmHeavyDoorBlockItem больше не ставит рендер.

build.gradle: (1) validateRendererCullingBounds — удалён список fixedObjItemPoses (19 правил с масштабами 0.82/0.85, 0.78/0.82 и центрами); (2) там же удалены требование `applyLargeRadarItemPose(` в LegacyMachineItemRenderer и проверка матрицы `applyRadiationGeneratorItemPose(` (+ запрет форм, + HEAD/NONE). Удалён файл models/block/template/rbmk_port_block.json.

## 2. plant_dead

BlockDeadPlant (BlockEnumMulti, multiName=false, multiTexture=true): 5 вариантов GENERIC, GRASS, FLOWER, BIGFLOWER, FERN.
- Kind переупорядочен по ординалам эталона, состояние по умолчанию GENERIC (meta 0).
- Предмет DeadPlantBlockItem (LegacyVariantBlockItem, CUSTOM_DATA variant / CUSTOM_MODEL_DATA 0..4), все варианты называются tile.plant_dead (новых языковых ключей не нужно); иконки — плоские текстуры (renderType 1), item/legacy_flat/plant_dead_*.json; во вкладке блоков 5 стеков.
- canPlaceBlockOn: добавлен waste_earth (было без него). Рамка выбора — полный блок (BlockEnumMulti не BlockBush; было 2..14×13). Material.plants: разрушается поршнем. Дропа нет (getItemDropped null) — как было; pick block даёт вариант.
- Генерация MapGenBedrockOil/MapGenBubble: rand.nextInt(5) по всем вариантам (было GRASS/GENERIC). OilSpot — без изменений.
- Текстуры plant_dead_*.png — побайтно эталон (проверено sha1).

## Проверки
- work/verification-m1.log: assemble, validateJarIntegrity, validateLangFiles, validateRegistrySources, validateObjModels, validateRecipeBaseline, validateEntityRendererCullingBounds, validateRendererCullingBounds, checkBuildingAudit, checkItemAudit — BUILD SUCCESSFUL; HBM_ITEM_AUDIT errors=0 warnings=13; building-audit: checked_parity_m1_item_cases=67 без проблем, creative_building_entries=741. Рецепты не менялись (baseline не обновлялся).
- work/verification-m1-client.log: runItemRenderAudit -PitemRenderSheets=machines,building,control — stacks=6231 errors=0 warnings=0, ошибок atomiclegacy в логе нет. Листы machines 3/5/6/8/9/11, building 11/20, control 39 просмотрены (вырезка run-visual/screenshots/m1_crops.png): позы зависят от контекста, кубы RBMK, 5 иконок plant_dead.
- machine_radar, machine_radar_large, sat_dock во вкладках порта не показываются — на листах не проверены.

## Остаток / ожидает ручной проверки
- Вид всех перечисленных предметов в руке (1-е/3-е лицо), на земле, в рамке; вращение тарелок радаров в слоте; отдача ствола boltgun при выстреле.
- boiler_burst (meta 1) не перенесён — в порте нет такого стека.
- Мир: rbmk_loader/steam_inlet/steam_outlet теперь обычные кубы (как в 1.7.10) — проверить вид в мире.
