package com.atomiclegacy.explosion;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.entity.TimedExplosiveEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * [parity H3] Port of the 1.7.10 {@code com.hbm.explosion.vanillant.ExplosionVNT} pipeline
 * (allocator, entity processor, block processor, effects, in that order) and of
 * {@code com.hbm.explosion.ExplosionNT}, used by mines, charges and bomb blocks.
 *
 * <p>Only the processors those blocks use are available: BlockAllocatorStandard / BlockAllocatorWater,
 * BlockProcessorStandard (default 1/size drop chance, setNoDrop, setAllDrop, setFortune),
 * EntityProcessorStandard and EntityProcessorCrossSmooth (with piercing and range modifier).</p>
 */
public final class LegacyVnt {

    private final ServerLevel level;
    private final Vec3 pos;
    private final float size;
    @Nullable
    private Entity exploder;

    private int resolution;
    private boolean waterAllocator;
    private boolean blockProcessor;
    private float dropChance = -1.0F;
    private int fortune;

    private boolean entityStandard;
    private boolean entityCrossSmooth;
    private double nodeDist;
    private float fixedDamage;
    private float pierceDT;
    private float pierceDR;
    private float rangeMod = 1.0F;

    @Nullable
    private Runnable sfx;

    private LegacyVnt(ServerLevel level, Vec3 pos, float size) {
        this.level = level;
        this.pos = pos;
        this.size = size;
    }

    public static LegacyVnt at(ServerLevel level, Vec3 pos, float size) {
        return new LegacyVnt(level, pos, size);
    }

    public static LegacyVnt at(ServerLevel level, double x, double y, double z, float size) {
        return new LegacyVnt(level, new Vec3(x, y, z), size);
    }

    public LegacyVnt exploder(@Nullable Entity exploder) {
        this.exploder = exploder;
        return this;
    }

    /** BlockAllocatorStandard(resolution); the no-argument constructor used 16. */
    public LegacyVnt allocStandard(int resolution) {
        this.resolution = resolution;
        this.waterAllocator = false;
        return this;
    }

    /** BlockAllocatorWater(resolution): liquids neither absorb power nor get destroyed. */
    public LegacyVnt allocWater(int resolution) {
        this.resolution = resolution;
        this.waterAllocator = true;
        return this;
    }

    /** BlockProcessorStandard with the default drop chance of 1 / size. */
    public LegacyVnt blocks() {
        this.blockProcessor = true;
        return this;
    }

    /** BlockProcessorStandard.setNoDrop(). */
    public LegacyVnt noDrop() {
        this.blockProcessor = true;
        this.dropChance = 0.0F;
        return this;
    }

    /** BlockProcessorStandard.setAllDrop(). */
    public LegacyVnt allDrop() {
        this.blockProcessor = true;
        this.dropChance = 1.0F;
        return this;
    }

    /** BlockProcessorStandard.setFortune(level). */
    public LegacyVnt fortune(int fortune) {
        this.fortune = fortune;
        return this;
    }

    /** EntityProcessorStandard + PlayerProcessorStandard. */
    public LegacyVnt entitiesStandard() {
        this.entityStandard = true;
        this.entityCrossSmooth = false;
        return this;
    }

    /** EntityProcessorCrossSmooth(nodeDist, fixedDamage).setupPiercing(pierceDT, pierceDR) + PlayerProcessorStandard. */
    public LegacyVnt entitiesCrossSmooth(double nodeDist, float fixedDamage, float pierceDT, float pierceDR) {
        this.entityCrossSmooth = true;
        this.entityStandard = false;
        this.nodeDist = nodeDist;
        this.fixedDamage = fixedDamage;
        this.pierceDT = pierceDT;
        this.pierceDR = pierceDR;
        return this;
    }

    /** withRangeMod(mod) of either entity processor. */
    public LegacyVnt rangeMod(float rangeMod) {
        this.rangeMod = rangeMod;
        return this;
    }

    public LegacyVnt sfx(Runnable sfx) {
        this.sfx = sfx;
        return this;
    }

    /** ExplosionVNT.makeStandard(): allocator 16, default drops, standard entities, ExplosionEffectStandard. */
    public LegacyVnt makeStandard() {
        return allocStandard(16).blocks().entitiesStandard().sfx(() -> vanillaEffect(this.level, this.pos));
    }

    /** ExplosionVNT.explode(). Returns the allocated positions (empty without a block allocator). */
    public Set<BlockPos> explode() {
        boolean processBlocks = this.resolution > 1 && this.blockProcessor;
        Set<BlockPos> affected = processBlocks
                ? allocate(this.level, this.pos, this.size, this.resolution, this.waterAllocator)
                : Set.of();
        if (this.entityStandard) {
            standardEntities(this.level, this.exploder, this.pos, this.size * 2.0F * this.rangeMod);
        } else if (this.entityCrossSmooth) {
            LegacyProjectileUtil.landmineDamage(this.level, this.pos, this.size, this.fixedDamage, this.nodeDist,
                    this.pierceDT, this.pierceDR, this.rangeMod);
        }
        if (processBlocks) {
            processBlocks(this.level, this.exploder, this.pos, this.size, affected,
                    this.dropChance < 0.0F ? 1.0F / this.size : this.dropChance, this.fortune);
        }
        if (this.sfx != null) {
            this.sfx.run();
        }
        return affected;
    }

    /**
     * ExplosionNT(world, exploder, x, y, z, size).overrideResolution(resolution).explode() with the
     * NOHURT / NODROP / ALLDROP attributes expressed as arguments. {@code dropChance < 0} is the default 1 / size.
     */
    public static Set<BlockPos> explodeNT(ServerLevel level, @Nullable Entity exploder, Vec3 pos, float size,
                                          int resolution, float dropChance, boolean hurt) {
        Set<BlockPos> affected = allocate(level, pos, size, resolution, false);
        if (hurt) {
            standardEntities(level, exploder, pos, size * 2.0F);
        }
        vanillaEffect(level, pos);
        processBlocks(level, exploder, pos, size, affected, dropChance < 0.0F ? 1.0F / size : dropChance, 0);
        return affected;
    }

    /**
     * 1.7.10 {@code World.newExplosion(entity, x, y, z, size, flaming, smoking)}; {@code createExplosion(e, x, y, z,
     * size, smoking)} is the same call with {@code flaming = false}. Block drops decay with 1 / size as they did then.
     */
    public static void vanilla(ServerLevel level, @Nullable Entity source, double x, double y, double z,
                               float size, boolean flaming, boolean smoking) {
        level.explode(source, x, y, z, size, flaming,
                smoking ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /** ExplosionEffectStandard / ExplosionNT.doExplosionB sound and central particle. */
    public static void vanillaEffect(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** BlockAllocatorStandard / BlockAllocatorWater ray march. */
    public static Set<BlockPos> allocate(ServerLevel level, Vec3 center, float size, int resolution, boolean water) {
        Set<BlockPos> affected = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                for (int k = 0; k < resolution; k++) {
                    if (i != 0 && i != resolution - 1 && j != 0 && j != resolution - 1 && k != 0 && k != resolution - 1) {
                        continue;
                    }
                    double dx = (float) i / (resolution - 1.0F) * 2.0F - 1.0F;
                    double dy = (float) j / (resolution - 1.0F) * 2.0F - 1.0F;
                    double dz = (float) k / (resolution - 1.0F) * 2.0F - 1.0F;
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= length;
                    dy /= length;
                    dz /= length;
                    float power = size * (0.7F + level.random.nextFloat() * 0.6F);
                    double x = center.x;
                    double y = center.y;
                    double z = center.z;
                    for (float step = 0.3F; power > 0.0F; power -= step * 0.75F) {
                        cursor.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
                        if (!level.isInWorldBounds(cursor)) {
                            break;
                        }
                        BlockState state = level.getBlockState(cursor);
                        boolean liquid = water && state.liquid();
                        if (!state.isAir() && !liquid) {
                            power -= (state.getExplosionResistance(level, cursor, null) + 0.3F) * step;
                        }
                        if (power > 0.0F && !state.isAir() && !liquid) {
                            affected.add(cursor.immutable());
                        }
                        x += dx * step;
                        y += dy * step;
                        z += dz * step;
                    }
                }
            }
        }
        return affected;
    }

    /** BlockProcessorStandard.process: per-stack drop roll, then Block.onBlockExploded. */
    // [parity P1: public for ExplosionNT ERRODE / NOSOUND missile paths]
    public static void processBlocks(ServerLevel level, @Nullable Entity exploder, Vec3 center, float size,
                                      Set<BlockPos> affected, float dropChance, int fortune) {
        Explosion compat = new Explosion(level, exploder, center.x, center.y, center.z, size, false,
                Explosion.BlockInteraction.DESTROY);
        ItemStack tool = ItemStack.EMPTY;
        if (fortune > 0) {
            // dropBlockAsItemWithChance(..., fortune): the modern loot tables read fortune from the tool stack.
            tool = new ItemStack(Items.STICK);
            tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FORTUNE), fortune);
        }
        for (BlockPos blockPos : affected) {
            BlockState state = level.getBlockState(blockPos);
            if (state.isAir()) {
                continue;
            }
            if (dropChance > 0.0F && state.canDropFromExplosion(level, blockPos, compat)) {
                BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(blockPos) : null;
                LootParams.Builder params = new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos))
                        .withParameter(LootContextParams.TOOL, tool)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, exploder);
                state.spawnAfterBreak(level, blockPos, tool, false);
                for (ItemStack drop : state.getDrops(params)) {
                    if (level.random.nextFloat() <= dropChance) {
                        Block.popResource(level, blockPos, drop);
                    }
                }
            }
            state.onBlockExploded(level, blockPos, compat);
        }
    }

    /** EntityProcessorStandard.process / ExplosionNT.doExplosionA entity half; {@code range} is already doubled. */
    // [parity P1: public for ExplosionNT missile paths]
    public static void standardEntities(ServerLevel level, @Nullable Entity exploder, Vec3 pos, float range) {
        AABB area = new AABB(pos.x - range - 1.0D, pos.y - range - 1.0D, pos.z - range - 1.0D,
                pos.x + range + 1.0D, pos.y + range + 1.0D, pos.z + range + 1.0D);
        DamageSource source = level.damageSources().explosion(exploder, placedBy(exploder));
        for (Entity entity : level.getEntities(exploder, area)) {
            double distanceScaled = Math.sqrt(entity.distanceToSqr(pos)) / range;
            if (distanceScaled > 1.0D) {
                continue;
            }
            double dx = entity.getX() - pos.x;
            double dy = entity.getEyeY() - pos.y;
            double dz = entity.getZ() - pos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance == 0.0D) {
                continue;
            }
            double density = Explosion.getSeenPercent(pos, entity);
            double knockback = (1.0D - distanceScaled) * density;
            entity.hurt(source, (float) ((int) ((knockback * knockback + knockback) / 2.0D * 8.0D * range + 1.0D)));
            double reduced = knockback;
            if (entity instanceof LivingEntity living) {
                // EnchantmentProtection.func_92092_a: blast protection knockback reduction.
                reduced *= 1.0D - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(
                    dx / distance * reduced, dy / distance * reduced, dz / distance * reduced));
            entity.hurtMarked = true;
        }
    }

    /** Explosion.getExplosivePlacedBy(). */
    @Nullable
    private static LivingEntity placedBy(@Nullable Entity exploder) {
        if (exploder instanceof TimedExplosiveEntity primed) {
            return primed.getOwner() instanceof LivingEntity living ? living : null;
        }
        return exploder instanceof LivingEntity living ? living : null;
    }
}
