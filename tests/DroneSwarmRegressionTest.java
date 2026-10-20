import java.util.List;

public final class DroneSwarmRegressionTest {
    private static int assertions;

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void charge(DroneSwarm swarm, int count) {
        for (int frame = 0; frame < count * 12 * 60; frame++) swarm.updateCharge();
    }

    private static void reusesVacantStation() {
        Ship ship = new Ship();
        ship.setX(600);
        ship.setY(350);
        DroneSwarm swarm = new DroneSwarm();
        swarm.setLevel(3);
        charge(swarm, 3);
        for (int i = 0; i < 3; i++) check(swarm.deploy(ship), "initial drones deploy");

        DroneSwarm.Drone first = swarm.getDrones().get(0);
        check(first.getStation() == 0, "first drone uses the tip station");
        swarm.destroyDrone(first);
        swarm.drainDestructions();
        charge(swarm, 1);
        check(swarm.deploy(ship), "replacement drone deploys");

        long tipCount = swarm.getDrones().stream().filter(drone -> drone.getStation() == 0).count();
        long rightCount = swarm.getDrones().stream().filter(drone -> drone.getStation() == 1).count();
        long leftCount = swarm.getDrones().stream().filter(drone -> drone.getStation() == 2).count();
        check(tipCount == 1 && rightCount == 1 && leftCount == 1,
                "vacant tip station is reused without duplicating a side station");
    }

    private static void collisionPreventsShot() {
        Ship ship = new Ship();
        ship.setX(100);
        ship.setY(100);
        DroneSwarm swarm = new DroneSwarm();
        swarm.setLevel(1);
        charge(swarm, 1);
        check(swarm.deploy(ship), "drone deploys before collision");

        ship.setY(60);
        swarm.update(ship, List.of(new DroneSwarm.Target(100, 160, 10)), 1200, 700);
        check(swarm.getActiveCount() == 0, "drone touching the ship is destroyed");
        check(swarm.drainShots().isEmpty(), "destroyed drone cannot shoot on its collision frame");
        check(swarm.drainDestructions().size() == 1, "collision emits one destruction");
    }

    private static void avoidsBlackHoleTarget() {
        Ship ship = new Ship();
        ship.setX(600);
        ship.setY(350);
        DroneSwarm swarm = new DroneSwarm();
        swarm.setLevel(1);
        charge(swarm, 1);
        check(swarm.deploy(ship), "drone deploys for hole avoidance test");
        BlackHole hole = new BlackHole(600, 180, 80);
        for (int frame = 0; frame < 180; frame++) hole.update();
        List<DroneSwarm.Target> unsafe = List.of(new DroneSwarm.Target(600, 180, 10));
        for (int frame = 0; frame < 30; frame++) {
            swarm.update(ship, unsafe, List.of(hole), 1200, 700);
        }
        check(swarm.drainShots().isEmpty(),
                "drone does not fire at an object inside a black hole");
        check(swarm.getActiveCount() == 1
                        && !hole.touchesCircle(swarm.getDrones().get(0).getX(),
                        swarm.getDrones().get(0).getY(), 6),
                "drone steers clear of the event horizon");
    }

    public static void main(String[] args) {
        reusesVacantStation();
        collisionPreventsShot();
        avoidsBlackHoleTarget();
        System.out.println("DroneSwarmRegressionTest passed " + assertions + " assertions");
    }
}
