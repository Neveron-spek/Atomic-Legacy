# Партия z1 — броня на игроке (2026-11-17)

Эталон: getArmorModel / getArmorTexture классов items/armor/*, ItemArmorMod#modRender, ModEventHandlerClient#onRenderArmorEvent, render/model/ModelArmor*.java, ModelRendererObj (HbmMods 936c60c). Рисование: render/LegacyFsbArmorLayer (таблица items/LegacyArmorBodyModels), render/LegacyAccessoryArmorLayer, render/T51ArmorLayer, render/TeslaBackLayer; игроки + зомби/скелеты/пиглины/стойка брони (в 1.7.10 — RenderPlayer/RenderBiped; стойка — техническая адаптация). Рука в 1-м лице: оригинал броню не рисует (renderFirstPersonArm) — порт тоже.

Общая ошибка ModelArmorBase-слоя (все OBJ-костюмы, no9, hat, ashglasses): из позы вычиталась текущая точка вращения части вместо постоянной originPoint (setRotationPoint конструктора) — при приседании/езде шлем, поножи и ботинки не смещались вместе с частями тела. Исправлено: translate(точка части) → поворот Z/Y/X → translate(−origin) → scale 1/16, как ModelRendererObj#render.

| Предмет | Оригинал | Было | Сделано | Статус |
|---|---|---|---|---|
| ajr_*, ajro_* | ModelArmorAJR / AJRO (AJR.obj, ajr_/ajro_ текстуры) | OBJ, ошибка origin | origin | реализовано, проверено автоматически |
| bismuth_* | ModelArmorBismuth (bismuth.png) | OBJ, ошибка origin | origin | то же |
| steamsuit_* | ArmorDesh → ModelArmorDesh (steamsuit.obj) | OBJ, ошибка origin | origin | то же |
| dieselsuit_* | ArmorDiesel → ModelArmorDiesel (bnuuy.obj, bnuuy_*) | OBJ, ошибка origin | origin | то же |
| hev_*, dns_* | ModelArmorHEV / ModelArmorDNT | OBJ, ошибка origin | origin | то же |
| rpa_* | ModelArmorRPA: Glow без освещения 240, вентилятор −currentTimeMillis/2 % 360° | вентилятор 9°/тик (180°/с вместо 500°/с), Glow с освещением | скорость по часам, Glow — unlit fullbright | то же |
| ncrpa_* | ModelArmorNCRPA: Eyes без освещения 240 | Eyes с освещением | unlit fullbright | то же |
| bj_helmet/plate/legs/boots | ModelArmorBJ type 0–3 | OBJ, ошибка origin | origin | то же |
| bj_plate_jetpack | ModelArmorBJ type 5: торс + Jetpack + руки | только Jetpack (торс и руки не рисовались) | торс, руки, Jetpack | то же |
| envsuit_* | ModelArmorEnvsuit: шлем с GL_BLEND, Lamps без текстуры цветом (1,1,0.8), 240 | шлем cutout, Lamps текстурой | шлем translucent, Lamps — цвет без текстуры | то же |
| fau_* | ModelArmorDigamma: Cassette и руки с GL_BLEND | cutout | translucent | то же |
| taurun_* | ModelArmorTaurun: ноги/ботинки −0.01/+0.01 по X | без сдвига | сдвиг | то же |
| trenchmaster_* | ModelArmorTrenchmaster: шлем GL_BLEND, Light 240 без освещения, сдвиг ног ±0.01 | шлем cutout, Light с освещением, без сдвига | всё по оригиналу | то же |
| t51_* | ModelArmorT51 | верно (свой слой, origin правильный) | не менялось | проверено автоматически (ресурсы) |
| no9 | ModelNo9: Helmet, Insignia; Flame без текстуры (1,1,0.8) 240 только при NBT isOn у игрока | Flame всегда, текстурой | Flame по isOn, цвет без текстуры | то же |
| no9 (фонарь) | ArmorNo9#onArmorTick (клиент): каждые 2 тика луч 50 блоков, lightUpRecursively (уровень min(15, 25−d·25/50), спад 1/блок, только непрозрачные стопорят), сброс через 5 тиков | отсутствовал | client/No9FlashlightClient + mixin-доступ к клиентскому хранилищу блочного света (setLightValue → setStoredLevel, updateLightByType → checkBlock) | реализовано, ожидает ручной проверки |
| HUD/Jetpack ON/OFF | PlayerInformPacket (угол экрана, ID_HUD 7 / ID_JETPACK 5, 1000 мс) | строка действий | PlayerInformPayload (те же id и время, зелёный/красный) | реализовано |
| hat | ModelHat: hat.obj renderAll, armor/hat.png | OBJ верно, ошибка origin (при приседании шляпа не опускалась с головой) | origin | реализовано, проверено автоматически |
| ashglasses | ModelGlasses: goggles.obj, armor/goggles.png | OBJ верно, ошибка origin | origin | реализовано, проверено автоматически |
| goggles | ArmorModel → ModelGoggles (бокс-модель 64×32), textures/models/Goggles.png | рисовался OBJ goggles.obj с armor/goggles.png (модель ashglasses) | бокс-модель ModelGoggles в LegacyAccessoryArmorLayer, текстура скопирована | реализовано, ожидает ручной проверки |
| gas_mask, gas_mask_m65/olde/mono, hazmat_helmet_red/grey, liquidator_helmet, attachment_mask(_mono) | ModelGasMask / ModelM65 | U4 | сверено, без изменений | верно |
| jetpack_fly/break/vector/boost, wings_murk/limp | JetpackBase.modRender → ModelJetPack / ModelArmorWings | U4 | сверено, без изменений | верно |
| back_tesla | ItemModTesla.modRender → ModelBackTesla (mod_tesla.obj) | TeslaBackLayer | сверено | верно |
| прочая броня (steel, titanium, alloy, cmb, paa, asbestos, security, cobalt, starmetal, robes, schrabidium, euphemium, dnt, zirconium, hazmat*, liquidator plate/legs/boots, jackt, jackt2, mask_rag, mask_piss, mask_of_infamy) | ItemArmor/ArmorFSB getArmorTexture armor/*.png | vanilla-слои textures/models/armor/*_layer_N | побайтно сверены с оригиналом (все совпадают) | верно |
| ModelArmorWingsPheo, ModelArmorTailPeep, ModelArmorWings 2/3, плащи | RenderAccessoryUtility — косметика для конкретных игроков (UUID/ник), не предметы | нет | не переносится (не предметы) | вне объёма |

Известные расхождения: блеск чар на OBJ-костюмах не рисуется (в 1.7.10 проход блеска вызывал render() модели, который сам перепривязывал текстуру брони — визуальный артефакт, не воспроизводится); GL_SMOOTH/GL_FLAT не переносится (в 1.21 всегда сглаживание по нормалям OBJ).

Ресурсы: textures/models/goggles.png ← textures/models/Goggles.png (регистр). OBJ не менялись (no9/t51/mod_tesla отличаются от оригинала только строками usemtl/mtllib).

Самопроверка: event/ParityZ1Diagnostics (checked_parity_z1_cases) — каждая строка LegacyArmorBodyModels: предмет зарегистрирован и это броня нужного слота, группа есть в OBJ, текстура есть; аксессуары U4/T-51/Tesla — предметы и ресурсы; любая прочая броня мода — текстуры vanilla-слоёв её материала.
