package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * [parity e3] 1.7.10 addInformation helpers and the static tooltips of items and blocks whose port class has no
 * tooltip code of its own. Texts are the hard-coded literals (or lang keys) of the pinned 1.7.10 sources; a line
 * without an EnumChatFormatting prefix was drawn in the default gray of the 1.7.10 tooltip.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class LegacyTooltips {
    private static final Map<String, Consumer<List<Component>>> STATIC = new HashMap<>();

    private LegacyTooltips() {
    }

    /** list.add("text") */
    public static void line(List<Component> tooltip, String text) {
        tooltip.add(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    /** list.add(EnumChatFormatting.X + "text") */
    public static void line(List<Component> tooltip, ChatFormatting color, String text) {
        tooltip.add(Component.literal(text).withStyle(color));
    }

    /** list.add(A + "label" + B + "value") */
    public static void pair(List<Component> tooltip, ChatFormatting labelColor, String label, ChatFormatting valueColor, String value) {
        MutableComponent line = Component.literal(label).withStyle(labelColor);
        tooltip.add(line.append(Component.literal(value).withStyle(valueColor)));
    }

    /** I18nUtil.resolveKey(key, args) */
    public static String resolve(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    /** for(String s : I18nUtil.resolveKey(key).split("\\$")) list.add(s) */
    public static void keyLines(List<Component> tooltip, String key) {
        for (String part : resolve(key).split("\\$")) {
            line(tooltip, part);
        }
    }

    /** ItemCustomLore: the optional "$"-separated description key. */
    public static void desc(List<Component> tooltip, String key) {
        String text = resolve(key);
        if (!text.equals(key)) {
            for (String part : text.split("\\$")) {
                line(tooltip, part);
            }
        }
    }

    /** "Hold <LSHIFT> to display more info" of ITooltipProvider / ItemConveyorWand (dark gray italic, key in yellow). */
    public static void holdShift(List<Component> tooltip) {
        tooltip.add(Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                .append(Component.literal("> to display more info").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
    }

    /** ConnectorRedWire / Pylon* / Substation / BasePipeAnchor.addInformation */
    public static void connection(List<Component> tooltip, String type, String range) {
        pair(tooltip, ChatFormatting.GOLD, "Connection Type: ", ChatFormatting.YELLOW, type);
        pair(tooltip, ChatFormatting.GOLD, "Connection Range: ", ChatFormatting.YELLOW, range);
    }

    /** The static 1.7.10 tooltip of {@code path}, or an empty list (also used by ParityE3TooltipDiagnostics). */
    public static List<Component> staticLines(String path) {
        List<Component> lines = new ArrayList<>();
        Consumer<List<Component>> source = STATIC.get(path);
        if (source != null) {
            source.accept(lines);
        }
        return lines;
    }

    public static boolean hasStaticLines(String path) {
        return STATIC.containsKey(path);
    }

    public static java.util.Set<String> staticIds() {
        return java.util.Collections.unmodifiableSet(STATIC.keySet());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!AtomicLegacy.MOD_ID.equals(id.getNamespace())) {
            return;
        }
        Consumer<List<Component>> source = STATIC.get(id.getPath());
        if (source == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        source.accept(lines);
        List<Component> tooltip = event.getToolTip();
        // addInformation lines directly follow the item name
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    /**
     * 1.7.10 drew every tooltip line after the item name in gray unless the line carried its own colour code; an
     * unstyled 1.21.1 component would be white.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltipDefaultColor(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !AtomicLegacy.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        for (int i = 1; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (line.getStyle().getColor() == null) {
                tooltip.set(i, line.copy().withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** list.add(EnumChatFormatting.RED + "[" + I18nUtil.resolveKey("trait.drop") + "]") */
    public static void dropTrait(List<Component> tooltip) {
        line(tooltip, ChatFormatting.RED, "[" + resolve("trait.atomiclegacy.drop") + "]");
    }

    private static void put(Consumer<List<Component>> source, String... ids) {
        for (String id : ids) {
            STATIC.put(id, source);
        }
    }

    static {
        // ItemDrop (the port registers both cells as fluid containers)
        put(t -> {
            line(t, "Warning: Exposure to matter will");
            line(t, "lead to violent annihilation!");
            dropTrait(t);
        }, "cell_antimatter");
        put(t -> {
            line(t, "Warning: Exposure to matter will");
            line(t, "create a fólkvangr field!");
            dropTrait(t);
        }, "cell_anti_schrabidium");
        // ItemExpensive
        put(t -> line(t, ChatFormatting.RED, "Expensive mode item"), "item_expensive");
        // ItemRag: I18nUtil.resolveKeyArray("item.rag.desc")
        put(t -> keyLines(t, "item.atomiclegacy.rag.desc"), "rag");
        // BlockPipe
        put(t -> line(t, "Purely decorative"),
                "deco_pipe", "deco_pipe_rusted", "deco_pipe_green", "deco_pipe_green_rusted", "deco_pipe_red",
                "deco_pipe_marked", "deco_pipe_rim", "deco_pipe_rim_rusted", "deco_pipe_rim_green",
                "deco_pipe_rim_green_rusted", "deco_pipe_rim_red", "deco_pipe_rim_marked", "deco_pipe_framed",
                "deco_pipe_framed_rusted", "deco_pipe_framed_green", "deco_pipe_framed_green_rusted",
                "deco_pipe_framed_red", "deco_pipe_framed_marked", "deco_pipe_quad", "deco_pipe_quad_rusted",
                "deco_pipe_quad_green", "deco_pipe_quad_green_rusted", "deco_pipe_quad_red", "deco_pipe_quad_marked");
        // CableDiode
        put(t -> line(t, ChatFormatting.GOLD, "Limits throughput and restricts flow direction"), "cable_diode");
        // ItemBlockLore
        put(t -> line(t, "Static fluid barrel"), "red_barrel", "pink_barrel", "lox_barrel", "taint_barrel");
        put(t -> {
            line(t, "There is some kind of joke here,");
            line(t, "but I can't quite tell what it is.");
            line(t, "");
            line(t, "Update, 2020-07-04:");
            line(t, "We deny any implications of a joke on");
            line(t, "the basis that it was so severely unfunny");
            line(t, "that people started stabbing their eyes out.");
            line(t, "");
            line(t, "Update, 2020-17-04:");
            line(t, "As it turns out, \"Diamond Gravel\" was");
            line(t, "never really a thing, rendering what might");
            line(t, "have been a joke as totally nonsensical.");
            line(t, "We apologize for getting your hopes up with");
            line(t, "this non-joke that hasn't been made.");
            line(t, "");
            line(t, "i added an item for a joke that isn't even here, what am i, stupid? can't even tell the difference between gravel and a gavel, how did i not forget how to breathe yet?");
        }, "gravel_diamond");
        // ItemOreBlock
        put(t -> {
            line(t, "Australium ore");
            line(t, "Deposit location: X:-400; Z:-400");
            line(t, "Estimated quantity: 490");
        }, "ore_australium");
        // ItemPrototypeBlock
        put(t -> {
            line(t, "It didn't have to be like this.");
            line(t, "");
            line(t, "You monster.");
        }, "nuke_prototype");
        // ConnectorRedWire, ConnectorRedWireSuper, PylonRedWire, PylonMedium, PylonLarge, Substation
        put(t -> connection(t, "Single", "10m"), "red_connector");
        put(t -> connection(t, "Single", "100m"), "red_connector_super");
        put(t -> connection(t, "Single", "25m"), "red_pylon", "red_pylon_steel");
        put(t -> connection(t, "Triple", "45m"), "red_pylon_medium_wood", "red_pylon_medium_wood_transformer",
                "red_pylon_medium_steel", "red_pylon_medium_steel_transformer");
        put(t -> {
            connection(t, "Quadruple", "100m");
            line(t, ChatFormatting.GOLD, "This pylon requires a substation!");
        }, "red_pylon_large");
        put(t -> connection(t, "Quadruple", "20m"), "substation");
    }
}
