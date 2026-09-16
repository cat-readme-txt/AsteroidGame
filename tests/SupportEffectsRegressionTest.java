import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SupportEffectsRegressionTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        testIdleGalaxyCache();
        testGridAfterAttractorsDisappear();
        testFloatingTextExpiry();
        testCosmicCollisionWithoutPaint();
        System.out.println("Support effect regression checks passed.");
    }

    private static void testIdleGalaxyCache() throws Exception {
        GalaxyBackground galaxy = new GalaxyBackground();
        BufferedImage screen = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = screen.createGraphics();
        try {
            galaxy.draw(graphics, 320, 240);
            CountingImage cached = new CountingImage(160, 120);
            field(GalaxyBackground.class, "nebulaLayer").set(galaxy, cached);
            for (int paint = 0; paint < 300; paint++) galaxy.draw(graphics, 320, 240);
            check(cached.graphicsCreated == 0, "Idle repaint must reuse the nebula cache");

            for (int tick = 0; tick < 12; tick++) galaxy.update();
            galaxy.draw(graphics, 320, 240);
            galaxy.draw(graphics, 320, 240);
            check(cached.graphicsCreated == 1, "Twelve updates must rebuild only once");

            for (int tick = 0; tick < 13; tick++) galaxy.update();
            galaxy.draw(graphics, 320, 240);
            check(cached.graphicsCreated == 2, "Skipped paints must not lose a pending rebuild");

            galaxy.draw(graphics, 640, 480);
            BufferedImage resized = (BufferedImage) field(GalaxyBackground.class, "nebulaLayer").get(galaxy);
            check(resized.getWidth() == 320 && resized.getHeight() == 240,
                    "Resize must recreate the correctly sized cache");
            galaxy.draw(graphics, 1, 1);
        } finally {
            graphics.dispose();
        }
    }

    private static void testGridAfterAttractorsDisappear() {
        SpaceGrid grid = new SpaceGrid();
        List<Asteroid> asteroids = new ArrayList<>();
        List<BlackHole> holes = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            asteroids.add(new Asteroid(Asteroid.Size.LARGE, 100 + i, 100, 0, 0));
        }
        holes.add(new BlackHole(100, 100, 50));
        drawGrid(grid, 640, 480, asteroids, holes);
        asteroids.clear();
        holes.clear();
        BufferedImage actual = null;
        for (int paint = 0; paint < 6; paint++) {
            actual = drawGrid(grid, 640, 480, asteroids, holes);
        }
        BufferedImage expected = drawGrid(new SpaceGrid(), 640, 480, asteroids, holes);
        check(Arrays.equals(pixels(expected), pixels(actual)),
                "Removed attractors must not remain in reused grid buffers");
        actual = drawGrid(grid, 320, 720, asteroids, holes);
        expected = drawGrid(new SpaceGrid(), 320, 720, asteroids, holes);
        check(Arrays.equals(pixels(expected), pixels(actual)), "Grid resize must remain correct");
    }

    private static BufferedImage drawGrid(SpaceGrid grid, int width, int height,
            List<Asteroid> asteroids, List<BlackHole> holes) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            grid.draw(graphics, width, height, asteroids, holes);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static void testFloatingTextExpiry() throws Exception {
        FloatingTextManager manager = new FloatingTextManager();
        List<FloatingTextManager.FloatingText> survivors = new ArrayList<>();
        for (int i = 0; i < 4_000; i++) {
            FloatingTextManager.FloatingText text = new FloatingTextManager.FloatingText(
                    "text " + i, 100, 100, Color.WHITE, i % 2 == 0 ? 1 : 12,
                    FloatingTextManager.Type.INFO);
            manager.add(text);
            if (i % 2 != 0) survivors.add(text);
        }
        manager.update();
        List<?> active = (List<?>) field(FloatingTextManager.class, "texts").get(manager);
        check(active.equals(survivors), "Expiry must preserve every survivor in draw order");
        BufferedImage screen = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = screen.createGraphics();
        try {
            Font original = graphics.getFont();
            for (int tick = 0; tick < 11; tick++) {
                manager.draw(graphics);
                manager.update();
            }
            check(original.equals(graphics.getFont()), "Floating text must restore the drawing font");
            check(active.isEmpty(), "Expired text must release all references");
        } finally {
            graphics.dispose();
        }
    }

    private static void testCosmicCollisionWithoutPaint() throws Exception {
        CosmicEntity entity = new CosmicEntity(640, 480, 1);
        field(CosmicEntity.class, "x").setDouble(entity, 100);
        field(CosmicEntity.class, "y").setDouble(entity, 100);
        Polygon ship = new Polygon(new int[]{100, 101, 100}, new int[]{100, 100, 101}, 3);
        entity.update(100, 100, true);
        check(!entity.isNearShip(ship), "Entering void must suppress collision before repaint");
        BufferedImage screen = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = screen.createGraphics();
        try {
            entity.draw(graphics, false);
            check(!entity.isNearShip(ship), "Drawing must not change collision state");
        } finally {
            graphics.dispose();
        }
        entity.updateWithBoids(100, 100, List.of(entity), false);
        check(entity.isNearShip(ship), "Leaving void must enable collision before repaint");
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class CountingImage extends BufferedImage {
        int graphicsCreated;

        CountingImage(int width, int height) {
            super(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public Graphics2D createGraphics() {
            graphicsCreated++;
            return super.createGraphics();
        }
    }
}
