package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/** {@code sit} and {@code stand}, for one dog or all of them. */
public final class SitCommand extends SubCommand {
    private final boolean sit;

    private SitCommand(CommandServices services, String name, boolean sit) {
        super(services, name, "mydog.sit", "<id | all>", true);
        this.sit = sit;
    }

    public static SitCommand sit(CommandServices services) {
        return new SitCommand(services, "sit", true);
    }

    public static SitCommand stand(CommandServices services) {
        return new SitCommand(services, "stand", false);
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
            services.dogs().sit(dog, sit);
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
