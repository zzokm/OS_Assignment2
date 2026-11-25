package scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import model.Process;

/**
 * Preemptive shortest remaining time first. Ready-queue ties use arrival time,
 * then input order. A running process keeps the CPU on a tie.
 */
public final class SJFScheduler implements Scheduler {
    private final int contextSwitchTime;
    private final List<TimeStepRecord> trace = new ArrayList<>();
    private final PriorityQueue<Integer> ready = new PriorityQueue<>(Comparator
            .comparingInt(this::remainingBurstTime)
            .thenComparingInt(this::arrivalTime)
            .thenComparingInt(Integer::intValue));
    private List<Process> processes = List.of();
    private List<Integer> arrivalOrder = List.of();
    private int nextArrival;
    private int time;
    private int completed;
    private int switchTicksLeft;
    private int runningIndex;
    private Process running;
    private Process lastRunning;

    /**
     * Charges a switch between consecutive runs of different processes.
     * Initial dispatch and dispatch after an idle tick have no switch cost.
     */
    public SJFScheduler(int contextSwitchTime) {
        if (contextSwitchTime < 0) {
            throw new IllegalArgumentException("Context switch time must be nonnegative");
        }
        this.contextSwitchTime = contextSwitchTime;
    }

    @Override
    public void loadProcesses(List<Process> workload) {
        List<Process> freshProcesses = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Process process : workload) {
            if (!ids.add(process.getId())) {
                throw new IllegalArgumentException("Duplicate process ID: " + process.getId());
            }
            order.add(freshProcesses.size());
            freshProcesses.add(new Process(process.getId(), process.getArrivalTime(),
                    process.getBurstTime(), process.getInitialPriority()));
        }
        order.sort(Comparator.comparingInt(index -> freshProcesses.get(index).getArrivalTime()));
        ready.clear();
        trace.clear();
        processes = freshProcesses;
        arrivalOrder = order;
        nextArrival = 0;
        time = 0;
        completed = 0;
        switchTicksLeft = 0;
        running = null;
        lastRunning = null;
    }

    @Override
    public boolean step() {
        if (completed == processes.size()) {
            return false;
        }
        admitArrivals();
        if (switchTicksLeft > 0) {
            recordSwitchTick();
            return true;
        }

        if (running != null && !ready.isEmpty()
                && remainingBurstTime(ready.peek()) < running.getRemainingBurstTime()) {
            running.markReady(time);
            ready.add(runningIndex);
            running = null;
        }
        if (running == null) {
            if (ready.isEmpty()) {
                lastRunning = null;
                trace.add(new TimeStepRecord(time++, Activity.IDLE, null));
                return true;
            }
            if (contextSwitchTime > 0 && lastRunning != null
                    && lastRunning != processes.get(ready.peek())) {
                // Leave candidates in the queue so arrivals during the switch can compete.
                lastRunning = null;
                switchTicksLeft = contextSwitchTime;
                recordSwitchTick();
                return true;
            }
            runningIndex = ready.remove();
            running = processes.get(runningIndex);
        }

        running.runOneTick(time);
        trace.add(new TimeStepRecord(time++, Activity.RUNNING, running.getId()));
        lastRunning = running;
        if (running.getState() == Process.State.FINISHED) {
            completed++;
            running = null;
        }
        return true;
    }

    private void admitArrivals() {
        while (nextArrival < arrivalOrder.size()) {
            int index = arrivalOrder.get(nextArrival);
            if (arrivalTime(index) > time) {
                break;
            }
            processes.get(index).markReady(time);
            ready.add(index);
            nextArrival++;
        }
    }

    private void recordSwitchTick() {
        trace.add(new TimeStepRecord(time++, Activity.CONTEXT_SWITCH, null));
        switchTicksLeft--;
    }

    private int remainingBurstTime(int index) {
        return processes.get(index).getRemainingBurstTime();
    }

    private int arrivalTime(int index) {
        return processes.get(index).getArrivalTime();
    }

    @Override
    public List<TimeStepRecord> getExecutionTrace() {
        return List.copyOf(trace);
    }

    @Override
    public List<Process> getProcesses() {
        return processes.stream().map(Process::new).toList();
    }
}
