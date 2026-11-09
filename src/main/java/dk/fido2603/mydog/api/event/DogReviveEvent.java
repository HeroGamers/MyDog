package dk.fido2603.mydog.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import dk.fido2603.mydog.api.DogInfo;

/** A dead dog is about to be revived. The reviver is only charged when this goes ahead. */
public class DogReviveEvent extends DogEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player reviver;
    private boolean cancelled;

    public DogReviveEvent(DogInfo dog, Player reviver) {
        super(dog);
        this.reviver = reviver;
    }

    /** Who is reviving the dog (its owner, or an admin); the dog appears next to them. */
    public Player getReviver() {
        return reviver;
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
