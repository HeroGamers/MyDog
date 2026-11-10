package dk.fido2603.mydog.api.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import dk.fido2603.mydog.api.DogInfo;

/** A dog is about to get experience. The amount can be changed, e.g. for experience boosts. */
public class DogExperienceEvent extends DogEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity killed;
    private int amount;
    private boolean cancelled;

    public DogExperienceEvent(DogInfo dog, int amount, Entity killed) {
        super(dog);
        this.amount = amount;
        this.killed = killed;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    /** What the dog killed to earn it, or null when it comes from elsewhere (e.g. another plugin). */
    public Entity getKilled() {
        return killed;
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
