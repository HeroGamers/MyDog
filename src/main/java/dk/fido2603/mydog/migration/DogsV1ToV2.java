package dk.fido2603.mydog.migration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades the dogs.yml written by MyDog 0.2.x and earlier to version 2.
 * <p>
 * Version 1 kept every dog at the top level, keyed by UUID, with CamelCase fields and the birthday as
 * {@code dd-MM-yyyy HH:mm} in the server's time zone. Version 2 keeps the dogs under {@code dogs:}, uses
 * kebab-case fields and an ISO-8601 instant for the birthday, and stores the name color as a color name.
 */
public final class DogsV1ToV2 implements Migration {
    private static final DateTimeFormatter OLD_BIRTHDAY = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final Logger log;

    public DogsV1ToV2(Logger log) {
        this.log = log;
    }

    @Override
    public int fromVersion() {
        return 1;
    }

    @Override
    public YamlConfiguration migrate(YamlConfiguration old) {
        YamlConfiguration data = new YamlConfiguration();
        int migrated = 0;

        for (String key : old.getKeys(false)) {
            ConfigurationSection oldDog = old.getConfigurationSection(key);
            if (oldDog == null || !isUuid(key)) {
                continue;
            }
            String owner = oldDog.getString("Owner");
            if (owner == null || !isUuid(owner)) {
                log.warning("Skipping dog " + key + " in dogs.yml: it has no valid owner.");
                continue;
            }

            ConfigurationSection dog = data.createSection("dogs." + key);
            dog.set("owner", owner);
            dog.set("id", oldDog.getInt("ID", 0));
            dog.set("name", oldDog.getString("Name"));
            String color = oldDog.getString("NameChatColor");
            if (color != null) {
                // ChatColor names (DARK_RED) match the color names used now (dark_red)
                dog.set("color", color.toLowerCase(Locale.ROOT));
            }
            dog.set("level", Math.max(1, oldDog.getInt("Level.Level", 1)));
            dog.set("experience", oldDog.getInt("Level.Experience", 0));
            dog.set("born", birthday(key, oldDog.getString("Birthday")));
            dog.set("attack-mode", oldDog.getBoolean("Angry", false));
            // Very old versions of MyDog wrote "isDead"
            dog.set("dead", oldDog.getBoolean("Dead", oldDog.getBoolean("isDead", false)));
            if (oldDog.isString("LastSeen.World")) {
                dog.set("last-seen.world", oldDog.getString("LastSeen.World"));
                dog.set("last-seen.x", oldDog.getDouble("LastSeen.X"));
                dog.set("last-seen.y", oldDog.getDouble("LastSeen.Y"));
                dog.set("last-seen.z", oldDog.getDouble("LastSeen.Z"));
            }
            migrated++;
        }

        log.info("Converted " + migrated + " dogs to the new dogs.yml format.");
        return data;
    }

    private String birthday(String dogId, String oldBirthday) {
        if (oldBirthday != null) {
            try {
                return LocalDateTime.parse(oldBirthday, OLD_BIRTHDAY).atZone(ZoneId.systemDefault()).toInstant().toString();
            } catch (DateTimeParseException e) {
                log.warning("Dog " + dogId + " has an unreadable birthday '" + oldBirthday + "', setting it to now.");
            }
        }
        return java.time.Instant.now().toString();
    }

    private static boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
