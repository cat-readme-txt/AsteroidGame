import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class Bullet {
    private static final int LIFETIME_FRAMES = 3 * 60;
    private static final int TRAIL_LENGTH = 7;
    private static final Color GLOW_COLOR = new Color(255, 255, 0, 100);
    private static final Color[] TRAIL_COLORS = new Color[TRAIL_LENGTH];
    static {
        for (int i = 0; i < TRAIL_LENGTH; i++) {
            TRAIL_COLORS[i] = new Color(255, 255, 100, (int)(150 * (i + 1) / 8.0f));
        }
    }

    private double x, y;
    private final double vx, vy;
    private final int radius = 1;
    private int age;
    private final double[] trailX = new double[TRAIL_LENGTH];
    private final double[] trailY = new double[TRAIL_LENGTH];
    private int trailNext, trailCount;
    private final Ellipse2D.Double bounds = new Ellipse2D.Double();
    private final Rectangle2D.Double boundingBox = new Rectangle2D.Double();

    public Bullet(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        vx = 10 * Math.sin(angle);
        vy = -10 * Math.cos(angle);
        updateBounds();
    }

    public void update(int universeWidth, int universeHeight) {
        if (!isAlive()) return;
        age++;
        x += vx;
        y += vy;

        // Continue wrapping during the three-second simulation lifetime.
        if (x < 0) x += universeWidth;
        else if (x > universeWidth) x -= universeWidth;
        if (y < 0) y += universeHeight;
        else if (y > universeHeight) y -= universeHeight;

        trailX[trailNext] = x;
        trailY[trailNext] = y;
        trailNext = (trailNext + 1) % TRAIL_LENGTH;
        if (trailCount < TRAIL_LENGTH) trailCount++;
        updateBounds();
    }

    public boolean isAlive() { return age < LIFETIME_FRAMES; }

    private void updateBounds() {
        bounds.setFrame(x - radius, y - radius, radius * 2, radius * 2);
        boundingBox.setRect(x - radius, y - radius, radius * 2, radius * 2);
    }

    // Read-only to callers; updated in place when the bullet moves.
    public Ellipse2D.Double getBounds() { return bounds; }
    public Rectangle2D.Double getBoundingBox() { return boundingBox; }

    public void draw(Graphics2D g2d) {
        for (int i = 0; i < trailCount; i++) {
            int index = (trailNext - trailCount + i + TRAIL_LENGTH) % TRAIL_LENGTH;
            int lifetime = 8 - trailCount + i;
            float alpha = lifetime / 8.0f;
            g2d.setColor(TRAIL_COLORS[lifetime - 1]);
            int size = (int)(4 * alpha);
            g2d.fillOval((int)trailX[index] - size/2, (int)trailY[index] - size/2, size, size);
        }
        g2d.setColor(GLOW_COLOR);
        g2d.fillOval((int)(x - radius - 2), (int)(y - radius - 2), (radius + 2) * 2, (radius + 2) * 2);
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
    }
}
