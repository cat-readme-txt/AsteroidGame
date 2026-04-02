import java.awt.*;

public class TeleportAnchor {
    private double anchorX, anchorY;
    private boolean anchorPlaced = false;
    private int cooldownTimer = 0;
    private final int maxCooldown = 5 * 60; // 5 seconds
    private float pulsePhase = 0;
    
    public void update() {
        if (cooldownTimer > 0) {
            cooldownTimer--;
        }
        pulsePhase += 0.1f;
    }
    
    public void placeAnchor(double x, double y) {
        anchorX = x;
        anchorY = y;
        anchorPlaced = true;
    }
    
    public boolean teleportToAnchor(Ship ship) {
        if (!anchorPlaced) return false;
        
        ship.setX(anchorX);
        ship.setY(anchorY);
        anchorPlaced = false;
        cooldownTimer = maxCooldown;
        return true;
    }
    
    public void drawAnchor(Graphics2D g2d) {
        if (!anchorPlaced) return;
        
        // Pulsing rings
        for (int i = 0; i < 4; i++) {
            int radius = (int)(20 + i * 10 + 5 * Math.sin(pulsePhase + i * 0.5));
            int alpha = 150 - i * 30;
            g2d.setColor(new Color(0, 255, 200, alpha));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int)anchorX - radius, (int)anchorY - radius, radius * 2, radius * 2);
        }
        
        // Central marker
        g2d.setColor(new Color(0, 255, 200, 200));
        g2d.fillOval((int)anchorX - 5, (int)anchorY - 5, 10, 10);
        
        // Cross marker
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine((int)anchorX - 15, (int)anchorY, (int)anchorX + 15, (int)anchorY);
        g2d.drawLine((int)anchorX, (int)anchorY - 15, (int)anchorX, (int)anchorY + 15);
        
        // Label
        g2d.setFont(new Font("Serif", Font.BOLD, 12));
        g2d.setColor(new Color(0, 255, 200));
        String label = "ANCHOR";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(label, (int)anchorX - fm.stringWidth(label) / 2, (int)anchorY - 25);
        
        g2d.setStroke(new BasicStroke(1));
    }
    
    public void drawTeleportFlash(Graphics2D g2d, int screenWidth, int screenHeight, int frame) {
        // Flash effect when teleporting
        float alpha = 1.0f - (frame / 15.0f);
        if (alpha > 0) {
            g2d.setColor(new Color(0, 255, 200, (int)(200 * alpha)));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
            
            // Circular wave effect
            int radius = frame * 40;
            g2d.setColor(new Color(0, 255, 200, (int)(150 * alpha)));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawOval((int)anchorX - radius, (int)anchorY - radius, radius * 2, radius * 2);
            g2d.setStroke(new BasicStroke(1));
        }
    }

    public void reset() {
        // Position does not matter when anchor is not placed, but reset anyway
        anchorX = 0;
        anchorY = 0;

        // As if an anchor was never placed
        anchorPlaced = false;

        // No cooldown running
        cooldownTimer = 0;

        // Reset pulse animation phase
        pulsePhase = 0f;
    }
    
    public boolean isAnchorPlaced() { return anchorPlaced; }
    public boolean isOnCooldown() { return cooldownTimer > 0; }
    public int getCooldownTimer() { return cooldownTimer; }
    public int getMaxCooldown() { return maxCooldown; }
}