package dk.fido2603.mydog.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import dk.fido2603.mydog.util.AtomicFiles;

/**
 * Loads a YAML file that carries a version number, upgrading it with {@link Migration}s when it is older than the
 * current version. The original file is copied to {@code <name>.v<old version>.bak} before it is upgraded.
 *
 * @param versionKey     the top-level key holding the version
 * @param currentVersion the version this build of the plugin writes
 * @param legacyVersion  the version to assume when an existing file has no version key
 */
public record VersionedYaml(String versionKey, int currentVersion, int legacyVersion, List<Migration> migrations) {

    public static final class LoadException extends Exception {
        public LoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Loads and, if needed, upgrades the file. Returns {@code null} when the file does not exist.
     * A file from a newer version of the plugin is loaded as-is, with a warning.
     */
    public YamlConfiguration load(File file, Logger log) throws LoadException {
        if (!file.exists()) {
            return null;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new LoadException("Could not read " + file.getName() + ": " + e.getMessage(), e);
        }

        int version = yaml.contains(versionKey) ? yaml.getInt(versionKey) : legacyVersion;
        if (version > currentVersion) {
            log.warning(file.getName() + " is version " + version + ", but this version of MyDog only knows up to version "
                    + currentVersion + ". Loading it as-is; consider updating MyDog.");
            return yaml;
        }
        if (version == currentVersion) {
            return yaml;
        }

        Map<Integer, Migration> byVersion = new HashMap<>();
        for (Migration migration : migrations) {
            byVersion.put(migration.fromVersion(), migration);
        }

        Path backup = file.toPath().resolveSibling(file.getName() + ".v" + version + ".bak");
        try {
            Files.copy(file.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new LoadException("Could not back up " + file.getName() + " before upgrading it", e);
        }

        int startVersion = version;
        while (version < currentVersion) {
            Migration migration = byVersion.get(version);
            if (migration == null) {
                throw new LoadException("Don't know how to upgrade " + file.getName() + " from version " + version, null);
            }
            try {
                yaml = migration.migrate(yaml);
            } catch (Migration.MigrationException e) {
                throw new LoadException("Could not upgrade " + file.getName() + " from version " + version + ": " + e.getMessage(), e);
            }
            version++;
            yaml.set(versionKey, version);
        }

        try {
            AtomicFiles.write(file.toPath(), yaml.saveToString());
        } catch (IOException e) {
            throw new LoadException("Could not save the upgraded " + file.getName(), e);
        }
        log.info("Upgraded " + file.getName() + " from version " + startVersion + " to " + currentVersion
                + ". The old file was kept as " + backup.getFileName() + ".");
        return yaml;
    }
}
