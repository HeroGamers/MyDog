package dk.fido2603.mydog.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.DyeColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dk.fido2603.mydog.objects.Dog;

class YamlDogRepositoryTest {
    @TempDir
    Path dir;

    private YamlDogRepository repository(File file) throws Exception {
        YamlDogRepository repository = new YamlDogRepository(file, Logger.getAnonymousLogger(), () -> false);
        repository.load();
        return repository;
    }

    @Test
    void savedDogsLoadBackTheSame() throws Exception {
        File file = dir.resolve("dogs.yml").toFile();
        UUID dogId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant born = Instant.parse("2024-05-01T12:00:00Z");

        Dog dog = new Dog(dogId, ownerId, 2, "Fido", NamedTextColor.DARK_RED, 4, 250, born, true, false,
                new Dog.LastSeen("world", 1.5, 64, -3));
        dog.setAppearance(new Dog.Appearance(DyeColor.LIGHT_BLUE, "minecraft:snowy", "minecraft:cute"));
        Dog noAppearance = new Dog(UUID.randomUUID(), ownerId, 1, "Rex", NamedTextColor.WHITE, 1, 0, born, false, true, null);

        YamlDogRepository first = repository(file);
        first.markChanged(dog);
        first.markChanged(noAppearance);
        first.saveNow();

        YamlDogRepository second = repository(file);
        Dog loaded = second.find(dogId).orElseThrow();
        assertEquals(ownerId, loaded.getOwnerId());
        assertEquals(2, loaded.getIdentifier());
        assertEquals("Fido", loaded.getDogName());
        assertEquals(NamedTextColor.DARK_RED, loaded.getColor());
        assertEquals(4, loaded.getLevel());
        assertEquals(250, loaded.getExperience());
        assertEquals(born, loaded.getBirthday());
        assertTrue(loaded.isAngry());
        assertEquals(new Dog.LastSeen("world", 1.5, 64, -3), loaded.getLastSeen());
        assertEquals(new Dog.Appearance(DyeColor.LIGHT_BLUE, "minecraft:snowy", "minecraft:cute"), loaded.getAppearance());

        Dog loadedRex = second.findByOwnerAndIdentifier(ownerId, 1).orElseThrow();
        assertTrue(loadedRex.isDead());
        assertNull(loadedRex.getAppearance());
        assertNull(loadedRex.getLastSeen());
    }
}
