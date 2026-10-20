import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ShipTrail {
    private static class TrailPoint {
        double x, y;
        int lifetime;
        int maxLifetime;
        
        TrailPoint(double x, double y) {
            this.x = x;
            this.y = y;
            this.maxLifetime = 15;
            this.lifetime = maxLifetime;
        }
        
        void update() {
            lifetime--;
        }
        
        boolean isAlive() {
            return lifetime > 0;
        }
        
        void draw(Graphics2D g2d) {
            float alpha = (float)lifetime / maxLifetime;
            
            g2d.setColor(new Color(180, 0, 255, (int)(150 * alpha)));
            
            int size = (int)(8 * alpha) + 2;
            g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
            
            // Glow
            int glowSize = size + 4;
            int glowAlpha = (int)(100 * alpha);
            g2d.setColor(new Color(200, 100, 255, glowAlpha));
            g2d.fillOval((int)x - glowSize/2, (int)y - glowSize/2, glowSize, glowSize);
        }
    }
    
    private final List<TrailPoint> points = new ArrayList<>();
    public void addPoint(double x, double y) {
        points.add(new TrailPoint(x, y));
    }
    
    public void update() {
        int write = 0;
        for (int read = 0; read < points.size(); read++) {
            TrailPoint p = points.get(read);
            p.update();
            if (p.isAlive()) {
                points.set(write++, p);
            }
        }
        points.subList(write, points.size()).clear();
    }
    
    public void draw(Graphics2D g2d) {
        for (TrailPoint p : points) {
            p.draw(g2d);
        }
    }
    
    public void clear() {
        points.clear();
    }
}
