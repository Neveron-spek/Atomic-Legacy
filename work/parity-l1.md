# l1 — рендер-аудит вкладок (2026-11-19)

runItemRenderAudit -PitemRenderSheets=все 9 вкладок: 160 листов, 5620 позиций вкладок (6226 стеков в аудите). Листы просмотрены: building (21), machines (13), materials (выборочно 5 + скрипт), consumables (4), nuclear_weapons (1), control (2); остальные (rockets_missiles, turrets, templates, control, materials) — только скриптом (пустые ячейки, missing-texture #F800F8, равенство размеров GUI/земля). Missing-texture: 0. Лог клиента: ERROR/WARN atomiclegacy нет (кроме ложного «ore_volcano: no quads», исправлено; vine_phosphor «unused frames» — mcmeta побайтно оригинал).

| Вкладка | Что видно | Причина | Статус |
|---|---|---|---|
| building | bedrock_ore_new: 156 вариантов — одна серая иконка | не перенесены TextureAtlasSpriteMutatable (RGBMutatorInterpolatedComponentRemap по типу), оверлеи признаков и оттенок сорта | исправлено: 36 запечённых текстур textures/item/bedrock_ore_mutated (work/l1_bedrock_ore_textures.py), 156 моделей + свойство atomiclegacy:ore_variant, ItemColors слой 0 = grade.tint |
| building | bedrock_ore_fragment_* одинаково белые; bismuth — общая текстура | ItemAutogen(FRAGMENT) не запечён; textureOverrides bismuth | исправлено: 43 текстуры material/bedrock_ore_fragment_* (work/l1_fragment_textures.py, цвета Mats.java), bismuth → bedrock_ore_fragment_bismuth |
| building | ore_volcano — блок во всю строку | composite-модель без display | исправлено: parent block/block + particle |
| building | brick_jungle_glyph ×16, steel_wall, steel_corner, steel_poles, rebar, oil_spill — 1:1 блок во всех контекстах | модели без display | исправлено: parent block/block (геометрия = renderInventoryBlock/BlockLayering) |
| building | wood_barrier огромный | нет display, мировая модель вместо BlockBarrier#renderInventoryBlock | исправлено: модель предмета (столб+2 доски, поворот 90°) |
| building | plant_dead, 9 растений (foxglove…tall_cd4, reeds), stalactite/stalagmite, balefire, fire_digamma — крестовая модель 1:1 | в 1.7.10 renderType 1 / ISBRH без 3D → плоская иконка | исправлено: item/generated с текстурой блока (у сталактитов — 12 моделей legacy_flat) |
| building | spotlight_* (6), tesla, tape_recorder — одинаковый размер во всех контекстах | подобранные масштабы (0.82/0.85) в ObjMachineItemRenderer | исправлено: spotlight — RenderLight (Ry-90, ×1.5 поверх блочных поз), tape_recorder — RenderTapeBlock (y−0.5), tesla — ItemRenderBase (T(0,−3) S6, без cull) |
| building | vending_machine вне ячеек рук/земли/рамки | собственная поза ObjMachine | исправлено: ItemRenderBase RenderVendingMachine (T(0,−4) S6.25, Soda/Obamna) |
| machines | 9 мини-панелей RBMK: фронт с деталями в GUI, тёмный куб в остальных | в 1.7.10 RBMKMiniPanelBase#renderInventoryBlock — простой блок 0.25..1 текстурой rbmk_display | исправлено: модели 16×16×12 |
| machines | rbmk_console / rbmk_crane_console — тёмные прямоугольники вне GUI | подобранная поза | исправлено: ItemRenderBase из ItemRenderLibrary (группа JoyStick = Joystick, renderPart без учёта регистра) |
| materials | sawblade в GUI тёмный | вероятно освещение, геометрия/матрица как в оригинале | не исправлено (не подтверждено) |
| building | sliding_blast_door_2, sliding_gate_door | нет в эталоне 936c60c (лишний контент) | не трогал — согласовать удаление |
| building | plant_dead: 1 предмет вместо 5 (EnumDeadPlantType) | содержание, не рендер | не трогал |
| machines/building | 19 предметов с подобранной позой ObjMachine (transition_seal, boltgun, ammo_press, radar, radar_large, radgen, blast_furnace, sat_dock, skeleton_holder, chimney_brick/industrial, boiler, industrial_boiler, condenser_powered, tower_small/large, barrel_corroded, barrel_antimatter, boat) + RbmkComponentItemRenderer (loader, steam inlet/outlet, autoloader 0.78/0.52) | масштаб не зависит от контекста, не ItemRenderBase | не исправлено — отдельный этап (перенос в Y1/U4 Base) |

Технически: дубли IClientItemExtensions сняты (tesla → BlockItem, VendingMachineBlockItem и RbmkComponentBlockItem для консолей/мини-панелей без своего BEWLR); build.gradle validateRendererCullingBounds — удалены правила подобранных поз tesla/spotlight/tape_recorder/vending. OBJ скопированы побайтно: legacy_tesla, legacy_vending_machine, legacy_rbmk_console, legacy_rbmk_crane_console.

Проверки (work/verification-l1.log): assemble, validateJarIntegrity, validateObjModels, validateEntityRendererCullingBounds, validateRendererCullingBounds, checkItemAudit (errors=0) — успешно. runItemRenderAudit после правок: stacks=6226 errors=0 warnings=0; листы building/machines/materials/consumables пересняты, исправленные позиции просмотрены.
