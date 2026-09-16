/** Converts elapsed time into fixed 60 Hz simulation steps, independent of repaint speed. */
public final class GameClock {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final int TICKS_PER_SECOND = 60;
    // Eight ticks covers callback rates down to about 7.5 Hz without letting a
    // restored/suspended browser tab monopolize the event thread.
    private static final int MAX_CATCH_UP_TICKS = 8;
    private long lastNanos;
    private long accumulatedTickNanos;

    public GameClock(long nowNanos) {
        reset(nowNanos);
    }

    public void reset(long nowNanos) {
        lastNanos = nowNanos;
        accumulatedTickNanos = 0;
    }

    public int advance(long nowNanos) {
        long elapsed = nowNanos - lastNanos;
        lastNanos = nowNanos;
        if (elapsed <= 0) return 0;
        // A suspended tab must not enqueue minutes of simulation on its next callback.
        elapsed = Math.min(elapsed, 250_000_000L);
        accumulatedTickNanos += elapsed * TICKS_PER_SECOND;
        int ticks = (int)(accumulatedTickNanos / NANOS_PER_SECOND);
        accumulatedTickNanos %= NANOS_PER_SECOND;
        return Math.min(ticks, MAX_CATCH_UP_TICKS);
    }
}
