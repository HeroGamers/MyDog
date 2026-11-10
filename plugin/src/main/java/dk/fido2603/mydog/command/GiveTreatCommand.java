package dk.fido2603.mydog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.config.StatBoost;
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
        super(services, "givetreat", "mydog.givetreat", "<player> <breed | personality | boost> <name> [amount]", false);
        this.treats = treats;
    }

    @Override
    public boolean isAvailable() {
        return services.settings().get().breeds().treats() || services.settings().get().boosts().enabled();
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
        TreatService.Treat treatType = parseTreat(args.get(1), args.get(2));
        if (treatType == null || !treats.isConfigured(treatType)) {
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

        ItemStack treat = treats.create(treatType, amount);
        // Whatever doesn't fit in the inventory lands at the player's feet
        for (ItemStack leftover : target.getInventory().addItem(treat).values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }

        var name = Messages.text("treat", Messages.displayName(args.get(2)) + " " + args.get(1).toLowerCase(Locale.ROOT));
        services.messages().send(sender, "command.givetreat.done", name,
                Messages.text("player", target.getName()), Messages.text("amount", amount));
        if (!target.equals(sender)) {
            services.messages().send(target, "command.givetreat.received", name, Messages.text("amount", amount));
        }
    }

    /** "breed snowy", "personality cute" or "boost health", or null if it isn't a treat. */
    private static TreatService.Treat parseTreat(String kindName, String name) {
        if (kindName.equalsIgnoreCase("boost")) {
            StatBoost stat = StatBoost.byId(name);
            return stat == null ? null : new TreatService.Boost(stat);
        }
        TreatKind kind = TreatKind.byId(kindName);
        NamespacedKey variant = TreatKind.parseKey(name);
        return kind == null || variant == null ? null : new TreatService.Look(kind, variant);
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return switch (args.size()) {
            case 1 -> services.server().getOnlinePlayers().stream().map(Player::getName).toList();
            case 2 -> List.of(TreatKind.BREED.id(), TreatKind.PERSONALITY.id(), "boost");
            case 3 -> {
                if (args.get(1).equalsIgnoreCase("boost")) {
                    yield services.settings().get().boosts().stats().keySet().stream().map(StatBoost::id).toList();
                }
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
