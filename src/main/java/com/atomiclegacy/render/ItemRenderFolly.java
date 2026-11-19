package com.atomiclegacy.render;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.client.gun.HbmAnimations;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.mags.IMagazine;
import com.atomiclegacy.registry.HbmSoundEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * [weapons batch F4] 1.7.10 com.hbm.render.item.weapon.sedna.ItemRenderFolly: the gun (texture moonlight.png) and, while
 * fully aimed, the VStarOS boot splash, the TTY lines and the target readout drawn with the font in front of the breech
 * (unlit), plus the vstar jingle played once per aiming session (client only, volume 0.5).
 */
public class ItemRenderFolly extends ItemRenderWeaponBase {

	/** ResourceManager.folly (models/weapons/folly.obj), folly_tex (textures/models/weapons/moonlight.png) */
	static final LegacyEntityObjMesh folly = LegacyEntityObjMesh.load(AtomicLegacy.id("models/weapons/folly.obj"));
	public static final ResourceLocation folly_tex = AtomicLegacy.id("textures/models/weapons/moonlight.png");

	public static long timeAiming;
	public static boolean jingle = false;
	public static boolean wasAiming = false;

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2F : 2.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		pose.translate(0, 0, 0.875);

		float offset = 0.8F;
		float aim = 0.75F;
		standardAimingTransform(pose, stack,
				-2.5F * offset, -1.5F * offset, 2.75F * offset,
				-2 * aim, -1 * aim, 2.25F * offset);
	}

	private VertexConsumer consumer() {
		return buffers.getBuffer(RenderType.entityCutout(folly_tex));
	}

	private void part(VertexConsumer vc, String name) {
		folly.renderGroup(name, pose, vc, light, overlay, 0xFFFFFFFF);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		LocalPlayer player = Minecraft.getInstance().player;
		VertexConsumer vc = consumer();
		double scale = 0.75D;
		pose.scale((float) scale, (float) scale, (float) scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] load = HbmAnimations.getRelevantTransformation("LOAD");
		double[] shell = HbmAnimations.getRelevantTransformation("SHELL");
		double[] screw = HbmAnimations.getRelevantTransformation("SCREW");
		double[] breech = HbmAnimations.getRelevantTransformation("BREECH");

		pose.translate(0, 1, -4);
		glRotate(pose, -equip[0], 1, 0, 0);
		pose.translate(0, -1, 4);

		pose.translate(0, -2, -2);
		glRotate(pose, load[0], 1, 0, 0);
		pose.translate(0, 2, 2);

		part(vc, "Cannon");

		pose.pushPose();
		pose.translate(recoil[0], recoil[1], recoil[2]);
		part(vc, "Barrel");
		pose.popPose();

		pose.pushPose();
		pose.translate(shell[0], shell[1], shell[2]);
		part(vc, "Shell");
		pose.popPose();

		pose.pushPose();
		pose.translate(breech[0], breech[1], breech[2]);
		part(vc, "Breech");
		pose.translate(0, 1, 0);
		glRotate(pose, screw[2], 0, 0, 1);
		pose.translate(0, -1, 0);
		part(vc, "Cog");
		pose.popPose();

		boolean isAiming = ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F;
		if(isAiming & !wasAiming) timeAiming = System.currentTimeMillis();

		if(isAiming) {

			String splash = getBootSplash();

			if(!jingle && !splash.isEmpty()) {
				// MainRegistry.proxy.playSoundClient(player, GUN_VYLET_PONY_CUTIEMARKS_AND_THE_THINGS_THAT_BIND_US_INTRO_JINGLE, 0.5F, 1F)
				player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), HbmSoundEvents.GUN_VSTAR_JINGLE.get(), SoundSource.PLAYERS, 0.5F, 1F, false);
				jingle = true;
			}

			Font font = Minecraft.getInstance().font;
			float variance = 0.85F + player.getRandom().nextFloat() * 0.15F;
			int color = 0xFF000000 | ((int) (variance * 255F) << 16) | ((int) (variance * 0.5F * 255F) << 8);

			if(System.currentTimeMillis() - timeAiming > 5000 && load[0] == 0) {
				IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
				String msg = mag.getAmount(stack, player.getInventory()) > 0 ? "+" : "No ammo";
				pose.pushPose();
				float crosshairSize = 0.01F;
				pose.translate((font.width(msg) / 2) * crosshairSize + 2, 1F + font.lineHeight * crosshairSize / 2F, -2.75F);
				pose.scale(crosshairSize, -crosshairSize, crosshairSize);
				glRotate(pose, 180D, 0, 1, 0);
				font.drawInBatch(msg, 0, 0, color, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
				pose.popPose();
			}

			pose.pushPose();
			float splashSize = 0.02F;
			pose.translate((font.width(splash) / 2) * splashSize + 2, 1F + font.lineHeight * splashSize / 2F, -2.75F);
			pose.scale(splashSize, -splashSize, splashSize);
			glRotate(pose, 180D, 0, 1, 0);
			font.drawInBatch(splash, 0, 0, color, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
			pose.popPose();

			List<String> tty = getTTY();
			if(!tty.isEmpty()) {
				pose.pushPose();
				float fontSize = 0.005F;
				pose.translate(2.5F, 1.375F, -2.75F);
				pose.scale(fontSize, -fontSize, fontSize);
				glRotate(pose, 180D, 0, 1, 0);
				for(String line : tty) {
					font.drawInBatch(line, 0, 0, color, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
					pose.translate(0, (font.lineHeight + 2), 0);
				}
				pose.popPose();
			}
		} else {
			jingle = false;
		}

		wasAiming = isAiming;
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 3D;
		pose.scale((float) scale, (float) scale, (float) scale);
		pose.translate(-0.25, 0.5, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 25, 1, 0, 0);
		glRotate(pose, 45, 0, 1, 0);
		pose.translate(0, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		pose.scale((float) scale, (float) scale, (float) scale);
		glRotate(pose, 90, 0, 1, 0);
		pose.translate(0, -1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, LegacyRenderType type, LivingEntity ent) {
		folly.renderAll(pose, consumer(), light, overlay, 0xFFFFFFFF);
	}

	public static String getBootSplash() {
		long now = System.currentTimeMillis();
		if(timeAiming + 5000 < now) return "";
		if(timeAiming + 3000 > now) return "";
		int splashIndex = (int)((now - timeAiming - 3000) * 35 / 2000) - 10;
		//use the StringBuilder this, can't eat the drywall that, this used to be a free country
		char[] letters = "VStarOS".toCharArray();
		String splash = "";
		for(int i = 0; i < letters.length; i++) {
			if(i < splashIndex - 1) splash += ChatFormatting.LIGHT_PURPLE;
			if(i == splashIndex - 1) splash += ChatFormatting.AQUA;
			if(i == splashIndex) splash += ChatFormatting.WHITE;
			if(i == splashIndex + 1) splash += ChatFormatting.AQUA;
			if(i == splashIndex + 2) splash += ChatFormatting.LIGHT_PURPLE;
			if(i > splashIndex + 2) splash += ChatFormatting.BLACK;
			splash += letters[i];
		}
		return splash;
	}

	public static List<String> getTTY() {
		List<String> tty = new ArrayList<>();
		long now = System.currentTimeMillis();
		int time = (int)((now - timeAiming));
		if(time < 3000) {
			if(time > 250) tty.add(ChatFormatting.GREEN + "POST successful - Code 0");
			if(time > 500) tty.add(ChatFormatting.GREEN + "8,388,608 bytes of RAM installed");
			if(time > 500) tty.add(ChatFormatting.GREEN + "5,187,427 bytes available");
			if(time > 750) tty.add(ChatFormatting.GREEN + "Reticulating splines...");
			if(time > 1500) tty.add(ChatFormatting.GREEN + "No keyboard found!");
			if(time > 2000) tty.add(ChatFormatting.GREEN + "Booting from /dev/sda1...");
		}
		if(time > 5000) {
			Player player = Minecraft.getInstance().player;
			HitResult mop = getMouseOver(player, 250);
			String target = ChatFormatting.GREEN + "Target: ";
			if(mop == null || mop.getType() == HitResult.Type.MISS) target += "N/A";
			if(mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) { BlockPos p = bhr.getBlockPos(); target += p.getX() + "/" + p.getY() + "/" + p.getZ(); }
			if(mop instanceof EntityHitResult ehr) target += ehr.getEntity().getName().getString();
			tty.add(target);
			tty.add(ChatFormatting.GREEN + "Angle: " + ((int)(-player.getXRot() * 100) / 100D));
		}
		return tty;
	}

	/** EntityDamageUtil.getMouseOver(player, distance): the block hit, or the closer pickable entity on the view line. */
	public static HitResult getMouseOver(Player player, double distance) {
		Vec3 eye = player.getEyePosition(interp);
		Vec3 look = player.getViewVector(interp);
		HitResult block = player.pick(distance, interp, false);
		double range = block.getType() == HitResult.Type.MISS ? distance : block.getLocation().distanceTo(eye);
		Vec3 end = eye.add(look.x * range, look.y * range, look.z * range);
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, end, player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D),
				(Entity e) -> !e.isSpectator() && e.isPickable(), range * range);
		return entity != null ? entity : block;
	}
}
