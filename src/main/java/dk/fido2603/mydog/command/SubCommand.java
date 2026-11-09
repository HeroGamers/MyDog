package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.objects.Dog;

/**
 * One {@code /mydog <name>} command. The router checks the permission and whether a player is needed before
 * {@link #execute} is called, and shows {@code command.help.<name>} from messages.yml in the help list.
 */
public abstract class SubCommand {
    /** Thrown when the arguments don't fit; the router answers with the command's usage. */
    public static final class UsageException extends Exception {
        public UsageException() {
            super(null, null, false, false);
        }
    }

    protected final CommandServices services;
    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final String usage;
    private final boolean playerOnly;

    /**
     * @param usage the arguments, like {@code "<id> <name>"}, or {@code ""} when it takes none
     */
    protected SubCommand(CommandServices services, String name, String permission, String usage, boolean playerOnly,
                         String... aliases) {
        this.services = services;
        this.name = name;
        this.permission = permission;
        this.usage = usage;
        this.playerOnly = playerOnly;
        this.aliases = List.of(aliases);
    }

    public final String name() {
        return name;
    }

    public final List<String> aliases() {
        return aliases;
    }

    public final String permission() {
        return permission;
    }

    public final boolean playerOnly() {
        return playerOnly;
    }

    public final String usage(String label) {
        return "/" + label + " " + name + (usage.isEmpty() ? "" : " " + usage);
    }

    /** Whether the command is turned on by the config (e.g. revival commands need revival on). */
    public boolean isAvailable() {
        return true;
    }

    public abstract void execute(CommandSender sender, List<String> args) throws CommandException, UsageException;

    /** Suggestions for the last argument; {@code args} holds everything typed after the command name. */
    public List<String> complete(CommandSender sender, List<String> args) {
        return List.of();
    }

    // Helpers for commands about the sender's own dogs

    protected static int parseIdentifier(String arg) throws CommandException {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new CommandException("command.invalid-id");
        }
    }

    /** One of the player's dogs, alive or dead, by ID. */
    protected Dog ownDog(Player player, String arg) throws CommandException {
        return services.dogs().find(player.getUniqueId(), parseIdentifier(arg))
                .orElseThrow(() -> new CommandException("command.dog-not-found"));
    }

    /** One of the player's living dogs, by ID. */
    protected Dog ownLivingDog(Player player, String arg) throws CommandException {
        Dog dog = ownDog(player, arg);
        if (dog.isDead()) {
            throw new CommandException("command.dog-not-found");
        }
        return dog;
    }

    /** All the player's living dogs for {@code all}, otherwise the one with that ID. */
    protected List<Dog> ownLivingDogs(Player player, String arg) throws CommandException {
        if (arg.equalsIgnoreCase("all")) {
            return services.dogs().aliveDogsOf(player.getUniqueId());
        }
        return List.of(ownLivingDog(player, arg));
    }

    protected List<String> livingDogIds(CommandSender sender) {
        return dogIds(sender, player -> services.dogs().aliveDogsOf(player.getUniqueId()));
    }

    protected List<String> allDogIds(CommandSender sender) {
        return dogIds(sender, player -> services.dogs().dogsOf(player.getUniqueId()));
    }

    protected List<String> deadDogIds(CommandSender sender) {
        return dogIds(sender, player -> services.dogs().deadDogsOf(player.getUniqueId()));
    }

    private static List<String> dogIds(CommandSender sender, Function<Player, List<Dog>> dogs) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (Dog dog : dogs.apply(player)) {
            ids.add(Integer.toString(dog.getIdentifier()));
        }
        return ids;
    }
}
