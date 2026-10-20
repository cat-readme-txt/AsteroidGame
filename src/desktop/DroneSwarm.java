import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** At most three independently steered drones. All distances are in screen pixels. */
public class DroneSwarm {
    public record Target(double x, double y, double radius) {}
    public record Shot(double x, double y, double angle) {}
    public record Destruction(double x, double y) {}

    public static final class Drone {
        private double x, y, vx, vy, angle;
        private int fireTimer;
        private final int station;

        private Drone(double x, double y, int station) {
            this.x = x;
            this.y = y;
            this.station = station;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getAngle() { return angle; }
        public int getStation() { return station; }
        public Ellipse2D getBounds() { return new Ellipse2D.Double(x - 6, y - 6, 12, 12); }
    }

    private static final double FIRE_RANGE = 165;
    private static final double PURSUIT_RANGE = 450;
    private static final double SPEED = 2.5;
    private static final int CHARGE_FRAMES = 12 * 60;
    private final List<Drone> drones = new ArrayList<>(3);
    private final List<Shot> shots = new ArrayList<>(3);
    private final List<Destruction> destructions = new ArrayList<>(3);
    private int level;
    private int charges;
    private int chargeTimer;

    public void setLevel(int newLevel) {
        level = Math.max(0, Math.min(3, newLevel));
        charges = Math.min(charges, level);
        while (drones.size() > level) destroyDrone(drones.get(0));
        if (level == 0) chargeTimer = 0;
    }

    public int getLevel() { return level; }
    public int getCharges() { return charges; }
    public int getMaxCharges() { return level; }
    public int getActiveCount() { return drones.size(); }
    public void clearActive() {
        for (Drone drone : drones) destructions.add(new Destruction(drone.x, drone.y));
        drones.clear();
        shots.clear();
    }
    public int getRechargeRemainingFrames() {
        return level == 0 || charges >= level ? 0 : CHARGE_FRAMES - chargeTimer;
    }
    public void setRechargeRemainingFrames(int frames) {
        if (level == 0 || charges >= level) return;
        if (frames == 0) { charges++; chargeTimer = 0; }
        else chargeTimer = Math.max(0, CHARGE_FRAMES - frames);
    }
    public double getChargePercent() {
        return level > 0 && charges < level ? (double) chargeTimer / CHARGE_FRAMES : 1.0;
    }
    public List<Drone> getDrones() { return List.copyOf(drones); }

    public void updateCharge() {
        if (level == 0 || charges >= level) return;
        if (++chargeTimer >= CHARGE_FRAMES) {
            charges++;
            chargeTimer = 0;
        }
    }

    public boolean deploy(Ship ship) {
        if (level == 0 || charges <= 0) return false;
        int station;
        if (drones.size() >= level) {
            Drone replaced = drones.get(0);
            station = replaced.station;
            destroyDrone(replaced);
        } else {
            station = 0;
            while (station < level) {
                int candidate = station;
                if (drones.stream().noneMatch(drone -> drone.station == candidate)) break;
                station++;
            }
        }
        double angle = ship.getAngle();
        double rightX = Math.cos(angle);
        double rightY = Math.sin(angle);
        double forwardX = Math.sin(angle);
        double forwardY = -Math.cos(angle);
        double x, y;
        if (station == 0) {
            x = ship.getX() + forwardX * 40;
            y = ship.getY() + forwardY * 40;
        } else {
            double side = station == 1 ? 1 : -1;
            x = ship.getX() + rightX * 19 * side + forwardX * 5;
            y = ship.getY() + rightY * 19 * side + forwardY * 5;
        }
        drones.add(new Drone(x, y, station));
        charges--;
        return true;
    }

    public boolean deployForDeveloper(Ship ship) {
        if (level == 0) return false;
        int previousCharges = charges;
        charges = Math.max(1, charges);
        boolean deployed = deploy(ship);
        charges = previousCharges;
        return deployed;
    }

    public void update(Ship ship, List<Target> targets, int width, int height) {
        update(ship, targets, List.of(), width, height);
    }

    public void update(Ship ship, List<Target> targets, List<BlackHole> holes,
                       int width, int height) {
        if (level == 0) return;
        int fireDelay = switch (level) {
            case 1 -> 5 * 60;
            case 2 -> 3 * 60;
            default -> 60;
        };

        for (Drone drone : drones) {
            if (drone.fireTimer > 0) drone.fireTimer--;
            Target nearest = findNearest(drone, targets, holes);
            double steerX = 0, steerY = 0;
            double targetDistance = nearest == null ? Double.POSITIVE_INFINITY
                    : Math.hypot(nearest.x - drone.x, nearest.y - drone.y);

            if (targetDistance < PURSUIT_RANGE) {
                double dx = nearest.x - drone.x, dy = nearest.y - drone.y;
                double desiredDistance = Math.max(95, nearest.radius + 65);
                double approach = targetDistance > desiredDistance + 14 ? 1
                        : targetDistance < desiredDistance - 14 ? -0.8 : 0;
                steerX += dx / Math.max(1, targetDistance) * approach;
                steerY += dy / Math.max(1, targetDistance) * approach;
            } else {
                double orbitAngle = ship.getAngle() + (drone.station - 1) * 0.9;
                double goalX = ship.getX() + 90 * Math.sin(orbitAngle);
                double goalY = ship.getY() - 90 * Math.cos(orbitAngle);
                double dx = goalX - drone.x, dy = goalY - drone.y;
                double distance = Math.max(1, Math.hypot(dx, dy));
                steerX += dx / distance * 0.75;
                steerY += dy / distance * 0.75;
            }

            // Separation is strongest at close range; pursuit remains possible beyond it.
            for (Target target : targets) {
                double dx = drone.x - target.x, dy = drone.y - target.y;
                double distance = Math.max(1, Math.hypot(dx, dy));
                double danger = target.radius + 48;
                if (distance < danger) {
                    double strength = (danger - distance) / danger * 2.2;
                    steerX += dx / distance * strength;
                    steerY += dy / distance * strength;
                }
            }
            double shipDx = drone.x - ship.getX(), shipDy = drone.y - ship.getY();
            double shipDistance = Math.max(1, Math.hypot(shipDx, shipDy));
            if (shipDistance < 75) {
                double strength = (75 - shipDistance) / 75 * 2.3;
                steerX += shipDx / shipDistance * strength;
                steerY += shipDy / shipDistance * strength;
            }
            for (Drone other : drones) {
                if (other == drone) continue;
                double dx = drone.x - other.x, dy = drone.y - other.y;
                double distance = Math.max(1, Math.hypot(dx, dy));
                if (distance < 50) {
                    double strength = (50 - distance) / 50 * 1.8;
                    steerX += dx / distance * strength;
                    steerY += dy / distance * strength;
                }
            }
            for (BlackHole hole : holes) {
                var away = hole.avoidanceForce(drone.x, drone.y);
                steerX += away.x * 3.8;
                steerY += away.y * 3.8;
                if (!hole.isPlayerOwned()) {
                    var pull = hole.applyGravity(drone.x, drone.y);
                    steerX += pull.x * 0.25;
                    steerY += pull.y * 0.25;
                }
            }

            drone.vx = (drone.vx + steerX * 0.16) * 0.96;
            drone.vy = (drone.vy + steerY * 0.16) * 0.96;
            double speed = Math.hypot(drone.vx, drone.vy);
            if (speed > SPEED) {
                drone.vx *= SPEED / speed;
                drone.vy *= SPEED / speed;
            }
            drone.x = Math.max(10, Math.min(width - 10, drone.x + drone.vx));
            drone.y = Math.max(10, Math.min(height - 10, drone.y + drone.vy));
            if (speed > 0.1) drone.angle = Math.atan2(drone.vx, -drone.vy);

        }

        Set<Drone> collided = new HashSet<>();
        for (Drone drone : drones) {
            boolean hit = Math.hypot(drone.x - ship.getX(), drone.y - ship.getY()) < 15;
            for (Target target : targets) {
                if (Math.hypot(drone.x - target.x, drone.y - target.y) < target.radius + 6) {
                    hit = true;
                    break;
                }
            }
            if (hit) collided.add(drone);
        }
        for (int i = 0; i < drones.size(); i++) {
            for (int j = i + 1; j < drones.size(); j++) {
                Drone a = drones.get(i), b = drones.get(j);
                if (Math.hypot(a.x - b.x, a.y - b.y) < 12) {
                    collided.add(a);
                    collided.add(b);
                }
            }
        }
        for (Drone drone : drones) {
            if (collided.contains(drone) || drone.fireTimer > 0) continue;
            Target nearest = findNearest(drone, targets, holes);
            if (nearest == null) continue;
            double targetDistance = Math.hypot(nearest.x - drone.x, nearest.y - drone.y);
            if (targetDistance > FIRE_RANGE || targetDistance <= nearest.radius + 8) continue;
            double angle = Math.atan2(nearest.x - drone.x, -(nearest.y - drone.y));
            shots.add(new Shot(drone.x, drone.y, angle));
            drone.fireTimer = fireDelay;
            drone.angle = angle;
        }
        for (Iterator<Drone> it = drones.iterator(); it.hasNext();) {
            Drone drone = it.next();
            if (collided.contains(drone)) {
                destructions.add(new Destruction(drone.x, drone.y));
                it.remove();
            }
        }
    }

    private Target findNearest(Drone drone, List<Target> targets, List<BlackHole> holes) {
        Target nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (Target target : targets) {
            boolean unsafe = false;
            for (BlackHole hole : holes) {
                if (hole.unsafeTarget(target.x, target.y, target.radius)) {
                    unsafe = true;
                    break;
                }
            }
            if (unsafe) continue;
            double distance = Math.hypot(target.x - drone.x, target.y - drone.y) - target.radius;
            if (distance < best) {
                nearest = target;
                best = distance;
            }
        }
        return nearest;
    }

    public void destroyDrone(Drone drone) {
        if (drones.remove(drone)) destructions.add(new Destruction(drone.x, drone.y));
    }

    public List<Shot> drainShots() {
        List<Shot> result = List.copyOf(shots);
        shots.clear();
        return result;
    }

    public List<Destruction> drainDestructions() {
        List<Destruction> result = List.copyOf(destructions);
        destructions.clear();
        return result;
    }

    public void draw(Graphics2D g2d) {
        java.awt.Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1.5f));
        for (Drone drone : drones) {
            double forwardX = Math.sin(drone.angle), forwardY = -Math.cos(drone.angle);
            double rightX = Math.cos(drone.angle), rightY = Math.sin(drone.angle);
            int[] xs = {
                (int) (drone.x + forwardX * 8),
                (int) (drone.x - forwardX * 5 + rightX * 6),
                (int) (drone.x - forwardX * 5 - rightX * 6)
            };
            int[] ys = {
                (int) (drone.y + forwardY * 8),
                (int) (drone.y - forwardY * 5 + rightY * 6),
                (int) (drone.y - forwardY * 5 - rightY * 6)
            };
            g2d.setColor(new Color(55, 76, 105, 210));
            g2d.fillPolygon(xs, ys, 3);
            g2d.setColor(new Color(145, 225, 255, 235));
            g2d.drawPolygon(xs, ys, 3);
            g2d.fillOval((int) drone.x - 2, (int) drone.y - 2, 4, 4);
        }
        g2d.setStroke(oldStroke);
    }

    public void reset() {
        drones.clear();
        shots.clear();
        destructions.clear();
        level = charges = chargeTimer = 0;
    }
}
