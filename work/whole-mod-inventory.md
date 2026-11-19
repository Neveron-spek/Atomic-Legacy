# Инвентаризация порта HBM's NTM 1.7.10 -> 1.21.1 NeoForge (2026-11-13)

Только чтение кода; сборка и игра не запускались. Оружие sedna и моды оружия не оценивались.

## Метод и главный вывод

- Оригинал: `blocks/ModBlocks.java` (988 объявлений, 426 блоков с TileEntity), `items/ModItems.java` + `ModItemsArmor.java` (1648 объявлений), `tileentity/TileMappings.java` (377 TE), `entity/EntityMappings.java` (161 сущность), `inventory/recipes/*` (57 обработчиков), `inventory/gui` (223 GUI).
- Порт: `registry/HbmBlocks.java` (1004 ID), `HbmBlockEntities.java` (блок -> BE), `HbmItems.java`, `LegacyHbmContent.java` (пустышки из `legacy/reinhardtshbm/blocks.txt`), `HbmEntityTypes.java` (95 ID), `legacy/reinhardtshbm/item_audit.tsv` (самоотчёт основы: 1722 formal, 74 retired), `data/reinhardtshbm/recipe` (5420 JSON).
- Сопоставление автоматическое по registry-ID; для блоков с TE сравнивалось наличие BE и число строк TE оригинала / BE порта. В счёт системы входят: блоки с TE или из функциональных пакетов, предметы с собственным классом (без `Item`, `ItemCustomLore`, `ItemEnumMulti`), сущности. Строительные блоки (206 семейств, сверены) и оружие в таблицу не входят.
- Статусы: (а) отсутствует / пустышка / только blockstate; (б) есть реализация, с 1.7.10 не сверена; (в) сверено по `work/STATE.md`.
- **Вывод: основа покрывает ~95 % объектов оригинала настоящими классами (пустышек-предметов нет: `items.txt` пуст; блок-пустышек по списку 12 — это двери, фактически реализованные через `door/HbmDoorDecl.java`). Почти всё находится в статусе (б): реализация есть, но сверена только малая часть. Остаток работы — это в основном сверка, а не перенос с нуля.**
- Выборочная сверка констант (совпали): пресс (maxPress 200), центрифуга (100 000 HE, 200 тиков), силовая броня AJR (2 500 000 / 10 000 / 2 000 / 25), импакты ракет (радиусы 15/30/50, нюк 100, doomsday 200 в `entity/LegacyLauncherImpact.java`), радиация чанков = `ChunkRadiationHandlerSimple` (умолчание оригинала). Это 5 точек, а не доказательство точности систем.
- Маркеров «simplified / TODO / stub» в машинах, реакторах, броне практически нет (56 совпадений на весь порт, почти все — оружие и worldgen API). Поведение GUI поштучно не сверялось: 153 экрана порта против 223 GUI оригинала (многие объединены: `RbmkComponentScreen`, `LegacyTurretScreen`, `CraneMachineScreen`, `LegacyNukeScreen`).

## Сводная таблица

| # | Система | Объектов в оригинале | (а) нет / пусто | (б) есть, не сверено | (в) сверено | Объём доработки |
|---|---|---|---|---|---|---|
| 1 | Машины обработки | 186 (99 бл., 85 пр., 2 сущ.) | 5 | 180 | 1 | большой (сверка) |
| 2 | Энергия | 87 (68 бл., 19 пр.) | 15 (13 — @Deprecated) | 72 | 0 | средний |
| 3 | Жидкости, трубы, нефть | 80 (56 бл., 24 пр.) | 2 | 78 | 0 | средний |
| 4 | Реакторы | 226 (103 бл., 121 пр., 2 сущ.) | 3 | 223 | 0 | большой |
| 5 | Ракеты, пусковые, спутники, радары | 170 (23 бл., 114 пр., 33 сущ.) | 4 | 166 | 0 | большой |
| 6 | Турели и артиллерия | 19 (13 бл., 4 пр., 2 сущ.) | 0 | 19 | 0 | средний |
| 7 | Бомбы, мины, заряды | 99 (52 бл., 14 пр., 33 сущ.) | ~5 сущ. | ~76 | ~18 | средний |
| 8 | Броня, моды брони, инструменты | 319 пр. | 1 (+2 GUI) | 318 | 0 | большой (сверка) |
| 9 | Мобы и боссы | 45 (40 сущ., 4 пр., 1 бл.) | 2 | 43 | 0 | средний |
| 10 | Еда, медицина, гранаты | 110 (106 пр., 4 сущ.) | 2 | 108 | 0 | малый |
| 11 | Генерация мира, руды, структуры | 140 (94 бл., 46 пр.) + 83 NBT | 4 (+6 исключены) | 136 | 0 | средний |
| 12 | Опасности | 75 (35 бл., 32 пр., 8 сущ.) | 5 сущ.-FX (не проверено) | 70 | 0 | средний |
| 13 | Логистика | 79 (57 бл., 9 пр., 13 сущ.) | 24 | 55 | 0 | средний |
| 14 | Декоративные блок-сущности, разное | 103 (76 бл., 27 пр.) | 2 (тестовые) | 99 | 2 | малый |
| 15 | Системные | 31 пр. + 13 команд, 62 достижения, 72 NEI, 19 конфигов | 9 команд, 2 GUI | остальное | 0 | средний |

Счёт получен скриптом по правилам имён/пакетов; погрешность отнесения к системе ±5 %.

## 1. Машины обработки

- Основа: `blocks/machine/*` оригинала против `tileentity/*BlockEntity.java` порта. У всех действующих машин есть BE сопоставимого размера (пресс 261 -> 904 строк, сборщик 501 -> 1449, химзавод 347 -> 1134, кристаллизатор 433 -> 764, дуговая печь 729 -> 766).
- (а): `machine_difurnace_off/on`, `machine_difurnace_extension` (в оригинале зарегистрированы, но устаревшие), `network_separator`, предмет `drive` (`ItemDrive`).
- (в): `machine_weapon_table`.
- Рецепты: типы есть для всех обработчиков оригинала; число JSON близко или больше (ChemicalPlant ~82/81, PUREX ~59/57, ArcWelder ~44/42, AmmoPress ~93/91, Cyclotron ~46/42; меньше оригинала: PyroOven ~24/13, RockMill ~14/9, Crucible ~20/14 — грубый счёт grep, требуется точная сверка). Нет типов: Radiolysis (3), Annihilator (в коде `machine/AnnihilatorRecipes.java`), Lemegeton (38), Magic (8).
- Подмена/добавление чужого: `machine_tacz_ammo_assembler` (`blocks/TaczAmmoAssemblerBlock.java`) — блока нет в оригинале; рецепты с условием `neoforge:mod_loaded` (18 шт.: Create Additions, IE, AE2); условия `enable_expensive_mode` / `less_bullshit_mode` в 374 рецептах — соответствие режимам оригинала не проверено.
- Объём: большой — ~100 машин и ~2500 машинных рецептов требуют сверки значений, структурных пропусков мало.

## 2. Энергия

- Есть BE: `machine_battery_socket`, `machine_battery_redd`, пилоны/подстанция (общий BE 846 строк), дизель, ДВС, турбины, Stirling, RTG, солнечный бойлер, конденсаторы, градирни.
- (а): `machine_converter_he_rf`, `machine_converter_rf_he` (действующие в оригинале, в порте нет; вместо них FE-мост в `registry/HbmCapabilities.java` — замена механики, см. комментарий в `integration/create/HbmCreateAdditionCompat.java`); 13 устаревших: `machine_battery*` (5), `capacitor_gold/niobium/tantalium/schrabidate`, `machine_rtg_furnace_off/on`, `machine_difurnace_rtg_off/on`.
- Подозрение на упрощение: `machine_industrial_boiler` (TE 328 строк -> BE 43), кабели `red_cable*` без BE (сеть вынесена в `power/PowerNetworkManager.java`) — не проверено.
- Объём: средний — сеть HE переписана целиком (3 файла `power/`), нужна сверка потерь/приоритетов и ёмкостей.

## 3. Жидкости, трубы, нефтепереработка

- Есть: все 15 блоков `machine/oil` (скважина, качалка, фрекинг, НПЗ, вакуумная, крекинг, риформинг, гидроочистка, коксование, пиролиз), баки, насосы, трубы (общий BE 403 строки).
- (а): `flow_control_pump`, `pipe_anchor_exhaust`.
- Подозрение на упрощение: бочки `barrel_plastic/corroded/steel/tcalloy/antimatter` (TE 372 -> BE 91), `machine_deuterium_tower` (65 -> 11).
- Жидкостные блоки (`mud_fluid`, `acid_fluid`, `corium_fluid` и др.) заменены системой `fluid/HbmFluidDefinitions.java` — поведение не проверено.
- Чужое: `integration/createdieselgenerators/HbmCreateDieselGeneratorsOilCompat.java`.
- Объём: средний.

## 4. Реакторы

- Есть: RBMK — все 31 блок (единый BE-модуль ~4900 строк), PWR, ZIRNOX (`machine_zirnox`), исследовательский (`machine_reactor_small`), Chicago Pile (графитовые блоки), Watz, термояд (9 блоков, 2576 строк), ICF, DFC (5), бридер, ускоритель `pa_*` (6).
- (а): `pile_device` (блок с `TileEntityPileBaseMK2/PileControl/PileVent`), предмет `pile_rod` (`ItemPileRodMK2`), `rbmk_fuel_hep239`.
- Подозрение на упрощение: `dfc_receiver/stabilizer/injector` (380 -> 212–242 строк).
- Предметы: 29 стержней RBMK, 28 таблеток, 16 отработанных — регистрируются динамически (`HbmItems.java:454`), параметры нейтронной физики не сверялись.
- Объём: большой — самая объёмная физика, зависит от систем 2, 3, 12.

## 5. Ракеты, пусковые, спутники, радары

- Есть: `launch_pad`, `launch_pad_large`, `compact_launcher`, `launch_table`, `launchpad_soyuz`, `launchpad_lambda`, `machine_radar(_large)`, `radar_screen`, `sat_dock`, `machine_satlinker`, сборка ракет; 28 готовых ракет и 55 деталей (`items/MissilePartItem.java`).
- Замена структуры: 29 сущностей ракет оригинала сведены к одной `launcher_missile` (`entity/LegacyLauncherMissileEntity.java`, 254 строки) с таблицей импактов; траектории ярусов Tier0–4 и ПРО не сверены.
- (а): `soyuz_launcher`, `struct_soyuz_core`, предметы `satellite` (`ItemSatellite`), `orbital_assembly`.
- Нет команды `/ntmsatellites`.
- Объём: большой.

## 6. Турели и артиллерия

- Все 13 блоков есть с BE (общий модуль 2252 строки + отдельные Chekhov 866, Jeremy 859), снаряды `entity_artillery_shell`, `entity_himars`, `ammo_arty`, `ammo_himars`.
- STATE отмечает исправление разброса пуль турелей (headingForceMult) — побочное, не сверка.
- Объём: средний — проверить дальности, энергопотребление, типы снарядов, списки целей.

## 7. Бомбы, мины, заряды

- (в): 11 `nuke_*`, сингулярности, облака/вихри/торекс (по STATE).
- (б): `bomb_multi`, `mine_ap/he/shrap/fat/naval`, `charge_dynamite/miner/c4/semtex`, `det_*`, бочки, детонаторы, `crashed_bomb`, `fireworks`, бомбардировщик.
- (а) сущности без найденного аналога: `entity_c130`, `entity_parachute_crate`, `entity_beam_bomb` (B92), `entity_tom_bust`, `entity_moonstone_blast` — возможно, объединены с менеджерами взрывов; не проверено.
- Объём: средний.

## 8. Броня, моды брони, инструменты

- Есть: все комплекты FSB (`items/ArmorFSBItem`, `PoweredArmorFSBItem`, `FueledArmorFSBItem`), моды брони (19 классов), реактивные ранцы, инструменты со способностями (`items/HbmToolProfile.java`, `HbmToolBehavior.java`).
- (а): `ballistic_gauntlet` (выведен основой); нет GUI пресетов способностей (`GUIScreenToolAbility`) и `ToolPreset`-экрана — не найдено по имени.
- Не найден отдельный аналог `DamageResistanceHandler` (0 файлов, 4 упоминания) — сопротивления брони по классам урона не проверены.
- Выведены основой: `multitool_*`, `weapon_bat*`, `weapon_saw`, `weapon_pipe_rusty`, `weapon_golf_club` — в ModItems оригинала на этом коммите их нет (не расхождение).
- Объём: большой по числу (319 предметов), но самодостаточный.

## 9. Мобы и боссы

- Есть: 9 глифидов, 5 криперов, крабы, FBI + дрон, Mask Man, UFO, охотничий вертолёт, Balls-O-Tron, Quackos, RAD-зверь, нежить, личинка.
- (а): `entity_digamma_quasar`, `entity_ntm_siege_laser`.
- Замена: экипировка нежити и ящики оружия переведены на TACZ (`integration/tacz/TaczWeaponCrateCompat.java`; STATE 2026-11-11).
- Объём: средний — здоровье, урон, ИИ, условия спавна, дроп.

## 10. Еда, медицина, гранаты

- Есть: все классы (`LegacyEnergyDrinkItem`, `LegacySyringeItem`, `LegacyPillItem`, `LegacyLemonItem`, `UniversalGrenadeItem` + `grenade_shell/filling/fuze/extra`).
- (а): `quesadilla`, `entity_waste_pearl`.
- Подозрение: `glowing_stew`, `balefire_scrambled`, `balefire_and_ham` заведены как обычная еда 6/0.6 (`LegacyHbmContent.java`) — эффекты оригинала не проверены.
- Объём: малый.

## 11. Генерация мира, структуры, руды

- Есть: 44 класса `worldgen/`, 192 JSON worldgen, 45 структур, бедрок-руды, нефть, метеориты, улей глифидов.
- (а): `ore_cinnebar`, `plant_flower`, `plant_tall` (выведены основой), `nugget_raw`; 6 глубинных блоков исключены пользователем.
- Скрыты: `ore_bedrock`, `ore_bedrock_coltan`.
- Не проверено: частоты, высоты, биомы, лут-пулы (`ItemPool` — 1 упоминание в порте против `itempool/` оригинала).
- Объём: средний.

## 12. Опасности

- Есть: `radiation/` (10 файлов), `pollution/` (7), 10 эффектов, газовые блоки (11 оригинала -> 13 порта), асбест, угольная пыль, дигамма.
- Упрощение, видимое по комментарию: `radiation/RadiationShielding.java` — «Lightweight, source-local shielding»; PRISM/3D-обработчики оригинала (опциональные) не перенесены.
- FX-сущности (`entity_chlorine_fx`, `entity_pink_cloud_fx`, `entity_cloud_fx`, `entity_agent_orange`, `entity_nuclear_fog`) как сущности отсутствуют — вероятно частицы, не проверено.
- Объём: средний — пороги и формулы доз.

## 13. Логистика

- Есть: краны (общий BE 1144), конвейеры, дроны (`drone/`), пневмотрубы (`pneumatic/`), RTTY-блоки, автокрафтер.
- (а): 13 рельсов (`rail_wood`, `rail_highspeed`, `rail_booster`, `rail_narrow_curve`, `rail_large_*` — только blockstate, скрыты), 5 вагонеток `entity_ntm_cart_*`, поезд `entity_ntm_cargo_tram(_trailer)`, предметы `cart`, `train`, блоки `radio_torch_relay`, `pipe_anchor_pneumatic`.
- Подозрение: `drone_crate_provider` (199 -> 84).
- Объём: средний; поезда — отдельная крупная подсистема.

## 14. Декоративные блок-сущности и разное

- (в): `pedestal`, `lantern_behemoth`. Двери (14) реализованы через `door/HbmDoorDecl.java`.
- (б): ящики, сейф, `mass_storage` (447 -> 226), бобблхеды, плюшевые, прожекторы, сирена, телепорт, силовое поле.
- (а): только тестовые `event_tester`, `obj_tester`. Не найдены: картотека (`TileEntityFileCabinet`).
- Объём: малый.

## 15. Системные

- Команды: в порте `/rhbm` и `/ntmlocate`; нет `/ntmcustomize`, `/ntmsatellites`, перезагрузок конфигов/рецептов, отладочных (9 из 13).
- Достижения: 63 файла против 62 — условия не сверены.
- NEI -> JEI: 62 категории против 72 обработчиков.
- Конфиги: 2 файла (2136 строк) против 19 — состав ключей не сверен.
- Книги: `book_guide_book` выведен; нет GUI Lemegeton, калькулятора, Slice Printer. Bobmazon есть (`handler/LegacyBobmazonOffers.java`). QMAW есть.
- Объём: средний.

## Рекомендуемый порядок следующих 5 партий

1. **Расходники и взрывчатка вне нюков** (системы 10, 7) — самодостаточно. ID: `syringe_*`, `radaway*`, `radx`, `pill_*`, `can_*`, `bottle_*`, `glowing_stew`, `balefire_scrambled`, `balefire_and_ham`, `quesadilla`, `grenade_universal` + `grenade_shell/filling/fuze/extra`, `mine_ap/he/shrap/fat/naval`, `charge_dynamite/miner/c4/semtex`, `det_*`, `bomb_multi`, детонаторы. Зависимости: эффекты (`HbmMobEffects`), `ExplosionVNT`.
2. **Броня, моды брони, инструменты** (система 8). ID: все `*_helmet/_plate/_legs/_boots` FSB, `ajr_*`, `t51_*`, `rpa_*`, `bj_*`, `hev_*`, `dnt_*`, `jetpack_*`, моды `ItemMod*`, инструменты `*_pickaxe/_sword/_axe` со способностями, `ballistic_gauntlet`. Зависимости: таблица сопротивлений `DamageResistanceHandler`, стол брони, GUI способностей, система 12.
3. **Базовая цепочка машин и энергия** (системы 1, 2). ID: `machine_press`, `machine_epress`, `furnace_iron/steel/combination`, `machine_blast_furnace`, `machine_shredder`, `machine_centrifuge`, `machine_crystallizer`, `machine_assembly_machine`, `machine_chemical_plant`, `machine_arc_furnace`, `machine_battery_socket`, `machine_battery_redd`, `red_cable*`, `red_pylon*`, `machine_diesel`, `machine_combustion_engine`, `machine_wood_burner`, `machine_converter_he_rf/rf_he`. Зависимости: рецепты пресса/сборщика/химзавода, шаблоны, апгрейды, JEI.
4. **Жидкости, нефть, логистика** (системы 3, 13). ID: `machine_fluidtank`, `barrel_*`, `fluid_duct_*`, `fluid_pump`, `flow_control_pump`, `machine_well`, `machine_pumpjack`, `machine_refinery`, `machine_vacuum_distill`, `machine_catalytic_cracker`, `machine_coker`, `crane_*`, `conveyor*`, `pneumatic_*`, `drone_*`. Зависимости: партия 3, жидкостная система.
5. **Турели, ракеты, радары, мобы** (системы 6, 5, 9). ID: `turret_*` (13), `launch_pad*`, `compact_launcher`, `launch_table`, `missile_*` (28), `mp_*`, `machine_radar*`, `sat_*`, `soyuz_launcher`, `struct_soyuz_core`, глифиды, `entity_digamma_quasar`. Зависимости: партии 3–4, взрывы, сверенные пули.

После них: реакторы (система 4, самая зависимая), генерация мира, опасности, системные, поезда и рельсы.
