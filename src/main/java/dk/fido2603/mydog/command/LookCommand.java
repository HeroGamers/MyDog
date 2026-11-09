package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.config.TreatKind;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.Permissions;

/**
 * {@code /mydog breed <id> [breed]} and {@code /mydog personality <id> [personality]}: shows a dog's breed or
 * personality, and lets dogs from before those existed pick one, once, for free. Changing it later takes a treat.
 */
public final class LookCommand extends SubCommand {
    private final TreatKind kind;

    private LookCommand(CommandServices services, TreatKind kind) {
        super(services, kind.id(), "mydog.breed", "<id> [" + kind.id() + "]", true);
        this.kind = kind;
    }

    public static LookCommand breed(CommandServices services) {
        return new LookCommand(services, TreatKind.BREED);
    }

    public static LookCommand personality(CommandServices services) {
        return new LookCommand(services, TreatKind.PERSONALITY);
    }

    @Override
    public boolean isAvailable() {
        return !kind.all().isEmpty();
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.isEmpty() || args.size() > 2) {
            throw new UsageException();
        }
        Dog dog = livingDog((Player) sender, args.getFirst(), Access.SHARED);
        String prefix = "command." + kind.id() + ".";
        boolean freeChoice = services.dogs().hasFreeChoice(dog, kind);

        if (args.size() == 1) {
            services.messages().send(sender, prefix + "info", Messages.dog(dog));
            if (freeChoice) {
                services.messages().send(sender, prefix + "free-choice", Messages.dog(dog), Messages.text("options", options()));
            }
            return;
        }

        // Picking is up to the owner; co-owners can only look
        if (!dog.getOwnerId().equals(((Player) sender).getUniqueId()) && !Permissions.has(sender, Permissions.ADMIN_OTHERS)) {
            throw new CommandException("command.owner-only", Messages.dog(dog));
        }
        if (!freeChoice) {
            throw new CommandException(prefix + "no-free-choice", Messages.dog(dog));
        }
        NamespacedKey key = TreatKind.parseKey(args.get(1));
        if (key == null || !kind.exists(key)) {
            throw new CommandException(prefix + "unknown", Messages.text("options", options()));
        }
        Wolf wolf = services.dogs().wolfOf(dog).orElseThrow(() -> new CommandException("command.dog-not-loaded"));
        services.dogs().changeLook(dog, wolf, kind, key);
        services.dogs().useFreeChoice(dog, kind);
        services.messages().send(sender, prefix + "done", Messages.dog(dog));
    }

    private String options() {
        return String.join(", ", names(kind.all()));
    }

    /** How players type them: "snowy", or the full key for anything outside the minecraft namespace. */
    static List<String> names(List<NamespacedKey> keys) {
        List<String> names = new ArrayList<>();
        for (NamespacedKey key : keys) {
            names.add(key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.toString());
        }
        return names;
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return switch (args.size()) {
            case 1 -> livingDogIds(sender, true);
            case 2 -> names(kind.all());
            default -> List.of();
        };
    }
}
