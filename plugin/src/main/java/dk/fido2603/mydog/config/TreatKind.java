package dk.fido2603.mydog.config;

import java.lang.reflect.Method;
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
 * <p>
 * Personalities ({@code Wolf.SoundVariant}) are only in Paper's API. Spigot has no such class, so they are reached
 * through reflection, and on Spigot there are simply none: no personality treats, free picks or command.
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

    /** Whether this server's API lets MyDog read and change it. Personalities need Paper. */
    public boolean supported() {
        return this == BREED || Personalities.TYPE != null;
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
            case PERSONALITY -> Personalities.TYPE != null ? Bukkit.getRegistry(Personalities.TYPE) : null;
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

    /** The wolf's current breed or personality, or null when the server can't tell. */
    public NamespacedKey of(Wolf wolf) {
        return switch (this) {
            case BREED -> wolf.getVariant().getKey();
            case PERSONALITY -> Personalities.get(wolf);
        };
    }

    /** Gives the wolf this breed or personality. Returns false if it doesn't exist (or can't be set) here. */
    public boolean apply(Wolf wolf, NamespacedKey key) {
        switch (this) {
            case BREED -> {
                Wolf.Variant variant = Registry.WOLF_VARIANT.get(key);
                if (variant == null) {
                    return false;
                }
                wolf.setVariant(variant);
                return true;
            }
            case PERSONALITY -> {
                Registry<? extends Keyed> registry = registry();
                Keyed variant = registry != null ? registry.get(key) : null;
                return variant != null && Personalities.set(wolf, variant);
            }
        }
        return false;
    }

    /** Reads a breed or personality the way players type it: "snowy" or "minecraft:snowy". */
    public static NamespacedKey parseKey(String input) {
        return NamespacedKey.fromString(input.toLowerCase(Locale.ROOT));
    }

    /** Paper's Wolf.SoundVariant and its getter and setter, all null on servers without them (Spigot). */
    private static final class Personalities {
        static final Class<? extends Keyed> TYPE;
        static final Method GET;
        static final Method SET;

        static {
            Class<? extends Keyed> type = null;
            Method get = null;
            Method set = null;
            try {
                type = Class.forName("org.bukkit.entity.Wolf$SoundVariant").asSubclass(Keyed.class);
                get = Wolf.class.getMethod("getSoundVariant");
                set = Wolf.class.getMethod("setSoundVariant", type);
            } catch (ReflectiveOperationException | ClassCastException e) {
                type = null;
            }
            TYPE = type;
            GET = get;
            SET = set;
        }

        static NamespacedKey get(Wolf wolf) {
            if (TYPE == null) {
                return null;
            }
            try {
                return GET.invoke(wolf) instanceof Keyed variant ? variant.getKey() : null;
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        static boolean set(Wolf wolf, Keyed variant) {
            if (TYPE == null) {
                return false;
            }
            try {
                SET.invoke(wolf, variant);
                return true;
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }
    }
}
