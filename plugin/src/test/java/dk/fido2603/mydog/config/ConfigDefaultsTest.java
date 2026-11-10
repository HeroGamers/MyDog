package dk.fido2603.mydog.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfigDefaultsTest {
    private final YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(
            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml")), StandardCharsets.UTF_8));

    private static YamlConfiguration yaml(String content) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(content);
        return yaml;
    }

    @Test
    void aMissingFeatureIsAddedWhole() throws Exception {
        // A config from before dog treats: it doesn't have breeds at all
        YamlConfiguration config = yaml("config-version: 2\ndebug: true\n");
        assertTrue(ConfigDefaults.addMissing(config, defaults));

        assertTrue(config.getBoolean("debug"), "existing settings are kept");
        assertTrue(config.getBoolean("breeds.treats.enabled"));
        // The recipe has to come along complete, ingredients included
        assertEquals("diamond", config.getString("breeds.treats.breeds.pale.ingredients.D"));
        assertEquals("gold_block", config.getString("breeds.treats.personalities.big.ingredients.G"));
    }

    @Test
    void entriesTheAdminRemovedStayRemoved() throws Exception {
        // The admin has breed treats, but removed snowy to have no snowy treat
        YamlConfiguration config = yaml("""
                breeds:
                  treats:
                    breeds:
                      pale: {craftable: false}
                """);
        ConfigDefaults.addMissing(config, defaults);

        assertFalse(config.isSet("breeds.treats.breeds.snowy"));
        assertFalse(config.isSet("breeds.treats.breeds.pale.shape"), "the admin's entry isn't filled in either");
        // Settings outside the free-form section are still added, and so is a missing free-form section
        assertTrue(config.isSet("breeds.treats.enabled"));
        assertTrue(config.isSet("breeds.treats.personalities.big"));
    }

    @Test
    void aCompleteConfigIsLeftAlone() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml")), StandardCharsets.UTF_8));
        assertFalse(ConfigDefaults.addMissing(config, defaults));
    }
}
