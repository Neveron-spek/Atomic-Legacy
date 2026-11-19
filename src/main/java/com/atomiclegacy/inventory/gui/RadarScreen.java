package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.tileentity.RadarTarget;
import com.atomiclegacy.inventory.container.RadarMenu;
import com.atomiclegacy.network.RadarControlPayload;
import com.atomiclegacy.network.RadarCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Direct visual layout port of GUIMachineRadarNT's main display. */
public final class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_radar_nt.png");
    private int lastMouseX;
    private int lastMouseY;

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 216;
        this.imageHeight = 234;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderHover(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        graphics.blit(TEXTURE, leftPos - 14, topPos + 84, 224, 0, 14, 66, 256, 256);
        graphics.blit(TEXTURE, leftPos - 14, topPos + 154, 224, 66, 14, 36, 256, 256);

        LegacyMachineBlockEntity machine = menu.machine();
        long power = menu.power();
        if (power > 0) {
            int bar = (int) (power * 200L / Math.max(1L, menu.powerCapacity()));
            graphics.blit(TEXTURE, leftPos + 8, topPos + 221, 0, 234, bar, 16, 256, 256);
        }
        if (machine == null) return;

        drawToggle(graphics, 0, 88, machine.radarScanMissiles());
        drawToggle(graphics, 1, 98, machine.radarScanShells());
        drawToggle(graphics, 2, 108, machine.radarScanPlayers());
        drawToggle(graphics, 3, 118, machine.radarSmartMode());
        drawToggle(graphics, 4, 128, machine.radarRedMode());
        drawToggle(graphics, 5, 138, machine.radarShowMap());
        if (power < machine.radarConsumptionValue()) return;

        // [parity g2] jammed: 5x5 random noise tiles instead of map, sweep and blips
        if (machine.radarJammed()) {
            if (minecraft == null || minecraft.level == null) return;
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    graphics.blit(TEXTURE, leftPos + 8 + i * 40, topPos + 17 + j * 40,
                            216, 118 + minecraft.level.random.nextInt(81), 40, 40, 256, 256);
                }
            }
            return;
        }

        if (machine.radarShowMap()) drawMap(graphics);
        drawSweep(graphics, machine.radarRotation(partialTick));
        for (RadarTarget target : menu.targets()) drawTarget(graphics, target, machine.radarRangeValue());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The original main radar GUI has no title or player inventory labels.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int action = actionAt(mouseX, mouseY);
        if (action < 0) return super.mouseClicked(mouseX, mouseY, button);
        PacketDistributor.sendToServer(new RadarControlPayload(menu.blockPos(), action));
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    private void drawToggle(GuiGraphics graphics, int action, int y, boolean enabled) {
        LegacyMachineBlockEntity machine = menu.machine();
        boolean noisyState = enabled;
        if (machine != null && machine.radarJammed() && minecraft != null && minecraft.level != null
                && minecraft.level.random.nextBoolean()) noisyState = !enabled;
        if (noisyState) graphics.blit(TEXTURE, leftPos - 10, topPos + y, 238, 4 + action * 10, 8, 8, 256, 256);
    }

    private void drawMap(GuiGraphics graphics) {
        for (int index = 0; index < 40_000; index++) {
            byte value = menu.mapCell(index);
            if (value <= 0) continue;
            // [parity g2] Tessellator#setColorOpaque_I(((b - 50) * 255 / 78) << 8): signed byte, no clamping
            int color = 0xFF000000 | ((((value - 50) * 255 / 78) << 8) & 0xFFFFFF);
            int x = index % 200;
            int z = index / 200;
            graphics.fill(leftPos + 8 + x, topPos + 17 + z, leftPos + 9 + x, topPos + 18 + z, color);
        }
    }

    /** [parity g2] gradient quad: centre (alpha 0), tr (alpha 255), tl = tr + 0.25 rad (alpha 0), bl (0,-5) (alpha 0). */
    private void drawSweep(GuiGraphics graphics, float rotation) {
        float rot = (float) -Math.toRadians(rotation + 180.0F);
        float cx = leftPos + 108;
        float cy = topPos + 117;
        float[] tr = rotateZ(100, 0, rot);
        float[] tl = rotateZ(100, 0, rot + 0.25F);
        float[] bl = rotateZ(0, -5, rot);
        graphics.flush();
        Matrix4f pose = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(pose, cx, cy, 0.0F).setColor(0, 255, 0, 0);
        buffer.addVertex(pose, cx + tr[0], cy + tr[1], 0.0F).setColor(0, 255, 0, 255);
        buffer.addVertex(pose, cx + tl[0], cy + tl[1], 0.0F).setColor(0, 255, 0, 0);
        buffer.addVertex(pose, cx + bl[0], cy + bl[1], 0.0F).setColor(0, 255, 0, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Vec3#rotateAroundZ of 1.7.10. */
    private static float[] rotateZ(double x, double y, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new float[] {(float) (x * cos + y * sin), (float) (y * cos - x * sin)};
    }

    private void drawTarget(GuiGraphics graphics, RadarTarget target, int range) {
        double x = (target.x() - menu.blockPos().getX()) / ((double) range * 2.0D + 1.0D) * (200D - 8D) - 4D;
        double z = (target.z() - menu.blockPos().getZ()) / ((double) range * 2.0D + 1.0D) * (200D - 8D) - 4D;
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + 108 + x, topPos + 117 + z, 0.0D);
        graphics.blit(TEXTURE, 0, 0, 216, target.blipLevel() * 8, 8, 8, 256, 256);
        graphics.pose().popPose();
    }

    private void renderHover(GuiGraphics graphics, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, 8, 221, 200, 7)) {
            graphics.renderComponentTooltip(font, List.of(K3GuiText.electricity(menu.power(), menu.powerCapacity())), mouseX, mouseY);
        }
        int action = actionAt(mouseX, mouseY);
        if (action >= 0) {
            String key = switch (action) {
                case 0 -> "gui.atomiclegacy.radar.detect_missiles";
                case 1 -> "gui.atomiclegacy.radar.detect_shells";
                case 2 -> "gui.atomiclegacy.radar.detect_players";
                case 3 -> "gui.atomiclegacy.radar.smart_mode";
                case 4 -> "gui.atomiclegacy.radar.redstone_mode";
                case 5 -> "gui.atomiclegacy.radar.show_map";
                case 6 -> "gui.atomiclegacy.radar.clear_map"; // [parity g2] (-10,178) "clear"
                default -> "gui.atomiclegacy.radar.configure"; // [parity g2] (-10,158) "gui1"
            };
            graphics.renderComponentTooltip(font, radarControlTooltip(action, key), mouseX, mouseY);
        }
        RadarTarget target = targetAt(mouseX, mouseY);
        if (target != null) {
            int range = menu.machine().radarRangeValue();
            int x = leftPos + (int) ((target.x() - menu.blockPos().getX()) / ((double) range * 2.0D + 1.0D) * (200D - 8D)) + 108;
            int z = topPos + (int) ((target.z() - menu.blockPos().getZ()) / ((double) range * 2.0D + 1.0D) * (200D - 8D)) + 117;
            graphics.renderComponentTooltip(font, List.of(Component.translatable(target.name()),
                    Component.literal(target.x() + " / " + target.z()),
                    Component.literal("Alt.: " + target.y())), x, z);
            return;
        }
        if (inside(mouseX, mouseY, 8, 17, 200, 200) && menu.machine() != null) {
            int x = (int) ((mouseX - leftPos - 108) * ((double) menu.machine().radarRangeValue() * 2.0D + 1.0D) / 192.0D + menu.blockPos().getX());
            int z = (int) ((mouseY - topPos - 117) * ((double) menu.machine().radarRangeValue() * 2.0D + 1.0D) / 192.0D + menu.blockPos().getZ());
            graphics.renderTooltip(font, Component.literal(x + " / " + z), mouseX, mouseY);
        }
    }

    private static List<Component> radarControlTooltip(int action, String key) {
        return switch (action) {
            case 3 -> List.of(
                    Component.translatable(key),
                    Component.translatable("gui.atomiclegacy.radar.smart_mode.description")
            );
            case 4 -> List.of(
                    Component.translatable(key),
                    Component.translatable("gui.atomiclegacy.radar.redstone_mode.range"),
                    Component.translatable("gui.atomiclegacy.radar.redstone_mode.tier")
            );
            default -> List.of(Component.translatable(key));
        };
    }

    private int actionAt(double mouseX, double mouseY) {
        if (inside(mouseX, mouseY, -10, 88, 8, 8)) return 0;
        if (inside(mouseX, mouseY, -10, 98, 8, 8)) return 1;
        if (inside(mouseX, mouseY, -10, 108, 8, 8)) return 2;
        if (inside(mouseX, mouseY, -10, 118, 8, 8)) return 3;
        if (inside(mouseX, mouseY, -10, 128, 8, 8)) return 4;
        if (inside(mouseX, mouseY, -10, 138, 8, 8)) return 5;
        if (inside(mouseX, mouseY, -10, 158, 8, 8)) return 7;
        if (inside(mouseX, mouseY, -10, 178, 8, 8)) return 6;
        return -1;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return K3GuiText.hover(mouseX, mouseY, leftPos + x, topPos + y, width, height); // [parity g2] checkClick
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_8
                && inside(lastMouseX, lastMouseY, 8, 17, 200, 200)) {
            int relaySlot = keyCode - GLFW.GLFW_KEY_1;
            RadarTarget target = targetAt(lastMouseX, lastMouseY);
            if (target != null) {
                PacketDistributor.sendToServer(new RadarCommandPayload(menu.blockPos(), relaySlot,
                        target.entityId(), 0, 0));
            } else {
                LegacyMachineBlockEntity machine = menu.machine();
                if (machine == null) return true;
                int range = machine.radarRangeValue();
                int x = (int) ((lastMouseX - leftPos - 108) * ((double) range * 2.0D + 1.0D) / 192.0D + menu.blockPos().getX());
                int z = (int) ((lastMouseY - topPos - 117) * ((double) range * 2.0D + 1.0D) / 192.0D + menu.blockPos().getZ());
                PacketDistributor.sendToServer(new RadarCommandPayload(menu.blockPos(), relaySlot, -1, x, z));
            }
            return true; // [parity g2] keyTyped plays no sound
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private RadarTarget targetAt(int mouseX, int mouseY) {
        LegacyMachineBlockEntity machine = menu.machine();
        if (machine == null) return null;
        int range = machine.radarRangeValue();
        for (RadarTarget target : menu.targets()) {
            int x = leftPos + (int) ((target.x() - menu.blockPos().getX()) / ((double) range * 2.0D + 1.0D) * (200D - 8D)) + 108;
            int z = topPos + (int) ((target.z() - menu.blockPos().getZ()) / ((double) range * 2.0D + 1.0D) * (200D - 8D)) + 117;
            if (mouseX + 5 > x && mouseX - 4 <= x && mouseY + 5 > z && mouseY - 4 <= z) {
                return target;
            }
        }
        return null;
    }
}
