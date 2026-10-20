import java.awt.Rectangle;
import java.util.List;

public class AbilityConstructionRegressionTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void tick(AbilityManager manager, int frames) {
        for (int i = 0; i < frames; i++) manager.update();
    }

    public static void main(String[] args) {
        AbilityLoadout loadout = new AbilityLoadout();
        check(loadout.isSlotAvailable(0) && loadout.isSlotAvailable(3),
                "all constructed slots start empty");
        check(!loadout.setSlot(2, "void"), "dedicated Void cannot occupy a constructed slot");
        check(!loadout.setSlot(1, "fade"), "removed Fade cannot be assigned");
        check(loadout.setSlot(1, "teleport"), "new ability can occupy a free slot");
        check(!loadout.setSlot(1, "shield"), "construction cannot replace another ability");
        loadout.resetForNewRun();
        check(loadout.isSlotAvailable(0) && loadout.isSlotAvailable(1),
                "new run clears constructed ability slots");

        AbilityManager abilities = new AbilityManager();
        abilities.setLevel("shield", 1);
        tick(abilities, 18 * 60 - 1);
        check(!abilities.isShieldReady(), "shield is still charging");
        tick(abilities, 1);
        check(abilities.isShieldReady(), "shield becomes ready");
        check(abilities.triggerShield(), "ready shield bursts");
        check(!abilities.isShieldReady(), "burst restarts shield charging");
        abilities.setLevel("shield", 3);
        tick(abilities, 7 * 60);
        check(abilities.isShieldReady(), "level 3 charges in seven seconds");
        check(abilities.getShieldBurstRadius(1200, 700)
                        >= Math.hypot(1200, 700),
                "level 3 burst covers the screen");

        Ship ship = new Ship();
        TeleportAnchor teleport = new TeleportAnchor();
        ship.setX(100);
        ship.setY(350);
        check(teleport.beginTeleport(ship, List.of(new Rectangle(0, 0, 600, 700)),
                1200, 700, 1), "teleport begins");
        check(teleport.getTargetX() > 800, "teleport chooses the clear side");
        check(teleport.getTargetX() <= 1110 && teleport.getTargetY() >= 90
                        && teleport.getTargetY() <= 610,
                "teleport keeps reaction room from screen edges");
        for (int i = 0; i < 29; i++) {
            check(!teleport.update(ship), "teleport waits for marker");
        }
        check(teleport.isPending() && ship.getX() == 100, "ship waits and is protected");
        check(teleport.update(ship), "teleport completes on delay frame");
        check(!teleport.isPending() && ship.getX() > 800, "ship moved to target");
        check(teleport.getMaxCooldown() == 10 * 60, "level 1 teleport cooldown");
        teleport.reset();
        check(teleport.beginTeleport(ship, List.of(), 1200, 700, 3),
                "level 3 teleport begins");
        for (int i = 0; i < 30; i++) teleport.update(ship);
        check(teleport.getMaxCooldown() == 2 * 60, "level 3 teleport cooldown");

        abilities.setLevel("drone", 1);
        tick(abilities, 12 * 60);
        check(abilities.getDroneCharges() == 1, "drone stores a charge");
        check(abilities.deployDrone(ship), "charge deploys a drone");
        check(abilities.getActiveDroneCount() == 1 && abilities.getDroneCharges() == 0,
                "deployment consumes one charge");
        tick(abilities, 12 * 60);
        check(abilities.deployDrone(ship), "deployment replaces oldest at capacity");
        check(abilities.getActiveDroneCount() == 1
                        && abilities.drainDroneDestructions().size() == 1,
                "old drone is destroyed on replacement");
        abilities.setLevel("drone", 3);
        tick(abilities, 12 * 60);
        check(abilities.deployDrone(ship), "second drone deploys");
        tick(abilities, 12 * 60);
        check(abilities.deployDrone(ship), "third drone deploys");
        check(abilities.getActiveDroneCount() == 3, "three drones at level 3");
        abilities.updateDrones(ship, List.of(
                new DroneSwarm.Target(ship.getX() + 90, ship.getY(), 8)),
                1200, 700);
        check(!abilities.drainDroneShots().isEmpty(), "drones fire at a nearby object");
        tick(abilities, 3 * 12 * 60);
        check(abilities.getDroneCharges() == 3, "stored drone charges reach level cap");
        tick(abilities, 12 * 60);
        check(abilities.getDroneCharges() == 3, "charging stops at the cap");
        DroneSwarm.Drone first = abilities.getDrones().get(0);
        abilities.updateDrones(ship, List.of(
                new DroneSwarm.Target(first.getX(), first.getY(), 8)),
                1200, 700);
        check(abilities.getActiveDroneCount() < 3
                        && !abilities.drainDroneDestructions().isEmpty(),
                "touching an object destroys a drone");

        AbilityManager pursuit = new AbilityManager();
        pursuit.setLevel("drone", 3);
        tick(pursuit, 12 * 60);
        check(pursuit.deployDrone(ship), "pursuit drone deploys");
        List<DroneSwarm.Target> distantTarget = List.of(
                new DroneSwarm.Target(ship.getX() + 300, ship.getY(), 12));
        boolean fired = false;
        for (int i = 0; i < 180; i++) {
            pursuit.updateDrones(ship, distantTarget, 1200, 700);
            if (!pursuit.drainDroneShots().isEmpty()) fired = true;
        }
        check(fired, "drone approaches and fires at a distant target");

        System.out.println("Ability construction regression checks passed: " + checks);
    }
}
