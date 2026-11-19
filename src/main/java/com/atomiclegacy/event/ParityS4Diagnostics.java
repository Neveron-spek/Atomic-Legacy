package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyBulletEntity;
import com.atomiclegacy.entity.LegacyChemicalEntity;
import com.atomiclegacy.entity.SoliniumCloudEntity;
import com.atomiclegacy.items.LegacyAmmoContainerItem;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.LegacyContentVisibility;
import com.atomiclegacy.tileentity.HbmStructureLoot;
import com.atomiclegacy.tileentity.PartEmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/** [parity S4: diagnostics] taint / statue_elb_f items, part_emitter range, ore_copper, gun kits, ammo container, culling flags. */
final class ParityS4Diagnostics {
    private ParityS4Diagnostics() {
    }

    private static BlockPos origin = BlockPos.ZERO;
    private static int site;

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        origin = new BlockPos(spawn.getX(), 270, spawn.getZ());
        site = 0;
        try {
            culling(level, problems, cases);
            taint(level, problems, cases);
            statue(level, problems, cases);
            check(PartEmitterBlockEntity.RANGE == 150, problems, cases, "part_emitter TargetPoint range 150");
            oreCopper(server, level, problems, cases);
            gunKits(level, problems, cases);
            ammoContainer(level, problems, cases);
            loot(server, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity S4 diagnostics crashed", e);
            problems.add("S4 diagnostics crashed: " + e);
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("S4: " + what);
    }

    private static BlockPos pad() {
        int index = site++;
        return origin.offset((index % 6) * 4 - 10, 0, (index / 6) * 4 + 12);
    }

    private static FakePlayer player(ServerLevel level, BlockPos pos, ItemStack held) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() - 1.5D, 0.0F, 0.0F);
        player.getInventory().items.replaceAll(s -> ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static List<ItemEntity> drops(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D));
    }

    private static void clearDrops(ServerLevel level, BlockPos pos) {
        drops(level, pos).forEach(ItemEntity::discard);
    }

    /// ignoreFrustumCheck entities no longer force noCulling; the rainbow cloud keeps distance < 25000 ///
    private static void culling(ServerLevel level, List<String> problems, int[] cases) {
        SoliniumCloudEntity cloud = HbmEntityTypes.SOLINIUM_CLOUD.get().create(level);
        check(cloud != null && !cloud.noCulling, problems, cases, "solinium cloud uses the finite renderer envelope");
        if (cloud != null) {
            cloud.setRainbow(true);
            check(cloud.shouldRenderAtSqrDistance(24_999) && !cloud.shouldRenderAtSqrDistance(25_001), problems, cases,
                    "rainbow cloud isInRangeToRenderDist distance < 25000");
            cloud.setRainbow(false);
            check(cloud.shouldRenderAtSqrDistance(1_000_000), problems, cases, "solinium cloud keeps the vanilla size rule");
        }
        LegacyChemicalEntity chem = HbmEntityTypes.LEGACY_CHEMICAL.get().create(level);
        check(chem != null && !chem.noCulling, problems, cases, "chemical entity uses the finite renderer envelope");
        check(!new LegacyBulletEntity(HbmEntityTypes.LEGACY_BULLET.get(), level).ignoreFrustumCheck, problems, cases,
                "bullets are frustum-culled unless the hook lambda sets ignoreFrustumCheck");
    }

    /// BlockTaint: ItemBlockBase without tab, getItemDropped null (silk touch only), falling taint drops as item ///
    private static void taint(ServerLevel level, List<String> problems, int[] cases) {
        Item item = HbmBlocks.TAINT.get().asItem();
        check(item != Items.AIR, problems, cases, "taint has an ItemBlock");
        check(LegacyContentVisibility.isCreativeHidden(new ItemStack(item)), problems, cases, "taint item has no creative tab");
        BlockState state = HbmBlocks.TAINT.get().defaultBlockState();
        check(state.requiresCorrectToolForDrops() && state.is(BlockTags.MINEABLE_WITH_PICKAXE), problems, cases,
                "taint Material.iron needs a pickaxe");
        BlockPos pos = pad();
        level.setBlock(pos, state, 2 | 16);
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        check(state.getDrops(lootParams(level, pos, pick)).isEmpty(), problems, cases, "taint drops nothing without silk touch");
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> silkDrops = state.getDrops(lootParams(level, pos, silk));
        check(silkDrops.size() == 1 && silkDrops.getFirst().is(item) && silkDrops.getFirst().getCount() == 1, problems, cases,
                "taint drops its ItemBlock with silk touch");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);

        // a falling taint that lands in a torch cannot be placed and drops itself (EntityFallingBlock#onUpdate)
        BlockPos floor = pad();
        clearDrops(level, floor);
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(floor.above(), Blocks.TORCH.defaultBlockState(), 3);
        for (int i = 2; i <= 4; i++) level.setBlock(floor.above(i), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floor.above(3), state, 2 | 16);
        FallingBlockEntity falling = FallingBlockEntity.fall(level, floor.above(3), state);
        for (int t = 0; t < 100 && !falling.isRemoved(); t++) falling.tick();
        boolean dropped = drops(level, floor).stream().anyMatch(e -> e.getItem().is(item));
        check(falling.isRemoved() && dropped, problems, cases, "falling taint that cannot land drops the taint item");
        clearDrops(level, floor);
        level.setBlock(floor.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floor, Blocks.AIR.defaultBlockState(), 3);
    }

    private static LootParams.Builder lootParams(ServerLevel level, BlockPos pos, ItemStack tool) {
        return new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool);
    }

    /// DecoBlockAlt statue_elb_f: plain ItemBlock, hidden, red key breaks it with drops ///
    private static void statue(ServerLevel level, List<String> problems, int[] cases) {
        Item item = HbmBlocks.STATUE_ELB_F.get().asItem();
        check(item != Items.AIR, problems, cases, "statue_elb_f has an ItemBlock");
        ItemStack stack = new ItemStack(item);
        check(LegacyContentVisibility.isCreativeHidden(stack) && LegacyContentVisibility.isJeiHidden(stack), problems, cases,
                "statue_elb_f item: no creative tab, NEI-hidden");
        BlockPos pos = pad();
        clearDrops(level, pos);
        level.setBlock(pos, HbmBlocks.STATUE_ELB_F.get().defaultBlockState(), 3);
        Item key = BuiltInRegistries.ITEM.get(AtomicLegacy.id("key_red_cracked"));
        FakePlayer player = player(level, pos, new ItemStack(key, 2));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        boolean dropped = drops(level, pos).stream().anyMatch(e -> e.getItem().is(item) && e.getItem().getCount() == 1);
        check(level.getBlockState(pos).isAir() && dropped && player.getMainHandItem().getCount() == 1, problems, cases,
                "cracked red key breaks statue_elb_f, drops it and is consumed");
        clearDrops(level, pos);
        player.getInventory().items.replaceAll(s -> ItemStack.EMPTY);
    }

    /// ore_copper: BlockGeneric(rock) 5/10, blockTab, smelts to ingot_copper (2.5 XP), 12 veins of 6 at Y 5..49 ///
    private static void oreCopper(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
        Item item = HbmBlocks.ORE_COPPER.get().asItem();
        BlockState state = HbmBlocks.ORE_COPPER.get().defaultBlockState();
        check(item != Items.AIR && !LegacyContentVisibility.isCreativeHidden(new ItemStack(item)), problems, cases,
                "ore_copper item in the block tab");
        check(state.getDestroySpeed(level, BlockPos.ZERO) == 5.0F && ParityT3Diagnostics.resistanceMatches(HbmBlocks.ORE_COPPER.get())
                && state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(BlockTags.NEEDS_STONE_TOOL), problems, cases,
                "ore_copper hardness 5, setResistance 10, any pickaxe"); // [parity T3: expected = original r*3/5]
        check(new ItemStack(item).is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/copper"))),
                problems, cases, "ore_copper is oreCopper");
        BlockPos pos = pad();
        level.setBlock(pos, state, 2 | 16);
        List<ItemStack> self = state.getDrops(lootParams(level, pos, new ItemStack(Items.WOODEN_PICKAXE)));
        check(self.size() == 1 && self.getFirst().is(item), problems, cases, "ore_copper drops itself");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
        var smelt = server.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(item)), level);
        check(smelt.isPresent() && smelt.get().value().getResultItem(server.registryAccess()).is(Items.COPPER_INGOT)
                && smelt.get().value().getExperience() == 2.5F, problems, cases, "ore_copper smelts to ingot_copper with 2.5 XP");
        var placed = server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(ResourceKey.create(Registries.PLACED_FEATURE, AtomicLegacy.id("ore_copper")));
        String text = placed == null ? "" : PlacedFeature.DIRECT_CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, placed)
                .result().map(Object::toString).orElse("");
        check(text.contains("\"count\":12") && text.contains("\"absolute\":5") && text.contains("\"absolute\":49")
                && text.contains("\"size\":6") && text.contains("atomiclegacy:ore_copper"), problems, cases,
                "ore_copper placed feature: copperSpawn 12, size 6, Y 5..49");
        var modifiers = server.registryAccess().registryOrThrow(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        check(modifiers.containsKey(AtomicLegacy.id("add_ore_copper")), problems, cases, "ore_copper biome modifier registered");
    }

    /// ItemRepairKit: -25 % of max durability from every config's wear in the hotbar, one use, 10 / 100 uses ///
    private static void gunKits(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad();
        Item kit1 = BuiltInRegistries.ITEM.get(AtomicLegacy.id("gun_kit_1"));
        Item kit2 = BuiltInRegistries.ITEM.get(AtomicLegacy.id("gun_kit_2"));
        check(new ItemStack(kit1).getMaxDamage() == 10 && new ItemStack(kit2).getMaxDamage() == 100
                && new ItemStack(kit1).getMaxStackSize() == 1, problems, cases, "gun kits 10 / 100 uses, stack size 1");
        check(!LegacyContentVisibility.isCreativeHidden(new ItemStack(kit1)) && !LegacyContentVisibility.isCreativeHidden(new ItemStack(kit2)),
                problems, cases, "gun kits are in a creative tab");
        ItemStack gun = new ItemStack(HbmItems.GUN_PEPPERBOX.get());
        ItemGunBaseNT gunItem = (ItemGunBaseNT) gun.getItem();
        float max = gunItem.getConfig(gun, 0).getDurability(gun);
        ItemGunBaseNT.setWear(gun, 0, max * 0.6F);
        FakePlayer player = player(level, pos, new ItemStack(kit1));
        player.getInventory().items.set(4, gun);
        kit1.use(level, player, InteractionHand.MAIN_HAND);
        float wear = ItemGunBaseNT.getWear(player.getInventory().items.get(4), 0);
        check(Math.abs(wear - max * 0.35F) < 0.01F && player.getMainHandItem().getDamageValue() == 1, problems, cases,
                "gun oil removes 25 % of max durability and takes one use");
        ItemGunBaseNT.setWear(player.getInventory().items.get(4), 0, 0F);
        kit1.use(level, player, InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().getDamageValue() == 1, problems, cases, "kit is not used up when nothing is worn");
        ItemStack gun2 = player.getInventory().items.get(4);
        ItemGunBaseNT.setWear(gun2, 0, max);
        player.getInventory().items.set(4, ItemStack.EMPTY);
        player.getInventory().items.set(20, gun2);
        kit1.use(level, player, InteractionHand.MAIN_HAND);
        check(ItemGunBaseNT.getWear(gun2, 0) == max, problems, cases, "repair kits only reach the hotbar");
        player.getInventory().items.replaceAll(s -> ItemStack.EMPTY);
    }

    /// ItemAmmoContainer: default ammo for up to three guns; makeshift (meta 1) halves it and skips expensive ammo ///
    private static void ammoContainer(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad();
        Item container = HbmItems.AMMO_CONTAINER.get();
        ItemGunBaseNT pepperbox = (ItemGunBaseNT) HbmItems.GUN_MARESLEG.get(); // setDefaultAmmo(G12, 12)
        ItemStack ammo = pepperbox.getDefaultAmmo();
        FakePlayer player = player(level, pos, new ItemStack(container));
        player.getInventory().items.set(10, new ItemStack(pepperbox));
        container.use(level, player, InteractionHand.MAIN_HAND);
        int count = countOf(player, ammo);
        check(!ammo.isEmpty() && count == ammo.getCount() && player.getMainHandItem().isEmpty(), problems, cases,
                "ammo container gives the default ammo and is consumed");

        player.getInventory().items.replaceAll(s -> ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, LegacyAmmoContainerItem.makeshift(container));
        player.getInventory().items.set(10, new ItemStack(pepperbox));
        player.getInventory().items.set(11, new ItemStack(HbmItems.GUN_FATMAN.get()));
        container.use(level, player, InteractionHand.MAIN_HAND);
        ItemStack nuke = ((ItemGunBaseNT) HbmItems.GUN_FATMAN.get()).getDefaultAmmo();
        check(countOf(player, ammo) == (int) Math.ceil(ammo.getCount() / 2D) && countOf(player, nuke) == 0, problems, cases,
                "makeshift container halves the ammo (ceil) and skips expensive default ammo");

        player.getInventory().items.replaceAll(s -> ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(container));
        container.use(level, player, InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().getCount() == 1, problems, cases, "container is kept when no gun is carried");
        player.getInventory().items.replaceAll(s -> ItemStack.EMPTY);
        clearDrops(level, pos);
    }

    private static int countOf(FakePlayer player, ItemStack like) {
        int total = 0;
        for (ItemStack s : player.getInventory().items) if (ItemStack.isSameItemSameComponents(s, like)) total += s.getCount();
        return total;
    }

    private static void loot(MinecraftServer server, List<String> problems, int[] cases) {
        boolean missing = HbmStructureLoot.diagnosticEntryProblems().stream()
                .anyMatch(p -> p.contains("gun_kit_1") || p.contains("gun_kit_2") || p.contains("ammo_container"));
        check(!missing, problems, cases, "item pools resolve gun_kit_1, gun_kit_2 and ammo_container");
        for (String id : List.of("gun_kit_1", "gun_kit_2", "ammo_container_makeshift", "smelting_ore_copper", "shredder/ore_copper_to_powder_copper")) {
            check(server.getRecipeManager().byKey(AtomicLegacy.id(id)).isPresent(), problems, cases, "recipe " + id + " loaded");
        }
    }
}
