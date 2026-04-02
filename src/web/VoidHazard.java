import java.awt.*;
import java.util.Random;

public class VoidHazard {
    public enum Type { ORB, TENDRIL, VORTEX }
    
    private static final Random rand = new Random();
    private final Type type;
    private double x, y;
    private double vx, vy;
    private int lifetime = 300;
    private int size;
    private float pulse = 0;
    private float rotation = 0;
    
    public VoidHazard(int screenWidth, int screenHeight, Type type) {
        this.type = type;
        
        // Spawn from edges
        if (rand.nextBoolean()) {
            x = rand.nextBoolean() ? -50 : screenWidth + 50;
            y = rand.nextInt(screenHeight);
        } else {
            x = rand.nextInt(screenWidth);
            y = rand.nextBoolean() ? -50 : screenHeight + 50;
        }
        
        // Move toward center with variation based on type
        double targetX = screenWidth / 2.0;
        double targetY = screenHeight / 2.0;
        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        double speed = switch(type) {
            case ORB -> 1.5 + rand.nextDouble();
            case TENDRIL -> 0.8 + rand.nextDouble() * 0.5;
            case VORTEX -> 2.0 + rand.nextDouble();
        };
        
        vx = (dx / dist) * speed;
        vy = (dy / dist) * speed;
        
        size = switch(type) {
            case ORB -> 20 + rand.nextInt(20);
            case TENDRIL -> 30 + rand.nextInt(15);
            case VORTEX -> 35 + rand.nextInt(20);
        };
    }
    
    public void update() {
        x += vx;
        y += vy;
        pulse += 0.1f;
        rotation += 0.05f;
        lifetime--;
    }
    
    public boolean isAlive() {
        return lifetime > 0;
    }
    
    public boolean intersects(Polygon shipBounds) {
        int checkRadius = size;
        return shipBounds.contains(x, y) || 
               isNearShip(shipBounds, checkRadius);
    }
    
    private boolean isNearShip(Polygon shipBounds, int radius) {
        for (int i = 0; i < shipBounds.npoints; i++) {
            double dx = shipBounds.xpoints[i] - x;
            double dy = shipBounds.ypoints[i] - y;
            if (Math.sqrt(dx * dx + dy * dy) < radius) {
                return true;
            }
        }
        return false;
    }
    
    public void draw(Graphics2D g2d, boolean voidToggled) {
        switch(type) {
            case ORB -> drawOrb(g2d, voidToggled);
            case TENDRIL -> drawTendril(g2d, voidToggled);
            case VORTEX -> drawVortex(g2d, voidToggled);
        }
    }
    
    private void drawOrb(Graphics2D g2d, boolean voidToggled) {
        int pulseSize = (int)(size + 8 * Math.sin(pulse));
        int baseAlpha = voidToggled ? 255 : 40;
        
        // Multi-layer threatening orb
        for (int layer = 3; layer >= 0; layer--) {
            int layerSize = pulseSize + layer * 15;
            int alpha = (baseAlpha / (layer + 1)) + (int)(30 * Math.sin(pulse * 2 + layer));
            alpha = Math.max(0, Math.min(255, alpha));
            g2d.setColor(new Color(200, 0, 255, alpha));
            g2d.fillOval((int)x - layerSize/2, (int)y - layerSize/2, layerSize, layerSize);
        }
        
        // Rotating spikes
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI * i / 8) + rotation;
            int spikeLength = (int)(size * 1.5);
            int innerRadius = size / 2;
            
            int x1 = (int)(x + innerRadius * Math.cos(angle));
            int y1 = (int)(y + innerRadius * Math.sin(angle));
            int x2 = (int)(x + spikeLength * Math.cos(angle));
            int y2 = (int)(y + spikeLength * Math.sin(angle));
            
            g2d.setStroke(new BasicStroke(4));
            g2d.setColor(new Color(255, 0, 200, voidToggled ? 200 : 40));
            g2d.drawLine(x1, y1, x2, y2);
            
            // Spike tips
            g2d.fillOval(x2 - 5, y2 - 5, 10, 10);
        }
        
        // Pulsing core
        int coreSize = (int)(size * 0.4 + size * 0.2 * Math.sin(pulse * 3));
        g2d.setColor(new Color(255, 100, 255, voidToggled ? 255 : 40));
        g2d.fillOval((int)x - coreSize/2, (int)y - coreSize/2, coreSize, coreSize);
        
        g2d.setStroke(new BasicStroke(1));
    }

    private void drawTendril(Graphics2D g2d, boolean voidToggled) {
        int baseAlpha = voidToggled ? 255 : 40;
        int segments = 10;
        
        // Thicker, more menacing tendrils
        for (int i = 0; i < segments; i++) {
            double segmentAngle = rotation * 2 + (i * 0.4);
            double offsetX = size * 0.4 * Math.sin(segmentAngle);
            double offsetY = size * 0.3 * Math.cos(segmentAngle);
            
            int segX = (int)(x + offsetX - i * 8);
            int segY = (int)(y + offsetY - i * 8);
            
            int segSize = size - i * 4;
            int alpha = Math.min(baseAlpha, baseAlpha - i * (baseAlpha / 15));
            
            // Main body
            g2d.setColor(new Color(180, 0, 255, alpha));
            g2d.fillOval(segX - segSize/2, segY - segSize/2, segSize, segSize);
            
            // Spines along body
            if (i % 2 == 0) {
                double spineAngle = segmentAngle + Math.PI / 2;
                int spineLength = size / 3;
                int sx1 = (int)(segX + spineLength * Math.cos(spineAngle));
                int sy1 = (int)(segY + spineLength * Math.sin(spineAngle));
                int sx2 = (int)(segX + spineLength * Math.cos(spineAngle + Math.PI));
                int sy2 = (int)(segY + spineLength * Math.sin(spineAngle + Math.PI));
                
                g2d.setStroke(new BasicStroke(3));
                g2d.setColor(new Color(255, 0, 200, alpha / 2));
                g2d.drawLine(segX, segY, sx1, sy1);
                g2d.drawLine(segX, segY, sx2, sy2);
            }
        }
        
        // Menacing head with teeth
        g2d.setColor(new Color(255, 50, 200, baseAlpha));
        g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
        
        // Teeth
        for (int i = 0; i < 6; i++) {
            double toothAngle = (i / 6.0 * Math.PI) + rotation;
            int tx = (int)(x + size/3 * Math.cos(toothAngle));
            int ty = (int)(y + size/3 * Math.sin(toothAngle));
            int[] toothX = {tx, tx - 5, tx + 5};
            int[] toothY = {(int)(ty - size/4), ty, ty};
            
            g2d.setColor(new Color(255, 255, 255, baseAlpha));
            g2d.fillPolygon(toothX, toothY, 3);
        }
        
        g2d.setStroke(new BasicStroke(1));
    }

    private void drawVortex(Graphics2D g2d, boolean voidToggled) {
        int baseAlpha = voidToggled ? 255 : 40;
        
        // Faster, more chaotic spiral
        int arms = 5;
        for (int arm = 0; arm < arms; arm++) {
            for (int i = 0; i < 25; i++) {
                double angle = rotation * 3 + (arm * 2 * Math.PI / arms) + (i * 0.25);
                double radius = i * 4 + 10 * Math.sin(pulse + i * 0.2);
                int px = (int)(x + radius * Math.cos(angle));
                int py = (int)(y + radius * Math.sin(angle));
                
                int particleSize = (int)(10 - i * 0.35);
                int testAlpha = Math.max(0, baseAlpha - i * 8);
                int alpha = Math.min(baseAlpha, testAlpha);
                
                g2d.setColor(new Color(200, 0, 255, alpha));
                g2d.fillOval(px - particleSize/2, py - particleSize/2, particleSize, particleSize);
                
                // Add connecting lines for more threatening appearance
                if (i > 0 && voidToggled) {
                    double prevAngle = rotation * 3 + (arm * 2 * Math.PI / arms) + ((i-1) * 0.25);
                    double prevRadius = (i-1) * 4 + 10 * Math.sin(pulse + (i-1) * 0.2);
                    int prevX = (int)(x + prevRadius * Math.cos(prevAngle));
                    int prevY = (int)(y + prevRadius * Math.sin(prevAngle));
                    
                    g2d.setStroke(new BasicStroke(2));
                    g2d.setColor(new Color(150, 0, 255, alpha / 2));
                    g2d.drawLine(prevX, prevY, px, py);
                }
            }
        }
        
        // Menacing central vortex eye
        int eyeSize = (int)(size * 0.6 + size * 0.3 * Math.sin(pulse * 4));
        g2d.setColor(new Color(255, 0, 200, baseAlpha));
        g2d.fillOval((int)x - eyeSize/2, (int)y - eyeSize/2, eyeSize, eyeSize);
        
        // Pupil
        int pupilSize = eyeSize / 3;
        g2d.setColor(new Color(0, 0, 0, baseAlpha));
        g2d.fillOval((int)x - pupilSize/2, (int)y - pupilSize/2, pupilSize, pupilSize);
        
        // Outer threat rings
        for (int i = 0; i < 3; i++) {
            int ringSize = (int)(size * (1.2 + i * 0.4) + 5 * Math.sin(pulse * 2 + i));
            g2d.setColor(new Color(200, 0, 255, baseAlpha / (i + 2)));
            g2d.setStroke(new BasicStroke(3 - i));
            g2d.drawOval((int)x - ringSize/2, (int)y - ringSize/2, ringSize, ringSize);
        }
        
        g2d.setStroke(new BasicStroke(1));
    }
    
    public Type getType() { return type; }
    public double getCenterX() { return x; }
    public double getCenterY() { return y; }
}