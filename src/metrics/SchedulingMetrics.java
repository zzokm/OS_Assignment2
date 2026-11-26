package metrics;

import java.util.List;
import model.Process;

public final class SchedulingMetrics {
    private final List<Process> processes;
    private final double averageTurnaroundTime;
    private final double averageWaitingTime;
    private final double averageResponseTime;

    public SchedulingMetrics(
            List<Process> processes,
            double averageTurnaroundTime,
            double averageWaitingTime,
            double averageResponseTime) {
        this.processes = List.copyOf(processes);
        this.averageTurnaroundTime = averageTurnaroundTime;
        this.averageWaitingTime = averageWaitingTime;
        this.averageResponseTime = averageResponseTime;
    }

    public List<Process> getProcesses() {
        return processes;
    }

    public double getAverageTurnaroundTime() {
        return averageTurnaroundTime;
    }

    public double getAverageWaitingTime() {
        return averageWaitingTime;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }
}
