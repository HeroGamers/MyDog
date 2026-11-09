package dk.fido2603.mydog.task;

import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;

/** Points dogs in attack mode at the closest monster near their owner. */
public final class AttackModeTask implements Runnable {
    public static final long PERIOD_TICKS = 20L * 2L;
    /** Dogs further than this from their owner are left alone. */
    private static final double MAX_DOG_DISTANCE = 20.0;
    /** How far around the owner to look for monsters. */
    private static final double SEARCH_RADIUS = 13.0;

    private final Server server;
    private final DogService dogService;

    public AttackModeTask(Server server, DogService dogService) {
        this.server = server;
        this.dogService = dogService;
    }

    @Override
    public void run() {
        for (Player player : server.getOnlinePlayers()) {
            Monster closest = null;
            boolean searched = false;

            for (Dog dog : dogService.aliveDogsOf(player.getUniqueId())) {
                if (!dog.isAngry()) {
                    continue;
                }
                Wolf wolf = dogService.wolfOf(dog).orElse(null);
                if (wolf == null || wolf.isSitting() || wolf.getTarget() != null || !wolf.getWorld().equals(player.getWorld())
                        || wolf.getLocation().distanceSquared(player.getLocation()) > MAX_DOG_DISTANCE * MAX_DOG_DISTANCE) {
                    continue;
                }
                if (!searched) {
                    closest = closestMonster(player);
                    searched = true;
                }
                if (closest != null) {
                    wolf.setTarget(closest);
                }
            }
        }
    }

    private static Monster closestMonster(Player player) {
        Monster closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : player.getNearbyEntities(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
            if (entity instanceof Monster monster) {
                double distance = entity.getLocation().distanceSquared(player.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = monster;
                }
            }
        }
        return closest;
    }
}
