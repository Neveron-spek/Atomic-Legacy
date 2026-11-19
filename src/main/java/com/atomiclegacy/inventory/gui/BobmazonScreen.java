package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.handler.LegacyBobmazonOffers;
import com.atomiclegacy.items.LegacyBobmazonItem;
import com.atomiclegacy.network.BobmazonOrderPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Original 217x229 Bobmazon catalog layout with three order buttons per page. */
public final class BobmazonScreen extends Screen {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/gui_bobmazon.png");
    private static final int WIDTH = 217;
    private static final int HEIGHT = 229;
    private final boolean hidden;
    private final List<LegacyBobmazonOffers.Offer> offers;
    private int leftPos;
    private int topPos;
    private int page;

    private BobmazonScreen(boolean hidden) {
        super(Component.translatable(hidden ? "item.atomiclegacy.bobmazon_hidden" : "item.atomiclegacy.bobmazon"));
        this.hidden = hidden;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) throw new IllegalStateException("Bobmazon catalog opened without a client level");
        this.offers = LegacyBobmazonOffers.offers(hidden, minecraft.level.registryAccess());
    }

    public static void open(boolean hidden) { Minecraft.getInstance().setScreen(new BobmazonScreen(hidden)); }

    @Override protected void init() { leftPos = (width - WIDTH) / 2; topPos = (height - HEIGHT) / 2; }

    /** [parity g2] GUIScreenBobmazon#getPageCount: (int) Math.ceil((offers.size() - 1) / 3) with integer division. */
    private int pageCount() {
        return (int) Math.ceil((offers.size() - 1) / 3);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        if (page < 0) page = 0;
        if (page > pageCount()) page = pageCount();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
        int end = Math.min(page * 3 + 3, offers.size());
        // FolderButton#drawButton / drawIcon
        for (int index = page * 3; index < end; index++) {
            int x = leftPos + 34;
            int y = topPos + 35 + 54 * index - page * 3 * 54;
            graphics.blit(TEXTURE, x, y, onButton(x, y, mouseX, mouseY) ? 176 + 41 + 18 : 176 + 41, 0, 18, 18);
        }
        if (page != 0) {
            graphics.blit(TEXTURE, prevX(), navY(), onButton(prevX(), navY(), mouseX, mouseY) ? 176 + 41 + 18 : 176 + 41, 18, 18, 18);
        }
        if (page != pageCount()) {
            graphics.blit(TEXTURE, nextX(), navY(), onButton(nextX(), navY(), mouseX, mouseY) ? 176 + 41 + 18 : 176 + 41, 36, 18, 18);
        }
        for (int index = page * 3; index < end; index++) {
            graphics.renderItem(offers.get(index).stack(), leftPos + 34 + 1, topPos + 35 + 54 * index - page * 3 * 54 + 1);
        }
        // Offer#drawRequirement
        for (int index = page * 3; index < end; index++) {
            drawRequirement(graphics, offers.get(index), leftPos + 34, topPos + 53 + 54 * index - page * 3 * 54);
        }
        String pages = (page + 1) + "/" + (pageCount() + 1);
        graphics.drawString(font, pages, leftPos + WIDTH / 2 - font.width(pages) / 2, topPos + 205, 4210752, false);
        // FolderButton#drawString: only the page buttons have info text
        if (page != 0 && onButton(prevX(), navY(), mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal("Previous"), mouseX, mouseY);
        }
        if (page != pageCount() && onButton(nextX(), navY(), mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal("Next"), mouseX, mouseY);
        }
    }

    private void drawRequirement(GuiGraphics graphics, LegacyBobmazonOffers.Offer offer, int x, int y) {
        graphics.blit(TEXTURE, x + 19, y - 4, 176 + 41, 62, 39, 8);
        int rating = offer.rating() * 4 - 1;
        if (rating > 0) graphics.blit(TEXTURE, x + 19, y - 4, 176 + 41, 54, rating, 8);
        String count = offer.stack().getCount() > 1 ? " x" + offer.stack().getCount() : "";
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 0.5F);
        graphics.drawString(font, offer.stack().getHoverName().getString() + count, (int) ((x + 20) / 0.5F), (int) ((y - 12) / 0.5F), 4210752, false);
        graphics.pose().popPose();
        String price = offer.caps() + " Cap";
        if (offer.caps() != 1) price += "s";
        graphics.drawString(font, price, x + 62, y - 3, 4210752, false);
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 0.5F);
        // every offer of 936c60c uses the default comment "No Ratings" and an empty author
        graphics.drawString(font, "No Ratings", (x + 20) * 2, (y + 8) * 2, 0x222222, false);
        graphics.pose().popPose();
        graphics.renderItem(requirementIcon(offer.requirement()), x + 1, y + 1);
    }

    /** Icon item of the 1.7.10 achievement behind each Requirement. */
    private static ItemStack requirementIcon(LegacyBobmazonOffers.Requirement requirement) {
        String id = switch (requirement) {
            case NONE -> "minecraft:book"; // AchievementList.openInventory
            case STEEL -> "atomiclegacy:machine_blast_furnace";
            case ASSEMBLY -> "atomiclegacy:machine_assembly_machine";
            case CHEMICS -> "atomiclegacy:machine_chemical_plant";
            case OIL -> "atomiclegacy:ingot_desh";
            case NUCLEAR -> "atomiclegacy:ingot_tcalloy";
            case HIDDEN -> "atomiclegacy:achievement_icon_questionmark";
        };
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)));
    }

    private int prevX() { return leftPos + 25 - 18; }
    private int nextX() { return leftPos + 25 + (27 * 4) + 18 + 41; }
    private int navY() { return topPos + 26 + (27 * 3); }

    /** FolderButton#isMouseOnButton */
    private static boolean onButton(int x, int y, double mouseX, double mouseY) {
        return x <= mouseX && x + 18 > mouseX && y < mouseY && y + 18 >= mouseY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [parity g2] any mouse button; buttons in FolderButton order: offers, previous, next
        int end = Math.min(page * 3 + 3, offers.size());
        for (int index = page * 3; index < end; index++) {
            if (onButton(leftPos + 34, topPos + 35 + 54 * index - page * 3 * 54, mouseX, mouseY)) {
                clickSound();
                PacketDistributor.sendToServer(new BobmazonOrderPayload(index));
                return true;
            }
        }
        if (page != 0 && onButton(prevX(), navY(), mouseX, mouseY)) {
            clickSound();
            if (page > 0) page--;
            return true;
        }
        if (page != pageCount() && onButton(nextX(), navY(), mouseX, mouseY)) {
            clickSound();
            if (page < pageCount()) page++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void clickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) { // [parity g2] keyTyped
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * [parity K1] GUIScreenBobmazon#updateScreen: page clamping and the closeScreen() for the player
     * "SolsticeUnlimitd" while the hidden catalog is held.
     */
    @Override
    public void tick() {
        super.tick();
        if (page < 0) page = 0;
        if (page > pageCount()) page = pageCount();
        if (minecraft != null && minecraft.player != null
                && minecraft.player.getMainHandItem().is(com.atomiclegacy.registry.HbmItems.BOBMAZON_HIDDEN.get())
                && "SolsticeUnlimitd".equals(minecraft.player.getGameProfile().getName())) {
            minecraft.player.closeContainer();
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
