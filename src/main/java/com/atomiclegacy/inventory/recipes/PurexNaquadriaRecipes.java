package com.atomiclegacy.inventory.recipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * [parity V4: PUREX naquadria] PUREXRecipes.registerDefaults registers purex.watznaqadah and purex.watznaqadria
 * only when OreDictionary "nuggetNaquadria" is non-empty, outputting a copy of its first entry x12. HBM itself
 * never registers that ore name, so standalone they do not exist. The 1.21 equivalent is the item tag
 * c:nuggets/naquadria: when it has entries, the two recipes are added with its first entry; otherwise none.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class PurexNaquadriaRecipes {
    public static final TagKey<Item> NUGGET_NAQUADRIA =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "nuggets/naquadria"));
    public static final ResourceLocation WATZ_NAQADAH =
            ResourceLocation.fromNamespaceAndPath(AtomicLegacy.MOD_ID, "purex/watz_naqadah");
    public static final ResourceLocation WATZ_NAQADRIA =
            ResourceLocation.fromNamespaceAndPath(AtomicLegacy.MOD_ID, "purex/watz_naqadria");

    private static final String TEMPLATE = """
            {"type":"atomiclegacy:purex","group":"autoswitch.watz","duration":60,"power":10000,
             "input_items":[{"ingredient":{"type":"neoforge:components","items":["atomiclegacy:watz_pellet_depleted"],
               "components":{"minecraft:custom_data":{"watz_type":"%s"},"minecraft:custom_model_data":%d}},"count":1}],
             "input_fluids":[{"fluid":"kerosene","amount":500},{"fluid":"nitric_acid","amount":250}],
             "output_items":[{"id":"%s","count":12},{"id":"atomiclegacy:nugget_euphemium","count":6},
               {"id":"atomiclegacy:nuclear_waste","count":2}],
             "output_fluids":[{"fluid":"watz","amount":1000}]}
            """;

    private PurexNaquadriaRecipes() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        apply(event.getServer());
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            apply(server);
        }
    }

    /** Re-evaluates the ore dictionary condition against the current tags and recipe manager. */
    public static void apply(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        boolean present = false;
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.id().equals(WATZ_NAQADAH) || holder.id().equals(WATZ_NAQADRIA)) {
                present = true;
            } else {
                recipes.add(holder);
            }
        }
        Optional<Item> first = firstNaquadriaNugget();
        if (first.isEmpty() && !present) {
            return;
        }
        first.ifPresent(item -> recipes.addAll(recipesFor(server.registryAccess(), item)));
        manager.replaceRecipes(recipes);
    }

    /** The two recipes with {@code nuggetNQR} = the first naquadria nugget. */
    public static List<RecipeHolder<PurexRecipe>> recipesFor(RegistryAccess access, Item nuggetNQR) {
        String nqr = BuiltInRegistries.ITEM.getKey(nuggetNQR).toString();
        // EnumWatzType ordinal = custom_model_data (SCHRABIDIUM 0 .. DU 9, NQD 10, NQR 11).
        return List.of(build(access, WATZ_NAQADAH, "nqd", 10, nqr),
                build(access, WATZ_NAQADRIA, "nqr", 11, "atomiclegacy:nugget_co60"));
    }

    public static Optional<Item> firstNaquadriaNugget() {
        return BuiltInRegistries.ITEM.getTag(NUGGET_NAQUADRIA)
                .flatMap(set -> set.stream().findFirst())
                .map(Holder::value);
    }

    private static RecipeHolder<PurexRecipe> build(RegistryAccess access, ResourceLocation id, String type,
                                                   int modelData, String firstOutput) {
        JsonElement json = JsonParser.parseString(TEMPLATE.formatted(type, modelData, firstOutput));
        PurexRecipe recipe = HbmRecipeTypes.PUREX_SERIALIZER.get().codec().codec()
                .parse(RegistryOps.create(JsonOps.INSTANCE, access), json)
                .getOrThrow(message -> new IllegalStateException("PUREX " + id + ": " + message));
        return new RecipeHolder<>(id, recipe);
    }
}
