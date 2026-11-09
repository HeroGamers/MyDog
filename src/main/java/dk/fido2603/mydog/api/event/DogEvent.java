package dk.fido2603.mydog.api.event;

import org.bukkit.event.Event;

import dk.fido2603.mydog.api.DogInfo;

/** An event about an existing dog. */
public abstract class DogEvent extends Event {
    private final DogInfo dog;

    protected DogEvent(DogInfo dog) {
        this.dog = dog;
    }

    /** The dog, as it was right before this event. */
    public DogInfo getDog() {
        return dog;
    }
}
