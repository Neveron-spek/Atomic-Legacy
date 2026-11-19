# e3 — рецепты верстака/печи/топливо, подсказки и свойства, второй проход по багам (2026-11-19)

Эталон 936c60c. Подробные таблицы частей: `work/parity-e3-tooltips.md` (подсказки, свойства), `work/parity-e3-bugs.md` (баги, данные игрока, команды, 58 пакетов).
Диагностика: `event/ParityE3Diagnostics` (+ `ParityE3BugDiagnostics`, `ParityE3TooltipDiagnostics`), строка `checked_parity_e3_cases` в BuildingPortDiagnostics.

## 1. Рецепты верстака, печь, топливо

Инструменты: `work/parity_e3_recipes.py` (разбор CraftingManager + crafting/*: addRecipeAuto/addShapelessAuto/GameRegistry.addRecipe/addShapelessRecipe/addSmelting, раскрытие вспомогательных методов (addMineralSet, addBillet, addTool, addArmor, add1To9…), циклов `for` по массивам/диапазонам, ShapedOreRecipe/ContainerUpgradeCraftingHandler; OreDictionary из OreDictManager.registerOres/registerGroups и автогенерации Mats; условия 528/LBSM/expensive/else), `parity_e3_recipe_alias.py` (соответствия имён), `parity_e3_recipe_fix.py` (переписывание ингредиентов и генерация отсутствующих), `parity_e3_recipe_manual.py` (ручные правки).

| | число |
|---|---|
| Рецептов эталона разобрано (уникальных) | 2257 (shaped 1535, shapeless 589, smelt 133); 64 вызова не разбираются статически (хендлеры RBMK/MKU/Cargo/Scraps/Grenade, slab/stairs recipeGen, Mekanism, цикл по `Fluids.getAll`, ItemScraps, тележки) |
| Рецептов порта (crafting + smelting) | 2529 |
| Совпало полностью | 2027 |
| Исправлено ингредиентов в существующих рецептах | 74 файла (скрипт) + 13 ручных |
| Добавлено отсутствующих рецептов | 93 сгенерированных + 16 ручных (14 ore_byproduct, ртуть из RTG-пеллеты, переплавка scrap_plastic) |
| Удалено рецептов без аналога в эталоне | 8 (alloy_sword, centri_stick, oil_pipe, piston_inserter, pipes_steel, billet_balefire_gold, billet_flashlead — в эталоне только PUREX, ingot_lead_from_wire_lead) |
| Опыт плавки исправлен | 12 (рудный nether/gneiss: эталон 12/24/256/2/5/10, в порте было вдвое меньше) |
| Топливо (FuelHandler) | 32 значения сверены — расхождений нет |

Важнейшие исправления:
- Резиновые ингредиенты `ANY_RUBBER` (латекс + резина) были сужены до `c:ingots/rubber` в 33 рецептах → `c:ingots/any_rubber`.
- `sand_mix` (BlockNTMSand BORON/LEAD/URANIUM/POLONIUM/QUARTZ): крафты давали дубли `sand_boron`/`sand_lead`/`sand_uranium`/`sand_polonium`, а PUREX/дуговая печь/плавка принимали их же. Теперь как в эталоне: результат — `sand_mix` нужного варианта; PUREX (3), дуговая печь (кварц), печь (4) принимают `sand_mix`. Рецепт кварцевого песка-дубля (`sand_quartz`) удалён (в эталоне есть только sand_mix QUARTZ). Дубли-блоки остались зарегистрированными (их удаление — вне e3).
- Неверные материалы: custom_fall и padlock_unbreakable (plate_bismuth/dineutronium → saturnite), vent_chlorine_seal (ingot_dineutronium → saturnite), powder_nitan_mix (iron → iodine), crystal_charred (gravel_obsidian → блок алюминия), powder_desh_ready (бак ртути → капли ртути), radio_autocal (обычная медная пластина → литая), battery_sc_empty (литая свинцовая → обычная), ammo_shell_explosive×3 (TNT/TATB → пластиковая взрывчатка), fluid_identifier_multi (analog → vacuum tube), watz_pellet_du (уран → U-238), assembly_nuke (литая → обычная), billet_mox_fuel (pu_mix → Pu-239), powder_flux (уголь → древесный уголь), литейные блоки (stone_slab → любые каменные плиты как `Blocks.stone_slab` с wildcard), furnace_iron/reinforced_sand (wildcard stonebrick/sandstone), ams_core_thingy/elec_*/power_net_tool/struct_launcher_core (любая батарея → battery_lead/quantum по эталону), жидкостные ингредиенты `Fluids.X.getDict(N)` → любой контейнер с N мБ (вёдра/баки, `legacy_fluid_container`) в 15 рецептах.
- `part_piston_hydraulic` (дубль) → `part_generic_piston_hydraulic` в 5 рецептах (как EnumPartType.PISTON_HYDRAULIC).
- Восстановлены: слитки из 9 тонких проводов (7), биомасса (11), бумага из опилок, ведро из стали, факелы из лигнита/кокса, бирка и поводок, смола из одуванчиков, BDCL, порох (2 рецепта без дубликатов), глина, динамит, заготовки из самородков/обратно (~20), MOX/Pu/Am/U-смеси из tiny-ключей, ядерные отходы 9↔1, поляризованное стекло, яйцо balefire, апгрейды stack/ejector уровней 2–3, электропоршень, анкерный пульт, плавки (брикеты → кокс/древесный уголь, гравий, обсидиан, алмаз, тринитит, стекло пепла, базальт, шрараний, комбинированный лом) и др. (полный список — вывод `python work/parity_e3_recipe_fix.py --gen`).
- Теги: `c:nuggets/niobium` и `c:nuggets/cobalt` дополнены фрагментами (в эталоне `nuggetNiobium/Cobalt` включает fragment_*), `c:concretes` приведён к ANY_CONCRETE (concrete, smooth, asbestos, ducrete, ducrete_smooth, colored, colored_ext; без кирпичей).
- ItemMetaUpgrade: базовые рецепты дают уровень 1, рецепты уровней 2–3 принимают предыдущий уровень по `custom_model_data` (вход уровня 1 — любой upgrade_stack/ejector: компонентный «точно без данных» в 1.21.1 не выразить).

Не исправлено / осталось (вывод `review`):
- 138 рецептов 1→9 / 9→1 в порте shapeless вместо shaped (результат идентичен; не менялось).
- 2 формы: reinforced_glass_pane/laminate_pane — в эталоне «   /###/###» (пустой верхний ряд), в порте «###/###»; в 1.21.1 пустой ряд обрезается, крафт тот же.
- 24 условия: `else`-ветки LBSM/528 оформлены как `not(lbsm)`/`528` — эквивалентно.
- Отсутствуют (нет предметов/вариантов в порте или нужен особый тип рецепта): глубинные блоки (исключены), crate_desh/crate_tungsten/safe (в порте как upgrade-рецепты), conveyor_wand_express (тип `conveyor_express`), oc_cable_paintable (OpenComputers), obj_tester (тестовый блок), holotape restored, vinyl_tile #1, book_of_, ink (poppy+…), platemetal reset, block_slag/fallout/копирование цвета, циклы ore_bedrock/ingot_steel_dusted (вне верстака), копирование медных блоков (ванильная медь).
- 316 «лишних» рецептов порта: в основном лестницы/плиты/краны/бетон/отходы, которые в эталоне создаются циклами recipeGen/хендлерами (не разбираются статически), LBSM-рецепты, deepslate/raw-плавки (согласованы), интеграции. Явно придуманные удалены (см. выше); остальные требуют ручного разбора по одному — не трогались.

## 2. Подсказки и свойства — см. `parity-e3-tooltips.md`

Кратко: 1531 предмет в таблице свойств (`legacy/reinhardtshbm/parity-e3-items.tsv`), 33 значения по умолчанию исправлены при загрузке; 23 класса подсказок приведены к эталону, 478 lang-ключей перенесены, цвет строк без кода — серый.
Поправки после сборки: таблица подставляла 64/0 там, где ModItems не задаёт размер стека/прочность, а класс задаёт их в конструкторе (ItemCanteen, ItemGrenadeUniversal, ItemBlades…) — принудительная установка стека 64 и проверка таких строк отключены. Крафтовый остаток (setContainerItem) добавлен 6 напиткам (bottle_cherry/quantum/sparkle/rad → bottle_empty, bottle2_korl/fritz → bottle2_empty). Пипетки: в эталоне setMaxDamage(1) (только запрет стака), в порте стек 1 без прочности — оставлено, записано.

## 3. Второй проход по багам — см. `parity-e3-bugs.md`

17 исправлений: 2 деления/переполнения (глифиды), 2 ConcurrentModification (обход сущностей), 5 утечек статических карт, 6 загрузок чанков из тикеров/сетей, 2 по данным игрока (сброс радиации и пр. при возврате из Энда как в эталоне — HbmLivingProps не копируются; клавиша ранца), 1 команда (`/rhbm … get` теперь уровень 2), 10 serverbound-пакетов (проверка загруженности чанка / открытого меню радара). Бесконечных обходов сетей не найдено.
Лог выделенного сервера (checkBuildingAudit/checkItemAudit): WARN от reinhardtshbm — только ожидаемые «Legacy block … has no exact modern state» аудита метаданных; ERROR — нет (после исправлений).

## Правки build.gradle
Нет. `updateRecipeBaseline` выполнен (recipe-baseline обновлён).

## Проверки
`work/verification-parity-e3.log`: assemble, validateJarIntegrity, validateLangFiles, validateRecipeJsonSchemas, validateRecipeSemantics, validateRegistrySources, validateRecipeBaseline, validateLegacyGlobalGameplayParity, validateTurretRecipeChain, validateRocketMissileRecipes, checkBuildingAudit (errors=0, checked_parity_e3_cases=3777), checkItemAudit (errors=0, warnings=14) — BUILD SUCCESSFUL; 0 ошибок разбора рецептов. Изменения в K3C-диагностике: дуговая печь проверяется на sand_mix QUARTZ.

## Ручная проверка
1. Верстак: 8 песка + урановый порошок → 8 «Uranium Sand» (sand_mix); переплавка → урановое стекло. Ящик для огнетушителя (песок бора) принимает тот же sand_mix.
2. Защитная ткань (hazmat_cloth_grey) крафтится и с ingot_biorubber.
3. 9 медных тонких проводов → медный слиток; 3 опилок → 3 бумаги; ведро из 3 стальных слитков.
4. Апгрейд стака: ур.1 + 2 конденсатора + 2 гидропоршня → ур.2; ур.2 + чипы + электропоршни → ур.3.
5. Выпитая бутылка Cherry в крафте оставляет пустую бутылку.
6. Подсказки: см. чек-лист в parity-e3-tooltips.md (Dangerous Drop, pile_rod, pellet_rtg, conveyor_wand Shift, particle_digamma 333.3mDRX/s).
7. Возврат из Энда сбрасывает радиацию/дигамму (как в 1.7.10); смерть — тоже.
