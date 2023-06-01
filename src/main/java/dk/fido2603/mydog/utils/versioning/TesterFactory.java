package dk.fido2603.mydog.utils.versioning;

import org.bukkit.plugin.Plugin;

// From DogOnFire's Versioning in Werewolf
// https://github.com/DogOnFire/Werewolf
public class TesterFactory {

    @SuppressWarnings("rawtypes")
    public static Tester getNewTester(Plugin plugin) {
        if (plugin == null) {
            return (Tester<Plugin>) t -> false;
        } else {
            return (Tester<Plugin>) Plugin::isEnabled;
        }
    }

    @SuppressWarnings("rawtypes")
    public static Tester getDefaultTester() {
        return t -> true;
    }

}