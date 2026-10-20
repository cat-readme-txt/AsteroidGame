import java.awt.Rectangle;
import java.util.EnumSet;

public final class CosmicEntityRegressionTest {
    private static int assertions;

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void checkEntity(CosmicEntity entity, int allowedWeight) {
        int size = entity.getSize();
        int weight = entity.getWeight();
        check(size >= CosmicEntity.MIN_SIZE && size <= CosmicEntity.MAX_SIZE, "size within full range");
        check(weight >= 1 && weight <= allowedWeight, "tier fits remaining weight");
        check(entity.getSizeTier().getWeight() == weight, "tier and weight agree");
        check(entity.getSizeValue() == weight, "legacy size value uses spawn weight");
        check((size <= 68 ? 1 : size <= 139 ? 2 : 3) == weight, "tier follows size thresholds");
        check(entity.getMaxHP() == CosmicEntity.bulletHitsForSize(size) * 25, "health follows size");

        Rectangle bounds = entity.getBounds();
        check(bounds.width == size && bounds.height == size, "hit box follows visual size");
        check(Math.abs(bounds.getCenterX() - entity.getX()) <= 1, "hit box tracks entity X");
        check(Math.abs(bounds.getCenterY() - entity.getY()) <= 1, "hit box tracks entity Y");
    }

    public static void main(String[] args) {
        check(CosmicEntity.bulletHitsForSize(CosmicEntity.MIN_SIZE) == 1, "smallest takes one bullet");
        check(CosmicEntity.bulletHitsForSize(CosmicEntity.MAX_SIZE) == 10, "largest takes ten bullets");
        for (int size = CosmicEntity.MIN_SIZE; size <= CosmicEntity.MAX_SIZE; size++) {
            int hits = CosmicEntity.bulletHitsForSize(size);
            check(hits >= 1 && hits <= 10, "bullet hits stay bounded");
            if (size > CosmicEntity.MIN_SIZE) {
                check(hits >= CosmicEntity.bulletHitsForSize(size - 1), "bullet hits rise with size");
            }
        }

        EnumSet<CosmicEntity.SizeTier> seen = EnumSet.noneOf(CosmicEntity.SizeTier.class);
        for (int allowedWeight = 1; allowedWeight <= 3; allowedWeight++) {
            for (int i = 0; i < 400; i++) {
                CosmicEntity entity = new CosmicEntity(640, 480, 5, allowedWeight);
                checkEntity(entity, allowedWeight);
                if (allowedWeight == 3) seen.add(entity.getSizeTier());

                int shots = CosmicEntity.bulletHitsForSize(entity.getSize());
                for (int shot = 1; shot < shots; shot++) {
                    entity.takeDamage(25);
                    check(!entity.isDead(), "entity survives until its final bullet");
                }
                entity.takeDamage(25);
                check(entity.isDead(), "entity dies on specified bullet");
            }
        }
        check(seen.size() == 3, "unrestricted spawning reaches every tier");

        CosmicEntity crossing = new CosmicEntity(640, 480, 5, 1);
        double startX = crossing.getX(), startY = crossing.getY();
        for (int i = 0; i < 12; i++) crossing.update(320, 240, true);
        check(Math.hypot(crossing.getX() - startX, crossing.getY() - startY) > 1,
                "cosmic entity courses through Void instead of freezing");

        boolean rejected = false;
        try {
            new CosmicEntity(640, 480, 5, 0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "zero remaining weight cannot spawn");
        System.out.println("Cosmic entity regression checks passed: " + assertions + " assertions.");
    }
}
