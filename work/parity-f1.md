# f1 — поштучный разбор «лишних» и отсутствующих рецептов верстака/печи (2026-11-19)

Эталон 936c60c. Инструменты: `work/parity_e3_recipes.py` (расширен), новые `work/parity_f1_recipe_extra.py` (развёртка циклов/хендлеров), `work/parity_f1_recipe_fix.py` (удаления и правки; `--dry` — только список).

## Расширение разбора
- Исправлена регулярка `for(int i = a; i < N; …)` (в e3 в ней был управляющий символ — int-циклы не разворачивались, отсюда `?i`).
- Развёртка по таблице `UNROLL` (значения выписаны из исходников): `BlockMultiSlab/BlockGenericStairs.recipeGen` (из конструкторов ModBlocks), `bricks`, `craneCasing`, 16 бетонов (dyes[15-i]), 14 platemetal, cm (EnumCMMaterials), Mats-автоген WIRE/BOLT, EnumCokeType, WasteClass long(5)/short(8), EnumChemDye, EnumBedrockOre и ingot_steel_dusted (подстановка = все meta).
- Разбор `ItemScraps.create(MAT_X, INGOT.q(N))`, `MAT_X.id` без `Mats.`, порт `reinhardtshbm:hot_smelting`, `{waste_class:N}`; различение ModBlocks/ModItems с одинаковым именем (fallout, hev_battery); алиасы ore_byproduct#B_*, page_of_#PAGEn, crayon/chemical_dye/platemetal/concrete_colored meta 0.
- Итог сравнения: совпало 2029 → 2232; «лишних» в порте 331 → 64 (все разобраны ниже, остаток — классы а/б с причиной); отсутствующих 44 → 22.

## 1. «Лишние» рецепты порта (331 по выводу `review` до f1)
Классы: а — есть в эталоне (сопоставлено/эквивалентно/исправлено), б — согласованное исключение, в — нет в эталоне → удалён, в? — оставлен с сомнением.

| рецепт | класс | эталон | сделано |
|---|---|---|---|
| ball_fireclay_from_limestone_tag_and_sand | б | интеграция Create (mod_loaded) | не трогалось |
| ball_fireclay_from_raw_aluminium | б | deepslate/raw — согласованное исключение | не трогалось |
| billet_americium_fuel_from_ingots | в | эталон: только слиток↔самородок (MineralRecipes 92) и заготовка из U238+Am (168-170) | удалён |
| billet_americium_fuel_from_nuggets | в | эталон: только слиток↔самородок (MineralRecipes 92) и заготовка из U238+Am (168-170) | удалён |
| billet_polonium_from_ore_byproduct_polonium | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| billet_technetium_from_ore_byproduct_technetium | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| block_advanced_alloy_from_ingots | в | удалено в эталоне (ignoreMappings) | удалён |
| block_coke_coal_from_coke | а | MineralRecipes 60-62 (EnumCokeType) | сопоставлено |
| block_coke_lignite_from_coke | а | MineralRecipes 60-62 (EnumCokeType) | сопоставлено |
| block_coke_petroleum_from_coke | а | MineralRecipes 60-62 (EnumCokeType) | сопоставлено |
| block_fallout_from_falloutitem | а | MineralRecipes 58/106-107, CraftingManager 531-534 | сопоставлено (блок/предмет одного имени) |
| block_slag_from_ingot_raw_slag | а | MineralRecipes 58/106-107, CraftingManager 531-534 | сопоставлено (блок/предмет одного имени) |
| bolt_dura_steel_from_ingot_dura_steel | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| bolt_from_ingot_lead | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| bolt_from_ingot_steel | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| bolt_tungsten_from_ingot_tungsten | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| book_of_ | а | CraftingManager 945 | сопоставлено (page_of_#PAGEn) |
| brick_asbestos_stairs_from_brick_asbestos | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_compound_stairs_from_brick_compound | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_concrete_broken_stairs_from_brick_concrete_broken | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_concrete_cracked_stairs_from_brick_concrete_cracked | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_concrete_mossy_stairs_from_brick_concrete_mossy | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_concrete_stairs_from_brick_concrete | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_ducrete_stairs_from_brick_ducrete | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_fire_stairs_from_brick_fire | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_light_stairs_from_brick_light | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_obsidian_stairs_from_brick_obsidian | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_slab_from_brick_asbestos | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_slab_from_brick_compound | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_slab_from_brick_fire | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_slab_from_brick_light | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_slab_from_brick_obsidian | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_slab_from_reinforced_brick | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| brick_slab_from_reinforced_stone | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| cart_painted_empty | а | ToolRecipes 158-169 (NBT-слепое совпадение) | без изменений |
| cart_steel_destroyer | а | ToolRecipes 158-169 (NBT-слепое совпадение) | без изменений |
| cart_steel_empty | а | ToolRecipes 158-159 | без изменений |
| cart_wood_empty | а | ToolRecipes 158-159 | без изменений |
| cart_wood_powder | а | ToolRecipes 158-169 (NBT-слепое совпадение) | без изменений |
| cart_wood_semtex | а | ToolRecipes 158-169 (NBT-слепое совпадение) | без изменений |
| cm_port_alloy | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_port_steel | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_port_tcalloy | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_sheet_alloy | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_sheet_desh | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_sheet_steel | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_sheet_tcalloy | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_tank_alloy | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_tank_desh | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_tank_steel | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cm_tank_tcalloy | а | CraftingManager 1053-1057 (цикл i<4) | сопоставлено |
| cmb_brick_reinforced_stairs_from_cmb_brick_reinforced | в | в эталоне нет этих ступеней | удалён |
| coke_lignite_from_block_coke | а | MineralRecipes 60-62 (EnumCokeType) | сопоставлено |
| coke_petroleum_from_block_coke | а | MineralRecipes 60-62 (EnumCokeType) | сопоставлено |
| concrete_asbestos_stairs_from_concrete_asbestos | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_brick_slab_from_brick_concrete | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_brick_slab_from_brick_concrete_broken | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_brick_slab_from_brick_concrete_cracked | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_brick_slab_from_brick_concrete_mossy | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_brick_slab_from_brick_ducrete | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_colored_black | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_blue | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_brown | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_cyan | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_gray | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_green | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_light_blue | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_lime | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_magenta | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_orange | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_pink | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_purple | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_red | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_silver | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_white | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_colored_yellow | а | CraftingManager 387-391 (16 красителей) | сопоставлено |
| concrete_slab_from_asphalt | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_slab_from_concrete | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_slab_from_concrete_asbestos | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_slab_from_concrete_smooth | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_slab_from_ducrete | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_slab_from_ducrete_smooth | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_smooth_from_concrete_colored_blue | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_brown | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_cyan | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_ext_hazard | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_ext_indigo | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_ext_machine | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_ext_machine_stripe | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_ext_pink | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_ext_purple | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_ext_sand | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_gray | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_green | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_light_blue | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_lime | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_magenta | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_orange | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_pink | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_purple | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_red | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_silver | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_white | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_from_concrete_colored_yellow | в | дубль подстановочных legacy_building/concrete_smooth_392/393 (CraftingManager 392-393) | удалён |
| concrete_smooth_stairs_from_concrete_smooth | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| concrete_stairs_from_concrete | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| crane_extractor_iron | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_extractor_iron_wand | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_extractor_steel | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_extractor_steel_wand | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_extractor_stone | а | CraftingManager 868-881 (цикл craneCasing) | исправлено по эталону |
| crane_extractor_stone_wand | а | CraftingManager 868-881 (цикл craneCasing) | исправлено по эталону |
| crane_grabber_iron | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_grabber_iron_wand | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_grabber_steel | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_grabber_steel_wand | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_grabber_stone | а | CraftingManager 868-881 (цикл craneCasing) | исправлено по эталону |
| crane_grabber_stone_wand | а | CraftingManager 868-881 (цикл craneCasing) | исправлено по эталону |
| crane_inserter_iron | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_inserter_iron_wand | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_inserter_steel | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_inserter_steel_wand | а | CraftingManager 868-881 (цикл craneCasing) | сопоставлено |
| crane_inserter_stone | а | CraftingManager 868-881 (цикл craneCasing) | исправлено по эталону |
| crane_inserter_stone_wand | а | CraftingManager 868-881 (цикл craneCasing) | исправлено по эталону |
| crate_template | в | удалено в эталоне (ignoreMappings) | удалён |
| crayon_blue | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_brown | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_cyan | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_gray | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_green | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_lightblue | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_lime | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_magenta | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_orange | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_pink | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_purple | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_red | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_silver | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_white | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| crayon_yellow | а | PowderRecipes 100 (EnumChemDye) | сопоставлено |
| ducrete_smooth_stairs_from_ducrete_smooth | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| ducrete_stairs_from_ducrete | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| energy_core | в | удалено в эталоне (ignoreMappings) | удалён |
| fallout_from_falloutitem | а | MineralRecipes 58/106-107, CraftingManager 531-534 | сопоставлено (блок/предмет одного имени) |
| fluid_duct_neo_from_ff_fluid_duct | в? | ff_fluid_duct нет в эталоне; конвертация порт-only предмета | оставлено (сомнение: путь миграции) |
| fluid_pump | в | блок без рецепта в эталоне | удалён |
| hev_battery_block_from_item | а | MineralRecipes 58/106-107, CraftingManager 531-534 | сопоставлено (блок/предмет одного имени) |
| holotape_image | а | CraftingManager 859 (HOLO_DIGAMMA → HOLO_RESTORED, ntmscrewdriver) | исправлено: варианты + тег c:tools/screwdrivers; holotape_damaged принимает HOLO_RESTORED |
| ingot_advanced_alloy_from_block | в | удалено в эталоне (ignoreMappings) | удалён |
| ingot_americium_fuel_from_billets | в | эталон: только слиток↔самородок (MineralRecipes 92) и заготовка из U238+Am (168-170) | удалён |
| ink | а | ConsumableRecipes 197 (red_flower wildcard = 9 цветов) | без изменений |
| lbsm_wash_deepslate_uranium | б | deepslate/raw — согласованное исключение | не трогалось |
| lbsm_wash_deepslate_uranium_bulk | б | deepslate/raw — согласованное исключение | не трогалось |
| lbsm_wire_aluminium | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_carbon | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_copper | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_gold | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_lead | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_magnetized_tungsten | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_red_copper | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_steel | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_tungsten | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| lbsm_wire_zirconium | а | CraftingManager 973-982 (Mats autogen WIRE [LBSM]/BOLT) | сопоставлено |
| legacy_building/brick_ducrete_370 | а | CraftingManager (точная форма, id из building-recipes.json) | оставлен; дубль без legacy_building удалён |
| legacy_building/brick_ducrete_371 | а | CraftingManager (точная форма, id из building-recipes.json) | оставлен; дубль без legacy_building удалён |
| legacy_building/brick_fire_929 | а | CraftingManager (точная форма, id из building-recipes.json) | оставлен; дубль без legacy_building удалён |
| legacy_building/concrete_smooth_392 | а | CraftingManager 392-393 (wildcard) | оставлен (подстановочный) |
| legacy_building/concrete_smooth_393 | а | CraftingManager 392-393 (wildcard) | оставлен (подстановочный) |
| legacy_building/filing_cabinet_935 | а | CraftingManager (точная форма, id из building-recipes.json) | оставлен; дубль без legacy_building удалён |
| legacy_building/vinyl_tile_938 | а | CraftingManager 938 (meta 0 через neoforge:difference) | без изменений |
| lightstone | в | дубль legacy_building/lightstone_422 (CraftingManager 422) | удалён |
| lightstone_bricks_stairs_from_lightstone_bricks | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| lightstone_tile_stairs_from_lightstone_tile | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| machine_controller_cdalloy | в | блок без рецепта в эталоне | удалён |
| machine_controller_tcalloy | в | блок без рецепта в эталоне | удалён |
| nuclear_waste/long_neptunium_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_neptunium_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_schrabidium_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_schrabidium_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_schrabidium_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_schrabidium_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_thorium_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_thorium_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_thorium_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_thorium_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_uranium233_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_uranium233_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_uranium233_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_uranium233_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_uranium235_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_uranium235_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_uranium235_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/long_uranium235_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_americium242_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_americium242_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_neptunium_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_neptunium_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_neptunium_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_neptunium_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium239_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium239_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium239_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium239_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium240_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium240_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium240_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium240_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium241_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium241_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium241_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_plutonium241_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_schrabidium_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_schrabidium_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_schrabidium_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_schrabidium_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_uranium233_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_uranium233_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_uranium233_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_uranium233_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_uranium235_depleted_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_uranium235_depleted_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_uranium235_from_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nuclear_waste/short_uranium235_to_tiny | а | MineralRecipes 96-104 (WasteClass) | сопоставлено (+ разбор waste_class) |
| nugget_americium_fuel_from_billet | в | эталон: только слиток↔самородок (MineralRecipes 92) и заготовка из U238+Am (168-170) | удалён |
| nugget_raw_aluminium_from_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_aluminium_to_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_boron_from_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_boron_to_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_copper_from_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_copper_to_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_lithium_from_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_lithium_to_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_sodium_from_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_sodium_to_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_strontium_from_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_strontium_to_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_titanium_from_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_titanium_to_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_tungsten_from_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_raw_tungsten_to_ingot | а | OreDictManager 666-670 (Mats NUGGET: Ti,Cu,W,Al,B,Na,Sr,Li) | без изменений (динамический цикл) |
| nugget_silicon_from_ore_byproduct_silicon | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| pipe_lead | в | 4 pipe_lead → pipe_lead (цикл); настоящий — weapon_pipe_lead (ToolRecipes 100) | удалён |
| platemetal/platemetal_black | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_blue | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_cyan | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_green | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_light_blue | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_light_gray | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_lime | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_magenta | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_orange | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_pink | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_purple | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_red | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_white | а | CraftingManager 396-399 | сопоставлено |
| platemetal/platemetal_yellow | а | CraftingManager 396-399 | сопоставлено |
| pneumatic_storage_access | в | блоки есть, рецептов в эталоне нет | удалён |
| pneumatic_storage_clutter | в | блоки есть, рецептов в эталоне нет | удалён |
| pneumatic_storage_exporter | в | блоки есть, рецептов в эталоне нет | удалён |
| pneumatic_storage_importer | в | блоки есть, рецептов в эталоне нет | удалён |
| pneumatic_storage_mono | в | блоки есть, рецептов в эталоне нет | удалён |
| powder_aluminium_from_ore_byproduct_aluminium | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_bismuth_from_ore_byproduct_bismuth | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_calcium_from_ore_byproduct_calcium | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_chlorocalcite_from_fragments | а | OreDictManager 653-662 (FRAGMENT → dust/ore) | без изменений |
| powder_copper_from_ore_byproduct_copper | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_iron_from_ore_byproduct_iron | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_lead_from_ore_byproduct_lead | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_lithium_from_ore_byproduct_lithium | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_ra226_from_ore_byproduct_radium | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_titanium_from_ore_byproduct_titanium | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| powder_uranium_from_ore_byproduct_uranium | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| reinforced_brick_stairs_from_reinforced_brick | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| reinforced_sand_stairs_from_reinforced_sand | в | в эталоне нет этих ступеней | удалён |
| reinforced_stone_stairs_from_reinforced_stone | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| sand_mix_quartz | б | CraftingManager 689; тег ae2:all_quartz_dust (AE2) шире dustNetherQuartz | не трогалось |
| scraps_magnetized_tungsten_from_tungsten_schrabidium | а | PowderRecipes 61-65 (ItemScraps.create; 144/72/72/72/288 сверены) | сопоставлено |
| scraps_red_copper_from_copper_redstone | а | PowderRecipes 61-65 (ItemScraps.create; 144/72/72/72/288 сверены) | сопоставлено |
| scraps_steel_bulk_from_iron_coal | а | PowderRecipes 61-65 (ItemScraps.create; 144/72/72/72/288 сверены) | сопоставлено |
| scraps_steel_from_iron_coal | а | PowderRecipes 61-65 (ItemScraps.create; 144/72/72/72/288 сверены) | сопоставлено |
| scraps_tcalloy_from_steel_technetium | а | PowderRecipes 61-65 (ItemScraps.create; 144/72/72/72/288 сверены) | сопоставлено |
| sliding_blast_door_2_from_sliding_blast_door | в | порт-only варианты дверей | удалён |
| sliding_blast_door_from_sliding_blast_door_2 | в | порт-only варианты дверей | удалён |
| sliding_gate_door_from_sliding_seal_door | в | порт-only варианты дверей | удалён |
| sliding_seal_door_from_sliding_gate_door | в | порт-only варианты дверей | удалён |
| smelting_ore_coltan | в | руда колтана в эталоне не плавится | удалён |
| smelting_ore_deepslate_aluminium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_australium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_beryllium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_cobalt | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_coltan | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_schrabidium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_thorium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_titanium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_tungsten | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_uranium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_ore_deepslate_uranium_scorched | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_aluminium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_australium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_beryllium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_cobalt | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_coltan | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_lead | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_lithium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_plutonium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_schrabidium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_thorium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_titanium | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_tungsten | б | deepslate/raw — согласованное исключение | не трогалось |
| smelting_raw_uranium | б | deepslate/raw — согласованное исключение | не трогалось |
| stamp_desh_flat_from_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_desh_flat_from_nether_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_iron_flat_from_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_iron_flat_from_nether_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_obsidian_flat_from_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_obsidian_flat_from_nether_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_steel_flat_from_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_steel_flat_from_nether_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_stone_flat_from_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_stone_flat_from_nether_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_titanium_flat_from_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stamp_titanium_flat_from_nether_brick | а | CraftingManager 346-354 (цикл bricks) | сопоставлено |
| stone_resource_bauxite_from_bedrock_fragments | а | OreDictManager 653-662 (FRAGMENT → dust/ore) | без изменений |
| stones_slab_from_lightstone_bricks | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| stones_slab_from_lightstone_tile | а | CraftingManager 90-95 recipeGen (ModBlocks 1530-1558) | сопоставлено парсером |
| struct_watz_core_tcalloy | а | CraftingManager 357/719 ANY_RESISTANTALLOY = 2 рецепта (cdalloy+tcalloy) | без изменений |
| sulfur_from_ore_byproduct_sulfur | а | MineralRecipes 428-441 | сопоставлено (ore_byproduct#B_*) |
| tile_lab_broken_stairs_from_tile_lab_broken | в | в эталоне нет ступеней tile_lab* | удалён |
| tile_lab_cracked_stairs_from_tile_lab_cracked | в | в эталоне нет ступеней tile_lab* | удалён |
| tile_lab_stairs_from_tile_lab | в | в эталоне нет ступеней tile_lab* | удалён |
| watz_pump_tcalloy | а | CraftingManager 357/719 ANY_RESISTANTALLOY = 2 рецепта (cdalloy+tcalloy) | без изменений |

Дополнительно (не было в списке «лишних», найдено при разборе):
- `smelting_ingot_chainsteel_smelting_167`, `smelting_ingot_meteorite_smelting_168`, `smelting_ingot_meteorite_forged_smelting_169`, `smelting_blade_meteorite_smelting_170` — обычные плавки-дубли `smelting_hot_*` (ванильная печь выбирала бы их и не нагревала) → удалены (в).
- `concrete_smooth_from_concrete_colored_black`, `…_ext_bronze`, `brick_ducrete_from_ducrete`, `brick_ducrete_from_ducrete_smooth`, `brick_fire`, `filing_cabinet_steel` — дубли рецептов legacy_building (id которых закреплены фикстурой BuildingPortDiagnostics) → удалены (в).
- `pink_stairs_from_pink_planks`: 6 → 4 (а). В эталоне два одинаковых рецепта: recipeGen (4, регистрируется первым, CraftingManager 93) и строка 634 (6); в 1.7.10 срабатывает первый, т.е. 4.
- Каменные краны (6 файлов): `Blocks.stonebrick` в ShapedOreRecipe = любой meta → 4 вида каменных кирпичей (а, как furnace_iron в e3).
- Тележки: не менялись. Попытка развести базы откатана: в 1.7.10 ShapedOreRecipe сравнивает тележку только по damage (база в NBT не учитывается), поэтому первый зарегистрированный рецепт (wood) затеняет steel/painted, а окрашенная принимает любую пустую — порт это воспроизводит (диагностика ParityS1).
- `holotape_damaged` принимает только HOLO_RESTORED (CraftingManager 860).

## 2. Отсутствующие в порте рецепты эталона (44 → 22)

| рецепт эталона | эталон | решение |
|---|---|---|
| smelt ore_bedrock (все 15 типов) → 16 булыжника, 0.1 | SmeltingRecipes 175-178 | добавлен `smelting_ore_bedrock_to_cobblestone` |
| schrabidium_plate (нагрудник) LBSM | ArmorRecipes 164/170 | добавлен `lbsm_schrabidium_chestplate` (файл `lbsm_schrabidium_plate` — это LBSM plate_schrabidium) |
| obj_tester | CraftingManager 677 | не перенесён: в порте нет предмета obj_tester (только модель; блок скрытый технический) |
| holotape_image HOLO_RESTORED | CraftingManager 859 | исправлен `holotape_image` |
| conveyor_wand EXPRESS ×8 | CraftingManager 226 | есть: тип `reinhardtshbm:conveyor_express` (а) |
| crate_desh, crate_tungsten, safe | CraftingManager 273-276 | есть: типы `crate_*_upgrade`, `safe` (ContainerUpgrade, а) |
| copper_block ↔ 9 copper_ingot | MineralRecipes 227/278 | ванильные рецепты (а) |
| pink_stairs ×6 | CraftingManager 634 | затенён recipeGen ×4 в 1.7.10 (а, см. выше) |
| ingot_pu_mix из nuggetPluonium240 | MineralRecipes 419 | опечатка ключа в эталоне — рецепт там не крафтится; не переносится |
| oc_cable_paintable | CraftingManager 234 | OpenComputers (б) |
| sand_mix QUARTZ, powder_meteorite | CraftingManager 689, PowderRecipes 53 | есть с тегом AE2 (б) |
| vinyl_tile #1, ink, platemetal сброс, gun_kit_2, fence_metal post, scrap_plastic→polymer | 938, Consumable 197, 400, 543, 680, Smelting 173 | эквивалентная форма (difference/список/OreDict bdcl) (а) |
| zirnox depleted → waste ×2 (meta 1) ×9 | RodRecipes 39-47 | эквивалентно: `spentFuelPoolCooling` = горячие отходы (а) |
| depth_brick/tiles, depth_nether_brick/tiles | CraftingManager 414-417 | исключены пользователем |

## Удалённые файлы (60, `data/reinhardtshbm/recipe/`)
- concrete_smooth_from_concrete_colored_black.json
- concrete_smooth_from_concrete_colored_blue.json
- concrete_smooth_from_concrete_colored_brown.json
- concrete_smooth_from_concrete_colored_cyan.json
- concrete_smooth_from_concrete_colored_ext_bronze.json
- concrete_smooth_from_concrete_colored_ext_hazard.json
- concrete_smooth_from_concrete_colored_ext_indigo.json
- concrete_smooth_from_concrete_colored_ext_machine.json
- concrete_smooth_from_concrete_colored_ext_machine_stripe.json
- concrete_smooth_from_concrete_colored_ext_pink.json
- concrete_smooth_from_concrete_colored_ext_purple.json
- concrete_smooth_from_concrete_colored_ext_sand.json
- concrete_smooth_from_concrete_colored_gray.json
- concrete_smooth_from_concrete_colored_green.json
- concrete_smooth_from_concrete_colored_light_blue.json
- concrete_smooth_from_concrete_colored_lime.json
- concrete_smooth_from_concrete_colored_magenta.json
- concrete_smooth_from_concrete_colored_orange.json
- concrete_smooth_from_concrete_colored_pink.json
- concrete_smooth_from_concrete_colored_purple.json
- concrete_smooth_from_concrete_colored_red.json
- concrete_smooth_from_concrete_colored_silver.json
- concrete_smooth_from_concrete_colored_white.json
- concrete_smooth_from_concrete_colored_yellow.json
- brick_ducrete_from_ducrete_smooth.json
- brick_ducrete_from_ducrete.json
- brick_fire.json
- filing_cabinet_steel.json
- lightstone.json
- smelting_ingot_chainsteel_smelting_167.json
- smelting_ingot_meteorite_smelting_168.json
- smelting_ingot_meteorite_forged_smelting_169.json
- smelting_blade_meteorite_smelting_170.json
- tile_lab_stairs_from_tile_lab.json
- tile_lab_cracked_stairs_from_tile_lab_cracked.json
- tile_lab_broken_stairs_from_tile_lab_broken.json
- reinforced_sand_stairs_from_reinforced_sand.json
- cmb_brick_reinforced_stairs_from_cmb_brick_reinforced.json
- block_advanced_alloy_from_ingots.json
- ingot_advanced_alloy_from_block.json
- crate_template.json
- energy_core.json
- machine_controller_cdalloy.json
- machine_controller_tcalloy.json
- fluid_pump.json
- pneumatic_storage_access.json
- pneumatic_storage_clutter.json
- pneumatic_storage_exporter.json
- pneumatic_storage_importer.json
- pneumatic_storage_mono.json
- billet_americium_fuel_from_ingots.json
- billet_americium_fuel_from_nuggets.json
- ingot_americium_fuel_from_billets.json
- nugget_americium_fuel_from_billet.json
- sliding_blast_door_2_from_sliding_blast_door.json
- sliding_blast_door_from_sliding_blast_door_2.json
- sliding_gate_door_from_sliding_seal_door.json
- sliding_seal_door_from_sliding_gate_door.json
- smelting_ore_coltan.json
- pipe_lead.json

## Добавленные файлы
`smelting_ore_bedrock_to_cobblestone`, `lbsm_schrabidium_chestplate`.
## Изменённые файлы
`crane_{extractor,grabber,inserter}_stone{,_wand}` (6), `pink_stairs_from_pink_planks`, `holotape_image`, `holotape_damaged`, recipe-baseline.

## Правки build.gradle
- validateRecipeJsonSchemas: из `requiredOutputs` убран `pipe_lead` (lead pipe в эталоне и порте делается на наковальне — Java-рецепты `HbmAnvilRecipes`; единственный JSON-источник был зацикленным рецептом 4 pipe_lead → pipe_lead).

## Ограничения
- Блоки без рецепта в эталоне остаются зарегистрированными (tile_lab*_stairs, reinforced_sand_stairs, cmb_brick_reinforced_stairs, sliding_blast_door_2, sliding_gate_door, ff_fluid_duct, pneumatic_storage_* и др.) — теперь только из творческого режима; их удаление/скрытие — вне f1.
- deepslate/raw-плавки coltan сохранены как исключение, хотя сама руда колтана в эталоне не плавится.
- `fluid_duct_neo_from_ff_fluid_duct` оставлен (в?): ff_fluid_duct нет в эталоне, рецепт служит конвертацией.
- Сопоставление тележек и nugget_raw/фрагментов проверено вручную (динамические циклы OreDictManager/ToolRecipes парсером не разворачиваются).

## Проверки
`work/verification-parity-f1.log`: updateRecipeBaseline, затем assemble, validateJarIntegrity, validateRecipeJsonSchemas, validateRecipeSemantics, validateRegistrySources, validateRecipeBaseline, validateLegacyGlobalGameplayParity, validateTurretRecipeChain, validateRocketMissileRecipes, validateFlattenedFoundryMolds, validateFlattenedScraps, checkBuildingAudit (errors=0), checkItemAudit (errors=0, warnings=16) — BUILD SUCCESSFUL. Ошибок разбора рецептов нет. По ходу диагностики поймали две мои ошибки, исправлено: obj_tester (в порте нет предмета → рецепт не добавлен), тележки (ParityS1 → откат).

## Ручная проверка
1. Каменный кран (вставщик/извлекатель/захват) крафтится из мшистых/потрескавшихся/резных каменных кирпичей.
2. Розовые ступени: 6 досок → 4.
3. Голопленка: DIGAMMA + отвёртка (любая) + изолента + полироль → RESTORED; повреждённая — только из RESTORED.
4. Бедроковая руда в печи → 16 булыжника.
5. LBSM simple armor: 8 шрабидиевых слитков → нагрудник.
6. Нагрев в печи слитков chainsteel/meteorite и лезвия даёт горячий предмет (дубли обычной плавки удалены).
7. Из творческого: tile_lab*_stairs, reinforced_sand_stairs, pneumatic_storage_*, sliding_gate_door больше не крафтятся (ожидаемо).
