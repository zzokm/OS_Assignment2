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
    private int turnaroundTime = -1;
    private int waitingTime = -1;
    private int responseTime = -1;
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
        this.remainingBurstTime = burstTime;
        this.effectivePriority = initialPriority;
        this.stateHistory.add(new StateChange(0, State.NEW));
    }

    public Process(Process source) {
        this.id = source.id;
        this.arrivalTime = source.arrivalTime;
        this.burstTime = source.burstTime;
        this.initialPriority = source.initialPriority;
        this.remainingBurstTime = source.remainingBurstTime;
        this.effectivePriority = source.effectivePriority;
        this.firstStartTime = source.firstStartTime;
        this.finishTime = source.finishTime;
        this.turnaroundTime = source.turnaroundTime;
        this.waitingTime = source.waitingTime;
        this.responseTime = source.responseTime;
        this.state = source.state;
        this.stateHistory.addAll(source.stateHistory);
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

    public void setRemainingBurstTime(int remainingBurstTime) {
        this.remainingBurstTime = remainingBurstTime;
    }

    public void setEffectivePriority(int effectivePriority) {
        this.effectivePriority = effectivePriority;
    }

    public void setFirstStartTime(int firstStartTime) {
        this.firstStartTime = firstStartTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }

    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }

    public int getTurnaroundTime() {
        if (turnaroundTime >= 0) return turnaroundTime;
        return finishTime < 0 ? -1 : finishTime - arrivalTime;
    }

    public int getWaitingTime() {
        if (waitingTime >= 0) return waitingTime;
        int tat = getTurnaroundTime();
        return tat < 0 ? -1 : tat - burstTime;
    }

    public int getResponseTime() {
        if (responseTime >= 0) return responseTime;
        return firstStartTime < 0 ? -1 : firstStartTime - arrivalTime;
    }

    
    public void markReady(int time) {
        if ((state != State.NEW && state != State.RUNNING) || time < arrivalTime) {
            throw new IllegalStateException("Process cannot become ready at time " + time);
        }
        changeState(time, State.READY);
    }

    
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
