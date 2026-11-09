package dk.fido2603.mydog.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;

/**
 * A snapshot of a dog, as it was when it was fetched. It doesn't change when the dog does; fetch it again for fresh
 * data, and use {@link MyDogApi} to make changes.
 *
 * @param dogId       the UUID of the dog's wolf entity; it changes when the dog is revived or summoned
 * @param ownerId     the owner, whom the wolf belongs to and follows
 * @param coOwners    players the owner shared the dog with
 * @param identifier  the number the owner picks the dog by in commands (unique per owner)
 * @param name        the dog's name, without colors
 * @param attackMode  whether the dog is in attack mode (rather than defend mode)
 * @param dead        dead dogs are kept so they can be revived, when revival is on
 * @param stored      put away with /mydog store; such a dog has no wolf until it is summoned
 * @param collar      the collar color, or null if MyDog hasn't seen the wolf yet
 * @param breed       the wolf variant (e.g. minecraft:snowy), or null if MyDog hasn't seen the wolf yet
 * @param personality the wolf sound variant (e.g. minecraft:cute), or null if MyDog hasn't seen the wolf yet
 */
public record DogInfo(UUID dogId, UUID ownerId, Set<UUID> coOwners, int identifier, String name, int level,
                      int experience, Instant birthday, boolean attackMode, boolean dead, boolean stored,
                      DyeColor collar, NamespacedKey breed, NamespacedKey personality) {
    public DogInfo {
        coOwners = Set.copyOf(coOwners);
    }
}
