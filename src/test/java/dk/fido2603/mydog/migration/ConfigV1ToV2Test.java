package dk.fido2603.mydog.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dk.fido2603.mydog.config.DefaultDogNames;

class ConfigV1ToV2Test {
    @TempDir
    Path dataFolder;

    /** A config.yml the way MyDog 0.2.x wrote it: every setting, with the built-in names and default messages. */
    private static YamlConfiguration oldConfig() {
        YamlConfiguration old = new YamlConfiguration();
        old.set("Settings.ServerName", "Your Server");
        old.set("Settings.Debug", true);
        old.set("Settings.ChatPrefix", "MyDog");
        old.set("Settings.InstantSaveConfig", false);
        old.set("Settings.AutomaticTeleportation", false);
        old.set("Settings.ExpandedSearch", true);
        old.set("Settings.EnableExperimentalTeleport", false);
        old.set("Settings.PlayerDistanceCheck", true);
        old.set("DogSettings.RandomCollarColor", true);
        old.set("DogSettings.UseLevels", true);
        old.set("DogSettings.TeleportOnWorldChange", false);
        old.set("Settings.TeleportAllTameables", false);
        old.set("DogSettings.OnlyShowNametagOnHover", false);
        old.set("DogSettings.ShowLevelsInNametag", true);
        old.set("DogSettings.AllowPlayerKillExp", false);
        old.set("DogSettings.AllowNametagRename", true);
        old.set("DogSettings.DogNames", DefaultDogNames.NAMES);
        old.set("DogSettings.AllowRevival", true);
        old.set("DogSettings.RevivalPricePerLevel", 150);
        old.set("DogSettings.RevivalUsingPlayerExp", true);
        old.set("DogSettings.AllowArrowDamage", false);
        old.set("DogSettings.Lifesteal", 0.1);
        old.set("DogSettings.EnableLowHealthMessage", true);
        old.set("DogSettings.LowHealthThreshold", 0.25);
        old.set("DogSettings.LowHealthMessageCooldown", 1000);
        old.set("DogSettings.Levels.1.Experience", 0);
        old.set("DogSettings.Levels.1.Health", 20.0);
        old.set("DogSettings.Levels.1.Damage", 4.0);
        old.set("DogSettings.Levels.2.Experience", 50);
        old.set("DogSettings.Levels.2.Health", 30.0);
        old.set("DogSettings.Levels.2.Damage", 6.0);
        old.set("PlayerInteraction.LevelUpSound", "ENTITY_WOLF_HOWL");
        // Changed by the admin
        old.set("PlayerInteraction.LevelUpString", "&a{dogNameColor}{dogName} &ais now level {level}!");
        // Left at their defaults
        old.set("PlayerInteraction.NewDogString", "&6&l[{chatPrefix}] &r&6Congratulations with your new dog, {dogNameColor}{dogName}&6!");
        old.set("PlayerInteraction.DeadDogLevelString", ", and got to &4Level {level}&c");
        return old;
    }

    private YamlConfiguration migrate() throws Migration.MigrationException {
        return new ConfigV1ToV2(dataFolder.toFile(), name -> getClass().getClassLoader().getResourceAsStream(name),
                Logger.getAnonymousLogger()).migrate(oldConfig());
    }

    @Test
    void movesSettingsToTheirNewKeys() throws Exception {
        YamlConfiguration config = migrate();
        assertTrue(config.getBoolean("debug"));
        assertFalse(config.getBoolean("teleport.enabled"));
        assertTrue(config.getBoolean("teleport.expanded-search"));
        assertFalse(config.getBoolean("teleport.on-world-change"));
        assertFalse(config.getBoolean("levels.player-kill-experience"));
        assertEquals(150, config.getInt("dogs.revival.price-per-level"));
        assertTrue(config.getBoolean("dogs.revival.use-player-levels"));
        assertEquals(0.1, config.getDouble("dogs.lifesteal"));
    }

    @Test
    void replacesTheLevelTable() throws Exception {
        YamlConfiguration config = migrate();
        assertEquals(List.of("1", "2"), List.copyOf(config.getConfigurationSection("levels.table").getKeys(false)));
        assertEquals(50, config.getInt("levels.table.2.experience"));
        assertEquals(30.0, config.getDouble("levels.table.2.health"));
    }

    @Test
    void dropsTheBuiltInNameListAndTheRemovedHowl() throws Exception {
        YamlConfiguration config = migrate();
        assertTrue(config.getStringList("dogs.names").isEmpty());
        assertEquals("entity.wolf.ambient", config.getString("levels.level-up-sound"));
    }

    @Test
    void keepsTheBundledComments() throws Exception {
        assertFalse(migrate().getComments("teleport.enabled").isEmpty());
    }

    @Test
    void movesOnlyChangedMessagesToMessagesYml() throws Exception {
        migrate();
        YamlConfiguration messages = YamlConfiguration.loadConfiguration(new File(dataFolder.toFile(), "messages.yml"));
        assertEquals("<green><dog_name> <green>is now level <level>!", messages.getString("dog.level-up"));
        // Unchanged messages get the new defaults
        assertEquals("<gold><bold>[<prefix>]</bold> Congratulations with your new dog, <dog_name>!",
                messages.getString("tame.new-dog"));
        assertEquals("MyDog", messages.getString("prefix"));
    }
}
