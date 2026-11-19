package com.atomiclegacy.handler;

import com.atomiclegacy.items.StandardAmmoItem.StandardAmmoType;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.blocks.PlushieType;
import com.atomiclegacy.blocks.SnowglobeType;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.BlueprintFolderItem;
import com.atomiclegacy.items.ConcreteColoredBlockItem;
import com.atomiclegacy.items.HbmFluidContainerItem;
import com.atomiclegacy.items.LegacyBobmazonItem;
import com.atomiclegacy.items.LegacyConserveItem;
import com.atomiclegacy.items.LegacyCustomKitItem;
import com.atomiclegacy.items.LegacyVariantBlockItem;
import com.atomiclegacy.items.PlushieBlockItem;
import com.atomiclegacy.items.SnowglobeBlockItem;
import com.atomiclegacy.items.VendingMachineBlockItem;
import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Complete non-firearm 1.7.10 Bobmazon order table. */
public final class LegacyBobmazonOffers {
    public record Offer(ItemStack stack, Requirement requirement, int caps, int rating) { }

    public enum Requirement {
        NONE(null), STEEL("blast_furnace"), ASSEMBLY("assembly"), CHEMICS("chemplant"),
        OIL("desh"), NUCLEAR("technetium"), HIDDEN("hidden");

        private final String advancement;

        Requirement(@Nullable String advancement) {
            this.advancement = advancement;
        }

        public boolean isMet(ServerPlayer player) {
            if (this == NONE || player.getAbilities().instabuild) return true;
            return advancement != null && HbmAdvancements.has(player, advancement);
        }
    }

    private LegacyBobmazonOffers() { }

    public static List<Offer> offers(boolean hidden, HolderLookup.Provider registries) {
        return hidden ? hiddenOffers(registries) : standardOffers();
    }

    @Nullable
    public static Offer request(ServerPlayer player, int index) {
        ItemStack held = player.getMainHandItem();
        LegacyBobmazonItem catalog;
        if (held.getItem() instanceof LegacyBobmazonItem mainHandCatalog) {
            catalog = mainHandCatalog;
        } else {
            held = player.getOffhandItem();
            if (!(held.getItem() instanceof LegacyBobmazonItem offhandCatalog)) return null;
            catalog = offhandCatalog;
        }
        List<Offer> offers = offers(catalog.hidden(), player.registryAccess());
        return index >= 0 && index < offers.size() ? offers.get(index) : null;
    }

    public static boolean isBottleCap(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (!AtomicLegacy.MOD_ID.equals(id.getNamespace())) return false;
        return switch (id.getPath()) {
            case "cap_fritz", "cap_korl", "cap_nuka", "cap_quantum", "cap_rad", "cap_sparkle" -> true;
            default -> false;
        };
    }

    private static List<Offer> standardOffers() {
        List<Offer> offers = new ArrayList<>();
        offers.add(offer("minecraft:torch", 64, Requirement.NONE, 2, 0));
        offers.add(offer("atomiclegacy:definitelyfood", 16, Requirement.NONE, 4, 0));
        offers.add(offer("atomiclegacy:nitra", 4, Requirement.CHEMICS, 16, 0));
        offers.add(offer("atomiclegacy:gun_kit_1", 1, Requirement.ASSEMBLY, 16, 0)); // [parity S4: gun kits]
        offers.add(offer("atomiclegacy:geiger_counter", 1, Requirement.NONE, 16, 0));
        offers.add(offer("atomiclegacy:matchstick", 16, Requirement.STEEL, 2, 0));
        offers.add(new Offer(BlueprintFolderItem.stackFor(BlueprintFolderItem.Variant.STANDARD), Requirement.ASSEMBLY, 64, 0));
        offers.add(new Offer(BlueprintFolderItem.stackFor(BlueprintFolderItem.Variant.DISCOVER), Requirement.OIL, 256, 0));
        offers.add(vendingMachineOffer(0));
        offers.add(vendingMachineOffer(1));
        offers.add(offer("minecraft:jungle_sapling", 1, Requirement.STEEL, 12, 9));
        offers.add(offer("atomiclegacy:plant_foxglove", 1, Requirement.STEEL, 16, 5));
        offers.add(offer("atomiclegacy:plant_tobacco", 1, Requirement.STEEL, 16, 9));
        offers.add(offer("atomiclegacy:plant_nightshade", 1, Requirement.STEEL, 16, 3));
        offers.add(offer("atomiclegacy:plant_hemp", 1, Requirement.STEEL, 4, 10));
        offers.add(offer("atomiclegacy:plant_mustard_willow", 1, Requirement.NUCLEAR, 64, 8));

        Item concreteItem = requiredItem("atomiclegacy:concrete_colored_ext");
        if (!(concreteItem instanceof ConcreteColoredBlockItem concrete)) throw new IllegalStateException("Bobmazon concrete offer resolved to " + concreteItem);
        for (int meta = 0; meta < 8; meta++) offers.add(new Offer(ConcreteColoredBlockItem.stackFor(concrete, meta).copyWithCount(16), Requirement.CHEMICS, 4, 0));

        Item snowglobeItem = requiredItem("atomiclegacy:snowglobe");
        if (!(snowglobeItem instanceof SnowglobeBlockItem snowglobe)) throw new IllegalStateException("Bobmazon snowglobe offer resolved to " + snowglobeItem);
        for (SnowglobeType type : SnowglobeType.values()) {
            // [parity S3: Bobmazon] BobmazonOfferFactory iterates every SnowglobeType, NONE (meta 0) included
            offers.add(new Offer(SnowglobeBlockItem.stackFor(snowglobe, type), Requirement.CHEMICS, 128, 0));
        }

        Item plushieItem = requiredItem("atomiclegacy:plushie");
        if (!(plushieItem instanceof PlushieBlockItem plushie)) throw new IllegalStateException("Bobmazon plushie offer resolved to " + plushieItem);
        for (PlushieType type : PlushieType.values()) {
            if (type != PlushieType.NONE) offers.add(new Offer(PlushieBlockItem.stackFor(plushie, type), Requirement.OIL, 16, type.ordinal() < 3 ? 10 : 0));
        }
        return List.copyOf(offers);
    }

    private static List<Offer> hiddenOffers(HolderLookup.Provider registries) {
        List<Offer> offers = new ArrayList<>();
        add(offers, "minecraft:iron_ingot", 64, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_steel", 64, Requirement.STEEL, 1);
        add(offers, "minecraft:copper_ingot", 64, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_red_copper", 64, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_titanium", 64, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_tungsten", 64, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_cobalt", 64, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_desh", 64, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_tantalium", 64, Requirement.STEEL, 5);
        add(offers, "atomiclegacy:ingot_bismuth", 16, Requirement.STEEL, 5);
        add(offers, "atomiclegacy:ingot_schrabidium", 16, Requirement.STEEL, 5);
        add(offers, "atomiclegacy:ingot_euphemium", 8, Requirement.STEEL, 16);
        add(offers, "atomiclegacy:ingot_dineutronium", 1, Requirement.STEEL, 16);
        add(offers, "atomiclegacy:ingot_starmetal", 16, Requirement.STEEL, 8);
        add(offers, "atomiclegacy:ingot_semtex", 16, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_u235", 16, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ingot_pu239", 16, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:ammo_container", 16, Requirement.STEEL, 5); // [parity S4: ammo_container]
        for (String id : List.of("nuke_starter_kit", "nuke_advanced_kit", "nuke_commercially_kit", "boy_kit", "prototype_kit", "missile_kit")) {
            add(offers, "atomiclegacy:" + id, 1, Requirement.STEEL, id.equals("prototype_kit") ? 10 : 5);
        }
        add(offers, "atomiclegacy:jetpack_vector", 1, Requirement.STEEL, 2);
        add(offers, "atomiclegacy:jetpack_tank", 1, Requirement.STEEL, 2);
        add(offers, "atomiclegacy:gun_kit_1", 1, Requirement.STEEL, 1); // [parity S4: gun kits]
        add(offers, "atomiclegacy:gun_kit_2", 1, Requirement.STEEL, 3);
        add(offers, "atomiclegacy:struct_launcher_core", 1, Requirement.STEEL, 3);
        add(offers, "atomiclegacy:struct_launcher_core_large", 1, Requirement.STEEL, 3);
        add(offers, "atomiclegacy:struct_launcher", 40, Requirement.STEEL, 7);
        add(offers, "atomiclegacy:struct_scaffold", 11, Requirement.STEEL, 7);
        add(offers, "atomiclegacy:loot_10", 1, Requirement.STEEL, 2);
        add(offers, "atomiclegacy:loot_15", 1, Requirement.STEEL, 2);
        add(offers, "atomiclegacy:loot_misc", 1, Requirement.STEEL, 2);
        add(offers, "atomiclegacy:crate_can", 1, Requirement.STEEL, 1);
        add(offers, "atomiclegacy:crate_ammo", 1, Requirement.STEEL, 2);
        // [parity S3: Bobmazon] new ItemStack(ModItems.crucible, 1, 3): the sword is offered with 3 damage
        ItemStack crucible = stack("atomiclegacy:crucible", 1);
        if (crucible.isDamageableItem()) crucible.setDamageValue(3);
        offers.add(new Offer(crucible, Requirement.STEEL, 10, 0));
        add(offers, "atomiclegacy:chopper", 1, Requirement.STEEL, 10);
        add(offers, "atomiclegacy:spawn_worm", 1, Requirement.STEEL, 10);
        add(offers, "atomiclegacy:spawn_ufo", 1, Requirement.STEEL, 10);
        add(offers, "atomiclegacy:sat_laser", 1, Requirement.HIDDEN, 8);
        add(offers, "atomiclegacy:sat_gerald", 1, Requirement.HIDDEN, 32);
        add(offers, "atomiclegacy:billet_yharonite", 4, Requirement.HIDDEN, 16);
        add(offers, "atomiclegacy:ingot_chainsteel", 1, Requirement.HIDDEN, 16);
        add(offers, "atomiclegacy:ingot_electronium", 1, Requirement.HIDDEN, 16);
        add(offers, "atomiclegacy:book_of_", 1, Requirement.HIDDEN, 16);
        add(offers, "atomiclegacy:mese_pickaxe", 1, Requirement.HIDDEN, 16);
        add(offers, "atomiclegacy:mysteryshovel", 1, Requirement.HIDDEN, 16);
        add(offers, "atomiclegacy:ntm_dirt", 1, Requirement.HIDDEN, 16);
        add(offers, "atomiclegacy:euphemium_kit", 1, Requirement.HIDDEN, 64);
        offers.add(new Offer(fusionManKit(registries), Requirement.HIDDEN, 64, 0));
        offers.add(new Offer(maidKit(registries), Requirement.HIDDEN, 64, 0)); // [parity c1]
        offers.add(new Offer(nutBucketKit(registries), Requirement.HIDDEN, 64, 0));
        offers.add(new Offer(frenchieKit(registries), Requirement.HIDDEN, 32, 0)); // [parity c1]
        return List.copyOf(offers);
    }

    private static ItemStack fusionManKit(HolderLookup.Provider registries) {
        ItemStack battery = stack("atomiclegacy:battery_spark", 1);
        BatteryPackItem.setStoredCharge(battery, BatteryPackItem.capacity(battery));
        // [parity S3: Bobmazon] ItemKitCustom.create: display name "§r" + name (not italic) and the gray lore line
        ItemStack kit = stack("atomiclegacy:kit_custom", 1);
        kit.set(DataComponents.CUSTOM_NAME, Component.literal("Fusion Man").withStyle(style -> style.withItalic(false)));
        kit.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(Component.literal("For the nuclear physicist on the go")
                .withStyle(style -> style.withItalic(false).withColor(net.minecraft.ChatFormatting.GRAY)))));
        return LegacyCustomKitItem.create(kit, registries, 0xff00ff, 0x800080,
                stack("atomiclegacy:fusion_klystron", 1), stack("atomiclegacy:fusion_torus", 1), stack("atomiclegacy:fusion_mhdt", 1),
                stack("atomiclegacy:machine_intake", 3), battery, stack("atomiclegacy:machine_chemical_factory", 4),
                stack("atomiclegacy:machine_fluidtank", 8), stack("atomiclegacy:red_wire_coated", 64), stack("atomiclegacy:red_cable", 64),
                fluidBarrel("deuterium", 64), fluidBarrel("tritium", 64), fluidBarrel("perfluoromethyl", 64),
                stack("atomiclegacy:red_pylon_large", 8), stack("atomiclegacy:substation", 4), stack("atomiclegacy:red_connector", 64),
                stack("atomiclegacy:wiring_red_copper", 1), stack("atomiclegacy:machine_chungus", 3), stack("atomiclegacy:template_folder", 1),
                new ItemStack(Items.PAPER, 64), new ItemStack(Items.INK_SAC, 64));
    }

    /** [parity c1] ItemKitCustom.create("Maid's Cleaning Utensils", "For the hard to reach spots", 0x00ff00, 0x008000, ...) */
    private static ItemStack maidKit(HolderLookup.Provider registries) {
        ItemStack kit = stack("atomiclegacy:kit_custom", 1);
        kit.set(DataComponents.CUSTOM_NAME, Component.literal("Maid's Cleaning Utensils").withStyle(style -> style.withItalic(false)));
        kit.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(Component.literal("For the hard to reach spots")
                .withStyle(style -> style.withItalic(false).withColor(net.minecraft.ChatFormatting.GRAY)))));
        return LegacyCustomKitItem.create(kit, registries, 0x00ff00, 0x008000,
                stack("atomiclegacy:gun_m2", 1),
                ammo(StandardAmmoType.BMG50_DU), ammo(StandardAmmoType.BMG50_DU), ammo(StandardAmmoType.BMG50_DU),
                ammo(StandardAmmoType.BMG50_DU), ammo(StandardAmmoType.BMG50_DU),
                stack("atomiclegacy:gun_autoshotgun", 1),
                ammo(StandardAmmoType.G12_MAGNUM), ammo(StandardAmmoType.G12_MAGNUM), ammo(StandardAmmoType.G12_MAGNUM),
                ammo(StandardAmmoType.G12_EXPLOSIVE), ammo(StandardAmmoType.G12_EXPLOSIVE));
    }

    /** [parity c1] ItemKitNBT.create(rpa set, gun_minigun_lacunae, 3 x 64 CAPACITOR_OVERCHARGE).setStackDisplayName("Frenchie's Reward") */
    private static ItemStack frenchieKit(HolderLookup.Provider registries) {
        return LegacyCustomKitItem.create(named(stack("atomiclegacy:kit_custom", 1), "Frenchie's Reward"), registries, 0, 0,
                stack("atomiclegacy:rpa_helmet", 1), stack("atomiclegacy:rpa_plate", 1), stack("atomiclegacy:rpa_legs", 1),
                stack("atomiclegacy:rpa_boots", 1), stack("atomiclegacy:gun_minigun_lacunae", 1),
                ammo(StandardAmmoType.CAPACITOR_OVERCHARGE), ammo(StandardAmmoType.CAPACITOR_OVERCHARGE), ammo(StandardAmmoType.CAPACITOR_OVERCHARGE));
    }

    /** new ItemStack(ModItems.ammo_standard, 64, EnumAmmo.X.ordinal()) */
    private static ItemStack ammo(StandardAmmoType type) {
        return com.atomiclegacy.items.StandardAmmoItem.stackFor(requiredItem("atomiclegacy:ammo_standard"), type).copyWithCount(64);
    }

    private static ItemStack nutBucketKit(HolderLookup.Provider registries) {
        ItemStack conserve = LegacyConserveItem.stackFor(requiredItem("atomiclegacy:canned_conserve"), LegacyConserveItem.Variant.SLIME).copyWithCount(64);
        // [parity S3: Bobmazon] ItemKitNBT.create sets no colors: ItemKitCustom#getColor returns 0 for both passes
        return LegacyCustomKitItem.create(named(stack("atomiclegacy:kit_custom", 1), "The Nut Bucket"), registries, 0, 0,
                named(stack("atomiclegacy:rod_of_discord", 1), "Cock Joke"), named(conserve, "Class A Horse Semen"),
                named(stack("atomiclegacy:pipe_lead", 1), "Get Nutted, Dumbass"), stack("atomiclegacy:gem_alexandrite", 1));
    }

    private static ItemStack fluidBarrel(String fluid, int count) {
        ItemStack stack = HbmFluidContainerItem.makeFull(() -> requiredItem("atomiclegacy:fluid_barrel_full"),
                HbmFluids.byName(fluid).orElseThrow(() -> new IllegalStateException("Missing HBM fluid " + fluid)));
        return stack.copyWithCount(count);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void add(List<Offer> offers, String itemId, int count, Requirement requirement, int caps) {
        offers.add(offer(itemId, count, requirement, caps, 0));
    }

    private static Offer offer(String itemId, int count, Requirement requirement, int caps, int rating) {
        return new Offer(stack(itemId, count), requirement, caps, rating);
    }

    private static ItemStack stack(String itemId, int count) {
        return new ItemStack(requiredItem(itemId), count);
    }

    private static Item requiredItem(String itemId) {
        ResourceLocation id = ResourceLocation.parse(itemId);
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR && !id.equals(ResourceLocation.withDefaultNamespace("air"))) throw new IllegalStateException("Missing Bobmazon offer item " + id);
        return item;
    }

    private static Offer vendingMachineOffer(int variant) {
        Item item = requiredItem("atomiclegacy:vending_machine");
        if (!(item instanceof VendingMachineBlockItem vending)) throw new IllegalStateException("Vending-machine Bobmazon offer resolved to " + item);
        return new Offer(LegacyVariantBlockItem.stackFor(vending, variant), Requirement.CHEMICS, 64, 0);
    }
}
