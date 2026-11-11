package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.Permissions;

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

    // Picking dogs. A dog is "<id>" (one of your own) or "<owner>:<id>" (one shared with you, or any dog for admins
    // with mydog.admin.others).

    /** Who may use a command on a dog. */
    protected enum Access {
        /** Its owner (and admins): giving it away, putting it down, changing its ID or level, ... */
        OWNER,
        /** Also its co-owners: looking after it. */
        SHARED
    }

    protected static int parseIdentifier(String arg) throws CommandException {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new CommandException("command.invalid-id");
        }
    }

    /** A dog, alive or dead, that the player may use this command on. */
    protected Dog dog(Player player, String arg, Access access) throws CommandException {
        int separator = arg.lastIndexOf(':');
        if (separator < 0) {
            return find(player.getUniqueId(), parseIdentifier(arg))
                    .orElseThrow(() -> new CommandException("command.dog-not-found"));
        }

        String ownerName = arg.substring(0, separator);
        UUID ownerId = services.dogs().ownerByName(ownerName)
                .orElseThrow(() -> new CommandException("command.owner-not-found", Messages.text("name", ownerName)));
        Dog dog = find(ownerId, parseIdentifier(arg.substring(separator + 1)))
                .orElseThrow(() -> new CommandException("command.dog-not-found"));

        if (ownerId.equals(player.getUniqueId()) || Permissions.has(player, Permissions.ADMIN_OTHERS)) {
            return dog;
        }
        if (dog.getCoOwners().contains(player.getUniqueId())) {
            if (access == Access.OWNER) {
                throw new CommandException("command.owner-only", Messages.dog(dog));
            }
            return dog;
        }
        // Don't tell players about dogs they have nothing to do with
        throw new CommandException("command.dog-not-found");
    }

    /** The owner's dog with this ID, unless another plugin has given it away since (dogs.follow-owner-changes). */
    private Optional<Dog> find(UUID ownerId, int identifier) {
        Optional<Dog> dog = services.dogs().find(ownerId, identifier);
        dog.ifPresent(services.dogs()::syncOwner);
        return dog.filter(found -> found.getOwnerId().equals(ownerId));
    }

    protected Dog livingDog(Player player, String arg, Access access) throws CommandException {
        Dog dog = dog(player, arg, access);
        if (dog.isDead()) {
            throw new CommandException("command.dog-not-found");
        }
        return dog;
    }

    /** For {@code all}: the player's own living dogs. Otherwise the one dog, which may be shared with them. */
    protected List<Dog> livingDogs(Player player, String arg) throws CommandException {
        if (arg.equalsIgnoreCase("all")) {
            services.dogs().aliveDogsOf(player.getUniqueId()).forEach(services.dogs()::syncOwner);
            return services.dogs().aliveDogsOf(player.getUniqueId());
        }
        return List.of(livingDog(player, arg, Access.SHARED));
    }

    /** IDs of the player's own living dogs, plus "owner:id" for dogs shared with them when {@code shared}. */
    protected List<String> livingDogIds(CommandSender sender, boolean shared) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        List<String> ids = ids(services.dogs().aliveDogsOf(player.getUniqueId()));
        if (shared) {
            for (Dog dog : services.dogs().sharedWith(player.getUniqueId())) {
                ids.add(qualifiedId(dog));
            }
        }
        return ids;
    }

    protected List<String> allDogIds(CommandSender sender) {
        return sender instanceof Player player ? ids(services.dogs().dogsOf(player.getUniqueId())) : List.of();
    }

    protected List<String> deadDogIds(CommandSender sender) {
        return sender instanceof Player player ? ids(services.dogs().deadDogsOf(player.getUniqueId())) : List.of();
    }

    /** "owner:id", how other players refer to a dog. */
    protected String qualifiedId(Dog dog) {
        String owner = services.server().getOfflinePlayer(dog.getOwnerId()).getName();
        return (owner != null ? owner : dog.getOwnerId().toString()) + ":" + dog.getIdentifier();
    }

    private static List<String> ids(List<Dog> dogs) {
        List<String> ids = new ArrayList<>();
        for (Dog dog : dogs) {
            ids.add(Integer.toString(dog.getIdentifier()));
        }
        return ids;
    }
}
