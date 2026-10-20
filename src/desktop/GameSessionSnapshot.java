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
    int incidentEssence,
    int absorbedBlackHoles,
    boolean infiniteFuel,
    int[] weaponLevels,
    int[] abilityLevels,
    String[] abilitySlots,
    int[] utilityLevels,
    String[] utilitySlots
) {
	public GameSessionSnapshot(int level, int score, double fuel, int levelStartCountdown,
			int levelDisplayAlpha, boolean levelStarting, double shipX, double shipY,
			double shipAngle) {
		this(level, score, fuel, levelStartCountdown, levelDisplayAlpha, levelStarting,
				shipX, shipY, shipAngle, 0, 0, false, new int[4], new int[4],
				new String[4], new int[4], new String[4]);
	}
}
