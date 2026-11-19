package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.LegacyCraftBookRecipes;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * [parity S3: JEI] Categories for the 1.7.10 NEIRegistry handlers that had no JEI counterpart: Annihilator, Ashpit,
 * Deuterium, Tooling, Construction, RBMK Rod Disassembly, RTG, Grenade Crafting, Satellite, Black Book and Crucible Smelting.
 * Titles are NEI's: hardcoded English strings stay literal, getLocalizedName() titles use the block name.
 */
public final class ParityS3Jei {
    public static final RecipeType<UniversalJeiRecipe> ANNIHILATOR = universal("annihilator");
    public static final RecipeType<UniversalJeiRecipe> ASHPIT = universal("ashpit");
    public static final RecipeType<UniversalJeiRecipe> DEUTERIUM = universal("deuterium");
    public static final RecipeType<UniversalJeiRecipe> TOOLING = universal("tooling");
    public static final RecipeType<UniversalJeiRecipe> CONSTRUCTION = universal("construction");
    public static final RecipeType<UniversalJeiRecipe> RBMK_DISASSEMBLY = universal("rbmk_disassembly");
    public static final RecipeType<UniversalJeiRecipe> RTG = universal("rtg");
    public static final RecipeType<UniversalJeiRecipe> GRENADE = universal("grenade");
    public static final RecipeType<ParityS3JeiRecipes.SatelliteDisplay> SATELLITE =
            RecipeType.create(AtomicLegacy.MOD_ID, "satellite", ParityS3JeiRecipes.SatelliteDisplay.class);
    public static final RecipeType<LegacyCraftBookRecipes.MagicDisplay> BOOK =
            RecipeType.create(AtomicLegacy.MOD_ID, "book_of_boxcars", LegacyCraftBookRecipes.MagicDisplay.class);
    public static final RecipeType<ParityS3JeiRecipes.CrucibleSmeltingDisplay> CRUCIBLE_SMELTING =
            RecipeType.create(AtomicLegacy.MOD_ID, "crucible_smelting", ParityS3JeiRecipes.CrucibleSmeltingDisplay.class);

    private ParityS3Jei() {
    }

    private static RecipeType<UniversalJeiRecipe> universal(String path) {
        return RecipeType.create(AtomicLegacy.MOD_ID, path, UniversalJeiRecipe.class);
    }

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new UniversalJeiCategory(helper, ANNIHILATOR, Component.literal("Annihilator"), stack(HbmBlocks.MACHINE_ANNIHILATOR.get())),
                new UniversalJeiCategory(helper, ASHPIT, name(HbmBlocks.MACHINE_ASHPIT.get()), stack(HbmBlocks.MACHINE_ASHPIT.get())),
                new UniversalJeiCategory(helper, DEUTERIUM, name(HbmBlocks.MACHINE_DEUTERIUM_EXTRACTOR.get()), stack(HbmBlocks.MACHINE_DEUTERIUM_EXTRACTOR.get())),
                new UniversalJeiCategory(helper, TOOLING, Component.literal("Tooling"), new ItemStack(HbmItems.BOLTGUN.get())),
                new UniversalJeiCategory(helper, CONSTRUCTION, Component.literal("Construction"), new ItemStack(HbmItems.BLOWTORCH.get())),
                new UniversalJeiCategory(helper, RBMK_DISASSEMBLY, Component.literal("RBMK Rod Disassembly"), stack(Blocks.CRAFTING_TABLE)),
                new UniversalJeiCategory(helper, RTG, Component.literal("RTG"), stack(HbmBlocks.MACHINE_RTG_GREY.get())),
                new UniversalJeiCategory(helper, GRENADE, Component.literal("Grenade Crafting"), new ItemStack(HbmItems.GRENADE_UNIVERSAL.get())),
                new SatelliteRecipeCategory(helper),
                new BookRecipeCategory(helper),
                new CrucibleSmeltingRecipeCategory(helper)
        );
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ANNIHILATOR, ParityS3JeiRecipes.annihilator());
        registration.addRecipes(ASHPIT, ParityS3JeiRecipes.ashpit());
        registration.addRecipes(DEUTERIUM, ParityS3JeiRecipes.deuterium());
        registration.addRecipes(TOOLING, ParityS3JeiRecipes.tooling());
        registration.addRecipes(CONSTRUCTION, ParityS3JeiRecipes.construction());
        registration.addRecipes(RBMK_DISASSEMBLY, ParityS3JeiRecipes.rbmkDisassembly());
        registration.addRecipes(RTG, ParityS3JeiRecipes.rtg());
        registration.addRecipes(GRENADE, ParityS3JeiRecipes.grenade());
        registration.addRecipes(SATELLITE, ParityS3JeiRecipes.satellites());
        registration.addRecipes(BOOK, ParityS3JeiRecipes.book());
        registration.addRecipes(CRUCIBLE_SMELTING, ParityS3JeiRecipes.crucibleSmelting(
                registration.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)));
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ANNIHILATOR.get(), ANNIHILATOR);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ASHPIT.get(), ASHPIT);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_DEUTERIUM_EXTRACTOR.get(), DEUTERIUM);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_DEUTERIUM_TOWER.get(), DEUTERIUM);
        for (ItemStack tool : ParityS3JeiRecipes.toolingMachines()) {
            registration.addRecipeCatalyst(tool, TOOLING);
        }
        for (ItemStack tool : ParityS3JeiRecipes.constructionMachines()) {
            registration.addRecipeCatalyst(tool, CONSTRUCTION);
        }
        registration.addRecipeCatalyst(Blocks.CRAFTING_TABLE, RBMK_DISASSEMBLY, GRENADE);
        for (ItemStack machine : ParityS3JeiRecipes.rtgMachines()) {
            registration.addRecipeCatalyst(machine, RTG);
        }
        registration.addRecipeCatalyst(HbmBlocks.SAT_DOCK.get(), SATELLITE);
        registration.addRecipeCatalyst(HbmItems.BOOK_OF.get(), BOOK);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CRUCIBLE.get(), CRUCIBLE_SMELTING);
    }

    /** Titles that NEI took from Block#getLocalizedName(). */
    static Component name(Block block) {
        return Component.translatable(block.getDescriptionId());
    }

    private static ItemStack stack(Block block) {
        return new ItemStack(block);
    }

    static List<RecipeType<?>> types() {
        return List.of(ANNIHILATOR, ASHPIT, DEUTERIUM, TOOLING, CONSTRUCTION, RBMK_DISASSEMBLY, RTG, GRENADE, SATELLITE, BOOK, CRUCIBLE_SMELTING);
    }
}
