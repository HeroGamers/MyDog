package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;

/** Writes dog data to dogs.yml now instead of waiting for the autosave. */
public final class SaveCommand extends SubCommand {
    public SaveCommand(CommandServices services) {
        super(services, "save", "mydog.save", "", false);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        services.plugin().saveDogs();
        services.messages().send(sender, "command.save.done");
    }
}
