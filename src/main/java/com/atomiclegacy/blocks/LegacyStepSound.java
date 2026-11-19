package com.atomiclegacy.blocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.atomiclegacy.mixin.parity.BlockBehaviourSoundAccessor;
import com.atomiclegacy.registry.HbmSoundTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [parity V4: step sounds] 1.7.10 stepSound of every original block (work/parity_v4_sounds.py ->
 * legacy/atomiclegacy/parity-v4-sounds.json): Block() defaults to soundTypeStone regardless of Material; BlockLeaves,
 * BlockLog, BlockStairs/BlockWall, NTMAnvil, BlockGraphiteDrilledBase constructors and ModBlocks setStepSound change
 * it. Vanilla 1.7.10 sound types map to the 1.21 SoundType of the same name, ModSoundTypes to HbmSoundTypes.
 * Applied once per block at registration; port-only blocks (no original id) keep their own sound.
 */
public final class LegacyStepSound {
    private static Map<String, String> table;
    /** Port id -> "port sound -> original sound" for every block whose sound the table changed (diagnostics dump). */
    public static final Map<String, String> CHANGED = new LinkedHashMap<>();

    /**
     * [parity V4: metal sound] 1.7.10 Block.soundTypeMetal = new SoundType("stone", 1.0F, 1.5F): the stone dig/step
     * sounds at pitch 1.5, not the separate 1.21 metal sound files.
     */
    public static final SoundType LEGACY_METAL = new SoundType(1.0F, 1.5F,
            net.minecraft.sounds.SoundEvents.STONE_BREAK, net.minecraft.sounds.SoundEvents.STONE_STEP,
            net.minecraft.sounds.SoundEvents.STONE_PLACE, net.minecraft.sounds.SoundEvents.STONE_HIT,
            net.minecraft.sounds.SoundEvents.STONE_FALL);

    private LegacyStepSound() {
    }

    public static <T extends Block> T apply(String id, T block) {
        SoundType expected = expected(id);
        if (expected != null) {
            SoundType current = block.defaultBlockState().getSoundType();
            if (current != expected) {
                synchronized (CHANGED) {
                    CHANGED.put(id, name(current) + " -> " + name(expected));
                }
                ((BlockBehaviourSoundAccessor) block).atomiclegacy$setSoundType(expected);
            }
        }
        return block;
    }

    /** Original sound name for a port id, or null for port-only blocks. */
    @Nullable
    public static String originalSound(String id) {
        // Port-only deepslate twins keep the 1.21 deepslate sound (no 1.7.10 counterpart).
        if (id.startsWith("ore_deepslate_") || id.startsWith("cluster_deepslate_")) {
            return null;
        }
        String sound = table().get(LegacyBlastResistance.originalId(id));
        if (sound == null && id.endsWith("_stairs")) {
            // 1.7.10 BlockStairs copies the parent's stepSound.
            sound = table().get(LegacyBlastResistance.originalId(id.substring(0, id.length() - "_stairs".length())));
        }
        return sound;
    }

    @Nullable
    public static SoundType expected(String id) {
        String sound = originalSound(id);
        return sound == null ? null : soundType(sound);
    }

    public static SoundType soundType(String sound) {
        return switch (sound) {
            case "stone" -> SoundType.STONE;
            case "wood" -> SoundType.WOOD;
            case "gravel" -> SoundType.GRAVEL;
            case "grass" -> SoundType.GRASS;
            case "metal" -> LEGACY_METAL;
            case "glass" -> SoundType.GLASS;
            case "cloth" -> SoundType.WOOL;
            case "sand" -> SoundType.SAND;
            case "snow" -> SoundType.SNOW;
            case "ladder" -> SoundType.LADDER;
            case "anvil" -> SoundType.ANVIL;
            case "grate" -> HbmSoundTypes.GRATE;
            case "pipe" -> HbmSoundTypes.PIPE;
            case "platemetal" -> HbmSoundTypes.PLATEMETAL;
            case "flesh" -> HbmSoundTypes.FLESH;
            default -> throw new IllegalStateException("unknown 1.7.10 sound type " + sound);
        };
    }

    public static String name(SoundType type) {
        if (type == SoundType.STONE) return "stone";
        if (type == SoundType.WOOD) return "wood";
        if (type == SoundType.GRAVEL) return "gravel";
        if (type == SoundType.GRASS) return "grass";
        if (type == LEGACY_METAL) return "metal";
        if (type == SoundType.METAL) return "metal_1.21";
        if (type == SoundType.GLASS) return "glass";
        if (type == SoundType.WOOL) return "cloth";
        if (type == SoundType.SAND) return "sand";
        if (type == SoundType.SNOW) return "snow";
        if (type == SoundType.LADDER) return "ladder";
        if (type == SoundType.ANVIL) return "anvil";
        if (type == HbmSoundTypes.GRATE) return "grate";
        if (type == HbmSoundTypes.PIPE) return "pipe";
        if (type == HbmSoundTypes.PLATEMETAL) return "platemetal";
        if (type == HbmSoundTypes.FLESH) return "flesh";
        return "other@" + Integer.toHexString(System.identityHashCode(type));
    }

    private static synchronized Map<String, String> table() {
        if (table == null) {
            Map<String, String> map = new HashMap<>();
            try (var input = LegacyStepSound.class.getResourceAsStream("/legacy/atomiclegacy/parity-v4-sounds.json")) {
                if (input == null) {
                    throw new IllegalStateException("missing parity-v4-sounds.json");
                }
                for (Map.Entry<String, JsonElement> entry : JsonParser.parseReader(
                        new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
            } catch (Exception e) {
                throw new IllegalStateException("cannot read original step sound table", e);
            }
            table = map;
        }
        return table;
    }
}
