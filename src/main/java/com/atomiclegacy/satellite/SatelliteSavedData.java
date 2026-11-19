package com.atomiclegacy.satellite;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.advancement.HbmAdvancements;
import com.atomiclegacy.items.SatelliteChipItem;
import com.atomiclegacy.inventory.recipes.SpaceAssemblerRecipe;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.entity.LegacyDeathBlastEntity;
import com.atomiclegacy.entity.OrbitalLaserEntity;
import com.atomiclegacy.entity.LegacyTomEntity;
import com.atomiclegacy.api.entity.LegacyRadarDetectable;
import com.atomiclegacy.pollution.HbmPollutionType;
import com.atomiclegacy.pollution.HbmPollutionWorlds;
import com.atomiclegacy.rtty.HbmRttySystem;
import com.atomiclegacy.rtty.RttyInteractive;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Per-world satellite registry, matching the 1.7.10 {@code satellites} WorldSavedData. */
public final class SatelliteSavedData extends SavedData {
    private static final String DATA_NAME = AtomicLegacy.MOD_ID + "_satellites";
    private static final int SCIENCE_COOLDOWN = 15 * 60 * 20;
    private static final int SENSOR_DURATION = 100 * 60 * 60 * 20;
    private static final int DEATH_RAY_CHARGE_TICKS = 5 * 60 * 20;
    private static final int PRECISION_LASER_CHARGE_TICKS = 5 * 20;
    private static final double MINER_SPEED = 1.0D / (15 * 60 * 20);
    private static final String DRIVE_DISK_EMPTY = "drive_disk_empty";
    private static final String DRIVE_DISK_FLIGHTDATA = "drive_disk_flightdata";
    private static final String DRIVE_DISK_ORBITDATA = "drive_disk_orbitdata";
    private static final SavedData.Factory<SatelliteSavedData> FACTORY = new SavedData.Factory<>(
            SatelliteSavedData::new,
            SatelliteSavedData::load
    );

    private final Map<Integer, SatelliteRecord> satellites = new HashMap<>();

    public static SatelliteSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean isFrequencyTaken(int frequency) {
        return this.satellites.containsKey(frequency);
    }

    /** [parity P2: /ntmsatellites] SatelliteSavedData.sats view for "list" and tab completion. */
    public Map<Integer, SatelliteRecord> satellites() {
        return java.util.Collections.unmodifiableMap(this.satellites);
    }

    /** [parity P2: /ntmsatellites] "descend": removes the satellite of this frequency. */
    public boolean descend(int frequency) {
        if (this.satellites.remove(frequency) == null) return false;
        setDirty();
        return true;
    }

    public Optional<SatelliteRecord> satellite(int frequency) {
        return Optional.ofNullable(this.satellites.get(frequency));
    }

    /** TileEntityMachineSatDock stores the last miner-cargo dispatch on the satellite itself. */
    public boolean markMinerDelivery(int frequency, long timestamp) {
        SatelliteRecord record = this.satellites.get(frequency);
        if (record == null || (record.kind != SatelliteKind.MINER && record.kind != SatelliteKind.LUNAR_MINER)) {
            return false;
        }
        record.lastOperation = timestamp;
        setDirty();
        return true;
    }

    /**
     * SatelliteBase#onCoordAction as dispatched by ItemSatDesignator (ray
     * trace) and ItemSatInterface/GUIScreenSatCoord (typed coordinates; y is
     * -1 when the Y field was left empty).  Only the death ray, Horizons,
     * precision laser and resonator override it; every other satellite
     * ignores the action silently.
     */
    // [parity P2: satellites] all four onCoordAction overrides, no extra chat messages
    public boolean performCoordinateAction(ServerLevel level, ServerPlayer player, SatelliteRecord record, int x, int y, int z) {
        switch (record.kind) {
            case LASER -> {
                record.targetX = x;
                record.targetZ = z;
                record.fireDeathRay(level, x, z);
            }
            case HORIZONS -> {
                record.targetX = x;
                record.targetZ = z;
                record.activateHorizons(level);
            }
            case PRECISION_LASER -> {
                record.targetX = x;
                record.targetZ = z;
                record.firePrecisionLaser(level, x, z);
            }
            case RESONATOR -> {
                // SatelliteResonator: sound at the departure and arrival
                // positions of the player, surface height for y < 0.
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                player.stopRiding();
                level.getChunk(x >> 4, z >> 4);
                int targetY = y < 0 ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) : y;
                player.connection.teleport(x + 0.5D, targetY, z + 0.5D, player.getYRot(), player.getXRot());
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
            default -> {
                return false;
            }
        }
        setDirty();
        return true;
    }

    public boolean performCoordinateAction(ServerLevel level, ServerPlayer player, SatelliteRecord record, BlockPos target) {
        return performCoordinateAction(level, player, record, target.getX(), target.getY(), target.getZ());
    }

    /** Execute the old laser satellite's panel click cooldown. */
    public boolean performPanelAction(ServerLevel level, SatelliteRecord record, BlockPos target) {
        if (record.kind != SatelliteKind.LASER
                || record.lastOperation + DEATH_RAY_CHARGE_TICKS >= level.getGameTime()) {
            return false;
        }
        record.lastOperation = level.getGameTime();
        setDirty();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ());
        // SatelliteLaser assigned the integer X/Z target directly to
        // EntityDeathBlast; do not add the half-block offset used by ordinary
        // block-centered effects.
        level.addFreshEntity(new LegacyDeathBlastEntity(
                level, target.getX(), y, target.getZ()));
        return true;
    }

    /**
     * Latest XSatelliteRegistry semantics: an occupied frequency receives the
     * payload as a part; only a previously unused frequency creates a new
     * satellite. This is what makes science-satellite sensors, assemblers and
     * orbital assembly jobs work without replacing the laboratory itself.
     */
    public boolean orbit(ServerLevel level, ItemStack payload) {
        return orbit(level, payload, 0.0D, 0.0D);
    }

    public boolean orbit(ServerLevel level, ItemStack payload, double launchX, double launchZ) {
        int frequency = SatelliteChipItem.frequency(payload);
        SatelliteRecord existing = this.satellites.get(frequency);
        if (existing != null) {
            if (!existing.deliverPart(level, payload)) {
                return false;
            }
            setDirty();
            return true;
        }

        SatelliteKind kind = SatelliteKind.fromItem(payload);
        if (kind == null) {
            return false;
        }
        SatelliteRecord record = new SatelliteRecord(kind);
        record.targetX = (int) Math.floor(launchX);
        record.targetZ = (int) Math.floor(launchZ);
        this.satellites.put(frequency, record);
        // [parity P2: satellites] SatelliteBase#onOrbit RTTY announcement; only
        // SatelliteHorizons grants an achievement (FOEQ is never triggered).
        HbmRttySystem.broadcast(level, "SAT_LINK", "Established connection to " + record.type()
                + " at " + record.targetX + " / " + record.targetZ);
        if (kind == SatelliteKind.HORIZONS) {
            HbmAdvancements.awardAll(level, "horizons_start");
        }
        setDirty();
        return true;
    }

    /** Runs the mutable latest-satellite state once per server-level tick. */
    public void tick(ServerLevel level) {
        boolean changed = false;
        for (SatelliteRecord record : this.satellites.values()) {
            changed |= record.tick(level);
        }
        if (changed) {
            setDirty();
        }
    }

    private static SatelliteSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SatelliteSavedData data = new SatelliteSavedData();
        ListTag entries = tag.getList("satellites", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            SatelliteKind kind = SatelliteKind.byId(entry.getString("kind"));
            if (kind != null) {
                data.satellites.put(entry.getInt("frequency"), SatelliteRecord.load(kind, entry, registries));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (Map.Entry<Integer, SatelliteRecord> entry : this.satellites.entrySet()) {
            CompoundTag satelliteTag = new CompoundTag();
            satelliteTag.putInt("frequency", entry.getKey());
            entry.getValue().save(satelliteTag, registries);
            entries.add(satelliteTag);
        }
        tag.put("satellites", entries);
        return tag;
    }

    /** Stable 1.7.10 Satellite.register() order and item mapping. */
    public enum SatelliteKind {
        MAPPER("mapper"),
        SCANNER("scanner"),
        RADAR("radar"),
        LASER("laser"),
        RESONATOR("resonator"),
        RELAY("relay"),
        MINER("miner"),
        LUNAR_MINER("lunar_miner"),
        HORIZONS("horizons"),
        PRECISION_LASER("precision_laser"),
        DETECTOR("detector"),
        RAY_SCAN("ray_scan"),
        SCIENCE("science");

        private final String id;

        SatelliteKind(String id) {
            this.id = id;
        }

        /** Satellite class simple name printed by /ntmsatellites list. */
        public String className() {
            return switch (this) {
                case MAPPER -> "SatelliteMapper";
                case SCANNER -> "SatelliteScanner";
                case RADAR -> "SatelliteRadar";
                case LASER -> "SatelliteDeathRay";
                case RESONATOR -> "SatelliteResonator";
                case RELAY -> "SatelliteRelay";
                case MINER -> "SatelliteMiner";
                case LUNAR_MINER -> "SatelliteLunarMiner";
                case HORIZONS -> "SatelliteHorizons";
                case PRECISION_LASER -> "SatellitePrecisionLaser";
                case DETECTOR -> "SatelliteDetector";
                case RAY_SCAN -> "SatelliteRayScan";
                case SCIENCE -> "SatelliteScience";
            };
        }

        /** Stable serialized/interface identifier used by the client panel. */
        public String id() {
            return this.id;
        }

        private static SatelliteKind byId(String id) {
            for (SatelliteKind kind : values()) {
                if (kind.id.equals(id)) return kind;
            }
            return null;
        }

        // [parity P2: satellites] XSatelliteRegistry#registerSatellite refuses a class that is
        // already mapped, so the legacy sat_mapper/sat_laser/... chips and sat_foeq never
        // create a satellite; only the ItemSatellite variants and sat_gerald do.
        public static SatelliteKind fromItem(ItemStack stack) {
            if (stack.is(HbmItems.SATELLITE_SPY.get())) return MAPPER;
            if (stack.is(HbmItems.SATELLITE_SCANNER.get())) return SCANNER;
            if (stack.is(HbmItems.SATELLITE_RADAR.get())) return RADAR;
            if (stack.is(HbmItems.SATELLITE_DEATH_RAY.get())) return LASER;
            if (stack.is(HbmItems.SATELLITE_XENIUM_RESONATOR.get())) return RESONATOR;
            if (stack.is(HbmItems.SATELLITE_RELAY.get())) return RELAY;
            if (stack.is(HbmItems.SATELLITE_MINER_ASTRO.get())) return MINER;
            if (stack.is(HbmItems.SATELLITE_MINER_LUNAR.get())) return LUNAR_MINER;
            if (stack.is(HbmItems.SAT_GERALD.get())) return HORIZONS;
            if (stack.is(HbmItems.SATELLITE_PRECISION_LASER.get())) return PRECISION_LASER;
            if (stack.is(HbmItems.SATELLITE_DETECTOR.get())) return DETECTOR;
            if (stack.is(HbmItems.SATELLITE_RAY_SCAN.get())) return RAY_SCAN;
            if (stack.is(HbmItems.SATELLITE_SCIENCE.get())) return SCIENCE;
            return null;
        }
    }

    /** State serialized by the mutable 1.7.10 satellite subclasses. */
    public static final class SatelliteRecord {
        private final SatelliteKind kind;
        private long lastOperation;
        private boolean used;
        private int targetX;
        private int targetZ;
        private String tx = "";
        private int targetedEntity = -1;
        private double minerProgress;
        private String driveInput = "";
        private String driveOutput = "";
        private final List<ItemStack> requestableItems = new ArrayList<>();
        private long lastScience;
        private int sensorProgress;
        private int sensorCount;
        private int assemblerCount;
        private double assemblerProgress;
        private final List<String> assemblerTasks = new ArrayList<>();
        private transient List<Integer> cachedRadarResults = new ArrayList<>();
        private transient List<Integer> filteredRadarResults = new ArrayList<>();
        private transient List<SatelliteEventTracker.RadiationBurst> cachedRadiationBursts = new ArrayList<>();
        private transient List<SatelliteEventTracker.RayEvent> cachedRayEvents = new ArrayList<>();

        private SatelliteRecord(SatelliteKind kind) {
            this.kind = kind;
        }

        private static SatelliteRecord load(SatelliteKind kind, CompoundTag tag, HolderLookup.Provider registries) {
            SatelliteRecord record = new SatelliteRecord(kind);
            record.lastOperation = tag.getLong("lastOperation");
            record.used = tag.getBoolean("used");
            // [parity P2: satellites] SatelliteHorizons#writeToNBT/readFromNBT skip super:
            // only "used" survives a reload (target and tx reset to 0/"").
            if (kind != SatelliteKind.HORIZONS) {
                record.targetX = tag.getInt("targetX");
                record.targetZ = tag.getInt("targetZ");
                record.tx = tag.getString("tx");
            }
            record.targetedEntity = tag.contains("targetedEntity") ? tag.getInt("targetedEntity") : -1;
            record.minerProgress = tag.getDouble("minerProgress");
            record.driveInput = tag.getString("driveInput");
            record.driveOutput = tag.getString("driveOutput");
            record.lastScience = tag.getLong("lastScience");
            record.sensorProgress = tag.getInt("sensorProgress");
            record.sensorCount = tag.getInt("sensorCount");
            record.assemblerCount = tag.getInt("assemblerCount");
            // SatelliteScience#readFromNBT never reads assemblerProgress back.
            ListTag items = tag.getList("requestableItems", Tag.TAG_COMPOUND);
            for (int index = 0; index < items.size(); index++) {
                ItemStack stack = ItemStack.parseOptional(registries, items.getCompound(index));
                if (!stack.isEmpty()) record.requestableItems.add(stack);
            }
            ListTag tasks = tag.getList("assemblerTasks", Tag.TAG_STRING);
            for (int index = 0; index < tasks.size(); index++) {
                String task = tasks.getString(index);
                if (!task.isBlank()) record.assemblerTasks.add(task);
            }
            return record;
        }

        private void save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putString("kind", this.kind.id);
            tag.putLong("lastOperation", this.lastOperation);
            tag.putBoolean("used", this.used);
            tag.putInt("targetX", this.targetX);
            tag.putInt("targetZ", this.targetZ);
            tag.putString("tx", this.tx);
            tag.putInt("targetedEntity", this.targetedEntity);
            tag.putDouble("minerProgress", this.minerProgress);
            if (!this.driveInput.isEmpty()) tag.putString("driveInput", this.driveInput);
            if (!this.driveOutput.isEmpty()) tag.putString("driveOutput", this.driveOutput);
            tag.putLong("lastScience", this.lastScience);
            tag.putInt("sensorProgress", this.sensorProgress);
            tag.putInt("sensorCount", this.sensorCount);
            tag.putInt("assemblerCount", this.assemblerCount);
            tag.putDouble("assemblerProgress", this.assemblerProgress);
            ListTag items = new ListTag();
            for (ItemStack stack : this.requestableItems) {
                if (!stack.isEmpty()) items.add(stack.saveOptional(registries));
            }
            tag.put("requestableItems", items);
            ListTag tasks = new ListTag();
            for (String task : this.assemblerTasks) tasks.add(net.minecraft.nbt.StringTag.valueOf(task));
            tag.put("assemblerTasks", tasks);
        }

        private boolean deliverPart(ServerLevel level, ItemStack part) {
            if (this.kind != SatelliteKind.SCIENCE) return false;
            if (part.is(HbmItems.SATELLITE_SCIENCE_SENSOR.get())) {
                this.sensorCount++;
                return true;
            }
            if (part.is(HbmItems.SATELLITE_SCIENCE_ASSEMBLER.get())) {
                this.assemblerCount++;
                return true;
            }
            Optional<RecipeHolder<SpaceAssemblerRecipe>> recipe = level.getRecipeManager().getRecipeFor(
                    HbmRecipeTypes.SPACE_ASSEMBLER.get(), new SingleRecipeInput(part), level);
            if (recipe.isPresent()) {
                SpaceAssemblerRecipe value = recipe.get().value();
                this.assemblerTasks.add(value.getGroup().isBlank()
                        ? recipe.get().id().toString()
                        : value.getGroup());
                return true;
            }
            return false;
        }

        private boolean tick(ServerLevel level) {
            if (this.kind == SatelliteKind.MINER || this.kind == SatelliteKind.LUNAR_MINER) {
                if (!this.requestableItems.isEmpty()) return false;
                this.minerProgress += MINER_SPEED;
                if (this.minerProgress >= 1.0D) {
                    this.minerProgress = 0.0D;
                    MinerLoot[] pool = this.kind == SatelliteKind.LUNAR_MINER
                            ? LUNAR_MINER_CARGO : ASTEROID_MINER_CARGO;
                    int count = 10 + level.random.nextInt(6);
                    for (int index = 0; index < count; index++) {
                        ItemStack result = chooseMinerLoot(pool, level.random.nextInt(totalWeight(pool)))
                                .create(level.random);
                        if (!result.isEmpty()) this.requestableItems.add(result);
                    }
                }
                return level.getGameTime() % 1_200L == 0L || this.minerProgress == 0.0D;
            }
            if (this.kind != SatelliteKind.SCIENCE) return false;
            boolean changed = false;
            if (this.sensorProgress < SENSOR_DURATION) {
                if (this.sensorCount > 0) {
                    this.sensorProgress += this.sensorCount;
                    changed = true;
                }
            } else {
                this.sensorProgress = 0;
                produceData(DRIVE_DISK_EMPTY, DRIVE_DISK_ORBITDATA);
                changed = true;
            }

            if (this.assemblerCount > 0 && this.requestableItems.isEmpty() && !this.assemblerTasks.isEmpty()) {
                String task = this.assemblerTasks.getFirst();
                RecipeHolder<SpaceAssemblerRecipe> recipe = spaceAssemblerRecipe(level, task).orElse(null);
                if (recipe == null) {
                    this.assemblerProgress = 0.0D;
                    this.assemblerTasks.removeFirst();
                    return true;
                }
                this.assemblerProgress += (double) this.assemblerCount / recipe.value().duration();
                if (this.assemblerProgress >= 1.0D) {
                    for (ItemStack result : recipe.value().results()) {
                        this.requestableItems.add(result.copy());
                    }
                    this.assemblerProgress = 0.0D;
                    this.assemblerTasks.removeFirst();
                }
                changed = true;
            }
            return changed;
        }

        private static final MinerLoot[] ASTEROID_MINER_CARGO = {
                minerLoot("powder_aluminium", 3, 3, 10), minerLoot("powder_iron", 3, 3, 10),
                minerLoot("powder_titanium", 2, 2, 8), minerLoot("crystal_tungsten", 2, 2, 7),
                minerLoot("powder_coal", 4, 4, 15), minerLoot("powder_uranium", 2, 2, 5),
                minerLoot("powder_plutonium", 1, 1, 5), minerLoot("powder_thorium", 2, 2, 7),
                minerLoot("powder_desh_mix", 3, 3, 5), minerLoot("powder_diamond", 2, 2, 7),
                minerLoot("minecraft:redstone", 5, 5, 15), minerLoot("powder_nitan_mix", 2, 2, 5),
                minerLoot("powder_power", 2, 2, 5), minerLoot("powder_copper", 5, 5, 15),
                minerLoot("powder_lead", 3, 3, 10), minerLoot("fluorite", 4, 4, 15),
                minerLoot("powder_lapis", 4, 4, 10), minerLoot("crystal_aluminium", 1, 1, 5),
                minerLoot("crystal_gold", 1, 1, 5), minerLoot("crystal_phosphorus", 1, 1, 10),
                minerLoot("gravel_diamond", 1, 1, 3), minerLoot("crystal_uranium", 1, 1, 3),
                minerLoot("crystal_plutonium", 1, 1, 3), minerLoot("crystal_trixite", 1, 1, 1),
                minerLoot("crystal_starmetal", 1, 1, 1), minerLoot("crystal_lithium", 2, 2, 4)
        };
        private static final MinerLoot[] LUNAR_MINER_CARGO = {
                minerLoot("moon_turf", 48, 48, 5), minerLoot("moon_turf", 32, 32, 7),
                minerLoot("moon_turf", 16, 16, 5), minerLoot("powder_lithium", 3, 3, 5),
                minerLoot("powder_iron", 3, 3, 5), minerLoot("crystal_iron", 1, 1, 1),
                minerLoot("crystal_lithium", 1, 1, 1)
        };

        private static MinerLoot minerLoot(String id, int minimum, int maximum, int weight) {
            return new MinerLoot(id, minimum, maximum, weight);
        }

        private static int totalWeight(MinerLoot[] pool) {
            int total = 0;
            for (MinerLoot entry : pool) total += entry.weight;
            return total;
        }

        private static MinerLoot chooseMinerLoot(MinerLoot[] pool, int roll) {
            for (MinerLoot entry : pool) {
                roll -= entry.weight;
                if (roll < 0) return entry;
            }
            return pool[pool.length - 1];
        }

        private record MinerLoot(String id, int minimum, int maximum, int weight) {
            private ItemStack create(net.minecraft.util.RandomSource random) {
                ResourceLocation key = this.id.contains(":")
                        ? ResourceLocation.parse(this.id)
                        : AtomicLegacy.id(this.id);
                net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(key);
                if (item == null || item == net.minecraft.world.level.block.Blocks.AIR.asItem()) {
                    return ItemStack.EMPTY;
                }
                return new ItemStack(item, this.minimum + random.nextInt(this.maximum - this.minimum + 1));
            }
        }

        private static Optional<RecipeHolder<SpaceAssemblerRecipe>> spaceAssemblerRecipe(ServerLevel level, String task) {
            return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SPACE_ASSEMBLER.get()).stream()
                    .filter(holder -> task.equals(holder.value().getGroup()) || task.equals(holder.id().toString()))
                    .findFirst();
        }

        /** Stable machine-readable identifier returned by TileEntityMachineSatLink. */
        public String type() {
            return switch (this.kind) {
                case MAPPER -> "NOT_A_SPY_SATELLITE_:)";
                case SCANNER -> "DEPTH_SCANNER";
                case RADAR -> "LEO_RADAR";
                case LASER -> "ORBITAL_FUN_PLATFORM_:)";
                case RESONATOR -> "XEN_RELAY";
                case RELAY -> "DIMENSIONAL_RELAY";
                case MINER -> "ASTEROID_MINER";
                case LUNAR_MINER -> "LUNAR_MINER";
                case HORIZONS -> "PAYLOAD_UNKNOWN";
                case PRECISION_LASER -> "ORBITAL_TATOO_REMOVER";
                case DETECTOR -> "UWB_EMISSION_DETECTOR";
                case RAY_SCAN -> "NB_RAY_SCANNER";
                case SCIENCE -> "SCIENCE_PROBE";
            };
        }

        public String tx() {
            return this.tx;
        }

        /**
         * Executes the exact text command surface used by SatelliteBase and
         * its latest 1.7.10 subclasses.  OpenComputers is intentionally not
         * involved; the caller is HBM's own RTTY controller.
         */
        public void onCommand(ServerLevel level, String... command) {
            if (command.length == 0 || command[0] == null) return;
            // [parity P2: satellites] SatelliteBase compares command names with equals().
            String name = command[0];

            if (name.equals("settarget")) {
                if (command.length == 3) {
                    this.targetX = RttyInteractive.parseInt(command[1]);
                    this.targetZ = RttyInteractive.parseInt(command[2]);
                } else if (command.length == 4) {
                    // Preserve the four-token coordinate form from SatelliteBase.
                    this.targetX = RttyInteractive.parseInt(command[1]);
                    this.targetZ = RttyInteractive.parseInt(command[3]);
                }
            } else if (name.equals("gettarget")) {
                this.tx = this.targetX + ";" + this.targetZ;
            } else if (name.equals("gettargetx")) {
                this.tx = Integer.toString(this.targetX);
            } else if (name.equals("gettargetz")) {
                this.tx = Integer.toString(this.targetZ);
            }

            switch (this.kind) {
                case MAPPER -> mapperCommand(level, name, command);
                case RADAR -> radarCommand(level, name, command);
                case LASER -> deathRayCommand(level, name);
                case RELAY -> relayCommand(level, name, command);
                case HORIZONS -> horizonsCommand(level, name);
                case PRECISION_LASER -> precisionLaserCommand(level, name, command);
                case DETECTOR -> detectorCommand(level, name, command);
                case RAY_SCAN -> rayScanCommand(level, name, command);
                default -> {
                    // Scanner, resonator, miners and science use only the
                    // common target commands or their direct item workflows.
                }
            }
        }

        private void mapperCommand(ServerLevel level, String name, String[] command) {
            if (name.equals("targetloaded")) {
                this.tx = Boolean.toString(level.hasChunk(this.targetX >> 4, this.targetZ >> 4))
                        .toUpperCase(Locale.ROOT);
                return;
            }
            if (name.equals("getsmog")) {
                this.tx = Integer.toString((int) Math.ceil(HbmPollutionWorlds.get(
                        level, new BlockPos(this.targetX, level.getMaxBuildHeight() - 1, this.targetZ),
                        HbmPollutionType.SOOT)));
                return;
            }
            if (name.equals("findplayer") && command.length == 2) {
                String lookingFor = command[1].toLowerCase(Locale.US);
                for (ServerPlayer player : level.players()) {
                    int x = (int) Math.floor(player.getX());
                    int z = (int) Math.floor(player.getZ());
                    double deltaX = x - this.targetX;
                    double deltaZ = z - this.targetZ;
                    if (deltaX * deltaX + deltaZ * deltaZ <= 250 * 250
                            && player.getGameProfile().getName().toLowerCase(Locale.US).equals(lookingFor)
                            && level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) < player.getY() + 2.0D) {
                        this.tx = x + ";" + z;
                        return;
                    }
                }
                this.tx = "NOT FOUND";
                return;
            }
            if (!name.equals("spotplayers")) return;
            List<String> names = new ArrayList<>();
            long maximumDistanceSquared = 250L * 250L;
            for (ServerPlayer player : level.players()) {
                int x = (int) Math.floor(player.getX());
                int z = (int) Math.floor(player.getZ());
                long deltaX = (long) x - this.targetX;
                long deltaZ = (long) z - this.targetZ;
                if (deltaX * deltaX + deltaZ * deltaZ <= maximumDistanceSquared
                        && level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) < player.getY() + 2.0D) {
                    names.add(player.getGameProfile().getName());
                }
            }
            this.tx = names.isEmpty() ? "NONE" : String.join(";", names);
        }

        private void radarCommand(ServerLevel level, String name, String[] command) {
            if (name.equals("survey")) {
                this.cachedRadarResults.clear();
                AABB area = new AABB(
                        this.targetX - 1_000.0D, level.getMinBuildHeight(), this.targetZ - 1_000.0D,
                        this.targetX + 1_000.0D, level.getMaxBuildHeight(), this.targetZ + 1_000.0D);
                // [parity P2: satellites] TileEntityMachineRadarNT.matchingEntities holds every
                // player and radar-detectable entity; the survey applies no scan params.
                for (Entity entity : level.getEntities((Entity) null, area, Entity::isAlive)) {
                    if (!(entity instanceof Player) && !(entity instanceof LegacyRadarDetectable)) continue;
                    double deltaX = (int) Math.floor(entity.getX()) - this.targetX;
                    double deltaZ = (int) Math.floor(entity.getZ()) - this.targetZ;
                    if (deltaX * deltaX + deltaZ * deltaZ <= 1_000.0D * 1_000.0D) {
                        this.cachedRadarResults.add(entity.getId());
                    }
                }
                this.filteredRadarResults = new ArrayList<>(this.cachedRadarResults);
                return;
            }
            if (name.equals("filter") && command.length == 2) {
                String filter = command[1].toLowerCase(Locale.ROOT);
                this.filteredRadarResults.clear();
                for (int entityId : this.cachedRadarResults) {
                    Entity entity = level.getEntity(entityId);
                    if (entity != null && entity.isAlive()
                            && entity.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains(filter)) {
                        this.filteredRadarResults.add(entityId);
                    }
                }
                return;
            }
            if (name.equals("count")) {
                this.tx = Integer.toString(this.filteredRadarResults.size());
                return;
            }
            if (command.length != 2) return;
            Entity target = radarTarget(level, command[1]);
            if (target == null) {
                this.tx = "";
            } else if (name.equals("gettargetid")) {
                this.tx = Integer.toString(target.getId());
            } else if (name.equals("getposition")) {
                this.tx = (int) Math.floor(target.getX()) + ";"
                        + (int) Math.floor(target.getY()) + ";"
                        + (int) Math.floor(target.getZ());
            } else if (name.equals("getname")) {
                this.tx = target.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            }
        }

        private Entity radarTarget(ServerLevel level, String indexText) {
            if (this.filteredRadarResults.isEmpty()) return null;
            int index = RttyInteractive.parseInt(indexText, 1, this.filteredRadarResults.size()) - 1;
            Entity target = level.getEntity(this.filteredRadarResults.get(index));
            return target != null && target.isAlive() ? target : null;
        }

        private void deathRayCommand(ServerLevel level, String name) {
            if (name.equals("fire")) {
                fireDeathRay(level, this.targetX, this.targetZ);
            } else if (name.equals("canfire")) {
                this.tx = Boolean.toString(canFireDeathRay(level)).toUpperCase(Locale.ROOT);
            }
        }

        private boolean canFireDeathRay(ServerLevel level) {
            return this.lastOperation + DEATH_RAY_CHARGE_TICKS < level.getGameTime();
        }

        private void fireDeathRay(ServerLevel level, int x, int z) {
            if (!canFireDeathRay(level)) return;
            this.lastOperation = level.getGameTime();
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            level.addFreshEntity(new LegacyDeathBlastEntity(level, x, y, z));
        }

        private void relayCommand(ServerLevel level, String name, String[] command) {
            if (!name.equals("relay") || command.length <= 3) return;
            ServerLevel targetLevel = relayLevel(level, command[1]);
            if (targetLevel == null) return;
            HbmRttySystem.broadcast(targetLevel, command[2],
                    String.join(" ", java.util.Arrays.copyOfRange(command, 3, command.length)));
        }

        private static ServerLevel relayLevel(ServerLevel source, String value) {
            ResourceKey<Level> key;
            if (value.equals("-1")) key = Level.NETHER;
            else if (value.equals("0")) key = Level.OVERWORLD;
            else if (value.equals("1")) key = Level.END;
            else {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id == null) return null;
                key = ResourceKey.create(Registries.DIMENSION, id);
            }
            return source.getServer().getLevel(key);
        }

        private void horizonsCommand(ServerLevel level, String name) {
            if (name.equals("fire")) {
                activateHorizons(level);
            }
            // SatelliteHorizons in the latest reference intentionally names
            // this command "settarget". Preserve that observable behavior.
            if (name.equals("settarget")) {
                this.tx = Boolean.toString(!this.used).toUpperCase(Locale.ROOT);
            }
        }

        private void activateHorizons(ServerLevel level) {
            if (this.used) return;
            this.used = true;
            this.lastOperation = level.getGameTime();
            LegacyTomEntity.spawn(level, this.targetX, this.targetZ);
            HbmAdvancements.awardAll(level, "horizons_end");
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("Horizons has been activated.").withStyle(net.minecraft.ChatFormatting.RED), false);
        }

        private void precisionLaserCommand(ServerLevel level, String name, String[] command) {
            if (name.equals("fire")) {
                if (this.targetedEntity != -1) {
                    Entity target = level.getEntity(this.targetedEntity);
                    this.targetedEntity = -1;
                    if (target == null || !target.isAlive()) return;

                    int x = (int) Math.floor(target.getX());
                    int z = (int) Math.floor(target.getZ());
                    double deltaX = x - this.targetX;
                    double deltaZ = z - this.targetZ;
                    if (deltaX * deltaX + deltaZ * deltaZ <= 1_000.0D * 1_000.0D) {
                        double offsetX = level.random.nextDouble() * 0.05D - 0.025D;
                        double offsetY = level.random.nextDouble() * 0.05D - 0.025D;
                        double offsetZ = level.random.nextDouble() * 0.05D - 0.025D;
                        firePrecisionLaser(level, target.getX() + offsetX,
                                target.getY() + offsetY, target.getZ() + offsetZ);
                        return;
                    }
                }
                firePrecisionLaser(level, this.targetX, this.targetZ);
            } else if (name.equals("canfire")) {
                this.tx = Boolean.toString(
                        this.lastOperation + PRECISION_LASER_CHARGE_TICKS < level.getGameTime())
                        .toUpperCase(Locale.ROOT);
            } else if (name.equals("setentitytarget") && command.length == 2) {
                this.targetedEntity = RttyInteractive.parseInt(command[1]);
            }
        }

        private boolean firePrecisionLaser(ServerLevel level, int x, int z) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            return firePrecisionLaser(level, x + 0.5D, y, z + 0.5D);
        }

        private boolean firePrecisionLaser(ServerLevel level, double x, double y, double z) {
            if (this.lastOperation + PRECISION_LASER_CHARGE_TICKS >= level.getGameTime()) return false;
            this.lastOperation = level.getGameTime();
            OrbitalLaserEntity blast = new OrbitalLaserEntity(level, x, y, z);
            blast.explode();
            level.addFreshEntity(blast);
            return true;
        }

        private void detectorCommand(ServerLevel level, String name, String[] command) {
            if (name.equals("survey")) {
                this.cachedRadiationBursts = new ArrayList<>(SatelliteEventTracker.radiationBursts(level));
            } else if (name.equals("count")) {
                this.tx = Integer.toString(this.cachedRadiationBursts.size());
            } else if (command.length == 2) {
                SatelliteEventTracker.RadiationBurst burst = radiationBurst(command[1]);
                if (burst == null) this.tx = "";
                else if (name.equals("gettype")) this.tx = burst.intensity().name();
                else if (name.equals("getposition")) this.tx = burst.x() + ";" + burst.z();
            }
        }

        private SatelliteEventTracker.RadiationBurst radiationBurst(String indexText) {
            if (this.cachedRadiationBursts.isEmpty()) return null;
            int index = RttyInteractive.parseInt(indexText, 1, this.cachedRadiationBursts.size()) - 1;
            return this.cachedRadiationBursts.get(index);
        }

        private void rayScanCommand(ServerLevel level, String name, String[] command) {
            if (name.equals("survey")) {
                this.cachedRayEvents = new ArrayList<>(SatelliteEventTracker.rayEvents(
                        level, this.targetX, this.targetZ, 250));
            } else if (name.equals("count")) {
                this.tx = Integer.toString(this.cachedRayEvents.size());
            } else if (command.length == 2) {
                SatelliteEventTracker.RayEvent event = rayEvent(command[1]);
                if (event == null) this.tx = "";
                else if (name.equals("getinfo")) this.tx = event.info();
                else if (name.equals("getposition")) this.tx = event.x() + ";" + event.z();
            }
        }

        private SatelliteEventTracker.RayEvent rayEvent(String indexText) {
            if (this.cachedRayEvents.isEmpty()) return null;
            int index = RttyInteractive.parseInt(indexText, 1, this.cachedRayEvents.size()) - 1;
            return this.cachedRayEvents.get(index);
        }

        // [parity P2: satellites] getInfo() of each SatelliteBase subclass, including the
        // pre-formatted "12s" / "3m5s" / "50%" arguments and BobMathUtil.format grouping.
        public List<SatelliteInfoLine> infoLines(ServerLevel level) {
            List<SatelliteInfoLine> lines = new ArrayList<>();
            lines.add(SatelliteInfoLine.of(satelliteTranslationKey()));
            if (this.kind == SatelliteKind.LASER || this.kind == SatelliteKind.PRECISION_LASER) {
                int charge = this.kind == SatelliteKind.LASER
                        ? DEATH_RAY_CHARGE_TICKS : PRECISION_LASER_CHARGE_TICKS;
                boolean canFire = this.lastOperation + charge < level.getGameTime();
                int cooldown = (int) ((this.lastOperation + charge) - level.getGameTime());
                lines.add(canFire
                        ? SatelliteInfoLine.of("satellite.atomiclegacy.ready")
                        : SatelliteInfoLine.of("satellite.atomiclegacy.cooldown", cooldown / 20 + "s"));
            } else if (this.kind == SatelliteKind.HORIZONS) {
                lines.add(SatelliteInfoLine.of(this.used
                        ? "satellite.atomiclegacy.spent" : "satellite.atomiclegacy.ready"));
            } else if (this.kind == SatelliteKind.MINER || this.kind == SatelliteKind.LUNAR_MINER) {
                lines.add(SatelliteInfoLine.of("satellite.atomiclegacy.miner_progress",
                        (int) Math.round(this.minerProgress * 100.0D) + "%"));
            } else if (this.kind == SatelliteKind.SCIENCE) {
                int cooldown = (int) ((this.lastScience + SCIENCE_COOLDOWN) - level.getGameTime());
                int seconds = cooldown / 20;
                lines.add(cooldown <= 0
                        ? SatelliteInfoLine.of("satellite.atomiclegacy.ready")
                        : SatelliteInfoLine.of("satellite.atomiclegacy.cooldown", (seconds / 60) + "m" + (seconds % 60) + "s"));
                if (this.sensorCount > 0) {
                    lines.add(SatelliteInfoLine.of("satellite.atomiclegacy.sensors", Integer.toString(this.sensorCount)));
                    lines.add(SatelliteInfoLine.of("satellite.atomiclegacy.pending",
                            String.format(Locale.US, "%,d", SENSOR_DURATION - this.sensorProgress)));
                }
                if (DRIVE_DISK_ORBITDATA.equals(this.driveOutput)) {
                    lines.add(SatelliteInfoLine.of("satellite.atomiclegacy.data"));
                }
                if (this.assemblerCount > 0) {
                    lines.add(SatelliteInfoLine.of("satellite.atomiclegacy.assemblers", Integer.toString(this.assemblerCount)));
                    lines.add(SatelliteInfoLine.of("satellite.atomiclegacy.progress",
                            (int) Math.round(this.assemblerProgress * 100.0D) + "%"));
                    lines.add(SatelliteInfoLine.of("satellite.atomiclegacy.queue", Integer.toString(this.assemblerTasks.size())));
                }
            }
            return lines;
        }

        public List<Component> info(ServerLevel level) {
            return infoLines(level).stream().map(SatelliteInfoLine::component).toList();
        }

        private String satelliteTranslationKey() {
            return switch (this.kind) {
                case MAPPER -> "item.atomiclegacy.satellite_spy";
                case SCANNER -> "item.atomiclegacy.satellite_scanner";
                case RADAR -> "item.atomiclegacy.satellite_radar";
                case LASER -> "item.atomiclegacy.satellite_death_ray";
                case RESONATOR -> "item.atomiclegacy.satellite_xenium_resonator";
                case RELAY -> "item.atomiclegacy.satellite_relay";
                case MINER -> "item.atomiclegacy.satellite_miner_astro";
                case LUNAR_MINER -> "item.atomiclegacy.satellite_miner_lunar";
                case HORIZONS -> "item.atomiclegacy.sat_gerald";
                case PRECISION_LASER -> "item.atomiclegacy.satellite_precision_laser";
                case DETECTOR -> "item.atomiclegacy.satellite_detector";
                case RAY_SCAN -> "item.atomiclegacy.satellite_ray_scan";
                case SCIENCE -> "item.atomiclegacy.satellite_science";
            };
        }

        public boolean hasDriveData(ServerLevel level) {
            if (!this.driveInput.isEmpty() && !this.driveOutput.isEmpty()) return true;
            if (this.kind == SatelliteKind.SCIENCE && level.getGameTime() > this.lastScience + SCIENCE_COOLDOWN) {
                produceData(DRIVE_DISK_EMPTY, DRIVE_DISK_FLIGHTDATA);
                this.lastScience = level.getGameTime();
                SatelliteSavedData.get(level).setDirty();
            }
            return !this.driveInput.isEmpty() && !this.driveOutput.isEmpty();
        }

        public ItemStack exchangeDrive(ServerLevel level, ItemStack input) {
            if (!hasDriveData(level) || !itemPath(input).equals(this.driveInput)) return ItemStack.EMPTY;
            ItemStack result = stackForPath(this.driveOutput);
            if (result.isEmpty()) return ItemStack.EMPTY;
            this.driveInput = "";
            this.driveOutput = "";
            SatelliteSavedData.get(level).setDirty();
            return result;
        }

        private void produceData(String input, String output) {
            this.driveInput = input;
            this.driveOutput = output;
        }

        private static String itemPath(ItemStack stack) {
            if (stack.isEmpty()) return "";
            net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return AtomicLegacy.MOD_ID.equals(id.getNamespace()) ? id.getPath() : "";
        }

        private static ItemStack stackForPath(String path) {
            if (!path.equals(DRIVE_DISK_FLIGHTDATA) && !path.equals(DRIVE_DISK_ORBITDATA)) return ItemStack.EMPTY;
            return new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id(path)));
        }

        public SatelliteKind kind() {
            return this.kind;
        }

        public long lastOperation() {
            return this.lastOperation;
        }

        public boolean used() {
            return this.used;
        }

        public int targetX() { return this.targetX; }
        public int targetZ() { return this.targetZ; }
        public int sensorCount() { return this.sensorCount; }
        public int sensorProgress() { return this.sensorProgress; }
        public int assemblerCount() { return this.assemblerCount; }
        public double assemblerProgress() { return this.assemblerProgress; }
        public int assemblerQueueSize() { return this.assemblerTasks.size(); }
        public List<ItemStack> requestableItems() {
            return this.requestableItems.stream().map(ItemStack::copy).toList();
        }

        public List<ItemStack> takeRequestableItems(ServerLevel level) {
            if (this.requestableItems.isEmpty()) return List.of();
            List<ItemStack> result = this.requestableItems.stream().map(ItemStack::copy).toList();
            this.requestableItems.clear();
            SatelliteSavedData.get(level).setDirty();
            return result;
        }
    }

    public record SatelliteInfoLine(String translationKey, String value, boolean hasValue) {
        public static SatelliteInfoLine of(String translationKey) {
            return new SatelliteInfoLine(translationKey, "", false);
        }

        public static SatelliteInfoLine of(String translationKey, String value) {
            return new SatelliteInfoLine(translationKey, value, true);
        }

        public Component component() {
            return this.hasValue
                    ? Component.translatable(this.translationKey, this.value)
                    : Component.translatable(this.translationKey);
        }
    }
}
