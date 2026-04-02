import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StarField {
    private static class Star {
        double x, y;
        double z; // depth (0 = far, 1 = near)
        double speed;
        int brightness;
        
        Star(double x, double y, double z, double speed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.speed = speed;
            this.brightness = (int) (100 + 155 * z);
        }
    }
    
    private final List<Star> stars = new ArrayList<>();
    private final Random rand = new Random();
    private int width, height;
    private boolean voidMode = false;
    
    public StarField(int width, int height, int count) {
        this.width = width;
        this.height = height;
        
        for (int i = 0; i < count; i++) {
            double x = rand.nextDouble() * width;
            double y = rand.nextDouble() * height;
            double z = rand.nextDouble();
            double speed = 0.1 + rand.nextDouble() * 0.3;
            stars.add(new Star(x, y, z, speed));
        }
    }
    
    public void setVoidMode(boolean voidMode) {
        this.voidMode = voidMode;
    }
    
    public void update(double shipX, double shipY, double shipVX, double shipVY) {
        for (Star star : stars) {
            // Parallax effect based on depth
            star.x -= shipVX * star.z * 0.5;
            star.y -= shipVY * star.z * 0.5;
            
            // Wrap around
            if (star.x < 0) star.x += width;
            if (star.x > width) star.x -= width;
            if (star.y < 0) star.y += height;
            if (star.y > height) star.y -= height;
        }
    }

    public void updateDimensions(int newWidth, int newHeight) {
        int oldWidth = this.width;
        int oldHeight = this.height;

        this.width = newWidth;
        this.height = newHeight;

        if (oldWidth > 0 && oldHeight > 0) {
            double sx = newWidth / (double) oldWidth;
            double sy = newHeight / (double) oldHeight;

            for (Star star : stars) {
                star.x *= sx;
                star.y *= sy;
            }
        }
    }
    
    public void draw(Graphics2D g2d) {
        for (Star star : stars) {
            int size = (int) (1 + star.z * 2);
            Color color;
            
            if (voidMode) {
                // Purple-tinted stars in void mode
                int purple = (int) (star.brightness * 0.8);
                color = new Color(purple, 0, star.brightness, 200);
            } else {
                // Normal white stars
                color = new Color(star.brightness, star.brightness, star.brightness, 200);
            }
            
            g2d.setColor(color);
            g2d.fillOval((int) star.x - size / 2, (int) star.y - size / 2, size, size);
            
            // Add glow for bright stars
            if (star.z > 0.7) {
                int glowSize = size + 2;
                int alpha = (int) (50 * star.z);
                Color glowColor = voidMode ? 
                    new Color(150, 0, 255, alpha) : 
                    new Color(255, 255, 255, alpha);
                g2d.setColor(glowColor);
                g2d.fillOval((int) star.x - glowSize / 2, (int) star.y - glowSize / 2, glowSize, glowSize);
            }
        }
    }
}