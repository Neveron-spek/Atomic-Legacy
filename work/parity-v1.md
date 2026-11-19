# Parity V1 — FluidNetMK2 для оставшихся машин, дым, ROR-прокси, старые котлы

Эталон: HbmMods 936c60c (`TilePort`, `TilePortShapes`, `TileEntityLoadedBase.setupFluidPorts`, `ModulePortManFluidAdaptive`, `TileEntityMachinePolluting`, `TileEntityBarrel`, `MachineBoiler`).
Диагностика: `event/ParityV1Diagnostics.java` (x = 35000, y = 200), лог `work/verification-parity-v1.log`.

## Инфраструктура
| Что | Сделано |
|---|---|
| `fluid/HbmPortDefs` | `PortDef.make` и `TilePortShapes` (around, mixer, horizontal, assembler, refinery, flare, solderer, liquefactor); `dir` = facing ядра (meta − 10), `rot` = `dir.getClockWise()` (= getRotation(UP)) |
| Делегаты TilePort (`setupOwner(delegate)`) | `HbmFluidNetUser.fluidNetOwner()`; `HbmFluidNetworks.subscribe` / очистка / `isBadLink` берут владельца — отдельный узел сети с собственными баками |

## Переведённые машины (порты = getPorts() оригинала, баки = getSending/ReceivingTanks)
| Блок | Соединений | Отд./Прин. | Примечание |
|---|---|---|---|
| machine_crystallizer | 8 (refinery) | 0/1 | |
| machine_gascent | 6 (around) | 0/1 | PseudoFluidTank'и не в сети, как в оригинале |
| machine_electrolyser | 8 | 2/2 | порты порта на +6 по оси (capability) оставлены только для интеграций |
| machine_assembly_machine, machine_chemical_plant | 12 (assembler) | 1/1, 3/3 | были MK2 без узлов; TACZ-ассемблер (интеграция) не тронут |
| machine_assembly_factory, machine_chemical_factory | 16 / 30 | 4/4, 12/12 | + делегат охлаждения: 4 порта, вода → LPS, отдельный узел |
| machine_arc_welder, machine_soldering_station | 10 / 8 (solderer) | 0/1 | |
| machine_cyclotron | 12 | 2/1 | |
| machine_drain | 3 (dir0, rot UP/DOWN) | 0/1 | LOW |
| machine_excavator | 4 | 0/1 | |
| machine_flare | 4 (flare) | 0/1 | LOW |
| machine_icf_press | 6 (around) | 0/2 | |
| machine_rockmill | 8 | 1/1 | |
| machine_silex | 2 | 0/1 | dir = rot(meta − 10) |
| machine_solidifier | 6 (liquefactor) | 0/1 | |
| machine_strand_caster | 4 | 1/1 | |
| machine_supercomputer | 5 | 1/1 | |
| machine_wood_burner | 2 | 0/1 | |
| pile_vent | 1 | 0/1 | |
| launch_pad, compact_launcher / launch_pad_large / launch_table | 8 / 8 / 36 | 0/2 | rusted и soyuz_launcher без портов, как в оригинале |
| launchpad_lambda, launchpad_soyuz | 5 / 12 | 0/2 | |
| dfc_emitter, dfc_injector, dfc_receiver | 6 (around) | 0/1, 0/2, 0/1 | |
| drone_crate | 6 (around) | по режиму | sendingMode/itemType как в оригинале |
| turret_fritz | 8 (solderer) | 0/1 | |
| fusion_boiler / breeder / klystron / mhdt / plasma_forge / torus | 4 / 5 / 3 / 3 / 10 / 26 | 1/1, 1/1, 0/1, 1/1, 0/1, 2/4 | |
| pa_source / rfc / quadrupole / dipole / detector | 10 / 6 / 4 / 8 / 5 | 1/1 | TileEntityCooledBase; у RFC dir = getRotation(DOWN) |
| machine_annihilator | 3 | 0/1 | LOW |
| machine_autosaw | 6 (around, включая верх) | 0/1 | раньше capability без UP — по 936c60c around |
| machine_thresher | 2 | 0/1 | |
| machine_precass | 12 (assembler) | 1/1 | |
| machine_radiolysis | 4 (flare) | 2/1 | |
| machine_orbus | 8 | 1/1 | скорости TileEntityMachineOrbus, LOW в режиме 1 |

## Дым
| Объект | Было | Стало |
|---|---|---|
| machine_turbinegas | только пар; прямое загрязнение + радиация + лишний SOOT раз в 20 тиков | 3 буфера по 150 мБ (smoke/smoke_leaded/smoke_poison, NBT smoke0–2); pollute(топливо, BURN, расход·0.5) раз в 5 тиков; переполнение — в загрязнение + шипение; дым уходит через те же getPorts() |
| machine_pyrooven | боковые порты и выхлоп — один узел | getPorts(): {tanks[1], smoke}; moduleSmokePorts — отдельный узел-делегат {smoke, smoke_leaded, smoke_poison} на выхлопе |

## ROR
| Объект | Сделано |
|---|---|
| machine_orbus | TileEntityBarrel: VAL:type/fill/fillpercent, FUN:setmode (с fallback); прокси — клетки getAllPorts |
| machine_fluidtank, machine_bigasstank, machine_bat9000 | добавлены раскладки прокси (раньше отвечала любая клетка) |

## Котлы
| Блок | Было | Стало |
|---|---|---|
| machine_boiler_off | рабочий котёл с GUI, баками и паром; дроп — сам блок | MachineBoiler(false): блок без TE, только facing (setDefaultDirection + поворот к игроку), дроп 3–6 scraps_steel, скрыт |
| machine_boiler_electric_off | рабочий электрокотёл | удалён: в оригинале в ignoreMappings |

## Проверки (ParityV1Diagnostics, 1131 случай)
Для каждой машины: число соединений, баки, приоритет; каждый порт — клетка машины, соединение — вне её (исключения оригинала: узлы chemical_factory на y+3 и rot·3 вне блока, UP газовой центрифуги в собственный верх). Передачи: бочка → кристаллизатор; вода на порт охлаждения фабрики заполняет только бак охлаждения; дым турбины и выхлоп пиропечи уходят в сеть. ROR orbus (значения, setmode с fallback, скорости/LOW); прокси orbus и баков; machine_boiler_off без TE, дроп 3–6 scraps_steel; electric_off, small_boiler* не зарегистрированы.

## Не сделано / расхождения
- Пиропечь: буферы дыма порта получают тип при первом заполнении (в оригинале тип SMOKE задан всегда); при пустом баке узла дыма нет — на перенос не влияет.
- machine_custom (paper_press): в порте упрощённая фиксированная машина без конфигов; порты оригинала строятся из ProxyCombo-компонентов структуры — не переведено.
- DFC core: в оригинале не участвует в сети; в порте остаётся capability (не трогал).
- Пневматика (v2) и ICF-ядро (v4) не трогались.
- Вопрос по U2: в BlockDummyable.fillSpace makeExtra/getAllPorts отсчитываются от точки установки x (ядро = x − dir·getOffset), а раскладки U2/V1 — от ядра. Для блоков с getOffset ≠ 0 и fillSpace без `dir·o` клетки прокси оригинала могут быть сдвинуты на dir·getOffset. Не менял — требуется решение.
