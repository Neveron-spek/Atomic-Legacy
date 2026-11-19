# Parity B1 — недостающее содержимое (2026-11-17)

Метод: `event/ParityB1Diagnostics` выгружает реестры запущенного сервера порта (блок/предмет/сущность/BE/меню/жидкость/эффект/частица/команда/достижение + класс) в `run-building-audit/parity-b1-registry.tsv`; `work/parity_b1_registry.py` разбирает `ModBlocks`, `ModItems` (+ фабрики оружия), `EntityMappings`, `TileMappings`, `Fluids`, команды, GUI/контейнеры, пакеты, частицы, достижения оригинала (936c60c) и сравнивает по id и по классу (`work/parity-b1-registry.json`). `work/parity_b1_mentions.py` — какие классы оригинала ни разу не упомянуты в порте. Снимок реестра до правок: `work/parity-b1-registry-before.tsv`.

Счёт оригинал / порт: блоки 980 / 1038, предметы 1814 / 3434 (метаданные развёрнуты), сущности 149 / 110 (29 ракет = одна сущность), TE 377 / 225 типов (общие BE), жидкости 156 / 156, эффекты 11 / 11, достижения 61 / 62, команды 13 / 8 ntm-команд + /rhbm.
По id не найдено: 36 блоков, 80 предметов. Обычный `Block` вместо особого класса: 41 блок; обычный `Item` вместо особого: 10 предметов. Ниже разобрано всё.

## 1. Нет функции при наличии объекта (класс-пустышка)

| Объект | Класс оригинала | Состояние в порте | Итог |
|---|---|---|---|
| ore_uranium, ore_uranium_scorched, ore_gneiss_uranium(_scorched), ore_nether_uranium(_scorched) (+ deepslate-варианты порта) | BlockOutgas(rock, true, 5, true), газ gas_radon | обычный Block: радон не выделялся ни случайным тиком, ни при добыче | сделано: `blocks/RadonOutgassingOreBlock` (случайный тик — радон в одну из 6 сторон; добыча без шёлкового касания — радон на месте блока) |
| ore_nether_smoldering | BlockSmolder | обычный Block: нет поджога идущих по блоку и частиц | сделано: `blocks/SmolderingOreBlock` (setFire(3), lava+flame над блоком) |
| inf_water, inf_water_mk2 | ItemInfiniteFluid(WATER, 50/500) | регистрировались только при конфиге `enableInfiniteWaterTankRecipes` (по умолчанию false) — в игре отсутствовали, стартовый набор выдавал пустоту | сделано: регистрируются всегда; рецепты — если не 528 (как `CraftingManager`), ключ конфига оставлен как дополнительный выключатель рецептов, по умолчанию true |
| block_* (32 шт.), block_white_phosphorus | BlockBeaconable / BlockHazard.makeBeaconable | обычный Block + тег `minecraft:beacon_base_blocks` (64 записи) | расхождения нет |
| cluster_iron/titanium/aluminium/copper | BlockCluster (дроп) | Block + таблица добычи | расхождения нет |
| ore_tikite | BlockDragonProof | Block + тег `dragon_immune` | расхождения нет |
| barricade | BlockNoDrop | Block без таблицы добычи | расхождения нет |
| ntm_dirt, block_slag, lightstone, machine_transformer, teleanchor, sellafield_slaked/bedrock, BlockPillar ×7, BlockDecoCT ×7, platemetal, block_insulator | только текстуры/иконки (без поведения) | Block + модели | расхождения нет (вид — область сессии рендеров) |
| ore_alexandrite | BlockDepthOre | Block | глубинная добыча исключена пользователем — не трогалось |
| coupling_tool, bismuth_tool | ItemCouplingTool / ItemAmatExtractor — пустые классы | Item | расхождения нет |
| glowing_stew, balefire_scrambled, balefire_and_ham, ingot_smore | ItemSoup(6) / ItemFood | Item с FoodProperties | не сверялось в этой партии |
| nugget_mercury, crystal_* (4) | ItemCustomLore | Item | только подсказка/редкость; не сверялось |

## 2. Отсутствующие предметы

| Объект | Класс оригинала | Состояние в порте | Итог |
|---|---|---|---|
| ammo_bag, ammo_bag_infinite | ItemAmmoBag (8 слотов, GUIAmmoBag, чтение магазинами sedna) | «выведены основой», ветки сумки вырезаны из MagazineBelt / MagazineSingleTypeBase | сделано: `items/LegacyAmmoBagItem`, `LegacyAmmoBagMenu`, `LegacyAmmoBagScreen`, ветки сумки возвращены в оба магазина, рецепты ×2, вкладка consumable |
| casing_bag | ItemCasingBag (15 слотов, pushCasing, IMagazine.handleAmmoBag) | отсутствовал, casingItem/casingAmount у BulletConfig не было | сделано: тот же класс (Kind.CASING_BAG), `IMagazine.handleAmmoBag`, таблица `LegacyCasingTable` (72 патрона, сгенерирована `work/parity_b1_casings.py` из фабрик оригинала с учётом clone()), рецепты ×2 |
| fusion_shield_tungsten/desh/chlorophyte/vaporwave | ItemFusionShield (реликт старого термояда, нигде не расходуется, рецептов нет) | текстуры/модели/en-ключи были, предметов не было | сделано: `items/LegacyFusionShieldItem` (счётчик damage, 2 строки подсказки, полоска), вкладка control; в ru_RU оригинала названий нет |
| gun_b92, gun_b92_ammo, weaponized_starblaster_cell (+ entity_beam_bomb) | GunB92, GunB92Cell, WeaponizedCell, EntityB92Beam | нет; текстуры есть; достижение `selenium` (крафт gun_b92) недостижимо | не сделано: старая (не sedna) пушка с собственной сущностью-лучом и рендером, ~900 строк — не уместилось. Приоритет 1 |
| gun_pa_melee, gun_pa_ranged | XFactoryPA.ItemGunPA (крафтятся; логика в IPAMelee/IPARanged брони RPA/NCRPA) | нет | не сделано: нужна подсистема PA-оружия брони (анимации, удары, компонент брони). Приоритет 2 |
| bacteria (AGAR, ANTHRAX_INERT, ANTHRAX_DEAD, ANTHRAX_MUTATED) + chem.agar, chem.growinertanthrax, chem.growmutatedanthrax, chem.mutateanthrax | ItemEnumMulti + ChemicalPlantRecipes | нет (K3D: NOT_PORTED) | не сделано: chem.mutateanthrax требует ChanceOutputMulti в рецепте химзавода (кодек, BE химзавода и химфабрики, экраны выбора, JEI); цепочка в оригинале незавершённая (бактерии никуда не идут). Приоритет 3 |
| dirt (DirtType.SWAMP), briquette PEAT, рецепт rock.swamp | BlockEnumMulti, ItemEnumMulti, RockMillRecipes | нет | не сделано: единственный источник ANTHRAX_INERT; блок в оригинале не генерируется (только креатив). Вместе с бактериями |
| gun_debug, ammo_debug, gun_autoshotgun_heretic, gun_minigun_dual | ItemGunBaseNT, WeaponQuality.DEBUG (без вкладки и рецептов) | нет | не сделано (отладочные) |
| drive, satellite, orbital_assembly, pile_rod, circuit, scraps, mold, drillbit, casing, piston_set, pa_coil, arc_electrode(_burnt), chemical_dye, part_generic, item_expensive, item_secret, ingot_metal, parts_legendary, plant_item, powder_ash, coke, briquette, chunk_ore, bedrock_ore_fragment, fuel_additive, grenade_shell/filling/fuze/extra, page_of_, achievement_icon, circuit_star_piece/component, scrap_plastic, weapon_mod_test/generic/special/caliber, ingot_raw, plate_cast, plate_welded, shell, pipe, bolt, wire_dense, wire_fine, part_barrel_*/receiver_*/mechanism/stock/grip | предметы с метаданными / ItemAutogen | развёрнуты в отдельные id (`<id>_<вариант>`, `wire_<материал>`) | расхождения нет (техническая адаптация) |
| nugget_raw | ItemAutogen(NUGGET) | нет развёрнутых id | не проверено: в оригинале NUGGET-автоген есть только у материалов без собственного самородка; требуется сверка списка материалов |
| cinnebar, crystal_cinnebar, ore_cinnebar | Item / BlockOre | переименованы в cinnabar (опечатка оригинала) | решение основы, не трогалось |
| ingot_copper, block_copper | Item / BlockBeaconable | заменены ванильной медью | решение основы, не трогалось |
| rtty_card, oc_cable_paintable | только при моде OpenComputers | нет | не применимо |
| customShape, remap | не предметы (артефакты разбора) | — | — |

## 3. Отсутствующие блоки

| Объект | Класс оригинала | Состояние в порте | Итог |
|---|---|---|---|
| machine_difurnace_off/on, machine_difurnace_extension | MachineDiFurnace(+Extension), TileEntityDiFurnace, GUIDiFurnace (@Deprecated, вкладка machineTab, рецепта нет) | нет | не сделано: три устаревшие машины без рецептов получения. Приоритет 4 |
| machine_difurnace_rtg_off/on | MachineDiFurnaceRTG (@Deprecated, machineTab) | нет | не сделано, то же |
| machine_rtg_furnace_off/on | MachineRtgFurnace (@Deprecated, без вкладки) | нет | не сделано, то же |
| machine_battery_potato, machine_battery, machine_lithium_battery, machine_schrabidium_battery, machine_dineutronium_battery | MachineBattery (@Deprecated, вкладка null) | нет | не сделано: скрытые устаревшие блоки |
| capacitor_gold/niobium/tantalium/schrabidate | MachineCapacitor (@Deprecated, вкладка null) | нет | не сделано: скрытые устаревшие блоки |
| pile_device | BlockPileDevice (метаданные) | pile_loader / pile_control / pile_vent | расхождения нет |
| hev_battery | HEVBattery | hev_battery_block (id занят предметом) | расхождения нет |
| stone_resource, plant_flower, plant_tall | блоки с метаданными | stone_resource_*, plant_* | расхождения нет; варианты CD0–CD4 (plant_flower/plant_tall, «needsOil») не сверялись |
| dirt | BlockEnumMulti(DirtType.SWAMP) | нет | см. раздел 2 |
| ore_depth_cinnebar/zirconium/borax, cluster_depth_iron/titanium/tungsten, ore_depth_nether_neodymium, depth_dnt + 6 исключённых | BlockDepthOre / BlockDepth | нет | исключено пользователем |
| event_tester, obj_tester | тестовые | нет | не нужны |

## 4. Рецепты

| Объект | Оригинал | Состояние в порте | Итог |
|---|---|---|---|
| chem.ticl4 | ChemicalPlantRecipes: 4 ильменита + кокс + 500 хлора → 4000 TiCl4, 60 т, 500 HE | не было (K3D: NOT_PORTED), хотя жидкость и chunk_ore_ilmenite уже есть | сделано: `recipe/chemical_plant/ticl4.json` |
| пиролиз TiCl4 | PyroOvenRecipes: 1000 TiCl4 → 125 хлора + 5 ingot_titanium, 60 т | не было | сделано: `recipe/pyrolysis/titanium_tetrachloride.json` |
| вращающаяся печь TiCl4 | RotaryFurnaceRecipes: 1000 TiCl4 + 1 флюс → титан INGOT.q(4), 40 т, пар 100 | не было | сделано: `recipe/rotary_furnace/titanium_titanium_tetrachloride_flux.json` |
| rock.swamp, rock.sculkgrass, rock.lightsculkgrass | RockMillRecipes | нет | не сделано (см. dirt); sculkgrass — только при стороннем моде |

## 5. Сущности, команды, пакеты, прочее

| Объект | Класс оригинала | Состояние в порте | Итог |
|---|---|---|---|
| entity_missile_* (29) | EntityMissile* | одна `launcher_missile` с таблицей | область a1, не трогалось |
| entity_beam_bomb | EntityB92Beam | нет | вместе с gun_b92 |
| entity_waste_pearl, entity_ntm_siege_laser, entity_nuclear_fog, entity_minecart_test | зарегистрированы, но никем не создаются | нет | не нужны (мёртвый код оригинала) |
| entity_moonstone_blast | EntityCloudTom (облако удара Тома) | удар Тома реализован `TomImpactEvents`/`TomImpactClientEffects` без этой сущности | не сверялось |
| entity_falling_block_nt | EntityFallingBlockNT (BlockFallingNT, наковальни) | HbmFallingBlock / ванильное падение | не сверялось |
| entity_cloud_fleija, entity_fallout, entity_nuke_mk3/mk5, entity_emp_blast, entity_raging_vortex, entity_zeta, entity_balefire, entity_tom_bust и др. | логические сущности взрывов | менеджеры взрывов и объединённые сущности (сверены прежними партиями) | область a1 / прежние партии |
| /ntmpackets | CommandPacketInfo | нет | не применимо (сеть 1.21) |
| /ntmclient, /ntmwikirender, dumpthreadsandcrashgame | клиентские/отладочные | нет на сервере | не сделано (отладочные) |
| пакеты (25) | PacketDispatcher | payload-ы порта (другая сетевая модель) | поштучно не сверялось |
| TE (377 → 225 типов), GUI (222), контейнеры (178), частицы (55) | — | объединены в общие BE/экраны; все блоки с TE оригинала имеют в порте класс с поведением (группировка «класс порта ← классы оригинала» в `parity-b1-registry.json`) | по именам сравнение ненадёжно; поштучно не сверялось |
| зелья (11), зачарования (0), жидкости (156), достижения (61) | — | 11 / 0 / 156 / 62 | полнота совпадает; условия достижений не сверялись |

## Изменения вне кода

- `build.gradle`: из `retiredDirectIds` (validateRecipeJsonSchemas) убран `ammo_bag`; в validateLegacyGlobalGameplayParity обновлена ожидаемая строка HbmItems и убраны запреты на `casing_bag_*.json` / `models/item/casing_bag.json`.
- Новые рецепты: 7 (`ammo_bag_leather`, `ammo_bag_rubber`, `casing_bag_leather`, `casing_bag_rubber`, `chemical_plant/ticl4`, `pyrolysis/titanium_tetrachloride`, `rotary_furnace/titanium_titanium_tetrachloride_flux`) — выполнен `updateRecipeBaseline`.
- Ресурсы: `textures/gui/gui_ammo_bag.png`, `gui_casing_bag.png` — побайтные копии; `models/item/casing_bag.json`; языковые ключи en_us (casing_bag, названия контейнеров) и ru_ru (сумки, контейнеры, inf_water) — из lang-файлов оригинала.
- Диагностики прежних партий: `ParityK3DDiagnostics.NOT_PORTED` — убран chem.ticl4; `ParityL3Diagnostics.EXPECTED_PYRO_STATIC` — добавлена строка TiCl4.
- zh_cn: три ключа (casing_bag, названия контейнеров) из zh_CN.lang оригинала — требование validateLangFiles.
- Вкладки: состав по `creative-tabs.properties` (ammo_bag/casing_bag — consumable, fusion_shield_*/inf_water* — control); порядок внутри вкладки для новых предметов не совпадает с порядком ModItems.
- Идентификаторы: содержимое сумок хранится в `custom_data` (`legacy_bag`, счётчики гильз `legacy_bag_casings` с ключом по id предмета вместо `unlocalizedName@meta`).

## Известные неточности сделанного

- Условие оригинала «патрон без NBT» для слота сумки заменено проверкой предмета (ammo_standard / ammo_secret): вариант патрона в порте хранится в компоненте.
- Таблица гильз ключуется типом патрона; конфиги порта, чей тип не совпадает с EnumAmmo оригинала (кроме CloneBulletConfig, наследующих базовый патрон), гильз не дают.
- Сумка патронов читается только из инвентаря игрока (как и в оригинале — `InventoryPlayer`).

## Проверка

`work/verification-b1.log`: assemble, validateJarIntegrity, validateLangFiles, validateRecipeJsonSchemas, validateRecipeSemantics, validateRegistrySources, validateObjModels, validateRecipeBaseline (6034 рецепта, после updateRecipeBaseline), checkBuildingAudit, checkItemAudit — BUILD SUCCESSFUL; оба аудита errors=0; `checked_parity_b1_cases=210`.
Самопроверка `ParityB1Diagnostics` (сервер): классы и случайный тик урановых руд, радон при тике и добыче; класс/свет тлеющей руды; регистрация inf_water и связь рецептов с режимом 528; загрузка 7 новых рецептов и значения chem.ticl4; виды/размер стопки/фильтр слотов/хранение/полоска сумок; счёт и расход патронов MagazineBelt из сумки и из бесконечной сумки (FakePlayer); pushCasing (две половины → одна гильза); таблица гильз (72 строки, типы и предметы существуют; 97 из 159 конфигов порта получают гильзу); параметры и полоска fusion_shield.
Клиент не запускался: экраны сумок, вид предметов, частицы тлеющей руды, вкладки — ожидают ручной проверки. Перезарядка MagazineSingleTypeBase из сумки и наполнение сумки гильз при реальной стрельбе в игре автоматически не проверены (проверены составные части).
