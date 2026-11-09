package dk.fido2603.mydog.migration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import dk.fido2603.mydog.config.DefaultDogNames;
import dk.fido2603.mydog.util.AtomicFiles;

/**
 * Upgrades the config.yml written by MyDog 0.2.x and earlier (no version key, CamelCase sections) to version 2.
 * The values are copied into the bundled config.yml so the result keeps its comments. Messages that were changed
 * from the old defaults move to messages.yml, converted to MiniMessage.
 */
public final class ConfigV1ToV2 implements Migration {
    /** Old key to new key, for values copied as-is. */
    private static final Map<String, String> MOVED = Map.ofEntries(
            Map.entry("Settings.Debug", "debug"),
            Map.entry("Settings.InstantSaveConfig", "saving.instant"),
            Map.entry("Settings.AutomaticTeleportation", "teleport.enabled"),
            Map.entry("Settings.TeleportAllTameables", "teleport.all-tameables"),
            Map.entry("Settings.EnableExperimentalTeleport", "teleport.experimental"),
            Map.entry("Settings.PlayerDistanceCheck", "teleport.distance-check.enabled"),
            Map.entry("Settings.ExpandedSearch", "teleport.expanded-search"),
            Map.entry("DogSettings.TeleportOnWorldChange", "teleport.on-world-change"),
            Map.entry("DogSettings.RandomCollarColor", "dogs.random-collar-color"),
            Map.entry("DogSettings.OnlyShowNametagOnHover", "dogs.nametag.only-on-hover"),
            Map.entry("DogSettings.ShowLevelsInNametag", "dogs.nametag.show-level"),
            Map.entry("DogSettings.AllowNametagRename", "dogs.allow-nametag-rename"),
            Map.entry("DogSettings.AllowArrowDamage", "dogs.arrow-damage"),
            Map.entry("DogSettings.Lifesteal", "dogs.lifesteal"),
            Map.entry("DogSettings.EnableLowHealthMessage", "dogs.low-health-warning.enabled"),
            Map.entry("DogSettings.LowHealthThreshold", "dogs.low-health-warning.threshold"),
            Map.entry("DogSettings.LowHealthMessageCooldown", "dogs.low-health-warning.cooldown-millis"),
            Map.entry("DogSettings.AllowRevival", "dogs.revival.enabled"),
            Map.entry("DogSettings.RevivalPricePerLevel", "dogs.revival.price-per-level"),
            Map.entry("DogSettings.RevivalUsingPlayerExp", "dogs.revival.use-player-levels"),
            Map.entry("DogSettings.UseLevels", "levels.enabled"),
            Map.entry("DogSettings.AllowPlayerKillExp", "levels.player-kill-experience"));

    /** Old message key, its default in 0.2.x, and the new key in messages.yml. */
    private record MovedMessage(String oldKey, String oldDefault, String newKey) {}

    private static final List<MovedMessage> MESSAGES = List.of(
            new MovedMessage("PlayerInteraction.LevelUpString",
                    "&5&l[{chatPrefix}] &r&5Your dog, {dogNameColor}{dogName}&5, just leveled up to &dLevel {level}&5!",
                    "dog.level-up"),
            new MovedMessage("PlayerInteraction.CannotTeleportTameableString",
                    "&c&l[{chatPrefix}] &r&cHello! Looks like you just teleported away from your Pet(s)! They can sadly not find a safe place to stay, so they are staying behind for now :( They will be waiting for you where you left them...",
                    "teleport.no-safe-location"),
            new MovedMessage("PlayerInteraction.NewDogString",
                    "&6&l[{chatPrefix}] &r&6Congratulations with your new dog, {dogNameColor}{dogName}&6!",
                    "tame.new-dog"),
            new MovedMessage("PlayerInteraction.DeadDogString",
                    "&c&l[{chatPrefix}] &r&cYour dog, {dogNameColor}{dogName}&c, just passed away... {dogNameColor}{dogName}&c lived for {time}{deadDogLevelString}.",
                    "dog.died"),
            new MovedMessage("PlayerInteraction.DeadDogLevelString",
                    ", and got to &4Level {level}&c",
                    "dog.died-level"),
            new MovedMessage("PlayerInteraction.CommandComehereString",
                    "&6&l[{chatPrefix}] &r&6Come here! Good doggo, {dogNameColor}{dogName}&6!",
                    "command.comehere.done"));

    private final File dataFolder;
    private final Function<String, InputStream> resources;
    private final Logger log;

    /** @param resources opens a file bundled in the plugin jar, like {@code Plugin#getResource} */
    public ConfigV1ToV2(File dataFolder, Function<String, InputStream> resources, Logger log) {
        this.dataFolder = dataFolder;
        this.resources = resources;
        this.log = log;
    }

    @Override
    public int fromVersion() {
        return 1;
    }

    @Override
    public YamlConfiguration migrate(YamlConfiguration old) throws MigrationException {
        YamlConfiguration config = bundled("config.yml");

        for (Map.Entry<String, String> moved : MOVED.entrySet()) {
            if (old.contains(moved.getKey())) {
                config.set(moved.getValue(), old.get(moved.getKey()));
            }
        }

        // The old plugin wrote the whole built-in name list into the config; only keep it when it was changed
        List<String> names = old.getStringList("DogSettings.DogNames");
        if (!names.isEmpty() && !names.equals(DefaultDogNames.NAMES)) {
            config.set("dogs.names", names);
        }

        ConfigurationSection oldLevels = old.getConfigurationSection("DogSettings.Levels");
        if (oldLevels != null && !oldLevels.getKeys(false).isEmpty()) {
            config.set("levels.table", null);
            for (String level : oldLevels.getKeys(false)) {
                config.set("levels.table." + level + ".experience", old.getInt("DogSettings.Levels." + level + ".Experience"));
                config.set("levels.table." + level + ".health", old.getDouble("DogSettings.Levels." + level + ".Health"));
                config.set("levels.table." + level + ".damage", old.getDouble("DogSettings.Levels." + level + ".Damage"));
            }
        }

        String oldSound = old.getString("PlayerInteraction.LevelUpSound");
        if (oldSound != null && !oldSound.equalsIgnoreCase("ENTITY_WOLF_HOWL")) {
            config.set("levels.level-up-sound", soundKey(oldSound));
        }

        migrateMessages(old);
        return config;
    }

    @SuppressWarnings({"deprecation", "removal"}) // Sound.valueOf is the only way to read the old enum-style names
    private String soundKey(String oldName) {
        try {
            return Sound.valueOf(oldName.toUpperCase(Locale.ROOT)).getKey().toString();
        } catch (IllegalArgumentException e) {
            log.warning("The level-up sound " + oldName + " no longer exists, using entity.wolf.ambient instead.");
            return "entity.wolf.ambient";
        }
    }

    private void migrateMessages(YamlConfiguration old) throws MigrationException {
        File messagesFile = new File(dataFolder, "messages.yml");
        YamlConfiguration messages = messagesFile.exists()
                ? YamlConfiguration.loadConfiguration(messagesFile)
                : bundled("messages.yml");

        boolean changed = false;
        String prefix = old.getString("Settings.ChatPrefix");
        if (prefix != null && !prefix.equals("MyDog")) {
            messages.set("prefix", LegacyText.toMiniMessage(prefix));
            changed = true;
        }
        for (MovedMessage message : MESSAGES) {
            String value = old.getString(message.oldKey());
            if (value != null && !value.equals(message.oldDefault())) {
                messages.set(message.newKey(), LegacyText.toMiniMessage(value));
                log.info("Moved your custom " + message.oldKey() + " message to messages.yml as " + message.newKey() + ".");
                changed = true;
            }
        }

        if (changed || !messagesFile.exists()) {
            try {
                AtomicFiles.write(messagesFile.toPath(), messages.saveToString());
            } catch (IOException e) {
                throw new MigrationException("could not write messages.yml: " + e.getMessage());
            }
        }
    }

    private YamlConfiguration bundled(String name) throws MigrationException {
        try (InputStream in = resources.apply(name)) {
            if (in == null) {
                throw new MigrationException(name + " is missing from the plugin jar");
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MigrationException("could not read the bundled " + name + ": " + e.getMessage());
        }
    }
}
