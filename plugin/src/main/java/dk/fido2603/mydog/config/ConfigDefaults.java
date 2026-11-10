package dk.fido2603.mydog.config;

import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;

/** Adds settings that a server's config.yml is missing (because they are newer than it) from the bundled defaults. */
public final class ConfigDefaults {
    /**
     * Lists the admin may shorten. Once a server's config has one of these sections, entries missing from it are left
     * out on purpose and not filled in; a section that is missing entirely (a new feature) is added whole.
     */
    public static final List<String> FREE_FORM_SECTIONS = List.of("levels.table", "levels.experience-per-kill",
            "breeds.treats.breeds", "breeds.treats.personalities");

    private ConfigDefaults() {
    }

    /** Copies missing settings (with their comments) from {@code defaults} into {@code config}. Returns whether any were. */
    public static boolean addMissing(YamlConfiguration config, YamlConfiguration defaults) {
        // Free-form sections this server's config already has, before anything is added
        List<String> ownedSections = FREE_FORM_SECTIONS.stream().filter(config::isSet).toList();
        boolean added = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key) || config.isSet(key)
                    || ownedSections.stream().anyMatch(section -> key.equals(section) || key.startsWith(section + "."))) {
                continue;
            }
            config.set(key, defaults.get(key));
            config.setComments(key, defaults.getComments(key));
            added = true;
        }
        return added;
    }
}
