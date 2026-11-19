package com.atomiclegacy.items;

import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * [parity I3] 1.7.10 ItemChainsaw(25, -0.05, tMatChainsaw, AXE, 5000, 1, 250, DIESEL, DIESEL_CRACK, KEROSENE, BIOFUEL,
 * GASOLINE, GASOLINE_LEADED, PETROIL, PETROIL_LEADED, COALGAS, COALGAS_LEADED) with SILK 0, RECURSION 2, CHAINSAW 1,
 * BEHEADER 0 and setShears(). The fuel is an untyped "fuel" integer (IFillableItem.tryFill caps at the fill rate,
 * providesFluid is false). The swing animation and held sound (IAnimatedItem / IHeldSoundProvider) are client-side.
 */
public final class LegacyChainsawItem extends HbmToolItems.AbilityTool
        implements IAnimatedItem<com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation> { // [parity U4: ItemChainsaw IAnimatedItem]
    public static final int CAPACITY = 5_000;
    public static final HbmToolProfile PROFILE = HbmToolProfile.create(HbmToolTier.CHAINSAW, 25.0F, -2.8F)
            .movement(-0.05D)
            .harvest(HbmToolBehavior.HarvestAbility.SILK, 0)
            .area(HbmToolBehavior.AreaAbility.RECURSION, 2)
            .weapon(HbmToolBehavior.WeaponAbility.CHAINSAW, 1)
            .weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0)
            .setShears();

    public LegacyChainsawItem() {
        super(PROFILE, HbmToolBehavior.ToolType.AXE, HbmToolItems.Energy.fuel(CAPACITY, 1, 250,
                "diesel", "diesel_crack", "kerosene", "biofuel", "gasoline", "gasoline_leaded",
                "petroil", "petroil_leaded", "coalgas", "coalgas_leaded"));
    }

    public static int fuelAmount(ItemStack stack) {
        return stack.getItem() instanceof LegacyChainsawItem chainsaw ? chainsaw.energy().fill(stack) : 0;
    }

    /** ItemToolAbilityFueled.getEmptyTool */
    public static ItemStack emptyTool(ItemStack stack) {
        if (stack.getItem() instanceof LegacyChainsawItem chainsaw) chainsaw.energy().setFill(stack, 0);
        return stack;
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new FuelHandler(stack, this);
    }

    private static final class FuelHandler implements IFluidHandlerItem {
        private final ItemStack stack;
        private final LegacyChainsawItem item;

        private FuelHandler(ItemStack stack, LegacyChainsawItem item) {
            this.stack = stack;
            this.item = item;
        }

        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? CAPACITY : 0; }

        @Override
        public boolean isFluidValid(int tank, FluidStack resource) {
            return tank == 0 && HbmFluids.fromNeoFluid(resource.getFluid())
                    .map(fluid -> this.item.energy().fuels().contains(fluid.name())).orElse(false);
        }

        /** ItemToolAbilityFueled.tryFill: min(amount, fillRate, maxFuel - fill). */
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            int fill = this.item.energy().fill(this.stack);
            int toFill = Math.min(resource.getAmount(), (int) this.item.energy().rate());
            toFill = Math.min(toFill, CAPACITY - fill);
            if (toFill > 0 && action.execute()) this.item.energy().setFill(this.stack, fill + toFill);
            return Math.max(toFill, 0);
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
        @Override public ItemStack getContainer() { return this.stack; }
    }

    // [parity U4: ItemChainsaw IAnimatedItem] SWING_ROT / SWING_TRANS read by the chainsaw item renderer
    /** ItemChainsaw#onEntitySwing: server side, while getItemDamage() < getMaxDamage() (always for the fueled tool). */
    @Override
    public boolean onEntitySwing(ItemStack stack, net.minecraft.world.entity.LivingEntity entityLiving, net.minecraft.world.InteractionHand hand) {
        if (!(entityLiving instanceof net.minecraft.server.level.ServerPlayer player)) {
            return false;
        }
        playAnimation(player, com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.SWING);
        return false;
    }

    /** ItemChainsaw#getAnimation. */
    @Override
    public com.atomiclegacy.render.anim.BusAnimation getAnimation(com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation type, ItemStack stack) {
        int forward = 150;
        int sideways = 100;
        int retire = 200;
        if (com.atomiclegacy.client.gun.HbmAnimations.getRelevantAnim() == null) {
            return new com.atomiclegacy.render.anim.BusAnimation()
                    .addBus("SWING_ROT", new com.atomiclegacy.render.anim.BusAnimationSequence()
                            .addPos(0, 0, 90, forward)
                            .addPos(45, 0, 90, sideways)
                            .addPos(0, 0, 0, retire))
                    .addBus("SWING_TRANS", new com.atomiclegacy.render.anim.BusAnimationSequence()
                            .addPos(0, 0, 3, forward)
                            .addPos(2, 0, 2, sideways)
                            .addPos(0, 0, 0, retire));
        }
        double[] rot = com.atomiclegacy.client.gun.HbmAnimations.getRelevantTransformation("SWING_ROT");
        double[] trans = com.atomiclegacy.client.gun.HbmAnimations.getRelevantTransformation("SWING_TRANS");
        if (System.currentTimeMillis() - com.atomiclegacy.client.gun.HbmAnimations.getRelevantAnim().startMillis < 50) return null;
        return new com.atomiclegacy.render.anim.BusAnimation()
                .addBus("SWING_ROT", new com.atomiclegacy.render.anim.BusAnimationSequence()
                        .addPos(rot[0], rot[1], rot[2], 0)
                        .addPos(0, 0, 90, forward)
                        .addPos(45, 0, 90, sideways)
                        .addPos(0, 0, 0, retire))
                .addBus("SWING_TRANS", new com.atomiclegacy.render.anim.BusAnimationSequence()
                        .addPos(trans[0], trans[1], trans[2], 0)
                        .addPos(0, 0, 3, forward)
                        .addPos(2, 0, 2, sideways)
                        .addPos(0, 0, 0, retire));
    }

    @Override
    public Class<com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation> getEnum() {
        return com.atomiclegacy.render.anim.AnimationEnums.ToolAnimation.class;
    }

    @Override
    public boolean shouldPlayerModelAim(ItemStack stack) {
        return false;
    }
}
