# [parity e3] Подсказки и свойства предметов

Эталон: HBM 1.7.10 `936c60c`. Статус: **реализовано, не собрано и не запущено** (gradle в этой части не запускался).
Инструменты: `work/parity_e3_tooltips.py` (таблица свойств, эвристика литералов `--literals`), `work/parity_e3_lang.py`
(перенос lang-ключей, список — `work/parity-e3-lang-extra.json`).

## Счётчики

| Что | Число |
|---|---|
| Предметов `ModItems.java`, разобранных скриптом / есть в порте по id | 1540 / 1531 |
| Строк таблицы `legacy/reinhardtshbm/parity-e3-items.tsv` (stack, damage, container, rarity, effect, desc) | 1531 |
| Классов эталона с `addInformation` | 334 |
| Из них с литералами/ключами, не найденными в порте (нестрогое сравнение начала строки; до правок → после) | 42 → 20 |
| Классов/предметов порта с исправленной подсказкой | 23 класса порта (см. ниже) |
| Добавлено lang-ключей (дословно из `.lang` эталона, 9 языков) | 478 (en_us 23, ru_ru 69) |
| Исправлено значений lang | 2 (`ammo_arty.nuke_symbol`, `nuke_note_2`: буквальные `\\u2620`, `'`) |

Свойства (B) статически по регистрациям порта не сверяются (значения спрятаны за фабриками `HbmItems`): расхождения
устраняются таблицей при загрузке и считаются диагностикой на сервере (число исправлений — строка лога
`[parity e3] N item default components set from the 1.7.10 ModItems table`).

## Исправлено

| Область | Было | Стало (как в 1.7.10) |
|---|---|---|
| Свойства всех предметов `ModItems` | не сверялись централизованно | `items/LegacyItemParity`: stack size, редкость, постоянный glint из таблицы применяются к default components (`ModifyDefaultComponentsEvent`); durability и container только проверяются |
| Цвет строк без кода цвета (все предметы мода) | белые (нестилизованный `Component`) | серые, как в 1.7.10 (`LegacyTooltips.onItemTooltipDefaultColor`, приоритет LOWEST) |
| `ItemCustomLore` `.desc` | не было 1 en / 42 ru (и др. языки) ключей | перенесены |
| `ItemDrop` (singularity*, black_hole, pellet_antimatter, crystal_xen, beta) | нет красной `[Dangerous Drop]`; pellet_antimatter — 1 красная строка | 2 серые строки + `[trait.drop]` |
| `cell_antimatter`, `cell_anti_schrabidium` | нет подсказки ItemDrop | 2 строки + `[trait.drop]` (статическая таблица `LegacyTooltips`) |
| `ItemPileRod` (7) | придуманные тексты `tooltip.*.pile_rod.*` | `desc.item.pileRod` + `<item>.desc`, разбиение по `$` |
| `ItemRTGPellet` (10) | свои ключи heat/decays/remaining | `.desc`, `desc.item.rtgHeat`, `desc.item.rtgDecay`, процент, при F3+H «EXTENDED INFO» (ticks, `ticksToDate`) |
| `ItemPACoil` | перефразировано, лишняя строка с названием | литералы, значения белым (RESET) |
| `ItemFusionCore`, `ItemToolBox`, `ItemColtanCompass`, `ItemPowerNetTool`, `ItemWiring`, `ItemCassette`, `ItemStarterKit` | перефразировано / другие цвета / лишние строки (координаты цели компаса) | литералы и цвета эталона; списки наборов ItemStarterKit как в эталоне |
| `ItemSettingsTool` | перефразировано, список скопированных настроек в подсказке | 3 литерала, синим имя источника / красным ` None ` |
| `ItemFluidIDMulti` | usage-строки, primary/secondary, свойства жидкости | `info`, имя, `info2`, имя |
| `ItemConveyorWand` | всегда 5 строк, перефразированный `vertical`, «selecting» | за LSHIFT: `item.conveyor_wand.desc`, `vertical.desc` (только обычный); иначе «Hold <LSHIFT> …» |
| `ItemDigamma` | перефразировано, `3.3 mDRX/s` | `trait.hlParticle`/`hlPlayer`, `[Digamma Radiation]`, `333.3mDRX/s`, `[Dangerous Drop]` |
| `ItemWand` (wand_k), `ItemWandS`, `ItemStructure*` (5) | перефразировано | литералы эталона (сохранённый блок — по registry id) |
| `ItemScraps` | жёлтым, имя материала, «additive» всегда | серым `formatAmount` (LSHIFT → mB), тёмно-красное «Additive, not castable!» только для расплава |
| `ItemGlitch` | LIGHT_PURPLE | серый литерал |
| `MachineGasFlare` (блок) | перефразировано | литералы, значения красным |
| `ArmorDNT` (dns_*), `ArmorBJJetpack`, `ArmorTrenchmaster` | только общий список ArmorFSB | свои строки (эффекты построчно, rocketBoots/fastFall/sprintBoost; `+ electricJetpack`/`+ glider`; moreAmmo) |
| `ItemMachineUpgrade` (сделано до обрыва, проверено на целостность) | придуманные тексты по типам | литералы эталона только для radius/health/smelter/shredder/centrifuge/crystallizer/screm/nullifier/gc_speed |
| Блоки (сделано до обрыва) | — | статические подсказки deco_pipe*, cable_diode, red_barrel/pink/lox/taint, gravel_diamond, ore_australium, nuke_prototype, red_connector*, red_pylon*, substation, `item_expensive`, `rag` |

## Не исправлено

| Что | Причина |
|---|---|
| Варианты `MainRegistry.polaroidID` (`.desc.P11`, ItemGlitch/ItemPolaroid — строка по ID, GunB92, ItemAMSCore, ItemCanteen, ItemEnergy, WeaponSpecial) | в порте нет polaroidID (ранее зафиксированное решение) |
| `ItemMachineUpgrade`: строки `IUpgradeInfoProvider` при открытом GUI машины | зависит от GUI машин (область e2) |
| `ItemStructureTool`: зелёная строка `enableDebugMode` | в порте нет опции (по умолчанию false) |
| `ItemBedrockOreBase`, `ItemAmmoArty`, прочие подсказки на ключах `tooltip.reinhardtshbm.*` с тем же английским текстом | текст совпадает, но это переводимые ключи вместо литералов; не переделывались. Всего в lang 492 ключа `tooltip.reinhardtshbm.*` — полная построчная сверка каждого не выполнена, эвристика ловит только отсутствующие начала строк |
| `MachineBattery` (устаревший блок), `BlockWandLoot`, `BlockWandLogic`, `ItemWandD`, `ItemRTTYCard` | не проверялись (устаревшее/отладочное, возможно область d2) |
| Предметы из `ModItemsArmor.java` и объявленные многострочно (`ItemSimpleConsumable` и т.п.) | в таблицу свойств не попали (скрипт читает однострочные регистрации `ModItems.java`) |
| Durability и container item | не принудительно: только отчёт диагностики |
| Расплющенные варианты (`<id>_<variant>`) мета-предметов | таблица сопоставляет только совпадающие id |

## Самопроверка

`com.reinhardt.hbm.event.ParityE3TooltipDiagnostics#run(MinecraftServer, List<String>)` (package-private, не подключён):
свойства по таблице, `.desc` у `LegacyLoreItem`, статические подсказки, наличие lang-ключей. Подсказки, зависящие
от NBT/Shift, и цвета — только вручную.
