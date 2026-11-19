package com.atomiclegacy.items;

import com.atomiclegacy.satellite.SatelliteSavedData;
import com.atomiclegacy.network.SatellitePanelOpenPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * ItemSatInterface (sat_coord) and ItemSatDesignator (sat_designator).
 * [parity P2: satellites] sat_coord opens GUIScreenSatCoord; sat_designator
 * ray-traces 300 blocks and forwards the hit to SatelliteBase#onCoordAction;
 * both only carry the ItemSatChip frequency tooltip.  INTERFACE is the
 * removed 1.7.10 sat_interface (MainRegistry ignoreMappings), kept only so
 * existing stacks load; it is hidden from creative tabs and has no recipe.
 */
public final class LegacySatelliteToolItem extends SatelliteChipItem {
    public enum Mode { COORDINATE, LASER, INTERFACE }

    /** ItemSatInterface.KEY_NBT_CONNECTED */
    public static final String KEY_NBT_CONNECTED = "connected";

    private final Mode mode;

    public LegacySatelliteToolItem(Properties properties, Mode mode) {
        super(properties.stacksTo(1), "");
        this.mode = mode;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (mode == Mode.INTERFACE) {
            if (!level.isClientSide && level instanceof ServerLevel serverLevel
                    && player instanceof ServerPlayer serverPlayer) {
                int frequency = frequency(stack);
                String kind = SatelliteSavedData.get(serverLevel).satellite(frequency)
                        .map(record -> record.kind().id())
                        .orElse("");
                PacketDistributor.sendToPlayer(serverPlayer,
                        new SatellitePanelOpenPayload(
                                frequency,
                                kind,
                                hand == InteractionHand.OFF_HAND));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (mode == Mode.COORDINATE) {
            // ItemSatInterface#onItemRightClick: open the GUI client-side only.
            if (level.isClientSide) {
                com.atomiclegacy.inventory.gui.SatelliteCoordScreen.open(player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            SatelliteSavedData data = SatelliteSavedData.get(serverLevel);
            SatelliteSavedData.SatelliteRecord record = data.satellite(frequency(stack)).orElse(null);
            if (record != null) {
                // Library.rayTrace(player, 300, 1) keeps the last block of a
                // miss, so the end position is used just like a hit.
                HitResult hit = player.pick(300.0D, 1.0F, false);
                if (hit instanceof BlockHitResult blockHit) {
                    BlockPos target = blockHit.getBlockPos().relative(blockHit.getDirection());
                    data.performCoordinateAction(serverLevel, serverPlayer, record, target);
                }
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    /** ItemSatInterface#onUpdate: the held coordinate interface caches whether its frequency is in orbit. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (mode != Mode.COORDINATE || level.isClientSide || !(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player) || player.getMainHandItem() != stack) {
            return;
        }
        boolean connected = SatelliteSavedData.get(serverLevel).isFrequencyTaken(frequency(stack));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(KEY_NBT_CONNECTED) || tag.getBoolean(KEY_NBT_CONNECTED) != connected) {
            tag.putBoolean(KEY_NBT_CONNECTED, connected);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static boolean isConnected(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(KEY_NBT_CONNECTED);
    }

    public boolean isInterface() {
        return this.mode == Mode.INTERFACE;
    }

    public boolean isCoordinateInterface() {
        return this.mode == Mode.COORDINATE;
    }
}
