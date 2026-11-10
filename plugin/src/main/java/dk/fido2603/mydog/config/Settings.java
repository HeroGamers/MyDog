package dk.fido2603.mydog.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;
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
public record Settings(boolean debug, Saving saving, Teleport teleport, Dogs dogs, AttackMode attackMode, Breeds breeds,
                       Levels levels) {

    /**
     * What dogs in attack mode go after.
     *
     * @param onlyListed       only the mobs in {@code targets}, instead of every hostile mob
     * @param defendOwnerFirst mobs attacking the owner come first, whether listed or not
     */
    public record AttackMode(boolean onlyListed, Set<EntityType> targets, Set<EntityType> ignore,
                             boolean defendOwnerFirst, boolean ignoreMythicMobs) {}

    /**
     * Free breed/personality choices for dogs from before those existed, and dog treats.
     *
     * @param breedCutoff       dogs born before this that are still Pale may pick a breed for free
     * @param personalityCutoff dogs born before this that still sound Classic may pick a personality for free
     */
    public record Breeds(boolean freeChoice, Instant breedCutoff, Instant personalityCutoff, boolean treats,
                         List<TreatRecipe> recipes) {

        public Optional<TreatRecipe> recipe(TreatKind kind, NamespacedKey variant) {
            return recipes.stream().filter(r -> r.kind() == kind && r.variant().equals(variant)).findFirst();
        }
    }

    /** A configured dog treat. {@code shape} and {@code ingredients} are only used when it is craftable. */
    public record TreatRecipe(TreatKind kind, NamespacedKey variant, boolean craftable, List<String> shape,
                              Map<Character, Material> ingredients) {}

    public record Saving(boolean instant, int autosaveMinutes) {}

    public record Teleport(boolean enabled, boolean onWorldChange, boolean allTameables,
                           boolean expandedSearch, boolean distanceCheck, double maxDistance, Comehere comehere) {}

    /** /mydog comehere: whether it may load the chunk a dog is in, and how often (per player). */
    public record Comehere(boolean loadChunks, int cooldownSeconds) {}

    public record Dogs(boolean randomCollarColor, List<String> names, boolean nametagOnlyOnHover,
                       boolean nametagShowLevel, boolean allowNametagRename, boolean arrowDamage, boolean crammingDamage,
                       boolean protectFromOtherPlayers, boolean invulnerable, int calmSeconds, double lifesteal,
                       LowHealthWarning lowHealthWarning, Revival revival, AutoFeed autoFeed, Armor armor) {}

    /** Wolf armor on dogs: never wearing out, and warning the owner below {@code warnBelow} durability left. */
    public record Armor(boolean unbreakable, int warnBelow) {}

    public record LowHealthWarning(boolean enabled, double threshold, long cooldownMillis) {}

    /** Hurt dogs eat wolf food from their owner's inventory when their health is below {@code below} (a share). */
    public record AutoFeed(boolean enabled, double below, double range) {}

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
                config.getBoolean("teleport.expanded-search"),
                config.getBoolean("teleport.distance-check.enabled"),
                Math.max(1.0, config.getDouble("teleport.distance-check.max-distance")),
                new Comehere(
                        config.getBoolean("teleport.comehere.load-chunks"),
                        atLeast(log, config, "teleport.comehere.cooldown-seconds", 0)));

        Dogs dogs = new Dogs(
                config.getBoolean("dogs.random-collar-color"),
                config.getStringList("dogs.names").stream().filter(name -> !name.isBlank()).toList(),
                config.getBoolean("dogs.nametag.only-on-hover"),
                config.getBoolean("dogs.nametag.show-level"),
                config.getBoolean("dogs.allow-nametag-rename"),
                config.getBoolean("dogs.arrow-damage"),
                config.getBoolean("dogs.cramming-damage"),
                config.getBoolean("dogs.protection.from-other-players"),
                config.getBoolean("dogs.protection.invulnerable"),
                atLeast(log, config, "dogs.calm-seconds", 0),
                share(log, config, "dogs.lifesteal"),
                new LowHealthWarning(
                        config.getBoolean("dogs.low-health-warning.enabled"),
                        share(log, config, "dogs.low-health-warning.threshold"),
                        Math.max(0L, config.getLong("dogs.low-health-warning.cooldown-millis"))),
                new Revival(
                        config.getBoolean("dogs.revival.enabled"),
                        atLeast(log, config, "dogs.revival.price-per-level", 0),
                        config.getBoolean("dogs.revival.use-player-levels")),
                new AutoFeed(
                        config.getBoolean("dogs.auto-feed.enabled"),
                        share(log, config, "dogs.auto-feed.below"),
                        Math.max(1.0, config.getDouble("dogs.auto-feed.range"))),
                new Armor(
                        config.getBoolean("dogs.armor.unbreakable"),
                        atLeast(log, config, "dogs.armor.warn-below", 0)));

        Levels levels = new Levels(
                config.getBoolean("levels.enabled"),
                sound(log, config.getString("levels.level-up-sound", "")),
                config.getBoolean("levels.player-kill-experience"),
                "scaled".equalsIgnoreCase(config.getString("levels.mode", "table")) ? scaledTable(log, config) : levelTable(log, config),
                experiencePerKill(log, section(config, "levels.experience-per-kill")),
                Math.max(0, config.getInt("levels.experience-per-kill.default")));

        AttackMode attackMode = new AttackMode(
                "list".equalsIgnoreCase(config.getString("attack-mode.target", "monsters")),
                entityTypes(log, config, "attack-mode.targets"),
                entityTypes(log, config, "attack-mode.ignore"),
                config.getBoolean("attack-mode.defend-owner-first"),
                config.getBoolean("attack-mode.ignore-mythic-mobs"));

        return new Settings(config.getBoolean("debug"), saving, teleport, dogs, attackMode, breeds(log, config), levels);
    }

    /**
     * A section of the config, or of the defaults when the config doesn't have it. Bukkit's own
     * getConfigurationSection hands out a new, empty section in that case, whose sub-sections have no keys.
     */
    private static ConfigurationSection section(ConfigurationSection config, String path) {
        if (config.isSet(path)) {
            return config.getConfigurationSection(path);
        }
        ConfigurationSection defaults = config.getDefaultSection();
        return defaults != null ? defaults.getConfigurationSection(path) : null;
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
        ConfigurationSection section = section(config, "levels.table");
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
                        levelSection.getDouble("damage"),
                        levelSection.isSet("player-damage") ? levelSection.getDouble("player-damage") : null,
                        effects(log, "levels.table." + key + ".effects", levelSection.getStringList("effects"))));
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

    private static LevelTable scaledTable(Logger log, ConfigurationSection config) {
        String path = "levels.scaled.";
        int maxLevel = atLeast(log, config, path + "max-level", 1);
        int maxExperience = atLeast(log, config, path + "max-experience", 0);
        double curve = config.getDouble(path + "experience-curve");
        if (curve <= 0.0) {
            log.warning(path + "experience-curve must be above 0, using 1.");
            curve = 1.0;
        }
        return LevelTable.scaled(maxLevel, maxExperience, curve,
                config.getDouble(path + "health.min"), config.getDouble(path + "health.max"),
                config.getDouble(path + "damage.min"), config.getDouble(path + "damage.max"));
    }

    /** Effects written as "speed:2" (Speed II), or just "speed" for level I. */
    private static Map<NamespacedKey, Integer> effects(Logger log, String path, List<String> entries) {
        Map<NamespacedKey, Integer> effects = new HashMap<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            // Also accept a namespaced key: minecraft:speed:2
            String name = parts.length == 3 ? parts[0] + ":" + parts[1] : parts[0];
            String level = parts.length == 1 ? "1" : parts[parts.length - 1];
            NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
            int amplifier;
            try {
                amplifier = Integer.parseInt(level);
            } catch (NumberFormatException e) {
                amplifier = -1;
            }
            if (key == null || amplifier < 0 || Registry.EFFECT.get(key) == null) {
                log.warning("Skipping '" + entry + "' in " + path + ": write effects like speed:2 (Speed II).");
                continue;
            }
            effects.put(key, amplifier);
        }
        return effects;
    }

    private static Set<EntityType> entityTypes(Logger log, ConfigurationSection config, String path) {
        Set<EntityType> types = EnumSet.noneOf(EntityType.class);
        for (String name : config.getStringList(path)) {
            EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (type == null) {
                log.warning("Unknown entity type '" + name + "' in " + path + ", skipping it.");
            } else {
                types.add(type);
            }
        }
        return types;
    }

    private static Breeds breeds(Logger log, ConfigurationSection config) {
        List<TreatRecipe> recipes = new ArrayList<>();
        for (TreatKind kind : TreatKind.values()) {
            String path = "breeds.treats." + kind.configSection();
            ConfigurationSection section = section(config, path);
            if (section == null) {
                continue;
            }
            for (String key : section.getKeys(false)) {
                NamespacedKey variant = TreatKind.parseKey(key);
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (variant == null || entry == null || !kind.exists(variant)) {
                    log.warning("Skipping " + path + "." + key + ": not a " + kind.id() + " this server knows.");
                    continue;
                }
                TreatRecipe recipe = recipe(log, path + "." + key, kind, variant, entry);
                recipes.add(recipe);
            }
        }
        return new Breeds(
                config.getBoolean("breeds.free-choice.enabled"),
                date(log, config, "breeds.free-choice.breed-cutoff"),
                date(log, config, "breeds.free-choice.personality-cutoff"),
                config.getBoolean("breeds.treats.enabled"),
                List.copyOf(recipes));
    }

    /** A treat recipe. A craftable recipe that doesn't add up is logged and turned into a non-craftable treat. */
    private static TreatRecipe recipe(Logger log, String path, TreatKind kind, NamespacedKey variant, ConfigurationSection entry) {
        boolean craftable = entry.getBoolean("craftable", true);
        List<String> shape = entry.getStringList("shape");
        Map<Character, Material> ingredients = new LinkedHashMap<>();
        ConfigurationSection ingredientSection = entry.getConfigurationSection("ingredients");
        if (ingredientSection != null) {
            for (String letter : ingredientSection.getKeys(false)) {
                Material material = Material.matchMaterial(ingredientSection.getString(letter, ""));
                if (letter.length() != 1 || material == null || !material.isItem()) {
                    log.warning(path + ": ingredient " + letter + " should be one letter and an item, e.g. D: diamond.");
                    craftable = false;
                    continue;
                }
                ingredients.put(letter.charAt(0), material);
            }
        }
        if (craftable) {
            String problem = shapeProblem(shape, ingredients.keySet());
            if (problem != null) {
                log.warning(path + ": " + problem + " The treat can't be crafted until this is fixed.");
                craftable = false;
            }
        }
        return new TreatRecipe(kind, variant, craftable, List.copyOf(shape), Map.copyOf(ingredients));
    }

    /** Why a shape can't be a crafting recipe, or null if it can. */
    static String shapeProblem(List<String> shape, Set<Character> ingredients) {
        if (shape.isEmpty() || shape.size() > 3) {
            return "the shape needs 1 to 3 rows.";
        }
        int width = shape.getFirst().length();
        boolean hasIngredient = false;
        for (String row : shape) {
            if (row.isEmpty() || row.length() > 3 || row.length() != width) {
                return "every row of the shape needs the same length, 1 to 3 letters.";
            }
            for (char c : row.toCharArray()) {
                if (c == ' ') {
                    continue;
                }
                if (!ingredients.contains(c)) {
                    return "the shape uses " + c + ", which isn't one of the ingredients.";
                }
                hasIngredient = true;
            }
        }
        return hasIngredient ? null : "the shape has no ingredients.";
    }

    private static Instant date(Logger log, ConfigurationSection config, String path) {
        String value = config.getString(path, "");
        try {
            return LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            log.warning(path + " must be a date like 2024-04-23, got '" + value + "'. No dogs get a free choice from it.");
            return Instant.EPOCH;
        }
    }
}
