import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Passive constructions and their independent four-slot loadout. */
public final class UtilitySystem {
    public static final int MODULATOR = 0;
    public static final int SWARM = 1;
    public static final int METRIC = 2;
    public static final int SHIELD = 3;
    public static final String[] NAMES = {"modulator", "swarm", "metric", "shield"};
    private static final String[] LABELS = {"Energy Modulator", "Swarm", "Metric Divergence", "Shield"};
    private static final Font PANEL_FONT = new Font("SansSerif", Font.BOLD, 11);
    private static final int FPS = 60;
    private static final double METRIC_OUTER_RADIUS = 260;

    public record Target(Object object, double x, double y, double radius) {}
    public record Blast(double x, double y, double radius) {}
    public record Destruction(double x, double y) {}

    public static final class SwarmBot {
        private double x, y, vx, vy;
        private final int station;
        private int lifetime;
        private SwarmBot(double x, double y, int station, int lifetime) {
            this.x = x; this.y = y; this.station = station; this.lifetime = lifetime;
        }
        public double getX() { return x; }
        public double getY() { return y; }
        public int getStation() { return station; }
    }

    private static final class Explosion {
        final double x, y, radius;
        int frames = 20;
        Explosion(Blast blast) { x = blast.x; y = blast.y; radius = blast.radius; }
    }

    private final int[] levels = new int[4];
    private final String[] slots = new String[4];
    private final List<SwarmBot> bots = new ArrayList<>(6);
    private final List<Blast> blasts = new ArrayList<>(6);
    private final List<Destruction> destructions = new ArrayList<>(6);
    private final List<Explosion> explosions = new ArrayList<>(6);
    private int swarmCountdown;
    private boolean firstWavePending;

    public int level(int utility) { return utility >= 0 && utility < 4 ? levels[utility] : 0; }
    public int[] levels() { return levels.clone(); }
    public String[] slots() { return slots.clone(); }
    public String getSlot(int slot) { return slot >= 0 && slot < 4 ? slots[slot] : null; }
    public boolean isSlotAvailable(int slot) { return slot >= 0 && slot < 4 && slots[slot] == null; }
    public boolean hasAvailableSlot() {
        for (String slot : slots) if (slot == null) return true;
        return false;
    }
    public int findSlot(String utility) {
        for (int i = 0; i < 4; i++) if (utility != null && utility.equals(slots[i])) return i;
        return -1;
    }
    public boolean assignSlot(int slot, String utility) {
        if (!isSlotAvailable(slot) || findSlot(utility) >= 0) return false;
        int id = indexOf(utility);
        if (id < 0 || levels[id] == 0) return false;
        slots[slot] = utility;
        return true;
    }
    public void clearSlot(int slot) { if (slot >= 0 && slot < 4) slots[slot] = null; }
    public void setLevel(int utility, int level) {
        if (utility < 0 || utility >= 4) return;
        int previous = levels[utility];
        levels[utility] = Math.max(0, Math.min(3, level));
        if (levels[utility] == 0) {
            int slot = findSlot(NAMES[utility]);
            if (slot >= 0) slots[slot] = null;
            if (utility == SWARM) { bots.clear(); swarmCountdown = 0; firstWavePending = false; }
        } else if (utility == SWARM) {
            if (previous == 0 || swarmCountdown == 0) {
                swarmCountdown = FPS;
                firstWavePending = true;
            }
            else swarmCountdown = Math.min(swarmCountdown, swarmPeriod());
        }
    }
    public static int indexOf(String utility) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(utility)) return i;
        return -1;
    }
    public int getSwarmCountdownFrames() { return level(SWARM) == 0 ? 0 : swarmCountdown; }
    public double getSwarmChargePercent() {
        return level(SWARM) == 0 ? 0
                : 1.0 - swarmCountdown / (double) (firstWavePending ? FPS : swarmPeriod());
    }
    public int getSwarmCount() { return bots.size(); }
    public List<SwarmBot> getBots() { return List.copyOf(bots); }

    private int swarmCount() { return switch (level(SWARM)) { case 1 -> 2; case 2 -> 4; case 3 -> 6; default -> 0; }; }
    private int swarmPeriod() { return switch (level(SWARM)) { case 1 -> 10 * FPS; case 2 -> 8 * FPS; case 3 -> 5 * FPS; default -> 0; }; }

    public void updateSwarm(Ship ship, List<Target> targets, List<BlackHole> holes,
                            int width, int height) {
        for (Iterator<Explosion> it = explosions.iterator(); it.hasNext();) {
            if (--it.next().frames <= 0) it.remove();
        }
        if (level(SWARM) == 0) return;
        if (--swarmCountdown <= 0) {
            bots.clear();
            spawnWave(ship);
            swarmCountdown = swarmPeriod();
            firstWavePending = false;
        }
        Set<Object> claimed = new HashSet<>();
        for (Iterator<SwarmBot> it = bots.iterator(); it.hasNext();) {
            SwarmBot bot = it.next();
            if (--bot.lifetime <= 0) { it.remove(); continue; }
            Target target = nearestUnclaimed(bot, targets, claimed, holes);
            double goalX, goalY;
            if (target != null) {
                claimed.add(target.object());
                goalX = target.x(); goalY = target.y();
            } else {
                double bearing = ship.getAngle() + Math.PI * 2 * bot.station / swarmCount();
                goalX = ship.getX() + 62 * Math.sin(bearing);
                goalY = ship.getY() - 62 * Math.cos(bearing);
            }
            double dx = goalX - bot.x, dy = goalY - bot.y;
            double distance = Math.max(1, Math.hypot(dx, dy));
            bot.vx = (bot.vx + dx / distance * 0.28) * 0.96;
            bot.vy = (bot.vy + dy / distance * 0.28) * 0.96;
            for (BlackHole hole : holes) {
                var away = hole.avoidanceForce(bot.x, bot.y);
                bot.vx += away.x * 0.85;
                bot.vy += away.y * 0.85;
                if (hole.isPlayerOwned()) continue;
                var pull = hole.applyGravity(bot.x, bot.y);
                bot.vx += pull.x * 0.25;
                bot.vy += pull.y * 0.25;
            }
            double speed = Math.hypot(bot.vx, bot.vy);
            if (speed > 3.8) { bot.vx *= 3.8 / speed; bot.vy *= 3.8 / speed; }
            bot.x = Math.max(5, Math.min(width - 5, bot.x + bot.vx));
            bot.y = Math.max(5, Math.min(height - 5, bot.y + bot.vy));
            boolean consumed = false;
            for (BlackHole hole : holes) {
                if (!hole.isPlayerOwned() && hole.touchesCircle(bot.x, bot.y, 6)) {
                    hole.grow(2.5);
                    destructions.add(new Destruction(bot.x, bot.y));
                    consumed = true;
                    break;
                }
            }
            if (consumed) { it.remove(); continue; }
            if (target != null && Math.hypot(bot.x - target.x(), bot.y - target.y())
                    <= target.radius() + 6) {
                Blast blast = new Blast(bot.x, bot.y, 85);
                blasts.add(blast);
                explosions.add(new Explosion(blast));
                it.remove();
            }
        }
    }

    private void spawnWave(Ship ship) {
        int count = swarmCount();
        for (int station = 0; station < count; station++) {
            boolean occupied = false;
            for (SwarmBot bot : bots) if (bot.station == station) { occupied = true; break; }
            if (occupied) continue;
            double bearing = ship.getAngle() + Math.PI * 2 * station / count;
            bots.add(new SwarmBot(ship.getX() + 54 * Math.sin(bearing),
                    ship.getY() - 54 * Math.cos(bearing), station, swarmPeriod()));
        }
    }

    private static Target nearestUnclaimed(SwarmBot bot, List<Target> targets,
                                           Set<Object> claimed, List<BlackHole> holes) {
        Target nearest = null;
        double best = 460;
        for (Target target : targets) {
            if (claimed.contains(target.object())) continue;
            boolean unsafe = false;
            for (BlackHole hole : holes) {
                if (hole.unsafeTarget(target.x(), target.y(), target.radius())) {
                    unsafe = true;
                    break;
                }
            }
            if (unsafe) continue;
            double distance = Math.hypot(target.x() - bot.x, target.y() - bot.y) - target.radius();
            if (distance < best) { best = distance; nearest = target; }
        }
        return nearest;
    }

    public List<Blast> drainBlasts() {
        List<Blast> result = List.copyOf(blasts);
        blasts.clear();
        return result;
    }
    public List<Blast> activeBlasts() {
        List<Blast> active = new ArrayList<>(explosions.size());
        for (Explosion fx : explosions) active.add(new Blast(fx.x, fx.y, fx.radius));
        return active;
    }
    public List<Destruction> drainDestructions() {
        List<Destruction> result = List.copyOf(destructions);
        destructions.clear();
        return result;
    }

    public double metricSpeedScale(double x, double y, double vx, double vy,
                                   double shipX, double shipY) {
        int level = level(METRIC);
        if (level == 0 || (shipX - x) * vx + (shipY - y) * vy <= 0) return 1;
        double distance = Math.hypot(shipX - x, shipY - y);
        int rings = level + 2;
        int crossed = 0;
        for (int ring = 1; ring <= rings; ring++) {
            if (distance <= METRIC_OUTER_RADIUS / Math.sqrt(ring)) crossed++;
        }
        double factor = switch (level) { case 1 -> 0.8; case 2 -> 0.5; default -> 0.3; };
        return Math.pow(factor, crossed);
    }

    public void drawMetricRings(Graphics2D g, double shipX, double shipY) {
        int level = level(METRIC);
        if (level == 0) return;
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1.2f));
        for (int ring = 1; ring <= level + 2; ring++) {
            int radius = (int) Math.round(METRIC_OUTER_RADIUS / Math.sqrt(ring));
            g.setColor(new Color(105, 220, 245, 85 + ring * 12));
            g.drawOval((int) shipX - radius, (int) shipY - radius,
                    2 * radius, 2 * radius);
        }
        g.setStroke(old);
    }

    public void drawSwarm(Graphics2D g) {
        Stroke old = g.getStroke();
        for (SwarmBot bot : bots) {
            int x = (int) bot.x, y = (int) bot.y;
            g.setColor(new Color(60, 135, 155));
            g.fillOval(x - 7, y - 7, 14, 14);
            g.setColor(new Color(220, 250, 255));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(x - 7, y - 7, 14, 14);
            g.fillOval(x - 2, y - 2, 4, 4);
        }
        for (Explosion fx : explosions) {
            double progress = (20 - fx.frames) / 20.0;
            int radius = (int) (fx.radius * progress);
            g.setColor(new Color(140, 225, 250, Math.max(0, fx.frames * 9)));
            g.setStroke(new BasicStroke(3));
            g.drawOval((int) fx.x - radius, (int) fx.y - radius,
                    radius * 2, radius * 2);
        }
        g.setStroke(old);
    }

    public void drawStatusPanel(Graphics2D g, int width, int height, AbilityManager abilities) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            int x = 12, y = 104, panelWidth = Math.min(222, Math.max(174, width / 5));
            int panelHeight = 250;
            copy.setColor(new Color(20, 25, 35, 155));
            copy.fillRoundRect(x, y, panelWidth, panelHeight, 12, 12);
            copy.setColor(new Color(80, 130, 155, 195));
            copy.setStroke(new BasicStroke(2));
            copy.drawRoundRect(x, y, panelWidth, panelHeight, 12, 12);
            copy.setFont(PANEL_FONT);
            copy.setColor(new Color(165, 225, 240));
            copy.drawString("UTILITIES", x + 14, y + 18);
            for (int slot = 0; slot < 4; slot++) {
                int id = indexOf(slots[slot]);
                int cx = x + 32, cy = y + 48 + slot * 54;
                boolean equipped = id >= 0;
                double progress = id == SWARM && swarmCountdown > 0
                        ? getSwarmChargePercent()
                        : id == SHIELD && !abilities.isShieldReady()
                        ? abilities.getShieldChargePercent() : -1;
                copy.setColor(new Color(equipped ? 50 : 32, equipped ? 65 : 35,
                        equipped ? 82 : 44, 190));
                copy.fillOval(cx - 20, cy - 20, 40, 40);
                if (progress >= 0) {
                    copy.setColor(new Color(20, 22, 35, 160));
                    copy.fillOval(cx - 20, cy - 20, 40, 40);
                    copy.setColor(new Color(245, 245, 255, 230));
                    copy.setStroke(new BasicStroke(2.5f));
                    copy.drawArc(cx - 23, cy - 23, 46, 46, 90,
                            -(int) (360 * Math.max(0, Math.min(1, progress))));
                }
                copy.setColor(equipped ? new Color(115, 215, 235)
                        : new Color(105, 112, 130));
                copy.setStroke(new BasicStroke(1.5f));
                copy.drawOval(cx - 20, cy - 20, 40, 40);
                copy.setFont(new Font("SansSerif", Font.BOLD, 15));
                String glyph = equipped ? switch (id) {
                    case MODULATOR -> "M";
                    case SWARM -> "S";
                    case METRIC -> "D";
                    default -> "✦";
                } : "+";
                copy.setColor(equipped ? Color.WHITE : new Color(140, 145, 155));
                copy.drawString(glyph, cx - copy.getFontMetrics().stringWidth(glyph) / 2,
                        cy + 5);
                copy.setFont(PANEL_FONT);
                copy.setColor(equipped ? new Color(225, 242, 248)
                        : new Color(125, 143, 155));
                copy.drawString((slot + 1) + "  " + (equipped ? LABELS[id] : "EMPTY"),
                        x + 63, cy - 3);
                if (equipped) {
                    String status = id == SWARM ? bots.size() + "/" + swarmCount()
                            + "  " + (swarmCountdown + 59) / 60 + "s"
                            : id == SHIELD ? abilities.isShieldReady() ? "READY"
                            : (abilities.getCooldownRemainingFrames("shield") + 59) / 60 + "s"
                            : "LV " + levels[id];
                    copy.setColor(new Color(145, 205, 222));
                    copy.drawString(status, x + 63, cy + 13);
                }
            }
        } finally {
            copy.dispose();
        }
    }
}
