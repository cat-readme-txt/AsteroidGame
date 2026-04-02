import java.awt.*;
import java.awt.geom.Point2D;

public class BlackHole {
    private double x, y;
    private int size;
    private int spawnTimer = 1 * 60;
    private float rotation = 0;
    private float pulse = 0;
    private final double pullStrength = 1;
    private final double pullRadius;
    private final double eventHorizon;
    private boolean fullySpawned = false;
    
    public BlackHole(double x, double y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.pullRadius = size * 3.75;
        this.eventHorizon = size / 2.0;
    }
    
    public void update() {
        if (spawnTimer > 0) {
            spawnTimer--;
            if (spawnTimer == 0) fullySpawned = true;
        }
        rotation += 0.08f;
        pulse += 0.05f;
    }
    
    public Point2D.Double applyGravity(double shipX, double shipY) {
        if (!fullySpawned) return new Point2D.Double(0, 0);
        double dx = x - shipX;
        double dy = y - shipY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < pullRadius && distance > eventHorizon) {
            // Inverse square gravity
            double strength = pullStrength * (pullRadius - distance) / pullRadius;
            double forceX = (dx / distance) * strength;
            double forceY = (dy / distance) * strength;
            return new Point2D.Double(forceX, forceY);
        }
        
        return new Point2D.Double(0, 0);
    }
    
    public boolean isShipConsumed(double shipX, double shipY) {
        if (!fullySpawned) return false;
        double dx = x - shipX;
        double dy = y - shipY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < eventHorizon;
    }
    
    public void draw(Graphics2D g2d) {
        // Spawn Animation
        float spawnScale = fullySpawned ? 1.0f : (60 - spawnTimer) / 60.0f;
        if (!fullySpawned) {
            int alpha = (int)(255 * (1 - spawnScale));
            g2d.setColor(new Color(150, 100, 255, alpha));
            int spawnSize = (int)(size * 2 * spawnScale);
            g2d.fillOval((int)x - spawnSize, (int)y - spawnSize, spawnSize * 2, spawnSize * 2);
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, spawnScale));
        
        // Gravitational lensing rings (distorted light)
        for (int i = 6; i > 0; i--) {
            int radius = (int)(size * 0.5) + i * (size / 6);
            int alpha = 25 - i * 3;
            
            // Blueish light being lensed
            g2d.setColor(new Color(100, 150, 255, alpha));
            g2d.setStroke(new BasicStroke(2 + i * 0.5f));
            g2d.drawOval((int)x - radius, (int)y - radius, radius * 2, radius * 2);
        }
        
        // Accretion disk - flattened rotating disk
        g2d.setStroke(new BasicStroke(1));
        for (int layer = 0; layer < 4; layer++) {
            int numParticles = 30 + layer * 10;
            for (int i = 0; i < numParticles; i++) {
                double angle = (i / (double)numParticles * Math.PI * 2) + rotation + (layer * 0.2);
                int diskRadius = (int)(size * 0.7) + layer * (size / 8);
                
                // Elliptical orbit (flattened disk)
                int px = (int)(x + diskRadius * Math.cos(angle));
                int py = (int)(y + diskRadius * Math.sin(angle) * 0.25); // Heavily flattened
                
                // Particles get redder as they approach (redshift)
                float distFactor = 1.0f - (layer / 4.0f);
                int red = (int)(255 * distFactor);
                int green = (int)(180 * distFactor);
                int blue = (int)(50 + 100 * (1 - distFactor));
                int alpha = (int)(180 - layer * 30 + 30 * Math.sin(pulse + i * 0.1));
                
                Color diskColor = new Color(red, green, blue, Math.max(0, Math.min(255, alpha)));
                
                int particleSize = 3 + (3 - layer);
                g2d.setColor(diskColor);
                g2d.fillOval(px - particleSize/2, py - particleSize/2, particleSize, particleSize);
            }
        }
        
        // Photon sphere (light orbiting the black hole)
        int photonRadius = (int)(size * 0.6);
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0 * Math.PI * 2) + rotation * 3;
            int px = (int)(x + photonRadius * Math.cos(angle));
            int py = (int)(y + photonRadius * Math.sin(angle) * 0.3);
            
            int alpha = (int)(150 + 50 * Math.sin(pulse * 2 + i));
            g2d.setColor(new Color(255, 255, 200, alpha));
            g2d.fillOval(px - 2, py - 2, 4, 4);
        }
        
        // Event horizon - pure black with subtle edge glow
        g2d.setColor(Color.BLACK);
        int horizonSize = (int)(size * 0.5);
        g2d.fillOval((int)x - horizonSize, (int)y - horizonSize, horizonSize * 2, horizonSize * 2);
        
        // Event horizon edge glow (Hawking radiation)
        int glowAlpha = (int)(100 + 50 * Math.sin(pulse));
        RadialGradientPaint horizonGlow = new RadialGradientPaint(
            (float)x, (float)y,
            horizonSize * 1.2f,
            new float[]{0.85f, 1.0f},
            new Color[]{
                new Color(0, 0, 0, 0),
                new Color(150, 200, 255, glowAlpha)
            }
        );
        g2d.setPaint(horizonGlow);
        g2d.fillOval(
            (int)x - (int)(horizonSize * 1.2), 
            (int)y - (int)(horizonSize * 1.2), 
            (int)(horizonSize * 2.4), 
            (int)(horizonSize * 2.4)
        );
        
        // Hawking radiation particles escaping
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0 * Math.PI * 2) + pulse * 2;
            int escapeRadius = (int)(horizonSize + 15 + 8 * Math.sin(pulse + i));
            int px = (int)(x + escapeRadius * Math.cos(angle));
            int py = (int)(y + escapeRadius * Math.sin(angle));
            
            int alpha = (int)(150 + 50 * Math.sin(pulse * 2 + i));
            g2d.setColor(new Color(200, 220, 255, alpha));
            g2d.fillOval(px - 2, py - 2, 4, 4);
        }
        
        g2d.setStroke(new BasicStroke(1));
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
}