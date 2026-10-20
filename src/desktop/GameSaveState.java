public record GameSaveState(
    int highestScore,
    int highestDuration,
    GameSessionSnapshot activeSession
) {
    public boolean hasActiveSession() {
        return activeSession != null;
    }
}
