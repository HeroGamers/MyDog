package dk.fido2603.mydog.storage;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import dk.fido2603.mydog.config.TreatKind;
import dk.fido2603.mydog.migration.DogsV1ToV2;
import dk.fido2603.mydog.migration.VersionedYaml;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.util.AtomicFiles;

/**
 * Keeps dogs in dogs.yml. The file is read once on startup; after that the in-memory copy is the truth and is written
 * back on a background thread, either right away or by the autosave task (see {@code saving.instant}).
 */
public final class YamlDogRepository implements DogRepository {
    public static final int DATA_VERSION = 2;

    private final File file;
    private final Logger log;
    private final BooleanSupplier instantSave;
    private final Map<UUID, Dog> dogs = new HashMap<>();
    /** One writer thread, so saves land on disk in the order they were taken. */
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "MyDog dogs.yml writer");
        thread.setDaemon(true);
        return thread;
    });
    private boolean dirty;

    public YamlDogRepository(File file, Logger log, BooleanSupplier instantSave) {
        this.file = file;
        this.log = log;
        this.instantSave = instantSave;
    }

    public void load() throws VersionedYaml.LoadException {
        VersionedYaml format = new VersionedYaml("data-version", DATA_VERSION, 1, List.of(new DogsV1ToV2(log)));
        YamlConfiguration data = format.load(file, log);
        dogs.clear();
        if (data == null) {
            log.info("No dogs.yml yet, starting with no dogs.");
            return;
        }

        ConfigurationSection section = data.getConfigurationSection("dogs");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Dog dog = read(key, section.getConfigurationSection(key));
                if (dog != null) {
                    dogs.put(dog.getDogId(), dog);
                }
            }
        }
        log.info("Loaded " + dogs.size() + " dogs.");
    }

    private Dog read(String key, ConfigurationSection section) {
        try {
            UUID dogId = UUID.fromString(key);
            UUID ownerId = UUID.fromString(section.getString("owner", ""));

            NamedTextColor color = NamedTextColor.NAMES.valueOr(section.getString("color", "white").toLowerCase(Locale.ROOT), NamedTextColor.WHITE);
            Instant born;
            try {
                born = Instant.parse(section.getString("born", ""));
            } catch (DateTimeParseException e) {
                born = Instant.now();
            }
            Dog.LastSeen lastSeen = section.isString("last-seen.world")
                    ? new Dog.LastSeen(section.getString("last-seen.world"), section.getDouble("last-seen.x"),
                            section.getDouble("last-seen.y"), section.getDouble("last-seen.z"))
                    : null;

            Dog dog = new Dog(dogId, ownerId, section.getInt("id"), section.getString("name", ""), color,
                    Math.max(1, section.getInt("level", 1)), section.getInt("experience"), born,
                    section.getBoolean("attack-mode"), section.getBoolean("dead"), lastSeen);
            if (section.isConfigurationSection("appearance")) {
                DyeColor collar;
                try {
                    collar = DyeColor.valueOf(section.getString("appearance.collar", "").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    collar = null;
                }
                dog.setAppearance(new Dog.Appearance(collar, section.getString("appearance.variant"),
                        section.getString("appearance.sound-variant")));
            }
            if (section.isConfigurationSection("stored")) {
                dog.setStored(new Dog.Stored(section.getDouble("stored.health"), section.getItemStack("stored.armor")));
            }
            for (String used : section.getStringList("free-choice-used")) {
                TreatKind kind = TreatKind.byId(used);
                if (kind != null) {
                    dog.setFreeChoiceUsed(kind);
                }
            }
            return dog;
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warning("Skipping dog " + key + " in dogs.yml: " + e.getMessage());
            return null;
        }
    }

    private static void write(ConfigurationSection section, Dog dog) {
        section.set("owner", dog.getOwnerId().toString());
        section.set("id", dog.getIdentifier());
        section.set("name", dog.getDogName());
        section.set("color", dog.getColor().toString());
        section.set("level", dog.getLevel());
        section.set("experience", dog.getExperience());
        section.set("born", dog.getBirthday().toString());
        section.set("attack-mode", dog.isAngry());
        section.set("dead", dog.isDead());
        Dog.LastSeen lastSeen = dog.getLastSeen();
        if (lastSeen != null && lastSeen.world() != null) {
            section.set("last-seen.world", lastSeen.world());
            section.set("last-seen.x", lastSeen.x());
            section.set("last-seen.y", lastSeen.y());
            section.set("last-seen.z", lastSeen.z());
        }
        Dog.Appearance appearance = dog.getAppearance();
        if (appearance != null) {
            section.set("appearance.collar", appearance.collar() != null ? appearance.collar().name().toLowerCase(Locale.ROOT) : null);
            section.set("appearance.variant", appearance.variant());
            section.set("appearance.sound-variant", appearance.soundVariant());
        }
        Dog.Stored stored = dog.getStored();
        if (stored != null) {
            section.set("stored.health", stored.health());
            section.set("stored.armor", stored.armor());
        }
        List<String> freeChoicesUsed = new ArrayList<>();
        for (TreatKind kind : TreatKind.values()) {
            if (dog.hasUsedFreeChoice(kind)) {
                freeChoicesUsed.add(kind.id());
            }
        }
        if (!freeChoicesUsed.isEmpty()) {
            section.set("free-choice-used", freeChoicesUsed);
        }
    }

    @Override
    public Optional<Dog> find(UUID dogId) {
        return Optional.ofNullable(dogs.get(dogId));
    }

    @Override
    public List<Dog> findByOwner(UUID ownerId) {
        List<Dog> owned = new ArrayList<>();
        for (Dog dog : dogs.values()) {
            if (dog.getOwnerId().equals(ownerId)) {
                owned.add(dog);
            }
        }
        owned.sort(Comparator.comparingInt(Dog::getIdentifier));
        return owned;
    }

    @Override
    public Optional<Dog> findByOwnerAndIdentifier(UUID ownerId, int identifier) {
        for (Dog dog : dogs.values()) {
            if (dog.getIdentifier() == identifier && dog.getOwnerId().equals(ownerId)) {
                return Optional.of(dog);
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<Dog> all() {
        return Collections.unmodifiableCollection(dogs.values());
    }

    @Override
    public void markChanged(Dog dog) {
        dogs.put(dog.getDogId(), dog);
        changed();
    }

    @Override
    public void delete(UUID dogId) {
        if (dogs.remove(dogId) != null) {
            changed();
        }
    }

    @Override
    public void changeDogId(Dog dog, UUID newDogId) {
        dogs.remove(dog.getDogId());
        dog.setDogId(newDogId);
        dogs.put(newDogId, dog);
        changed();
    }

    private void changed() {
        dirty = true;
        if (instantSave.getAsBoolean()) {
            saveAsync();
        }
    }

    @Override
    public void saveAsync() {
        if (!dirty) {
            return;
        }
        String snapshot = snapshot();
        writer.execute(() -> writeFile(snapshot));
    }

    @Override
    public void saveNow() {
        if (dirty) {
            String snapshot = snapshot();
            writer.execute(() -> writeFile(snapshot));
        }
        writer.shutdown();
        try {
            if (!writer.awaitTermination(30, TimeUnit.SECONDS)) {
                log.severe("Timed out waiting for dogs.yml to be written!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Taken on the server thread so the background write never sees a dog half-way through a change. */
    private String snapshot() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("data-version", DATA_VERSION);
        ConfigurationSection section = data.createSection("dogs");
        for (Dog dog : dogs.values()) {
            write(section.createSection(dog.getDogId().toString()), dog);
        }
        dirty = false;
        return data.saveToString();
    }

    private void writeFile(String content) {
        try {
            AtomicFiles.write(file.toPath(), content);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not save dogs.yml", e);
        }
    }
}
