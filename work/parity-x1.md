# Parity X1 — кастомные машины (HbmMods 936c60c)

| Оригинал | Порт | Статус |
|---|---|---|
| CustomMachineConfigJSON (config/hbmConfig/hbmCustomMachines.json, writeDefault, readConfig, все поля) | config/CustomMachineConfigJSON (FMLCommonSetup) | перенесено; пример paperPress совпадает по структуре, значениям, порядку и форматированию JsonWriter; id блоков/предметов порта, ore dict → теги |
| CustomMachineRecipes + SerializableRecipe (hbmRecipes/hbmCustomMachines.json, шаблон _hbmCustomMachines.json, файл REMOVE UNDERSCORE…) | inventory/recipes/CustomMachineRecipes | перенесено; форматы item/dict/nbt, [id,count,meta,chance], [fluid,mB,pressure]; float пишется как double (Gson 2.2.4) |
| core recipe (CraftingManager.addRecipeAuto(recipeShape, recipeParts)) | CustomMachineControllerRecipe (тип reinhardtshbm:custom_machine_controller, data/recipe/custom_machine.json) + показ в JEI crafting | перенесено |
| BlockCustomMachine (light 1F, 5/10, мета-фасинг, GUI/wand_s, дропы без слотов 10–15, pick block) | blocks/CustomMachineBlock | перенесено; конфигурация в custom_data "machineType" вместо damage i+100 |
| ItemCustomMachine (подпредметы, имя по языку, INVALID MACHINE CONTROLLER) | items/CustomMachineItem + вкладка machine | перенесено |
| TileEntityCustomMachine (init, updateEntity, generatorMode, рецепт, pollution/radiation, flux от reactor_research, тепло, checkStructure/buildStructure, слоты, NBT) | tileentity/CustomMachineBlockEntity | перенесено; power/progress/heat/flux не сохраняются, как в оригинале |
| TilePort/setupAllPorts, updatePortPIFIFO/POFIFO, moduleSmokePorts | PowerEndpoint (порты = cm_port + контроллер, 6 сторон), HbmFluidNetUser.fluidPorts + subscribe, дым через те же порты | перенесено; при сломанной структуре машина выходит из сетей (в оригинале остаётся до смены сети) |
| BlockCMPort + TileEntityProxyCombo (cachedPosition) | CustomMachinePortBlock + CustomMachinePortBlockEntity (dummy-прокси) | перенесено |
| BlockCMGlass (cm_tank) | CustomMachineGlassBlock, модели cutout | перенесено (раньше рендерилось непрозрачно, грани между баками рисовались) |
| BlockCMAnchor | CustomMachineAnchorBlock | установка по взгляду (2..5) вместо грани клика |
| ItemCMStructure (structure_custommachine, CMstructureOutput.txt, подсказка, overlay) | items/CustomMachineStructureItem + look overlay | перенесено (раньше — чужой режим LegacyStructureExportToolItem) |
| ContainerMachineCustom / SlotPattern / ModulePatternMatcher | CustomMachineMenu, tileentity/ModulePatternMatcher | перенесено; стандартный режим — exact (DroneItemMatcher, у плоских предметов нет subtypes) |
| GUIMachineCustom (gui_custom.png) | CustomMachineScreen, текстура побайтно | перенесено |
| RenderCustomMachine + SmallBlockPronter | CustomMachineBlockEntityRenderer | перенесено; translucent RenderType пишет глубину (в оригинале depthMask false) |
| CustomMachineHandler (NEI, по категории на конфигурацию) | CustomMachineRecipeCategory (JEI) | перенесено; раскладка и drawExtras, тексты с тенью (GuiDraw) |
| CommandReloadRecipes → SerializableRecipe.initialize | /ntmreload → CustomMachineRecipes.initialize | перенесено |
| SerializableRecipePacket (enableServerRecipeSync, по умолчанию выкл.) | — | не перенесено (в порте нет системы hbmRecipes и ключа конфига) |

Ограничения при ошибках конфигурации: выход за массивы баков/слотов (в оригинале исключение) — рецепт пропускается; выходной бак другого типа ограничен ёмкостью.
