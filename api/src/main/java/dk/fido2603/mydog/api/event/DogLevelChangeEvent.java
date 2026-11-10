package dk.fido2603.mydog.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import dk.fido2603.mydog.api.DogInfo;

/**
 * A dog's level is about to change. When cancelled after a level-up from experience, the dog keeps the experience
 * and tries to level up again the next time it gets some.
 */
public class DogLevelChangeEvent extends DogEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    public enum Cause {
        /** The dog earned enough experience. */
        EXPERIENCE,
        /** /mydog setlevel, or another plugin. */
        SET
    }

    private final int oldLevel;
    private final int newLevel;
    private final Cause cause;
    private boolean cancelled;

    public DogLevelChangeEvent(DogInfo dog, int oldLevel, int newLevel, Cause cause) {
        super(dog);
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.cause = cause;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
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
