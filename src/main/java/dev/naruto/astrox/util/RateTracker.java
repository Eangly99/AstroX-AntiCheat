package dev.naruto.astrox.util;

public final class RateTracker {
    private final long windowNanos;
    private long windowStart;
    private int count;

    public RateTracker(long windowMillis) {
        this.windowNanos = windowMillis * 1_000_000L;
        this.windowStart = System.nanoTime();
    }

    public int hit() {
        long now = System.nanoTime();
        if (now - windowStart >= windowNanos) {
            windowStart = now;
            count = 0;
        }
        return ++count;
    }

    public void reset() {
        windowStart = System.nanoTime();
        count = 0;
    }
}
