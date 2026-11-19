# Партия T4 — JEI по NEI оригинала (2026-11-17)

Эталон: `work/original` (HbmMods 936c60c), `handler/nei/*`, `main/NEIRegistry.java`. Продолжение work/parity-s3.md.

Общие основы (новые файлы, `integration/jei`):
- `NeiUniversalCategory<T>` — NEIUniversalHandler: gui_nei.png (5,11,166×65), getInputCoords/getOutputCoords, рамки 18×18 (5,87), рамка операции (59,87,18×36) в 74,14, машина в 75,31; рецепт порта переводится в RecipeSet из `getRecipes()` обработчика (порядок входов/выходов, фиксированные NEI-количества, ×10, PU), плюс `drawExtras` обработчика.
- `NeiGenericCategory<T>` — NEIGenericRecipeHandler: getInputSlotPositions/getOutputSlotPositions, смещения AssemblyMachineRecipeHandler (>9 / >12 входов), предметы раньше жидкостей, шаблон-чертёж 75,10 с высокой рамкой (77,87,18×50) и машина 75,38, `printNEIExtras` (ticks + HE/t; NoPower — только ticks; PUREX — одна зелёная строка), скрытие рецептов из пулов `secret.` (NEI_HIDE_SECRETS = true).
- `NeiTemplateCategory<T>` — TemplateRecipeHandler: фон getGuiTexture (5,11,166×65), координаты PositionedStack без сдвига, `drawProgressBar` → анимированные drawable JEI (0 вправо, 1 вниз, 3 вверх, +4 убывание).
- `UniversalJeiRecipe.Slot`: добавлены `pressure` (строки ItemFluidIcon «N PU» и мигающее «Pressurized, use compressor!») и `itemLines` (шанс каждой альтернативы ChanceOutputMulti, красный «x.y%»).

Обозначения: **T4** — приведено в T4; **=** — было совпадающим (S3), не менялось; **—** — нет в порте.

| # | NEI-обработчик (заголовок) | Категория | T4 |
|---|---|---|---|
| 4 | SolderingStationHandler (имя блока) | soldering_station | T4: Universal, входы toppings→pcb→solder→жидкость, «%,d ticks»/«%,d HE/t» у 160 (y 43/55) |
| 5 | BlastFurnaceHandler (имя блока) | blast_furnace | T4: Generic/NoPower, gui_nei вместо guidifurnace |
| 6–7 | Anvil / Smithing «Anvil» | anvil_construction / anvil_smithing | = |
| 8 | PressRecipeHandler «Press» | press | T4: gui_nei_press.png, вход 48,42 (×1), штамп 48,6, результат 111,24, полоса 47,24 вниз 20 т |
| 9 | ShredderRecipeHandler «Shredder» | shredder | T4: gui_nei_shredder.png (оригинал), 39,24 / 129,24, лезвия сталь/титан/деш 84,6 и 84,42, энергия 3,6 (убывает вверх 480 т), прогресс 80,23 |
| 10–12 | Crucible Smelting/Alloying/Casting | crucible_smelting / crucible / crucible_foundry | = (заголовок «Crucible Casting» приведён) |
| 13 | AssemblyMachineRecipeHandler | assembly_machine | T4: Generic со смещениями ассемблера, чертёж-шаблон, ChanceOutputMulti — один слот с шансами |
| 14 | PrecAssRecipeHandler | precision_assembler | T4: Generic; weighted — один слот, independent — шанс weight/100 |
| 15 | ChemicalPlantRecipeHandler | chemical_plant | T4: Generic, машины chemical_plant + chemical_factory |
| 16 | RefineryRecipeHandler «Refinery» | refinery | T4: gui_nei_refinery.png (оригинал), 1000 mB → фракции ×10 в 111,6/129,15/111,24/129,33, твёрдое 111,42, полосы 3,6 и 78,24 |
| 17 | CentrifugeRecipeHandler «Centrifuge» | centrifuge | T4: Universal |
| 18 | GasCentrifugeRecipeHandler «Gas Centrifuge» | gas_centrifuge | T4: gui_nei_centrifuge_gas.png, 47,24, 4 выхода (дополнены `nothing`), батареи getBatteries 3,42, энергия, прогресс 80/150 т, значок high speed 25,24, «N G. Cents» (4/2/1/2) |
| 19 | CyclotronRecipeHandler «Cyclotron» | cyclotron | T4: gui_nei_cyclotron.png, 21,24 / 75,24 / 129,24, прогресс 44,24; по записи на каждый предмет мишени |
| 20 | VacuumRecipeHandler «Vacuum Refinery» | vacuum_distill | T4: Universal, 1000 mB 2 PU → 4×(fill×10); неоригинальная текстура удалена |
| 21 | CrackingHandler «Cracking» | cracking | T4: Universal, {100 mB, 200 mB пара} → {вых.1, вых.2, 2 mB отраб. пара}; неоригинальная текстура удалена |
| 22 | RadiolysisRecipeHandler «Radiolysis» | radiolysis | T4: полоса перенесена в 52,19 (была 47,8) |
| 23 | ReformingHandler «Reforming» | reforming | T4: Universal, 1000 mB → 3×(fill×10); неоригинальная текстура удалена, катализатор убран |
| 24 | HydrotreatingHandler «Hydrotreating» | hydrotreating | T4: Universal, {1000 mB, водород ×10 1 PU} → 2×(×10); катализатор убран |
| 25 | PUREXRecipeHandler | purex | T4: Generic, строка «N ticks   N HE/t» 0x306030 y 57 |
| 26 | OreSlopperHandler | ore_slopper | = (заголовок — имя блока) |
| 27 | CrystallizerRecipeHandler «Acidizer» | crystallizer | T4: Universal {кислота, предмет}, «Effectiveness: +N% per level» 8,52; рецепты scrap_plastic_* скрыты как в NEI |
| 28 | BookRecipeHandler «Black Book» | book_of_boxcars | = |
| 29–30 | Fusion / PlasmaForge | fusion / plasma_forge | T4: подписи printNEIExtras — getShortNumber, «HE/t»/«Ky/t»/«TU/t» без пробела; plasma_forge скрывает secret-пулы |
| 31 | SILEXRecipeHandler «SILEX» | silex | T4: фон по умолчанию, вход 12,24, выходы 68/116 (sep = >4 ? 3 : 2), шансы relx+18, целочисленное produced/consumed у 52,43, длина волны у 33,8 |
| 32 | FuelPoolHandler | fuel_pool | T4: заголовок «Spent Fuel Pool Drum» |
| 33–36 | RBMK Disassembly / Waste Decay / Satellite / Grenade | — | = |
| 37 | ZirnoxRecipeHandler | — | — (reactor_zirnox нет в порте) |
| 38 | PWRRecipeHandler | pwr | T4: заголовок — имя pwr_controller |
| 39 | WatzRecipeHandler | watz | T4: Universal вместо пустого холста |
| 40 | RTGRecipeHandler | rtg | = |
| 41 | LiquefactionHandler | liquefaction | T4: Universal |
| 42 | SolidificationHandler | solidification | T4: Universal |
| 43 | CokingHandler | coker | T4: Universal {жидкость} → {твёрдое, побочная жидкость} |
| 44 | PyroHandler | pyro_oven | T4: Universal {жидкость, предмет} → {предмет, жидкость} |
| 45 | FractioningHandler | fraction_tower | T4: Universal, вход 100 mB; неоригинальная текстура удалена |
| 46 | BoilingHandler | boiling | T4: заголовок — имя machine_boiler |
| 47 | CombinationHandler | combination_oven | T4: Universal |
| 48 | SawmillHandler | sawmill | = |
| 49 | MixerHandler | mixer | T4: Universal {жидк.1, жидк.2, твёрдое} → жидкость |
| 50 | OutgasserHandler | rbmk_outgasser | = (fusionOnly-рецептов в данных нет) |
| 51 | FusionBreederHandler | fusion_breeder_fluid | T4: Universal; пустая fusion_breeder_item удалена |
| 52–53 | ElectrolyserFluid/Metal (имя блока) | electrolyzer_fluid / electrolyzer_metal | T4: Universal (металл: {предмет, 100 mB азотной} → жидкие обрезки + побочные) |
| 55 | ArcWelderHandler | arc_welder | T4: Universal, getShortNumber ticks / HE/t у 164 (y 45/57) |
| 56 | ExposureChamberHandler | exposure_chamber | T4: Universal {вход ×8, частица} → выход ×8 |
| 57–58 | ArcFurnaceSolid/Fluid | arc_furnace_solid / liquid | = |
| 59 | RotaryFurnaceHandler | rotary_furnace | T4: Universal, «%,d ticks», «Steam: %,d mB/t» у 160; слот топлива и пар-на-операцию порта убраны |
| 60 | AmmoPressHandler | ammo_press | T4: Universal (непустые ячейки 3×3) |
| 61 | CompressorHandler | compressor | T4: Universal, вход/выход с PU; рецепты без смены жидкости скрыты (как в NEI) |
| 62 | ParticleAcceleratorHandler | particle_accelerator | T4: Universal, «Momentum: %,d» 8,52 |
| 64 | RockMillRecipeHandler | rock_mill | T4: Generic, взвешенный выход одним слотом |
| 65 | SuperComputerRecipeHandler | supercomputer | T4: Generic, взвешенный выход одним слотом |
| 66 | SpaceAssemblerHandler | space_assembler | T4: Generic/NoPower, заголовок и машина — спутник Science Assembler |
| 67 | FluidRecipeHandler | legacy_fluid_container | = |
| 68 | CustomMachineHandler | custom_machine | T4: gui_nei_custom.png, «Paper Press», жидкость 12,6, предмет 12,24, выход 102,24, машина 75,42, строка загрязнения у 160 y 75 |
| 1–3, 54, 63 | Tooling, Construction, Annihilator, Ashpit, Deuterium | (S3) | = |

Удалены (нет обработчика в NEIRegistry, не интеграция): breeder_reactor, blast_furnace_fuel, armor_table, silex_ir/visible/uv/gamma/digamma, fusion_breeder_item. Оставлена интеграция tacz_ammo_assembler (раскладка ассемблера).

Текстуры: скопированы побайтно nei/gui_nei_press, gui_nei_cyclotron, gui_nei_centrifuge_gas, gui_nei_custom, gui_nei_shredder, gui_nei_refinery. Удалены jei/gui_nei (отличался), jei/gui_nei_refinery и jei/gui_nei_shredder (отличались, заменены копиями в nei/), jei/gui_nei_cracking, gui_nei_fraction_tower, gui_nei_reforming, gui_nei_vacuum_distill (не из оригинала).

Переводы: удалены 34 неиспользуемых ключа jei.* только порта (en_us, zh_cn; work/integration/lang/*.json = null).

Техадаптация: смена альтернатив — цикл JEI (NEI: cycleticks/20, /24, /48); JEI требует иконку категории — машина; строки, которые NEI клал в лор, выводятся подсказкой слота; подписи y 63/75 у Paper Press выходят за 65-пиксельный фон, как и в NEI.

## Статическая сверка (work/tmp-t4/audit_t4.py)

| проверка | результат |
|---|---|
| текстуры JEI | 15 используемых, все побайтно равны gui/nei оригинала; лишних нет (gui_nei_boiler, gui_nei_fusion — копии оригинала, категориями не используются) |
| NEIUniversalHandler#getInputCoords / getOutputCoords | 45 / 36 позиций совпадают |
| NEIGenericRecipeHandler#getInputSlotPositions / getOutputSlotPositions | 21 позиция / равно getOutputCoords |
| рамки 5,87 / 59,87 / 77,87, шаблон 75,10 и машина 75,38 | есть |
| Press, Shredder, Refinery, Cyclotron, Gas Centrifuge, Radiolysis | слоты 3/3, 4/4, 6/6, 3/3, 6/6, 3/3; полосы 1/1, 2/2, 2/2, 1/1, 2/2, 1/1 |
| категории на основах | Universal 24, Generic 8, Template 7 |
| problems | 0 |
