package dk.fido2603.mydog;

import java.util.UUID;

import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.MyDog;

public class MyDogAPI {
    private MyDog plugin = null;

    public MyDogAPI(MyDog p) {
        this.plugin = p;
    }

    public boolean isDog(UUID dogUUID) {
        if (plugin.getServer().getEntity(dogUUID) instanceof Wolf) {
            return MyDog.getDogManager().isDog(dogUUID);
        }
        return false;
    }

    public boolean isDog(Wolf dog) {
        return MyDog.getDogManager().isDog(dog.getUniqueId());
    }
}
