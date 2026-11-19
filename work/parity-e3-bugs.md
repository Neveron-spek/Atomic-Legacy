# e3 — второй проход по багам (2026-11-18)

Статический обзор кода порта против эталона 936c60c. Gradle и сервер не запускались: **ничего из перечисленного не скомпилировано и не проверено в игре**. Самопроверка: `event/ParityE3BugDiagnostics.run(MinecraftServer, List<String>)` (в BuildingPortDiagnostics не подключена).

## Баги

| # | класс | баг | где | причина | статус |
|---|---|---|---|---|---|
| 1 | 1 | ArithmeticException в тике спавнера глифидов при `swarmCooldown` > 107 374 182 | `config/HbmConfig.glyphidSwarmCooldown` | `Math.multiplyExact(сек, 20)` (в эталоне обычное `* 20`) | исправлено: насыщение до Integer.MAX_VALUE |
| 2 | 1 | IllegalArgumentException `nextInt(отрицательное)` при `scoutSwarmChance` = Integer.MAX_VALUE | `tileentity/GlyphidSpawnerBlockEntity` | переполнение `chance + 1` | исправлено: `min(chance, MAX-1) + 1` |
| 3 | 2 | Изменение реестра сущностей во время обхода `getAllEntities()`: `hurt()` ломает стойку для брони (удаление + дроп) | `event/TomImpactEvents.burnExposedLiving` | обход живого итератора | исправлено: обход копии |
| 4 | 2 | То же при детонации копья (contaminate DIGAMMA2 может убить) | `entity/DigammaSpearEntity.detonate` | то же | исправлено: обход копии |
| 5 | 4 | Очередь «отходов» ZIRNOX переживала остановку сервера и выполнялась в следующем мире (одиночная игра, тот же ключ измерения) | `worldgen/ZirnoxWasteManager.TASKS` | нет очистки | исправлено: Level.Unload + ServerStopped (`event/ParityE3Events`) |
| 6 | 4 | Решатель загрязнения с незавершённым снимком оставался в карте и мог применить снимок старого мира к новому | `pollution/HbmPollutionWorlds.SOLVERS` | `clear()` нигде не вызывался | исправлено: там же |
| 7 | 4 | Записи пьедесталов-оберегов с игровым временем прошлого мира | `event/MeteorPedestalBroadcasts.ENTRIES` | нет очистки | исправлено: там же |
| 8 | 4 | UUID→тик следующего «plink» не чистился; в мире с меньшим игровым временем звук надолго подавлялся | `items/LegacyArmorParity.NEXT_PLINK` | нет очистки при выходе | исправлено: PlayerLoggedOut |
| 9 | 4 | UUID→состояние Ctrl/Alt инструмента настроек не чистилось | `items/SettingsToolItem.KEY_STATES` | то же | исправлено: PlayerLoggedOut |
| 10 | 5 | Обход энергосети загружал (и генерировал) чанки: кабель/линия пилонов/порт машины у границы незагруженного чанка, цепочкой до 8192 узлов; также `pruneInvalidEndpoints` | `power/PowerNetworkManager` (isPowerNode, canEnterPowerNode, powerObject, resolvePowerCorePos, powerFlowLimit, requiresDirectedPowerRouting) | `getBlockEntity/getBlockState` без проверки; в эталоне Nodespace знает только узлы загруженных TE | исправлено: `hasChunkAt` |
| 11 | 5 | То же для жидкостей: машина у границы чанка каждый тик дёргала соседний незагруженный чанк | `fluid/HbmFluidNetworks` (canPipeConnectTo, drainFrom, fillInto, netUser, collectPipes ×2) | эталон: `Compat.getTileStandard` (chunkExists) | исправлено |
| 12 | 5 | Нейтронный поток RBMK и консоль загружали чанк | `tileentity/RbmkComponentBlockEntity.rbmkColumnAt` | то же | исправлено |
| 13 | 5 | Луч нейтронов Чикагской поленницы загружал чанк | `neutron/PileNeutronNetwork.resolve` | то же | исправлено |
| 14 | 5 | Каждый блок луча прожектора раз в 5 тиков загружал чанк лампы | `tileentity/FloodlightBeamBlockEntity.tick` | эталон FloodlightBeam: `chunkExists(source)` | исправлено: незагруженный источник — луч остаётся |
| 15 | 5 | Горящая земля распространялась в незагруженный чанк | `blocks/WasteEarthBlock.tickBurningEarth` | эталон WasteEarth:137 `blockExists` | исправлено |
| 16 | 6 | При возврате из Энда сохранялись радиация, дигамма, асбест, чёрное лёгкое, огонь/фосфор/балефайр, заражение MKU, эффекты contamination | attachments `living_radiation`, `living_hazards`, `mku_contagion` | NeoForge копирует все attachments при клоне без смерти; в 1.7.10 HbmLivingProps не копируются никогда | исправлено: `ParityE3Events.onPlayerClone` (LOWEST) сбрасывает при `!wasDeath`. **Это повторение особенности эталона (портал из Энда снимает радиацию); убирается удалением одного вызова `resetLivingProps`** |
| 17 | 6 | Нажатая клавиша ранца не сбрасывалась при смене измерения и при клоне | `items/LegacyJetpackItem.INPUT_ACTIVE` | нет аналога `onPlayerChangeDimension` (JETPACK=false) | исправлено: `ParityE3Events` |
| 18 | 7 | `/rhbm radiation get`, `/rhbm radiation chunk get`, `/rhbm pollution get` доступны любому игроку (команды нет в эталоне; заменяла счётчик Гейгера) | `command/RhbmCommand` | `requires` только на изменяющих ветках | исправлено: уровень 2 на корне |
| 19 | 8 | Пакет с произвольной позицией загружал чанк до проверки расстояния | `RbmkConsoleControl`, `RbmkControlRod`, `RbmkAutoControl`, `RbmkCraneControl`, `DfcControl`, `TurretJeremyControl`, `TurretChekhovControl`, `LegacyTurretControl` | `getBlockEntity(payload.pos)` первым действием | исправлено: `isLoaded` |
| 20 | 8 | С открытым GUI одного радара можно было управлять любым другим радаром мира и запускать его ракеты | `RadarControlPayload`, `RadarCommandPayload` | позиция пакета не сверялась с позицией меню | исправлено |

## Данные игрока: эталон и порт

Эталон: смерть и возврат из Энда создают нового EntityPlayerMP (Clone); `onPlayerClone` переносит `HbmPlayerProps.serialize` (shield, maxShield, enableBackpack, enableHUD, reputation, isOnLadder, enableMagnet). HbmLivingProps и остальное — с нуля. Обычная смена измерения сохраняет сущность.

| данные | эталон: смерть | эталон: из Энда | эталон: смена измерения | порт после правок |
|---|---|---|---|---|
| radiation, digamma, radEnv/radBuf, contamination | сброс | сброс | сохр. | `living_radiation`: сброс / сброс (п. 16) / сохр. |
| asbestos, blacklung, fire, phosphorus, balefire, blackFire | сброс | сброс | сохр. | `living_hazards`: так же |
| contagion | сброс | сброс | сохр. | `mku_contagion`: так же |
| oil | сброс | сброс | сохр. | отдельного поля в attachments не найдено — не сверялось |
| shield, maxShield | сохр. | сохр. | сохр. | `player_shield` copyOnDeath — совпадает |
| enableMagnet, isOnLadder | сохр. | сохр. | сохр. | `player_armor_state` copyOnDeath — совпадает |
| reputation | сохр. | сохр. | сохр. | `player_reputation` copyOnDeath — совпадает |
| enableBackpack, enableHUD | сохр. | сохр. | сохр. | persistentData `hbm_parity_i2`, копия в ArmorParityEvents.onClone, синхронизация при входе и респавне — совпадает |
| maskManTimer | сброс | сброс | сохр. | persistentData `hbm_maskManTimer` (не PlayerPersisted) — совпадает |
| fbiMark, radMark, hasDucked (PERSISTED_NBT_TAG) | сохр. | сохр. | сохр. | `legacy_mob_spawn_state` copyOnDeath — совпадает |
| keysPressed (JETPACK, DASH) | сброс | сброс | JETPACK/DASH = false | сброс JETPACK (п. 17); DASH в порте — разовый пакет |
| dashCooldown, stamina, totalDashCount, plinkCooldown, grenadeDeployment, lastDamage | сброс | сброс | сохр. | не сверялось построчно |
| Dr_Nostalgia: шляпа и beta при респавне | выдача | — | — | не сверялось |

Синхронизация клиенту после респавна и смены измерения: attachments с `.sync()` рассылает NeoForge (по исходникам 21.1.250 не перепроверялось до конца); переключатели ранца/HUD — `LegacyArmorModParity.sync` при входе и респавне.

## Команды (уровень прав: эталон → порт)

- ntmcustomize: 0, всем → без requires — совпадает
- ntmrad: 4 → 4
- ntmreapnetworks: 4 → 4
- ntmreload: 4 → 4
- ntmloadchunk: 4 → 4
- ntmlocate: 4 → 4
- ntmsatellites: 4 → 4
- ntmserver: только игрок → только игрок
- ntmclient, dumpthreadsandcrashgame, ntmwikirender: клиентские (client/ParityS2ClientCommands, QmawClientEvents) — права не сверялись
- ntmpackets (CommandPacketInfo, уровень 4): в порте не зарегистрирована
- rhbm (только порт): чтение было без прав → 2 (п. 18)

## Проверенные serverbound-пакеты (58)

Меню открыто + позиция меню = позиция пакета, значения ограничены в BE: AnnihilatorControl, ToggleHeater, OilburnerBurnRate, HeatExchangerControl, GasTurbineControl, WoodBurnerControl, MicrowaveControl, LambdaLaunchpadControl, SoyuzLaunchpadControl, CompressorControl, FunnelControl, MissileAssemblyControl, FusionMachineControl, MixerControl, ResearchReactorControl, ReactorControl, ZirnoxControl, PwrControl, ParticleAcceleratorControl, BatteryReddControl, BatterySocketControl, RadioRecControl, RttyConfig, RadioTorchControl, RadioTelexControl, AutocalControl, SoyuzLauncherControl, PneumaticAccess.Click (containerId + stillValid).

Проверка предмета в руке: SetFluidIdentifier, SetRttyPagerChannel, SetRttyDesignatorChannel, ManualDesignator, BobmazonOrder (каталог в руке, индекс в границах), SatellitePanelClick, SatelliteCoord, GunKey (индекс клавиши в границах).

Только состояние самого игрока: SettingsToolKeys, LegacyDuck, ToggleMagnet, TrainInventory (игрок в вагоне), JetpackControl, ArmorDash, PortableCrateOpen, PortableCrateSlotClick, ArmorParity.ToggleKey.

isLoaded + расстояние как в эталоне (hasPermission): WandConfig (tandem/jigsaw — без расстояния, как в эталоне), RbmkPanelControl, FlowControlPumpControl, NetworkSeparatorControl.

Исправлены (п. 19–20): RbmkConsoleControl, RbmkControlRod, RbmkAutoControl, RbmkCraneControl, DfcControl, TurretJeremyControl, TurretChekhovControl, LegacyTurretControl, RadarControl, RadarCommand.

PlayerInform, PollutionSync и остальные — только к клиенту.

## Записано, не исправлялось

- Энергосеть: компонент ограничен 8192 узлами, запасной обход труб — 1024 (UNINOS в эталоне без предела). Сеть больше предела делится на части. Циклов и рекурсии без ограничения не найдено.
- Лучи на дальние расстояния без проверки загруженности (DFC emitter 50, injector/stabilizer 15, FEL 24, прожектор, лазер ICF, вентилятор): в 1.7.10 `getBlock` на сервере тоже загружал чанк, поведение оставлено.
- SatellitePanelClick / SatelliteCoord: удар спутника по произвольным координатам загружает чанк — как в эталоне.
- SoyuzLauncherControl: `mode = (byte) value` без ограничения — как в эталоне.
- RbmkConsoleControl: отрицательное число выбранных колонок в пакете → исключение декодера (отключает только отправителя).
- Меню (область e2): делители `*Scaled` в 129 меню проверены скриптом — нулевых делителей из несинхронизированных данных не найдено.
- `LegacyBomberEntity`: `tickCount % bombRate` при `bomb_rate` = 0 из NBT — защищено коротким вычислением условия (bombStart/bombStop тоже 0), как в эталоне.
- Обзор классов 1–3 выполнен поиском по шаблонам (≈400 делений/остатков, 43 обхода игроков/сущностей, 12 рекурсивных функций, 6 обходов сетей), не построчным чтением 2681 файла.
