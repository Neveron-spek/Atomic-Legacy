package com.atomiclegacy.items;

import com.google.gson.stream.JsonWriter;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * [parity X1: custom machines] ItemCMStructure (structure_custommachine): click the custom machine anchor, then two
 * corners, the third click writes every non-air block between them relative to the anchor (rotated by the anchor's
 * facing) as "components" to hbmConfig/CMstructureOutput.txt. The tooltip and overlay texts are the original literals.
 */
public final class CustomMachineStructureItem extends Item {
    public CustomMachineStructureItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static File file() {
        return new File(CustomMachineConfigJSON.configHbmDir(), "CMstructureOutput.txt");
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Nullable
    public static BlockPos getAnchor(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag nbt = data.copyTag();
        return new BlockPos(nbt.getInt("anchorX"), nbt.getInt("anchorY"), nbt.getInt("anchorZ"));
    }

    public static void setAnchor(ItemStack stack, int x, int y, int z) {
        CompoundTag nbt = tag(stack);
        nbt.putInt("anchorX", x);
        nbt.putInt("anchorY", y);
        nbt.putInt("anchorZ", z);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    /** ForgeDirection.getOrientation(world.getBlockMetadata(anchor)) */
    private static Direction anchorDirection(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (state.hasProperty(BlockStateProperties.FACING)) return state.getValue(BlockStateProperties.FACING);
        return Direction.DOWN;
    }

    public static void writeToFile(File config, CompoundTag nbt, Level world) {
        int anchorX = nbt.getInt("anchorX");
        int anchorY = nbt.getInt("anchorY");
        int anchorZ = nbt.getInt("anchorZ");
        int x1 = nbt.getInt("x1");
        int y1 = nbt.getInt("y1");
        int z1 = nbt.getInt("z1");
        int x2 = nbt.getInt("x2");
        int y2 = nbt.getInt("y2");
        int z2 = nbt.getInt("z2");
        Direction dir = anchorDirection(world.getBlockState(new BlockPos(anchorX, anchorY, anchorZ)));
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        try {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(Files.newOutputStream(config.toPath()), StandardCharsets.UTF_8));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("components").beginArray();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {

                        int compY = y - anchorY;
                        int compX = 0;
                        int compZ = 0;

                        if (dir == Direction.SOUTH) {
                            compX = anchorX - x;
                            compZ = anchorZ - z;
                        }
                        if (dir == Direction.NORTH) {
                            compX = x - anchorX;
                            compZ = z - anchorZ;
                        }

                        if (dir == Direction.WEST) {
                            compZ = x - anchorX;
                            compX = anchorZ - z;
                        }
                        if (dir == Direction.EAST) {
                            compZ = anchorX - x;
                            compX = z - anchorZ;
                        }

                        if (x == anchorX && y == anchorY && z == anchorZ) continue;
                        BlockState state = world.getBlockState(new BlockPos(x, y, z));
                        if (state.isAir()) continue;

                        writer.beginObject().setIndent("");
                        writer.name("block").value(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                        writer.name("x").value(compX);
                        writer.name("y").value(compY);
                        writer.name("z").value(compZ);
                        writer.name("metas").beginArray().value(CustomMachineConfigJSON.legacyMeta(state)).endArray();
                        writer.endObject().setIndent("  ");
                    }
                }
            }
            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (IOException e) {
            AtomicLegacy.LOGGER.error("Failed to write {}", config, e);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (world.getBlockState(pos).is(HbmBlocks.CUSTOM_MACHINE_ANCHOR.get())) {
            setAnchor(stack, x, y, z);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        if (getAnchor(stack) == null) {
            return InteractionResult.PASS;
        }
        CompoundTag nbt = tag(stack);
        if (!nbt.contains("x1")) {
            nbt.putInt("x1", x);
            nbt.putInt("y1", y);
            nbt.putInt("z1", z);
        } else if (!nbt.contains("x2")) {
            nbt.putInt("x2", x);
            nbt.putInt("y2", y);
            nbt.putInt("z2", z);
        } else {
            if (!world.isClientSide) writeToFile(file(), nbt, world);
            nbt.remove("x1");
            nbt.remove("y1");
            nbt.remove("z1");
            nbt.remove("x2");
            nbt.remove("y2");
            nbt.remove("z2");
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Click Custom Machine Structure Positioning Anchor to").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Confirm the location of the custom machine core block.").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Output all blocks between Position1 and Position2 with").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("metadata to \"CMstructureOutput.txt\" in hbmConfig.").withStyle(ChatFormatting.YELLOW));
    }

    /** printHook: the overlay lines while the wand is held. */
    public static List<String> overlayLines(ItemStack stack) {
        List<String> text = new ArrayList<>();
        BlockPos anchor = getAnchor(stack);

        if (anchor == null) {
            text.add(ChatFormatting.RED + "No Anchor");
        } else {
            CompoundTag nbt = tag(stack);
            text.add(ChatFormatting.GOLD + "Anchor: " + anchor.getX() + " / " + anchor.getY() + " / " + anchor.getZ());
            if (nbt.contains("x1")) {
                text.add(ChatFormatting.YELLOW + "Position1: " + nbt.getInt("x1") + " / " + nbt.getInt("y1") + " / " + nbt.getInt("z1"));
            }
            if (nbt.contains("x2")) {
                text.add(ChatFormatting.YELLOW + "Position2: " + nbt.getInt("x2") + " / " + nbt.getInt("y2") + " / " + nbt.getInt("z2"));
            }
        }
        return text;
    }
}
