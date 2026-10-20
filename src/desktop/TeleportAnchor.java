import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/** Chooses a clear destination, shows it briefly, then moves the protected ship. */
public class TeleportAnchor {
    private static final int DELAY_FRAMES = 30;
    private static final int ARRIVAL_PROTECTION_FRAMES = 15;
    private static final int MIN_EDGE_DISTANCE = 90;
    private static final double MIN_LANDING_CLEARANCE = 32;
    private double anchorX, anchorY;
    private boolean pending;
    private int pendingFrames;
    private int arrivalProtectionFrames;
    private int cooldownTimer;
    private int maxCooldown = 10 * 60;
    private float pulsePhase;

    public boolean beginTeleport(Ship ship, List<Rectangle> obstacles,
                                 int width, int height, int level) {
        if (ship == null || level < 1 || level > 3 || pending || cooldownTimer > 0) return false;
        setLevel(level);
        double[] destination = findSafestPoint(obstacles, width, height);
        anchorX = destination[0];
        anchorY = destination[1];
        pending = true;
        pendingFrames = DELAY_FRAMES;
        arrivalProtectionFrames = 0;
        return true;
    }

    /** Keeps the active cooldown aligned with the upgraded ability display. */
    public void setLevel(int level) {
        if (level < 1 || level > 3) return;
        int nextCooldown = switch (level) {
            case 1 -> 10 * 60;
            case 2 -> 5 * 60;
            default -> 2 * 60;
        };
        if (cooldownTimer > 0 && maxCooldown > 0) {
            cooldownTimer = (int) Math.ceil(cooldownTimer * (double) nextCooldown / maxCooldown);
        }
        maxCooldown = nextCooldown;
    }

    private double[] findSafestPoint(List<Rectangle> obstacles, int width, int height) {
        // Spawned asteroids enter from the edges. Reserve reaction room even when
        // an edge happens to have the greatest immediate object clearance.
        int inset = Math.min(MIN_EDGE_DISTANCE, Math.max(0, Math.min(width, height) / 4));
        int left = inset;
        int top = inset;
        int right = Math.max(left, width - inset);
        int bottom = Math.max(top, height - inset);
        double bestX = width / 2.0, bestY = height / 2.0;
        if (obstacles == null || obstacles.isEmpty()) return new double[]{bestX, bestY};

        double bestScore = -1;
        // Interior lattice followed by two local refinements.
        int step = 28;
        for (int y = top; y <= bottom; y += step) {
            for (int x = left; x <= right; x += step) {
                double score = clearanceSquared(x, y, obstacles);
                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        for (int scale : new int[]{7, 2}) {
            double centerX = bestX, centerY = bestY;
            for (int iy = -4; iy <= 4; iy++) {
                for (int ix = -4; ix <= 4; ix++) {
                    double x = Math.max(left, Math.min(right, centerX + ix * scale));
                    double y = Math.max(top, Math.min(bottom, centerY + iy * scale));
                    double score = clearanceSquared(x, y, obstacles);
                    if (score > bestScore) {
                        bestScore = score;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }
        return new double[]{bestX, bestY};
    }

    private double clearanceSquared(double x, double y, List<Rectangle> obstacles) {
        double closest = Double.POSITIVE_INFINITY;
        for (Rectangle obstacle : obstacles) {
            if (obstacle == null) continue;
            double dx = x - Math.max(obstacle.getMinX(), Math.min(x, obstacle.getMaxX()));
            double dy = y - Math.max(obstacle.getMinY(), Math.min(y, obstacle.getMaxY()));
            closest = Math.min(closest, dx * dx + dy * dy);
        }
        return closest;
    }

    /** Returns true only on the frame when the ship reaches its destination. */
    public boolean update(Ship ship) {
        return update(ship, null, 0, 0);
    }

    public boolean update(Ship ship, List<Rectangle> obstacles, int width, int height) {
        tickClock();
        if (!pending || ship == null) return false;
        if (obstacles != null && width > 0 && height > 0
                && clearanceSquared(anchorX, anchorY, obstacles)
                        < MIN_LANDING_CLEARANCE * MIN_LANDING_CLEARANCE) {
            double[] destination = findSafestPoint(obstacles, width, height);
            anchorX = destination[0];
            anchorY = destination[1];
        }
        if (--pendingFrames > 0) return false;
        ship.setX(anchorX);
        ship.setY(anchorY);
        pending = false;
        pendingFrames = 0;
        cooldownTimer = maxCooldown;
        arrivalProtectionFrames = ARRIVAL_PROTECTION_FRAMES;
        return true;
    }

    private void tickClock() {
        if (!pending && cooldownTimer > 0) cooldownTimer--;
        if (arrivalProtectionFrames > 0) arrivalProtectionFrames--;
        pulsePhase += 0.14f;
    }

    public void drawAnchor(Graphics2D g2d) {
        if (!pending) return;
        java.awt.Stroke oldStroke = g2d.getStroke();
        Font oldFont = g2d.getFont();
        int x = (int) Math.round(anchorX), y = (int) Math.round(anchorY);
        int alpha = 110 + (int) (90 * (0.5 + 0.5 * Math.sin(pulsePhase)));
        g2d.setColor(new Color(90, 245, 215, alpha));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - 18, y - 18, 36, 36);
        g2d.drawLine(x - 26, y, x - 10, y);
        g2d.drawLine(x + 10, y, x + 26, y);
        g2d.drawLine(x, y - 26, x, y - 10);
        g2d.drawLine(x, y + 10, x, y + 26);
        if ((pendingFrames / 5) % 2 == 0) {
            g2d.fillOval(x - 3, y - 3, 6, 6);
        }
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        String label = "TELEPORT";
        g2d.drawString(label, x - fm.stringWidth(label) / 2, y - 30);
        g2d.setStroke(oldStroke);
        g2d.setFont(oldFont);
    }

    public void drawTeleportFlash(Graphics2D g2d, int width, int height, int frame) {
        float alpha = Math.max(0, 1f - frame / 15f);
        if (alpha <= 0) return;
        g2d.setColor(new Color(90, 245, 215, (int) (90 * alpha)));
        g2d.fillRect(0, 0, width, height);
        int radius = frame * 28;
        g2d.setColor(new Color(90, 245, 215, (int) (160 * alpha)));
        g2d.drawOval((int) anchorX - radius, (int) anchorY - radius,
                radius * 2, radius * 2);
    }

    public void reset() {
        anchorX = anchorY = 0;
        pending = false;
        pendingFrames = cooldownTimer = 0;
        arrivalProtectionFrames = 0;
        maxCooldown = 10 * 60;
        pulsePhase = 0;
    }

    public boolean isPending() { return pending; }
    public boolean isProtected() { return pending || arrivalProtectionFrames > 0; }
    public boolean isOnCooldown() { return cooldownTimer > 0; }
    public int getCooldownTimer() { return cooldownTimer; }
    public void setCooldownTimer(int frames) { cooldownTimer = Math.max(0, frames); }
    public int getMaxCooldown() { return maxCooldown; }
    public double getTargetX() { return anchorX; }
    public double getTargetY() { return anchorY; }
}
