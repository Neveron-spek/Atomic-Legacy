# parity c1 — отсутствующее оружие (2026-11-17)

Эталон: HbmMods 1.7.10, коммит 936c60c (`work/original`). Статусы: «реализовано» — код перенесён; «авто» — проверено `ParityC1Diagnostics` на выделенном сервере (`checked_parity_c1_cases`); «ручная» — ожидает проверки в клиенте (клиент в этой среде не запускался).

## 1. B92

| Объект | Оригинал | Сделано | Статус |
|---|---|---|---|
| gun_b92 | `GunB92` (Item, не sedna): стак 1, редкость uncommon, зачаровываемость 1, +3.5 урона, действие bow 72000; ПКМ без заряда или со Shift — зарядка (анимация 30 тиков, на 15-м +1 заряд и звук `weapon.b92Reload` 2.0/0.9); 11-й заряд — сброс и Fleija 50 с радужным облаком; ПКМ с зарядом — натяжение, отпускание после 10 тиков — по лучу на заряд (разброс `min(i*0.2, 1)` по гауссу со 2-го луча), звук `weapon.sparkShoot` 5.0/1.0, заряд 0, анимация | `items/GunB92Item`; NBT `animation`/`energy` — те же ключи в CUSTOM_DATA; подсказка — те же строки | реализовано, авто (зарядка, шаги 15/30, перезаряд 11, выстрел 3 лучами, отказ до 10 тиков, значения анимации); звук/вид — ручная |
| gun_b92_ammo | `GunB92Cell`: стак 1; в инвентаре тянет из B92 по 1 заряду за тик, пока в пушке > 1, до 25; meta 1 = полная | `items/GunB92CellItem`; «meta 1» = `energy == 25` | реализовано, авто |
| weaponized_starblaster_cell | `WeaponizedCell`: брошенная — через 50 с или в огне исчезает и (при `dropStar`) Fleija 100 + радужное облако; частицы reddust/smoke/lava | `items/WeaponizedCellItem`, конфиг `droppedStarCellEffect` (`10.02_dropStar`, true) | реализовано, авто (таймер); взрыв и частицы — ручная |
| entity_beam_bomb | `EntityB92Beam`: 0.5×0.5, скорость 4.5, без гравитации, 100 тиков; блок — взрыв на следующий тик; сущность — взрыв каждый тик касания без удаления; вода — взрыв; взрыв = `statFacFleija(10)` + радужное облако | `entity/B92BeamEntity`, тип `entity_beam_bomb` | реализовано, авто (начало, курс, стена, срок жизни) |
| рендер луча | `RenderBeam5`: 9 вложенных труб 0…0.175 × 2, синий с белой сердцевиной, аддитивно | `render/B92BeamRenderer` (+ граница отсечения 2.02) | реализовано, ручная |
| рендер предмета | `ItemRenderGunAnim` + `ModelB92` (21 бокс, 64×64, `ModelB92SM.png`), наклон и ход затвора по анимации | `render/ItemRenderGunAnim`, слой `legacy_b92`; в инвентаре иконка, как в оригинале | реализовано, ручная |
| рецепты | gun_b92 `DDD/SSC/  R`; gun_b92_ammo `PSP/ESE/PSP`; rigged cell: бак перекиси + полная ячейка + медная проволока | 3 JSON; полная ячейка — `neoforge:components`, custom_data строкой `"{energy:25}"` (иначе число из JSON становится byte и не совпадает с int предмета) | реализовано, авто (в т.ч. отказ для 24 зарядов и пустой ячейки) |
| достижение `selenium` | `AchievementHandler`: крафт gun_b92 | таблица уже была, предмет появился | авто (таблица + рецепт); выдача в игре — ручная |
| метеорит «Star Blaster» | `Meteorite` case 12: взрыв 10 и gun_b92 с именем `§9Star Blaster§r` | `worldgen/MeteoriteGenerator` — добавлен предмет | реализовано, не проверено |
| иммунитет к ударной волне | `ExplosionNukeGeneric`: EntityB92Beam в списке исключений | `NukeExplosionManager.isLegacyExplosionExempt` | реализовано, не проверено |
| вкладка | weaponTab; порядок ModItems | gun_b92 первым из стволов; gun_b92_ammo после модов оружия; rigged cell в конце вкладки | реализовано, авто (порядок) |

## 2. Оружие силовой брони

| Объект | Оригинал | Сделано | Статус |
|---|---|---|---|
| gun_pa_melee | `XFactoryPA.ItemGunPA`, UTILITY, draw 10, прицел NONE, `Receiver(0)` без магазина, пустая подсказка; ЛКМ/ПКМ → компонент нагрудника | `XFactoryPA.createMelee` | реализовано, авто |
| gun_pa_ranged | то же, draw 0, прицел CROSS, плоская иконка full3D, без рендера | `XFactoryPA.createRanged`, модель `item/handheld` | реализовано, авто |
| условие брони | `IPAWeaponsProvider`: нагрудник RPA/NCRPA и полный комплект (`hasFSBArmorIgnoreCharge`); RPA — только ближний бой | `XFactoryPA.getMeleeComponentCommon/getRangedComponentCommon` по группе FSB нагрудника | реализовано, авто (без брони, неполный комплект, RPA, NCRPA) |
| ArmorRPAMelee | ЛКМ: 14 тиков, удары на 3 и 9 (15, ×2.5 при здоровье ≥ 100), повтор при удержании на 14; ПКМ: 20 тиков, шлепок на 8 (35, отбрасывание 1.5); DT −5/−15, пробитие 0.1/0.25; дальность 3 (+0.5 к хитбоксу); звук `weapon.fire.smack`; расчленение 1/10 (1/3) при убийстве | `XFactoryPA.ArmorRPAMelee` | реализовано, авто (тайминги, урон 37.5/37.5/87.5, повтор) |
| ArmorNCRPAMelee | ЛКМ: 25 тиков, удары на 5 и 15; ПКМ: 30 тиков, взмах на 5; те же уроны; звук `weapon.fire.stab`; расчленение при убийстве | `XFactoryPA.ArmorNCRPAMelee` | реализовано, авто |
| ArmorNCRPARanged | ЛКМ — управляемая ракета (`rocket_ncrpa_steer`, life 400), ПКМ — обычная (`rocket_ncrpa`); из инвентаря (MagazineBelt), урон 25, смещение ±0.25, кулдаун 10, звук `weapon.rpgShoot` 0.5; без ракет — щелчок | `XFactoryPA.ArmorNCRPARanged`; 10 типов снарядов `ROCKET_NCRPA_*` в конце `StandardAmmoType`, рендер RPZB | реализовано, авто (оба выстрела, расход, кулдаун, сухой щелчок) |
| анимации / вид от первого лица | `playAnim` (шины EQUIP, SWINGRIGHT/LEFT, SLAPTURN/SLAP, SWEEPTURN/SWEEPCUT), `renderFirstPerson` — руки брони | шины — в компонентах; отрисовка — `render/ItemRenderPAMelee` (руки remnant/ncrpa) | реализовано, ручная |
| конфиг | 7 значений ServerConfig | `HbmConfig.MELEE_*`, `RANGED_NCRPA_DAMAGE`, в таблице `/ntmserver` | реализовано, авто (значения по умолчанию) |
| рецепты | melee ` C /MWM`; ranged `C/W/P` | 2 JSON (`c:wires_dense/gold`, `c:ingots/plastic`) | реализовано, авто |
| вкладка | weaponTab после gun_drill | так же | реализовано, авто |

## 3. Попутно

| Объект | Оригинал | Сделано | Статус |
|---|---|---|---|
| Bobmazon «Maid's Cleaning Utensils» | `ItemKitCustom` 0x00ff00/0x008000: gun_m2, 5×64 BMG50_DU, gun_autoshotgun, 3×64 G12_MAGNUM, 2×64 G12_EXPLOSIVE; HIDDEN, 64 | `LegacyBobmazonOffers.maidKit` | реализовано, авто (порядок, цена) |
| Bobmazon «Frenchie's Reward» | `ItemKitNBT`: комплект RPA, gun_minigun_lacunae, 3×64 CAPACITOR_OVERCHARGE; HIDDEN, 32 | `LegacyBobmazonOffers.frenchieKit` | реализовано, авто |
| gun_autoshotgun_heretic | DEBUG (без вкладки и рецепта): 10ga, магазин 250, урон 100, delay 3, без износа, рендер Sexy с `sexy_heretic.png`, мод CHOKE | `XFactory10ga.createAutoshotgunHeretic`, `Lego.LAMBDA_NOWEAR_FIRE`, конфиг `GUN_HERETIC_DAMAGE` | реализовано, поведение не проверялось |
| gun_debug, ammo_debug | DEBUG: два ресивера, свой рендер `ItemRenderDebug`, свои конфиги пуль | — | не сделано (отладочные, без рецепта и вкладки) |
| gun_minigun_dual | DEBUG: два конфига, `ItemRenderMinigunDual` | — | не сделано (отладочный) |

## Расхождения и ограничения

- B92: счётчики анимации и заряда ведёт только сервер (в оригинале `onUpdate` шёл на обеих сторонах); `damageItem(1)` не перенесён — у предмета нет прочности, вызов ничего не делал; события Forge ArrowNock/ArrowLoose не публикуются; `MainRegistry.polaroidID` в порте нет — первая строка подсказки всегда «Stay away from me compootur!».
- Луч: «блока нет» хранится флагом, а не координатами (−1, −1, −1) — в 1.21 это настоящий блок; клиент только двигает луч, попадания считает сервер; затенение труб в оригинале зависело от состояния GL, здесь цвета без затенения.
- Ближний бой PA: `attackEntityFromNT(..., allowSpecialCancel = false)` выполнен общей функцией порта для физического урона sedna; нюанс игнорирования отмены события атаки не воспроизводится; `SkeletonCreator.composeEffectGib` в порте нет (как у прочего оружия).
- Конфиги `rocket_ncrpa*` создаются при первом выстреле (как ленты в оригинале), чтобы индексы `BulletConfig.configs` в сохранённых стволах не сдвигались.
- Тюнинг от первого лица при натяжении B92 (цепочка лука 1.7.10) воспроизведён по коду ванильного `ItemRenderer`, глазами не сверялся.

## Ресурсы (побайтные копии, переименования)

- `textures/models/ModelB92SM.png` → `textures/models/model_b92sm.png`
- `sounds/weapon/b92Reload.ogg` → `sounds/weapon/b92reload.ogg` (событие `weapon.b92reload`)
- `textures/models/weapons/sexy_heretic.png` — то же имя
- `gun_b92_ammo` теперь использует `gun_b92_ammo_alt.png`, rigged cell — `gun_b92_ammo_weaponized.png` (как `setTextureName` оригинала); `gun_pa_ranged.png`, `gun_b92.png` уже были и совпадают побайтно.
- Языки: ключи `gun_pa_melee`, `gun_pa_ranged`, `gun_autoshotgun_heretic` (en, de, ja, pt_br, ru, uk, zh), `gun_b92`, `gun_b92_ammo`, `weaponized_starblaster_cell` (de, fr, it, ja, pl, pt_br, uk; для ru — только rigged cell: названий B92 в ru_RU оригинала нет) — из lang-файлов оригинала.

## Изменения вне нового кода

- `build.gradle`: без изменений.
- Рецепты: 5 новых (`gun_b92`, `gun_b92_ammo`, `weaponized_starblaster_cell`, `gun_pa_melee`, `gun_pa_ranged`) — выполнен `updateRecipeBaseline`.
- `weaponized_starblaster_cell` убран из списков «выведенных» (`HbmItems.RETIRED_LEGACY_CATALOG_ITEM_IDS`, `LegacyHbmContent.RETIRED_LEGACY_ITEM_IDS`).
- `ParityR1Diagnostics`: фабрика реального стака для gun_b92_ammo; `GunModsG2Diagnostics`: heretic в списке CHOKE.
- `ParityS3Diagnostics`: ожидаемое число скрытых предложений Bobmazon 53 → 55. `ParityS2ConfigTable`: +`10.02_dropStar`, +8 серверных значений.
- Общие правки: `OrchestrasC2.getMouseOver(…, threshold)`, `HbmToolBehavior.gib` стал public, `XFactoryRocket` (типы и `ncrpaConfigs`), `GunBatchE1BulletRenderer` (рендер RPZB для ракет NCRPA), `XWeaponModManager` (CHOKE для heretic).

## Проверки

`work/verification-parity-c1.log`: assemble и все validate-задачи — успешно; `checkBuildingAudit` errors=0, `checked_parity_c1_cases=71`; `checkItemAudit` errors=0. Клиент не запускался.
