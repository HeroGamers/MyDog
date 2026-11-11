package dk.fido2603.mydog.api.event;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import dk.fido2603.mydog.api.DogInfo;

/**
 * A buyer accepted a dog trade. Fired before any money changes hands. Also fired, with price 0, when another plugin
 * gave the dog to another player (MyDog config {@code dogs.follow-owner-changes}); cancelling it gives the dog back.
 */
public class DogTradeEvent extends DogEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID sellerId;
    private final Player buyer;
    private final double price;
    private boolean cancelled;

    public DogTradeEvent(DogInfo dog, UUID sellerId, Player buyer, double price) {
        super(dog);
        this.sellerId = sellerId;
        this.buyer = buyer;
        this.price = price;
    }

    /** The dog's owner, who offered it. They may be offline. */
    public UUID getSellerId() {
        return sellerId;
    }

    public Player getBuyer() {
        return buyer;
    }

    /** The price; 0 for a free trade. */
    public double getPrice() {
        return price;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
