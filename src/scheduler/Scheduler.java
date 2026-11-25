package scheduler;

import java.util.List;
import metrics.SchedulingMetrics;
import model.Process;

public interface Scheduler {
    enum Activity { RUNNING, IDLE, CONTEXT_SWITCH }

    /** Describes [time, time + 1). The process ID is present only for RUNNING. */
    record TimeStepRecord(int time, Activity activity, String processId) {}

    /** Start a new simulation at time zero using fresh copies of the input workload. */
    void loadProcesses(List<Process> processes);

    /** Advance one tick. Return false when there is no tick left to execute. */
    default boolean step() {
        return false;
    }

    /** Run the loaded simulation from its current position through completion. */
    default void schedule() {
        runSimulation();
    }

    default void runSimulation() {
        while (step()) {
            // Step loop
        }
    }

    List<?> getExecutionTrace();

    /** Return process snapshots in input or completion order, including metrics. */
    List<Process> getProcesses();

    default SchedulingMetrics getMetrics() {
        return metrics.MetricsEngine.compute(getProcesses());
    }
}
