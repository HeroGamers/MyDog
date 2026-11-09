package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/** Sets a dog free (alive or dead): the wolf goes wild and the dog is forgotten. */
public final class ReleaseCommand extends SubCommand {
    public ReleaseCommand(CommandServices services) {
        super(services, "release", "mydog.free", "<id>", true, "free", "setfree");
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 1) {
            throw new UsageException();
        }
        Dog dog = ownDog((Player) sender, args.getFirst());
        services.messages().send(sender, "command.release.done", Messages.dog(dog));
        services.dogs().release(dog);
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return args.size() == 1 ? allDogIds(sender) : List.of();
    }
}
