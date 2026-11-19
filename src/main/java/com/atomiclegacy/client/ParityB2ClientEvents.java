package com.atomiclegacy.client;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes;
import com.atomiclegacy.worldgen.CraterBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** [parity b2] Client-side hooks of the b2 batch. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class ParityB2ClientEvents {
    private ParityB2ClientEvents() {
    }

    /** ModEventHandler#onPlayerLeftClient: SerializableRecipe.clearReceivedRecipes(). */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CustomMachineRecipes.clearReceivedRecipes();
    }

    /**
     * EntityEffectHandler.onUpdate, client player branch: three "townaura" particles (mycelium in 1.21) per tick
     * around the player while standing in the crater or inner crater biome.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) return;
        Holder<Biome> biome = mc.level.getBiome(player.blockPosition());
        if (biome.is(CraterBiomes.CRATER) || biome.is(CraterBiomes.CRATER_INNER)) {
            RandomSource rand = player.getRandom();
            for (int i = 0; i < 3; i++) {
                mc.level.addParticle(ParticleTypes.MYCELIUM, player.getX() + rand.nextGaussian() * 3, player.getY() + rand.nextGaussian() * 2,
                        player.getZ() + rand.nextGaussian() * 3, 0, 0, 0);
            }
        }
    }
}
