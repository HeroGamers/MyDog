package dk.fido2603.mydog.config;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Wolf;

/**
 * The two things about a wolf that dog treats and free choices can change: its breed (texture variant, 1.20.5) and
 * its personality (sound variant, 1.21.5).
 * <p>
 * All registry lookups for these live here. Paper deprecates {@code Registry.WOLF_VARIANT} and
 * {@code Bukkit.getRegistry(Class)} in favor of its own RegistryAccess, which Spigot doesn't have.
 */
@SuppressWarnings("deprecation")
public enum TreatKind {
    BREED("breed", "breeds", NamespacedKey.minecraft("pale")),
    PERSONALITY("personality", "personalities", NamespacedKey.minecraft("classic"));

    private final String id;
    private final String configSection;
    private final NamespacedKey legacyDefault;

    TreatKind(String id, String configSection, NamespacedKey legacyDefault) {
        this.id = id;
        this.configSection = configSection;
        this.legacyDefault = legacyDefault;
    }

    /** "breed" or "personality": used in commands, message keys and the treat item tag. */
    public String id() {
        return id;
    }

    /** The section under breeds.treats in config.yml. */
    public String configSection() {
        return configSection;
    }

    /** What every wolf got when this was added to Minecraft (Pale / Classic). */
    public NamespacedKey legacyDefault() {
        return legacyDefault;
    }

    public static TreatKind byId(String id) {
        for (TreatKind kind : values()) {
            if (kind.id.equalsIgnoreCase(id)) {
                return kind;
            }
        }
        return null;
    }

    private Registry<? extends Keyed> registry() {
        return switch (this) {
            case BREED -> Registry.WOLF_VARIANT;
            // Not every server software exposes this registry; without it personalities can't be changed
            case PERSONALITY -> Bukkit.getRegistry(Wolf.SoundVariant.class);
        };
    }

    public boolean exists(NamespacedKey key) {
        Registry<? extends Keyed> registry = registry();
        return registry != null && registry.get(key) != null;
    }

    /** All breeds or personalities the server knows. */
    public List<NamespacedKey> all() {
        Registry<? extends Keyed> registry = registry();
        return registry == null ? List.of() : registry.stream().map(Keyed::getKey)
                .sorted(Comparator.comparing(NamespacedKey::toString)).toList();
    }

    /** The wolf's current breed or personality. */
    public NamespacedKey of(Wolf wolf) {
        return switch (this) {
            case BREED -> wolf.getVariant().getKey();
            case PERSONALITY -> wolf.getSoundVariant().getKey();
        };
    }

    /** Gives the wolf this breed or personality. Returns false if it doesn't exist. */
    public boolean apply(Wolf wolf, NamespacedKey key) {
        switch (this) {
            case BREED -> {
                Wolf.Variant variant = Registry.WOLF_VARIANT.get(key);
                if (variant == null) {
                    return false;
                }
                wolf.setVariant(variant);
            }
            case PERSONALITY -> {
                Registry<Wolf.SoundVariant> registry = Bukkit.getRegistry(Wolf.SoundVariant.class);
                Wolf.SoundVariant variant = registry != null ? registry.get(key) : null;
                if (variant == null) {
                    return false;
                }
                wolf.setSoundVariant(variant);
            }
        }
        return true;
    }

    /** Reads a breed or personality the way players type it: "snowy" or "minecraft:snowy". */
    public static NamespacedKey parseKey(String input) {
        return NamespacedKey.fromString(input.toLowerCase(Locale.ROOT));
    }
}
