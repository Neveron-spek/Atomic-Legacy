package com.atomiclegacy.handler;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.entity.LegacyGrenadeEntity;
import com.atomiclegacy.entity.UniversalGrenadeEntity;
import com.atomiclegacy.items.IndustrialFertilizerItem;
import com.atomiclegacy.items.UniversalGrenadeItem;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

/** Direct modern registration of the four 1.7.10 HBM dispenser behaviors. */
public final class LegacyDispenserBehaviors {
    private LegacyDispenserBehaviors() {
    }

    public static void register() {
        DispenserBlock.registerBehavior(HbmItems.GRENADE_UNIVERSAL.get(), new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction facing = source.state().getValue(DispenserBlock.FACING);
                double force = UniversalGrenadeItem.shell(stack).throwForce();
                Vec3 direction = Vec3.atLowerCornerOf(facing.getNormal()).scale(force);
                UniversalGrenadeEntity grenade = new UniversalGrenadeEntity(
                        source.level(), stack.copyWithCount(1), null, direction);
                Vec3 center = source.center().add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.75D));
                grenade.setPos(center.x, center.y, center.z);
                source.level().addFreshEntity(grenade);
                stack.shrink(1);
                return stack;
            }
        });

        registerDynamite(HbmItems.STICK_DYNAMITE.get(), LegacyGrenadeEntity.Kind.DYNAMITE);
        registerDynamite(HbmItems.STICK_DYNAMITE_FISHING.get(), LegacyGrenadeEntity.Kind.FISHING);

        Item fertilizer = BuiltInRegistries.ITEM.get(AtomicLegacy.id("powder_fertilizer"));
        DispenserBlock.registerBehavior(fertilizer, new DefaultDispenseItemBehavior() {
            private boolean success;

            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction facing = source.state().getValue(DispenserBlock.FACING);
                BlockPos target = source.pos().relative(facing);
                success = IndustrialFertilizerItem.fertilizeCube(stack, source.level(), target, null, true);
                return stack;
            }

            @Override
            protected void playSound(BlockSource source) {
                source.level().playSound(null, source.pos(),
                        success ? SoundEvents.DISPENSER_DISPENSE : SoundEvents.DISPENSER_FAIL,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        });
    }

    private static void registerDynamite(Item item, LegacyGrenadeEntity.Kind kind) {
        DispenserBlock.registerBehavior(item, new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction facing = source.state().getValue(DispenserBlock.FACING);
                Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
                Vec3 spawn = source.center().add(normal.scale(0.7D));
                // [parity H2: stick_dynamite] BehaviorProjectileDispense: setThrowableHeading(dx, dy + 0.1F, dz, 1.1F, 6.0F)
                // of EntityGrenadeBouncyBase (0.0075 Gaussian spread per unit of inaccuracy, applied before the velocity)
                var random = source.level().random;
                Vec3 motion = new Vec3(normal.x, normal.y + 0.1F, normal.z).normalize().add(
                        random.nextGaussian() * 0.007499999832361937D * 6.0D,
                        random.nextGaussian() * 0.007499999832361937D * 6.0D,
                        random.nextGaussian() * 0.007499999832361937D * 6.0D).scale(1.1F);
                source.level().addFreshEntity(new LegacyGrenadeEntity(source.level(), kind, spawn, motion));
                stack.shrink(1);
                return stack;
            }

            /** BehaviorProjectileDispense#playDispenseSound: playAuxSFX(1002) instead of the click (1000). */
            @Override
            protected void playSound(BlockSource source) {
                source.level().levelEvent(1002, source.pos(), 0);
            }
        });
    }
}
