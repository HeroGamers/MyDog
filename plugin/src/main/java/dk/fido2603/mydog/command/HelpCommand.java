package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import dk.fido2603.mydog.message.Messages;

/** Lists the commands the sender can use. */
public final class HelpCommand extends SubCommand {
    private final CommandRouter router;

    public HelpCommand(CommandServices services, CommandRouter router) {
        super(services, "help", "mydog.help", "", false);
        this.router = router;
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        Messages messages = services.messages();
        messages.send(sender, "command.header");
        for (SubCommand command : router.commands()) {
            if (CommandRouter.canUse(sender, command)) {
                messages.send(sender, "command.help.entry",
                        Messages.text("usage", command.usage("mydog")),
                        Messages.component("description", messages.component("command.help." + command.name())));
            }
        }
    }
}
