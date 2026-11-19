package com.atomiclegacy.items;

import com.atomiclegacy.render.ObjMachineItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Block item that keeps placement vanilla while rendering the machine's complete OBJ assembly. */
public class ObjMachineBlockItem extends BlockItem {
    public ObjMachineBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /**
     * [parity m1] Items drawn by their 1.7.10 IItemRenderer (U4LegacyItemRenderSetup / M1LegacyItemRenderers) or, for
     * renderType -1 blocks without one, by the flat block icon; they must not get the OBJ renderer as well.
     */
    public static final java.util.Set<String> LEGACY_RENDERED_IDS = java.util.Set.of(
            "machine_ammo_press", "machine_blast_furnace", "sat_dock", "chimney_brick", "chimney_industrial",
            "machine_boiler", "machine_industrial_boiler", "machine_condenser_powered", "machine_tower_small",
            "machine_tower_large", "boat", "skeleton_holder");

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        installRenderer(this, consumer);
    }

    public static void installRenderer(Item item, Consumer<IClientItemExtensions> consumer) {
        if (LEGACY_RENDERED_IDS.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath())) {
            return;
        }
        installRenderer(consumer);
    }

    public static void installRenderer(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final ObjMachineItemRenderer renderer = new ObjMachineItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}
