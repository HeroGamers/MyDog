package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.service.Permissions;
import dk.fido2603.mydog.service.TradeService;

public final class TradeAcceptCommand extends SubCommand {
    public TradeAcceptCommand(CommandServices services) {
        super(services, "tradeaccept", Permissions.TRADE, "", true);
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException {
        Player buyer = (Player) sender;
        TradeService.Trade trade = services.trades().pendingFor(buyer.getUniqueId())
                .orElseThrow(() -> new CommandException("command.trade.no-pending"));

        switch (services.trades().accept(buyer)) {
            case ACCEPTED -> {
                services.messages().send(sender, "command.trade.accepted");
                Player seller = services.server().getPlayer(trade.sellerId());
                if (seller != null) {
                    services.messages().send(seller, "command.trade.accepted-sender");
                }
            }
            case NO_TRADE -> throw new CommandException("command.trade.no-pending");
            case DOG_MISSING -> throw new CommandException("command.trade.dog-missing");
            case NO_ECONOMY -> throw new CommandException("command.trade.no-economy");
            case CANNOT_AFFORD -> throw new CommandException("command.trade.cannot-afford");
            case LIMIT -> throw new CommandException("command.trade.limit");
        }
    }
}
