package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.BoxcarBlock;
import com.atomiclegacy.blocks.LegacyCrateLogic;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.DecorationEmitterBlockEntity;
import com.atomiclegacy.tileentity.LanternBlockEntity;
import com.atomiclegacy.tileentity.SafeBlockEntity;
import com.atomiclegacy.tileentity.StorageCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * [parity R4] Dedicated-server checks for the decorative tile entities and storage crates against the pinned
 * 1.7.10 sources (ModBlocks, BlockStorageCrate, ItemBlockStorageCrate, TileEntityCrateBase, ContainerUpgradeCraftingHandler,
 * BlockCrate/BlockCanCrate/BlockAmmoCrate/BlockSupplyCrate, BlockLoot, BlockEmitter, Spikes, TileEntityLantern, DecoBlock).
 * All cases run in the spawn chunks at y = 270 (item drops and the glyphid need loaded entity sections).
 * Not covered: GUIs, rendering, sounds, particles.
 */
final class ParityR4Diagnostics {
    private static int site;

    private ParityR4Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        BlockPos spawn = level.getSharedSpawnPos();
        origin = new BlockPos(spawn.getX(), 270, spawn.getZ());
        try {
            properties(problems, cases);
            crateItemRoundTrip(level, problems, cases);
            crateBreaking(level, problems, cases);
            safeAndUpgrade(server, level, problems, cases);
            lootCrates(level, problems, cases);
            emitter(level, problems, cases);
            misc(server, level, problems, cases);
            lantern(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity R4 diagnostics crashed", e);
            problems.add("R4 diagnostics crashed: " + e);
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) {
            problems.add("R4: " + what);
        }
    }

    private static BlockPos origin = BlockPos.ZERO;

    /** Item drops need loaded entity sections, so the pads sit in the spawn chunks at y = 270. */
    private static BlockPos pad() {
        int index = site++;
        return origin.offset((index % 6) * 3 - 8, 0, (index / 6) * 3 - 8);
    }

    private static ItemStack stack(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static List<ItemEntity> drops(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D));
    }

    private static void clearDrops(ServerLevel level, BlockPos pos) {
        drops(level, pos).forEach(Entity::discard);
    }

    private static FakePlayer player(ServerLevel level, BlockPos pos, ItemStack held) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() - 1.5D, 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.getInventory().items.subList(1, 36).replaceAll(s -> ItemStack.EMPTY);
        return player;
    }

    /// ModBlocks:2181-2185 setHardness / setResistance (1.7.10 explosion resistance = setResistance * 3 / 5) ///
    private static void properties(List<String> problems, int[] cases) {
        Object[][] rows = {
                {HbmBlocks.CRATE_IRON.get(), 5F, 6F}, {HbmBlocks.CRATE_STEEL.get(), 5F, 6F},
                {HbmBlocks.CRATE_DESH.get(), 5F, 6F}, {HbmBlocks.CRATE_TUNGSTEN.get(), 7.5F, 180F},
                {HbmBlocks.SAFE.get(), 7.5F, 6000F},
        };
        for (Object[] row : rows) {
            Block block = (Block) row[0];
            String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
            check(block.defaultDestroyTime() == (float) row[1], problems, cases, id + " hardness " + block.defaultDestroyTime());
            check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, id + " resistance " + block.getExplosionResistance()); // [parity T3: expected = original r*3/5]
            check(block.asItem().getDefaultMaxStackSize() == 64, problems, cases, id + " item stacks to 64 when empty");
        }
        StorageCrateBlockEntity.Kind[] kinds = StorageCrateBlockEntity.Kind.values();
        int[][] layout = {{36, 0x404040}, {54, 0x404040}, {104, 0x404040}, {27, 0x404040}, {27, 0xFFFFFF}, {15, 0x404040}};
        for (int i = 0; i < kinds.length; i++) {
            check(kinds[i].slots() == layout[i][0] && kinds[i].titleColor(true) == layout[i][1], problems, cases,
                    "crate kind " + kinds[i] + " slots/title colour");
        }
    }

    /// BlockStorageCrate#removedByPlayer + onBlockPlacedBy + addInformation ///
    private static void crateItemRoundTrip(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad();
        level.setBlock(pos, HbmBlocks.CRATE_STEEL.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof StorageCrateBlockEntity crate)) {
            check(false, problems, cases, "crate_steel block entity");
            return;
        }
        crate.setItem(0, new ItemStack(Items.DIAMOND, 5));
        crate.setItem(53, new ItemStack(Items.STICK, 1));
        crate.setPins(1234);
        crate.setLockMod(0.02D);
        crate.lock();
        crate.setCustomName("Box");
        check(!crate.canPlaceItemThroughFace(1, new ItemStack(Items.STONE), Direction.UP), problems, cases, "locked crate refuses insertion");
        check(AbstractContainerMenu.getRedstoneSignalFromContainer(crate) == LegacyCrateLogic.comparator(level, pos)
                && LegacyCrateLogic.comparator(level, pos) > 0, problems, cases, "crate comparator output");
        ItemStack drop = LegacyCrateLogic.dropStack(HbmBlocks.CRATE_STEEL.get(), crate, level);
        var data = LegacyCrateLogic.data(drop);
        check(data.contains("slot0") && data.contains("slot53") && data.getInt("lock") == 1234 && data.getDouble("lockMod") == 0.02D
                        && data.contains("stacklock") && "Box".equals(drop.getHoverName().getString()),
                problems, cases, "crate drop carries slots, lock, stacklock and name");
        List<Component> tooltip = new ArrayList<>();
        LegacyCrateLogic.appendTooltip(drop, Item.TooltipContext.of(level), tooltip);
        check(tooltip.size() == 2 && tooltip.get(0).getString().equals("This container is locked.")
                && tooltip.get(1).getString().equals("It feels heavy..."), problems, cases, "locked crate tooltip " + tooltip);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        clearDrops(level, pos);
        level.setBlock(pos, HbmBlocks.CRATE_STEEL.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof StorageCrateBlockEntity placed) {
            placed.loadFromItem(drop, level.registryAccess());
            check(placed.isLocked() && placed.pins() == 1234 && placed.lockMod() == 0.02D && placed.getItem(0).getCount() == 5
                    && placed.getItem(53).is(Items.STICK) && "Box".equals(placed.customName()), problems, cases, "crate placed from item");
            placed.unlock();
            ItemStack open = LegacyCrateLogic.dropStack(HbmBlocks.CRATE_STEEL.get(), placed, level);
            tooltip.clear();
            LegacyCrateLogic.appendTooltip(open, Item.TooltipContext.of(level), tooltip);
            check(tooltip.size() == 3 && tooltip.get(0).getString().equals("Contains:")
                    && tooltip.get(1).getString().startsWith(" - ") && tooltip.get(1).getString().endsWith(" x5"),
                    problems, cases, "crate contents tooltip " + tooltip);
            check(!LegacyCrateLogic.data(open).contains("lock"), problems, cases, "unlocked crate stores no lock");
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        clearDrops(level, pos);
    }

    /// removedByPlayer (survival with pickaxe / creative) and breakBlock spill (explosion etc.) ///
    private static void crateBreaking(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad();
        BlockState state = HbmBlocks.CRATE_IRON.get().defaultBlockState();
        level.setBlock(pos, state, 3);
        if (level.getBlockEntity(pos) instanceof StorageCrateBlockEntity crate) {
            crate.setItem(3, new ItemStack(Items.COBBLESTONE, 64));
        }
        FakePlayer player = player(level, pos, new ItemStack(Items.IRON_PICKAXE));
        state.getBlock().playerWillDestroy(level, pos, state, player);
        var be = level.getBlockEntity(pos);
        level.removeBlock(pos, false);
        check(drops(level, pos).isEmpty(), problems, cases, "harvested crate does not spill");
        state.getBlock().playerDestroy(level, player, pos, state, be, player.getMainHandItem());
        List<ItemEntity> harvested = drops(level, pos);
        check(harvested.size() == 1 && harvested.get(0).getItem().is(HbmBlocks.CRATE_IRON.get().asItem())
                        && LegacyCrateLogic.data(harvested.get(0).getItem()).contains("slot3"),
                problems, cases, "harvested crate drops itself with contents");
        clearDrops(level, pos);

        level.setBlock(pos, state, 3);
        if (level.getBlockEntity(pos) instanceof StorageCrateBlockEntity crate) {
            crate.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        int spilled = drops(level, pos).stream().mapToInt(e -> e.getItem().getCount()).sum();
        check(spilled == 64 && drops(level, pos).stream().noneMatch(e -> e.getItem().is(HbmBlocks.CRATE_IRON.get().asItem()))
                && drops(level, pos).stream().allMatch(e -> e.getItem().getCount() <= 30), problems, cases,
                "non-player removal spills contents in 10..30 stacks, no crate item");
        check(state.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)).isEmpty(), problems, cases, "crate getItemDropped is null");
        clearDrops(level, pos);

        level.setBlock(pos, state, 3);
        if (level.getBlockEntity(pos) instanceof StorageCrateBlockEntity crate) {
            crate.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        }
        FakePlayer creative = player(level, pos, ItemStack.EMPTY);
        creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        state.getBlock().playerWillDestroy(level, pos, state, creative);
        level.removeBlock(pos, false);
        check(drops(level, pos).isEmpty(), problems, cases, "creative break drops nothing (dropInv = false)");
        creative.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        clearDrops(level, pos);
    }

    /// TileEntitySafe (15 slots) + ContainerUpgradeCraftingHandler (CraftingManager:273-276) ///
    private static void safeAndUpgrade(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad();
        level.setBlock(pos, HbmBlocks.SAFE.get().defaultBlockState(), 3);
        check(level.getBlockEntity(pos) instanceof SafeBlockEntity safe && safe.getContainerSize() == 15
                && safe.kind() == StorageCrateBlockEntity.Kind.SAFE, problems, cases, "safe has 15 slots");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        clearDrops(level, pos);

        ItemStack crate = new ItemStack(HbmBlocks.CRATE_STEEL.get());
        var data = new net.minecraft.nbt.CompoundTag();
        data.put("slot2", new ItemStack(Items.APPLE, 3).save(level.registryAccess()));
        LegacyCrateLogic.setData(crate, data);
        crate.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
        ItemStack lead = stack("plate_lead", 1);
        ItemStack titanium = stack("plate_titanium", 1);
        CraftingInput input = CraftingInput.of(3, 3, List.of(lead, titanium, lead, titanium, crate, titanium, lead, titanium, lead));
        var result = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(h -> h.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
        check(result.is(HbmBlocks.SAFE.get().asItem()) && LegacyCrateLogic.data(result).contains("slot2")
                && "Named".equals(result.getHoverName().getString()), problems, cases, "safe upgrade keeps crate tag and name");
    }

    /// BlockCrate#setDrops/dropItems, BlockAmmoCrate#getContents, BlockSupplyCrate#removedByPlayer ///
    private static void lootCrates(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad();
        FakePlayer player = player(level, pos, new ItemStack(HbmItems.CROWBAR.get()));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        level.setBlock(pos, HbmBlocks.CRATE_RED.get().defaultBlockState(), 3);
        level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        List<ItemEntity> red = drops(level, pos);
        long secrets = red.stream().filter(e -> e.getItem().is(HbmItems.AMMO_SECRET.get())).count();
        check(level.getBlockState(pos).isAir() && red.size() == 14 && secrets == 3, problems, cases,
                "red crate drops all 14 redList entries (" + red.size() + ", secrets " + secrets + ") "
                        + red.stream().map(e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath()).toList());
        clearDrops(level, pos);

        Set<String> guns = Set.of("gun_light_revolver", "gun_maresleg", "gun_heavy_revolver", "gun_greasegun", "gun_liberator",
                "gun_flaregun", "gun_panzerschreck", "gun_fliegerfaust");
        level.setBlock(pos, HbmBlocks.CRATE_WEAPON.get().defaultBlockState(), 3);
        level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        List<ItemEntity> weapons = drops(level, pos);
        check(!weapons.isEmpty() && weapons.stream().allMatch(e -> guns.contains(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath())),
                problems, cases, "weapon crate drops only the weaponList guns");
        clearDrops(level, pos);

        level.setBlock(pos, HbmBlocks.CRATE_AMMO.get().defaultBlockState(), 3);
        level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        List<ItemEntity> ammo = drops(level, pos);
        check(ammo.stream().anyMatch(e -> e.getItem().is(stack("cap_nuka", 1).getItem()) && e.getItem().getCount() >= 12 && e.getItem().getCount() <= 32)
                && ammo.stream().anyMatch(e -> e.getItem().is(stack("syringe_metal_stimpak", 1).getItem())), problems, cases,
                "ammo crate always drops 12..32 caps and stimpaks");
        clearDrops(level, pos);

        level.setBlock(pos, HbmBlocks.CRATE_SUPPLY.get().defaultBlockState(), 3);
        List<ItemStack> explosion = level.getBlockState(pos).getDrops(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos)));
        check(explosion.size() == 1 && explosion.get(0).is(HbmBlocks.CRATE_SUPPLY.get().asItem()), problems, cases,
                "supply crate drops itself without a player");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        clearDrops(level, pos);
    }

    /// BlockEmitter: ItemDye.field_150922_c colour, stackSize-- ///
    private static void emitter(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad();
        level.setBlock(pos, HbmBlocks.DECO_EMITTER.get().defaultBlockState(), 3);
        FakePlayer player = player(level, pos, new ItemStack(Items.RED_DYE, 2));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        check(level.getBlockEntity(pos) instanceof DecorationEmitterBlockEntity e && e.color() == 11743532
                && player.getMainHandItem().getCount() == 1, problems, cases, "emitter red dye colour 11743532 and dye consumed");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void misc(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
        var damageTypes = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        check(damageTypes.getHolder(HbmDamageTypes.SPIKES).map(h -> h.is(DamageTypeTags.BYPASSES_ARMOR)).orElse(false),
                problems, cases, "spikes damage type bypasses armour");
        check(HbmBlocks.BOXCAR.get().defaultBlockState().getValue(BoxcarBlock.LEGACY_META0), problems, cases,
                "boxcar placed by EntityBoxcar keeps metadata 0 rendering");
        BlockPos pos = pad();
        level.setBlock(pos, HbmBlocks.DECO_LOOT.get().defaultBlockState(), 3);
        check(level.getBlockState(pos).getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)).isEmpty(), problems, cases, "deco_loot drops nothing itself");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        check(net.minecraft.world.item.DyeColor.BLACK.getFireworkColor() == 1973019, problems, cases, "dye colour table");
        ResourceLocation safeTab = AtomicLegacy.id("safe");
        check(BuiltInRegistries.ITEM.get(safeTab) instanceof com.atomiclegacy.items.StorageCrateBlockItem, problems, cases,
                "safe uses ItemBlockStorageCrate");
    }

    /// TileEntityLantern: blindness 100 ticks on glyphids within 7.5 of the lamp head ///
    private static void lantern(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = origin.offset(8, 0, 8);
        var glyphid = HbmEntityTypes.GLYPHID.get().create(level);
        if (glyphid == null) {
            check(false, problems, cases, "glyphid entity");
            return;
        }
        glyphid.moveTo(pos.getX() + 0.5D, pos.getY() + 5.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
        glyphid.setNoAi(true);
        level.addFreshEntity(glyphid);
        LanternBlockEntity.blindGlyphids(level, pos);
        var effect = glyphid.getEffect(MobEffects.BLINDNESS);
        check(effect != null && effect.getDuration() == 100 && effect.getAmplifier() == 0, problems, cases, "lantern blinds glyphids");
        glyphid.discard();
    }
}
