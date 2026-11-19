# Партия A3 — тепловые машины, бойлеры, лазер ICF

Эталон: work/original (1.7.10, 936c60c). Самопроверка: event/ParityA3Diagnostics (checked_parity_a3_cases).

| Машина | Параметр | Оригинал | Было | Сделано | Статус |
|---|---|---|---|---|---|
| machine_boiler | взрыв при полном баке пара | ExplosionVNT 5F, EntityProcessorStandard.withRangeMod(3F), PlayerProcessorStandard, ExplosionEffectStandard, без разрушения блоков | ванильный level.explode 5F (урон в радиусе 10) | LegacyVnt.entitiesStandard().rangeMod(3).sfx(vanillaEffect) | исправлено, проверено A3 |
| machine_boiler / machine_industrial_boiler | баки 16 000/64 000, пар ×100, maxHeat 3,2M/12,8M, diffusion 0.1, остывание heat/1000, огонь Тома, порты, ROR | — | совпадало | — | сверено |
| heater_electric | потолок тепла | нет потолка (heatEnergy += setting·100) | обрезалось до 100 000 | addHeatUncapped | исправлено, проверено A3 |
| heater_electric | потребление, буфер, x0.999, забор снизу ×0.85, отвёртка 0..10 | pow(s,1.4)·200, ×20 | совпадало | — | сверено |
| heater_firebox / heater_oven | baseHeat 100/500, timeMult 1/0.125, cap 100k/500k, модуль сгорания, зола, сажа, забор печи ×0.5 | — | совпадало | — | сверено |
| heater_heatex | баки 24 000, amountToCool 24 000, tickDelay ≥1, ×0.999, FT_Coolable HEATEXCHANGER (пар 0.5), порты, только по оси | — | совпадало | — | сверено |
| FT_Heatable / FT_Coolable | таблицы BOILER, HEATEXCHANGER, ICF, PWR, TURBINE | Fluids.java 624–677 | совпадало | — | сверено, A3 |
| machine_hephaestus | идентификатор жидкости | input.setTankType(любой тип); без шага HEATEXCHANGER оба бака NONE; чат «Changed type to X!» жёлтым; при приседании — нет | невалидный тип игнорировался, звук опыта, ключ «Changed fluid type to», без проверки приседания | как в оригинале (identify) | исправлено, проверено A3 |
| machine_hephaestus | выходной бак при смене типа | setTankType — жидкость другого типа удаляется | старый тип оставался до опустошения | setType каждый тик | исправлено, проверено A3 |
| machine_hephaestus | скан 15×15 по слою (10 слоёв), лава 5 / вулк. лава 150 / ore_volcano 300 ×3 (20 тиков), шаг без КПД, баки 24 000, 8 портов | — | совпадало | — | сверено |
| machine_solar_boiler / solar_mirror | баки 100/10 000, heat/50, пар ×100, sun = sky − darken − 11, канал виден небу, tY ≥ y | — | совпадало | — | сверено |
| machine_steam_engine | баки 2000/20, КПД 0.85, 170 HE/оп., ускорение ±0.1 (0..40), звук на обороте | громкость getVolume(1.0F) | без глушителя | MachineMuffler.attenuate | исправлено |
| machine_condenser / _powered / tower_small / tower_large | баки 100 / 1M (10 HE/мБ, допуск 0.95, maxPower 10M) / 1000 / 10 000, waterTimer 20 | — | совпадало | — | сверено |
| machine_stirling (*_steel, *_creative) | diffusion 0.1, КПД 0.5, 300/1500, overspeedLimit 300, выброс шестерни | — | совпадало (Q3) | — | сверено |
| machine_flare | звуки fizz / flamethrowerShoot | getVolume(1.5F) | без глушителя | MachineMuffler.attenuate | исправлено |
| machine_flare | 64 000 мБ, 100 000 HE, vent 50 / burn 10 ×(1+SPEED), штраф 5/10, EFFECT /3 | — | совпадало | — | сверено |
| пилоны / подстанция / коннекторы | длина провода 10/100/25/45/100/20, тип SINGLE/TRIPLE/QUAD | — | совпадало | — | сверено |
| icf_controller | заливка сборки | контроллер заранее в карте сборки (не ошибка, входит в maxSize 1024) | клетка рядом с контроллером давала «Non-laser block» — сборка невозможна | контроллер в found до заливки | исправлено, проверено A3 |
| icf_controller | приём лазера | блок icf на луче и ядро ровно на (i+8, y−3) | любой блок ICF с любой стороны | проверка смещения ядра | исправлено |
| icf_controller | AABB урона луча | min/max координат, затем +0.2/+0.8 | для отрицательных направлений бокс сжат на 0.6 | как в оригинале | исправлено, проверено A3 |
| icf_controller | прочность разрушаемого блока | getExplosionResistance(null) < 6000 | сопротивление без учёта ядра для фиктивных блоков | state.getExplosionResistance(level,pos,null) | исправлено |
| icf_controller | индикатор мощности | printHook «power/maxHE» (короткие числа) | сообщение в строке действий при ПКМ (придумано) | ParityA3LookOverlays; сообщение убрано | исправлено |
| icf_controller | maxPower √cap·2.5M + √min(turbo,cap)·5M, луч 1..49, урон inFire 50 + огонь 5 с | — | совпадало | — | сверено, A3 |
| icf | maxLaser | icf.maxLaser += getMaxPower() (сумма контроллеров) | максимум | сумма | исправлено, проверено A3 |
| icf_block | поломка собранной части | removeTileEntity, затем возврат исходной детали, controller.assembled = false | блок-сущность icf_block оставалась на возвращённой детали и сохранялась в чанк (ошибка «Invalid block entity» при загрузке) | removeBlockEntity перед возвратом | исправлено, проверено A3 |
| machine_sawmill | звук перегрузки | (x+0.5, y+1, z+0.5) | центр блока | как в оригинале | исправлено |
| machine_sawmill / machine_ashpit | diffusion 0.1, порог 100, progress += heat/10, 600, опилки 0.5/0.1, перегрузка >300 (60/300); пороги золы 2000/8000 | — | совпадало | — | сверено |
| icf_block | проверка ядра каждые 20 тиков | при невыгруженном чанке ядра ничего не делает | getBlockEntity грузил чанк | level.isLoaded | исправлено |

Не сверено в A3: турбовентилятор, radgen, зарядник, fluid_pump, сливы (сверены ранее в Q3/Q4/U1 — повторно не проверялись); рендеры и GUI-текстуры.
Открыто: ошибки сборки ICF — строка в строке действий вместо мирового маркера (как PWR/Pile); проверка смещения ядра ICF для луча автоматически не проверена (нужна полная структура).

Проверки (work/verification-parity-a3.log): assemble, validateJarIntegrity, validateLangFiles, validateRegistrySources, validateRecipeBaseline, validatePowerParity, validateCapacitorParity, validateStellarFluxParity, validateThermalParity, checkBuildingAudit (errors=0, checked_parity_a3_cases=23), checkItemAudit (errors=0) — успешно. build.gradle и recipe-baseline не менялись. Клиент не запускался.
