package model;

import java.util.List;

public record TimeStepRecord(
        int time,
        String runningProcessId,
        String event,
        List<Process> tier1Queue,
        List<Process> tier2Queue
) {
    public TimeStepRecord {
        tier1Queue = tier1Queue == null ? List.of() : List.copyOf(tier1Queue);
        tier2Queue = tier2Queue == null ? List.of() : List.copyOf(tier2Queue);
    }
}
