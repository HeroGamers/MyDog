package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.config.TreatKind;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.service.TreatService;

/**
 * {@code /mydog givetreat <player> <breed|personality> <name> [amount]}: hands out dog treats, also ones that can't be
 * crafted. Works from the console, so shop, auction and crate plugins can run it.
 */
public final class GiveTreatCommand extends SubCommand {
    private static final int MAX_AMOUNT = 64 * 36;

    private final TreatService treats;

    public GiveTreatCommand(CommandServices services, TreatService treats) {
        super(services, "givetreat", "mydog.givetreat", "<player> <breed | personality> <name> [amount]", false);
        this.treats = treats;
    }

    @Override
    public boolean isAvailable() {
        return services.settings().get().breeds().treats();
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() < 3 || args.size() > 4) {
            throw new UsageException();
        }
        Player target = services.server().getPlayerExact(args.getFirst());
        if (target == null) {
            throw new CommandException("command.trade.player-not-found");
        }
        TreatKind kind = TreatKind.byId(args.get(1));
        NamespacedKey variant = TreatKind.parseKey(args.get(2));
        if (kind == null || variant == null || !treats.isConfigured(new TreatService.Treat(kind, variant))) {
            throw new CommandException("command.givetreat.unknown");
        }
        int amount = 1;
        if (args.size() == 4) {
            try {
                amount = Integer.parseInt(args.get(3));
            } catch (NumberFormatException e) {
                amount = 0;
            }
            if (amount < 1 || amount > MAX_AMOUNT) {
                throw new CommandException("command.givetreat.invalid-amount", Messages.text("max", MAX_AMOUNT));
            }
        }

        ItemStack treat = treats.create(new TreatService.Treat(kind, variant), amount);
        // Whatever doesn't fit in the inventory lands at the player's feet
        for (ItemStack leftover : target.getInventory().addItem(treat).values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }

        var name = Messages.text("treat", Messages.displayName(variant.toString()) + " " + kind.id());
        services.messages().send(sender, "command.givetreat.done", name,
                Messages.text("player", target.getName()), Messages.text("amount", amount));
        if (!target.equals(sender)) {
            services.messages().send(target, "command.givetreat.received", name, Messages.text("amount", amount));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return switch (args.size()) {
            case 1 -> services.server().getOnlinePlayers().stream().map(Player::getName).toList();
            case 2 -> List.of(TreatKind.BREED.id(), TreatKind.PERSONALITY.id());
            case 3 -> {
                TreatKind kind = TreatKind.byId(args.get(1));
                List<NamespacedKey> configured = new ArrayList<>();
                for (Settings.TreatRecipe recipe : services.settings().get().breeds().recipes()) {
                    if (recipe.kind() == kind) {
                        configured.add(recipe.variant());
                    }
                }
                yield LookCommand.names(configured);
            }
            default -> List.of();
        };
    }
}
