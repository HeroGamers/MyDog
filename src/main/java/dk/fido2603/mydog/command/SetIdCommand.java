package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.objects.Dog;

/** Gives a dog a different ID. A dog that already had that ID gets the lowest free one. */
public final class SetIdCommand extends SubCommand {
    public SetIdCommand(CommandServices services) {
        super(services, "setid", "mydog.setid", "<id> <new id>", true, "changeid");
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 2) {
            throw new UsageException();
        }
        Dog dog = ownDog((Player) sender, args.getFirst());
        int newId;
        try {
            newId = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            newId = 0;
        }
        if (newId < 1) {
            throw new CommandException("command.setid.invalid");
        }
        services.dogs().changeIdentifier(dog, newId);
        services.messages().send(sender, "command.setid.done");
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return args.size() == 1 ? allDogIds(sender) : List.of();
    }
}
