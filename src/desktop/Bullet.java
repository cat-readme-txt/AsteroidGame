import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class Bullet {
    private static final int LIFETIME_FRAMES = 180;
    private static final double SPEED = 10;
    private static final Color[] COLORS = {
            new Color(255, 235, 170), new Color(230, 196, 255),
            new Color(195, 143, 255), new Color(162, 100, 255)
    };
    private double x, y;
    private final double vx, vy;
    private final int radius;
    private final int piercingLevel;
    private final int enlargementLevel;
    private final Set<Object> hitTargets = Collections.newSetFromMap(new IdentityHashMap<>());
    private int age;
    private boolean consumed;
    private final Ellipse2D.Double bounds = new Ellipse2D.Double();
    private final Rectangle2D.Double boundingBox = new Rectangle2D.Double();

    public Bullet(double x, double y, double angle) { this(x, y, angle, 0, 0); }
    public Bullet(double x, double y, double angle, boolean piercing) {
        this(x, y, angle, piercing ? 1 : 0, 0);
    }
    public Bullet(double x, double y, double angle, int piercingLevel, int enlargementLevel) {
        this.x = x;
        this.y = y;
        this.piercingLevel = Math.max(0, Math.min(3, piercingLevel));
        this.enlargementLevel = Math.max(0, Math.min(3, enlargementLevel));
        radius = 3 + 2 * this.enlargementLevel;
        vx = SPEED * Math.sin(angle);
        vy = -SPEED * Math.cos(angle);
        updateBounds();
    }

    public void update(int universeWidth, int universeHeight) {
        if (!isAlive()) return;
        age++;
        x += vx;
        y += vy;
        if (x < 0) x += universeWidth;
        else if (x > universeWidth) x -= universeWidth;
        if (y < 0) y += universeHeight;
        else if (y > universeHeight) y -= universeHeight;
        updateBounds();
    }

    public boolean canHit(Object target) {
        return isAlive() && target != null && !hitTargets.contains(target);
    }

    public boolean registerHit(Object target) {
        if (!canHit(target)) return isAlive();
        hitTargets.add(target);
        int maxHits = switch (piercingLevel) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> Integer.MAX_VALUE;
            default -> 1;
        };
        if (hitTargets.size() >= maxHits) consumed = true;
        return isAlive();
    }

    private void updateBounds() {
        bounds.setFrame(x - radius, y - radius, radius * 2, radius * 2);
        boundingBox.setRect(x - radius, y - radius, radius * 2, radius * 2);
    }

    public boolean isAlive() { return !consumed && age < LIFETIME_FRAMES; }
    public boolean isPiercing() { return piercingLevel > 0; }
    public int getPiercingLevel() { return piercingLevel; }
    public int getEnlargementLevel() { return enlargementLevel; }
    public int getRadius() { return radius; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocityX() { return vx; }
    public double getVelocityY() { return vy; }
    public int getAge() { return age; }
    public Ellipse2D.Double getBounds() { return bounds; }
    public Rectangle2D.Double getBoundingBox() { return boundingBox; }

    public void draw(Graphics2D g2d) {
        g2d.setColor(COLORS[enlargementLevel]);
        g2d.fillOval((int) Math.round(x - radius), (int) Math.round(y - radius),
                radius * 2, radius * 2);
    }
}
