package com.atomiclegacy.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationSequence;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 com.hbm.render.anim.HbmAnimations: one animation timer per hotbar
 * slot and gun config index. The GL transform helper is replaced by
 * {@link #getRelevantTransformation} consumers that apply PoseStack operations.
 */
public final class HbmAnimations {

	private HbmAnimations() {
	}

	public static final Animation[][] hotbar = new Animation[9][8]; //now with 8 parallel rails per slot! time to get railed!

	/** Keyframe-based animation system primarily used for guns. */
	public static class Animation {
		//the "name" of the animation slot. if the item has a different key than
		//the animation, the animation will be canceled.
		public String key;
		//the starting time of the animation
		public long startMillis;
		//the animation bus
		public BusAnimation animation;
		// If set, don't cancel this animation when the timer ends, instead wait for the next to start
		public boolean holdLastFrame = false;

		public Animation(String key, long startMillis, BusAnimation animation) {
			this.key = key;
			this.startMillis = startMillis;
			this.animation = animation;
		}

		public Animation(String key, long startMillis, BusAnimation animation, boolean holdLastFrame) {
			this(key, startMillis, animation);
			this.holdLastFrame = holdLastFrame;
		}
	}

	public static void clear() {
		for(Animation[] slot : hotbar) java.util.Arrays.fill(slot, null);
	}

	public static Animation getRelevantAnim() { return getRelevantAnim(0); }

	public static Animation getRelevantAnim(int index) {
		Player player = Minecraft.getInstance().player;
		if(player == null) return null;
		int slot = player.getInventory().selected;
		ItemStack stack = player.getMainHandItem();

		if(stack.isEmpty())
			return null;

		if(slot < 0 || slot > 8) { //for freak of nature hotbars, probably won't work right but at least it doesn't crash
			slot = Math.abs(slot) % 9;
		}

		if(hotbar[slot][index] == null)
			return null;

		if(hotbar[slot][index].key.equals(stack.getItem().getDescriptionId())) {
			return hotbar[slot][index];
		}

		return null;
	}

	public static double[] getRelevantTransformation(String bus) { return getRelevantTransformation(bus, 0); }

	public static double[] getRelevantTransformation(String bus, int index) {

		Animation anim = getRelevantAnim(index);

		if(anim != null) {

			BusAnimation buses = anim.animation;
			int millis = (int)(System.currentTimeMillis() - anim.startMillis);

			BusAnimationSequence seq = buses.getBus(bus);

			if(seq != null) {
				double[] trans = seq.getTransformation(millis);

				if(trans != null)
					return trans;
			}
		}

		return new double[] {
			0, 0, 0, // position
			0, 0, 0, // rotation
			1, 1, 1, // scale
			0, 0, 0, // offset
			0, 1, 2, // XYZ order
		};
	}

	/** HbmAnimations#applyRelevantTransformation: the GL calls applied to the given pose. */
	public static void applyRelevantTransformation(PoseStack pose, String bus) { applyRelevantTransformation(pose, bus, 0); }
	public static void applyRelevantTransformation(PoseStack pose, String bus, int index) {
		double[] transform = getRelevantTransformation(bus, index);
		int[] rot = new int[] { (int)transform[12], (int)transform[13], (int)transform[14] };

		pose.translate(transform[0], transform[1], transform[2]);
		rotate(pose, transform[3 + rot[0]], rot[0]);
		rotate(pose, transform[3 + rot[1]], rot[1]);
		rotate(pose, transform[3 + rot[2]], rot[2]);
		pose.translate(-transform[9], -transform[10], -transform[11]);
		pose.scale((float) transform[6], (float) transform[7], (float) transform[8]);
	}

	/** GL11.glRotated(angle, axis == 0, axis == 1, axis == 2) */
	private static void rotate(PoseStack pose, double angle, int axis) {
		switch(axis) {
		case 0 -> pose.mulPose(Axis.XP.rotationDegrees((float) angle));
		case 1 -> pose.mulPose(Axis.YP.rotationDegrees((float) angle));
		case 2 -> pose.mulPose(Axis.ZP.rotationDegrees((float) angle));
		default -> { } // glRotated with a zero axis vector does nothing
		}
	}
}
