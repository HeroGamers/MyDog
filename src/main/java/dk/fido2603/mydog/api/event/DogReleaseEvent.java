package dk.fido2603.mydog.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import dk.fido2603.mydog.api.DogInfo;

/** A dog is about to be set free: its wolf goes wild and MyDog forgets it. */
public class DogReleaseEvent extends DogEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player releaser;
    private boolean cancelled;

    public DogReleaseEvent(DogInfo dog, Player releaser) {
        super(dog);
        this.releaser = releaser;
    }

    /** Who is releasing the dog (its owner, or an admin). */
    public Player getReleaser() {
        return releaser;
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
