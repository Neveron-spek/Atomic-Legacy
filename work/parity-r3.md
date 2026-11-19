# Parity R3 — генерация мира (система 11)

Эталон: HbmWorldGen, HbmWorld (MapGenChainloader), NTMWorldGenerator, MainRegistry (OreCave/OreLayer3D/SchistStratum), WorldConfig/StructureConfig/MobConfig (значения `createConfig*` по умолчанию), ItemPool*, LootGenerator.
Диагностика: `event/ParityR3Diagnostics` (`checked_parity_r3_cases` в building-audit).

Адаптация высот: числа Y оригинала сохранены как **абсолютные** Y 1.21 (мир 1.7.10 был 0..255). Прежде min был `above_bottom` (−64+N), а max абсолютным — диапазон рос вчетверо и уходил в deepslate; теперь `absolute N .. N+variance−1`, как `minHeight + nextInt(variance)`. Привязанное к дну мира (бедрок-руда, бедрок-нефть) — от `minBuildHeight`.

## Руды (placed feature: жил/чанк, размер, Y)

| ID | Оригинал | Порт до | Итог |
|---|---|---|---|
| ore_uranium | 7, 5, 5..24 | Y −59..24 | исправлено |
| ore_thorium | 7, 5, 5..29 | Y от дна | исправлено |
| ore_titanium | 8, 6, 5..34 | Y от дна | исправлено |
| ore_sulfur | 5, 8, 5..34 | Y от дна | исправлено |
| ore_aluminium | 7, 6, 5..44 | Y от дна | исправлено |
| ore_fluorite | 6, 4, 5..49 | Y от дна | исправлено |
| ore_niter | 6, 6, 5..34 | Y от дна | исправлено |
| ore_tungsten | 10, 8, 5..34 | Y от дна | исправлено |
| ore_lead | 6, 9, 5..34 | Y от дна | исправлено |
| ore_beryllium | 6, 4, 5..34 | Y от дна | исправлено |
| ore_rare | 6, 5, 5..24 | Y от дна | исправлено |
| ore_lignite | 2, 24, 35..59 | Y от дна | исправлено |
| ore_asbestos | 2, 4, 16..31 | Y от дна | исправлено |
| ore_cinnabar | 1, 4, 8..23 | Y от дна | исправлено |
| ore_cobalt | 2, 4, 4..11 | Y от дна | исправлено |
| cluster_iron/titanium/aluminium/copper | 4/2/3/4, 6, 15..59/44/49/34 | Y от дна | исправлено |
| stone_resource_limestone | 1, 16, 25..54 | = | совпадает |
| ore_alexandrite | 1/100 чанков, 3, 10..14 | Y от дна | исправлено |
| ore_coltan (528) | coltanRate 2, 4, 15..54 | Y от дна | исправлено |
| ore_tikite (Энд) | 8, 6, 0..126 | = | совпадает |
| незер: uranium/tungsten/sulfur/fire/coal/cobalt/plutonium | 8/10/26/24/8/2/8; 6/10/12/6/32/6/4; 0..126, уголь 16..111, кобальт 100..125 | = | совпадает (NetherOreLegacyFeature), тлеющая руда 30 попыток, geysir_nether 1 |
| сланец (stone_gneiss, шум 0.01, порог 5, Y 30±) | gneiss iron 25, gold 10, uranium 21, **copper 36**, asbestos 6, lithium 6, rare 6, gas 15; Y 30..39 | без меди | **перенесён ore_gneiss_copper** (блок, ресурсы, лут, переплавка 5 XP → minecraft:copper_ingot (HBM ingot_copper в порту = ванильный слиток), шредер ×2 powder_copper, теги c:ores/copper, 9 переводов) и его генерация |
| ore_copper (HBM) 12, 6, 5..49 | — | не генерируется | решение основы: ванильная медь 1.21 вместо HBM ore_copper — открытый вопрос |
| ore_borax, ore_zirconium (замена ore_depth_*) | глубинная система (исключена) | копия распределения киновари | Y приведён к абсолютному; само наличие — открытый вопрос |
| bedrock_deepslate_cluster_* (замена cluster_depth_*) | DepthDeposit у бедрока | — | не сверялось (глубинная система исключена) |
| ore_oil / ore_oil_sand / ore_deepslate_oil (жилы) | нет в оригинале | biome modifier `neoforge:none` | оставлены отключёнными, проверяется |

## Особые образования

| Объект | Оригинал | Итог |
|---|---|---|
| Газовый карман gas_flammable / gas_explosive | 1/12, 1/0; размер 32; Y 30..39 | совпадает |
| Нефтяной пузырь MapGenBubble | 1/100 (÷3 в жарких сухих), r 8..16, Y 15..39 | совпадает |
| Битуминозный песок | 1/200, песок, жаркие без дождя, r 16..48, Y 56..71, fuzzy | совпадает |
| Бедрок-нефть MapGenBedrockOil | 1/200, |x|,|z|<5, y 0..4, L1 ≤ 6; заменяет stone **или** bedrock; пятно 50×σ5 | исправлено: порт заменял только бедрок, теперь stone/deepslate/bedrock |
| Бедрок-руда | 1/10, центр 2×2, 3×3, вес незера glowstone 100/phosphorus 50/quartz 100 | совпадает; вмещающая порода deepslate/netherrack вместо stone_depth(_nether) — исключённые блоки |
| Австралиум | 0..3 попытки (bound перебрасывается каждый шаг), Y 15..29, жила 50, −450..−350 | исправлено: Y был от дна, bound один раз, порядок RNG |
| Колтан-залежь 528 | 2×5 попыток, Y 15..39, центр gauss·1500, 750/r | совпадает |
| Метеорит | 1/200 (config 2.23) | совпадает |
| Кратер Sellafield (radfreq) | 1/5000, пустыня | совпадает (RadiationHotspotFeature) |
| Хлорный гейзер | 1/3000, plains, на траве | совпадает |
| Улей глифидов | 1/256 | совпадает |
| Сульфидные/асбестовые пещеры, гематит/боксит/малахит | пороги и масштабы MainRegistry | совпадает |
| Цветы, камыш, розовое дерево 1/1000, keyhole 1/4, лампа-бегемот 1/2000, чертёжный сундук 5000×5000 | | совпадает |
| Антенна 250, атом 500, библиотека 64, бомба-неразорвавшаяся 500, корабль 1000, бочка 5000, вещатель 5000, мины 64, сейфы 2500, капсула 100, арктика 500, джунгли 2000, пирамида 4000 | | совпадают (значения по умолчанию сверены) |
| Биомы кратеров (BiomeGenCraterBase, id 80–82) | меняются ядерными взрывами | не перенесено: часть системы ядерных взрывов и радиации, не самостоятельный worldgen |

## Структуры NTMWorldGenerator

| Параметр | Оригинал | Итог |
|---|---|---|
| structureMaxChunks | 16 | **исправлено** (было 12 — значение 1.12.2) |
| structureMinChunks | 4 | совпадает |
| Веса 41 структуры, plainsNull 20, oceanNull 35 | StructureConfig | совпадают |
| submarine | вес 2, ocean/deep_ocean, смещение −10, submarine.nbt | **перенесено** (NBT, кандидат, конфиг, structure/structure_set JSON) |
| factory, crane | heightVariation ≤ 0.2 и не вода | **исправлено**: порт брал isFlatBiome (+SPARSE) |
| Остальные условия биомов | heightVariation/BiomeDictionary | приближения тегами 1.21 основы оставлены (spire ≤0.05 ≈ «не холмы/горы») |
| NBT | 83 файла; crane.nbt не используется (используется crane_mod), repeater_radio не используется | 81 в порту, строгий конвертер палитр и целевых блоков wand_loot — в диагностике |

## Лут (ItemPool*)

Все 49 пулов сгенерированы заново из ItemPool*.java эталона в исходном порядке; min/max/вес не изменены, мета переведена в плоские ID/варианты порта.
Было: 17 пулов расходились (выпавшие стволы, патроны и наборы, сдвинутые веса, min 0→1 в REPAIR_MATERIALS, SPACESHIP coil_copper 2..16→1..2, неверные материалы pipe 2600=iron, wire_fine mingrade=red_copper, wire_dense mingrade=red_copper), 5 пулов отсутствовали (POOL_PILE_MAKESHIFT_GUN, POOL_SAT_LUNAR, POOL_SAT_MINER, POOL_SNACKS, POOL_SODA), в POOL_PILE_MED_SYRINGE и POOL_SUPPLIES были 7 предметов другого мода `reinhardt_protocol_medical` — удалены.
Добавлено для точности: вариант папки чертежей «secret» (meta 2: модель, текстура, пул `secret.`), пустая паяльная лампа (`getEmptyTool`), противогаз M65 с повреждением 60.
Заполнение контейнеров — как `generateChestContents`: случайный слот с перезаписью, стаки сверх максимума — по одному. Кучи LootGenerator: книга «beacon» (book_lore, 12 стр.), CAPNUKE (nuke_standard / rocket_heat и их позиции), MAKESHIFT_GUN (ствол из пула 50%), FLAREGUN (ракетница + g26_flare + supply/weapon) — исправлены.
Консервы — canned_conserve с вариантом (BEEF/MYSTERY/TUBE), пепел meta 0 — powder_ash_wood.
Не перенесено: gun_kit_1, gun_kit_2 (ItemRepairKit зависит от износа оружия) и ammo_container (снят основой вместе с базовым оружием; в POOL_PILE_SUPPLIES вес 40 из 210) — записи пулов есть, при генерации пропускаются (распределение остальных смещается); диагностика допускает только эти три.
Строгая проверка: палитры всех 81 NBT и целевые блоки wand_loot конвертируются без ошибок; все пулы wand_loot в NBT существуют.
