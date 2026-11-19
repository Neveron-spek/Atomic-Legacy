# Parity S2 — системная часть (команды, достижения, конфиги)

Эталон: HbmMods 1.7.10 `936c60c4`. Диагностика: `event/ParityS2Diagnostics.java` (`checked_parity_s2_cases`), таблицы
`event/ParityS2ConfigTable.java` и `event/ParityS2AdvancementTable.java` сгенерированы из исходников оригинала скриптами
`work/tmp-s2/gen_table.py`, `work/tmp-s2/gen_adv.py`.

## Команды (12 команд оригинала + абстрактный CommandReloadConfig)

| Команда | Класс оригинала | Права оригинала | Порт | Статус |
|---|---|---|---|---|
| /ntmlocate structure/list | CommandLocate | 4, только игрок | NtmLocateCommand | исправлено: ключи `commands.locate.no_match/none_found/success.coordinates` (цвета), поиск кольцами `radius < 256` по `selectedNameAt` для всех структур (был свой поиск 1024 чанка и своя строка с расстоянием), чанк старта `floor(x)/16`, консоль — ошибка «нужен игрок» |
| /ntmsatellites orbit/descend/list | CommandSatellites | 4 | NtmSatellitesCommand | сверено, без изменений |
| /ntmcustomize [args] | CommandCustomize | 0 (все) | ParityS2Commands | перенесено (ICustomizable, тексты) |
| /ntmrad set/clear | CommandRadiation | 4 | ParityS2Commands | перенесено (0..100000, тексты, usage красным) |
| /ntmreapnetworks | CommandReapNetworks | 4 | ParityS2Commands | перенесено: вместо UniNodespace сбрасываются сети энергии, жидкостей (трубы перерегистрируются) и пневмотруб порта |
| /ntmreload | CommandReloadRecipes | 4 | ParityS2Commands | адаптация: перезагрузка data pack (рецепты/лут/теги порта — data pack); JSON-конфигов жидкостей/брони/LogicBlock в порте нет |
| /ntmserver help/list/reload/get/set | CommandReloadServer | любой игрок (как в оригинале) | ParityS2Commands + ParityS2RunningConfig | перенесено поверх ModConfigSpec (тексты, типы Float/Integer/Boolean/Double, ключ GUN_DOUBLE_BARREL_DAMAGE → урон дрели как в оригинале) |
| /ntmclient … | CommandReloadClient | клиентская | ParityS2ClientCommands | перенесено (клиентская команда), 5 привязанных ключей |
| /ntmloadchunk x z | CommandDebugChunkLoad | 4 | ParityS2Commands | перенесено (NBT 1.21 `block_entities`, диапазон Y мира) |
| /dumpthreadsandcrashgame dump/crash | SuicideThreadDump | клиентская | ParityS2ClientCommands | перенесено (FATAL→ERROR в slf4j, `halt(0)`) |
| /ntmwikirender <type> | CommandWikiRender | 4 (серверная, открывает клиентский GUI) | ParityS2ClientCommands | адаптация: клиентская команда, GUIScreenWikiRender через NativeImage; один стек на предмет (метаданных нет); фильтр по именам классов порта |
| /ntmpackets | CommandPacketInfo | 4 | — | НЕ перенесено: в порте нет PacketThreading (сетевой стек NeoForge) |

/rhbm (radiation/pollution) — команда основы порта, в оригинале её нет; оставлена (открытый вопрос).

## Достижения

61 достижение оригинала (`new Achievement` в MainRegistry) + технический корень `root` (в 1.7.10 одна страница «Nuclear Tech»,
в 1.21 дереву нужен корень). Сверены: родители, иконки, рамка (setSpecial → challenge), ключи текстов.
- Удалено: `foeq.json` и выдача в SoyuzEntity (в MainRegistry нет achFOEQ, только строки lang).
- Иконка tasteofblood: fluid_icon с ASCHRAB (было без жидкости).
- Тексты: en_us исправлены 2 опечатки (SILEX «Exitation», RBMK «Roentegen»); добавлены переводы 8 языков из .lang оригинала
  (de/fr/it/ja/pl/pt_br/ru/uk; если в .lang языка ключа нет — остаётся откат на en_us, как в 1.7.10); корень — «Nuclear Tech»,
  описание пустое (придуманное описание и zh-перевод удалены).
- Триггер digamma_see: выдаётся при любом digamma > 0 (было ≥ 0.01).
- Триггеры остальных сверены по местам вызова оригинала (крафт/плавка/наковальня/выходные слоты, подбор слизи, гнейс, смерти боссов, MK3/MK5,
  Horizons, PA детектор, RBMK/Watz/ZIRNOX, ключи, бутылки и т.д.); недостижимые в оригинале (sacrifice, impossible, tasteofblood, c20_5,
  digamma_up_on_top, horizons_bonus) остаются `minecraft:impossible`.

## Конфиги

Проверяется 208 ключей Forge-конфига/RBMKDials/hbmMachines.json + 99 ServerConfig/GunValues + 5 ClientConfig:
значение по умолчанию порта == оригиналу (диагностика).

Добавлено и подключено (`config/HbmServerParityConfig.java`, секции `hbmServer`, `dangerousItemsS2` общего TOML):
CRATE_OPEN_HELD, CRATE_KEEP_CONTENTS (ящики/сейф), ITEM_HAZARD_DROP_TICKRATE (`max(1, n)`), ENABLE_MKU, MAX_BUFFER_LENGTH (буфер AUTOCAL
усекается, как ParseContext#writeBuffer), MAX_STACK_SIZE (в оригинале стек не используется MS-ES1), WeaponConfig 10.05_dropDead.

Не привязаны (система не перенесена или значение захардкожено — не проверено поштучно):
- ServerConfig: DAMAGE_COMPATIBILITY_MODE (нет режима SuperCompatibility), STRUCTURE_DEBUG (порт передаёт флаг debug параметром), DEBUG_PORT_VIEW (нет TilePort).
- GunValues: GUN_HERETIC_DAMAGE, GUN_MINIGUN_DUAL_LEFT/RIGHT_DAMAGE (DEBUG-оружие не перенесено), MELEE_RPA_*, MELEE_NCRPA_*, RANGED_NCRPA_DAMAGE (XFactoryPA не перенесён).
- ClientConfig (21): GEIGER_OFFSET_*, INFO_OFFSET_*, INFO_POSITION, ITEM_TOOLTIP_SHOW_OREDICT/CUSTOM_NUKE, MAIN_MENU_WACKY_SPLASHES, DODD_RBMK_DIAGNOSTIC,
  RENDER_CABLE_HANG, NUKE_HUD_FLASH/SHAKE, RENDER_REEDS, NEI_HIDE_SECRETS, RENDER_REBAR_*, RENDER_HELIOSTAT_BEAM_LIMIT, TOOL_HUD_INDICATOR_X/Y, SHOW_BLOCK_META_OVERLAY, BADGES_HUD.
- BombConfig (19): радиусы gadget/boy/man/mike/tsar/prototype/fleija/missile/mirv/nuka/solinium/n2, limitExplosionLifespan, blastSpeed, mk5BlastTime, falloutRange/Delay, enableChunkLoading, explosionAlgorithm (в NukeExplosionManager константы).
- RadiationConfig (16): FOG_00/01, AMBIENT_00_nether, RADWORLD_00..03, RADIATION_00_enableContamination, RADIATION_01_enableChunkRads, RADIATION_99_enablePRISM, HAZ_02..06 (hot/explosive/hydro/blinding/fibrosis), POL_08_smokeStackSootMult.
- ToolConfig (13): recursion*, ability*.
- MobConfig (13): D02/D03 mob weapons, G12 largeHiveThreshold, I01/I02 infestation, GC01..GC08 шансы глифидов.
- WeaponConfig: 10.02_dropStar (WeaponizedCell не найден в порте), 18.00_linearAnimations.
- GeneralConfig (23): packet threading 0.01–0.04, crashOnThermos, server recipe sync, debug mode, MOTD, guide book, skyboxes, impact world provider, render dist check, keybind overlap, sound extension/channels, Mekanism, load screen, crosshairs, extended logging, modded-dimension ores, preferredOutputMod, X528_oreColtanFrequency.
- PotionConfig 8.00–8.11 (ID эффектов) — неприменимо в 1.21 (реестр).
- StructureConfig (44): 5.00_enableStructures ("flag" → в порте boolean), lootAmountFactor, debugStructures, веса структур 5.07–5.49; WorldConfig (59): частоты руд/кластеров, веса бедрок-руд, крейтер-биомы — зона R3 (не дублировалось).
- RBMKDials: в оригинале game rules, в основе порта — ключи конфига; dialReasimRange (10) и dialSaveDials в порте отсутствуют.
- hbmMachines.json: проверены 8 машин (турбины, двигатель, Стирлинг, силовое поле, радары, конвертеры); остальные ~22 IConfigurableMachine — константы.
- Ключи порта без аналога в оригинале: конвертер heToFeConversionRate, autoCableConversion, Create/IE-совместимость, enableLargeMachineOcclusionCulling,
  enableInfiniteWaterTankRecipes, pinkTreeSpawnRate/stoneKeyholeSpawnRate (в оригинале захардкожены), паровая турбина (в оригинале не настраивается).
