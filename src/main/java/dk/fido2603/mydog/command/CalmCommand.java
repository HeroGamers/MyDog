package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/**
 * Calms dogs down: they drop whatever they are fighting and leave everything alone for a few seconds. For when a
 * friend hits you by accident, or the pack runs into a horde it can't win against.
 */
public final class CalmCommand extends SubCommand {
    public CalmCommand(CommandServices services) {
        super(services, "calm", "mydog.calm", "[id | all]", true);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() > 1) {
            throw new UsageException();
        }
        String which = args.isEmpty() ? "all" : args.getFirst();
        List<Dog> dogs = livingDogs((Player) sender, which);
        for (Dog dog : dogs) {
            services.dogs().calm(dog);
        }
        var seconds = Messages.text("seconds", services.settings().get().dogs().calmSeconds());
        if (which.equalsIgnoreCase("all")) {
            services.messages().send(sender, "command.calm.all", seconds);
        } else {
            services.messages().send(sender, "command.calm.one", Messages.dog(dogs.getFirst()), seconds);
        }
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        if (args.size() != 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(livingDogIds(sender, true));
        options.add("all");
        return options;
    }
}
