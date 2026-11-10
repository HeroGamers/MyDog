package dk.fido2603.mydog.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import dk.fido2603.mydog.api.event.DogTradeEvent;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/** Pending dog trades. A player can have one incoming offer at a time, and an offer expires after 30 seconds. */
public final class TradeService {
    private static final long EXPIRY_TICKS = 20L * 30L;

    public record Trade(UUID dogId, UUID sellerId, double price, BukkitTask expiry) {}

    public enum AcceptResult { ACCEPTED, NO_TRADE, DOG_MISSING, NO_ECONOMY, CANNOT_AFFORD, LIMIT, CANCELLED }

    private final Plugin plugin;
    private final DogService dogService;
    private final Messages messages;
    private final Map<UUID, Trade> pending = new HashMap<>();

    public TradeService(Plugin plugin, DogService dogService, Messages messages) {
        this.plugin = plugin;
        this.dogService = dogService;
        this.messages = messages;
    }

    public Optional<Trade> pendingFor(UUID buyerId) {
        return Optional.ofNullable(pending.get(buyerId));
    }

    /** Offers a dog to a buyer. The caller has already checked that the offer is allowed. */
    public void offer(Dog dog, Player seller, Player buyer, double price) {
        UUID buyerId = buyer.getUniqueId();
        BukkitTask expiry = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(buyerId) == null) {
                return;
            }
            Player currentBuyer = plugin.getServer().getPlayer(buyerId);
            if (currentBuyer != null) {
                messages.send(currentBuyer, "command.trade.expired");
            }
            Player currentSeller = plugin.getServer().getPlayer(seller.getUniqueId());
            if (currentSeller != null) {
                messages.send(currentSeller, "command.trade.expired-sender");
            }
        }, EXPIRY_TICKS);
        pending.put(buyerId, new Trade(dog.getDogId(), seller.getUniqueId(), price, expiry));
    }

    public AcceptResult accept(Player buyer) {
        Trade trade = pending.get(buyer.getUniqueId());
        if (trade == null) {
            return AcceptResult.NO_TRADE;
        }

        Optional<Dog> dog = dogService.find(trade.dogId());
        // The dog must still belong to the seller, and its wolf must be loaded so it can change owner
        if (dog.isEmpty() || dog.get().isDead() || !dog.get().getOwnerId().equals(trade.sellerId())
                || dogService.wolfOf(dog.get()).isEmpty()) {
            return AcceptResult.DOG_MISSING;
        }
        if (!dogService.canOwnMore(buyer)) {
            return AcceptResult.LIMIT;
        }
        if (dogService.fire(new DogTradeEvent(MyDogApiService.info(dog.get()), trade.sellerId(), buyer, trade.price())).isCancelled()) {
            return AcceptResult.CANCELLED;
        }

        if (trade.price() > 0.0) {
            Optional<Economy> economy = Economy.find(plugin.getServer());
            if (economy.isEmpty()) {
                return AcceptResult.NO_ECONOMY;
            }
            if (!economy.get().has(buyer, trade.price()) || !economy.get().withdraw(buyer, trade.price())) {
                return AcceptResult.CANNOT_AFFORD;
            }
            economy.get().deposit(plugin.getServer().getOfflinePlayer(trade.sellerId()), trade.price());
        }

        remove(buyer.getUniqueId());
        dogService.transfer(dog.get(), buyer);
        return AcceptResult.ACCEPTED;
    }

    /** Drops the buyer's pending trade, returning it if there was one. */
    public Optional<Trade> remove(UUID buyerId) {
        Trade trade = pending.remove(buyerId);
        if (trade != null) {
            trade.expiry().cancel();
        }
        return Optional.ofNullable(trade);
    }
}
