package dk.fido2603.mydog.service;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Entity;

/**
 * Recognizes mobs that belong to other plugins: Citizens NPCs and MythicMobs mobs (which includes McPets pets and
 * MythicMobs mounts). MythicMobs is reached through reflection, so MyDog doesn't need it to build or run.
 */
public final class PluginMobs {
    private final Server server;
    private final Logger log;
    /** MythicBukkit.inst().getMobManager(), and its isMythicMob(Entity). Looked up once, on first use. */
    private Object mythicMobManager;
    private Method isMythicMob;
    private boolean mythicLookedUp;

    public PluginMobs(Server server, Logger log) {
        this.server = server;
        this.log = log;
    }

    /** Citizens (and most other NPC plugins) mark their NPCs with "NPC" metadata. */
    public boolean isNpc(Entity entity) {
        return entity.hasMetadata("NPC");
    }

    public boolean isMythicMob(Entity entity) {
        if (!mythicLookedUp) {
            mythicLookedUp = true;
            lookUpMythicMobs();
        }
        if (isMythicMob == null) {
            return false;
        }
        try {
            return (boolean) isMythicMob.invoke(mythicMobManager, entity);
        } catch (ReflectiveOperationException | ClassCastException e) {
            log.warning("Could not ask MythicMobs about an entity, ignoring MythicMobs from now on: " + e);
            isMythicMob = null;
            return false;
        }
    }

    /** NPCs and MythicMobs: never registered as dogs. */
    public boolean belongsToOtherPlugin(Entity entity) {
        return isNpc(entity) || isMythicMob(entity);
    }

    private void lookUpMythicMobs() {
        if (server.getPluginManager().getPlugin("MythicMobs") == null) {
            return;
        }
        try {
            Class<?> mythicBukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object instance = mythicBukkit.getMethod("inst").invoke(null);
            mythicMobManager = instance.getClass().getMethod("getMobManager").invoke(instance);
            isMythicMob = mythicMobManager.getClass().getMethod("isMythicMob", Entity.class);
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.warning("MythicMobs is installed, but MyDog couldn't hook into it, so it can't tell MythicMobs mobs apart: " + e);
        }
    }
}
