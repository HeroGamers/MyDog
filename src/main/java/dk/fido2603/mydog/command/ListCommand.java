package dk.fido2603.mydog.command;

import java.text.DecimalFormat;
import java.util.List;

import net.kyori.adventure.text.Component;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/** Lists the player's living dogs, with their health when loaded. */
public final class ListCommand extends SubCommand {
    public ListCommand(CommandServices services) {
        super(services, "list", "mydog.dogs", "", true, "dogs");
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        Messages messages = services.messages();
        List<Dog> dogs = services.dogs().aliveDogsOf(((Player) sender).getUniqueId());
        messages.send(sender, "command.header");
        if (dogs.isEmpty()) {
            messages.send(sender, "command.list.empty");
            return;
        }

        DecimalFormat format = new DecimalFormat("#.#");
        for (Dog dog : dogs) {
            Component health = services.dogs().wolfOf(dog).map(wolf -> messages.component("command.list.health",
                            Messages.text("current", format.format(wolf.getHealth())),
                            Messages.text("max", format.format(maxHealth(wolf)))))
                    .orElse(Component.empty());
            messages.send(sender, "command.list.entry", Messages.dog(dog), Messages.component("health", health));
        }
    }

    static double maxHealth(Wolf wolf) {
        AttributeInstance maxHealth = wolf.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth != null ? maxHealth.getValue() : wolf.getHealth();
    }
}
