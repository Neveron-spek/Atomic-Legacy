package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyClayTabletItem;
import com.atomiclegacy.tileentity.LegacyPedestalRecipes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

/** [parity g2] GUIScreenClayTablet: pedestal recipe clue picked by the stack's tabletSeed, partially revealed. */
public final class ClayTabletScreen extends Screen {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/guide_pedestal.png");
    private static final int WIDTH = 142;
    private static final int HEIGHT = 84;
    private final InteractionHand hand;
    private int tabletMeta = 0;
    private int left;
    private int top;

    private ClayTabletScreen(InteractionHand hand) {
        super(Component.translatable("item.atomiclegacy.clay_tablet"));
        this.hand = hand;
    }

    public static void open(InteractionHand hand) { Minecraft.getInstance().setScreen(new ClayTabletScreen(hand)); }
    @Override protected void init() { left = (width - WIDTH) / 2; top = (height - HEIGHT) / 2; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        ItemStack held = minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(hand);
        if (!held.isEmpty()) tabletMeta = held.getItem() instanceof LegacyClayTabletItem ? LegacyClayTabletItem.meta(held) : held.getDamageValue();

        int tabletOffset = tabletMeta == 1 ? 84 : 0;
        int iconOffset = tabletMeta == 1 ? 16 : 0;
        float revealChance = tabletMeta == 1 ? 0.25F : 0.5F;
        graphics.blit(TEXTURE, left, top, 0, tabletOffset, WIDTH, HEIGHT);

        List<LegacyPedestalRecipes.Recipe> recipeSet = LegacyPedestalRecipes.recipeSet(Math.abs(tabletMeta) % LegacyPedestalRecipes.RECIPE_SET_COUNT);
        if (!held.isEmpty() && held.getItem() instanceof LegacyClayTabletItem && LegacyClayTabletItem.hasSeed(held) && !recipeSet.isEmpty()) {
            Random rand = new Random(LegacyClayTabletItem.seed(held));
            LegacyPedestalRecipes.Recipe recipe = recipeSet.get(rand.nextInt(recipeSet.size()));
            switch (recipe.extraName()) {
                case "FULL_MOON" -> graphics.blit(TEXTURE, left + 120, top + 62, 142 + iconOffset, 32, 16, 16);
                case "NEW_MOON" -> graphics.blit(TEXTURE, left + 120, top + 62, 142 + iconOffset, 48, 16, 16);
                case "SUN" -> graphics.blit(TEXTURE, left + 120, top + 62, 142 + iconOffset, 64, 16, 16);
                default -> { }
            }
            for (int l = 0; l < 3; l++) {
                for (int r = 0; r < 3; r++) {
                    int x = left + 7 + r * 27;
                    int y = top + 7 + l * 27;
                    if (rand.nextFloat() > revealChance) {
                        graphics.blit(TEXTURE, x, y, 142 + iconOffset, 16, 16, 16);
                    } else {
                        List<ItemStack> inputs = recipe.displayInput(r + l * 3);
                        if (inputs == null) {
                            graphics.blit(TEXTURE, x, y, 142 + iconOffset, 0, 16, 16);
                            continue;
                        }
                        ItemStack input = inputs.isEmpty() ? new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AtomicLegacy.id("nothing")))
                                : inputs.get((int) (Math.abs(System.currentTimeMillis() / 1000) % inputs.size()));
                        graphics.renderItem(input, x, y);
                        graphics.renderItemDecorations(font, input, x, y);
                    }
                }
            }
            ItemStack output = recipe.displayOutput();
            graphics.renderItem(output, left + WIDTH / 2 - 8, top - 20);
            graphics.renderItemDecorations(font, output, left + WIDTH / 2 - 8, top - 20);
            String label = output.getHoverName().getString();
            RenderSystem.disableDepthTest();
            graphics.drawString(font, label, left + (WIDTH - font.width(label)) / 2, top - 30, 0xFFFFFF, false);
            graphics.flush();
            RenderSystem.enableDepthTest();
        } else {
            for (int l = 0; l < 3; l++) {
                for (int r = 0; r < 3; r++) {
                    graphics.blit(TEXTURE, left + 7 + r * 27, top + 7 + l * 27, 142 + iconOffset, 16, 16, 16);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) { // keyTyped
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
