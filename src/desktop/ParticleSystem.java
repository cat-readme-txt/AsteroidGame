import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    public int getParticleCount() { return particles.size(); }
    private static final int MAX_PARTICLES = 650;
    private static final double PARTICLE_COUNT_SCALE = 0.6;

    private static class Particle {
        double x, y;
        double vx, vy;
        int lifetime;
        int maxLifetime;
        Color color;
        int size;
        boolean homeToShip;
        int homingDelay;
        
        Particle(double x, double y, double vx, double vy, Color color, int lifetime, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.maxLifetime = lifetime;
            this.lifetime = lifetime;
            this.size = size;
        }
        
        void update(double shipX, double shipY) {
            if (homeToShip && homingDelay-- <= 0 && Double.isFinite(shipX) && Double.isFinite(shipY)) {
                double dx = shipX - x, dy = shipY - y;
                double distance = Math.hypot(dx, dy);
                if (distance < 12) { lifetime = 0; return; }
                vx = vx * 0.83 + dx / distance * 2.2;
                vy = vy * 0.83 + dy / distance * 2.2;
            }
            x += vx;
            y += vy;
            vx *= 0.98; // friction
            vy *= 0.98;
            lifetime--;
        }
        
        boolean isAlive() {
            return lifetime > 0;
        }
        
        void draw(Graphics2D g2d) {
            float alpha = (float) lifetime / maxLifetime;
            Composite old = g2d.getComposite();
            g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));

            g2d.setColor(color);                 // base RGB (alpha can be 255)
            g2d.fillOval((int) x - size/2, (int) y - size/2, size, size);

            g2d.setComposite(old);
        }
    }
    
    private final List<Particle> particles = new ArrayList<>();
    private final Random rand = new Random();
    
    public void createExplosion(double x, double y, Color color, int count, double speed) {
        if (particles.size() >= MAX_PARTICLES) {
            return;
        }

        count = Math.max(1, (int)Math.round(count * PARTICLE_COUNT_SCALE));
        count = Math.min(count, MAX_PARTICLES - particles.size());
        for (int i = 0; i < count; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double velocity = speed * (0.5 + rand.nextDouble());
            double vx = Math.cos(angle) * velocity;
            double vy = Math.sin(angle) * velocity;
            int lifetime = 24 + rand.nextInt(24);
            int size = 2 + rand.nextInt(3);
            particles.add(new Particle(x, y, vx, vy, color, lifetime, size));
        }
    }
    
    public void createAsteroidExplosion(double x, double y, Asteroid.Size size) {
        int count = switch (size) {
            case LARGE -> 30;
            case MEDIUM -> 20;
            case SMALL -> 10;
        };
        createExplosion(x, y, new Color(115, 135, 135), count, 3.0);
    }
    
    public void createLaserExplosion(double x, double y, Asteroid.Size size) {
        int count = switch (size) {
            case LARGE -> 40;
            case MEDIUM -> 25;
            case SMALL -> 15;
        };
        createExplosion(x, y, new Color(180, 0, 255, 255), count, 4.0);
    }
    
    public void createBulletExplosion(double x, double y) {
    	createExplosion(x, y, Color.YELLOW, 50, 5.0);
        createExplosion(x, y, new Color(255, 255, 255, 255), 40, 3.0);
    }
    
    public void createVoidExplosion(double x, double y) {
    	createExplosion(x, y, new Color(100, 50, 200, 180), 50, 5.0);
        createExplosion(x, y, new Color(55, 0, 155, 255), 40, 3.0);
    }
    
    public void createDroneExplosion(double x, double y) {
        createExplosion(x, y, new Color(125, 220, 255, 235), 8, 2.2);
        createExplosion(x, y, new Color(255, 245, 195, 230), 4, 1.4);
    }
    
    public void createShipExplosion(double x, double y) {
        // Purple layer
        createExplosion(x, y, new Color(180, 0, 255, 255), 24, 6.0);
        // Pink layer
        createExplosion(x, y, new Color(255, 100, 200, 255), 18, 4.5);
        // Yellow layer
        createExplosion(x, y, new Color(255, 255, 0, 255), 14, 3.0);
        // White core
        createExplosion(x, y, new Color(255, 255, 255, 255), 10, 2.0);
    }
    
    public void createBlackHoleExplosion(double x, double y) {
        // Multiple layers of explosion
        createExplosion(x, y, new Color(150, 200, 255, 180), 80, 8.0);
        createExplosion(x, y, new Color(100, 50, 200, 255), 50, 5.0);
        createExplosion(x, y, new Color(255, 255, 255, 255), 30, 2.0);
    }

    public void createBlackHoleLaserImpact(double x, double y) {
        createExplosion(x, y, new Color(175, 75, 255), 16, 3.0);
        createExplosion(x, y, Color.WHITE, 12, 2.0);
    }

    public void createBlackHoleAbsorption(double x, double y) {
        int count = Math.min(48, MAX_PARTICLES - particles.size());
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / Math.max(1, count);
            double speed = 1.5 + rand.nextDouble() * 2;
            Particle particle = new Particle(x, y, Math.cos(angle) * speed,
                    Math.sin(angle) * speed, i % 2 == 0 ? Color.BLACK : Color.WHITE,
                    150, 3 + rand.nextInt(3));
            particle.homeToShip = true;
            particle.homingDelay = 14 + rand.nextInt(14);
            particles.add(particle);
        }
    }
    
    public void createCosmicEntityExplosion(double x, double y) {
        // Multiple layers of explosion
        createExplosion(x, y, new Color(55, 0, 155, 180), 65, 5.0);
        createExplosion(x, y, new Color(100, 50, 200, 255), 40, 3.0);
        createExplosion(x, y, new Color(255, 255, 255, 255), 30, 2.0);
    }

    public void createVoidCreatureExplosion(double x, double y) {
        createExplosion(x, y, new Color(180, 0, 255, 180), 50, 5.0);
        createExplosion(x, y, new Color(100, 50, 200, 255), 40, 3.0);
        createExplosion(x, y, new Color(255, 255, 255, 255), 30, 2.0);
    }
    
    public void update() { update(Double.NaN, Double.NaN); }

    public void update(double shipX, double shipY) {
        int write = 0;
        for (int read = 0; read < particles.size(); read++) {
            Particle p = particles.get(read);
            p.update(shipX, shipY);
            if (p.isAlive()) {
                particles.set(write++, p);
            }
        }
        particles.subList(write, particles.size()).clear();
    }
    
    public void draw(Graphics2D g2d) {
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }
    
    public void clear() {
        particles.clear();
    }
}
