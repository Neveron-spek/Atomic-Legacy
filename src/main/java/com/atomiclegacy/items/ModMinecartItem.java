package com.atomiclegacy.items;

import java.util.Locale;

import com.atomiclegacy.entity.cart.MinecartCrateEntity;
import com.atomiclegacy.entity.cart.MinecartDestroyerEntity;
import com.atomiclegacy.entity.cart.MinecartOreEntity;
import com.atomiclegacy.entity.cart.MinecartPowderEntity;
import com.atomiclegacy.entity.cart.MinecartSemtexEntity;
import com.atomiclegacy.entity.cart.NtmMinecartEntity;
import com.atomiclegacy.registry.HbmItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * [parity S1: carts] com.hbm.items.tool.ItemModMinecart: one item, the cart type in the old damage value
 * ({@link LegacyItemComponents#CART_TYPE}) and the EnumCartBase in the old "cartBase" NBT
 * ({@link LegacyItemComponents#CART_BASE}). Max stack 4, placed on vanilla-style rails, with its own dispenser behaviour.
 */
public class ModMinecartItem extends Item {

    public enum EnumCartBase {
        VANILLA,
        WOOD,
        STEEL,
        PAINTED
    }

    public enum EnumMinecart {
        EMPTY(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED),
        CRATE(EnumCartBase.VANILLA),
        DESTROYER(EnumCartBase.STEEL, EnumCartBase.PAINTED),
        POWDER(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED),
        SEMTEX(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED);

        public final int types;

        EnumMinecart(EnumCartBase... types) {
            int mask = 0;
            for (EnumCartBase type : types) {
                mask |= (1 << type.ordinal());
            }
            this.types = mask;
        }

        public boolean supportsBase(int type) {
            return (this.types & (1 << type)) > 0;
        }

        public boolean supportsBase(EnumCartBase type) {
            return supportsBase(type.ordinal());
        }
    }

    public ModMinecartItem(Properties properties) {
        super(properties.stacksTo(4)
                .component(LegacyItemComponents.CART_TYPE.get(), 0)
                .component(LegacyItemComponents.CART_BASE.get(), 0));
        DispenserBlock.registerBehavior(this, DISPENSE_BEHAVIOR);
    }

    public static EnumMinecart getType(ItemStack stack) {
        int damage = stack.getOrDefault(LegacyItemComponents.CART_TYPE.get(), 0);
        EnumMinecart[] values = EnumMinecart.values();
        return values[Math.abs(damage % values.length)]; // EnumUtil.grabEnumSafely
    }

    public static EnumCartBase getBaseType(ItemStack stack) {
        int meta = stack.getOrDefault(LegacyItemComponents.CART_BASE.get(), 0);
        EnumCartBase[] values = EnumCartBase.values();
        return values[Math.abs(meta % values.length)];
    }

    public static ItemStack createCartItem(EnumCartBase base, EnumMinecart cart) {
        ItemStack stack = new ItemStack(HbmItems.CART.get(), 1);
        stack.set(LegacyItemComponents.CART_TYPE.get(), cart.ordinal());
        stack.set(LegacyItemComponents.CART_BASE.get(), base.ordinal());
        return stack;
    }

    /** item model predicate: base * 10 + type (two-layer icon: cart.&lt;base&gt; + cart_overlay.&lt;type&gt;). */
    public static float modelVariant(ItemStack stack) {
        return getBaseType(stack).ordinal() * 10 + getType(stack).ordinal();
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId() + "." + getType(stack).name().toLowerCase(Locale.US));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (EnumMinecart cart : EnumMinecart.values()) {
            for (EnumCartBase base : EnumCartBase.values()) {
                if (cart.supportsBase(base)) {
                    output.accept(createCartItem(base, cart));
                }
            }
        }
    }

    private static final DefaultDispenseItemBehavior DISPENSE_BEHAVIOR = new DefaultDispenseItemBehavior() {
        private final DefaultDispenseItemBehavior behaviourDefaultDispenseItem = new DefaultDispenseItemBehavior();

        @Override
        public ItemStack execute(BlockSource source, ItemStack stack) {
            Direction enumfacing = source.state().getValue(DispenserBlock.FACING);
            Level world = source.level();
            double x = source.center().x + enumfacing.getStepX() * 1.125D;
            double y = source.center().y + enumfacing.getStepY() * 1.125D;
            double z = source.center().z + enumfacing.getStepZ() * 1.125D;
            BlockPos front = source.pos().relative(enumfacing);
            BlockState block = world.getBlockState(front);
            double yOffset;

            if (BaseRailBlock.isRail(block)) {
                yOffset = 0.0D;
            } else {
                if (!block.isAir() || !BaseRailBlock.isRail(world.getBlockState(front.below()))) {
                    return this.behaviourDefaultDispenseItem.dispense(source, stack);
                }
                yOffset = -1.0D;
            }

            NtmMinecartEntity entityminecart = createMinecart(world, x, y + yOffset, z, stack);

            if (stack.has(DataComponents.CUSTOM_NAME)) {
                entityminecart.setCustomName(stack.getHoverName());
            }

            world.addFreshEntity(entityminecart);
            stack.shrink(1);
            return stack;
        }
    };

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (BaseRailBlock.isRail(world.getBlockState(context.getClickedPos()))) {
            ItemStack stack = context.getItemInHand();
            if (!world.isClientSide) {
                Vec3 hit = context.getClickLocation();
                NtmMinecartEntity entityminecart = createMinecart(world, hit.x, hit.y, hit.z, stack);
                if (stack.has(DataComponents.CUSTOM_NAME)) {
                    entityminecart.setCustomName(stack.getHoverName());
                }
                world.addFreshEntity(entityminecart);
            }
            stack.shrink(1);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    public static NtmMinecartEntity createMinecart(Level world, double x, double y, double z, ItemStack stack) {
        EnumMinecart type = getType(stack);
        EnumCartBase base = getBaseType(stack);
        return switch (type) {
            case CRATE -> new MinecartCrateEntity(world, x, y, z, base, stack);
            case DESTROYER -> new MinecartDestroyerEntity(world, x, y, z, base);
            case EMPTY -> new MinecartOreEntity(world, x, y, z, base);
            case POWDER -> new MinecartPowderEntity(world, x, y, z, base);
            case SEMTEX -> new MinecartSemtexEntity(world, x, y, z, base);
        };
    }
}
