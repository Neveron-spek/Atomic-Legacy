package com.atomiclegacy.render;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * 1.7.10 IItemRenderer knew only ENTITY, EQUIPPED, EQUIPPED_FIRST_PERSON and INVENTORY. Item frames drew their
 * content through RenderItem as ENTITY and head-worn stacks went through the EQUIPPED path, so the modern FIXED
 * and HEAD contexts reuse those transforms instead of failing (an item frame holding such a block crashed the client).
 */
public final class LegacyItemContexts {

    private LegacyItemContexts() {}

    public static ItemDisplayContext remap(ItemDisplayContext context) {
        return switch (context) {
            case FIXED -> ItemDisplayContext.GROUND;
            case HEAD -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            default -> context;
        };
    }
}
