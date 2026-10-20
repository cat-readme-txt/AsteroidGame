import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.EnumSet;

public final class VoidCreatureRegressionTest {
    private static int assertions;

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void setPosition(VoidCreature creature, double x, double y) {
        try {
            Field xField = VoidCreature.class.getDeclaredField("x");
            Field yField = VoidCreature.class.getDeclaredField("y");
            xField.setAccessible(true);
            yField.setAccessible(true);
            xField.setDouble(creature, x);
            yField.setDouble(creature, y);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    public static void main(String[] args) {
        check(VoidCreature.bulletHitsForSize(VoidCreature.MIN_SIZE) == 1,
                "smallest creature dies to one standard bullet");
        check(VoidCreature.bulletHitsForSize(VoidCreature.MAX_SIZE) == 10,
                "largest creature dies to ten standard bullets");
        for (int size = VoidCreature.MIN_SIZE; size <= VoidCreature.MAX_SIZE; size++) {
            int hits = VoidCreature.bulletHitsForSize(size);
            check(hits >= 1 && hits <= 10, "health stays within one to ten hits");
            if (size > VoidCreature.MIN_SIZE) {
                check(hits >= VoidCreature.bulletHitsForSize(size - 1),
                        "health increases monotonically with size");
            }
        }

        EnumSet<VoidCreature.SizeTier> seen = EnumSet.noneOf(VoidCreature.SizeTier.class);
        for (int remainingWeight = 1; remainingWeight <= 3; remainingWeight++) {
            for (int i = 0; i < 400; i++) {
                VoidCreature creature = VoidCreature.createSpecter(640, 480, 5, remainingWeight);
                int size = creature.getSize();
                int weight = creature.getWeight();
                check(size >= VoidCreature.MIN_SIZE && size <= VoidCreature.MAX_SIZE,
                        "size fits the full range");
                check(weight >= 1 && weight <= remainingWeight,
                        "spawn respects remaining weight");
                check(creature.getSizeTier().getWeight() == weight,
                        "weight agrees with size tier");
                check(weight == (size <= 68 ? 1 : size <= 139 ? 2 : 3),
                        "tier follows the size thresholds");
                check(creature.getMaxHP() == VoidCreature.bulletHitsForSize(size) * 25,
                        "health follows exact size rather than tier alone");
                check(creature.getCurrentHP() == creature.getMaxHP(),
                        "spawn begins at full health");
                if (remainingWeight == 3) seen.add(creature.getSizeTier());

                int hits = VoidCreature.bulletHitsForSize(size);
                for (int shot = 1; shot < hits; shot++) {
                    creature.takeDamage(25);
                    check(!creature.isDead(), "creature survives before the final hit");
                }
                creature.takeDamage(25);
                check(creature.isDead(), "creature dies on the required hit");
            }
        }
        check(seen.size() == 3, "unrestricted spawning reaches every tier");

        VoidCreature creature = VoidCreature.createSpecter(640, 480, 5, 1);
        setPosition(creature, 100, 100);
        Rectangle bounds = creature.getBounds();
        check(bounds.width == creature.getSize() && bounds.height == creature.getSize(),
                "bounds track size");
        check(Math.abs(bounds.getCenterX() - creature.getX()) <= 1,
                "bounds track X");
        check(Math.abs(bounds.getCenterY() - creature.getY()) <= 1,
                "bounds track Y");

        Rectangle center = new Rectangle(95, 95, 10, 10);
        check(!creature.intersects(center), "creature cannot collide outside void mode");
        creature.update(100, 100, true);
        check(creature.intersects(center), "creature collides in void mode");
        creature.update(100, 100, false);
        check(!creature.intersects(center), "collision turns off outside void mode");
        double driftingX = creature.getX(), driftingY = creature.getY();
        for (int i = 0; i < 12; i++) creature.update(100, 100, false);
        check(Math.hypot(creature.getX() - driftingX, creature.getY() - driftingY) > 1,
                "creature courses through the main world instead of freezing");
        setPosition(creature, 100, 100);

        BufferedImage image = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        creature.draw(graphics, true);
        graphics.dispose();
        check(image.getRGB(100, 100) != 0, "creature renders a visible center");
        BufferedImage ghost = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        graphics = ghost.createGraphics();
        creature.draw(graphics, false);
        graphics.dispose();
        check((ghost.getRGB(100, 100) >>> 24) < (image.getRGB(100, 100) >>> 24),
                "creature is more translucent in the main world");

        VoidCreature animated = VoidCreature.createTier(300, 300, VoidCreature.SizeTier.LARGE);
        setPosition(animated, 150, 150);
        BufferedImage firstFrame = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        graphics = firstFrame.createGraphics();
        animated.draw(graphics, true);
        graphics.dispose();
        for (int i = 0; i < 30; i++) animated.update(150, 150, true);
        setPosition(animated, 150, 150);
        BufferedImage laterFrame = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        graphics = laterFrame.createGraphics();
        animated.draw(graphics, true);
        graphics.dispose();
        int changedPixels = 0;
        for (int y = 50; y < 250; y++) {
            for (int x = 50; x < 250; x++) {
                if (firstFrame.getRGB(x, y) != laterFrame.getRGB(x, y)) changedPixels++;
            }
        }
        check(changedPixels > 100, "void creature silhouette and color animate over time");

        boolean rejected = false;
        try {
            VoidCreature.createSpecter(640, 480, 5, 0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "zero remaining weight cannot spawn");
        System.out.println("Void creature regression checks passed: " + assertions + " assertions.");
    }
}
