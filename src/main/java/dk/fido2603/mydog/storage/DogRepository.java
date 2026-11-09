package dk.fido2603.mydog.storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dk.fido2603.mydog.objects.Dog;

/**
 * Where dogs are kept. All dogs are held in memory; {@link #markChanged} schedules them to be written out.
 * Only call this from the server thread.
 */
public interface DogRepository {
    Optional<Dog> find(UUID dogId);

    /** All dogs of an owner, alive and dead, sorted by ID. */
    List<Dog> findByOwner(UUID ownerId);

    Optional<Dog> findByOwnerAndIdentifier(UUID ownerId, int identifier);

    Collection<Dog> all();

    /** Adds a new dog, or records that an existing dog has changed. */
    void markChanged(Dog dog);

    void delete(UUID dogId);

    /** Moves a dog to a new UUID, e.g. when it is revived as a new wolf. */
    void changeDogId(Dog dog, UUID newDogId);

    /** Writes changes in the background, if there are any. */
    void saveAsync();

    /** Writes changes and waits until they are on disk. Used on shutdown. */
    void saveNow();
}
