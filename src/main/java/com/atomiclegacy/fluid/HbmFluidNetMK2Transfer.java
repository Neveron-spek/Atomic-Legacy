package com.atomiclegacy.fluid;

import com.atomiclegacy.power.PowerEndpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * [parity T1: fluid net] Synchronous per-tick transfer of {@code api.hbm.fluidmk2.FluidNetMK2#update()}
 * (HbmMods 936c60c), reproduced with its quirks: pressures 0..5 are separate pools, receivers are
 * served from HIGHEST to LOWEST priority by truncated weights of their demand, the cumulative
 * {@code received[p]} is subtracted from the budget after every served tier, providers are debited
 * by truncated weights and the rounding leftover is taken from random providers ("scapegoat",
 * at most 100 iterations, {@code NodeNet.rand}).
 */
public final class HbmFluidNetMK2Transfer {
    /** NodeNet.rand */
    static final Random RAND = new Random();
    private static final int PRESSURES = HbmFluidNetUser.HIGHEST_VALID_PRESSURE + 1;
    private static final int PRIORITIES = PowerEndpoint.ConnectionPriority.VALUES.length;

    private HbmFluidNetMK2Transfer() {
    }

    /** IFluidProviderMK2 / IFluidReceiverMK2 as seen by one network of one fluid type. */
    public interface Endpoint {
        boolean isProvider();

        boolean isReceiver();

        int[] providingPressureRange();

        int[] receivingPressureRange();

        long fluidAvailable(int pressure);

        long providerSpeed(int pressure);

        void useUpFluid(int pressure, long amount);

        long demand(int pressure);

        long receiverSpeed(int pressure);

        PowerEndpoint.ConnectionPriority priority();

        /** returns the remainder */
        long transferFluid(int pressure, long amount);
    }

    /**
     * Runs one FluidNetMK2 update over the subscribed providers and receivers of a net. Returns the
     * fluidTracker value (fluid removed from providers during this update).
     */
    @SuppressWarnings("unchecked")
    public static long update(Collection<? extends Endpoint> providerEntries, Collection<? extends Endpoint> receiverEntries) {
        if (providerEntries.isEmpty()) return 0L;
        if (receiverEntries.isEmpty()) return 0L;

        long totalFluidInNetwork = 0;
        long[] fluidAvailable = new long[PRESSURES];
        List<Endpoint>[] providers = new List[PRESSURES];
        List<Long>[] providerValues = new List[PRESSURES];
        long[][] fluidDemand = new long[PRESSURES][PRIORITIES];
        List<Endpoint>[][] receivers = new List[PRESSURES][PRIORITIES];
        List<Long>[][] receiverValues = new List[PRESSURES][PRIORITIES];
        for (int i = 0; i < PRESSURES; i++) {
            providers[i] = new ArrayList<>();
            providerValues[i] = new ArrayList<>();
            for (int j = 0; j < PRIORITIES; j++) {
                receivers[i][j] = new ArrayList<>();
                receiverValues[i][j] = new ArrayList<>();
            }
        }

        // setupFluidProviders
        for (Endpoint provider : providerEntries) {
            int[] pressureRange = provider.providingPressureRange();
            for (int p = pressureRange[0]; p <= pressureRange[1]; p++) {
                long available = Math.min(provider.fluidAvailable(p), provider.providerSpeed(p));
                providers[p].add(provider);
                providerValues[p].add(available);
                fluidAvailable[p] += available;
                totalFluidInNetwork += provider.fluidAvailable(p);
            }
        }

        // setupFluidReceivers
        for (Endpoint receiver : receiverEntries) {
            int[] pressureRange = receiver.receivingPressureRange();
            for (int p = pressureRange[0]; p <= pressureRange[1]; p++) {
                long required = Math.min(receiver.demand(p), receiver.receiverSpeed(p));
                int priority = receiver.priority().ordinal();
                receivers[p][priority].add(receiver);
                receiverValues[p][priority].add(required);
                fluidDemand[p][priority] += required;
            }
        }

        // transferFluid
        long[] received = new long[PRESSURES];
        long[] notAccountedFor = new long[PRESSURES];

        for (int p = 0; p < PRESSURES; p++) {
            long totalAvailable = fluidAvailable[p];
            for (int i = PRIORITIES - 1; i >= 0; i--) {
                long toTransfer = Math.min(fluidDemand[p][i], totalAvailable);
                if (toTransfer <= 0) continue;
                long priorityDemand = fluidDemand[p][i];
                for (int j = 0; j < receivers[p][i].size(); j++) {
                    double weight = (double) receiverValues[p][i].get(j) / (double) (priorityDemand);
                    long toSend = (long) Math.max(toTransfer * weight, 0D);
                    toSend -= receivers[p][i].get(j).transferFluid(p, toSend);
                    received[p] += toSend;
                }
                totalAvailable -= received[p];
            }
            notAccountedFor[p] = received[p];
        }

        for (int p = 0; p < PRESSURES; p++) {
            for (int j = 0; j < providers[p].size(); j++) {
                double weight = (double) providerValues[p].get(j) / (double) fluidAvailable[p];
                long toUse = (long) Math.max(received[p] * weight, 0D);
                providers[p].get(j).useUpFluid(p, toUse);
                notAccountedFor[p] -= toUse;
            }
        }

        for (int p = 0; p < PRESSURES; p++) {
            int iterationsLeft = 100;
            while (iterationsLeft > 0 && notAccountedFor[p] > 0 && !providers[p].isEmpty()) {
                iterationsLeft--;
                Endpoint scapegoat = providers[p].get(RAND.nextInt(providers[p].size()));
                long toUse = Math.min(notAccountedFor[p], scapegoat.fluidAvailable(p));
                scapegoat.useUpFluid(p, toUse);
                notAccountedFor[p] -= toUse;
            }
        }

        long totalAfterTransfer = 0;
        for (Endpoint provider : providerEntries) {
            for (int p = 0; p < PRESSURES; p++) {
                totalAfterTransfer += provider.fluidAvailable(p);
            }
        }
        return totalFluidInNetwork - totalAfterTransfer;
    }
}
