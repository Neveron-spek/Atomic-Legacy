package com.atomiclegacy.registry;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.weapon.sedna.impl.ItemGunChemthrower;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** [weapons batch F1] IFillableItem of gun_chemthrower as the fluid item capability (filled/drained by the tanks of this port). */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GunBatchF1Capabilities {

	private GunBatchF1Capabilities() {
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(
				Capabilities.FluidHandler.ITEM,
				(stack, context) -> stack.getItem() instanceof ItemGunChemthrower gun ? gun.createFluidHandler(stack) : null,
				HbmItems.GUN_CHEMTHROWER.get()
		);
	}
}
