import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class WeaponSystemRegressionTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void checkHitLimit(int level, int expectedHits) {
        Bullet bullet = new Bullet(100, 100, 0, level, 0);
        for (int hit = 1; hit <= expectedHits; hit++) {
            Object target = new Object();
            check(bullet.canHit(target), "new target is hittable");
            check(bullet.registerHit(target) == (hit < expectedHits), "hit budget at level " + level);
            check(!bullet.canHit(target), "a bullet does not hit the same target twice");
        }
    }

    public static void main(String[] args) {
        WeaponSystem weapons = new WeaponSystem();
        List<Bullet> shots = new ArrayList<>();
        weapons.fire(shots, 100, 100, 0, 1);
        check(shots.size() == 1, "one base shot");
        check(shots.get(0).getRadius() == 3, "base bullet is slightly larger");

        for (int i = 0; i < 3; i++) {
            check(weapons.construct(ConstructPanel.FRONT_NOZZLES), "front upgrade");
            check(weapons.construct(ConstructPanel.REAR_NOZZLES), "rear upgrade");
            check(weapons.construct(ConstructPanel.PIERCING), "piercing upgrade");
            check(weapons.construct(ConstructPanel.BULLET_ENLARGEMENT), "enlargement upgrade");
        }
        for (int option = 0; option < 4; option++)
            check(!weapons.construct(option), "every weapon caps at three");
        shots.clear();
        weapons.fire(shots, 100, 100, 0, 0.72);
        check(shots.size() == 13, "six added front and six rear shots");
        check(shots.get(1).getVelocityX() < 0 && shots.get(2).getVelocityX() > 0,
                "front pair fans left and right");
        check(Math.abs(Math.atan2(shots.get(2).getVelocityX(), -shots.get(2).getVelocityY())
                - Math.toRadians(15)) < 0.001, "first front pair spreads fifteen degrees");
        check(Math.abs(Math.atan2(shots.get(6).getVelocityX(), -shots.get(6).getVelocityY())
                - Math.toRadians(45)) < 0.001, "outer front pair spreads forty five degrees");
        check(shots.get(3).getVelocityX() < shots.get(1).getVelocityX(),
                "next front pair fans farther");
        check(shots.get(7).getVelocityY() > 0, "rear pair fires backward");
		List<VoidLaser> lasers = new ArrayList<>();
		weapons.fireVoidLasers(lasers, 100, 100, 0, 1.0, 1200, 700);
		check(lasers.size() == 13, "Void laser uses all front and rear nozzles");
		check(lasers.get(1).getLaserLine().getX2() < lasers.get(1).getLaserLine().getX1()
				&& lasers.get(2).getLaserLine().getX2() > lasers.get(2).getLaserLine().getX1(),
				"Void laser front pair spreads left and right");
		check(lasers.get(7).getLaserLine().getY2() > lasers.get(7).getLaserLine().getY1(),
				"Void laser rear pair fires backward");
        for (Bullet bullet : shots) {
            check(bullet.getPiercingLevel() == 3, "all nozzles share piercing level");
            check(bullet.getEnlargementLevel() == 3 && bullet.getRadius() == 9,
                    "all nozzles share enlargement level");
        }

        checkHitLimit(0, 1);
        checkHitLimit(1, 3);
        checkHitLimit(2, 5);
        Bullet unlimited = new Bullet(0, 0, 0, 3, 3);
        for (int i = 0; i < 12; i++) check(unlimited.registerHit(new Object()), "max piercing lasts");
        for (int i = 0; i < 180; i++) unlimited.update(1000, 700);
        check(!unlimited.isAlive(), "max piercing still expires by lifespan");

        BufferedImage image = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        weapons.drawNozzles(g, 120, 120, 0, 1);
        g.dispose();
        check((image.getRGB(120, 65) >>> 24) > 0, "forward aiming pointer renders");
        int centerBaseAlpha = image.getRGB(120, 75) >>> 24;
        int centerMiddleAlpha = image.getRGB(120, 60) >>> 24;
        int centerTipAlpha = image.getRGB(120, 47) >>> 24;
        check(centerBaseAlpha > centerMiddleAlpha && centerMiddleAlpha > centerTipAlpha
                        && centerTipAlpha > 0,
                "skinny center triangle fades continuously toward its tip");
        check((image.getRGB(120, 45) >>> 24) == 0,
                "center pointer ends fully transparent");
        check((image.getRGB(108, 74) >>> 24) > 0,
                "short additional triangular pointer renders");
        check((image.getRGB(108, 60) >>> 24) == 0,
                "added forward pointer ends well before the center tip");
        check((image.getRGB(120, 120) >>> 24) == 0, "guide leaves ship body clear");
        BufferedImage droneImage = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        g = droneImage.createGraphics();
        weapons.drawNozzles(g, 120, 120, 0, 0.58);
        g.dispose();
        check((droneImage.getRGB(120, 55) >>> 24) == 0,
                "drone pointer is shorter than ship pointer");
        check((droneImage.getRGB(120, 80) >>> 24) > 0,
                "drone still has a visible front pointer");
        System.out.println("Weapon system regression checks passed: " + checks);
    }
}
