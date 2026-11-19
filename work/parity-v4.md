# Parity V4 — мелкие расхождения (2026-11-17)

| # | Пункт | Оригинал (936c60c4) | Было в порте | Сделано |
|---|-------|---------------------|--------------|---------|
| 1 | ICF: форма | MachineICF.getAllDimensions: тело {5,0,1,1,8,8} + крылья {1,1,-1,2,8,8} и {1,1,2,-1,8,8} на y+3 | только тело 17×6×3 | IcfCoreBlock.FOOTPRINT + крылья x −8..8, y 2..4, z ±2 (408 клеток) |
| 1 | ICF: порты | TileEntityICF.getPorts: верх, низ (ядро), 4 боковых dir·±2 ± rot·6, y+3 | боковые на ±3 (вне машины) | боковые на ±2 (в крыльях), направления SOUTH/NORTH как раньше |
| 1 | ICF: прокси | makeExtra(getAllPorts) = TileEntityProxyCombo().inventory().fluid(); ядро — ISidedInventory | любая клетка — insert-only инвентарь, у ядра инвентаря нет | инвентарь только у 5 портов (SidedInvWrapper) и ядра; прочие клетки без инвентаря |
| 1 | ICF: установка | BlockDummyable, getOffset() = 1: ядро на клик + dir·1 | ядро на месте клика | ядро сдвигается на 1 от игрока (как LargeFluidTankBlock) |
| 1 | ICF: сборка/разборка | TileEntityICFStruct заменяет компоненты (вкл. крылья) клетками | компоненты крыльев оставались блоками внутри модели | крылья заполняются клетками; разборка удаляет все клетки |
| 1 | ICF: рендер | RenderICF рисует модель целиком | модель уже с крыльями, границы по OBJ | без изменений |
| 2 | PUREX watznaqadah/watznaqadria | регистрируются только если OreDictionary "nuggetNaquadria" не пуст (HBM его не регистрирует) | отсутствуют | PurexNaquadriaRecipes: при непустом теге c:nuggets/naquadria добавляются оба рецепта (первый элемент тега ×12); без тега — нет, как в оригинале. Остальные 57 — полный набор (Q2) |
| 3 | Луч MK5 | collectTip хранит lastPos; processChunk стирает i < \|lastPos − pos\| (lastPos во float); центр (int)posX | стирал все собранные блоки, включая конец; центр double | конечный блок луча стирается только если float-длина > его шага; центр лучей (int) |
| 4 | Звуки блоков | Block(): soundTypeStone независимо от Material; BlockLeaves grass, BlockLog wood, BlockStairs/BlockWall — звук родителя, NTMAnvil anvil, BlockGraphiteDrilledBase metal; затем setStepSound | машины metal(), block_insulator metal и др. | таблица work/parity_v4_sounds.py → parity-v4-sounds.json (994 id), LegacyStepSound применяет при регистрации; block_insulator → WOOL; flesh (glyphid_base/spawner) перенесён со звуками; клетки мультиблоков звучат как ядро |
| 5 | waste_leaves / leaves_layer | ItemShears: Material.leaves → 15 | ножницы 1 | getDestroyProgress: ножницы 15 (без тега leaves, мотыга не ускоряется) |
| 5 | rail_wood | RailGeneric("axe", 0): кирка 1, топор — скорость топора | кирка быстрая через #rails | getDestroyProgress: не-топор = 1; тег rails оставлен |
| 6 | Радиусы частиц/звуков | part_emitter 150 | закрыто в S4 | открытых пунктов в STATE нет |

Соответствие звуков 1.7.10 → 1.21: stone/piston → STONE, metal → METAL, cloth → WOOL, wood, gravel, grass, glass, sand, snow, ladder, anvil — одноимённые; grate/pipe/platemetal/flesh — HbmSoundTypes. Порт-only блоки (без оригинала) и deepslate-двойники руд не меняются; порт-only ступени берут звук родителя.
Список изменённых звуков: run-building-audit/parity-v4-sounds.tsv (столбец changed).
