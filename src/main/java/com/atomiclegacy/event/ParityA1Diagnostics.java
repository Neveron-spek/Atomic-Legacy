package com.atomiclegacy.event;

import com.atomiclegacy.blocks.LegacyVariantBlock;
import com.atomiclegacy.entity.LegacyTurbineBladeEntity;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmDamageTypes;
import com.atomiclegacy.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * [parity A1: diagnostics] Radiation sources outside contaminate (stone_porous has no invented absorption, rad_absorber
 * tiers), the TURBINE warhead blade (EntityBulletBaseNT with BulletConfigSyncingUtil.TURBINE) and its damage type.
 */
final class ParityA1Diagnostics {
    private ParityA1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            porous(level, problems, cases);
            absorber(level, problems, cases);
            bladeRegistration(level, problems, cases);
            bladeFlight(level, problems, cases);
            bladeBlock(level, problems, cases);
            bladeEntity(level, problems, cases);
        } catch (RuntimeException e) {
            problems.add("A1 diagnostics threw " + e);
        }
        return cases[0];
    }

    private static BlockPos base(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos pos = new BlockPos(spawn.getX() + 208, 250, spawn.getZ() - 208);
        level.getChunkAt(pos);
        return pos;
    }

    private static void clear(ServerLevel level, BlockPos from, BlockPos to) {
        for (BlockPos p : BlockPos.betweenClosed(from, to)) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** BlockPorous extends BlockStone: no tick, no radiation; drops cobblestone, silk touch smooth stone. */
    private static void porous(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = base(level);
        Block porous = HbmBlocks.STONE_POROUS.get();
        level.setBlock(pos, porous.defaultBlockState(), Block.UPDATE_ALL);
        ChunkRadiationData rad = ChunkRadiationData.get(level);
        rad.setRadiation(pos, 100.0D);
        check(!level.getBlockTicks().hasScheduledTick(pos, porous) && !porous.defaultBlockState().isRandomlyTicking(),
                problems, cases, "stone_porous schedules no tick and does not tick randomly");
        porous.defaultBlockState().tick(level, pos, level.random);
        check(Math.abs(rad.getRadiation(pos) - 100.0D) < 1.0E-6D, problems, cases,
                "stone_porous does not absorb chunk radiation (got " + rad.getRadiation(pos) + ")");
        rad.setRadiation(pos, 0.0D);

        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        List<ItemStack> drops = Block.getDrops(porous.defaultBlockState(), level, pos, null, null, pick);
        check(drops.size() == 1 && drops.get(0).is(Items.COBBLESTONE) && drops.get(0).getCount() == 1, problems, cases,
                "stone_porous drops 1 cobblestone (BlockStone.getItemDropped) got " + drops);
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        drops = Block.getDrops(porous.defaultBlockState(), level, pos, null, null, silk);
        check(drops.size() == 1 && drops.get(0).is(Items.STONE), problems, cases,
                "stone_porous silk touch drops smooth stone (createStackedBlock) got " + drops);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** BlockAbsorber: decrementRad 2.5 / 10 / 100 / 10000 every 10 ticks. */
    private static void absorber(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = base(level).above(2);
        ChunkRadiationData rad = ChunkRadiationData.get(level);
        float[] amounts = {2.5F, 10.0F, 100.0F, 10_000.0F};
        for (int tier = 0; tier < amounts.length; tier++) {
            BlockState state = HbmBlocks.RAD_ABSORBER.get().defaultBlockState().setValue(LegacyVariantBlock.VARIANT, tier);
            level.setBlock(pos, state, Block.UPDATE_ALL);
            rad.setRadiation(pos, 20_000.0D);
            state.tick(level, pos, level.random);
            check(Math.abs(rad.getRadiation(pos) - (20_000.0D - amounts[tier])) < 1.0E-3D, problems, cases,
                    "rad_absorber tier " + tier + " absorbs " + amounts[tier] + " (got " + (20_000.0D - rad.getRadiation(pos)) + ")");
        }
        rad.setRadiation(pos, 0.0D);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void bladeRegistration(ServerLevel level, List<String> problems, int[] cases) {
        EntityType<?> type = HbmEntityTypes.LEGACY_TURBINE_BLADE.get();
        check(type.getWidth() == 0.5F && type.getHeight() == 0.5F && type.clientTrackingRange() == 16
                        && type.updateInterval() == 1, problems, cases,
                "entity_bullet_mk3 setSize(0.5, 0.5), tracking 250 blocks, update every tick");
        DamageSource source = level.damageSources().source(HbmDamageTypes.REVOLVER_BULLET);
        check(source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.BYPASSES_ARMOR)
                        && "revolverBullet".equals(source.type().msgId()), problems, cases,
                "revolver_bullet: msgId revolverBullet, projectile, not armor bypassing");
    }

    private static LegacyTurbineBladeEntity blade(ServerLevel level, Vec3 at) {
        LegacyTurbineBladeEntity blade = new LegacyTurbineBladeEntity(level, at.x, at.y, at.z, 0.5D, 0.0D);
        level.addFreshEntity(blade);
        return blade;
    }

    private static void tick(LegacyTurbineBladeEntity e) {
        e.setOldPosAndRot();
        e.tickCount++;
        e.tick();
    }

    /** velocity 1, gravity 0, drag 1, maxAge 200 (dies on the 202nd update, ticksExisted tested before increment). */
    private static void bladeFlight(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos b = base(level).above(20);
        clear(level, b.offset(-1, -2, -2), b.offset(4, 2, 2));
        LegacyTurbineBladeEntity blade = blade(level, Vec3.atCenterOf(b));
        double x0 = blade.getX();
        for (int i = 0; i < 4; i++) tick(blade);
        check(Math.abs(blade.getX() - x0 - 2.0D) < 1.0E-9D && Math.abs(blade.getY() - (b.getY() + 0.5D)) < 1.0E-9D,
                problems, cases, "blade moves 0.5 blocks/tick without gravity (dx=" + (blade.getX() - x0) + ")");
        blade.setDeltaMovement(0.0D, 0.0D, 0.0D);
        while (blade.tickCount < 201 && !blade.isRemoved()) tick(blade);
        check(!blade.isRemoved(), problems, cases, "blade alive at update 201");
        tick(blade);
        check(blade.isRemoved(), problems, cases, "blade despawns at update 202 (ticksExisted > maxAge 200)");
        blade.discard();
    }

    /** onBlockImpact: destroysBlocks (hardness <= 120, no drop), then setDead. */
    private static void bladeBlock(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos b = base(level).above(30);
        clear(level, b.offset(-1, -2, -2), b.offset(6, 2, 2));
        BlockPos wall = b.east(2);
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        LegacyTurbineBladeEntity blade = blade(level, Vec3.atCenterOf(b));
        for (int i = 0; i < 6 && !blade.isRemoved(); i++) tick(blade);
        check(blade.isRemoved() && level.getBlockState(wall).isAir(), problems, cases,
                "blade breaks stone on impact and dies");
        check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(wall).inflate(2.0D)).isEmpty(), problems, cases,
                "func_147480_a(x, y, z, false): no block drop");
        level.setBlock(wall, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
        blade = blade(level, Vec3.atCenterOf(b));
        for (int i = 0; i < 6 && !blade.isRemoved(); i++) tick(blade);
        check(blade.isRemoved() && level.getBlockState(wall).isAir(), problems, cases,
                "blade breaks obsidian (hardness 50 <= 120)");
        clear(level, wall, wall);
    }

    /** Nearest entity: damage rand(100..150) with revolverBullet, bullet dies. */
    private static void bladeEntity(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos b = base(level).above(40);
        clear(level, b.offset(-1, -2, -2), b.offset(6, 2, 2));
        Pig pig = EntityType.PIG.create(level);
        if (pig == null) {
            problems.add("A1 could not create pig");
            return;
        }
        pig.setNoAi(true);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0D);
        pig.setHealth(1000.0F);
        pig.moveTo(b.getX() + 2.5D, b.getY(), b.getZ() + 0.5D, 0.0F, 0.0F);
        pig.setNoGravity(true);
        level.addFreshEntity(pig);
        LegacyTurbineBladeEntity blade = blade(level, Vec3.atCenterOf(b));
        for (int i = 0; i < 6 && !blade.isRemoved(); i++) tick(blade);
        float dealt = 1000.0F - pig.getHealth();
        check(blade.isRemoved() && dealt >= 100.0F && dealt <= 150.0F && pig.getLastDamageSource() != null
                        && pig.getLastDamageSource().is(HbmDamageTypes.REVOLVER_BULLET), problems, cases,
                "blade hits pig for 100..150 revolverBullet and dies (dealt " + dealt + ")");
        pig.discard();
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add("A1 " + message);
    }
}
