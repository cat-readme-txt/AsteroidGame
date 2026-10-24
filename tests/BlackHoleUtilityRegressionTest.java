import java.awt.geom.Line2D;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.List;

public final class BlackHoleUtilityRegressionTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        BlackHole natural = new BlackHole(200, 200, 80);
        for (int i = 0; i < 180; i++) natural.update(1200, 700);
        check(natural.isActive(), "natural hole activates after its warning");
        double before = natural.getSize();
        natural.grow(5);
        check(natural.getSize() == before + 5, "consumed matter grows the hole");
        Line2D beam = new Line2D.Double(100, 200, 400, 200);
        double impact = natural.firstIntersection(beam).x;
        check(impact < natural.getX() && impact > 140, "laser clips at the near edge");
        natural.shrinkFromLaser();
        check(natural.getSize() == before - 1, "Void laser shrinks the hole");
        while (natural.getSize() > 32) natural.shrinkFromLaser();
        check(natural.canAbsorb(3) && !natural.canAbsorb(2),
                "only a maxed modulator marks a small natural hole");
        while (natural.getSize() >= 18) natural.shrinkFromLaser();
        check(natural.shouldBreak(0) && !natural.shouldBreak(3),
                "an unmodulated hole breaks below the smaller threshold");

        BlackHole fired = BlackHole.fired(1190, 350, 44, Math.PI / 2, 40);
        for (int i = 0; i < 4; i++) fired.update(1200, 700);
        check(fired.isActive() && fired.getX() < 40, "launched hole wraps at the edge");
        check(!fired.isShipConsumed(fired.getX(), fired.getY()),
                "player hole is harmless to the ship");
        double ownedSize = fired.getSize();
        fired.shrinkFromLaser();
        check(fired.getSize() == ownedSize, "Void lasers do not shrink a purple player hole");
        double largerLife = Math.max(natural.getLifetimeFrames(), fired.getLifetimeFrames());
        double smallerLife = Math.min(natural.getLifetimeFrames(), fired.getLifetimeFrames());
        BlackHole merged = BlackHole.merge(natural, fired);
        check(merged.isPlayerOwned() && merged.getSize() > fired.getSize(),
                "native and player holes join into a larger purple hole");
        check(Math.abs(merged.getLifetimeFrames() - (largerLife + smallerLife / 2)) < 0.001,
                "merged lifetime uses longer plus half shorter");
        BlackHole large = new BlackHole(300, 300, 100);
        BlackHole smallHole = new BlackHole(500, 300, 40);
        for (int i = 0; i < 180; i++) { large.update(1200, 700); smallHole.update(1200, 700); }
        large.attractToward(smallHole);
        smallHole.attractToward(large);
        large.update(1200, 700);
        smallHole.update(1200, 700);
        check(smallHole.getX() - 500 < -(large.getX() - 300),
                "smaller hole moves faster toward the larger mass");
        BlackHole shortLife = new BlackHole(300, 300, 80);
        BlackHole giant = new BlackHole(500, 300, 280);
        for (int i = 0; i < 180; i++) { shortLife.update(); giant.update(); }
        giant.grow(1000);
        check(giant.getSize() == 420, "black hole grows to the raised maximum size");
        check(giant.getLifetimeMultiplier() > shortLife.getLifetimeMultiplier()
                        && Math.abs(giant.getLifetimeMultiplier() - 3.2) < 0.001,
                "larger holes spend their life pool more slowly");
        check(giant.getEstimatedRemainingFrames() > shortLife.getEstimatedRemainingFrames()
                        && giant.getPullRadius() <= 350,
                "growth extends life without increasing long-distance gravity");
        check(giant.avoidanceForce(giant.getX() + 260, giant.getY()).x > 0
                        && giant.unsafeTarget(giant.getX() + 220, giant.getY(), 15),
                "active black holes mark near targets unsafe and push bots outward");
        for (int i = 0; i < 1900; i++) { giant.update(); shortLife.update(); }
        check(shortLife.isExpired() && giant.isActive(),
                "the smaller hole expires first");
        for (int i = 0; i < 2060; i++) giant.update();
        check(giant.isActive(), "a fully grown natural hole spans two 33-second levels");
        BlackHole grownFired = BlackHole.fired(300, 300, 44, 0, 0);
        grownFired.update();
        grownFired.grow(1000);
        for (int i = 0; i < 3960; i++) grownFired.update();
        check(grownFired.isActive(), "a fully grown launched hole also spans two full levels");

        VoidEnergy energy = new VoidEnergy();
        energy.setModulatorLevel(3);
        energy.setEnergyForDeveloper(60);
        energy.activate();
        energy.update();
        check(Math.abs(energy.getEnergy() - 60.1) < 0.001,
                "idle Void energy rises naturally beyond fifty");
        energy.absorbForAction(2);
        check(energy.getEnergy() < 60.1 && energy.getEnergy() >= 0,
                "modulator drains Void energy above fifty");
        energy.setEnergyForDeveloper(99.9);
        energy.update();
        check(energy.getEnergy() == 100,
                "idle Void energy can reach one hundred with the modulator");
        energy.setEnergyForDeveloper(40);
        energy.absorbForAction(2);
        check(energy.getEnergy() > 40, "actions still gain energy below fifty");
        energy.setEnergyForDeveloper(0);
        energy.update();
        check(energy.getEnergy() > 0, "passive absorption still starts at zero");

        UtilitySystem utilities = new UtilitySystem();
        utilities.setLevel(UtilitySystem.METRIC, 1);
        double outer = utilities.metricSpeedScale(200, 0, -2, 0, 0, 0);
        double inner = utilities.metricSpeedScale(80, 0, -2, 0, 0, 0);
        check(Math.abs(outer - 0.8) < 0.001 && Math.abs(inner - 0.512) < 0.001,
                "three inverse-square rings apply cumulative level-one slowdown");
        check(utilities.metricSpeedScale(80, 0, 2, 0, 0, 0) == 1,
                "objects moving away are not slowed");
        utilities.setLevel(UtilitySystem.SWARM, 1);
        check(utilities.getSwarmChargePercent() == 0,
                "Swarm cooldown ring starts empty after construction");
        Ship ship = new Ship();
        for (int i = 0; i < 60; i++) utilities.updateSwarm(ship, List.of(), List.of(), 1200, 700);
        check(utilities.getSwarmCount() == 2, "first Swarm wave has two spaced bots");
        check(utilities.getSwarmChargePercent() == 0,
                "Swarm cooldown ring restarts when a wave spawns");
        double firstX = utilities.getBots().get(0).getX();
        double secondX = utilities.getBots().get(1).getX();
        double firstY = utilities.getBots().get(0).getY();
        double secondY = utilities.getBots().get(1).getY();
        check(Math.hypot(firstX - secondX, firstY - secondY) > 80,
                "Swarm wave starts at separate points around ship");
        for (int i = 0; i < 600; i++) utilities.updateSwarm(ship, List.of(), List.of(), 1200, 700);
        check(utilities.getSwarmCount() == 2 && utilities.getSwarmCountdownFrames() == 600,
                "Swarm replaces its wave at the ten-second interval");
        utilities.setLevel(UtilitySystem.SWARM, 2);
        for (int i = 0; i < 480; i++) utilities.updateSwarm(ship, List.of(), List.of(), 1200, 700);
        check(utilities.getSwarmCount() == 4 && utilities.getSwarmCountdownFrames() == 480,
                "level-two Swarm has four bots at an eight-second interval");
        utilities.setLevel(UtilitySystem.SWARM, 3);
        for (int i = 0; i < 300; i++) utilities.updateSwarm(ship, List.of(), List.of(), 1200, 700);
        check(utilities.getSwarmCount() == 6 && utilities.getSwarmCountdownFrames() == 300,
                "level-three Swarm has six bots at a five-second interval");
        UtilitySystem safeSwarm = new UtilitySystem();
        safeSwarm.setLevel(UtilitySystem.SWARM, 1);
        Ship safeShip = new Ship();
        safeShip.setX(600);
        safeShip.setY(350);
        BlackHole danger = new BlackHole(600, 180, 80);
        for (int i = 0; i < 180; i++) danger.update();
        List<UtilitySystem.Target> unsafeTarget = List.of(
                new UtilitySystem.Target(new Object(), 600, 180, 10));
        for (int i = 0; i < 90; i++) {
            safeSwarm.updateSwarm(safeShip, unsafeTarget, List.of(danger), 1200, 700);
        }
        check(safeSwarm.drainBlasts().isEmpty() && safeSwarm.getSwarmCount() == 2,
                "Swarm avoids a target within the black hole danger area");

        File save = Files.createTempFile("asteroid-game-v3", ".dat").toFile();
        try {
            GameSaveStore store = new GameSaveStore(save);
            GameSessionSnapshot session = new GameSessionSnapshot(8, 250, 300, 0, 255,
                    false, 300, 200, 0, 24, 2, true,
                    new int[]{1, 2, 0, 0}, new int[]{1, 2, 0, 0},
                    new String[]{"teleport", "blackhole", null, null},
                    new int[]{3, 1, 0, 1}, new String[]{"modulator", "swarm", null, "shield"});
            store.saveActiveSession(500, 30, session);
            GameSessionSnapshot loaded = store.loadState().activeSession();
            check(loaded != null && loaded.infiniteFuel() && loaded.absorbedBlackHoles() == 2
                            && loaded.incidentEssence() == 24, "save retains absorption and M-AM state");
            check(loaded.utilityLevels()[0] == 3 && "blackhole".equals(loaded.abilitySlots()[1])
                            && loaded.weaponLevels()[1] == 2, "save retains all construction loadouts");
        } finally { Files.deleteIfExists(save.toPath()); }
        File oldSave = Files.createTempFile("asteroid-game-v3-migration", ".dat").toFile();
        try {
            GameSaveStore store = new GameSaveStore(oldSave);
            GameSessionSnapshot oldSession = new GameSessionSnapshot(8, 250, 300, 0, 255,
                    false, 300, 200, 0, 24, 2, true,
                    new int[4], new int[]{1, 3, 2, 1},
                    new String[]{"teleport", "blackhole", "drone", "nova"},
                    new int[4], new String[4]);
            store.saveActiveSession(500, 30, oldSession);
            try (RandomAccessFile file = new RandomAccessFile(oldSave, "rw")) {
                file.seek(4);
                file.writeInt(3);
            }
            GameSessionSnapshot migrated = store.loadState().activeSession();
            check(migrated != null && migrated.abilityLevels()[0] == 1
                            && migrated.abilityLevels()[1] == 2
                            && migrated.abilityLevels()[2] == 1
                            && migrated.abilityLevels()[3] == 3
                            && "blackhole".equals(migrated.abilitySlots()[1]),
                    "version-three ability levels migrate to the new display order");
        } finally { Files.deleteIfExists(oldSave.toPath()); }
        System.out.println("Black hole and utility regression checks passed: " + checks);
    }
}
