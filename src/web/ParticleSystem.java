import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    private static final int MAX_PARTICLES = 650;
    private static final double WEB_PARTICLE_SCALE = 0.6;

    private static class Particle {
        double x, y;
        double vx, vy;
        int lifetime;
        int maxLifetime;
        Color color;
        int size;
        
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
        
        void update() {
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

        count = Math.max(1, (int)Math.round(count * WEB_PARTICLE_SCALE));
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
    	// Multiple layers of explosion
        createExplosion(x, y, new Color(100, 200, 255, 255), 50, 5.0);
        createExplosion(x, y, new Color(255, 200, 0, 255), 40, 3.0);
        createExplosion(x, y, new Color(255, 255, 255, 255), 30, 2.0);
    }
    
    public void createShipExplosion(double x, double y) {
        // Purple layer
        createExplosion(x, y, new Color(180, 0, 255, 255), 60, 6.0);
        // Pink layer
        createExplosion(x, y, new Color(255, 100, 200, 255), 50, 4.5);
        // Yellow layer
        createExplosion(x, y, new Color(255, 255, 0, 255), 40, 3.0);
        // White core
        createExplosion(x, y, new Color(255, 255, 255, 255), 30, 2.0);
    }
    
    public void createBlackHoleExplosion(double x, double y) {
        // Multiple layers of explosion
        createExplosion(x, y, new Color(150, 200, 255, 180), 80, 8.0);
        createExplosion(x, y, new Color(100, 50, 200, 255), 50, 5.0);
        createExplosion(x, y, new Color(255, 255, 255, 255), 30, 2.0);
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
    
    public void update() {
        int write = 0;
        for (int read = 0; read < particles.size(); read++) {
            Particle p = particles.get(read);
            p.update();
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
