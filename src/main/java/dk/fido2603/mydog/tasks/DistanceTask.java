package dk.fido2603.mydog.tasks;

import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.objects.Dog;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import java.util.ArrayList;
import java.util.List;

public class DistanceTask implements Runnable {
    private MyDog plugin;

    public DistanceTask(MyDog instance) {
        this.plugin = instance;
    }

    @Override
    public void run() {
        plugin.logDebug("Running the distance checker!");
        List<Entity> entities = new ArrayList<>();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (Dog dog : MyDog.getDogManager().getAliveDogs((player.getUniqueId()))) {
                Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
                if (wolf != null && !wolf.isSitting()) {
                    double distance = 0.0;
                    // if they are in two seperate worlds, it's safe to say that the distance is above 30 lol
                    if (!player.getWorld().getUID().equals(wolf.getWorld().getUID())) {
                        distance = 1000;
                    } else {
                        distance = player.getLocation().distance(wolf.getLocation());
                    }

                    // A quick dirty check for ground below player
                    if (distance >= 30.0 && player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                        if (!plugin.experimentalTeleport) {
                            wolf.teleport(player);
                        } else {
                            entities.add(wolf);
                        }
                    }
                }
            }
        }

        if (plugin.experimentalTeleport) {
            MyDog.getTeleportationManager().teleportEntities(entities, null, "DistanceChecker");
        }
    }
}
