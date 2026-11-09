package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/**
 * {@code /mydog share <id> <player>} lets a friend look after a dog (call it, command it, pet it, rename it, ...),
 * {@code /mydog unshare <id> <player>} takes that back. The dog keeps following its owner, and only the owner can give
 * it away.
 */
public final class ShareCommand extends SubCommand {
    private final boolean share;

    private ShareCommand(CommandServices services, String name, boolean share) {
        super(services, name, "mydog.share", "<id> <player>", true);
        this.share = share;
    }

    public static ShareCommand share(CommandServices services) {
        return new ShareCommand(services, "share", true);
    }

    public static ShareCommand unshare(CommandServices services) {
        return new ShareCommand(services, "unshare", false);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 2) {
            throw new UsageException();
        }
        Dog dog = livingDog((Player) sender, args.getFirst(), Access.OWNER);
        String name = args.get(1);

        if (share) {
            Player friend = services.server().getPlayerExact(name);
            if (friend == null) {
                throw new CommandException("command.trade.player-not-found");
            }
            if (friend.getUniqueId().equals(dog.getOwnerId())) {
                throw new CommandException("command.share.self");
            }
            if (dog.getCoOwners().contains(friend.getUniqueId())) {
                throw new CommandException("command.share.already", Messages.dog(dog), Messages.text("player", friend.getName()));
            }
            services.dogs().share(dog, friend.getUniqueId());
            services.messages().send(sender, "command.share.done", Messages.dog(dog), Messages.text("player", friend.getName()));
            services.messages().send(friend, "command.share.received", Messages.dog(dog),
                    Messages.text("player", sender.getName()), Messages.text("qualified_id", qualifiedId(dog)));
        } else {
            UUID coOwner = coOwnerByName(dog, name)
                    .orElseThrow(() -> new CommandException("command.unshare.not-shared", Messages.dog(dog), Messages.text("player", name)));
            services.dogs().unshare(dog, coOwner);
            services.messages().send(sender, "command.unshare.done", Messages.dog(dog), Messages.text("player", name));
            Player former = services.server().getPlayer(coOwner);
            if (former != null) {
                services.messages().send(former, "command.unshare.received", Messages.dog(dog), Messages.text("player", sender.getName()));
            }
        }
    }

    private Optional<UUID> coOwnerByName(Dog dog, String name) {
        return dog.getCoOwners().stream()
                .filter(id -> name.equalsIgnoreCase(services.server().getOfflinePlayer(id).getName()))
                .findFirst();
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        if (args.size() == 1) {
            return livingDogIds(sender, false);
        }
        if (args.size() != 2 || !(sender instanceof Player player)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        if (share) {
            for (Player online : services.server().getOnlinePlayers()) {
                if (!online.equals(player)) {
                    names.add(online.getName());
                }
            }
        } else {
            try {
                for (UUID coOwner : dog(player, args.getFirst(), Access.OWNER).getCoOwners()) {
                    String name = services.server().getOfflinePlayer(coOwner).getName();
                    if (name != null) {
                        names.add(name);
                    }
                }
            } catch (CommandException e) {
                // No such dog; nothing to suggest
            }
        }
        return names;
    }
}
