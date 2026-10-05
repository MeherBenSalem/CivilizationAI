package tn.naizo.smartvillagers.schedule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * One decrement per call. Must be ticked once per server tick, not once per dimension.
 */
public final class ReplyDelayQueue {
    private final List<Task> pending = new ArrayList<>();

    public void enqueue(Task task) {
        synchronized (pending) {
            pending.add(task);
        }
    }

    public List<Task> takeReady() {
        List<Task> ready = new ArrayList<>();
        synchronized (pending) {
            Iterator<Task> iterator = pending.iterator();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                task.remainingTicks--;
                if (task.remainingTicks <= 0) {
                    ready.add(task);
                    iterator.remove();
                }
            }
        }
        return ready;
    }

    public void clear() {
        synchronized (pending) {
            pending.clear();
        }
    }

    public int size() {
        synchronized (pending) {
            return pending.size();
        }
    }

    public static final class Task {
        public final UUID playerId;
        public final UUID villagerId;
        public final String playerMessage;
        public final String replyText;
        int remainingTicks;

        public Task(UUID playerId, UUID villagerId, String playerMessage, String replyText, int remainingTicks) {
            this.playerId = playerId;
            this.villagerId = villagerId;
            this.playerMessage = playerMessage;
            this.replyText = replyText;
            this.remainingTicks = Math.max(1, remainingTicks);
        }
    }
}
