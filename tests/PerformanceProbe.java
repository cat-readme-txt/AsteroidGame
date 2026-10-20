import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Native Java2D headless microbenchmark; reports measurements, not pass/fail thresholds.
 * Compile with the game sources and run with src/desktop on the classpath for font assets.
 * Optional arguments: label, warmup frame count, measured frame count.
 */
public final class PerformanceProbe {
    private static final int WIDTH = 1_200;
    private static final int HEIGHT = 700;
    private static final int ASTEROID_COUNT = 24;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path probeHome = Files.createTempDirectory("asteroid-performance-home-").toRealPath();
        System.setProperty("user.home", probeHome.toString());
        // APPDATA/XDG_DATA_HOME can override user.home on other platforms. Refuse live storage.
        if (!AppPaths.appDataDir().toAbsolutePath().normalize().startsWith(probeHome)) {
            throw new IllegalStateException("Run with APPDATA and XDG_DATA_HOME unset to isolate probe saves");
        }
        String label = args.length > 0 ? args[0] : "current";
        int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        int measured = args.length > 2 ? Integer.parseInt(args[2]) : 600;
        if (warmup < 0 || measured < 1) throw new IllegalArgumentException("Invalid frame counts");

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    run(label, warmup, measured);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        } finally {
            try (var paths = Files.walk(probeHome)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void run(String label, int warmup, int measured) throws Exception {
        java.lang.management.ThreadMXBean standardBean = ManagementFactory.getThreadMXBean();
        com.sun.management.ThreadMXBean allocationBean = standardBean instanceof com.sun.management.ThreadMXBean
                ? (com.sun.management.ThreadMXBean) standardBean : null;
        if (allocationBean != null && allocationBean.isThreadAllocatedMemorySupported()) {
            allocationBean.setThreadAllocatedMemoryEnabled(true);
        } else {
            allocationBean = null;
        }
        System.out.printf("label=%s java=%s canvas=%dx%d warmup=%d measured=%d saveHome=%s%n",
                label, System.getProperty("java.version"), WIDTH, HEIGHT, warmup, measured,
                System.getProperty("user.home"));

        AsteroidGame game = new AsteroidGame();
        ((Timer) field(AsteroidGame.class, "timer").get(game)).stop();
        game.setSize(WIDTH, HEIGHT);
        game.updateDimensions();
        Method tick;
        try {
            tick = AsteroidGame.class.getDeclaredMethod("updateGameTick");
            tick.setAccessible(true);
        } catch (NoSuchMethodException baseline) {
            tick = null;
        }
        Method fixedTick = tick;
        ActionEvent event = new ActionEvent(game, ActionEvent.ACTION_PERFORMED, "probe");
        BufferedImage menuCanvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D menuGraphics = menuCanvas.createGraphics();
        try {
            Frame menuFrame = () -> {
                if (fixedTick == null) game.actionPerformed(event);
                else fixedTick.invoke(game);
                game.paint(menuGraphics);
            };
            measure(label, "menu_update_paint", menuFrame, allocationBean, warmup, measured);
            System.out.printf("%s menu entities: asteroids=%d voidCreatures=%d%n", label,
                    collectionSize(game, "asteroids"),
                    collectionSize(game, "voidCreatures"));
        } finally {
            menuGraphics.dispose();
        }

        field(AsteroidGame.class, "gameState").set(game, "playing");
        field(AsteroidGame.class, "level").setInt(game, 0);
        field(AsteroidGame.class, "levelStarting").setBoolean(game, false);
        field(AsteroidGame.class, "levelTimer").setInt(game, Integer.MAX_VALUE);
        @SuppressWarnings("unchecked")
        Collection<Asteroid> asteroids = (Collection<Asteroid>) field(AsteroidGame.class, "asteroids").get(game);
        for (int i = 0; i < ASTEROID_COUNT; i++) {
            asteroids.add(new Asteroid(Asteroid.Size.SMALL,
                    50 + (i % 8) * 140, 50 + (i / 8) * 180, 0, 0));
        }
        BufferedImage gameplayCanvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gameplayGraphics = gameplayCanvas.createGraphics();
        try {
            Frame gameplayFrame = () -> {
                if (fixedTick == null) game.actionPerformed(event);
                else fixedTick.invoke(game);
                game.paint(gameplayGraphics);
            };
            measure(label, "gameplay_24_asteroids", gameplayFrame, allocationBean, warmup, measured);
        } finally {
            gameplayGraphics.dispose();
        }

        BufferedImage componentCanvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D componentGraphics = componentCanvas.createGraphics();
        try {
            GalaxyBackground isolatedGalaxy = new GalaxyBackground();
            measure(label, "component_galaxy", () -> {
                isolatedGalaxy.update();
                isolatedGalaxy.draw(componentGraphics, WIDTH, HEIGHT);
            }, allocationBean, warmup, measured);

            SpaceGrid isolatedGrid = new SpaceGrid();
            java.util.List<Asteroid> gridAsteroids = new java.util.ArrayList<>();
            for (int i = 0; i < ASTEROID_COUNT; i++) {
                gridAsteroids.add(new Asteroid(Asteroid.Size.SMALL,
                        50 + (i % 8) * 140, 50 + (i / 8) * 180, 0, 0));
            }
            measure(label, "component_grid_24_asteroids",
                    () -> isolatedGrid.draw(componentGraphics, WIDTH, HEIGHT,
                            gridAsteroids, java.util.List.of()),
                    allocationBean, warmup, measured);

            SpaceGrid emptyGrid = new SpaceGrid();
            measure(label, "component_grid_empty",
                    () -> emptyGrid.draw(componentGraphics, WIDTH, HEIGHT,
                            java.util.List.of(), java.util.List.of()),
                    allocationBean, warmup, measured);

            StarField isolatedStars = new StarField(WIDTH, HEIGHT, 150);
            measure(label, "component_stars_150", () -> isolatedStars.draw(componentGraphics),
                    allocationBean, warmup, measured);

            measure(label, "component_draw_24_asteroids", () -> {
                for (Asteroid asteroid : gridAsteroids) asteroid.draw(componentGraphics);
            }, allocationBean, warmup, measured);

            if (fixedTick != null) {
                measure(label, "component_game_update_24_asteroids",
                        () -> fixedTick.invoke(game), allocationBean, warmup, measured);
            }
            measure(label, "component_game_paint_24_asteroids",
                    () -> game.paint(componentGraphics), allocationBean, warmup, measured);

            Method hud = AsteroidGame.class.getDeclaredMethod("drawEnhancedHUD", Graphics2D.class);
            hud.setAccessible(true);
            measure(label, "component_enhanced_hud", () -> hud.invoke(game, componentGraphics),
                    allocationBean, warmup, measured);

            AbilityManager probeAbilities = (AbilityManager) field(AsteroidGame.class, "abilityManager").get(game);
            AbilityLoadout probeLoadout = (AbilityLoadout) field(AsteroidGame.class, "abilityLoadout").get(game);
            measure(label, "component_ability_bar",
                    () -> probeAbilities.drawAbilityBar(componentGraphics, WIDTH, HEIGHT, probeLoadout, "V"),
                    allocationBean, warmup, measured);

            ConstructPanel constructPanel = new ConstructPanel();
            ConstructPanel.Model constructModel = new ConstructPanel.Model(0, new int[4], new int[4],
                    new String[] {"void", null, null, null}, -1);
            measure(label, "component_construct_panel",
                    () -> constructPanel.draw(componentGraphics, WIDTH, HEIGHT, constructModel),
                    allocationBean, warmup, measured);
        } finally {
            componentGraphics.dispose();
        }
    }

    private static void measure(String label, String name, Frame frame,
            com.sun.management.ThreadMXBean allocationBean, int warmup, int measured) throws Exception {
        for (int i = 0; i < warmup; i++) frame.run();
        System.gc();
        long retainedBefore = usedHeap();
        long threadId = Thread.currentThread().getId();
        long allocatedBefore = allocationBean == null ? -1 : allocationBean.getThreadAllocatedBytes(threadId);
        long started = System.nanoTime();
        for (int i = 0; i < measured; i++) frame.run();
        long elapsed = System.nanoTime() - started;
        long allocatedAfter = allocationBean == null ? -1 : allocationBean.getThreadAllocatedBytes(threadId);
        System.gc();
        long retainedAfter = usedHeap();
        double bytesPerFrame = allocatedBefore < 0 || allocatedAfter < 0 ? -1
                : (allocatedAfter - allocatedBefore) / (double) measured;
        System.out.printf("%s %s: ns/frame=%.0f bytes/frame=%.0f retained_heap_delta_bytes=%d%n",
                label, name, elapsed / (double) measured, bytesPerFrame, retainedAfter - retainedBefore);
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static int collectionSize(Object target, String name) throws Exception {
        return ((Collection<?>) field(target.getClass(), name).get(target)).size();
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    @FunctionalInterface
    private interface Frame {
        void run() throws Exception;
    }
}
