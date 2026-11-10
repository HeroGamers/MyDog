package dk.fido2603.mydog.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;
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

    @Test
    void attackModeListsAreEntityTypes() {
        List<String> unknown = new ArrayList<>();
        for (String path : List.of("attack-mode.targets", "attack-mode.ignore")) {
            for (String name : config.getStringList(path)) {
                try {
                    EntityType.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    unknown.add(path + ": " + name);
                }
            }
        }
        assertTrue(unknown.isEmpty(), "Not entity types: " + unknown);
    }

    /** Every bundled treat recipe must be craftable as written. */
    @Test
    void everyTreatRecipeIsValid() {
        List<String> problems = new ArrayList<>();
        for (String path : List.of("breeds.treats.breeds", "breeds.treats.personalities", "boosts.treats")) {
            ConfigurationSection section = Objects.requireNonNull(config.getConfigurationSection(path));
            for (String key : section.getKeys(false)) {
                Set<Character> ingredients = new HashSet<>();
                ConfigurationSection ingredientSection = Objects.requireNonNull(section.getConfigurationSection(key + ".ingredients"));
                for (String letter : ingredientSection.getKeys(false)) {
                    String material = ingredientSection.getString(letter, "");
                    if (!isMaterial(material)) {
                        problems.add(key + ": " + material + " is not a material");
                    }
                    ingredients.add(letter.charAt(0));
                }
                String problem = Settings.shapeProblem(section.getStringList(key + ".shape"), ingredients);
                if (problem != null) {
                    problems.add(key + ": " + problem);
                }
            }
        }
        assertTrue(problems.isEmpty(), "Bad treat recipes: " + problems);
    }

    /**
     * Whether Material has a constant with this name. Looking the field up doesn't initialize Material, which (unlike
     * calling Material.matchMaterial) would need a running server.
     */
    private static boolean isMaterial(String name) {
        try {
            Material.class.getField(name.toUpperCase(Locale.ROOT));
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /** One treat per breed and personality Minecraft 26.3 has. */
    @Test
    void hasATreatForEveryBreedAndPersonality() {
        assertEquals(List.of("ashen", "black", "chestnut", "pale", "rusty", "snowy", "spotted", "striped", "woods"),
                config.getConfigurationSection("breeds.treats.breeds").getKeys(false).stream().sorted().toList());
        assertEquals(List.of("angry", "big", "classic", "cute", "grumpy", "puglin", "sad"),
                config.getConfigurationSection("breeds.treats.personalities").getKeys(false).stream().sorted().toList());
    }
}
