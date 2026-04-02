import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FloatingTextManager {
    public enum Type { XP, FUEL, DAMAGE, INFO }
    
    public static class FloatingText {
        private String text;
        private double x, y;
        private double vy = -0.8;
        private double vx = 0;
        private int lifetime;
        private int maxLifetime;
        private Color color;
        private Type type;
        private float scale = 1.0f;
        
        public FloatingText(String text, double x, double y, Color color, int lifetime, Type type) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.maxLifetime = lifetime;
            this.lifetime = lifetime;
            this.type = type;
            
            this.vx = (Math.random() - 0.5) * 0.5;
        }
        
        public void update() {
            y += vy;
            x += vx;
            lifetime--;
            
            if (maxLifetime - lifetime < 10) {
                scale = 0.5f + (maxLifetime - lifetime) / 20.0f;
            } else {
                scale = 1.0f;
            }
        }
        
        public boolean isAlive() {
            return lifetime > 0;
        }
        
        public void draw(Graphics2D g2d) {
            float alpha = (float) lifetime / maxLifetime;
            
            Font originalFont = g2d.getFont();
            Font scaledFont = new Font("Arial", Font.BOLD, (int)(12 * scale));
            g2d.setFont(scaledFont);
            
            // Shadow
            Color shadowColor = new Color(0, 0, 0, (int)(200 * alpha));
            g2d.setColor(shadowColor);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text, (int)(x - fm.stringWidth(text) / 2) + 2, (int)y + 2);
            
            // Glow based on type
            if (type == Type.XP) {
                for (int i = 2; i > 0; i--) {
                    int glowAlpha = (int)(50 * alpha);
                    g2d.setColor(new Color(100, 200, 255, glowAlpha));
                    g2d.drawString(text, (int)(x - fm.stringWidth(text) / 2) - i, (int)y - i);
                    g2d.drawString(text, (int)(x - fm.stringWidth(text) / 2) + i, (int)y + i);
                }
            } else if (type == Type.FUEL) {
                for (int i = 2; i > 0; i--) {
                    int glowAlpha = (int)(50 * alpha);
                    g2d.setColor(new Color(255, 200, 0, glowAlpha));
                    g2d.drawString(text, (int)(x - fm.stringWidth(text) / 2) - i, (int)y - i);
                    g2d.drawString(text, (int)(x - fm.stringWidth(text) / 2) + i, (int)y + i);
                }
            }
            
            // Main text color
            Color drawColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int)(255 * alpha)
            );
            g2d.setColor(drawColor);
            g2d.drawString(text, (int)(x - fm.stringWidth(text) / 2), (int)y);
            
            g2d.setFont(originalFont);
        }
    }
    
    private List<FloatingText> texts = new ArrayList<>();
    
    // Convenience factory methods
    public static FloatingText createXP(int amount, double x, double y) {
        return new FloatingText(
            "+" + amount + " XP",
            x, y,
            new Color(100, 200, 255),
            60,
            Type.XP
        );
    }
    
    public static FloatingText createFuel(int amount, double x, double y) {
        return new FloatingText(
            "+" + amount,
            x, y,
            new Color(255, 200, 50),
            60,
            Type.FUEL
        );
    }
    
    public static FloatingText createDamage(int amount, double x, double y) {
        return new FloatingText(
            "-" + amount,
            x, y,
            new Color(255, 50, 50),
            45,
            Type.DAMAGE
        );
    }
    
    public static FloatingText createInfo(String message, double x, double y) {
        return new FloatingText(
            message,
            x, y,
            new Color(255, 255, 255),
            90,
            Type.INFO
        );
    }
    
    public void add(FloatingText text) {
        texts.add(text);
    }
    
    public void update() {
        Iterator<FloatingText> it = texts.iterator();
        while (it.hasNext()) {
            FloatingText text = it.next();
            text.update();
            if (!text.isAlive()) {
                it.remove();
            }
        }
    }
    
    public void draw(Graphics2D g2d) {
        for (FloatingText text : texts) {
            text.draw(g2d);
        }
    }
    
    public void clear() {
        texts.clear();
    }
}