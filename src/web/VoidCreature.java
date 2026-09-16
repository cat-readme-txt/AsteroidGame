import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
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
    private static final BasicStroke THIN_STROKE = new BasicStroke(1f);
    private static final BasicStroke SHELL_STROKE = new BasicStroke(1.5f);
    private static final BasicStroke HEAD_STROKE = new BasicStroke(2f);
    private static final BasicStroke LEG_STROKE = new BasicStroke(2.5f);
    private static final int[][] MOUTHPARTS = {
        {-8, 10, -12, 18}, {-4, 10, -7, 16}, {4, 10, 7, 16}, {8, 10, 12, 18}
    };
    // The only two rendering modes; palettes are shared by every centipede.
    private static final Color[] ACTIVE_PALETTE = centipedePalette(255);
    private static final Color[] GHOST_PALETTE = centipedePalette(80);
    private final Polygon mouthTip = new Polygon(new int[3], new int[3], 3);

    private static Color[] centipedePalette(int alpha) {
        return new Color[] {
            new Color(45, 35, 55, alpha), new Color(53, 41, 65, alpha),
            new Color(90, 40, 120, alpha / 2), new Color(65, 55, 75, alpha),
            new Color(30, 25, 40, alpha), new Color(55, 45, 65, alpha),
            new Color(100, 50, 130, alpha / 3), new Color(75, 65, 85, alpha),
            new Color(85, 75, 95, alpha), new Color(75, 65, 85, alpha / 2),
            new Color(60, 50, 70, Math.max(0, alpha - 30))
        };
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
    // Fixed-capacity, distance-sampled head trail. No per-tick path allocation.
    private static final double PATH_STEP = 2.0;
    private double[] pathX, pathY;
    private int pathHead, pathCount;
    private double pathRemainder, growthDistance;
    private double headX, headY;
    private double exitTravel, exitOffsetX, exitOffsetY;
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
        vc.segments = new java.util.ArrayList<>(vc.maxSegmentCount);
        int pathCapacity = (int)Math.ceil(vc.maxSegmentCount * SEGMENT_SPACING / PATH_STEP) + 2;
        vc.pathX = new double[pathCapacity];
        vc.pathY = new double[pathCapacity];
        
        // Determine spawn edge and initial direction
        int edge = vc.rand.nextInt(4); // 0=left, 1=right, 2=top, 3=bottom
        double startX, startY, initAngle;
        
        switch(edge) {
            case 0 -> {
                // Left edge
                startX = 0;
                startY = vc.rand.nextInt(screenHeight);
                initAngle = 0; // Move right
            }
            case 1 -> {
                // Right edge
                startX = screenWidth;
                startY = vc.rand.nextInt(screenHeight);
                initAngle = Math.PI; // Move left
            }
            case 2 -> {
                // Top edge
                startX = vc.rand.nextInt(screenWidth);
                startY = 0;
                initAngle = Math.PI / 2; // Move down
            }
            default -> {
                // Bottom edge
                startX = vc.rand.nextInt(screenWidth);
                startY = screenHeight;
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
        vc.headX = vc.pathX[0] = startX;
        vc.headY = vc.pathY[0] = startY;
        vc.pathCount = 1;
        
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
        isInVoid = voidActive;
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
        if (segments.isEmpty()) return;
        if (!exitingVoid) {
            if (dirCooldown > 0) dirCooldown--;
            if (dirCooldown == 0 && rand.nextInt(90) == 0) {
                desiredAngle = wrapAngle(desiredAngle + (rand.nextDouble() * 2 - 1) * Math.toRadians(60));
                dirCooldown = DIR_CHANGE_COOLDOWN_FRAMES;
            }
        }
        targetAngle = turnToward(targetAngle, desiredAngle,
                exitingVoid ? MAX_TURN_RATE * 1.8 : MAX_TURN_RATE);
        double travel = exitingVoid ? speed * 2 : speed;
        double dx = Math.cos(targetAngle), dy = Math.sin(targetAngle);
        double oldX = headX, oldY = headY;
        headX += travel * dx;
        headY += travel * dy;

        // Sample at fixed distances so following costs O(segment count), regardless of age.
        for (double offset = PATH_STEP - pathRemainder; offset <= travel; offset += PATH_STEP) {
            pathHead = (pathHead + 1) % pathX.length;
            pathX[pathHead] = oldX + offset * dx;
            pathY[pathHead] = oldY + offset * dy;
            pathCount = Math.min(pathCount + 1, pathX.length);
        }
        pathRemainder = (pathRemainder + travel) % PATH_STEP;
        if (exitingVoid) {
            exitTravel += travel;
        } else {
            growthDistance = Math.min((maxSegmentCount - 1) * SEGMENT_SPACING, growthDistance + travel);
            while (segments.size() < maxSegmentCount && segments.size() * SEGMENT_SPACING <= growthDistance) {
                segments.add(new BodySegment(0, 0)); // Append at the tail; existing segments keep their places.
            }
        }

        boolean allOutside = exitingVoid;
        for (int i = 0; i < segments.size(); i++) {
            BodySegment segment = segments.get(i);
            double px = headX, py = headY;
            if (i == 0) {
                segment.angle = targetAngle;
            } else {
                double sampleOffset = (i * SEGMENT_SPACING - pathRemainder) / PATH_STEP;
                int stepsBack = (int)Math.floor(sampleOffset);
                double fraction = sampleOffset - stepsBack;
                int newer = Math.floorMod(pathHead - stepsBack, pathX.length);
                int older = Math.floorMod(newer - 1, pathX.length);
                px = pathX[newer] + (pathX[older] - pathX[newer]) * fraction;
                py = pathY[newer] + (pathY[older] - pathY[newer]) * fraction;
                segment.angle = Math.atan2(pathY[newer] - pathY[older], pathX[newer] - pathX[older]);
            }
            // Each segment follows the old wrapped route until it reaches the exit-start point.
            boolean onExitPath = exitingVoid && exitTravel >= i * SEGMENT_SPACING;
            segment.x = onExitPath ? px - exitOffsetX : wrapCoordinate(px, screenW);
            segment.y = onExitPath ? py - exitOffsetY : wrapCoordinate(py, screenH);
            if (!onExitPath || !isOutside(segment, 100)) allOutside = false;
        }
        x = segments.get(0).x;
        y = segments.get(0).y;
        // Keep the original head as the controller until the entire tail has left.
        if (allOutside) segments.clear();
    }

    private static double wrapCoordinate(double value, int extent) {
        if (value >= 0 && value <= extent) return value;
        double wrapped = value % extent;
        return wrapped < 0 ? wrapped + extent : wrapped;
    }

    private boolean isOutside(BodySegment segment, int margin) {
        return segment.x < -margin || segment.x > screenW + margin
                || segment.y < -margin || segment.y > screenH + margin;
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
    
    public void startExitingVoid() {
        if (type != CreatureType.CENTIPEDE) return;
        if (exitingVoid) return;

        exitingVoid = true;
        exitTravel = 0;
        exitOffsetX = headX - x;
        exitOffsetY = headY - y;

        // Head towards nearest screen edge
        double toLeft = x;
        double toRight = screenW - x;
        double toTop = y;
        double toBottom = screenH - y;

        double min = Math.min(Math.min(toLeft, toRight), Math.min(toTop, toBottom));

        if (min == toLeft) desiredAngle = Math.PI;
        else if (min == toRight) desiredAngle = 0;
        else if (min == toTop) desiredAngle = -Math.PI / 2;
        else desiredAngle = Math.PI / 2;
    }

    public boolean isExitingVoid() { return exitingVoid; }

    public void updateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) return;
        screenW = width;
        screenH = height;
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
    
    public boolean intersects(Polygon bounds) {
        if (!isInVoid || isDead() || bounds.npoints == 0) return false;
        Rectangle2D broadBounds = bounds.getBounds();
        if (type == CreatureType.CENTIPEDE) {
            for (BodySegment segment : segments) {
                if (intersectsCircle(bounds, broadBounds, segment.x, segment.y, 8)) return true;
            }
            return false;
        }
        return intersectsCircle(bounds, broadBounds, x, y, (int)(size * 0.7) / 2);
    }

    private static boolean intersectsCircle(Polygon bounds, Rectangle2D broadBounds, double cx, double cy, double radius) {
        if (!broadBounds.intersects(cx - radius, cy - radius, radius * 2, radius * 2)) return false;
        if (bounds.contains(cx, cy)) return true;
        double radiusSquared = radius * radius;
        for (int i = 0; i < bounds.npoints; i++) {
            int next = (i + 1) % bounds.npoints;
            if (Line2D.ptSegDistSq(bounds.xpoints[i], bounds.ypoints[i],
                    bounds.xpoints[next], bounds.ypoints[next], cx, cy) < radiusSquared) return true;
        }
        return false;
    }

    public boolean intersects(Rectangle2D bounds) {
        if (!isInVoid || isDead()) return false;
        if (type == CreatureType.CENTIPEDE) {
            for (BodySegment segment : segments) {
                if (intersectsCircle(bounds, segment.x, segment.y, 8)) return true;
            }
            return false;
        }
        return intersectsCircle(bounds, x, y, (int)(size * 0.7) / 2);
    }

    private static boolean intersectsCircle(Rectangle2D bounds, double cx, double cy, double radius) {
        double dx = cx - Math.max(bounds.getMinX(), Math.min(cx, bounds.getMaxX()));
        double dy = cy - Math.max(bounds.getMinY(), Math.min(cy, bounds.getMaxY()));
        return dx * dx + dy * dy < radius * radius;
    }

    public boolean intersects(Line2D line) {
        if (!isInVoid || isDead()) return false;
        if (type == CreatureType.CENTIPEDE) {
            for (BodySegment segment : segments) {
                if (line.ptSegDistSq(segment.x, segment.y) < 64) return true;
            }
            return false;
        }
        double radius = (int)(size * 0.7) / 2;
        return line.ptSegDistSq(x, y) < radius * radius;
    }

    public void draw(Graphics2D g2d, boolean voidMode) {
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
        
        Color[] palette = baseAlpha == 255 ? ACTIVE_PALETTE : GHOST_PALETTE;
        AffineTransform oldTransform = g2d.getTransform();
        java.awt.Stroke oldStroke = g2d.getStroke();
        // Draw from tail to head so head appears on top
        for (int i = segments.size() - 1; i >= 0; i--) {
            BodySegment seg = segments.get(i);
            
            if (isOutside(seg, 64)) continue;
            g2d.translate(seg.x, seg.y);
            g2d.rotate(seg.angle);
            
            // Calculate segment size (larger in middle)
            double segmentRatio = 1.0 - Math.abs((i / (double)segments.size()) - 0.5) * 0.3;
            int segWidth = (int)(20 * segmentRatio);  // Increased from 16
            int segLength = 16;  // Increased from 14
            
            
            // Draw legs FIRST (so they appear under the body)
            if (i > 0 && i % 3 == 0) {  // Draw legs every 3 segments
                double legPhase = limbPhase + i * 0.4;
                
                // Left side legs
                drawLongerTopDownLeg(g2d, -segWidth/2 - 2, -4, legPhase, true, palette[10]);
                drawLongerTopDownLeg(g2d, -segWidth/2 - 2, 4, legPhase + 0.5, true, palette[10]);
                
                // Right side legs
                drawLongerTopDownLeg(g2d, segWidth/2 + 2, -4, legPhase + Math.PI, false, palette[10]);
                drawLongerTopDownLeg(g2d, segWidth/2 + 2, 4, legPhase + Math.PI + 0.5, false, palette[10]);
            }
            
            // Main body segment with darker colors
            g2d.setColor(palette[i % 2]);
            g2d.fillOval(-segWidth/2, -segLength/2, segWidth, segLength);
            
            // Minimal purple accent stripes (every 10th segment)
            if (i % 10 == 0) {
                g2d.setColor(palette[2]);
                g2d.setStroke(SHELL_STROKE);
                g2d.drawArc(-segWidth/2 + 2, -segLength/2 + 2, segWidth - 4, segLength - 4, 0, 360);
            }
            
            // Exoskeleton plates
            g2d.setColor(palette[3]);
            g2d.setStroke(SHELL_STROKE);
            
            // Outer shell edge
            g2d.drawOval(-segWidth/2, -segLength/2, segWidth, segLength);
            
            // Keep texture on every third plate. At full length, drawing two
            // extra arcs on all 150 segments dominated the browser renderer.
            if (i % 3 == 0) {
                g2d.setStroke(THIN_STROKE);
                g2d.drawArc(-segWidth/2 + 2, -segLength/2 + 2, segWidth - 4, segLength - 4, 0, 180);
                g2d.drawArc(-segWidth/2 + 2, -segLength/2 + 2, segWidth - 4, segLength - 4, 180, 180);
            }
            
            // Joint line between segments
            if (i < segments.size() - 1 && i % 2 == 0) {
                g2d.setColor(palette[4]);
                g2d.setStroke(SHELL_STROKE);
                g2d.drawLine(-segWidth/2, -segLength/2 + 1, segWidth/2, -segLength/2 + 1);
            }
            
            // Restore transform
            g2d.setTransform(oldTransform);
        }
        
        // Draw head separately
        BodySegment head = segments.get(0);
        
        if (isOutside(head, 64)) {
            g2d.setStroke(oldStroke);
            return;
        }
        g2d.translate(head.x, head.y);
        g2d.rotate(head.angle);
        
        // Head body - larger
        g2d.setColor(palette[5]);
        g2d.fillOval(-14, -12, 28, 24);
        
        // Subtle purple glow on head
        g2d.setColor(palette[6]);
        g2d.fillOval(-16, -14, 32, 28);
        
        // Head exoskeleton ridges
        g2d.setColor(palette[7]);
        g2d.setStroke(HEAD_STROKE);
        g2d.drawOval(-14, -12, 28, 24);
        g2d.drawArc(-12, -10, 24, 20, 20, 140);
        
        // Mandibles (same as before)
        g2d.setColor(palette[8]);
        g2d.setStroke(HEAD_STROKE);
        for (int[] mp : MOUTHPARTS) {
            g2d.drawLine(mp[0], mp[1], mp[2], mp[3]);
            mouthTip.xpoints[0] = mp[2];
            mouthTip.xpoints[1] = mp[2] - 2;
            mouthTip.xpoints[2] = mp[2] + 2;
            mouthTip.ypoints[0] = mp[3] + 2;
            mouthTip.ypoints[1] = mouthTip.ypoints[2] = mp[3];
            mouthTip.invalidate();
            g2d.fillPolygon(mouthTip);
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
            
            g2d.setColor(palette[9]);
            g2d.setStroke(SHELL_STROKE);
            g2d.drawLine(baseX, baseY, tipX, tipY);
            g2d.fillOval(tipX - 2, tipY - 2, 4, 4);
        }
        
        g2d.setStroke(oldStroke);
        g2d.setTransform(oldTransform);
    }

    private void drawLongerTopDownLeg(Graphics2D g2d, int startX, int startY, double phase, boolean isLeft, Color color) {
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
        
        g2d.setColor(color);
        g2d.setStroke(LEG_STROKE);
        
        // First segment
        g2d.drawLine(startX, startY, mid1X, mid1Y);
        
        // First joint
        g2d.fillOval(mid1X - 2, mid1Y - 2, 4, 4);
        
        // Second segment
        g2d.setStroke(HEAD_STROKE);
        g2d.drawLine(mid1X, mid1Y, mid2X, mid2Y);
        
        // Second joint
        g2d.fillOval(mid2X - 2, mid2Y - 2, 4, 4);
        
        // Third segment (thinnest)
        g2d.setStroke(SHELL_STROKE);
        g2d.drawLine(mid2X, mid2Y, tipX, tipY);
        
        // Foot
        g2d.fillOval(tipX - 2, tipY - 2, 4, 4);
        
        g2d.setStroke(THIN_STROKE);
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
