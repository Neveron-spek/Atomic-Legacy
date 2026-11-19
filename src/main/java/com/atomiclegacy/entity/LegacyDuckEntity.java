package com.atomiclegacy.entity;

import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

/** Direct modern equivalent of the 1.7.10 EntityDuck. */
public final class LegacyDuckEntity extends Chicken {
    public LegacyDuckEntity(EntityType<? extends LegacyDuckEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return HbmSoundEvents.DUCC.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HbmSoundEvents.DUCC.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundEvents.DUCC.get();
    }

    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return new LegacyDuckEntity(HbmEntityTypes.DUCK.get(), level);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(getCombatTracker().getDeathMessage(), false);
        }
        super.die(source);
    }

    // [parity P3: inherited 1.7.10 EntityChicken.dropFewItems] the entity_fucc_a_ducc type has no loot table, so the
    // chicken drops are reproduced here: rand(3) + rand(1 + looting) feathers and one raw or (burning) cooked chicken.
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int looting = 0;
        if (getLastHurtByMob() != null && !getLastHurtByMob().getMainHandItem().isEmpty()) {
            looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),
                    getLastHurtByMob().getMainHandItem());
        }
        int count = random.nextInt(3) + random.nextInt(1 + looting);
        for (int i = 0; i < count; i++) spawnAtLocation(net.minecraft.world.item.Items.FEATHER);
        spawnAtLocation(isOnFire() ? net.minecraft.world.item.Items.COOKED_CHICKEN : net.minecraft.world.item.Items.CHICKEN);
    }
}
