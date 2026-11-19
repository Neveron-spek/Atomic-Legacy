# l2: все проверки build.gradle, логи игры, старый ID (2026-11-19)

Итоговый прогон: `assemble check` + 54 задачи validate*/check* (кроме трёх LatestUpstream*) — BUILD SUCCESSFUL (work/verification-l2e.log); runNukeAudit — errors=0 (work/verification-l2-nuke.log). Building audit: 206 семейств / 737 записей; item audit: errors=0, warnings=13; 4 runtime recipe audit: COMPLETE.

## Проверки

| задача | результат | причина | исправлено |
|---|---|---|---|
| validateBlockDestroyParticles | падала → проходит | правило устарело: модели батарей/дифурнасов (слияние сегодня) используют `minecraft:block/orientable_with_bottom`, не внесённый в список ванильных родителей | build.gradle: добавлен родитель с particle `#front` (сверено с моделью клиента 1.21.1) |
| validateCraneRecipeParity | падала → проходит | правило устарело: в 1.7.10 `Blocks.stonebrick` — ингредиент с wildcard-метаданными (CraftingManager/ShapedOreRecipe), т.е. любой вариант каменного кирпича; рецепты (4 варианта) верны | build.gradle: ожидаемый ингредиент — список из 4 вариантов |
| validatePortRegressionFixes | падала → проходит | правило устарело: экран гидроочистителя после parity e2 рисует белый заголовок (GUIMachineHydrotreater: `0xffffff`) и подсказки баков через HbmFluidTooltipRenderer (как ещё 21 экран) | build.gradle: правило требует белый заголовок y=5, серую надпись инвентаря x=8 и HbmFluidTooltipRenderer |
| validateRuntimeRecipes (сервер падал) | падала → проходит | RecipeDiagnostics ждал 12 рецептов роторной печи; слияние добавило 13-й (TiCl4 + флюс → 4 слитка титана, 40 тиков, 100 пара), и в RotaryFurnaceRecipes 1.7.10 их 13 | RecipeDiagnostics: 13; machine-work-recipe-counts.tsv: 13 |
| validateMachineRecipeModeMatrix | падала → проходит | (1) ждала файл `…schrabidium_ingot_dineutronium.json`; в 1.7.10 — `SBD.ingot()` = Schrabidate, файл и тег порта верны; (2) ждала 90 групп expensive в сборочной машине, в AssemblyMachineRecipes 1.7.10 их 88 (= порт) | scripts/audit-machine-recipe-mode-matrix.ps1 |
| validateMachineRecipeCoverage | падала (130 проблем) → проходит | (1) путь `com/atomiclegacy/blockentity` (такого пакета нет, BE в `tileentity`) — найдено 1 семейство из 45; (2) теги `[parity T1/V2/P2]` в комментариях списков принимались за константы; (3) 4 строки контракта для блоков, убранных из категорий; (4) 29 восстановленных блоков без рецепта в 1.7.10 не задокументированы; (5) счётчики рецептов заморожены до восстановлений | скрипт: tileentity, вырезание `//`-комментариев, контракт 0 допускает пустое семейство, вывод ожидаемого значения; BLAST_FURNACE_FUEL — допущенное неиспользуемое семейство; контракты: −4 строки, +32 строки (у всех 29 + 3 «_on» проверено: в 1.7.10 рецепта нет); счётчики пересняты, уменьшения сверены: exposure 4, blast_furnace 13 (15 − 2 рецепта на ingotRedstone), crucible 13 (без GT6), breeder 31, fusion_breeder_item 0 (ParityM3) |
| validateItemAcquisition, …528 | падали (29 предметов) → проходят | (1) рецепт шреддера `bedrock_ore → gravel` брал служебный legacy-предмет `bedrock_ore`; в 1.7.10 `ModItems.bedrock_ore` — ItemBedrockOreNew, в порте это `bedrock_ore_new` (**баг**); (2) источники из кода/лута не были перечислены (анфракс из rock mill swamp, combine_scrap с вертолёта, double barrel и selenium steel из лута, меч из хим. установки); (3) у 11 предметов нет источника и в 1.7.10 | recipe/shredder/bedrock_ore_to_gravel.json → `bedrock_ore_new`; ParityK3BDiagnostics так же; dynamic-item-sources.tsv: 5 источников + 11 EXCEPTION с причиной |
| checkBuildingAudit | после п. выше падала → проходит | K3B ждал `R:bedrock_ore` | исправлено вместе с рецептом |
| остальные 46 validate*/check* | проходят | — | — |
| runNukeAudit | errors=0 | — | подготовлен run-nuke-audit (eula, server.properties) |
| validateLatestUpstreamContent, validateLatestUpstreamRecipeExports | не запускались | требуют внешний каталог `..\..\HBM_1.7.10新` | — |
| validateLatestUpstreamMachineRecipes | не исправлялась (31 проблема) | сверяет с другим коммитом e674541, не с эталоном 936c60c: флюиды hydrazine/leuf6/watz_heavy и т.д. в 936c60c отсутствуют, порядок/ID ингредиентов (pa_detector, epress) в порте совпадает с 936c60c, autoswitch пилерод реализован в AssemblyMachineAutoSwitch | — (задача устарела по эталону) |

recipe-baseline: обновлён один хэш (shredder/bedrock_ore_to_gravel.json), 6102 рецепта.

## Регрессии от переименования reinhardtshbm → atomiclegacy

| где | что ломалось | исправлено |
|---|---|---|
| scripts/*.ps1 (Exec-проверки) | `$modId = "reinhardtshbm"`, пути `data/reinhardtshbm/recipe`, `com/reinhardt/hbm/...` — все Exec-аудиты читали несуществующие пути | заменено на atomiclegacy / com/atomiclegacy |
| gradle/verification/*.tsv | ID `reinhardtshbm:*` в контрактах, источниках и счётчиках | заменено |
| tools/*.ps1, tools/*.py (генераторы) | те же пути и ID; rebuild_language_json писал имя вкладки «Reinhardt's HBM» | заменено; имя вкладки «Atomic Legacy» (как в lang) |
| Java/ресурсы | ссылок, ломающих поведение, нет: CustomMachineConfigJSON (миграция старого конфига), `reinhardt_protocol_compat` (ID внешнего мода), комментарий в OBJ | не трогалось |

## Логи игры пользователя (PrismLauncher 1.21.1(2))

| ошибка | причина | исправлено |
|---|---|---|
| `Tried to load invalid item: 'Not a string'` ×2 (генерация мира: Worker-Main, DH World Gen) | в lighthouse.nbt книга записана с SHORT id 386; HbmLegacyNbtTemplate.migrateLegacyItemStack обрабатывал 386 только для INT, стек оставался с числовым id | short и int обрабатываются одинаково (386 → written_book, как в 1.7.10); стек, оставшийся без строкового id, удаляется, а не грузится. Скомпилировано, в игре не проверено |
| краш 14:22 `Not building!` (ItemRenderMaresleg, jar reinhardtshbm) | старая сборка; в текущем коде part() уже берёт consumer заново (`[fix: crash]`) | уже исправлено |
| рельсы rail_wood / rail_highspeed / rail_booster без имени (item audit) | ключи не попали в lang после слияния | добавлены из .lang 1.7.10 во все 10 языков (fr — 2 ключа, как в оригинале); rail_narrow добавлен в 7 языков, где его не было |
| reinhardtshbm-common.toml «not correct», Unknown recipe category, c:plates | логи 13–17.11, исправлено в b3 | — |
| goat_horn missing sound, Sampler2, Xaero, Sable mixin, Can't keep up | ванилла / другие моды / окружение | — |

## Записано (не исправлялось)

- Старые миры с предметами `reinhardtshbm:*` после переименования теряют содержимое мода: алиасов реестра нет (у пользователя миры, похоже, новые). Нужен ли перенос через алиасы — решение пользователя.
- BLAST_FURNACE_FUEL + 13 JSON в recipe/blast_furnace_fuel — мёртвые данные порта (печь берёт топливо через burn module, импорт в BE не используется). Кандидат на удаление после согласования.
- Шреддер `powder_lapis → powder_cobalt_tiny` использует предмет, в 1.7.10 — все члены OreDict dustLapis; тег c:dusts/lapis сейчас содержит только powder_lapis, поведение то же.
- В основном проекте последний коммит «Fallout: crater gets only inert sellafield_slaked» расходится с l2 (здесь runNukeAudit проверяет согласованный активный sellafield). Учесть при слиянии.
