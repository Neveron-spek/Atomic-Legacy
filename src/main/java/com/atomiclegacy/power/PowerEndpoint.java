package com.atomiclegacy.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.List;

public interface PowerEndpoint {
    BlockPos getPowerPos();

    default List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(Direction.values().length);
        BlockPos pos = getPowerPos();
        for (Direction direction : Direction.values()) {
            connectors.add(pos.relative(direction).immutable());
        }
        return List.copyOf(connectors);
    }

    default boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    long getAvailableOutput();

    long getRequestedInput();

    default ConnectionPriority getPowerPriority() {
        return ConnectionPriority.NORMAL;
    }

    void applyPower(long usedOutput, long receivedInput);

    /**
     * [parity Q3: charger] Leftover that IEnergyReceiverMK2.transferPower would return for {@code offered}
     * (PowerNetMK2: "leftovers are subtracted from the intended amount to use up"). Must not mutate state;
     * {@link #applyPower} still receives the full offered amount.
     */
    default long receiveLeftover(long offered) {
        return 0L;
    }

    Component getPowerStatus();

    enum ConnectionPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST;

        public static final ConnectionPriority[] VALUES = new ConnectionPriority[]{LOWEST, LOW, NORMAL, HIGH, HIGHEST};
    }
}
