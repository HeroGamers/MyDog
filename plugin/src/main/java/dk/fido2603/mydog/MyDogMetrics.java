package dk.fido2603.mydog;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.Economy;
import dk.fido2603.mydog.storage.DogRepository;

/**
 * Anonymous usage statistics on bStats (https://bstats.org/plugin/bukkit/MyDog/18606), with MyDog's own charts. Each
 * chart also has to be added on that page, with the same id and type. Server owners turn bStats off for all plugins
 * in plugins/bStats/config.yml.
 * <p>
 * bStats asks for the chart values on its own thread, where dogs and settings must not be read, so the charts read a
 * {@link Snapshot} that {@link #refresh()} takes on the main thread.
 */
final class MyDogMetrics {
    private static final int BSTATS_ID = 18606;
    /** How often the snapshot is taken, in ticks (5 minutes); bStats sends data every 30 minutes. */
    static final long REFRESH_TICKS = 5 * 60 * 20L;

    /** What the charts show, taken on the main thread. */
    private record Snapshot(int dogs, int owners, Map<String, Integer> breeds, Map<String, Integer> features,
                            Map<String, Integer> integrations, String levelMode) {}

    private final JavaPlugin plugin;
    private final DogRepository dogs;
    private final Supplier<Settings> settings;
    private final Metrics metrics;
    private volatile Snapshot snapshot;

    MyDogMetrics(JavaPlugin plugin, DogRepository dogs, Supplier<Settings> settings) {
        this.plugin = plugin;
        this.dogs = dogs;
        this.settings = settings;
        refresh();

        metrics = new Metrics(plugin, BSTATS_ID);
        metrics.addCustomChart(new SingleLineChart("dogs", () -> snapshot.dogs()));
        metrics.addCustomChart(new SingleLineChart("dog_owners", () -> snapshot.owners()));
        metrics.addCustomChart(new AdvancedPie("breeds", () -> snapshot.breeds()));
        metrics.addCustomChart(new AdvancedPie("features", () -> snapshot.features()));
        metrics.addCustomChart(new AdvancedPie("integrations", () -> snapshot.integrations()));
        metrics.addCustomChart(new SimplePie("level_mode", () -> snapshot.levelMode()));
    }

    /** Main thread: takes a new snapshot of the chart values. */
    void refresh() {
        Settings config = settings.get();

        Set<UUID> owners = new HashSet<>();
        Map<String, Integer> breeds = new LinkedHashMap<>();
        for (Dog dog : dogs.all()) {
            owners.add(dog.getOwnerId());
            Dog.Appearance appearance = dog.getAppearance();
            NamespacedKey variant = appearance != null && appearance.variant() != null
                    ? NamespacedKey.fromString(appearance.variant()) : null;
            if (variant != null) {
                // Vanilla breeds without the namespace ("snowy"), datapack ones with it
                String breed = variant.getNamespace().equals(NamespacedKey.MINECRAFT) ? variant.getKey() : variant.toString();
                breeds.merge(breed, 1, Integer::sum);
            }
        }

        Map<String, Integer> features = new LinkedHashMap<>();
        addIf(features, "Levels", config.levels().enabled());
        addIf(features, "Revival", config.dogs().revival().enabled());
        addIf(features, "Auto-feed", config.dogs().autoFeed().enabled());
        addIf(features, "Breed treats", config.breeds().treats());
        addIf(features, "Free breed choice", config.breeds().freeChoice());
        addIf(features, "Stat boosts", config.boosts().enabled());
        addIf(features, "Teleport", config.teleport().enabled());
        addIf(features, "Low-health warning", config.dogs().lowHealthWarning().enabled());
        addIf(features, "Protection from players", config.dogs().protectFromOtherPlayers());
        addIf(features, "Invulnerable", config.dogs().invulnerable());
        addIf(features, "Lifesteal", config.dogs().lifesteal() > 0);
        addIf(features, "Unbreakable armor", config.dogs().armor().unbreakable());
        addIf(features, "Instant saving", config.saving().instant());

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        Map<String, Integer> integrations = new LinkedHashMap<>();
        addIf(integrations, "Vault economy", Economy.find(plugin.getServer()).isPresent());
        addIf(integrations, "MythicMobs", pluginManager.isPluginEnabled("MythicMobs"));
        addIf(integrations, "Citizens", pluginManager.isPluginEnabled("Citizens"));

        String levelMode = !config.levels().enabled() ? "Disabled" : config.levels().table().scaled() ? "Scaled" : "Table";

        snapshot = new Snapshot(dogs.all().size(), owners.size(), Map.copyOf(breeds), Map.copyOf(features),
                Map.copyOf(integrations), levelMode);
    }

    private static void addIf(Map<String, Integer> map, String name, boolean present) {
        if (present) {
            map.put(name, 1);
        }
    }

    void shutdown() {
        metrics.shutdown();
    }
}
