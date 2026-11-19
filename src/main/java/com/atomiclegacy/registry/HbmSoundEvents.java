package com.atomiclegacy.registry;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, AtomicLegacy.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_GREASEGUN_FIRE = sound("weapon.fire.greasegun");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PISTOL_FIRE = sound("weapon.fire.pistol");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_UZI_FIRE = sound("weapon.fire.uzi");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_ASSAULT_FIRE = sound("weapon.fire.assault");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SWITCHMODE1 = sound("weapon.switchmode1");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SWITCHMODE2 = sound("weapon.switchmode2");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_MAG_SMALL_REMOVE = sound("weapon.reload.magsmallremove");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_REVOLVER_CLOSE = sound("weapon.reload.revolverclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_IMPACT = sound("weapon.reload.impact");
    // [weapons batch A: sounds]
    /** NTMSounds GUN_AMAT_FIRE, GUN_STARF_FIRE, GUN_BOLT_OPEN, GUN_BOLT_CLOSE (hbm:weapon.fire.amat, .pistolLight, .reload.boltOpen/boltClose). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_AMAT_FIRE = sound("weapon.fire.amat");
    /** [weapons batch D2] NTMSounds GUN_RIFLE_SILENCER (hbm:weapon.fire.silenced) and GUN_AMAT_SILENCER (hbm:weapon.silencerShoot). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_RIFLE_SILENCER = sound("weapon.fire.silenced");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_AMAT_SILENCER = sound("weapon.silencershoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_STARF_FIRE = sound("weapon.fire.pistollight");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_BOLT_OPEN = sound("weapon.reload.boltopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_BOLT_CLOSE = sound("weapon.reload.boltclose");
    /** hbm:weapon.explosionTiny (ExplosionEffectTiny of Lego.tinyExplode). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_EXPLOSION_TINY = sound("weapon.explosiontiny");
    // [weapons batch B: sounds]
    // [weapons batch C1: sounds]
    /** NTMSounds.GUN_POWDER_FIRE (hbm:weapon.fire.blackPowder) */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_POWDER_FIRE = sound("weapon.fire.blackpowder");
    /** NTMSounds.GUN_REVOLVER_SPIN (hbm:weapon.reload.revolverSpin) */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_REVOLVER_SPIN = sound("weapon.reload.revolverspin");
    /** NTMSounds GUN_RIFLE_FIRE, GUN_HEAVY_REVOLVER_FIRE, GUN_LEVER_COCK, GUN_SMACK (hbm:weapon.fire.rifle, weapon.44Shoot, weapon.reload.leverCock, weapon.fire.smack). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_RIFLE_FIRE = sound("weapon.fire.rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_HEAVY_REVOLVER_FIRE = sound("weapon.44shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_LEVER_COCK = sound("weapon.reload.levercock");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SMACK = sound("weapon.fire.smack");
    /** [weapon mods G2: sounds] NTMSounds.GUN_STAB_A_FUCKER (hbm:weapon.fire.stab), bayonet stab. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_STAB_A_FUCKER = sound("weapon.fire.stab");
    /** NTMSounds GUN_GO_GO_GADGET_FUCK_EVERYTHING_IN_THIS_GENERAL_DIRECTION (hbm:alarm.trainHorn). */
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_TRAIN_HORN = sound("alarm.trainhorn");
    // [weapons batch C2: sounds]
    // [weapons batch C3: sounds]
    /** NTMSounds GUN_POWDER_FIRE (hbm:weapon.fire.blackPowder). */
    /** NTMSounds GUN_MINIGUN_FIRE (hbm:weapon.calShoot). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_MINIGUN_FIRE = sound("weapon.calshoot");
    /** NTMSounds GUN_LASER_GATLING_FIRE (hbm:weapon.fire.laserGatling). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_LASER_GATLING_FIRE = sound("weapon.fire.lasergatling");
    /** [weapons batch E2] NTMSounds GUN_TESLA_FIRE, GUN_LASER_PISTOL_FIRE, GUN_LASER_RIFLE_FIRE (hbm:weapon.fire.tesla, weapon.fire.laserPistol, weapon.fire.laser). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_TESLA_FIRE = sound("weapon.fire.tesla");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_LASER_PISTOL_FIRE = sound("weapon.fire.laserpistol");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_LASER_RIFLE_FIRE = sound("weapon.fire.laser");
    /** [weapons batch F2] NTMSounds GUN_COIL_FIRE / GUN_COIL_RELOAD (hbm:weapon.coilgunShoot / coilgunReload), GUN_FATMAN_FIRE / GUN_FATMAN_RELOAD (weapon.fire.fatman / weapon.reload.fatmanFull), GUN_TAU_FIRE / GUN_TAU_STOPFIRE / GUN_TAU_LOOP (weapon.fire.tau / tauRelease / tauLoop). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_COIL_FIRE = sound("weapon.coilgunshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_COIL_RELOAD = sound("weapon.coilgunreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_FATMAN_FIRE = sound("weapon.fire.fatman");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_FATMAN_RELOAD = sound("weapon.reload.fatmanfull");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_TAU_FIRE = sound("weapon.fire.tau");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_TAU_STOPFIRE = sound("weapon.fire.taurelease");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_TAU_LOOP = sound("weapon.fire.tauloop");
    /** NTMSounds GUN_HEAVY_RIFLE_FIRE (hbm:weapon.fire.rifleHeavy). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_HEAVY_RIFLE_FIRE = sound("weapon.fire.rifleheavy");
    /** NTMSounds GUN_REVOLVER_SPIN (hbm:weapon.reload.revolverSpin). */
    /** NTMSounds GUN_RIFLE_COCK (hbm:weapon.reload.rifleCock). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_RIFLE_COCK = sound("weapon.reload.riflecock");
    // [weapons batch C4: sounds]
    /** NTMSounds GUN_SHOTGUN_FIRE, GUN_LIBERATOR_FIRE, GUN_SHOTGUN_LOAD (GUN_LEVER_COCK above) (hbm:weapon.fire.shotgun, .fire.shotgunAlt, .reload.leverCock, .reload.shotgunReload). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOTGUN_FIRE = sound("weapon.fire.shotgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_LIBERATOR_FIRE = sound("weapon.fire.shotgunalt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOTGUN_LOAD = sound("weapon.reload.shotgunreload");
    // [weapons batch D3: sounds]
    /** NTMSounds GUN_CANISTER_INSERT, GUN_GRENADE_RELOAD/OPEN/CLOSE, GUN_UNDERBARREL_FIRE, GUN_CONGO_FIRE, GUN_MK108_FIRE. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_CANISTER_INSERT = sound("weapon.reload.insertcanister");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_GRENADE_RELOAD = sound("weapon.glreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_GRENADE_OPEN = sound("weapon.glopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_GRENADE_CLOSE = sound("weapon.glclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_UNDERBARREL_FIRE = sound("weapon.hkshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_CONGO_FIRE = sound("weapon.glshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_MK108_FIRE = sound("weapon.fire.mk108");
    /** ItemCrucible deploy and swing (hbm:weapon.cDeploy / hbm:weapon.cSwing). */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_DEPLOY = sound("weapon.cdeploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_SWING = sound("weapon.cswing");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_LATCH_OPEN = sound("weapon.reload.openlatch");
    // [weapons batch F1: sounds] NTMSounds GUN_FLAMER_LOOP (hbm:weapon.fire.flameLoop), GUN_VALVE (hbm:weapon.reload.pressureValve).
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_FLAMER_LOOP = sound("weapon.fire.flameloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_VALVE = sound("weapon.reload.pressurevalve");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_DRY_FIRE = sound("weapon.reload.dryfireclick");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PISTOL_COCK = sound("weapon.reload.pistolcock");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_MAG_REMOVE = sound("weapon.reload.magremove");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_MAG_INSERT = sound("weapon.reload.maginsert");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_REVOLVER_COCK = sound("weapon.reload.revolvercock");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_MAG_SMALL_INSERT = sound("weapon.reload.magsmallinsert");
    // [weapons batch D1: sounds]
    /** NTMSounds GUN_SPAS_FIRE, GUN_SHREDDER_FIRE, GUN_SHREDDER_CYCLE (hbm:weapon.shotgunShoot, weapon.fire.shotgunAuto, weapon.fire.shredderCycle). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SPAS_FIRE = sound("weapon.shotgunshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHREDDER_FIRE = sound("weapon.fire.shotgunauto");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHREDDER_CYCLE = sound("weapon.fire.shreddercycle");
    /** NTMSounds GUN_SHOTGUN_COCK, GUN_SHOTGUN_OPEN, GUN_SHOTGUN_CLOSE, GUN_WHACK (hbm:weapon.reload.shotgunCock/CockOpen/CockClose, weapon.foley.gunWhack). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOTGUN_COCK = sound("weapon.reload.shotguncock");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOTGUN_OPEN = sound("weapon.reload.shotguncockopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOTGUN_CLOSE = sound("weapon.reload.shotguncockclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_WHACK = sound("weapon.foley.gunwhack");
    /** NTMSounds GUN_SOLDIER_TF2_BOAT_EXE_WAV_MP3 (hbm:weapon.boat) and EntityDuchessGambit impact (hbm:alarm.gambit). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_BOAT = sound("weapon.boat");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_GAMBIT = sound("alarm.gambit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_MOTOR = sound("block.motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_START = sound("block.assembler_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_STOP = sound("block.assembler_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_STRIKE = sound("block.assembler_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_CUT = sound("block.assembler_cut");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEMICAL_PLANT_OPERATE = sound("block.chemical_plant");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER = sound("block.boiler");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_GROAN = sound("block.boiler_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_BREAK = sound("block.crate_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROADCAST_1 = sound("block.broadcast1");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROADCAST_2 = sound("block.broadcast2");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROADCAST_3 = sound("block.broadcast3");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOBBLE = sound("block.bobble");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLUSHIE_SQUEAK = sound("block.squeaky_toy");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNDUN_HOWL = sound("block.hunduns_magnificent_howl");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_NEAR_SINGLE = sound("block.horn_near_single");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_FAR_SINGLE = sound("block.horn_far_single");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_NEAR_DUAL = sound("block.horn_near_dual");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_FAR_DUAL = sound("block.horn_far_dual");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRESS_OPERATE = sound("block.pressoperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> PYRO_OVEN_OPERATE = sound("block.pyro_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENTRIFUGE_OPERATE = sound("block.centrifugeoperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIPE_PLACED = sound("block.pipeplaced");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLATEMETAL_PLACE = sound("block.platemetal_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLATEMETAL_STEP = sound("step.platemetal");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRATE_STEP = sound("step.metal_block");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLESH = sound("block.flesh"); // [parity V4: step sounds] ModSoundTypes.flesh
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_ENGINE_OPERATE = sound("block.steam_engine_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE_LOOP = sound("block.engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBOFAN_OPERATE = sound("block.turbofan_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_ASSEMBLY = sound("block.missile_assembly");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONAR_PING = sound("block.sonar_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_TURBINE_RUNNING = sound("block.largeturbine");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARN_OVERSPEED = sound("block.warnoverspeed");
    // [parity Q3: turbofan] NTMSounds.TURBOFAN_DAMAGE = "hbm:block.damage"
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DAMAGE = sound("block.damage");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUNGUS_TURBINE_RUNNING = sound("block.chungusturbinerunning");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUNGUS_LEVER = sound("block.chunguslever");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVER_LARGE = sound("block.leverlarge");
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_LEVER_START = sound("block.leverstart"); // [parity T2: NTMSounds.LEVER_START]
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_LEVER_STOP = sound("block.leverstop"); // [parity T2: NTMSounds.LEVER_STOP]
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_SPARK = sound("block.spark"); // [parity T2: NTMSounds.SPARK]
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCK_OPEN = sound("block.lockopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCK_HANG = sound("block.lockhang");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_OPEN = sound("block.crate_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_CLOSE = sound("block.crate_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORAGE_CLOSE = sound("block.storage_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORAGE_OPEN = sound("block.storage_open"); // [parity R2: mass storage and drone dock open sound]
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBE_FWOOMP = sound("weapon.reload.tubefwoomp"); // [parity R2: pneumatic tube send sound]
    public static final DeferredHolder<SoundEvent, SoundEvent> OPEN_C = sound("block.openc");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOSE_C = sound("block.closec");
    public static final DeferredHolder<SoundEvent, SoundEvent> OPEN_DOOR = sound("block.opendoor");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOSE_DOOR = sound("block.closedoor");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SCREM = sound("block.screm");
    public static final DeferredHolder<SoundEvent, SoundEvent> WGH_START = sound("block.door.wgh_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> WGH_STOP = sound("block.door.wgh_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_TRANSITION_SEAL_OPEN = sound("block.door.transition_seal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_ALARM6 = sound("block.door.alarm6");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_SLIDING_OPENED = sound("block.door.sliding_door_opened");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_SLIDING_OPENING = sound("block.door.sliding_door_opening");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_SLIDING_SHUT = sound("block.door.sliding_door_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_GARAGE_MOVE = sound("block.door.garage_move");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_GARAGE_STOP = sound("block.door.garage_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_LEVER = sound("block.door.lever");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_WGH_BIG_START = sound("block.door.wgh_big_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_WGH_BIG_STOP = sound("block.door.wgh_big_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_QE_SLIDING_SHUT = sound("block.door.qe_sliding_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_QE_SLIDING_OPENED = sound("block.door.qe_sliding_opened");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_QE_SLIDING_OPENING = sound("block.door.qe_sliding_opening");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_SLIDING_SEAL_OPEN = sound("block.door.sliding_seal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_SLIDING_SEAL_STOP = sound("block.door.sliding_seal_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_SCRAPE_NEW = sound("block.vault_scrape_new");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_THUD_NEW = sound("block.vault_thud_new");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_START = sound("block.reactorstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_STOP = sound("block.reactorstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE_GAS_RUNNING = sound("block.turbinegasrunning");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE_GAS_STARTUP = sound("block.turbinegasstartup");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE_GAS_SHUTDOWN = sound("block.turbinegasshutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> FENSU_HUM = sound("block.fensuhum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEPHAESTUS_RUNNING = sound("block.hephaestus_running");
    public static final DeferredHolder<SoundEvent, SoundEvent> FEL_LOOP = sound("block.fel");
    public static final DeferredHolder<SoundEvent, SoundEvent> FUSION_REACTOR_RUNNING = sound("block.fusion_reactor_running");
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_EXPLOSION = sound("block.rbmk_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_SHUTDOWN = sound("block.shutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_AZ5_COVER = sound("block.rbmk_az5_cover"); // [parity M1: AZ-5 cover sound]
    public static final DeferredHolder<SoundEvent, SoundEvent> DIGAMMA_FLASH = sound("weapon.dflash");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TESLA = sound("weapon.tesla");
    // [parity A2: bosses] hbm:entity.bombDet (BOT prime death), hbm:weapon.teslaShoot (Mask man orb)
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_BOMB_DET = sound("entity.bombdet");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TESLA_SHOOT = sound("weapon.teslashoot");
    // [parity I2: sounds] hbm:weapon.flamethrowerShoot (jetpacks), hbm:item.gasmaskScrew (NTMSounds.FILTER_SCREW), hbm:entity.slicer (injector_knife)
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_FLAMETHROWER_SHOOT = sound("weapon.flamethrowershoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_GASMASK_SCREW = sound("item.gasmaskscrew");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_SLICER = sound("entity.slicer");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CRUCIBLE_DEPLOY = sound("weapon.crucible_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CRUCIBLE_SWING = sound("weapon.crucible_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_SHOOT = sound("weapon.flamethrower_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SPARK_SHOOT = sound("weapon.spark_shoot");
    /** [parity c1] hbm:weapon.b92Reload (GunB92 charge). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_B92_RELOAD = sound("weapon.b92reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_EXPLOSION_MEDIUM = sound("weapon.explosion_medium");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_EXPLOSION_SMALL_NEAR = sound("weapon.explosion_small_near");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_EXPLOSION_SMALL_FAR = sound("weapon.explosion_small_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_JEREMY_FIRE = sound("turret.jeremy_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_JEREMY_RELOAD = sound("turret.jeremy_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_CHEKHOV_FIRE = sound("turret.chekhov_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_HOWARD_FIRE = sound("turret.howard_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_HOWARD_RELOAD = sound("turret.howard_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_RICHARD_FIRE = sound("turret.richard_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_RICHARD_RELOAD = sound("turret.richard_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_SENTRY_FIRE = sound("turret.sentry_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_SENTRY_LOCKON = sound("turret.sentry_lockon");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_MORTAR_WHISTLE = sound("turret.mortarwhistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TAU_SHOOT = sound("weapon.taushoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SAW_SHOOT = sound("weapon.saw_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_RICOCHET = sound("weapon.ricochet");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_GRENADE_BOUNCE = sound("weapon.grenade_bounce");
    // [parity H2: grenade_universal] hbm:weapon.grenadeBounce (EntityGrenadeUniversal; WEAPON_GRENADE_BOUNCE is hbm:weapon.gBounce),
    // NTMSounds.GRENADE_TECH / GRENADE_NUKA (hbm:weapon.reload.grenadeTech / grenadeNuka)
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_GRENADE_BOUNCE_UNIVERSAL = sound("weapon.grenadebounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_TECH = sound("weapon.reload.grenadetech");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_NUKA = sound("weapon.reload.grenadenuka");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ROCKET_FLAME = sound("weapon.rocketflame");
    /** [weapons batch E1] NTMSounds.GUN_ROCKET_FIRE (hbm:weapon.rpgShoot); GUN_ROCKET_FLAME is WEAPON_ROCKET_FLAME. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_ROCKET_FIRE = sound("weapon.rpgshoot");
    // [weapons batch F4: sounds] NTMSounds GUN_PLEASE_REMOVE_MY_EARDRUMS_THANKS (hbm:weapon.fire.loudestNoiseOnEarth), GUN_VYLET_PONY_..._INTRO_JINGLE (weapon.fire.vstar),
    // GUN_ABERRATOR_FIRE (weapon.fire.aberrator), GUN_LOCKON (weapon.fire.lockon), GUN_SCREW (weapon.reload.screw), GUN_ROCKET_INSERT (weapon.reload.insertRocket)
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PLEASE_REMOVE_MY_EARDRUMS_THANKS = sound("weapon.fire.loudestnoiseonearth");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_VSTAR_JINGLE = sound("weapon.fire.vstar");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_ABERRATOR_FIRE = sound("weapon.fire.aberrator");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_LOCKON = sound("weapon.fire.lockon");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SCREW = sound("weapon.reload.screw");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_ROCKET_INSERT = sound("weapon.reload.insertrocket");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ROBIN_EXPLOSION = sound("weapon.robin_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_MUKE_EXPLOSION = sound("weapon.mukeexplosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_EXPLOSION_LARGE_NEAR = sound("weapon.explosionlargenear");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_EXPLOSION_LARGE_FAR = sound("weapon.explosionlargefar");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CASING_SMALL = sound("weapon.casing.small");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CASING_MEDIUM = sound("weapon.casing.medium");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CASING_LARGE = sound("weapon.casing.large");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CASING_SHELL = sound("weapon.casing.shell");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_1 = sound("item.geiger1");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_2 = sound("item.geiger2");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_3 = sound("item.geiger3");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_4 = sound("item.geiger4");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_5 = sound("item.geiger5");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_6 = sound("item.geiger6");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP_IRON = sound("step.iron");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP_IRON_LAND = sound("step.iron_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP_POWERED = sound("step.powered");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP_METAL = sound("step.metal"); // [parity I1: ArmorFSB step sound]
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP_IRON_JUMP = sound("step.iron_jump"); // [parity I1: ArmorFSB jump sound]
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_UNPACK = sound("item.unpack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SPRAY = sound("item.spray"); // [parity S4: gun kits] NTMSounds.SPRAY_CAN
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_REPAIR = sound("item.repair"); // [parity S4: gun kits] NTMSounds.REPAIR
    public static final DeferredHolder<SoundEvent, SoundEvent> PIN_UNLOCK = sound("item.pinunlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIN_BREAK = sound("item.pinbreak");
    public static final DeferredHolder<SoundEvent, SoundEvent> UPGRADE_PLUG = sound("item.upgradeplug");
    public static final DeferredHolder<SoundEvent, SoundEvent> PILE_BOLT_OPEN = sound("block.pile_bolt_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> VENDING_MACHINE_DISPENSE = sound("block.vending_machine_dispense");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUIT_BATTERY = sound("item.battery");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLAYER_COUGH = sound("player.cough");
    // [parity H1: sounds] hbm:item.syringe, item.radaway, item.vice, item.jetpackTank, weapon.laserBang
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SYRINGE = sound("item.syringe");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_RADAWAY = sound("item.radaway");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_VICE = sound("item.vice");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_JETPACK_TANK = sound("item.jetpacktank");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_LASER_BANG = sound("weapon.laserbang");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLAYER_GULP = sound("player.gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLAYER_GROAN = sound("player.groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> POTATOS_RANDOM = sound("potatos.random");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOMIT = sound("entity.vomit");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUCC = sound("entity.ducc");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEGAQUACC = sound("entity.megaquacc");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_OLD_EXPLOSION = sound("entity.oldexplosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_BOMBER_LOOP = sound("entity.bomber_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_BOMBER_SMALL_LOOP = sound("entity.bomber_small_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_PLANE_CRASH = sound("entity.plane_crash");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_PLANE_SHOT_DOWN = sound("entity.plane_shot_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CYBERCRAB = sound("entity.cybercrab");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_UFO_BEAM = sound("entity.ufo_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_UFO_BLAST = sound("entity.ufo_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_BALLS_LASER = sound("weapon.balls_laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_OSIPR_SHOOT = sound("weapon.osipr_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CHOPPER_FLYING_LOOP = sound("entity.chopper_flying_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CHOPPER_DROP = sound("entity.chopper_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CHOPPER_CHARGE = sound("entity.chopper_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CHOPPER_DAMAGE = sound("entity.chopper_damage");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CHOPPER_MINE_LOOP = sound("entity.chopper_mine_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CHOPPER_CRASHING_LOOP = sound("entity.chopper_crashing_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISC_NULL_CHOPPER = sound("misc.null_chopper");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISC_NULL_CRASHING = sound("misc.null_crashing");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISC_NULL_MINE = sound("misc.null_mine");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_BOMB_WHISTLE = sound("entity.bomb_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_MISSILE_TAKEOFF = sound("weapon.missile_takeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TRAIN_IMPACT = sound("weapon.train_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> METEORITE_FALLING_LOOP = sound("entity.meteoritefallingloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOYUZ_READY = sound("block.soyuzready");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOYUZ_TAKEOFF = sound("entity.soyuztakeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOYUZED = sound("alarm.soyuzed");
    public static final DeferredHolder<SoundEvent, SoundEvent> HATCH_ALARM = sound("alarm.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_AUTOPILOT = sound("alarm.autopilot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_AMS = sound("alarm.ams_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_BLAST_DOOR = sound("alarm.blast_door_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_APC_LOOP = sound("alarm.apc_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_KLAXON = sound("alarm.klaxon");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_FO_KLAXON_A = sound("alarm.fo_klaxon_a");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_FO_KLAXON_B = sound("alarm.fo_klaxon_b");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_REGULAR = sound("alarm.regular_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_CLASSIC = sound("alarm.classic");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_BANK = sound("alarm.bank_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_BEEP = sound("alarm.beep_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_CONTAINER = sound("alarm.container_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_SWEEP = sound("alarm.sweep_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_STRIDER = sound("alarm.strider_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_AIR_RAID = sound("alarm.air_raid");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_NOSTROMO = sound("alarm.nostromo_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_EAS = sound("alarm.eas_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_APC_PASS = sound("alarm.apc_pass");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN_RAZORTRAIN = sound("alarm.razortrain_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOM_CHIME = sound("alarm.chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> TECH_BLEEP = sound("tool.techbleep");
    public static final DeferredHolder<SoundEvent, SoundEvent> TECH_BOOP = sound("tool.techboop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_GUN = sound("item.boltgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_LANDMINE_ARM = sound("weapon.fstbmb_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE_START = WEAPON_LANDMINE_ARM;
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE_BEEP = sound("weapon.fstbmb_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEBRIS = sound("block.debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> METAL_IMPACT = sound("block.metal_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARC_FURNACE_OPERATE = sound("block.arc_furnace_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECORD_LAMBDA_CORE = sound("music.record_lambda_core");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECORD_SECTOR_SWEEP = sound("music.record_sector_sweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECORD_VORTAL_COMBAT = sound("music.record_vortal_combat");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECORD_GLASS = sound("music.transmission");

    private HbmSoundEvents() {
    }

    // [weapons batch F3: sounds] NTMSounds GUN_EXTINGUISHER_FIRE (hbm:weapon.extinguisher), GUN_CHARGE_FIRE (weapon.fire.grenade).
    // GUN_VALVE and GUN_ROCKET_INSERT, also used by batch F3, are declared above by batches F1 and F4.
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EXTINGUISHER_FIRE = sound("weapon.extinguisher");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_CHARGE_FIRE = sound("weapon.fire.grenade");

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    // [parity I3: melee sounds] NTMSounds GAVEL, BONK, STOP, BANG, SLICE, KAPENG and weapon.chainsaw
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_WHACK = sound("weapon.whack");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_BONK = sound("weapon.bonk");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_STOP = sound("weapon.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_BANG = sound("weapon.bang");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SLICE = sound("weapon.slice");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_KAPENG = sound("weapon.kapeng");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CHAINSAW = sound("weapon.chainsaw");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
        ResourceLocation id = AtomicLegacy.id(path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
