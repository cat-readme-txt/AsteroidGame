import java.awt.*;
import java.awt.geom.Line2D;

public class VoidLaser {
    private final double startX, startY;
    private final double angle;
    private final int screenWidth, screenHeight;
    private final int lifetime = 10; // frames the laser persists
    private int currentFrame = 0;
    
    public VoidLaser(double startX, double startY, double angle, int screenWidth, int screenHeight) {
        this.startX = startX;
        this.startY = startY;
        this.angle = angle;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
    
    public boolean isAlive() {
        currentFrame++;
        return currentFrame < lifetime;
    }
    
    public Line2D.Double getLaserLine() {
        // Calculate laser endpoint (extends across screen)
        double maxLength = Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight);
        double endX = startX + maxLength * Math.sin(angle);
        double endY = startY - maxLength * Math.cos(angle);
        
        return new Line2D.Double(startX, startY, endX, endY);
    }
    
    public boolean intersects(Polygon polygon) {
        Line2D.Double laser = getLaserLine();
        
        // Check if laser line intersects any edge of the polygon
        for (int i = 0; i < polygon.npoints; i++) {
            int j = (i + 1) % polygon.npoints;
            Line2D.Double edge = new Line2D.Double(
                polygon.xpoints[i], polygon.ypoints[i],
                polygon.xpoints[j], polygon.ypoints[j]
            );
            
            if (laser.intersectsLine(edge)) {
                return true;
            }
        }
        
        // Also check if polygon contains laser start point
        return polygon.contains(startX, startY);
    }
    
    public void draw(Graphics2D g2d) {
        Line2D.Double laser = getLaserLine();
        
        // Draw multiple layers for glow effect
        float alpha = 1.0f - (currentFrame / (float) lifetime);
        
        // Outer glow
        g2d.setColor(new Color(200, 0, 255, (int)(60 * alpha)));
        g2d.setStroke(new BasicStroke(8));
        g2d.draw(laser);
        
        // Middle layer
        g2d.setColor(new Color(220, 100, 255, (int)(120 * alpha)));
        g2d.setStroke(new BasicStroke(4));
        g2d.draw(laser);
        
        // Core
        g2d.setColor(new Color(255, 200, 255, (int)(255 * alpha)));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(laser);
        
        // Reset stroke
        g2d.setStroke(new BasicStroke(1));
    }
}