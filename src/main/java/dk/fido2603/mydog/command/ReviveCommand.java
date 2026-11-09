package dk.fido2603.mydog.command;

import java.util.List;
import java.util.Optional;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.Economy;

/**
 * Brings a dead dog back for its revival price, paid in money or experience levels. Without an economy plugin,
 * revival paid in money is free.
 */
public final class ReviveCommand extends SubCommand {
    public ReviveCommand(CommandServices services) {
        super(services, "revive", "mydog.revive", "<id>", true);
    }

    @Override
    public boolean isAvailable() {
        return services.settings().get().dogs().revival().enabled();
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 1) {
            throw new UsageException();
        }
        Player player = (Player) sender;
        Dog dog = dog(player, args.getFirst(), Access.OWNER);
        if (!dog.isDead()) {
            throw new CommandException("command.revive.not-found");
        }

        int price = services.dogs().revivalPrice(dog);
        Settings.Revival revival = services.settings().get().dogs().revival();
        Optional<Economy> economy = revival.usePlayerLevels() ? Optional.empty() : Economy.find(services.server());
        // Check first, charge only once the revive went ahead (another plugin may stop it)
        if (revival.usePlayerLevels() && player.getLevel() < price) {
            throw new CommandException("command.revive.not-enough-levels", Messages.text("missing", price - player.getLevel()));
        }
        if (economy.isPresent() && price > 0 && !economy.get().has(player, price)) {
            throw new CommandException("command.revive.not-enough-money");
        }

        if (!services.dogs().revive(dog, player)) {
            throw new CommandException("command.cancelled");
        }
        if (revival.usePlayerLevels()) {
            player.setLevel(player.getLevel() - price);
        } else if (economy.isPresent() && price > 0) {
            economy.get().withdraw(player, price);
        }
        services.messages().send(sender, "command.revive.done", Messages.dog(dog));
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return args.size() == 1 ? deadDogIds(sender) : List.of();
    }
}
