package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ItemLike;

public enum HbmToolTier implements Tier {
    // [parity I3] 1.7.10 ToolMaterial values (MainRegistry.tMat*, ModItems local materials, vanilla WOOD/STONE/EMERALD):
    // harvest level, maxUses, efficiency, damageVsEntity, enchantability, setRepairItem ("" = no repair item).
    WOOD_COMPAT(0, 59, 2.0F, 0.0F, 15, "#minecraft:planks"),
    PIPE_LEAD(1, 250, 1.5F, 3.0F, 25, ""),
    BOTTLE_OPENER(1, 250, 1.5F, 0.5F, 200, "plate_steel"),
    SCHRABIDIUM_HAMMER(3, 0, 50.0F, 999_999_996.0F, 200, "block_schrabidium"),
    SCHRABIDIUM(3, 10000, 50.0F, 100.0F, 200, "ingot_schrabidium"),
    CHAINSAW(3, 1500, 50.0F, 22.0F, 0, "ingot_steel"),
    STEEL(3, 750, 8.0F, 2.0F, 10, "ingot_steel"),
    TITANIUM(3, 1000, 9.0F, 2.5F, 15, "ingot_titanium"),
    ALLOY(3, 2000, 15.0F, 5.0F, 5, ""),
    CMB(3, 8500, 40.0F, 55.0F, 100, "ingot_combine_steel"),
    ELEC(3, 0, 30.0F, 12.0F, 2, ""),
    DESH(2, 0, 7.5F, 2.0F, 10, "ingot_desh"),
    COBALT(3, 750, 9.0F, 2.5F, 60, ""),
    COBALT_DECORATED(3, 2500, 15.0F, 2.5F, 75, "ingot_cobalt"),
    STARMETAL(3, 3000, 20.0F, 2.5F, 100, "ingot_starmetal"),
    BISMUTH(4, 0, 50.0F, 0.0F, 200, "ingot_bismuth"),
    VOLCANIC(4, 0, 50.0F, 0.0F, 200, "ingot_bismuth"),
    CHLOROPHYTE(4, 0, 75.0F, 0.0F, 200, "powder_chlorophyte"),
    MESE(4, 0, 100.0F, 0.0F, 200, "plate_paa"),
    MESE_GAVEL(4, 0, 50.0F, 0.0F, 200, "plate_paa"),
    DWARVEN(2, 0, 4.0F, 0.0F, 10, "minecraft:copper_ingot"),
    METEORITE(4, 0, 50.0F, 0.0F, 200, "plate_paa"),
    SHIMMER(1, 0, 25.0F, 26.0F, 200, ""),
    STONE_COMPAT(1, 131, 4.0F, 1.0F, 5, "minecraft:cobblestone"),
    DIAMOND_COMPAT(3, 1561, 8.0F, 3.0F, 10, "minecraft:diamond");

    private final int level;
    private final int uses;
    private final float speed;
    private final float damageBonus;
    private final int enchantmentValue;
    private final String repairItemId;
    private Ingredient repairIngredient;

    HbmToolTier(int level, int uses, float speed, float damageBonus, int enchantmentValue, String repairItemId) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damageBonus = damageBonus;
        this.enchantmentValue = enchantmentValue;
        this.repairItemId = repairItemId;
    }

    /** 1.7.10 harvest level of the material. */
    public int legacyLevel() {
        return this.level;
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.damageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        if (this.level <= 0) {
            return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
        }
        if (this.level == 1) {
            return BlockTags.INCORRECT_FOR_STONE_TOOL;
        }
        if (this.level == 2) {
            return BlockTags.INCORRECT_FOR_IRON_TOOL;
        }
        if (this.level == 3) {
            return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
        }
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        if (this.repairIngredient == null) {
            if (this.repairItemId.isEmpty()) {
                return this.repairIngredient = Ingredient.EMPTY;
            }
            if (this.repairItemId.startsWith("#")) {
                return this.repairIngredient = Ingredient.of(TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        ResourceLocation.parse(this.repairItemId.substring(1))));
            }
            ResourceLocation repairId = this.repairItemId.indexOf(':') >= 0
                    ? ResourceLocation.parse(this.repairItemId)
                    : AtomicLegacy.id(this.repairItemId);
            this.repairIngredient = BuiltInRegistries.ITEM
                    .getOptional(repairId)
                    .map(item -> Ingredient.of((ItemLike) item))
                    .orElse(Ingredient.EMPTY);
        }
        return this.repairIngredient;
    }
}
