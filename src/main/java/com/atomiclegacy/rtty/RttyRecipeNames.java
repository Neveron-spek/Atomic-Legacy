package com.atomiclegacy.rtty;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * [parity U2: ROR] ModuleMachineBase#getRecipeName returns the GenericRecipe internal name ("null" when unset).
 * Assembly, chemical, fusion and plasma forge recipes keep that name as their recipe group; PUREX recipes
 * use the group for autoswitch, so their names are derived from the file name (PUREXRecipes.java).
 */
public final class RttyRecipeNames {
    /** ModuleMachineBase.recipe default. */
    public static final String NULL = "null";

    /** Port PUREX recipe file names whose 1.7.10 name is not the file name without underscores. */
    private static final Map<String, String> PUREX_EXCEPTIONS = Map.of(
            "zirnox_natural_uranium", "purex.zirnoxnu",
            "zirnox_thorium", "purex.zirnoxthmeu",
            "pwr_bfb_am_mix", "purex.pwrbfbam",
            "pwr_bfb_pu241", "purex.pwrbfpu241",
            "watz_schrabidium", "purex.watzschrab",
            "vitrify_liquid", "purex.vitliquid",
            "vitrify_gas", "purex.vitgaseous",
            "vitrify_solid", "purex.vitsolid",
            "schrab_pwr_mep", "purex.schrabpwr",
            "schrab_pwr_men", "purex.schrabmen");

    private RttyRecipeNames() {
    }

    public static String purex(@Nullable ResourceLocation recipe) {
        if (recipe == null) return NULL;
        String path = recipe.getPath();
        path = path.substring(path.lastIndexOf('/') + 1);
        String exception = PUREX_EXCEPTIONS.get(path);
        return exception != null ? exception : "purex." + path.replace("_", "");
    }

    /** The recipe group is the internal name; "null" if no recipe is selected. */
    public static String group(@Nullable String group) {
        return group == null || group.isEmpty() ? NULL : group;
    }
}
