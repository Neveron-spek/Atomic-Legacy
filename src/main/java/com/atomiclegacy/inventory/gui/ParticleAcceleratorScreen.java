package com.atomiclegacy.inventory.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.ParticleAcceleratorBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.ParticleAcceleratorMenu;
import com.atomiclegacy.items.PACoilItem;
import com.atomiclegacy.network.ParticleAcceleratorControlPayload;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.tileentity.ParticleAcceleratorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * [parity e2] GUIPASource / GUIPARFC / GUIPAQuadrupole / GUIPADipole / GUIPADetector of the 1.7.10 reference
 * (176x204, textures gui/particleaccelerator/gui_*.png), one screen switching on the machine kind.
 */
public class ParticleAcceleratorScreen extends AbstractContainerScreen<ParticleAcceleratorMenu> {
    private EditBox threshold;
    private boolean thresholdUpdating;
    private boolean thresholdEdited;

    public ParticleAcceleratorScreen(ParticleAcceleratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    private ParticleAcceleratorBlock.Kind kind() {
        return this.menu.kind();
    }

    /** x of coolantTanks[0]; coolantTanks[1] is 18 px to the right */
    private int tankX() {
        return switch (kind()) {
            case RFC -> 89;
            case QUADRUPOLE -> 116;
            default -> 134;
        };
    }

    /** x of the power gauge */
    private int powerX() {
        return switch (kind()) {
            case RFC -> 53;
            case QUADRUPOLE -> 26;
            default -> 8;
        };
    }

    @Override
    protected void init() {
        super.init();
        this.threshold = null;
        if (kind() == ParticleAcceleratorBlock.Kind.DIPOLE) {
            this.threshold = new EditBox(this.font, this.leftPos + 47, this.topPos + 77, 66, 8, Component.empty());
            this.threshold.setTextColor(0x00ff00);
            this.threshold.setTextColorUneditable(0x00ff00);
            this.threshold.setBordered(false);
            this.threshold.setMaxLength(9);
            this.thresholdUpdating = true;
            this.threshold.setValue("" + this.menu.threshold());
            this.thresholdUpdating = false;
            this.threshold.setResponder(this::thresholdTyped);
            addRenderableWidget(this.threshold);
        }
    }

    /** GUIPADipole#keyTyped: strip one leading zero, empty -> "0", digits -> NBTControlPacket "threshold" */
    private void thresholdTyped(String ignored) {
        if (this.thresholdUpdating) return;
        this.thresholdUpdating = true;
        this.thresholdEdited = true;
        String text = this.threshold.getValue();
        if (text.startsWith("0")) this.threshold.setValue(text.substring(1));
        if (this.threshold.getValue().isEmpty()) this.threshold.setValue("0");
        text = this.threshold.getValue();
        if (!text.isEmpty() && text.chars().allMatch(Character::isDigit)) {
            int num;
            try {
                num = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                num = 0; // NumberUtils.toInt default
            }
            send(ParticleAcceleratorControlPayload.SET_THRESHOLD, num);
        }
        this.thresholdUpdating = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // the reference reads dipole.threshold in initGui; here the value arrives with the first menu data sync
        if (this.threshold != null && !this.thresholdEdited && !this.threshold.isFocused()) {
            String value = "" + this.menu.threshold();
            if (!value.equals(this.threshold.getValue())) {
                this.thresholdUpdating = true;
                this.threshold.setValue(value);
                this.thresholdUpdating = false;
            }
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String value = this.threshold != null ? this.threshold.getValue() : null;
        super.resize(minecraft, width, height);
        if (value != null && this.threshold != null) {
            this.thresholdUpdating = true;
            this.threshold.setValue(value);
            this.thresholdUpdating = false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.threshold != null && this.threshold.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            this.threshold.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.threshold != null && this.threshold.isFocused() && !this.threshold.isMouseOver(mouseX, mouseY)) {
            this.threshold.setFocused(false);
            setFocused(null);
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (kind() == ParticleAcceleratorBlock.Kind.SOURCE) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 105, this.topPos + 28, 10, 10)) {
                K3GuiText.click();
                send(ParticleAcceleratorControlPayload.CANCEL, 0);
                return true;
            }
        } else if (kind() == ParticleAcceleratorBlock.Kind.DIPOLE) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 62, this.topPos + 29, 12, 12)) {
                K3GuiText.click();
                send(ParticleAcceleratorControlPayload.CYCLE_LOWER, 0);
                return true;
            }
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 62, this.topPos + 43, 12, 12)) {
                K3GuiText.click();
                send(ParticleAcceleratorControlPayload.CYCLE_UPPER, 0);
                return true;
            }
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 62, this.topPos + 57, 12, 12)) {
                K3GuiText.click();
                send(ParticleAcceleratorControlPayload.CYCLE_REDSTONE, 0);
                return true;
            }
        }
        return handled;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int tankX = this.leftPos + tankX();
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, tankX, this.topPos + 36, 16, 52,
                coldFluid(), this.menu.coldAmount(), this.menu.coolantCapacity());
        E2TankGui.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, tankX + 18, this.topPos + 36, 16, 52,
                hotFluid(), this.menu.hotAmount(), this.menu.coolantCapacity());
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + powerX(), this.topPos + 18, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(K3GuiText.electricity(this.menu.power(), this.menu.maxPower())), mouseX, mouseY);
        }

        if (kind() == ParticleAcceleratorBlock.Kind.SOURCE) {
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 105, this.topPos + 16, 10, 10)) {
                List<Component> info = new ArrayList<>();
                info.add(Component.literal("§9Last momentum: §r" + String.format(Locale.US, "%,d", this.menu.lastSpeed())));
                for (Component line : LegacyInfoText.lines("pa." + stateName() + ".desc")) {
                    info.add(Component.literal("§e" + line.getString()));
                }
                guiGraphics.renderComponentTooltip(this.font, info, mouseX, mouseY);
            }
            if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 105, this.topPos + 28, 10, 10)) {
                guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("§cCancel operation")), mouseX, mouseY);
            }
        } else if (kind() == ParticleAcceleratorBlock.Kind.DIPOLE) {
            orientationInfo(guiGraphics, mouseX, mouseY, 29, this.menu.dirLower());
            orientationInfo(guiGraphics, mouseX, mouseY, 43, this.menu.dirUpper());
            orientationInfo(guiGraphics, mouseX, mouseY, 57, this.menu.dirRedstone());
        }
    }

    private void orientationInfo(GuiGraphics guiGraphics, int mouseX, int mouseY, int y, int dir) {
        if (K3GuiText.hover(mouseX, mouseY, this.leftPos + 62, this.topPos + y, 12, 12)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("§9Player orientation"),
                    Component.literal("§cOutput orientation:"),
                    Component.literal(directionName(dir))), mouseX, mouseY);
        }
    }

    /** TileEntityPADipole.ditToForgeDir(dir).name() */
    private static String directionName(int dir) {
        return switch (dir) {
            case 1 -> "EAST";
            case 2 -> "SOUTH";
            case 3 -> "WEST";
            default -> "NORTH";
        };
    }

    private String stateName() {
        return this.menu.state().name().toLowerCase(Locale.US);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ParticleAcceleratorBlock.Kind kind = kind();
        if (kind != ParticleAcceleratorBlock.Kind.RFC) {
            String name = this.title.getString();
            int offset = kind == ParticleAcceleratorBlock.Kind.DETECTOR ? 8 : 9;
            int y = kind == ParticleAcceleratorBlock.Kind.SOURCE ? 4 : kind == ParticleAcceleratorBlock.Kind.DETECTOR ? 5 : 6;
            guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2 - offset, y, 0xffffff, false);
        }
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);

        int tankX = tankX();
        guiGraphics.drawString(this.font, "§b/123K", tankX + 2, 22, 4210752, false);
        int heat = (int) Math.ceil(this.menu.temperature());
        String label = (heat > 123 ? "§c" : "§b") + heat + "K";
        guiGraphics.drawString(this.font, label, tankX + 32 - this.font.width(label), 12, 4210752, false);

        if (kind == ParticleAcceleratorBlock.Kind.SOURCE) {
            ParticleAcceleratorBlockEntity.PAState paState = this.menu.state();
            String state = Component.translatable("pa." + stateName()).getString();
            guiGraphics.drawString(this.font, state, 79 - this.font.width(state) / 2, 76, paState.color, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ParticleAcceleratorBlock.Kind kind = kind();
        ResourceLocation texture = texture(kind);
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.blit(texture, left, top, 0, 0, this.imageWidth, this.imageHeight);

        long power = this.menu.power();
        int j = (int) (power * 52 / this.menu.maxPower());
        if (j > 0) {
            guiGraphics.blit(texture, left + powerX(), top + 70 - j, 184, 52 - j, 16, j);
        }

        int heat = (int) Math.ceil(this.menu.temperature());
        boolean cold = heat <= 123;
        boolean powered = power >= this.menu.usage();

        switch (kind) {
            case SOURCE -> {
                if (cold) guiGraphics.blit(texture, left + 44, top + 16, 176, 8, 8, 8);
                if (powered) guiGraphics.blit(texture, left + 44, top + 41, 176, 8, 8, 8);
                // the reference passes the 0..255 channel values straight to glColor4f, i.e. every non-zero channel is full
                int color = this.menu.state().color;
                guiGraphics.setColor((color & 0xff0000) != 0 ? 1.0F : 0.0F, (color & 0x00ff00) != 0 ? 1.0F : 0.0F, (color & 0x0000ff) != 0 ? 1.0F : 0.0F, 1.0F);
                guiGraphics.blit(texture, left + 45, top + 73, 176, 52, 68, 14);
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            case DIPOLE -> {
                if (cold) guiGraphics.blit(texture, left + 93, top + 54, 176, 8, 8, 8);
                int coil = coilType();
                if (coil >= 0) {
                    guiGraphics.blit(texture, left + 103, top + 54, 176, 8, 8, 8);
                    guiGraphics.blit(texture, left + 83, top + 20, 200 + (coil % 2) * 28, 96 + (coil / 2) * 28, 28, 28);
                }
                if (powered) guiGraphics.blit(texture, left + 83, top + 54, 176, 8, 8, 8);

                float yaw = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getYRot() : 0.0F;
                addLine(guiGraphics, 68, 35, 0x8080ff, 180);
                addLine(guiGraphics, 68, 35, 0xff0000, yaw - this.menu.dirLower() * 90);
                addLine(guiGraphics, 68, 49, 0x8080ff, 180);
                addLine(guiGraphics, 68, 49, 0xff0000, yaw - this.menu.dirUpper() * 90);
                addLine(guiGraphics, 68, 63, 0x8080ff, 180);
                addLine(guiGraphics, 68, 63, 0xff0000, yaw - this.menu.dirRedstone() * 90);
            }
            case QUADRUPOLE -> {
                if (cold) guiGraphics.blit(texture, left + 75, top + 64, 176, 8, 8, 8);
                int coil = coilType();
                if (coil >= 0) {
                    guiGraphics.blit(texture, left + 85, top + 64, 176, 8, 8, 8);
                    guiGraphics.blit(texture, left + 65, top + 30, 200 + (coil % 2) * 28, (coil / 2) * 28, 28, 28);
                }
                if (powered) guiGraphics.blit(texture, left + 65, top + 64, 176, 8, 8, 8);
            }
            case DETECTOR -> {
                if (cold) guiGraphics.blit(texture, left + 43, top + 18, 176, 8, 8, 8);
                if (powered) guiGraphics.blit(texture, left + 43, top + 43, 176, 8, 8, 8);
            }
            default -> {
            }
        }

        int tankX = left + tankX();
        E2TankGui.renderTank(guiGraphics, tankX, top + 88, 16, 52, coldFluid(), this.menu.coldAmount(), this.menu.coolantCapacity());
        E2TankGui.renderTank(guiGraphics, tankX + 18, top + 88, 16, 52, hotFluid(), this.menu.hotAmount(), this.menu.coolantCapacity());
    }

    /**
     * GUIPADipole#addLine: GL line (glLineWidth 3) from (x, y) along Vec3(0, 6, 0).rotateAroundZDeg(yaw),
     * i.e. (6 * sin(yaw), 6 * cos(yaw)) in gui pixels; drawn here as a 3 screen pixel wide quad.
     */
    private void addLine(GuiGraphics guiGraphics, int x, int y, int color, float yaw) {
        double rad = yaw / 180.0D * Math.PI;
        float dx = 6.0F * (float) Math.sin(rad);
        float dy = 6.0F * (float) Math.cos(rad);
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        if (scale <= 0.0F) scale = 1.0F;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(this.leftPos + x, this.topPos + y, 0.0F);
        pose.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        pose.scale(1.0F / scale, 1.0F / scale, 1.0F);
        guiGraphics.fill(0, -1, Math.round(6.0F * scale), 2, 0xFF000000 | color);
        pose.popPose();
    }

    /** EnumCoilType ordinal (GOLD, NIOBIUM, BSCCO, CHLOROPHYTE) of the coil in slot 1, -1 without a coil */
    private int coilType() {
        ItemStack stack = this.menu.coilStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof PACoilItem)) return -1;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.endsWith("niobium")) return 1;
        if (path.endsWith("bscco")) return 2;
        if (path.endsWith("chlorophyte")) return 3;
        return 0;
    }

    private static HbmFluidDefinition coldFluid() {
        return HbmFluids.byName("perfluoromethyl_cold").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition hotFluid() {
        return HbmFluids.byName("perfluoromethyl").orElse(HbmFluids.none());
    }

    private static ResourceLocation texture(ParticleAcceleratorBlock.Kind kind) {
        String name = switch (kind) {
            case RFC -> "gui_rfc";
            case QUADRUPOLE -> "gui_quadrupole";
            case DIPOLE -> "gui_dipole";
            case DETECTOR -> "gui_detector";
            default -> "gui_source";
        };
        return AtomicLegacy.id("textures/gui/particleaccelerator/" + name + ".png");
    }

    private void send(int action, int value) {
        PacketDistributor.sendToServer(new ParticleAcceleratorControlPayload(this.menu.blockPos(), action, value));
    }
}
