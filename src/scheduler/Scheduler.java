package scheduler;

import java.util.List;
import model.Process;

public interface Scheduler {
    enum Activity { RUNNING, IDLE, CONTEXT_SWITCH }

    /** Describes [time, time + 1). The process ID is present only for RUNNING. */
    record TimeStepRecord(int time, Activity activity, String processId) {}

    /** Start a new simulation at time zero using fresh copies of the input workload. */
    void loadProcesses(List<Process> processes);

    /** Advance one tick. Return false when there is no tick left to execute. */
    boolean step();

    /** Run the loaded simulation from its current position through completion. */
    default void schedule() {
        while (step()) {
            // Each step records its own result.
        }
    }

    default void runSimulation() {
        schedule();
    }

    List<TimeStepRecord> getExecutionTrace();

    /** Return process snapshots in input order, including metrics as they become available. */
    List<Process> getProcesses();
}
