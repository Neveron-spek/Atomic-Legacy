# Партия T2 — периферия РБМК и Redstone-over-Radio (ROR)

Эталон: HbmMods 1.7.10 `936c60c4`. Проверка: `work/verification-parity-t2.log`, диагностика `event/ParityT2Diagnostics.java` (площадка x = 30000, z = 30400).

| Объект (оригинал) | Было в порте | Сделано | Статус |
|---|---|---|---|
| IRORInteractive.getCommand/getParams | `split(sep, -1)`: «cmd!» давал параметр "", «a!b!» — ошибку | `split(sep)` как в 1.7.10 (хвостовые пустые части отбрасываются) | реализовано, проверено автоматически |
| IRORInteractive.parseInt/parseLong/parseBool | parseInt отвергал Infinity/NaN; parseLong/parseBool отсутствовали | parseInt = `(int) Math.round(Double)` как в оригинале; добавлены parseLong, parseBool | реализовано, проверено автоматически |
| RTTYSystem.broadcast (сумма чисел за тик) | `Math.addExact` — при переполнении заменял сигнал | обычное сложение long (переполнение как в Java 1.7.10) | реализовано, проверено автоматически |
| TileEntityRBMKRod IROR | нет | columnheat, rodheat, depletion, xenon, fastflux, slowflux, flux (формулы оригинала) | реализовано, проверено автоматически (без топлива) |
| TileEntityRBMKControl / ControlAuto IROR | нет | extraction = (int)(level·100) | реализовано, проверено автоматически |
| TileEntityRBMKControlManual IRORInteractive | нет | setrods!0..100, extendrods!-100..100 | реализовано, проверено автоматически (также через контроллер ROR) |
| TileEntityRBMKBoiler / Heater IROR | нет | feed/steam/consumption; in/out | реализовано, проверено автоматически |
| TileEntityProxyCombo колонн РБМК | любой муляж колонны вёл к ядру, любая деталь РБМК считалась ROR-машиной | прокси только верхний муляж стержней, ручных стержней, бойлера, нагревателя, охладителя, дегазатора, хранилища; через прокси колонна IRORInteractive (selfdestruct контроллера); остальные блоки РБМК — не ROR (читатель/контроллер не ставятся) | реализовано, проверено автоматически |
| rbmk_gauge (TileEntityRBMKGauge) | модель с выдуманными стрелками (тепло/поток/стержень/редстоун) | 4 стрелки: канал RTTY, polling, цвет, подпись, min/max, сглаживание 0.1, подписи min/max (getShortNumber > 10000), GUI 256×204 | реализовано, проверено автоматически (логика); GUI/рендер — ручная проверка |
| rbmk_key_pad | статичные кнопки | 4 кнопки: щелчок по четверти, polling-фиксация, таймер 7 тиков, звук random.click 1/0.75, GUI | то же |
| rbmk_lever | выдуманные значения | 2 рычага: FLIP_SPEED 0.1, команды вкл/выкл, polling, звуки block.leverStart/leverStop/spark, искры tau small ×5 + адрон, интерполяция клиента (3 шага) | то же |
| rbmk_indicator | выдуманные лампы | 6 ламп, диапазон и обратный диапазон, яркость 1/0.35, подписи без fullbright, GUI 256×258 (два блита фона) | то же |
| rbmk_numitron | пустые трубки | 2 дисплея, 7 разрядов (маска 0b01111111), сокращение чисел, ведущие нули, переполнение 9999999/-999999, текстура numitron_lights | то же |
| rbmk_graph | пустые экраны | 2 графика по 30 значений (каждые 10 тиков), фиксированные min/max (перестановка), линии и подписи | то же |
| rbmk_terminal | только модель | команды chan/start/stop/send/horse/selfdestruct/clear, 17 строк истории, повтор каждый тик, оранжевый цвет при повторе; IRORInteractive clear/write/set<n>/submit; экран без фона с подсказками 0.5× | реализовано, проверено автоматически (логика) |
| rbmk_display / rbmk_display_blank | клик открывал «пустое» меню | onBlockActivated → false (как Block); поворот отвёрткой уже был (M1) | реализовано |
| Отвёртка на панелях | — | IToolable.onScrew только SCREWDRIVER (не ручная дрель/дефузор); keypad/lever — только присев (иначе срабатывает блок); износ 1 | реализовано; ручная проверка |
| Компараторный выход lever/key_pad/gauge/numitron | был (придуман) | убран: в 1.7.10 у РБМК нет hasComparatorInputOverride | реализовано, проверено автоматически |
| Прочность РБМК | 5 / 10 у всех | RBMKBase/консоли/панели 3 / 30; autoloader, loader, steam_inlet/outlet 50 / 60 | реализовано, проверено автоматически |
| Рецепты 9 панелей + консоли | есть | сверены с CraftingManager 775–783, 989–990; rbmk_lever и rbmk_loader: CU.ingot() → тег `c:ingots/copper` (было `minecraft:copper_ingot`) | проверено автоматически |
| Подсказки панелей (addStandardInfo) | нет | `block.reinhardtshbm.<панель>.desc` из .lang (10 языков, где есть) | реализовано |
| Заголовки GUI container.rbmk* | нет | 6 ключей из .lang | реализовано |
| TileEntityGeiger IROR | нет | VAL:rad = ceil(ticker) | реализовано |
| Предметный рендер rbmk_indicator | лампы красные/жёлтые, fullbright | без окраски, обычный свет (renderAll) | реализовано; ручная проверка |
| GUI-текстуры, модели, звуки | GUI-текстур и звуков не было | 6 GUI-текстур и 8 ogg побайтно; 7 текстур моделей совпадают побайтно; OBJ отличаются только строкой `usemtl` | проверено автоматически |

Не перенесено в T2 (остаётся): IROR ~35 машин (бочки/цистерны, PUREX, PA, Watz, турели, пусковые, батареи, двигатели, сборочные/химические, спутниковые линкеры, котлы, нагреватели, сепаратор/счётчик/датчики сетей и др.); методы OpenComputers панелей (OC нет); тестовый канал «2012-08-06» (Song of Storms для radiorec); детальная сверка крановой консоли и rbmk_display-рендера.
