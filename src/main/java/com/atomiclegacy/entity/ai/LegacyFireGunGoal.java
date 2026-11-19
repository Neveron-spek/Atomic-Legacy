package com.atomiclegacy.entity.ai;

import com.atomiclegacy.items.weapon.sedna.GunConfig;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunKeybind;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.atomiclegacy.items.weapon.sedna.Receiver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * 1.7.10 com.hbm.entity.mob.ai.EntityAIFireGun, line for line. The old task
 * kept the default mutex bits (0), so this goal claims no flags and runs next
 * to every other goal; updateTask ran every tick.
 */
public class LegacyFireGunGoal extends Goal {

	private final Mob host;

	public double attackMoveSpeed = 1.0D; // how fast we move while in this state
	public double maxRange = 20; // how far our target can be before we stop shooting
	public int burstTime = 10; // maximum number of ticks in a burst (for automatic weapons)
	public int minWait = 10; // minimum number of ticks to wait between bursts/shots
	public int maxWait = 40; // maximum number of ticks to wait between bursts/shots
	public float inaccuracy = 30; // how many degrees of inaccuracy does the AI have
	public boolean randomBurst = true; //whether the burst time should be fixed or random
	// state timers
	private int attackTimer = 0;
	private FireState state = FireState.IDLE;
	private int stateTimer = 0;

	private static enum FireState {
		IDLE,
		WAIT,
		FIRING,
		RELOADING,
	}

	public LegacyFireGunGoal(Mob host) {
		this.host = host;
		this.setFlags(EnumSet.noneOf(Goal.Flag.class));
	}

	@Override
	public boolean canUse() {
		return host.getTarget() != null && getYerGun() != null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = host.getTarget();
		ItemStack stack = host.getMainHandItem();
		ItemGunBaseNT gun = getYerGun();

		// The modern selector only re-checks canContinueToUse every other tick;
		// the old one never ran updateTask once shouldExecute had turned false.
		if(target == null || gun == null) return;

		gun.inventoryTick(stack, host.level(), host, 0, true);

		double distanceToTargetSquared = host.distanceToSqr(target.getX(), target.getY(), target.getZ());
		boolean canSeeTarget = host.getSensing().hasLineOfSight(target);

		if(canSeeTarget) {
			attackTimer++;
		} else {
			attackTimer = 0;
		}

		if(distanceToTargetSquared < maxRange * maxRange && attackTimer > 20) {
			host.getNavigation().stop();
		} else {
			host.getNavigation().moveTo(target, attackMoveSpeed);
		}

		host.getLookControl().setLookAt(target, 30.0F, 30.0F);

		stateTimer--;
		if(stateTimer < 0) {
			stateTimer = 0;

			if(state == FireState.WAIT) {
				updateState(FireState.IDLE, 0, gun, stack);
			} else if(state != FireState.IDLE) {
				updateState(FireState.WAIT, host.level().random.nextInt(maxWait - minWait) + minWait, gun, stack);
			}
		} else if(state == FireState.FIRING) {
			// Keep the trigger held throughout the duration of firing
			updateKeybind(gun, stack, GunKeybind.GUN_PRIMARY);
		}

		if(canSeeTarget && distanceToTargetSquared < maxRange * maxRange) {
			if(state == FireState.IDLE) {
				GunConfig config = gun.getConfig(stack, 0);
				Receiver rec = config.getReceivers(stack)[0];
				if(rec.getMagazine(stack).getAmount(stack, null) <= 0) {
					updateState(FireState.RELOADING, 20, gun, stack);
				} else if(ItemGunBaseNT.getState(stack, 0) == GunState.IDLE) {
					int time = randomBurst ? host.level().random.nextInt(burstTime) : burstTime;
					updateState(FireState.FIRING, time, gun, stack);
				}
			}
		}
	}

	// The fall-through is the original's: FIRING presses primary, then reload, then
	// releases everything; RELOADING presses reload and releases it again.
	@SuppressWarnings("fallthrough")
	private void updateState(FireState toState, int time, ItemGunBaseNT gun, ItemStack stack) {
		state = toState;
		stateTimer = time;

		switch(state) {
		case FIRING: updateKeybind(gun, stack, GunKeybind.GUN_PRIMARY);
		case RELOADING: updateKeybind(gun, stack, GunKeybind.RELOAD);
		default: clearKeybinds(gun, stack); break;
		}
	}

	private void clearKeybinds(ItemGunBaseNT gun, ItemStack stack) {
		updateKeybind(gun, stack, null);
	}

	private void updateKeybind(ItemGunBaseNT gun, ItemStack stack, GunKeybind bind) {
		// Turn body to face firing direction, since the gun is attached to that, not the head
		// Also apply accuracy debuff just before firing
		if(bind != null && bind != GunKeybind.RELOAD) {
			float inacc = inaccuracy * (getYerGun().getConfig(stack, 0).getReceivers(stack)[0].getHipfireSpread(stack) * 20);
			host.yHeadRot += (host.level().random.nextFloat() - 0.5F) * inacc;
			host.setXRot(host.getXRot() + (host.level().random.nextFloat() - 0.5F) * inacc);
			host.setYRot(host.yHeadRot);
		}

		gun.handleKeybind(host, null, stack, GunKeybind.GUN_PRIMARY, bind == GunKeybind.GUN_PRIMARY);
		gun.handleKeybind(host, null, stack, GunKeybind.GUN_SECONDARY, bind == GunKeybind.GUN_SECONDARY);
		gun.handleKeybind(host, null, stack, GunKeybind.GUN_TERTIARY, bind == GunKeybind.GUN_TERTIARY);
		gun.handleKeybind(host, null, stack, GunKeybind.RELOAD, bind == GunKeybind.RELOAD);
	}

	public ItemGunBaseNT getYerGun() {
		ItemStack stack = host.getMainHandItem();

		if(stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return null;

		return (ItemGunBaseNT) stack.getItem();
	}

}
