public final class GameClockRegressionTest {
    public static void main(String[] args) {
        checkCadence(1_000_000L, 1_000);
        checkCadence(10_000_000L, 100);
        checkCadence(20_000_000L, 50);
        checkCadence(50_000_000L, 20);
        checkCadence(125_000_000L, 8);
        GameClock clock = new GameClock(0);
        check(clock.advance(0) == 0, "zero elapsed time");
        check(clock.advance(10_000_000_000L) == 8, "long stalls cap catch-up work");
        check(clock.advance(10_000_000_001L) == 0, "stall backlog is discarded");
        clock.reset(20_000_000_000L);
        check(clock.advance(20_010_000_000L) == 0, "reset clears fractional debt");
        check(clock.advance(20_020_000_000L) == 1, "fractional time accumulates");
        check(clock.advance(20_019_000_000L) == 0, "backward input never runs a negative step");
        clock.reset(Long.MAX_VALUE - 10_000_000L);
        check(clock.advance(Long.MIN_VALUE + 10_000_000L) == 1, "nanoTime wraparound");
        System.out.println("GameClockRegressionTest: PASS");
    }

    private static void checkCadence(long interval, int callbacks) {
        GameClock clock = new GameClock(0);
        int total = 0;
        for (int i = 1; i <= callbacks; i++) total += clock.advance(i * interval);
        check(total == 60, "one second must simulate 60 ticks at " + interval + "ns cadence");
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
