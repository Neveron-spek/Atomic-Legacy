package com.atomiclegacy.items;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.BobbleheadType;
import com.atomiclegacy.explosion.LegacyVnt;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.CentrifugeRecipe;
import com.atomiclegacy.inventory.recipes.CrystallizerRecipe;
import com.atomiclegacy.network.MaxwellGibEffectPayload;
import com.atomiclegacy.network.PlayerInformPayload;
import com.atomiclegacy.radiation.RadiationEvents;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmMobEffects;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.registry.HbmSoundEvents;
import com.atomiclegacy.tileentity.FluidPipeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * [parity I3] Port of com.hbm.handler.ability (IToolAreaAbility, IToolHarvestAbility, IWeaponAbility,
 * AvailableAbilities, ToolPreset) and of the shared logic of ItemToolAbility / ItemSwordAbility
 * (Configuration NBT, preset cycling, onBlockStartBreak / breakExtraBlock / standardDigPost, tooltips).
 */
public final class HbmToolBehavior {
    // [parity V3: ToolConfig] 11.00_recursionDepth, 11.01_recursionStone, 11.02_recursionNetherrack: HbmParityV3Config.
    /** ServerProxy.ID_TOOLABILITY */
    public static final int ID_TOOLABILITY = 11;

    private static final ThreadLocal<Boolean> IN_ABILITY = ThreadLocal.withInitial(() -> false);
    /** ItemToolAbility.dropX/Y/Z: all drops of one dig operation land at the reference block. */
    private static final ThreadLocal<BlockPos> DROP_POS = new ThreadLocal<>();

    private HbmToolBehavior() {
    }

    /** ItemToolAbility.EnumToolType (SWORD = ItemSword-based items). */
    public enum ToolType { PICKAXE, AXE, SHOVEL, MINER, SWORD }

    public enum AreaAbility {
        RECURSION("tool.ability.recursion", "tool.ability.atomiclegacy.recursion", 1, new Object[]{3, 4, 5, 6, 7, 9, 10}),
        HAMMER("tool.ability.hammer", "tool.ability.atomiclegacy.hammer", 2, new Object[]{1, 2, 3, 4}),
        HAMMER_FLAT("tool.ability.hammer_flat", "tool.ability.atomiclegacy.hammer_flat", 3, new Object[]{1, 2, 3, 4}),
        EXPLOSION("tool.ability.explosion", "tool.ability.atomiclegacy.explosion", 4, new Object[]{2.5F, 5F, 10F, 15F});

        final String nbtName;
        final String key;
        final int sortOrder;
        final Object[] values;

        AreaAbility(String nbtName, String key, int sortOrder, Object[] values) {
            this.nbtName = nbtName;
            this.key = key;
            this.sortOrder = sortOrder;
            this.values = values;
        }

        public int levels() {
            return this.values.length;
        }

        public String extension(int level) {
            return " (" + this.values[level] + ")";
        }

        public String translationKey() {
            return this.key;
        }

        public float value(int level) {
            return ((Number) this.values[clampLevel(level, levels())]).floatValue();
        }

        /** [parity V3: ToolConfig] IToolAreaAbility.isAllowed: abilityVein / abilityHammer (both hammers) / abilityExplosion. */
        public boolean isAllowed() {
            return switch (this) {
                case RECURSION -> com.atomiclegacy.config.HbmParityV3Config.abilityVein();
                case HAMMER, HAMMER_FLAT -> com.atomiclegacy.config.HbmParityV3Config.abilityHammer();
                case EXPLOSION -> com.atomiclegacy.config.HbmParityV3Config.abilityExplosion();
            };
        }

        /** IToolAreaAbility.allowsHarvest */
        public boolean allowsHarvest() {
            return this != EXPLOSION;
        }

        static AreaAbility byName(String name) {
            for (AreaAbility ability : values()) if (ability.nbtName.equals(name)) return ability;
            return null;
        }
    }

    public enum HarvestAbility {
        SILK("tool.ability.silktouch", "tool.ability.atomiclegacy.silk", 101, new Object[0]),
        LUCK("tool.ability.luck", "tool.ability.atomiclegacy.luck", 102, new Object[]{1, 2, 3, 4, 5, 9}),
        SMELTER("tool.ability.smelter", "tool.ability.atomiclegacy.smelter", 103, new Object[0]),
        SHREDDER("tool.ability.shredder", "tool.ability.atomiclegacy.shredder", 104, new Object[0]),
        CENTRIFUGE("tool.ability.centrifuge", "tool.ability.atomiclegacy.centrifuge", 105, new Object[0]),
        CRYSTALLIZER("tool.ability.crystallizer", "tool.ability.atomiclegacy.crystallizer", 106, new Object[0]),
        MERCURY("tool.ability.mercury", "tool.ability.atomiclegacy.mercury", 107, new Object[0]);

        final String nbtName;
        final String key;
        final int sortOrder;
        final Object[] values;

        HarvestAbility(String nbtName, String key, int sortOrder, Object[] values) {
            this.nbtName = nbtName;
            this.key = key;
            this.sortOrder = sortOrder;
            this.values = values;
        }

        public int levels() {
            return Math.max(1, this.values.length);
        }

        public String extension(int level) {
            return this.values.length == 0 ? "" : " (" + this.values[level] + ")";
        }

        public String translationKey() {
            return this.key;
        }

        /** [parity V3: ToolConfig] IToolHarvestAbility.isAllowed. */
        public boolean isAllowed() {
            return switch (this) {
                case SILK -> com.atomiclegacy.config.HbmParityV3Config.abilitySilk();
                case LUCK -> com.atomiclegacy.config.HbmParityV3Config.abilityLuck();
                case SMELTER -> com.atomiclegacy.config.HbmParityV3Config.abilityFurnace();
                case SHREDDER -> com.atomiclegacy.config.HbmParityV3Config.abilityShredder();
                case CENTRIFUGE -> com.atomiclegacy.config.HbmParityV3Config.abilityCentrifuge();
                case CRYSTALLIZER -> com.atomiclegacy.config.HbmParityV3Config.abilityCrystallizer();
                case MERCURY -> com.atomiclegacy.config.HbmParityV3Config.abilityMercury();
            };
        }

        public int power(int level) {
            return this.values.length == 0 ? 0 : (Integer) this.values[clampLevel(level, levels())];
        }

        static HarvestAbility byName(String name) {
            for (HarvestAbility ability : values()) if (ability.nbtName.equals(name)) return ability;
            return null;
        }
    }

    public enum WeaponAbility {
        RADIATION("weapon.ability.atomiclegacy.radiation", 201, new Object[]{15F, 50F, 500F}),
        VAMPIRE("weapon.ability.atomiclegacy.vampire", 202, new Object[]{2F, 3F, 5F, 10F, 50F}),
        STUN("weapon.ability.atomiclegacy.stun", 203, new Object[]{2, 3, 5, 10, 15}),
        PHOSPHORUS("weapon.ability.atomiclegacy.phosphorus", 204, new Object[]{60, 90}),
        FIRE("weapon.ability.atomiclegacy.fire", 206, new Object[]{5, 10}),
        CHAINSAW("weapon.ability.atomiclegacy.chainsaw", 207, new Object[]{15, 10}),
        BEHEADER("weapon.ability.atomiclegacy.beheader", 208, new Object[0]),
        BOBBLE("weapon.ability.atomiclegacy.bobble", 209, new Object[0]);

        final String key;
        final int sortOrder;
        final Object[] values;

        WeaponAbility(String key, int sortOrder, Object[] values) {
            this.key = key;
            this.sortOrder = sortOrder;
            this.values = values;
        }

        public int levels() {
            return Math.max(1, this.values.length);
        }

        public String extension(int level) {
            if (this.values.length == 0) return "";
            if (this == CHAINSAW) return " (1:" + this.values[level] + ")";
            return " (" + this.values[level] + ")";
        }

        public String translationKey() {
            return this.key;
        }

        public float value(int level) {
            return this.values.length == 0 ? 0.0F : ((Number) this.values[clampLevel(level, levels())]).floatValue();
        }
    }

    public enum SpecialBehavior {
        NONE,
        REDSTONE_SWORD
    }

    /** ToolPreset; null = IToolAreaAbility.NONE / IToolHarvestAbility.NONE. */
    public static final class Preset {
        public AreaAbility area;
        public int areaLevel;
        public HarvestAbility harvest;
        public int harvestLevel;

        public Preset(AreaAbility area, int areaLevel, HarvestAbility harvest, int harvestLevel) {
            this.area = area;
            this.areaLevel = areaLevel;
            this.harvest = harvest;
            this.harvestLevel = harvestLevel;
        }

        public boolean isNone() {
            return this.area == null && this.harvest == null;
        }

        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putString("area", this.area == null ? "" : this.area.nbtName);
            tag.putInt("areaLevel", this.areaLevel);
            tag.putString("harvest", this.harvest == null ? "" : this.harvest.nbtName);
            tag.putInt("harvestLevel", this.harvestLevel);
            return tag;
        }

        static Preset read(CompoundTag tag) {
            Preset preset = new Preset(AreaAbility.byName(tag.getString("area")), tag.getInt("areaLevel"),
                    HarvestAbility.byName(tag.getString("harvest")), tag.getInt("harvestLevel"));
            preset.areaLevel = Math.min(preset.areaLevel, (preset.area == null ? 1 : preset.area.levels()) - 1);
            preset.harvestLevel = Math.min(preset.harvestLevel, (preset.harvest == null ? 1 : preset.harvest.levels()) - 1);
            return preset;
        }

        void restrictTo(HbmToolProfile profile) {
            int maxArea = this.area == null ? 0 : profile.maxLevel(this.area);
            if (maxArea == -1) {
                this.area = null;
                this.areaLevel = 0;
            } else if (this.areaLevel > maxArea) {
                this.areaLevel = maxArea;
            } else if (this.areaLevel < 0) {
                this.areaLevel = 0;
            }
            if (this.area != null && !this.area.allowsHarvest()) {
                this.harvest = null;
                this.harvestLevel = 0;
            }
            int maxHarvest = this.harvest == null ? 0 : profile.maxLevel(this.harvest);
            if (maxHarvest == -1) {
                this.harvest = null;
                this.harvestLevel = 0;
            } else if (this.harvestLevel > maxHarvest) {
                this.harvestLevel = maxHarvest;
            } else if (this.harvestLevel < 0) {
                this.harvestLevel = 0;
            }
        }
    }

    /** ItemToolAbility.Configuration */
    public static final class Configuration {
        public List<Preset> presets = new ArrayList<>();
        public int currentPreset;

        public Preset active() {
            return this.presets.get(this.currentPreset);
        }

        void reset(HbmToolProfile profile) {
            this.currentPreset = 0;
            this.presets = new ArrayList<>();
            this.presets.add(new Preset(null, 0, null, 0));
            for (HbmToolProfile.AbilityLevel<AreaAbility> entry : profile.areaAbilities()) {
                this.presets.add(new Preset(entry.ability(), clampLevel(entry.level(), entry.ability().levels()), null, 0));
            }
            for (HbmToolProfile.AbilityLevel<HarvestAbility> entry : profile.harvestAbilities()) {
                this.presets.add(new Preset(null, 0, entry.ability(), clampLevel(entry.level(), entry.ability().levels())));
            }
            this.presets.sort(Comparator
                    .comparingInt((Preset p) -> p.harvest == null ? 100 : p.harvest.sortOrder)
                    .thenComparingInt(p -> p.harvestLevel)
                    .thenComparingInt(p -> p.area == null ? 0 : p.area.sortOrder)
                    .thenComparingInt(p -> p.areaLevel));
        }
    }

    public static Configuration getConfiguration(ItemStack stack, HbmToolProfile profile) {
        Configuration config = new Configuration();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? null : data.copyTag();
        if (tag == null || !tag.contains("ability") || !tag.contains("abilityPresets")) {
            config.reset(profile);
            return config;
        }
        readConfiguration(config, tag);
        for (Preset preset : config.presets) preset.restrictTo(profile);
        if (config.presets.isEmpty()) {
            // 1.7.10 would fail on presets.get(0) for an empty list; the mod itself never writes one.
            config.reset(profile);
        }
        return config;
    }

    private static void readConfiguration(Configuration config, CompoundTag tag) {
        config.currentPreset = tag.getInt("ability");
        ListTag list = tag.getList("abilityPresets", Tag.TAG_COMPOUND);
        int count = Math.min(list.size(), 99);
        config.presets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) config.presets.add(Preset.read(list.getCompound(i)));
        config.currentPreset = Math.max(0, Math.min(config.currentPreset, config.presets.size() - 1));
    }

    public static void setConfiguration(ItemStack stack, Configuration config) {
        CompoundTag tag = HbmToolItems.materializedTag(stack);
        tag.putInt("ability", config.currentPreset);
        ListTag list = new ListTag();
        for (Preset preset : config.presets) list.add(preset.write());
        tag.put("abilityPresets", list);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** ItemToolAbility.receiveControl (GUIScreenToolAbility itself is not ported; the server-side handling is). */
    public static void receiveControl(ItemStack stack, CompoundTag data, HbmToolProfile profile) {
        Configuration config = new Configuration();
        readConfiguration(config, data);
        for (Preset preset : config.presets) preset.restrictTo(profile);
        setConfiguration(stack, config);
    }

    /** ToolPreset.getMessage() as info line 11: "[Enabled " + names + extensions in yellow; deactivated in gold. */
    public static PlayerInformPayload presetMessage(Preset preset) {
        if (preset.isNone()) {
            return new PlayerInformPayload(ID_TOOLABILITY, "[Tool ability deactivated]", 0xFFAA00, 3000);
        }
        boolean hasArea = preset.area != null;
        boolean hasHarvest = preset.harvest != null;
        return PlayerInformPayload.translated(ID_TOOLABILITY, "[Enabled %s%s%s%s%s", 0xFFFF55, 3000,
                hasArea ? "@" + preset.area.key : "",
                hasArea ? preset.area.extension(preset.areaLevel) : "",
                hasArea && hasHarvest ? " + " : "",
                hasHarvest ? "@" + preset.harvest.key : "",
                hasHarvest ? preset.harvest.extension(preset.harvestLevel) : "");
    }

    // ---------------------------------------------------------------- item hooks

    public static Item.Properties properties(HbmToolProfile profile) {
        return new Item.Properties().stacksTo(1).rarity(profile.rarity()).attributes(attributes(profile));
    }

    /** uses == 0 in 1.7.10 meant isDamageable() == false; modern items get a hidden Unbreakable component. */
    public static Item.Properties unbreakable(Item.Properties properties) {
        return properties.component(DataComponents.UNBREAKABLE, new Unbreakable(false));
    }

    /**
     * getItemAttributeModifiers: attack damage "damage" (operation 0) and movement speed "movement"
     * (operation 1 = ADD_MULTIPLIED_BASE). Attack speed has no 1.7.10 counterpart (no attack cooldown).
     */
    public static ItemAttributeModifiers attributes(HbmToolProfile profile) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        builder.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, profile.attackDamage(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        builder.add(Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, profile.attackSpeed(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        if (profile.movementModifier() != 0.0D) {
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(AtomicLegacy.id("tool_movement"), profile.movementModifier(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    /** ItemToolAbility.handleKeybind(ABILITY_CYCLE): the default ability key is the use key (-99, right mouse). */
    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, HbmToolItems.AbilityItem item) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !item.isAbilityTool() || !item.canOperate(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        Configuration config = getConfiguration(stack, item.profile());
        if (config.presets.size() < 2) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            cycle(stack, player, item, config);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void cycle(ItemStack stack, Player player, HbmToolItems.AbilityItem item, Configuration config) {
        if (player.isShiftKeyDown()) {
            config.currentPreset = 0;
        } else {
            config.currentPreset = (config.currentPreset + 1) % config.presets.size();
        }
        setConfiguration(stack, config);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, presetMessage(config.active()));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.25F, config.active().isNone() ? 0.75F : 1.25F);
    }

    public static InteractionResult useOn(UseOnContext context, HbmToolProfile profile) {
        if (profile.specialBehavior() == SpecialBehavior.REDSTONE_SWORD) {
            return useRedstoneSword(context);
        }
        return InteractionResult.PASS;
    }

    /** AvailableAbilities.addInformation + the ItemToolAbility depth rock line. */
    public static void addTooltip(List<Component> tooltip, HbmToolProfile profile, boolean toolAbilities) {
        if (toolAbilities) {
            List<Object[]> lines = new ArrayList<>();
            for (HbmToolProfile.AbilityLevel<AreaAbility> e : profile.areaAbilities()) {
                int level = clampLevel(e.level(), e.ability().levels());
                lines.add(new Object[]{e.ability().sortOrder, level, e.ability().key, e.ability().extension(level)});
            }
            for (HbmToolProfile.AbilityLevel<HarvestAbility> e : profile.harvestAbilities()) {
                int level = clampLevel(e.level(), e.ability().levels());
                lines.add(new Object[]{e.ability().sortOrder, level, e.ability().key, e.ability().extension(level)});
            }
            lines.sort(Comparator.comparingInt((Object[] o) -> (Integer) o[0]).thenComparingInt(o -> (Integer) o[1]));
            if (!lines.isEmpty()) {
                tooltip.add(Component.literal("Abilities: ").withStyle(ChatFormatting.GRAY));
                for (Object[] line : lines) {
                    tooltip.add(Component.literal("  ").withStyle(ChatFormatting.GRAY)
                            .append(Component.translatable((String) line[2]).append((String) line[3]).withStyle(ChatFormatting.GOLD)));
                }
                tooltip.add(Component.literal("Right click to cycle through presets!").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Sneak-click to go to first preset!").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("Alt-click to open customization GUI!").withStyle(ChatFormatting.GRAY));
            }
        }
        List<HbmToolProfile.AbilityLevel<WeaponAbility>> weapons = new ArrayList<>(profile.weaponAbilities());
        weapons.sort(Comparator.comparingInt(e -> e.ability().sortOrder));
        if (!weapons.isEmpty()) {
            tooltip.add(Component.literal("Weapon modifiers: ").withStyle(ChatFormatting.GRAY));
            for (HbmToolProfile.AbilityLevel<WeaponAbility> e : weapons) {
                int level = clampLevel(e.level(), e.ability().levels());
                tooltip.add(Component.literal("  ").withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable(e.ability().key).append(e.ability().extension(level)).withStyle(ChatFormatting.RED)));
            }
        }
        if (toolAbilities && profile.rockBreaker()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Can break depth rock!").withStyle(ChatFormatting.RED));
        }
    }

    /** ItemToolAbility.hasEffect: enchanted or a non-empty active preset. */
    public static boolean isFoil(ItemStack stack, HbmToolProfile profile) {
        return !getConfiguration(stack, profile).active().isNone();
    }

    /** ItemToolAbility / ItemSwordAbility.hitEntity (abilities only; the durability hit is applied by the item). */
    public static void hitEntity(ItemStack stack, LivingEntity victim, LivingEntity attacker, HbmToolItems.AbilityItem item) {
        if (attacker.level().isClientSide || !(attacker instanceof Player player) || !item.canOperate(stack)) {
            return;
        }
        if (!item.isAbilityTool() && stack.is(BuiltInRegistries.ITEM.get(AtomicLegacy.id("mese_gavel")))) {
            // ItemSwordAbility: "hacky hacky hack" NTMSounds.GAVEL for mese_gavel
            attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), HbmSoundEvents.WEAPON_WHACK.get(),
                    SoundSource.PLAYERS, 3.0F, 1.0F);
        }
        List<HbmToolProfile.AbilityLevel<WeaponAbility>> weapons = new ArrayList<>(item.profile().weaponAbilities());
        weapons.sort(Comparator.comparingInt(e -> e.ability().sortOrder));
        for (HbmToolProfile.AbilityLevel<WeaponAbility> e : weapons) {
            onHit(e.ability(), clampLevel(e.level(), e.ability().levels()), attacker.level(), player, victim);
        }
    }

    public static void onHit(WeaponAbility ability, int level, Level world, Player player, LivingEntity living) {
        switch (ability) {
            case RADIATION -> RadiationEvents.contaminateCreative(living, ability.value(level));
            case VAMPIRE -> {
                float amount = ability.value(level);
                if (living.getHealth() <= 0) return;
                living.setHealth(living.getHealth() - amount);
                if (living.getHealth() <= 0) living.die(living.damageSources().magic());
                player.heal(amount);
            }
            case STUN -> {
                int duration = (int) ability.value(level);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration * 20, 4));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration * 20, 4));
            }
            case PHOSPHORUS -> living.addEffect(new MobEffectInstance(HbmMobEffects.PHOSPHORUS_BURN, (int) ability.value(level) * 20, 4));
            case FIRE -> living.igniteForSeconds((int) ability.value(level));
            case CHAINSAW -> {
                int divider = (int) ability.value(level);
                if (living.getHealth() <= 0.0F) {
                    int count = Math.min((int) Math.ceil(living.getMaxHealth() / divider), 250);
                    Item nitra = BuiltInRegistries.ITEM.get(AtomicLegacy.id("nitra_small"));
                    for (int i = 0; i < count; i++) {
                        living.spawnAtLocation(new ItemStack(nitra), 1.0F);
                        world.addFreshEntity(new ExperienceOrb(world, living.getX(), living.getY(), living.getZ(), 1));
                    }
                    gib(living);
                    world.playSound(null, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                            HbmSoundEvents.WEAPON_CHAINSAW.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
                }
            }
            case BEHEADER -> {
                if (living.getHealth() > 0.0F) return;
                if (living instanceof AbstractSkeleton) {
                    if (!(living instanceof WitherSkeleton)) {
                        living.spawnAtLocation(new ItemStack(Items.SKELETON_SKULL), 0.0F);
                    } else if (world.random.nextInt(20) == 0) {
                        living.spawnAtLocation(new ItemStack(Items.WITHER_SKELETON_SKULL), 0.0F);
                    } else {
                        living.spawnAtLocation(new ItemStack(Items.COAL, 3), 0.0F);
                    }
                } else if (living instanceof Zombie) {
                    living.spawnAtLocation(new ItemStack(Items.ZOMBIE_HEAD), 0.0F);
                } else if (living instanceof Creeper) {
                    living.spawnAtLocation(new ItemStack(Items.CREEPER_HEAD), 0.0F);
                } else if (living instanceof MagmaCube) {
                    living.spawnAtLocation(new ItemStack(Items.MAGMA_CREAM, 3), 0.0F);
                } else if (living instanceof Slime) {
                    living.spawnAtLocation(new ItemStack(Items.SLIME_BALL, 3), 0.0F);
                } else if (living instanceof Player victim) {
                    ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                    head.set(DataComponents.PROFILE, new ResolvableProfile(victim.getGameProfile()));
                    living.spawnAtLocation(head, 0.0F);
                } else {
                    living.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, 3), 0.0F);
                    living.spawnAtLocation(new ItemStack(Items.BONE, 2), 0.0F);
                }
            }
            case BOBBLE -> {
                if (living instanceof Monster mob && mob.getHealth() <= 0.0F) {
                    int chance = mob.getMaxHealth() > 20 ? 750 : 1000;
                    if (world.random.nextInt(chance) == 0) {
                        Item bobble = BuiltInRegistries.ITEM.get(AtomicLegacy.id("bobblehead"));
                        BobbleheadType type = BobbleheadType.byOrdinal(world.random.nextInt(BobbleheadType.values().length - 1) + 1);
                        ItemStack drop = bobble instanceof BobbleheadBlockItem bobbleItem
                                ? BobbleheadBlockItem.stackFor(bobbleItem, type) : new ItemStack(bobble);
                        mob.spawnAtLocation(drop, 0.0F);
                    }
                }
            }
        }
    }

    /** ConfettiUtil.gib: giblets packet (150 blocks) and mob.zombie.woodbreak; SkeletonCreator.composeEffectGib is not ported. */
    public static void gib(LivingEntity entity) {
        if (entity instanceof Ocelot || !(entity.level() instanceof ServerLevel level)) return;
        int type = 0;
        if (entity instanceof Slime || entity instanceof Creeper) type = 1;
        if (entity instanceof AbstractGolem || entity instanceof Blaze
                || entity instanceof com.atomiclegacy.entity.LegacyCyberCrabEntity
                || entity instanceof com.atomiclegacy.entity.LegacyTeslaCrabEntity
                || entity instanceof com.atomiclegacy.entity.LegacyTaintCrabEntity
                || entity instanceof com.atomiclegacy.entity.LegacyFbiDroneEntity
                || entity instanceof com.atomiclegacy.entity.LegacyRadBeastEntity
                || entity instanceof com.atomiclegacy.entity.LegacyUfoEntity
                || entity instanceof com.atomiclegacy.entity.LegacyWormHeadEntity
                || entity instanceof com.atomiclegacy.entity.LegacyWormBodyEntity) type = 2;
        if (entity instanceof AbstractSkeleton) return;
        double y = entity.getY() + entity.getBbHeight() * 0.5D;
        MaxwellGibEffectPayload payload = new MaxwellGibEffectPayload(entity.getId(), entity.getX(), y, entity.getZ(),
                entity.getBbWidth(), entity.getBbHeight(), type);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity.getX(), y, entity.getZ()) <= 150.0D * 150.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                SoundSource.HOSTILE, 2.0F, 0.95F + entity.getRandom().nextFloat() * 0.2F);
    }

    // ---------------------------------------------------------------- digging

    public static boolean typeMatches(ToolType type, BlockState state) {
        return switch (type) {
            case PICKAXE -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            case AXE -> state.is(BlockTags.MINEABLE_WITH_AXE);
            case SHOVEL -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case MINER -> state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case SWORD -> false;
        };
    }

    /** ItemToolAbility.canHarvestBlock: SILK preset or getDigSpeed > 1. */
    public static boolean canHarvestBlock(ItemStack stack, BlockState state, HbmToolItems.AbilityItem item) {
        if (!item.canOperate(stack)) return false;
        if (getConfiguration(stack, item.profile()).active().harvest == HarvestAbility.SILK) return true;
        return typeMatches(item.toolType(), state);
    }

    public static boolean canShearBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, HbmToolItems.AbilityItem item) {
        return item.profile().shears() && state.getBlock() instanceof IShearable shearable && shearable.isShearable(player, stack, level, pos);
    }

    private static boolean isKeyhole(BlockState state) {
        ResourceKey<Block> key = state.getBlockHolder().unwrapKey().orElse(null);
        return key != null && key.location().equals(AtomicLegacy.id("stone_keyhole"));
    }

    /** ItemToolAbility.onBlockStartBreak, called from the server BreakEvent; true = the vanilla break is replaced. */
    public static boolean onBlockStartBreak(ItemStack stack, BlockPos pos, ServerPlayer player, HbmToolItems.AbilityItem item) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        if (isKeyhole(state)) return false;
        if (!((canHarvestBlock(stack, state, item) || canShearBlock(stack, state, level, pos, player, item)) && item.canOperate(stack))) {
            return false;
        }
        Preset preset = getConfiguration(stack, item.profile()).active();
        IN_ABILITY.set(true);
        DROP_POS.set(pos.immutable());
        try {
            Runnable post = preHarvestAll(preset, stack, level);
            boolean skipRef = preset.area != null && onDig(preset.area, preset.areaLevel, level, pos, player, item);
            if (!skipRef) breakExtraBlock(level, pos, player, pos, item);
            post.run();
        } finally {
            IN_ABILITY.set(false);
            DROP_POS.remove();
        }
        return true;
    }

    /** SILK / LUCK pre/postHarvestAll: EnchantmentUtil add (appended) and remove (first matching entry). */
    private static Runnable preHarvestAll(Preset preset, ItemStack tool, ServerLevel level) {
        if (preset.harvest != HarvestAbility.SILK && preset.harvest != HarvestAbility.LUCK) return () -> { };
        ResourceKey<Enchantment> key = preset.harvest == HarvestAbility.SILK ? Enchantments.SILK_TOUCH : Enchantments.FORTUNE;
        int added = preset.harvest == HarvestAbility.SILK ? 1 : preset.harvest.power(preset.harvestLevel);
        Holder<Enchantment> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        int existing = EnchantmentHelper.getItemEnchantmentLevel(holder, tool);
        // 1.7.10 reads the first list entry: an existing enchantment keeps its level during the harvest.
        if (existing == 0) EnchantmentHelper.updateEnchantments(tool, m -> m.set(holder, added));
        return () -> {
            // removeEnchantment drops the first entry, i.e. the pre-existing one when there was one.
            if (existing == 0) EnchantmentHelper.updateEnchantments(tool, m -> m.removeIf(h -> h.is(key)));
            else EnchantmentHelper.updateEnchantments(tool, m -> m.set(holder, added));
        };
    }

    /** IToolAreaAbility.onDig; returns true to skip the reference block. */
    private static boolean onDig(AreaAbility ability, int level, ServerLevel world, BlockPos pos, ServerPlayer player, HbmToolItems.AbilityItem item) {
        switch (ability) {
            case RECURSION -> {
                BlockState b = world.getBlockState(pos);
                if (b.is(Blocks.STONE) && !com.atomiclegacy.config.HbmParityV3Config.recursiveStone()) return false;
                if (b.is(Blocks.NETHERRACK) && !com.atomiclegacy.config.HbmParityV3Config.recursiveNetherrack()) return false;
                recurse(world, pos, pos, player, item, 0, (int) ability.value(level), new HashSet<>());
                return false;
            }
            case HAMMER -> {
                int range = (int) ability.value(level);
                for (int a = pos.getX() - range; a <= pos.getX() + range; a++)
                    for (int b = pos.getY() - range; b <= pos.getY() + range; b++)
                        for (int c = pos.getZ() - range; c <= pos.getZ() + range; c++) {
                            if (a == pos.getX() && b == pos.getY() && c == pos.getZ()) continue;
                            breakExtraBlock(world, new BlockPos(a, b, c), player, pos, item);
                        }
                return false;
            }
            case HAMMER_FLAT -> {
                int range = (int) ability.value(level);
                HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
                if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) return true;
                int xRange = range;
                int yRange = range;
                int zRange = 0;
                switch (blockHit.getDirection()) {
                    case DOWN, UP -> {
                        yRange = 0;
                        zRange = range;
                    }
                    case NORTH, SOUTH -> {
                        xRange = range;
                        zRange = 0;
                    }
                    case WEST, EAST -> {
                        xRange = 0;
                        zRange = range;
                    }
                }
                for (int a = pos.getX() - xRange; a <= pos.getX() + xRange; a++)
                    for (int b = pos.getY() - yRange; b <= pos.getY() + yRange; b++)
                        for (int c = pos.getZ() - zRange; c <= pos.getZ() + zRange; c++) {
                            if (a == pos.getX() && b == pos.getY() && c == pos.getZ()) continue;
                            breakExtraBlock(world, new BlockPos(a, b, c), player, pos, item);
                        }
                return false;
            }
            case EXPLOSION -> {
                float strength = ability.value(level);
                Vec3 center = Vec3.atCenterOf(pos);
                // ExplosionNT ALLDROP + NOHURT + NOPARTICLE, then createExplosion(player, ..., 0.1F, false)
                LegacyVnt.explodeNT(world, player, center, strength, 16, 1.0F, false);
                LegacyVnt.vanilla(world, player, center.x, center.y, center.z, 0.1F, false, false);
                return true;
            }
        }
        return false;
    }

    private static final List<BlockPos> OFFSETS = new ArrayList<>(26);

    static {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dy != 0 || dz != 0) OFFSETS.add(new BlockPos(dx, dy, dz));
    }

    /** RECURSION.recurse / breakExtra */
    private static void recurse(ServerLevel world, BlockPos at, BlockPos ref, ServerPlayer player, HbmToolItems.AbilityItem item, int depth, int radius, Set<BlockPos> visited) {
        List<BlockPos> shuffled = new ArrayList<>(OFFSETS);
        Collections.shuffle(shuffled);
        for (BlockPos offset : shuffled) {
            BlockPos next = at.offset(offset);
            if (visited.contains(next)) continue;
            int d = depth + 1;
            if (d > com.atomiclegacy.config.HbmParityV3Config.recursionDepth()) continue;
            visited.add(next);
            if (next.equals(ref)) continue;
            if (Math.sqrt(next.distSqr(ref)) > radius) continue;
            BlockState b = world.getBlockState(next);
            BlockState refState = world.getBlockState(ref);
            if (!isSameBlock(b, refState, world.getBlockEntity(ref), world.getBlockEntity(next))) continue;
            if (!sameMeta(b, refState)) continue;
            if (player.getMainHandItem().isEmpty()) continue;
            breakExtraBlock(world, next, player, ref, item);
            recurse(world, next, ref, player, item, d, radius, visited);
        }
    }

    private static boolean isSameBlock(BlockState b1, BlockState b2, BlockEntity te1, BlockEntity te2) {
        if (te1 instanceof FluidPipeBlockEntity p1 && te2 instanceof FluidPipeBlockEntity p2 && b1.getBlock() == b2.getBlock()) {
            return p1.type() == p2.type();
        }
        return b1.getBlock() == b2.getBlock();
    }

    /** meta == refMeta: state properties, ignoring connection flags (TE-driven in 1.7.10) and the lit redstone ore split. */
    private static boolean sameMeta(BlockState a, BlockState b) {
        for (Property<?> property : a.getProperties()) {
            if (property == BlockStateProperties.LIT && a.is(Blocks.REDSTONE_ORE)) continue;
            String name = property.getName();
            if (name.equals("north") || name.equals("south") || name.equals("east") || name.equals("west")
                    || name.equals("up") || name.equals("down")) continue;
            if (b.hasProperty(property) && !a.getValue(property).equals(b.getValue(property))) return false;
        }
        return true;
    }

    /** ItemToolAbility.breakExtraBlock */
    public static void breakExtraBlock(ServerLevel world, BlockPos pos, ServerPlayer player, BlockPos ref, HbmToolItems.AbilityItem item) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;
        if (!(canHarvestBlock(stack, state, item) || canShearBlock(stack, state, world, pos, player, item))
                || state.getDestroySpeed(world, pos) == -1.0F || isKeyhole(state)) return;
        BlockState refState = world.getBlockState(ref);
        float refStrength = refState.getDestroyProgress(player, world, ref);
        float strength = state.getDestroyProgress(player, world, pos);
        if (!state.canHarvestBlock(world, pos, player) || refStrength / strength > 10f) return;
        BlockEvent.BreakEvent event = CommonHooks.fireBlockBreak(world, player.gameMode.getGameModeForPlayer(), player, pos, state);
        if (event.isCanceled()) return;
        Preset preset = getConfiguration(stack, item.profile()).active();
        onHarvestBlock(preset, world, pos, player, state);
    }

    /** IToolHarvestAbility.onHarvestBlock */
    private static void onHarvestBlock(Preset preset, ServerLevel world, BlockPos pos, ServerPlayer player, BlockState state) {
        HarvestAbility ability = preset.harvest;
        if (ability == null || ability == HarvestAbility.SILK || ability == HarvestAbility.LUCK) {
            harvestBlock(false, world, pos, player);
            return;
        }
        BlockPos drop = DROP_POS.get() == null ? pos : DROP_POS.get();
        switch (ability) {
            case SMELTER -> {
                List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, world, pos, world.getBlockEntity(pos)));
                boolean doesSmelt = false;
                for (int i = 0; i < drops.size(); i++) {
                    ItemStack stack = drops.get(i).copy();
                    Optional<ItemStack> result = world.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), world)
                            .map(holder -> holder.value().getResultItem(world.registryAccess()));
                    if (result.isPresent() && !result.get().isEmpty()) {
                        ItemStack smelted = result.get().copy();
                        smelted.setCount(smelted.getCount() * stack.getCount());
                        drops.set(i, smelted);
                        doesSmelt = true;
                    }
                }
                harvestBlock(doesSmelt, world, pos, player);
                if (doesSmelt) for (ItemStack stack : drops) spawnDrop(world, drop, stack.copy());
            }
            case SHREDDER -> {
                ItemStack result = shredderResult(world, new ItemStack(state.getBlock().asItem()));
                boolean doesShred = !result.isEmpty() && !result.is(BuiltInRegistries.ITEM.get(AtomicLegacy.id("scrap")));
                harvestBlock(doesShred, world, pos, player);
                if (doesShred) spawnDrop(world, drop, result.copy());
            }
            case CENTRIFUGE -> {
                List<ItemStack> result = centrifugeResult(world, new ItemStack(state.getBlock().asItem()));
                boolean doesCentrifuge = result != null;
                harvestBlock(doesCentrifuge, world, pos, player);
                if (doesCentrifuge) for (ItemStack st : result) if (!st.isEmpty()) spawnDrop(world, drop, st.copy());
            }
            case CRYSTALLIZER -> {
                ItemStack result = crystallizerResult(world, new ItemStack(state.getBlock().asItem()));
                boolean doesCrystallize = result != null;
                harvestBlock(doesCrystallize, world, pos, player);
                if (doesCrystallize) spawnDrop(world, drop, result.copy());
            }
            case MERCURY -> {
                int mercury = 0;
                if (state.is(Blocks.REDSTONE_ORE)) mercury = player.getRandom().nextInt(5) + 4;
                if (state.is(Blocks.REDSTONE_BLOCK)) mercury = player.getRandom().nextInt(7) + 8;
                boolean doesConvert = mercury > 0;
                harvestBlock(doesConvert, world, pos, player);
                if (doesConvert) spawnDrop(world, drop, new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("nugget_mercury")), mercury));
            }
            default -> harvestBlock(false, world, pos, player);
        }
    }

    private static void spawnDrop(ServerLevel world, BlockPos drop, ItemStack stack) {
        world.addFreshEntity(new ItemEntity(world, drop.getX() + 0.5, drop.getY() + 0.5, drop.getZ() + 0.5, stack));
    }

    /** ShredderRecipes.getShredderResult (scrap when there is no recipe). */
    public static ItemStack shredderResult(ServerLevel world, ItemStack stack) {
        ItemStack scrap = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("scrap")));
        if (stack.isEmpty()) return scrap;
        return world.getRecipeManager().getRecipeFor(HbmRecipeTypes.SHREDDER.get(), new SingleRecipeInput(stack), world)
                .map(holder -> holder.value().result().copy())
                .orElse(scrap);
    }

    /** CentrifugeRecipes.getOutput (null when there is no recipe). */
    public static List<ItemStack> centrifugeResult(ServerLevel world, ItemStack stack) {
        if (stack.isEmpty()) return null;
        return world.getRecipeManager().getRecipeFor(HbmRecipeTypes.CENTRIFUGE.get(), new CentrifugeRecipe.Input(stack), world)
                .map(holder -> holder.value().results().stream().map(ItemStack::copy).toList())
                .orElse(null);
    }

    /** CrystallizerRecipes.getOutput(stack, Fluids.PEROXIDE).output (null when there is no recipe). */
    public static ItemStack crystallizerResult(ServerLevel world, ItemStack stack) {
        if (stack.isEmpty()) return null;
        HbmFluidDefinition peroxide = HbmFluids.byName("peroxide").orElse(null);
        for (var holder : world.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRYSTALLIZER.get())) {
            CrystallizerRecipe recipe = holder.value();
            if (recipe.acid().type() == peroxide && recipe.ingredient().test(stack)) return recipe.result().copy();
        }
        return null;
    }

    /** IToolHarvestAbility.harvestBlock */
    private static void harvestBlock(boolean skipDefaultDrops, ServerLevel world, BlockPos pos, ServerPlayer player) {
        if (skipDefaultDrops) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof HbmToolItems.AbilityItem item) item.damageTool(stack, 1, player);
            else if (!stack.isEmpty()) stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        } else {
            standardDigPost(world, pos, player);
        }
    }

    /** ItemToolAbility.standardDigPost (no experience drop, as in 1.7.10). */
    public static void standardDigPost(ServerLevel world, BlockPos pos, ServerPlayer player) {
        BlockState state = world.getBlockState(pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        world.levelEvent(player, 2001, pos, Block.getId(state));
        if (player.getAbilities().instabuild) {
            removeBlock(world, pos, state, false, player);
            player.connection.send(new ClientboundBlockUpdatePacket(world, pos));
            return;
        }
        ItemStack itemstack = player.getMainHandItem();
        boolean canHarvest = state.canHarvestBlock(world, pos, player);
        boolean removed = removeBlock(world, pos, state, canHarvest, player);
        ItemStack tool = itemstack.copy();
        if (!itemstack.isEmpty()) {
            itemstack.mineBlock(world, state, pos, player);
        }
        if (removed && canHarvest) {
            player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
            player.causeFoodExhaustion(0.005F);
            BlockPos drop = DROP_POS.get() == null ? pos : DROP_POS.get();
            for (ItemStack stack : Block.getDrops(state, world, pos, blockEntity, player, tool)) {
                Block.popResource(world, drop, stack);
            }
            state.spawnAfterBreak(world, pos, tool, false);
        }
    }

    private static boolean removeBlock(ServerLevel world, BlockPos pos, BlockState state, boolean canHarvest, ServerPlayer player) {
        BlockState willDestroy = state.getBlock().playerWillDestroy(world, pos, state, player);
        boolean removed = willDestroy.onDestroyedByPlayer(world, pos, player, canHarvest, world.getFluidState(pos));
        if (removed) willDestroy.getBlock().destroy(world, pos, willDestroy);
        return removed;
    }

    // ---------------------------------------------------------------- misc

    private static InteractionResult useRedstoneSword(UseOnContext context) {
        // RedstoneSword.onItemUse: offset by side, canPlayerEdit, place wire into air, always 14 damage and true.
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        ItemStack stack = context.getItemInHand();
        if (!player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            if (level.isEmptyBlock(pos)) {
                level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.ITEM_BREAK,
                        SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
            }
            stack.hurtAndBreak(14, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static int clampLevel(int level, int levels) {
        // AvailableAbilities.addAbility: illegal levels are replaced by levels() - 1.
        return level < 0 || level >= levels ? levels - 1 : level;
    }

    @EventBusSubscriber(modid = AtomicLegacy.MOD_ID)
    public static final class Events {
        private Events() {
        }

        /** Forge Item.onBlockStartBreak hook: runs after other BreakEvent listeners and replaces the vanilla break. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onBreak(BlockEvent.BreakEvent event) {
            if (event.isCanceled() || IN_ABILITY.get() || !(event.getPlayer() instanceof ServerPlayer player)) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof HbmToolItems.AbilityItem item) || !item.isAbilityTool()) return;
            if (onBlockStartBreak(stack, event.getPos(), player, item)) {
                event.setCanceled(true);
                // fireBlockBreak re-sends the old state for a cancelled event; queue the real state afterwards.
                player.serverLevel().getChunkSource().blockChanged(event.getPos());
            }
        }
    }
}
