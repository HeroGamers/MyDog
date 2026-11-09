package dk.fido2603.mydog.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import dk.fido2603.mydog.api.DogInfo;

/**
 * A dog is about to be teleported to its owner: because the owner teleported, wandered too far away, or the dog's
 * chunk is unloading. Cancel it to keep dogs out of a region, a world or a minigame.
 */
public class DogFollowEvent extends DogEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Wolf wolf;
    private final Player owner;
    private final Location to;
    private boolean cancelled;

    public DogFollowEvent(DogInfo dog, Wolf wolf, Player owner, Location to) {
        super(dog);
        this.wolf = wolf;
        this.owner = owner;
        this.to = to;
    }

    public Wolf getWolf() {
        return wolf;
    }

    public Player getOwner() {
        return owner;
    }

    /** Where the dog would land. */
    public Location getTo() {
        return to.clone();
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
