package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** [parity I1] 1.7.10 ArmorFSB step/jump/fall handling and HbmPlayerProps.plink. */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
public final class LegacyArmorParity {
    /** HbmPlayerProps.plinkCooldownLength; the cooldown was decremented once per player tick. */
    public static final int PLINK_COOLDOWN_LENGTH = 10;
    /** DamageSource.causePlayerDamage(player).setDamageBypassesArmor() of ArmorFSB.handleFall. */
    public static final ResourceKey<DamageType> HARD_LANDING =
            ResourceKey.create(Registries.DAMAGE_TYPE, AtomicLegacy.id("hard_landing"));
    private static final Map<UUID, Long> NEXT_PLINK = new ConcurrentHashMap<>();
    /** Client-side "hfr_nextStepDistance" of ArmorFSB.steppy. */
    private static final Map<Player, Float> NEXT_STEP = new WeakHashMap<>();

    private LegacyArmorParity() {
    }

    /** [e3] logout cleanup: the stored tick belongs to the world the player left. */
    public static void clearPlink(Player player) {
        NEXT_PLINK.remove(player.getUUID());
    }

    /** HbmPlayerProps.plink(player, "random.break", 0.5F, 1.0F + rand * 0.5F) used by ArmorDNT and ArmorTrenchmaster. */
    public static void plink(Player player) {
        long now = player.level().getGameTime();
        Long next = NEXT_PLINK.get(player.getUUID());
        if (next != null && now < next) {
            return;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK,
                SoundSource.PLAYERS, 0.5F, 1.0F + player.getRandom().nextFloat() * 0.5F);
        NEXT_PLINK.put(player.getUUID(), now + PLINK_COOLDOWN_LENGTH);
    }

    /** ArmorFSB.step (setStep) of the chestplate's set. */
    public static Supplier<SoundEvent> stepSound(String group) {
        return switch (group) {
            case "t51", "ajr", "ajro", "blackjack", "fau", "dns", "liquidator" -> HbmSoundEvents.STEP_METAL::get;
            case "rpa", "ncrpa" -> HbmSoundEvents.STEP_POWERED::get;
            default -> null;
        };
    }

    /** ArmorFSB.jump (setJump). */
    public static Supplier<SoundEvent> jumpSound(String group) {
        return switch (group) {
            case "t51", "ajr", "ajro", "blackjack", "fau", "dns", "liquidator" -> HbmSoundEvents.STEP_IRON_JUMP::get;
            case "rpa", "ncrpa" -> HbmSoundEvents.STEP_POWERED::get;
            default -> null;
        };
    }

    /** ArmorFSB.fall (setFall). */
    public static Supplier<SoundEvent> fallSound(String group) {
        return switch (group) {
            case "t51", "ajr", "ajro", "blackjack", "fau", "dns", "liquidator" -> HbmSoundEvents.STEP_IRON_LAND::get;
            case "rpa", "ncrpa" -> HbmSoundEvents.STEP_POWERED::get;
            default -> null;
        };
    }

    /** ArmorFSB.handleFall (LivingFallEvent / PlayerFlyableFallEvent). Fall damage itself is not touched. */
    public static void handleFall(Player player, float fallDistance) {
        String group = ArmorFSBItem.activeSetGroup(player);
        if (group.isEmpty()) {
            return;
        }
        if (ArmorFSBItem.hasFeature(player, ArmorFSBItem.Feature.HARD_LANDING) && fallDistance > 10) {
            DamageSource source = new DamageSource(player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HARD_LANDING), player);
            for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(3, 0, 3))) {
                if (e instanceof ItemEntity) {
                    continue;
                }
                Vec3 vec = new Vec3(player.getX() - e.getX(), 0, player.getZ() - e.getZ());
                if (vec.length() < 3) {
                    double intensity = 3 - vec.length();
                    e.setDeltaMovement(e.getDeltaMovement().add(vec.x * intensity * -2, 0.1D * intensity, vec.z * intensity * -2));
                    e.hurtMarked = true;
                    e.hurt(source, (float) (intensity * 10));
                }
            }
        }
        Supplier<SoundEvent> fall = fallSound(group);
        if (fall != null && fallDistance > 0.5) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), fall.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    /** ArmorFSB.handleJump (LivingJumpEvent). */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        Supplier<SoundEvent> jump = jumpSound(ArmorFSBItem.activeSetGroup(player));
        if (jump != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), jump.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    /** ArmorFSB.handleTick -> steppy: client side, once per vanilla footstep while on the ground. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            return;
        }
        Supplier<SoundEvent> step = player.onGround() ? stepSound(ArmorFSBItem.activeSetGroup(player)) : null;
        if (step == null) {
            return;
        }
        float walked = player.moveDist;
        Float next = NEXT_STEP.get(player);
        if (next == null || walked < next - 1.0F) {
            NEXT_STEP.put(player, (float) Mth.floor(walked) + 1.0F);
            return;
        }
        if (walked >= next) {
            BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2D, player.getZ());
            if (!player.level().getBlockState(below).isAir()) {
                player.playSound(step.get(), 0.25F, 1.0F);
            }
            NEXT_STEP.put(player, (float) Mth.floor(walked) + 1.0F);
        }
    }
}
