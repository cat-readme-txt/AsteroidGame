import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Asteroid{
    public enum Size { LARGE, MEDIUM, SMALL }

    private double x, y;
    private double vx, vy;
    private double normalvx, normalvy;
    private double slowedvx, slowedvy;
    private final Size size;
    private Polygon shape;
    private Polygon worldShape;
    private Rectangle localBounds;
    private final Rectangle worldBounds = new Rectangle();
    private double levelMultiplier = 1.0;
    private boolean hitFlash = false;
    private int hitFlashTimer = 0;
    private static final Random rand = new Random();

    public Asteroid(Size size, double x, double y, double vx, double vy) {
        this.size = size;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.normalvx = vx;
        this.normalvy = vy;
        generateShape();
    }

	public static Asteroid randomAsteroid(int screenWidth, int screenHeight) {
	    Size size = Size.LARGE;
	
	    // Spawn just off the screen (left/right or top/bottom)
	    double x, y;
	    if (rand.nextBoolean()) {
	        x = rand.nextBoolean() ? -50 : screenWidth + 50;
	        y = rand.nextInt(screenHeight);
	    } else {
	        x = rand.nextInt(screenWidth);
	        y = rand.nextBoolean() ? -50 : screenHeight + 50;
	    }
	
	    // Target: center of screen
	    double targetX = screenWidth / 2.0;
	    double targetY = screenHeight / 2.0;
	
	    // Direction vector
	    double dx = targetX - x;
	    double dy = targetY - y;
	
	    // Normalize and apply speed
	    double distance = Math.sqrt(dx * dx + dy * dy);
	    double speed = 1 + rand.nextDouble() * 1.5;
	    double vx = (dx / distance) * speed;
	    double vy = (dy / distance) * speed;
	
	    return new Asteroid(size, x, y, vx, vy);
	}

    private void generateShape() {
        int radius;
        switch (size) {
            case LARGE -> radius = 40;
            case MEDIUM -> radius = 25;
            case SMALL -> radius = 15;
            default -> radius = 20;
        }

        int points = 8;
        int[] xs = new int[points];
        int[] ys = new int[points];
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double r = radius * (0.8 + 0.4 * rand.nextDouble()); // Jagged shape
            xs[i] = (int) (r * Math.cos(angle));
            ys[i] = (int) (r * Math.sin(angle));
        }
        shape = new Polygon(xs, ys, points);
        worldShape = new Polygon(new int[points], new int[points], points);
        localBounds = shape.getBounds();
        updateBounds();
    }

    public void update() {
        x += vx;
        y += vy;
        updateBounds();
        
        if (hitFlash) {
            hitFlashTimer--;
            if (hitFlashTimer <= 0) {
                hitFlash = false;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.translate(x, y);
        
        if (hitFlash) {
            // White glow overlay
            float alpha = hitFlashTimer / 10.0f;
            g2d.setColor(new Color(255, 255, 255, (int)(200 * alpha)));
            int glowSize = (int)(worldBounds.width * 1.5);
            g2d.fillOval(-glowSize/2, -glowSize/2, glowSize, glowSize);
        }
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawPolygon(shape);
        g2d.translate(-x, -y);
    }

    public boolean intersects(Polygon shipBounds) {
        if (shipBounds == null || !worldBounds.intersects(shipBounds.getBounds2D())) return false;
        Area asteroidArea = new Area(worldShape);
        asteroidArea.intersect(new Area(shipBounds));
        return !asteroidArea.isEmpty();
    }

	public List<Asteroid> split() {
	    List<Asteroid> fragments = new ArrayList<>();
	    Size newSize;
	    switch (size) {
	        case LARGE -> newSize = Size.MEDIUM;
	        case MEDIUM -> newSize = Size.SMALL;
	        default -> {
	            return Collections.emptyList(); // SMALL can't split
	        }
	    }
	    for (int i = 0; i < 2; i++) {
	        double newVX = normalvx + (rand.nextDouble() - 0.5);
	        double newVY = normalvy + (rand.nextDouble() - 0.5);
	        fragments.add(new Asteroid(newSize, x, y, newVX, newVY));
	    }
	    return fragments;
	}

    public void triggerHitFlash() {
        hitFlash = true;
        hitFlashTimer = 10; // 10 frames
    }

    private void updateBounds() {
        int positionX = (int)x;
        int positionY = (int)y;
        for (int i = 0; i < shape.npoints; i++) {
            worldShape.xpoints[i] = shape.xpoints[i] + positionX;
            worldShape.ypoints[i] = shape.ypoints[i] + positionY;
        }
        worldShape.invalidate();
        worldBounds.setBounds(localBounds.x + positionX, localBounds.y + positionY,
                localBounds.width, localBounds.height);
    }

    // These cached shapes are read-only to callers and move with this asteroid.
    public Polygon getBounds() { return worldShape; }
    public Rectangle getBoundingBox() { return worldBounds; }

    public boolean hasExited(int screenWidth, int screenHeight) {
        // Keep inward-moving spawns; retire only after the entire shape leaves an edge.
        return (worldBounds.getMaxX() < 0 && vx <= 0)
                || (worldBounds.getMinX() > screenWidth && vx >= 0)
                || (worldBounds.getMaxY() < 0 && vy <= 0)
                || (worldBounds.getMinY() > screenHeight && vy >= 0);
    }

    public Size getSize() { return size; }
    public void setMultiplier(double newMultiplier) {
        if (newMultiplier <= 0 || newMultiplier == levelMultiplier) return;
        double ratio = newMultiplier / levelMultiplier;
        normalvx *= ratio;
        normalvy *= ratio;
        vx *= ratio;
        vy *= ratio;
        slowedvx *= ratio;
        slowedvy *= ratio;
        levelMultiplier = newMultiplier;
    }
    public void toggleTimeSlow(boolean isActive) {
    	if (isActive) { 
    		vx=slowedvx; 
    		vy=slowedvy; 
    	} else { 
    		vx=normalvx;
    		vy=normalvy; 
    	}
    }
    public void setSlowedVelocity(double timeScale) {
    	slowedvx = normalvx * timeScale;
    	slowedvy = normalvy * timeScale;
    }
}
