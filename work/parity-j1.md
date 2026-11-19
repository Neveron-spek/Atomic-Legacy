# J1 (2026-11-19, второй проход буквы J) — ИИ мобов, polaroidID, рецепты 1↔9

Эталон 936c60c. Диагностика: `event/ParityJ1bDiagnostics` (имя `ParityJ1Diagnostics` уже занято партией J1 «радиация» 2026-11-14), строка `checked_parity_j1b_cases` в BuildingPortDiagnostics.

## 1. ИИ мобов

Новые классы `entity/ai`: `LegacyPathFinder` (A* 1.7.10 PathFinder), `LegacyPathNavigation`, `LegacyMoveControl` (EntityMoveHelper), `LegacyCreatureAi` (старый ИИ EntityCreature/EntityLiving).

| что | 1.7.10 | было в порте | стало |
|---|---|---|---|
| поиск пути (все перечисленные мобы) | PathFinder: одна цель floor(x−w/2), только 4 горизонтальных соседа, узел принимается при расстоянии до ЦЕЛИ < maxDist, стоимость/эвристика — квадраты расстояний, без штрафов; частичный путь к ближайшему узлу, null если это старт; ChunkCache maxDist+8 (PathFinderUtils +16) | ванильный A* 1.21 (диагонали, лимит длины от старта, штрафы) | `LegacyPathFinder` поверх WalkNodeEvaluator 1.21 (проходимость узлов из 1.21) |
| PathFinderUtils | точка maxDist в сторону цели, опущенная на опору (10 вниз, иначе 10 вверх: не blocksMovement над isNormalCube) | ванильный moveTo к цели | `partialPathToEntity/Coord`; флаг воды (isPathingInWater = вода непроходима) — временный штраф WATER/WATER_BORDER −1 |
| глифиды, RAD beast, личинка (parasite maggot), призрак — старый ИИ | EntityCreature.updateEntityActionState: путь к цели при отсутствии или 1/20 (16 бл.); блуждание (нет пути 1/180, или 1/120, или fleeingTick, entityAge < 100; 10 точек ±6/±3, getBlockPathWeight, путь 10 бл.); следование: пропуск узлов ближе width·2 по горизонтали, поворот ≤ 30°/т, moveForward = атрибут, при hasAttacked стрейф (RAD beast), прыжок при подъёме/упоре/в воде 0.8, faceEntity(цель, 30, 30); 1/100 за тик — ветка EntityLiving (стоп, взгляд на игрока в 8 бл. / случайный поворот) и сброс пути; rotationYawHead = rotationYaw | ванильные цели FloatGoal/RandomStrollGoal/MeleeAttackGoal/RandomLookAroundGoal, MoveControl 90°/т | `LegacyCreatureAi` в customServerAiStep; цели движения удалены (остались только цели выбора цели у глифидов); навигация без ванильного следования, MoveControl/LookControl отключены |
| глифиды | EntityMob.attackEntity: attackTime 20, дистанция < 2, пересечение по Y, видимость | MeleeAttackGoal (сам вёл к цели) | прямой порт в customServerAiStep |
| глифиды, extended targeting | при !hasPath — PathFinderUtils к цели (16, вода непроходима) | только для waypoint | для любой цели (entityToAttack) |
| глифиды, задачи | при !hasPath — PathFinderUtils к taskX/Y/Z, maxDist = (int)(dist·1.2), вода непроходима; рытьё — то же | ванильный moveTo | `partialPathToCoord`; пауза 10 т после неудачного поиска сохранена (тех. адаптация, в 1.7.10 — каждый тик) |
| глифиды, бегство при слепоте | fleeingTick 80 → блуждание каждый тик | отдельная реализация | общий `repath` (fleeingTick) |
| FBI | updateAITasks: каждый тик setPath(PathFinderUtils к цели, 16, вода проходима) | ванильный moveTo | `partialPathToEntity` |
| новый ИИ: FBI, MaskMan, UndeadSoldier, Cyber/Tesla/Taint crab, BlockSpider | EntityMoveHelper: moveForward = 0 каждый тик; поворот ≤ 30°/т; скорость = speed·movementSpeed; прыжок, если точка выше floor(minY+0.5) и ближе 1 бл. по горизонтали; путь — PathFinder 1.7.10 с followRange | MoveControl 1.21 (90°/т, свои правила прыжка), ванильный A* | `LegacyMoveControl` + `LegacyPathNavigation` (следование по пути — ванильное PathNavigation) |

Расхождения (вынужденные): проходимость/высота узлов, старт пути и прыжки через блоки — от WalkNodeEvaluator 1.21; предохранитель 8192 узла на поиск (в 1.7.10 без лимита); entityAge ≈ noActionTime 1.21 (сбрасывается так же — игрок ближе 32 бл.); isJumping не «залипает» в ветке простоя (JumpControl 1.21 сбрасывает его каждый тик); у глифидов high-priority waypoint как entityToAttack только ведёт путь, удара/взмаха по нему нет (как и раньше в порте); EntityMoveHelper для новых мобов применён к движению по пути, режим STRAFE (нет в 1.7.10) — ванильный. Не тронуты: голубь, черви (свои схемы движения), утки/кряквы/криперы (ванильные базовые классы). Следование по пути у новых мобов — ванильное PathNavigation (в 1.7.10 PathNavigate.pathFollow — близкая логика).

## 2. MainRegistry.polaroidID

`AtomicLegacy.polaroidID`: на JVM-сессию, rand(18)+1 без 4 и 9 (generalOverride = 0, в 1.7.10 конфигом не задаётся). Перенесены все 35 использований эталона, кроме плащей:

- Текстуры: `polaroid_N` / `glitch_N` — модели `item/polaroid_1..18`, `glitch_1..18` и overrides по свойству `atomiclegacy:polaroid_id` = id/32 (`client/PolaroidClient`).
- Подсказки: ItemPolaroid (18 вариантов), ItemGlitch (пустая строка + 18 вариантов), GunB92 (11 и 18), bottle_sparkle / bottle_rad, canteen_vodka, shimmer_sledge / shimmer_axe, ams_core_thingy (строка и блеск только при 11), ItemCustomLore `.desc.P11` (9 ключей; добавлены в en_us/it/ja/pl/pt/ru/uk/zh_cn из lang эталона).
- book_secret во вкладке расходников только при 11; JEI скрывает book_secret, book_of_, burnt_bark, ams_core_thingy только при ≠ 11 (NEIConfig).
- Радар (GUIMachineRadarNTSlots): заголовок «Reda» при 11.
- Каталитический риформер: при 11 вращающаяся аликорн-лошадь (HorsePronter, текстура `models/horse/dyx`, рог и крылья, сигарета) — модели `block/trinkets/horse_dyx_*`, текстура скопирована из эталона.
- Balefire-гриб при 11 (иначе 1/100): мини-нюк/ExplosionNukeSmall, мины, разбившиеся бомбы, катапульта (muke и tinytot), гранаты, ядерный глифид.
- FEL в режиме DRX: digamma_matter вместо fire_digamma при 11.
- Не перенесено: RenderAccessoryUtility (плащи CapeHbm2 / CapeBlaze2) — плащей в порте нет, это область j2 («именная косметика»); значение доступно как `AtomicLegacy.polaroidID`.

## 3. Рецепты 1↔9

169 рецептов 1→9 (MineralRecipes: add1To9Pair/add1To9/add1To9SameMeta/addMineralSet и прямые addRecipe `"#"`) переведены из crafting_shapeless в crafting_shaped с формой `"#"` (ключ и количество прежние); список — сверка `work/parity_e3_recipes.py pattern` (после смены mod ID скрипт запускался копией с `atomiclegacy`). 9→1 уже были shaped `###/###/###`. После правки сверка: pattern 2 (reinforced_glass_pane/laminate_pane — пустой верхний ряд, в 1.21.1 непредставим), kind-расхождений 0. Ингредиенты не менялись (у части блоков в порте тег `c:storage_blocks/*` вместо точного предмета — прежнее решение, вне задачи). updateRecipeBaseline выполнен.

Попутно: `crystal_charred` регистрировался как простой Item (без `.desc`), теперь ItemCustomLore как в эталоне (`LegacyLoreItem`).

## Проверки

`work/verification-parity-j1.log`: assemble, validateJarIntegrity, validateLangFiles, validateRecipeJsonSchemas, validateRecipeSemantics, validateRegistrySources, validateRecipeBaseline, validateGlyphidSpawnSafety, validateEntityRendererCullingBounds, checkBuildingAudit, checkItemAudit — успешно; оба аудита errors=0; checked_parity_j1b_cases=98. build.gradle не менялся. Клиент не запускался.
