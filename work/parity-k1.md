# k1 — экраны выбора рецептов, ИИ голубя/глифидов, Bobmazon, machine_reactor (2026-11-19)

Эталон 936c60c. Диагностика: `event/ParityK1bDiagnostics` (имя `ParityK1Diagnostics` занято), строка `checked_parity_k1b_cases` в BuildingPortDiagnostics.

## 1. Экраны выбора рецептов

Проверка эталона: в `GUIScreenRecipeSelector`/`GuiInfoContainerProcessor` подсказка рецепта — **только текст** (`GUIElements.drawHoveringTextRecipe(recipe.print())`); иконки рисуются в сетке, в слоте выбора, на кнопке машины и полупрозрачными «призраками» в пустых входных слотах (они уже были в порте). Иконок внутри подсказки в эталоне нет — не добавлялись.

| что | эталон | было в порте | стало |
|---|---|---|---|
| экран | один `GUIScreenRecipeSelector` | 11 копий с разной логикой | общий `inventory/gui/LegacyRecipeSelectorScreen<R>`; 11 подклассов задают иконку, имя, описание и пакет выбора |
| стиль подсказки | drawHoveringTextRecipe: фон 0xF0100010, рамка 0xFFFF8000→0xFFFFFF00, отступ после заголовка 6, шаг 10, свой сдвиг у краёв | ванильная подсказка | `LegacyRecipeTooltip.render`; также на кнопках выбора рецепта всех машин (химзавод/фабрика, сборщик/фабрика, тигель, прецизионный, PUREX, дробилка, суперкомпьютер, токамак, плазменная кузня) |
| текст | `GenericRecipe#print`: жёлтое имя (+ «Internal: имя» при LSHIFT), автопереключение золотом, красные «Duration: Xs», «Consumption: N HE/t», **Input:**, «  Nx предмет» серым (вариант руды меняется раз в секунду), «  N mB жидкость [at N PU]» синим, **Output:**, ChanceOutput «Nx … (X%)», ChanceOutputMulti «One of:» | свои форматы, мощность/время в тиках, «x N», без процентов/давления | по эталону; тигель — `CrucibleRecipe#print` (материалы, LSHIFT → mB); токамак — `FusionRecipe#print` (KyU/t, TU/t, flux/t); кузня — строка «Plasma Input Energy: N TU/t» |
| имя рецепта | `getLocalizedName`: setNamed → ключ имени; иначе имя иконки; nameWrapper (rock.crushing, purex.recycle, purex.schrab, precass.recycle, com.copy) | имя иконки / ключ группы / «Rock Mill» | `inventory/recipes/LegacyRecipeNames` (сгенерирован из *Recipes.java эталона: 43 setNamed, 88 обёрток); у тигля имя иконки (CrucibleRecipes не setNamed) |
| иконки | getIcon | ass.unpackage — пустой пакет, ass.nitra — выход, PUREX — всегда 1-й ингредиент, переработка прецизионного — выход | ass.unpackage — значок жидкости 32 000 mB, ass.nitra — 1-й ингредиент; PUREX: uzh/flash*/vit* — выход, thoriumsalt — значок жидкости, остальные — 1-й ингредиент; переработка — сломанный предмет; в сетке без числа стака |
| поиск | `matchesSearch`: подстрока локализованного имени | id, группа, ингредиенты, выходы, жидкости | только имя (через JechSearchCompat) |
| сортировка | `recipeOrderedList` (порядок регистрации) | по id (химзавод и тигель уже по эталону) | `LegacyRecipeNames.comparator` для сборщика (+ ass.package/unpackage по Fluids.getInNiceOrder), кузни, токамака, прецизионного (переработка сразу за рецептом), PUREX, дробилки, суперкомпьютера; химфабрика — `ChemicalPlantRecipeOrder`. Изменено и в меню, и в блок-сущностях (индексы выбора совпадают на сервере и клиенте) |
| поведение | поле поиска не в фокусе; ENTER — фокус; клик по полю — фокус по клику; «очистить» без звука; колесо мыши в любом месте; ↑↓ PgUp PgDn Home End; ESC/инвентарь (без фокуса) → предыдущий экран; поиск сбрасывает страницу; закрытие всегда со щелчком | фокус сразу, клик по значку лупы фокусирует, колесо только над GUI, нет клавиш листания | по эталону; «Press ENTER to toggle focus» |

Добавлены ключи эталона во все языки, где они есть в 1.7.10: gui.recipe.consumption/input/output/atPressure/fusionIn/fusionOut/fusionFlux, fus.*, ass.* и chem.* (setNamed), обёртки имён.

Расхождения (технические): пакет выбора отправляется только при изменении (в 1.7.10 — при каждом закрытии, тот же итог); подсказки «Close/Clear search/Press ENTER…» и «Click to set recipe» — ванильный стиль 1.21 (drawCreativeTabHoveringText); слой подсказки z=400.
Нет в порте (не восстанавливалось, предметов/совместимости нет): ass.digimemer (Mekanism), purex.watznaqadah/watznaqadria, rock.sculkgrass/lightsculkgrass — в списке порядка остаются, на экранах не появляются.

## 2. ИИ

- Голубь (`EntityPigeon`, isAIEnabled = true) — новый ИИ, а не старый EntityCreature. Как у остальных мобов нового ИИ (j1): `LegacyPathNavigation(manualFollow=false)` (поиск пути 1.7.10) + `LegacyMoveControl` (EntityMoveHelper). Цели не менялись.
- Черви / BOT Prime (`EntityWormBaseNT`, `EntityBOTPrimeBase`) — своя схема движения в updateEntityActionState/updateMovement, PathFinder не используется → не трогались.
- Глифиды: в эталоне `EntityCreature.updateEntityActionState` вызывает `attackEntity(entityToAttack)` и для high-priority EntityWaypoint (setTarget(taskWaypoint)): EntityMob.attackEntity (attackTime 20, дистанция < 2, перекрытие по Y, видимость) → `EntityGlyphid.attackEntityAsMob` (взмах, урон по waypoint не проходит). В порте атака была только по LivingEntity — теперь `legacyAttackEntity(Entity)` для любой цели, включая waypoint.

## 3. Bobmazon

`GUIScreenBobmazon#updateScreen`: ограничение страницы и `closeScreen()` при скрытом каталоге в основной руке у игрока «SolsticeUnlimitd» — перенесено в `BobmazonScreen#tick` (`LocalPlayer.closeContainer`). Открытие: в эталоне `getOffers(getHeldItem())` — в порте выбирается каталог из руки, которой его используют (в 1.7.10 второй руки нет). Закрытие ESC/инвентарь и знаки-хеши для скрытого каталога уже совпадали.

## 4. machine_reactor

Рабочий реактор-размножитель зарегистрирован под id эталона `machine_reactor` (блок, предмет, блок-сущность, меню, модель, blockstate, лут, тег кирки, вкладка machine). Блок совместимости удалён. `HbmBlocks.MACHINE_REACTOR` (было `MACHINE_REACTOR_BREEDING`). Название en_us по эталону «Breeding Reactor (LEGACY)», переводы tile.machine_reactor.name из 1.7.10 (de/fr/it/ja/pl/pt/uk/zh). Таблицы parity-v4-sounds / parity-e2-gui-slots обновлены; в parity-t3-resistance остаётся поле Java эталона `machine_reactor_breeding` (это имя поля, id уже machine_reactor). Рецептов у блока нет и в эталоне (@Deprecated).

Удалены: `blocks/LegacyReactorCompatibilityBlock.java`; ресурсы `*_breeding` переименованы в `machine_reactor` (старые `blockstates/machine_reactor.json`, `models/block/machine_reactor.json` (cube_all), `models/item/machine_reactor.json`, `loot_table/blocks/machine_reactor.json` блока-заглушки заменены).

## Проверки

`work/verification-parity-k1.log`: весь набор задач BUILD SUCCESSFUL; checked_parity_k1b_cases=34, building errors=0, item errors=0 warnings=16. build.gradle не менялся; recipe-baseline не обновлялся (файлы рецептов не менялись).

## Ручная проверка

1. Химзавод: навести на кнопку рецепта — оранжево-жёлтая рамка, «Duration: 5.0s», «Consumption: 250HE/t», «Input:», «  1x Sugar», жидкости синим; с LSHIFT строка «Internal: chem.agar».
2. Открыть выбор рецепта: поле поиска не в фокусе, E закрывает; ENTER — фокус, ввод «steel» — только рецепты с этим словом в имени; ↑/↓/PgDn/End листают; колесо мыши — в любом месте экрана.
3. Сборщик: рецепты в порядке эталона (первые — пластины), распаковка жидкостей — со значком жидкости; токамак — «Deuterium Plasma», три фиолетовые строки.
4. Дробилка: «Crushing of Cobblestone», «One of:» с процентами; PUREX: «Reprocessing of …».
5. Глифид с high-priority waypoint рядом машет лапой каждую секунду.
6. Голубь бродит и ест хлеб как раньше.
7. Реактор-размножитель в вкладке машин, называется «Breeding Reactor (LEGACY)», ставится/работает/дропается.
