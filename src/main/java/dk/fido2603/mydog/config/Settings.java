package dk.fido2603.mydog.config;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

/**
 * Everything in config.yml, read and checked once per (re)load. Bad values are logged and replaced with a default.
 */
public record Settings(boolean debug, Saving saving, Teleport teleport, Dogs dogs, Levels levels) {

    public record Saving(boolean instant, int autosaveMinutes) {}

    public record Teleport(boolean enabled, boolean onWorldChange, boolean allTameables, boolean experimental,
                           boolean expandedSearch, boolean distanceCheck, double maxDistance) {}

    public record Dogs(boolean randomCollarColor, List<String> names, boolean nametagOnlyOnHover,
                       boolean nametagShowLevel, boolean allowNametagRename, boolean arrowDamage, boolean crammingDamage,
                       double lifesteal,
                       LowHealthWarning lowHealthWarning, Revival revival) {}

    public record LowHealthWarning(boolean enabled, double threshold, long cooldownMillis) {}

    public record Revival(boolean enabled, int pricePerLevel, boolean usePlayerLevels) {}

    public record Levels(boolean enabled, Sound levelUpSound, boolean playerKillExperience, LevelTable table,
                         Map<EntityType, Integer> experiencePerKill, int defaultExperience) {

        /**
         * Experience for killing this entity: its entry in the table, or the default for other mobs. Players only
         * count through their own entry, and entities that aren't mobs (armor stands, mannequins) give nothing.
         */
        public int experienceFor(Entity entity) {
            EntityType type = entity.getType();
            if (type == EntityType.PLAYER) {
                return playerKillExperience ? experiencePerKill.getOrDefault(type, 0) : 0;
            }
            Integer experience = experiencePerKill.get(type);
            if (experience != null) {
                return experience;
            }
            return entity instanceof Mob ? defaultExperience : 0;
        }
    }

    /** Reads settings from a config that has the bundled config.yml as its defaults. */
    public static Settings load(ConfigurationSection config, Logger log) {
        Saving saving = new Saving(
                config.getBoolean("saving.instant"),
                atLeast(log, config, "saving.autosave-minutes", 1));

        Teleport teleport = new Teleport(
                config.getBoolean("teleport.enabled"),
                config.getBoolean("teleport.on-world-change"),
                config.getBoolean("teleport.all-tameables"),
                config.getBoolean("teleport.experimental"),
                config.getBoolean("teleport.expanded-search"),
                config.getBoolean("teleport.distance-check.enabled"),
                Math.max(1.0, config.getDouble("teleport.distance-check.max-distance")));

        Dogs dogs = new Dogs(
                config.getBoolean("dogs.random-collar-color"),
                config.getStringList("dogs.names").stream().filter(name -> !name.isBlank()).toList(),
                config.getBoolean("dogs.nametag.only-on-hover"),
                config.getBoolean("dogs.nametag.show-level"),
                config.getBoolean("dogs.allow-nametag-rename"),
                config.getBoolean("dogs.arrow-damage"),
                config.getBoolean("dogs.cramming-damage"),
                share(log, config, "dogs.lifesteal"),
                new LowHealthWarning(
                        config.getBoolean("dogs.low-health-warning.enabled"),
                        share(log, config, "dogs.low-health-warning.threshold"),
                        Math.max(0L, config.getLong("dogs.low-health-warning.cooldown-millis"))),
                new Revival(
                        config.getBoolean("dogs.revival.enabled"),
                        atLeast(log, config, "dogs.revival.price-per-level", 0),
                        config.getBoolean("dogs.revival.use-player-levels")));

        Levels levels = new Levels(
                config.getBoolean("levels.enabled"),
                sound(log, config.getString("levels.level-up-sound", "")),
                config.getBoolean("levels.player-kill-experience"),
                levelTable(log, config),
                experiencePerKill(log, config.getConfigurationSection("levels.experience-per-kill")),
                Math.max(0, config.getInt("levels.experience-per-kill.default")));

        return new Settings(config.getBoolean("debug"), saving, teleport, dogs, levels);
    }

    private static int atLeast(Logger log, ConfigurationSection config, String path, int min) {
        int value = config.getInt(path);
        if (value < min) {
            log.warning(path + " must be at least " + min + ", using " + min + ".");
            return min;
        }
        return value;
    }

    private static double share(Logger log, ConfigurationSection config, String path) {
        double value = config.getDouble(path);
        if (value < 0.0 || value > 1.0) {
            log.warning(path + " must be between 0 and 1, got " + value + ".");
            return Math.clamp(value, 0.0, 1.0);
        }
        return value;
    }

    private static Sound sound(Logger log, String key) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
        Sound sound = namespacedKey != null ? Registry.SOUNDS.get(namespacedKey) : null;
        if (sound == null) {
            log.warning("Unknown levels.level-up-sound '" + key + "', using entity.wolf.ambient.");
            return Sound.ENTITY_WOLF_AMBIENT;
        }
        return sound;
    }

    private static LevelTable levelTable(Logger log, ConfigurationSection config) {
        ConfigurationSection section = config.getConfigurationSection("levels.table");
        Map<Integer, LevelTable.Level> levels = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection levelSection = section.getConfigurationSection(key);
                int level;
                try {
                    level = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    level = 0;
                }
                if (level < 1 || levelSection == null) {
                    log.warning("Skipping levels.table." + key + ": levels must be numbers from 1 up, with experience, health and damage.");
                    continue;
                }
                levels.put(level, new LevelTable.Level(level,
                        levelSection.getInt("experience"),
                        levelSection.getDouble("health"),
                        levelSection.getDouble("damage")));
            }
        }

        if (levels.isEmpty()) {
            ConfigurationSection defaults = config.getDefaultSection();
            if (defaults != null) {
                log.warning("levels.table has no valid levels, using the default table.");
                return levelTable(log, defaults);
            }
            levels.put(1, new LevelTable.Level(1, 0, 20, 4));
        }
        if (!levels.containsKey(1)) {
            log.warning("levels.table has no level 1; new dogs start at level 1 and get the lowest level's stats.");
        }
        return new LevelTable(levels);
    }

    private static Map<EntityType, Integer> experiencePerKill(Logger log, ConfigurationSection section) {
        Map<EntityType, Integer> experience = new EnumMap<>(EntityType.class);
        if (section == null) {
            return experience;
        }
        for (String key : section.getKeys(false)) {
            if (key.equals("default")) {
                continue;
            }
            EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
            if (type == null) {
                log.warning("Unknown entity type '" + key + "' in levels.experience-per-kill, skipping it.");
                continue;
            }
            experience.put(type, Math.max(0, section.getInt(key)));
        }
        return experience;
    }
}
