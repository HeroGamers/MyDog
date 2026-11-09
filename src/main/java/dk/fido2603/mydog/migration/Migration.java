package dk.fido2603.mydog.migration;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades a YAML file from one version to the next. Migrations are chained by {@link VersionedYaml}, so each one only
 * has to know about the version right before it.
 */
public interface Migration {
    /** The version this migration upgrades from. It produces {@code fromVersion() + 1}. */
    int fromVersion();

    /** Returns the upgraded file. It may change {@code old} in place and return it, or build a new one. */
    YamlConfiguration migrate(YamlConfiguration old) throws MigrationException;

    final class MigrationException extends Exception {
        public MigrationException(String message) {
            super(message);
        }
    }
}
