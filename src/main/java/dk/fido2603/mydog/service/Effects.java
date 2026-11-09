package dk.fido2603.mydog.service;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Particle animations around dogs. Each one stops by itself, or as soon as the entity is gone. */
public final class Effects {
    private static final double RADIUS = 0.8;

    private final Plugin plugin;

    public Effects(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Two spirals, aqua and black, turning opposite ways around the dog. */
    public void levelUp(Entity entity) {
        spiral(entity, new Particle.DustOptions(Color.AQUA, 1), 1);
        spiral(entity, new Particle.DustOptions(Color.BLACK, 1), -1);
    }

    private void spiral(Entity entity, Particle.DustOptions dust, int direction) {
        animate(entity, Math.PI / 8, Math.PI * 8, t -> {
            double x = RADIUS * Math.cos(direction * t);
            double y = RADIUS * Math.sin(t);
            double z = RADIUS * Math.sin(direction * t);
            Location location = entity.getLocation().add(x, 0.5 + y, z);
            location.getWorld().spawnParticle(Particle.DUST, location, 1, dust);
        });
    }

    /** Hearts circling the dog. */
    public void petting(Entity entity) {
        animate(entity, Math.PI / 4, Math.PI * 4, t -> {
            double x = RADIUS * Math.cos(t);
            double y = Math.random() * RADIUS;
            double z = RADIUS * Math.sin(t);
            Location location = entity.getLocation().add(x, 0.5 + y, z);
            location.getWorld().spawnParticle(Particle.HEART, location, 1);
        });
    }

    private interface Frame {
        void draw(double t);
    }

    /** Draws one frame per tick, with t going up by {@code step} until it passes {@code end}. */
    private void animate(Entity entity, double step, double end, Frame frame) {
        new BukkitRunnable() {
            private double t;

            @Override
            public void run() {
                t += step;
                if (t > end || !entity.isValid()) {
                    cancel();
                    return;
                }
                frame.draw(t);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
