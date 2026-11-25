package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Process {
    public enum State { NEW, READY, RUNNING, FINISHED }

    public record StateChange(int time, State state) {}

    private final String id;
    private final int arrivalTime;
    private final int burstTime;
    private final int initialPriority;
    private final List<StateChange> stateHistory = new ArrayList<>();
    private int remainingBurstTime;
    private int effectivePriority;
    private int firstStartTime = -1;
    private int finishTime = -1;
    private State state = State.NEW;

    public Process(String id, int arrivalTime, int burstTime, int initialPriority) {
        this.id = Objects.requireNonNull(id, "id");
        if (id.isBlank() || arrivalTime < 0 || burstTime <= 0) {
            throw new IllegalArgumentException(
                    "A process needs a nonblank ID, nonnegative arrival time, and positive burst time");
        }
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.initialPriority = initialPriority;
        remainingBurstTime = burstTime;
        effectivePriority = initialPriority;
        stateHistory.add(new StateChange(0, State.NEW));
    }

    public Process(Process source) {
        id = source.id;
        arrivalTime = source.arrivalTime;
        burstTime = source.burstTime;
        initialPriority = source.initialPriority;
        remainingBurstTime = source.remainingBurstTime;
        effectivePriority = source.effectivePriority;
        firstStartTime = source.firstStartTime;
        finishTime = source.finishTime;
        state = source.state;
        stateHistory.addAll(source.stateHistory);
    }

    public String getId() { return id; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getRemainingBurstTime() { return remainingBurstTime; }
    public int getInitialPriority() { return initialPriority; }
    public int getEffectivePriority() { return effectivePriority; }
    public int getFirstStartTime() { return firstStartTime; }
    public int getFinishTime() { return finishTime; }
    public State getState() { return state; }
    public List<StateChange> getStateHistory() { return List.copyOf(stateHistory); }

    public void setEffectivePriority(int effectivePriority) {
        this.effectivePriority = effectivePriority;
    }

    /** Returns -1 until the process has finished. */
    public int getTurnaroundTime() {
        return finishTime < 0 ? -1 : finishTime - arrivalTime;
    }

    /** Returns -1 until the process has finished. */
    public int getWaitingTime() {
        return finishTime < 0 ? -1 : getTurnaroundTime() - burstTime;
    }

    /** Returns -1 until the process has used the CPU. */
    public int getResponseTime() {
        return firstStartTime < 0 ? -1 : firstStartTime - arrivalTime;
    }

    /** Admit a new process or return a preempted process to a ready queue. */
    public void markReady(int time) {
        if ((state != State.NEW && state != State.RUNNING) || time < arrivalTime) {
            throw new IllegalStateException("Process cannot become ready at time " + time);
        }
        changeState(time, State.READY);
    }

    /** Execute during [time, time + 1), completing at the end if the burst is exhausted. */
    public void runOneTick(int time) {
        if (state != State.READY && state != State.RUNNING) {
            throw new IllegalStateException("Process must be ready or running to execute");
        }
        if (state == State.READY) {
            changeState(time, State.RUNNING);
        }
        if (firstStartTime < 0) {
            firstStartTime = time;
        }
        remainingBurstTime--;
        if (remainingBurstTime == 0) {
            finishTime = time + 1;
            changeState(finishTime, State.FINISHED);
        }
    }

    private void changeState(int time, State nextState) {
        state = nextState;
        stateHistory.add(new StateChange(time, nextState));
    }
}
