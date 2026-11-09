package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/** Calls dogs to the player, loading the chunk they were last seen in if needed. Sitting dogs stand up. */
public final class ComehereCommand extends SubCommand {
    public ComehereCommand(CommandServices services) {
        super(services, "comehere", "mydog.comehere", "<id | all>", true);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 1) {
            throw new UsageException();
        }
        Player player = (Player) sender;
        for (Dog dog : ownLivingDogs(player, args.getFirst())) {
            if (dog.isStored()) {
                services.messages().send(sender, "command.comehere.stored", Messages.dog(dog));
                continue;
            }
            Optional<Wolf> wolf = services.dogs().wolfOf(dog).or(() -> loadWolf(dog));
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
    }

    /** Loads the chunk the dog was last seen in and looks for its wolf there. */
    private Optional<Wolf> loadWolf(Dog dog) {
        Location lastSeen = dog.getLastSeen() != null ? dog.getLastSeen().toLocation(services.server()) : null;
        if (lastSeen == null) {
            return Optional.empty();
        }
        Chunk chunk = lastSeen.getChunk();
        if (!chunk.isLoaded() && !chunk.load(false)) {
            return Optional.empty();
        }
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
        List<String> options = new ArrayList<>(livingDogIds(sender));
        options.add("all");
        return options;
    }
}
