package com.atomiclegacy.power;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * [parity U1: network_separator] A multiblock core whose power ports are separate TilePort nodes
 * (ModulePortManPower#onlyInput / #onlyOutput of 936c60c): every port is its own {@link PowerEndpoint}
 * located at a dummy position of the machine. The power graph never runs through the machine itself, so the
 * networks on both sides of the ports stay separate nets.
 */
public interface PowerSplitEndpoints {
    /** The endpoint whose {@link PowerEndpoint#getPowerPos()} is {@code pos}, or null. */
    @Nullable
    PowerEndpoint splitPowerEndpointAt(BlockPos pos);
}
