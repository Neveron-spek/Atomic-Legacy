# parity-e1 — генерация мира, оружие мобов, рендер crane_router/partitioner и wand-блоков (2026-11-18)

Эталон: work/original (HbmMods 1.7.10, 936c60c). Статусы: «= (значения)» — сверено по числам в этой партии; «= (наличие)» — объект есть в порте, перенесён прежними партиями (R3/S2/S4), в e1 построчно не пересверялся; «исправлено» — правка e1.

## 1. Генерация мира

| Объект | Эталон | Порт | Статус |
|---|---|---|---|
| Руды верхнего мира (uranium, thorium, titanium, sulfur, aluminium, copper, fluorite, niter, tungsten, lead, beryllium, rare, lignite, asbestos, cinnabar, cobalt) | generateOre: жил/чанк, размер, minY + rand(variance) | placed/configured_feature: count, size, uniform min..max | = (значения), 16 проверок в диагностике |
| ore_copper (HBM) | 12 жил, размер 6, y 5..49 | reinhardtshbm:ore_copper генерируется с теми же числами (biome_modifier активен) | = ; не менялось. Вопреки постановке, HBM-медь в порте есть |
| Кластеры iron/titanium/aluminium/copper, limestone | 4/2/3/4 ×6, y 15..59/44/49/34; 1×16, y 25..54 | то же | = (значения) |
| alexandrite | 1/100 чанков, 1 жила ×3, y 10..14 | rarity 100, size 3, 10..14 | = |
| ore_coltan (528) | coltanRate 2 ×4, y 15..54, по флагу; залежь colRange 750 | ore_coltan_528 + ColtanDepositFeature | = (значения / наличие) |
| ore_tikite (Энд) | 8 ×6, y 0..126 по end_stone | то же | = |
| Руды Незера | U 8×6, W 10×10, S 26×12, P 24×6 (0..126), уголь 8×32 (16..111), Co 2×6 (100..125), Pu 8×4 по флагу; smoldering ×30, geysir_nether ×1 | NetherOreLegacyFeature — те же числа | = (значения) |
| Гнейсовые руды в сланце | 25/10/21/36/6/6/6/15 жил, размер 6 (газ 10), y 30..39 | SchistStratumFeature — те же | = (значения) |
| Глубинные кластеры | cluster_depth_* 24/32/32 | bedrock_deepslate_cluster_* | = (наличие); depth-руды cinnebar/zirconium/borax и stone_depth исключены пользователем; ore_borax/ore_zirconium (жильные, придуманы основой) оставлены как есть |
| Бедрок-руды | 1/10 чанка, +8+rand(2); верхний мир — generateAuto (цвет 0xD78A16, тир по плотности); Незер — glowstone 100 / phosphorus 50 / quartz 100 | BedrockOreDepositFeature | = (значения) |
| Нефтяной пузырь | 1/100 (÷3 при t≥2 и осадках <0.1), y 15..39, радиус 8 + rand(8) = 8..15 | радиус был 8..16 | исправлено (max исключён) |
| Нефтеносный песок | 1/200, y 56..71, радиус 16..47, fuzzy, только без дождя и t≥1.5 | радиус был 16..48 | исправлено |
| Бедрок-нефть | 1/200, пятно 5/50 | BedrockOilDepositFeature | = (наличие); порт дополнительно ставит жилы пористого камня (16×8, y 10..59) — этого в MapGenBedrockOil нет, оставлено, не согласовано |
| ore_oil / ore_deepslate_oil / ore_oil_sand (жильные json основы) | нет в эталоне | biome_modifier = neoforge:none (отключены) | = |
| Газовые пузыри | flammable 1/12, explosive 0; 1 жила ×32, y 30..39 | то же | = (значения) |
| Радиационный кратер (sellafield_slaked) | 1/5000, пустыня, радиус 8..63 | RadiationHotspotFeature | = (значения) |
| Сера/асбест-пещеры, hematite/bauxite/malachite, сланец | OreCave / OreLayer3D / SchistStratum | OreCaveFeature, ResourceOreLayer3DFeature, SchistStratumFeature | = (наличие) |
| Растения | foxglove 1/16 (BiomeGenForest), nightshade 1/8 (roofedForest), tobacco 1/8 (джунгли), hemp 1/64, reeds 1/4 река, 1/8 пляж | foxglove шёл по тегу IS_FOREST (включал grove) | исправлено: forest, flower_forest, birch, old_growth_birch, dark_forest |
| Метеорит | 1/200, +8, y − rand(10) | MeteoriteFeature/Generator | = (значение частоты; генератор — наличие) |
| Улей глифидов | 1/256, +8, заражённый 1/10 | GlyphidHiveFeature | = (значения) |
| Антенна, DesertAtom, библиотека, dud, корабль, бочка | 250 / 500 / 64 / 500 / 1000 / 5000, условия t/осадков | LegacySurface + HbmLegacyScatterStructure | = (значения частот и условий; геометрия — наличие) |
| Broadcaster, мины AP/HE, фонарь, сейф, арктическое хранилище, пирамида, джунгли | 5000 / 64 / 16 (528) / 2000 / 2500 / 500 / 4000 / 2000 | то же | = (значения); сейф: 4/3/2/1 из 10, лут 3–6 / 2–4 / 1–3 / 1–2, пауки 30 % |
| Хлорный гейзер | 1/3000, только plains, на траве | biome_modifier minecraft:plains | = |
| Капсула «Союза» | 1/100, только biome == beach, y − 4, meta 3 (ржавая текстура), record_glass в случайном слоте | генерация уже была; блок без meta 3, биом по тегу IS_BEACH (включал snowy_beach) | исправлено: свойство `rusty` у soyuz_capsule (по умолчанию false, ставит только генерация), рендер берёт soyuz_lander_rust.png (побайтно из эталона), биом только minecraft:beach |
| Розовое дерево | 1/1000, колонна x/z, log meta 0 (дуб, вертикальный) | заменялись и лежачие дубовые брёвна | исправлено: только ось Y |
| stone_keyhole | 1/4, +8, y 6..18, по камню | то же | = |
| Сундук чертежей | сетка 5000, вне ±100, 50 %, POOL_BLUEPRINTS ×50, обсидиан 3×3×3 | то же | = |
| NBT-структуры (38 условий + 2 нулевых веса + meteor_dungeon) | веса StructureConfig, смещения, min/maxHeight, шаг 4..16 чанков | HbmLegacyStructureSelection — веса, смещения и высоты совпадают | = (значения) |
| Условия биомов структур | heightVariation / rootHeight / BiomeDictionary 1.7.10 | были подобраны по современным тегам и расходились | исправлено, см. ниже |
| Процедурные features / bunker, jigsaw-пулы метеоритного подземелья | MapGenNTMFeatures, BunkerComponents, пулы NTMWorldGenerator | HbmLegacyProceduralPiece, HbmLegacyJigsawGenerator | = (наличие) |
| Лут | ItemPool* — 49 пулов | HbmStructureLoot — все 49 имён есть; POOL_VAULT_RUSTY сверен поэлементно | = (имена; содержимое остальных — наличие) |
| crane_mod.nbt, repeater_radio.nbt | объявлены в StructureManager, условий спавна нет | не скопированы | не генерируются и в эталоне |
| satelliteSpawn, geyserVaporSpawn | опции есть, в коде генерации не используются | нет | = |

### Условия биомов (worldgen/LegacyBiomeTraits)

Современные биомы не содержат heightVariation/rootHeight и типов BiomeDictionary, поэтому введена таблица «биом 1.21 → значения его предка в 1.7.10»; биомы без предка и модовые — по тегам.

| Структуры | Эталон | Было | Стало |
|---|---|---|---|
| spire | variation ≤ 0.05, не вода | любые не-горы (леса, тайга, болота) | plains, desert, savanna(+plateau), snowy_plains, beach, wooded_badlands |
| laboratory, radio, broadcaster_tower | isFlatBiome: ≤ 0.2 и SPARSE | plains, desert, snowy_plains | savanna, savanna_plateau, wooded_badlands |
| vertibird ×2, desert_shack ×3, dead_dish_small | SANDY | включали пляж | desert и badlands, без пляжа |
| dish, falloutshelter_house, water_pump | PLAINS (+SWAMP) | без саванны | plains, sunflower_plains, savanna, savanna_plateau (+swamp) |
| factory, crane | ≤ 0.2, не вода | включали mushroom_fields, stony_shore | исключены |
| forestchem, forest_post, plane1/2, tower_base | ≤ 0.3, не вода (tower_base ещё и не SANDY) | включали windswept_hills, stony_shore | исключены |
| arctic station, military_base | SNOWY | без frozen_ocean / frozen_river | включены |
| oil_rig | OCEAN и rootHeight ≥ −1.5 | любой океан | без deep_ocean |
| ruins A–J | не вода и canSpawnLightningBolt | hasPrecipitation (включал снежные) | дождь и базовая температура ≥ 0.15 |
| meteor_dungeon | rootHeight ≥ 0 | любые не-водные | без болота |

Ограничение (снято в K2): таблица сверена с MCP 9.08 и Forge 1.7.10 BiomeDictionary, см. parity-k2.md; биомы без предка (cherry_grove, meadow, mangrove_swamp, пещерные и т.п.) — приближение по тегам.

## 2. Оружие мобов (EntityAIFireGun, MobConfig)

| Элемент | Эталон | Порт | Статус |
|---|---|---|---|
| EntityAIFireGun | скорость 1.0, дальность 20, очередь 10, пауза 10–40, неточность 30; IDLE/WAIT/FIRING/RELOADING | entity/ai/LegacyFireGunGoal | реализовано |
| MobUtil.addFireTask, пулы slotPoolGuns (0.3/1/3/5), Tier1..3 | веса эталона, шанс дропа 0 | entity/LegacyMobEquipment | реализовано |
| getSkelegun | порог ln(soot)·0.25; <0.3 — pepperbox 5 / пусто 20 | тот же порядок бросков | реализовано |
| decorateMob / addAITasks (ModEventHandler) | зомби: хазмат 0.5 % при soot > 2; скелет: оружие 1 к 50 | event/LegacyMobSpawnEvents; добавлены zombie_villager, zombified_piglin, wither_skeleton (подклассы EntityZombie/EntitySkeleton) | реализовано |
| LogicBlockActions SKELETONS_GUN_TIER_1..3 | параметры тиров | LogicBlockEntity → assignLogicSkeletonTier | реализовано |
| Конфиги 12.D01–D03 | true / true / 0 | enableMobGear, enableMobWeapons, mobWeaponSootReduction | реализовано |

Расхождения: старт Goal может запаздывать на 1 тик (canUse опрашивается через тик); декорация только для MobSpawnType.NATURAL (приходил ли SpecialSpawn 1.7.10 от спавнеров — не проверено); husk/drowned/stray/bogged не декорируются (в 1.7.10 их не было); диагностика не покрывает три добавленных типа мобов; в ParityS2ConfigTable строк D02/D03 нет.

## 3. Рендер блоков

| Блок | Эталон | Порт | Статус |
|---|---|---|---|
| crane_router | RenderBlockMultipass: куб crane_in + оверлей по граням, цвета ff0000/ff8000/ffff00/00ff00/0080ff/8000ff; предмет — только crane_in | client/model/CraneRouterBlockModel | реализовано |
| crane_partitioner | RenderPartitioner: crane_buffer.obj, 6 текстур, поворот по meta | obj-модель + blockstate; предмет с display y+180 | реализовано; затенение стандартное, не формула ObjUtil |
| wand_jigsaw, wand_tandem | иконки [meta][side] и uvRotate по RenderBlockSideRotation | client/model/WandBlockModel + blocks/LegacyBlockRenderTables | реализовано, 6×6 |
| wand_loot, wand_logic | верх = meta, низ зеркально, бока 0 | то же | реализовано, 4×6 |

Расхождения: перестановка сторон и формулы UV сверены с RenderBlocks 1.7.10 (MCP 9.08, см. parity-k2.md); размещение wand_jigsaw/tandem — getNearestLookingDirection вместо determineOrientation (граничные углы); неиспользуемая crane_router_blue.png оставлена.

## Проверки

- work/verification-parity-e1.log: assemble, validateJarIntegrity, validateLangFiles, validateRegistrySources, validateObjModels, validateLatestModeRadarLootAndTrainParity, validateGlyphidSpawnSafety, checkBuildingAudit, checkItemAudit — BUILD SUCCESSFUL; building audit errors=0, checked_parity_e1_cases=935; item audit errors=0 (warnings=14).
- Диагностика event/ParityE1Diagnostics (+ ParityE1MobChecks, ParityE1RenderChecks): 23 руды по json, 30 умолчаний частот, условия биомов структур, свойство капсулы, пулы/Goal оружия, таблицы поворотов.
- build.gradle и recipe-baseline не менялись.
- Клиент не запускался; в игре ничего вручную не проверено. Сама генерация в мире (появление структур/капсулы) автоматически не проверялась — только значения и условия.

## Ручная проверка

1. Новый мир, пляж (minecraft:beach): капсула «Союза» утоплена на 4 блока, ржавая текстура, внутри пластинка; поставленная игроком капсула — обычная.
2. `/ntmlocate` (или полёт): laboratory/radio/broadcaster_tower — в саванне; вертиберды и лачуги — в пустыне/бесплодных землях, не на пляже; spire — на равнине/в пустыне, не в лесу.
3. Нефтяное пятно: залежь под ним, скважина даёт нефть.
4. Скелеты в сильно задымлённом (сажа > 5) чанке — часть с огнестрелом, стреляют очередями, оружие не выпадает.
5. crane_router: шесть цветных граней; crane_partitioner: задняя стенка к игроку, иконка как в 1.7.10; wand_jigsaw/tandem/loot/logic — ориентация рисунка граней во всех направлениях.
