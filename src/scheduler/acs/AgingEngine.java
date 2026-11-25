package scheduler.acs;

import java.util.Objects;

import model.Process;

public class AgingEngine {
    public static final int AGING_INTERVAL = 4;

    public void applyAging(Process process, int waitingTimeInTier2) {
        Objects.requireNonNull(process, "process");
        if (waitingTimeInTier2 < 0) {
            throw new IllegalArgumentException("Tier 2 waiting time must not be negative");
        }

        long agedPriority = (long) process.getInitialPriority()
                - waitingTimeInTier2 / AGING_INTERVAL;
        process.setEffectivePriority((int) Math.max(Integer.MIN_VALUE, agedPriority));
    }
}
