# Parity V3 — конфиги оригинала и горячие слитки

Эталон: HbmMods 1.7.10 `936c60c4`. Новые ключи: `config/HbmParityV3Config.java` (секции общего TOML `03_nukes`, `06_explosions`,
`11_tools`, `12_mobs`, `13_radiation`, `14_hazard`, `16_pollution`, `rbmkDials`; имена с точкой — один ключ TOML), клиентские —
`config/HbmClientConfig.java` секция `hbmClient`. Умолчания проверяет таблица `ParityS2ConfigTable` (FORGE +58, CLIENT +9),
поведение — `event/ParityV3Diagnostics.java` (`checked_parity_v3_cases`).

## Подключено

| Конфиг | Ключ | Умолч. | Куда подключён |
|---|---|---|---|
| BombConfig | 3.00 gadget / 3.02 man / 3.03 mike / 3.04 tsar / 3.05 prototype / 3.06 fleija / 3.12 solinium / 3.13 n2 | 150/175/250/500/150/50/150/200 | `LegacyNukeDefinition.radius()` (блоки-бомбы; Tsar без полной загрузки — manRadius, Mike — всегда mikeRadius, как в оригинале) |
| BombConfig | 3.01_boyRadius | 120 | `NukeExplosionManager.scheduleLittleBoy` |
| BombConfig | 3.07_missileRadius | 100 | ракеты nuclear r, cluster 2r, doomsday 2r, rusted r; ExplosiveCharge (nuke), арт.снаряд NUKE |
| BombConfig | 6.01_blastSpeed | 1024 | начальная скорость MK3 (`Mk3ColumnExplosion`: Fleija, Solinium, ядро DFC, custom-схрабидий) |
| BombConfig | 6.02_mk5BlastTime | 50 | бюджет MK5 (лучи и разрушение, мс/тик) и бюджет осадков EntityFalloutRain |
| BombConfig | 6.03_falloutRange | 100 | масштаб осадков `(int)(len*2.5+add)*range/100` |
| BombConfig | 6.04_falloutDelay | 4 | задержка между порциями осадков (MK5) |
| ToolConfig | 11.00 recursionDepth / 11.01 recursionStone / 11.02 recursionNetherrack | 1000/false/false | жилокопатель `HbmToolBehavior` |
| ToolConfig | 11.03–11.12 ability* (10) | true | `HbmToolProfile.areaAbilities()/harvestAbilities()/maxLevel` (подсказки, пресеты, restrictTo) |
| RadiationConfig | FOG_00_threshold / FOG_01_threshold | 100 / 20 (≤0 → 20, setDef) | туман `ChunkRadiationData.updateSystem` |
| RadiationConfig | AMBIENT_00_nether | 0.1 | минимум RAD/s в аду (`RadiationEvents`) |
| RadiationConfig | RADWORLD_00_toggle | true | `handleWorldDestruction` |
| RadiationConfig | RADWORLD_03_regrow | false | `WasteEarthBlock.randomTick` (waste_earth/waste_mycelium → dirt) |
| RadiationConfig | RADIATION_00_enableContamination | true | `HbmLivingRadiation.getRadiation` = 0, set/add игнорируются (чтение NBT не затрагивается) |
| RadiationConfig | RADIATION_01_enableChunkRads | true | радиация чанков: чтение 0, запись отбрасывается, updateSystem/разрушение/NBT чанка выключены |
| RadiationConfig | HAZ_02 hot / HAZ_03 explosive / HAZ_04 hydroactive / HAZ_05 blinding | false | инвентарь (поджог, слепота), `HbmReactiveHazards` (держатель и выброшенный предмет) |
| MobConfig | 12.GC01–GC08 | списки оригинала | уже были подключены (`HbmConfig.GLYPHID_*_CHANCE`); добавлены в таблицу |
| ClientConfig | GEIGER_OFFSET_HORIZONTAL / VERTICAL | 0 / 0 | новый HUD счётчика Гейгера `client/ParityV3GeigerHud` (renderRadCounter) |
| ClientConfig | INFO_POSITION / INFO_OFFSET_HORIZONTAL / VERTICAL | 0 | `PlayerInformOverlay` переписан по RenderInfoSystem |
| ClientConfig | DODD_RBMK_DIAGNOSTIC | true | все look-оверлеи `HbmLookOverlay` (в оригинале флаг включает все ILookOverlay) |
| ClientConfig | RENDER_CABLE_HANG | true | провисание проводов `PowerPylonWireLayout` (выкл. — один прямой отрезок) |
| ClientConfig | RENDER_REEDS | true | глубина камыша `ReedsBakedModel` (выкл. — 1) |
| ClientConfig | RENDER_HELIOSTAT_BEAM_LIMIT | 250 | лимит лучей солнечного котла |

Ключи без эффекта и в оригинале (код их не читает), определены для совпадения файла конфига: 3.08_mirvRadius, 3.10_nukaRadius,
6.05_enableChunkLoading, 6.06_explosionAlgorithm (читаются только ExplosionNukeRayParallelized, который MK5 не создаёт), RADWORLD_01_amount,
RADWORLD_02_minimum (handleWorldDestruction — константы 10/10), HAZ_06_disableFibrosis, POL_08_smokeStackSootMult, 12.G12_largeHiveThreshold,
12.I01_enableInfestation, 12.I02_baseInfestChance, dialReasimRange (getReaSimRange без вызовов).

## Не подключено (система не перенесена)

- 6.00_limitExplosionLifespan — взрывы порта не сохраняются между загрузками мира (ключ действует только при чтении NBT MK3).
- RADIATION_99_enablePRISM — ChunkRadiationHandlerPRISM не перенесён.
- 12.D02_enableMobWeapons, 12.D03_mobWeaponSootReduction — нет getSkelegun/MobUtil.slotPoolGuns (скелеты с оружием по саже).
- DAMAGE_COMPATIBILITY_MODE — нет EntityDamageUtil.attackEntityFromNT (SEDNA MK1/MK2); урон порта через ванильный hurt.
- dialSaveDials — правила игры RBMK заменены ключами конфига.
- ClientConfig (12): ITEM_TOOLTIP_SHOW_OREDICT, ITEM_TOOLTIP_SHOW_CUSTOM_NUKE (нет подсказок словаря руд и custom-нюка), MAIN_MENU_WACKY_SPLASHES,
  NUKE_HUD_FLASH, NUKE_HUD_SHAKE (нет вспышки/тряски HUD), NEI_HIDE_SECRETS (NEI), RENDER_REBAR_SIMPLE (статическая модель блока),
  RENDER_REBAR_LIMIT (рендер по BE, без общего списка), TOOL_HUD_INDICATOR_X/Y (индикатор способности не перенесён),
  SHOW_BLOCK_META_OVERLAY (метаданных нет), BADGES_HUD (значки не перенесены).

## Найденные расхождения (исправлены в пользу оригинала)

- Осадки: обрабатывали 1 чанк за порцию; в оригинале — сколько успеет за mk5 = 50 мс.
- Масштаб осадков не умножался на falloutRange/100 (при умолчании совпадало).
- Солнечный котёл рисовал не больше 64 лучей, в оригинале 250.
- DODD_RBMK_DIAGNOSTIC в оригинале выключает все look-оверлеи (не только RBMK).
- Оверлей сообщений (RenderInfoSystem): был без фона, в (8, 8+…), без затухания; теперь позиция 15/15, фон, сортировка по длительности, затухание.
- HUD счётчика Гейгера (geiger_counter в инвентаре) отсутствовал; придуманная строка «hud.reinhardtshbm.geiger» у ArmorFSB customGeiger удалена
  (в оригинале customGeiger только скрывает HUD счётчика).
- WasteEarth: после превращения в землю проверка гриба над блоком больше не пропускается (как в оригинале).

## Горячие слитки

ItemHot.heat — общее статическое поле, последним создаётся ingot_chainsteel (100): все горячие предметы нагреваются до 100, доля = heat/100
(без ограничения), остывание 1/тик в инвентаре (100 тиков), на следующем тике после 0 весь CUSTOM_DATA удаляется (stackTagCompound = null).
Урона и поджога у ItemHot нет. ItemHotDusted.getMaxHeat (200−10·мета) не используется (статический метод не переопределяется).
Добавлено: печные рецепты оригинала (SmeltingRecipes 167–181) `smelting_hot_*` для ingot_chainsteel, ingot_meteorite, ingot_meteorite_forged,
blade_meteorite (опыт 0) и ingot_steel_dusted (опыт 1, сохраняет степень) — тип `reinhardtshbm:hot_smelting` (SmeltingRecipe с heatUp в assemble);
раньше нагреть слиток было нечем. Степень ковки dusted-стали хранилась в damage, который в 1.21 без max_damage обнуляется (цепочка
до chainsteel не работала) — теперь компонент `reinhardtshbm:dusted_grade`; наковальня сравнивает предмет+степень без учёта нагрева
(ComparableStack); подсказка «Forged %s time(s)» (`item.hot_dusted.forged`, en/ja/pt_br/ru/uk/zh из .lang оригинала).
