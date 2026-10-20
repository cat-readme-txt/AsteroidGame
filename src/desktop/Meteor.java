import java.awt.*;
import java.util.Random;

public class Meteor {
    private static final Random random = new Random();

    float x, y;          // Current position
    float speedX, speedY; // Velocity
    int length;          // Length of the meteor trail
    float alpha;         // Transparency for fading tail

    int screenWidth, screenHeight;

    public Meteor(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        reset();
    }

    // Initialize/reset meteor with random position & velocity
    public void reset() {
        x = random.nextInt(screenWidth);
        y = random.nextInt(screenHeight / 2) - screenHeight; // Start offscreen above
        speedX = 2 + random.nextFloat() * 3;  // Speed x (2 to 5 px/frame)
        speedY = 4 + random.nextFloat() * 3;  // Speed y (4 to 7 px/frame)
        length = 10 + random.nextInt(10);     // Trail length 10 to 20 pixels
        alpha = 0.6f + random.nextFloat() * 0.4f; // Transparency 0.6 to 1.0
    }

    // Update meteor position
    public void update() {
        x += speedX;
        y += speedY;

        // If meteor goes off screen, reset it
        if (x > screenWidth || y > screenHeight) {
            reset();
        }
    }

    public void updateDimensions(int newWidth, int newHeight) {
        this.screenWidth = newWidth;
        this.screenHeight = newHeight;
    }

    // Draw the meteor as a line with fading alpha tail
    public void draw(Graphics2D g2d) {
        // Save original composite
        Composite originalComposite = g2d.getComposite();

        // Draw tail as a gradient of small segments with decreasing alpha
        for (int i = 0; i < length; i++) {
            float segmentAlpha = alpha * (1.0f - ((float)i / length));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, segmentAlpha));
            int tailX = (int)(x - speedX * i / length);
            int tailY = (int)(y - speedY * i / length);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(tailX, tailY, tailX, tailY); // Draw a point
        }

        // Restore original composite
        g2d.setComposite(originalComposite);
    }
}
