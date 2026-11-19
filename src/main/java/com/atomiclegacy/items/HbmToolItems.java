package com.atomiclegacy.items;

import com.atomiclegacy.items.weapon.sedna.mags.MagazineElectricEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

/**
 * [parity I3] Item classes for 1.7.10 ItemToolAbility (+Power / +Fueled) and ItemSwordAbility (+Power),
 * ItemSwordMeteorite and ModSword / BigSword / RedstoneSword (plain swords with lore).
 */
public final class HbmToolItems {
    private HbmToolItems() {
    }

    public interface AbilityItem {
        HbmToolProfile profile();

        HbmToolBehavior.ToolType toolType();

        /** true for ItemToolAbility (presets, area/harvest abilities), false for ItemSwordAbility. */
        boolean isAbilityTool();

        Energy energy();

        default boolean canOperate(ItemStack stack) {
            return energy().canOperate(stack);
        }

        /** ItemStack.damageItem(amount, entity) including the power / fuel setDamage overrides. */
        default void damageTool(ItemStack stack, int amount, LivingEntity entity) {
            energy().damage(stack, amount, entity);
        }
    }

    public enum EnergyKind { NONE, POWER, FUEL }

    /** ItemToolAbilityPower / ItemSwordAbilityPower (maxPower, chargeRate, consumption) or ItemToolAbilityFueled. */
    public record Energy(EnergyKind kind, long max, long rate, long consumption, List<String> fuels) {
        public static final Energy NONE = new Energy(EnergyKind.NONE, 0, 0, 0, List.of());

        public static Energy power(long maxPower, long chargeRate, long consumption) {
            return new Energy(EnergyKind.POWER, maxPower, chargeRate, consumption, List.of());
        }

        public static Energy fuel(int maxFuel, int consumption, int fillRate, String... fuels) {
            return new Energy(EnergyKind.FUEL, maxFuel, fillRate, consumption, List.of(fuels));
        }

        public boolean canOperate(ItemStack stack) {
            return switch (this.kind) {
                case NONE -> true;
                case POWER -> charge(stack) >= this.consumption;
                case FUEL -> fill(stack) >= this.consumption;
            };
        }

        /** getCharge: a stack without NBT counts as fully charged. */
        public long charge(ItemStack stack) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            return data == null ? this.max : data.copyTag().getLong("charge");
        }

        public void setCharge(ItemStack stack, long charge) {
            CompoundTag tag = materializedTag(stack);
            tag.putLong("charge", charge);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        /** getFill: a stack without NBT counts as full. */
        public int fill(ItemStack stack) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            return data == null ? (int) this.max : data.copyTag().getInt("fuel");
        }

        public void setFill(ItemStack stack, int fill) {
            CompoundTag tag = materializedTag(stack);
            tag.putInt("fuel", fill);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        void damage(ItemStack stack, int amount, LivingEntity entity) {
            if (entity.level().isClientSide) return;
            if (this.kind == EnergyKind.NONE) {
                if (stack.isDamageableItem()) stack.hurtAndBreak(amount, entity, EquipmentSlot.MAINHAND);
                return;
            }
            // ItemStack.damageItem: no damage for creative players; Unbreaking via attemptDamageItem, then setDamage.
            if (entity instanceof Player player && player.getAbilities().instabuild) return;
            int damage = entity.level() instanceof ServerLevel level
                    ? EnchantmentHelper.processDurabilityChange(level, stack, amount) : amount;
            if (damage <= 0) return;
            if (this.kind == EnergyKind.POWER) {
                long charge = charge(stack) - damage * this.consumption;
                setCharge(stack, Math.max(charge, 0));
            } else {
                setFill(stack, (int) Math.max(fill(stack) - damage * this.consumption, 0));
            }
        }

        long stored(ItemStack stack) {
            return this.kind == EnergyKind.POWER ? charge(stack) : fill(stack);
        }

        boolean barVisible(ItemStack stack) {
            return this.kind != EnergyKind.NONE && stored(stack) < this.max;
        }

        /** Forge 1.7.10 durability bar: width round(13 - health * 13), colour (255 - j) << 16 | j << 8. */
        int barWidth(ItemStack stack) {
            double health = 1.0D - (double) stored(stack) / (double) this.max;
            return (int) Math.round(13.0D - health * 13.0D);
        }

        int barColor(ItemStack stack) {
            double health = 1.0D - (double) stored(stack) / (double) this.max;
            int j = (int) Math.round(255.0D - health * 255.0D);
            return (255 - j) << 16 | j << 8;
        }

        void tooltip(ItemStack stack, List<Component> tooltip) {
            if (this.kind == EnergyKind.POWER) {
                tooltip.add(Component.literal("Charge: " + MagazineElectricEngine.getShortNumber(charge(stack)) + " / "
                        + MagazineElectricEngine.getShortNumber(this.max)).withStyle(ChatFormatting.GRAY));
            } else if (this.kind == EnergyKind.FUEL) {
                tooltip.add(Component.literal("Fuel: " + fill(stack) + "/" + this.max + "mB").withStyle(ChatFormatting.GOLD));
                for (String fuel : this.fuels) {
                    com.atomiclegacy.registry.HbmFluids.byName(fuel).ifPresent(type -> tooltip.add(
                            Component.literal("- ").append(Component.translatable(type.translationKey())).withStyle(ChatFormatting.YELLOW)));
                }
            }
        }
    }

    /** Copy of the stack NBT; a powered/fuelled stack without NBT first gets its implicit full charge/fuel written. */
    public static CompoundTag materializedTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) return data.copyTag();
        CompoundTag tag = new CompoundTag();
        if (stack.getItem() instanceof AbilityItem item) {
            Energy energy = item.energy();
            if (energy.kind() == EnergyKind.POWER) tag.putLong("charge", energy.max());
            if (energy.kind() == EnergyKind.FUEL) tag.putInt("fuel", (int) energy.max());
        }
        return tag;
    }

    /** Tier with the 1.7.10 getMaxDamage() (setMaxDamage overrides; 0 is replaced by a hidden Unbreakable component). */
    record UsesTier(HbmToolTier base, int uses) implements Tier {
        @Override public int getUses() { return this.uses; }
        @Override public float getSpeed() { return this.base.getSpeed(); }
        @Override public float getAttackDamageBonus() { return this.base.getAttackDamageBonus(); }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return this.base.getIncorrectBlocksForDrops(); }
        @Override public int getEnchantmentValue() { return this.base.getEnchantmentValue(); }
        @Override public Ingredient getRepairIngredient() { return this.base.getRepairIngredient(); }
    }

    static boolean unbreakable(HbmToolProfile profile, Energy energy) {
        return energy.kind() != EnergyKind.NONE || profile.durability() <= 0;
    }

    static Tier tier(HbmToolProfile profile, Energy energy) {
        return new UsesTier(profile.tier(), unbreakable(profile, energy) ? 1 : profile.durability());
    }

    static Item.Properties props(HbmToolProfile profile, Energy energy) {
        Item.Properties properties = HbmToolBehavior.properties(profile);
        return unbreakable(profile, energy) ? HbmToolBehavior.unbreakable(properties) : properties;
    }

    private static TagKey<Block> mineable(HbmToolBehavior.ToolType type) {
        return switch (type) {
            case AXE -> BlockTags.MINEABLE_WITH_AXE;
            case SHOVEL -> BlockTags.MINEABLE_WITH_SHOVEL;
            default -> BlockTags.MINEABLE_WITH_PICKAXE;
        };
    }

    /** ItemToolAbility (+Power, +Fueled). No vanilla strip/path/till actions: the 1.7.10 class extended ItemTool. */
    public static class AbilityTool extends DiggerItem implements AbilityItem {
        private final HbmToolProfile profile;
        private final HbmToolBehavior.ToolType type;
        private final Energy energy;

        public AbilityTool(HbmToolProfile profile, HbmToolBehavior.ToolType type, Energy energy) {
            super(tier(profile, energy), mineable(type), props(profile, energy));
            this.profile = profile;
            this.type = type;
            this.energy = energy;
        }

        @Override public HbmToolProfile profile() { return this.profile; }
        @Override public HbmToolBehavior.ToolType toolType() { return this.type; }
        @Override public boolean isAbilityTool() { return true; }
        @Override public Energy energy() { return this.energy; }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            if (!canOperate(stack)) return 1.0F;
            return HbmToolBehavior.typeMatches(this.type, state) ? this.profile.tier().getSpeed() : 1.0F;
        }

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
            if (!canOperate(stack)) return false;
            if (HbmToolBehavior.getConfiguration(stack, this.profile).active().harvest == HbmToolBehavior.HarvestAbility.SILK) return true;
            return HbmToolBehavior.typeMatches(this.type, state) && !state.is(this.profile.tier().getIncorrectBlocksForDrops());
        }

        @Override
        public boolean canPerformAction(ItemStack stack, ItemAbility action) {
            return switch (this.type) {
                case PICKAXE -> action == ItemAbilities.PICKAXE_DIG;
                case AXE -> action == ItemAbilities.AXE_DIG;
                case SHOVEL -> action == ItemAbilities.SHOVEL_DIG;
                case MINER -> action == ItemAbilities.PICKAXE_DIG || action == ItemAbilities.SHOVEL_DIG;
                case SWORD -> false;
            };
        }

        /** ItemTool.onBlockDestroyed: 1 damage when the hardness is not 0. */
        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
            if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) damageTool(stack, 1, miner);
            return true;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            HbmToolBehavior.hitEntity(stack, target, attacker, this);
            return true;
        }

        /** ItemToolAbility.hitEntity: stack.damageItem(1, attacker). */
        @Override
        public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            damageTool(stack, 1, attacker);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return HbmToolBehavior.use(level, player, hand, this);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return HbmToolBehavior.useOn(context, this.profile);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            this.energy.tooltip(stack, tooltip);
            HbmToolBehavior.addTooltip(tooltip, this.profile, true);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return stack.isEnchanted() || HbmToolBehavior.isFoil(stack, this.profile);
        }

        @Override public boolean isBarVisible(ItemStack stack) { return this.energy.kind() == EnergyKind.NONE ? super.isBarVisible(stack) : this.energy.barVisible(stack); }
        @Override public int getBarWidth(ItemStack stack) { return this.energy.kind() == EnergyKind.NONE ? super.getBarWidth(stack) : this.energy.barWidth(stack); }
        @Override public int getBarColor(ItemStack stack) { return this.energy.kind() == EnergyKind.NONE ? super.getBarColor(stack) : this.energy.barColor(stack); }
    }

    /** ItemSwordAbility (+Power), ItemSwordMeteorite, ModSword, BigSword, RedstoneSword. */
    public static class AbilitySword extends SwordItem implements AbilityItem {
        private final HbmToolProfile profile;
        private final Energy energy;
        private final List<String> lore;
        private final String loreKey;

        public AbilitySword(HbmToolProfile profile, Energy energy, List<String> lore, String loreKey) {
            super(tier(profile, energy), props(profile, energy));
            this.profile = profile;
            this.energy = energy;
            this.lore = List.copyOf(lore);
            this.loreKey = loreKey;
        }

        public AbilitySword(HbmToolProfile profile) {
            this(profile, Energy.NONE, List.of(), null);
        }

        @Override public HbmToolProfile profile() { return this.profile; }
        @Override public HbmToolBehavior.ToolType toolType() { return HbmToolBehavior.ToolType.SWORD; }
        @Override public boolean isAbilityTool() { return false; }
        @Override public Energy energy() { return this.energy; }

        /** ItemSword.onBlockDestroyed: 2 damage when the hardness is not 0. */
        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
            if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) damageTool(stack, 2, miner);
            return true;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            HbmToolBehavior.hitEntity(stack, target, attacker, this);
            return true;
        }

        @Override
        public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            damageTool(stack, 1, attacker);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            InteractionResult result = HbmToolBehavior.useOn(context, this.profile);
            return result != InteractionResult.PASS ? result : super.useOn(context);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            this.energy.tooltip(stack, tooltip);
            HbmToolBehavior.addTooltip(tooltip, this.profile, false);
            for (String line : this.lore) LegacyConsumableSupport.literal(tooltip, line);
            if (this.loreKey != null) {
                // ItemSwordMeteorite: I18nUtil.resolveKeyArray(name + ".desc") in italics
                String text = Component.translatable(this.loreKey).getString();
                if (!text.equals(this.loreKey)) {
                    for (String line : text.split("\\$")) tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        }

        @Override public boolean isBarVisible(ItemStack stack) { return this.energy.kind() == EnergyKind.NONE ? super.isBarVisible(stack) : this.energy.barVisible(stack); }
        @Override public int getBarWidth(ItemStack stack) { return this.energy.kind() == EnergyKind.NONE ? super.getBarWidth(stack) : this.energy.barWidth(stack); }
        @Override public int getBarColor(ItemStack stack) { return this.energy.kind() == EnergyKind.NONE ? super.getBarColor(stack) : this.energy.barColor(stack); }
    }

    /** ItemToolAbilityPower: IBatteryItem (getDischargeRate 0). */
    public static class PoweredTool extends AbilityTool implements HbmChargeableItem {
        public PoweredTool(HbmToolProfile profile, HbmToolBehavior.ToolType type, long maxPower, long chargeRate, long consumption) {
            super(profile, type, Energy.power(maxPower, chargeRate, consumption));
        }

        @Override public long hbmCharge(ItemStack stack) { return energy().charge(stack); }
        @Override public long hbmCapacity(ItemStack stack) { return energy().max(); }
        @Override public long hbmChargeRate(ItemStack stack) { return energy().rate(); }
        @Override public long hbmDischargeRate(ItemStack stack) { return 0L; }
        @Override public void hbmSetCharge(ItemStack stack, long charge) { energy().setCharge(stack, charge); }
    }

    /** ItemSwordAbilityPower: IBatteryItem (getDischargeRate 0). */
    public static class PoweredSword extends AbilitySword implements HbmChargeableItem {
        public PoweredSword(HbmToolProfile profile, long maxPower, long chargeRate, long consumption) {
            super(profile, Energy.power(maxPower, chargeRate, consumption), List.of(), null);
        }

        @Override public long hbmCharge(ItemStack stack) { return energy().charge(stack); }
        @Override public long hbmCapacity(ItemStack stack) { return energy().max(); }
        @Override public long hbmChargeRate(ItemStack stack) { return energy().rate(); }
        @Override public long hbmDischargeRate(ItemStack stack) { return 0L; }
        @Override public void hbmSetCharge(ItemStack stack, long charge) { energy().setCharge(stack, charge); }
    }
}
