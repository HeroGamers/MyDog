package dk.fido2603.mydog.command;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.Economy;
import dk.fido2603.mydog.service.Permissions;

/** Offers a dog to another online player, for free or for money. */
public final class TradeCommand extends SubCommand {
    public TradeCommand(CommandServices services) {
        super(services, "trade", Permissions.TRADE, "<id> <player> [price]", true);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() < 2 || args.size() > 3) {
            throw new UsageException();
        }
        Player seller = (Player) sender;
        Dog dog = ownDog(seller, args.getFirst());
        if (dog.isDead()) {
            throw new CommandException("command.trade.dead-dog");
        }

        Player buyer = services.server().getPlayerExact(args.get(1));
        if (buyer == null) {
            throw new CommandException("command.trade.player-not-found");
        }
        if (buyer.getUniqueId().equals(seller.getUniqueId())) {
            throw new CommandException("command.trade.self");
        }

        double price = 0.0;
        if (args.size() == 3) {
            try {
                price = Double.parseDouble(args.get(2));
            } catch (NumberFormatException e) {
                price = -1.0;
            }
            if (price < 0.0 || !Double.isFinite(price)) {
                throw new CommandException("command.trade.invalid-price");
            }
        }

        if (price > 0.0) {
            Optional<Economy> economy = Economy.find(services.server());
            if (economy.isEmpty()) {
                throw new CommandException("command.trade.no-economy");
            }
            if (!economy.get().has(buyer, price)) {
                throw new CommandException("command.trade.receiver-cannot-afford");
            }
        }
        if (services.trades().pendingFor(buyer.getUniqueId()).isPresent()) {
            throw new CommandException("command.trade.receiver-has-pending");
        }
        if (!Permissions.has(buyer, Permissions.TRADE)) {
            throw new CommandException("command.trade.receiver-no-permission");
        }
        if (!services.dogs().canOwnMore(buyer)) {
            throw new CommandException("command.trade.receiver-limit");
        }

        services.trades().offer(dog, seller, buyer, price);

        Component levelText = services.settings().get().levels().enabled()
                ? services.messages().component("command.trade.request-level", Messages.dog(dog))
                : Component.empty();
        Component priceText = price > 0.0
                ? Component.text(new DecimalFormat("#.##").format(price) + "$")
                : services.messages().component("command.trade.request-free");
        services.messages().send(buyer, "command.trade.request", Messages.dog(dog),
                Messages.text("sender", seller.getName()),
                Messages.component("price", priceText),
                Messages.component("level_text", levelText));
        services.messages().send(sender, "command.trade.sent");
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return switch (args.size()) {
            case 1 -> livingDogIds(sender);
            case 2 -> {
                List<String> names = new ArrayList<>();
                for (Player player : services.server().getOnlinePlayers()) {
                    if (!player.equals(sender)) {
                        names.add(player.getName());
                    }
                }
                yield names;
            }
            default -> List.of();
        };
    }
}
