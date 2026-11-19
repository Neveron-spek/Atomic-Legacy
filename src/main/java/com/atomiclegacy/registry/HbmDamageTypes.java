package com.atomiclegacy.registry;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class HbmDamageTypes {
    public static final ResourceKey<DamageType> RADIATION = key("radiation");
    public static final ResourceKey<DamageType> DIGAMMA = key("digamma");
    public static final ResourceKey<DamageType> RUBBLE = key("rubble");
    public static final ResourceKey<DamageType> LEAD = key("lead");
    // [parity H1: damage types] ModDamageSource euthanizedSelf, euthanizedSelf2, euthanized, overdose, bang
    public static final ResourceKey<DamageType> EUTHANIZED_SELF = key("euthanized_self");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF2 = key("euthanized_self2");
    public static final ResourceKey<DamageType> EUTHANIZED = key("euthanized");
    public static final ResourceKey<DamageType> OVERDOSE = key("overdose");
    public static final ResourceKey<DamageType> BANG = key("bang");
    public static final ResourceKey<DamageType> METEORITE = key("meteorite");
    public static final ResourceKey<DamageType> MONOXIDE = key("monoxide");
    public static final ResourceKey<DamageType> ASBESTOS = key("asbestos");
    public static final ResourceKey<DamageType> BLACK_LUNG = key("blacklung");
    public static final ResourceKey<DamageType> SHRAPNEL = key("shrapnel");
    public static final ResourceKey<DamageType> SEDNA_PHYSICAL = key("sedna_physical");
    public static final ResourceKey<DamageType> TAU = key("tau");
    /** Projectile damage used by the old MASKMAN_BOLT fired by the orbital death ray. */
    public static final ResourceKey<DamageType> LASER = key("laser");
    // [parity A2: boss damage] ModDamageSource.s_combineball (UFO beam; projectile, bypasses armor),
    // s_bullet (BulletConfiguration default, UFO/maskman rockets), s_emplacer (EntityBullet "chopper").
    public static final ResourceKey<DamageType> CMB = key("cmb");
    public static final ResourceKey<DamageType> REVOLVER_BULLET = key("revolver_bullet");
    public static final ResourceKey<DamageType> EMPLACER = key("emplacer");
    public static final ResourceKey<DamageType> SEDNA_EXPLOSIVE = key("sedna_explosive");
    public static final ResourceKey<DamageType> SEDNA_FIRE = key("sedna_fire");
    /** [weapons batch C3] DamageResistanceHandler.DamageClass.LASER sedna source (BulletConfig.getDamage, death.sedna.laser). */
    public static final ResourceKey<DamageType> SEDNA_LASER = key("sedna_laser");
    /** [weapons batch D1] DamageClass.PLASMA sedna source (shredder submunitions; the original has no death.sedna.plasma translation). */
    public static final ResourceKey<DamageType> SEDNA_PLASMA = key("sedna_plasma");
    /** [weapons batch E2] DamageClass.ELECTRIC sedna source (XFactoryEnergy tesla beams, death.sedna.electric). */
    public static final ResourceKey<DamageType> SEDNA_ELECTRIC = key("sedna_electric");
    /** [weapons batch F2] DamageClass.SUBATOMIC sedna source (tau beams) and ModDamageSource.tauBlast (tau overcharge). */
    public static final ResourceKey<DamageType> SEDNA_SUBATOMIC = key("sedna_subatomic");
    public static final ResourceKey<DamageType> TAU_BLAST = key("tau_blast");
    /** [weapons batch D1] ModDamageSource.boat (EntityDuchessGambit): absolute, bypasses armor. */
    public static final ResourceKey<DamageType> BOAT = key("boat");
    public static final ResourceKey<DamageType> ELECTRICITY = key("electricity");
    public static final ResourceKey<DamageType> MICROWAVE = key("microwave");
    public static final ResourceKey<DamageType> DEATH = key("death");
    public static final ResourceKey<DamageType> NUCLEAR_BLAST = key("nuclear_blast");
    public static final ResourceKey<DamageType> ACID = key("acid");
    public static final ResourceKey<DamageType> BOIL = key("boil");
    /** [weapons batch F1] ModDamageSource.s_flamethrower (EntityChemical, indirect: chemical + thrower). */
    public static final ResourceKey<DamageType> FLAMETHROWER = key("flamethrower");
    public static final ResourceKey<DamageType> MUD_POISONING = key("mud_poisoning");
    public static final ResourceKey<DamageType> CLOUD = key("cloud");
    public static final ResourceKey<DamageType> PC = key("pc"); // [parity J1: ModDamageSource.pc, barbed_wire_ultradeath]
    public static final ResourceKey<DamageType> TAINT = key("taint");
    public static final ResourceKey<DamageType> TURBOFAN = key("turbofan");
    public static final ResourceKey<DamageType> BLACK_HOLE = key("blackhole");
    public static final ResourceKey<DamageType> BROADCAST = key("broadcast");
    public static final ResourceKey<DamageType> BOXCAR = key("boxcar");
    public static final ResourceKey<DamageType> SPIKES = key("spikes"); // [parity R4: spikes] ModDamageSource.spikes (bypasses armor)
    public static final ResourceKey<DamageType> MKU = key("mku");
    // [parity M3: dfc] ModDamageSource.ams / amsCore (absolute, bypasses armor).
    public static final ResourceKey<DamageType> AMS = key("ams");
    public static final ResourceKey<DamageType> AMS_CORE = key("ams_core");
    public static final ResourceKey<DamageType> ENERVATION = key("enervation"); // [parity Q4: ModDamageSource.enervation, radiobox; absolute, bypasses armor]


    private HbmDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, AtomicLegacy.id(name));
    }
}
