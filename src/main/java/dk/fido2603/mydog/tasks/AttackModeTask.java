package dk.fido2603.mydog.tasks;

import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.objects.Dog;
import org.bukkit.entity.*;

import java.util.List;

public class AttackModeTask implements Runnable {
    private MyDog plugin;

    public AttackModeTask(MyDog instance) {
        this.plugin = instance;
    }

    @Override
    public void run() {
        plugin.logDebug("Running the angry dog target checker!");
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (Dog dog : MyDog.getDogManager().getAliveDogs((player.getUniqueId()))) {
                if (dog.isAngry()) {
                    Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
                    // If the dog has no target
                    if (wolf != null && !wolf.isSitting() && wolf.getTarget() == null) {
                        double distance;

                        // if they are in two seperate worlds, it's safe to say that the distance is above 20 lol
                        if (!player.getWorld().getUID().equals(wolf.getWorld().getUID())) {
                            distance = 1000;
                        } else {
                            distance = player.getLocation().distance(wolf.getLocation());
                        }

                        // If distance is below or equal to 20, find a new target near the player
                        if (distance <= 20.0) {
                            List<Entity> entities = player.getNearbyEntities(13, 13, 13);
                            // Get the closest target
                            double lastDistance = Double.MAX_VALUE;
                            Entity closest = null;
                            for (Entity entity : entities) {
                                double distanceToTarget = player.getLocation().distance(entity.getLocation());
                                if (entity instanceof Monster && distanceToTarget < lastDistance) {
                                    lastDistance = distanceToTarget;
                                    closest = entity;
                                }
                            }
                            if (closest != null) {
                                wolf.setTarget((LivingEntity) closest);
                            }
                        }
                    }
                }
            }
        }
    }
}
