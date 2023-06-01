package dk.fido2603.mydog;

import java.util.UUID;

import dk.fido2603.mydog.objects.Dog;
import org.bukkit.entity.Wolf;

public class MyDogAPI {
    private final MyDog plugin;

    public MyDogAPI(MyDog p) {
        this.plugin = p;
    }

    public boolean isDog(UUID dogUUID) {
        return MyDog.getDogManager().isDog(dogUUID);
    }

    public boolean isDog(Wolf dog) {
        return MyDog.getDogManager().isDog(dog.getUniqueId());
    }

    public Dog getDog(UUID dogUUID) {
        return MyDog.getDogManager().getDog(dogUUID);
    }
}
