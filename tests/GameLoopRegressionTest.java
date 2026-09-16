import java.awt.Polygon;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Headless regression coverage for entity lifetimes, collision removal, and session cleanup. */
public final class GameLoopRegressionTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        String originalHome = System.getProperty("user.home");
        String originalOs = System.getProperty("os.name");
        Path temporaryHome = Files.createTempDirectory("asteroid-loop-test-");
        // AppPaths uses user.home on macOS, independently of APPDATA/XDG_DATA_HOME.
        System.setProperty("user.home", temporaryHome.toString());
        System.setProperty("os.name", "Mac OS X");
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    testBulletLifetimeAndWrapping();
                    testShipGeometryCache();
                    testAsteroidRetirementAndSlowRecovery();
                    testLaserLifecycle();
                    testMenuLifecycle();
                    testPendingChangesOnDeathAndNewGame();
                    testProjectileCollisionConsumption();
                    testPopulationBookkeeping();
                    testLongSessionObjectBounds();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
            System.out.println("GameLoopRegressionTest: " + assertions + " assertions passed");
        } finally {
            System.setProperty("user.home", originalHome);
            System.setProperty("os.name", originalOs);
            try (var paths = Files.walk(temporaryHome)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void testBulletLifetimeAndWrapping() {
        Bullet bullet = new Bullet(1198, 300, Math.PI / 2);
        bullet.update(1200, 700);
        check(Math.abs(bullet.getBounds().getCenterX() - 8) < 0.00001, "bullet wraps while alive");
        for (int frame = 1; frame < 179; frame++) bullet.update(1200, 700);
        check(bullet.isAlive(), "bullet remains alive until its final tick");
        bullet.update(1200, 700);
        check(!bullet.isAlive(), "bullet expires after 180 ticks");
    }

    private static void testShipGeometryCache() {
        Ship ship = new Ship();
        ship.setX(400);
        ship.setY(300);
        ship.setAngle(0);
        Polygon first = ship.getBounds();
        check(first == ship.getBounds(), "unchanged ship reuses its collision polygon");
        check(first.xpoints[0] == 400 && first.ypoints[0] == 290,
                "zero-angle collision nose points upward");
        check(ship.getTipPosition().x == 400 && ship.getTipPosition().y == 290,
                "shot origin matches the rendered nose");
        ship.setAngle(Math.PI / 2);
        Polygon rotated = ship.getBounds();
        check(rotated == first, "rotation updates the cached polygon in place");
        check(rotated.xpoints[0] == 410 && rotated.ypoints[0] == 300,
                "rotated collision nose follows ship angle");
        check(Math.abs(ship.getTipPosition().x - 410) < 0.00001
                        && Math.abs(ship.getTipPosition().y - 300) < 0.00001,
                "rotated shot origin follows ship angle");
    }

    private static void testAsteroidRetirementAndSlowRecovery() {
        Asteroid entering = new Asteroid(Asteroid.Size.LARGE, -100, 200, 2, 0);
        Asteroid leaving = new Asteroid(Asteroid.Size.LARGE, -100, 200, -2, 0);
        check(!entering.hasExited(1200, 700), "offscreen inward spawn survives");
        check(leaving.hasExited(1200, 700), "fully offscreen outward asteroid retires");
        Asteroid touching = new Asteroid(Asteroid.Size.LARGE, 0, 200, -2, 0);
        check(!touching.hasExited(1200, 700), "partly visible asteroid survives");
        Asteroid accelerated = new Asteroid(Asteroid.Size.SMALL, 100, 100, 2, 0);
        int acceleratedBefore = accelerated.getBounds().xpoints[0];
        accelerated.setMultiplier(2.0);
        accelerated.update();
        check(accelerated.getBounds().xpoints[0] == acceleratedBefore + 4,
                "level multiplier accelerates the asteroid instance it is applied to");
        Asteroid asteroid = new Asteroid(Asteroid.Size.LARGE, 200, 200, 4, 0);
        int before = asteroid.getBounds().xpoints[0];
        asteroid.setSlowedVelocity(0.25);
        asteroid.toggleTimeSlow(true);
        asteroid.update();
        check(asteroid.getBounds().xpoints[0] == before + 1, "slow applies configured speed");
        asteroid.toggleTimeSlow(false);
        asteroid.update();
        check(asteroid.getBounds().xpoints[0] == before + 5, "normal speed restores after slow");
        Polygon shape = asteroid.getBounds();
        check(shape.getBounds().equals(asteroid.getBoundingBox()), "cached polygon and bounding box agree");
        check(!asteroid.intersects(new Polygon(new int[] {900, 910, 910}, new int[] {500, 500, 510}, 3)),
                "distant polygon broad phase rejects collision");
    }

    private static void testLaserLifecycle() {
        VoidLaser laser = new VoidLaser(100, 100, Math.PI / 2, 1200, 700);
        for (int i = 0; i < 100; i++) check(laser.isAlive(), "queries do not advance laser lifetime");
        for (int i = 0; i < 9; i++) laser.update();
        check(laser.isAlive(), "laser alive before tenth tick");
        laser.update();
        check(!laser.isAlive(), "laser expires on tenth tick");
    }

    private static void testMenuLifecycle() throws Exception {
        AsteroidGame game = createGame(false);
        int mouse = game.getMouseListeners().length;
        int motion = game.getMouseMotionListeners().length;
        int wheel = game.getMouseWheelListeners().length;
        for (int i = 0; i < 100; i++) game.setupMenu();
        check(game.getMouseListeners().length == mouse, "menu mouse listener count stays constant");
        check(game.getMouseMotionListeners().length == motion, "menu motion listener count stays constant");
        check(game.getMouseWheelListeners().length == wheel, "menu wheel listener count stays constant");
        BufferedImage canvas = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            game.paint(graphics);
            Object firstLayer = get(game, "menuBackgroundLayer");
            game.paint(graphics);
            check(get(game, "menuBackgroundLayer") == firstLayer,
                    "idle menu reuses its composed background image");
            game.setSize(1000, 600);
            game.updateDimensions();
            game.paint(graphics);
            check(get(game, "menuBackgroundLayer") != firstLayer,
                    "menu background cache rebuilds after resize");
        } finally {
            graphics.dispose();
        }
        StoryTerminal terminal = (StoryTerminal)get(game, "storyTerminal");
        set(terminal, "bootTime", 0L);
        tick(game);
        check((int)get(terminal, "bootPhase") == 0, "hidden story does not update while menu idles");
        set(game, "showingStory", true);
        tick(game);
        check((int)get(terminal, "bootPhase") > 0, "visible story still updates");
    }

    private static void testPendingChangesOnDeathAndNewGame() throws Exception {
        AsteroidGame game = createGame(true);
        stageChanges(game);
        invoke(game, "startDeathSequence");
        checkPendingEmpty(game);
        for (String name : new String[] {"asteroids", "bullets", "voidLasers", "voidHazards", "blackHoles", "cosmicEntities", "voidCreatures"}) {
            check(collection(game, name).isEmpty(), "death clears " + name);
        }
        stageChanges(game);
        game.startNewGame();
        checkPendingEmpty(game);
        check((int)get(game, "currentTotalCreatureSizeValue") == 0, "new run resets creature total");
        check((int)get(game, "currentTotalEntitySizeValue") == 0, "new run resets entity total");
        check((int)get(game, "spawnCreaturesCooldown") == 0, "new run resets spawn cooldown");
    }

    private static void testProjectileCollisionConsumption() throws Exception {
        AsteroidGame game = createGame(true);
        collection(game, "asteroids").add(new Asteroid(Asteroid.Size.SMALL, 200, 200, 0, 0));
        collection(game, "asteroids").add(new Asteroid(Asteroid.Size.SMALL, 200, 200, 0, 0));
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        tick(game);
        check(collection(game, "asteroids").size() == 1, "one bullet destroys only its first asteroid");
        check(collection(game, "bullets").isEmpty(), "hit bullet is removed");
        check((int)get(game, "score") == 5, "one asteroid earns one reward");

        game = createGame(true);
        collection(game, "asteroids").add(new Asteroid(Asteroid.Size.MEDIUM, 200, 200, 0, 0));
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        tick(game);
        check(collection(game, "asteroids").size() == 2, "two simultaneous bullets split an asteroid only once");
        check((int)get(game, "score") == 10, "removed asteroid cannot award duplicate rewards");

        game = createGame(true);
        CosmicEntity entity = new CosmicEntity(1200, 700, 1);
        set(entity, "x", 200.0);
        set(entity, "y", 200.0);
        set(entity, "currentHP", 100);
        collection(game, "cosmicEntities").add(entity);
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        tick(game);
        check((int)get(entity, "currentHP") == 75, "bullet damages cosmic entity once");

        game = createGame(true);
        Asteroid asteroid = new Asteroid(Asteroid.Size.SMALL, 200, 200, 4, 0);
        collection(game, "asteroids").add(asteroid);
        AbilityManager abilities = (AbilityManager)get(game, "abilityManager");
        set(abilities, "timeSlowActive", true);
        set(abilities, "timeSlowDuration", 2);
        int originalX = asteroid.getBounds().xpoints[0];
        tick(game);
        int slowX = asteroid.getBounds().xpoints[0];
        tick(game);
        check(slowX - originalX < 4, "game loop applies time slow");
        check(asteroid.getBounds().xpoints[0] - slowX == 4, "game loop restores speed when ability expires");
    }

    private static void testPopulationBookkeeping() throws Exception {
        AsteroidGame game = createGame(true);
        set(game, "level", 1);
        set(game, "targetTotalCreatureSizeValue", 0);
        set(game, "targetTotalEntitySizeValue", 4);
        invoke(game, "trySpawnEntities");
        check(!collection(game, "cosmicEntities").isEmpty(), "entity spawning uses entity budget");
        set(game, "targetTotalCreatureSizeValue", 1);
        invoke(game, "trySpawnCreatures");
        check(!collection(game, "voidCreatures").isEmpty(), "remaining size budget of one can spawn");
        Ship ship = (Ship)get(game, "ship");
        ship.getVoidEnergy().activate();
        set(game, "targetTotalCreatureSizeValue", get(game, "currentTotalCreatureSizeValue"));
        set(game, "spawnCreaturesCooldown", 60);
        invoke(game, "trySpawnCreatures");
        long centipedes = collection(game, "voidCreatures").stream()
                .map(value -> (VoidCreature)value)
                .filter(value -> value.getType() == VoidCreature.CreatureType.CENTIPEDE).count();
        check(centipedes == 1, "centipede spawns despite full specter budget and cooldown");
        invoke(game, "trySpawnCreatures");
        check(collection(game, "voidCreatures").stream().map(value -> (VoidCreature)value)
                .filter(value -> value.getType() == VoidCreature.CreatureType.CENTIPEDE).count() == 1,
                "centipede cannot spawn twice");
        for (Object entity : collection(game, "cosmicEntities")) {
            collection(game, "cosmicEntitiesToRemove").add(entity);
            collection(game, "cosmicEntitiesToRemove").add(entity);
        }
        for (Object creature : collection(game, "voidCreatures")) collection(game, "creaturesToRemove").add(creature);
        invoke(game, "applyPendingChanges");
        check((int)get(game, "currentTotalEntitySizeValue") == 0, "duplicate removal cannot make entity total negative");
        check((int)get(game, "currentTotalCreatureSizeValue") == 0, "removal reconciles creature total including free centipede");

        game = createGame(true);
        set(game, "fadeActive", true);
        set(game, "fadeTimer", 100000);
        for (int level = 1; level <= 40; level++) {
            collection(game, "asteroids").clear();
            set(game, "levelStarting", true);
            set(game, "levelStartCountdown", 1);
            tick(game);
            for (int attempt = 0; attempt < 50; attempt++) {
                set(game, "spawnCreaturesCooldown", 0);
                set(game, "spawnEntitiesCooldown", 0);
                invoke(game, "trySpawnCreatures");
                invoke(game, "trySpawnEntities");
            }
            int creatures = collection(game, "voidCreatures").stream().mapToInt(value -> ((VoidCreature)value).getSizeValue()).sum();
            int entities = collection(game, "cosmicEntities").stream().mapToInt(value -> ((CosmicEntity)value).getSizeValue()).sum();
            check(creatures == (int)get(game, "currentTotalCreatureSizeValue"), "creature total survives level transition");
            check(entities == (int)get(game, "currentTotalEntitySizeValue"), "entity total survives level transition");
            check(creatures <= (int)get(game, "targetTotalCreatureSizeValue") + 2, "specter population stays within target plus one size overshoot");
            check(entities <= (int)get(game, "targetTotalEntitySizeValue") + 3, "entity population stays within target plus one size overshoot");
            check(collection(game, "voidHazards").size() <= 5,
                    "void hazards remain bounded across level transitions");
        }
    }

    private static void testLongSessionObjectBounds() throws Exception {
        AsteroidGame game = createGame(true);
        int maxBullets = 0;
        for (int frame = 0; frame < 10000; frame++) {
            if (frame % 10 == 0) collection(game, "bullets").add(new Bullet(0, 0, Math.PI / 2));
            if (frame % 20 == 0) collection(game, "asteroids").add(new Asteroid(Asteroid.Size.SMALL, -100, 200, -2, 0));
            tick(game);
            maxBullets = Math.max(maxBullets, collection(game, "bullets").size());
            check(collection(game, "asteroids").isEmpty(), "outward asteroids are removed during long session");
        }
        check(maxBullets <= 18, "continuous shooting reaches bounded live bullet population");
        for (int frame = 0; frame < 180; frame++) tick(game);
        check(collection(game, "bullets").isEmpty(), "all bullets retire after shooting stops");
        checkPendingEmpty(game);
    }

    private static AsteroidGame createGame(boolean playing) throws Exception {
        AsteroidGame game = new AsteroidGame();
        ((Timer)get(game, "timer")).stop();
        game.setSize(1200, 700);
        game.updateDimensions();
        if (playing) {
            set(game, "gameState", "playing");
            set(game, "level", 0);
            set(game, "levelStarting", false);
            set(game, "levelTimer", Integer.MAX_VALUE);
        }
        return game;
    }

    private static void stageChanges(AsteroidGame game) throws Exception {
        Asteroid asteroid = new Asteroid(Asteroid.Size.SMALL, 200, 200, 1, 0);
        collection(game, "asteroidsToAdd").add(asteroid);
        collection(game, "asteroidsToRemove").add(asteroid);
        collection(game, "hazardsToAdd").add(new VoidHazard(1200, 700, VoidHazard.Type.values()[0]));
        collection(game, "bulletsToRemove").add(new Bullet(200, 200, 0));
        collection(game, "lasersToRemove").add(new VoidLaser(200, 200, 0, 1200, 700));
        collection(game, "creaturesToRemove").add(VoidCreature.createSpecter(1200, 700, 1));
        collection(game, "cosmicEntitiesToRemove").add(new CosmicEntity(1200, 700, 1));
    }

    private static void checkPendingEmpty(AsteroidGame game) throws Exception {
        for (String name : new String[] {"asteroidsToAdd", "hazardsToAdd", "asteroidsToRemove", "lasersToRemove", "bulletsToRemove", "voidHazardsToRemove", "blackHolesToRemove", "cosmicEntitiesToRemove", "creaturesToRemove"}) {
            check(collection(game, name).isEmpty(), "pending collection clears: " + name);
        }
    }

    private static void tick(AsteroidGame game) throws Exception {
        invoke(game, "updateGameTick");
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> collection(Object target, String name) throws Exception {
        return (Collection<Object>)get(target, name);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void invoke(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
