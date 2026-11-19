# Партия S3 — NEI → JEI, книги и GUI (2026-11-17)

Эталон: `work/original` (HbmMods 936c60c). NEIRegistry регистрирует 67 обработчиков + CustomMachineHandler на каждый конфиг; остальные файлы `handler/nei` — базовые классы (NEIUniversalHandler, NEIGenericRecipeHandler), AnvilOverlayHandler и незарегистрированный AlloyFurnaceRecipeHandler.

Обозначения: **=** совпадает (данные, заголовок, раскладка/текстура NEI); **T** заголовок приведён к NEI в S3; **L** раскладка/текстура отличается от NEI (не переделано, открыто); **N** перенесено в S3; **—** нет машины/данных в порте.

| # | NEI-обработчик (заголовок NEI) | JEI-категория порта | Статус |
|---|---|---|---|
| 1 | ToolingHandler «Tooling» | `tooling` (UniversalJeiCategory) | N — все 4 конверсии BlockToolConversion; заменяет icf_tooling (было 2) |
| 2 | ConstructionHandler «Construction» | `construction` | N — Watz, compact launcher, launch table, Soyuz, ICF, fusion torus, метки «NxN64 + M»; заменяет watz_construction/icf_construction |
| 3 | AnnihilatorHandler «Annihilator» | `annihilator` | N — 16 вех, только при enable528 (как registerDefaults), красная строка «%,d» |
| 4 | SolderingStationHandler (имя блока) | soldering_station | L (GUI машины вместо NEIUniversal) |
| 5 | BlastFurnaceHandler (имя блока) | blast_furnace | L (guidifurnace вместо gui_nei) |
| 6 | AnvilRecipeHandler «Anvil» | anvil_construction | T |
| 7 | SmithingRecipeHandler «Anvil» | anvil_smithing | T |
| 8 | PressRecipeHandler «Press» | press | T, L (gui_epress; gui_nei_press не перенесён) |
| 9 | ShredderRecipeHandler «Shredder» | shredder | L (jei/gui_nei_shredder.png отличается побайтно) |
| 10 | CrucibleSmeltingHandler «Crucible Smelting» | `crucible_smelting` | N — gui_nei_crucible_smelting, вход 48,24, тигель 75,42, выходы по 3 |
| 11 | CrucibleAlloyingHandler «Crucible Alloying» | crucible | T (текстура =) |
| 12 | CrucibleCastingHandler «Crucible Casting» | crucible_foundry | = |
| 13 | AssemblyMachineRecipeHandler | assembly_machine | L |
| 14 | PrecAssRecipeHandler | precision_assembler | L |
| 15 | ChemicalPlantRecipeHandler | chemical_plant | L |
| 16 | RefineryRecipeHandler «Refinery» | refinery | T, L (jei/gui_nei_refinery.png отличается) |
| 17 | CentrifugeRecipeHandler «Centrifuge» | centrifuge | L |
| 18 | GasCentrifugeRecipeHandler «Gas Centrifuge» | gas_centrifuge | L (gui_nei_centrifuge_gas не перенесён) |
| 19 | CyclotronRecipeHandler «Cyclotron» | cyclotron | L (gui_nei_cyclotron не перенесён) |
| 20 | VacuumRecipeHandler «Vacuum Refinery» | vacuum_distill | L (текстура порта не из оригинала) |
| 21 | CrackingHandler «Cracking» | cracking | T, L (текстура порта не из оригинала) |
| 22 | RadiolysisRecipeHandler «Radiolysis» | radiolysis | T |
| 23 | ReformingHandler «Reforming» | reforming | T, L |
| 24 | HydrotreatingHandler «Hydrotreating» | hydrotreating | T, L |
| 25 | PUREXRecipeHandler | purex | L |
| 26 | OreSlopperHandler | ore_slopper | = |
| 27 | CrystallizerRecipeHandler «Acidizer» | crystallizer | T, L |
| 28 | BookRecipeHandler «Black Book» | `book_of_boxcars` | N — gui_book.png, 7 рецептов MagicRecipes |
| 29 | FusionRecipeHandler | fusion | = |
| 30 | PlasmaForgeRecipeHandler | plasma_forge | = |
| 31 | SILEXRecipeHandler «SILEX» | silex | T; +5 категорий по длинам волн — только в порте |
| 32 | FuelPoolHandler «Spent Fuel Pool Drum» | fuel_pool | = (рамки слотов NEIUniversal добавлены) |
| 33 | RBMKRodDisassemblyHandler «RBMK Rod Disassembly» | `rbmk_disassembly` | N — обогащение 0–4 и ксеноновая полоса, результат = рецепт верстака |
| 34 | RBMKWasteDecayHandler «Nuclear Waste Decay» | storage_drum | T + раскладка NEIUniversal (была gui_utility) |
| 35 | SatelliteHandler «Satellite» | `satellite` | N — 4 спутника, пулы POOL_SAT_MINER/LUNAR, «x.y%» |
| 36 | GrenadeRecipeHandler «Grenade Crafting» | `grenade` | N — 475 сочетаний |
| 37 | ZirnoxRecipeHandler | — | — reactor_zirnox в порте нет |
| 38 | PWRRecipeHandler | pwr | = (рамки добавлены) |
| 39 | WatzRecipeHandler | watz | L (пустой холст) |
| 40 | RTGRecipeHandler «RTG» (если rtgDecay) | `rtg` | N — 10 пеллет; катализатор только machine_rtg_grey (machine_difurnace_rtg_off нет в порте) |
| 41 | LiquefactionHandler | liquefaction | L |
| 42 | SolidificationHandler | solidification | L |
| 43 | CokingHandler | coker | L |
| 44 | PyroHandler | pyro_oven | L |
| 45 | FractioningHandler | fraction_tower | L (текстура порта не из оригинала) |
| 46 | BoilingHandler | boiling | = |
| 47 | CombinationHandler | combination_oven | L |
| 48 | SawmillHandler | sawmill | = |
| 49 | MixerHandler | mixer | L |
| 50 | OutgasserHandler | rbmk_outgasser | = |
| 51 | FusionBreederHandler | fusion_breeder_fluid + fusion_breeder_item | L (разделено на 2) |
| 52–53 | ElectrolyserFluid/MetalHandler (имя блока) | electrolyzer_fluid/metal | T, L |
| 54 | AshpitHandler (имя блока) | `ashpit` | N — 9 рецептов (уголь/дерево/прочее, fly ash 2000 mB, soot 8000 mB) |
| 55 | ArcWelderHandler | arc_welder | L |
| 56 | ExposureChamberHandler | exposure_chamber | L |
| 57–58 | ArcFurnaceSolid/FluidHandler (имя блока) | arc_furnace_solid/liquid | T, рамки добавлены |
| 59 | RotaryFurnaceHandler | rotary_furnace | L |
| 60 | AmmoPressHandler | ammo_press | L |
| 61 | CompressorHandler | compressor | L |
| 62 | ParticleAcceleratorHandler | particle_accelerator | L |
| 63 | DeuteriumHandler (имя блока) | `deuterium` | N — 1000 mB воды → 20 mB тяжёлой воды |
| 64 | RockMillRecipeHandler | rock_mill | L |
| 65 | SuperComputerRecipeHandler | supercomputer | L |
| 66 | SpaceAssemblerHandler | space_assembler | L |
| 67 | FluidRecipeHandler «Fluid Containers» | legacy_fluid_container | T + gui_nei_fluid.png (скопирован), раскладка 30/48/120, только заполнение |
| 68 | CustomMachineHandler (по конфигу) | custom_machine | не сверялось |

Только в порте (не удалялись): breeder_reactor, blast_furnace_fuel, armor_table, tacz_ammo_assembler (интеграция TACZ), silex_ir/visible/uv/gamma/digamma.

Удалённые файлы: `integration/jei/WatzConstructionJeiRecipe.java`, `WatzConstructionRecipeCategory.java`, `IcfConstructionJeiRecipe.java`, `IcfConstructionRecipeCategory.java`, `IcfToolingJeiRecipe.java`, `IcfToolingRecipeCategory.java`.

Техническая адаптация: ItemFluidIcon → FluidStack JEI; строки, которые NEI клал в лор стека (ItemStackUtil.addTooltipToStack), выводятся подсказкой слота JEI; рецепты плавки тигля без OreDictionary сгруппированы по одинаковому результату; JEI требует иконку категории — взята машина.

## Книги и GUI

| Объект | Было в порте | S3 |
|---|---|---|
| book_of_ / book_lemegeton (GUIBook/GUILemegeton) | GUI был; подписи обычным шрифтом | подписи шрифтом standard galactic (`minecraft:alt`) |
| Калькулятор (GUIScreenCalculator, клавиша N) | отсутствовал | `CalculatorScreen` + `util/Calculator` (дословная копия), режим QMAW по Tab, история 6, буфер обмена, колесо/стрелки/Delete/средняя кнопка |
| Slice Printer (GUIScreenSlicePrinter) | придуманный интерактивный экран с легендой и кнопками, запись границ в NBT предмета, 2 подсказки | экран оригинала: пурпурный фон, слой за кадром, изометрия, `printer/<дата>/slice_N.png`, чат «Slices saved to: …»; подсказка оригинала; без NBT; неcобранный PWR печатает только контроллер |
| book_guide (ItemGuideBook) | есть (`book_guide_book`, RBMK и Starter во вкладке) | подсказка: одна серая строка `String.join(" ", …)` вместо нескольких золотых |
| Bobmazon | состав совпадал, кроме перечисленного | снежный шар NONE добавлен; crucible с damage 3; «Fusion Man» — не курсивное имя + лор; «The Nut Bucket» — цвета 0/0 |
| QMAW | 264 страницы, 50 с изменёнными/удалёнными текстами | тексты 50 страниц восстановлены побайтно из оригинала; cinnabar: id триггера/иконки оставлен адаптированным |

Нет в порте/не переносилось: gun_kit_1/2, ammo_container, «Maid's Cleaning Utensils», «Frenchie's Reward» (оружейная система выведена).

## Проверки

work/verification-parity-s3.log: assemble, validateJarIntegrity, validateLangFiles, validateRecipeJsonSchemas, validateRecipeSemantics, validateRegistrySources, checkItemAudit (3516, errors=0) — OK. checkBuildingAudit: checked_parity_s3_cases=38, ошибок S3 нет; errors=3 от R1 (pink cloud / cloud / poison FX), не связано с S3 (в первом прогоне проходили). Статическая сверка: текстуры NEI побайтно (`cmp`), QMAW — work/tmp-s3/qmaw_cmp.py (264/264, отличается только адаптированный id cinnabar).
