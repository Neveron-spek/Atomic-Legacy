# Parity R2 — логистика (система 13), 2026-11-16

Эталон: HbmMods 1.7.10 `936c60c`. Статусы: И — исправлено/перенесено, С — сверено без расхождений, Н — не перенесено/не сверено (причина в примечании).

| Объект / подсистема | Источник 1.7.10 | Статус | Что сверено и изменено |
|---|---|---|---|
| conveyor, _express, _double, _triple, _chute, _lift | BlockConveyor*, EntityMovingItem/Package | И | Прочность 2/2 (было 5/10), Material.iron (нужен инструмент), звук камня; скорость 0.0625/тик, express ×3, chute ×5/×3 — совпадают (диагностика: 16 тиков = 1.0 блока) |
| machine_conveyor_press | MachineConveyorPress | С | 5/10 |
| crane_extractor/inserter/grabber/router/boxer/unboxer | TileEntityCrane*, GUICrane*, ContainerCrane* | И | GUI заменён на оригинальные текстуры gui_crane_*.png, кнопки/подсказки/надписи 1.7.10 (был придуманный «asset-independent» GUI с текстом направлений); названия container.crane*; задержки 20/10/5/2, количества 1/4/16/64, раскладка слотов — совпадают; звук камня |
| crane_router, crane_partitioner | CraneRouter, CranePartitioner (не IToolable) | И | Отвёртка больше ничего не делает (было: настройка направлений/сообщение) |
| crane_splitter | CraneSplitter.onScrew/printHook | И | Убрано придуманное сообщение в чат; соотношение показывается обзором «Splitter ratio: L:R» |
| Все краны (отвёртка) | BlockCraneBase.onScrew | И | Убраны придуманные сообщения о направлениях |
| Фильтры кранов/счётчика/трубы | ModulePatternMatcher | С* | Режимы exact→wildcard→теги→exact; режима bedrock нет (нет ItemBedrockOreNew-эквивалента), «exact» сравнивает компоненты — принятая ранее адаптация |
| pneumatic_tube | PneumoTube, TileEntityPneumoTube, PneumaticNetwork, GUIPneumoTube | И (перенесено) | В порте была только соединительная труба. Перенесены: компрессор/конечная точка (отвёртка, приседание — выход), отправка 64 «массы», порядок слотов (первый/последний/случайно), получателей (по кругу/случайно), 5 попыток, дальность по давлению 10/25/100/250/1000, 50 мБ воздуха за отправку, бак 4000 мБ, приём ≤clamp((4000-fill)/25,1,100)/тик, 15 фильтров + белый/чёрный список, инверсия редстоуна, звук tubeFwoomp (0.25, 0.9–1.1, задержка 20), GUI gui_pneumatic_pipe/endpoint, модель с коннекторами in/out/air, формы выделения и столкновения; прочность 2/10, звук трубы |
| pneumatic_tube_paintable | PneumoTubePaintableBlock | Ч | Механика трубы общая; полноблочный вид, покраска блоком, сверло и дефьюзер не перенесены |
| pneumatic_storage_access/clutter/mono/importer/exporter | PneumoStorage* | И/Н | Прочность 5/10 (было 0.1), звук трубы. Логика хранилищ (StackCache) не сверялась |
| pipe_anchor_pneumatic | PneumoPipeAnchor | Н | Нужна общая система якорей для пневмосети (порт-якорь привязан к жидкостной сети) |
| drone_waypoint, drone_waypoint_request | DroneWaypoint* | И | Прочность 0.1 (было 5), Material.circuits (без инструмента); обзор «Waypoint distance / Next waypoint» |
| drone_crate, drone_dock, drone_crate_provider/requester | DroneCrate/DroneDock, TileEntityDrone* | И | Звук камня; drone_dock — hbm:block.storageOpen 2.0 (был ванильный сундук); provider сверен (63 строки оригинала, не упрощён); обзор drone_crate |
| Дроны | EntityDeliveryDrone, EntityRequestDrone, EntityDroneBase | И | Скорость request-дрона 0.625 (была 0.375); цель (x+0.5, y, z+0.5) (была y+1); движение одним moveEntity с +1 по Y при горизонтальном столкновении |
| radio_torch_sender/receiver/logic/counter/reader/controller | RadioTorch*, TileEntityRadioTorch*, GUIScreenRadioTorch*, GUICounterTorch | И | Перенесены GUI отправителя/приёмника/логического приёмника/счётчика (были чат-сообщения и придуманная привязка через пейджер); логика тиков по оригиналу (getIndirectPowerLevelTo, polling, сброс в 0, selfdestruct = ExplosionVNT 5 с CrossSmooth(1,50), piercing 5/0.5); счётчик — фильтры с режимами и forceUpdate; чтец без придуманного «аналогового» режима; canBlockStay оригинала; Material.circuits; обзоры Freq/Signal |
| radio_torch_relay | RadioTorchRelay | И (перенесено) | Блок без функции (в оригинале //TODO), обзор, рендер rtty_relay; рецепта и перевода в оригинале нет |
| RTTYSystem тестовый канал «2012-08-06» | RTTYSystem.getTestSender | Н | Нужен NoteBuilder-формат телекса |
| mass_storage (4 вида) | TileEntityMassStorage, BlockMassStorage, GUIMassStorage | И | quickExtract только полным стеком и при включённом выводе; кнопки «Provide» кладут в выходной слот (было — в инвентарь игрока и только при выводе); слот фильтра — призрачный; быстрое перекладывание из выхода; звуки storageOpen/Close 0.5; при взрыве выпадают слоты 0 и 2, блок и запас теряются; RTTY type/fill/fillpercent/toggleoutput; подсказки GUI вместо придуманной надписи |
| rail_wood, rail_highspeed, rail_booster | RailGeneric, RailBooster | И (перенесено) | Скорость 0.2/1.0/1.0, ускоритель ×1.15, isFlexibleRail всегда true, рецепты 16/16/6, подсказки, Material.circuits, теги rails и mineable |
| rail_narrow | RailGeneric | И | Material.circuits, без столкновения, звук камня |
| rail_narrow_curve, rail_large_* (9), вагонетки (5), cart, train, entity_ntm_cargo_tram(_trailer), клавиша trainInv | BlockRailWaypointSystem, EntityMinecart*, EntityRailCarBase, TrainCargoTram* | Н | Не перенесено в этой партии (≈5500 строк оригинала, отдельная подсистема) |
| LegacyBuildingEntityRenderer, LegacyTorpedoEntityRenderer | RenderBoxcar (ignoreFrustumCheck) | И | Отсечение по радиусу OBJ через EntityRenderBounds (проверка validateEntityRendererCullingBounds) |
