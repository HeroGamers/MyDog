package dk.fido2603.mydog.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;

/**
 * MyDog's API for other plugins. MyDog registers it in Bukkit's services manager while it is enabled:
 *
 * <pre>{@code
 * RegisteredServiceProvider<MyDogApi> provider = Bukkit.getServicesManager().getRegistration(MyDogApi.class);
 * if (provider != null) {
 *     MyDogApi myDog = provider.getProvider();
 * }
 * }</pre>
 *
 * Add MyDog to {@code softdepend} (or {@code depend}) in your plugin.yml so it is enabled first. Only call the API from
 * the server thread. Everything in this package is meant to stay compatible between MyDog versions; nothing outside it
 * is.
 */
public interface MyDogApi {
    /** Whether the entity is a MyDog dog. */
    boolean isDog(Entity entity);

    /** A dog by the UUID of its wolf. Also finds dead and stored dogs, whose wolf doesn't exist. */
    Optional<DogInfo> getDog(UUID dogId);

    /** The dog an entity is, if it is one. */
    Optional<DogInfo> getDog(Entity entity);

    /** A player's dog by the number they use in commands. */
    Optional<DogInfo> getDog(UUID ownerId, int identifier);

    /** All dogs a player owns (alive, dead and stored), sorted by identifier. */
    List<DogInfo> getDogs(UUID ownerId);

    /** Living dogs other players have shared with this player. */
    List<DogInfo> getSharedDogs(UUID playerId);

    /** The dog's wolf, if it is loaded. */
    Optional<Wolf> getWolf(UUID dogId);

    /**
     * Gives a dog experience, leveling it up (with the usual message and effects) when it has enough.
     *
     * @return false if there is no such living dog, or levels are turned off
     */
    boolean giveExperience(UUID dogId, int experience);

    /**
     * Sets a dog's level, updating its health and damage. The owner gets the level-up message.
     *
     * @return false if there is no such living dog, levels are turned off, or the level is outside the level table
     */
    boolean setLevel(UUID dogId, int level);

    /**
     * Renames a dog. Color codes are removed.
     *
     * @return false if there is no such dog, or the name is empty
     */
    boolean rename(UUID dogId, String name);
}
