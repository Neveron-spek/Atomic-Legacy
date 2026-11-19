# Parity j2 — рендер (2026-11-19)

Эталон: HbmMods 1.7.10 936c60c (`work/original`). Область: блеск брони, именная косметика, B92 от первого лица, UV жезлов.

## 1. Блеск чар на OBJ-броне
- 1.7.10 `RendererLivingEntity`: `shouldRenderPass == 15` (зачарованный предмет) повторяет проход модели с glint.
- `render/LegacyFsbArmorLayer` (все костюмы FSB, no9, hat, ash glasses): после группы — та же группа в `RenderType.armorEntityGlint()` при `stack.hasFoil()`, для CUTOUT/BLEND/GLOW. Нетекстурированные лампы (envsuit Lamps, no9 Flame) без блеска.
- `render/T51ArmorLayer`: то же для групп T-51.
- `LegacyAccessoryArmorLayer`: шлемы (gas mask, M65, goggles) уже рисовали блеск; моды брони/реактивные ранцы в 1.7.10 рисовались из SetArmorModel вне прохода — блеска не было, не добавлен.
- Расхождение (как в z1): в 1.7.10 проход блеска вызывал `render()` модели, который перепривязывал текстуру брони, т.е. фактически рисовал аддитивную фиолетовую копию текстуры брони; в порте — штатная текстура блеска. Согласовано постановкой задачи.

## 2. Именная косметика (RenderAccessoryUtility / ModEventHandlerClient)
- `util/ShadyUtil` — таблица UUID оригинала (все 30 + contributors), `capeFor` = `getCloakFromPlayer` (тот же порядок проверок, `Player*` → CapeTest), `accessoryFor` = условия `onRenderArmorEvent`.
- Плащи: `mixin/client/AbstractClientPlayerCapeMixin` подменяет `capeTexture` в `getSkin()` (аналог `func_152121_a(Type.CAPE, …)`). 27 текстур скопированы побайтно (cmp) в `textures/models/capes/<имя в нижнем регистре>.png` (CapeHbm3 → capehbm3 и т.д.; неиспользуемые CapeHidden/Template/Unknown/Hoboy/_mk1/_mk2/Schrabby не копировались).
- Крылья ModelArmorWings(2) wings_bob (HbMinecraft), (3) wings_black (the_NCR), ModelArmorWingsPheo (Barnaby99_x / «pheo7»: группа Wings + 8 аддитивных конусов-пламён в корне модели, наклон 28.6479° при приседании), ModelArmorTailPeep (LePeeperSauvage) — в `LegacyAccessoryArmorLayer`, только без нагрудника и без эффекта невидимости, по UUID или нику. Ресурсы уже были и совпадают побайтно.
- `RenderAccessoryUtility` (RenderPlayerEvent.Pre): невидимость с уровнем > 0 скрывает игрока целиком; ник `martmn` — вместо игрока билборд `textures/particle/fart.png` (скопирован), 2×2, на 1.25 над ногами, поворот −yaw/pitch смотрящего поверх камеры (эквивалентно GL-цепочке 1.7.10), без освещения.
- Расхождения: `MainRegistry.polaroidID` в этой копии нет (делает j1) — `RenderAccessoryUtility.polaroid11()` всегда false, т.е. плащи CapeHbm2/CapeBlaze2 не выбираются; после слияния с j1 подключить. Билборд martmn в 1.7.10 в виде от третьего лица смещался на расстояние камеры (позиция считалась от игрока, а не камеры) — в порте всегда на игроке. Ник = `GameProfile.getName()` (1.7.10 getDisplayName без NameFormat). Elytra 1.21 берёт текстуру плаща, если своей нет (ванильное поведение).

## 3. B92 от первого лица
- `ItemRenderGunAnim.applyFirstPersonHand` переписан построчно по `ItemRenderer.renderItemInFirstPerson` 1.7.10: при использовании пропускается только трансляция взмаха, а повороты взмаха (`getSwingProgress`) и масштаб 0.4 применяются всегда (раньше обнулялся весь взмах); цепочка лука без изменений значений; проверка использования — по руке (`getUsedItemHand`); для левой руки зеркалятся x и повороты Y/Z, как в LegacyIItemRenderer.
- Рендер модели (EQUIPPED_FIRST_PERSON / EQUIPPED / ENTITY) сверен с `ItemRenderGunAnim` эталона — совпадает. Третье лицо: isFull3D=false, поза лука — UseAnim.BOW.

## 4. UV жезлов (wand_jigsaw/tandem/loot/logic)
- Сверено с `RenderBlockSideRotation`, `IBlockSideRotation`, `BlockWand*#getRotationFromSide`: соответствие граней uvRotate* (источник {0,1,3,2,5,4}), `rotationFromSide`, `topToBottom`, метаданные loot/logic по повороту — совпадают; расхождений не найдено, правок нет. Нижняя грань `legacyUv` сверена с RenderBlocks по памяти (исходника ванильного 1.7.10 в среде нет) — совпадает.
- Инвентарь: getIcon(side, 0) без поворота, как renderInventoryBlock.

## Проверки
- `event/ParityJ2Diagnostics` (строка `checked_parity_j2_cases`): таблица плащей по 27 UUID, варианты polaroid 11, `Player*`, отсутствие плаща, 8 условий аксессуаров, наличие 27 плащей и 6 текстур, OBJ-группы, таблица граней жезлов и выборочные значения.
- Сборка: см. work/STATE.md (запись j2).

## Ручная проверка
1. Зачаровать костюм FSB (напр. HEV) и T-51 — фиолетовый блеск поверх всех частей; лампы без блеска.
2. В dev-клиенте (ник Player###) — плащ CapeTest; ник «HbMinecraft»/«the_NCR»/«pheo7»/«LePeeperSauvage» (offline) без нагрудника — крылья bob / black (взмахи в воздухе, сложены на земле), топор-ранец с пламенем, хвост; с нагрудником или невидимостью — нет.
3. Ник martmn — вместо модели квадрат fart.png, повернутый к смотрящему.
4. B92: натяжение ПКМ от первого лица — плавный отвод и дрожание как у лука 1.7.10; удар во время натяжения — повороты взмаха сохраняются.
5. Жезлы jigsaw/tandem во всех 6 направлениях, loot/logic во 4 — стрелки на гранях как в 1.7.10.
