package com.atomiclegacy.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.atomiclegacy.render.RenderAccessoryUtility;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * [parity j2] ModEventHandlerClient#preRenderEvent: player.func_152121_a(Type.CAPE, getCloakFromPlayer(player)) replaced
 * the player's cape location whenever RenderAccessoryUtility returned one. 1.21.1 reads the cape from getSkin().
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerCapeMixin {

	@Inject(method = "getSkin()Lnet/minecraft/client/resources/PlayerSkin;", at = @At("RETURN"), cancellable = true)
	private void atomiclegacy$legacyCape(CallbackInfoReturnable<PlayerSkin> cir) {
		ResourceLocation cloak = RenderAccessoryUtility.getCloakFromPlayer((AbstractClientPlayer) (Object) this);
		if(cloak == null) return;
		PlayerSkin skin = cir.getReturnValue();
		cir.setReturnValue(new PlayerSkin(skin.texture(), skin.textureUrl(), cloak, skin.elytraTexture(), skin.model(), skin.secure()));
	}
}
