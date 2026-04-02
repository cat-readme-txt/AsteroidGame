import java.awt.*;
import java.util.Random;

public class CosmicEntity {
    private static final Random rand = new Random();
    private double x, y;
    private double targetX, targetY;
    private double vx, vy;
    private double speed;
    private int size = 100;
    private int maxHP;
    private int currentHP;
    private Color bodyColor;
    private float pulse = 0;
    private boolean isInVoid = false;
    private float tentaclePhase = 0;
    private int screenWidth, screenHeight;
    
    public CosmicEntity(int screenWidth, int screenHeight, int level) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Vary size (70-150) based on level
        size = 20 + rand.nextInt(131);

        // Speed inversely proportional to size (0.8-4.0)
        speed = 4.2 - (size / 150.0 * 4.0);
        speed = Math.max(0.8, Math.min(4.0, speed));

        // HP based on size
        maxHP = (int)(50 + (size / 150.0) * 500);
        currentHP = maxHP;

        // Color gradient from red (small) to blue (large)
        float sizeRatio = (size - 20) / 130.0f;  // adjust range to 0-1
        int red = (int)(255 * (1 - sizeRatio)) + 55;
        int blue = (int)(255 * sizeRatio) + 55;
        int green = Math.min(red, blue) / 4;  
        red = Math.max(0, Math.min(255, red));
        blue = Math.max(0, Math.min(255, blue));
        green = Math.max(0, Math.min(255, green));
        bodyColor = new Color(red, green, blue);
        
        // Spawn from random edge
        if (rand.nextBoolean()) {
            x = rand.nextBoolean() ? -100 : screenWidth + 100;
            y = rand.nextInt(screenHeight);
        } else {
            x = rand.nextInt(screenWidth);
            y = rand.nextBoolean() ? -100 : screenHeight + 100;
        }
        
        updateTarget(screenWidth / 2.0, screenHeight / 2.0);
    }
    
    public void updateTarget(double newTargetX, double newTargetY) {
        targetX = newTargetX;
        targetY = newTargetY;
        
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            vx = (dx / distance) * speed;
            vy = (dy / distance) * speed;
        }
    }
    
    public void update(double shipX, double shipY, boolean voidActive) {
        // Only chase the ship when NOT in void
        if (!voidActive) {
            updateTarget(shipX, shipY);
            x += vx;
            y += vy;
        }
        // If in void, don't update position
        
        pulse += 0.1f;
        tentaclePhase += 0.05f;
    }

    public void updateWithBoids(double shipX, double shipY, java.util.List<CosmicEntity> neighbors, boolean voidActive) {
        // Only apply boids when NOT in void
        if (voidActive) {
            pulse += 0.1f;
            tentaclePhase += 0.05f;
            return;
        }
        // Tunables
        final double NEIGHBOR_R = 150.0;
        final double SEP_R = 50.0;
        final double NEIGHBOR_R2 = NEIGHBOR_R * NEIGHBOR_R;
        final double SEP_R2 = SEP_R * SEP_R;

        double separationX = 0, separationY = 0;
        double alignmentX = 0, alignmentY = 0;
        double cohesionX = 0, cohesionY = 0;
        int count = 0;

        // Optional: cap checks to reduce worst-case time
        final int MAX_CHECKS = 40;           // try 24–60
        int checked = 0;

        // Optional: stride sampling to avoid checking every neighbor every time
        // If neighbors is dense, this is a big win.
        int step = 1;
        int n = neighbors.size();
        if (n > 120) step = 2;
        if (n > 240) step = 3;

        for (int i = 0; i < n; i += step) {
            CosmicEntity other = neighbors.get(i);
            if (other == this) continue;

            double dx = other.x - x;
            double dy = other.y - y;
            double dist2 = dx * dx + dy * dy;

            if (dist2 < NEIGHBOR_R2) {
                // Separation only if close; needs invDist
                if (dist2 < SEP_R2 && dist2 > 1e-9) {
                    double invDist = 1.0 / Math.sqrt(dist2);
                    separationX -= dx * invDist;
                    separationY -= dy * invDist;
                }

                alignmentX += other.vx;
                alignmentY += other.vy;

                cohesionX += other.x;
                cohesionY += other.y;

                count++;

                if (++checked >= MAX_CHECKS) break;
            }
        }

        if (count > 0) {
            double invCount = 1.0 / count;
            alignmentX *= invCount;
            alignmentY *= invCount;
            cohesionX = (cohesionX * invCount) - x;
            cohesionY = (cohesionY * invCount) - y;
        }

        // Prey on player (avoid sqrt if you can tolerate approximate normalization)
        double preyX = shipX - x;
        double preyY = shipY - y;
        double preyDist2 = preyX * preyX + preyY * preyY;
        if (preyDist2 > 1e-9) {
            double invPreyDist = 1.0 / Math.sqrt(preyDist2);
            preyX *= invPreyDist;
            preyY *= invPreyDist;
        }

        // Combine behaviors
        double ax = (separationX * 1.5 + alignmentX * 1.0 + cohesionX * 0.01 + preyX * 2.0);
        double ay = (separationY * 1.5 + alignmentY * 1.0 + cohesionY * 0.01 + preyY * 2.0);

        // Scale into desired speed band (keeps motion stable)
        vx = ax * speed / 4.0;
        vy = ay * speed / 4.0;

        // Limit speed using squared magnitude (one sqrt only if needed)
        double v2 = vx * vx + vy * vy;
        double s2 = speed * speed;
        if (v2 > s2 && v2 > 1e-12) {
            double invV = 1.0 / Math.sqrt(v2);
            vx *= speed * invV;
            vy *= speed * invV;
        }

        x += vx;
        y += vy;
    }
    
    public boolean isNearShip(Polygon shipBounds) {
        if (isInVoid) return false;
        // Collision at tentacle midpoints (body + 50% of tentacle length)
        int collisionRadius = (int)(size * 0.8);
        
        Rectangle entityBounds = new Rectangle(
            (int)x - collisionRadius/2,
            (int)y - collisionRadius/2,
            collisionRadius,
            collisionRadius
        );
        
        for (int i = 0; i < shipBounds.npoints; i++) {
            if (entityBounds.contains(shipBounds.xpoints[i], shipBounds.ypoints[i])) {
                return true;
            }
        }
        
        for (int i = 0; i < shipBounds.npoints; i++) {
            double dx = shipBounds.xpoints[i] - x;
            double dy = shipBounds.ypoints[i] - y;
            if (Math.sqrt(dx * dx + dy * dy) < collisionRadius/2) {
                return true;
            }
        }
        
        return false;
    }

    public void takeDamage(int damage) {
        currentHP -= damage;
        if (currentHP < 0) currentHP = 0;
    }

    public boolean isDead() {
        return currentHP <= 0;
    }

    public int getSizeValue() {
        if (size < 40) return 1;
        if (size < 80) return 2;
        if (size < 120) return 3;
        return 4;
    }
    
    public void draw(Graphics2D g2d, boolean voidToggled) {
        isInVoid = voidToggled;
        
        if (isInVoid) {
            Composite old = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
            drawCosmicEntityBody(g2d, 40);
            g2d.setComposite(old);
        } else {
            drawCosmicEntityBody(g2d, 255);
            drawHPBar(g2d);
        }
    }

    private void drawCosmicEntityBody(Graphics2D g2d, int alpha) {
        // Main body (eldritch sphere)
        int bodySize = (int)(size + 10 * Math.sin(pulse));
        // Outer aura
        for (int i = 3; i > 0; i--) {
            int auraSize = bodySize + i * 20;
            int adjustedAlpha = 40 - i * 10;
            // g2d.setColor(new Color(55, 0, 155, alpha));
            g2d.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), adjustedAlpha));
            g2d.fillOval((int)x - auraSize/2, (int)y - auraSize/2, auraSize, auraSize);
        }

        // Core body
        GradientPaint bodyGradient = new GradientPaint(
            (int)x - bodySize/2, (int)y - bodySize/2, bodyColor,
            (int)x + bodySize/2, (int)y + bodySize/2, 
            new Color(bodyColor.getRed()/2, bodyColor.getGreen()/2, bodyColor.getBlue()/2)
        );
        g2d.setPaint(bodyGradient);
        g2d.fillOval((int)x - bodySize/2, (int)y - bodySize/2, bodySize, bodySize);
            
        // Multiple eyes
        for (int i = 0; i < 5; i++) {
            double eyeAngle = (i / 5.0 * Math.PI * 2) + pulse;
            int eyeRadius = 30;
            int eyeX = (int)(x + eyeRadius * Math.cos(eyeAngle));
            int eyeY = (int)(y + eyeRadius * Math.sin(eyeAngle));
            
            // Eye white
            g2d.setColor(new Color(255, 200, 200));
            g2d.fillOval(eyeX - 10, eyeY - 10, 20, 20);
            
            // Pupil (looking at center)
            // g2d.setColor(new Color(150, 0, 0));
            g2d.setColor(bodyColor);
            g2d.fillOval(eyeX - 5, eyeY - 5, 10, 10);
            
            // Glint
            g2d.setColor(Color.WHITE);
            g2d.fillOval(eyeX - 2, eyeY - 2, 4, 4);
        }
        
        // Writhing tentacles
        int tentacleCount = 8;
        for (int i = 0; i < tentacleCount; i++) {
            drawTentacle(g2d, i, tentacleCount);
        }
        
        // Pulsing core
        int coreSize = (int)(30 + 10 * Math.sin(pulse * 2));
        int coreAlpha = (int)(200 + 55 * Math.sin(pulse * 3));
        g2d.setColor(new Color(100, 50, 200, coreAlpha));
        g2d.fillOval((int)x - coreSize/2, (int)y - coreSize/2, coreSize, coreSize);

        drawHPBar(g2d);
    }   
    
    private void drawTentacle(Graphics2D g2d, int index, int total) {
        double baseAngle = (index / (double)total) * Math.PI * 2;
        int segments = 8;
        
        g2d.setStroke(new BasicStroke(6));
        
        for (int seg = 0; seg < segments; seg++) {
            double segmentPhase = tentaclePhase + (index * 0.5) + (seg * 0.2);
            double angle = baseAngle + Math.sin(segmentPhase) * 0.5;
            double length = 15 + seg * 8;
            
            int x1 = seg == 0 ? (int)x : 
                (int)(x + (length - 8) * Math.cos(baseAngle + Math.sin(segmentPhase - 0.2) * 0.5));
            int y1 = seg == 0 ? (int)y :
                (int)(y + (length - 8) * Math.sin(baseAngle + Math.sin(segmentPhase - 0.2) * 0.5));
            
            int x2 = (int)(x + length * Math.cos(angle));
            int y2 = (int)(y + length * Math.sin(angle));
            
            int alpha = 200 - seg * 20;
            // g2d.setColor(new Color(100, 0, 80, alpha));
            g2d.setColor(new Color(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), alpha));
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        g2d.setStroke(new BasicStroke(1));
    }

    private void drawHPBar(Graphics2D g2d) {
        if (currentHP == maxHP) return;  // No need to draw full HP
        int barWidth = size;
        int barHeight = 4;
        int barX = (int)x - barWidth/2;
        int barY = (int)y - size/2 - 12;
        
        g2d.setColor(new Color(50, 50, 50, 180));
        g2d.fillRect(barX, barY, barWidth, barHeight);
        
        float hpPercent = (float)currentHP / maxHP;
        int fillWidth = (int)(barWidth * hpPercent);
        
        Color hpColor = hpPercent > 0.5f ? new Color(0, 255, 100) :
                    hpPercent > 0.25f ? new Color(255, 200, 0) :
                    new Color(255, 50, 50);
        
        g2d.setColor(hpColor);
        g2d.fillRect(barX, barY, fillWidth, barHeight);
        
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
}