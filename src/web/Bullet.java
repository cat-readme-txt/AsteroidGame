import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

public class Bullet {
    private static class BulletTrailPoint {
        double x, y;
        int lifetime = 8;
        
        BulletTrailPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
    private double x, y;      // Position
    private final double angle;     // Direction in radians
    private final double speed = 10; // Speed of the bullet
    private final int radius = 1; // Size of the bullet
    private final List<BulletTrailPoint> trail = new ArrayList<>();

    public Bullet(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public void update(int universeWidth, int universeHeight) {
        // Move bullet in direction of angle
        x += speed * Math.sin(angle);
        y -= speed * Math.cos(angle);

        // Loop universe boundaries
        if (x < 0) x += universeWidth;
        else if (x > universeWidth) x -= universeWidth;

        if (y < 0) y += universeHeight;
        else if (y > universeHeight) y -= universeHeight;
        
        // Update trail
        trail.add(new BulletTrailPoint(x, y));
        trail.removeIf(p -> --p.lifetime <= 0);
    }

    public Ellipse2D.Double getBounds() {
        return new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2);
    }

    public void draw(Graphics2D g2d) {
        // Draw trail
        for (int i = 0; i < trail.size(); i++) {
            BulletTrailPoint p = trail.get(i);
            float alpha = p.lifetime / 8.0f;
            g2d.setColor(new Color(255, 255, 100, (int)(150 * alpha)));
            int size = (int)(4 * alpha);
            g2d.fillOval((int)p.x - size/2, (int)p.y - size/2, size, size);
        }
        // Draw main bullet with glow
        g2d.setColor(new Color(255, 255, 0, 100));
        g2d.fillOval((int)(x - radius - 2), (int)(y - radius - 2), (radius + 2) * 2, (radius + 2) * 2);
        
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
    }
}
