package scheduler.acs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import metrics.MetricsEngine;
import metrics.SchedulingMetrics;
import model.Process;
import model.TimeStepRecord;
import scheduler.Scheduler;

public class ACSScheduler implements Scheduler {
    private final int quantum;
    private final int contextSwitch;

    private List<Process> originals = new ArrayList<>();

    private final List<TimeStepRecord> trace = new ArrayList<>();
    private final List<Process> finished = new ArrayList<>();
    private boolean hasRun = false;

    private Tier1Queue tier1;
    private Tier2PriorityQueue tier2;

    private List<Process> arrivals;
    private int nextArrival;

    private int clock;

    private Process running;
    private int runningTier;

    private Process pendingLoad;
    private int pendingTier;
    private int csRemaining;

    private Process lastRun;

    private Set<Process> started;

    private final StringBuilder events = new StringBuilder();

    public ACSScheduler(int quantum, int contextSwitch) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Quantum must be positive: " + quantum);
        }
        if (contextSwitch < 0) {
            throw new IllegalArgumentException("Context switch must be >= 0: " + contextSwitch);
        }
        this.quantum = quantum;
        this.contextSwitch = contextSwitch;
    }

    @Override
    public void loadProcesses(List<Process> processes) {
        if (processes == null || processes.isEmpty()) {
            throw new IllegalArgumentException("Process list must not be empty");
        }
        originals = new ArrayList<>();
        for (Process p : processes) {
            originals.add(new Process(p));
        }
        trace.clear();
        finished.clear();
        hasRun = false;
    }

    @Override
    public void runSimulation() {
        if (originals.isEmpty()) {
            throw new IllegalStateException("No processes loaded");
        }
        resetState();

        while (finished.size() < arrivals.size()) {
            admitArrivals();

            if (running == null && pendingLoad == null) {
                dispatch();
            }

            trace.add(snapshotRecord());
            advanceOneUnit();
        }

        log("All processes completed");
        trace.add(snapshotRecord());
        hasRun = true;
    }

    @Override
    public List<TimeStepRecord> getExecutionTrace() {
        return Collections.unmodifiableList(trace);
    }

    @Override
    public SchedulingMetrics getMetrics() {
        if (!hasRun) {
            throw new IllegalStateException("Call runSimulation() first");
        }
        return MetricsEngine.compute(finished);
    }

    public List<Process> getCompletedProcesses() {
        return Collections.unmodifiableList(finished);
    }

    public int getQuantum() {
        return quantum;
    }

    public int getContextSwitch() {
        return contextSwitch;
    }

    private void resetState() {
        tier1 = new Tier1Queue(quantum);
        tier2 = new Tier2PriorityQueue();

        arrivals = new ArrayList<>();
        for (Process o : originals) {
            Process w = new Process(o);
            w.setRemainingBurstTime(w.getBurstTime());
            w.setEffectivePriority(w.getInitialPriority());
            arrivals.add(w);
        }
        arrivals.sort(Comparator.comparingInt(Process::getArrivalTime));

        nextArrival = 0;
        clock = 0;
        running = null;
        runningTier = 0;
        pendingLoad = null;
        pendingTier = 0;
        csRemaining = 0;
        lastRun = null;
        started = Collections.newSetFromMap(new IdentityHashMap<Process, Boolean>());

        trace.clear();
        finished.clear();
        events.setLength(0);
    }

    private void admitArrivals() {
        while (nextArrival < arrivals.size()
                && arrivals.get(nextArrival).getArrivalTime() <= clock) {
            Process p = arrivals.get(nextArrival++);
            tier1.enqueue(p);
            log(p.getId() + " arrived in Tier 1");
        }
    }

    private void dispatch() {
        Process next;
        int tier;

        if (!tier1.isEmpty()) {
            next = tier1.dequeue();
            tier = 1;
        } else if (!tier2.isEmpty()) {
            next = tier2.dequeueHighestPriority(clock);
            tier = 2;
            log(next.getId() + " selected from Tier 2 (effective priority "
                    + next.getEffectivePriority() + ")");
        } else {
            return;
        }

        Process previous = lastRun;
        lastRun = next;

        boolean needsSwitch = contextSwitch > 0 && previous != null && previous != next;
        if (needsSwitch) {
            pendingLoad = next;
            pendingTier = tier;
            csRemaining = contextSwitch;
            log("Context switch saves " + previous.getId() + " and loads " + next.getId());
        } else {
            startRunning(next, tier);
        }
    }

    private void startRunning(Process p, int tier) {
        running = p;
        runningTier = tier;
        if (tier == 1) {
            tier1.beginSlice(p);
        }
    }

    private void advanceOneUnit() {
        if (pendingLoad != null) {
            clock++;
            csRemaining--;
            if (csRemaining == 0) {
                startRunning(pendingLoad, pendingTier);
                pendingLoad = null;
            }
            return;
        }

        if (running == null) {
            clock++;
            lastRun = null;
            return;
        }

        Process p = running;

        if (started.add(p)) {
            p.setResponseTime(clock - p.getArrivalTime());
        }
        p.setRemainingBurstTime(p.getRemainingBurstTime() - 1);
        clock++;

        Tier1Queue.SliceStatus status;
        if (runningTier == 1) {
            status = tier1.recordTick(p);
        } else {
            status = p.getRemainingBurstTime() <= 0
                    ? Tier1Queue.SliceStatus.COMPLETED
                    : Tier1Queue.SliceStatus.CONTINUE;
        }

        switch (status) {
            case COMPLETED:
                complete(p);
                break;
            case QUANTUM_EXHAUSTED:
                demote(p);
                break;
            default:
                break;
        }
    }

    private void complete(Process p) {
        p.setFinishTime(clock);
        p.setTurnaroundTime(clock - p.getArrivalTime());
        p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
        finished.add(p);
        running = null;
        log(p.getId() + " finished");
    }

    private void demote(Process p) {
        tier2.enqueue(p, clock);
        running = null;
        log(p.getId() + " demoted to Tier 2 (used full quantum)");
    }

    private void log(String message) {
        if (events.length() > 0) {
            events.append("; ");
        }
        events.append(message);
    }

    private TimeStepRecord snapshotRecord() {
        List<Process> t1 = tier1.snapshot();
        List<Process> t2 = new ArrayList<>();
        for (Process p : tier2.getSnapshot()) {
            t2.add(new Process(p));
        }

        if (pendingLoad != null) {
            Process shown = new Process(pendingLoad);
            if (pendingTier == 1) {
                t1.add(0, shown);
            } else {
                t2.add(0, shown);
            }
        }

        String who;
        if (running != null) {
            who = running.getId();
        } else if (pendingLoad != null) {
            who = "CS";
        } else {
            who = "IDLE";
        }

        String event = events.toString();
        events.setLength(0);

        return new TimeStepRecord(clock, who, event, t1, t2);
    }
}
