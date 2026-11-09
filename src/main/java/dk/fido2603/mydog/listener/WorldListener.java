package dk.fido2603.mydog.listener;

import java.util.List;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.plugin.Plugin;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.service.TeleportService;

/** Dogs being loaded and unloaded, and following their owner around. */
public final class WorldListener implements Listener {
    /** Pets follow a teleporting player a moment later, once the player has arrived. */
    private static final long FOLLOW_DELAY_TICKS = 3L;

    private final Plugin plugin;
    private final DogService dogService;
    private final TeleportService teleportService;
    private final Supplier<Settings> settings;

    public WorldListener(Plugin plugin, DogService dogService, TeleportService teleportService, Supplier<Settings> settings) {
        this.plugin = plugin;
        this.dogService = dogService;
        this.teleportService = teleportService;
        this.settings = settings;
    }

    /** Loaded dogs get their nametag and level stats (re)applied, so config changes reach them. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Wolf wolf) {
                dogService.dogOf(wolf).ifPresent(dog -> dogService.updateWolf(dog, wolf));
            }
        }
    }

    /** Pets in a chunk that is unloading are brought to their owner instead of being left behind. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        if (settings.get().teleport().enabled()) {
            teleportService.bringToOwners(event.getEntities(), null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!settings.get().teleport().enabled()) {
            return;
        }
        List<Wolf> wolves = dogService.aliveDogsOf(event.getPlayer().getUniqueId()).stream()
                .flatMap(dog -> dogService.wolfOf(dog).stream())
                .filter(wolf -> !wolf.isSilent())
                .toList();
        if (wolves.isEmpty()) {
            return;
        }
        Location destination = event.getTo();
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> teleportService.bringToOwners(wolves, destination), FOLLOW_DELAY_TICKS);
    }

    /** Dogs don't go through portals on their own; they follow their owner instead. The End is the exception. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(EntityPortalEvent event) {
        if (!settings.get().teleport().enabled() || dogService.dogOf(event.getEntity()).isEmpty()) {
            return;
        }
        Location to = event.getTo();
        if (to != null && to.getWorld() != null && to.getWorld().getEnvironment() != World.Environment.THE_END) {
            event.setCancelled(true);
        }
    }
}
