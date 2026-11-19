package com.atomiclegacy.power;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * [parity L1: network] Synchronous per-tick transfer of 1.7.10
 * {@code api.hbm.energymk2.PowerNetMK2#update()} (commit 936c60c).
 *
 * <p>Reproduced exactly, including the original quirks: every priority tier is
 * offered the remaining budget and then {@code toTransfer -= energyUsed} with
 * the cumulative {@code energyUsed} (also for empty tiers), per-receiver
 * amounts are truncated doubles, providers are debited by truncated weights
 * and the rounding leftover is taken from random providers ("scapegoat", at
 * most 100 iterations). Endpoints expose {@code min(power, speed)} as
 * available output and {@code min(max - power, speed)} as requested input,
 * which are the original {@code src}/{@code rec} values.</p>
 */
public final class PowerNetMK2Transfer {
    /** NodeNet.rand */
    private static final Random RAND = new Random();

    private PowerNetMK2Transfer() {
    }

    /** Returns the energy taken from providers (PowerNetMK2.energyTracker contribution for pure providers). */
    public static long transfer(List<PowerEndpoint> endpoints) {
        List<Integer> providers = new ArrayList<>();
        List<Long> providerSrc = new ArrayList<>();
        long powerAvailable = 0L;
        for (int e = 0; e < endpoints.size(); e++) {
            long src = endpoints.get(e).getAvailableOutput();
            if (src > 0L) {
                providers.add(e);
                providerSrc.add(src);
                powerAvailable += src;
            }
        }
        if (providers.isEmpty()) {
            return 0L;
        }

        int tiers = PowerEndpoint.ConnectionPriority.VALUES.length;
        List<List<Integer>> receivers = new ArrayList<>(tiers);
        List<List<Long>> receiverRec = new ArrayList<>(tiers);
        for (int i = 0; i < tiers; i++) {
            receivers.add(new ArrayList<>());
            receiverRec.add(new ArrayList<>());
        }
        long[] demand = new long[tiers];
        long totalDemand = 0L;
        for (int e = 0; e < endpoints.size(); e++) {
            PowerEndpoint entry = endpoints.get(e);
            long rec = entry.getRequestedInput();
            if (rec > 0L) {
                int p = entry.getPowerPriority().ordinal();
                receivers.get(p).add(e);
                receiverRec.get(p).add(rec);
                demand[p] += rec;
                totalDemand += rec;
            }
        }
        if (totalDemand <= 0L) {
            return 0L;
        }

        long toTransfer = Math.min(powerAvailable, totalDemand);
        long energyUsed = 0L;
        long[] received = new long[endpoints.size()];
        for (int i = tiers - 1; i >= 0; i--) {
            List<Integer> list = receivers.get(i);
            List<Long> recs = receiverRec.get(i);
            long priorityDemand = demand[i];
            for (int j = 0; j < list.size(); j++) {
                long rec = recs.get(j);
                double weight = (double) rec / (double) priorityDemand;
                long toSend = (long) Math.min(Math.max(toTransfer * weight, 0D), rec);
                received[list.get(j)] += toSend;
                // [parity Q3: charger] PowerNetMK2: energyUsed += (toSend - transferPower(toSend))
                energyUsed += toSend - (toSend > 0L ? endpoints.get(list.get(j)).receiveLeftover(toSend) : 0L);
            }
            toTransfer -= energyUsed;
        }

        long leftover = energyUsed;
        long[] used = new long[endpoints.size()];
        for (int j = 0; j < providers.size(); j++) {
            double weight = (double) providerSrc.get(j) / (double) powerAvailable;
            long toUse = (long) Math.max(energyUsed * weight, 0D);
            used[providers.get(j)] += toUse;
            leftover -= toUse;
        }

        int iterationsLeft = 100;
        while (iterationsLeft > 0 && leftover > 0L && !providers.isEmpty()) {
            iterationsLeft--;
            int selected = RAND.nextInt(providers.size());
            int index = providers.get(selected);
            // The original removes up to getPower(); the port endpoint only
            // exposes min(power, providerSpeed), which bounds the extra debit.
            long toUse = Math.min(leftover, Math.max(0L, providerSrc.get(selected) - used[index]));
            used[index] += toUse;
            leftover -= toUse;
        }

        for (int i = 0; i < endpoints.size(); i++) {
            if (used[i] > 0L || received[i] > 0L) {
                endpoints.get(i).applyPower(used[i], received[i]);
            }
        }
        return energyUsed - leftover;
    }
}
