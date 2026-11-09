package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/**
 * {@code /mydog store <id | all>} puts dogs away (their wolf disappears) and {@code /mydog summon <id | all>} brings
 * them back next to the owner, with the health and armor they had.
 */
public final class StoreCommand extends SubCommand {
    private final boolean store;

    private StoreCommand(CommandServices services, String name, boolean store) {
        super(services, name, "mydog.store", "<id | all>", true);
        this.store = store;
    }

    public static StoreCommand store(CommandServices services) {
        return new StoreCommand(services, "store", true);
    }

    public static StoreCommand summon(CommandServices services) {
        return new StoreCommand(services, "summon", false);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 1) {
            throw new UsageException();
        }
        Player player = (Player) sender;
        boolean all = args.getFirst().equalsIgnoreCase("all");
        List<Dog> dogs = livingDogs(player, args.getFirst()).stream().filter(dog -> dog.isStored() != store).toList();
        if (dogs.isEmpty()) {
            throw new CommandException(all ? "command." + name() + ".none" : "command." + name() + ".already");
        }

        int done = 0;
        for (Dog dog : dogs) {
            if (store) {
                // Only a loaded wolf can be put away, so its health and armor are known
                Optional<Wolf> wolf = services.dogs().wolfOf(dog);
                if (wolf.isEmpty()) {
                    services.messages().send(sender, "command.store.not-loaded", Messages.dog(dog));
                    continue;
                }
                services.dogs().store(dog, wolf.get());
            } else {
                services.dogs().summon(dog, player);
            }
            done++;
            if (!all) {
                services.messages().send(sender, "command." + name() + ".one", Messages.dog(dog));
            }
        }
        if (all && done > 0) {
            services.messages().send(sender, "command." + name() + ".all", Messages.text("count", done));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        if (args.size() != 1 || !(sender instanceof Player player)) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        for (Dog dog : services.dogs().aliveDogsOf(player.getUniqueId())) {
            if (dog.isStored() != store) {
                options.add(Integer.toString(dog.getIdentifier()));
            }
        }
        options.add("all");
        return options;
    }
}
