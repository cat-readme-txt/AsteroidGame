import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;

public class AbilityManager {
    private static final int FPS = 60;
    private static final int SHIELD_BURST_FRAMES = 22;

    private static final class Ability {
        final String name;
        final Color color;
        int level;
        int cooldownTimer;
        int maxCooldown = 1;
        int flashTimer;
        boolean active;

        Ability(String name, Color color) {
            this.name = name;
            this.color = color;
        }

        void update() {
            if (cooldownTimer > 0 && --cooldownTimer == 0) flashTimer = 20;
            if (flashTimer > 0) flashTimer--;
        }

        void startCooldown() {
            cooldownTimer = maxCooldown;
        }

        boolean isOnCooldown() { return cooldownTimer > 0; }
        double cooldownPercent() {
            return maxCooldown <= 0 ? 0 : Math.min(1.0, (double) cooldownTimer / maxCooldown);
        }
    }

    private final Ability voidAbility = new Ability("Void", new Color(185, 105, 255));
    private final Ability teleportAbility = new Ability("Teleport", new Color(90, 225, 205));
    private final Ability shieldAbility = new Ability("Shield", new Color(175, 115, 255));
    private final Ability droneAbility = new Ability("Drone", new Color(140, 215, 255));
    private final Ability novaAbility = new Ability("Nova", new Color(255, 150, 235));
    private final Ability blackHoleAbility = new Ability("Black Hole", new Color(185, 105, 255));
    private final DroneSwarm drones = new DroneSwarm();

    private int shieldChargeFrames;
    private int shieldBurstFrames;
    private int lastShieldBurstRadius = 160;
    private boolean shieldReady;

    public enum NovaState { READY, CHARGING, EXPLODING, COOLDOWN }
    private NovaState novaState = NovaState.READY;
    private int novaChargeTimer;
    private int novaExplosionTimer;
    private double novaExplosionRadius;
    private static final int NOVA_CHARGE_FRAMES = FPS;
    private static final int NOVA_EXPLOSION_FRAMES = FPS;

    public AbilityManager() {
        voidAbility.level = 1;
        voidAbility.maxCooldown = 3 * FPS;
        teleportAbility.maxCooldown = 10 * FPS;
        novaAbility.maxCooldown = 30 * FPS;
        blackHoleAbility.maxCooldown = 30 * FPS;
    }

    private Ability find(String name) {
        if (name == null) return null;
        return switch (name.toLowerCase()) {
            case "void" -> voidAbility;
            case "teleport" -> teleportAbility;
            case "shield" -> shieldAbility;
            case "drone" -> droneAbility;
            case "nova" -> novaAbility;
            case "blackhole" -> blackHoleAbility;
            default -> null;
        };
    }

    public int getLevel(String name) {
        Ability ability = find(name);
        return ability == null ? 0 : ability.level;
    }

    public boolean isEnabled(String name) {
        Ability ability = find(name);
        return ability != null && ability.level > 0;
    }

    public int getCooldownRemainingFrames(String name) {
        Ability ability = find(name);
        if (ability == null || ability.level == 0) return 0;
        if (ability == shieldAbility) return shieldReady ? 0 : Math.max(0, shieldChargeTime() - shieldChargeFrames);
        if (ability == droneAbility) return drones.getRechargeRemainingFrames();
        return ability.cooldownTimer;
    }

    public boolean setCooldownRemainingFrames(String name, int frames) {
        Ability ability = find(name);
        if (ability == null || ability.level == 0 || frames < 0) return false;
        if (ability == shieldAbility) {
            if (frames > shieldChargeTime()) return false;
            shieldReady = frames == 0;
            shieldChargeFrames = shieldChargeTime() - frames;
        } else if (ability == droneAbility) {
            if (frames > 12 * FPS || (drones.getCharges() >= drones.getMaxCharges() && frames > 0)) return false;
            drones.setRechargeRemainingFrames(frames);
        } else {
            ability.cooldownTimer = frames;
            if (ability == novaAbility) novaState = frames == 0 ? NovaState.READY : NovaState.COOLDOWN;
        }
        return true;
    }

    public void clearDrones() { drones.clearActive(); }

    public void setLevel(String name, int level) {
        Ability ability = find(name);
        if (ability == null) return;
        int next = ability == voidAbility ? 1 : Math.max(0, Math.min(3, level));
        if (next == ability.level) return;
        int previous = ability.level;
        double cooldownFraction = ability.maxCooldown > 0
                ? (double) ability.cooldownTimer / ability.maxCooldown : 0;
        ability.level = next;
        if (ability == teleportAbility) {
            ability.maxCooldown = switch (next) {
                case 2 -> 5 * FPS;
                case 3 -> 2 * FPS;
                default -> 10 * FPS;
            };
            ability.cooldownTimer = (int) Math.ceil(cooldownFraction * ability.maxCooldown);
        } else if (ability == novaAbility) {
            ability.maxCooldown = switch (next) {
                case 2 -> 20 * FPS;
                case 3 -> 10 * FPS;
                default -> 30 * FPS;
            };
            ability.cooldownTimer = (int) Math.ceil(cooldownFraction * ability.maxCooldown);
        } else if (ability == blackHoleAbility) {
            ability.maxCooldown = switch (next) {
                case 2 -> 20 * FPS;
                case 3 -> 10 * FPS;
                default -> 30 * FPS;
            };
            ability.cooldownTimer = (int) Math.ceil(cooldownFraction * ability.maxCooldown);
        } else if (ability == shieldAbility) {
            if (previous == 0 || next == 0) {
                shieldChargeFrames = 0;
                shieldReady = false;
                shieldBurstFrames = 0;
            } else if (!shieldReady) {
                shieldChargeFrames = Math.min(shieldChargeFrames, shieldChargeTime() - 1);
            }
        } else if (ability == droneAbility) {
            drones.setLevel(next);
        }
        if (next == 0) {
            ability.cooldownTimer = 0;
            ability.active = false;
        }
    }

    public void update() {
        voidAbility.update();
        teleportAbility.update();
        shieldAbility.update();
        droneAbility.update();
        novaAbility.update();
        blackHoleAbility.update();

        if (isEnabled("shield") && !shieldReady) {
            if (++shieldChargeFrames >= shieldChargeTime()) {
                shieldChargeFrames = shieldChargeTime();
                shieldReady = true;
                shieldAbility.flashTimer = 20;
            }
        }
        if (shieldBurstFrames > 0) shieldBurstFrames--;
        if (isEnabled("drone")) drones.updateCharge();

        if (novaState == NovaState.CHARGING) {
            if (++novaChargeTimer >= NOVA_CHARGE_FRAMES) {
                novaState = NovaState.EXPLODING;
                novaChargeTimer = 0;
                novaExplosionTimer = 0;
            }
        } else if (novaState == NovaState.EXPLODING) {
            novaExplosionRadius = 1000.0 * ++novaExplosionTimer / NOVA_EXPLOSION_FRAMES;
            if (novaExplosionTimer >= NOVA_EXPLOSION_FRAMES) {
                novaState = NovaState.COOLDOWN;
                novaAbility.startCooldown();
                novaExplosionRadius = 0;
            }
        } else if (novaState == NovaState.COOLDOWN && !novaAbility.isOnCooldown()) {
            novaState = NovaState.READY;
        }
    }

    private int shieldChargeTime() {
        return switch (shieldAbility.level) {
            case 2 -> 12 * FPS;
            case 3 -> 7 * FPS;
            default -> 18 * FPS;
        };
    }

    public boolean isShieldReady() { return shieldReady && isEnabled("shield"); }
    public double getShieldChargePercent() {
        return shieldAbility.level == 0 ? 0
                : shieldReady ? 1 : (double) shieldChargeFrames / shieldChargeTime();
    }
    public double getShieldBoundaryRadius() { return 34; }
    public int getShieldBurstRadius(int width, int height) {
        return switch (shieldAbility.level) {
            case 1 -> 160;
            case 2 -> 320;
            case 3 -> (int) Math.ceil(Math.hypot(width, height));
            default -> 0;
        };
    }

    public boolean triggerShield() {
        if (!isShieldReady()) return false;
        shieldReady = false;
        shieldChargeFrames = 0;
        shieldBurstFrames = SHIELD_BURST_FRAMES;
        return true;
    }

    public boolean isShieldActive() { return shieldBurstFrames > 0; }
    public double getShieldRadius() {
        return lastShieldBurstRadius * (1.0 - shieldBurstFrames / (double) SHIELD_BURST_FRAMES);
    }
    public void rememberShieldBurstRadius(int width, int height) {
        lastShieldBurstRadius = getShieldBurstRadius(width, height);
    }

    public boolean deployDrone(Ship ship) { return isEnabled("drone") && drones.deploy(ship); }
    public boolean spawnDroneForDeveloper(Ship ship) {
        return isEnabled("drone") && drones.deployForDeveloper(ship);
    }
    public void updateDrones(Ship ship, List<DroneSwarm.Target> targets, int width, int height) {
        drones.update(ship, targets, width, height);
    }
    public void updateDrones(Ship ship, List<DroneSwarm.Target> targets,
                             List<BlackHole> holes, int width, int height) {
        drones.update(ship, targets, holes, width, height);
    }
    public List<DroneSwarm.Drone> getDrones() { return drones.getDrones(); }
    public List<DroneSwarm.Shot> drainDroneShots() { return drones.drainShots(); }
    public List<DroneSwarm.Destruction> drainDroneDestructions() {
        return drones.drainDestructions();
    }
    public int getDroneCharges() { return drones.getCharges(); }
    public double getDroneChargePercent() { return drones.getChargePercent(); }
    public int getDroneMaxCharges() { return drones.getMaxCharges(); }
    public int getActiveDroneCount() { return drones.getActiveCount(); }
    public void destroyDrone(DroneSwarm.Drone drone) { drones.destroyDrone(drone); }
    public boolean activateNova(VoidEnergy ignored) {
        if (!isEnabled("nova") || novaState != NovaState.READY) return false;
        novaState = NovaState.CHARGING;
        novaChargeTimer = 0;
        return true;
    }

    public boolean canLaunchBlackHole() {
        return blackHoleAbility.level > 0 && !blackHoleAbility.isOnCooldown();
    }

    public void startBlackHoleCooldown() {
        if (blackHoleAbility.level > 0) blackHoleAbility.startCooldown();
    }
    public boolean isNovaCharging() { return novaState == NovaState.CHARGING; }
    public NovaState getNovaState() { return novaState; }
    public boolean isNovaExploding() { return novaState == NovaState.EXPLODING; }
    public double getNovaExplosionRadius() { return novaExplosionRadius; }
    public float getNovaChargePercent() {
        return novaState == NovaState.CHARGING
                ? (float) novaChargeTimer / NOVA_CHARGE_FRAMES : 0;
    }

    public void drawAbilityBar(Graphics2D g2d, int width, int height,
                               AbilityLoadout loadout, String voidKey) {
        drawAbilityBar(g2d, width, height, loadout, voidKey,
                new String[] {"1", "2", "3", "4"});
    }

    public void drawAbilityBar(Graphics2D g2d, int width, int height,
                               AbilityLoadout loadout, String voidKey, String[] slotKeys) {
        int barWidth = Math.min(400, width - 32), barHeight = 70;
        int barX = (width - barWidth) / 2, barY = height - barHeight - 20;
        Stroke oldStroke = g2d.getStroke();
        Font oldFont = g2d.getFont();
        g2d.setColor(new Color(20, 25, 35, 95));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 12, 12);
        g2d.setColor(new Color(80, 100, 130, 180));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(barX, barY, barWidth, barHeight, 12, 12);
        int spacing = barWidth / AbilityLoadout.SLOT_COUNT;
        for (int slot = 0; slot < AbilityLoadout.SLOT_COUNT; slot++) {
            Ability ability = find(loadout.getSlot(slot));
            int centerX = barX + slot * spacing + spacing / 2;
            int centerY = barY + 32;
            drawAbilityIcon(g2d, ability, centerX, centerY, slotKeys[slot]);
        }
        int voidX = Math.min(width - 42, barX + barWidth + 50);
        g2d.setColor(new Color(55, 32, 82, 105));
        g2d.fillRoundRect(voidX - 37, barY, 74, barHeight, 14, 14);
        g2d.setColor(new Color(190, 128, 255, 190));
        g2d.drawRoundRect(voidX - 37, barY, 74, barHeight, 14, 14);
        drawAbilityIcon(g2d, voidAbility, voidX, barY + 32, voidKey);
        g2d.setStroke(oldStroke);
        g2d.setFont(oldFont);
    }

    private void drawAbilityIcon(Graphics2D g2d, Ability ability,
                                 int cx, int cy, String keyLabel) {
        int size = 43, x = cx - size / 2, y = cy - size / 2;
        boolean unlocked = ability != null && ability.level > 0;
        boolean chargingPassive = (ability == shieldAbility && unlocked && !shieldReady)
                || (ability == droneAbility && unlocked && drones.getCharges() == 0);
        boolean bright = unlocked && !chargingPassive;
        g2d.setColor(new Color(bright ? 50 : 32, bright ? 60 : 35,
                bright ? 80 : 44, 165));
        g2d.fillOval(x, y, size, size);
        double progress = -1;
        if (ability == shieldAbility && unlocked && !shieldReady) {
            progress = getShieldChargePercent();
        } else if (ability == droneAbility && unlocked
                && drones.getCharges() < drones.getMaxCharges()) {
            progress = drones.getChargePercent();
        } else if (ability != null && ability.isOnCooldown()) {
            progress = 1 - ability.cooldownPercent();
        }
        if (progress >= 0) {
            g2d.setColor(new Color(20, 22, 35, 160));
            g2d.fillOval(x, y, size, size);
            g2d.setColor(new Color(245, 245, 255, 230));
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.drawArc(x - 3, y - 3, size + 6, size + 6,
                    90, -(int) (360 * Math.min(1, progress)));
        }
        if (ability != null && ability.active && unlocked) {
            g2d.setColor(ability.color);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(x - 2, y - 2, size + 4, size + 4);
        }
        g2d.setColor(bright ? ability.color : new Color(105, 112, 130));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(x, y, size, size);
        if (ability != null) {
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.setColor(bright ? Color.WHITE : new Color(140, 145, 155));
            String letter = ability.name.substring(0, 1);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(letter, cx - fm.stringWidth(letter) / 2,
                    cy + fm.getAscent() / 2 - 2);
        } else {
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2d.setColor(new Color(140, 145, 155));
            g2d.drawString("+", cx - 3, cy + 4);
        }
        g2d.setFont(new Font("SansSerif", Font.BOLD, keyLabel.length() > 4 ? 8 : 10));
        g2d.setColor(new Color(210, 220, 235));
        String label = keyLabel;
        g2d.drawString(label, cx - g2d.getFontMetrics().stringWidth(label) / 2, cy + 31);
        if (ability == droneAbility && unlocked) {
            String count = Integer.toString(drones.getCharges());
            g2d.setColor(new Color(225, 240, 255));
            g2d.drawString(count, cx + 21, cy - 12);
        }
    }

    public void drawShieldEffect(Graphics2D g2d, double shipX, double shipY) {
        Stroke oldStroke = g2d.getStroke();
        if (isShieldReady()) {
            int radius = (int) getShieldBoundaryRadius();
            g2d.setColor(new Color(160, 95, 255, 65));
            g2d.fillOval((int) shipX - radius, (int) shipY - radius,
                    radius * 2, radius * 2);
            g2d.setColor(new Color(195, 140, 255, 225));
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.drawOval((int) shipX - radius, (int) shipY - radius,
                    radius * 2, radius * 2);
        }
        if (shieldBurstFrames > 0) {
            int radius = (int) getShieldRadius();
            int alpha = (int) (170.0 * shieldBurstFrames / SHIELD_BURST_FRAMES);
            g2d.setColor(new Color(195, 140, 255, alpha));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval((int) shipX - radius, (int) shipY - radius,
                    radius * 2, radius * 2);
        }
        g2d.setStroke(oldStroke);
    }

    public void drawDrone(Graphics2D g2d, double shipX, double shipY) {
        drones.draw(g2d);
    }

    public void drawNovaBlast(Graphics2D g2d, int width, int height,
                              double shipX, double shipY) {
        Stroke oldStroke = g2d.getStroke();
        Font oldFont = g2d.getFont();
        if (novaState == NovaState.CHARGING) {
            float progress = (float) novaChargeTimer / NOVA_CHARGE_FRAMES;
            int radius = 30 + (int) (20 * Math.sin(progress * Math.PI * 4));
            g2d.setColor(new Color(255, 255, 255, (int) (190 * progress)));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawOval((int) shipX - radius, (int) shipY - radius,
                    radius * 2, radius * 2);
        } else if (novaState == NovaState.EXPLODING) {
            float progress = (float) novaExplosionTimer / NOVA_EXPLOSION_FRAMES;
            int alpha = (int) (205 * (1 - progress));
            for (int i = 0; i < 4; i++) {
                int radius = (int) novaExplosionRadius - i * 45;
                if (radius <= 0) continue;
                g2d.setColor(new Color(255, 245, 255, alpha / (i + 1)));
                g2d.setStroke(new BasicStroke(6 - i));
                g2d.drawOval((int) shipX - radius, (int) shipY - radius,
                        radius * 2, radius * 2);
            }
            g2d.setColor(new Color(255, 250, 255, alpha / 2));
            g2d.fillRect(0, 0, width, height);
        }
        g2d.setStroke(oldStroke);
        g2d.setFont(oldFont);
    }

    public void startVoidCooldown() { voidAbility.startCooldown(); }
    public boolean isVoidOnCooldown() { return voidAbility.isOnCooldown(); }
    public void setVoidActive(boolean active) { voidAbility.active = active; }
    public void setTeleportActive(boolean active) { teleportAbility.active = active; }
    public void startTeleportCooldown() { teleportAbility.startCooldown(); }
    public boolean isTeleportOnCooldown() { return teleportAbility.isOnCooldown(); }

    public void resetNova() {
        novaState = NovaState.READY;
        novaChargeTimer = novaExplosionTimer = 0;
        novaExplosionRadius = 0;
    }

    public void reset() {
        for (Ability ability : new Ability[]{
                voidAbility, teleportAbility, shieldAbility, droneAbility, novaAbility}) {
            ability.level = ability == voidAbility ? 1 : 0;
            ability.active = false;
            ability.cooldownTimer = ability.flashTimer = 0;
        }
        voidAbility.maxCooldown = 3 * FPS;
        teleportAbility.maxCooldown = 10 * FPS;
        novaAbility.maxCooldown = 30 * FPS;
        shieldChargeFrames = shieldBurstFrames = 0;
        shieldReady = false;
        lastShieldBurstRadius = 160;
        drones.reset();
        resetNova();
    }
}
