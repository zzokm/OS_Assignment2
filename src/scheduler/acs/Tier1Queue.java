package scheduler.acs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import model.Process;

public class Tier1Queue {
    public enum SliceStatus {
        CONTINUE,
        COMPLETED,
        QUANTUM_EXHAUSTED
    }

    private final int quantum;

    private final Deque<Process> queue = new ArrayDeque<>();

    private Process active;

    private int sliceUsed;

    public Tier1Queue(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Quantum must be positive: " + quantum);
        }
        this.quantum = quantum;
    }

    public void enqueue(Process p) {
        queue.addLast(p);
    }

    public Process dequeue() {
        return queue.pollFirst();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public int getQuantum() {
        return quantum;
    }

    public int getSliceUsed() {
        return sliceUsed;
    }

    public void beginSlice(Process p) {
        active = p;
        sliceUsed = 0;
    }

    public SliceStatus recordTick(Process p) {
        if (p != active) {
            throw new IllegalStateException("Process " + p.getId() + " does not hold the Tier 1 slice");
        }
        sliceUsed++;

        if (p.getRemainingBurstTime() <= 0) {
            active = null;
            sliceUsed = 0;
            return SliceStatus.COMPLETED;
        }
        if (sliceUsed >= quantum) {
            active = null;
            sliceUsed = 0;
            return SliceStatus.QUANTUM_EXHAUSTED;
        }
        return SliceStatus.CONTINUE;
    }

    public List<Process> snapshot() {
        List<Process> copy = new ArrayList<>();
        for (Process p : queue) {
            copy.add(new Process(p));
        }
        return copy;
    }

    public void clear() {
        queue.clear();
        active = null;
        sliceUsed = 0;
    }
}
