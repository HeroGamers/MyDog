package dk.fido2603.mydog.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VersionedYamlTest {
    private static final Logger LOG = Logger.getAnonymousLogger();

    @TempDir
    Path dir;

    /** Renames "a" to "b" (v1 to v2) and "b" to "c" (v2 to v3). */
    private static final List<Migration> MIGRATIONS = List.of(rename(1, "a", "b"), rename(2, "b", "c"));

    private static Migration rename(int from, String oldKey, String newKey) {
        return new Migration() {
            @Override
            public int fromVersion() {
                return from;
            }

            @Override
            public YamlConfiguration migrate(YamlConfiguration old) {
                old.set(newKey, old.get(oldKey));
                old.set(oldKey, null);
                return old;
            }
        };
    }

    private File write(String content) throws IOException {
        Path file = dir.resolve("test.yml");
        Files.writeString(file, content);
        return file.toFile();
    }

    @Test
    void runsMigrationsInOrderAndBacksUpTheOriginal() throws Exception {
        File file = write("a: 42\n");
        YamlConfiguration yaml = new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG);

        assertEquals(42, yaml.getInt("c"));
        assertEquals(3, yaml.getInt("version"));
        // Saved to disk, with the original kept next to it
        assertEquals(42, YamlConfiguration.loadConfiguration(file).getInt("c"));
        assertEquals("a: 42\n", Files.readString(dir.resolve("test.yml.v1.bak")));
    }

    @Test
    void startsFromTheVersionInTheFile() throws Exception {
        File file = write("version: 2\nb: 7\n");
        YamlConfiguration yaml = new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG);
        assertEquals(7, yaml.getInt("c"));
    }

    @Test
    void leavesCurrentAndNewerFilesAlone() throws Exception {
        File file = write("version: 4\nz: 1\n");
        YamlConfiguration yaml = new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG);
        assertEquals(1, yaml.getInt("z"));
        assertTrue(Files.notExists(dir.resolve("test.yml.v4.bak")));
    }

    @Test
    void missingFileGivesNull() throws Exception {
        assertNull(new VersionedYaml("version", 3, 1, MIGRATIONS).load(dir.resolve("nope.yml").toFile(), LOG));
    }

    @Test
    void failsWhenAMigrationIsMissing() throws Exception {
        File file = write("a: 1\n");
        VersionedYaml format = new VersionedYaml("version", 3, 1, List.of(rename(2, "b", "c")));
        assertThrows(VersionedYaml.LoadException.class, () -> format.load(file, LOG));
    }
}
