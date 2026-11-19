# Партия S1 — поезда, большие рельсы, вагонетки (2026-11-17)

Эталон: HbmMods 1.7.10 `936c60c4`. Статусы: И — реализовано по оригиналу; А — техническая адаптация (описана); Н — не перенесено.

| Объект | Источник 1.7.10 | Статус | Примечание |
|---|---|---|---|
| rail_narrow_straight, rail_narrow_curve | RailNarrowStraight/Curve (BlockDummyable) | И | 5x1 и 5x5, узкая колея, радиус 4,5; straight рисуется в каждом блоке (как ISBRH); теперь с ItemBlock (как register(block)); заглушка LegacyNarrowStraightRailBlock удалена |
| rail_large_straight, _straight_short | RailStandardStraight/Short | И | 5x2 / 1x2, y+0,1875, смещение колеи 0,5 к доп. колонке |
| rail_large_curve, _curve_7, _curve_9 | RailStandardCurveBase/Wide7/Wide9 | И | радиусы 4/6/8, точные таблицы checkRequirement/fillSpace (Wide7 проверяет 13 клеток базы — как в оригинале) |
| rail_large_ramp | RailStandardRamp | И | второй слой, подъём 1/5, детальные хитбоксы (bounding) из 5 ступеней |
| rail_large_buffer | RailStandardBuffer | И | окно упора с collisionBogieDistance; ошибка оригинала (Z-ветка сравнивает с xCoord) сохранена |
| rail_large_switch, _switch_flipped | BlockRailWaypointSystem + RailStandardSwitch(Flipped) | И/А | RailDef main/side, canCross, переключение кликом; isSwitched хранится свойством `switched` ядра вместо TileEntityRailSwitch (нужно для модели табличек); flipped сохраняет непереставленный checkRequirement оригинала |
| Общее поведение рельсов | BlockDummyable, MultiblockHandlerXR | И | meta 0–15 1:1 (`meta`), findCore, отказ при занятом месте с возвратом предмета, каскадное удаление, 1 предмет при ломании в выживании, 5,0/10,0, Material.iron, звук камня, без вкладки, без рецептов (закомментированы в CraftingManager) |
| Отрисовка рельсов | RenderRail + IRenderBlock.renderWorld/renderInventory | А | baked OBJ (neoforge:obj) по meta с матрицами поворота/сдвига из renderWorld; инвентарь — матрицы renderInventory; в OBJ добавлены только mtllib/usemtl; затенение граней — vanilla shade_quads вместо формулы ObjUtil (близкие значения) |
| entity_ntm_cargo_tram | TrainCargoTram / EntityRailCarElectric / Ridable / Cargo / Base | И/А | все числа оригинала (0,01; 0,95; <0,1; 0,5; 1; 1,5; 2,5; 2,75; 29 слотов; 10×100 HE; слот 28; места водителя/пассажиров, 3 дамми 2×1); синхронизация yaw/pitch через synched data вместо motionX-хака; позиция седока −1,62 (в 1.7.10 posY клиента = глаза) |
| entity_ntm_cargo_tram_trailer | TrainCargoTramTrailer | И | 45 слотов, GUI по клику (заголовок — сырой ключ container.trainTramTrailer, перевода нет в оригинале), ящики в кузове по числу занятых слотов (watcher 10), удар = исчезновение без дропа |
| LogicalTrainUnit, сцепка | EntityRailCarBase | И/А | generateTrain, combineWagons, moveTrainByApproach, getTotalSpeed, collideTrain, coupling_tool (<1 блока, сообщение «Coupled …»); места, где 1.7.10 падал с NPE (null из getRelPosAlongRail в moveWagonTo/moveTrainByApproach), — сход с рельсов вместо краха тика мира |
| entity_ntm_seat_dummy, entity_ntm_bounding_dummy | SeatDummyEntity, BoundingBoxDummyEntity | И | не сохраняются, пересылают удар/клик поезду, рендер пустой |
| updateMotion | ModEventHandler.worldTick (Phase.END) | И | LevelTickEvent.Post на сервере |
| train (cargo_tram, cargo_tram_trailer) | ItemTrain | И | стак 1, скрыт, подсказки Engine/Gauge/…, установка только на IRailNTM той же колеи |
| GUI трамвая/прицепа | GUITrainCargoTram(Trailer) | И | 176×204 / 176×222, текстуры побайтно, полоса заряда и индикатор, белый заголовок |
| Клавиша trainInv | HbmKeybinds.trainKey (R) | И | `key.reinhardtshbm.train_inv`, пакет train_inventory, открывает GUI только водителю трамвая |
| entity_ntm_cart_crate | EntityMinecartCrate + ContainerCrateSteel/GUICrateSteel | И | 54 слота в StorageCrateMenu (STEEL), содержимое в предмете при поломке, взрыв >6000 байт, блокировка ящика в руке |
| entity_ntm_cart_destroyer | EntityMinecartDestroyer + ContainerCartDestroyer/GUICartDestroyer | И/А | 18 фантомных фильтров, каждые 5 тиков, звук mob.zombie.woodbreak, анимация GUI; «damage» = компонент damage + custom_model_data |
| entity_ntm_cart_ore/powder/semtex | EntityMinecartOre/Powder/Semtex | И | без езды и без взрывов (как в оригинале), RenderNeoCart с содержимым |
| cart (12 вариантов) | ItemModMinecart | И/А | тип = компонент cart_type (damage), основа = cart_base («cartBase»), стак 4, вкладка как tabTransport, раздатчик; иконка из двух слоёв |
| Рендер вагонеток | RenderNeoCart, RenderMinecart | И/А | OBJ побайтно; трансформация vanilla minecart + −0,4375 оригинала |
| Рецепты вагонеток | ToolRecipes 157–169 | И | 7 JSON (скрипт work/tmp-s1/gen_s1_recipes.py со сверкой исходника): wood/steel/painted empty, steel destroyer, wood powder, wood semtex, crate (копия NBT, func_92100_c). Затенённые рецепты (painted destroyer, steel/painted powder и semtex) не создаются: ингредиент-вагонетка в 1.7.10 не учитывает NBT, срабатывал только первый |
| Переводы | *.lang | И | 17 ключей ×9 локалей; trailer и narrow_* — только pt_BR (как в оригинале) |
| entity_minecart_test | EntityMinecartTest | Н | отладочная сущность без предмета |
| Красные линии узлов стрелки | ParticleUtil.spawnDroneLine | Н | отладочные частицы дронов в порте отсутствуют |
