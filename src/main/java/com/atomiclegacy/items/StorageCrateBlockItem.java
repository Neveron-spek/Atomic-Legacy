package com.atomiclegacy.items;

import com.atomiclegacy.blocks.LegacyCrateLogic;
import com.atomiclegacy.blocks.StorageCrateBlock;
import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** [parity R4: crates] ItemBlockStorageCrate (crates and the safe); stack size 64 as in 1.7.10. */
public class StorageCrateBlockItem extends BlockItem {
    private final StorageCrateBlockEntity.Kind kind;

    public StorageCrateBlockItem(StorageCrateBlock block, Properties properties) {
        this(block, block.kind(), properties);
    }

    public StorageCrateBlockItem(Block block, StorageCrateBlockEntity.Kind kind, Properties properties) {
        super(block, properties);
        this.kind = kind;
    }

    public StorageCrateBlockEntity.Kind kind() {
        return this.kind;
    }

    /** CRATE_OPEN_HELD: placing requires sneaking. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        // [parity S2: configs] ItemBlockStorageCrate#onItemUse checks ServerConfig.CRATE_OPEN_HELD
        if (com.atomiclegacy.config.HbmServerParityConfig.crateOpenHeld()
                && context.getPlayer() != null && !context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!com.atomiclegacy.config.HbmServerParityConfig.crateOpenHeld()) { // [parity S2: configs]
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        return LegacyCrateLogic.useHeld(level, player, hand, this.kind);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        LegacyCrateLogic.appendTooltip(stack, context, tooltip);
    }
}
