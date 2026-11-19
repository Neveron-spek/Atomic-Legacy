package com.atomiclegacy.items;

import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity I3] Constructor data of 1.7.10 ItemToolAbility / ItemSwordAbility / WeaponSpecial: material, the "damage"
 * and "movement" arguments, the addAbility(...) calls, setRarity, setShears, setDepthRockBreaker and setMaxDamage.
 */
public record HbmToolProfile(
        HbmToolTier tier,
        float attackDamage,
        float attackSpeed,
        double movementModifier,
        Rarity rarity,
        boolean miner,
        List<AbilityLevel<HbmToolBehavior.AreaAbility>> areaAbilities,
        List<AbilityLevel<HbmToolBehavior.HarvestAbility>> harvestAbilities,
        List<AbilityLevel<HbmToolBehavior.WeaponAbility>> weaponAbilities,
        HbmToolBehavior.SpecialBehavior specialBehavior,
        boolean shears,
        boolean rockBreaker,
        int maxDamage
) {
    public HbmToolProfile {
        areaAbilities = List.copyOf(areaAbilities);
        harvestAbilities = List.copyOf(harvestAbilities);
        weaponAbilities = List.copyOf(weaponAbilities);
    }

    public static HbmToolProfile create(HbmToolTier tier, float attackDamage, float attackSpeed) {
        return new HbmToolProfile(tier, attackDamage, attackSpeed, 0.0D, Rarity.COMMON, false,
                List.of(), List.of(), List.of(), HbmToolBehavior.SpecialBehavior.NONE, false, false, -1);
    }

    public HbmToolProfile movement(double movementModifier) {
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, areaAbilities,
                harvestAbilities, weaponAbilities, specialBehavior, shears, rockBreaker, maxDamage);
    }

    public HbmToolProfile rarity(Rarity rarity) {
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, areaAbilities,
                harvestAbilities, weaponAbilities, specialBehavior, shears, rockBreaker, maxDamage);
    }

    /** EnumToolType.MINER: harvest levels "pickaxe" and "shovel". */
    public HbmToolProfile asMiner() {
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, true, areaAbilities,
                harvestAbilities, weaponAbilities, specialBehavior, shears, rockBreaker, maxDamage);
    }

    public HbmToolProfile area(HbmToolBehavior.AreaAbility ability, int level) {
        List<AbilityLevel<HbmToolBehavior.AreaAbility>> list = new ArrayList<>(areaAbilities);
        list.removeIf(entry -> entry.ability() == ability);
        list.add(new AbilityLevel<>(ability, level));
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, list,
                harvestAbilities, weaponAbilities, specialBehavior, shears, rockBreaker, maxDamage);
    }

    public HbmToolProfile harvest(HbmToolBehavior.HarvestAbility ability, int level) {
        List<AbilityLevel<HbmToolBehavior.HarvestAbility>> list = new ArrayList<>(harvestAbilities);
        list.removeIf(entry -> entry.ability() == ability);
        list.add(new AbilityLevel<>(ability, level));
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, areaAbilities,
                list, weaponAbilities, specialBehavior, shears, rockBreaker, maxDamage);
    }

    public HbmToolProfile weapon(HbmToolBehavior.WeaponAbility ability, int level) {
        List<AbilityLevel<HbmToolBehavior.WeaponAbility>> list = new ArrayList<>(weaponAbilities);
        list.removeIf(entry -> entry.ability() == ability);
        list.add(new AbilityLevel<>(ability, level));
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, areaAbilities,
                harvestAbilities, list, specialBehavior, shears, rockBreaker, maxDamage);
    }

    public HbmToolProfile special(HbmToolBehavior.SpecialBehavior specialBehavior) {
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, areaAbilities,
                harvestAbilities, weaponAbilities, specialBehavior, shears, rockBreaker, maxDamage);
    }

    /** ItemToolAbility.setShears(). */
    public HbmToolProfile setShears() {
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, areaAbilities,
                harvestAbilities, weaponAbilities, specialBehavior, true, rockBreaker, maxDamage);
    }

    /** ItemToolAbility.setDepthRockBreaker(). */
    public HbmToolProfile depthRockBreaker() {
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, areaAbilities,
                harvestAbilities, weaponAbilities, specialBehavior, shears, true, maxDamage);
    }

    /** Item.setMaxDamage(int) after construction (0 = not damageable). */
    public HbmToolProfile maxDamage(int maxDamage) {
        return new HbmToolProfile(tier, attackDamage, attackSpeed, movementModifier, rarity, miner, areaAbilities,
                harvestAbilities, weaponAbilities, specialBehavior, shears, rockBreaker, maxDamage);
    }

    /** Effective 1.7.10 getMaxDamage(): setMaxDamage override or ToolMaterial.getMaxUses(). */
    public int durability() {
        return this.maxDamage >= 0 ? this.maxDamage : this.tier.getUses();
    }

    public boolean hasCyclableAbilities() {
        return !this.areaAbilities.isEmpty() || !this.harvestAbilities.isEmpty();
    }

    /**
     * [parity V3: ToolConfig ability*] AvailableAbilities.addAbility only stores an ability whose isAllowed() is true;
     * the 1.7.10 flags were read before the items were built, here they are applied when the lists are read.
     */
    @Override
    public List<AbilityLevel<HbmToolBehavior.AreaAbility>> areaAbilities() {
        for (AbilityLevel<HbmToolBehavior.AreaAbility> entry : this.areaAbilities) {
            if (!entry.ability().isAllowed()) {
                return this.areaAbilities.stream().filter(e -> e.ability().isAllowed()).toList();
            }
        }
        return this.areaAbilities;
    }

    @Override
    public List<AbilityLevel<HbmToolBehavior.HarvestAbility>> harvestAbilities() {
        for (AbilityLevel<HbmToolBehavior.HarvestAbility> entry : this.harvestAbilities) {
            if (!entry.ability().isAllowed()) {
                return this.harvestAbilities.stream().filter(e -> e.ability().isAllowed()).toList();
            }
        }
        return this.harvestAbilities;
    }

    /** AvailableAbilities.maxLevel: -1 when the ability is not available. */
    public int maxLevel(Enum<?> ability) {
        for (AbilityLevel<?> entry : areaAbilities()) if (entry.ability() == ability) return entry.level(); // [parity V3]
        for (AbilityLevel<?> entry : harvestAbilities()) if (entry.ability() == ability) return entry.level();
        for (AbilityLevel<?> entry : this.weaponAbilities) if (entry.ability() == ability) return entry.level();
        return -1;
    }

    public record AbilityLevel<T extends Enum<T>>(T ability, int level) {
    }
}
