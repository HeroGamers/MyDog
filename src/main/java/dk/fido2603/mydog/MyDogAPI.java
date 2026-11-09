package dk.fido2603.mydog;

import java.util.UUID;

import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.objects.Dog;

/**
 * For other plugins: {@code new MyDogAPI(JavaPlugin.getPlugin(MyDog.class))}.
 */
public class MyDogAPI {
    private final MyDog plugin;

    public MyDogAPI(MyDog plugin) {
        this.plugin = plugin;
    }

    /** Whether the UUID belongs to a dog, alive or dead. */
    public boolean isDog(UUID dogUUID) {
        return plugin.dogService().find(dogUUID).isPresent();
    }

    public boolean isDog(Wolf dog) {
        return plugin.dogService().dogOf(dog).isPresent();
    }

    /** The dog's data, or {@code null} if it isn't a dog. Treat it as read-only. */
    public Dog getDog(UUID dogUUID) {
        return plugin.dogService().find(dogUUID).orElse(null);
    }
}
