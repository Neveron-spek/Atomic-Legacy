# Parity A1 — прямые начисления радиации, боеголовки TURBINE/CLOUD, звук бомбардировщика

Эталон: HbmMods 1.7.10 `936c60c`. Повторная сверка всех вызовов `contaminate / incrementRad / decrementRad / setRadiation / incrementRadiation / applyDigammaData` оригинала (≈90 мест) с портом после K2/J1.

## 1. Радиация существам и в чанк

| Объект | Оригинал | Было | Сделано | Статус |
|---|---|---|---|---|
| stone_porous (BlockPorous) | BlockStone: нет тиков и радиации; дроп булыжник, шёлк — камень | каждые 90–109 тиков −10 RAD чанка (придумано основой); дроп сам блок | поглощение удалено; лут-таблица: булыжник / шёлк → stone | исправлено, проверено автоматически |
| rad_absorber | decrementRad 2.5/10/100/10000 раз в 10 тиков | так же | — | верно, проверено |
| Corium, toxic/schrabidic/rad_lava, ToxicBlock | CREATIVE 1/1/1/5, корий + урон radiation 2 | так же (K2) | — | верно |
| gas_meltdown | contaminate CREATIVE 0.5 + эффект 60 с ур.2; incrementRad 5 | так же | — | верно |
| gas_radon / dense / tomb | RAD_BYPASS 0.05 / CREATIVE 0.5 / RAD_BYPASS 0.5 | так же | — | верно |
| BlockHazard/BlockHazardFalling/BlockOre/sellafield | 0.1×предмет раз в 20 тиков / sellafield 0.5·(meta+1) по случайным тикам | так же (K2 + правка 17.11) | — | верно |
| YellowBarrel | 5 / 0.5 / 35 | так же | — | верно |
| StorageDrum, ZIRNOX, DemonLamp, обломки RBMK, LogicBlock | radiate с исходными дальностями/типами | LegacyRadiate (K2) | — | верно |
| Переполнение бочки / FT_VentRadiation | onFluidRelease → overflow·radPerMB | так же | — | верно |
| RBMK (стержень без крышки, лучи) | flux·0.05, ·(1−hits/h) | так же | — | верно |
| Watz / исследовательский реактор / аннигилятор / DFC / CustomMachine | 1000 / heat/max·50 и 50 / min(rad·5,1000) / 100 только без взрыва / ± radiationAmount | так же; лишний аргумент max игнорируется | — | верно |
| Мини-нюки (ExplosionNukeSmall), мина, гранаты, катапульта | 50/(d+1) целочисленно ×radMod; 50F/(d+1)×1/1.5/0.25 | так же | — | верно |
| MK5, MK3 солиниум, Amat, CrashedBomb | см. K2 | так же | — | верно |
| EntityMist / EntityChemical | CREATIVE radPerMB·5; чанк ·2 / ·5 | так же | — | верно |
| Ядерный крипер, НЛО, RADBeast | 0.25 / 5 / +100 в чанк | так же | — | верно (RADBeast: источник урона с атакующим — область a2) |
| IWeaponAbility RADIATION, Folly, FEL, копьё/пламя дигаммы, balefire/blackFire | 15/50/500, 150, 25 и 0.1 DRX, 0.05/10, 5 | так же | — | верно |
| Эффекты/предметы (Radaway, таблетки, сигарета, энергетики, кофе, вставки, Shackles, кварц, медаль, decon) | исходные числа | так же | — | верно |
| Выброшенные предметы | HazardTypeRadiation.updateEntity пуст | загрязнение отключено (J1) | — | верно |
| Биомы кратеров (craterBiome 5 / inner 25 / outer 0.5 RAD/с, вода ×5) | EntityEffectHandler по биому | нет (биомы кратеров не перенесены) | не сделано: вне объёма, есть согласованная замена — активный sellafield | расхождение (известное) |
| EntitySiegeCraft (5) | осадные сущности | нет в порте | — | вне объёма |

## 2. Боеголовки

| Объект | Оригинал | Было | Сделано | Статус |
|---|---|---|---|---|
| TURBINE (mp_warhead_15_turbine) | ExplosionLarge 10 + (int)strength пуль EntityBulletBaseNT(TURBINE): урон 100–150 revolverBullet (снаряд), скорость 0.5 бл/тик, без гравитации, 200 тиков, ломает блоки с твёрдостью ≤120 без дропа, без рикошета/пробития, рендер лезвия blade_titanium | LegacyShrapnelEntity | новая сущность `entity_bullet_mk3` (LegacyTurbineBladeEntity: логика EntityThrowableNT/EntityBulletBaseNT для этого конфига), рендер STYLE_BLADE, тип урона revolver_bullet (+is_projectile, death.attack.revolverBullet в 9 языках) | реализовано, проверено автоматически; вид — вручную |
| CLOUD | spawnPoisonCloud(750, 2.5, 2): серверные EntityPinkCloudFX, отслеживаются (1000) и рисуются MultiCloudRenderer — **видимы** | уже настоящие entity_pink_cloud_fx (R1) | — (вывод P1 «невидимо» ошибочен) | верно |

## 3. Звук бомбардировщика

| Объект | Оригинал | Было | Сделано | Статус |
|---|---|---|---|---|
| EntityBomber loop | AudioDynamic keepAlive 20, категория sounds.json "player", громкость 2·(1−d/250), без отсечки по дистанции | 10 тиков, HOSTILE, звук останавливался за 250 бл | 20, PLAYERS, без отсечки (громкость ≥0) | исправлено; на слух — вручную |
