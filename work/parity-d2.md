# parity d2 — остаток списка «не сделано» из b1/c1 (2026-11-18)

Эталон: HbmMods 1.7.10, коммит 936c60c (`work/original`). Статусы: «реализовано» — код перенесён; «авто» — проверено на выделенном сервере (`ParityD2Diagnostics`, `ParityD2GunDiagnostics`, `ParityD2MiscDiagnostics`, строка `checked_parity_d2_cases`); «ручная» — ожидает проверки в клиенте (клиент не запускался).

## 1. Бактерии и взвешенный выход химзавода

| Объект | Оригинал | Сделано | Статус |
|---|---|---|---|
| bacteria ×4 | `ItemEnumMulti(EnumBacteriaType)`: AGAR, ANTHRAX_INERT, ANTHRAX_DEAD, ANTHRAX_MUTATED; partsTab | `bacteria_agar`, `bacteria_anthrax_inert`, `bacteria_anthrax_dead`, `bacteria_anthrax_mutated` (`HbmItems.BACTERIA_ITEMS`), вкладка parts | реализовано, авто |
| briquette PEAT | `EnumBriquetteType.PEAT`; FuelHandler `single * 6`; DiFurnace 200 | `briquette_peat`; `LegacyFurnaceFuels` 1200, кирпичная печь 1200 | реализовано, авто |
| dirt (SWAMP) | `BlockEnumMulti(Material.ground, DirtType)`, твёрдость 5.0 (второй `setHardness`), звук gravel, blockTab; названия в lang нет | блок `dirt_swamp` (5.0 / 5.0), вкладка block; имя — сырой ключ оригинала `tile.dirt.swamp.name` | реализовано, авто |
| выход рецепта химзавода | `GenericRecipes.IOutput`: `ChanceOutput` (шанс на каждый предмет стопки), `ChanceOutputMulti` (выбор по весу); `ModuleMachineBase`: выходы сверх числа слотов отбрасываются, взвешенному выходу нужен пустой слот | `ChemicalPlantRecipe.ItemOutput` (кодек JSON: стопка / `{"single"}` / `{"multi"}`, сетевой кодек), `canFitItemOutputs`/`produceOutputs` химзавода и химфабрики по `ModuleMachineBase`, подсказка выбора рецепта («One of:» и проценты как `getLabel`), поиск по всем вариантам, JEI (`Slot.weighted` / `Slot.chance`) | реализовано, авто (кодек, распределение 1/79/20, шанс, машина); экраны и JEI — ручная |
| chem.agar | 100 т, 250 HE; 1000 salient + сахар + опилки → 1 agar | `chemical_plant/agar.json` | реализовано, авто |
| chem.growinertanthrax / growmutatedanthrax | 600 т, 500 HE; 4000 воды + бактерия + agar → 2 бактерии | 2 JSON | реализовано, авто |
| chem.mutateanthrax | 600 т, 2000 HE; 4000 воды + inert + agar + самородок Co-60 → `ChanceOutputMulti(MUTATED вес 1)`, `ChanceOutput(DEAD, 79)`, `ChanceOutput(INERT, 20)`, `ChanceOutput(nuclear_waste_tiny)` | JSON повторяет запись оригинала буквально | реализовано, авто (цикл машины: mutated + dead + inert, отходы не выдаются) |
| rock.swamp | 100 т, 25 HE; dirt SWAMP → по весу: 4 peat (150) / dirt (49) / inert anthrax (1); группа автопереключения crushing | `rock_mill/swamp.json` | реализовано, авто |

**Находка по оригиналу.** В `chem.mutateanthrax` скобка `ChanceOutputMulti` закрыта после первого элемента. Пул состоит из одного `MUTATED`, остальные три выхода — обычные (вес вне пула не действует). Рецепт в оригинале всегда даёт mutated + dead + inert; четвёртый выход (отходы) отбрасывается, потому что у машины три выходных слота. Перенесено как есть. Если нужен «задуманный» вариант (1 % / 79 % / 20 % + отходы) — это изменение содержания, нужно решение пользователя.

## 2. Устаревшие, но зарегистрированные машины

| Объект | Оригинал | Сделано | Статус |
|---|---|---|---|
| machine_difurnace_off / on | `MachineDiFurnace` + `TileEntityDiFurnace`: слоты 0/1 вход, 2 топливо, 3 выход; maxFuel 12800, 400 т, 1 топлива за тик; рецепты `BlastFurnaceRecipes` (оба порядка, размер стопки не важен); таблица топлива (уголь 200 … лава 12800); стороны загрузки по слотам (ПКМ по пустому слоту); off — machineTab, on — без вкладки, свет 1.0 | `LegacyDeprecatedFurnaceBlock(DIFURNACE)` + `LegacyDeprecatedFurnaceBlockEntity`; пара off/on — один класс, работа = свойство `lit` (как у кирпичной печи порта); `covered` — высокие текстуры под надстройкой | реализовано, авто (топливо, 400 т, расход, лава→ведро, стороны, свет); вид/GUI/частицы — ручная |
| machine_difurnace_extension | `MachineDiFurnaceExtension`: модель `difurnace_extension.obj`, открывает GUI печи снизу, прокси инвентаря; печь под ней идёт ×3; machineTab | `DiFurnaceExtensionBlock`, obj-модель, item handler-прокси | реализовано, авто (×3: 134 т, прокси, covered); вид — ручная |
| machine_difurnace_rtg_off / on | `MachineDiFurnaceRTG` + `TileEntityDiFurnaceRTG`: 9 слотов (2 входа, выход, 6 пеллет), нужно тепло ≥ 15, прогресс += тепло, 1200; off — machineTab, on — скрыт | `Kind.DIFURNACE_RTG`; порядок вызовов `hasPower()` (каждый вызов старит пеллеты) сохранён | реализовано, авто (скорость, 1200, правила слотов); GUI — ручная |
| machine_rtg_furnace_off / on | `MachineRtgFurnace` + `TileEntityRtgFurnace`: вход, 3 пеллеты, выход; ванильная плавка, прогресс += тепло, 1000; без вкладки | `Kind.RTG_FURNACE`; прогресс не сохраняется при перезагрузке (в оригинале ключи NBT `cookTime`/`CookTime` не совпадают) | реализовано, авто; GUI — ручная |
| GUI трёх печей | `GUIDiFurnace`, `GUIMachineDiFurnaceRTG`, `GUIRtgFurnace`, контейнеры | `LegacyDeprecatedFurnaceMenu` / `LegacyDeprecatedFurnaceScreen` (один тип меню, вид по kind) | реализовано, ручная |
| machine_battery_potato, machine_battery, machine_lithium_battery, machine_schrabidium_battery, machine_dineutronium_battery | `MachineBattery(maxPower)` 10k / 1M / 50M / 25G / 1T + `TileEntityMachineBattery`: 2 слота (0 — разрядка предмета в блок, 1 — зарядка предмета), режимы redLow/redHigh, приоритет, компаратор, сохранение данных в дропе, подсказка, оверлей; вкладка null | `LegacyBatteryBlock` / `LegacyBatteryBlockEntity` / `LegacyBatteryMenu` / `LegacyBatteryScreen`; кнопки через `BatterySocketControlPayload`; оверлей в `HbmLookOverlay` | реализовано, авто (ёмкости, слоты, компаратор, кнопки, перенос данных); GUI/оверлей — ручная |
| capacitor_gold / niobium / tantalium / schrabidate | `MachineCapacitor` без TileEntity (пассивный блок, 6 направлений), вкладка null | ещё 4 `CapacitorBlock`, модель `capacitor.obj` порта со своими текстурами | реализовано, авто (класс, нет BE, скрыты); вид — ручная |

**Находка по оригиналу.** `TileEntityMachineBattery` строит порты через `TilePortShapes.solderer(x, y, z, getBlockMetadata())`. Форма рассчитана на метаданные мультиблока (`meta - 10`), у батареи они 2..5, поэтому все направления портов — `ForgeDirection.UNKNOWN`: узел стоит в самом блоке и соединяется только сам с собой. Кабель рядом с блоком в его сеть не входит. В оригинале на этом коммите блоки LEGACY обмениваются энергией только через два слота предметов. Перенесено так же: блок не является `PowerEndpoint`. Режимы и приоритет переключаются и сохраняются, но на сеть не влияют (как в оригинале).

## 3. Отладочное оружие

| Объект | Оригинал | Сделано | Статус |
|---|---|---|---|
| ammo_debug | `GunFactory`: `new Item()`, текстура `ammo_45`, без вкладки; вариантов нет | `HbmItems.AMMO_DEBUG`, убран из «выведенных» | реализовано, авто |
| gun_debug | DEBUG, прочность 600, draw 15, inspect 23, L_CLASSIC; ресиверы: урон 10 и 5, delay 14, reload 46, jam 23, магазин 12, звук `weapon.44Shoot`; ПКМ — второй ресивер | `GunFactory.createDebug`, `Lego.LAMBDA_SECOND_*`, `Orchestras.DEBUG_ORCHESTRA`, `ItemRenderDebug`, HUD | реализовано, авто (параметры, циклы через FakePlayer); вид и анимации — ручная |
| конфиги ammo_debug / ammo_debug_shot | spread 0.01 / 0.05, 1 / 6 снарядов, рикошет 45, гильза CASING44 | `StandardAmmoType.DEBUG`, `DEBUG_SHOT`, `CasingConfig.DEBUG44` (в конец enum) | реализовано, авто |
| gun_minigun_dual | DEBUG, два конфига: прочность 50000, draw/inspect 20, L_CIRCLE, урон 6 / 6 (ServerConfig), delay 1, авто, лента r762×6; ЛКМ — левый, ПКМ — правый; моды SLOWDOWN, SPEEDUP, калибры 5.56 и .50 | `XFactory762mm.createMinigunDual`, `OrchestrasC3.ORCHESTRA_MINIGUN_DUAL`, `ItemRenderMinigunDual`, конфиги `GUN_MINIGUN_DUAL_LEFT/RIGHT_DAMAGE`, моды | реализовано, авто; вид — ручная |
| скрытость | `WeaponQuality.DEBUG`: без вкладки и рецептов | `=hidden` в `creative-tabs.properties`, рецептов нет | авто |

## 4. Не сверявшееся из b1

| Объект | Оригинал | Состояние порта / сделано | Статус |
|---|---|---|---|
| nugget_raw | `ItemAutogen(NUGGET)` только для 8 материалов: titanium, copper, tungsten, aluminium, boron, sodium, strontium, lithium; OreDict `nugget<Name>`; форма nugget тигля; пара слиток ↔ 9 самородков; опасности Li/Na/Sr | не было; добавлены 8 предметов `nugget_raw_<mat>`, теги `c:nuggets/<mat>`, 16 рецептов, литьё и переплавка, опасности | реализовано, авто; текстуры — ручная |
| plant_flower / plant_tall | 6 цветков (FOXGLOVE, TOBACCO, NIGHTSHADE, WEED, CD0, CD1) + 4 высоких (WEED, CD2–CD4) | варианты и цепочка роста были и совпадают; исправлено: костная мука на foxglove/tobacco/nightshade, верх высокого растения исчезает без дропа, добавлены креативные стадии `plant_flower_cd1`, `plant_tall_weed`, `plant_tall_cd2/cd3/cd4`, растения перенесены во вкладку блоков (blockTab) | реализовано, авто; рост в игре — ручная |
| EntityCloudTom | сущность 20×40, 500 тиков, вспышка неба каждый тик; `RenderCloudTom`: расширяющееся кольцо с текстурой `tomblast`; звука нет | в порте было 500 частиц (придумано); добавлены `entity_moonstone_blast` (`LegacyCloudTomEntity`), рендер, текстура; частицы убраны | реализовано, авто (параметры); вид — ручная |
| EntityFallingBlockNT | наковальни NTM (без урона), обвал нависающих блоков под осадками (`EntityFalloutRain`), 2 действия LogicBlock | наковальни и LogicBlock совпадали; обвала от осадков не было — добавлен (`NuclearFalloutTerrainEffects`) | реализовано, авто (составные части); в игре — ручная |
| ItemSoup и соседние | `ItemSoup(6)`: голод 6, 0.6, стак 1, миска; `ingot_smore` = `ItemFood(10, 20F)` | совпадает | расхождений нет, авто |
| /ntmclient | `CommandReloadClient`: клиентская, help/list/reload/get/set | есть с партии S2 (`client/ParityS2ClientCommands`) | расхождений нет |
| /ntmpackets | `CommandPacketInfo`: отладчик пула потоков `PacketThreading` | не применимо: в порте нет `PacketThreading` (сеть NeoForge), команде нечего показывать | не применимо |

## Расхождения и ограничения

- Кабель рядом со старой батареей: в оригинале рисуется «рукав» к блоку (канал `canConnect` = true), хотя энергия не идёт; в порте блок не `PowerEndpoint`, рукава нет.
- Пары off/on печей: в оригинале это два блока, которые подменяют друг друга; в порте оба id зарегистрированы, но рабочее состояние — свойство `lit` того блока, который поставлен (соглашение основы, как у `machine_furnace_brick`). Блок `_on`, поставленный командой, в простое остаётся `_on` с `lit=false`.
- Названия рецептов химзавода с `setupNamed` (в т.ч. `chem.growinertanthrax`, `chem.mutateanthrax`) и их значки `setIcon` экран выбора порта не показывает — это было до d2 у всех рецептов и в этой партии не менялось. Ключи `chem.*` в lang порта отсутствуют.
- Подсказка выхода: обычные выходы — в формате порта (` - Имя xN`), взвешенные/вероятностные — в формате оригинала (`One of:`, проценты).
- GUI старой батареи: из двух наложенных подсказок оригинала (общая энергия и «энергия + дельта») показывается вторая, первая в оригинале полностью закрыта ею.
- dirt_swamp: инструмент добычи не назначен (как у прочих «земляных» блоков порта).
- Обвал от осадков: оригинал пропускает `BlockDummyable`; в порте пропускаются все блоки мода с block entity (одноблочные машины не обваливаются).
- Облако Тома: дальность отслеживания ограничена дистанцией сервера (в оригинале 1000 блоков); первая оболочка имеет UV = 1/0, как в оригинале.
- `gun_debug`, `ammo_debug`: названия есть только в pt_BR оригинала — в остальных локалях показывается ключ.
- Индексы `BulletConfig.configs`: два новых конфига создаются при регистрации `gun_debug`; лениво создаваемые конфиги (ракеты NCRPA) сдвигаются на 2 относительно прежних сборок.
- В `FoundryMoldItem.outputFor` слиток натрия отливается в `powder_sodium` (в оригинале `ingot_raw` sodium) — замечено, не менялось.

## Ресурсы (побайтные копии, переименования)

- Предметы: `bacteria.{agar,anthrax_inert,anthrax_dead,anthrax_mutated}.png`, `briquette.peat.png` — те же имена; блок `dirt.swamp.png` — то же имя.
- Блоки машин (те же имена, `textures/blocks` → `textures/block`): `difurnace_*` ×9, `rtg_difurnace_*` ×4, `machine_rtg_furnace_{base_alt,off_alt,on_alt,side_alt}`, `battery_*` ×15, `capacitor_{gold,niobium,tantalium,schrabidate}_{top,side,bottom,inner_top,inner_side}`.
- GUI: `RTGfurnace.png` → `textures/gui/rtgfurnace.png`; `processing/gui_rtg_difurnace.png`, `storage/gui_battery.png` — те же имена; `GUIDiFurnace.png` уже был как `guidifurnace.png` (совпадает побайтно).
- Модель `models/blocks/difurnace_extension.obj` → `models/obj/machines/difurnace_extension.obj` (+ `mtllib`/`usemtl`, новый `.mtl` — техническая адаптация, как у `capacitor_copper`).
- Оружие: `textures/models/weapons/debug_gun.png`, `minigun_dual.png` — те же имена.
- `textures/models/explosion/tomblast.png` — то же имя.
- `nugget_raw_<mat>.png` ×8 — сгенерированы из `nugget_raw.png` алгоритмом оригинала (`RGBMutatorInterpolatedComponentRemap`), как прочие автоген-текстуры порта; не побайтные копии.
- Языки (из lang оригинала): бактерии и торф (en, de); названия блоков машин и конденсаторов, `container.diFurnace` / `diFurnaceRTG` / `rtgFurnace` / `battery`, `battery.priority.*`, `desc.gui.rtgBFurnace.desc`, `desc.gui.rtg.pelletHeat` — все локали, где ключ есть; `gun_minigun_dual` (7 локалей), `gun_debug` / `ammo_debug` (pt_br); `nugget_raw_*` (en, ru — по шаблону `item.nugget_raw.name` + `hbmmat.*`).

## Изменения вне нового кода

- `build.gradle`: без изменений.
- Рецепты: 21 новый (4 `chemical_plant/*anthrax*`+`agar`, `rock_mill/swamp`, 16 `nugget_raw_*`) — выполнен `updateRecipeBaseline`.
- `ParityK3DDiagnostics`: список `NOT_PORTED` пуст; строка таблицы `chem.mutateanthrax` — счётчики выходов `1,1,1,1` вместо артефакта разбора.
- `LegacyHbmContent.RETIRED_LEGACY_BLOCK_IDS`: убраны 11 id (батареи, конденсаторы, machine_rtg_furnace_*). `HbmItems.RETIRED_LEGACY_CATALOG_ITEM_IDS`: убран `ammo_debug`.
- `creative-tabs.properties`: `machine_difurnace_on`, `machine_rtg_furnace_off/on` = hidden; `dirt_swamp` = block; три DEBUG-предмета = hidden; 10 растений = block.
- `data/minecraft/tags/block/mineable/pickaxe.json`: +16 блоков машин.
- `BatterySocketControlPayload.handle`: ветка для `LegacyBatteryMenu`.
- `GunModsG1Diagnostics` / `GunModsG2Diagnostics`, `ParityS2ConfigTable`: учтён `gun_minigun_dual`. `ParityQ1Diagnostics`: строка `rock_mill/swamp`.

## Проверки

`work/verification-parity-d2.log`: assemble, validateJarIntegrity, validateLangFiles, validateRecipeJsonSchemas, validateRecipeSemantics, validateRegistrySources, validateObjModels, validateRecipeBaseline, validateLegacyGlobalGameplayParity, validatePowerParity, validateCapacitorParity, checkBuildingAudit, checkItemAudit — BUILD SUCCESSFUL; `checkBuildingAudit` errors=0, `checked_parity_d2_cases=360`; `checkItemAudit` errors=0. Клиент не запускался.

По ходу проверки исправлено:
- рост горчичной ивы CD2→CD3→CD4: при смене стадии нижняя половина откатывалась к CD0 (обновление формы от уже заменённой верхней половины) — верхняя половина теперь ставится без обновления формы;
- `ParityQ1Diagnostics`: в таблицу камнедробилки добавлена строка `rock_mill/swamp`;
- `zh_cn.json`: 14 ключей (бактерии, торф, dirt_swamp, nugget_raw_*) со значениями en_us — требование validateLangFiles; в zh_CN оригинала этих ключей нет, 1.7.10 показывал бы английский текст.
