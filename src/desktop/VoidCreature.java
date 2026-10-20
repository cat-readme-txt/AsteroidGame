import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

public class VoidCreature {
    private static final Random rand = new Random();
    public static final int MIN_SIZE = CosmicEntity.MIN_SIZE;
    public static final int MAX_SIZE = CosmicEntity.MAX_SIZE;

    public enum SizeTier {
        SMALL(1, MIN_SIZE, 68),
        MEDIUM(2, 69, 139),
        LARGE(3, 140, MAX_SIZE);

        private final int weight;
        private final int minSize;
        private final int maxSize;

        SizeTier(int weight, int minSize, int maxSize) {
            this.weight = weight;
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        public int getWeight() { return weight; }
    }

    private double x, y;
    private double vx, vy;
    private final int size;
    private final double speed;
    private final int maxHP;
    private int currentHP;
    private float animPhase;
    private boolean isInVoid;
    private final int screenWidth, screenHeight;
    private final Rectangle bounds = new Rectangle();

    private VoidCreature(int screenWidth, int screenHeight, int size) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.size = size;
        speed = Math.max(0.6, Math.min(3.5, 3.5 - size * 0.016));
        maxHP = bulletHitsForSize(size) * 25;
        currentHP = maxHP;

        int outside = size / 2 + 20;
        if (rand.nextBoolean()) {
            x = rand.nextBoolean() ? -outside : screenWidth + outside;
            y = rand.nextInt(Math.max(1, screenHeight));
        } else {
            x = rand.nextInt(Math.max(1, screenWidth));
            y = rand.nextBoolean() ? -outside : screenHeight + outside;
        }
        updateVelocity(screenWidth / 2.0, screenHeight / 2.0);
    }

    public static VoidCreature createSpecter(int screenWidth, int screenHeight, int level) {
        return createSpecter(screenWidth, screenHeight, level, 3);
    }

    public static VoidCreature createSpecter(int screenWidth, int screenHeight, int level, int maxWeight) {
        if (maxWeight < 1) throw new IllegalArgumentException("maxWeight must be positive");
        SizeTier tier = SizeTier.values()[rand.nextInt(Math.min(maxWeight, 3))];
        int size = tier.minSize + rand.nextInt(tier.maxSize - tier.minSize + 1);
        return new VoidCreature(screenWidth, screenHeight, size);
    }

    public static VoidCreature createTier(int screenWidth, int screenHeight, SizeTier tier) {
        if (tier == null) throw new IllegalArgumentException("tier required");
        int size = tier.minSize + rand.nextInt(tier.maxSize - tier.minSize + 1);
        return new VoidCreature(screenWidth, screenHeight, size);
    }

    public void updateVelocity(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 0) {
            vx = dx / distance * speed;
            vy = dy / distance * speed;
        }
    }

    public void update(double shipX, double shipY, boolean voidActive) {
        isInVoid = voidActive;
        if (voidActive) {
            updateVelocity(shipX, shipY);
            x += vx;
            y += vy;
        } else {
            double drift = Math.max(0.32, speed * 0.42);
            boolean outside = x < 0 || x > screenWidth || y < 0 || y > screenHeight;
            double angle = outside
                    ? Math.atan2(screenHeight / 2.0 - y, screenWidth / 2.0 - x)
                    : Math.atan2(vy, vx) - 0.015;
            vx = Math.cos(angle) * drift;
            vy = Math.sin(angle) * drift;
            x += vx;
            y += vy;
            int margin = size + 24;
            if (x < -margin) x = screenWidth + margin;
            else if (x > screenWidth + margin) x = -margin;
            if (y < -margin) y = screenHeight + margin;
            else if (y > screenHeight + margin) y = -margin;
        }
        animPhase += 0.08f;
    }

    public void moveBy(double dx, double dy) { x += dx; y += dy; }

    public void takeDamage(int damage) {
        currentHP = Math.max(0, currentHP - damage);
    }

    public boolean isDead() { return currentHP <= 0; }

    static int bulletHitsForSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) throw new IllegalArgumentException("size out of range");
        return 1 + (int)Math.round((size - MIN_SIZE) * 9.0 / (MAX_SIZE - MIN_SIZE));
    }

    public SizeTier getSizeTier() {
        if (size <= SizeTier.SMALL.maxSize) return SizeTier.SMALL;
        if (size <= SizeTier.MEDIUM.maxSize) return SizeTier.MEDIUM;
        return SizeTier.LARGE;
    }

    public int getWeight() { return getSizeTier().getWeight(); }
    public int getSizeValue() { return getWeight(); }
    public int getSize() { return size; }
    public int getMaxHP() { return maxHP; }
    public int getCurrentHP() { return currentHP; }
    public double getX() { return x; }
    public double getY() { return y; }

    public Rectangle getBounds() {
        bounds.setBounds((int)Math.round(x - size / 2.0), (int)Math.round(y - size / 2.0), size, size);
        return bounds;
    }

    public boolean intersects(Polygon shipBounds) {
        if (!isInVoid || isDead() || shipBounds.npoints == 0) return false;
        Rectangle2D broadBounds = shipBounds.getBounds();
        return intersectsCircle(shipBounds, broadBounds, x, y, size * 0.35);
    }

    private static boolean intersectsCircle(Polygon shipBounds, Rectangle2D broadBounds,
            double cx, double cy, double radius) {
        if (!broadBounds.intersects(cx - radius, cy - radius, radius * 2, radius * 2)) return false;
        if (shipBounds.contains(cx, cy)) return true;
        double radiusSquared = radius * radius;
        for (int i = 0; i < shipBounds.npoints; i++) {
            int next = (i + 1) % shipBounds.npoints;
            if (Line2D.ptSegDistSq(shipBounds.xpoints[i], shipBounds.ypoints[i],
                    shipBounds.xpoints[next], shipBounds.ypoints[next], cx, cy) < radiusSquared) return true;
        }
        return false;
    }

    public boolean intersects(Rectangle2D rectangle) {
        if (!isInVoid || isDead()) return false;
        double radius = size * 0.35;
        double dx = x - Math.max(rectangle.getMinX(), Math.min(x, rectangle.getMaxX()));
        double dy = y - Math.max(rectangle.getMinY(), Math.min(y, rectangle.getMaxY()));
        return dx * dx + dy * dy < radius * radius;
    }

    public boolean intersects(Line2D line) {
        if (!isInVoid || isDead()) return false;
        double radius = size * 0.35;
        return line.ptSegDistSq(x, y) < radius * radius;
    }

    public void draw(Graphics2D g2d, boolean voidMode) {
        if (!voidMode) {
            Composite old = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.09f));
            drawSpecter(g2d);
            g2d.setComposite(old);
        } else {
            drawSpecter(g2d);
            drawHPBar(g2d);
        }
    }

    private void drawSpecter(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        try {
            g.translate(x, y);
            g.scale(size, size);
            float hue = (float) (0.77 + 0.018 * Math.sin(animPhase * 0.4));
            Color shell = Color.getHSBColor(hue, 0.80f, 0.37f);
            Color rim = Color.getHSBColor(hue, 0.53f, 0.92f);
            Color eye = Color.getHSBColor(hue, 0.20f, 1f);
            g.setStroke(new BasicStroke(Math.max(0.012f, 1.1f / size),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Six independently undulating hooks surround a dark dimensional tear.
            for (int arm = 0; arm < 6; arm++) {
                Graphics2D limb = (Graphics2D) g.create();
                try {
                    limb.rotate(arm * Math.PI / 3 + 0.13 * Math.sin(animPhase * 0.3));
                    double wave = 0.055 * Math.sin(animPhase * 1.35 + arm * 1.4);
                    Path2D.Double hook = new Path2D.Double();
                    hook.moveTo(-0.045, -0.13);
                    hook.curveTo(-0.14, -0.22, -0.21 + wave, -0.38,
                            -0.12 + wave, -0.46);
                    hook.curveTo(-0.075 + wave, -0.50, -0.055 + wave, -0.53,
                            -0.12 + wave, -0.53);
                    hook.curveTo(0.045 + wave, -0.55, 0.065 + wave, -0.46,
                            0.015 + wave, -0.40);
                    hook.curveTo(-0.025 + wave, -0.32, 0.12, -0.24, 0.045, -0.13);
                    hook.closePath();
                    limb.setColor(shell);
                    limb.fill(hook);
                    limb.setColor(rim);
                    limb.draw(hook);
                    limb.setColor(new Color(212, 166, 255, 115));
                    Path2D.Double nerve = new Path2D.Double();
                    nerve.moveTo(0, -0.18);
                    nerve.curveTo(-0.03, -0.26, -0.10 + wave, -0.36,
                            -0.045 + wave, -0.46);
                    limb.draw(nerve);
                } finally {
                    limb.dispose();
                }
            }

            Path2D.Double body = new Path2D.Double();
            for (int point = 0; point <= 24; point++) {
                double angle = point * Math.PI / 12;
                double radius = 0.235 + 0.024 * Math.sin(angle * 6 + animPhase * 1.1);
                double px = Math.cos(angle) * radius, py = Math.sin(angle) * radius;
                if (point == 0) body.moveTo(px, py);
                else body.lineTo(px, py);
            }
            body.closePath();
            g.setColor(new Color(22, 5, 42));
            g.fill(body);
            g.setColor(rim);
            g.draw(body);
            g.setColor(new Color(93, 28, 151));
            g.fill(new Ellipse2D.Double(-0.17, -0.17, 0.34, 0.34));
            g.setColor(new Color(4, 1, 12));
            g.fill(new Ellipse2D.Double(-0.13, -0.13, 0.26, 0.26));

            // Detached eyes circle the body rather than forming a face.
            for (int orbit = 0; orbit < 4; orbit++) {
                double angle = animPhase * (orbit % 2 == 0 ? 0.47 : -0.35)
                        + orbit * Math.PI / 2;
                double radius = 0.32 + 0.035 * Math.sin(animPhase * 0.9 + orbit);
                double ex = Math.cos(angle) * radius, ey = Math.sin(angle) * radius;
                double eyeRadius = orbit == 0 ? 0.046 : 0.033;
                g.setColor(new Color(26, 4, 42));
                g.fill(new Ellipse2D.Double(ex - eyeRadius * 1.35, ey - eyeRadius * 1.35,
                        eyeRadius * 2.7, eyeRadius * 2.7));
                g.setColor(rim);
                g.draw(new Ellipse2D.Double(ex - eyeRadius * 1.35, ey - eyeRadius * 1.35,
                        eyeRadius * 2.7, eyeRadius * 2.7));
                g.setColor(eye);
                g.fill(new Ellipse2D.Double(ex - eyeRadius, ey - eyeRadius,
                        eyeRadius * 2, eyeRadius * 2));
                g.setColor(new Color(29, 5, 53));
                g.fill(new Ellipse2D.Double(ex - eyeRadius * 0.30, ey - eyeRadius * 0.30,
                        eyeRadius * 0.60, eyeRadius * 0.60));
            }
        } finally {
            g.dispose();
        }
    }

    private void drawHPBar(Graphics2D g2d) {
        if (currentHP == maxHP) return;
        int barWidth = Math.max(10, size);
        int barHeight = 4;
        int barX = (int)x - barWidth / 2;
        int barY = (int)y - size / 2 - 12;

        g2d.setColor(new Color(35, 46, 54, 180));
        g2d.fillRect(barX, barY, barWidth, barHeight);
        float hpPercent = (float)currentHP / maxHP;
        g2d.setColor(hpPercent > 0.5f ? new Color(186, 128, 255) :
                hpPercent > 0.25f ? new Color(255, 208, 101) : new Color(255, 108, 117));
        g2d.fillRect(barX, barY, (int)(barWidth * hpPercent), barHeight);
        g2d.setColor(new Color(216, 184, 255, 180));
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
}
