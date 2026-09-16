import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public final class GalaxyBackground {
    private static final int NEBULA_COUNT = 5;

    // Visual tuning
    private static final float BASE_SAT = 0.62f;
    private static final float BASE_BRI1 = 0.20f;
    private static final float BASE_BRI2 = 0.30f;

    // Performance tuning
    private static final float CACHE_SCALE = 0.5f;     // render nebula layer at 50% res
    // The nebula hue moves slowly, so rebuilding its radial gradients at 5 Hz
    // preserves the animation without paying that cost on every few paints.
    private static final int REBUILD_EVERY_N_FRAMES = 12;

    private final Random rand = new Random(1337);

    private float t = 0f;          // time accumulator
    private int frameCounter = 0;
    private boolean nebulaDirty = true;

    // Precomputed nebula blob params (stable over time)
    private final float[] nx = new float[NEBULA_COUNT];   // normalized 0..1
    private final float[] ny = new float[NEBULA_COUNT];   // normalized 0..1
    private final float[] nr = new float[NEBULA_COUNT];   // normalized radius factor
    private final float[] hueOffset = new float[NEBULA_COUNT];

    // Cached layer
    private BufferedImage nebulaLayer;
    private BufferedImage displayLayer;
    private int cachedW = -1, cachedH = -1;
    private int displayW = -1, displayH = -1;
    private boolean displayDirty = true;

    public GalaxyBackground() {
        // Stable positions/sizes so motion/color feels continuous
        for (int i = 0; i < NEBULA_COUNT; i++) {
            nx[i] = 0.18f + i * 0.16f + (rand.nextFloat() - 0.5f) * 0.06f; // clustered but not uniform
            ny[i] = 0.45f + (rand.nextFloat() - 0.5f) * 0.25f;
            nr[i] = 0.75f + rand.nextFloat() * 0.55f; // radius scale
            hueOffset[i] = i * 0.08f + (rand.nextFloat() - 0.5f) * 0.03f;
        }
    }

    public void update() {
        // Smooth continuous flow. 0.001f @ 60fps -> slow; keep but make it continuous.
        t += 0.0010f;
        if (t > 10_000f) t = 0f; // avoid float drift
        if (++frameCounter >= REBUILD_EVERY_N_FRAMES) {
            frameCounter = 0;
            nebulaDirty = true;
        }
    }

    public void draw(Graphics2D g2d, int width, int height) {
        if (width <= 0 || height <= 0) return;

        float baseHue = smoothHue(t);
        // Render the gradients at reduced resolution, then compose one opaque
        // full-size frame only when the very slow hue animation advances.
        int lw = Math.max(1, Math.round(width * CACHE_SCALE));
        int lh = Math.max(1, Math.round(height * CACHE_SCALE));

        boolean sizeChanged = (lw != cachedW || lh != cachedH || width != displayW || height != displayH);
        if (sizeChanged || nebulaLayer == null) {
            cachedW = lw;
            cachedH = lh;
            nebulaLayer = new BufferedImage(cachedW, cachedH, BufferedImage.TYPE_INT_ARGB_PRE);
            nebulaDirty = true;
        }
        if (displayLayer == null || width != displayW || height != displayH) {
            displayW = width;
            displayH = height;
            displayLayer = new BufferedImage(displayW, displayH, BufferedImage.TYPE_INT_RGB);
            displayDirty = true;
        }
        // Painting does not advance the animation. In menus/paused states the
        // same frame may be painted repeatedly, so consume each invalidation once.
        if (nebulaDirty) {
            rebuildNebulaLayer(baseHue);
            nebulaDirty = false;
            displayDirty = true;
        }
        if (displayDirty) rebuildDisplayLayer(baseHue);

        // The costly bilinear upscale happens only when the nebula changes.
        g2d.drawImage(displayLayer, 0, 0, null);
    }

    private void rebuildDisplayLayer(float baseHue) {
        Graphics2D displayGraphics = displayLayer.createGraphics();
        try {
            displayGraphics.setComposite(AlphaComposite.Src);
            Color colorTop = Color.getHSBColor(baseHue, BASE_SAT, BASE_BRI1);
            Color colorBottom = Color.getHSBColor(wrap01(baseHue + 0.18f), 0.55f, BASE_BRI2);
            displayGraphics.setPaint(new GradientPaint(0, 0, colorTop, 0, displayH, colorBottom));
            displayGraphics.fillRect(0, 0, displayW, displayH);
            displayGraphics.setComposite(AlphaComposite.SrcOver);
            displayGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            displayGraphics.drawImage(nebulaLayer, 0, 0, displayW, displayH, null);
        } finally {
            displayGraphics.dispose();
        }
        displayDirty = false;
    }

    /** Drop the full-size copy when another composed cache already owns these pixels. */
    public void releaseDisplayLayer() {
        displayLayer = null;
        displayW = -1;
        displayH = -1;
        displayDirty = true;
    }

    private void rebuildNebulaLayer(float baseHue) {
        Graphics2D ng = nebulaLayer.createGraphics();
        try {
            // Render hints: keep quality reasonable but not max
            ng.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            ng.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Clear
            ng.setComposite(AlphaComposite.Src);
            ng.setColor(new Color(0, 0, 0, 0));
            ng.fillRect(0, 0, cachedW, cachedH);

            // Additive-ish blending helps the nebula glow without heavy alpha stacking
            ng.setComposite(AlphaComposite.SrcOver);

            // Make hue drift coherent: nebula hues stay near base hue
            for (int i = 0; i < NEBULA_COUNT; i++) {
                int x = Math.round(nx[i] * cachedW);
                int y = Math.round(ny[i] * cachedH);
                int radius = Math.max(1, Math.round((0.35f * Math.min(cachedW, cachedH)) * nr[i])); // scaled to layer size

                float nebHue = wrap01(baseHue + hueOffset[i]);
                Color core = withAlpha(Color.getHSBColor(nebHue, 0.75f, 0.35f), 90);
                Color mid  = withAlpha(Color.getHSBColor(nebHue, 0.70f, 0.33f), 35);
                Color edge = new Color(0, 0, 0, 0);

                RadialGradientPaint paint = new RadialGradientPaint(
                        new Point2D.Float(x, y),
                        radius,
                        new float[]{0f, 0.55f, 1f},
                        new Color[]{core, mid, edge},
                        MultipleGradientPaint.CycleMethod.NO_CYCLE
                );

                ng.setPaint(paint);
                ng.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            }
        } finally {
            ng.dispose();
        }
    }

    // Smooth, consistent hue flow (avoids harsh wrap feeling)
    private static float smoothHue(float t) {
        // base drift + a tiny sinusoid for organic variation
        float drift = (t * 0.06f); // slower effective hue travel
        float wobble = (float) (0.03f * Math.sin(t * 0.8f));
        return wrap01(drift + wobble);
    }

    private static float wrap01(float v) {
        v = v % 1f;
        if (v < 0f) v += 1f;
        return v;
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
