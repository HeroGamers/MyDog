package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.service.Permissions;
import dk.fido2603.mydog.service.TradeService;

public final class TradeDeclineCommand extends SubCommand {
    public TradeDeclineCommand(CommandServices services) {
        super(services, "tradedecline", Permissions.TRADE, "", true, "tradedeny");
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException {
        TradeService.Trade trade = services.trades().remove(((Player) sender).getUniqueId())
                .orElseThrow(() -> new CommandException("command.trade.no-pending"));

        services.messages().send(sender, "command.trade.denied");
        Player seller = services.server().getPlayer(trade.sellerId());
        if (seller != null) {
            services.messages().send(seller, "command.trade.denied-sender");
        }
    }
}
