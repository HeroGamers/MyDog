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
        Dog dog = services.dogs().find(player.getUniqueId(), parseIdentifier(args.getFirst()))
                .filter(Dog::isDead)
                .orElseThrow(() -> new CommandException("command.revive.not-found"));

        int price = services.dogs().revivalPrice(dog);
        Settings.Revival revival = services.settings().get().dogs().revival();
        if (revival.usePlayerLevels()) {
            if (player.getLevel() < price) {
                throw new CommandException("command.revive.not-enough-levels", Messages.text("missing", price - player.getLevel()));
            }
            player.setLevel(player.getLevel() - price);
        } else {
            Optional<Economy> economy = Economy.find(services.server());
            if (economy.isPresent() && price > 0 && !economy.get().withdraw(player, price)) {
                throw new CommandException("command.revive.not-enough-money");
            }
        }

        services.dogs().revive(dog, player);
        services.messages().send(sender, "command.revive.done", Messages.dog(dog));
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return args.size() == 1 ? deadDogIds(sender) : List.of();
    }
}
