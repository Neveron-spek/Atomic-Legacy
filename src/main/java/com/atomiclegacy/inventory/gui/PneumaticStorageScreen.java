package com.atomiclegacy.inventory.gui;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.PneumaticStorageBlock;
import com.atomiclegacy.network.PneumaticAccessPayloads;
import com.atomiclegacy.tileentity.PneumaticStorageBlockEntity;
import com.atomiclegacy.inventory.container.PneumaticStorageMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Uses the original endpoint GUI sheets; this is not a JEI background. */
public final class PneumaticStorageScreen extends AbstractContainerScreen<PneumaticStorageMenu> {
    private static final ResourceLocation ACCESS = texture("access");
    private static final ResourceLocation CLUTTER = texture("clutter");
    private static final ResourceLocation MONO = texture("mono");
    private static final ResourceLocation IMPORTER = texture("importer");
    private static final ResourceLocation EXPORTER = texture("exporter");

    // [parity V2: storage] GUIPneumoStorageAccess / ContainerPneumoStorageAccess client state (static as in 1.7.10)
    private static int sorting = 0;
    private static boolean startFocussed = false;
    private static boolean detailedSearch = false;
    private EditBox search;
    private String searchString = "";
    private int scrollIndex = 0;
    private int scrollBounds = 1;
    private int listingSize = 0;
    private boolean draggingScroll = false;
    private int builtVersion = -1;
    private boolean indexDirty = true;

    public PneumaticStorageScreen(PneumaticStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        switch (menu.kind()) {
            case ACCESS -> {
                imageWidth = 210;
                imageHeight = 251;
            }
            case CLUTTER -> {
                imageWidth = 200;
                imageHeight = 235;
            }
            case MONO -> {
                imageWidth = 200;
                imageHeight = 181;
            }
            case IMPORTER, EXPORTER -> {
                imageWidth = 176;
                imageHeight = 185;
            }
        }
        inventoryLabelX = menu.kind() == PneumaticStorageBlock.Kind.ACCESS ? 42 : 8;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private boolean access() {
        return menu.kind() == PneumaticStorageBlock.Kind.ACCESS;
    }

    @Override
    protected void init() {
        super.init();
        int contentOffset = access() ? 34 : 0;
        titleLabelX = contentOffset + 88 - font.width(title) / 2;
        if (access()) {
            String text = search != null ? search.getValue() : "";
            search = new EditBox(font, leftPos + 45 + 34, topPos + 127 + 2, 86, 12, Component.empty());
            search.setTextColor(0xffffff);
            search.setTextColorUneditable(0xa0a0a0);
            search.setBordered(false);
            search.setMaxLength(50);
            search.setValue(text);
            search.setResponder(value -> {
                scrollIndex = 0;
                searchString = value.toLowerCase(Locale.US);
                indexDirty = true;
            });
            if (startFocussed) {
                search.setFocused(true);
                setFocused(search);
            }
            addRenderableWidget(search);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (access()) {
            rebuildClientIndexIfNeeded();
        }
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (access()) {
            infoStat(graphics, mouseX, mouseY, 7, 7, "Sorting: " + ChatFormatting.YELLOW + "Amount");
            infoStat(graphics, mouseX, mouseY, 7, 25, "Sorting: " + ChatFormatting.YELLOW + "Item ID");
            infoStat(graphics, mouseX, mouseY, 7, 43, "Sorting: " + ChatFormatting.YELLOW + "Name");
            infoStat(graphics, mouseX, mouseY, 7, 61, "Sorting: " + ChatFormatting.YELLOW + "Internal Name");
            infoStat(graphics, mouseX, mouseY, 7, 79, "Focus search by default: " + (startFocussed ? ChatFormatting.GREEN + "ON" : ChatFormatting.RED + "OFF"));
            infoStat(graphics, mouseX, mouseY, 7, 97, "Inlude tooltips in search: " + (detailedSearch ? ChatFormatting.GREEN + "ON" : ChatFormatting.RED + "OFF"));
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void infoStat(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, String text) {
        if (checkClick(mouseX, mouseY, x, y, 18, 18)) {
            graphics.renderTooltip(font, Component.literal(text), mouseX, mouseY);
        }
    }

    /** GuiInfoContainer.checkClick */
    private boolean checkClick(double x, double y, int left, int top, int sizeX, int sizeY) {
        return leftPos + left <= x && leftPos + left + sizeX > x && topPos + top < y && topPos + top + sizeY >= y;
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (access()) {
            if (search != null) { // GuiTextField.mouseClicked: focus follows the click
                boolean inside = search.isMouseOver(mouseX, mouseY);
                search.setFocused(inside);
                if (!inside && getFocused() == search) setFocused(null);
            }
            if (button == 0) {
                for (int i = 0; i < 4; i++) {
                    if (checkClick(mouseX, mouseY, 7, 7 + i * 18, 18, 18)) {
                        click();
                        sorting = i;
                        scrollIndex = 0;
                        indexDirty = true;
                    }
                }
                if (checkClick(mouseX, mouseY, 7, 79, 18, 18)) {
                    click();
                    startFocussed = !startFocussed;
                }
                if (checkClick(mouseX, mouseY, 7, 97, 18, 18)) {
                    click();
                    detailedSearch = !detailedSearch;
                    scrollIndex = 0;
                    indexDirty = true;
                }
                if (leftPos + 153 + 34 <= mouseX && leftPos + 153 + 34 + 14 > mouseX
                        && topPos + 16 < mouseY && topPos + 16 + 108 >= mouseY) {
                    draggingScroll = true;
                    dragScroll(mouseY);
                }
            }
        } else if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if ((menu.kind() == PneumaticStorageBlock.Kind.CLUTTER
                    || menu.kind() == PneumaticStorageBlock.Kind.MONO)
                    && isHovering(174, 36, 20, 8, mouseX, mouseY)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
            if (menu.kind() == PneumaticStorageBlock.Kind.EXPORTER) {
                if (isHovering(142, 16, 18, 18, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    return true;
                }
                if (isHovering(142, 34, 18, 18, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
                    return true;
                }
                if (isHovering(142, 52, 18, 18, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 3);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (access() && draggingScroll) {
            dragScroll(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void dragScroll(double mouseY) {
        int range = 92; // 106 scroll bar size, -7 pixels on top and bottom
        int sY = Mth.clamp((int) mouseY - topPos - 24, 0, 92);
        double scrollFrac = (double) sY / (double) range;
        setScroll((int) Math.round(scrollBounds * scrollFrac));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (access() && scrollY != 0 && checkClick(mouseX, mouseY, 0, 0, imageWidth, imageHeight)) {
            setScroll(getScroll() - (scrollY > 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (access() && search != null && search.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int getScroll() {
        return Mth.clamp(scrollIndex, 0, scrollBounds);
    }

    private void setScroll(int scroll) {
        int previous = getScroll();
        scrollIndex = Mth.clamp(scroll, 0, scrollBounds);
        if (previous != scrollIndex) indexDirty = true;
    }

    /** Grid clicks: ContainerPneumoStorageAccess.slotClick sends LEFT_CLICK / RIGHT_CLICK / SHIFT_CLICK. */
    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        if (access() && slot != null && slotId >= 0 && slotId < PneumaticStorageMenu.ACCESS_SLOTS) {
            int clickType = -1;
            if (type == ClickType.PICKUP && button == 0) clickType = 0;
            else if (type == ClickType.PICKUP && button == 1) clickType = 1;
            else if (type == ClickType.QUICK_MOVE && button == 0) clickType = 2;
            if (clickType >= 0) {
                PacketDistributor.sendToServer(new PneumaticAccessPayloads.Click(menu.containerId, clickType, slot.getItem().copy()));
            }
            return;
        }
        super.slotClicked(slot, slotId, button, type);
    }

    /** ContainerPneumoStorageAccess.rebuildClientIndex */
    private void rebuildClientIndexIfNeeded() {
        if (!indexDirty && builtVersion == menu.clientAccessEntriesVersion()) {
            return;
        }
        builtVersion = menu.clientAccessEntriesVersion();
        indexDirty = false;
        List<PneumaticAccessPayloads.Entry> slots = new ArrayList<>(menu.clientAccessEntries());
        slots.removeIf(entry -> entry.amount() <= 0);
        if (!searchString.isEmpty()) {
            slots.removeIf(entry -> !matchesSearch(entry.stack()));
        }
        listingSize = slots.size();
        slots.sort(sorter());
        scrollBounds = Math.max(1, (int) Math.ceil(listingSize / 8D - 6D));
        if (scrollIndex > scrollBounds) scrollIndex = scrollBounds;
        if (scrollIndex < 0) scrollIndex = 0;
        int offset = scrollIndex * 8;
        List<PneumaticAccessPayloads.Entry> visible = new ArrayList<>();
        for (int i = 0; i < PneumaticStorageMenu.ACCESS_SLOTS && offset + i < slots.size(); i++) {
            visible.add(slots.get(offset + i));
        }
        menu.setAccessGrid(visible);
    }

    private boolean matchesSearch(ItemStack stack) {
        boolean contains = stack.getHoverName().getString().toLowerCase(Locale.US).contains(searchString);
        if (contains || !detailedSearch || minecraft == null || minecraft.level == null) {
            return contains;
        }
        TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        List<Component> lines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(minecraft.level), lines, flag);
        for (Component line : lines) {
            if (line.getString().toLowerCase(Locale.US).contains(searchString)) return true;
        }
        return false;
    }

    private static Comparator<PneumaticAccessPayloads.Entry> sorter() {
        return switch (sorting) {
            case 1 -> SORT_BY_ID;
            case 2 -> SORT_BY_LOCALIZED;
            case 3 -> SORT_BY_INTERNAL;
            default -> SORT_BY_STACK_SIZE;
        };
    }

    private static int itemId(PneumaticAccessPayloads.Entry entry) {
        return BuiltInRegistries.ITEM.getId(entry.stack().getItem());
    }

    /** CacheSlotDummy.nbt == null first; flattened 1.21 items have no metadata. */
    private static int nbtOrder(PneumaticAccessPayloads.Entry entry) {
        return entry.stack().getComponentsPatch().isEmpty() ? 0 : 1;
    }

    private static final Comparator<PneumaticAccessPayloads.Entry> SORT_BY_ID = (o1, o2) -> {
        if (itemId(o1) != itemId(o2)) return itemId(o1) < itemId(o2) ? -1 : 1;
        if (o1.amount() != o2.amount()) return o1.amount() > o2.amount() ? -1 : 1;
        return Integer.compare(nbtOrder(o1), nbtOrder(o2));
    };

    private static final Comparator<PneumaticAccessPayloads.Entry> SORT_BY_STACK_SIZE = (o1, o2) -> {
        if (o1.amount() != o2.amount()) return o1.amount() > o2.amount() ? -1 : 1;
        if (itemId(o1) != itemId(o2)) return itemId(o1) < itemId(o2) ? -1 : 1;
        return Integer.compare(nbtOrder(o1), nbtOrder(o2));
    };

    private static final Comparator<PneumaticAccessPayloads.Entry> SORT_BY_INTERNAL = (o1, o2) -> {
        int compare = o1.stack().getDescriptionId().compareToIgnoreCase(o2.stack().getDescriptionId());
        return compare != 0 ? compare : SORT_BY_ID.compare(o1, o2);
    };

    private static final Comparator<PneumaticAccessPayloads.Entry> SORT_BY_LOCALIZED = (o1, o2) -> {
        int compare = o1.stack().getHoverName().getString().compareToIgnoreCase(o2.stack().getHoverName().getString());
        return compare != 0 ? compare : SORT_BY_ID.compare(o1, o2);
    };

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        switch (menu.kind()) {
            case ACCESS -> {
                graphics.blit(ACCESS, leftPos + 34, topPos, 0, 0, 176, imageHeight);
                graphics.blit(ACCESS, leftPos, topPos, 176, 15, 32, 122);
                // [parity V2: storage] sorting / toggle highlights and the scroll knob
                graphics.blit(ACCESS, leftPos + 7, topPos + 7 + sorting * 18, 208, 0, 18, 18);
                if (startFocussed) graphics.blit(ACCESS, leftPos + 7, topPos + 79, 208, 18, 18, 18);
                if (detailedSearch) graphics.blit(ACCESS, leftPos + 7, topPos + 97, 208, 18, 18, 18);
                double scrollProgress = Math.min(1.0D, (double) getScroll() / (double) scrollBounds);
                int scrollY = 17 + (int) (scrollProgress * (106 - 15));
                graphics.blit(ACCESS, leftPos + 34 + 154, topPos + scrollY, draggingScroll ? 188 : 176, 0, 12, 15);
            }
            case CLUTTER -> graphics.blit(CLUTTER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            case MONO -> graphics.blit(MONO, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            case IMPORTER -> graphics.blit(IMPORTER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            case EXPORTER -> graphics.blit(EXPORTER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }

        PneumaticStorageBlockEntity storage = menu.storage();
        if (storage == null) {
            return;
        }
        if (menu.kind() == PneumaticStorageBlock.Kind.MONO) {
            for (int slot = 0; slot < PneumaticStorageBlockEntity.MONO_SLOTS; slot++) {
                if (!storage.getItem(slot).isEmpty()) { // [parity V2: storage] bar whenever the type is set
                    int width = storage.monoAmount(slot) * 124 / PneumaticStorageBlockEntity.MONO_CAPACITY;
                    graphics.blit(MONO, leftPos + 44, topPos + 17 + slot * 18, 0, 181, width, 16);
                }
            }
        }
        if (menu.kind() == PneumaticStorageBlock.Kind.CLUTTER || menu.kind() == PneumaticStorageBlock.Kind.MONO) {
            int pressure = storage.pressure();
            ResourceLocation texture = menu.kind() == PneumaticStorageBlock.Kind.CLUTTER ? CLUTTER : MONO;
            graphics.blit(texture, leftPos + 174 + 4 * (pressure - 1), topPos + 36, 200, 0, 4, 8);
            // [parity V2: storage] GUIElements.drawSmoothGauge(guiLeft + 184, guiTop + 25, z, fill / max, 5, 2, 1, 0xCA6C43, 0xAB4223)
            K3LegacyGauge.draw(graphics, leftPos + 184, topPos + 25,
                    (double) storage.airTank().amount() / (double) PneumaticStorageBlockEntity.AIR_CAPACITY, 5, 2, 1, 0xCA6C43, 0xAB4223);
        }
        if (menu.kind() == PneumaticStorageBlock.Kind.EXPORTER) {
            if (!storage.continuousRequest()) {
                graphics.blit(EXPORTER, leftPos + 142, topPos + 16, 176, 0, 18, 18);
            }
            if (storage.requestMode() == PneumaticStorageBlockEntity.REQUEST_FULL_STACK) {
                graphics.blit(EXPORTER, leftPos + 142, topPos + 34, 194, 0, 18, 18);
            } else if (storage.requestMode() == PneumaticStorageBlockEntity.REQUEST_FULL_REQUEST) {
                graphics.blit(EXPORTER, leftPos + 142, topPos + 34, 194, 18, 18, 18);
            }
            if (storage.rttyConfiguredMode()) {
                graphics.blit(EXPORTER, leftPos + 142, topPos + 52, 176, 18, 18, 18);
                // The authored 1.7.10 sheet greys the manual filter area while
                // the independent RTTY filter bank is active.
                graphics.blit(EXPORTER, leftPos + 14, topPos + 14, 77, 14, 58, 58);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (access()) {
            // [parity V2: storage] BobMathUtil.getShortNumber at half scale, right-aligned to the slot corner, over the item
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 300.0F);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            for (int slot = 0; slot < PneumaticStorageMenu.ACCESS_SLOTS; slot++) {
                Slot gridSlot = menu.slots.get(slot);
                if (gridSlot.hasItem()) {
                    String label = K3GuiText.shortNumber(menu.accessAmount(slot));
                    int x = (int) ((gridSlot.x + 16) / 0.5D) - font.width(label);
                    int y = (int) ((gridSlot.y + 16) / 0.5D) - font.lineHeight;
                    graphics.drawString(font, label, x, y, -1, true);
                }
            }
            graphics.pose().popPose();
        } else if (menu.kind() == PneumaticStorageBlock.Kind.MONO && menu.storage() != null) {
            for (int slot = 0; slot < PneumaticStorageBlockEntity.MONO_SLOTS; slot++) {
                if (!menu.storage().getItem(slot).isEmpty()) {
                    int amount = menu.storage().monoAmount(slot);
                    // [parity V2: storage] GUIPneumoStorageMono: truncated tenths printed with Double.toString
                    String percent = " (" + (((int) (amount * 1000D / (double) PneumaticStorageBlockEntity.MONO_CAPACITY)) / 10D) + "%)";
                    graphics.drawString(font, String.format(Locale.US, "%,d", amount) + percent, 50, 22 + slot * 18, 0x000000, false);
                }
            }
        }
    }

    private static ResourceLocation texture(String suffix) {
        return AtomicLegacy.id("textures/gui/storage/gui_pneumatic_" + suffix + ".png");
    }

    /** GUIPneumoStorageAccess.renderToolTip: GUIElements tooltips whose border colours carry no alpha (invisible). */
    @net.neoforged.fml.common.EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class AccessTooltipColors {
        private AccessTooltipColors() {
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void onTooltipColor(net.neoforged.neoforge.client.event.RenderTooltipEvent.Color event) {
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof PneumaticStorageScreen screen && screen.access()
                    && !event.getItemStack().isEmpty()) {
                event.setBorderStart(0x00D57C4F);
                event.setBorderEnd(0x00AB4223);
            }
        }
    }
}
