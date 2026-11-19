package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyRttyPagerItem;
import com.atomiclegacy.network.SetRttyDesignatorChannelPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Original 184x42 RTTY designator channel editor. */
public final class RttyDesignatorScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            AtomicLegacy.id("textures/gui/machine/gui_rtty_designator.png");
    private static final int IMAGE_WIDTH = 184;
    private static final int IMAGE_HEIGHT = 42;

    private final ItemStack designator;
    private final InteractionHand hand;
    private EditBox channel;
    private int left;
    private int top;

    private RttyDesignatorScreen(ItemStack designator, InteractionHand hand) {
        super(Component.translatable("container.atomiclegacy.rtty_designator"));
        this.designator = designator;
        this.hand = hand;
    }

    public static void open(ItemStack designator, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new RttyDesignatorScreen(designator, hand));
    }

    @Override
    protected void init() {
        left = (width - IMAGE_WIDTH) / 2;
        top = (height - IMAGE_HEIGHT) / 2;
        channel = new EditBox(font, left + 31, top + 23, 82, 14, Component.empty());
        // [parity g2] GUIScreenRBMKKeyPad.setupTextFieldStandard: green text, no background
        channel.setTextColor(0x00FF00);
        channel.setTextColorUneditable(0x00FF00);
        channel.setBordered(false);
        channel.setMaxLength(LegacyRttyPagerItem.MAX_CHANNEL_LENGTH);
        channel.setValue(LegacyRttyPagerItem.channel(designator));
        addRenderableWidget(channel);
        // [parity g2] GuiTextField starts unfocused
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, left, top, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        graphics.drawString(font, title, left + (IMAGE_WIDTH - font.width(title)) / 2, top + 6, 4210752, false); // [parity g2] 0x404040
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (K3GuiText.hover(mouseX, mouseY, left + 137, top + 17, 18, 18)) {
            applyChannel();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // [parity g2] keyTyped: text field first, then Esc / inventory key close; Enter does nothing
        if (channel.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE && channel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode) && !channel.isFocused()) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applyChannel() {
        LegacyRttyPagerItem.setChannel(designator, channel.getValue());
        PacketDistributor.sendToServer(new SetRttyDesignatorChannelPayload(
                hand == InteractionHand.OFF_HAND, channel.getValue()));
        K3GuiText.click(); // [parity g2] gui.button.press
    }
}
