package com.atomiclegacy.inventory.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * [parity K3] Autoswitch groups of AssemblyMachineRecipes (GenericRecipe#setGroup) of the pinned 1.7.10 commit.
 * Members are the original recipe names, which the port keeps as the recipe "group" field; registration order is
 * the order in which ModuleMachineBase#canProcess tries them.
 */
public final class AssemblyMachineAutoSwitch {
    private static final Map<String, List<String>> GROUPS = Map.of(
            "autoswitch.plates", List.of("ass.plateiron", "ass.plategold", "ass.platetitanium", "ass.platealu", "ass.platesteel",
                    "ass.platelead", "ass.platecopper", "ass.plateschrab", "ass.platecmb", "ass.plategunmetal",
                    "ass.plateweaponsteel", "ass.platesaturnite", "ass.platedura"),
            "autoswitch.pilerod", List.of("ass.pilepabe", "ass.pilepobe", "ass.pilezr", "ass.pilenu", "ass.pilethorium"),
            "autoswitch.cyclotron", List.of("ass.partlith", "ass.partberyl", "ass.partcoal", "ass.partcop", "ass.partplut")
    );

    private AssemblyMachineAutoSwitch() {
    }

    /** The autoswitch group the named recipe belongs to. */
    public static Optional<String> groupOf(String recipeName) {
        for (Map.Entry<String, List<String>> entry : GROUPS.entrySet()) {
            if (entry.getValue().contains(recipeName)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** All recipes of the group except {@code recipeName}, in registration order. */
    public static List<String> otherMembers(String recipeName) {
        Optional<String> group = groupOf(recipeName);
        if (group.isEmpty()) {
            return List.of();
        }
        List<String> members = new ArrayList<>(GROUPS.get(group.get()));
        members.remove(recipeName);
        return members;
    }

    public static Map<String, List<String>> groups() {
        return GROUPS;
    }
}
