package dk.fido2603.mydog.utils.versioning;

// From DogOnFire's Versioning in Werewolf
// https://github.com/DogOnFire/Werewolf
public interface Tester<T> {
    boolean isEnabled(T t);
}