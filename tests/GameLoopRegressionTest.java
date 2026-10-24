import java.awt.Polygon;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
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
                    testVoidPropulsion();
                    testAsteroidRetirement();
                    testLaserLifecycle();
                    testDeveloperTerminalAndVoidState();
                    testDeveloperResetDefaults();
                    testHeldShootingWithMovement();
                    testMenuLifecycle();
                    testStorySkippingAndPauseKeys();
                    testPendingChangesOnDeathAndNewGame();
                    testProjectileCollisionConsumption();
                    testPassiveShieldBurst();
                    testTeleportCollisionProtection();
                    testUpgradedProjectileCollisions();
                    testCompanionAsteroidFuel();
                    testIncidentEssenceRewards();
                    testConstructPurchasesAndSlots();
                    testConfigurableConstructionKey();
                    testConstructionControlShortcuts();
                    testBlackHoleChargeAndTerminalUnlock();
                    testPopulationBookkeeping();
                    testAsteroidPopulationCap();
                    testBlackHoleConsumption();
                    testOwnedBlackHoleLaser();
                    testResumedHazards();
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

        Object firstTarget = new Object();
        Object secondTarget = new Object();
        Bullet piercing = new Bullet(100, 100, 0, true);
        check(piercing.registerHit(firstTarget), "piercing bullet survives its first hit");
        check(!piercing.canHit(firstTarget), "piercing bullet cannot damage one target twice");
        check(piercing.registerHit(secondTarget), "piercing bullet survives another hit");
        for (int frame = 0; frame < 180; frame++) piercing.update(1200, 700);
        check(!piercing.isAlive(), "piercing bullet still expires at its normal lifetime");

        Bullet limited = new Bullet(100, 100, Math.PI / 2, 1, 2);
        check(limited.getRadius() == 7, "bullet enlargement changes collision size");
        check(limited.registerHit(firstTarget), "level-one piercing survives first hit");
        check(limited.registerHit(secondTarget), "level-one piercing survives second hit");
        check(!limited.registerHit(new Object()), "level-one piercing ends after three hits");
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

    private static void testVoidPropulsion() {
        Ship hyperShip = new Ship();
        hyperShip.setHyper(true);
        Ship voidShip = new Ship();
        voidShip.getVoidEnergy().setEnergyForDeveloper(25);
        double startingFuel = voidShip.getFuel();

        hyperShip.applyThrust();
        hyperShip.updatePhysics();
        voidShip.applyThrust();
        voidShip.updatePhysics();
        check(voidShip.getVelocity() > hyperShip.getVelocity(),
                "Void thrust accelerates faster than M-AM hyper thrust");
        check(voidShip.getFuel() == startingFuel
                        && Math.abs(voidShip.getVoidEnergy().getEnergy() - 24.9) < 0.00001,
                "stored Void energy powers thrust without spending M-AM");
        double initialVoidAcceleration = voidShip.getVelocity();

        for (int tick = 1; tick < 100; tick++) {
            hyperShip.applyThrust();
            hyperShip.updatePhysics();
            voidShip.applyThrust();
            voidShip.updatePhysics();
        }
        check(voidShip.getVelocity() > hyperShip.getVelocity()
                        && voidShip.getFuel() == startingFuel,
                "Void propulsion maintains a higher speed limit without fuel cost");

        Ship voidHyperShip = new Ship();
        voidHyperShip.setHyper(true);
        voidHyperShip.getVoidEnergy().setEnergyForDeveloper(25);
        voidHyperShip.applyThrust();
        voidHyperShip.updatePhysics();
        check(voidHyperShip.getVelocity() > initialVoidAcceleration
                        && voidHyperShip.getFuel() == startingFuel,
                "hyper with Void energy accelerates further without spending fuel");

        Ship ordinaryTurn = new Ship();
        Ship chargedTurn = new Ship();
        chargedTurn.getVoidEnergy().setEnergyForDeveloper(10);
        ordinaryTurn.rotateLeft();
        chargedTurn.rotateLeft();
        check(Math.abs(ordinaryTurn.getAngle() - chargedTurn.getAngle()) < 0.00001
                        && Math.abs(chargedTurn.getVoidEnergy().getEnergy() - 9.9) < 0.00001,
                "stored Void energy drains on turns without changing turn speed");

        Ship activeVoidShip = new Ship();
        activeVoidShip.getVoidEnergy().setEnergyForDeveloper(40);
        activeVoidShip.getVoidEnergy().activate();
        activeVoidShip.applyThrust();
        check(activeVoidShip.getFuel() == startingFuel
                        && activeVoidShip.getVoidEnergy().getEnergy() > 40,
                "Void thrust uses no M-AM and still gains energy below the modulator threshold");
        activeVoidShip.getVoidEnergy().setModulatorLevel(1);
        activeVoidShip.getVoidEnergy().setEnergyForDeveloper(60);
        activeVoidShip.applyThrust();
        check(activeVoidShip.getVoidEnergy().getEnergy() < 60,
                "modulated Void thrust drains energy above fifty");
    }

    private static void testAsteroidRetirement() {
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
        asteroid.update();
        check(asteroid.getBounds().xpoints[0] == before + 4, "asteroid uses normal speed");
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
        VoidLaser spent = new VoidLaser(100, 100, 0, 1200, 700);
        spent.markSpent();
        check(spent.isSpent() && spent.isAlive(), "spent laser remains visible until normal expiry");
    }

    private static void testDeveloperTerminalAndVoidState() throws Exception {
        AsteroidGame game = createGame(true);
        DeveloperTerminal terminal = (DeveloperTerminal) get(game, "developerTerminal");
        terminal.executeLine("set cosmic-weight 3");
        terminal.executeLine("set void-weight 2");
        check((int) get(game, "targetTotalEntityWeight") == 3
                        && (int) get(game, "currentTotalEntityWeight") == 3,
                "cosmic weight override spawns its exact budget immediately");
        check((int) get(game, "targetTotalCreatureWeight") == 2
                        && (int) get(game, "currentTotalCreatureWeight") == 2,
                "void weight override spawns its exact budget immediately");
        terminal.executeLine("set level 9");
        check((int) get(game, "targetTotalEntityWeight") == 3
                        && (int) get(game, "targetTotalCreatureWeight") == 2,
                "independent weight overrides survive level changes");
        terminal.executeLine("set -f level -1");
        check((int) get(game, "level") == 9 && terminal.lines().get(terminal.lines().size() - 1).contains("Usage: set level"),
                "force cannot bypass invalid level or omit target usage");
        terminal.executeLine("set essence 0");
        check((int) get(game, "incidentEssence") == 0, "zero essence is rejected");
        terminal.executeLine("set essence 5");
        check((int) get(game, "incidentEssence") == 5, "positive essence is accepted");
        terminal.executeLine("set ability.teleport.level 2");
        check(((AbilityManager) get(game, "abilityManager")).getLevel("teleport") == 0,
                "terminal waits for slot before constructing ability");
        terminal.executeLine("5");
        check(((AbilityManager) get(game, "abilityManager")).getLevel("teleport") == 0,
                "invalid slot cancels construction without changes");
        terminal.executeLine("set ability.teleport.level 2");
        terminal.executeLine("");
        check(((AbilityManager) get(game, "abilityManager")).getLevel("teleport") == 0,
                "empty slot submission cancels construction without changes");
        terminal.executeLine("set ability.teleport.level 2");
        terminal.executeLine("1");
        check("teleport".equals(((AbilityLoadout) get(game, "abilityLoadout")).getSlot(0)),
                "valid slot completes construction");
        terminal.executeLine("set ability.teleport.cooldown 4");
        check(((TeleportAnchor) get(game, "teleportAnchor")).getCooldownTimer() == 240,
                "ability cooldown setter updates live teleport clock");
        terminal.executeLine("set ability.teleport.cooldown 0");
        check(((TeleportAnchor) get(game, "teleportAnchor")).getCooldownTimer() == 0,
                "zero cooldown refreshes teleport immediately");
        terminal.executeLine("set slot.2 3");
        check(((AbilityLoadout) get(game, "abilityLoadout")).getSlot(1) == null,
                "locked ability cannot enter slot without force");
        terminal.executeLine("set -f slot.2 3");
        terminal.executeLine("no");
        check("nova".equals(((AbilityLoadout) get(game, "abilityLoadout")).getSlot(1))
                        && ((AbilityManager) get(game, "abilityManager")).getLevel("nova") == 0,
                "force no places a gray locked ability");
        terminal.executeLine("set -f slot.3 3");
        terminal.executeLine("yes");
        check("nova".equals(((AbilityLoadout) get(game, "abilityLoadout")).getSlot(2))
                        && ((AbilityManager) get(game, "abilityManager")).getLevel("nova") == 1,
                "force yes unlocks and moves the ability");
        terminal.executeLine("set ability.nova.cooldown 4");
        check(((AbilityManager) get(game, "abilityManager")).getCooldownRemainingFrames("nova") == 240,
                "Nova cooldown can be changed independently");
        terminal.executeLine("set ability.nova.cooldown 0");
        check(((AbilityManager) get(game, "abilityManager")).getCooldownRemainingFrames("nova") == 0,
                "Nova zero cooldown refreshes the ability");
        terminal.executeLine("set ability.void.cooldown 2");
        check((int) get(game, "voidCooldownTimer") == 120,
                "dedicated Void cooldown remains adjustable");
        terminal.executeLine("status --cosmic-weight --void-weight");
        check(terminal.lines().get(terminal.lines().size() - 1).contains("void-weight = 2"),
                "status accepts selected internal-variable tags");
        terminal.executeLine("ls --variables/status");
        check(terminal.lines().get(terminal.lines().size() - 1).contains("cosmic-spawn.cooldown.frames"),
                "variable listing includes internal status keys");
        terminal.executeLine("clear cosmic entities");
        check(collection(game, "cosmicEntities").isEmpty(), "clear removes cosmic entities");
        terminal.executeLine("spawn cosmic large");
        check((int) get(game, "currentTotalEntityWeight") == 3,
                "spawn cosmic large creates a weight-three entity");
        terminal.executeLine("spawn cosmic large 3");
        check(collection(game, "cosmicEntities").size() == 1,
                "spawn cap rejects an oversized request without partial changes");
        terminal.executeLine("spawn asteroid small 2");
        check(collection(game, "asteroids").size() == 2, "specific asteroid tier spawns");
        terminal.executeLine("set fuel 800");
        check((int) ((Ship) get(game, "ship")).getFuel() == 800, "developer fuel setter works");
        terminal.executeLine("set weapon.front.level 3");
        check(((WeaponSystem) get(game, "weaponSystem")).level(ConstructPanel.FRONT_NOZZLES) == 3,
                "developer weapon setter works");
        terminal.executeLine("set utility.shield.level 1");
        check(((AbilityManager) get(game, "abilityManager")).getLevel("shield") == 1,
                "terminal enables passive Shield utility");
        ((AbilityManager) get(game, "abilityManager")).setCooldownRemainingFrames("shield", 240);
        check(((AbilityManager) get(game, "abilityManager")).getCooldownRemainingFrames("shield") == 240,
                "Shield utility charge time can be set");
        ((AbilityManager) get(game, "abilityManager")).setCooldownRemainingFrames("shield", 0);
        check(((AbilityManager) get(game, "abilityManager")).isShieldReady(),
                "zero Shield cooldown finishes its charge");
        terminal.executeLine("set utility.shield.level 0");
        check(!((AbilityManager) get(game, "abilityManager")).isEnabled("shield"),
                "level zero disables Shield utility");
        terminal.executeLine("spawn void-hazard orb 2");
        check(terminal.lines().get(terminal.lines().size() - 1).contains("Unknown spawn target"),
                "removed Void hazards cannot be spawned");
        terminal.executeLine("set ability.drone.level 1");
        terminal.executeLine("2");
        terminal.executeLine("set ability.drone.cooldown 0");
        int droneCharges = ((AbilityManager) get(game, "abilityManager")).getDroneCharges();
        terminal.executeLine("spawn drone");
        check(((AbilityManager) get(game, "abilityManager")).getActiveDroneCount() == 1
                        && ((AbilityManager) get(game, "abilityManager")).getDroneCharges() == droneCharges,
                "developer drone spawn leaves charged ability resources unchanged");
        terminal.executeLine("spawn black-hole 2");
        check(collection(game, "blackHoles").size() == 3,
                "developer black holes include the warned level hazard within cap");
        terminal.executeLine("set ship.x 9999");
        check((int) ((Ship) get(game, "ship")).getX() != 9999,
                "ship coordinate outside the screen is rejected");
        ControlConfig controls = (ControlConfig) get(game, "controlConfig");
        controls.setDeveloperTerminalEnabled(true);
        press(game, KeyEvent.VK_T, 'T');
        check((boolean) get(game, "showingDeveloperTerminal"), "T opens enabled terminal");
        set(game, "levelTimer", 1000);
        tick(game);
        check((int) get(game, "levelTimer") == 1000, "terminal pauses simulation");
        press(game, KeyEvent.VK_ESCAPE, (char) 27);
        check(!(boolean) get(game, "showingDeveloperTerminal"), "Escape closes terminal");
        set(game, "showingConstruct", true);
        set(game, "pendingAbility", 0);
        press(game, KeyEvent.VK_T, 'T');
        press(game, KeyEvent.VK_ESCAPE, (char) 27);
        check((boolean) get(game, "showingConstruct") && (int) get(game, "pendingAbility") == 0,
                "opening terminal preserves an in-progress ability slot assignment");

        ((Ship) get(game, "ship")).getVoidEnergy().activate();
        ((Ship) get(game, "ship")).getVoidEnergy().setEnergyForDeveloper(0);
        invoke(game, "handleShoot");
        check(collection(game, "voidLasers").size() == 7,
                "Void active at zero energy fires every constructed front laser");
        StarField stars = (StarField) get(game, "starField");
        stars.setVoidMode(true);
        Method scene = AsteroidGame.class.getDeclaredMethod("changeScene", String.class);
        scene.setAccessible(true);
        scene.invoke(game, "menu");
        check(!(boolean) get(stars, "voidMode"), "leaving Void resets menu star colors");
    }

    private static void testDeveloperResetDefaults() throws Exception {
        AsteroidGame game = createGame(true);
        DeveloperTerminal terminal = (DeveloperTerminal) get(game, "developerTerminal");
        terminal.executeLine("set level 5");
        terminal.executeLine("set void-weight 10");
        terminal.executeLine("reset void-weight");
        check((int) get(game, "targetTotalCreatureWeight") == 3
                        && !(boolean) get(game, "voidWeightOverride"),
                "reset void weight restores the level-five budget and unfreezes progression");
        terminal.executeLine("set level 6");
        check((int) get(game, "targetTotalCreatureWeight") == 4,
                "later levels adjust reset void weight normally");
        terminal.executeLine("set cosmic-weight 10");
        terminal.executeLine("reset cosmic-weight");
        check((int) get(game, "targetTotalEntityWeight") == 4
                        && !(boolean) get(game, "cosmicWeightOverride"),
                "reset cosmic weight restores the level-six budget");
        terminal.executeLine("set weapon.front.level 3");
        terminal.executeLine("reset weapon.front.level");
        check(((WeaponSystem) get(game, "weaponSystem")).level(ConstructPanel.FRONT_NOZZLES) == 0,
                "reset weapon level removes its construction");
        terminal.executeLine("set essence 8");
        terminal.executeLine("reset essence");
        check((int) get(game, "incidentEssence") == 0,
                "reset essence restores the new-run zero balance");
        terminal.executeLine("set fuel 800");
        terminal.executeLine("reset fuel");
        check((int) ((Ship) get(game, "ship")).getFuel() == 500,
                "reset fuel restores the ship's initial tank");
        terminal.executeLine("set ship.x 900");
        terminal.executeLine("reset ship.x");
        check((int) ((Ship) get(game, "ship")).getX() == 600,
                "reset ship X moves it to screen center");
        terminal.executeLine("set ability.teleport.level 1");
        terminal.executeLine("1");
        terminal.executeLine("set ability.teleport.cooldown 4");
        terminal.executeLine("reset ability.teleport.cooldown");
        check(((TeleportAnchor) get(game, "teleportAnchor")).getCooldownTimer() == 0,
                "reset ability cooldown refreshes it");
        terminal.executeLine("reset slot.1");
        check(((AbilityLoadout) get(game, "abilityLoadout")).getSlot(0) == null,
                "reset slot restores the empty default");
        terminal.executeLine("reset ability.teleport.level");
        check(((AbilityManager) get(game, "abilityManager")).getLevel("teleport") == 0,
                "reset ability level returns it to locked state");
        terminal.executeLine("reset level");
        check((int) get(game, "level") == 0
                        && (int) get(game, "targetTotalEntityWeight") == 0
                        && (int) get(game, "targetTotalCreatureWeight") == 0,
                "reset level restores stage zero and its natural weights");
        terminal.executeLine("reset --help");
        check(terminal.lines().get(terminal.lines().size() - 1).contains("Examples:"),
                "reset help lists examples");
    }

    private static void testHeldShootingWithMovement() throws Exception {
        AsteroidGame game = createGame(true);
        press(game, KeyEvent.VK_A, 'A');
        check(collection(game, "bullets").size() == 1,
                "pressing shoot fires immediately");
        press(game, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
        press(game, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
        for (int frame = 0; frame < 20; frame++) tick(game);
        int whileTurning = collection(game, "bullets").size();
        check(whileTurning >= 3, "holding shoot continues to fire while thrusting and turning");
        game.keyReleased(new KeyEvent(game, KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(), 0, KeyEvent.VK_A, 'A'));
        for (int frame = 0; frame < 12; frame++) tick(game);
        check(collection(game, "bullets").size() == whileTurning,
                "releasing shoot stops new volleys");

        AsteroidGame voidGame = createGame(true);
        ((WeaponSystem) get(voidGame, "weaponSystem")).setLevelForDeveloper(
                ConstructPanel.FRONT_NOZZLES, 1);
        ((WeaponSystem) get(voidGame, "weaponSystem")).setLevelForDeveloper(
                ConstructPanel.REAR_NOZZLES, 1);
        ((Ship) get(voidGame, "ship")).getVoidEnergy().setEnergyForDeveloper(20);
        press(voidGame, KeyEvent.VK_A, 'A');
        check(collection(voidGame, "voidLasers").size() == 5,
                "front and rear nozzle upgrades also fire Void lasers");
    }

    private static void testMenuLifecycle() throws Exception {
        AsteroidGame game = createGame(false);
        game.dispatchEvent(new MouseEvent(game, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 600, 385, 1, false));
        check((boolean) get(game, "showingInstructions"),
                "Instructions has its own discoverable menu button");
        check(!((java.util.Map<?, ?>) get(get(game, "settingsPanel"), "categoryBounds"))
                        .containsKey("Instructions"),
                "Instructions is no longer a Settings category");
        press(game, KeyEvent.VK_ESCAPE, (char) 27);
        check(!(boolean) get(game, "showingInstructions"), "Escape closes Instructions");
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

    private static void testStorySkippingAndPauseKeys() throws Exception {
        AsteroidGame game = createGame(false);
        set(game, "showingStory", true);
        StoryTerminal story = (StoryTerminal) get(game, "storyTerminal");
        press(game, KeyEvent.VK_ENTER, '\n');
        check(!(boolean) get(story, "booting") && "menu".equals(get(game, "gameState")),
                "Enter skips story boot without starting a game");
        press(game, KeyEvent.VK_ENTER, '\n');
        check((int) get(story, "currentSegment") == 1
                        && ((List<?>) get(story, "displayedLines")).size() == 1,
                "Enter completes the current line and advances exactly once");
        set(story, "waitingForInput", true);
        press(game, KeyEvent.VK_ENTER, '\n');
        check(!(boolean) get(story, "waitingForInput")
                        && (int) get(story, "currentSegment") == 1,
                "Enter resumes at a section pause without skipping an extra line");
        press(game, KeyEvent.VK_ESCAPE, (char) 27);
        check(!(boolean) get(game, "showingStory"), "Escape exits story at any point");

        game = createGame(true);
        set(game, "gameState", "paused");
        press(game, KeyEvent.VK_ENTER, '\n');
        check("playing".equals(get(game, "gameState")), "Enter resumes a paused game");
        set(game, "gameState", "paused");
        press(game, KeyEvent.VK_ESCAPE, (char) 27);
        check("menu".equals(get(game, "gameState")), "Escape returns from pause to menu");
    }

    private static void testPendingChangesOnDeathAndNewGame() throws Exception {
        AsteroidGame game = createGame(true);
        ParticleSystem particles = (ParticleSystem)get(game, "particleSystem");
        particles.createShipExplosion(100, 100);
        check(!collection(particles, "particles").isEmpty(), "playing particles exist before death");
        stageChanges(game);
        invoke(game, "startDeathSequence");
        check(collection(particles, "particles").isEmpty(), "entering death clears playing particles");
        checkPendingEmpty(game);
        for (String name : new String[] {"asteroids", "bullets", "voidLasers", "blackHoles", "cosmicEntities", "voidCreatures"}) {
            check(collection(game, name).isEmpty(), "death clears " + name);
        }
        stageChanges(game);
        particles.createShipExplosion(100, 100);
        set(game, "deathCountdown", 1);
        tick(game);
        check("menu".equals(get(game, "gameState")), "death returns to menu");
        check(collection(particles, "particles").isEmpty(), "returning to menu clears death particles");
        particles.createShipExplosion(100, 100);
        game.startNewGame();
        check(collection(particles, "particles").isEmpty(), "new game clears menu particles");
        check((int)get(game, "incidentEssence") == 0, "new game starts without incident essence");
        checkPendingEmpty(game);
        check((int)get(game, "currentTotalCreatureWeight") == 0, "new run resets creature weight");
        check((int)get(game, "currentTotalEntityWeight") == 0, "new run resets entity total");
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

        game = createGame(true);
        collection(game, "asteroids").add(new Asteroid(Asteroid.Size.MEDIUM, 200, 200, 0, 0));
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        tick(game);
        check(collection(game, "asteroids").size() == 2, "two simultaneous bullets split an asteroid only once");

        game = createGame(true);
        CosmicEntity entity = new CosmicEntity(1200, 700, 1);
        set(entity, "x", 200.0);
        set(entity, "y", 200.0);
        set(entity, "currentHP", 100);
        collection(game, "cosmicEntities").add(entity);
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        tick(game);
        check((int)get(entity, "currentHP") == 75, "bullet damages cosmic entity once");

    }

    private static void testUpgradedProjectileCollisions() throws Exception {
        AsteroidGame game = createGame(true);
        collection(game, "asteroids").add(new Asteroid(Asteroid.Size.SMALL, 200, 200, 0, 0));
        collection(game, "asteroids").add(new Asteroid(Asteroid.Size.SMALL, 200, 200, 0, 0));
        Bullet piercing = new Bullet(200, 210, 0, true);
        collection(game, "bullets").add(piercing);
        tick(game);
        check(collection(game, "asteroids").isEmpty(),
                "one piercing bullet destroys two overlapping asteroids");
        check(collection(game, "bullets").contains(piercing),
                "piercing bullet stays alive after both collisions");

        game = createGame(true);
        ((WeaponSystem)get(game, "weaponSystem")).construct(ConstructPanel.BULLET_ENLARGEMENT);
        Asteroid first = new Asteroid(Asteroid.Size.SMALL, 200, 200, 0, 0);
        Asteroid second = new Asteroid(Asteroid.Size.SMALL, 260, 200, 0, 0);
        collection(game, "asteroids").add(first);
        collection(game, "asteroids").add(second);
        Bullet enlarged = new Bullet(200, 210, 0, 0, 1);
        collection(game, "bullets").add(enlarged);
        tick(game);
        check(!collection(game, "asteroids").contains(first), "enlarged bullet destroys its first target");
        check(collection(game, "asteroids").contains(second), "bullet does not track a second target");
        check(!collection(game, "bullets").contains(enlarged), "unpierced bullet ends at one hit");
    }

    private static void testCompanionAsteroidFuel() throws Exception {
        AsteroidGame droneGame = createGame(true);
        Ship droneShip = (Ship)get(droneGame, "ship");
        droneShip.setX(600);
        droneShip.setY(350);
        droneShip.setFuel(200);
        AbilityManager abilities = (AbilityManager)get(droneGame, "abilityManager");
        abilities.setLevel("drone", 1);
        check(abilities.spawnDroneForDeveloper(droneShip), "fuel test deploys a drone");
        Asteroid droneTarget = new Asteroid(Asteroid.Size.SMALL, 600, 190, 0, 0);
        collection(droneGame, "asteroids").add(droneTarget);
        for (int frame = 0; frame < 30; frame++) tick(droneGame);
        check(!collection(droneGame, "asteroids").contains(droneTarget)
                        && droneShip.getFuel() == 205,
                "drone asteroid kill grants five M-AM");

        AsteroidGame swarmGame = createGame(true);
        Ship swarmShip = (Ship)get(swarmGame, "ship");
        swarmShip.setX(600);
        swarmShip.setY(350);
        swarmShip.setFuel(200);
        ((UtilitySystem)get(swarmGame, "utilitySystem")).setLevel(UtilitySystem.SWARM, 1);
        Asteroid swarmTarget = new Asteroid(Asteroid.Size.SMALL, 600, 276, 0, 0);
        collection(swarmGame, "asteroids").add(swarmTarget);
        for (int frame = 0; frame < 90; frame++) tick(swarmGame);
        check(!collection(swarmGame, "asteroids").contains(swarmTarget)
                        && swarmShip.getFuel() == 205,
                "Swarm asteroid kill grants five M-AM");

        AsteroidGame fragmentGame = createGame(true);
        Ship fragmentShip = (Ship)get(fragmentGame, "ship");
        fragmentShip.setFuel(200);
        collection(fragmentGame, "asteroidsToAdd").add(
                new Asteroid(Asteroid.Size.SMALL, 400, 200, 0, 0));
        Method destroyFragments = AsteroidGame.class.getDeclaredMethod(
                "destroyPendingSwarmFragments", UtilitySystem.Blast.class);
        destroyFragments.setAccessible(true);
        destroyFragments.invoke(fragmentGame, new UtilitySystem.Blast(400, 200, 85));
        check(collection(fragmentGame, "asteroidsToAdd").isEmpty()
                        && fragmentShip.getFuel() == 205,
                "Swarm blast grants M-AM for newly split fragments");
    }

    private static void testPassiveShieldBurst() throws Exception {
        AsteroidGame game = createGame(true);
        AbilityManager abilities = (AbilityManager)get(game, "abilityManager");
        abilities.setLevel("shield", 1);
        for (int frame = 0; frame < 18 * 60; frame++) abilities.update();
        check(abilities.isShieldReady(), "purchased shield finishes charging");

        Ship ship = (Ship)get(game, "ship");
        double x = ship.getX(), y = ship.getY();
        Asteroid contact = new Asteroid(Asteroid.Size.SMALL, x + 40, y, 0, 0);
        Asteroid nearby = new Asteroid(Asteroid.Size.SMALL, x + 120, y, 0, 0);
        Asteroid distant = new Asteroid(Asteroid.Size.SMALL, x + 250, y, 0, 0);
        collection(game, "asteroids").add(contact);
        collection(game, "asteroids").add(nearby);
        collection(game, "asteroids").add(distant);
        VoidEnergy energy = ship.getVoidEnergy();
        energy.activate();
        set(energy, "energy", 90.0);
        tick(game);
        check(!collection(game, "asteroids").contains(contact)
                        && !collection(game, "asteroids").contains(nearby),
                "charged shield clears contact and nearby asteroids without splitting");
        check(collection(game, "asteroids").contains(distant),
                "level-one shield leaves objects outside its burst radius");
        check(energy.getEnergy() > 90 && energy.getEnergy() < 100,
                "contact before the void threshold does not clear accumulated energy");
        check(!abilities.isShieldReady(), "shield begins recharging after the burst");

        game = createGame(true);
        abilities = (AbilityManager)get(game, "abilityManager");
        abilities.setLevel("shield", 1);
        for (int frame = 0; frame < 18 * 60; frame++) abilities.update();
        energy = ((Ship)get(game, "ship")).getVoidEnergy();
        energy.activate();
        set(energy, "energy", 100.0);
        tick(game);
        check("playing".equals(get(game, "gameState")),
                "charged shield prevents death when void energy reaches one hundred");
        check(energy.getEnergy() == 0 && !energy.isActive(),
                "threshold burst clears void energy and exits void mode");
        check(abilities.isShieldActive() && !abilities.isShieldReady(),
                "void threshold consumes the ready shield and starts a burst");
    }

    private static void testTeleportCollisionProtection() throws Exception {
        AsteroidGame game = createGame(true);
        Ship ship = (Ship)get(game, "ship");
        Asteroid overlap = new Asteroid(Asteroid.Size.SMALL, ship.getX(), ship.getY(), 0, 0);
        collection(game, "asteroids").add(overlap);
        TeleportAnchor anchor = (TeleportAnchor)get(game, "teleportAnchor");
        check(anchor.beginTeleport(ship, List.of(overlap.getBoundingBox()), 1200, 700, 1),
                "teleport starts while a hazard overlaps the ship");
        double firstTargetX = anchor.getTargetX(), firstTargetY = anchor.getTargetY();
        Asteroid arrivingHazard = new Asteroid(Asteroid.Size.SMALL, firstTargetX, firstTargetY, 0, 0);
        collection(game, "asteroids").add(arrivingHazard);
        for (int frame = 0; frame < 29; frame++) tick(game);
        check(anchor.isPending() && "playing".equals(get(game, "gameState")),
                "ship survives an overlapping asteroid during the teleport delay");
        check(Math.hypot(anchor.getTargetX() - firstTargetX, anchor.getTargetY() - firstTargetY) > 40,
                "teleport retargets when an asteroid arrives at its marked destination");
        tick(game);
        check(!anchor.isPending() && "playing".equals(get(game, "gameState")),
                "teleport finishes at a safe location without a death transition");
        check(Math.hypot(ship.getX() - 600, ship.getY() - 350) > 100,
                "teleport destination moves the ship away from the overlapping hazard");
        check(Math.hypot(ship.getX() - firstTargetX, ship.getY() - firstTargetY) > 40,
                "teleport avoids the hazard that occupied its original destination");
        collection(game, "asteroids").add(new Asteroid(Asteroid.Size.SMALL,
                ship.getX(), ship.getY(), 0, 0));
        tick(game);
        check("playing".equals(get(game, "gameState")),
                "short arrival protection prevents immediate death at the landing spot");
    }

    private static void testIncidentEssenceRewards() throws Exception {
        AsteroidGame game = createGame(true);
        CosmicEntity entity = new CosmicEntity(1200, 700, 5, 1);
        set(entity, "x", 200.0);
        set(entity, "y", 200.0);
        set(entity, "currentHP", 25);
        collection(game, "cosmicEntities").add(entity);
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        tick(game);
        check(collection(game, "cosmicEntities").isEmpty(), "one shot destroys a weakened small entity");
        int entityReward = (int)get(game, "incidentEssence");
        check(entityReward >= 1 && entityReward <= 2, "small entity grants one or two incident essence");
        tick(game);
        check((int)get(game, "incidentEssence") == entityReward, "removed entity cannot award twice");

        game = createGame(true);
        VoidCreature creature = VoidCreature.createSpecter(1200, 700, 5);
        set(creature, "x", 200.0);
        set(creature, "y", 200.0);
        set(creature, "currentHP", 25);
        int maxReward = creature.getWeight() == 1 ? 2 : creature.getWeight() == 2 ? 3 : 5;
        collection(game, "voidCreatures").add(creature);
        ((Ship)get(game, "ship")).getVoidEnergy().activate();
        collection(game, "bullets").add(new Bullet(200, 210, 0));
        tick(game);
        check(collection(game, "voidCreatures").isEmpty(), "bullet destroys a weakened void creature");
        int creatureReward = (int)get(game, "incidentEssence");
        check(creatureReward >= 1 && creatureReward <= maxReward,
                "void creature reward fits its size class");
        game.startNewGame();
        check((int)get(game, "incidentEssence") == 0, "new run resets earned incident essence");
    }

    private static void testConstructPurchasesAndSlots() throws Exception {
        AsteroidGame game = createGame(true);
        set(game, "incidentEssence", 20);
        ConstructPanel panel = (ConstructPanel)get(game, "constructPanel");
        WeaponSystem weapons = (WeaponSystem)get(game, "weaponSystem");
        panel.handleKey(KeyEvent.VK_2, constructModel(game));
        applyConstructAction(game, panel.handleKey(KeyEvent.VK_1, constructModel(game)));
        check(weapons.level(ConstructPanel.FRONT_NOZZLES) == 1, "front nozzle purchase builds one pair");
        applyConstructAction(game, panel.handleKey(KeyEvent.VK_4, constructModel(game)));
        check(weapons.level(ConstructPanel.BULLET_ENLARGEMENT) == 1, "bullet enlargement purchase starts first size level");
        applyConstructAction(game, panel.handleKey(KeyEvent.VK_3, constructModel(game)));
        check(weapons.level(ConstructPanel.PIERCING) == 1, "piercing purchase is available");
        applyConstructAction(game, panel.handleKey(KeyEvent.VK_4, constructModel(game)));
        check(weapons.level(ConstructPanel.BULLET_ENLARGEMENT) == 2, "bullet enlargement upgrades a second time");
        check((int)get(game, "incidentEssence") == 0, "each of four weapon purchases costs five essence");
        game.startNewGame();
        check(((WeaponSystem)get(game, "weaponSystem")).level(ConstructPanel.BULLET_ENLARGEMENT) == 0,
                "a new game resets weapon constructions");

        game = createGame(true);
        set(game, "incidentEssence", 20);
        panel = (ConstructPanel)get(game, "constructPanel");
        panel.handleKey(KeyEvent.VK_3, constructModel(game));
        applyConstructAction(game, panel.handleKey(KeyEvent.VK_1, constructModel(game)));
        AbilityManager abilities = (AbilityManager)get(game, "abilityManager");
        check(abilities.getLevel("teleport") == 1, "constructing teleport unlocks level one");
        check((int)get(game, "incidentEssence") == 10, "ability construction costs ten essence");
        check((int)get(game, "pendingAbility") == ConstructPanel.TELEPORT,
                "new ability awaits an available slot");
        applyConstructAction(game, panel.handleKey(KeyEvent.VK_1, constructModel(game)));
        AbilityLoadout loadout = (AbilityLoadout)get(game, "abilityLoadout");
        check("teleport".equals(loadout.getSlot(0)), "first slot shortcut assigns teleport");
        check(loadout.isSlotAvailable(1), "Void uses a dedicated fifth slot");
        check((int)get(game, "pendingAbility") == -1, "valid assignment clears the pending state");
        applyConstructAction(game, panel.handleKey(KeyEvent.VK_1, constructModel(game)));
        check(abilities.getLevel("teleport") == 2, "later construction upgrades the slotted ability");
        check((int)get(game, "incidentEssence") == 0, "second ability construction costs ten essence");
        game.startNewGame();
        check(((AbilityManager)get(game, "abilityManager")).getLevel("teleport") == 0,
                "new game resets constructed ability levels");
        check(((AbilityLoadout)get(game, "abilityLoadout")).getSlot(0) == null,
                "new game clears purchased ability slots");
    }

    private static void testConfigurableConstructionKey() throws Exception {
        AsteroidGame game = createGame(true);
        set(game, "incidentEssence", 10);
        ControlConfig controls = (ControlConfig) get(game, "controlConfig");
        check(controls.setControl("constructTeleport", KeyEvent.VK_Y),
                "Teleport construction shortcut can be changed in the game config");
        press(game, KeyEvent.VK_C, 'C');
        check((boolean) get(game, "showingConstruct"), "C opens the construction panel");
        press(game, KeyEvent.VK_3, '3');
        press(game, KeyEvent.VK_1, '1');
        AbilityManager abilities = (AbilityManager) get(game, "abilityManager");
        check(abilities.getLevel("teleport") == 0,
                "The old construction shortcut no longer purchases Teleport");
        press(game, KeyEvent.VK_Y, 'Y');
        check(abilities.getLevel("teleport") == 1,
                "The configured construction shortcut purchases Teleport");
    }

    private static void testConstructionControlShortcuts() throws Exception {
        AsteroidGame game = createGame(true);
        ConstructPanel panel = (ConstructPanel) get(game, "constructPanel");
        pressControl(game, KeyEvent.VK_A);
        check((boolean) get(game, "showingConstruct")
                        && "WEAPONS".equals(get(panel, "view").toString()),
                "Ctrl+A opens the armory without essence");
        check(collection(game, "bullets").isEmpty() && !(boolean) get(game, "shooting"),
                "Ctrl+A does not fire the shoot key");
        pressControl(game, KeyEvent.VK_Q);
        check("ABILITIES".equals(get(panel, "view").toString()),
                "Ctrl+empty slot opens ability construction without essence");

        game = createGame(true);
        panel = (ConstructPanel) get(game, "constructPanel");
        pressControl(game, KeyEvent.VK_Q);
        check((boolean) get(game, "showingConstruct")
                        && "ABILITIES".equals(get(panel, "view").toString()),
                "Ctrl+empty slot opens abilities directly from play at zero essence");

        game = createGame(true);
        AbilityManager abilities = (AbilityManager) get(game, "abilityManager");
        AbilityLoadout loadout = (AbilityLoadout) get(game, "abilityLoadout");
        ControlConfig controls = (ControlConfig) get(game, "controlConfig");
        abilities.setLevel("teleport", 1);
        check(loadout.setSlot(2, "teleport"), "Teleport can occupy the third slot");
        check(controls.setControl("ability3", KeyEvent.VK_Y),
                "Equipped slot key can be rebound");
        pressControl(game, KeyEvent.VK_E);
        check(abilities.getLevel("teleport") == 1,
                "Old slot key does not upgrade after rebind");
        pressControl(game, KeyEvent.VK_Y);
        check(abilities.getLevel("teleport") == 1
                        && (int) get(game, "incidentEssence") == 0,
                "Insufficient essence leaves the equipped ability unchanged");
        check(((String) get(game, "shortcutMessage")).contains("Insufficient Incident Essence")
                        && (int) get(game, "shortcutMessageTicks") > 0,
                "Insufficient essence shows a transient bottom message");
        check(!(boolean) get(game, "showingConstruct"),
                "Failed shortcut does not open construction");
        set(game, "incidentEssence", 20);
        pressControl(game, KeyEvent.VK_Y);
        check(abilities.getLevel("teleport") == 2
                        && (int) get(game, "incidentEssence") == 10,
                "Ctrl+rebound slot purchases exactly one level for ten essence");
        check((int) get((TeleportAnchor) get(game, "teleportAnchor"), "maxCooldown") == 5 * 60,
                "Shortcut upgrade updates Teleport's cooldown");
        check(!(boolean) get(game, "showingConstruct"),
                "Funded shortcut upgrades without opening the panel");
        press(game, KeyEvent.VK_Y, 'Y');
        check(abilities.getLevel("teleport") == 2,
                "Unmodified slot key does not purchase an upgrade");
        pressControl(game, KeyEvent.VK_Y);
        check(abilities.getLevel("teleport") == 3
                        && (int) get(game, "incidentEssence") == 0,
                "A second shortcut purchase reaches the level cap");
        pressControl(game, KeyEvent.VK_Y);
        check(abilities.getLevel("teleport") == 3
                        && (int) get(game, "incidentEssence") == 0,
                "Level cap prevents further shortcut purchases");
    }

    private static void pressControl(AsteroidGame game, int keyCode) {
        game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), KeyEvent.CTRL_DOWN_MASK, keyCode,
                KeyEvent.CHAR_UNDEFINED));
    }

    private static void press(AsteroidGame game, int keyCode, char keyChar) {
        game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, keyCode, keyChar));
    }

    private static void testBlackHoleChargeAndTerminalUnlock() throws Exception {
        AsteroidGame game = createGame(true);
        check(!game.getFocusTraversalKeysEnabled(), "Tab reaches the charge cancellation control");
        DeveloperTerminal terminal = (DeveloperTerminal) get(game, "developerTerminal");
        terminal.executeLine("set ability.blackhole.level 1");
        check(terminal.lines().get(terminal.lines().size() - 1).contains("Set black holes absorbed to 1"),
                "terminal asks before granting the first absorbed hole");
        terminal.executeLine("no");
        check((int) get(game, "absorbedBlackHoles") == 0,
                "declining the developer prompt keeps absorption locked");
        terminal.executeLine("set ability.blackhole.level 1");
        terminal.executeLine("yes");
        terminal.executeLine("1");
        AbilityManager abilities = (AbilityManager) get(game, "abilityManager");
        check(abilities.getLevel("blackhole") == 1 && (int) get(game, "absorbedBlackHoles") == 1,
                "accepting the developer prompt constructs Black Hole");
        int slotKey = ((ControlConfig) get(game, "controlConfig")).getKey("ability1");
        press(game, slotKey, 'Q');
        check((boolean) get(game, "chargingBlackHole"), "holding the slot key starts charge");
        set(game, "blackHoleChargeFrames", 20);
        game.keyReleased(new KeyEvent(game, KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(), 0, slotKey, 'Q'));
        check(collection(game, "blackHoles").size() == 1
                        && abilities.getCooldownRemainingFrames("blackhole") == 30 * 60,
                "key release launches the hole and starts level-one cooldown");
        BlackHole launched = (BlackHole) collection(game, "blackHoles").iterator().next();
        check(launched.isPlayerOwned() && launched.isTraveling(),
                "launched hole is a moving player-owned hole");
        abilities.setCooldownRemainingFrames("blackhole", 0);
        press(game, slotKey, 'Q');
        press(game, KeyEvent.VK_TAB, '\t');
        check(!(boolean) get(game, "chargingBlackHole"), "Tab cancels an active charge");
        press(game, slotKey, 'Q');
        check(!(boolean) get(game, "chargingBlackHole"),
                "a canceled charge cannot restart until the slot key is released");
        game.keyReleased(new KeyEvent(game, KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(), 0, slotKey, 'Q'));
        check(collection(game, "blackHoles").size() == 1,
                "releasing a canceled charge fires nothing");
        press(game, slotKey, 'Q');
        check((boolean) get(game, "chargingBlackHole"),
                "charge can begin again after a fresh key press");

        AsteroidGame absorptionGame = createGame(true);
        Ship ship = (Ship) get(absorptionGame, "ship");
        UtilitySystem utilities = (UtilitySystem) get(absorptionGame, "utilitySystem");
        utilities.setLevel(UtilitySystem.MODULATOR, 3);
        utilities.assignSlot(0, "modulator");
        ship.getVoidEnergy().setModulatorLevel(3);
        BlackHole small = new BlackHole(ship.getX() + 30, ship.getY(), 30);
        for (int i = 0; i < 180; i++) small.update(1200, 700);
        collection(absorptionGame, "blackHoles").add(small);
        tick(absorptionGame);
        check((int) get(absorptionGame, "absorbedBlackHoles") == 1
                        && ship.isInfiniteFuel(),
                "a nearby marked hole is absorbed and grants infinite M-AM");
    }

    private static void testPopulationBookkeeping() throws Exception {
        AsteroidGame game = createGame(true);
        set(game, "level", 2);
        invoke(game, "configureLevelHazards");
        invoke(game, "trySpawnCreatures");
        invoke(game, "trySpawnEntities");
        check(collection(game, "cosmicEntities").isEmpty(), "cosmic entities wait until level three");
        check(collection(game, "voidCreatures").isEmpty(), "void creatures wait until level three");

        for (int level = 3; level <= 15; level++) {
            collection(game, "cosmicEntities").clear();
            collection(game, "voidCreatures").clear();
            set(game, "level", level);
            invoke(game, "configureLevelHazards");
            int expectedWeight = Math.min(10, level - 2);
            check((int)get(game, "targetTotalEntityWeight") == expectedWeight,
                    "cosmic level budget begins at one and caps at ten");
            check((int)get(game, "targetTotalCreatureWeight") == expectedWeight,
                    "void creature level budget matches cosmic budget");
            for (int attempt = 0; attempt < 12; attempt++) {
                set(game, "spawnCreaturesCooldown", 0);
                set(game, "spawnEntitiesCooldown", 0);
                invoke(game, "trySpawnCreatures");
                invoke(game, "trySpawnEntities");
            }
            int creatures = collection(game, "voidCreatures").stream().mapToInt(value -> ((VoidCreature)value).getWeight()).sum();
            int entities = collection(game, "cosmicEntities").stream().mapToInt(value -> ((CosmicEntity)value).getWeight()).sum();
            check(creatures == expectedWeight, "void spawning fills available weight exactly");
            check(entities == expectedWeight, "cosmic spawning fills available weight exactly");
            check(creatures == (int)get(game, "currentTotalCreatureWeight"), "creature total matches live population");
            check(entities == (int)get(game, "currentTotalEntityWeight"), "entity total matches live population");
            if (level < 8) check(collection(game, "blackHoles").isEmpty(), "black holes wait until level eight");
            else check(collection(game, "blackHoles").size() >= 1
                            && collection(game, "blackHoles").size() <= 3,
                    "late levels add warned holes up to the native cap");
            check(creatures <= (int)get(game, "targetTotalCreatureWeight"), "creature weight never exceeds its budget");
            check(entities <= (int)get(game, "targetTotalEntityWeight"), "entity weight never exceeds its budget");
            if (level == 3) {
                check(collection(game, "voidCreatures").size() == 1
                                && ((VoidCreature)collection(game, "voidCreatures").iterator().next()).getWeight() == 1,
                        "first level's only void creature is small");
                check(collection(game, "cosmicEntities").size() == 1
                                && ((CosmicEntity)collection(game, "cosmicEntities").iterator().next()).getWeight() == 1,
                        "first level's only cosmic entity is small");
            }
        }

        for (Object entity : collection(game, "cosmicEntities")) {
            collection(game, "cosmicEntitiesToRemove").add(entity);
            collection(game, "cosmicEntitiesToRemove").add(entity);
        }
        for (Object creature : collection(game, "voidCreatures")) collection(game, "creaturesToRemove").add(creature);
        invoke(game, "applyPendingChanges");
        check((int)get(game, "currentTotalEntityWeight") == 0, "duplicate removal cannot make entity total negative");
        check((int)get(game, "currentTotalCreatureWeight") == 0, "removal reconciles creature total");

        set(game, "level", 4);
        collection(game, "cosmicEntities").add(new CosmicEntity(1200, 700, 4, 1));
        collection(game, "voidCreatures").add(VoidCreature.createSpecter(1200, 700, 4, 1));
        invoke(game, "configureLevelHazards");
        set(game, "spawnCreaturesCooldown", 0);
        set(game, "spawnEntitiesCooldown", 0);
        invoke(game, "trySpawnCreatures");
        invoke(game, "trySpawnEntities");
        check(collection(game, "cosmicEntities").size() == 2
                        && collection(game, "cosmicEntities").stream().allMatch(entity -> ((CosmicEntity)entity).getWeight() == 1),
                "one remaining cosmic weight forces a small spawn");
        check(collection(game, "voidCreatures").size() == 2
                        && collection(game, "voidCreatures").stream().allMatch(creature -> ((VoidCreature)creature).getWeight() == 1),
                "one remaining void weight forces a small spawn");
    }

    private static void testAsteroidPopulationCap() throws Exception {
        AsteroidGame game = createGame(true);
        set(game, "levelStarting", true);
        set(game, "levelStartCountdown", 1);
        tick(game);
        check(collection(game, "asteroids").size() == 2, "first level starts with two asteroids");

        game = createGame(true);
        for (int i = 0; i < 24; i++) {
            collection(game, "asteroids").add(new Asteroid(Asteroid.Size.SMALL, 300, 300, 0, 0));
        }
        collection(game, "asteroidsToAdd").add(new Asteroid(Asteroid.Size.SMALL, 300, 300, 0, 0));
        invoke(game, "applyPendingChanges");
        check(collection(game, "asteroids").size() == 24, "split fragments respect the asteroid cap");
    }

    private static void testBlackHoleConsumption() throws Exception {
        AsteroidGame game = createGame(true);
        BlackHole hole = new BlackHole(400, 300, 80);
        collection(game, "blackHoles").add(hole);
        Asteroid consumed = new Asteroid(Asteroid.Size.LARGE, 400, 300, 0, 0);
        Asteroid distant = new Asteroid(Asteroid.Size.SMALL, 800, 500, 0, 0);
        collection(game, "asteroids").add(consumed);
        collection(game, "asteroids").add(distant);
        check(hole.isWarningActive(), "black hole warns before becoming dangerous");
        check(!hole.isAsteroidConsumed(consumed.getBoundingBox()), "warning does not consume asteroids");
        for (int frame = 0; frame < 179; frame++) tick(game);
        check(collection(game, "asteroids").contains(consumed), "asteroid survives the warning interval");
        CosmicEntity entity = new CosmicEntity(1200, 700, 8, CosmicEntity.SizeTier.SMALL);
        VoidCreature creature = VoidCreature.createTier(1200, 700, VoidCreature.SizeTier.SMALL);
        set(entity, "x", 400.0);
        set(entity, "y", 300.0);
        set(creature, "x", 400.0);
        set(creature, "y", 300.0);
        collection(game, "cosmicEntities").add(entity);
        collection(game, "voidCreatures").add(creature);
        Bullet bullet = new Bullet(400, 300, 0);
        VoidLaser laser = new VoidLaser(400, 300, 0, 1200, 700);
        collection(game, "bullets").add(bullet);
        collection(game, "voidLasers").add(laser);
        AbilityManager abilities = (AbilityManager) get(game, "abilityManager");
        abilities.setLevel("drone", 1);
        check(abilities.spawnDroneForDeveloper((Ship) get(game, "ship")), "developer drone deployed");
        DroneSwarm.Drone drone = abilities.getDrones().get(0);
        set(drone, "x", 400.0);
        set(drone, "y", 300.0);
        tick(game);
        check(!hole.isWarningActive(), "black hole activates after three seconds");
        check(!collection(game, "asteroids").contains(consumed), "event horizon destroys intersecting asteroid");
        check(collection(game, "asteroids").contains(distant), "distant asteroid remains intact");
        check(collection(game, "asteroidsToAdd").isEmpty(), "black hole does not split asteroids");
        check(!collection(game, "cosmicEntities").contains(entity), "black hole consumes cosmic entities");
        check(!collection(game, "voidCreatures").contains(creature), "black hole consumes void creatures");
        check(!collection(game, "bullets").contains(bullet), "black hole consumes bullets");
        check(collection(game, "voidLasers").contains(laser) && laser.isSpent(),
                "black hole clips and spends the laser while its impact remains visible");
        check(abilities.getActiveDroneCount() == 0, "black hole consumes drones");
        check((int) get(game, "incidentEssence") == 0,
                "black hole consumption gives no player essence");
        ParticleSystem particles = (ParticleSystem)get(game, "particleSystem");
        check(!collection(particles, "particles").isEmpty(), "consumed asteroid plays its destruction animation");
    }

    private static void testOwnedBlackHoleLaser() throws Exception {
        AsteroidGame game = createGame(true);
        BlackHole owned = BlackHole.fired(400, 300, 44, 0, 0);
        owned.update();
        collection(game, "blackHoles").add(owned);
        VoidLaser laser = new VoidLaser(300, 300, Math.PI / 2, 1200, 700);
        Method clip = AsteroidGame.class.getDeclaredMethod("clipLaserToBlackHole", VoidLaser.class);
        clip.setAccessible(true);
        check((boolean) clip.invoke(game, laser) && laser.isSpent(),
                "a Void laser still ends at a player-owned hole");
        check(owned.getSize() == 44,
                "a Void laser impact does not shrink a player-owned purple hole");
    }

    private static void testResumedHazards() throws Exception {
        AsteroidGame original = createGame(true);
        set(original, "level", 8);
        set(original, "incidentEssence", 31);
        set(original, "absorbedBlackHoles", 2);
        ((Ship) get(original, "ship")).setInfiniteFuel(true);
        UtilitySystem utilities = (UtilitySystem) get(original, "utilitySystem");
        utilities.setLevel(UtilitySystem.MODULATOR, 3);
        check(utilities.assignSlot(0, "modulator"), "modulator occupies utility slot one");
        ((AbilityManager) get(original, "abilityManager")).setLevel("blackhole", 2);
        check(((AbilityLoadout) get(original, "abilityLoadout")).setSlot(1, "blackhole"),
                "Black Hole occupies ability slot two");
        ((WeaponSystem) get(original, "weaponSystem")).construct(ConstructPanel.FRONT_NOZZLES);
        original.saveGame();

        AsteroidGame resumed = createGame(false);
        resumed.resumeGame();
        check((int)get(resumed, "level") == 8, "resume restores the saved stage");
        check((int) get(resumed, "incidentEssence") == 31
                        && (int) get(resumed, "absorbedBlackHoles") == 2
                        && ((Ship) get(resumed, "ship")).isInfiniteFuel(),
                "resume restores essence, absorption, and infinite M-AM");
        check(((UtilitySystem) get(resumed, "utilitySystem")).level(UtilitySystem.MODULATOR) == 3
                        && "modulator".equals(((UtilitySystem) get(resumed, "utilitySystem")).getSlot(0))
                        && ((Ship) get(resumed, "ship")).getVoidEnergy().getModulatorLevel() == 3,
                "resume restores the equipped utility and its effect");
        check(((AbilityManager) get(resumed, "abilityManager")).getLevel("blackhole") == 2
                        && "blackhole".equals(((AbilityLoadout) get(resumed, "abilityLoadout")).getSlot(1))
                        && ((WeaponSystem) get(resumed, "weaponSystem")).level(ConstructPanel.FRONT_NOZZLES) == 1,
                "resume restores ability and weapon constructions");
        check((int)get(resumed, "targetTotalEntityWeight") == 6,
                "resume restores the level-eight cosmic entity budget");
        check((int)get(resumed, "targetTotalCreatureWeight") == 6,
                "resume restores the level-eight void creature budget");
        check(collection(resumed, "blackHoles").size() == 1,
                "resumed late stage gets its black hole warning");
        check(((BlackHole)collection(resumed, "blackHoles").iterator().next()).isWarningActive(),
                "resumed black hole starts with a warning");
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
        collection(game, "bulletsToRemove").add(new Bullet(200, 200, 0));
        collection(game, "lasersToRemove").add(new VoidLaser(200, 200, 0, 1200, 700));
        collection(game, "creaturesToRemove").add(VoidCreature.createSpecter(1200, 700, 1));
        collection(game, "cosmicEntitiesToRemove").add(new CosmicEntity(1200, 700, 1));
    }

    private static void checkPendingEmpty(AsteroidGame game) throws Exception {
        for (String name : new String[] {"asteroidsToAdd", "asteroidsToRemove", "lasersToRemove", "bulletsToRemove", "blackHolesToRemove", "cosmicEntitiesToRemove", "creaturesToRemove"}) {
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

    private static ConstructPanel.Model constructModel(AsteroidGame game) throws Exception {
        Method method = AsteroidGame.class.getDeclaredMethod("constructModel");
        method.setAccessible(true);
        return (ConstructPanel.Model)method.invoke(game);
    }

    private static void applyConstructAction(AsteroidGame game, ConstructPanel.Action action) throws Exception {
        Method method = AsteroidGame.class.getDeclaredMethod("applyConstructAction", ConstructPanel.Action.class);
        method.setAccessible(true);
        method.invoke(game, action);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
