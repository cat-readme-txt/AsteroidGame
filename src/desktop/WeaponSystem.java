import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.List;

/** Run-local weapon constructions shared by the ship and its drones. */
public final class WeaponSystem {
    private static final double SPREAD_STEP = Math.toRadians(15);
    private final int[] levels = new int[4];

    public int level(int option) {
        return option >= 0 && option < levels.length ? levels[option] : 0;
    }

    public int[] levels() { return levels.clone(); }

    public boolean setLevelForDeveloper(int option, int level) {
        if (option < 0 || option >= levels.length || level < 0 || level > 3) return false;
        levels[option] = level;
        return true;
    }

    public boolean construct(int option) {
        if (option < 0 || option >= levels.length || levels[option] >= 3) return false;
        levels[option]++;
        return true;
    }

    public void fire(List<Bullet> bullets, double x, double y, double angle, double scale) {
        addShot(bullets, x, y, angle, scale, 0, true);
        for (int tier = 1; tier <= levels[ConstructPanel.FRONT_NOZZLES]; tier++) {
            addShot(bullets, x, y, angle, scale, -tier * SPREAD_STEP, true);
            addShot(bullets, x, y, angle, scale, tier * SPREAD_STEP, true);
        }
        for (int tier = 1; tier <= levels[ConstructPanel.REAR_NOZZLES]; tier++) {
            addShot(bullets, x, y, angle, scale, -tier * SPREAD_STEP, false);
            addShot(bullets, x, y, angle, scale, tier * SPREAD_STEP, false);
        }
    }

    public void fireVoidLasers(List<VoidLaser> lasers, double x, double y, double angle,
                               double scale, int width, int height) {
        addVoidLaser(lasers, x, y, angle, scale, 0, true, width, height);
        for (int tier = 1; tier <= levels[ConstructPanel.FRONT_NOZZLES]; tier++) {
            addVoidLaser(lasers, x, y, angle, scale, -tier * SPREAD_STEP, true, width, height);
            addVoidLaser(lasers, x, y, angle, scale, tier * SPREAD_STEP, true, width, height);
        }
        for (int tier = 1; tier <= levels[ConstructPanel.REAR_NOZZLES]; tier++) {
            addVoidLaser(lasers, x, y, angle, scale, -tier * SPREAD_STEP, false, width, height);
            addVoidLaser(lasers, x, y, angle, scale, tier * SPREAD_STEP, false, width, height);
        }
    }

    private static void addVoidLaser(List<VoidLaser> lasers, double x, double y, double angle,
                                     double scale, double spread, boolean forward,
                                     int width, int height) {
        double firingAngle = angle + (forward ? spread : Math.PI + spread);
        double distance = (forward ? 20 : 16) * scale;
        lasers.add(new VoidLaser(x + Math.sin(firingAngle) * distance,
                y - Math.cos(firingAngle) * distance, firingAngle, width, height));
    }

    private void addShot(List<Bullet> bullets, double x, double y, double angle,
                         double scale, double spread, boolean forward) {
        double firingAngle = angle + (forward ? spread : Math.PI + spread);
        double distance = (forward ? 20 : 16) * scale;
        bullets.add(new Bullet(x + Math.sin(firingAngle) * distance,
                y - Math.cos(firingAngle) * distance, firingAngle,
                levels[ConstructPanel.PIERCING], levels[ConstructPanel.BULLET_ENLARGEMENT]));
    }

    /** A compact compass dial marks the ship's available firing bearings. */
    public void drawNozzles(Graphics2D g, double x, double y, double angle, double scale) {
        drawNozzles(g, x, y, angle, scale, true);
    }

    public void drawNozzles(Graphics2D g, double x, double y, double angle,
                            double scale, boolean showAddedShots) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(x, y);
            copy.rotate(angle);
            copy.scale(scale, scale);
            copy.setColor(new Color(80, 166, 185));
            Stroke oldStroke = copy.getStroke();
            copy.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double maxThickness = 4.0; // 4px thick in center, tapering to ends
			double step = 0.5;
			Color baseColor = new Color(80, 166, 185);
			for (double a = 0.0; a < 180.0; a += step) {
				double smoothFactor = Math.sin(Math.abs(a - 90.0) / 90.0 * Math.PI / 2.0); // 1.0 at center, 0.0 at ends
				smoothFactor = 1.0 - smoothFactor; 

				float currentThickness = Math.max(0.1f, (float) (smoothFactor * maxThickness));
				int alpha = Math.max(0, Math.min(255, (int) (smoothFactor * 255)));

				copy.setStroke(new BasicStroke(currentThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				copy.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha));
				copy.draw(new java.awt.geom.Arc2D.Double(-42, -42, 84, 84, a, step * 1.5, java.awt.geom.Arc2D.OPEN));
			}
            copy.drawLine(-46, 0, -40, 0);
            copy.drawLine(40, 0, 46, 0);
            copy.setStroke(oldStroke);
            fillPointer(copy, -42, -75, 4, new Color(220, 246, 250));
            int frontLevel = showAddedShots ? levels[ConstructPanel.FRONT_NOZZLES] : 0;
            int rearLevel = showAddedShots ? levels[ConstructPanel.REAR_NOZZLES] : 0;
            for (int tier = 1; tier <= frontLevel; tier++) {
                drawBearing(copy, -tier * SPREAD_STEP, false);
                drawBearing(copy, tier * SPREAD_STEP, false);
            }
            for (int tier = 1; tier <= rearLevel; tier++) {
                if (tier == 1) {
					for (double a = 180.0; a < 360.0; a += step) {
						double smoothFactor = Math.sin(Math.abs(a - 270.0) / 90.0 * Math.PI / 2.0);
						smoothFactor = 1.0 - smoothFactor;

						float currentThickness = Math.max(0.1f, (float) (smoothFactor * maxThickness));
						int alpha = Math.max(0, Math.min(255, (int) (smoothFactor * 255)));

						copy.setStroke(new BasicStroke(currentThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						copy.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha));
						copy.draw(new java.awt.geom.Arc2D.Double(-42, -42, 84, 84, a, step * 1.5, java.awt.geom.Arc2D.OPEN));
					}
				}
                drawBearing(copy, -tier * SPREAD_STEP, true);
                drawBearing(copy, tier * SPREAD_STEP, true);
            }
        } finally {
            copy.dispose();
        }
    }

    private static void drawBearing(Graphics2D g, double spread, boolean rear) {
        AffineTransform transform = g.getTransform();
        if (rear) g.rotate(Math.PI);
        g.rotate(spread);
        fillPointer(g, -44, -56, 2, new Color(139, 225, 242));
        g.setTransform(transform);
    }

    private static void fillPointer(Graphics2D g, double baseY, double tipY,
                                    double halfBaseWidth, Color color) {
        Path2D.Double pointer = new Path2D.Double();
        pointer.moveTo(-halfBaseWidth, baseY);
        pointer.lineTo(0, tipY);
        pointer.lineTo(halfBaseWidth, baseY);
        pointer.closePath();
        Paint previous = g.getPaint();
        g.setPaint(new LinearGradientPaint(0, (float) baseY, 0, (float) tipY,
                new float[]{0f, 0.35f, 1f},
                new Color[]{color,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 185),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}));
        g.fill(pointer);
        g.setPaint(previous);
    }
}
