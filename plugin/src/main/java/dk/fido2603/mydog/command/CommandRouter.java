package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.service.Permissions;

/** Handles {@code /mydog} and hands the arguments to the matching {@link SubCommand}. */
public final class CommandRouter implements TabExecutor {
    private final CommandServices services;
    private final List<SubCommand> commands = new ArrayList<>();
    private final Map<String, SubCommand> byName = new HashMap<>();

    public CommandRouter(CommandServices services) {
        this.services = services;
    }

    public void register(SubCommand command) {
        commands.add(command);
        byName.put(command.name(), command);
        for (String alias : command.aliases()) {
            byName.put(alias, command);
        }
    }

    /** The commands, in the order they were registered. */
    public List<SubCommand> commands() {
        return Collections.unmodifiableList(commands);
    }

    /** Whether the sender may use the command right now. */
    public static boolean canUse(CommandSender sender, SubCommand command) {
        return command.isAvailable() && Permissions.has(sender, command.permission())
                && (!command.playerOnly() || sender instanceof Player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = services.messages();
        if (args.length == 0) {
            if (sender instanceof Player player) {
                int owned = services.dogs().dogsOf(player.getUniqueId()).size();
                messages.send(sender, "command.info", Messages.text("owned", owned),
                        Messages.component("dogs", messages.component(owned == 1 ? "command.info-dog" : "command.info-dogs")));
            } else {
                execute(sender, label, byName.get("help"), List.of());
            }
            return true;
        }

        SubCommand sub = byName.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null || !sub.isAvailable()) {
            messages.send(sender, "command.unknown");
            return true;
        }
        if (!Permissions.has(sender, sub.permission())) {
            messages.send(sender, "command.no-permission");
            return true;
        }
        if (sub.playerOnly() && !(sender instanceof Player)) {
            messages.send(sender, "command.player-only");
            return true;
        }
        execute(sender, label, sub, List.of(args).subList(1, args.length));
        return true;
    }

    private void execute(CommandSender sender, String label, SubCommand sub, List<String> args) {
        try {
            sub.execute(sender, args);
        } catch (CommandException e) {
            services.messages().send(sender, e.messageKey(), e.tags());
        } catch (SubCommand.UsageException e) {
            services.messages().send(sender, "command.usage", Messages.text("usage", sub.usage(label)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            for (SubCommand sub : commands) {
                if (canUse(sender, sub)) {
                    options.add(sub.name());
                    options.addAll(sub.aliases());
                }
            }
        } else {
            SubCommand sub = byName.get(args[0].toLowerCase(Locale.ROOT));
            if (sub != null && canUse(sender, sub)) {
                options.addAll(sub.complete(sender, List.of(args).subList(1, args.length)));
            }
        }
        List<String> matches = StringUtil.copyPartialMatches(args[args.length - 1], options, new ArrayList<>());
        Collections.sort(matches);
        return matches;
    }
}
