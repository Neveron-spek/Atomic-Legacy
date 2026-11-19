package com.atomiclegacy.items;

import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.config.CustomMachineConfigJSON.MachineConfiguration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * [parity X1: custom machines] ItemCustomMachine: one creative stack per configuration (niceList order), the display
 * name is the configuration's localization for the client language or its localizedName, otherwise
 * "INVALID MACHINE CONTROLLER".
 */
public class CustomMachineItem extends BlockItem {
    public CustomMachineItem(Block block, Properties properties) {
        super(block, properties);
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (MachineConfiguration config : CustomMachineConfigJSON.niceList) {
            output.accept(CustomMachineConfigJSON.stackFor(config));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        MachineConfiguration conf = CustomMachineConfigJSON.configFor(stack);

        if (conf != null) {
            String localized = CustomMachineConfigJSON.localized(conf, languageCode());
            return Component.literal(localized != null ? localized : conf.localizedName);
        }

        return Component.literal("INVALID MACHINE CONTROLLER");
    }

    /** MainRegistry.proxy.getLanguageCode(): the client's language, "en_US" on the server proxy. */
    public static String languageCode() {
        if (FMLEnvironment.dist == Dist.CLIENT) return ClientLanguage.get();
        return "en_US";
    }

    private static final class ClientLanguage {
        static String get() {
            return net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected();
        }
    }
}
