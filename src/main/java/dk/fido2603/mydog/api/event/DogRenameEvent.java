package dk.fido2603.mydog.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import dk.fido2603.mydog.api.DogInfo;

/** A dog is about to get a new name, through /mydog rename, a name tag, or another plugin. */
public class DogRenameEvent extends DogEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player renamer;
    private String newName;
    private boolean cancelled;

    public DogRenameEvent(DogInfo dog, String newName, Player renamer) {
        super(dog);
        this.newName = newName;
        this.renamer = renamer;
    }

    /** The new name, without colors. */
    public String getNewName() {
        return newName;
    }

    /** Changes the name the dog gets. Colors are removed; an empty name cancels the rename. */
    public void setNewName(String newName) {
        this.newName = newName;
    }

    /** Who is renaming the dog, or null when another plugin does it. */
    public Player getRenamer() {
        return renamer;
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
