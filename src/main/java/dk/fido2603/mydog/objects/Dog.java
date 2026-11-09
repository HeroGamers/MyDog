package dk.fido2603.mydog.objects;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import dk.fido2603.mydog.config.TreatKind;

/**
 * A dog's saved data. This is a plain data object: changing it does not touch the wolf entity or save anything.
 * Go through {@link dk.fido2603.mydog.service.DogService} to change a dog, so the wolf and dogs.yml stay in sync.
 */
public final class Dog {
    /** Where a dog was last seen, kept by world name so it survives the world being unloaded. */
    public record LastSeen(String world, double x, double y, double z) {
        public static LastSeen of(Location location) {
            World world = location.getWorld();
            return new LastSeen(world != null ? world.getName() : null, location.getX(), location.getY(), location.getZ());
        }

        /** The location, or {@code null} when the world is not loaded. */
        public Location toLocation(Server server) {
            World w = world != null ? server.getWorld(world) : null;
            return w != null ? new Location(w, x, y, z) : null;
        }
    }

    private UUID dogId;
    private UUID ownerId;
    private int identifier;
    private String name;
    private NamedTextColor color;
    private int level;
    private int experience;
    private Instant birthday;
    private boolean angry;
    private boolean dead;
    private LastSeen lastSeen;
    /** The wolf's looks, kept so a revived dog looks like it did. Null until the wolf has been seen. */
    private Appearance appearance;
    private final Set<TreatKind> freeChoicesUsed = EnumSet.noneOf(TreatKind.class);

    /**
     * How the wolf looks and sounds. Variants are stored by key (e.g. {@code minecraft:snowy}).
     * Any part may be null when it isn't known.
     */
    public record Appearance(DyeColor collar, String variant, String soundVariant) {}

    public Dog(UUID dogId, UUID ownerId, int identifier, String name, NamedTextColor color, int level, int experience,
               Instant birthday, boolean angry, boolean dead, LastSeen lastSeen) {
        this.dogId = dogId;
        this.ownerId = ownerId;
        this.identifier = identifier;
        this.name = name;
        this.color = color;
        this.level = level;
        this.experience = experience;
        this.birthday = birthday;
        this.angry = angry;
        this.dead = dead;
        this.lastSeen = lastSeen;
    }

    /** The UUID of the wolf entity. It changes when the dog is revived, since that spawns a new wolf. */
    public UUID getDogId() {
        return dogId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    /** The number players use to pick this dog in commands. Unique per owner. */
    public int getIdentifier() {
        return identifier;
    }

    public String getDogName() {
        return name;
    }

    /** The color the dog's name is shown in, taken from its collar. */
    public NamedTextColor getColor() {
        return color;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public Instant getBirthday() {
        return birthday;
    }

    /** Whether the dog is in attack mode (looks for monsters near its owner) rather than defend mode. */
    public boolean isAngry() {
        return angry;
    }

    public boolean isDead() {
        return dead;
    }

    public LastSeen getLastSeen() {
        return lastSeen;
    }

    public Appearance getAppearance() {
        return appearance;
    }

    /** Whether the owner has used the free breed or personality choice that old dogs get. */
    public boolean hasUsedFreeChoice(TreatKind kind) {
        return freeChoicesUsed.contains(kind);
    }

    public void setFreeChoiceUsed(TreatKind kind) {
        freeChoicesUsed.add(kind);
    }

    public void setAppearance(Appearance appearance) {
        this.appearance = appearance;
    }

    /** Use {@link dk.fido2603.mydog.storage.DogRepository#changeDogId} instead; the repository is keyed by this. */
    public void setDogId(UUID dogId) {
        this.dogId = dogId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public void setDogName(String name) {
        this.name = name;
    }

    public void setColor(NamedTextColor color) {
        this.color = color;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public void setAngry(boolean angry) {
        this.angry = angry;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void setLastSeen(LastSeen lastSeen) {
        this.lastSeen = lastSeen;
    }
}
