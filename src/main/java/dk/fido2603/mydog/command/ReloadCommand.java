package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;

/** Reloads config.yml and messages.yml. Dog data stays as it is in memory. */
public final class ReloadCommand extends SubCommand {
    public ReloadCommand(CommandServices services) {
        super(services, "reload", "mydog.reload", "", false);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException {
        if (!services.plugin().reload()) {
            throw new CommandException("command.reload.failed");
        }
        services.messages().send(sender, "command.reload.done");
    }
}
