package com.atomiclegacy.items;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** [parity I3] A 1.7.10 material with maxUses 0 (not damageable): durability 1 plus a hidden Unbreakable component. */
public record HbmToolTierUses(HbmToolTier base) implements Tier {
    @Override public int getUses() { return 1; }
    @Override public float getSpeed() { return this.base.getSpeed(); }
    @Override public float getAttackDamageBonus() { return this.base.getAttackDamageBonus(); }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return this.base.getIncorrectBlocksForDrops(); }
    @Override public int getEnchantmentValue() { return this.base.getEnchantmentValue(); }
    @Override public Ingredient getRepairIngredient() { return this.base.getRepairIngredient(); }
}
