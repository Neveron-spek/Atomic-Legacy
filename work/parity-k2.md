# Parity K2 — сверка мест «по памяти» с исходниками ванильного 1.7.10

Эталон HBM: HbmMods 1.7.10 936c60c (`work/original`). Ванильные классы прочитаны (не исполнялись, в проект не копировались), сверены только значения.

Источники:
- MCP 9.08 (Minecraft 1.7.10) — https://github.com/CodeMajorGeek/lwjgl3-mcp908 (`src/minecraft_server/.../world/biome/BiomeGenBase|Mutated|Forest|Savanna|Mesa|Hills|Taiga|Plains|Snow|Decorator.java`, `src/minecraft/.../client/renderer/RenderBlocks.java`, `ItemRenderer.java`). Форк меняет биндинг LWJGL3; GL-цепочки и формулы — стандартный декомпилят MCP.
- Forge 1.7.10 `BiomeDictionary` — https://github.com/MinecraftForge/MinecraftForge/blob/1.7.10/src/main/java/net/minecraftforge/common/BiomeDictionary.java

## 1. worldgen/LegacyBiomeTraits

Высоты: константы BiomeGenBase.Height, setHeight в определениях биомов, мутации: BiomeGenMutated (+0.1 / +0.2), Forest (flower: var +0.2), Savanna.Mutated (·0.5+0.3 / ·0.5+1.2), Mesa Bryce (без setHeight → 0.1/0.2), Snow spikes (+0.3/+0.4), Taiga Mega Spruce (без изменений), Hills M (копия).
Типы: registerVanillaBiomes; мутированные id+128 там не зарегистрированы → makeBestGuess (treesPerChunk, rainfall>0.85, temperature, topBlock).

| Биом 1.21 | Предок 1.7.10 | Было | Стало |
|---|---|---|---|
| eroded_badlands | Mesa (Bryce) 165 | 0.1/0.2, SANDY | 0.1/0.2, SANDY+PLAINS (guess: variation 0.2, не влажный → PLAINS; верх sand → SANDY) |
| old_growth_birch_forest | Birch Forest M 155 (BiomeGenMutated) | 0.2/0.4, FOREST; isForestClass=true | 0.2/0.4, без типов (treesPerChunk 0 → только HILLS); isForestClass=false (не instanceof BiomeGenForest) |
| windswept_savanna | Savanna M 163 | 0.3625/1.225, — | 0.3625/1.225, SPARSE (treesPerChunk 2) |
| flower_forest | Flower Forest 132 (BiomeGenForest тип 1) | 0.1/0.4, FOREST | совпадает |

Остальные 30 строк таблицы (высоты и отслеживаемые типы) совпадают. Последствия: на eroded_badlands теперь возможны dish, falloutshelter_house, water_pump (PLAINS) — как в 1.7.10; на old_growth_birch_forest нет «лисьей перчатки» из HbmWorldGen (instanceof BiomeGenForest). SPARSE у windswept_savanna на структуры не влияет (isFlatBiome требует variation ≤ 0.2).
Объединённые в 1.18 предки (Birch Forest Hills M, Savanna Plateau M, Extreme Hills+ M) — взят основной вариант; биомы без предка — по тегам, как прежде.

## 2. UV граней wand_*

RenderBlocks.renderFace{YNeg,YPos,ZNeg,ZPos,XNeg,XPos} (без AO, flipTexture/field_152631_f = false для renderStandardBlock): грани берут uvRotateBottom/Top/East/West/North/South → источник {0,1,3,2,5,4} в LegacyBlockRenderTables совпадает. Формулы UV для 6 граней × 4 поворотов сверены с CableModelGeometry.legacyUv численно (своя транскрипция, асимметричный бокс, привязка UV к позиции вершины): 0 расхождений. Правок нет.

## 3. B92 от первого лица

ItemRenderer.renderItemInFirstPerson (ветка предмета): трансляция взмаха только при getItemInUseCount()==0; смещение 0.7/−0.65−(1−equip)·0.6/−0.9 ×0.8, поворот 45°, повороты взмаха sin(f²π)·20 / sin(√fπ)·20 / ·80 и масштаб 0.4 всегда; ветка bow (−18/−12/−8, −0.9/0.2, f10, f11, дрожание, 0.1·f11, ∓335/∓50, scale z 1+0.2·f11). `ItemRenderGunAnim.applyFirstPersonHand` совпадает построчно. Правок нет. Зеркалирование для левой руки — адаптация (в 1.7.10 второй руки нет).

## 4. Прочие места «по памяти»

В work/parity-*.md других пометок нет (parity-j1 — ИИ глифидов, зона k1, не трогалось).

## Проверки

- event/ParityK2VanillaDiagnostics (подключён в BuildingPortDiagnostics, `checked_parity_k2_vanilla_cases`): 35 строк таблицы, класс BiomeGenForest, структуры на eroded_badlands / windswept_savanna / old_growth_birch_forest. Имя отличается от запрошенного: `ParityK2Diagnostics` уже занят прежним этапом K2 (радиация).
- work/verification-parity-k2.log: assemble, validateJarIntegrity, validateRegistrySources, checkBuildingAudit, checkItemAudit — BUILD SUCCESSFUL; building audit errors=0, checked_parity_k2_vanilla_cases=41; item audit errors=0 (warnings=16).
- Клиент не запускался; UV и B92 не менялись.
