package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.FusionMachineBlock;
import com.atomiclegacy.tileentity.FusionMachineBlockEntity;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.container.FusionMachineMenu;
import com.atomiclegacy.network.FusionMachineControlPayload;
import com.atomiclegacy.inventory.recipes.PlasmaForgeRecipe;
import com.atomiclegacy.inventory.recipes.FusionRecipe;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FusionMachineScreen extends AbstractContainerScreen<FusionMachineMenu> {
    private static final ResourceLocation TORUS = AtomicLegacy.id("textures/gui/reactors/gui_fusion_torus.png");
    private static final ResourceLocation KLYSTRON = AtomicLegacy.id("textures/gui/reactors/gui_fusion_klystron.png");
    private static final ResourceLocation BREEDER = AtomicLegacy.id("textures/gui/reactors/gui_fusion_breeder.png");
    private static final ResourceLocation PLASMA_FORGE = AtomicLegacy.id("textures/gui/reactors/gui_fusion_plasmaforge.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final float GHOST_ALPHA = 0.20F;
    private static final int LABEL_COLOR = 0x404040;

    @Nullable
    private EditBox klystronTarget;

    public FusionMachineScreen(FusionMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        switch (menu.kind()) {
            case TORUS -> {
                this.imageWidth = 230;
                this.imageHeight = 244;
                this.inventoryLabelX = 35;
                this.inventoryLabelY = 151;
            }
            case KLYSTRON -> {
                this.imageWidth = 194;
                this.imageHeight = 200;
                this.inventoryLabelX = 35;
                this.inventoryLabelY = 107;
            }
            case PLASMA_FORGE -> {
                this.imageWidth = 176;
                this.imageHeight = 244;
                this.inventoryLabelX = 8;
                this.inventoryLabelY = 150;
            }
            default -> {
                this.imageWidth = 176;
                this.imageHeight = 200;
                this.inventoryLabelX = 35;
                this.inventoryLabelY = 107;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = switch (this.menu.kind()) {
            case TORUS -> 106 - this.font.width(this.title) / 2;
            case KLYSTRON -> 115 - this.font.width(this.title) / 2;
            case PLASMA_FORGE -> 70 - this.font.width(this.title) / 2;
            default -> this.imageWidth / 2 - this.font.width(this.title) / 2;
        };
        if (this.menu.kind() == FusionMachineBlock.Kind.KLYSTRON) {
            this.klystronTarget = new EditBox(this.font, this.leftPos + 84, this.topPos + 22, 102, 12, Component.empty());
            this.klystronTarget.setBordered(false);
            this.klystronTarget.setTextColor(0x00FF00);
            this.klystronTarget.setTextColorUneditable(0x00FF00);
            this.klystronTarget.setMaxLength(12);
            this.klystronTarget.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
            this.klystronTarget.setValue(Long.toString(this.menu.outputTarget()));
            this.klystronTarget.setResponder(this::sendKlystronTarget);
            this.addWidget(this.klystronTarget);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.menu.kind() == FusionMachineBlock.Kind.TORUS && in(mouseX, mouseY, 43, 80, 18, 18)) {
            playClick();
            if (this.minecraft != null) {
                this.minecraft.setScreen(new FusionRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        if (this.menu.kind() == FusionMachineBlock.Kind.PLASMA_FORGE && in(mouseX, mouseY, 7, 80, 18, 18)) {
            playClick();
            if (this.minecraft != null) {
                this.minecraft.setScreen(new PlasmaForgeRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        if (this.klystronTarget != null && this.klystronTarget.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.klystronTarget);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.klystronTarget != null && this.klystronTarget.isFocused() && this.klystronTarget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.klystronTarget != null && this.klystronTarget.isFocused() && this.klystronTarget.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.klystronTarget != null) {
            this.klystronTarget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderHoverTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        switch (this.menu.kind()) {
            case TORUS -> renderTorus(guiGraphics);
            case KLYSTRON -> renderKlystron(guiGraphics);
            case BREEDER -> renderBreeder(guiGraphics);
            case PLASMA_FORGE -> renderPlasmaForge(guiGraphics);
            default -> {
            }
        }
    }

    // [parity M3: fusion] GUIFusionTorus.drawGuiContainerBackgroundLayer
    private void renderTorus(GuiGraphics graphics) {
        graphics.blit(TORUS, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        int power = (int) (this.menu.power() * 62 / Math.max(1L, this.menu.maxPower()));
        graphics.blit(TORUS, this.leftPos + 8, this.topPos + 80 - power, 230, 62 - power, 16, power, TEX_W, TEX_H);
        double progress = this.menu.progress() / 10_000.0D;
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            graphics.blit(TORUS, this.leftPos + 98, this.topPos + 81, 0, 244, j, 6, TEX_W, TEX_H);
        }
        double bonus = this.menu.bonusProgress();
        if (bonus > 0) {
            int j = (int) Math.min(Math.ceil(70 * bonus), 70);
            graphics.blit(TORUS, this.leftPos + 98, this.topPos + 91, 0, 250, j, 6, TEX_W, TEX_H);
        }
        boolean hasRecipe = this.menu.hasSelectedFusionRecipe();
        if (hasRecipe && this.menu.power() >= this.menu.selectedFusionPower()) {
            graphics.blit(TORUS, this.leftPos + 160, this.topPos + 115, 246, 14, 8, 8, TEX_W, TEX_H);
        }
        if (this.menu.temperature() <= 123) {
            graphics.blit(TORUS, this.leftPos + 170, this.topPos + 115, 246, 14, 8, 8, TEX_W, TEX_H);
        }
        if (this.menu.didProcess()) {
            graphics.blit(TORUS, this.leftPos + 180, this.topPos + 115, 246, 14, 8, 8, TEX_W, TEX_H);
        }
        if (this.menu.didProcess()) {
            graphics.blit(TORUS, this.leftPos + 87, this.topPos + 76, 249, 0, 3, 6, TEX_W, TEX_H);
            graphics.blit(TORUS, this.leftPos + 92, this.topPos + 76, 249, 0, 3, 6, TEX_W, TEX_H);
        } else if (hasRecipe) {
            graphics.blit(TORUS, this.leftPos + 87, this.topPos + 76, 246, 0, 3, 6, TEX_W, TEX_H);
            graphics.blit(TORUS, this.leftPos + 92, this.topPos + 76, 246, 0, 3, 6, TEX_W, TEX_H);
        }
        double inputGauge = !hasRecipe ? 0 : Math.min((double) this.menu.klystronEnergy() / (double) this.menu.selectedFusionIgnitionTemp(), 1.5) / 1.5D;
        double outputGauge = !hasRecipe ? 0 : Math.min((double) this.menu.plasmaEnergy() / (double) this.menu.selectedFusionOutputTemp(), 1);
        K3LegacyGauge.draw(graphics, this.leftPos + 52, this.topPos + 124, inputGauge, 5, 2, 1, 0xA00000);
        K3LegacyGauge.draw(graphics, this.leftPos + 88, this.topPos + 124, outputGauge, 5, 2, 1, 0xA00000);
        K3LegacyGauge.draw(graphics, this.leftPos + 124, this.topPos + 124, this.menu.fuelConsumption() / 10_000.0D, 5, 2, 1, 0xA00000);
        graphics.renderItem(this.menu.recipeIcon(), this.leftPos + 44, this.topPos + 81);
        drawFluid(graphics, 44, 70, 16, 52, 0);
        drawFluid(graphics, 62, 70, 16, 52, 1);
        drawFluid(graphics, 80, 70, 16, 52, 2);
        drawFluid(graphics, 152, 70, 16, 52, 3);
        drawFluid(graphics, 188, 98, 16, 52, 4);
        drawFluid(graphics, 206, 98, 16, 52, 5);
    }

    // [parity M3: fusion] GUIFusionKlystron.drawGuiContainerBackgroundLayer
    private void renderKlystron(GuiGraphics graphics) {
        graphics.blit(KLYSTRON, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        int power = (int) (this.menu.power() * 52 / Math.max(1L, this.menu.maxPower()));
        graphics.blit(KLYSTRON, this.leftPos + 8, this.topPos + 70 - power, 194, 52 - power, 16, power, TEX_W, TEX_H);
        long output = this.menu.output();
        long target = this.menu.outputTarget();
        double outputGauge = target <= 0 ? 0 : ((double) output / (double) target);
        double airGauge = (double) this.menu.tankAmount(0) / (double) this.menu.tankCapacity(0);
        double powerGauge = speedScaled(this.menu.maxPower(), this.menu.power());
        if (powerGauge >= 0.5 && output > 0) {
            graphics.blit(KLYSTRON, this.leftPos + 160, this.topPos + 71, 210, 8, 8, 8, TEX_W, TEX_H);
        } else if (powerGauge > 0) {
            graphics.blit(KLYSTRON, this.leftPos + 160, this.topPos + 71, 210, 0, 8, 8, TEX_W, TEX_H);
        }
        if (airGauge >= 0.5 && output > 0) {
            graphics.blit(KLYSTRON, this.leftPos + 170, this.topPos + 71, 210, 8, 8, 8, TEX_W, TEX_H);
        } else if (airGauge > 0) {
            graphics.blit(KLYSTRON, this.leftPos + 170, this.topPos + 71, 210, 0, 8, 8, TEX_W, TEX_H);
        }
        if (output >= target && output > 0) {
            graphics.blit(KLYSTRON, this.leftPos + 180, this.topPos + 71, 210, 8, 8, 8, TEX_W, TEX_H);
        } else if (output > 0) {
            graphics.blit(KLYSTRON, this.leftPos + 180, this.topPos + 71, 210, 0, 8, 8, TEX_W, TEX_H);
        }
        K3LegacyGauge.draw(graphics, this.leftPos + 52, this.topPos + 80, outputGauge, 5, 2, 1, 0xA00000);
        K3LegacyGauge.draw(graphics, this.leftPos + 88, this.topPos + 80, airGauge, 5, 2, 1, 0xA00000);
        K3LegacyGauge.draw(graphics, this.leftPos + 124, this.topPos + 80, powerGauge, 5, 2, 1, 0xA00000);
    }

    // [parity M3: fusion] GUIFusionBreeder.drawGuiContainerBackgroundLayer
    private void renderBreeder(GuiGraphics graphics) {
        graphics.blit(BREEDER, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        double progress = this.menu.progress() / 10_000.0D;
        int p = (int) Math.ceil(progress * 42 / FusionMachineBlockEntity.BREEDER_CAPACITY);
        if (p > 0) {
            graphics.blit(BREEDER, this.leftPos + 67, this.topPos + 48, 176, 0, p, 10, TEX_W, TEX_H);
        }
        double gauge = 1D - Math.pow(Math.E, -this.menu.neutronEnergy() * 10 / FusionMachineBlockEntity.BREEDER_CAPACITY);
        K3LegacyGauge.draw(graphics, this.leftPos + 88, this.topPos + 32, gauge, 5, 2, 1, 0xA00000);
        drawFluid(graphics, 26, 70, 16, 52, 0);
        drawFluid(graphics, 134, 70, 16, 52, 1);
    }

    // [parity M3: fusion] GUIMachinePlasmaForge.drawGuiContainerBackgroundLayer
    private void renderPlasmaForge(GuiGraphics graphics) {
        graphics.blit(PLASMA_FORGE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        int power = (int) (this.menu.power() * 62 / Math.max(1L, this.menu.maxPower()));
        graphics.blit(PLASMA_FORGE, this.leftPos + 152, this.topPos + 80 - power, 176, 62 - power, 16, power, TEX_W, TEX_H);
        double progress = this.menu.progress() / 10_000.0D;
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            graphics.blit(PLASMA_FORGE, this.leftPos + 62, this.topPos + 81, 176, 62, j, 16, TEX_W, TEX_H);
        }
        boolean hasRecipe = this.menu.selectedPlasmaForgeRecipe().isPresent();
        if (this.menu.didProcess()) {
            graphics.blit(PLASMA_FORGE, this.leftPos + 51, this.topPos + 76, 195, 0, 3, 6, TEX_W, TEX_H);
            graphics.blit(PLASMA_FORGE, this.leftPos + 56, this.topPos + 76, 195, 0, 3, 6, TEX_W, TEX_H);
        } else if (hasRecipe) {
            graphics.blit(PLASMA_FORGE, this.leftPos + 51, this.topPos + 76, 192, 0, 3, 6, TEX_W, TEX_H);
            if (this.menu.power() >= this.menu.selectedPlasmaForgePower()) {
                graphics.blit(PLASMA_FORGE, this.leftPos + 56, this.topPos + 76, 192, 0, 3, 6, TEX_W, TEX_H);
            }
        }
        double inputGauge = !hasRecipe ? 0 : Math.min((double) this.menu.plasmaEnergy() / (double) this.menu.selectedPlasmaForgeIgnitionTemp(), 1.5) / 1.5D;
        double boosterGauge = this.menu.rawMaxBooster() <= 0 ? 0 : (double) this.menu.booster() / (double) this.menu.rawMaxBooster();
        K3LegacyGauge.draw(graphics, this.leftPos + 34, this.topPos + 124, inputGauge, 5, 2, 1, 0xA00000);
        K3LegacyGauge.draw(graphics, this.leftPos + 70, this.topPos + 124, boosterGauge, 5, 2, 1, 0xA00000);
        graphics.renderItem(this.menu.recipeIcon(), this.leftPos + 8, this.topPos + 81);
        renderPlasmaForgeGhosts(graphics);
        drawFluid(graphics, 80, 70, 16, 52, 0);
    }

    private static double speedScaled(double max, double level) {
        if (max == 0) {
            return 0D;
        }
        if (level >= max * 0.5) {
            return 1D;
        }
        return level / max * 2D;
    }

    private void renderPlasmaForgeGhosts(GuiGraphics graphics) {
        RecipeHolder<PlasmaForgeRecipe> holder = this.menu.selectedPlasmaForgeRecipe().orElse(null);
        if (holder == null) {
            return;
        }
        List<PlasmaForgeRecipe.CountedIngredient> inputs = holder.value().inputItems();
        for (int index = 0; index < inputs.size(); index++) {
            int slotIndex = FusionMachineBlockEntity.PLASMA_INPUT_START + index;
            if (slotIndex >= this.menu.slots.size()) {
                break;
            }
            Slot slot = this.menu.slots.get(slotIndex);
            if (slot.hasItem()) {
                continue;
            }
            ItemStack ghost = PlasmaForgeRecipeSelectorScreen.displayIngredient(inputs.get(index));
            GhostItemRenderer.render(graphics, ghost, this.leftPos + slot.x, this.topPos + slot.y, GHOST_ALPHA);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawFittedTitle(graphics);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
        if (this.menu.kind() == FusionMachineBlock.Kind.TORUS) {
            int heat = this.menu.temperature();
            int color = heat > 123 ? 0xAA0000 : 0x00AAAA;
            String label = heat + "K";
            graphics.drawString(this.font, label, 220 - this.font.width(label), 22, color, false);
            graphics.drawString(this.font, "/123K", 190, 32, 0x00AAAA, false);
        } else if (this.menu.kind() == FusionMachineBlock.Kind.KLYSTRON) {
            String result = "= " + com.atomiclegacy.inventory.gui.K3GuiText.shortNumber(this.menu.outputTarget()) + "KyU";
            if (this.menu.outputTarget() == FusionMachineBlockEntity.KLYSTRON_MAX_OUTPUT) {
                result += " (max)";
            }
            graphics.drawString(this.font, result, 183 - this.font.width(result), 40, 0x00FF00, false);
        }
    }

    private void drawFittedTitle(GuiGraphics graphics) {
        int centerX = switch (this.menu.kind()) {
            case TORUS -> 106;
            case KLYSTRON -> 115;
            case PLASMA_FORGE -> 70;
            default -> this.imageWidth / 2;
        };
        int maxWidth = switch (this.menu.kind()) {
            case TORUS -> 130;
            case KLYSTRON -> 150;
            case PLASMA_FORGE -> 126;
            default -> 136;
        };
        int width = this.font.width(this.title);
        if (width <= maxWidth) {
            graphics.drawString(this.font, this.title, centerX - width / 2, this.titleLabelY, LABEL_COLOR, false);
            return;
        }

        float scale = maxWidth / (float) width;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, this.titleLabelY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, this.title, -width / 2, 0, LABEL_COLOR, false);
        graphics.pose().popPose();
    }

    // [parity M3: fusion] drawScreen() tooltips of GUIFusionTorus / Klystron / Breeder / GUIMachinePlasmaForge.
    private void renderHoverTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.menu.kind() == FusionMachineBlock.Kind.TORUS) {
            energyTooltip(graphics, mouseX, mouseY, 8, 18, 16, 62);
            tankTooltip(graphics, mouseX, mouseY, 44, 18, 16, 52, 0);
            tankTooltip(graphics, mouseX, mouseY, 62, 18, 16, 52, 1);
            tankTooltip(graphics, mouseX, mouseY, 80, 18, 16, 52, 2);
            tankTooltip(graphics, mouseX, mouseY, 152, 18, 16, 52, 3);
            tankTooltip(graphics, mouseX, mouseY, 188, 46, 16, 52, 4);
            tankTooltip(graphics, mouseX, mouseY, 206, 46, 16, 52, 5);
            RecipeHolder<FusionRecipe> holder = this.menu.selectedFusionRecipeHolder().orElse(null);
            if (holder != null) {
                FusionRecipe recipe = holder.value();
                info(graphics, mouseX, mouseY, 43, 115, List.of(arrow("-> ", ChatFormatting.GREEN,
                        K3GuiText.shortNumber(this.menu.klystronEnergy()) + "KyU / " + K3GuiText.shortNumber(recipe.ignitionTemp()) + "KyU")));
                info(graphics, mouseX, mouseY, 79, 115, List.of(arrow("<- ", ChatFormatting.RED,
                        K3GuiText.shortNumber(this.menu.plasmaEnergy()) + "TU / " + K3GuiText.shortNumber(recipe.outputTemp()) + "TU")));
                List<Component> lines = new java.util.ArrayList<>();
                for (FusionRecipe.FusionFluidStack fluid : recipe.inputFluids()) {
                    int consumption = (int) Math.ceil(fluid.amount() * (this.menu.fuelConsumption() / 10_000.0D));
                    lines.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(consumption + "mB/t ").withStyle(ChatFormatting.RESET)
                                    .append(Component.translatable(fluid.fluid().translationKey()))));
                }
                info(graphics, mouseX, mouseY, 115, 115, lines);
            } else {
                info(graphics, mouseX, mouseY, 43, 115, List.of(Component.literal("0KyU / 0KyU")));
                info(graphics, mouseX, mouseY, 79, 115, List.of(Component.literal("0TU / 0TU")));
            }
            if (isHovering(43, 81, 18, 18, mouseX, mouseY)) {
                if (holder != null) LegacyRecipeTooltip.render(graphics, this.font, FusionRecipeSelectorScreen.describeRecipe(holder), mouseX, mouseY);
                else graphics.renderComponentTooltip(this.font, LegacyRecipeTooltip.setRecipe(), mouseX, mouseY);
            }
        } else if (this.menu.kind() == FusionMachineBlock.Kind.KLYSTRON) {
            energyTooltip(graphics, mouseX, mouseY, 8, 18, 16, 52);
            info(graphics, mouseX, mouseY, 43, 71, List.of(arrow("<- ", ChatFormatting.RED,
                    K3GuiText.shortNumber(this.menu.output()) + "KyU / " + K3GuiText.shortNumber(this.menu.outputTarget()) + "KyU")));
            tankTooltip(graphics, mouseX, mouseY, 76, 71, 18, 18, 0);
            info(graphics, mouseX, mouseY, 115, 71, List.of(arrow("-> ", ChatFormatting.GREEN,
                    K3GuiText.shortNumber(this.menu.output()) + "HE / " + K3GuiText.shortNumber(this.menu.outputTarget()) + "HE")));
        } else if (this.menu.kind() == FusionMachineBlock.Kind.BREEDER) {
            info(graphics, mouseX, mouseY, 79, 23, List.of(arrow("-> ", ChatFormatting.GREEN, this.menu.neutronEnergy() + " flux/t")));
            if (isHovering(67, 46, 42, 14, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, List.of(Component.literal(
                        String.format(java.util.Locale.US, "%,d", (int) Math.ceil(this.menu.progress() / 10_000.0D)) + " / "
                                + String.format(java.util.Locale.US, "%,d", (int) Math.ceil(FusionMachineBlockEntity.BREEDER_CAPACITY)) + " flux")),
                        mouseX, mouseY);
            }
            tankTooltip(graphics, mouseX, mouseY, 26, 18, 16, 52, 0);
            tankTooltip(graphics, mouseX, mouseY, 134, 18, 16, 52, 1);
        } else if (this.menu.kind() == FusionMachineBlock.Kind.PLASMA_FORGE) {
            tankTooltip(graphics, mouseX, mouseY, 80, 18, 16, 52, 0);
            energyTooltip(graphics, mouseX, mouseY, 152, 18, 16, 62);
            RecipeHolder<PlasmaForgeRecipe> holder = this.menu.selectedPlasmaForgeRecipe().orElse(null);
            if (isHovering(7, 81, 18, 18, mouseX, mouseY)) {
                if (holder != null) LegacyRecipeTooltip.render(graphics, this.font, PlasmaForgeRecipeSelectorScreen.describeRecipe(holder), mouseX, mouseY);
                else graphics.renderComponentTooltip(this.font, LegacyRecipeTooltip.setRecipe(), mouseX, mouseY);
            }
            if (holder != null) {
                info(graphics, mouseX, mouseY, 25, 115, List.of(arrow("-> ", ChatFormatting.GREEN,
                        K3GuiText.shortNumber(this.menu.plasmaEnergy()) + "TU / " + K3GuiText.shortNumber(holder.value().ignitionTemp()) + "TU")));
            } else {
                info(graphics, mouseX, mouseY, 25, 115, List.of(Component.literal("0TU / 0TU")));
            }
        }
    }

    private static Component arrow(String arrow, ChatFormatting color, String text) {
        return Component.literal(arrow).withStyle(color).append(Component.literal(text).withStyle(ChatFormatting.RESET));
    }

    private void info(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, List<Component> lines) {
        if (!lines.isEmpty() && isHovering(x, y, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, int tank) {
        if (isHovering(x, y, width, height, mouseX, mouseY)) {
            HbmFluidDefinition fluid = fluid(tank);
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(fluid, this.menu.tankAmount(tank), this.menu.tankCapacity(tank)), mouseX, mouseY);
        }
    }

    private void energyTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (isHovering(x, y, width, height, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.atomiclegacy.energy",
                    this.menu.power(),
                    this.menu.maxPower()
            )), mouseX, mouseY);
        }
    }

    // [parity M3: fusion] FluidTank.renderTank: tinted fluid texture, (fill * height) / maxFill.
    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, int tank) {
        int scaled = this.menu.tankAmount(tank) * height / this.menu.tankCapacity(tank);
        DfcScreenUtil.drawFluid(this.minecraft, graphics, this.leftPos, this.topPos, x, bottomY, width, scaled, fluid(tank));
    }

    private HbmFluidDefinition fluid(int tank) {
        return HbmFluids.byOldId(this.menu.tankFluidId(tank)).orElse(HbmFluids.none());
    }

    private boolean in(double mouseX, double mouseY, int x, int y, int width, int height) {
        double left = this.leftPos + x;
        double top = this.topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private void playClick() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void sendKlystronTarget(String value) {
        long target = 0L;
        if (!value.isBlank()) {
            try {
                target = Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                target = FusionMachineBlockEntity.KLYSTRON_MAX_OUTPUT;
            }
        }
        PacketDistributor.sendToServer(new FusionMachineControlPayload(this.menu.blockPos(), FusionMachineControlPayload.ACTION_SET_KLYSTRON_TARGET, target));
    }
}
