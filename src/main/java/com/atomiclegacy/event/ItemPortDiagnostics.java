package com.atomiclegacy.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.worldgen.structure.HbmLegacyNbtTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Opt-in ({@code -Datomiclegacy.itemAudit=true}) dedicated-server sweep over every item and block of the mod.
 * It does not compare behaviour with 1.7.10; it drives the code paths a player can reach with each item and
 * records every exception: stack creation and NBT round trip, name/translation, tooltip, inventory ticks
 * (selected and not), right click into the air and onto stone, a dropped item entity, block placement with
 * block-entity ticks and interaction, breaking, every block state's shapes/light, and the legacy metadata
 * converter for metadata 0 (what 1.7.10 setBlock without metadata produces). Report: item-audit.txt.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class ItemPortDiagnostics {

    private static final BlockPos PAD = new BlockPos(0, 200, 0);
    private static final int TICKS = 10;
    private static final long SLOW_NANOS = 2_000_000_000L;

    private static final List<String> ERRORS = new ArrayList<>();
    private static final List<String> WARNINGS = new ArrayList<>();
    private static final List<String> INFO = new ArrayList<>();
    private static final Map<String, Integer> PHASES = new TreeMap<>();

    private ItemPortDiagnostics() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean("atomiclegacy.itemAudit")) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        List<String> report = new ArrayList<>();
        try {
            run(server, level, report);
        } catch (Throwable t) {
            ERRORS.add("audit aborted: " + summarize(t));
        }
        report.add("errors=" + ERRORS.size());
        report.add("warnings=" + WARNINGS.size());
        report.add("info=" + INFO.size());
        report.add("phases=" + PHASES);
        report.add("-- errors");
        report.addAll(ERRORS);
        report.add("-- warnings");
        report.addAll(WARNINGS);
        report.add("-- info");
        report.addAll(INFO);
        try {
            Files.write(Path.of("item-audit.txt"), report, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        AtomicLegacy.LOGGER.info("HBM_ITEM_AUDIT_COMPLETE errors={} warnings={}", ERRORS.size(), WARNINGS.size());
        server.halt(false);
    }

    private static void run(MinecraftServer server, ServerLevel level, List<String> report) {
        JsonObject lang = loadLang();
        level.getChunk(PAD);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                level.setBlock(PAD.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(PAD.getX() + 0.5D, PAD.getY(), PAD.getZ() + 2.5D);
        player.setXRot(30.0F);
        player.setYRot(180.0F);

        Map<Item, List<ItemStack>> stacks = collectStacks(level);
        int items = 0;
        int tested = 0;
        for (Map.Entry<Item, List<ItemStack>> entry : stacks.entrySet()) {
            Item item = entry.getKey();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            items++;
            for (ItemStack stack : entry.getValue()) {
                tested++;
                String label = id.getPath() + (entry.getValue().size() > 1 ? "{" + variantLabel(stack) + "}" : "");
                auditStack(level, player, item, stack, label, lang);
                cleanup(level, player);
            }
        }
        report.add("items=" + items);
        report.add("stacks=" + tested);
        report.add("blocks=" + auditBlocks(level));
    }

    private static Map<Item, List<ItemStack>> collectStacks(ServerLevel level) {
        Map<Item, List<ItemStack>> out = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!AtomicLegacy.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            List<ItemStack> list = new ArrayList<>();
            list.add(item.getDefaultInstance());
            out.put(item, list);
        }
        try {
            CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                for (ItemStack stack : tab.getDisplayItems()) {
                    List<ItemStack> list = out.get(stack.getItem());
                    if (list == null) {
                        continue;
                    }
                    boolean known = false;
                    for (ItemStack existing : list) {
                        if (ItemStack.isSameItemSameComponents(existing, stack)) {
                            known = true;
                            break;
                        }
                    }
                    if (!known) {
                        list.add(stack.copy());
                    }
                }
            }
        } catch (Throwable t) {
            ERRORS.add("creative tab contents: " + summarize(t));
        }
        return out;
    }

    private static void auditStack(ServerLevel level, FakePlayer player, Item item, ItemStack stack, String label, JsonObject lang) {
        phase(label, "nbt", () -> {
            Tag tag = stack.save(level.registryAccess());
            ItemStack back = ItemStack.parse(level.registryAccess(), tag).orElseThrow(() -> new IllegalStateException("parse failed"));
            if (!ItemStack.isSameItemSameComponents(stack, back)) {
                throw new IllegalStateException("NBT round trip changed the stack: " + tag);
            }
        });
        phase(label, "name", () -> {
            String key = stack.getDescriptionId();
            String name = stack.getHoverName().getString();
            if (lang != null && !lang.has(key) && name.equals(key)) {
                WARNINGS.add(label + " [lang]: no en_us entry for " + key);
            }
            item.getMaxStackSize(stack);
            stack.getMaxDamage();
            stack.getRarity();
            item.getCraftingRemainingItem(stack);
            stack.getFoodProperties(player);
            stack.getUseAnimation();
            stack.getUseDuration(player);
        });
        phase(label, "tooltip", () -> {
            try {
                stack.getTooltipLines(Item.TooltipContext.of(level), player, TooltipFlag.ADVANCED);
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                // client-only code reached from the dedicated server; not a player-facing bug
                INFO.add(label + " [tooltip]: client-only reference " + e.getMessage());
            }
        });
        phase(label, "inventoryTick", () -> {
            ItemStack held = stack.copy();
            player.getInventory().clearContent();
            player.getInventory().setItem(0, held);
            player.getInventory().selected = 0;
            for (int i = 0; i < TICKS; i++) {
                item.inventoryTick(held, level, player, 0, true);
            }
            ItemStack idle = stack.copy();
            player.getInventory().setItem(5, idle);
            for (int i = 0; i < TICKS; i++) {
                item.inventoryTick(idle, level, player, 5, false);
            }
        });
        phase(label, "use", () -> {
            ItemStack held = stack.copy();
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var result = item.use(level, player, InteractionHand.MAIN_HAND);
            if (result.getResult().consumesAction() && player.isUsingItem()) {
                ItemStack using = player.getUseItem();
                if (!using.isEmpty()) {
                    using.getItem().finishUsingItem(using, level, player);
                }
                player.stopUsingItem();
            }
        });
        phase(label, "useOn", () -> {
            ItemStack held = stack.copy();
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            BlockPos target = PAD.below();
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target).add(0.0D, 0.5D, 0.0D), Direction.UP, target, false);
            InteractionResult result = item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (item instanceof BlockItem blockItem) {
                auditPlacedBlock(level, player, blockItem, stack, label, result);
            }
        });
        phase(label, "itemEntity", () -> {
            ItemEntity entity = new ItemEntity(level, PAD.getX() + 0.5D, PAD.getY() + 0.1D, PAD.getZ() + 0.5D, stack.copy());
            level.addFreshEntity(entity);
            for (int i = 0; i < TICKS && !entity.isRemoved(); i++) {
                entity.tick();
            }
            entity.discard();
        });
    }

    private static void auditPlacedBlock(ServerLevel level, FakePlayer player, BlockItem blockItem, ItemStack stack, String label, InteractionResult useOnResult) {
        Block block = blockItem.getBlock();
        BlockPos pos = PAD;
        if (!level.getBlockState(pos).is(block)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(PAD.below()).add(0.0D, 0.5D, 0.0D), Direction.UP, PAD.below(), false);
            ItemStack held = stack.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            InteractionResult placed = blockItem.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, held, hit));
            if (!level.getBlockState(pos).is(block)) {
                INFO.add(label + " [place]: not placed (" + useOnResult + "/" + placed + ")");
                return;
            }
        }
        BlockState state = level.getBlockState(pos);
        phase(label, "blockTick", () -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                tickBlockEntity(level, pos, be);
            }
            if (level.getBlockState(pos).isRandomlyTicking()) {
                level.getBlockState(pos).randomTick(level, pos, level.random);
            }
        });
        phase(label, "blockUse", () -> {
            if (!level.getBlockState(pos).is(block)) {
                return;
            }
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            level.getBlockState(pos).useWithoutItem(level, player, hit);
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
            ItemStack held = stack.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            level.getBlockState(pos).useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit);
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                tickBlockEntity(level, pos, be);
                be.saveWithFullMetadata(level.registryAccess());
            }
        });
        phase(label, "blockBreak", () -> {
            if (level.getBlockState(pos).is(block)) {
                level.getBlockState(pos).getDrops(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos)));
                level.destroyBlock(pos, true, player);
            }
        });
        // multi-block placements may have altered the pad: restore it
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 8; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos p = PAD.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        level.removeBlockEntity(p);
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos p = PAD.offset(x, -1, z);
                if (!level.getBlockState(p).is(Blocks.STONE)) {
                    level.removeBlockEntity(p);
                    level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void tickBlockEntity(ServerLevel level, BlockPos pos, BlockEntity be) {
        BlockState state = level.getBlockState(pos);
        BlockEntityTicker ticker = state.getTicker(level, be.getType());
        if (ticker == null) {
            return;
        }
        for (int i = 0; i < TICKS; i++) {
            if (be.isRemoved() || !level.getBlockState(pos).is(state.getBlock())) {
                return;
            }
            ticker.tick(level, pos, level.getBlockState(pos), be);
        }
    }

    private static int auditBlocks(ServerLevel level) {
        int blocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!AtomicLegacy.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            blocks++;
            String label = "block:" + id.getPath();
            phase(label, "states", () -> {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    state.getShape(level, PAD, CollisionContext.empty());
                    state.getCollisionShape(level, PAD, CollisionContext.empty());
                    state.getLightEmission(level, PAD);
                    state.getLightBlock(level, PAD);
                    state.isCollisionShapeFullBlock(level, PAD);
                    state.getPistonPushReaction();
                }
            });
            phase(label, "legacyMeta0", () -> HbmLegacyNbtTemplate.stateFromLegacyId(id.toString(), 0));
            for (int meta = 1; meta < 16; meta++) {
                try {
                    HbmLegacyNbtTemplate.stateFromLegacyId(id.toString(), meta);
                } catch (Throwable t) {
                    INFO.add(label + " [legacyMeta" + meta + "]: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }
        }
        return blocks;
    }

    private static void cleanup(ServerLevel level, FakePlayer player) {
        player.getInventory().clearContent();
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.setPos(PAD.getX() + 0.5D, PAD.getY(), PAD.getZ() + 2.5D);
        List<Entity> entities = level.getEntities(player, new AABB(PAD).inflate(256.0D, 256.0D, 256.0D));
        for (Entity entity : entities) {
            if (entity != player) {
                entity.discard();
            }
        }
    }

    private static void phase(String label, String phase, Runnable body) {
        PHASES.merge(phase, 1, Integer::sum);
        long start = System.nanoTime();
        try {
            body.run();
        } catch (Throwable t) {
            ERRORS.add(label + " [" + phase + "]: " + summarize(t));
        }
        long took = System.nanoTime() - start;
        if (took > SLOW_NANOS) {
            WARNINGS.add(label + " [" + phase + "]: slow, " + (took / 1_000_000L) + " ms");
        }
    }

    private static String summarize(Throwable t) {
        StringBuilder sb = new StringBuilder(t.getClass().getName());
        if (t.getMessage() != null) {
            sb.append(": ").append(t.getMessage().replace('\n', ' '));
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            sb.append(" <- ").append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null) {
                sb.append(": ").append(cause.getMessage().replace('\n', ' '));
            }
        }
        int frames = 0;
        for (StackTraceElement element : (cause != null ? cause : t).getStackTrace()) {
            if (element.getClassName().startsWith("com.atomiclegacy")) {
                sb.append(" @ ").append(element.getClassName().substring(element.getClassName().lastIndexOf('.') + 1))
                        .append('.').append(element.getMethodName()).append(':').append(element.getLineNumber());
                if (++frames >= 3) {
                    break;
                }
            }
        }
        return sb.toString();
    }

    private static String variantLabel(ItemStack stack) {
        String key = stack.getDescriptionId();
        int dot = key.lastIndexOf('.');
        String tail = dot >= 0 ? key.substring(dot + 1) : key;
        var components = stack.getComponentsPatch();
        return components.isEmpty() ? tail : tail + "+" + components.size() + "c";
    }

    private static JsonObject loadLang() {
        try (var input = ItemPortDiagnostics.class.getResourceAsStream("/assets/atomiclegacy/lang/en_us.json")) {
            if (input == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            WARNINGS.add("en_us.json unreadable: " + e);
            return null;
        }
    }
}
