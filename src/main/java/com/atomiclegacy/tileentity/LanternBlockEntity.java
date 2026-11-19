package com.atomiclegacy.tileentity;

import com.atomiclegacy.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** TileEntityLantern: blinds glyphids around the lamp head. */
public final class LanternBlockEntity extends BlockEntity {
    public LanternBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LANTERN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LanternBlockEntity lantern) {
        // [parity R4: lantern] TileEntityLantern#updateEntity: glyphids within 7.5 of (x+.5, y+5.5, z+.5) are blinded
        if (level.isClientSide || level.getGameTime() % 20L != 0L) {
            return;
        }
        blindGlyphids(level, pos);
    }

    public static void blindGlyphids(Level level, BlockPos pos) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 5.5D;
        double cz = pos.getZ() + 0.5D;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(cx, cy, cz, cx, cy, cz).inflate(7.5D);
        for (com.atomiclegacy.entity.GlyphidEntity glyphid : level.getEntitiesOfClass(com.atomiclegacy.entity.GlyphidEntity.class, box)) {
            glyphid.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
