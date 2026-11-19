# b3 — поиск и исправление багов (2026-11-17)

Источники: логи игры пользователя (PrismLauncher 1.21.1(2): latest.log, debug.log, 2026-11-13…17 *.log.gz, crash-reports), логи аудитов проекта (run-building-audit, run-item-audit, run-nuke-audit, run-nuke-real, run-recipe-audit*, run-visual), собственные запуски b3 (сервер: checkBuildingAudit/checkItemAudit; клиент: runItemRenderAudit), все validate*/check* задачи build.gradle, статический обзор.

## Исправлено

| # | баг | где | причина | статус | как проверить |
|---|---|---|---|---|---|
| 1 | ~3000 строк WARN «Unknown recipe category: reinhardtshbm:…» при каждом входе в мир | новый `client/HbmRecipeBookCategories.java` | для машинных типов рецептов не был зарегистрирован category finder книги рецептов | исправлено (UNKNOWN — то же, что давал vanilla-fallback); клиентский запуск b3: 0 строк | войти в мир, в latest.log нет «Unknown recipe category» |
| 2 | NPE «AtlasSet$AtlasEntry.atlas()» на каждый плазменный импульс (21 раз в логе 13.11), частица не рисовалась, «Error executing task on Client» | `client/particle/GunPlasmaPulseParticle.java` | атлас частиц брался через `Minecraft.getTextureAtlas(LOCATION_PARTICLES)` (ModelManager его не содержит) | исправлено (TextureManager → TextureAtlas.getSprite); скомпилировано, в игре не проверено | выстрел с plasmablast-импульсом (XFactory12ga spawnPulse): видно кольцо shockwave, в логе нет ошибок `gun_plasma_pulse` |
| 3 | ERROR «Couldn't load tag c:plates/advanced_alloy», из-за него не загружался весь тег `c:plates` (клиент и сервер) | `data/c/tags/item/plates/advanced_alloy.json` (удалён), `data/c/tags/item/plates.json` | тег ссылался на `plate_advanced_alloy`, которого нет в 1.7.10 (MainRegistry ignoreMappings) и в порте | исправлено; логи сервера и клиента b3: 0 | в логе нет «Couldn't load tag» |
| 4 | «reinhardtshbm-common.toml is not correct. Correcting» + «Incorrect key 03_nukes.3 / 06_explosions.6 / 11_tools.11 / 12_mobs.12» при каждом запуске; у этих ключей не было комментариев | `config/HbmParityV3Config.java` | ключи 1.7.10 с точкой («3.00_gadgetRadius»): ModConfigSpec.correct делит строковый путь по «.» | исправлено: точка в TOML-ключе пишется как «_» (`3_00_gadgetRadius`). Побочный эффект: значения ключей этих четырёх секций, изменённые в старом файле, вернутся к значениям по умолчанию | логи b3: 0 предупреждений |
| 5 | «Missing textures in model reinhardtshbm:fluid_pump#inventory» | `assets/.../models/block/fluid_pump.json` | модель-основа предмета ссылалась на несуществующие fluid_duct_solid(_o) | исправлено: block_steel (setBlockTextureName в 1.7.10); клиент b3: 0 | в логе нет Missing textures |
| 6 | QMAW: 48 триггеров справки по патронам пропускались | `client/qmaw/QmawLoader.java` | таблица ординалов EnumAmmo содержала 26 вариантов из 96, хотя все варианты ammo_standard восстановлены | исправлено: полная таблица из GunFactory.EnumAmmo; клиент b3: 415 триггеров (было 367), 0 пропущено | навести на патрон ammo_standard и открыть справку QMAW |
| 7 | В machine_tape_drive нельзя вставлять диски автоматикой | `tileentity/TapeDriveBlockEntity.java`, `event/ParityQ4Diagnostics.java` | 1.7.10: canInsertItem = isItemValidForSlot = `item == ModItems.drive`; порт (Q4) возвращал false, диагностика закрепляла ошибку | исправлено, диагностика переписана; checkBuildingAudit проходит | воронка на tape drive: диск входит, посторонний предмет — нет |
| 8 | WandConfigPayload: сохранение/загрузка структур wand-блока с любого расстояния и из незагруженного чанка | `network/WandConfigPayload.java` | не было TileEntityWandStructure.hasPermission (distSq ≤ 64) и isLoaded | исправлено; скомпилировано | — |
| 9 | flow_control_pump и network_separator (модели шире блока) рисовались только до 64 блоков | `render/FlowControlPumpBlockEntityRenderer.java`, `render/NetworkSeparatorBlockEntityRenderer.java` | обычный BlockEntityRenderer вместо LongRangeBlockEntityRenderer | исправлено; в игре не проверено | отойти дальше 64 блоков: модели видны |

## Правки проверок (build.gradle)

10 задач вне обычного набора падали из-за правил базы порта, противоречивших эталону 936c60c, или из-за удалённых файлов. После правок проходят все (work/verification-b3.log).

| задача | что было | правка |
|---|---|---|
| validatePortRegressionFixes | FileNotFound (SoyuzCapsuleItemModel.java, LegacyLauncherImpact.java удалены прежними партиями); фрагменты blast furnace заменены K3; рецепт halogen ждал тег `c:dusts/bromine` (в 1.7.10 — ModItems.powder_bromine) | source() сообщает об отсутствующем файле вместо падения; правила перенацелены на U4LegacyItemRenderSetup, LegacyMissileImpacts, ORIGINAL_SLOTS, powder_bromine |
| validateRendererCullingBounds | 3 новых рендера вне списков; `getCounterClockWise(` в ParticleAcceleratorBlockEntity (RFC: getRotation(DOWN)) | FluidPump в whitelist (OBJ ровно 1 блок); два рендера → LongRange (п. 9); поворот вынесен в `HbmPortDefs.rotDown`, поведение то же |
| validateBlueprintParity | считал secret-чертежи удалёнными | secret.* есть в 1.7.10 (POOL_PREFIX_SECRET; ass.50bmgbypass → secret.psalm; в творческой вкладке не показываются) |
| validateCinnabarProcessingParity | требовал ore_cinnabar в needs_stone_tool | в 1.7.10 у ore_cinnebar нет harvest level |
| validateCadmiumProcessingParity | требовал явный input_count=1 | поле необязательно; рецепт совпадает с 1.7.10 |
| validateModernOreGeology | запрещал токен depthRockBreaker | это флаг ItemToolAbility 1.7.10; глубинные блоки по-прежнему исключены |
| validateLatestUpstreamBehaviorParity | текстовые фрагменты Pwr/Zirnox/турель | обновлены под текущий код (сверено с 1.7.10) |
| validateLatestMachineCapabilityPorts | «deprecated Soyuz», «tape drive без автоматики» | soyuz_launcher/struct_soyuz_core есть в 1.7.10; tape drive — п. 7 |
| validateNuclearBlastParity | фрагмент формулы Solinium | обновлён (15000..250000) |
| validateLegacyGlobalGameplayParity | «retired firearm» (Bobmazon, ballistic_gauntlet, casing_bag), 22 muffled-контроллера | оружие восстановлено по 1.7.10; счётчик 24 |

Не запускались / не чинились: validateLatestUpstreamContent (падает: нет каталогов `..\..\HBM_1.7.10`, сверяет с другим коммитом e674541), validateLatestUpstreamMachineRecipes и validateLatestUpstreamRecipeExports (те же внешние каталоги), validateRuntimeRecipes*, validateItemAcquisition*, validateMachineRecipeModeMatrix, validateMachineRecipeCoverage, runNukeAudit. recipe-baseline не менялся.

## Записано (не исправлялось)

| баг | где | причина | кому |
|---|---|---|---|
| Arc welder и soldering station не показывают обрабатываемый предмет на машине | `render/ArcWelderBlockEntityRenderer.java`, `render/SolderingStationBlockEntityRenderer.java`; у BE нет синхронизации `display` | RenderArcWelder / RenderSolderingStation 1.7.10 рисуют `display` (EntityItem, масштаб 1.5) | сессия рендеров |
| В скрытом каталоге Bobmazon нет двух наборов: «Maid's Cleaning Utensils» (gun_m2, gun_autoshotgun, BMG50_DU, G12_MAGNUM/EXPLOSIVE) и «Frenchie's Reward» (RPA, gun_minigun_lacunae, CAPACITOR_OVERCHARGE) | `handler/LegacyBobmazonOffers.java` (комментарий «retired firearms») | остаток отказа базы порта от оружия; нужные предметы теперь зарегистрированы | b1 |
| casing_bag, ammo_bag, ammo_bag_infinite числятся в RETIRED-списках; рецептов casing_bag (CraftingManager:621–622) нет | `registry/HbmItems.java` RETIRED_LEGACY_CATALOG_ITEM_IDS, `registry/LegacyHbmContent.java` RETIRED_LEGACY_ITEM_IDS | то же; фактический статус регистрации не сверялся | b1 |

## Не баги / среда

- Крэши 11.11 «Invalid legacy skeleton-holder metadata 0» (LogicBlockEntity): в текущем коде этого исключения уже нет.
- 17.11 13:15 NoClassDefFoundError HbmLegacyJigsawGenerator$Component, «Missing legacy HBM structure NBT meteor-core.nbt», ZipException: JAR мода был заменён при запущенной игре (файл на месте).
- mixin «Error loading class dev/ryanhcode/sable/…»: опциональная совместимость с Sable (required=false).
- «Unused frames in sprite vine_phosphor_*: [5]»: mcmeta как в оригинале.
- «limits mip level» (aberrator 207×124, mp_w_15_he 72×72): исходные размеры текстур; атлас блоков остаётся с 4 mip-уровнями.
- «Received synced attachments from unknown block entity» (6): гонка синхронизации attachment muffled_machine с приходом чанка.
- «Can't keep up» (5 за сессию, 2–4 с): по логу причина не определяется.
- ~250 WARN «Legacy block … has no exact modern state» в run-item-audit: аудит намеренно перебирает метаданные.
- Статический обзор (ссылки на клиентские классы вне client-пакетов, ключи NBT save/load всех BE и entity, 59 серверных пакетов на проверку расстояния/меню, stillValid, quickMoveStack 129 меню и ghost-слоты, статические карты уровней, удаление dummy-блоков): других дефектов не найдено. Расхождения ключей NBT (например ZirnoxDestroyed «OnFire»/«fire») — намеренное повторение оригинала или чтение старых сохранений. Обзор эвристический, не построчный; деление на ноль, ConcurrentModification и циклы в сетях целенаправленно не искались.

## Проверки

- work/verification-b3.log: assemble + все статические validate*/check* (кроме перечисленных выше) — BUILD SUCCESSFUL; HBM_BUILDING_AUDIT_COMPLETE families=206 entries=727; HBM_ITEM_AUDIT_COMPLETE errors=0 warnings=14.
- work/b3-build3.log: обязательный набор — успешно.
- work/b3-render-audit.log: runItemRenderAudit stacks=6174 errors=0 warnings=1 — запуск с правками 1–5, 8, 9; после правок 6–7 клиент запускался только до загрузки ресурсов (work/b3-client-qmaw.log).
