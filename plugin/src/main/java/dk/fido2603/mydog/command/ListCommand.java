package dk.fido2603.mydog.command;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.Permissions;

/**
 * Lists the player's living dogs, with their health when loaded, and the dogs shared with them. Admins with
 * mydog.admin.others can list another player's dogs with {@code /mydog list <player>}.
 */
public final class ListCommand extends SubCommand {
    public ListCommand(CommandServices services) {
        super(services, "list", "mydog.dogs", "[player]", true, "dogs");
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() > 1) {
            throw new UsageException();
        }
        Player player = (Player) sender;
        Messages messages = services.messages();
        if (args.size() == 1) {
            if (!Permissions.has(sender, Permissions.ADMIN_OTHERS)) {
                throw new CommandException("command.no-permission");
            }
            messages.send(sender, "command.header");
            UUID ownerId = services.dogs().ownerByName(args.getFirst())
                    .orElseThrow(() -> new CommandException("command.owner-not-found", Messages.text("name", args.getFirst())));
            List<Dog> dogs = services.dogs().aliveDogsOf(ownerId);
            if (dogs.isEmpty()) {
                messages.send(sender, "command.list.empty");
            }
            for (Dog dog : dogs) {
                send(sender, "command.list.shared-entry", dog);
            }
            return;
        }

        messages.send(sender, "command.header");
        List<Dog> dogs = services.dogs().aliveDogsOf(player.getUniqueId());
        List<Dog> shared = services.dogs().sharedWith(player.getUniqueId());
        if (dogs.isEmpty() && shared.isEmpty()) {
            messages.send(sender, "command.list.empty");
            return;
        }
        for (Dog dog : dogs) {
            send(sender, "command.list.entry", dog);
        }
        if (!shared.isEmpty()) {
            messages.send(sender, "command.list.shared-header");
            for (Dog dog : shared) {
                send(sender, "command.list.shared-entry", dog);
            }
        }
    }

    /** A line for a dog; "shared-entry" lines show it as owner:id. */
    private void send(CommandSender sender, String key, Dog dog) {
        Messages messages = services.messages();
        DecimalFormat format = new DecimalFormat("#.#");
        Component health = services.dogs().wolfOf(dog).map(wolf -> messages.component("command.list.health",
                        Messages.text("current", format.format(wolf.getHealth())),
                        Messages.text("max", format.format(maxHealth(wolf)))))
                .orElse(dog.isStored() ? messages.component("command.list.stored") : Component.empty());
        messages.send(sender, key, Messages.dog(dog), Messages.component("health", health),
                Messages.text("qualified_id", qualifiedId(dog)));
    }

    static double maxHealth(Wolf wolf) {
        AttributeInstance maxHealth = wolf.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth != null ? maxHealth.getValue() : wolf.getHealth();
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        if (args.size() != 1 || !Permissions.has(sender, Permissions.ADMIN_OTHERS)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Player online : services.server().getOnlinePlayers()) {
            names.add(online.getName());
        }
        return names;
    }
}
