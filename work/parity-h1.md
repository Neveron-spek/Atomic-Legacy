# h1 — industrial generator, breeding reactor, старые батареи, рецепты f1 (2026-11-19)

Эталон 936c60c. Копия NeoNTM-wt/h1. Имя `ParityH1Diagnostics` уже занято старой партией H1, поэтому проверки этой партии — `event/ParityH1MachinesDiagnostics`, их число прибавлено к строке `checked_parity_h1_cases`.

## 1. machine_industrial_generator
Эталон: `MachineIGenerator` зарегистрирован, вкладка machineTab, размер 1×1 (`getDimensions` = 0, offset 0), `onBlockActivated` без GUI (openGui закомментирован), `TileEntityMachineIGenerator`: 21 слот, ни одна сторона их не открывает, `updateEntity` пустой. `RenderIGenerator`: igen.obj (Body, Rotor), масштаб 1/6, поворот по метаданным. Подсказка при наведении: «Industrial Generator Memorial» / «In memory of all that we have lost». Рецепта нет (на наковальне закомментирован).
| что | сделано |
|---|---|
| блок | `IndustrialGeneratorBlock` — EntityBlock, `ENTITYBLOCK_ANIMATED`; FACING от игрока, как у BlockDummyable |
| TE | `IndustrialGeneratorBlockEntity` (id `machine_industrial_generator`) без инвентаря и тика: 21 слот эталона недоступен ни игроку, ни воронкам |
| рендер | `render/IndustrialGeneratorBlockEntityRenderer` по RenderIGenerator (NORTH 180, WEST 270, SOUTH 0, EAST 90; scale 1/6; z −0.5; без отсечения граней) |
| модели | blockstate → `block/particle/machine_industrial_generator` (частицы `minecraft:block/gold_block`, как setBlockTextureName эталона); предмет — прежний рендер ItemRenderLibrary (U4) |
| удалено | `models/block/machine_industrial_generator.json` (куб) и `textures/block/industrial_generator.png` — текстуры нет в эталоне |
| оверлей | `client/ParityH1LookOverlays`: заголовок 0xFF8000, тень 0x804000; в `HbmLookOverlayApi.OverlayData` добавлен цвет тени (bgCol printGeneric; по умолчанию прежний 0x404000) |
| ContainerIGenerator/GUIIGenerator | не перенесены: в эталоне недостижимы |

## 2. machine_reactor_breeding
В порте работает под id `machine_reactor_breeding` (эталон: `setBlockName("machine_reactor")`). Это сложившееся соответствие порта (t3, M2, e2). Блок совместимости `machine_reactor` (`LegacyReactorCompatibilityBlock`) остаётся скрытым и без рецепта. Переименовывать id не стал: затронуло бы регистрации, данные, языки и сохранённые миры.
Рецепты BreederRecipes: 31 JSON, flux совпадает (×2 dual, ×3 quad, метеоритный меч 1000). Исправлены расхождения:
- `canPlaceItem`/`canPlaceItemThroughFace`: в слот 0 принимается любой предмет (isItemValidForSlot i==0); раньше — только предметы с рецептом.
- `setItem`/`removeItem` больше не сбрасывают прогресс (эталон сбрасывает его только через canProcess на следующем тике).
- canProcess: выход того же предмета и `count < maxStackSize`, как в эталоне.
- Shift-клик в меню: в слот 0 идут rod/rod_dual/rod_quad любого варианта (ItemBreedingRod); травлёный метеоритный меч не переносится.

## 3. Старые батареи (potato, battery, lithium, schrabidium, dineutronium) — согласованное отступление
В эталоне `TilePortShapes.solderer` при метаданных 2..5 даёт порты UNKNOWN, а `getRelevantMode(true)` читает кэш, который никто не обновляет. По решению пользователя от 2026-11-19 это очевидные ошибки, они исправлены:
- `LegacyBatteryBlockEntity` реализует `PowerEndpoint` на 6 сторонах (MachineBattery.canConnect = true, поэтому кабель рисует «рукав»). FE-capability не добавлялась.
- Режим: сигнал редстоуна (isBlockIndirectlyGettingPowered → hasNeighborSignal) → redHigh, иначе redLow. Отдача в режимах output/buffer — `min(power, max/600)` за тик, приём в input/buffer — `min(max−power, max/200)` (getProviderSpeed/getReceiverSpeed). Приоритет берётся с кнопки GUI (LOW/NORMAL/HIGH).
- Порядок тика как в updateEntity: сначала сеть, потом слоты 1 и 0. При установке и снятии блока топология сети помечается изменённой.
- `ParityD2Diagnostics`: проверка «не PowerEndpoint» заменена на «PowerEndpoint, без FE».

## 4. Рецепты из parity-f1 §2 (22 отсутствующих)
Сверено вручную, потому что `work/parity_e3_recipes.py` не запускается: нет модуля `parity_e3_recipe_alias.py` (VARIANTS), в основном проекте его тоже нет.
| рецепт | итог |
|---|---|
| conveyor_wand EXPRESS ×8 | есть `conveyor_express` (8 обычных + 1000 mB смазки) |
| crate_desh, crate_tungsten, safe | есть `crate_desh_upgrade`, `crate_tungsten_upgrade`, `safe`; узоры и ингредиенты совпадают с CraftingManager 273-276 |
| ingot_pu_mix (MineralRecipes 419, `nuggetPluonium240`) | отступление: опечатка исправлена на `nuggetPlutonium240`. В порте `nuggetPlutonium239/240` и `tinyPu239/240` — одни и те же теги `c:nuggets/pu239/pu240`. Поэтому исправленный рецепт совпадает с существующим `ingot_pu_mix_mineral_420` (6×pu239 + 3×pu240 → 1). Отдельный дубль-JSON не добавлялся |
| copper_block, pink_stairs, vinyl_tile, ink, platemetal, gun_kit_2, fence_metal, scrap_plastic, zirnox depleted | эквивалентны (см. f1) |
| obj_tester | не перенесён: в порте нет предмета, блок технический и скрытый |
| oc_cable_paintable | OpenComputers, интеграции нет |
| sand_mix QUARTZ, powder_meteorite | есть с тегом AE2 |
| depth_* | исключены пользователем |
Файлы рецептов не менялись, `updateRecipeBaseline` не требовался.

## Файлы
Новые: `tileentity/IndustrialGeneratorBlockEntity`, `render/IndustrialGeneratorBlockEntityRenderer`, `client/ParityH1LookOverlays`, `event/ParityH1MachinesDiagnostics`, `models/block/particle/machine_industrial_generator.json`.
Изменены: `blocks/IndustrialGeneratorBlock`, `blocks/LegacyBatteryBlock`, `tileentity/LegacyBatteryBlockEntity`, `tileentity/BreederReactorBlockEntity`, `inventory/container/BreederReactorMenu`, `registry/HbmBlockEntities`, `render/HbmClientRenderers`, `api/client/HbmLookOverlayApi`, `client/HbmLookOverlay`, `event/ParityD2Diagnostics`, `event/BuildingPortDiagnostics`, blockstate и модель предмета machine_industrial_generator.
Удалены: `models/block/machine_industrial_generator.json`, `textures/block/industrial_generator.png`.
build.gradle: без правок.

## Ручная проверка
1. Industrial Generator: поставить блок. Небольшая модель igen развёрнута по взгляду игрока, ПКМ не открывает GUI, при наведении — оранжевая надпись «Industrial Generator Memorial» и строка «In memory of all that we have lost». Частицы — золотой блок.
2. Старая батарея (`/give @s reinhardtshbm:machine_battery`) рядом с красным кабелем: у кабеля есть рукав. Режим «вывод» отдаёт энергию машине в сети, «ввод» заряжается от генератора. Редстоун переключает на второй режим, кнопка приоритета работает.
3. Реактор-размножитель у исследовательского реактора: в слот 0 можно положить любой предмет, работают только стержни с рецептом. Shift-клик переносит стержни (rod/dual/quad), меч — нет. Прогресс не сбрасывается, когда в стопку входа добавляют стержни.
