import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

public class SpaceGrid {
    private static final int GRID_SIZE = 72;
    private static final int POINT_SPACING = 12;
    private static final double ASTEROID_RADIUS = 150.0;
    private static final double BLACK_HOLE_RADIUS = 300.0;
    private static final double ASTEROID_RADIUS_SQ = ASTEROID_RADIUS * ASTEROID_RADIUS;
    private static final double BLACK_HOLE_RADIUS_SQ = BLACK_HOLE_RADIUS * BLACK_HOLE_RADIUS;
    private static final float CACHE_SCALE = 0.5f;
    private static final int REBUILD_EVERY_N_FRAMES = 3;
    private static final BasicStroke GRID_STROKE = new BasicStroke(1f);
    private static final Color GRID_COLOR = new Color(100, 100, 150, 30);

    private BufferedImage cachedLayer;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private int sourceWidth = -1;
    private int sourceHeight = -1;
    private int frameCounter = 0;

    public void draw(Graphics2D g2d, int width, int height, List<Asteroid> asteroids, List<BlackHole> blackHoles) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int layerWidth = Math.max(1, Math.round(width * CACHE_SCALE));
        int layerHeight = Math.max(1, Math.round(height * CACHE_SCALE));
        boolean sizeChanged = layerWidth != cachedWidth || layerHeight != cachedHeight;
        sourceWidth = width;
        sourceHeight = height;

        if (sizeChanged || cachedLayer == null) {
            cachedWidth = layerWidth;
            cachedHeight = layerHeight;
            cachedLayer = new BufferedImage(layerWidth, layerHeight, BufferedImage.TYPE_INT_ARGB);
            rebuildLayer(asteroids, blackHoles);
        } else if (frameCounter % REBUILD_EVERY_N_FRAMES == 0) {
            rebuildLayer(asteroids, blackHoles);
        }
        frameCounter++;

        Object oldInterpolation = g2d.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(cachedLayer, 0, 0, width, height, null);
        if (oldInterpolation != null) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
    }

    private void rebuildLayer(List<Asteroid> asteroids, List<BlackHole> blackHoles) {
        double[] asteroidX = new double[asteroids.size()];
        double[] asteroidY = new double[asteroids.size()];
        for (int i = 0; i < asteroids.size(); i++) {
            Rectangle bounds = asteroids.get(i).getBounds().getBounds();
            asteroidX[i] = bounds.getCenterX() * cachedWidth / Math.max(1.0, sourceWidth);
            asteroidY[i] = bounds.getCenterY() * cachedHeight / Math.max(1.0, sourceHeight);
        }

        double[] blackHoleX = new double[blackHoles.size()];
        double[] blackHoleY = new double[blackHoles.size()];
        for (int i = 0; i < blackHoles.size(); i++) {
            BlackHole bh = blackHoles.get(i);
            blackHoleX[i] = bh.getX() * cachedWidth / Math.max(1.0, sourceWidth);
            blackHoleY[i] = bh.getY() * cachedHeight / Math.max(1.0, sourceHeight);
        }

        Graphics2D layerGraphics = cachedLayer.createGraphics();
        try {
            layerGraphics.setBackground(new Color(0, 0, 0, 0));
            layerGraphics.clearRect(0, 0, cachedWidth, cachedHeight);
            layerGraphics.setColor(GRID_COLOR);
            layerGraphics.setStroke(GRID_STROKE);
            layerGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            layerGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            for (int x = 0; x < cachedWidth; x += Math.max(1, Math.round(GRID_SIZE * CACHE_SCALE))) {
                drawCurvedVerticalLine(layerGraphics, x, asteroidX, asteroidY, blackHoleX, blackHoleY);
            }
            for (int y = 0; y < cachedHeight; y += Math.max(1, Math.round(GRID_SIZE * CACHE_SCALE))) {
                drawCurvedHorizontalLine(layerGraphics, y, asteroidX, asteroidY, blackHoleX, blackHoleY);
            }
        } finally {
            layerGraphics.dispose();
        }
    }

    private void drawCurvedVerticalLine(
        Graphics2D g2d,
        int baseX,
        double[] asteroidX,
        double[] asteroidY,
        double[] blackHoleX,
        double[] blackHoleY
    ) {
        int segments = Math.max(2, cachedHeight / Math.max(1, Math.round(POINT_SPACING * CACHE_SCALE)));
        int[] xPoints = new int[segments];
        int[] yPoints = new int[segments];

        for (int i = 0; i < segments; i++) {
            int y = i * Math.max(1, Math.round(POINT_SPACING * CACHE_SCALE));
            double displacement = calculateHorizontalDisplacement(baseX, y, asteroidX, asteroidY, blackHoleX, blackHoleY);
            xPoints[i] = baseX + (int) displacement;
            yPoints[i] = y;
        }

        g2d.drawPolyline(xPoints, yPoints, segments);
    }

    private void drawCurvedHorizontalLine(
        Graphics2D g2d,
        int baseY,
        double[] asteroidX,
        double[] asteroidY,
        double[] blackHoleX,
        double[] blackHoleY
    ) {
        int segments = Math.max(2, cachedWidth / Math.max(1, Math.round(POINT_SPACING * CACHE_SCALE)));
        int[] xPoints = new int[segments];
        int[] yPoints = new int[segments];

        for (int i = 0; i < segments; i++) {
            int x = i * Math.max(1, Math.round(POINT_SPACING * CACHE_SCALE));
            double displacement = calculateVerticalDisplacement(x, baseY, asteroidX, asteroidY, blackHoleX, blackHoleY);
            xPoints[i] = x;
            yPoints[i] = baseY + (int) displacement;
        }

        g2d.drawPolyline(xPoints, yPoints, segments);
    }

    private double calculateHorizontalDisplacement(
        int baseX,
        int y,
        double[] asteroidX,
        double[] asteroidY,
        double[] blackHoleX,
        double[] blackHoleY
    ) {
        double displacement = 0.0;
        for (int i = 0; i < asteroidX.length; i++) {
            double dx = asteroidX[i] - baseX;
            double dy = asteroidY[i] - y;
            double distSq = dx * dx + dy * dy;
            if (distSq < ASTEROID_RADIUS_SQ * CACHE_SCALE * CACHE_SCALE) {
                double dist = Math.sqrt(distSq);
                double strength = (ASTEROID_RADIUS * CACHE_SCALE - dist) / (ASTEROID_RADIUS * CACHE_SCALE);
                displacement += dx * strength * 0.3;
            }
        }
        for (int i = 0; i < blackHoleX.length; i++) {
            double dx = blackHoleX[i] - baseX;
            double dy = blackHoleY[i] - y;
            double distSq = dx * dx + dy * dy;
            if (distSq < BLACK_HOLE_RADIUS_SQ * CACHE_SCALE * CACHE_SCALE) {
                double dist = Math.sqrt(distSq);
                double strength = (BLACK_HOLE_RADIUS * CACHE_SCALE - dist) / (BLACK_HOLE_RADIUS * CACHE_SCALE);
                displacement += dx * strength * 2.0;
            }
        }
        return displacement;
    }

    private double calculateVerticalDisplacement(
        int x,
        int baseY,
        double[] asteroidX,
        double[] asteroidY,
        double[] blackHoleX,
        double[] blackHoleY
    ) {
        double displacement = 0.0;
        for (int i = 0; i < asteroidX.length; i++) {
            double dx = asteroidX[i] - x;
            double dy = asteroidY[i] - baseY;
            double distSq = dx * dx + dy * dy;
            if (distSq < ASTEROID_RADIUS_SQ * CACHE_SCALE * CACHE_SCALE) {
                double dist = Math.sqrt(distSq);
                double strength = (ASTEROID_RADIUS * CACHE_SCALE - dist) / (ASTEROID_RADIUS * CACHE_SCALE);
                displacement += dy * strength * 0.3;
            }
        }
        for (int i = 0; i < blackHoleX.length; i++) {
            double dx = blackHoleX[i] - x;
            double dy = blackHoleY[i] - baseY;
            double distSq = dx * dx + dy * dy;
            if (distSq < BLACK_HOLE_RADIUS_SQ * CACHE_SCALE * CACHE_SCALE) {
                double dist = Math.sqrt(distSq);
                double strength = (BLACK_HOLE_RADIUS * CACHE_SCALE - dist) / (BLACK_HOLE_RADIUS * CACHE_SCALE);
                displacement += dy * strength * 2.0;
            }
        }
        return displacement;
    }
}
