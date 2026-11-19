package com.atomiclegacy.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public class HbmRailBlock extends RailBlock {
    public static final float VANILLA_SPEED = 0.4F;

    private final float maxSpeed;
    private final boolean flexible;
    private final boolean slopable;
    private final boolean booster;

    public HbmRailBlock(BlockBehaviour.Properties properties, float maxSpeed, boolean flexible, boolean slopable, boolean booster) {
        super(properties);
        this.maxSpeed = maxSpeed;
        this.flexible = flexible;
        this.slopable = slopable;
        this.booster = booster;
    }

    public float maxSpeed() {
        return this.maxSpeed;
    }

    public boolean flexible() {
        return this.flexible;
    }

    public boolean slopable() {
        return this.slopable;
    }

    // [parity R2] RailGeneric: isFlexibleRail returns !isPowered() (always true, super(false)) and canMakeSlopes
    // always returns true. The flexible/slopable flags only select the turned icon and the tooltip lines.
    @Override
    public boolean isFlexibleRail(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canMakeSlopes(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        return this.maxSpeed;
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (!this.booster) {
            return;
        }
        Vec3 motion = cart.getDeltaMovement();
        cart.setDeltaMovement(motion.x * 1.15F, motion.y * 1.15F, motion.z * 1.15F); // RailBooster.onMinecartPass
    }


    // [parity V4: dig speed] rail_wood = RailGeneric("axe", 0): only axes are effective in 1.7.10 (ItemPickaxe only knows
    // the vanilla rails); minecraft:rails stays for minecarts, so the pickaxe speed it brings is undone here.
    @Override
    protected float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        if (this == com.atomiclegacy.registry.HbmBlocks.RAIL_WOOD.get()
                && !player.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_DIG)) {
            return LegacyDigSpeed.progress(state, player, level, pos, 1.0F);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
