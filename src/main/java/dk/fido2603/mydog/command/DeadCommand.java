package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/** Lists the player's dead dogs and what reviving them costs. */
public final class DeadCommand extends SubCommand {
    public DeadCommand(CommandServices services) {
        super(services, "dead", "mydog.dead", "", true);
    }

    @Override
    public boolean isAvailable() {
        return services.settings().get().dogs().revival().enabled();
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        Messages messages = services.messages();
        List<Dog> dogs = services.dogs().deadDogsOf(((Player) sender).getUniqueId());
        messages.send(sender, "command.header");
        if (dogs.isEmpty()) {
            messages.send(sender, "command.dead.empty");
            return;
        }

        String entry = services.settings().get().dogs().revival().usePlayerLevels()
                ? "command.dead.entry-levels"
                : "command.dead.entry";
        for (Dog dog : dogs) {
            messages.send(sender, entry, Messages.dog(dog), Messages.text("price", services.dogs().revivalPrice(dog)));
        }
    }
}
