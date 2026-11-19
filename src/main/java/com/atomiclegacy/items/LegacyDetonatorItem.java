package com.atomiclegacy.items;

import com.atomiclegacy.blocks.LegacyBombs;
import com.atomiclegacy.blocks.LegacyBombs.BombReturnCode;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * 1.7.10 ItemDetonator, ItemLaserDetonator and ItemMultiDetonator. Chat lines, colours, sounds and the
 * IBomb return codes follow those classes; their English strings were hard-coded there and stay literal.
 */
public class LegacyDetonatorItem extends Item {
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String XS = "xValues";
    private static final String YS = "yValues";
    private static final String ZS = "zValues";
    private final String legacyId;

    public LegacyDetonatorItem(Properties properties, String legacyId) {
        super(properties.stacksTo(1));
        this.legacyId = legacyId;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    /** onItemUse: only the sneaking branch consumes the click; otherwise onItemRightClick runs. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || isLaser()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            if (isMulti()) {
                addLocation(stack, context.getClickedPos());
                player.sendSystemMessage(prefixed(stack, Component.literal("Position added!")
                        .withStyle(ChatFormatting.GREEN)));
            } else {
                setLocation(stack, context.getClickedPos());
                player.sendSystemMessage(prefixed(stack, Component.literal("Position set!")
                        .withStyle(ChatFormatting.GREEN)));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 2.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isLaser()) {
            return useLaser(level, player, stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel server = (ServerLevel) level;
        if (isMulti()) {
            useMulti(server, player, stack);
        } else {
            useSingle(server, player, stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** ItemDetonator.onItemRightClick. */
    private void useSingle(ServerLevel level, Player player, ItemStack stack) {
        BlockPos pos = location(stack);
        if (pos == null) {
            player.sendSystemMessage(prefixed(stack, Component.literal("No position set!")
                    .withStyle(ChatFormatting.RED)));
            return;
        }
        if (!LegacyBombs.isBomb(level, pos)) {
            player.sendSystemMessage(prefixed(stack, codeMessage(BombReturnCode.ERROR_NO_BOMB)));
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        BombReturnCode code = LegacyBombs.explode(level, pos, player);
        player.sendSystemMessage(prefixed(stack, codeMessage(code)));
    }

    /** ItemMultiDetonator.onItemRightClick. */
    private void useMulti(ServerLevel level, Player player, ItemStack stack) {
        List<BlockPos> positions = locations(stack);
        if (positions.isEmpty()) {
            player.sendSystemMessage(prefixed(stack, Component.literal("No position set!")
                    .withStyle(ChatFormatting.RED)));
            return;
        }
        if (player.isShiftKeyDown()) {
            clearLocations(stack);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 2.0F, 1.0F);
            player.sendSystemMessage(prefixed(stack, Component.literal("Locations cleared!")
                    .withStyle(ChatFormatting.RED)));
            return;
        }
        int triggered = 0;
        for (BlockPos pos : positions) {
            if (LegacyBombs.isBomb(level, pos) && LegacyBombs.explode(level, pos, player).wasSuccessful()) {
                triggered++;
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(prefixed(stack, Component.literal("Triggered " + triggered + "/" + positions.size() + "!")
                .withStyle(ChatFormatting.YELLOW)));
    }

    /** ItemLaserDetonator.onItemRightClick: Library.rayTrace(player, 500, 1); the result goes to the inform overlay. */
    private InteractionResultHolder<ItemStack> useLaser(Level level, Player player, ItemStack stack) {
        HitResult hit = player.pick(500.0D, 1.0F, false);
        BlockPos pos = hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : BlockPos.containing(hit.getLocation());
        if (level.isClientSide) {
            Vec3 vec = new Vec3(pos.getX() + 0.5D - player.getX(), pos.getY() + 0.5D - player.getY(),
                    pos.getZ() + 0.5D - player.getZ());
            double length = Math.min(vec.length(), 15.0D);
            vec = vec.normalize();
            for (int index = 0; index < length; index++) {
                double offset = level.random.nextDouble() * length + 3.0D;
                level.addParticle(DustParticleOptions.REDSTONE,
                        player.getX() + vec.x * offset, player.getY() + vec.y * offset, player.getZ() + vec.z * offset,
                        0.0D, 0.0D, 0.0D);
            }
            return InteractionResultHolder.success(stack);
        }
        ServerLevel server = (ServerLevel) level;
        if (LegacyBombs.isBomb(server, pos)) {
            BombReturnCode code = LegacyBombs.explode(server, pos, player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(codeMessage(code), true);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(codeMessage(BombReturnCode.ERROR_NO_BOMB), true);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isLaser()) {
            tooltip.add(Component.literal("Aim & click to detonate!").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (isMulti()) {
            tooltip.add(Component.literal("Shift right-click block to add position,").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("right-click to detonate!").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Shift right-click in the air to clear positions.").withStyle(ChatFormatting.GRAY));
            List<BlockPos> positions = locations(stack);
            if (positions.isEmpty()) {
                tooltip.add(Component.literal("No position set!").withStyle(ChatFormatting.RED));
            } else {
                for (BlockPos pos : positions) {
                    tooltip.add(Component.literal(pos.getX() + " / " + pos.getY() + " / " + pos.getZ())
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
            return;
        }
        tooltip.add(Component.literal("Shift right-click to set position,").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("right-click to detonate!").withStyle(ChatFormatting.GRAY));
        BlockPos pos = location(stack);
        tooltip.add(pos == null
                ? Component.literal("No position set!").withStyle(ChatFormatting.RED)
                : Component.literal("Linked to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                        .withStyle(ChatFormatting.YELLOW));
    }

    /** ChatBuilder.start("[").nextTranslation(name).next("] ") in dark aqua, followed by the message. */
    public static MutableComponent prefixed(ItemStack stack, Component message) {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(stack.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(message);
    }

    /** nextTranslation(ret.getUnlocalizedMessage()).color(ret.wasSuccessful() ? YELLOW : RED). */
    public static MutableComponent codeMessage(BombReturnCode code) {
        return Component.translatable(code.getUnlocalizedMessage())
                .withStyle(code.wasSuccessful() ? ChatFormatting.YELLOW : ChatFormatting.RED);
    }

    /**
     * Compatibility entry point for callers that only need to know whether an IBomb went off
     * (dead man's switch, diagnostics).
     */
    public static boolean detonate(Level level, BlockPos pos, @Nullable Player player) {
        return level instanceof ServerLevel server && LegacyBombs.explode(server, pos, player).wasSuccessful();
    }

    private boolean isLaser() {
        return legacyId.equals("detonator_laser");
    }

    private boolean isMulti() {
        return legacyId.equals("detonator_multi");
    }

    @Nullable
    public static BlockPos location(ItemStack stack) {
        CompoundTag tag = data(stack);
        return tag.contains(X) && tag.contains(Y) && tag.contains(Z)
                ? new BlockPos(tag.getInt(X), tag.getInt(Y), tag.getInt(Z))
                : null;
    }

    public static void setLocation(ItemStack stack, BlockPos pos) {
        CompoundTag tag = data(stack);
        tag.putInt(X, pos.getX());
        tag.putInt(Y, pos.getY());
        tag.putInt(Z, pos.getZ());
        setData(stack, tag);
    }

    public static void addLocation(ItemStack stack, BlockPos pos) {
        CompoundTag tag = data(stack);
        tag.putIntArray(XS, append(tag.getIntArray(XS), pos.getX()));
        tag.putIntArray(YS, append(tag.getIntArray(YS), pos.getY()));
        tag.putIntArray(ZS, append(tag.getIntArray(ZS), pos.getZ()));
        setData(stack, tag);
    }

    private static void clearLocations(ItemStack stack) {
        CompoundTag tag = data(stack);
        tag.putIntArray(XS, new int[0]);
        tag.putIntArray(YS, new int[0]);
        tag.putIntArray(ZS, new int[0]);
        setData(stack, tag);
    }

    public static List<BlockPos> locations(ItemStack stack) {
        CompoundTag tag = data(stack);
        int[] xs = tag.getIntArray(XS);
        int[] ys = tag.getIntArray(YS);
        int[] zs = tag.getIntArray(ZS);
        int length = Math.min(xs.length, Math.min(ys.length, zs.length));
        List<BlockPos> positions = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            positions.add(new BlockPos(xs[index], ys[index], zs[index]));
        }
        return positions;
    }

    private static int[] append(int[] values, int value) {
        int[] result = new int[values.length + 1];
        System.arraycopy(values, 0, result, 0, values.length);
        result[values.length] = value;
        return result;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void setData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
