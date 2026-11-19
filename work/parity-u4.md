# Партия U4 — рендереры предметов и модели брони (2026-11-17)

Эталон: ClientProxy#registerItemRenderer, ItemRenderLibrary, IItemRendererProvider (HbmMods 936c60c). Пересчёт: work/tmp-u4/gap.py по исходникам + item-model-shapes.tsv: 89 предметов с IItemRenderer рисовались в порте моделью без собственного рендерера; из них 7 уже имеют запечённую 3D-модель (machine_centrifuge, machine_gascent, machine_epress, machine_press, machine_large_turbine, soyuz_capsule — не трогались), conveyor_wand — блок-модель конвейера по типу (ItemRenderBlock = renderBlockAsItem, эквивалентно), red_pylon — плоский, добавлен (в model-gap.txt пропущен).

Общий подход: render/LegacyIItemRenderer (BEWLR: снятие ванильного размещения и цепочки 1.7.10/Forge для INVENTORY/ENTITY/EQUIPPED/EQUIPPED_FIRST_PERSON, хелперы ENTITY_ROTATION/BOBBING через перехват ItemEntityRenderer, isFull3D-ветка RenderPlayer, renderItemInFirstPerson через applyForgeHandTransform), render/LegacyItemModelWrapper (необработанные оригиналом контексты — исходная модель предмета), render/U4LegacyItemRenderSetup (регистрация).

| Предмет | Рендерер оригинала | Сделано |
|---|---|---|
| steel/titanium/alloy/cobalt/cobalt_decorated/starmetal/schrabidium/cmb/dnt_sword, desh_sword (ttp_low) | ItemRenderTransformer | иконка renderItemIn2D с параметрами ClientProxy в руке/1-м лице, плоская в слоте; свечение renderGlint3D/Flat при чарах; земля — ваниль (ENTITY не обрабатывался) |
| bismuth/volcanic/chlorophyte/mese _pickaxe/_axe | ItemRenderTransformer (tfp/sfp default) | то же |
| meteorite_sword_* (11) | ItemRendererMeteorSword(r,g,b) | иконка + постоянное цветное свечение (цвет×0.36), 3D и плоское |
| ingot_steel_dusted, ingot_chainsteel, ingot_meteorite, ingot_meteorite_forged, blade_meteorite | ItemRendererHot | слот: иконка + «_hot» с альфой heat/100 (статическое ItemHot.heat=100) |
| missile_* (29) | ItemRenderMissileGeneric | модель ракеты (LauncherMissileRenderer.renderAssembly, scale 1.5 tier2), преобразования и вращение в слоте по оригиналу |
| missile_custom | ItemRenderMissile | сборка MissileMultipart; без структуры — иконка |
| shimmer_sledge, shimmer_axe, stopsign, sopsign, chernobylsign | ItemRenderShim | OBJ (копии), текстуры |
| wood/lead/diamond/mese_gavel | ItemRenderGavel | OBJ gavel.obj, 4 текстуры |
| chainsaw | ItemRenderChainsaw | OBJ, бегущие зубья (при топливе), анимация взмаха SWING (IAnimatedItem добавлен в LegacyChainsawItem), отмена взмаха руки |
| detonator_laser | ItemRenderDetonatorLaser | OBJ, красные огни, жёлтая волна, мигающие красные цифры |
| big_sword / redstone_sword | ItemRenderBigSword / ItemRenderRedstoneSword | ModelBigSword / ModelSword (бокс-модели) |
| steel_roof | ItemRenderDecoBlock | ModelSteelRoof в руке/на земле; слот — иконка блока |
| boxcar, machine_industrial_generator, sawblade, ammo_himars | ItemRenderLibrary (ItemRenderBase) | OBJ boxcar/igen/sawmill Blade/turret_himars (вращение пилы и HIMARS в слоте, текстура по варианту) |
| red_pylon | RenderPylon (IItemRendererProvider) | OBJ pylon "Pylon" |
| conveyor_wand | ItemRenderBlock | не менялось: модель блока конвейера по типу |

Броня (render/LegacyAccessoryArmorLayer, игроки + гуманоидные мобы):

| Предмет | Оригинал | Сделано |
|---|---|---|
| gas_mask | ArmorGasMask → ModelGasMask, GasMask.png | бокс-модель на голове, ванильный слой скрыт |
| gas_mask_m65 / _olde / _mono | ModelM65 (+фильтр при наличии) | то же, текстуры ModelM65 / mask_olde / ModelM65Mono |
| hazmat_helmet_red / _grey | ArmorHazmatMask → ModelM65, ModelHazRed/Grey | то же |
| liquidator_helmet | ArmorLiquidatorMask → ModelM65 | то же |
| attachment_mask / _mono (мод шлема) | ItemModGasmask.modRender | M65 на голове |
| jetpack_fly/break/vector/boost (надет или мод нагрудника) | JetpackBase.modRender → ModelJetPack | бокс-модель на спине, 4 текстуры JetPack* |
| wings_murk / wings_limp | WingsMurk → ModelArmorWings (murk.obj) | крылья с анимацией взмаха/состояний по коду |

Преобразования ресурсов: регистр имён в нижний регистр (GasMask.png→gas_mask.png, ModelM65*.png→model_m65*.png, ModelHaz*→model_haz_*, JetPack*→jet_pack*, ModelBigSwordTexture→model_big_sword_texture, ModelSwordRedstone→model_sword_redstone); OBJ shimmer_* перенесены в models/weapons/, igen/sawmill/turret_himars — побайтные копии в models/entity/legacy_*.obj (существующие копии порта изменены под запечённые модели).
