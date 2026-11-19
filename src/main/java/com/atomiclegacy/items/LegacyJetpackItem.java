package com.atomiclegacy.items;

import com.atomiclegacy.network.ArmorParityPayloads;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.util.ArmorModHandler;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * [parity I2] JetpackBase / JetpackFueledBase / JetpackRegular / JetpackBreak / JetpackVectorized /
 * JetpackBooster of 1.7.10: armor mod for the chestplate's plate_only slot that can also be worn on
 * its own in the chest slot. onArmorTick runs on both sides like the original (the client moves the
 * local player, the server keeps fuel, sound, particles and the flight-kick reset).
 */
public final class LegacyJetpackItem extends ArmorModItem {
    public enum Profile { REGULAR, BREAK, VECTOR, BOOST }

    /** JetpackFueledBase NBT key "fuel". */
    private static final String FUEL = "fuel";
    private static final Set<UUID> INPUT_ACTIVE = ConcurrentHashMap.newKeySet();
    /** Local player's jump key (client only); set by the client tick handler. */
    public static volatile boolean clientJumpHeld;

    private final String fuelName;
    private final int capacity;
    private final Profile profile;

    public LegacyJetpackItem(Properties properties, String fuelName, int capacity, Profile profile) {
        super(properties.stacksTo(1), ArmorModHandler.PLATE_ONLY, false, true, false, false);
        this.fuelName = fuelName;
        this.capacity = capacity;
        this.profile = profile;
    }

    public Profile profile() {
        return this.profile;
    }

    public static int fuel(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getInt(FUEL);
    }

    public int capacity() {
        return this.capacity;
    }

    public String fuelName() {
        return this.fuelName;
    }

    public static void setInputActive(Player player, boolean active) {
        if (active) {
            INPUT_ACTIVE.add(player.getUUID());
        } else {
            INPUT_ACTIVE.remove(player.getUUID());
        }
    }

    private static boolean isInputActive(Player player) {
        return INPUT_ACTIVE.contains(player.getUUID());
    }

    /** EnumKeybind.JETPACK = the jump key. */
    public static boolean isJumpHeld(Player player) {
        return player.level().isClientSide ? clientJumpHeld : isInputActive(player);
    }

    /**
     * Built-in BJ/DNS jetpacks were separate ArmorFSB subclasses in 1.7.10.
     * Their 1.21.1 chest items remain PoweredArmorFSBItem, so keep the flight
     * controller here and share the same network input used by armor-mod packs.
     */
    public static void tickBuiltInJetpack(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof PoweredArmorFSBItem powered)
                || !ArmorFSBItem.hasFSBArmor(player)
                || powered.hbmCharge(chest) <= 0L) {
            return;
        }

        boolean bj = chest.is(HbmItems.BJ_PLATE_JETPACK.get());
        boolean dns = chest.is(HbmItems.DNS_PLATE.get());
        if (!bj && !dns) {
            return;
        }
        boolean thrust = isInputActive(player) || player.getAbilities().flying || player.isFallFlying();
        boolean hover = dns && !player.onGround() && !player.isCrouching();
        if (!thrust && !hover) {
            return;
        }

        var velocity = player.getDeltaMovement();
        if (thrust) {
            double cap = dns ? 0.6D : 0.4D;
            double lift = dns ? 0.2D : 0.1D;
            double y = Math.min(cap, velocity.y + lift);
            if (dns) {
                var look = player.getLookAngle();
                velocity = velocity.add(look.x * 0.08D, 0.0D, look.z * 0.08D);
            }
            player.setDeltaMovement(velocity.x, y, velocity.z);
        } else if (velocity.y < 0.0D) {
            player.setDeltaMovement(velocity.x, Math.max(dns ? -0.1D : -0.08D, velocity.y + 0.1D), velocity.z);
        }
        player.fallDistance = 0.0F;
        if (!player.getAbilities().instabuild && player.tickCount % (dns ? 2 : 5) == 0) {
            powered.hbmSetCharge(chest, powered.hbmCharge(chest) - Math.max(1L, powered.consumption()));
        }
    }


    public boolean acceptsFuel(String fluidName) {
        return this.fuelName.equals(fluidName);
    }

    /** Mirrors JetpackFueledBase#tryFill: returns the amount actually accepted. */
    public int fill(ItemStack stack, String fluidName, int amount) {
        if (amount <= 0 || !acceptsFuel(fluidName)) {
            return 0;
        }
        int fill = fuel(stack);
        int toFill = Math.min(amount, this.capacity - fill);
        setFuel(stack, fill + toFill);
        return toFill;
    }

    public static void setFuel(ItemStack stack, int amount) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt(FUEL, amount);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    /** JetpackFueledBase#useUpFuel */
    private static void useUpFuel(Player player, ItemStack stack, int rate) {
        if (player.tickCount % rate == 0) {
            setFuel(stack, fuel(stack) - 1);
        }
    }

    /** JetpackBase#isValidArmor: armorType == 1 (can be worn on its own). */
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    /** JetpackBase#modUpdate: tick the installed jetpack, then write it back. Server path. */
    @Override
    public void tickArmor(Player player, ItemStack armor) {
        ItemStack jetpack = ArmorModHandler.pryMods(armor, player.registryAccess())[ArmorModHandler.PLATE_ONLY];
        if (jetpack.isEmpty()) {
            return;
        }
        onArmorTick(player.level(), player, jetpack);
        ArmorModHandler.applyMod(armor, jetpack, player.registryAccess());
    }

    private static void sendParticles(Level level, Player player, int mode) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null, player.getX(), player.getY(), player.getZ(), 100,
                    new ArmorParityPayloads.Effect(ArmorParityPayloads.Effect.JETPACK, player.getId(), mode, 0));
        }
    }

    private static void playThrust(Level level, Player player, float pitch) {
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.WEAPON_FLAMETHROWER_SHOOT.get(),
                    SoundSource.PLAYERS, 0.25F, pitch);
        }
    }

    /** onArmorTick of the four 1.7.10 jetpack classes. */
    public void onArmorTick(Level level, Player player, ItemStack stack) {
        boolean active = LegacyArmorModParity.isJetpackActive(player);
        boolean sneaking = player.isShiftKeyDown();
        Vec3 m = player.getDeltaMovement();
        double mx = m.x, my = m.y, mz = m.z;
        switch (this.profile) {
            case REGULAR -> {
                if (!level.isClientSide && fuel(stack) > 0 && active) sendParticles(level, player, 0);
                if (fuel(stack) > 0 && active) {
                    player.fallDistance = 0;
                    if (my < 0.4D) my += 0.1D;
                    player.setDeltaMovement(mx, my, mz);
                    playThrust(level, player, 1.5F);
                    useUpFuel(player, stack, 5);
                    com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower.resetFlightTime(player);
                }
            }
            case BREAK -> {
                boolean backpack = LegacyArmorModParity.enableBackpack(player);
                if (!level.isClientSide && fuel(stack) > 0 && (active || (!player.onGround() && !sneaking && backpack))) {
                    sendParticles(level, player, 0);
                }
                if (fuel(stack) > 0) {
                    boolean playerTriesToHover = sneaking && active;
                    boolean playerShouldHover = playerTriesToHover || !sneaking;
                    if (active && !playerTriesToHover) {
                        player.fallDistance = 0;
                        if (my < 0.4D) my += 0.1D;
                        player.setDeltaMovement(mx, my, mz);
                        playThrust(level, player, 1.5F);
                        useUpFuel(player, stack, 5);
                    } else if (playerShouldHover && !player.onGround() && backpack) {
                        player.fallDistance = 0;
                        if (my < -1) my += 0.2D;
                        else if (my < -0.1) my += 0.1D;
                        else if (my < 0) my = 0;
                        mx *= 1.025D;
                        mz *= 1.025D;
                        player.setDeltaMovement(mx, my, mz);
                        playThrust(level, player, 1.5F);
                        useUpFuel(player, stack, 10);
                    }
                    com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower.resetFlightTime(player);
                }
            }
            case VECTOR, BOOST -> {
                boolean boost = this.profile == Profile.BOOST;
                if (!level.isClientSide && fuel(stack) > 0 && active) sendParticles(level, player, 1);
                if (fuel(stack) > 0 && active) {
                    if (my < (boost ? 0.6D : 0.4D)) my += 0.1D;
                    Vec3 look = player.getLookAngle();
                    if (new Vec3(mx, my, mz).length() < (boost ? 5 : 2)) {
                        double f = boost ? 0.25 : 0.1;
                        mx += look.x * f;
                        my += look.y * f;
                        mz += look.z * f;
                        if (look.y > 0) player.fallDistance = 0;
                    }
                    player.setDeltaMovement(mx, my, mz);
                    playThrust(level, player, boost ? 1.0F : 1.5F);
                    useUpFuel(player, stack, boost ? 1 : 3);
                    com.atomiclegacy.items.weapon.sedna.impl.ItemGunChargeThrower.resetFlightTime(player);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (this.profile) {
            case REGULAR -> tooltip.add(Component.literal("Regular jetpack for simple upwards momentum."));
            case BREAK -> {
                tooltip.add(Component.literal("Regular jetpack that will automatically hover mid-air."));
                tooltip.add(Component.literal("Sneaking will stop hover mode."));
                tooltip.add(Component.literal("Hover mode will consume less fuel and increase air-mobility."));
            }
            case VECTOR -> {
                tooltip.add(Component.literal("High-mobility jetpack."));
                tooltip.add(Component.literal("Higher fuel consumption."));
            }
            case BOOST -> {
                tooltip.add(Component.literal("High-powered vectorized jetpack."));
                tooltip.add(Component.literal("Highly increased fuel consumption."));
            }
        }
        tooltip.add(Component.translatable("hbmfluid." + this.fuelName).append(": " + fuel(stack) + "mB / " + this.capacity + "mB")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(line(ChatFormatting.GOLD, "Can be worn on its own!"));
    }

    @Override
    public void addDesc(List<Component> tooltip, ItemStack mod, ItemStack armor) {
        tooltip.add(descLine(ChatFormatting.RED, mod, " (").copy()
                .append(Component.translatable("hbmfluid." + this.fuelName))
                .append(": " + fuel(mod) + "mB / " + this.capacity + "mB)"));
    }
}
