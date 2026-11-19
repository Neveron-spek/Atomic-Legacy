package com.atomiclegacy.blocks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * [parity J1] 1.7.10 Block.onEntityWalking cadence. Entity.moveEntity called it once per step
 * (distanceWalkedOnStepModified > nextStepDistance, then nextStepDistance = (int) distance + 1), never for riding
 * entities or for players sneaking on the ground. Modern Block.stepOn runs every tick and already picks the same
 * block (getOnPosLegacy, 0.2 below the feet, fences/walls/gates below); this gate restores the per-step rate.
 */
public final class LegacyWalkingTrigger {
    private static final Map<Entity, Float> NEXT_STEP = new WeakHashMap<>();

    private LegacyWalkingTrigger() {
    }

    /** True when 1.7.10 would have called onEntityWalking for this stepOn tick (server side). */
    public static synchronized boolean onEntityWalking(Entity entity) {
        if (entity.level().isClientSide || entity.isPassenger()) {
            return false;
        }
        if (entity instanceof Player && entity.onGround() && entity.isShiftKeyDown()) {
            return false;
        }
        float distance = entity.moveDist;
        Float next = NEXT_STEP.get(entity);
        if (next != null && distance <= next) {
            return false;
        }
        NEXT_STEP.put(entity, (float) ((int) distance + 1));
        return true;
    }
}
