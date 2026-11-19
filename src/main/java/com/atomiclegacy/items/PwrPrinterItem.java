package com.atomiclegacy.items;

import com.atomiclegacy.blocks.PwrBlock;
import com.atomiclegacy.tileentity.PwrBlockEntity;
import com.atomiclegacy.tileentity.PwrControllerBlockEntity;
import com.atomiclegacy.network.PwrPrinterOpenPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PwrPrinterItem extends Item {

    public PwrPrinterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOnPwr(stack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useOnPwr(context.getItemInHand(), context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // [parity S3: slice printer] ItemPWRPrinter#addInformation (hardcoded English, gray tooltip line)
        tooltip.add(Component.literal("Use on a constructed PWR controller to generate construction diagrams").withStyle(ChatFormatting.GRAY));
    }

    private static InteractionResult useOnPwr(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(clicked);
        if (!(blockEntity instanceof PwrControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }
        // [parity M2: PWR printer] ItemPWRPrinter#onItemUse is silent: no chat messages and no sound
        if (!level.isClientSide) {
            // [parity S3: slice printer] findBounds flood-fills only assembled BlockPWR cells, so an unassembled
            // reactor prints just the controller; the bounds were never written to the item.
            Direction facing = level.getBlockState(clicked).getValue(PwrBlock.FACING);
            Scan scan = controller.assembled()
                    ? scan(level, clicked, facing)
                    : new Scan(clicked.immutable(), clicked, clicked, 0, 0, 0, facing.getOpposite(), List.of());
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, createOpenPayload(scan));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Scan scan(Level level, BlockPos controllerPos, Direction controllerFacing) {
        Direction dir = controllerFacing.getOpposite();
        BlockPos start = controllerPos.relative(dir);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<ScannedPart> scannedParts = new ArrayList<>();
        // ItemPWRPrinter seeded the controller into its visited set before it
        // entered the vessel.  This keeps it in the diagram without counting
        // it as a reactor part or traversing back out through the front face.
        visited.add(controllerPos.immutable());
        queue.add(start);
        int rods = 0;
        int ports = 0;
        // The legacy printer included the controller in its bounds and slice
        // images, but only followed the reactor-facing side into the vessel.
        BlockPos min = controllerPos;
        BlockPos max = controllerPos;
        int parts = 0;
        while (!queue.isEmpty() && visited.size() < 4096) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            BlockStateInfo info = blockInfo(level, pos);
            if (!info.isPwr()) {
                continue;
            }
            parts++;
            scannedParts.add(new ScannedPart(pos.immutable(), info.kind()));
            min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
            if (info.kind() == PwrBlock.Kind.FUEL) {
                rods++;
            }
            if (info.kind() == PwrBlock.Kind.PORT) {
                ports++;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && blockInfo(level, next).isPwr()) {
                    queue.add(next);
                }
            }
        }
        return new Scan(controllerPos.immutable(), min, max, parts, rods, ports, dir, List.copyOf(scannedParts));
    }

    private static BlockStateInfo blockInfo(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PwrBlockEntity part) {
            return new BlockStateInfo(true, part.originalKind());
        }
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof PwrBlock pwr) {
            return new BlockStateInfo(true, pwr.kind());
        }
        return new BlockStateInfo(false, PwrBlock.Kind.BLOCK);
    }

    private static PwrPrinterOpenPayload createOpenPayload(Scan scan) {
        BlockPos min = scan.min();
        List<PwrPrinterOpenPayload.Part> parts = new ArrayList<>(scan.scannedParts().size() + 1);
        // The controller is part of every diagram, matching ItemPWRPrinter's
        // old whitelist of pwr_block + pwr_controller.
        parts.add(new PwrPrinterOpenPayload.Part(
                scan.controllerPos().getX() - min.getX(),
                scan.controllerPos().getY() - min.getY(),
                scan.controllerPos().getZ() - min.getZ(),
                PwrBlock.Kind.CONTROLLER
        ));
        for (ScannedPart part : scan.scannedParts()) {
            parts.add(new PwrPrinterOpenPayload.Part(
                    part.pos().getX() - min.getX(),
                    part.pos().getY() - min.getY(),
                    part.pos().getZ() - min.getZ(),
                    part.kind()
            ));
        }
        return new PwrPrinterOpenPayload(
                scan.max().getX() - min.getX() + 1,
                scan.max().getY() - min.getY() + 1,
                scan.max().getZ() - min.getZ() + 1,
                scan.inward(),
                parts
        );
    }

    private record BlockStateInfo(boolean isPwr, PwrBlock.Kind kind) {
    }

    private record ScannedPart(BlockPos pos, PwrBlock.Kind kind) {
    }

    private record Scan(
            BlockPos controllerPos,
            BlockPos min,
            BlockPos max,
            int parts,
            int rods,
            int ports,
            Direction inward,
            List<ScannedPart> scannedParts
    ) {
    }
}
