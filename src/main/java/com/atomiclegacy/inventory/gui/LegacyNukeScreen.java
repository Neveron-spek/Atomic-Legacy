package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyNukeDefinition;
import com.atomiclegacy.inventory.container.LegacyNukeMenu;
import com.atomiclegacy.tileentity.LegacyNukeBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Uses the exact 1.7.10 schematic background selected by the bomb type. */
public final class LegacyNukeScreen extends AbstractContainerScreen<LegacyNukeMenu> {
    private static final ResourceLocation IVY_MIKE_OVERLAY = AtomicLegacy.id("textures/gui/weapon/ivymikeschematic.png");

    public LegacyNukeScreen(LegacyNukeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        LegacyNukeDefinition definition = menu.definition();
        this.imageWidth = definition == LegacyNukeDefinition.TSAR ? 256 : 176;
        this.imageHeight = definition.guiHeight();
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        // [parity e2] GUINukeTsar draws "container.inventory" at x = 48
        if (definition == LegacyNukeDefinition.TSAR) {
            this.inventoryLabelX = 48;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        // [parity e2] GUINukeMike draws its title at y = 4
        if (menu.definition() == LegacyNukeDefinition.MIKE) {
            this.titleLabelY = 4;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture(menu.definition()), this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
        // [parity e2] component / readiness overlays of the individual GUINuke* screens
        switch (menu.definition()) {
            case TSAR -> renderTsarOverlays(graphics);
            case GADGET, MAN -> {
                if (menu.slotHasExpectedItem(1)) own(graphics, 82, 19, 176, 0, 24, 24);
                if (menu.slotHasExpectedItem(2)) own(graphics, 106, 19, 200, 0, 24, 24);
                if (menu.slotHasExpectedItem(3)) own(graphics, 82, 43, 176, 24, 24, 24);
                if (menu.slotHasExpectedItem(4)) own(graphics, 106, 43, 200, 24, 24, 24);
                if (menu.isReady()) own(graphics, 134, 35, 176, 48, 16, 16);
            }
            case MIKE -> {
                if (menu.isReady() && !menu.isFilled()) own(graphics, 5, 35, 177, 1, 16, 16);
                if (menu.isReady() && menu.isFilled()) own(graphics, 5, 35, 177, 19, 16, 16);
                if (menu.slotHasExpectedItem(5)) own(graphics, 75, 25, 176, 49, 80, 36);
                if (menu.slotHasExpectedItem(6)) own(graphics, 79, 30, 180, 88, 58, 26);
                if (menu.slotHasExpectedItem(7)) own(graphics, 140, 30, 240, 88, 12, 26);
                if (menu.slotHasExpectedItem(0)) own(graphics, 24, 20, 209, 1, 23, 23);
                if (menu.slotHasExpectedItem(2)) own(graphics, 47, 20, 232, 1, 23, 23);
                if (menu.slotHasExpectedItem(1)) own(graphics, 24, 43, 209, 24, 23, 23);
                if (menu.slotHasExpectedItem(3)) own(graphics, 47, 43, 232, 24, 23, 23);
            }
            case FLEIJA -> {
                if (menu.slotHasExpectedItem(0)) own(graphics, 7, 88, 176, 0, 30, 20);
                if (menu.slotHasExpectedItem(1)) own(graphics, 139, 88, 206, 0, 30, 20);
                if (menu.slotHasExpectedItem(2)) own(graphics, 57, 77, 176, 62, 18, 14);
                if (menu.slotHasExpectedItem(3)) own(graphics, 57, 91, 176, 76, 18, 14);
                if (menu.slotHasExpectedItem(4)) own(graphics, 57, 105, 176, 90, 18, 14);
                if (menu.slotHasExpectedItem(5)) own(graphics, 85, 77, 176, 20, 18, 15);
                if (menu.slotHasExpectedItem(6)) own(graphics, 103, 77, 194, 20, 18, 15);
                if (menu.slotHasExpectedItem(7)) own(graphics, 85, 92, 176, 35, 18, 12);
                if (menu.slotHasExpectedItem(8)) own(graphics, 103, 92, 194, 35, 18, 12);
                if (menu.slotHasExpectedItem(9)) own(graphics, 85, 104, 176, 47, 18, 15);
                if (menu.slotHasExpectedItem(10)) own(graphics, 103, 104, 194, 47, 18, 15);
            }
            case SOLINIUM -> {
                if (menu.slotHasExpectedItem(0)) own(graphics, 24, 84, 0, 222, 22, 14);
                if (menu.slotHasExpectedItem(1)) own(graphics, 46, 84, 22, 222, 18, 14);
                if (menu.slotHasExpectedItem(2)) own(graphics, 76, 84, 52, 222, 18, 14);
                if (menu.slotHasExpectedItem(3)) own(graphics, 94, 84, 70, 222, 22, 14);
                if (menu.slotHasExpectedItem(4)) own(graphics, 64, 84, 40, 222, 12, 28);
                if (menu.slotHasExpectedItem(5)) own(graphics, 24, 98, 0, 236, 22, 14);
                if (menu.slotHasExpectedItem(6)) own(graphics, 46, 98, 22, 236, 18, 14);
                if (menu.slotHasExpectedItem(7)) own(graphics, 76, 98, 52, 236, 18, 14);
                if (menu.slotHasExpectedItem(8)) own(graphics, 94, 98, 70, 236, 22, 14);
                if (menu.isReady()) own(graphics, 134, 90, 176, 0, 16, 16);
            }
            case N2 -> {
                int count = 0;
                for (int i = 0; i < 12; i++) if (menu.slotHasExpectedItem(i)) count++;
                if (count > 0) own(graphics, 35, 120 - 6 * count, 176, 0, 34, 6 * count);
            }
            case BALEFIRE -> {
                if (menu.slotHasExpectedItem(0)) own(graphics, 19, 90, 176, 0, 30, 16);
                net.minecraft.world.item.ItemStack battery = menu.getSlot(1).getItem();
                String id = battery.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(battery.getItem()).getPath();
                if (id.equals("battery_spark")) own(graphics, 88, 93, 176, 16, 18, 10);
                else if (id.equals("battery_trixite")) own(graphics, 88, 93, 194, 16, 18, 10);
            }
            case CUSTOM -> {
                LegacyNukeBlockEntity nuke = menu.nukeEntity();
                if (nuke != null) {
                    menu.isReady(); // recomputes the payload levels from the current slots
                    if (nuke.euph > 0) own(graphics, 142, 89, 176, 108, 18, 18);
                    else if (nuke.schrab > 0) own(graphics, 106, 89, 176, 90, 18, 18);
                    else if (nuke.amat > 0) own(graphics, 70, 89, 176, 54, 18, 18);
                    else if (nuke.hydro > 0) own(graphics, 52, 89, 176, 36, 18, 18);
                    else if (nuke.nuke > 0) own(graphics, 34, 89, 176, 18, 18, 18);
                    else if (nuke.tnt > 0) own(graphics, 16, 89, 176, 0, 18, 18);
                    if (nuke.dirty > 0 && nuke.nuke > 0 && nuke.amat == 0 && nuke.schrab == 0 && nuke.euph == 0) {
                        own(graphics, 88, 89, 176, 72, 18, 18);
                    }
                }
            }
            default -> {
            }
        }
        if (infoKey() != null) {
            // drawInfoPanel(guiLeft - 16, guiTop + 16, 16, 16, 2)
            graphics.blit(K3GuiText.UTILITY, this.leftPos - 16, this.topPos + 16, 8, 0, 16, 16, 256, 256);
        }
    }

    private void own(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(texture(menu.definition()), this.leftPos + x, this.topPos + y, (float) u, (float) v, width, height, 256, 256);
    }

    private String infoKey() {
        return switch (menu.definition()) {
            case GADGET -> "desc.gui.nukeGadget.desc";
            case MAN -> "desc.gui.nukeMan.desc";
            case MIKE -> "desc.gui.nukeMike.desc";
            case TSAR -> "desc.gui.nukeTsar.desc";
            default -> null;
        };
    }

    /** [parity e2] item tooltips (were missing) + drawCustomInfoStat areas of GUINukeGadget/Man/Mike/Tsar/Custom */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        String key = infoKey();
        if (key != null && K3GuiText.hover(mouseX, mouseY, this.leftPos - 16, this.topPos + 16, 16, 16)) {
            graphics.renderComponentTooltip(this.font, LegacyInfoText.lines(key), this.leftPos - 8, this.topPos + 16 + 16);
        }
        LegacyNukeBlockEntity nuke = menu.nukeEntity();
        if (menu.definition() == LegacyNukeDefinition.CUSTOM && nuke != null) {
            String y = "§e";
            String i = "§o";
            float nukeAdj = nuke.nuke == 0 ? 0F : Math.min(nuke.nuke + nuke.tnt / 2, 200);
            float hydroAdj = nuke.hydro == 0 ? 0F : Math.min(nuke.hydro + nuke.nuke / 2 + nuke.tnt / 4, 350);
            float amatAdj = nuke.amat == 0 ? 0F : Math.min(nuke.amat + nuke.hydro / 2 + nuke.nuke / 4 + nuke.tnt / 8, 350);
            float schrabAdj = nuke.schrab == 0 ? 0F : Math.min(nuke.schrab + nuke.amat / 2 + nuke.hydro / 4 + nuke.nuke / 8 + nuke.tnt / 16, 250);
            String[][] text = {
                    {y + "Conventional Explosives (Level " + nuke.tnt + "/" + Math.min(nuke.tnt, 150) + ")", "Caps at 150",
                            "N²-like above level 75", i + "\"Goes boom\""},
                    {y + "Nuclear (Level " + nuke.nuke + "/" + nukeAdj + ")", "Requires TNT level 16", "Caps at 200", "Has fallout",
                            i + "\"Now I am become death, destroyer of worlds.\""},
                    {y + "Thermonuclear (Level " + nuke.hydro + "/" + hydroAdj + ")", "Requires nuclear level 100", "Caps at 350",
                            "Reduces added fallout by salted stage by 75%", i + "\"And for my next trick, I'll make",
                            i + "the island of Elugelab disappear!\""},
                    {y + "Antimatter (Level " + nuke.amat + "/" + amatAdj + ")", "Requires nuclear level 50", "Caps at 350",
                            i + "\"Antimatter, Balefire, whatever.\""},
                    {y + "Salted (Level " + nuke.dirty + "/" + Math.min(nuke.dirty, 100) + ")", "Extends fallout of nuclear and",
                            "thermonuclear stages", "Caps at 100", i + "\"Not to be confused with tablesalt.\""},
                    {y + "Schrabidium (Level " + nuke.schrab + "/" + schrabAdj + ")", "Requires nuclear level 50", "Caps at 250",
                            i + "\"For the hundredth time,", i + "you can't bypass these caps!\""},
                    {y + "Ice cream (Level unknown)", i + "\"Probably not ice cream but the label came off.\""}};
            int[] xs = {16, 34, 52, 70, 88, 106, 142};
            for (int n = 0; n < xs.length; n++) {
                if (K3GuiText.hover(mouseX, mouseY, this.leftPos + xs[n], this.topPos + 89, 18, 18)) {
                    java.util.List<Component> lines = new java.util.ArrayList<>();
                    for (String line : text[n]) {
                        lines.add(Component.literal(line));
                    }
                    graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                4210752, false);
    }

    private static ResourceLocation texture(LegacyNukeDefinition definition) {
        String name = switch (definition) {
            case GADGET -> "gadgetschematic";
            case MAN -> "fatmanschematic";
            case MIKE -> "ivymikeschematic";
            case TSAR -> "tsarbombaschematic";
            case FLEIJA -> "fleijaschematic";
            case PROTOTYPE -> "gui_prototype";
            case SOLINIUM -> "soliniumschematic";
            case N2 -> "n2schematic";
            case CUSTOM -> "gunbombschematic";
            case BALEFIRE -> "fstbmbschematic";
        };
        return AtomicLegacy.id("textures/gui/weapon/" + name + ".png");
    }

    private void renderTsarOverlays(GuiGraphics graphics) {
        if (menu.isFilled()) {
            blit(graphics, 18, 50, 176, 18, 16, 16);
        } else if (menu.isReady()) {
            blit(graphics, 18, 50, 176, 0, 16, 16);
        }

        for (int slot = 0; slot < 4; slot++) {
            if (menu.slotHasExpectedItem(slot)) {
                switch (slot) {
                    case 0 -> blit(graphics, 40, 36, 209, 1, 23, 23);
                    case 2 -> blit(graphics, 63, 36, 232, 1, 23, 23);
                    case 1 -> blit(graphics, 40, 59, 209, 24, 23, 23);
                    case 3 -> blit(graphics, 63, 59, 232, 24, 23, 23);
                    default -> {
                    }
                }
            }
        }

        if (menu.slotHasExpectedItem(5)) {
            blit(graphics, 91, 41, 176, 220, 80, 36);
        }
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(IVY_MIKE_OVERLAY, this.leftPos + x, this.topPos + y, (float) u, (float) v,
                width, height, 256, 256);
    }
}
