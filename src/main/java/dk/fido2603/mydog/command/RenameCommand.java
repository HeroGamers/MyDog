package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;

/** Renames a dog. The name may contain spaces. */
public final class RenameCommand extends SubCommand {
    private static final int MAX_NAME_LENGTH = 16;

    public RenameCommand(CommandServices services) {
        super(services, "rename", "mydog.rename", "<id> <name>", true);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() < 2) {
            throw new UsageException();
        }
        Dog dog = ownLivingDog((Player) sender, args.getFirst());
        String name = DogService.cleanName(String.join(" ", args.subList(1, args.size())));
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new CommandException("command.rename.invalid-length");
        }
        services.dogs().rename(dog, name);
        services.messages().send(sender, "command.rename.done", Messages.dog(dog));
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return args.size() == 1 ? livingDogIds(sender) : List.of();
    }
}
