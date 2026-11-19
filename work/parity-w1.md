# Parity W1 — аирдроп C-130 (2026-11-17)

| Оригинал 1.7.10 | Порт | Статус |
|---|---|---|
| EntityMappings `entity_c130`, 1000, velocity | HbmEntityTypes.LEGACY_C130, 8×4, clientTrackingRange 63 | реализовано, диагностика |
| TrackerUtil.setTrackingRange(c130, 250) | LegacyC130Entity.setTrackingRange + broadcastToPlayer (квадрат ±250 по X/Z, как EntityTrackerEntry) | реализовано; потолок 1.21 — дальность обзора сервера |
| EntityPlaneBase: 50 HP (DataWatcher 17), 200 тиков, движение motion, rotation(), падение −0.025 + gasfire ×10, ExplosionVNT 15 makeStandard, planeCrash/planeShotDown 25, нет урона от nuclearBlast, canBeCollidedWith = HP>0, интерполяция клиента turnProgress | LegacyC130Entity | реализовано, диагностика (кроме взрыва при падении) |
| HP из NBT не восстанавливается (поле health = 50, DataWatcher перезаписывается) | так же | сохранено как в оригинале |
| ForgeChunkManager: принудительная загрузка своего чанка | HbmChunkTickets.ARTILLERY_PROJECTILES, координаты тикета в NBT (техн.) | реализовано |
| EntityC130.fac: случайный курс, скорость 2, старт за 200 блоков, высота +100 | fac() | диагностика |
| Сброс на тике 100 (lifetime/2) при HP>0: pos − motion·7, Y −10; SUPPLIES 5×POOL_SUPPLIES; WEAPONS 1–2×POOL_WEAPONS + 6×POOL_AMMO | tick() + HbmStructureLoot.randomStack | диагностика |
| Звук hbm:entity.bomberLoop: громкость 2, дальность 250, keepAlive 20, категория player | C130ClientSounds (entity.bomber_loop, PLAYERS, AudioDynamic-формула громкости) | ожидает ручной проверки |
| EntityParachuteCrate: motionY −0.02 пока > −0.2 (итог −0.22), posY ≤ 600, первый не-воздух → crate_supply на Y+1 + items.addAll, огнестойкий | LegacyParachuteCrateEntity | диагностика |
| XFactory40mm LAMBDA_SPAWN_C130_SUPPLIESS/WEAPONS: тик 40, techBleep у стрелка, getHeightValue, loadAndSpawnEntityInWorld (5×5 чанков) | XFactory40mm.spawnPlane, LegacyC130Entity.loadAndSpawn; высота — Heightmap MOTION_BLOCKING (конвенция порта) | диагностика |
| RenderC130: c130.obj Plane + Prop1–4 (вращение currentTimeMillis·15 вокруг (10, 4.2, z)), c130_0.png | LegacyC130EntityRenderer, models/entity/c130_*.json | ожидает ручной проверки |
| RenderParachuteCrate: conservecrate.obj (crate_can) + Chute из soyuz_lander.obj (soyuz_chute), качание ±5° вокруг (0,7,0) по getTotalWorldTime | LegacyParachuteCrateEntityRenderer | ожидает ручной проверки |
| ignoreFrustumCheck / isInRangeToRenderDist = true | shouldRenderAtSqrDistance = true; фрустум — EntityRenderBounds (радиус по OBJ) | validateEntityRendererCullingBounds |

Ресурсы: c130_0.png — побайтно; c130.obj — оригинал + строки `mtllib c130.mtl`/`usemtl c130` (как bomber_dornier.obj); c130.mtl — новый (map_Kd). conservecrate.obj, soyuz_lander.obj, crate_can.png, soyuz_chute.png — уже были в порте.

Расхождения: дальность отслеживания 1.21 ограничена view distance сервера (круг), в 1.7.10 override 250 обходил этот потолок; RNG ItemPool не тратит nextInt при min==max (распределение то же). Взрыв при крушении в диагностике не проверялся (разрушает мир).
