package com.atomiclegacy.client.model;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.tileentity.BedrockOreBlockEntity;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * [parity d3] BlockBedrockOreTE + RenderBlockMultipass: pass 0 is the bedrock cube, pass 1 the
 * ore_random_(shape % 10 + 1) overlay multiplied by the tile entity colour. The item (shouldRenderItemMulti
 * = false) keeps pass 0 only.
 */
public final class BedrockOreBlockModel extends BakedModelWrapper<BakedModel> {
    private static final ModelResourceLocation[] SHAPES = new ModelResourceLocation[10];
    private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final Map<Item, Integer> STACK_COLORS = new IdentityHashMap<>();

    static {
        for (int i = 0; i < SHAPES.length; i++) {
            SHAPES[i] = ModelResourceLocation.standalone(AtomicLegacy.id("block/ore_bedrock_shape_" + i));
        }
    }

    private final BakedModel[] shapes;

    private BedrockOreBlockModel(BakedModel base, BakedModel[] shapes) {
        super(base);
        this.shapes = shapes;
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation shape : SHAPES) {
            event.register(shape);
        }
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        BakedModel[] shapes = new BakedModel[SHAPES.length];
        for (int i = 0; i < SHAPES.length; i++) {
            shapes[i] = models.get(SHAPES[i]);
            if (shapes[i] == null) {
                return;
            }
        }
        for (String id : new String[] {"ore_bedrock", "ore_bedrock_block"}) {
            ModelResourceLocation location = new ModelResourceLocation(AtomicLegacy.id(id), "");
            BakedModel base = models.get(location);
            if (base != null && !(base instanceof BedrockOreBlockModel)) {
                models.put(location, new BedrockOreBlockModel(base, shapes));
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        Integer shape = data.get(BedrockOreBlockEntity.SHAPE);
        if (state == null || shape == null) {
            return this.originalModel.getQuads(state, side, random, data, renderType);
        }
        return this.shapes[Math.floorMod(shape, this.shapes.length)].getQuads(state, side, random, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return data.has(BedrockOreBlockEntity.SHAPE) ? CUTOUT : this.originalModel.getRenderTypes(state, random, data);
    }

    /** BlockBedrockOreTE#colorMultiplier for pass 1. */
    public static int color(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        if (tintIndex != 1 || level == null || pos == null
                || !(level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore)) {
            return 0xFFFFFFFF;
        }
        int color = ore.color();
        if (color == 0) {
            color = stackColor(ore.resource());
        }
        return 0xFF000000 | color;
    }

    /** ClientProxy#getStackColor(stack, true): map colour for blocks, amplified icon average otherwise. */
    private static synchronized int stackColor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0x000000;
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultMapColor().col;
        }
        Integer cached = STACK_COLORS.get(stack.getItem());
        if (cached != null) {
            return cached;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0).getParticleIcon();
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        long r = 0L;
        long g = 0L;
        long b = 0L;
        // ColorUtil#getAverageColorFromStack: new Color(int) drops alpha, so every pixel is counted
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int abgr = sprite.getPixelRGBA(0, x, y);
                r += abgr & 0xFF;
                g += abgr >> 8 & 0xFF;
                b += abgr >> 16 & 0xFF;
            }
        }
        int pixels = Math.max(1, width * height);
        int avgR = (int) (r / pixels);
        int avgG = (int) (g / pixels);
        int avgB = (int) (b / pixels);
        // ColorUtil#amplifyColor(hex, 255)
        int max = Math.max(Math.max(1, avgR), Math.max(avgG, avgB));
        int color = avgR * 255 / max << 16 | avgG * 255 / max << 8 | avgB * 255 / max;
        STACK_COLORS.put(stack.getItem(), color);
        return color;
    }
}
