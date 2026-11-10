package dk.fido2603.mydog.api.event;

import org.bukkit.entity.Wolf;
import org.bukkit.event.HandlerList;

import dk.fido2603.mydog.api.DogInfo;

/**
 * A dog died. A revivable dog is kept as dead, so its owner can revive it; otherwise MyDog forgets it.
 */
public class DogDeathEvent extends DogEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Wolf wolf;
    private boolean revivable;

    public DogDeathEvent(DogInfo dog, Wolf wolf, boolean revivable) {
        super(dog);
        this.wolf = wolf;
        this.revivable = revivable;
    }

    /** The wolf that died. */
    public Wolf getWolf() {
        return wolf;
    }

    /** Whether the dog can be revived. Starts as dogs.revival.enabled. */
    public boolean isRevivable() {
        return revivable;
    }

    public void setRevivable(boolean revivable) {
        this.revivable = revivable;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
