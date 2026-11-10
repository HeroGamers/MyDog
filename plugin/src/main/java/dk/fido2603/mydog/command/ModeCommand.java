package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/** {@code attack} and {@code defend}: whether dogs go after monsters near their owner. */
public final class ModeCommand extends SubCommand {
    private final boolean attack;

    private ModeCommand(CommandServices services, String name, boolean attack) {
        super(services, name, "mydog.togglemode", "<id | all>", true);
        this.attack = attack;
    }

    public static ModeCommand attack(CommandServices services) {
        return new ModeCommand(services, "attack", true);
    }

    public static ModeCommand defend(CommandServices services) {
        return new ModeCommand(services, "defend", false);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 1) {
            throw new UsageException();
        }
        List<Dog> dogs = livingDogs((Player) sender, args.getFirst());
        if (args.getFirst().equalsIgnoreCase("all")) {
            services.messages().send(sender, "command." + name() + ".all");
        } else {
            services.messages().send(sender, "command." + name() + ".one", Messages.dog(dogs.getFirst()));
        }
        for (Dog dog : dogs) {
            services.dogs().setAttackMode(dog, attack);
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
