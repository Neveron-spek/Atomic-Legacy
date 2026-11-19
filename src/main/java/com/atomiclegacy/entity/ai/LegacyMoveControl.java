package com.atomiclegacy.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * [parity J1] 1.7.10 EntityMoveHelper.onUpdateMoveHelper for new-AI mobs: moveForward is cleared every tick; a
 * pending move turns the yaw by at most 30 degrees per tick, sets the AI move speed (speed * movementSpeed) and
 * jumps only when the waypoint is higher than floor(minY + 0.5) and closer than one block horizontally.
 * The strafe operation (no 1.7.10 counterpart) keeps the vanilla handling.
 */
public class LegacyMoveControl extends MoveControl {
    public LegacyMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        if (this.operation == Operation.STRAFE || this.operation == Operation.JUMPING) {
            super.tick();
            return;
        }
        this.mob.setZza(0.0F);
        if (this.operation != Operation.MOVE_TO) {
            return;
        }
        this.operation = Operation.WAIT;
        int feet = Mth.floor(this.mob.getBoundingBox().minY + 0.5D);
        double dx = this.wantedX - this.mob.getX();
        double dz = this.wantedZ - this.mob.getZ();
        double dy = this.wantedY - feet;
        double distance = dx * dx + dy * dy + dz * dz;
        if (distance >= 2.500000277905201E-7D) {
            float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 30.0F));
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (dy > 0.0D && dx * dx + dz * dz < 1.0D) {
                this.mob.getJumpControl().jump();
            }
        }
    }
}
