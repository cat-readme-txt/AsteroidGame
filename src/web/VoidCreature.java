import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.util.Random;

public class VoidCreature {
    private static class BodySegment {
        double x, y;
        double angle;
        
        BodySegment(double x, double y) {
            this.x = x;
            this.y = y;
            this.angle = 0;
        }
    }
    private static final Random rand = new Random();
    
    public enum CreatureType { CENTIPEDE, SPECTER }
    
    private CreatureType type;
    private double x, y;
    private double vx, vy;
    private int size;
    private double speed;
    private int maxHP;
    private int currentHP;
    private float animPhase = 0;
    private float limbPhase = 0;
    private boolean isInVoid;
    private java.util.List<BodySegment> segments;
    private double targetAngle = 0;
    private boolean exitingVoid = false;
    private int maxSegmentCount;
    private int screenW, screenH;
    private static final double SEGMENT_SPACING = 12.0;
    private double distSinceLastSegment = 0.0;
    private double desiredAngle = 0.0;

    // radians per update (tune this)
    private static final double MAX_TURN_RATE = Math.toRadians(2.5);  // ~2.5° per frame
    // optional: cap random direction change frequency
    private static final int DIR_CHANGE_COOLDOWN_FRAMES = 45;
    private int dirCooldown = 0;
    
    private VoidCreature(int screenWidth, int screenHeight, int level) {
        this.screenW = screenWidth;
        this.screenH = screenHeight;
        
        // Spawn from edges
        if (rand.nextBoolean()) {
            x = rand.nextBoolean() ? -50 : screenWidth + 50;
            y = rand.nextInt(screenHeight);
        } else {
            x = rand.nextInt(screenWidth);
            y = rand.nextBoolean() ? -50 : screenHeight + 50;
        }
        
        updateVelocity(screenWidth / 2.0, screenHeight / 2.0);
    }

    public static VoidCreature createCentipede(int screenWidth, int screenHeight, int level) {
        VoidCreature vc = new VoidCreature(screenWidth, screenHeight, level);
        vc.type = CreatureType.CENTIPEDE;
        vc.size = 80 + vc.rand.nextInt(41);
        vc.speed = 1.2 + vc.rand.nextDouble() * 0.8;
        vc.maxHP = 200 + vc.size * 2;
        vc.currentHP = vc.maxHP;
        
        // Set random max segment count
        vc.maxSegmentCount = 100 + vc.rand.nextInt(51);
        
        // Initialize with just the head
        vc.segments = new java.util.ArrayList<>();
        
        // Determine spawn edge and initial direction
        int edge = vc.rand.nextInt(4); // 0=left, 1=right, 2=top, 3=bottom
        double startX, startY, initAngle;
        
        switch(edge) {
            case 0 -> {
                // Left edge
                startX = -5;
                startY = vc.rand.nextInt(screenHeight);
                initAngle = 0; // Move right
            }
            case 1 -> {
                // Right edge
                startX = screenWidth + 5;
                startY = vc.rand.nextInt(screenHeight);
                initAngle = Math.PI; // Move left
            }
            case 2 -> {
                // Top edge
                startX = vc.rand.nextInt(screenWidth);
                startY = -5;
                initAngle = Math.PI / 2; // Move down
            }
            default -> {
                // Bottom edge
                startX = vc.rand.nextInt(screenWidth);
                startY = screenHeight + 5;
                initAngle = -Math.PI / 2; // Move up
            }
        }
        
        vc.x = startX;
        vc.y = startY;
        vc.targetAngle = initAngle;
        vc.desiredAngle = initAngle;
        
        // Create only the head initially
        BodySegment head = new BodySegment(startX, startY);
        head.angle = initAngle;
        vc.segments.add(head);
        
        return vc;
    }

    public static VoidCreature createSpecter(int screenWidth, int screenHeight, int level) {
        VoidCreature vc = new VoidCreature(screenWidth, screenHeight, level);
        vc.type = CreatureType.SPECTER;
        vc.size = 30 + vc.rand.nextInt(71);
        vc.speed = 3.5 - (vc.size / 100.0 * 3.0);
        vc.speed = Math.max(0.5, Math.min(3.0, vc.speed));
        vc.maxHP = (int)(50 + (vc.size / 100.0) * 250);
        vc.currentHP = vc.maxHP;
        float sizeRatio = (vc.size - 30) / 70.0f;
        
        return vc;
    }
    
    public void updateVelocity(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        if (dist > 0) {
            vx = (dx / dist) * speed;
            vy = (dy / dist) * speed;
        }
    }
    
    public void update(double shipX, double shipY, boolean voidActive) {
        if (type == CreatureType.CENTIPEDE) {
            updateCentipede();
        } else {
            // Specters only chase when void is active
            if (voidActive) {
                updateVelocity(shipX, shipY);
                x += vx;
                y += vy;
            }
        }
        animPhase += 0.1f;
        limbPhase += 0.1f;
    }

    private void updateCentipede() {
        if (segments == null || segments.isEmpty()) return;
        // Random direction changes (only when not exiting)
        if (!exitingVoid) {
            if (dirCooldown > 0) dirCooldown--;

            // Occasionally pick a new desired angle (but don’t instantly snap)
            if (dirCooldown == 0 && rand.nextInt(90) == 0) {
                // Small-ish change instead of fully random (looks more organic)
                double jitter = rand.nextDouble() * Math.toRadians(60); // +/- 60°
                desiredAngle = wrapAngle(desiredAngle + jitter);

                dirCooldown = DIR_CHANGE_COOLDOWN_FRAMES;
            }
        }
        // Smoothly steer targetAngle toward desiredAngle
        double turnRate = exitingVoid ? MAX_TURN_RATE * 1.8 : MAX_TURN_RATE;
        targetAngle = turnToward(targetAngle, desiredAngle, turnRate);
        
        // Move head
        double headSpeed = exitingVoid ? speed * 2 : speed;
        double oldHeadX = segments.get(0).x;
        double oldHeadY = segments.get(0).y;
        
        double newHeadX = oldHeadX + headSpeed * Math.cos(targetAngle);
        double newHeadY = oldHeadY + headSpeed * Math.sin(targetAngle);
        
        // Update head position
        segments.get(0).x = newHeadX;
        segments.get(0).y = newHeadY;
        
        // Calculate head angle
        double dx = newHeadX - oldHeadX;
        double dy = newHeadY - oldHeadY;
        if (Math.abs(dx) > 0.1 || Math.abs(dy) > 0.1) {
            segments.get(0).angle = Math.atan2(dy, dx);
        }
        
        // Wrap head if it crosses screen boundary
        if (!exitingVoid) wrapSegment(segments.get(0));

        // Accumulate head travel distance (how far the head moved this frame)
        double headMoveDist = Math.sqrt(dx * dx + dy * dy);
        distSinceLastSegment += headMoveDist;

        // Grow body while entering / roaming (not exiting)
        if (!exitingVoid && segments.size() < maxSegmentCount) {

            // Add as many as needed to maintain spacing
            while (distSinceLastSegment >= SEGMENT_SPACING && segments.size() < maxSegmentCount) {

                // Insert new segment at the previous head position (or slightly behind it)
                BodySegment newSeg = new BodySegment(oldHeadX, oldHeadY);
                newSeg.angle = segments.get(0).angle;

                // Insert right after head so the chain starts forming immediately
                segments.add(1, newSeg);

                distSinceLastSegment -= SEGMENT_SPACING;
            }
        }
        
        // Update body segments to follow
        for (int i = 1; i < segments.size(); i++) {
            BodySegment current = segments.get(i);
            BodySegment prev = segments.get(i - 1);
            
            dx = prev.x - current.x;
            dy = prev.y - current.y;
            
            // Handle wrapping discontinuity
            if (Math.abs(dx) > screenW / 2) {
                dx = dx > 0 ? dx - screenW : dx + screenW;
            }
            if (Math.abs(dy) > screenH / 2) {
                dy = dy > 0 ? dy - screenH : dy + screenH;
            }
            
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            if (dist > 0.1) {
                double ratio = SEGMENT_SPACING / dist;
                current.x = prev.x - dx * ratio;
                current.y = prev.y - dy * ratio;
                current.angle = Math.atan2(dy, dx);
                
                // Wrap this segment if needed
                if (!exitingVoid) wrapSegment(current);
            }
        }
        
        // Update centipede position to head
        x = segments.get(0).x;
        y = segments.get(0).y;
        
        // Remove tail segments that go offscreen when exiting
        if (exitingVoid) {
            segments.removeIf(seg -> 
                seg.x < -100 || seg.x > screenW + 100 || 
                seg.y < -100 || seg.y > screenH + 100
            );
        }
    }

    private static double wrapAngle(double a) {
        // Normalize to (-PI, PI]
        while (a <= -Math.PI) a += 2 * Math.PI;
        while (a > Math.PI) a -= 2 * Math.PI;
        return a;
    }

    private static double turnToward(double current, double target, double maxStep) {
        double delta = wrapAngle(target - current);
        if (delta > maxStep) delta = maxStep;
        else if (delta < -maxStep) delta = -maxStep;
        return wrapAngle(current + delta);
    }
    
    private void wrapSegment(BodySegment seg) {
        if (seg.x < 0) seg.x += screenW;
        else if (seg.x > screenW) seg.x -= screenW;
        
        if (seg.y < 0) seg.y += screenH;
        else if (seg.y > screenH) seg.y -= screenH;
    }

    public void startExitingVoid() {
        if (type != CreatureType.CENTIPEDE) return;
        if (exitingVoid) return;

        exitingVoid = true;

        // Head towards nearest screen edge
        double toLeft = x;
        double toRight = screenW - x;
        double toTop = y;
        double toBottom = screenH - y;

        double min = Math.min(Math.min(toLeft, toRight), Math.min(toTop, toBottom));

        if (min == toLeft) targetAngle = Math.PI;
        else if (min == toRight) targetAngle = 0;
        else if (min == toTop) targetAngle = -Math.PI / 2;
        else targetAngle = Math.PI / 2;
    }

    public boolean isFullyOffscreen() {
        if (type != CreatureType.CENTIPEDE) return true;
        
        // Centipede is fully gone when all segments are deleted
        return segments.isEmpty();
    }
    
    public void takeDamage(int damage) {
        currentHP -= damage;
        if (currentHP < 0) currentHP = 0;
    }
    
    public boolean isDead() {
        return currentHP <= 0;
    }
    
    public boolean intersects(Polygon shipBounds) {
        if (!isInVoid) return false;
        
        if (type == CreatureType.CENTIPEDE) {
            for (BodySegment seg : segments) {
                for (int i = 0; i < shipBounds.npoints; i++) {
                    double dx = shipBounds.xpoints[i] - seg.x;
                    double dy = shipBounds.ypoints[i] - seg.y;
                    if (Math.sqrt(dx * dx + dy * dy) < 8) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        // Specter collision
        int collisionRadius = (int)(size * 0.7);
        for (int i = 0; i < shipBounds.npoints; i++) {
            double dx = shipBounds.xpoints[i] - x;
            double dy = shipBounds.ypoints[i] - y;
            if (Math.sqrt(dx * dx + dy * dy) < collisionRadius/2) {
                return true;
            }
        }
        return false;
    }
    
    public void draw(Graphics2D g2d, boolean voidMode) {
        isInVoid = voidMode;
        
        if (!voidMode) {
            drawGhostly(g2d);
        } else {
            drawActive(g2d);
        }
    }
    
    private void drawGhostly(Graphics2D g2d) {
        Composite old = g2d.getComposite();
        
        if (type == CreatureType.CENTIPEDE) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            drawCentipede(g2d, 80);
        } else {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
            drawSpecter(g2d, 40);
        }
        
        g2d.setComposite(old);
    }
    
    private void drawActive(Graphics2D g2d) {
        switch (type) {
            case CENTIPEDE -> drawCentipede(g2d, 255);
            case SPECTER -> drawSpecter(g2d, 255);
        }
        
        if (type != CreatureType.CENTIPEDE) {
            drawHPBar(g2d);
        }
    }

    private void drawSpecter(Graphics2D g2d, int baseAlpha) {
        // Jellyfish/ghost hybrid - magenta/pink
        int bodySize = (int)(size * 1.2);
        
        // Pulsing bell/dome
        double pulseAmount = 0.15 * Math.sin(animPhase * 2);
        int bellHeight = (int)(bodySize * (0.6 + pulseAmount));
        
        // Bell gradient - magenta to pink
        int bellRed = Math.max(0, Math.min(255, 200));
        int bellGreen = Math.max(0, Math.min(255, 50));
        int bellBlue = Math.max(0, Math.min(255, 180));
        int bellAlpha = Math.max(0, Math.min(255, baseAlpha / 3));
        
        g2d.setColor(new Color(bellRed, bellGreen, bellBlue, bellAlpha));
        g2d.fillArc((int)x - bodySize/2, (int)y - bellHeight/2, bodySize, bellHeight, 0, 180);
        
        // Bioluminescent spots
        for (int i = 0; i < 4; i++) { // Reduced from 6
            double angle = (2 * Math.PI * i / 4) + animPhase;
            int spotX = (int)(x + (bodySize/3) * Math.cos(angle));
            int spotY = (int)(y - bellHeight/4 + (bodySize/4) * Math.sin(angle));
            int spotAlpha = Math.max(0, Math.min(255, (int)(baseAlpha * (0.7 + 0.3 * Math.sin(animPhase * 2 + i)))));
            
            int spotRed = Math.max(0, Math.min(255, 255));
            int spotGreen = Math.max(0, Math.min(255, 100));
            int spotBlue = Math.max(0, Math.min(255, 230));
            
            g2d.setColor(new Color(spotRed, spotGreen, spotBlue, spotAlpha));
            g2d.fillOval(spotX - 4, spotY - 4, 8, 8);
        }
        
        // Flowing tentacles - reduced complexity
        int tentacles = 8; 
        for (int i = 0; i < tentacles; i++) {
            double baseAngle = (2 * Math.PI * i / tentacles);
            
            // Segmented tentacles
            for (int seg = 0; seg < 5; seg++) {
                double segPhase = limbPhase + i * 0.4 + seg * 0.3;
                double sway = Math.sin(segPhase) * 0.4;
                double angle = baseAngle + sway;
                
                double baseRadius = bodySize * 0.4;
                double length = baseRadius + seg * (size / 5.0);
                
                int tx = (int)(x + length * Math.cos(angle));
                int ty = (int)(y + bellHeight/2 + length * Math.sin(angle) * 0.3);
                
                int segSize = Math.max(2, size / (6 + seg * 2));
                int alpha = Math.max(0, Math.min(255, baseAlpha - seg * (baseAlpha / 5)));
                
                int tentRed = Math.max(0, Math.min(255, 180));
                int tentGreen = Math.max(0, Math.min(255, 40));
                int tentBlue = Math.max(0, Math.min(255, 160));
                
                g2d.setColor(new Color(tentRed, tentGreen, tentBlue, alpha));
                g2d.fillOval(tx - segSize/2, ty - segSize/2, segSize, segSize);
            }
        }
        
        // Pulsing core
        int coreSize = (int)(size * 0.5 + size * 0.15 * Math.sin(animPhase * 3));
        int coreRed = Math.max(0, Math.min(255, 255));
        int coreGreen = Math.max(0, Math.min(255, 150));
        int coreBlue = Math.max(0, Math.min(255, 255));
        int coreAlpha = Math.max(0, Math.min(255, baseAlpha));
        
        g2d.setColor(new Color(coreRed, coreGreen, coreBlue, coreAlpha));
        g2d.fillOval((int)x - coreSize/2, (int)y - coreSize/2, coreSize, coreSize);
    }

    private void drawCentipede(Graphics2D g2d, int baseAlpha) {
        if (segments == null || segments.isEmpty()) return;
        
        // Draw from tail to head so head appears on top
        for (int i = segments.size() - 1; i >= 0; i--) {
            BodySegment seg = segments.get(i);
            
            // Save transform
            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(seg.x, seg.y);
            g2d.rotate(seg.angle);
            
            // Calculate segment size (larger in middle)
            double segmentRatio = 1.0 - Math.abs((i / (double)segments.size()) - 0.5) * 0.3;
            int segWidth = (int)(20 * segmentRatio);  // Increased from 16
            int segLength = 16;  // Increased from 14
            
            int alpha = Math.max(0, Math.min(255, baseAlpha));
            
            // Draw legs FIRST (so they appear under the body)
            if (i > 0 && i % 3 == 0) {  // Draw legs every 3 segments
                double legPhase = limbPhase + i * 0.4;
                
                // Left side legs
                drawLongerTopDownLeg(g2d, -segWidth/2 - 2, -4, legPhase, true, alpha);
                drawLongerTopDownLeg(g2d, -segWidth/2 - 2, 4, legPhase + 0.5, true, alpha);
                
                // Right side legs
                drawLongerTopDownLeg(g2d, segWidth/2 + 2, -4, legPhase + Math.PI, false, alpha);
                drawLongerTopDownLeg(g2d, segWidth/2 + 2, 4, legPhase + Math.PI + 0.5, false, alpha);
            }
            
            // Main body segment with darker colors
            Color segmentColor = new Color(
                Math.max(0, Math.min(255, 45 + (i % 2) * 8)),
                Math.max(0, Math.min(255, 35 + (i % 2) * 6)),
                Math.max(0, Math.min(255, 55 + (i % 2) * 10)),
                alpha
            );
            g2d.setColor(segmentColor);
            g2d.fillOval(-segWidth/2, -segLength/2, segWidth, segLength);
            
            // Minimal purple accent stripes (every 10th segment)
            if (i % 10 == 0) {
                Color accentColor = new Color(90, 40, 120, alpha / 2);
                g2d.setColor(accentColor);
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawArc(-segWidth/2 + 2, -segLength/2 + 2, segWidth - 4, segLength - 4, 0, 360);
            }
            
            // Exoskeleton plates
            Color plateColor = new Color(
                Math.max(0, Math.min(255, 65)),
                Math.max(0, Math.min(255, 55)),
                Math.max(0, Math.min(255, 75)),
                alpha
            );
            g2d.setColor(plateColor);
            g2d.setStroke(new BasicStroke(1.5f));
            
            // Outer shell edge
            g2d.drawOval(-segWidth/2, -segLength/2, segWidth, segLength);
            
            // Inner segment lines for texture
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawArc(-segWidth/2 + 2, -segLength/2 + 2, segWidth - 4, segLength - 4, 0, 180);
            g2d.drawArc(-segWidth/2 + 2, -segLength/2 + 2, segWidth - 4, segLength - 4, 180, 180);
            
            // Joint line between segments
            if (i < segments.size() - 1) {
                g2d.setColor(new Color(30, 25, 40, alpha));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawLine(-segWidth/2, -segLength/2 + 1, segWidth/2, -segLength/2 + 1);
            }
            
            // Restore transform
            g2d.setTransform(oldTransform);
        }
        
        // Draw head separately
        BodySegment head = segments.get(0);
        
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(head.x, head.y);
        g2d.rotate(head.angle);
        
        // Head body - larger
        g2d.setColor(new Color(55, 45, 65, baseAlpha));
        g2d.fillOval(-14, -12, 28, 24);
        
        // Subtle purple glow on head
        g2d.setColor(new Color(100, 50, 130, baseAlpha / 3));
        g2d.fillOval(-16, -14, 32, 28);
        
        // Head exoskeleton ridges
        g2d.setColor(new Color(75, 65, 85, baseAlpha));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(-14, -12, 28, 24);
        g2d.drawArc(-12, -10, 24, 20, 20, 140);
        
        // Mandibles (same as before)
        int[][] mouthparts = {
            {-8, 10, -12, 18},
            {-4, 10, -7, 16},
            {4, 10, 7, 16},
            {8, 10, 12, 18}
        };
        
        g2d.setColor(new Color(85, 75, 95, baseAlpha));
        g2d.setStroke(new BasicStroke(2f));
        for (int[] mp : mouthparts) {
            g2d.drawLine(mp[0], mp[1], mp[2], mp[3]);
            int[] tipX = {mp[2], mp[2] - 2, mp[2] + 2};
            int[] tipY = {mp[3] + 2, mp[3], mp[3]};
            g2d.fillPolygon(tipX, tipY, 3);
        }
        
        // Antennae (same as before)
        for (int i = 0; i < 4; i++) {
            double tentacleBaseAngle = (i < 2) ? -Math.PI/4 : Math.PI/4;
            if (i % 2 == 1) tentacleBaseAngle += (i < 2 ? -0.3 : 0.3);
            
            double tentaclePhase = animPhase + i * 0.5;
            double sway = 4 * Math.sin(tentaclePhase);
            
            int baseX = (int)(8 * Math.cos(tentacleBaseAngle));
            int baseY = -8;
            int tipX = (int)(baseX + sway + 12 * Math.cos(tentacleBaseAngle));
            int tipY = (int)(baseY - 12);
            
            g2d.setColor(new Color(75, 65, 85, baseAlpha / 2));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawLine(baseX, baseY, tipX, tipY);
            g2d.fillOval(tipX - 2, tipY - 2, 4, 4);
        }
        
        g2d.setStroke(new BasicStroke(1));
        g2d.setTransform(oldTransform);
    }

    private void drawLongerTopDownLeg(Graphics2D g2d, int startX, int startY, double phase, boolean isLeft, int alpha) {
        // Three-section leg extending outward from body (longer)
        double extension = 18 + 8 * Math.sin(phase); // Increased extension
        double angle = isLeft ? -Math.PI/2.5 : Math.PI/2.5;
        
        // Add wave motion to angle
        angle += Math.sin(phase) * 0.4;
        
        // First section (from body) - longer
        int mid1X = (int)(startX + extension * 0.4 * Math.cos(angle));
        int mid1Y = (int)(startY + extension * 0.4 * Math.sin(angle));
        
        // Second section (middle joint)
        double secondAngle = angle + (isLeft ? -0.3 : 0.3) + Math.sin(phase + 1) * 0.2;
        int mid2X = (int)(mid1X + extension * 0.35 * Math.cos(secondAngle));
        int mid2Y = (int)(mid1Y + extension * 0.35 * Math.sin(secondAngle));
        
        // Third section (tip)
        double thirdAngle = secondAngle + (isLeft ? -0.2 : 0.2);
        int tipX = (int)(mid2X + extension * 0.25 * Math.cos(thirdAngle));
        int tipY = (int)(mid2Y + extension * 0.25 * Math.sin(thirdAngle));
        
        g2d.setColor(new Color(60, 50, 70, Math.max(0, Math.min(255, alpha - 30))));
        g2d.setStroke(new BasicStroke(2.5f));
        
        // First segment
        g2d.drawLine(startX, startY, mid1X, mid1Y);
        
        // First joint
        g2d.fillOval(mid1X - 2, mid1Y - 2, 4, 4);
        
        // Second segment
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine(mid1X, mid1Y, mid2X, mid2Y);
        
        // Second joint
        g2d.fillOval(mid2X - 2, mid2Y - 2, 4, 4);
        
        // Third segment (thinnest)
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(mid2X, mid2Y, tipX, tipY);
        
        // Foot
        g2d.fillOval(tipX - 2, tipY - 2, 4, 4);
        
        g2d.setStroke(new BasicStroke(1));
    }
    
    private void drawHPBar(Graphics2D g2d) {
        int barWidth = size;
        int barHeight = 5;
        int barX = (int)x - barWidth/2;
        int barY = (int)y - size/2 - 15;
        
        // Background
        g2d.setColor(new Color(50, 50, 50, 200));
        g2d.fillRect(barX, barY, barWidth, barHeight);
        
        // HP fill
        float hpPercent = (float)currentHP / maxHP;
        int fillWidth = (int)(barWidth * hpPercent);
        
        Color hpColor = hpPercent > 0.5f ? new Color(0, 255, 100) :
                       hpPercent > 0.25f ? new Color(255, 200, 0) :
                       new Color(255, 50, 50);
        
        g2d.setColor(hpColor);
        g2d.fillRect(barX, barY, fillWidth, barHeight);
        
        // Border
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
    
    public int getSizeValue() {
        // Centipede doesn't count toward size limits
        if (type == CreatureType.CENTIPEDE) return 0;
        
        // Returns size value for spawn management (1-3 based on size)
        if (size < 50) return 1;
        if (size < 75) return 2;
        return 3;
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public CreatureType getType() { return type; }
}