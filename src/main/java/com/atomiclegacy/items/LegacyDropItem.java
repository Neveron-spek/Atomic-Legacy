package com.atomiclegacy.items;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.entity.LegacyBlackHoleEntity;
import com.atomiclegacy.entity.LegacyProjectileUtil;
import com.atomiclegacy.explosion.LegacyChaosEffects;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.entity.LegacyVortexEntity;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** ItemDrop behavior for the remaining destructive 1.7.10 standalone items. */
public final class LegacyDropItem extends Item {
    private static final String TARGET_KEY = "target";

    public enum Kind {
        BETA, BLACK_HOLE, DETONATOR_DE, DETONATOR_DEADMAN, PELLET_ANTIMATTER,
        CRYSTAL_XEN, SINGULARITY, SINGULARITY_COUNTER_RESONANT, SINGULARITY_SUPER_HEATED,
        SINGULARITY_SPARK
    }

    private final Kind kind;

    public LegacyDropItem(Kind kind) {
        super(kind == Kind.PELLET_ANTIMATTER ? new Properties() : new Properties().stacksTo(1));
        this.kind = kind;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (this.kind == Kind.BETA) {
            entity.discard();
            return true;
        }
        if (level.isClientSide) {
            return false;
        }
        if (this.kind == Kind.DETONATOR_DE) {
            // createExplosion(entityItem, x, y, z, 15.0F, true): smoking, not flaming
            if (level instanceof ServerLevel server && com.atomiclegacy.config.HbmServerParityConfig.dropDead()) { // [parity S2: configs] WeaponConfig.dropDead
                com.atomiclegacy.explosion.LegacyVnt.vanilla(server, entity, entity.getX(), entity.getY(), entity.getZ(), 15.0F, false, true);
            }
            entity.discard();
            return true;
        }
        if (this.kind == Kind.DETONATOR_DEADMAN) {
            detonateDeadman(level, entity, stack);
            entity.discard();
            return true;
        }
        if (!entity.onGround()) {
            return false;
        }

        switch (this.kind) {
            case PELLET_ANTIMATTER -> {
                if (HbmConfig.DROPPED_ANTIMATTER_CELL_EFFECT.get()) {
                    LegacyProjectileUtil.antimatterExplosion(
                            (ServerLevel) level, entity.position(), 20.0F, entity);
                }
            }
            case CRYSTAL_XEN -> {
                if (level instanceof ServerLevel serverLevel && HbmConfig.DROPPED_XEN_CRYSTAL_EFFECT.get()) {
                    LegacyChaosEffects.floatAndLift(serverLevel, entity.blockPosition());
                }
            }
            case SINGULARITY -> {
                if (HbmConfig.DROPPED_SINGULARITY_EFFECT.get()) {
                    LegacyVortexEntity.spawn(level, entity.position(), 1.5F, 0.0025F, false);
                }
            }
            case SINGULARITY_COUNTER_RESONANT, SINGULARITY_SUPER_HEATED -> {
                if (HbmConfig.DROPPED_SINGULARITY_EFFECT.get()) {
                    LegacyVortexEntity.spawn(level, entity.position(), 2.5F, 0.0025F, false);
                }
            }
            case BLACK_HOLE -> {
                if (HbmConfig.DROPPED_SINGULARITY_EFFECT.get()) {
                    LegacyBlackHoleEntity.spawn(level, entity.position(), 1.5F, false);
                }
            }
            case SINGULARITY_SPARK -> {
                if (HbmConfig.DROPPED_SINGULARITY_EFFECT.get()) {
                    LegacyBlackHoleEntity.spawnRaging(level, entity.position(), 3.5F);
                }
            }
            default -> {
                return false;
            }
        }
        entity.discard();
        return true;
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (this.kind != Kind.DETONATOR_DEADMAN || context.getPlayer() == null
                || !context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        BlockPos target = context.getClickedPos();
        root.putLong(TARGET_KEY, target.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        // ItemDrop.onItemUse: "Position set!" is added on the client only; hbm:item.techBoop at 2.0F
        if (context.getLevel().isClientSide) {
            context.getPlayer().displayClientMessage(Component.literal("Position set!"), false);
        }
        context.getLevel().playSound(null, context.getPlayer().getX(), context.getPlayer().getY(),
                context.getPlayer().getZ(), com.atomiclegacy.registry.HbmSoundEvents.TECH_BOOP.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static void detonateDeadman(Level level, ItemEntity entity, ItemStack stack) {
        detonateDeadmanTarget(level, stack);
        level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 0.0F, false, Level.ExplosionInteraction.NONE);
    }

    /**
     * Detonates the block bound to a dead-man's switch.
     *
     * @return {@code true} when the stack contained a bound target.  The
     * caller must still consume a tagged switch even if the target is no
     * longer a valid bomb, matching the 1.7.10 death hook.
     */
    public static boolean detonateDeadmanTarget(Level level, ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(TARGET_KEY)) {
            return false;
        }
        BlockPos target = BlockPos.of(root.getLong(TARGET_KEY));
        LegacyDetonatorItem.detonate(level, target, null);
        return true;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return switch (this.kind) {
            case SINGULARITY, SINGULARITY_COUNTER_RESONANT, SINGULARITY_SUPER_HEATED,
                    BLACK_HOLE, SINGULARITY_SPARK, PELLET_ANTIMATTER -> true;
            default -> false;
        };
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return switch (this.kind) {
            case SINGULARITY, SINGULARITY_COUNTER_RESONANT, SINGULARITY_SUPER_HEATED,
                    BLACK_HOLE, SINGULARITY_SPARK -> new ItemStack(HbmItems.NUCLEAR_WASTE.get());
            case PELLET_ANTIMATTER -> new ItemStack(HbmItems.CELL_EMPTY.get());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (this.kind) {
            case SINGULARITY -> {
                tooltip.add(Component.literal("You may be asking:"));
                tooltip.add(Component.literal("\"But HBM, a manifold with an undefined"));
                tooltip.add(Component.literal("state of spacetime? How is this possible?\""));
                tooltip.add(Component.literal("Long answer short:"));
                tooltip.add(Component.literal("\"I have no idea!\""));
            }
            case SINGULARITY_COUNTER_RESONANT -> {
                tooltip.add(Component.literal("Nullifies resonance of objects in"));
                tooltip.add(Component.literal("non-euclidean space, creates variable"));
                tooltip.add(Component.literal("gravity well. Spontaneously spawns"));
                tooltip.add(Component.literal("tesseracts. If a tesseract happens to"));
                tooltip.add(Component.literal("appear near you, do not look directly"));
                tooltip.add(Component.literal("at it."));
            }
            case SINGULARITY_SUPER_HEATED -> {
                tooltip.add(Component.literal("Continuously heats up matter by"));
                tooltip.add(Component.literal("resonating every planck second."));
                tooltip.add(Component.literal("Tends to catch fire or to create"));
                tooltip.add(Component.literal("small plasma arcs. Not edible."));
            }
            case BLACK_HOLE -> {
                tooltip.add(Component.literal("Contains a regular singularity"));
                tooltip.add(Component.literal("in the center. Large enough to"));
                tooltip.add(Component.literal("stay stable. It's not the end"));
                tooltip.add(Component.literal("of the world as we know it,"));
                tooltip.add(Component.literal("and I don't feel fine."));
            }
            case PELLET_ANTIMATTER -> {
                LegacyTooltips.line(tooltip, "Very heavy antimatter cluster.");
                LegacyTooltips.line(tooltip, "Gets rid of black holes.");
            }
            // ItemDrop.addInformation: plain lines followed by the red [trait.drop] line
            case DETONATOR_DE -> {
                tooltip.add(Component.literal("Explodes when dropped!").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("[").append(Component.translatable("trait.atomiclegacy.drop"))
                        .append("]").withStyle(ChatFormatting.RED));
            }
            case DETONATOR_DEADMAN -> {
                tooltip.add(Component.literal("Shift right-click to set position,").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("drop to detonate!").withStyle(ChatFormatting.GRAY));
                CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                if (!root.contains(TARGET_KEY)) {
                    tooltip.add(Component.literal("No position set!").withStyle(ChatFormatting.GRAY));
                } else {
                    BlockPos target = BlockPos.of(root.getLong(TARGET_KEY));
                    tooltip.add(Component.literal("Set pos to " + target.getX() + ", " + target.getY() + ", "
                            + target.getZ()).withStyle(ChatFormatting.GRAY));
                }
                tooltip.add(Component.literal("[").append(Component.translatable("trait.atomiclegacy.drop"))
                        .append("]").withStyle(ChatFormatting.RED));
            }
            default -> {
            }
        }
        if (this.kind != Kind.DETONATOR_DE && this.kind != Kind.DETONATOR_DEADMAN) {
            // ItemDrop.addInformation ends with the red [trait.drop] line for every ItemDrop
            LegacyTooltips.dropTrait(tooltip);
        }
    }
}
