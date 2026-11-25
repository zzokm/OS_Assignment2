package output;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import metrics.SchedulingMetrics;
import model.Process;
import model.TimeStepRecord;
import scheduler.Scheduler;

/**
 * Formats execution traces and scheduling metric tables for console and GUI display.
 */
public final class TraceTableFormatter {
    private TraceTableFormatter() {}

    public static String formatMetricsTable(SchedulingMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s | %-12s | %-11s | %-15s | %-12s | %-13s%n",
                "Process", "Arrival Time", "Finish Time", "Turnaround Time", "Waiting Time", "Response Time"));
        sb.append("-".repeat(84)).append(System.lineSeparator());

        for (Process p : metrics.getProcesses()) {
            sb.append(String.format("%-8s | %-12d | %-11d | %-15d | %-12d | %-13d%n",
                    p.getId(),
                    p.getArrivalTime(),
                    p.getFinishTime(),
                    p.getTurnaroundTime(),
                    p.getWaitingTime(),
                    p.getResponseTime()));
        }

        sb.append("-".repeat(84)).append(System.lineSeparator());
        sb.append(String.format(Locale.US, "Average Turnaround Time: %.2f time units%n",
                metrics.getAverageTurnaroundTime()));
        sb.append(String.format(Locale.US, "Average Waiting Time:    %.2f time units%n",
                metrics.getAverageWaitingTime()));
        sb.append(String.format(Locale.US, "Average Response Time:   %.2f time units%n",
                metrics.getAverageResponseTime()));
        return sb.toString();
    }

    public static String formatAcsTrace(List<TimeStepRecord> trace) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-5s | %-8s | %-34s | %-34s | %s%n",
                "Time", "CPU", "Tier 1 Queue", "Tier 2 Queue", "Events"));
        sb.append("-".repeat(110)).append(System.lineSeparator());

        for (TimeStepRecord step : trace) {
            sb.append(String.format("%-5d | %-8s | %-34s | %-34s | %s%n",
                    step.time(),
                    step.runningProcessId(),
                    formatQueue(step.tier1Queue()),
                    formatQueue(step.tier2Queue()),
                    step.event()));
        }
        return sb.toString();
    }

    public static String formatSjfTrace(List<Scheduler.TimeStepRecord> trace) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s | %-15s | %-10s%n", "Time", "Activity", "Process"));
        sb.append("-".repeat(38)).append(System.lineSeparator());

        for (Scheduler.TimeStepRecord step : trace) {
            String proc = step.processId() == null ? "-" : step.processId();
            sb.append(String.format("%-6d | %-15s | %-10s%n",
                    step.time(), step.activity(), proc));
        }
        return sb.toString();
    }

    private static String formatQueue(List<Process> queue) {
        if (queue == null || queue.isEmpty()) {
            return "[Empty]";
        }
        return queue.stream()
                .map(p -> p.getId() + "(rem:" + p.getRemainingBurstTime()
                        + ", pri:" + p.getEffectivePriority() + ")")
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
