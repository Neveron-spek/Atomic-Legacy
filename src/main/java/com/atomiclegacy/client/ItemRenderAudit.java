package com.atomiclegacy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.atomiclegacy.AtomicLegacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Opt-in (-Datomiclegacy.itemRenderAudit=true) client sweep: opens a flat world and draws every mod item stack
 * (default instance plus creative-tab variants) through the real item renderer in every display context, builds
 * its client tooltip and records exceptions and missing models. Report: item-render-audit.txt in the game directory.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class ItemRenderAudit {

    private static final boolean ENABLED = Boolean.getBoolean("atomiclegacy.itemRenderAudit");
    private static final int PER_FRAME = 40;
    private static boolean worldRequested;
    private static List<ItemStack> stacks;
    private static int index;
    private static int warmup;
    private static boolean done;
    private static final List<String> ERRORS = new ArrayList<>();
    private static final List<String> WARNINGS = new ArrayList<>();

    private ItemRenderAudit() {}

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if (!ENABLED || worldRequested || !(event.getScreen() instanceof TitleScreen)) return;
        worldRequested = true;
        Minecraft mc = Minecraft.getInstance();
        LevelSettings settings = new LevelSettings("itemrenderaudit", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("itemrenderaudit-" + System.currentTimeMillis(), settings, new WorldOptions(12345L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                event.getScreen());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ENABLED || done) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (warmup++ < 100) return;
        if (stacks == null) stacks = collect(mc);

        for (int n = 0; n < PER_FRAME && index < stacks.size(); n++, index++) {
            ItemStack stack = stacks.get(index);
            String label = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + (stack.getComponentsPatch().isEmpty() ? "" : "{" + stack.getDescriptionId() + "}");
            try {
                event.getGuiGraphics().renderItem(stack, 4, 4);
                event.getGuiGraphics().renderItemDecorations(mc.font, stack, 4, 4);
                event.getGuiGraphics().flush();
            } catch (Throwable t) {
                ERRORS.add(label + " [gui]: " + summarize(t));
            }
            for (ItemDisplayContext context : ItemDisplayContext.values()) {
                if (context == ItemDisplayContext.NONE || context == ItemDisplayContext.GUI) continue;
                MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
                try {
                    boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                    mc.getItemRenderer().renderStatic(mc.player, stack, context, left, new PoseStack(), buffers, mc.level,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                    buffers.endBatch();
                } catch (Throwable t) {
                    ERRORS.add(label + " [" + context + "]: " + summarize(t));
                    try { buffers.endBatch(); } catch (Throwable ignored) { }
                }
            }
            try {
                BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
                if (model == mc.getModelManager().getMissingModel()) {
                    WARNINGS.add(label + ": missing item model");
                } else if (!model.isCustomRenderer()
                        && model.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation())
                        && model.getQuads(null, null, net.minecraft.util.RandomSource.create(0)).isEmpty()) {
                    WARNINGS.add(label + ": model has no quads and no particle texture");
                }
            } catch (Throwable t) {
                ERRORS.add(label + " [model]: " + summarize(t));
            }
            try {
                stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.ADVANCED);
            } catch (Throwable t) {
                ERRORS.add(label + " [tooltip]: " + summarize(t));
            }
        }

        if (index >= stacks.size()) {
            done = true;
            int blockEntities = auditBlockEntities(mc);
            // model shape per item id, for comparing against the 1.7.10 list of custom item renderers
            List<String> shapes = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (!AtomicLegacy.MOD_ID.equals(id.getNamespace())) continue;
                try {
                    BakedModel model = mc.getItemRenderer().getModel(item.getDefaultInstance(), mc.level, mc.player, 0);
                    int quads = model.getQuads(null, null, net.minecraft.util.RandomSource.create(0)).size();
                    for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) quads += model.getQuads(null, d, net.minecraft.util.RandomSource.create(0)).size();
                    shapes.add(id.getPath() + "	" + model.isCustomRenderer() + "	" + model.isGui3d() + "	" + quads + "	" + (item instanceof net.minecraft.world.item.BlockItem));
                } catch (Throwable t) {
                    shapes.add(id.getPath() + "	error");
                }
            }
            try {
                Files.write(mc.gameDirectory.toPath().resolve("item-model-shapes.tsv"), shapes, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
            List<String> report = new ArrayList<>();
            report.add("stacks=" + stacks.size());
            report.add("block_entity_states=" + blockEntities);
            report.add("errors=" + ERRORS.size());
            report.add("warnings=" + WARNINGS.size());
            report.add("-- errors");
            report.addAll(ERRORS);
            report.add("-- warnings");
            report.addAll(WARNINGS);
            try {
                Files.write(mc.gameDirectory.toPath().resolve("item-render-audit.txt"), report, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
            AtomicLegacy.LOGGER.info("HBM_ITEM_RENDER_AUDIT_COMPLETE stacks={} errors={} warnings={}", stacks.size(), ERRORS.size(), WARNINGS.size());
            String sheets = System.getProperty("atomiclegacy.itemRenderSheets", "");
            if (!sheets.isBlank()) {
                try {
                    TabRenderSheets.render(mc, sheets);
                } catch (Throwable t) {
                    AtomicLegacy.LOGGER.error("item render sheets failed", t);
                }
            }
            mc.stop();
        }
    }

    /** Draws every block entity of the mod through its renderer for up to 24 states of its block (fresh, empty block entity). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int auditBlockEntities(Minecraft mc) {
        int count = 0;
        net.minecraft.core.BlockPos pos = mc.player.blockPosition().above(3);
        for (net.minecraft.world.level.block.Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!AtomicLegacy.MOD_ID.equals(id.getNamespace()) || !(block instanceof net.minecraft.world.level.block.EntityBlock entityBlock)) continue;
            int states = 0;
            for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (states++ >= 24) break;
                String label = "block:" + id.getPath() + (states == 1 ? "" : state.getValues().toString());
                MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
                try {
                    net.minecraft.world.level.block.entity.BlockEntity be = entityBlock.newBlockEntity(pos, state);
                    if (be == null) continue;
                    be.setLevel(mc.level);
                    net.minecraft.client.renderer.blockentity.BlockEntityRenderer renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
                    if (renderer == null) continue;
                    count++;
                    for (float partial : new float[] {0F, 0.5F}) {
                        renderer.render(be, partial, new PoseStack(), buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    }
                    renderer.getRenderBoundingBox(be);
                    buffers.endBatch();
                } catch (Throwable t) {
                    ERRORS.add(label + " [blockEntityRenderer]: " + summarize(t));
                    try { buffers.endBatch(); } catch (Throwable ignored) { }
                }
            }
        }
        return count;
    }

    private static List<ItemStack> collect(Minecraft mc) {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (AtomicLegacy.MOD_ID.equals(id.getNamespace())) out.add(item.getDefaultInstance());
        }
        try {
            CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                for (ItemStack stack : tab.getDisplayItems()) {
                    if (!AtomicLegacy.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) continue;
                    if (stack.getComponentsPatch().isEmpty()) continue;
                    boolean known = false;
                    for (ItemStack existing : out) {
                        if (ItemStack.isSameItemSameComponents(existing, stack)) { known = true; break; }
                    }
                    if (!known) out.add(stack.copy());
                }
            }
        } catch (Throwable t) {
            ERRORS.add("creative tab contents: " + summarize(t));
        }
        return out;
    }

    private static String summarize(Throwable t) {
        StringBuilder sb = new StringBuilder(t.getClass().getName());
        if (t.getMessage() != null) sb.append(": ").append(t.getMessage().replace('\n', ' '));
        int frames = 0;
        for (StackTraceElement element : t.getStackTrace()) {
            if (element.getClassName().startsWith("com.atomiclegacy") && !element.getClassName().endsWith("ItemRenderAudit")) {
                sb.append(" @ ").append(element.getClassName().substring(element.getClassName().lastIndexOf('.') + 1))
                        .append('.').append(element.getMethodName()).append(':').append(element.getLineNumber());
                if (++frames >= 3) break;
            }
        }
        return sb.toString();
    }
}
