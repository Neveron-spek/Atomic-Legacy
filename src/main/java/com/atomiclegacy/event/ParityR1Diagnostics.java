package com.atomiclegacy.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.ConcreteColoredBlock;
import com.atomiclegacy.blocks.TaintBlock;
import com.atomiclegacy.entity.LegacyCloudFxEntity;
import com.atomiclegacy.items.BlowtorchItem;
import com.atomiclegacy.items.ConcreteColoredBlockItem;
import com.atomiclegacy.items.FilingCabinetBlockItem;
import com.atomiclegacy.items.GasMaskItem;
import com.atomiclegacy.items.HbmToolItems;
import com.atomiclegacy.items.LegacyBombCallerItem;
import com.atomiclegacy.items.LegacyCrayonItem;
import com.atomiclegacy.items.MetalFenceBlockItem;
import com.atomiclegacy.items.NuclearWasteItem;
import com.atomiclegacy.items.OreBasaltBlockItem;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.items.SellafieldBlockItem;
import com.atomiclegacy.items.WasteFuelItem;
import com.atomiclegacy.pollution.HbmArmorProtection;
import com.atomiclegacy.pollution.HbmPollution;
import com.atomiclegacy.pollution.HbmPollutionConstants;
import com.atomiclegacy.pollution.HbmPollutionData;
import com.atomiclegacy.pollution.HbmPollutionType;
import com.atomiclegacy.radiation.HbmLivingHazards;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * [parity R1: diagnostics] custom_data recipes against real item stacks, gas blocks, lung disease,
 * pollution, taint and the 1.7.10 FX entities.
 */
final class ParityR1Diagnostics {
    private static final int BASE_X = 24064;
    private static final int BASE_Z = 24064;
    private static final int BASE_Y = 200;
    private static final List<int[]> FORCED = new ArrayList<>();

    private ParityR1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            customDataRecipes(server, problems, cases);
            forceSite(level);
            gases(level, problems, cases);
            lungs(level, problems, cases);
            pollution(level, problems, cases);
            taint(level, problems, cases);
            fxEntities(level, problems, cases);
        } catch (RuntimeException | ReflectiveOperationException e) {
            AtomicLegacy.LOGGER.error("Parity R1 diagnostics crashed", e);
            problems.add("R1 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) {
                level.setChunkForced(chunk[0], chunk[1], false);
            }
            FORCED.clear();
        }
        return cases[0];
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String message) {
        cases[0]++;
        if (!ok) {
            problems.add("R1: " + message);
        }
    }

    // ---------------------------------------------------------------- custom_data recipes

    private static void customDataRecipes(MinecraftServer server, List<String> problems, int[] cases) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        Map<ResourceLocation, Resource> files = server.getResourceManager().listResources("recipe",
                rl -> rl.getNamespace().equals(AtomicLegacy.MOD_ID) && rl.getPath().endsWith(".json"));
        int checked = 0;
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            JsonElement root;
            try (Reader reader = file.getValue().openAsReader()) {
                root = JsonParser.parseReader(reader);
            } catch (Exception e) {
                problems.add("R1: cannot read " + file.getKey() + ": " + e);
                continue;
            }
            String path = file.getKey().getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getKey().getNamespace(),
                    path.substring("recipe/".length(), path.length() - ".json".length()));
            List<JsonObject> holders = new ArrayList<>();
            collect(root, holders);
            for (JsonObject holder : holders) {
                JsonObject components = holder.getAsJsonObject("components");
                JsonElement raw = components.get("minecraft:custom_data");
                CompoundTag tag;
                try {
                    tag = raw.isJsonPrimitive() ? TagParser.parseTag(raw.getAsString())
                            : CompoundTag.CODEC.parse(JsonOps.INSTANCE, raw).getOrThrow();
                } catch (Exception e) {
                    problems.add("R1: " + id + " custom_data does not parse: " + e.getMessage());
                    continue;
                }
                if (!hasNumeric(tag)) {
                    continue;
                }
                checked++;
                if (server.getRecipeManager().byKey(id).isEmpty()) {
                    if (root.isJsonObject() && root.getAsJsonObject().has("neoforge:conditions")) {
                        continue; // disabled by its load condition (e.g. the expensive recipe mode)
                    }
                    problems.add("R1: recipe " + id + " with numeric custom_data is not loaded");
                    continue;
                }
                boolean ingredient = holder.has("items") || "neoforge:components".equals(str(holder, "type"));
                String itemId = ingredient ? firstItem(holder.get("items")) : str(holder, "id");
                Item item = itemId == null ? null : BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                if (item == null || item == net.minecraft.world.item.Items.AIR) {
                    problems.add("R1: " + id + " custom_data holder without item");
                    continue;
                }
                ItemStack real = realStack(item, tag);
                if (real == null) {
                    problems.add("R1: " + id + " no real-stack factory for " + itemId + " " + tag);
                    continue;
                }
                if (ingredient) {
                    Ingredient decoded = Ingredient.CODEC.parse(ops, holder).getOrThrow();
                    check(problems, cases, decoded.test(real), id + " ingredient rejects the real " + itemId + " " + real.getComponentsPatch());
                } else {
                    ItemStack decoded = ItemStack.CODEC.parse(ops, holder).getOrThrow();
                    check(problems, cases, ItemStack.isSameItemSameComponents(decoded, real),
                            id + " result " + decoded.getComponentsPatch() + " differs from the real " + real.getComponentsPatch());
                }
            }
        }
        check(problems, cases, checked >= 150, "expected >= 150 numeric custom_data recipe holders, found " + checked);
    }

    private static void collect(JsonElement element, List<JsonObject> out) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("components") && object.get("components").isJsonObject()
                    && object.getAsJsonObject("components").has("minecraft:custom_data")) {
                out.add(object);
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collect(entry.getValue(), out);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collect(child, out);
            }
        }
    }

    private static boolean hasNumeric(CompoundTag tag) {
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            if (value instanceof NumericTag || value instanceof CompoundTag nested && hasNumeric(nested)) {
                return true;
            }
        }
        return false;
    }

    private static String str(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    private static String firstItem(JsonElement items) {
        if (items == null) return null;
        if (items.isJsonPrimitive()) return items.getAsString();
        if (items.isJsonArray()) {
            JsonArray array = items.getAsJsonArray();
            return array.isEmpty() ? null : array.get(0).getAsString();
        }
        return null;
    }

    /** Builds the stack the way the game and the item itself create it (only the values are read from the recipe). */
    private static ItemStack realStack(Item item, CompoundTag tag) {
        ItemStack probe = new ItemStack(item);
        probe.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (item instanceof com.atomiclegacy.items.BrokenItem && tag.contains("item_id")) {
            Item source = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString("item_id")));
            return com.atomiclegacy.items.BrokenItem.make(new ItemStack(source, tag.getInt("item_count")));
        }
        if (item instanceof com.atomiclegacy.items.GunB92CellItem) { // [parity c1] GunB92Cell.getFullCell()
            ItemStack cell = new ItemStack(item);
            com.atomiclegacy.items.GunB92CellItem.setPower(cell, tag.getInt("energy"));
            return cell;
        }
        if (item instanceof NuclearWasteItem waste) return waste.stackFor(waste.wasteClass(probe));
        if (item instanceof ConcreteColoredBlockItem concrete) return ConcreteColoredBlockItem.stackFor(concrete, ConcreteColoredBlock.meta(probe));
        if (item instanceof LegacyBombCallerItem) return LegacyBombCallerItem.stackFor(LegacyBombCallerItem.type(probe));
        if (item instanceof LegacyCrayonItem) return LegacyCrayonItem.stackFor(item, tag.getInt("crayon"));
        if (item instanceof ScrapsItem) return ScrapsItem.create(ScrapsItem.contents(probe), ScrapsItem.isLiquid(probe));
        if (item instanceof SellafieldBlockItem sellafield) return SellafieldBlockItem.stackFor(sellafield, tag.getInt("level"));
        if (item instanceof WasteFuelItem) return tag.getBoolean(WasteFuelItem.HOT_TAG) ? WasteFuelItem.hot(new ItemStack(item)) : new ItemStack(item);
        if (item instanceof MetalFenceBlockItem fence) return tag.getBoolean("post") ? MetalFenceBlockItem.postStack(fence) : new ItemStack(item);
        if (item instanceof FilingCabinetBlockItem cabinet) return FilingCabinetBlockItem.stackFor(cabinet, tag.getInt("variant"));
        if (item instanceof OreBasaltBlockItem basalt) return OreBasaltBlockItem.stackFor(basalt, tag.getInt("variant"));
        if (item instanceof HbmToolItems.AbilityItem tool && tag.contains("fuel")) {
            ItemStack stack = new ItemStack(item);
            tool.energy().setFill(stack, tag.getInt("fuel"));
            return stack;
        }
        if (item instanceof BlowtorchItem) {
            try {
                ItemStack stack = new ItemStack(item);
                Method key = BlowtorchItem.class.getDeclaredMethod("key", int.class);
                Method setAmount = BlowtorchItem.class.getDeclaredMethod("setAmount", ItemStack.class, int.class, int.class);
                key.setAccessible(true);
                setAmount.setAccessible(true);
                for (int tank = 0; tank < 2; tank++) {
                    String name;
                    try {
                        name = (String) key.invoke(item, tank);
                    } catch (ReflectiveOperationException e) {
                        continue;
                    }
                    if (tag.contains(name)) setAmount.invoke(item, stack, tank, tag.getInt(name));
                }
                return stack;
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- site helpers

    private static void forceSite(ServerLevel level) {
        int cx = BASE_X >> 4;
        int cz = BASE_Z >> 4;
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                level.setChunkForced(cx + dx, cz + dz, true);
                level.getChunk(cx + dx, cz + dz);
                FORCED.add(new int[] {cx + dx, cz + dz});
            }
        }
    }

    private static void clearBox(ServerLevel level, BlockPos center, int r) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    private static net.minecraft.world.entity.animal.Pig zombie(ServerLevel level, BlockPos pos) {
        net.minecraft.world.entity.animal.Pig zombie = EntityType.PIG.create(level);
        zombie.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return zombie;
    }

    private static FakePlayer maskedPlayer(ServerLevel level) {
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("7d1f3a59-6e1c-4f4e-9b1e-2f6d1c0a9e21"), "parity_r1"));
        p.setGameMode(GameType.SURVIVAL);
        p.removeAllEffects();
        HbmLivingHazards.clear(p);
        ItemStack mask = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("gas_mask")));
        GasMaskItem.installFilter(mask, new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("gas_mask_filter"))), p);
        p.setItemSlot(EquipmentSlot.HEAD, mask);
        return p;
    }

    // ---------------------------------------------------------------- gases

    private static void gases(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos p = new BlockPos(BASE_X, BASE_Y, BASE_Z);
        clearBox(level, p, 3);

        // chlorine: effects, mask protection with filter wear
        BlockState chlorine = HbmBlocks.CHLORINE_GAS.get().defaultBlockState();
        net.minecraft.world.entity.animal.Pig z = zombie(level, p);
        chlorine.entityInside(level, p, z);
        MobEffectInstance poison = z.getEffect(MobEffects.POISON);
        MobEffectInstance wither = z.getEffect(MobEffects.WITHER);
        check(problems, cases, poison != null && poison.getDuration() == 400 && poison.getAmplifier() == 2
                && wither != null && wither.getDuration() == 20 && wither.getAmplifier() == 1
                && z.hasEffect(MobEffects.BLINDNESS) && z.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1
                && z.getEffect(MobEffects.DIG_SLOWDOWN).getDuration() == 600, "chlorine gas: BlockGasClorine effects");
        FakePlayer masked = maskedPlayer(level);
        if (HbmArmorProtection.hasHeadProtection(masked, HbmArmorProtection.HazardClass.GAS_LUNG)) {
            int before = GasMaskItem.getInstalledFilter(masked.getItemBySlot(EquipmentSlot.HEAD), level.registryAccess()).getDamageValue();
            chlorine.entityInside(level, p, masked);
            int after = GasMaskItem.getInstalledFilter(masked.getItemBySlot(EquipmentSlot.HEAD), level.registryAccess()).getDamageValue();
            check(problems, cases, !masked.hasEffect(MobEffects.POISON) && after == before + 1, "chlorine gas: mask protects and filter wears by 1");
        } else {
            problems.add("R1: gas_mask + gas_mask_filter does not protect against GAS_LUNG");
        }

        // monoxide damage
        net.minecraft.world.entity.animal.Pig mono = zombie(level, p);
        float hp = mono.getHealth();
        HbmBlocks.GAS_MONOXIDE.get().defaultBlockState().entityInside(level, p, mono);
        check(problems, cases, Math.abs(mono.getHealth() - (hp - 1.0F)) < 1e-4F
                && mono.getLastDamageSource() != null && mono.getLastDamageSource().is(HbmDamageTypes.MONOXIDE), "monoxide gas: 1 damage of type monoxide");

        // asbestos / coal / meltdown lung counters
        net.minecraft.world.entity.animal.Pig lungs = zombie(level, p);
        HbmBlocks.GAS_ASBESTOS.get().defaultBlockState().entityInside(level, p, lungs);
        HbmBlocks.GAS_COAL.get().defaultBlockState().entityInside(level, p, lungs);
        HbmBlocks.GAS_MELTDOWN.get().defaultBlockState().entityInside(level, p, lungs);
        HbmLivingHazards hz = HbmLivingHazards.get(lungs);
        check(problems, cases, hz.getAsbestos() == 1 + 5 && hz.getBlackLung() == 10, "asbestos +1, coal dust +10 black lung, meltdown +5 asbestos");

        // spreading and decay with fixed seeds
        Object[][] gases = {
                {HbmBlocks.CHLORINE_GAS.get(), 10, true}, {HbmBlocks.GAS_ASBESTOS.get(), 50, false},
                {HbmBlocks.GAS_COAL.get(), 20, false}, {HbmBlocks.GAS_MONOXIDE.get(), 100, true},
                {HbmBlocks.GAS_RADON.get(), 50, true}};
        for (Object[] gas : gases) {
            Block block = (Block) gas[0];
            int decayDenominator = (Integer) gas[1];
            boolean mostlyDown = (Boolean) gas[2];
            int removed = 0;
            int down = 0;
            int trials = 2000;
            RandomSource random = RandomSource.create(4711L);
            for (int i = 0; i < trials; i++) {
                level.setBlock(p, block.defaultBlockState(), 2 | 16);
                block.defaultBlockState().tick(level, p, random);
                boolean gone = level.getBlockState(p).isAir();
                int moved = 0;
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                    if (level.getBlockState(p.relative(d)).is(block)) {
                        moved++;
                        if (d == net.minecraft.core.Direction.DOWN) down++;
                        level.setBlock(p.relative(d), Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }
                if (gone && moved == 0) removed++;
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            double expected = trials / (double) decayDenominator;
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            check(problems, cases, Math.abs(removed - expected) <= Math.max(12.0D, expected * 0.35D),
                    name + ": decay 1/" + decayDenominator + " per update, got " + removed + "/" + trials);
            if (mostlyDown) {
                check(problems, cases, down > (trials - removed) * 0.6D, name + ": first direction is mostly down (" + down + ")");
            }
        }

        // flammable gas next to a fire source becomes fire
        level.setBlock(p.below(), Blocks.STONE.defaultBlockState(), 2 | 16);
        level.setBlock(p.east(), Blocks.LAVA.defaultBlockState(), 2 | 16);
        level.setBlock(p, HbmBlocks.GAS_FLAMMABLE.get().defaultBlockState(), 2 | 16);
        HbmBlocks.GAS_FLAMMABLE.get().defaultBlockState().tick(level, p, RandomSource.create(1L));
        check(problems, cases, level.getBlockState(p).is(Blocks.FIRE), "flammable gas next to lava combusts into fire");
        clearBox(level, p, 3);
    }

    // ---------------------------------------------------------------- lungs

    private static void lungs(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        Method handle = com.atomiclegacy.pollution.PollutionEvents.class.getDeclaredMethod("handleLungDisease",
                ServerLevel.class, LivingEntity.class, BlockPos.class);
        handle.setAccessible(true);
        BlockPos p = new BlockPos(BASE_X + 8, BASE_Y, BASE_Z);
        net.minecraft.world.entity.animal.Pig z = zombie(level, p);
        HbmLivingHazards hz = HbmLivingHazards.get(z);
        hz.setAsbestos(z, (int) (HbmLivingHazards.MAX_ASBESTOS * 0.8D));
        hz.setBlackLung(z, 100);
        HbmLivingHazards.set(z, hz);
        handle.invoke(null, level, z, p);
        MobEffectInstance weakness = z.getEffect(MobEffects.WEAKNESS);
        check(problems, cases, weakness != null && weakness.getAmplifier() == 2 && weakness.getDuration() == 100,
                "lung disease applies to mobs too: weakness II for total > 0.75");
        check(problems, cases, HbmLivingHazards.get(z).getBlackLung() == 99, "black lung below half heals by 1 per tick");

        net.minecraft.world.entity.animal.Pig dead = zombie(level, p);
        HbmLivingHazards dh = HbmLivingHazards.get(dead);
        dh.setAsbestos(dead, HbmLivingHazards.MAX_ASBESTOS);
        HbmLivingHazards.set(dead, dh);
        check(problems, cases, HbmLivingHazards.get(dead).getAsbestos() == 0 && dead.getHealth() <= 0.0F,
                "asbestos at the maximum resets to 0 with 1000 asbestos damage");
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(p).inflate(8.0D))
                .forEach(net.minecraft.world.entity.Entity::discard);

        FakePlayer player = maskedPlayer(level);
        HbmLivingHazards ph = HbmLivingHazards.get(player);
        ph.setAsbestos(player, 500);
        ph.setBlackLung(player, 500);
        HbmLivingHazards.set(player, ph);
        player.setGameMode(GameType.CREATIVE);
        handle.invoke(null, level, player, p);
        check(problems, cases, HbmLivingHazards.get(player).getAsbestos() == 0 && HbmLivingHazards.get(player).getBlackLung() == 0,
                "creative players get asbestos and black lung reset");
        player.setGameMode(GameType.SURVIVAL);
    }

    // ---------------------------------------------------------------- pollution

    @SuppressWarnings("unchecked")
    private static void pollution(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos p = new BlockPos(BASE_X, 64, BASE_Z);
        HbmPollutionData data = HbmPollutionData.get(level);
        data.clearAround(p, 1);
        HbmPollution.increment(level, p, HbmPollutionType.SOOT, 1.0D);
        HbmPollution.increment(level, p, HbmPollutionType.SOOT, 1.0D);
        double mult = com.atomiclegacy.config.HbmConfig.pollutionMultiplier();
        check(problems, cases, Math.abs(data.get(p, HbmPollutionType.SOOT) - 2.0D * mult) < 1e-6,
                "pollution accumulates per 64x64 region (incrementPollution * pollutionMult)");
        data.clearAround(p, 1);

        Method key = HbmPollutionData.class.getDeclaredMethod("key", int.class, int.class);
        key.setAccessible(true);
        Method solve = com.atomiclegacy.pollution.HbmPollutionWorlds.class.getDeclaredMethod("solve", Map.class);
        solve.setAccessible(true);
        int rx = p.getX() >> HbmPollutionConstants.REGION_SHIFT;
        int rz = p.getZ() >> HbmPollutionConstants.REGION_SHIFT;
        long center = (Long) key.invoke(null, rx, rz);
        long east = (Long) key.invoke(null, rx + 1, rz);
        HbmPollutionData.PollutionValues values = new HbmPollutionData.PollutionValues();
        values.set(HbmPollutionType.SOOT, 100.0D);
        values.set(HbmPollutionType.POISON, 20.0D);
        values.set(HbmPollutionType.HEAVYMETAL, 10.0D);
        Map<Long, HbmPollutionData.PollutionValues> snapshot = new HashMap<>();
        snapshot.put(center, values);
        Map<Long, HbmPollutionData.PollutionValues> next = (Map<Long, HbmPollutionData.PollutionValues>) solve.invoke(null, snapshot);
        HbmPollutionData.PollutionValues c = next.get(center);
        HbmPollutionData.PollutionValues e = next.get(east);
        check(problems, cases, c != null && e != null
                && Math.abs(c.get(HbmPollutionType.SOOT) - 100.0D * 0.8D * 0.99D) < 1e-3
                && Math.abs(c.get(HbmPollutionType.POISON) - 18.0D) < 1e-3
                && Math.abs(c.get(HbmPollutionType.HEAVYMETAL) - 9.995D) < 1e-3
                && Math.abs(e.get(HbmPollutionType.SOOT) - 5.0D) < 1e-3
                && Math.abs(e.get(HbmPollutionType.POISON) - 0.5D) < 1e-3
                && e.get(HbmPollutionType.HEAVYMETAL) == 0.0D, "pollution update: decay and spread to the 4 neighbours");

        Map<HbmPollutionType, Double> flue = HbmPollution.releaseMap(HbmFluids.byName("flue").orElseThrow());
        check(problems, cases, Math.abs(flue.getOrDefault(HbmPollutionType.SOOT, 0.0D) - HbmPollutionConstants.SOOT_PER_SECOND * 0.005D * 5.0D) < 1e-9,
                "FLUE release = SOOT_GAS * 5");
    }

    // ---------------------------------------------------------------- taint

    private static void taint(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos p = new BlockPos(BASE_X + 16, BASE_Y, BASE_Z);
        clearBox(level, p, 5);
        for (BlockPos pos : BlockPos.betweenClosed(p.offset(-4, -4, -4), p.offset(4, 4, 4))) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2 | 16);
        }
        level.setBlock(p, HbmBlocks.TAINT.get().defaultBlockState(), 2 | 16);
        HbmBlocks.TAINT.get().defaultBlockState().randomTick(level, p, RandomSource.create(99L));
        int spread = 0;
        boolean wrong = false;
        for (BlockPos pos : BlockPos.betweenClosed(p.offset(-4, -4, -4), p.offset(4, 4, 4))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(HbmBlocks.TAINT.get()) || pos.equals(p)) continue;
            spread++;
            int dist = Math.abs(pos.getX() - p.getX()) + Math.abs(pos.getY() - p.getY()) + Math.abs(pos.getZ() - p.getZ());
            int age = state.getValue(TaintBlock.AGE);
            if (dist > 4 || (age != 1 && age != 3)) wrong = true;
        }
        check(problems, cases, spread > 0 && !wrong, "taint spreads within manhattan 4 as meta+1 (air nearby) or meta+3");

        net.minecraft.world.entity.animal.Pig z = zombie(level, p.above(6));
        BlockState taint = HbmBlocks.TAINT.get().defaultBlockState().setValue(TaintBlock.AGE, 5);
        MobEffectInstance effect = null;
        for (int i = 0; i < 5000 && effect == null; i++) {
            taint.entityInside(level, p, z);
            effect = z.getEffect(HbmMobEffects.TAINT);
        }
        check(problems, cases, effect != null && effect.getAmplifier() == 10 && effect.getDuration() == 300 && effect.getCures().isEmpty(),
                "taint block: Taint (15 - meta) for 15 s without curative items");
        clearBox(level, p, 5);
    }

    // ---------------------------------------------------------------- FX entities

    private static void fxEntities(ServerLevel level, List<String> problems, int[] cases) {
        String[] ids = {"entity_chlorine_fx", "entity_pink_cloud_fx", "entity_cloud_fx", "entity_agent_orange"};
        for (String id : ids) {
            check(problems, cases, BuiltInRegistries.ENTITY_TYPE.containsKey(AtomicLegacy.id(id)), "FX entity " + id + " registered");
        }
        // [parity S4: diagnostics] entity area tests need entity-ticking spawn chunks (y 270): at x 24088 the entity
        // section of a fresh audit world is not yet accessible, so getEntities did not see the added pig.
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos p = new BlockPos(spawn.getX() - 20, 270, spawn.getZ() - 20);
        clearBox(level, p, 3);
        BlockPos site = new BlockPos(BASE_X + 24, BASE_Y, BASE_Z); // block tests stay on the block site
        clearBox(level, site, 3);

        // pinkCloud queries the level; use an added zombie for the area tests
        float hp;
        net.minecraft.world.entity.animal.Pig added = zombie(level, p);
        level.addFreshEntity(added);
        hp = added.getHealth();
        LegacyCloudFxEntity.pinkCloud(level, p.getX(), p.getY(), p.getZ(), 2.0D);
        check(problems, cases, Math.abs(added.getHealth() - (hp - 5.0F)) < 1e-4F && added.getLastDamageSource() != null
                && added.getLastDamageSource().is(HbmDamageTypes.PC), "pink cloud: ModDamageSource.pc 5");
        added.invulnerableTime = 0;
        added.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 200, 0));
        hp = added.getHealth();
        LegacyCloudFxEntity.cloud(level, p.getX(), p.getY(), p.getZ(), 2.0D);
        check(problems, cases, !added.hasEffect(HbmMobEffects.TAINT) && added.hasEffect(HbmMobEffects.MUTATION)
                && added.getEffect(HbmMobEffects.MUTATION).getDuration() == 72000 && Math.abs(added.getHealth() - (hp - 5.0F)) < 1e-4F,
                "cloud: taint becomes 1 h mutation, then ModDamageSource.cloud 5");
        added.removeAllEffects();
        LegacyCloudFxEntity.poison(level, p.getX(), p.getY(), p.getZ(), 2.0D);
        check(problems, cases, added.hasEffect(MobEffects.POISON) && added.getEffect(MobEffects.POISON).getAmplifier() == 2,
                "chlorine/agent orange FX: ExplosionChaos.poison effects");
        added.discard();

        // agent orange hits grass: coarse dirt around, leaves removed
        BlockPos ground = site.below(2);
        level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);
        level.setBlock(ground.east(), Blocks.OAK_LEAVES.defaultBlockState(), 2 | 16);
        LegacyCloudFxEntity orange = new LegacyCloudFxEntity(HbmEntityTypes.AGENT_ORANGE_FX.get(), level);
        orange.setPos(ground.getX() + 0.5D, ground.getY() + 1.05D, ground.getZ() + 0.5D);
        orange.setDeltaMovement(0.0D, -0.1D, 0.0D);
        for (int i = 0; i < 5 && !orange.isRemoved(); i++) {
            orange.tick();
        }
        check(problems, cases, orange.isRemoved() && level.getBlockState(ground).is(Blocks.COARSE_DIRT)
                && level.getBlockState(ground.east()).isAir(), "agent orange FX: grass -> coarse dirt, leaves removed (solinium)");

        // chlorine FX lifetime starts at 700..800 and the cloud moves
        LegacyCloudFxEntity fx = LegacyCloudFxEntity.create(level, LegacyCloudFxEntity.Kind.CHLORINE, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
        fx.tick();
        check(problems, cases, fx.maxAge >= 700 && fx.maxAge <= 800 && fx.particleAge == 1, "chlorine FX: maxAge 700 + rand(101)");
        clearBox(level, p, 3);
        clearBox(level, site, 3);
    }
}
