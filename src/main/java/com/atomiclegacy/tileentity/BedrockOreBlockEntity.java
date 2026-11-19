package com.atomiclegacy.tileentity;

import com.atomiclegacy.fluid.HbmFluidStack;
import com.atomiclegacy.items.BedrockOreItem;
import com.atomiclegacy.registry.HbmBlockEntities;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BedrockOreBlockEntity extends BlockEntity {
    private ItemStack resource = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, BedrockOreItem.Grade.BASE, BedrockOreItem.Type.LIGHT_METAL);
    private HbmFluidStack acidRequirement = HbmFluidStack.EMPTY;
    private int tier = 1;
    private int color; // [parity d3: TileEntityBedrockOre.color defaults to 0 = derived from the resource on the client]
    private int shape;

    public BedrockOreBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BEDROCK_ORE.get(), pos, blockState);
    }

    public ItemStack resource() {
        return this.resource;
    }

    public HbmFluidStack acidRequirement() {
        return this.acidRequirement;
    }

    public int tier() {
        return this.tier;
    }

    public int color() {
        return this.color;
    }

    public int shape() {
        return this.shape;
    }

    public void configure(ItemStack resource, HbmFluidStack acidRequirement, int color, int tier, int shape) {
        this.resource = resource.isEmpty() ? this.resource : resource.copy();
        this.acidRequirement = acidRequirement == null ? HbmFluidStack.EMPTY : acidRequirement;
        this.color = color == 0 ? this.color : color;
        this.tier = Math.max(1, tier);
        this.shape = Math.floorMod(shape, 10);
        setChanged();
        // [parity d3: world.markBlockForUpdate -> getDescriptionPacket]
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel && serverLevel.getServer().isSameThread()) {
            serverLevel.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public void setResource(ItemStack resource) {
        configure(resource, this.acidRequirement, this.color, this.tier, this.shape + 1);
    }

    public void setAcidRequirement(HbmFluidStack acidRequirement) {
        configure(this.resource, acidRequirement, this.color, this.tier, this.shape);
    }

    public void setTier(int tier) {
        configure(this.resource, this.acidRequirement, this.color, tier, this.shape);
    }

    public Component statusLine() {
        Component fluid = this.acidRequirement.isEmpty()
                ? Component.translatable("message.atomiclegacy.bedrock_ore.no_fluid")
                : Component.translatable(
                        "message.atomiclegacy.bedrock_ore.fluid_requirement",
                        this.acidRequirement.amount(),
                        Component.translatable(this.acidRequirement.type().translationKey())
                );
        return Component.translatable("message.atomiclegacy.bedrock_ore.status", this.resource.getHoverName(), this.tier, fluid);
    }

    /** [parity d3] BlockBedrockOreTE#getIcon(world) pass 1: overlays[shape % 10]; read by client/model/BedrockOreBlockModel. */
    public static final net.neoforged.neoforge.client.model.data.ModelProperty<Integer> SHAPE = new net.neoforged.neoforge.client.model.data.ModelProperty<>();

    @Override
    public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
        return net.neoforged.neoforge.client.model.data.ModelData.builder().with(SHAPE, this.shape).build();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Resource", this.resource.saveOptional(registries));
        tag.put("Acid", this.acidRequirement.save());
        tag.putInt("Tier", this.tier);
        tag.putInt("Color", this.color);
        tag.putInt("Shape", this.shape);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.resource = ItemStack.parseOptional(registries, tag.getCompound("Resource"));
        if (this.resource.isEmpty()) {
            this.resource = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, BedrockOreItem.Grade.BASE, BedrockOreItem.Type.LIGHT_METAL);
        }
        this.acidRequirement = HbmFluidStack.load(tag.getCompound("Acid"));
        this.tier = Math.max(1, tag.getInt("Tier"));
        this.color = tag.getInt("Color");
        this.shape = Math.floorMod(tag.getInt("Shape"), 10);
        if (this.level != null && this.level.isClientSide) {
            requestModelDataUpdate();
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 8);
        }
    }
}
