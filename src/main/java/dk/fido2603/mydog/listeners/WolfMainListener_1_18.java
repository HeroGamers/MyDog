package dk.fido2603.mydog.listeners;

import dk.fido2603.mydog.MyDog;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.List;

public class WolfMainListener_1_18 implements Listener {
    private MyDog plugin = null;

    public WolfMainListener_1_18(MyDog p) {
        this.plugin = p;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityLoad(EntitiesLoadEvent event) {
        WolfMainListener.checkForDogs(event.getEntities().toArray(new Entity[0]));
    }

    // If entities are unloaded, check if any of them are tameables
    @EventHandler(priority = EventPriority.LOWEST)
    public void oneEntityUnload(EntitiesUnloadEvent event) {
        if (!plugin.automaticTeleportation) {
            return;
        }

        List<Entity> entities = event.getEntities();

        if (plugin.experimentalTeleport) {
            MyDog.getTeleportationManager().teleportEntities(entities, null, "EntityUnload");
        } else {
            MyDog.getTeleportationManager().doTeleportEntities(entities, null);
        }
    }
}
