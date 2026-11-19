package com.atomiclegacy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.atomiclegacy.AtomicLegacy;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HbmKeyMappings {
    public static final KeyMapping TOGGLE_MAGNET = new KeyMapping(
            "key.atomiclegacy.toggle_magnet",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.atomiclegacy"
    );
    /** 1.7.10 bismuth armour dash trigger. */
    public static final KeyMapping ARMOR_DASH = new KeyMapping(
            "key.atomiclegacy.armor_dash",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT, // HbmKeybinds.dashKey = KEY_LSHIFT (V is toggleHUD)
            "key.categories.atomiclegacy"
    );
    /** Opens the Quick Manual and Wiki page for the item currently under the cursor. */
    public static final KeyMapping QMAW = new KeyMapping(
            "key.atomiclegacy.qmaw",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F1,
            "key.categories.atomiclegacy"
    );
    // [parity S3: calculator] HbmKeybinds.calculatorKey = KEY_N
    public static final KeyMapping CALCULATOR = new KeyMapping(
            "key.atomiclegacy.calculator",
            GLFW.GLFW_KEY_N,
            "key.categories.atomiclegacy"
    );
    // [parity S1: trainInv] HbmKeybinds.trainKey = hbm.key.trainInv, KEY_R
    public static final KeyMapping TRAIN_INV = new KeyMapping(
            "key.atomiclegacy.train_inv",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.atomiclegacy"
    );
    /** Exact 1.7.10 RBMK crane controls (arrow keys plus Enter). */
    public static final KeyMapping CRANE_UP = craneKey("crane_move_up", GLFW.GLFW_KEY_UP);
    public static final KeyMapping CRANE_DOWN = craneKey("crane_move_down", GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping CRANE_LEFT = craneKey("crane_move_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping CRANE_RIGHT = craneKey("crane_move_right", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping CRANE_LOAD = craneKey("crane_load", GLFW.GLFW_KEY_ENTER);

    /** 1.7.10 HbmKeybinds gun keys: reload R, primary LMB (-100), secondary RMB (-99), sights MMB (-98). */
    public static final KeyMapping RELOAD = new KeyMapping(
            "key.atomiclegacy.reload",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.atomiclegacy"
    );
    public static final KeyMapping GUN_PRIMARY = new KeyMapping(
            "key.atomiclegacy.gun_primary",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.categories.atomiclegacy"
    );
    public static final KeyMapping GUN_SECONDARY = new KeyMapping(
            "key.atomiclegacy.gun_secondary",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            "key.categories.atomiclegacy"
    );
    public static final KeyMapping GUN_TERTIARY = new KeyMapping(
            "key.atomiclegacy.gun_tertiary",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "key.categories.atomiclegacy"
    );

    private static KeyMapping craneKey(String name, int keyCode) {
        return new KeyMapping(
                "key.atomiclegacy." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                keyCode,
                "key.categories.atomiclegacy"
        );
    }

    private HbmKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MAGNET);
        event.register(ARMOR_DASH);
        event.register(QMAW);
        event.register(CALCULATOR); // [parity S3: calculator]
        event.register(TRAIN_INV); // [parity S1: trainInv]
        event.register(CRANE_UP);
        event.register(CRANE_DOWN);
        event.register(CRANE_LEFT);
        event.register(CRANE_RIGHT);
        event.register(CRANE_LOAD);
        event.register(RELOAD);
        event.register(GUN_PRIMARY);
        event.register(GUN_SECONDARY);
        event.register(GUN_TERTIARY);
    }
}
