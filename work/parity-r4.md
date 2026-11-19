# Сверка R4 — декоративные блок-сущности и разное (2026-11-16)

Эталон: HbmMods 1.7.10 `936c60c`. Список по `TileMappings`/`ModBlocks`: объекты с TileEntity или особым поведением. В этой версии оригинала нет торговых автоматов, телевизоров/голограмм, часов, флагов, знаков/плакатов, клавиатур; из статуй есть только `statue_elb_f`; creative-блоки (`machine_stirling_creative`, `fusion_klystron_creative`, `battery_creative`) относятся к системам энергии/реакторов и здесь не сверялись.

| Объект | Итог | Расхождения → исправление |
|---|---|---|
| crate_iron / crate_steel / crate_desh / crate_tungsten | исправлено | прочность 5/6, 5/6, 5/6, 7.5/180 (было 5/10, 5/20, 7.5/300, 15/10000); цвета заголовка GUI 4210752, у вольфрамового всегда белый (было выдуманные 0x1C1C1C/0x3F1515/0xA0A0A0/0xFFCA53); у деш-ящика подпись инвентаря `ySize-96+3`; подсказка «Contains: / - X xN / ...and N more.», «This container is locked.», «It feels heavy/empty...», «Skittering emanates from within...» вместо придуманной «slots used»; переименование (имя в заголовке, в дропе, надпись при наведении); звуки открытия/закрытия crateOpen/crateClose; дроп: ломание игроком с инструментом → предмет с содержимым, замком и `stacklock`; в творческом — ничего; без игрока (взрыв) — содержимое рассыпается пачками 10–30, сам ящик пропадает; предупреждение 6 кБ; пустые ящики стакаются до 64; открытие ящика в руке (ПКМ, ставится только с Shift; запертый — при ключе с теми же штифтами в инвентаре; слот с открытым ящиком заблокирован); замок и key_kit применяются к ящику, а не открывают его; компаратор; вставка в запертый ящик запрещена; shift-клик по ContainerBase без перекладывания инвентарь↔хотбар |
| safe | исправлено | прочность 7.5/6000 (было 7.5/10000); предмет ItemBlockStorageCrate (содержимое, подсказка, открытие в руке, стак 64); общий с ящиками формат данных; рецепт-улучшение копирует тег и имя (раньше конвертировал в другой формат) |
| crate / crate_weapon / crate_lead / crate_metal / crate_red | исправлено | weaponList — 8 пушек оригинала вместо выдуманного TACZ-пула; redList дополнен lilmac, autoshotgun_sexy, maresleg_broken и тремя ammo_secret *_equestrian (14 предметов); звук crateBreak в углу блока |
| crate_ammo | исправлено | 14 бросков патронов оригинала (было 5), выпадение как dropBlockAsItem |
| crate_can | исправлено | выпадение как dropBlockAsItem |
| crate_jungle | сверено | — |
| crate_supply | исправлено | без игрока (взрыв) выпадает сам ящик; вскрытие ломом с эффектом разрушения и dropBlockAsItem |
| filing_cabinet | исправлено | содержимое рассыпается как BlockDecoContainer; замок/key_kit применяются к шкафу |
| bobblehead / snowglobe / plushie | сверено | типы, тексты, звуки, дроп совпадают |
| deco_computer / deco_crt / deco_toaster / tape_recorder / steel_poles / pole_top / pole_satellite_receiver | сверено | формы, ориентация, рецепты, разборка на наковальне |
| boxcar | исправлено | был кубом с текстурой; теперь OBJ-модель boxcar с поворотом по метаданным RenderDecoBlock, вариант «метаданные 0» (лежащий вагон от EntityBoxcar) |
| boat, steel_wall/corner/roof/beam | сверено ранее (строительные) | — |
| statue_elb_f | частично | поведение красного ключа совпадает; в порте нет предмета блока (дроп при ломании ключом невозможен) — открытый вопрос |
| deco_emitter | исправлено | ориентация по BlockPistonBase.determineOrientation; цвет красителя из таблицы 1.7.10 (firework colours); краситель тратится и в творческом; эффект 4 (частицы plasmablast вдоль луча) |
| part_emitter | сверено | радиус рассылки частиц — стандартный ванильный (32), в оригинале 150 |
| lantern | исправлено | ослепление глифидов (было заглушкой «pending») |
| lamp_demon, floodlight, spotlight_* | сверено | константы и поведение совпадают |
| deco_loot | исправлено | исчезает без частиц/дропа (setBlockToAir), сам не выпадает |
| spikes | исправлено | тип урона spikes (обходит броню, «%1$s got impaled.»), звук slicer |
| hev_battery (блок `hev_battery_block`) | исправлено | требуется шлем ArmorFSBPowered, расходуется всегда, звук у игрока, без частиц; вкладка блока machine, предмета hev_battery — control |
| book_guide, brick_jungle_trap, block_meteor_treasure, brick_concrete_marked, stone_keyhole, brick_forgotten_lock | сверено выборочно | — |
| event_tester, obj_tester | не перенесены | отладочные, скрытые |

Ресурсы: GUI-текстуры ящиков/сейфа/шкафа, текстуры брелоков, звуки — побайтно совпадают; OBJ отличаются только адаптацией для загрузчика NeoForge (mtllib/usemtl, o→g). Новые ресурсы: `textures/block/boxcar_model.png` (копия textures/models/boxcar.png), `models/obj/blocks/boxcar_block*.obj` (вершины boxcar.obj со сдвигом/поворотом рендера). Переводы названий ящиков, сейфа, контейнеров и декора добавлены из .lang оригинала (de/fr/it/ja/pl/pt_br/ru/uk).
