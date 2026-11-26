package scheduler;

import java.util.List;
import metrics.SchedulingMetrics;
import model.Process;

public interface Scheduler {
    enum Activity { RUNNING, IDLE, CONTEXT_SWITCH }

    
    record TimeStepRecord(int time, Activity activity, String processId) {}

    
    void loadProcesses(List<Process> processes);

    
    default boolean step() {
        return false;
    }

    
    default void schedule() {
        runSimulation();
    }

    default void runSimulation() {
        while (step()) {
        }
    }

    List<?> getExecutionTrace();

    
    List<Process> getProcesses();

    default SchedulingMetrics getMetrics() {
        return metrics.MetricsEngine.compute(getProcesses());
    }
}
