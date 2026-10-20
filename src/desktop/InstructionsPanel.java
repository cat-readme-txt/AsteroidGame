import java.awt.*;
import java.awt.event.KeyEvent;

public final class InstructionsPanel {
    private final ControlConfig controlConfig;
    private int scroll;
    private int maxScroll;
    private Rectangle closeBounds = new Rectangle();

    public InstructionsPanel(ControlConfig controlConfig) {
        this.controlConfig = controlConfig;
    }

    public boolean click(int x, int y) { return closeBounds.contains(x, y); }

    public void scroll(int amount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll + amount * 36));
    }

    public void draw(Graphics2D g2, int screenWidth, int screenHeight) {
        int x = Math.max(24, screenWidth / 12);
        int y = Math.max(24, screenHeight / 12);
        int w = screenWidth - 2 * x;
        int h = screenHeight - 2 * y;
        g2.setColor(new Color(12, 17, 34, 250));
        g2.fillRoundRect(x, y, w, h, 16, 16);
        g2.setColor(new Color(95, 200, 220, 170));
        g2.drawRoundRect(x, y, w, h, 16, 16);
        closeBounds = new Rectangle(x + w - 44, y + 12, 30, 30);
        g2.setColor(new Color(70, 85, 110, 180));
        g2.fillRoundRect(closeBounds.x, closeBounds.y, 30, 30, 7, 7);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2.drawString("×", closeBounds.x + 9, closeBounds.y + 21);
        Shape clip = g2.getClip();
        g2.setClip(x + 22, y + 48, w - 44, h - 70);
        g2.translate(0, -scroll);
        int bottom = drawInstructionsMiddle(g2, x + 36, y + 82, w - 92);
        g2.translate(0, scroll);
        g2.setClip(clip);
        maxScroll = Math.max(0, bottom - (y + h - 26));
        scroll = Math.min(scroll, maxScroll);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(155, 170, 190));
        g2.drawString(maxScroll > 0 ? "Scroll to read  ·  Esc to close" : "Esc to close",
                x + 36, y + h - 13);
    }

    private int drawInstructionsMiddle(Graphics2D g2, int x, int y, int width) {
        int textWidth = Math.max(180, width - 28);
        Color cyan = new Color(100, 220, 235);
        Color amber = new Color(255, 205, 120);
        Color violet = new Color(205, 155, 255);
        Color green = new Color(145, 225, 175);
        Color body = new Color(225, 232, 242);

        g2.setFont(new Font("SansSerif", Font.BOLD, 25));
        g2.setColor(Color.WHITE);
        g2.drawString("How to play", x, y);
        y += 16;
        g2.setColor(cyan);
        g2.fillRoundRect(x, y, textWidth, 2, 2, 2);
        y += 30;

        y = drawInstructionHeading(g2, "FLIGHT", x, y, cyan);
        y = drawInstructionControl(g2, "thrust", "Thrust forward; release to coast.", x, y, textWidth, cyan);
        y = drawInstructionControl(g2, "left", "Turn left.", x, y, textWidth, cyan);
        y = drawInstructionControl(g2, "right", "Turn right.", x, y, textWidth, cyan);
        y = drawInstructionControl(g2, "hyper", "Use hyper mode for a speed boost.", x, y, textWidth, cyan);
        y = drawInstructionBullet(g2, "The ship and asteroids wrap around screen edges.", x, y, textWidth, cyan, body);
        y += 14;

        y = drawInstructionHeading(g2, "COMBAT & M-AM", x, y, amber);
        y = drawInstructionControl(g2, "shoot", "Fire bullets; Void mode or stored energy fires a laser.", x, y, textWidth, amber);
        y = drawInstructionBullet(g2, "In this sci-fi setting, the ship runs on matter-antimatter (M-AM) fuel. Its highly efficient reactor turns a tiny reserve into sustained thrust and hyper mode.", x, y, textWidth, amber, body);
        y = drawInstructionBullet(g2, "Destroy asteroids: recovered matter feeds core-powered antimatter synthesis and replenishes M-AM. Larger asteroids split.", x, y, textWidth, amber, body);
        y += 14;

        y = drawInstructionHeading(g2, "VOID WORLD", x, y, violet);
        y = drawInstructionBullet(g2, "Use Void to cross asteroids and build energy for lasers.", x, y, textWidth, violet, body);
        y = drawInstructionBullet(g2, "Stored Void energy gives fuel-free thrust faster than M-AM hyper; hyper adds more speed. Turning rate stays the same. Outside Void, thrust, turns, and lasers discharge stored energy.", x, y, textWidth, violet, body);
        y = drawInstructionBullet(g2, "Void energy at 100 ends the run unless a charged Shield utility clears it.", x, y, textWidth, violet, new Color(255, 190, 190));
        y = drawInstructionBullet(g2, "Void creatures are lethal on contact.", x, y, textWidth, violet, body);
        y += 14;

        y = drawInstructionHeading(g2, "ABILITIES", x, y, green);
        String slotKeys = controlConfig.getKeyName(controlConfig.getKey("ability1")) + ", "
            + controlConfig.getKeyName(controlConfig.getKey("ability2")) + ", "
            + controlConfig.getKeyName(controlConfig.getKey("ability3")) + ", "
            + controlConfig.getKeyName(controlConfig.getKey("ability4"));
        y = drawInstructionBullet(g2, "Void uses its own key (" + controlConfig.getKeyName(controlConfig.getKey("void")) + "). Constructed abilities use " + slotKeys + "; edit keys in Key bindings.", x, y, textWidth, green, body);
        y = drawInstructionBullet(g2, "Press C to construct with Incident Essence. Ability construction keys are editable in Key bindings.", x, y, textWidth, green, body);
        y = drawInstructionBullet(g2, "Ctrl+A opens the armory. Ctrl+an ability slot key upgrades its equipped ability for 10 Incident Essence; an empty slot opens ability construction.", x, y, textWidth, green, body);
        y = drawInstructionBullet(g2, "Assign a new ability to an empty slot by click or slot number.", x, y, textWidth, green, body);
        y = drawInstructionBullet(g2, "Construction: 1 Utilities, 2 Weapons, 3 Abilities, 4 Return. Utilities have four separate passive slots.", x, y, textWidth, green, body);
        y = drawInstructionBullet(g2, "Energy Modulator lets idle Void energy rise naturally to 100; player actions above 50 drain it, while actions below 50 speed its gain. Its max level lets you absorb weakened black holes. Swarm bots attack separate targets. Metric Divergence slows approaching asteroids and enemies across visible rings. Shield is passive.", x, y, textWidth, green, body);
        y = drawInstructionBullet(g2, "After absorbing a black hole, construct the Black Hole ability. Each level needs one more absorbed hole and 10 Essence. Hold its slot key to aim farther, release to launch; Tab cancels. Cooldowns: 30/20/10 seconds.", x, y, textWidth, green, body);
        y += 14;

        y = drawInstructionHeading(g2, "LATER THREATS", x, y, amber);
        y = drawInstructionBullet(g2, "Cosmic entities and void creatures appear from level 3. Their sizes use 1, 2, or 3 spawn weight.", x, y, textWidth, amber, body);
        y = drawInstructionBullet(g2, "Defeat them for Incident Essence. New Game resets the balance.", x, y, textWidth, violet, body);
        y = drawInstructionBullet(g2, "Black holes appear from level 8 after a warning. Void lasers shrink them. A maxed Energy Modulator marks small holes for pickup; absorption grants infinite M-AM.", x, y, textWidth, amber, body);
        return y + 20;
    }

    private int drawInstructionHeading(Graphics2D g2, String title, int x, int y, Color accent) {
        g2.setColor(accent);
        g2.fillRoundRect(x, y - 14, 4, 17, 3, 3);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.drawString(title, x + 14, y);
        return y + 26;
    }

    private int drawInstructionControl(Graphics2D g2, String action, String description,
                                       int x, int y, int width, Color accent) {
        String key = controlConfig.getKeyName(controlConfig.getKey(action));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        int keyWidth = Math.max(62, g2.getFontMetrics().stringWidth(key) + 18);
        g2.setColor(new Color(30, 48, 65, 160));
        g2.fillRoundRect(x, y - 16, keyWidth, 23, 6, 6);
        g2.setColor(accent);
        g2.drawString(key, x + 9, y);
        int descriptionY = drawInstructionText(g2, description, x + keyWidth + 12, y,
            width - keyWidth - 12, new Color(225, 232, 242));
        return Math.max(y + 28, descriptionY + 5);
    }

    private int drawInstructionBullet(Graphics2D g2, String text, int x, int y,
                                      int width, Color accent, Color body) {
        g2.setColor(accent);
        g2.fillOval(x + 3, y - 7, 6, 6);
        return drawInstructionText(g2, text, x + 18, y, width - 18, body) + 4;
    }

    private int drawInstructionText(Graphics2D g2, String text, int x, int y,
                                    int width, Color color) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.setColor(color);
        FontMetrics metrics = g2.getFontMetrics();
        int lineWidth = Math.max(50, width);
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && metrics.stringWidth(candidate) > lineWidth) {
                g2.drawString(line.toString(), x, y);
                y += 19;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (line.length() > 0) {
            g2.drawString(line.toString(), x, y);
            y += 19;
        }
        return y;
    }

}
