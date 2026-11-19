package com.atomiclegacy.items;

import com.atomiclegacy.render.UniversalGrenadeItemRenderer;
import com.atomiclegacy.entity.UniversalGrenadeEntity;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.render.anim.AnimationEnums.GunAnimation;
import com.atomiclegacy.render.anim.BusAnimation;
import com.atomiclegacy.render.anim.BusAnimationKeyframe.IType;
import com.atomiclegacy.render.anim.BusAnimationSequence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * The configurable grenade from 1.7.10.  Its four component choices are
 * carried on the completed item so copies, entities and recipes retain the
 * exact selected configuration.
 */
public final class UniversalGrenadeItem extends Item implements IAnimatedItem<GunAnimation> {
    private static final String SHELL_KEY = "shell";
    private static final String FILLING_KEY = "filling";
    private static final String FUZE_KEY = "fuze";
    private static final String EXTRA_KEY = "extra";
    /** Key of the previous port revision; removed from stacks so that they stack again. */
    private static final String OLD_DEPLOYMENT_KEY = "deployment";

    /**
     * [parity H2: grenade_universal] HbmPlayerProps.grenadeDeployment: transient per-player counter, kept separately
     * by the client and the server player object exactly like the 1.7.10 extended properties.
     */
    private static final Map<Player, Integer> GRENADE_DEPLOYMENT = Collections.synchronizedMap(new WeakHashMap<>());

    public UniversalGrenadeItem(Properties properties) {
        super(properties);
        // the constructor of ItemGrenadeFilling created the bullet configs of the fillings
        com.atomiclegacy.entity.UniversalGrenadeFillings.init();
    }

    public static int deployment(Player player) {
        return GRENADE_DEPLOYMENT.getOrDefault(player, 0);
    }

    public static void setDeployment(Player player, int value) {
        GRENADE_DEPLOYMENT.put(player, value);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final UniversalGrenadeItemRenderer renderer = new UniversalGrenadeItemRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return shell(stack).stackLimit();
    }

    /** IEquipReceiver#onEquip */
    public void onEquip(Player player, ItemStack stack) {
        setDeployment(player, 0);
        // HbmAnimationPacket(GunAnimation.EQUIP.ordinal(), 0)
        playAnimation(player, GunAnimation.EQUIP);
    }

    /** ItemGrenadeUniversal#onItemRightClick: no sound, thrown once the draw animation of the shell has finished. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 1.7.10 has no off hand; the deployment counter only runs for the selected hotbar slot
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        Shell shell = shell(stack);
        if (deployment(player) >= shell.drawDuration()) {
            if (!level.isClientSide) {
                level.addFreshEntity(new UniversalGrenadeEntity(level, player, stack));
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!stack.isEmpty()) this.onEquip(player, stack);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /** ItemGrenadeUniversal#onUpdate */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean isHeld) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (customData(stack).contains(OLD_DEPLOYMENT_KEY)) {
            CompoundTag tag = customData(stack);
            tag.remove(OLD_DEPLOYMENT_KEY);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        boolean wasHeld = ItemGunBaseNT.getIsEquipped(stack);
        if (!wasHeld && isHeld) {
            this.onEquip(player, stack);
        } else if (isHeld) {
            int deployment = deployment(player) + 1;
            setDeployment(player, deployment);
            Shell shell = shell(stack);
            // world.playSoundAtEntity: audible to everyone from the server, a no-op on the 1.7.10 client
            if (!level.isClientSide) {
                if (shell == Shell.FRAG && deployment == 18) {
                    playAtEntity(level, player, HbmSoundEvents.GUN_REVOLVER_COCK.get(), 1F, 1F);
                }
                if (shell == Shell.STICK) {
                    if (deployment == 16) playAtEntity(level, player, HbmSoundEvents.GUN_BOLT_OPEN.get(), 1F, 1.25F);
                    if (deployment == 25) playAtEntity(level, player, HbmSoundEvents.GUN_BOLT_OPEN.get(), 1F, 1.25F);
                }
                if (shell == Shell.TECH && deployment == 18) {
                    playAtEntity(level, player, HbmSoundEvents.GRENADE_TECH.get(), 1F, 1F);
                }
                if (shell == Shell.NUKE && deployment == 26) {
                    playAtEntity(level, player, HbmSoundEvents.GRENADE_NUKA.get(), 1F, 1F);
                }
            }
        }
        // ItemGunBaseNT.setIsEquipped(stack, isHeld) every tick; only written when the stored value would change
        if (wasHeld != isHeld || !customData(stack).contains(ItemGunBaseNT.KEY_EQUIPPED)) ItemGunBaseNT.setIsEquipped(stack, isHeld);
    }

    private static void playAtEntity(Level level, Player player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** ItemGrenadeUniversal#getAnimation: the EQUIP (pin pulling) animation of each shell. */
    @Override
    public BusAnimation getAnimation(GunAnimation type, ItemStack stack) {
        if (type != GunAnimation.EQUIP) return null;
        Shell shell = shell(stack);

        if (shell == Shell.FRAG || shell == Shell.TECH) {
            return new BusAnimation()
                    .addBus("BODYMOVE", new BusAnimationSequence().setPos(0, -5, 0).addPos(0, -3, 0, 350).addPos(0, 0, 0, 350, IType.SIN_DOWN))
                    .addBus("BODYTURN", new BusAnimationSequence().addPos(0, 0, 45, 350).addPos(0, 0, -15, 350, IType.SIN_DOWN).hold(200).addPos(0, 0, -20, 100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("RINGMOVE", new BusAnimationSequence().hold(900).addPos(0, 0, 1, 150).addPos(0, -3, 3, 300))
                    .addBus("RINGTURN", new BusAnimationSequence().hold(900).addPos(0, 0, 45, 300))
                    .addBus("RENDERRING", new BusAnimationSequence().setPos(1, 1, 1).hold(1350).setPos(0, 0, 0));
        }

        if (shell == Shell.STICK) {
            return new BusAnimation()
                    .addBus("BODYMOVE", new BusAnimationSequence().setPos(0, -7, 0).addPos(0, 3, 0, 750, IType.SIN_DOWN).holdUntil(1900).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BODYTURN", new BusAnimationSequence().setPos(0, 0, 90).addPos(0, 0, -45, 750, IType.SIN_DOWN).holdUntil(1900).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("RINGMOVE", new BusAnimationSequence().hold(800).addPos(0, -0.25, 0, 200, IType.SIN_FULL).hold(250).addPos(0, -0.5, 0, 200, IType.SIN_FULL).addPos(2, -5, 0, 350, IType.SIN_UP))
                    .addBus("RINGTURN", new BusAnimationSequence().hold(800).addPos(0, 360, 0, 200, IType.SIN_FULL).hold(250).addPos(0, 360 * 2, 0, 200, IType.SIN_FULL))
                    .addBus("RENDERRING", new BusAnimationSequence().setPos(1, 1, 1).hold(2100).setPos(0, 0, 0));
        }

        if (shell == Shell.NUKE) {
            return new BusAnimation()
                    .addBus("BODYMOVE", new BusAnimationSequence().setPos(0, -5, 0).hold(250).addPos(0, 0, 0, 850, IType.SIN_DOWN))
                    .addBus("BODYTURN", new BusAnimationSequence().setPos(0, 0, 90).hold(250).addPos(0, 0, -25, 850, IType.SIN_DOWN).hold(200).addPos(0, 0, -30, 100, IType.SIN_DOWN).addPos(0, 0, 0, 750, IType.SIN_FULL))
                    .addBus("RINGMOVE", new BusAnimationSequence().hold(1300).addPos(0, 0, 1, 150).addPos(0, -3, 3, 300))
                    .addBus("RINGTURN", new BusAnimationSequence().hold(1300).addPos(0, 0, 720, 500)) // SPEEN
                    .addBus("RENDERRING", new BusAnimationSequence().setPos(1, 1, 1).hold(1750).setPos(0, 0, 0));
        }

        return null;
    }

    @Override
    public boolean shouldPlayerModelAim(ItemStack stack) {
        return false;
    }

    @Override
    public Class<GunAnimation> getEnum() {
        return GunAnimation.class;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(componentName("grenade_shell", shell(stack).id()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(componentName("grenade_filling", filling(stack).id()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(componentName("grenade_fuze", fuze(stack).id()).withStyle(ChatFormatting.YELLOW));
        Extra extra = extra(stack);
        if (extra != null) {
            tooltip.add(componentName("grenade_extra", extra.id()).withStyle(ChatFormatting.RED));
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Shell shell : Shell.values()) {
            for (Filling filling : Filling.values()) {
                if (!filling.supports(shell)) {
                    continue;
                }
                for (Fuze fuze : Fuze.values()) {
                    output.accept(make(shell, filling, fuze, null));
                    for (Extra extra : Extra.values()) {
                        output.accept(make(shell, filling, fuze, extra));
                    }
                }
            }
        }
    }

    public static ItemStack make(Shell shell, Filling filling, Fuze fuze, @Nullable Extra extra) {
        ItemStack stack = new ItemStack(HbmItems.GRENADE_UNIVERSAL.get());
        CompoundTag tag = customData(stack);
        tag.putString(SHELL_KEY, shell.id());
        tag.putString(FILLING_KEY, filling.id());
        tag.putString(FUZE_KEY, fuze.id());
        if (extra != null) {
            tag.putString(EXTRA_KEY, extra.id());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        // The legacy renderer used the same complete-grenade mesh for every
        // configuration; the component information is shown by tint/tooltips.
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0));
        return stack;
    }

    public static Shell shell(ItemStack stack) {
        return Shell.byId(readId(stack, SHELL_KEY));
    }

    public static Filling filling(ItemStack stack) {
        return Filling.byId(readId(stack, FILLING_KEY));
    }

    public static Fuze fuze(ItemStack stack) {
        return Fuze.byId(readId(stack, FUZE_KEY));
    }

    @Nullable
    public static Extra extra(ItemStack stack) {
        String value = readId(stack, EXTRA_KEY);
        return value.isEmpty() ? null : Extra.byId(value);
    }

    private static MutableComponent componentName(String baseId, String variant) {
        return Component.translatable("item.atomiclegacy." + baseId + "." + variant);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static String readId(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(key);
    }

    public enum Shell {
        FRAG(4, 30, 0.5D, 1.0D),
        STICK(4, 43, 0.25D, 1.5D),
        TECH(2, 30, 0.5D, 1.0D),
        NUKE(1, 43, 0.25D, 1.5D);

        private final int stackLimit;
        private final int drawDuration;
        private final double bounce;
        private final double throwForce;

        Shell(int stackLimit, int drawDuration, double bounce, double throwForce) {
            this.stackLimit = stackLimit;
            this.drawDuration = drawDuration;
            this.bounce = bounce;
            this.throwForce = throwForce;
        }

        public String id() { return name().toLowerCase(Locale.ROOT); }
        public int stackLimit() { return stackLimit; }
        public int drawDuration() { return drawDuration; }
        public double bounce() { return bounce; }
        public double throwForce() { return throwForce; }
        public static Shell byId(String id) {
            for (Shell value : values()) if (value.id().equals(id)) return value;
            return FRAG;
        }
    }

    public enum Filling {
        POWDER(0x424242, 0x939176, Shell.FRAG, Shell.STICK),
        HE(0x595533, 0xA49D62, Shell.FRAG, Shell.STICK),
        DEMO(0x595533, 0xDD4029, Shell.FRAG, Shell.STICK),
        INC(0x5A5A5A, 0xFF5F21, Shell.FRAG, Shell.STICK),
        WP(0xDCDCDC, 0xFF5F21, Shell.FRAG, Shell.STICK),
        CLUSTER(0x5A5A5A, 0xFFC711, Shell.FRAG, Shell.STICK),
        EMP(0x93A1AC, 0x00FFFF, Shell.TECH),
        PLASMA(0x655B2C, 0x4CFF00, Shell.TECH),
        LASER(0x493A3A, 0xFF0000, Shell.TECH),
        CLUSTER_HEAVY(0x5A5A5A, 0xFF5F21, Shell.NUKE),
        NUCLEAR(0xDFD7A8, 0xA49D62, Shell.NUKE),
        NUCLEAR_DEMO(0xDFD7A8, 0xDD4029, Shell.NUKE),
        SCHRAB(0x00BDBD, 0x000000, Shell.NUKE);

        private final Shell[] shells;
        private final int bodyColor;
        private final int labelColor;
        Filling(int bodyColor, int labelColor, Shell... shells) {
            this.bodyColor = bodyColor;
            this.labelColor = labelColor;
            this.shells = shells;
        }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public int bodyColor() { return bodyColor; }
        public int labelColor() { return labelColor; }
        public boolean supports(Shell shell) {
            for (Shell compatible : shells) if (compatible == shell) return true;
            return false;
        }
        public static Filling byId(String id) {
            for (Filling value : values()) if (value.id().equals(id)) return value;
            return HE;
        }
    }

    public enum Fuze {
        S3(0x000000), S7(0x404040), S15(0x808080), IMPACT(0xE36C17), AIRBURST(0x56A137);
        private final int bandColor;
        Fuze(int bandColor) { this.bandColor = bandColor; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public int bandColor() { return bandColor; }
        public static Fuze byId(String id) {
            for (Fuze value : values()) if (value.id().equals(id)) return value;
            return S3;
        }
    }

    public enum Extra {
        GLUE, PROXY_FUZE, FRAG_SLEEVE, TRIPLEX;
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public static Extra byId(String id) {
            for (Extra value : values()) if (value.id().equals(id)) return value;
            return null;
        }
    }
}
