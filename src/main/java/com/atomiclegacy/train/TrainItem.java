package com.atomiclegacy.train;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.atomiclegacy.blocks.rail.IRailNTM;
import com.atomiclegacy.blocks.rail.IRailNTM.MoveContext;
import com.atomiclegacy.blocks.rail.IRailNTM.RailCheckType;
import com.atomiclegacy.items.LegacyItemComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** [parity S1: trains] com.hbm.items.special.ItemTrain (ItemEnumMulti over EnumTrainType, max stack 1, no creative tab). */
public class TrainItem extends Item {

    public enum EnumTrainType {
        //                                          Engine          Gauge               Max Speed   Accel.      Eng. Brake  Parking Brake
        CARGO_TRAM(CargoTramEntity::new,            "Electric",     "Standard Gauge",   "10m/s",    "0.2m/s²",  "<1m/s",    "Yes"),
        CARGO_TRAM_TRAILER(CargoTramTrailerEntity::new, null,       "Standard Gauge",   "Yes",      null,       null,       "No");

        public final Function<Level, ? extends RailCarEntity> train;
        public final String engine;
        public final String maxSpeed;
        public final String acceleration;
        public final String brakeThreshold;
        public final String parkingBrake;
        public final String gauge;

        EnumTrainType(Function<Level, ? extends RailCarEntity> train, String engine, String gauge, String maxSpeed, String acceleration, String brakeThreshold, String parkingBrake) {
            this.train = train;
            this.engine = engine;
            this.maxSpeed = maxSpeed;
            this.acceleration = acceleration;
            this.brakeThreshold = brakeThreshold;
            this.parkingBrake = parkingBrake;
            this.gauge = gauge;
        }
    }

    public TrainItem(Properties properties) {
        super(properties.stacksTo(1).component(LegacyItemComponents.TRAIN_TYPE.get(), 0));
    }

    public static EnumTrainType getType(ItemStack stack) {
        int damage = stack.getOrDefault(LegacyItemComponents.TRAIN_TYPE.get(), 0);
        EnumTrainType[] values = EnumTrainType.values();
        return values[Math.abs(damage % values.length)];
    }

    public static ItemStack stackFor(Item item, EnumTrainType type) {
        ItemStack stack = new ItemStack(item);
        stack.set(LegacyItemComponents.TRAIN_TYPE.get(), type.ordinal());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId() + "." + getType(stack).name().toLowerCase(Locale.US));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        EnumTrainType train = getType(stack);
        if (train.engine != null) list.add(line("Engine: ", train.engine));
        list.add(line("Gauge: ", train.gauge));
        if (train.maxSpeed != null) list.add(line("Max Speed: ", train.maxSpeed));
        if (train.acceleration != null) list.add(line("Acceleration: ", train.acceleration));
        if (train.brakeThreshold != null) list.add(line("Engine Brake Threshold: ", train.brakeThreshold));
        if (train.parkingBrake != null) list.add(line("Parking Brake: ", train.parkingBrake));
    }

    private static Component line(String label, String value) {
        return Component.literal(label).withStyle(ChatFormatting.GREEN).append(Component.literal(value).withStyle(ChatFormatting.RESET));
    }

    /** ItemEnumMulti.getSubItems (the item itself has no creative tab). */
    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (EnumTrainType type : EnumTrainType.values()) output.accept(stackFor(this, type));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block b = world.getBlockState(pos).getBlock();
        ItemStack stack = context.getItemInHand();

        if (b instanceof IRailNTM rail) {
            EnumTrainType type = getType(stack);
            RailCarEntity train = type.train.apply(world);

            if (train != null && train.getGauge() == rail.getGauge(world, pos.getX(), pos.getY(), pos.getZ())) {
                Vec3 hit = context.getClickLocation();
                train.setPos(hit.x, hit.y, hit.z);
                BlockPos anchor = train.getCurrentAnchorPos();
                train.setYRot(context.getPlayer() != null ? context.getPlayer().getYRot() : context.getRotation());
                LegacyVec3 corePos = train.getRelPosAlongRail(anchor, 0, new MoveContext(RailCheckType.CORE, 0));

                if (corePos != null) {
                    train.setPos(corePos.xCoord, corePos.yCoord, corePos.zCoord);
                    LegacyVec3 frontPos = train.getRelPosAlongRail(anchor, train.getLengthSpan(), new MoveContext(RailCheckType.FRONT, train.getCollisionSpan() - train.getLengthSpan()));
                    LegacyVec3 backPos = train.getRelPosAlongRail(anchor, -train.getLengthSpan(), new MoveContext(RailCheckType.BACK, train.getCollisionSpan() - train.getLengthSpan()));

                    if (frontPos != null && backPos != null) {
                        if (!world.isClientSide) {
                            train.setTrainRotation(RailCarEntity.generateYaw(frontPos, backPos), train.getXRot());
                            world.addFreshEntity(train);
                        }
                        stack.shrink(1);
                        return InteractionResult.sidedSuccess(world.isClientSide);
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }
}
