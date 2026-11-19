package com.atomiclegacy.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.command.ParityS2RunningConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * [parity S2: commands] Client-side commands of 1.7.10: CommandReloadClient (/ntmclient, ClientCommandHandler),
 * SuicideThreadDump (/dumpthreadsandcrashgame) and CommandWikiRender (/ntmwikirender, a server command in 1.7.10
 * that only works in single player because it opens a client GUI; registered client-side here).
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class ParityS2ClientCommands {
    private ParityS2ClientCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static CommandSyntaxException error(String text) {
        return new SimpleCommandExceptionType(Component.literal(text)).create();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal("ntmclient")
                .executes(context -> clientConfig(context.getSource(), ""))
                .then(net.minecraft.commands.Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((context, builder) -> ParityS2RunningConfig.client().suggest(builder))
                        .executes(context -> clientConfig(context.getSource(), StringArgumentType.getString(context, "args")))));
        dispatcher.register(net.minecraft.commands.Commands.literal("dumpthreadsandcrashgame")
                .executes(context -> {
                    throw error("Requires argument \"dump\" or \"crash\"!");
                })
                .then(net.minecraft.commands.Commands.argument("mode", StringArgumentType.greedyString())
                        .executes(context -> dumpThreads(StringArgumentType.getString(context, "mode")))));
        dispatcher.register(net.minecraft.commands.Commands.literal("ntmwikirender")
                .executes(context -> {
                    throw error(net.minecraft.ChatFormatting.GREEN + "/ntmwikirender <type> " + net.minecraft.ChatFormatting.LIGHT_PURPLE
                            + "- Render screenshots of a selected item type (e.g ItemGunBaseNT). Intended for developers and wiki editors only.");
                })
                .then(net.minecraft.commands.Commands.argument("args", StringArgumentType.greedyString())
                        .executes(context -> wikiRender(StringArgumentType.getString(context, "args").split(" ")[0]))));
    }

    private static int clientConfig(CommandSourceStack source, String raw) throws CommandSyntaxException {
        return ParityS2RunningConfig.client().process(source, raw, message -> source.sendSuccess(() -> message, false));
    }

    /** SuicideThreadDump#processCommand. */
    private static int dumpThreads(String mode) throws CommandSyntaxException {
        String[] args = mode.split(" ");
        if (args.length != 1 || !(args[0].equals("dump") || args[0].equals("crash"))) {
            throw error("Requires argument \"dump\" or \"crash\"!");
        }
        for (ThreadInfo thread : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
            dumpThread(thread);
        }
        if (args[0].equals("crash")) {
            Runtime.getRuntime().halt(0); // FMLCommonHandler.exitJava(0, true): hard exit
        }
        return 1;
    }

    private static void dumpThread(ThreadInfo info) {
        org.slf4j.Logger log = AtomicLegacy.LOGGER;
        log.error("===========================================");
        log.error("Thread: " + info.getThreadName() + " PID: " + info.getThreadId());
        log.error("Suspended: " + info.isSuspended());
        log.error("Blocked: " + info.getBlockedTime() + "ms, " + info.getBlockedCount() + "x");
        log.error("Runs Native: " + info.isInNative());
        log.error("State: " + info.getThreadState().name());
        log.error("-------------------------------------------");
        if (info.getLockedMonitors().length != 0) {
            log.error("Following locks found:");
            for (MonitorInfo monitor : info.getLockedMonitors()) {
                log.error("- " + monitor.getLockedStackFrame());
            }
            log.error("-------------------------------------------");
        }
        log.error("Stacktrace:");
        for (StackTraceElement line : info.getStackTrace()) {
            log.error("- " + line);
        }
        log.error("===========================================");
    }

    /** CommandWikiRender#processCommand. */
    private static int wikiRender(String prefix) {
        AtomicLegacy.LOGGER.info("Taking a screenshot of " + prefix);
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(AtomicLegacy.MOD_ID)) continue;
            // ignoredItems: achievement_icon (flattened to achievement_icon_*), spawn eggs, mob spawner
            if (id.getPath().startsWith("achievement_icon") || item instanceof SpawnEggItem || item == Items.SPAWNER) continue;
            boolean classMatch = item.getClass().getSimpleName().equalsIgnoreCase(prefix)
                    || item instanceof BlockItem blockItem && blockItem.getBlock().getClass().getSimpleName().equalsIgnoreCase(prefix);
            if (!classMatch) continue;
            stacks.add(new ItemStack(item)); // flattened: one stack per item (no getSubItems metadata)
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.tell(() -> minecraft.setScreen(new WikiRenderScreen(stacks.toArray(new ItemStack[0]), prefix, "wiki-block-renders-256", 16)));
        return 1;
    }

    /** GUIScreenWikiRender: renders every stack once and saves the 16x16 slot area as PNG. */
    public static final class WikiRenderScreen extends Screen {
        private final ItemStack[] preview;
        private final String prefix;
        private final String saveLocation;
        private final int scale;
        private int index;

        public WikiRenderScreen(ItemStack[] stacks, String prefix, String directory, int scale) {
            super(Component.empty());
            this.preview = stacks;
            this.prefix = prefix;
            this.saveLocation = directory;
            this.scale = scale;
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFFFF00FF);
            if (index >= preview.length) {
                this.onClose();
                return;
            }
            ItemStack stack = preview[index];
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, scale);
            graphics.pose().translate(9.0D, (double) this.height / scale - 9.0D, -200.0D);
            graphics.renderItem(stack, -8, -8);
            graphics.renderItemDecorations(this.font, stack, -8, -8);
            graphics.pose().popPose();
            graphics.flush();

            int zoom = scale * (int) Math.round(this.minecraft.getWindow().getGuiScale());
            try {
                String slotName = stack.getHoverName().getString().replaceAll("§.", "").replaceAll("[^\\w ().-]+", "");
                if (!slotName.endsWith(".name")) {
                    saveScreenshot(new File(this.minecraft.gameDirectory, saveLocation), prefix + slotName + ".png",
                            zoom, zoom, zoom * 16, zoom * 16, 0xFFFF00FF);
                }
            } catch (Exception ignored) {
                // Just skip any failures caused by display name or rendering
            }
            index++;
        }

        /** GUIScreenWikiRender#saveScreenshot; (x, y) is measured from the bottom-left corner like glReadPixels. */
        private void saveScreenshot(File directory, String fileName, int x, int y, int width, int height, int transparentColor) {
            try (NativeImage full = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
                 NativeImage image = new NativeImage(width, height, false)) {
                directory.mkdir();
                int top = full.getHeight() - y - height;
                for (int iy = 0; iy < height; iy++) {
                    for (int ix = 0; ix < width; ix++) {
                        int abgr = full.getPixelRGBA(x + ix, top + iy);
                        image.setPixelRGBA(ix, iy, abgr == transparentColor ? 0 : abgr); // magenta is the same in ARGB and ABGR
                    }
                }
                image.writeToFile(new File(directory, fileName));
            } catch (Exception ex) {
                AtomicLegacy.LOGGER.warn("Failed to save NTM screenshot", ex);
            }
        }
    }
}
