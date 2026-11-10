package dk.fido2603.mydog.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DogsV1ToV2Test {
    private static final String DOG = "6f1f2d1a-2b8e-4f43-9a0e-1c2d3e4f5a6b";
    private static final String OWNER = "0c9b8a7d-6e5f-4a3b-8c2d-1e0f9a8b7c6d";

    private static YamlConfiguration migrate(String oldYaml) throws InvalidConfigurationException {
        YamlConfiguration old = new YamlConfiguration();
        old.loadFromString(oldYaml);
        return new DogsV1ToV2(Logger.getAnonymousLogger()).migrate(old);
    }

    @Test
    void convertsAFullDog() throws InvalidConfigurationException {
        YamlConfiguration data = migrate("""
                %s:
                  Owner: %s
                  ID: 3
                  Name: Fido
                  NameChatColor: DARK_RED
                  LastSeen:
                    World: world
                    X: 10.5
                    Y: 64.0
                    Z: -3.25
                  Level:
                    Level: 4
                    Experience: 250
                  Birthday: 24-12-2023 18:30
                  Angry: true
                  Dead: false
                """.formatted(DOG, OWNER));

        String dog = "dogs." + DOG + ".";
        assertEquals(OWNER, data.getString(dog + "owner"));
        assertEquals(3, data.getInt(dog + "id"));
        assertEquals("Fido", data.getString(dog + "name"));
        assertEquals("dark_red", data.getString(dog + "color"));
        assertEquals(4, data.getInt(dog + "level"));
        assertEquals(250, data.getInt(dog + "experience"));
        assertTrue(data.getBoolean(dog + "attack-mode"));
        assertFalse(data.getBoolean(dog + "dead"));
        assertEquals("world", data.getString(dog + "last-seen.world"));
        assertEquals(-3.25, data.getDouble(dog + "last-seen.z"));
        assertEquals(LocalDateTime.of(2023, 12, 24, 18, 30).atZone(ZoneId.systemDefault()).toInstant().toString(),
                data.getString(dog + "born"));
    }

    @Test
    void readsTheOldIsDeadKey() throws InvalidConfigurationException {
        YamlConfiguration data = migrate("""
                %s:
                  Owner: %s
                  isDead: true
                """.formatted(DOG, OWNER));
        assertTrue(data.getBoolean("dogs." + DOG + ".dead"));
    }

    @Test
    void skipsDogsWithoutAnOwner() throws InvalidConfigurationException {
        YamlConfiguration data = migrate("""
                %s:
                  Name: Ghost
                """.formatted(DOG));
        assertFalse(data.contains("dogs." + DOG));
    }
}
