public record GameSessionSnapshot(
    int level,
    int score,
    double fuel,
    int levelStartCountdown,
    int levelDisplayAlpha,
    boolean levelStarting,
    double shipX,
    double shipY,
    double shipAngle,
    int shipLevel,
    int shipXP,
    int shipXPToNextLevel
) {
}
