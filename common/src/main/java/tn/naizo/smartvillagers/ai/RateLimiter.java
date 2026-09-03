package tn.naizo.smartvillagers.ai;

import tn.naizo.smartvillagers.config.SmartVillagersConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RateLimiter {
    private final Map<UUID, Long> lastRequestMs = new ConcurrentHashMap<>();
    private final AtomicInteger concurrent = new AtomicInteger();
    private final AtomicInteger windowCount = new AtomicInteger();
    private volatile long windowStartMs = System.currentTimeMillis();

    public boolean tryAcquire(UUID playerId) {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        long now = System.currentTimeMillis();

        synchronized (this) {
            if (now - windowStartMs >= 60_000L) {
                windowStartMs = now;
                windowCount.set(0);
            }
            if (windowCount.get() >= config.globalRequestsPerMinute()) {
                return false;
            }
        }

        Long last = lastRequestMs.get(playerId);
        if (last != null && now - last < config.playerCooldownMs()) {
            return false;
        }

        if (concurrent.get() >= config.maxConcurrent()) {
            return false;
        }

        lastRequestMs.put(playerId, now);
        synchronized (this) {
            windowCount.incrementAndGet();
        }
        concurrent.incrementAndGet();
        return true;
    }

    public void release() {
        concurrent.updateAndGet(value -> Math.max(0, value - 1));
    }

    public void reset() {
        lastRequestMs.clear();
        concurrent.set(0);
        windowCount.set(0);
        windowStartMs = System.currentTimeMillis();
    }
}
