package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.LauncherBlockEntity;
import com.atomiclegacy.render.LauncherMissileRenderer;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.container.LauncherMenu;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Random;

/** Exact rectangles and controls from the four 1.7.10 launcher screens. */
public final class LauncherScreen extends AbstractContainerScreen<LauncherMenu> {
    private final ResourceLocation texture;
    public LauncherScreen(LauncherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = menu.custom() ? 222 : 236;
        inventoryLabelY = imageHeight - 94;
        titleLabelY = menu.custom() ? 6 : 4;
        texture = AtomicLegacy.id("textures/gui/weapon/" + switch (menu.launcher().kind()) {
            case COMPACT -> "gui_launch_table_small.png"; case TABLE -> "gui_launch_table.png";
            case PAD_RUSTED -> "gui_launch_pad_rusted.png";
            // [parity e2] TileEntityLaunchPadBase.provideGUI: both pads open GUILaunchPadLarge (gui_launch_pad_large.png)
            case PAD_SMALL -> "gui_launch_pad_large.png";
            case PAD_LARGE -> "gui_launch_pad_large.png";
            default -> throw new IllegalArgumentException("Invalid launcher GUI");
        });
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        blit(g, 0, 0, 0, 0, imageWidth, imageHeight);
        var be = menu.launcher();
        if (menu.custom()) {
            blit(g, 134, 113, 176, 96, (int)(be.power() * 34 / 100_000), 6);
            int solid = be.solidFuel() * 52 / be.solidCapacity();
            blit(g, 152, 88 - solid, 176, 96 - solid, 16, solid);
            if (be.validMissile(be.getItem(0))) blit(g, 25, 35, 176, 26, 18, 18);
            if (be.hasDesignator()) blit(g, 25, 71, 176, 26, 18, 18);
            customIndicator(g, 121, be.liquidState());
            customIndicator(g, 139, be.oxidizerState());
            customIndicator(g, 157, be.solidState());
            if (be.kind() == LauncherBlockEntity.Kind.TABLE) {
                int index = be.tableSize().ordinal() - 2;
                blit(g, 7 + index * 18, 98, 176 + index * 18, 8, 18, 18);
            }
            fluid(g, be.fuelTank(), 116, 70, 34);
            fluid(g, be.oxidizerTank(), 134, 70, 34);
            // [parity e2] drawInfoPanel(guiLeft - 16, guiTop + 36, 16, 16, 2) and (.., guiTop + 52, 16, 16, 11)
            g.blit(K3GuiText.UTILITY, leftPos - 16, topPos + 36, 8, 0, 16, 16, 256, 256);
            g.blit(K3GuiText.UTILITY, leftPos - 16, topPos + 52, 24, 32, 16, 16, 256, 256);
            if (be.validMissile(be.getItem(0))) LauncherMissileRenderer.renderGui(g, leftPos, topPos, be.getItem(0));
        } else if (be.kind() == LauncherBlockEntity.Kind.PAD_RUSTED) {
            boolean codes = itemIs(be.getItem(1), "launch_code"), key = itemIs(be.getItem(2), "launch_key");
            if (codes) blit(g, 121, 32, 192, 0, 6, 8);
            if (key) blit(g, 139, 32, 192, 0, 6, 8);
            if (codes && key && be.missileLoaded()) {
                int code = new Random(be.getBlockPos().getX() * 131_071 + be.getBlockPos().getZ()).nextInt(100_000_000);
                for (int i = 0, magnitude = 1; i < 8; i++, magnitude *= 10)
                    blit(g, 109 + i * 6, 85, 192 + 6 * ((code % (magnitude * 10)) / magnitude), 8, 6, 8);
            }
            if (be.missileLoaded()) LauncherMissileRenderer.renderGui(g, leftPos, topPos,
                    new ItemStack(BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id("missile_doomsday_rusted")).orElseThrow()));
        } else {
            if (be.validMissile(be.getItem(0))) blit(g, 112, 23, be.power() >= 75_000 ? 192 : 198, 0, 6, 8);
            for (int i = 0; i < 2; i++) {
                int value = be.gaugeState(i);
                if (value != 0) blit(g, 130 + 18 * i, 23, value == 1 ? 192 : 198, 0, 6, 8);
            }
            int power = (int)(be.power() * 52 / 100_000);
            blit(g, 107, 88 - power, 176, 52 - power, 16, power);
            fluid(g, be.fuelTank(), 125, 88, 52);
            fluid(g, be.oxidizerTank(), 143, 88, 52);
            if (!be.getItem(0).isEmpty()) LauncherMissileRenderer.renderGui(g, leftPos, topPos, be.getItem(0));
            int state = be.state();
            Component status = Component.translatable(state == 0
                    ? "gui.atomiclegacy.launcher.status.not_ready"
                    : state == 1
                    ? "gui.atomiclegacy.launcher.status.loading"
                    : "gui.atomiclegacy.launcher.status.ready");
            float scale = state == 0 ? .5F : state == 1 ? .6F : .8F;
            g.pose().pushPose();
            g.pose().translate(leftPos + 34, topPos + 107, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, status, -font.width(status) / 2, -font.lineHeight / 2, state == 0 ? 0xff0000 : state == 1 ? 0xff8000 : 0x00ff00, false);
            g.pose().popPose();
        }
    }
    private void blit(GuiGraphics g, int x, int y, int u, int v, int w, int h) {
        if (w > 0 && h > 0) g.blit(texture, leftPos + x, topPos + y, u, v, w, h);
    }
    private void customIndicator(GuiGraphics g, int x, int state) {
        if (state != -1) blit(g, x, 23, state == 1 ? 176 : 182, 0, 6, 8);
    }
    private void fluid(GuiGraphics g, HbmFluidTank tank, int x, int bottom, int height) {
        if (tank.type().isNone() || tank.amount() <= 0) return;
        int filled = tank.amount() * height / tank.capacity(), color = tank.type().color();
        ResourceLocation image = AtomicLegacy.id("textures/gui/fluids/" + tank.type().name() + ".png");
        RenderSystem.setShaderColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1F);
        for (int y = 0; y < filled; y += 16) {
            int h = Math.min(16, filled - y);
            g.blit(image, leftPos + x, topPos + bottom - y - h, 0, 16 - h, 16, h, 16, 16);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        var be = menu.launcher();
        if (be.kind() == LauncherBlockEntity.Kind.PAD_RUSTED && isHovering(26, 36, 16, 16, mouseX, mouseY))
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.atomiclegacy.launcher.release_missile").withStyle(net.minecraft.ChatFormatting.YELLOW), // [parity e2] EnumChatFormatting.YELLOW
                    Component.translatable("gui.atomiclegacy.launcher.locked_line1"),
                    Component.translatable("gui.atomiclegacy.launcher.locked_line2"),
                    Component.translatable("gui.atomiclegacy.launcher.damaged_line1"),
                    Component.translatable("gui.atomiclegacy.launcher.damaged_line2")), mouseX, mouseY);
        else if (be.kind() == LauncherBlockEntity.Kind.PAD_RUSTED) { /* [parity e2] GUILaunchPadRusted has no other info areas */ }
        else if (menu.custom() && isHovering(152, 36, 16, 52, mouseX, mouseY))
            g.renderTooltip(font, Component.translatable("gui.atomiclegacy.launcher.solid_fuel", be.solidFuel()), mouseX, mouseY);
        else if (isHovering(menu.custom() ? 134 : 107, menu.custom() ? 113 : 36, menu.custom() ? 34 : 16, menu.custom() ? 6 : 52, mouseX, mouseY))
            g.renderTooltip(font, K3GuiText.electricity(be.power(), 100_000), mouseX, mouseY); // [parity e2] drawElectricityInfo
        else for (int i = 0; i < 2; i++) {
            var tank = i == 0 ? be.fuelTank() : be.oxidizerTank();
            if (isHovering((menu.custom() ? 116 : 125) + 18 * i, menu.custom() ? 36 : 36, 16, menu.custom() ? 34 : 52, mouseX, mouseY))
                g.renderComponentTooltip(font, HbmFluidTooltip.forTank(tank.type(), tank.amount(), tank.capacity()), mouseX, mouseY); // [parity e2] FluidTank.renderTankInfo
        }
        parityInfo(g, mouseX, mouseY, be);
    }
    /** [parity e2] remaining drawCustomInfo / drawCustomInfoStat / drawStackText calls of the launcher GUIs */
    private void parityInfo(GuiGraphics g, int mouseX, int mouseY, LauncherBlockEntity be) {
        if (menu.custom()) {
            if (be.kind() == LauncherBlockEntity.Kind.TABLE) {
                String[] sizes = {"Size 10 & 10/15", "Size 15 & 15/20", "Size 20"};
                for (int i = 0; i < 3; i++)
                    if (within(mouseX, mouseY, 7 + 18 * i, 98, 18)) g.renderTooltip(font, Component.literal(sizes[i]), mouseX, mouseY);
            }
            if (K3GuiText.hover(mouseX, mouseY, leftPos - 16, topPos + 36, 16, 16))
                g.renderComponentTooltip(font, be.kind() == LauncherBlockEntity.Kind.TABLE
                        ? List.of(Component.literal("Accepts custom missiles"), Component.literal("of all sizes, as long as the"), Component.literal("correct size setting is selected."))
                        : List.of(Component.literal("Only accepts custom missiles"), Component.literal("of size 10 and 10/15.")), leftPos - 8, topPos + 36 + 16);
            if (K3GuiText.hover(mouseX, mouseY, leftPos - 16, topPos + 36 + 16, 16, 16))
                g.renderComponentTooltip(font, List.of(Component.literal("Detonator can only trigger center block.")), leftPos - 8, topPos + 36 + 16);
        } else if (be.kind() != LauncherBlockEntity.Kind.PAD_RUSTED && menu.getCarried().isEmpty() && !menu.getSlot(1).hasItem()
                && isHovering(menu.getSlot(1).x, menu.getSlot(1).y, 16, 16, mouseX, mouseY)) {
            List<ItemStack> list = new java.util.ArrayList<>();
            for (String id : new String[] {"designator", "designator_range", "designator_manual"})
                BuiltInRegistries.ITEM.getOptional(AtomicLegacy.id(id)).map(ItemStack::new).ifPresent(list::add);
            LegacyTurretScreenCompat.renderStackHint(g, font, mouseX, mouseY, list);
        }
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        int action = -1;
        if (menu.launcher().kind() == LauncherBlockEntity.Kind.PAD_RUSTED && within(x, y, 26, 36, 16)) action = 0;
        if (menu.launcher().kind() == LauncherBlockEntity.Kind.TABLE) for (int i = 0; i < 3; i++)
            if (within(x, y, 7 + 18 * i, 98, 18)) action = i + 2;
        if (action >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    private boolean within(double x, double y, int bx, int by, int size) {
        return x >= leftPos + bx && x < leftPos + bx + size && y > topPos + by && y <= topPos + by + size;
    }
    private static boolean itemIs(ItemStack stack, String id) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(AtomicLegacy.id(id)); }
}
