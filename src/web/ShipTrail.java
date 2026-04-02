import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ShipTrail {
    private static class TrailPoint {
        double x, y;
        int lifetime;
        int maxLifetime;
        boolean isRainbow;
        float hueOffset;
        
        TrailPoint(double x, double y, boolean isRainbow, float hueOffset) {
            this.x = x;
            this.y = y;
            this.isRainbow = isRainbow;
            this.hueOffset = hueOffset;
            this.maxLifetime = isRainbow ? 20 : 15;
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
            
            if (isRainbow) {
                float hue = (System.currentTimeMillis() / 1000.0f + hueOffset) % 1.0f;
                Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(200 * alpha)));
            } else {
                g2d.setColor(new Color(180, 0, 255, (int)(150 * alpha)));
            }
            
            int size = (int)(8 * alpha) + 2;
            g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
            
            // Glow
            int glowSize = size + 4;
            int glowAlpha = (int)(100 * alpha);
            if (isRainbow) {
                float hue = (System.currentTimeMillis() / 1000.0f + hueOffset) % 1.0f;
                Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), glowAlpha));
            } else {
                g2d.setColor(new Color(200, 100, 255, glowAlpha));
            }
            g2d.fillOval((int)x - glowSize/2, (int)y - glowSize/2, glowSize, glowSize);
        }
    }
    
    private final List<TrailPoint> points = new ArrayList<>();
    private float hueCounter = 0;
    
    public void addPoint(double x, double y, boolean isHyper, boolean isVoid) {
        if (isHyper) {
            points.add(new TrailPoint(x, y, true, hueCounter));
            hueCounter += 0.05f;
        } else if (isVoid) {
            points.add(new TrailPoint(x, y, false, 0));
        }
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