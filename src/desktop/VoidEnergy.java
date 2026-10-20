import java.awt.*;

public class VoidEnergy {
    private static final Font BAR_FONT = new Font("SansSerif", Font.BOLD, 11);
    private double energy = 0;
    private final double maxEnergy = 100;
    private final double absorptionRate = 0.1; // Base passive absorption rate
    private double actionMultiplier = 1.0; // Increases with actions
    private boolean active = false;
    private int modulatorLevel;

    public void setModulatorLevel(int level) {
        modulatorLevel = Math.max(0, Math.min(3, level));
    }

    public int getModulatorLevel() { return modulatorLevel; }
    
    public void activate() {
        active = true;
    }
    
    public void deactivate() {
        active = false;
        actionMultiplier = 1.0;
    }
    
    public void update() {
        if (active) {
            // Passive absorption continues even with the modulator at high energy.
            energy += absorptionRate * (modulatorLevel > 0 && energy > 50
                    ? 1.0 : actionMultiplier);
            if (energy > maxEnergy) { energy = maxEnergy;  }
            // Reset multiplier when no action
            if (actionMultiplier > 1.0) {
                actionMultiplier -= 0.02;
                if (actionMultiplier < 1.0) actionMultiplier = 1.0;
            }
        } else if (energy > 0) {
            // Slowly dissipate when not in void world
            energy -= 0.05;
            if (energy < 0) energy = 0;
            // Reset Multiplier
            actionMultiplier = 1.0;
        }
    }
    
    public void dissipateForAction(double amount) {
        if (energy > 0) {
            energy -= amount;
            if (energy > maxEnergy) {
                energy = maxEnergy;
            }
            // Increase action multiplier (caps at 3x)
            actionMultiplier = Math.min(actionMultiplier + 0.05, 3.0);
        }
    }

    public void absorbForAction(double amount) {
        if (energy > 0) {
            if (active && modulatorLevel > 0 && energy > 50) {
                energy = Math.max(0, energy - amount * (0.75 + 0.5 * modulatorLevel));
                actionMultiplier = 1.0;
            } else {
                energy = Math.min(maxEnergy, energy + amount);
                actionMultiplier = Math.min(actionMultiplier + 0.05, 3.0);
            }
        }
    }
    
    public boolean isOverThreshold() {
        return energy >= maxEnergy;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public double getEnergy() {
        return energy;
    }

    public void setEnergyForDeveloper(double value) {
        energy = Math.max(0, Math.min(maxEnergy, value));
    }
    
    public double getMaxEnergy() {
        return maxEnergy;
    }
    
    public double getEnergyPercent() {
        return energy / maxEnergy;
    }
    
    public void reset() {
        energy = 0;
        active = false;
        actionMultiplier = 1.0;
    }
    
    public void drawBar(Graphics2D g2d, int screenWidth, int screenHeight) {
        if (energy <= 0) return;

        int barWidth = Math.min(260, screenWidth - 32);
        int barHeight = 22;
        int barX = screenWidth / 2 - barWidth / 2;
        int barY = screenHeight - 120;

        g2d.setColor(new Color(20, 25, 35, 115));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 9, 9);
        int fillWidth = Math.max(0, (int) ((barWidth - 4) * getEnergyPercent()));
        if (fillWidth > 0) {
            g2d.setColor(getEnergyPercent() > 0.8
                    ? new Color(220, 75, 156, 190) : new Color(155, 90, 225, 190));
            g2d.fillRoundRect(barX + 2, barY + 2, fillWidth, barHeight - 4, 7, 7);
        }
        g2d.setColor(new Color(160, 120, 220, 190));
        g2d.drawRoundRect(barX, barY, barWidth, barHeight, 9, 9);

        g2d.setColor(Color.WHITE);
        g2d.setFont(BAR_FONT);
        String text = "VOID ENERGY  " + (int) Math.round(getEnergyPercent() * 100) + "%";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, barX + barWidth / 2 - fm.stringWidth(text) / 2,
                barY + barHeight / 2 + fm.getAscent() / 2 - 2);
    }

    public double getAbsorptionRate() {
        return absorptionRate;
    }
}
