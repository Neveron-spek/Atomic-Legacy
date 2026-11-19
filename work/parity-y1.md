# Партия Y1 — заглушки рендера блоков/предметов (2026-11-17)

Эталон: ClientProxy (bindTileEntitySpecialRenderer, registerBlockHandler), ItemRenderLibrary, IItemRendererProvider (HbmMods 936c60c).
Поиск: work/tmp-y1/gap.py (getRenderType оригинала: -1/ISBRH против модели blockstate и BER порта), work/tmp-y1/itemgap.py (IItemRenderer-предметы оригинала против модели предмета порта + run-visual/item-model-shapes.tsv: isCustomRenderer=false).
OBJ — побайтные копии `models/entity/legacy_*.obj` (spikes: копия + строки mtllib/usemtl для загрузчика NeoForge).

## Исправлено

| Объект | Было в порте | Оригинал | Сделано |
|---|---|---|---|
| soyuz_capsule (блок) | BER рисовал cube_all `block/soyuz_capsule` (зелёный куб) | RenderCapsule: soyuz_lander «Capsule», (0.5,-0.25,0.5), Y −25°, Z 15°, cull | SoyuzCapsuleBlockEntityRenderer по оригиналу; blockstate → block/soyuz_capsule_empty (без геометрии; частицы — иконка soyuz_capsule вместо legacy_placeholder) |
| soyuz_capsule (предмет) | весь soyuz_lander (с парашютом Chute) с придуманными display | ItemRenderBase: inv translate(0,-4,0) scale 5; «Capsule» | Y1LegacyItemRenderers.SoyuzCapsule; удалены SoyuzCapsuleItemModel, soyuz_capsule_world.json, obj/block/soyuz_capsule.* |
| entity_soyuz_capsule | OBJ с mtl, без отсечения граней, блочный рендер-тип | RenderSoyuzCapsule: Capsule+Chute, cull | LegacyEntityObjMesh, entityCutout, NO_OVERLAY |
| machine_press, machine_epress, machine_large_turbine | запечённый OBJ, придуманные display | ItemRenderLibrary | ItemRenderBase-рендереры (части, сдвиги головы 0.5/1.5, турбина Body+Blades без cull) |
| machine_centrifuge, machine_gascent | то же | RenderCentrifuge#getRenderer | renderAll / «Centrifuge» |
| machine_wood_burner, machine_chemical_plant, machine_diesel, machine_intake | то же | Render*#getRenderer | по оригиналу (части Base/Slider/Spinner/Frame у хим. завода) |
| machine_wood_burner (текстура) | textures/block/machine_wood_burner.png отличалась от оригинала (36 пикселей) | models/machines/wood_burner.png | заменена побайтной копией (мир и предмет) |
| fluid_pump | orientable-куб, нет BER | RenderFluidPump (fluid_diode.obj, поворот по meta), ItemRenderBase | FluidPumpBlockEntityRenderer + предмет; текстура fluid_diode.png скопирована; частицы block_steel |
| spikes | cube_all | RenderSpikeBlock: OBJ blocks/spikes.obj с иконкой блока | OBJ-модель блока (translate 0.5,0,0.5), предмет — ванильная поза блока |
| pwr_block | cube_all с листом pwr_block_ct | IBlockCT: pwr_block / pwr_casing_port (meta 1), связь с pwr_block, pwr_controller | DecorativeCtmBlockModel: per-face текстуры |
| pile_block | cube_bottom_top | IBlockCT getFragments по meta и стороне | DecorativeCtmBlockModel: top/control_top, input, output, core, pile_block |

## Найдено, не исправлено

| Объект | Расхождение | Причина |
|---|---|---|
| crane_boxer/extractor/grabber/inserter/router/unboxer, crane_splitter, crane_partitioner | cube_all (верхняя текстура на всех гранях); в оригинале IBlockSideRotation: 6+13 иконок по входу/выходу, поворот UV; у splitter/partitioner — своя геометрия | нужна динамическая модель + не перенесён override выхода (отвёртка) |
| ore_bedrock | cube_all ore_bedrock_block; в оригинале 2 прохода: bedrock + ore_random_N (shape) с цветом TE | текстуры ore_random_* не перенесены, блок в порте не генерируется |
| wand_air/jigsaw/logic/loot/tandem | cube_all; в оригинале куб и внутренние грани (renderFromInside) | технические блоки без вкладки |
| machine_industrial_generator, machine_reactor | простой куб / блок совместимости | в порте нет машины (механика не перенесена) |
| 228 блоков с blockstate `block/empty` | частицы разрушения — legacy_placeholder (= текстура reinforced_stone) вместо иконки блока оригинала | массовая правка blockstate, вне объёма |
| soyuz_capsule meta 3 (ржавая текстура) | — | генерация капсулы на пляже (WorldConfig.capsuleStructure) не перенесена |
| Затенение OBJ-блоков (spikes) | shade_quads NeoForge против формулы ObjUtil ((ny+0.7)·0.9 −0.1|nx| +0.1|nz|, ≥0.45) | платформа; как у прочих OBJ-блоков порта |

radiorec, steel_beam/wall/corner/roof, boat, boxcar, machine_storage_drum — статические TESR оригинала, в порте — эквивалентная запечённая геометрия (проверена выборочно radiorec), не заглушки.
