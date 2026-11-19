# Parity U1 — сеть жидкостей после T1

Эталон: HbmMods 936c60c (`TilePort`, `TilePortShapes`, `ModulePortManFluidAdaptive/Power`, `UniNodespace`, `BlockNetworkSeparator`, `GUIDiode`).

| Объект / механика | Оригинал | Было | Стало | Статус |
|---|---|---|---|---|
| Узлы портов машин | TilePort: узел на позициях портов, соединения `DirPos`; все порты машины одного типа — один узел (PortDef.combine) | машины — конечные точки по capability рядом с трубами | `HbmFluidNetUser.fluidPorts(type)` + `HbmFluidNetworks.subscribe` каждый тик; union-find труб и узлов машин (checkConnection), узел переиспользует трубу/узел в позиции порта (enableIfMissing) | реализовано, проверено автоматически (U1) |
| Прямое соседство машин | узлы соседних машин сливаются в одну сеть | проталкивание `fillInto` в capability соседа | общая мини-сеть; не-HBM блоки с capability у соединения порта — конечные точки той же сети (интеграции) | проверено (две бочки без труб) |
| Машина между двумя группами труб | одна сеть | две сети, переток только через буфер машины | одна сеть | проверено (бочка в режиме 3 между трубами) |
| Роли портов | in/out отдельные узлы (FlowControlPump getInPort/getOutPort) | — | `PortRole RECEIVE/PROVIDE` | реализовано (flow_control_pump) |
| Переведённые машины (порты = getPorts, баки = getSending/ReceivingTanks) | IFluidStandard*MK2 | собственное проталкивание/вытягивание | турбины large/industrial/chungus/gas, конденсаторы (обычный, силовой, градирни), паровой двигатель, солнечный и тепловой котлы, экстрактор/башня дейтерия, ожижитель, воздухозаборник, крекинг, риформинг, коксование, колонна, гидроочистка, НПЗ, вакуумная перегонка, миксер, буровой лазер, скважина, станок-качалка, гидроразрыв, ore slopper, бочка отходов, Watz, Hephaestus, доменная печь, комбинированная печь, заправщик, ZIRNOX, ICF, PWR (pwr_port), насосы electric/steam, двигатель внутреннего сгорания, дизель, роторная печь (3 группы портов по типу), турбовентилятор, пиропечь, нагреватели (нефтяной, теплообменник, топка, печь), дымовые трубы, колонны RBMK (котёл, нагреватель, охладитель, outgasser, inlet/outlet, порты по наличию rbmk_loader), бочки/баки, компрессор, fluid_pump, flow_control_pump | реализовано; порты/баки 52 блоков проверены автоматически |
| Давление/скорости | vacuum distill oil pressure 2; hydrotreater H2 pressure 1; ZIRNOX 1 000 000 (tilted 0); НПЗ 0 при hasExploded; доменная печь max(fill*50/max, 8); дымовая труба demand 1 000 000 | давление 0 | как в оригинале | проверено автоматически |
| machine_fluidtank порты | 4 порта (dir±rot) | 8 (старый getConPos) | 4 | исправлено |
| fluid_pump | PortDef.make(x,y,z,dir,-dir): одна сеть | две сети | одна сеть | проверено |
| network_separator | BlockDummyable {1,0,0,0,1,1}, in rot(UP)/out rot(DOWN) отдельные узлы, буфер = лимит, урезанный спросом исходящей сети, redstone, GUIDiode, ROR deltatick/deltasecond/setpriority/setlimit, overlay, рецепт SCS/DTD/SCS | отсутствовал | перенесён; `PowerSplitEndpoints`: порты — отдельные конечные точки на позициях dummy, граф питания через машину не проходит | реализовано, проверено автоматически (2 сети, переток, сохранение энергии); вид/GUI — ручная проверка |
| pipe_anchor_pneumatic | PneumoPipeAnchor (узел PNEUMATIC, связь по ключу) | отсутствует | не перенесён | не выполнено (см. ниже) |
| fluid_duct_mk2 | нет (дубликат fluid_duct_neo) | блок+предмет | удалён (текстура `fluid_duct_mk2.png` = `pipe_neo.png` осталась, её использует fluid_duct_neo) | реализовано |
| fluid_duct_solid / _sealed | fluid_duct_solid в ignoreMappings (удалён), sealed — нет | блоки, рецепты, теги, класс предмета | удалены вместе с Kind.SOLID/SOLID_SEALED | реализовано |
| 7 вёдер c:buckets/* | нет | diesel/…/biofuel вёдра + мировые блоки fluid_* | удалены вёдра, блоки и теги c:buckets/* (иных потребителей нет) | реализовано |
| concrete_liquid | создаётся, но `GameRegistry.registerBlock` закомментирован — в игре нет | нет | не переносится (как в оригинале) | проверено (не зарегистрирован) |
| ROR бочек/баков | TileEntityBarrel/MachineFluidTank: VAL:type/fill/fillpercent, FUN:setmode (с fallback) | нет | FluidTankBlockEntity (barrel_*, machine_fluidtank, BAT9000, bigass) | проверено автоматически |

Известные расхождения:
- machine_turbinegas: в порте нет баков дыма (smoke×3) — отдаёт только пар.
- пиропечь: боковые порты и выхлоп одного типа дыма объединены в один узел (в оригинале — два узла).
- machine_boiler_off / machine_boiler_electric_off: в оригинале простой блок без TE; в порте — котёл с паром (SmallBoiler), оставлен на старом проталкивании (неоригинальный контент — на решение).
- чистые приёмники, не переведённые в этой партии (кристаллизатор, ПРЕСС, центрифуги, электролизёр, сборочные, химзавод и др.), остаются конечными точками по capability (NORMAL, давление 0); в мини-сеть входят, но не объединяют сети.
- машины в соседстве с непереведёнными HBM-машинами продолжают получать от тех проталкивание.
- ROR через любой dummy мультиблока (в оригинале — только proxy-блоки).
