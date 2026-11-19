# Партия U2 — ROR/RTTY машин (IRORValueProvider / IRORInteractive)

Эталон: HbmMods 1.7.10 `936c60c4`. Диагностика `event/ParityU2Diagnostics.java` (x = 33000, y = 200), лог `work/verification-parity-u2.log`.
Таблица getFunctionInfo сгенерирована скриптом из исходников оригинала (63 блока) и сверяется в диагностике побайтно.

## Инфраструктура
| Что | Сделано |
|---|---|
| Прокси dummy (TileEntityProxyCombo) | `rtty/RttyProxyLayouts`: клетки makeExtra/getAllPorts каждой машины (core + nx·rot + nz·dir), ALL для турелей и launch_pad_large, NONE для cargo_elevator, compact_launcher, launch_table, launch_pad_rusted, turret_howard_damaged, machine_tacz_ammo_assembler; RBMK — правило T2. Подключено в RadioTorchBlock/RadioTorchBlockEntity (одна строка в каждом) |
| Имена рецептов (getRecipeName) | `rtty/RttyRecipeNames`: группа рецепта = внутреннее имя (ass./chem./fus./plsm.), PUREX — по имени файла + 10 исключений; без рецепта — "null" |

## Машины (новое)
| Блок(и) | Значения / функции | Примечание |
|---|---|---|
| machine_purex | progress, recipe, active | |
| machine_assembly_machine, machine_chemical_plant | progress, recipe, active; setrecipe!name | restrictedMode: скорость ×0.25, вариант полосы GUI (176,77), сброс выбором в GUI; неизвестное имя сохраняется как в оригинале |
| machine_assembly_factory, machine_chemical_factory | progress1-4, recipe1-4, anyactive, active1-4 | progress фабрики сборки = тики/workTime порта |
| watz | heat, flux, mud, coolant_hot, coolant_cold | |
| pa_source/dipole/quadrupole/rfc/detector | по оригиналу; cancel, setthreshold | pa_beamline — не ROR |
| machine_battery_socket | fill, maxfill, fillpercent, delta; setmode/setredmode (fallback), setpriority | |
| machine_chungus, machine_industrial_turbine | output; flywheel | |
| machine_turbinegas | 11 значений; setauto, setthrottle, setstate | NumberFormatException игнорируется |
| machine_combustion_engine | state, throttle, power, fuel, efficiency; setstate, setthrottle (0..30) | |
| machine_boiler, machine_industrial_boiler | input, output (взорванный котёл → 0) | |
| heater_heatex, heater_oilburner | hotfluid/coldfluid/heat; heat/fuel/burnRate/state, setstate, setburnrate | |
| launch_pad, launch_pad_large, compact_launcher, launch_table | state/solid…, canlaunch; launch, launch!entity, launch!x:z | rusted и старый soyuz_launcher — не ROR |
| launchpad_lambda | countdown…isauto, canlaunch; launch, setauto | launch без проверки erected, как в оригинале |
| launchpad_soyuz | state (имя SoyuzStatus)…cargomode; launch, launch!x:z, setcargomode | добавлены targetX/Z (не сохраняются) и confirmTarget |
| dfc_emitter | setpower, toggle, switch | только interactive |
| dfc_stabilizer | durability, durabilitypercent; setpower | |
| pile_control / pile_loader | setrods, extendrods / meta, depletion, deppercent, lifetime, temp | |
| red_cable_gauge, fluid_duct_gauge | deltatick, deltasecond | у трубного датчика добавлен тикер (окно 20 тиков) |
| machine_satlinker | freq; setfreq (1..10 000 000) | |
| turret_chekhov, turret_jeremy | 7 функций TurretBaseNT | |
| pwr_block (части PWR) | ROR контроллера, только бывший pwr_port; остальные → "" | |

## Исправлено в существующих
| Объект | Было | Стало |
|---|---|---|
| cargo_elevator | VAL:extension выдавался, «setextension!height» | только interactive, «setextension!amount» |
| machine_excavator, machine_flare, pneumatic exporter | `equals("1")` / on-off | parseBool |
| fusion_torus / plasma_forge | recipe = хвост пути, "" | внутреннее имя, "null"; setrecipe по имени без сброса прогресса |
| остальные турели (LegacyTurret) | порядок FUN у arty/himars, enqueue у всех типов, длина имени | порядок оригинала, enqueue только артиллерия, addName без ограничений GUI |
| reactor_zirnox slottype | TH232_FUEL, LITHIUM_FUEL, ZFB_MOX_FUEL | TH232, LITHIUM, ZFB_MOX |
| pwr_controller depletion | защита от деления на 0 | как в оригинале |
| обычные трубы, все pneumatic storage, все legacy-машины, все части fusion | считались ROR | только оригинальные компоненты |

## Не перенесено / расхождения
- network_separator и machine_battery/лития/… (deprecated, скрытые в оригинале) в порте отсутствуют — их ROR не перенесён.
- PUREX: рецептов purex.watznaqadah/watznaqadria нет в порте.
- Читатель на ProxyCombo-dummy машин без ROR (в оригинале ставится, значения null) не моделируется.
- Конденсаторы (capacitor_*) в оригинале ROR не имеют.
- ICF: в порте нет «крыльев» MachineICF.fillSpace (y+2..4, ±2 по dir, ±8 по rot), поэтому нет 4 боковых портов-прокси (±6, 3, ±2); работает только верхний (0, 5, 0). Расхождение структуры (не ROR), в диагностике отмечено как известное.
- Диагностика ставит машины через setBlock + setPlacedBy (грань EAST, Союз NORTH); оси прокси подтверждены: все клетки makeExtra оказались dummy своего ядра.
