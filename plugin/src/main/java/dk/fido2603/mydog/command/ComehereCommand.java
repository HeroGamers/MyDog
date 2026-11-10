package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.Permissions;

/** Calls dogs to the player, loading the chunk they were last seen in if needed. Sitting dogs stand up. */
public final class ComehereCommand extends SubCommand {
    /** When each player last made comehere load chunks (epoch millis). */
    private final Map<UUID, Long> lastChunkLoad = new HashMap<>();

    public ComehereCommand(CommandServices services) {
        super(services, "comehere", "mydog.comehere", "<id | all>", true);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 1) {
            throw new UsageException();
        }
        Player player = (Player) sender;
        Settings.Comehere config = services.settings().get().teleport().comehere();
        // Chunks loaded for this command, so dogs in the same chunk only cost one load
        Map<Long, Chunk> loaded = new HashMap<>();
        boolean cooldownTold = false;

        for (Dog dog : livingDogs(player, args.getFirst())) {
            if (dog.isStored()) {
                services.messages().send(sender, "command.comehere.stored", Messages.dog(dog));
                continue;
            }
            Optional<Wolf> wolf = services.dogs().wolfOf(dog);
            if (wolf.isEmpty()) {
                // Loading chunks is the expensive part, so only that is limited
                if (!config.loadChunks()) {
                    services.messages().send(sender, "command.comehere.not-loaded", Messages.dog(dog));
                    continue;
                }
                long waitSeconds = cooldownLeft(player, config);
                if (waitSeconds > 0) {
                    if (!cooldownTold) {
                        services.messages().send(sender, "command.comehere.cooldown", Messages.text("seconds", waitSeconds));
                        cooldownTold = true;
                    }
                    continue;
                }
                wolf = loadWolf(dog, loaded);
            }
            if (wolf.isEmpty()) {
                services.messages().send(sender, dog.getLastSeen() == null ? "command.dog-not-loaded" : "command.comehere.unreachable");
                continue;
            }
            wolf.get().teleport(player.getLocation());
            wolf.get().setSitting(false);
            services.dogs().updateLastSeen(dog, wolf.get());
            services.dogs().updateWolf(dog, wolf.get());
            services.messages().send(sender, "command.comehere.done", Messages.dog(dog));
        }
        if (!loaded.isEmpty()) {
            lastChunkLoad.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /** Seconds the player still has to wait before comehere loads chunks again, or 0. */
    private long cooldownLeft(Player player, Settings.Comehere config) {
        if (Permissions.has(player, "mydog.comehere.nocooldown")) {
            return 0;
        }
        long since = System.currentTimeMillis() - lastChunkLoad.getOrDefault(player.getUniqueId(), 0L);
        long left = config.cooldownSeconds() * 1000L - since;
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    /** Loads the chunk the dog was last seen in (once per command) and looks for its wolf there. */
    private Optional<Wolf> loadWolf(Dog dog, Map<Long, Chunk> loaded) {
        Location lastSeen = dog.getLastSeen() != null ? dog.getLastSeen().toLocation(services.server()) : null;
        if (lastSeen == null) {
            return Optional.empty();
        }
        Chunk chunk = lastSeen.getChunk();
        // Never generates terrain, and isn't kept loaded: the server unloads the chunk again as usual
        if (!chunk.isLoaded() && !chunk.load(false)) {
            return Optional.empty();
        }
        loaded.putIfAbsent(((long) chunk.getX() << 32) ^ (chunk.getZ() & 0xffffffffL), chunk);
        // Entities load separately from their chunk; getEntities() waits for them
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Wolf wolf && wolf.getUniqueId().equals(dog.getDogId())) {
                return Optional.of(wolf);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        if (args.size() != 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(livingDogIds(sender, true));
        options.add("all");
        return options;
    }
}
