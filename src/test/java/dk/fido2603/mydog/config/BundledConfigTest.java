package dk.fido2603.mydog.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class BundledConfigTest {
    private final YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(
            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml")), StandardCharsets.UTF_8));

    /** A typo in the experience table would only show up as a warning on a live server. */
    @Test
    void everyExperienceEntryIsAnEntityType() {
        ConfigurationSection table = Objects.requireNonNull(config.getConfigurationSection("levels.experience-per-kill"));
        List<String> unknown = new ArrayList<>();
        for (String key : table.getKeys(false)) {
            if (key.equals("default")) {
                continue;
            }
            try {
                EntityType.valueOf(key.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                unknown.add(key);
            }
        }
        assertTrue(unknown.isEmpty(), "Not entity types: " + unknown);
    }
}
