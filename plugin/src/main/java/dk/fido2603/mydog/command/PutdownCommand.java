package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.objects.Dog;

/** Kills one of the player's dogs. It dies like any other dog, so it can be revived. */
public final class PutdownCommand extends SubCommand {
    public PutdownCommand(CommandServices services) {
        super(services, "putdown", "mydog.putdown", "<id>", true, "kill");
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 1) {
            throw new UsageException();
        }
        Dog dog = livingDog((Player) sender, args.getFirst(), Access.OWNER);
        Wolf wolf = services.dogs().wolfOf(dog).orElseThrow(() -> new CommandException("command.dog-not-loaded"));

        services.messages().send(sender, "command.putdown.done");
        wolf.setHealth(0);
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return args.size() == 1 ? livingDogIds(sender, false) : List.of();
    }
}
