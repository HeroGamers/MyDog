package dk.fido2603.mydog.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.util.DebugLog;

/** Brings dogs (and optionally other pets) to their owners, onto a safe spot. */
public final class TeleportService {
    /** How long (in ticks) an entity is skipped after an experimental teleport, so it isn't moved twice at once. */
    private static final long IN_FLIGHT_TICKS = 20L * 10L;
    /** How far up and down from the target the search for a safe spot goes. */
    private static final int SEARCH_HEIGHT = 64;

    private final Plugin plugin;
    private final DogService dogService;
    private final Supplier<Settings> settings;
    private final Messages messages;
    private final DebugLog debug;
    private final Set<UUID> inFlight = new HashSet<>();

    public TeleportService(Plugin plugin, DogService dogService, Supplier<Settings> settings, Messages messages, DebugLog debug) {
        this.plugin = plugin;
        this.dogService = dogService;
        this.settings = settings;
        this.messages = messages;
        this.debug = debug;
    }

    /**
     * Teleports the pets among {@code entities} that should follow their owner.
     *
     * @param target where to put them, or {@code null} for wherever each owner is
     */
    public void bringToOwners(Collection<? extends Entity> entities, Location target) {
        Settings.Teleport config = settings.get().teleport();
        // A safe spot is searched for once per owner; an empty value means none was found
        Map<UUID, Optional<Location>> safeSpots = new HashMap<>();

        for (Entity entity : entities) {
            // Like vanilla, pets on a lead stay put (teleporting them would also break the lead)
            if (!(entity instanceof Tameable pet) || !(entity instanceof Sittable sittable) || sittable.isSitting()
                    || pet.isLeashed() || !(pet.getOwner() instanceof Player owner) || !owner.isOnline()
                    || inFlight.contains(entity.getUniqueId())) {
                continue;
            }
            Optional<Dog> dog = dogService.dogOf(entity);
            if ((dog.isEmpty() && !config.allTameables()) || (dog.isPresent() && dog.get().isDead())
                    || !Permissions.has(owner, Permissions.TELEPORT)
                    || (!entity.getWorld().equals(owner.getWorld()) && !config.onWorldChange())) {
                continue;
            }

            Optional<Location> spot = safeSpots.computeIfAbsent(owner.getUniqueId(), id -> {
                Optional<Location> found = findSafeLocation(target != null ? target : owner.getLocation(), config.expandedSearch());
                if (found.isEmpty()) {
                    debug.log(() -> "No safe spot near " + owner.getName() + ", their pets stay behind.");
                    messages.send(owner, "teleport.no-safe-location");
                }
                return found;
            });
            if (spot.isEmpty()) {
                continue;
            }

            debug.log(() -> "Teleporting " + entity.getType() + " " + entity.getUniqueId() + " to " + owner.getName());
            if (config.experimental()) {
                UUID entityId = entity.getUniqueId();
                inFlight.add(entityId);
                plugin.getServer().getScheduler().runTask(plugin, () -> teleport(entity, dog, spot.get()));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> inFlight.remove(entityId), IN_FLIGHT_TICKS);
            } else {
                teleport(entity, dog, spot.get());
            }
        }
    }

    private void teleport(Entity entity, Optional<Dog> dog, Location location) {
        if (!entity.isValid() || !entity.teleport(location)) {
            return;
        }
        dog.ifPresent(d -> {
            Wolf wolf = (Wolf) entity;
            dogService.updateLastSeen(d, wolf);
            dogService.updateWolf(d, wolf);
        });
    }

    /**
     * The closest safe spot to a location: first straight up and down, and with {@code expanded} also in the columns
     * up to two blocks east and south.
     */
    public Optional<Location> findSafeLocation(Location origin, boolean expanded) {
        if (isSafe(origin)) {
            return Optional.of(origin.clone());
        }
        int reach = expanded ? 2 : 0;
        for (int dx = 0; dx <= reach; dx++) {
            for (int dz = 0; dz <= reach; dz++) {
                Optional<Location> found = searchColumn(origin.clone().add(dx, 0, dz));
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    /** Searches a column outwards from the location's height, nearest first. */
    private Optional<Location> searchColumn(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return Optional.empty();
        }
        int startY = location.getBlockY();
        for (int offset = 0; offset <= SEARCH_HEIGHT; offset++) {
            for (int y : new int[]{startY + offset, startY - offset}) {
                if (y <= world.getMinHeight() || y >= world.getMaxHeight()) {
                    continue;
                }
                Location candidate = location.clone();
                candidate.setY(y);
                if (isSafe(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    /** Somewhere a pet can stand: room for its feet, and something solid (or water) below. */
    private static boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block ground = feet.getRelative(BlockFace.DOWN);
        Material feetType = feet.getType();
        boolean roomForFeet = (feet.isPassable() && !feet.isLiquid())
                || Tag.WOOL_CARPETS.isTagged(feetType) || feetType == Material.SNOW;
        return roomForFeet && (ground.getType().isSolid() || ground.getType() == Material.WATER);
    }
}
