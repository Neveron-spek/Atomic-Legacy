package com.atomiclegacy.items;

import com.atomiclegacy.network.PlayerInformPayload;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * [parity N1: oil detector] Line-by-line port of 1.7.10 ItemOilDetector (936c60c).
 * The port-only deep reservoir / Create Diesel Generators / bedrock report was removed.
 * ore_deepslate_oil is the port's deepslate variant of ore_oil and counts as ore_oil.
 */
public class OilDetectorItem extends Item {
    private static final int ID_DETONATOR = 8;

    public OilDetectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.atomiclegacy.oil_detector.desc1"));
        tooltipComponents.add(Component.translatable("item.atomiclegacy.oil_detector.desc2"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        boolean oil = false;
        boolean direct = false;
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();

        if (column(level, x, y, z, 5)) direct = true;
        if (column(level, x + 5, y, z, 5)) oil = true;
        if (column(level, x - 5, y, z, 5)) oil = true;
        if (column(level, x, y, z + 5, 5)) oil = true;
        if (column(level, x, y, z - 5, 5)) oil = true;
        if (column(level, x + 10, y, z, 10)) oil = true;
        if (column(level, x - 10, y, z, 10)) oil = true;
        if (column(level, x, y, z + 10, 10)) oil = true;
        if (column(level, x, y, z - 10, 10)) oil = true;
        if (column(level, x + 5, y, z + 5, 5)) oil = true;
        if (column(level, x - 5, y, z + 5, 5)) oil = true;
        if (column(level, x + 5, y, z - 5, 5)) oil = true;
        if (column(level, x - 5, y, z - 5, 5)) oil = true;

        if (direct) {
            oil = true;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            String key;
            int color;
            if (direct) {
                key = "item.atomiclegacy.oil_detector.bullseye";
                color = 0x00AA00; // DARK_GREEN
            } else if (oil) {
                key = "item.atomiclegacy.oil_detector.detected";
                color = 0xFFAA00; // GOLD
            } else {
                key = "item.atomiclegacy.oil_detector.noOil";
                color = 0xFF5555; // RED
            }
            // PlayerInformPacket without millis -> displayTooltip(msg, 1000, ID_DETONATOR)
            PacketDistributor.sendToPlayer(serverPlayer, PlayerInformPayload.translated(ID_DETONATOR, key, color, 1000));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.swing(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** {@code for(int i = y + 15; i > bottom; i--) if(world.getBlock(x, i, z) == ModBlocks.ore_oil)}. */
    private static boolean column(Level level, int x, int y, int z, int bottom) {
        boolean found = false;
        for (int i = y + 15; i > bottom; i--) {
            if (isOreOil(level.getBlockState(new BlockPos(x, i, z)))) {
                found = true;
            }
        }
        return found;
    }

    private static boolean isOreOil(BlockState state) {
        return state.is(HbmBlocks.ORE_OIL.get()) || state.is(HbmBlocks.ORE_DEEPSLATE_OIL.get());
    }
}
