package scheduler.acs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import model.Process;

public class Tier2PriorityQueue {
    private static final class Entry {
        private final Process process;
        private final int enqueuedAt;
        private final long sequence;

        private Entry(Process process, int enqueuedAt, long sequence) {
            this.process = process;
            this.enqueuedAt = enqueuedAt;
            this.sequence = sequence;
        }
    }

    private static final Comparator<Entry> PRIORITY_ORDER = Comparator
            .comparingInt((Entry entry) -> entry.process.getEffectivePriority())
            .thenComparingLong(entry -> entry.sequence);

    private final List<Entry> queue = new ArrayList<>();
    private final AgingEngine agingEngine = new AgingEngine();
    private long nextSequence;

    public void enqueue(Process process, int currentTime) {
        if (currentTime < 0) {
            throw new IllegalArgumentException("Current time must not be negative");
        }
        queue.add(new Entry(Objects.requireNonNull(process, "process"), currentTime, nextSequence++));
    }

    public void enqueue(Process process) {
        enqueue(process, 0);
    }

    public Process dequeueHighestPriority(int currentTime) {
        if (currentTime < 0) {
            throw new IllegalArgumentException("Current time must not be negative");
        }
        if (queue.isEmpty()) {
            return null;
        }

        for (Entry entry : queue) {
            agingEngine.applyAging(entry.process, currentTime - entry.enqueuedAt);
        }

        int highestPriorityIndex = 0;
        for (int i = 1; i < queue.size(); i++) {
            if (PRIORITY_ORDER.compare(queue.get(i), queue.get(highestPriorityIndex)) < 0) {
                highestPriorityIndex = i;
            }
        }
        return queue.remove(highestPriorityIndex).process;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public List<Process> getSnapshot() {
        List<Entry> orderedEntries = new ArrayList<>(queue);
        orderedEntries.sort(PRIORITY_ORDER);

        List<Process> snapshot = new ArrayList<>(orderedEntries.size());
        for (Entry entry : orderedEntries) {
            snapshot.add(new Process(entry.process));
        }
        return snapshot;
    }

    public void clear() {
        queue.clear();
        nextSequence = 0;
    }
}
