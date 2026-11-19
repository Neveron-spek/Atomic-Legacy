package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyBookLoreItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Pixel-layout port of 1.7.10 GUIBookLore. */
public final class LegacyBookLoreScreen extends Screen {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/book/book_lore.png");
    private static final int WIDTH = 272;
    private static final int HEIGHT = 182;
    private static final int PAGE_WIDTH = 100;
    private static final int PAGE_OFFSET = 130;

    private final ItemStack book;
    private final String key;
    private final CompoundTag data;
    private final int maxSpread;
    private int left;
    private int top;
    private int spread;

    private LegacyBookLoreScreen(ItemStack book) {
        super(book.getHoverName());
        this.book = book.copy();
        this.key = LegacyBookLoreItem.key(book);
        this.data = LegacyBookLoreItem.data(book);
        this.maxSpread = Math.max(0, (int) Math.ceil(LegacyBookLoreItem.pages(book) / 2.0D) - 1);
    }

    public static void open(ItemStack book) {
        Minecraft.getInstance().setScreen(new LegacyBookLoreScreen(book));
    }

    @Override
    protected void init() {
        if (key.isEmpty()) {
            onClose();
            return;
        }
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int coverColor = LegacyBookLoreItem.coverColor(book);
        graphics.setColor(((coverColor >> 16) & 0xFF) / 255.0F, ((coverColor >> 8) & 0xFF) / 255.0F,
                (coverColor & 0xFF) / 255.0F, 1.0F);
        graphics.blit(TEXTURE, left, top, 0, 0, WIDTH, HEIGHT, 512, 512);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, left + 7, top + 7, 0, 182, 258, 165, 512, 512);
        drawArrows(graphics, mouseX, mouseY);
        drawPages(graphics);
    }

    private void drawArrows(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean overY = mouseY >= top + 155 && mouseY < top + 165;
        if (spread > 0) {
            graphics.blit(TEXTURE, left + 24, top + 155, overY && mouseX >= left + 24 && mouseX <= left + 42 ? 295 : 272,
                    13, 18, 10, 512, 512);
        }
        if (spread < maxSpread) {
            graphics.blit(TEXTURE, left + 230, top + 155, overY && mouseX >= left + 230 && mouseX <= left + 248 ? 295 : 272,
                    0, 18, 10, 512, 512);
        }
    }

    private void drawPages(GuiGraphics graphics) {
        int pageCount = LegacyBookLoreItem.pages(book);
        for (int side = 0; side < 2; side++) {
            int page = spread * 2 + side;
            if (page >= pageCount) {
                continue;
            }
            String text = pageText(page);
            List<FormattedCharSequence> lines = wrap(text);
            int x = left + 20 + side * PAGE_OFFSET;
            for (int line = 0; line < lines.size(); line++) {
                graphics.drawString(font, lines.get(line), x, top + 20 + line * 9, 0xFF0F0F0F, false);
            }
        }
    }

    private String pageText(int page) {
        CompoundTag arguments = data.getCompound("p" + page);
        List<Object> values = new ArrayList<>();
        for (int index = 1; arguments.contains("a" + index); index++) {
            values.add(arguments.getString("a" + index));
        }
        return Component.translatable("book_lore." + key + ".page." + page, values.toArray()).getString();
    }

    /** [parity g1] GUIBookLore.drawGuiContainerForegroundLayer: greedy word wrap at 100 px, "$" starts a new line. */
    private List<FormattedCharSequence> wrap(String text) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        lines.add(words[0]);
        int indent = font.width(words[0]);
        for (int w = 1; w < words.length; w++) {
            if (words[w].equals("$")) {
                if (w + 1 < words.length && !words[w + 1].equals("$")) {
                    lines.add(words[++w]);
                    indent = font.width(words[w]);
                } else {
                    lines.add("");
                }
                continue;
            }
            indent += font.width(" " + words[w]);
            if (indent <= PAGE_WIDTH) {
                lines.set(lines.size() - 1, lines.get(lines.size() - 1) + " " + words[w]);
            } else {
                lines.add(words[w]);
                indent = font.width(words[w]);
            }
        }
        List<FormattedCharSequence> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(Component.literal(line).getVisualOrderText());
        }
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= top + 155 && mouseY < top + 165) {
            if (spread > 0 && mouseX >= left + 24 && mouseX <= left + 42) {
                spread--;
                click();
                return true;
            }
            if (spread < maxSpread && mouseX >= left + 230 && mouseX <= left + 248) {
                spread++;
                click();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
