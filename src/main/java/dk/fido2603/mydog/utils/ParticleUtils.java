package dk.fido2603.mydog.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import dk.fido2603.mydog.MyDog;

public class ParticleUtils {
    private MyDog plugin = null;

    public ParticleUtils(MyDog p) {
        this.plugin = p;
    }

    public void newLevelUpParticle(Entity entity) {
        double r = 0.8;
        new BukkitRunnable() {
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 1);
            double t = 0;

            public void run() {
                Location loc = entity.getLocation();
                loc.setY(loc.getY() + 0.5);
                t = t + Math.PI / 8;
                double x = r * Math.cos(t);
                //double y = 0.05*t;
                double y = r * Math.sin(t);
                double z = r * Math.sin(t);

                loc.add(x, y, z);
                loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, dustOptions);
                loc.subtract(x, y, z);

                if (t > Math.PI * 8) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        new BukkitRunnable() {
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.BLACK, 1);
            double t = 0;

            public void run() {
                Location loc = entity.getLocation();
                loc.setY(loc.getY() + 0.5);
                t = t + Math.PI / 8;
                double x = r * Math.cos(-t);
                //double y = 0.05*t;
                double y = r * Math.sin(t);
                double z = r * Math.sin(-t);

                loc.add(x, y, z);
                loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, dustOptions);
                loc.subtract(x, y, z);

                if (t > Math.PI * 8) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void newLevelUpParticle2(Location loc) {
        new BukkitRunnable() {
            double t = 0;
            double r = 2;
            double c = 0.05;

            public void run() {
                t = t + Math.PI / 16;
                double x = r * Math.cos(t);
                double y = c * t;
                double z = r * Math.sin(t);

                loc.add(x, y, z);

                loc.getWorld().spawnParticle(Particle.FLAME, loc, 1);

                loc.subtract(x, y, z);

                if (t > Math.PI * 8) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void newPettingParticle(Entity entity) {
        double r = 0.8;

        new BukkitRunnable() {
            double t = 0;

            public void run() {
                Location loc = entity.getLocation();
                loc.setY(loc.getY() + 0.5);
                t = t + Math.PI / 4;
                double x = r * Math.cos(t);
                //double y = 0.05*t;
                double y = Math.random() * r;
                double z = r * Math.sin(t);

                loc.add(x, y, z);
                loc.getWorld().spawnParticle(Particle.HEART, loc, 1);
                loc.subtract(x, y, z);

                if (t > Math.PI * 4) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

	/*public void newLevelUpParticle2(Location loc)
	{
		new BukkitRunnable()
		{
			double t = 0;

			public void run()
			{
				t = t + 0.1*Math.PI;
				for (double theta = 0; theta <= 2*Math.PI; theta = theta + Math.PI/8)
				{
					double x = t*Math.cos(theta);
					double y = Math.exp(-0.1*t)*Math.sin(t)+1.5;
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}*/
}