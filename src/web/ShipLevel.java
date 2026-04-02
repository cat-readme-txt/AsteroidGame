import java.awt.*;

public class ShipLevel {
    private int level = 1;
    private int xp = 0;
    private int xpToNextLevel = 20;
    /*
    // Ability unlock levels
    private static final int VOID_UNLOCK_LEVEL = 3;
    private static final int FADE_UNLOCK_LEVEL = 5;
    private static final int TELEPORT_UNLOCK_LEVEL = 7;
    private static final int SHIELD_UNLOCK_LEVEL = 9;
    private static final int TIME_SLOW_UNLOCK_LEVEL = 12;
    private static final int DRONE_UNLOCK_LEVEL = 15;
    private static final int NOVA_UNLOCK_LEVEL = 20;*/

    private static final int VOID_UNLOCK_LEVEL = 1;
    private static final int FADE_UNLOCK_LEVEL = 2;
    private static final int TELEPORT_UNLOCK_LEVEL = 3;
    private static final int SHIELD_UNLOCK_LEVEL = 4;
    private static final int TIME_SLOW_UNLOCK_LEVEL = 5;
    private static final int DRONE_UNLOCK_LEVEL = 6;
    private static final int NOVA_UNLOCK_LEVEL = 7;
    
    public void addXP(int amount) {
        xp += amount;
        
        // Check for level up
        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel;
            level++;
            xpToNextLevel = calculateNextLevelXP();
        }
    }
    
    private int calculateNextLevelXP() {
        // Exponential growth: 20, 30, 45, 68, 102, 153, ...
        return (int)(20 * Math.pow(1.5, level - 1));
    }
    
    public boolean isAbilityUnlocked(String ability) {
        return switch (ability.toLowerCase()) {
            case "void" -> level >= VOID_UNLOCK_LEVEL;
            case "fade" -> level >= FADE_UNLOCK_LEVEL;
            case "teleport" -> level >= TELEPORT_UNLOCK_LEVEL;
            case "shield" -> level >= SHIELD_UNLOCK_LEVEL;
            case "timeslow" -> level >= TIME_SLOW_UNLOCK_LEVEL;
            case "drone" -> level >= DRONE_UNLOCK_LEVEL;
            case "nova" -> level >= NOVA_UNLOCK_LEVEL;
            default -> false;
        };
    }
    /*
   public boolean isAbilityUnlocked(String ability) {
        return switch (ability.toLowerCase()) {
            case "void" -> true;
            case "fade" -> true;
            case "teleport" -> true;
            case "shield" -> true;
            case "timeslow" -> true;
            case "drone" -> true;
            case "nova" -> true;
            default -> false;
        };
    }*/
    public void drawLevelUI(Graphics2D g2, int screenWidth, int screenHeight) {
        int panelX = 20;
        int panelY = 90;
        int panelWidth = 200;
        int panelHeight = 120;
        
        // Panel background
        g2.setColor(new Color(20, 30, 50, 200));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        
        // Border with level-based color
        Color borderColor = getLevelColor();
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        g2.setStroke(new BasicStroke(1));
        
        // Level text
        g2.setFont(new Font("Serif", Font.BOLD, 20));
        g2.setColor(borderColor);
        String levelText = "SHIP LEVEL " + level;
        g2.drawString(levelText, panelX + 15, panelY + 28);
        
        // XP bar background
        int barX = panelX + 15;
        int barY = panelY + 40;
        int barWidth = panelWidth - 30;
        int barHeight = 20;
        
        g2.setColor(new Color(30, 40, 60));
        g2.fillRect(barX, barY, barWidth, barHeight);
        
        // XP bar fill
        float xpPercent = (float)xp / xpToNextLevel;
        int fillWidth = (int)(barWidth * xpPercent);
        
        GradientPaint xpGradient = new GradientPaint(
            barX, barY, new Color(100, 200, 255),
            barX + barWidth, barY, new Color(0, 150, 255)
        );
        g2.setPaint(xpGradient);
        g2.fillRect(barX, barY, fillWidth, barHeight);
        
        // XP text
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        String xpText = xp + " / " + xpToNextLevel + " XP";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(xpText, barX + barWidth/2 - fm.stringWidth(xpText)/2, barY + 15);
        /*
        // Ability icons
        drawAbilityIcon(g2, "V", "Void", isAbilityUnlocked("void"), 
            panelX + 15, panelY + 75, new Color(180, 0, 255));
        drawAbilityIcon(g2, "F", "Fade", isAbilityUnlocked("fade"), 
            panelX + 75, panelY + 75, new Color(255, 215, 0));
        drawAbilityIcon(g2, "T", "Tele", isAbilityUnlocked("teleport"), 
            panelX + 135, panelY + 75, new Color(0, 255, 200));
            
        */
    }
    
    private void drawAbilityIcon(Graphics2D g2, String key, String name, 
                                 boolean unlocked, int x, int y, Color color) {
        int size = 30;
        
        // Icon background
        if (unlocked) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
            g2.fillRoundRect(x, y, size, size, 5, 5);
            g2.setColor(color);
        } else {
            g2.setColor(new Color(40, 40, 40, 150));
            g2.fillRoundRect(x, y, size, size, 5, 5);
            g2.setColor(new Color(80, 80, 80));
        }
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, size, size, 5, 5);
        g2.setStroke(new BasicStroke(1));
        
        // Key letter
        g2.setFont(new Font("Serif", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(key, x + size/2 - fm.stringWidth(key)/2, y + size/2 + fm.getAscent()/2 - 2);
    }
    
    public Color getLevelColor() {
        if (level < 3) return new Color(150, 150, 150); // Gray
        if (level < 5) return new Color(100, 200, 100); // Green
        if (level < 7) return new Color(100, 150, 255); // Blue
        if (level < 10) return new Color(200, 100, 255); // Purple
        return new Color(255, 215, 0); // Gold
    }
    
    public void drawLevelUpEffect(Graphics2D g2, int shipx, int shipy, int WIDTH, int HEIGHT, int frame) {
        // Expanding circle effect
        int maxRadius = 400;
        int currentRadius = (int)(maxRadius * (frame / 60.0));
        float alpha = 1.0f - (frame / 60.0f);
        
        if (alpha > 0) {
            Color levelColor = getLevelColor();
            g2.setColor(new Color(
                levelColor.getRed(), 
                levelColor.getGreen(), 
                levelColor.getBlue(), 
                (int)(150 * alpha)
            ));
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(
                shipx - currentRadius, 
                shipy - currentRadius,
                currentRadius * 2, 
                currentRadius * 2
            );
            g2.setStroke(new BasicStroke(1));
            
            // "LEVEL UP!" text
            if (frame < 45) {
                g2.setFont(new Font("Serif", Font.BOLD, 60));
                g2.setColor(new Color(255, 255, 255, (int)(255 * alpha)));
                String text = "LEVEL UP!";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, 
                    WIDTH / 2 - fm.stringWidth(text)/2,
                    HEIGHT / 2 + fm.getAscent()/2);
            }
        }
    }
    
    // Getters
    public int getLevel() { return level; }
    public int getXP() { return xp; }
    public int getXPToNextLevel() { return xpToNextLevel; }
    
    // Setters for save/load
    public void setLevel(int level) { 
        this.level = level;
        this.xpToNextLevel = calculateNextLevelXP();
    }
    public void setXP(int xp) { this.xp = xp; }
    public void setXPToNextLevel(int xp) { this.xpToNextLevel = xp; }
}