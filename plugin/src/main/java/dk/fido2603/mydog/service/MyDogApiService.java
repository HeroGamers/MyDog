package dk.fido2603.mydog.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.api.DogInfo;
import dk.fido2603.mydog.api.MyDogApi;
import dk.fido2603.mydog.api.event.DogLevelChangeEvent;
import dk.fido2603.mydog.config.LevelTable;
import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.config.TreatKind;
import dk.fido2603.mydog.objects.Dog;

/** {@link MyDogApi} on top of {@link DogService}, so changes from other plugins follow the same rules as commands. */
public final class MyDogApiService implements MyDogApi {
    private final DogService dogs;
    private final Supplier<Settings> settings;

    public MyDogApiService(DogService dogs, Supplier<Settings> settings) {
        this.dogs = dogs;
        this.settings = settings;
    }

    static DogInfo info(Dog dog) {
        Dog.Appearance appearance = dog.getAppearance();
        return new DogInfo(dog.getDogId(), dog.getOwnerId(), dog.getCoOwners(), dog.getIdentifier(), dog.getDogName(),
                dog.getLevel(), dog.getExperience(), dog.getBirthday(), dog.isAngry(), dog.isDead(), dog.isStored(),
                appearance != null ? appearance.collar() : null,
                DogService.current(dog, TreatKind.BREED), DogService.current(dog, TreatKind.PERSONALITY));
    }

    @Override
    public boolean isDog(Entity entity) {
        return dogs.dogOf(entity).isPresent();
    }

    @Override
    public Optional<DogInfo> getDog(UUID dogId) {
        return dogs.find(dogId).map(MyDogApiService::info);
    }

    @Override
    public Optional<DogInfo> getDog(Entity entity) {
        return dogs.dogOf(entity).map(MyDogApiService::info);
    }

    @Override
    public Optional<DogInfo> getDog(UUID ownerId, int identifier) {
        return dogs.find(ownerId, identifier).map(MyDogApiService::info);
    }

    @Override
    public List<DogInfo> getDogs(UUID ownerId) {
        return dogs.dogsOf(ownerId).stream().map(MyDogApiService::info).toList();
    }

    @Override
    public List<DogInfo> getSharedDogs(UUID playerId) {
        return dogs.sharedWith(playerId).stream().map(MyDogApiService::info).toList();
    }

    @Override
    public Optional<Wolf> getWolf(UUID dogId) {
        return dogs.find(dogId).flatMap(dogs::wolfOf);
    }

    @Override
    public boolean giveExperience(UUID dogId, int experience) {
        Optional<Dog> dog = dogs.find(dogId).filter(d -> !d.isDead());
        if (dog.isEmpty() || !settings.get().levels().enabled() || experience <= 0) {
            return false;
        }
        dogs.giveExperience(dog.get(), experience, null);
        return true;
    }

    @Override
    public boolean setLevel(UUID dogId, int level) {
        Optional<Dog> dog = dogs.find(dogId).filter(d -> !d.isDead());
        Settings.Levels levels = settings.get().levels();
        LevelTable table = levels.table();
        if (dog.isEmpty() || !levels.enabled() || level < table.minLevel() || level > table.maxLevel()) {
            return false;
        }
        return dogs.setLevel(dog.get(), level, DogLevelChangeEvent.Cause.SET);
    }

    @Override
    public boolean rename(UUID dogId, String name) {
        Optional<Dog> dog = dogs.find(dogId);
        if (dog.isEmpty() || name == null || DogService.cleanName(name).isEmpty()) {
            return false;
        }
        return dogs.rename(dog.get(), name, null);
    }
}
