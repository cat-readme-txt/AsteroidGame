import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Moving black hole shared by natural hazards and the player's launched ability. */
public class BlackHole {
    private static final int WARNING_FRAMES = 3 * 60;
    private static final int DEATH_FRAMES = 18;
    private static final double MAX_SIZE = 420;
    private static final Font WARNING_FONT = new Font("SansSerif", Font.BOLD, 13);
    private double x, y, vx, vy, size;
    private double lifetime;
    private double travelRemaining;
    private int spawnTimer;
    private int joinFrames;
    private int deathFrames;
    private double pulse;
    private final boolean playerOwned;
    private boolean traveling;
    private boolean dying;

    public BlackHole(double x, double y, int size) {
        this(x, y, size, false);
        spawnTimer = WARNING_FRAMES;
    }

    private BlackHole(double x, double y, double size, boolean playerOwned) {
        this.x = x;
        this.y = y;
        this.size = Math.min(MAX_SIZE, Math.max(12, size));
        this.playerOwned = playerOwned;
        lifetime = (playerOwned ? 22 : 30) * 60;
    }

    public static BlackHole fired(double x, double y, int size, double angle, double distance) {
        BlackHole hole = new BlackHole(x, y, size, true);
        hole.traveling = true;
        hole.travelRemaining = Math.max(0, distance);
        hole.vx = Math.sin(angle) * 12;
        hole.vy = -Math.cos(angle) * 12;
        return hole;
    }

    public static BlackHole merge(BlackHole a, BlackHole b) {
        double massA = a.size * a.size, massB = b.size * b.size;
        double mass = massA + massB;
        BlackHole joined = new BlackHole((a.x * massA + b.x * massB) / mass,
                (a.y * massA + b.y * massB) / mass, Math.sqrt(mass),
                a.playerOwned || b.playerOwned);
        joined.vx = (a.vx * massA + b.vx * massB) / mass;
        joined.vy = (a.vy * massA + b.vy * massB) / mass;
        joined.lifetime = Math.max(a.lifetime, b.lifetime)
                + Math.min(a.lifetime, b.lifetime) / 2.0;
        joined.joinFrames = 22;
        return joined;
    }

    public void attractToward(BlackHole other) {
        if (!isActive() || !other.isActive()) return;
        double dx = other.x - x, dy = other.y - y;
        double distance = Math.hypot(dx, dy);
        if (distance < 1 || distance > 900) return;
        double massFraction = other.size * other.size
                / (size * size + other.size * other.size);
        double attenuation = Math.pow(180 / Math.max(180, distance), 2);
        double acceleration = 0.1 * massFraction * attenuation;
        vx += dx / distance * acceleration;
        vy += dy / distance * acceleration;
    }

    public void update() { update(1200, 700); }

    public void update(int width, int height) {
        pulse += 0.08;
        if (spawnTimer > 0) { spawnTimer--; return; }
        if (joinFrames > 0) joinFrames--;
        if (dying) { deathFrames--; return; }
        if (traveling) {
            double step = Math.min(travelRemaining, Math.hypot(vx, vy));
            double velocity = Math.max(0.001, Math.hypot(vx, vy));
            x = wrap(x + vx / velocity * step, width);
            y = wrap(y + vy / velocity * step, height);
            travelRemaining -= step;
            if (travelRemaining <= 0.001) { traveling = false; vx = vy = 0; }
        } else {
            double maxSpeed = Math.max(0.35, Math.min(1.8, 120 / size));
            double velocity = Math.hypot(vx, vy);
            if (velocity > maxSpeed) { vx *= maxSpeed / velocity; vy *= maxSpeed / velocity; }
            x = wrap(x + vx, width);
            y = wrap(y + vy, height);
            vx *= 0.997;
            vy *= 0.997;
        }
        // The remaining life pool is spent faster as the size multiplier falls.
        lifetime -= 1.0 / getLifetimeMultiplier();
        if (lifetime <= 0) beginDeath();
    }

    private static double wrap(double value, int extent) {
        if (extent <= 0) return value;
        return ((value % extent) + extent) % extent;
    }

    public void beginDeath() {
        if (!dying) { dying = true; deathFrames = DEATH_FRAMES; }
    }

    public boolean isExpired() { return dying && deathFrames <= 0; }
    public boolean isActive() { return spawnTimer == 0 && !traveling && !dying; }
    public boolean isWarningActive() { return spawnTimer > 0; }
    public boolean isTraveling() { return traveling; }
    public boolean isPlayerOwned() { return playerOwned; }
    public double getLifetimeFrames() { return lifetime; }
    public double getLifetimeMultiplier() {
        return Math.max(0.4, 0.95 - 0.55 * size / MAX_SIZE);
    }
    public double getEstimatedRemainingFrames() { return lifetime * getLifetimeMultiplier(); }
    public double getSize() { return size; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getEventHorizonRadius() { return size / 2.0; }
    public double getPullRadius() { return Math.min(350, size * 2.75); }
    public double getAvoidanceRadius() { return Math.min(360, getEventHorizonRadius() + 130); }
    public int getWarningSecondsRemaining() { return (spawnTimer + 59) / 60; }

    public void grow(double amount) {
        if (isActive()) size = Math.min(MAX_SIZE, size + Math.max(0, amount));
    }

    public Point2D.Double avoidanceForce(double bodyX, double bodyY) {
        if (!isActive()) return new Point2D.Double();
        double dx = bodyX - x, dy = bodyY - y;
        double distance = Math.hypot(dx, dy);
        double radius = getAvoidanceRadius();
        if (distance >= radius) return new Point2D.Double();
        if (distance < 0.001) return new Point2D.Double(1, 0);
        double strength = Math.min(1, (radius - distance)
                / Math.max(1, radius - getEventHorizonRadius()));
        return new Point2D.Double(dx / distance * strength, dy / distance * strength);
    }

    public boolean unsafeTarget(double targetX, double targetY, double targetRadius) {
        return isActive() && Math.hypot(targetX - x, targetY - y)
                <= getEventHorizonRadius() + targetRadius + 48;
    }

    public void shrinkFromLaser() {
        if (isActive()) size = Math.max(12, size - 6);
    }

    public boolean canAbsorb(int modulatorLevel) {
        return isActive() && !playerOwned && modulatorLevel >= 3 && size <= 32;
    }

    public boolean shouldBreak(int modulatorLevel) {
        return isActive() && size < 18 && !canAbsorb(modulatorLevel);
    }

    public Point2D.Double applyGravity(double bodyX, double bodyY) {
        if (!isActive()) return new Point2D.Double();
        double dx = x - bodyX, dy = y - bodyY;
        double distance = Math.hypot(dx, dy);
        double radius = getPullRadius();
        if (distance <= getEventHorizonRadius() || distance >= radius) return new Point2D.Double();
        double strength = Math.min(1.8, 1.0 * (radius - distance) / radius);
        return new Point2D.Double(dx / distance * strength, dy / distance * strength);
    }

    public boolean isShipConsumed(double shipX, double shipY) {
        return !playerOwned && isActive()
                && Math.hypot(x - shipX, y - shipY) < getEventHorizonRadius();
    }

    public boolean isAsteroidConsumed(java.awt.Rectangle bounds) { return touches(bounds); }

    public boolean touches(Rectangle2D bounds) {
        if (!isActive()) return false;
        double nearX = Math.max(bounds.getMinX(), Math.min(x, bounds.getMaxX()));
        double nearY = Math.max(bounds.getMinY(), Math.min(y, bounds.getMaxY()));
        return Point2D.distanceSq(nearX, nearY, x, y)
                <= getEventHorizonRadius() * getEventHorizonRadius();
    }

    public boolean touchesCircle(double centerX, double centerY, double radius) {
        double reach = getEventHorizonRadius() + radius;
        return isActive() && Point2D.distanceSq(centerX, centerY, x, y) <= reach * reach;
    }

    public boolean touches(Line2D line) { return firstIntersection(line) != null; }

    public Point2D.Double firstIntersection(Line2D line) {
        if (spawnTimer > 0 || traveling || isExpired()) return null;
        double sx = line.getX1(), sy = line.getY1();
        double dx = line.getX2() - sx, dy = line.getY2() - sy;
        double ox = sx - x, oy = sy - y;
        double a = dx * dx + dy * dy;
        if (a < 0.0001) return null;
        double c = ox * ox + oy * oy - getEventHorizonRadius() * getEventHorizonRadius();
        if (c <= 0) return new Point2D.Double(sx, sy);
        double b = 2 * (ox * dx + oy * dy);
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return null;
        double t = (-b - Math.sqrt(discriminant)) / (2 * a);
        return t >= 0 && t <= 1 ? new Point2D.Double(sx + dx * t, sy + dy * t) : null;
    }

    public boolean touches(Polygon polygon) {
        if (!isActive() || polygon.npoints == 0) return false;
        if (polygon.contains(x, y)) return true;
        for (int i = 0; i < polygon.npoints; i++) {
            int next = (i + 1) % polygon.npoints;
            if (Line2D.ptSegDistSq(polygon.xpoints[i], polygon.ypoints[i],
                    polygon.xpoints[next], polygon.ypoints[next], x, y)
                    <= getEventHorizonRadius() * getEventHorizonRadius()) return true;
        }
        return false;
    }

    public void draw(Graphics2D graphics) { draw(graphics, false); }

    public void draw(Graphics2D graphics, boolean absorbMark) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            int cx = (int) Math.round(x), cy = (int) Math.round(y);
            int horizon = Math.max(6, (int) Math.round(getEventHorizonRadius()));
            float alpha = spawnTimer > 0 ? 1 - spawnTimer / (float) WARNING_FRAMES
                    : dying ? Math.max(0, deathFrames / (float) DEATH_FRAMES) : 1;
            g.setComposite(java.awt.AlphaComposite.SrcOver.derive(alpha));
            Color bright = playerOwned ? new Color(191, 108, 255) : new Color(255, 164, 72);
            Color dim = playerOwned ? new Color(99, 42, 161) : new Color(168, 84, 40);
            int outer = horizon + 18;
            g.setColor(dim);
            g.setStroke(new BasicStroke(5));
            g.drawOval(cx - outer, cy - outer, outer * 2, outer * 2);
            g.setColor(bright);
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(cx - horizon - 8, cy - horizon - 8,
                    (horizon + 8) * 2, (horizon + 8) * 2);
            g.setColor(Color.BLACK);
            g.fillOval(cx - horizon, cy - horizon, horizon * 2, horizon * 2);
            g.setColor(playerOwned ? new Color(225, 180, 255) : new Color(255, 210, 150));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(cx - horizon, cy - horizon, horizon * 2, horizon * 2);
            if (joinFrames > 0) {
                int ring = outer + (22 - joinFrames) * 2;
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3));
                g.drawOval(cx - ring, cy - ring, ring * 2, ring * 2);
            }
            if (absorbMark && canAbsorb(3)) {
                int mark = outer + 8 + (int) (2 * Math.sin(pulse));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawLine(cx - mark - 7, cy, cx - mark + 1, cy);
                g.drawLine(cx + mark - 1, cy, cx + mark + 7, cy);
                g.drawLine(cx, cy - mark - 7, cx, cy - mark + 1);
                g.drawLine(cx, cy + mark - 1, cx, cy + mark + 7);
            }
            if (spawnTimer > 0) {
                int ring = horizon + 25;
                g.setColor(new Color(255, 175, 110));
                g.setStroke(new BasicStroke(2));
                g.drawOval(cx - ring, cy - ring, ring * 2, ring * 2);
                String label = "BLACK HOLE IN " + getWarningSecondsRemaining() + "s";
                g.setFont(WARNING_FONT);
                g.drawString(label, cx - g.getFontMetrics().stringWidth(label) / 2,
                        cy - ring - 12);
            }
        } finally { g.dispose(); }
    }
}
