package dk.fido2603.mydog.task;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.service.TeleportService;

/** Brings dogs that have wandered too far from their owner back to them. */
public final class DistanceTask implements Runnable {
    public static final long PERIOD_TICKS = 20L * 10L;

    private final Server server;
    private final DogService dogService;
    private final TeleportService teleportService;
    private final Supplier<Settings> settings;

    public DistanceTask(Server server, DogService dogService, TeleportService teleportService, Supplier<Settings> settings) {
        this.server = server;
        this.dogService = dogService;
        this.teleportService = teleportService;
        this.settings = settings;
    }

    @Override
    public void run() {
        Settings.Teleport config = settings.get().teleport();
        if (!config.enabled() || !config.distanceCheck()) {
            return;
        }
        double maxDistanceSquared = config.maxDistance() * config.maxDistance();

        for (Player player : server.getOnlinePlayers()) {
            // Wait until the owner is standing on something, so the dogs don't land mid-air
            if (!player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                continue;
            }
            List<Wolf> strays = new ArrayList<>();
            for (var dog : dogService.aliveDogsOf(player.getUniqueId())) {
                dogService.wolfOf(dog).ifPresent(wolf -> {
                    if (!wolf.isSitting() && (!wolf.getWorld().equals(player.getWorld())
                            || wolf.getLocation().distanceSquared(player.getLocation()) >= maxDistanceSquared)) {
                        strays.add(wolf);
                    }
                });
            }
            if (!strays.isEmpty()) {
                teleportService.bringToOwners(strays, null);
            }
        }
    }
}
