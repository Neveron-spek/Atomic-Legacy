package com.atomiclegacy.render;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/**
 * [parity U4: item renderers] Keeps the original item model (its icon, overrides and vanilla transforms) and turns the
 * contexts that the 1.7.10 IItemRenderer handled into a custom (BEWLR) render without transforms. Contexts that
 * handleRenderType rejected keep rendering the wrapped model exactly as before, as 1.7.10 fell back to RenderItem.
 */
public final class LegacyItemModelWrapper extends BakedModelWrapper<BakedModel> {

    private final LegacyIItemRenderer renderer;
    private final Map<BakedModel, LegacyItemModelWrapper> resolved = new IdentityHashMap<>();
    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        public @Nullable BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                            @Nullable LivingEntity entity, int seed) {
            BakedModel base = LegacyItemModelWrapper.this.originalModel;
            BakedModel result = base.getOverrides().resolve(base, stack, level, entity, seed);
            if (result == null) return null;
            if (!LegacyItemModelWrapper.this.renderer.handlesStack(stack)) return result;
            if (result == base) return LegacyItemModelWrapper.this;
            synchronized (LegacyItemModelWrapper.this.resolved) {
                return LegacyItemModelWrapper.this.resolved.computeIfAbsent(result,
                        r -> new LegacyItemModelWrapper(r, LegacyItemModelWrapper.this.renderer));
            }
        }
    };

    private LegacyItemModelWrapper(BakedModel original, LegacyIItemRenderer renderer) {
        super(original);
        this.renderer = renderer;
    }

    /** The wrapped vanilla model (its particle icon is the 1.7.10 item icon, IIcon of pass 0). */
    public BakedModel original() {
        return this.originalModel;
    }

    public static void wrap(Map<ModelResourceLocation, BakedModel> models, ResourceLocation itemId, LegacyIItemRenderer renderer) {
        ModelResourceLocation location = ModelResourceLocation.inventory(itemId);
        BakedModel model = models.get(location);
        if (model == null) {
            throw new IllegalStateException("No item model to wrap for the legacy item renderer: " + itemId);
        }
        if (!(model instanceof LegacyItemModelWrapper)) {
            models.put(location, new LegacyItemModelWrapper(model, renderer));
        }
    }

    private boolean handles(ItemDisplayContext context) {
        return this.renderer.handleRenderType(ItemStack.EMPTY, LegacyIItemRenderer.legacyType(context));
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
        if (handles(context)) {
            return this;
        }
        return this.originalModel.applyTransform(context, poseStack, applyLeftHandTransform);
    }

    /** ItemEntityRenderer lifts by 0.25 * GROUND scale.y; the BEWLR undoes the identity value. */
    @Override
    public ItemTransforms getTransforms() {
        return handles(ItemDisplayContext.GROUND) ? ItemTransforms.NO_TRANSFORMS : this.originalModel.getTransforms();
    }

    /** GUI 3D lighting (RenderHelper.enableGUIStandardItemLighting state) when the renderer draws the slot itself. */
    @Override
    public boolean usesBlockLight() {
        return handles(ItemDisplayContext.GUI) || this.originalModel.usesBlockLight();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }
}
