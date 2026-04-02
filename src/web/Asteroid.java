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
    private final double normalvx, normalvy;
    private double slowedvx, slowedvy;
    private final Size size;
    private Polygon shape;
    private static double levelMultiplier;
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
        levelMultiplier = 1;
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
	    double speed = 1 + rand.nextDouble() * 1.5 * levelMultiplier;
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
    }

    public void update() {
        x += vx;
        y += vy;
        
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
            int glowSize = (int)(getBounds().getBounds().width * 1.5);
            g2d.fillOval(-glowSize/2, -glowSize/2, glowSize, glowSize);
        }
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawPolygon(shape);
        g2d.translate(-x, -y);
    }

	public boolean intersects(Polygon shipBounds) {
	    Polygon moved = new Polygon();
	    for (int i = 0; i < shape.npoints; i++) {
	        moved.addPoint(shape.xpoints[i] + (int)x, shape.ypoints[i] + (int)y);
	    }
	    
	    Area asteroidArea = new Area(moved);
	    Area shipArea = new Area(shipBounds);
	    
	    asteroidArea.intersect(shipArea);
	    
	    return !asteroidArea.isEmpty();  // True if there's overlap
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
	        double newVX = vx + (rand.nextDouble() - 0.5);
	        double newVY = vy + (rand.nextDouble() - 0.5);
	        fragments.add(new Asteroid(newSize, x, y, newVX, newVY));
	    }
	    return fragments;
	}

    public void triggerHitFlash() {
        hitFlash = true;
        hitFlashTimer = 10; // 10 frames
    }

    public Polygon getBounds() {
        Polygon moved = new Polygon();
        for (int i = 0; i < shape.npoints; i++) {
            moved.addPoint(shape.xpoints[i] + (int)x, shape.ypoints[i] + (int)y);
        }
        return moved;
    }

    public Size getSize() { return size; }
    public void setMultiplier(double newMultiplier) { levelMultiplier = newMultiplier; }
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
