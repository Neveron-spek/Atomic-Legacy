# Parity S4 (2026-11-17)

| Пункт | Оригинал 1.7.10 | Было в порте | Сделано |
|---|---|---|---|
| Culling: chlorine/pc/cloud/orange FX | EntityModFX: ignoreFrustumCheck, distance < 25000 | рендерер без shouldRender, проверка не разбирала `provider(...)` | LegacyCloudFxRenderer.shouldRender: точный радиус 5 билбордов (Random(100)) × 3.75; разбор статических `X.provider(` в validateEntityRendererCullingBounds |
| Culling: Duchess Gambit | ignoreFrustumCheck, distance < 25000 | shouldRender без фрустума | радиус OBJ вокруг (0,0,-1), дистанция по правилу сущности |
| Culling: beam-пули | ignoreFrustumCheck, renderDistanceWeight 10 | без фрустума | сфера beamLength + 13 (ширина вспышки folly), дистанция сущности |
| Culling: chemthrower | ignoreFrustumCheck | noCulling = true | конверт по стилю (GAS exp·10·√2, GASFLAME exp·2·√2, AMAT/LIGHTNING длина луча); noCulling убран |
| Culling: solinium / rainbow cloud | ignoreFrustumCheck; rainbow distance < 25000 | noCulling, нет shouldRender, rainbow без лимита дистанции | радиус сферы × (age+1)·1.025³ / age; rainbow — distance < 25000 |
| Culling: крюк charge thrower | ignoreFrustumCheck (провод до игрока) | noCulling = true | поле LegacyBulletEntity.ignoreFrustumCheck; конверт = крюк ∪ AABB владельца ±1.5 |
| Culling: grenade_universal | RenderGrenadeUniversal (поза THROWN, ×0.0625) | контракт groundBounds (поза GROUND) | thrownRadius по baked-моделям в позе THROWN; контракт проверки обновлён |
| taint | ItemBlockBase, вкладка null, getItemDropped null, Material.iron, MapColor.gray | без предмета, noLootTable, COLOR_PURPLE | предмет (скрыт), лут только шёлковое касание, requiresCorrectToolForDrops, MapColor.STONE, подсказка «DO NOT TOUCH, BREATHE OR STARE AT.»; падающий taint выпадает предметом (ванильный FallingBlockEntity) |
| statue_elb_f | ItemBlock, без вкладки, NEI-hide, func_147480_a(..., true); в euphemium_kit | без предмета, ключ ломал без дропа | предмет (иконка code), дроп себя, ключ ломает с дропом, добавлен в euphemium_kit; имя «tile.#undef.name» (в оригинале нет перевода) |
| part_emitter | TargetPoint 150 (gasfire — от частицы, tower — от угла блока) | sendParticles, 32 блока | рассылка игрокам измерения в радиусе 150, пакет с overrideLimiter |
| ore_copper | BlockGeneric(rock) 5/10, blockTab, 12 жил × 6, Y 5..49, smelt → ingot_copper 2.5 XP, oreCopper | блока не было | блок, текстура, лут, тег c:ores/copper, плавка в ванильный слиток (ingot_copper порта = minecraft:copper_ingot), шредер ×2 powder_copper, фича + biome modifier; порядок вкладки niter → copper → tungsten |
| gun_kit_1 / gun_kit_2 | ItemRepairKit(10/100): хотбар, −25 % макс. прочности износа, звуки spray/repair | не зарегистрированы | предметы, текстуры, звуки, рецепты (any_rubber + 1000 мБ woodoil + iron; gun_kit_1 + wrench + ducttape + 1000 мБ lubricant/bdcl; целые kit/wrench), Bobmazon, лут-пулы |
| ammo_container | ItemAmmoContainer meta 0/1: до 3 случайных оружий, defaultAmmo, meta 1 — половина (ceil), без isDefaultExpensive | не зарегистрирован | предмет, alt-модель (custom_model_data 1), рецепт meta 1 (4 nitra), подсказки, Bobmazon ×16, ящик оружия, ItemGlitch case 8, лут-пулы |

Технические адаптации: max_damage = dura (1.21 ломает предмет при damage == max, 1.7.10 — при damage > dura−1; число использований одинаково); meta 1 контейнера = custom_model_data 1; ShapelessOreRecipe требует damage 0 у gun_kit_1 и wrench — ингредиент neoforge:components damage 0. Сопротивление ore_copper записано буквальным setResistance 10 (общая договорённость порта по ×3/5 открыта).
