# Parity U3 — прочность, инструмент, технические ячейки, порог нюка (2026-11-17)

Эталон: ModBlocks 936c60c4 + конструкторы (таблица `legacy/reinhardtshbm/parity-t3-resistance.json`, генератор `work/parity_t3_resistance.py` дополнен полями `material`, `harvest_tool`). destroyTime 1.21 = hardness 1.7.10 (камень 1.5 в обеих). Проверка: `ParityU3Diagnostics`, дамп `run-building-audit/parity-u3-hardness.tsv` (1014 блоков сопоставлены, 33 без аналога).

Инструмент (Forge 10.13, setHarvestLevel только у рельсов): кирка эффективна и добывает любым уровнем для Material rock/iron/anvil; топор — wood/plants/vine; лопата — только ванильные блоки; меч 1,5 — plants/vine/coral/leaves/gourd; ножницы 15 — leaves; инструмент обязателен для rock/iron/anvil/snow/craftedSnow/web. Теги генерирует `work/parity_u3_tools.py`.

| что | изменение |
|---|---|
| hardness (78 блоков) | тяжёлые двери 5→10 (secure_access_door 20), door_office/bunker/red 5→10, ore_meteor 15→5, ore_deepslate_*/cluster_deepslate_* 4.5→5 (coltan, schrabidium 15), 8 жидкостей 100→0, block_polymer/bakelite/rubber 5→3, compressor ×2/solidifier/liquefactor 5→10, pwr_block 5→15, crashed_bomb 3.6e6→-1, seal_hatch и statue_elb_f -1→∞, stone_keyhole(_meta) -1→0, wand_* и logic_block(_invis) →0, brick_fire_slab/double 15→5, pink_planks/slab/double/stairs 0.5→0, waste_earth 0.5→0.6, block_foam 0.2→0.5 |
| теги | needs_stone −26, needs_iron −321, needs_diamond −6 (все пусты); mineable/pickaxe +40 −101; mineable/axe +23; mineable/shovel −1 (ore_oil_sand); новый sword_efficient +14 |
| requiresCorrectToolForDrops | +15 (block_electrical_scrap, crate_lead/metal/red/jungle/ammo, ore_volcano, concrete_super_broken, reinforced_glass/_pane, reinforced_laminate/_pane, gravel_obsidian, block_foam, foam_layer); −7 (block_insulator, spotlight_beam, floodlight_beam, lamp_tritium ×4) |
| ячейки | heavy_door_part, machine_dummy, floodlight_dummy, dummy_plate_cargo реализуют `LegacyCoreProxy`: getExplosionResistance и (уже было) getDestroyProgress — по ядру; NukeExplosionManager (луч, мгновенная радиация), LegacyMukeExplosion, LegacyRadiate читают устойчивость ядра |
| нюк | убраны проверки «bedrock» и «устойчивость ≥ 3 600 000» в destroyCollected/fastDestroyCollected: в ExplosionNukeRayBatched.processChunk удаляется любой не-воздух на собранном луче |

Сохранено значение порта (решение 2026-11-16 / открытый вопрос): ore_borax, ore_zirconium, ore_nether_neodymium, ore_alexandrite и deepslate-двойники borax/zirconium/alexandrite — у оригинальных BlockDepthOre hardness −1.
Известные расхождения: waste_leaves/leaves_layer — ножницы 1 вместо 15 (в 1.21 только через #leaves, который даёт и скорость мотыги); rail_wood — кирка быстра через #minecraft:rails ⊂ mineable/pickaxe; ячейки сами имеют hardness 5 (getDestroySpeed в 1.21 не позиционный, прогресс добычи и устойчивость берутся у ядра); у нюка порт удаляет и последний собранный блок луча, оригинал — блоки до |lastPos−pos| (граница по float).
