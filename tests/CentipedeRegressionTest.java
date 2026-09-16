import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Standalone, deterministic checks for centipede movement and lifecycle. */
public final class CentipedeRegressionTest {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 720;
    private static final double EPSILON = 1e-6;
    private static final Map<Class<?>, Map<String, Field>> FIELDS = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        testBoundedRoamingAndTailGrowth();
        testExitLifecycle();
        testCollisionDoesNotDependOnPainting();
        testResizing();
        System.out.println("Centipede regression checks passed.");
    }

    private static void testBoundedRoamingAndTailGrowth() throws Exception {
        VoidCreature creature = create(173L);
        int maxSegments = integer(creature, "maxSegmentCount");
        Object[] previousSegments = new Object[maxSegments];
        double[] previousX = new double[maxSegments];
        double[] previousY = new double[maxSegments];
        double[] pathX = (double[]) value(creature, "pathX");
        double[] pathY = (double[]) value(creature, "pathY");
        check(pathX.length == pathY.length, "Trail coordinate buffers must match");
        check(pathX.length <= maxSegments * 6 + 8, "Trail memory must be bounded by body length");

        int previousCount = 0;
        boolean turnedLeft = false;
        boolean turnedRight = false;
        boolean crossedSeam = false;
        double speed = number(creature, "speed");
        for (int frame = 0; frame < 30_000; frame++) {
            double oldHeadX = number(creature, "headX");
            double oldHeadY = number(creature, "headY");
            double oldHeading = number(creature, "targetAngle");
            creature.update(WIDTH / 2.0, HEIGHT / 2.0, true);

            double headingChange = Math.IEEEremainder(number(creature, "targetAngle") - oldHeading,
                    Math.PI * 2);
            check(Math.abs(headingChange) <= Math.toRadians(2.5) + EPSILON,
                    "Roaming must obey its turn-rate limit");
            turnedLeft |= headingChange < -EPSILON;
            turnedRight |= headingChange > EPSILON;
            double headDistance = Math.hypot(number(creature, "headX") - oldHeadX,
                    number(creature, "headY") - oldHeadY);
            close(headDistance, speed, "Head speed must remain constant across wrapping");

            List<?> segments = segments(creature);
            check(segments.size() >= previousCount && segments.size() <= maxSegments,
                    "Roaming body must grow only up to its configured maximum");
            for (int i = 0; i < segments.size(); i++) {
                Object segment = segments.get(i);
                double x = number(segment, "x");
                double y = number(segment, "y");
                finite(x, y);
                if (i < previousCount) {
                    check(segment == previousSegments[i], "Growth must append at the tail without reshuffling");
                    crossedSeam |= Math.abs(x - previousX[i]) > WIDTH / 2.0
                            || Math.abs(y - previousY[i]) > HEIGHT / 2.0;
                    double displacement = Math.hypot(Math.IEEEremainder(x - previousX[i], WIDTH),
                            Math.IEEEremainder(y - previousY[i], HEIGHT));
                    check(displacement <= speed + EPSILON,
                            "Existing body segments must follow continuously without snapping: " + displacement);
                }
                if (i > 0) {
                    Object ahead = segments.get(i - 1);
                    double distance = Math.hypot(Math.IEEEremainder(number(ahead, "x") - x, WIDTH),
                            Math.IEEEremainder(number(ahead, "y") - y, HEIGHT));
                    check(distance <= 12.0 + EPSILON,
                            "Following segments must stay within their 12-pixel trail interval");
                    check(distance >= 11.5,
                            "Following segments must remain separated around the permitted smooth turns");
                }
                previousSegments[i] = segment;
                previousX[i] = x;
                previousY[i] = y;
            }
            previousCount = segments.size();
            check(value(creature, "pathX") == pathX && value(creature, "pathY") == pathY,
                    "Long-running movement must reuse its fixed trail buffers");
            int pathCount = integer(creature, "pathCount");
            int pathHead = integer(creature, "pathHead");
            check(pathCount > 0 && pathCount <= pathX.length, "Trail count must stay within capacity");
            check(pathHead >= 0 && pathHead < pathX.length, "Trail cursor must stay within capacity");
        }
        check(previousCount == maxSegments, "Long-running centipede must reach its intended full length");
        check(turnedLeft && turnedRight, "Wandering must support both turn directions");
        check(crossedSeam, "Long-running movement check must exercise screen wrapping");
    }

    private static void testExitLifecycle() throws Exception {
        // Different deterministic spawns and trails exercise all boundary transitions.
        for (int seed = 0; seed < 12; seed++) {
            VoidCreature creature = create(seed);
            for (int i = 0; i < 2_500; i++) creature.update(400, 300, true);
            assertFinishesExit(creature, seed % 2 == 0, 4_000);
        }
        // Leaving void immediately after creation must also terminate from a boundary spawn.
        for (int seed = 20; seed < 28; seed++) {
            assertFinishesExit(create(seed), false, 1_000);
        }
    }

    private static void assertFinishesExit(VoidCreature creature, boolean reenterVoid, int limit)
            throws Exception {
        List<?> initialSegments = segments(creature);
        Object originalHead = initialSegments.get(0);
        int originalCount = initialSegments.size();
        creature.startExitingVoid();
        check(creature.isExitingVoid(), "Exit state must be observable immediately");
        double exitAngle = number(creature, "desiredAngle");
        creature.startExitingVoid();
        close(number(creature, "desiredAngle"), exitAngle, "Repeated exit requests must be idempotent");

        int frame = 0;
        while (!creature.isFullyOffscreen() && frame < limit) {
            double oldHeading = number(creature, "targetAngle");
            double oldHeadX = number(creature, "headX");
            double oldHeadY = number(creature, "headY");
            creature.update(400, 300, reenterVoid);
            close(Math.hypot(number(creature, "headX") - oldHeadX, number(creature, "headY") - oldHeadY),
                    number(creature, "speed") * 2, "Exiting must preserve its intended doubled movement speed");
            check(creature.isExitingVoid(), "Reentering void must not strand an already exiting centipede");
            close(number(creature, "desiredAngle"), exitAngle, "Exit heading must not return to wandering");
            double headingChange = Math.IEEEremainder(number(creature, "targetAngle") - oldHeading,
                    Math.PI * 2);
            check(Math.abs(headingChange) <= Math.toRadians(2.5) * 1.8 + EPSILON,
                    "Exit steering must remain smooth");
            List<?> segments = segments(creature);
            check(segments.size() <= originalCount, "Exiting centipedes must not grow");
            if (!segments.isEmpty()) {
                check(segments.get(0) == originalHead, "Exiting must never promote a body segment into the head");
                for (Object segment : segments) finite(number(segment, "x"), number(segment, "y"));
            }
            frame++;
        }
        check(creature.isFullyOffscreen(), "Every exit must finish within " + limit + " updates");
        creature.update(400, 300, reenterVoid);
        check(creature.isFullyOffscreen(), "Completed exits must stay complete");
    }

    private static void testCollisionDoesNotDependOnPainting() throws Exception {
        VoidCreature creature = create(551L);
        for (int i = 0; i < 100; i++) creature.update(400, 300, true);
        assertHeadCollisions(creature, true);

        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            creature.draw(graphics, false);
            assertHeadCollisions(creature, true);
            creature.update(400, 300, false);
            assertHeadCollisions(creature, false);
            creature.draw(graphics, true);
            assertHeadCollisions(creature, false);
        } finally {
            graphics.dispose();
        }
    }

    private static void assertHeadCollisions(VoidCreature creature, boolean expected) {
        int x = (int) Math.round(creature.getX());
        int y = (int) Math.round(creature.getY());
        Polygon polygon = new Polygon(new int[]{x - 2, x + 2, x + 2, x - 2},
                new int[]{y - 2, y - 2, y + 2, y + 2}, 4);
        Rectangle2D rectangle = new Rectangle2D.Double(x - 2, y - 2, 4, 4);
        Line2D line = new Line2D.Double(x - 20, y, x + 20, y);
        check(creature.intersects(polygon) == expected, "Polygon collision must follow simulation void state");
        check(creature.intersects(rectangle) == expected, "Rectangle collision must follow simulation void state");
        check(creature.intersects(line) == expected, "Line collision must follow simulation void state");
        check(!creature.intersects(new Rectangle2D.Double(-10_000, -10_000, 4, 4)),
                "Distant rectangles must not collide");
        check(!creature.intersects(new Line2D.Double(-10_000, -10_000, -9_990, -10_000)),
                "Distant lines must not collide");
    }

    private static void testResizing() throws Exception {
        VoidCreature creature = create(81L);
        for (int i = 0; i < 2_000; i++) creature.update(400, 300, true);
        int[][] sizes = {{640, 480}, {1_920, 1_080}, {320, 240}, {960, 720}};
        for (int[] size : sizes) {
            creature.updateDimensions(size[0], size[1]);
            check(integer(creature, "screenW") == size[0] && integer(creature, "screenH") == size[1],
                    "Creature boundaries must follow the resized playfield");
            for (int frame = 0; frame < 150; frame++) {
                creature.update(size[0] / 2.0, size[1] / 2.0, true);
                for (Object segment : segments(creature)) finite(number(segment, "x"), number(segment, "y"));
            }
        }
        assertFinishesExit(creature, false, 4_000);
    }

    private static VoidCreature create(long seed) throws Exception {
        ((Random) value(null, VoidCreature.class, "rand")).setSeed(seed);
        return VoidCreature.createCentipede(WIDTH, HEIGHT, 1);
    }

    private static List<?> segments(VoidCreature creature) throws Exception {
        return (List<?>) value(creature, "segments");
    }

    private static Object value(Object target, String name) throws Exception {
        return value(target, target.getClass(), name);
    }

    private static Object value(Object target, Class<?> type, String name) throws Exception {
        return field(type, name).get(target);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Map<String, Field> classFields = FIELDS.computeIfAbsent(type, unused -> new HashMap<>());
        Field field = classFields.get(name);
        if (field == null) {
            field = type.getDeclaredField(name);
            field.setAccessible(true);
            classFields.put(name, field);
        }
        return field;
    }

    private static double number(Object target, String name) throws Exception {
        return field(target.getClass(), name).getDouble(target);
    }

    private static int integer(Object target, String name) throws Exception {
        return field(target.getClass(), name).getInt(target);
    }

    private static void finite(double x, double y) {
        check(Double.isFinite(x) && Double.isFinite(y), "Coordinates must remain finite");
    }

    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) <= EPSILON, message + " (" + actual + " versus " + expected + ")");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
