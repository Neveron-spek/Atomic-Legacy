# parity g2 — ручная сверка отрисовки GUI (часть 01, 41 GUI эталона → 33 экрана порта)

Эталон: `work/original` (HbmMods 1.7.10 @ 936c60c), список `work/g-gui-part01`. Слоты не трогались (сверены в e2).
Общая правка хитбоксов: где порт использовал ванильный `isHovering` (на 1 px шире с каждой стороны) или `>=/<`, для областей
`drawCustomInfoStat` / `drawElectricityInfo` / `checkClick` поставлена проверка `GuiInfoContainer` (`x <= m < x+w`, `y < m <= y+h`, `K3GuiText.hover`).
Баки — `E2TankGui.renderTank/renderTankInfo` (FluidTank, нижняя кромка, ориентация 1 с зеркальным U).

| GUI эталона | Экран порта | Сверено | Расхождение | Сделано | Статус |
|---|---|---|---|---|---|
| GUIMachineRadGen | RadGenScreen | 12 полос, энергия, подсказки, заголовок | хитбоксы isHovering | хитбоксы | исправлено |
| GUIMachineRadarNT | RadarScreen | фон, панель, 6 переключателей, карта, развёртка, метки, подсказки, клики, клавиши 1–8 | подсказки 158/178 перепутаны; нет подсказки энергии; нет «глушения» (шум 5×5); цвет карты (знаковый byte, без clamp); развёртка — линия вместо градиентного четырёхугольника; метки/наведение со смещением 104/113 вместо 108/117 и без дробной позиции; подсказка метки «Altitude:» вместо «Alt.:» и у мыши; звук на клавишах 1–8; хитбоксы | всё приведено к эталону | исправлено |
| GUIMachineRadarNTSlots | RadarSlotsScreen | фон, энергия, заголовок | нет кнопки (5,5) «Switch View» и подсказки | кнопка + подсказка, RadarControlPayload action 7 из меню слотов открывает экран радара | исправлено |
| GUIMachineRockMill | RockMillScreen | энергия, прогресс, LED, иконка, призрак входа, баки, подсказки | энергия: свои ключи вместо HE; прогресс floor вместо ceil; баки плиткой без зеркала; призрак — иконка рецепта при 20 % вместо цикла входа + 50 % маски; счётчик на иконке | исправлено | исправлено |
| GUIMachineSatLinker | SatelliteLinkerScreen | панели, подсказки в фикс. позиции | хитбоксы | хитбоксы | исправлено |
| GUIMachineSiren | SirenScreen | фон, трек/тип/громкость | заголовок не по центру | центрирование | исправлено |
| GUIMachineStrandCaster | StrandCasterScreen | металл, баки, подсказки, заголовок | заголовок серый вместо белого; баки нарисованы на 24 px ниже (низ 62/113 вместо 38/89); металл — заливка вместо тонированной текстуры + аддитив 0.3; потолок 79 вместо 92; подсказка «пусто»/количество не Mats.formatAmount | исправлено | исправлено |
| GUIMachineSuperComputer | SupercomputerScreen | энергия, прогресс, LED, иконка, призраки, баки | баки на 16 px ниже; энергия своими ключами; нет звука клика селектора; призраки; счётчик на иконке | исправлено | исправлено |
| GUIMachineTurbineGas | GasTurbineScreen | кнопки, ползунок, табло, RPM, термометр, панели, баки, подсказки | баки заливкой; термометр от max(20,t); «C» без «°»; расход %.1f вместо double; подсказки инфо-панелей у мыши (эталон: guiLeft−8, guiTop+60); якорь ползунка не обновлялся при клике по кнопкам; только ЛКМ | исправлено | исправлено |
| GUIMassStorage | MassStorageScreen | шкала, подсказки, кнопки | хитбоксы кнопок, только ЛКМ | исправлено | исправлено |
| GUIOilburner | HeaterScreen (ветка oilburner) | бак, тепло, ползунок, зажигание, подсказки | бак плиткой, хитбоксы, только ЛКМ | исправлено только в ветке oilburner | исправлено |
| GUIOreSlopper | OreSlopperScreen | прогресс, энергия, индикатор, баки, заголовок −9 | энергия своими ключами; баки | исправлено | исправлено |
| GUIPlasticBag | LegacyHeldInventoryScreen | фон, «Inventory», без заголовка | — | — | совпадает |
| GUIPump | FlowControlPumpScreen | поле, 2 кнопки, подписи, пакет | — | — | совпадает |
| GUIRTTYDesignator | RttyDesignatorScreen | поле, кнопка, заголовок | поле без зелёного цвета и с рамкой; заголовок 0x404000; Enter сохранял; звук в мире; поле в фокусе сразу; клавиша инвентаря не закрывала | исправлено | исправлено |
| GUIRadioRec | RadioRecScreen | поле, 2 кнопки, подсказки | клавиша инвентаря закрывала при вводе; только ЛКМ; хитбоксы | исправлено | исправлено |
| GUISatDock | SatelliteDockScreen | панель, подсказка | хитбокс | исправлено | исправлено |
| GUIScreenBobble | BobbleheadScreen | плашка, строки, звук | центровка (int)(left+150−w/2) с дробным left; клавиша инвентаря жёстко E | исправлено | исправлено |
| GUIScreenBobmazon | BobmazonScreen | кнопки, страницы, описание предложения | раскладка не по эталону: лишние строки требования, подсказка предмета, кнопка «вперёд» с v=18 вместо 36, без подсветки навигации, номер страницы с тенью, позиции названия/цены/звёзд; нет «No Ratings», значка достижения; закрытие при смене предмета (нет в эталоне) | переписан render/клики по GUIScreenBobmazon | исправлено |
| GUIScreenClayTablet | ClayTabletScreen | весь экран | заглушка: всегда закрытые клетки | реализован выбор рецепта по tabletSeed, раскрытие 50/25 %, иконки луны/солнца, выход и подпись; порядок PedestalRecipes.recipeSets; meta 1 таблички | исправлено |
| GUIScreenDesignator | ManualDesignatorScreen | 22 кнопки, X/Z, подсказки | клавиша инвентаря не закрывала | исправлено | исправлено |
| GUIScreenGuide | GuideBookScreen | обложка, стрелки, тексты, заголовки, картинки, номера | перенос строк font.split вместо разбиения по пробелам; округление round вместо (int); центровка обложки/заголовка; rbmk title5 нарисован в масштабе 1/0.9 (lang «.scale» влияет только на отступ текста); картинки textures/items рисовались предметом; только ЛКМ; клавиша E жёстко | исправлено | исправлено |
| GUIScreenHolotape | HolotapeImageScreen | плашка, текст | нет звука block.bobble; перенос font.split вместо autoBreak; центровка; клавиша | исправлено | исправлено |
| GUIScreenPager | RttyPagerScreen | как Designator | те же 6 расхождений | исправлено | исправлено |
| GUIScreenRBMKDisplay/Gauge/Graph/Indicator/KeyPad/Lever/Terminal | RbmkPanelScreen | поля (позиции, длины), переключатели, тона звука, пакеты, заголовки, терминал | — | — | совпадает |
| GUIScreenRadioTelex | RadioTelexScreen | TX/RX буферы, курсор, осциллограф, 11 кнопок, подсказки | TX: § и код скрывались (в эталоне видны), управляющие символы окрашивались форматом; RX: без накопления смещения и сдвига −14; осциллограф — заливки 2 px вместо линий 3 px экрана с дробным смещением; подсказки без цветов; DEL сбрасывал курсор; клавиша инвентаря закрывала при вводе; только ЛКМ | исправлено | исправлено |
| GUIScreenRadioTorch / GUIScreenRadioTorchLogic | RadioTorchSignalScreen | поля, кнопки, условия, прокрутка, подсказки | — | — | совпадает |
| GUIScreenRadioTorchController / Reader | RttyConfigScreen | поле(я), кнопки, подсказки функций | индикатор polling из локальной копии; подсказка функций без проверки IRORInfo/IRORValueProvider; хитбоксы; только ЛКМ | исправлено | исправлено |
| GUIScreenSatCoord | SatelliteCoordScreen | 3 поля, подсветка, индикаторы, кнопка | — | — | совпадает |
| GUIScreenSnowglobe | SnowglobeScreen | плашка, строки | надпись без разбиения по «$» и autoBreak; центровка; клавиша | исправлено | исправлено |
| GUISoyuzCapsule | SoyuzCapsuleScreen | заголовок 115/0x7daf71 | — | — | совпадает |
| GUITapeDrive | TapeDriveScreen | белый заголовок | — | — | совпадает |
| GUIWeaponTable | WeaponTableScreen | превью, переключатель конфигурации | — | — | совпадает |

## Правки вне экранов
- `network/RadarControlPayload`: action 7 из `RadarSlotsMenu` открывает меню радара.
- `tileentity/LegacyPedestalRecipes`: `recipeSet(set)` в порядке регистрации эталона, `Recipe.displayInput/displayOutput/extraName`.
- `items/LegacyClayTabletItem`: meta через CUSTOM_DATA `meta`, `stack(item, meta)`, `hasSeed`; экран открывается по руке.
- `tileentity/DungeonSpawnerBlockEntity`, `LogicBlockEntity`: награда скелета — табличка meta 1 (как `new ItemStack(clay_tablet, 1, 1)`).
- `tileentity/RadioTorchBlockEntity.hasRttyInfo`.
- Новый `inventory/gui/LegacyTextWrap` (I18nClient.autoBreak/autoBreakWithParagraphs).
- Общие классы E2TankGui/K3GuiText не менялись. `HeaterScreen` и `LegacyHeldInventoryScreen` общие с g1: правки только в ветке oilburner, второй не менялся.

## Остаток / ограничения
- Polaroid-пасхалки (`polaroidID == 11` → «Reda» в заголовке радара) — в порте нет polaroidID.
- Bobmazon: `updateScreen`-закрытие для «SolsticeUnlimitd» не перенесено.
- Guide: `chemistry_template.png` и `canister_gasoline.png` отсутствуют и в эталоне (там была бы missing-текстура) — рисуется иконка предмета.
- RadioTelex: ширина `getCharWidth` 1.21 для управляющих символов может отличаться от 1.7.10 на 1–2 px.
- Описание рецепта в подсказке селектора (RockMill/Supercomputer) не сверялось (относится к RecipeSelector).
- В игре вручную не проверялось.
