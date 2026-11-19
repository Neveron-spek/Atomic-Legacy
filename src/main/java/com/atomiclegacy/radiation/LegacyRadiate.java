package com.atomiclegacy.radiation;

import com.atomiclegacy.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * [parity K2: radiate] The point-source "radiate" loop shared by 1.7.10 TileEntityStorageDrum, TileEntityZirnoxDestroyed,
 * TileEntityDemonLamp, RBMKDebrisRadiating and LogicBlockActions (and EntityNukeExplosionMK5):
 * <pre>
 * vec = (e.pos + eyeHeight) - source; len = |vec|; res = sum of block explosion resistance at floor(source + dir * i),
 * i = 1 .. len - 1; res = max(res, 1); eRads = rads / res / len^2; ContaminationUtil.contaminate(e, RADIATION, type, eRads)
 * </pre>
 * There is no cap besides the body clamp; the former port versions used an invented sampled "shielding" and a
 * minimum distance of 1 instead.
 */
public final class LegacyRadiate {
    @FunctionalInterface
    public interface Hit {
        void accept(LivingEntity entity, double len);
    }

    private LegacyRadiate() {
    }

    /** Entities in AxisAlignedBB.getBoundingBox(source).expand(range, range, range). */
    public static void radiate(ServerLevel level, Vec3 source, double range, float rads,
                               ContaminationUtil.ContaminationType type, Hit after) {
        radiate(level, source, new AABB(source, source).inflate(range), rads, false, type, after);
    }

    public static void radiate(ServerLevel level, Vec3 source, AABB box, float rads, boolean lenAtLeastOne,
                               ContaminationUtil.ContaminationType type, Hit after) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            double len = dose(level, source, e, rads, lenAtLeastOne, type);
            if (after != null) {
                after.accept(e, len);
            }
        }
    }

    /** One entity; returns len. */
    public static double dose(ServerLevel level, Vec3 source, LivingEntity e, float rads, boolean lenAtLeastOne,
                              ContaminationUtil.ContaminationType type) {
        Vec3 vec = new Vec3(e.getX() - source.x, e.getEyeY() - source.y, e.getZ() - source.z);
        double len = vec.length();
        vec = vec.normalize();
        if (lenAtLeastOne) {
            len = Math.max(len, 1D); // LogicBlockActions
        }
        float res = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 1; i < len; i++) {
            cursor.set((int) Math.floor(source.x + vec.x * i), (int) Math.floor(source.y + vec.y * i),
                    (int) Math.floor(source.z + vec.z * i));
            res += com.atomiclegacy.blocks.LegacyCoreProxy.blockResistance(level, cursor, level.getBlockState(cursor)); // [parity U3: dummy parts]
        }
        if (res < 1) {
            res = 1;
        }
        float eRads = rads;
        eRads /= res;
        eRads /= (float) (len * len);
        ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, type, eRads);
        return len;
    }
}
