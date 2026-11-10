package dk.fido2603.mydog.api.event;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A tamed wolf is about to become a MyDog dog. Cancelling it leaves the wolf a plain tamed wolf.
 */
public class DogTameEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    public enum Cause {
        /** The player tamed a wild wolf. */
        TAMED,
        /** A puppy was born from bred wolves. */
        BRED,
        /** A wolf tamed before MyDog was installed was right-clicked by its owner. */
        EXISTING_WOLF
    }

    private final Wolf wolf;
    private final Player owner;
    private final Cause cause;
    private boolean cancelled;

    public DogTameEvent(Wolf wolf, Player owner, Cause cause) {
        this.wolf = wolf;
        this.owner = owner;
        this.cause = cause;
    }

    public Wolf getWolf() {
        return wolf;
    }

    public Player getOwner() {
        return owner;
    }

    public Cause getCause() {
        return cause;
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
