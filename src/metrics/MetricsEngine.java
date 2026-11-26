package metrics;

import java.util.Comparator;
import java.util.List;
import model.Process;

public final class MetricsEngine {
    private MetricsEngine() {}

    public static SchedulingMetrics compute(List<Process> processes) {
        if (processes == null || processes.isEmpty()) {
            throw new IllegalArgumentException("Process list must not be null or empty");
        }

        List<Process> sorted = processes.stream()
                .map(Process::new)
                .sorted(Comparator.comparing(Process::getId))
                .toList();

        double totalTurnaround = 0.0;
        double totalWaiting = 0.0;
        double totalResponse = 0.0;
        int responseCount = 0;

        for (Process p : sorted) {
            int tat = p.getTurnaroundTime();
            int wt = p.getWaitingTime();
            int rt = p.getResponseTime();

            if (tat < 0 || wt < 0) {
                throw new IllegalStateException(
                        "Cannot compute metrics before process finishes: " + p.getId());
            }

            totalTurnaround += tat;
            totalWaiting += wt;
            if (rt >= 0) {
                totalResponse += rt;
                responseCount++;
            }
        }

        int n = sorted.size();
        double avgTat = totalTurnaround / n;
        double avgWt = totalWaiting / n;
        double avgRt = responseCount > 0 ? totalResponse / responseCount : 0.0;

        return new SchedulingMetrics(sorted, avgTat, avgWt, avgRt);
    }
}
