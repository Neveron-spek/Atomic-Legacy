package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.PwrBlock;
import com.atomiclegacy.network.PwrPrinterOpenPayload;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * [parity S3: slice printer] GUIScreenSlicePrinter: draws one PWR layer per frame on a magenta screen with the
 * original isometric transform, saves the whole frame as printer/&lt;date&gt;/slice_&lt;y&gt;.png (magenta made
 * transparent), and after the top layer posts "Slices saved to: .minecraft/printer/&lt;date&gt;" and closes.
 */
public final class PwrPrinterScreen extends Screen {
    private static final int MAGENTA = 0xFFFF00FF;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    private final PwrPrinterOpenPayload snapshot;
    private final int sizeX, sizeY, sizeZ;
    private final Direction dir;
    private final String dirname;
    private final Map<PwrBlock.Kind, BlockState> states = new EnumMap<>(PwrBlock.Kind.class);
    private int yIndex;

    private PwrPrinterScreen(PwrPrinterOpenPayload snapshot) {
        super(Component.empty());
        this.snapshot = snapshot;
        this.sizeX = snapshot.sizeX();
        this.sizeY = snapshot.sizeY();
        this.sizeZ = snapshot.sizeZ();
        this.dir = snapshot.inward();
        this.dirname = DATE_FORMAT.format(new Date());
        // TileEntityBlockPWR#block: the printer renders the original component, not the assembled pwr_block.
        for (Block block : List.of(HbmBlocks.PWR_BLOCK.get(), HbmBlocks.PWR_CASING.get(), HbmBlocks.PWR_CHANNEL.get(),
                HbmBlocks.PWR_CONTROL.get(), HbmBlocks.PWR_CONTROLLER.get(), HbmBlocks.PWR_FUELROD.get(), HbmBlocks.PWR_FUEL.get(),
                HbmBlocks.PWR_HEATEX.get(), HbmBlocks.PWR_HEATSINK.get(), HbmBlocks.PWR_NEUTRON_SOURCE.get(),
                HbmBlocks.PWR_PORT.get(), HbmBlocks.PWR_REFLECTOR.get())) {
            if (block instanceof PwrBlock pwr) {
                this.states.putIfAbsent(pwr.kind(), block.defaultBlockState());
            }
        }
    }

    public static void open(PwrPrinterOpenPayload snapshot) {
        Minecraft.getInstance().setScreen(new PwrPrinterScreen(snapshot));
    }

    /** GuiScreen#drawScreen does not draw the default background; the magenta fill covers everything. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        graphics.fill(0, 0, width, height, MAGENTA);

        // Once we've reached the top slice, close the GUI
        if (yIndex >= sizeY) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("Slices saved to: .minecraft/printer/" + dirname), false);
            }
            onClose();
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        setupRotation(pose);
        for (PwrPrinterOpenPayload.Part part : snapshot.parts()) {
            if (part.y() != yIndex) continue;
            BlockState state = states.get(part.kind());
            if (state == null) continue;
            int x = part.x();
            int z = part.z();
            int dx = x;
            int dz = z;
            // swizzle instead of rotating, so the PWR controller faces the correct rotation
            if (dir == Direction.WEST) {
                dx = sizeZ - 1 - z;
                dz = x;
            } else if (dir == Direction.SOUTH) {
                dx = sizeX - 1 - x;
                dz = sizeZ - 1 - z;
            } else if (dir == Direction.EAST) {
                dx = z;
                dz = sizeX - 1 - x;
            }
            pose.pushPose();
            pose.translate(dx, 0, dz);
            mc.getBlockRenderer().renderSingleBlock(state, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        graphics.flush();
        pose.popPose();

        saveScreenshot(mc);
        yIndex++;
    }

    private void setupRotation(PoseStack pose) {
        double scale = -24;
        pose.translate(width / 2, height / 2 - 36, 400);
        pose.scale((float) scale, (float) scale, (float) scale);
        pose.scale(1, 1, 0.5F); // incredible flattening power
        pose.mulPose(Axis.XP.rotationDegrees(-30));
        pose.mulPose(Axis.YP.rotationDegrees(225));
        if (dir == Direction.WEST || dir == Direction.EAST) {
            pose.translate(sizeX / -2D, -sizeY / 2D, sizeZ / -2D);
        } else {
            pose.translate(sizeZ / -2D, -sizeY / 2D, sizeX / -2D);
        }
    }

    /** GUIScreenWikiRender#saveScreenshot(printerDir, dirname, "slice_" + yIndex + ".png", full frame, 0xFFFF00FF). */
    private void saveScreenshot(Minecraft mc) {
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            for (int iy = 0; iy < image.getHeight(); iy++) {
                for (int ix = 0; ix < image.getWidth(); ix++) {
                    if (image.getPixelRGBA(ix, iy) == MAGENTA) {
                        image.setPixelRGBA(ix, iy, 0);
                    }
                }
            }
            Path directory = mc.gameDirectory.toPath().resolve("printer").resolve(dirname);
            Files.createDirectories(directory);
            image.writeToFile(directory.resolve("slice_" + yIndex + ".png"));
        } catch (Exception ex) {
            AtomicLegacy.LOGGER.warn("Failed to save NTM screenshot", ex);
        }
    }
}
