package dk.fido2603.mydog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import dk.fido2603.mydog.api.MyDogApi;
import dk.fido2603.mydog.command.CalmCommand;
import dk.fido2603.mydog.command.ComehereCommand;
import dk.fido2603.mydog.command.CommandRouter;
import dk.fido2603.mydog.command.CommandServices;
import dk.fido2603.mydog.command.DeadCommand;
import dk.fido2603.mydog.command.GiveTreatCommand;
import dk.fido2603.mydog.command.HelpCommand;
import dk.fido2603.mydog.command.ListCommand;
import dk.fido2603.mydog.command.LookCommand;
import dk.fido2603.mydog.command.ModeCommand;
import dk.fido2603.mydog.command.PutdownCommand;
import dk.fido2603.mydog.command.ReleaseCommand;
import dk.fido2603.mydog.command.ReloadCommand;
import dk.fido2603.mydog.command.RenameCommand;
import dk.fido2603.mydog.command.ReviveCommand;
import dk.fido2603.mydog.command.SaveCommand;
import dk.fido2603.mydog.command.SetIdCommand;
import dk.fido2603.mydog.command.SetLevelCommand;
import dk.fido2603.mydog.command.ShareCommand;
import dk.fido2603.mydog.command.SitCommand;
import dk.fido2603.mydog.command.StatsCommand;
import dk.fido2603.mydog.command.StoreCommand;
import dk.fido2603.mydog.command.TradeAcceptCommand;
import dk.fido2603.mydog.command.TradeCommand;
import dk.fido2603.mydog.command.TradeDeclineCommand;
import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.listener.CombatListener;
import dk.fido2603.mydog.listener.InteractListener;
import dk.fido2603.mydog.listener.TamingListener;
import dk.fido2603.mydog.listener.TreatListener;
import dk.fido2603.mydog.listener.WorldListener;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.migration.ConfigV1ToV2;
import dk.fido2603.mydog.migration.VersionedYaml;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.service.Effects;
import dk.fido2603.mydog.service.MyDogApiService;
import dk.fido2603.mydog.service.PluginMobs;
import dk.fido2603.mydog.service.TeleportService;
import dk.fido2603.mydog.service.TradeService;
import dk.fido2603.mydog.service.TreatService;
import dk.fido2603.mydog.storage.YamlDogRepository;
import dk.fido2603.mydog.task.AttackModeTask;
import dk.fido2603.mydog.task.DistanceTask;
import dk.fido2603.mydog.task.FeedTask;
import dk.fido2603.mydog.util.AtomicFiles;
import dk.fido2603.mydog.util.DebugLog;

/*           THIS PLUGIN IS HEAVILY INSPIRED BY           */
/*                   DOGONFIRE'S MYHORSE                  */
/*														  */
/*          https://github.com/DogOnFire/MyHorse          */
public final class MyDog extends JavaPlugin {
    public static final int CONFIG_VERSION = 2;
    private static final int BSTATS_ID = 18606;
    /** Tables the admin may shorten; missing entries in them are not filled in from the defaults. */
    private static final List<String> FREE_FORM_SECTIONS = List.of("levels.table.", "levels.experience-per-kill.",
            "breeds.treats.breeds.", "breeds.treats.personalities.");

    private volatile Settings settings;
    private Messages messages;
    private YamlDogRepository dogs;
    private DogService dogService;
    private TreatService treatService;
    private Metrics metrics;
    private int minutesSinceSave;

    @Override
    public void onEnable() {
        try {
            settings = loadSettings();
            messages = new Messages(this, fullName());
            messages.load();
            YamlDogRepository repository = new YamlDogRepository(new File(getDataFolder(), "dogs.yml"), getLogger(),
                    () -> settings.saving().instant());
            repository.load();
            dogs = repository;
        } catch (VersionedYaml.LoadException e) {
            getLogger().severe(e.getMessage());
            getLogger().severe("MyDog is disabling itself so it doesn't overwrite your files. Fix the problem and restart.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Supplier<Settings> currentSettings = this::settings;
        DebugLog debug = new DebugLog(getLogger(), () -> settings.debug());
        PluginMobs pluginMobs = new PluginMobs(getServer(), getLogger());
        dogService = new DogService(getServer(), dogs, currentSettings, messages, new Effects(this), debug, pluginMobs);
        TradeService tradeService = new TradeService(this, dogService, messages);
        TeleportService teleportService = new TeleportService(dogService, currentSettings, messages, debug);
        treatService = new TreatService(this, messages, currentSettings);
        treatService.registerRecipes();

        for (Listener listener : List.of(
                new TamingListener(this, dogService, messages, currentSettings),
                new InteractListener(dogService, messages, currentSettings),
                new CombatListener(getServer(), dogService, messages, currentSettings),
                new WorldListener(this, dogService, teleportService, currentSettings),
                new TreatListener(this, dogService, treatService, messages))) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        registerCommands(new CommandServices(this, getServer(), dogService, tradeService, messages, currentSettings));

        getServer().getScheduler().runTaskTimer(this,
                new DistanceTask(getServer(), dogService, teleportService, currentSettings), 20L * 60L, DistanceTask.PERIOD_TICKS);
        getServer().getScheduler().runTaskTimer(this,
                new AttackModeTask(getServer(), dogService, pluginMobs, currentSettings), 20L * 30L, AttackModeTask.PERIOD_TICKS);
        getServer().getScheduler().runTaskTimer(this,
                new FeedTask(getServer(), dogService, messages, currentSettings), FeedTask.PERIOD_TICKS, FeedTask.PERIOD_TICKS);
        getServer().getScheduler().runTaskTimer(this, this::autosave, 20L * 60L, 20L * 60L);

        // The API other plugins use, through Bukkit's services manager
        getServer().getServicesManager().register(MyDogApi.class, new MyDogApiService(dogService, currentSettings), this,
                ServicePriority.Normal);

        metrics = new Metrics(this, BSTATS_ID);
    }

    private void registerCommands(CommandServices services) {
        CommandRouter router = new CommandRouter(services);
        router.register(new HelpCommand(services, router));
        router.register(new ReloadCommand(services));
        router.register(new SaveCommand(services));
        router.register(new ListCommand(services));
        router.register(new PutdownCommand(services));
        router.register(new ReleaseCommand(services));
        router.register(new ComehereCommand(services));
        router.register(StoreCommand.store(services));
        router.register(StoreCommand.summon(services));
        router.register(SitCommand.sit(services));
        router.register(SitCommand.stand(services));
        router.register(ModeCommand.attack(services));
        router.register(ModeCommand.defend(services));
        router.register(new CalmCommand(services));
        router.register(new TradeCommand(services));
        router.register(new TradeAcceptCommand(services));
        router.register(new TradeDeclineCommand(services));
        router.register(ShareCommand.share(services));
        router.register(ShareCommand.unshare(services));
        router.register(new StatsCommand(services));
        router.register(new RenameCommand(services));
        router.register(new SetIdCommand(services));
        router.register(new SetLevelCommand(services));
        router.register(new DeadCommand(services));
        router.register(new ReviveCommand(services));
        router.register(LookCommand.breed(services));
        router.register(LookCommand.personality(services));
        router.register(new GiveTreatCommand(services, treatService));

        PluginCommand command = getCommand("mydog");
        if (command != null) {
            command.setExecutor(router);
            command.setTabCompleter(router);
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (metrics != null) {
            metrics.shutdown();
        }
        // Only save data that was loaded successfully, or a broken file would be overwritten with nothing
        if (dogs != null) {
            dogs.saveNow();
        }
    }

    public Settings settings() {
        return settings;
    }

    /** For the deprecated {@link MyDogAPI}. */
    DogService dogService() {
        return dogService;
    }

    /**
     * Reloads config.yml and messages.yml and reapplies nametags and stats to loaded dogs. If either file can't be
     * read, nothing changes and false is returned.
     */
    public boolean reload() {
        try {
            Settings reloaded = loadSettings();
            messages.load();
            settings = reloaded;
        } catch (VersionedYaml.LoadException e) {
            getLogger().severe("Reload failed: " + e.getMessage());
            return false;
        }
        treatService.registerRecipes();
        for (Dog dog : List.copyOf(dogs.all())) {
            dogService.updateWolf(dog);
        }
        return true;
    }

    public void saveDogs() {
        dogs.saveAsync();
    }

    private void autosave() {
        minutesSinceSave++;
        if (minutesSinceSave >= settings.saving().autosaveMinutes()) {
            minutesSinceSave = 0;
            dogs.saveAsync();
        }
    }

    /**
     * Loads config.yml: writes the default one on first start, upgrades an old one, and adds settings that are missing
     * because they were added in a newer version of MyDog.
     */
    private Settings loadSettings() throws VersionedYaml.LoadException {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveResource("config.yml", false);
        }
        VersionedYaml format = new VersionedYaml("config-version", CONFIG_VERSION, 1, List.of(new ConfigV1ToV2(getDataFolder(), this::getResource, getLogger())));
        YamlConfiguration config = format.load(file, getLogger());
        YamlConfiguration defaults = bundledConfig();

        boolean added = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key) || config.contains(key)
                    || FREE_FORM_SECTIONS.stream().anyMatch(key::startsWith)) {
                continue;
            }
            config.set(key, defaults.get(key));
            config.setComments(key, defaults.getComments(key));
            added = true;
        }
        if (added) {
            try {
                AtomicFiles.write(file.toPath(), config.saveToString());
                getLogger().info("Added new settings to config.yml.");
            } catch (IOException e) {
                getLogger().warning("Could not add new settings to config.yml: " + e.getMessage());
            }
        }

        config.setDefaults(defaults);
        return Settings.load(config, getLogger());
    }

    private YamlConfiguration bundledConfig() throws VersionedYaml.LoadException {
        try (InputStream in = getResource("config.yml")) {
            if (in == null) {
                throw new VersionedYaml.LoadException("config.yml is missing from the plugin jar", null);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new VersionedYaml.LoadException("Could not read the bundled config.yml", e);
        }
    }

    // JavaPlugin#getDescription() is deprecated on Paper in favor of getPluginMeta(), which Spigot doesn't have
    @SuppressWarnings("deprecation")
    private String fullName() {
        return getDescription().getFullName();
    }
}
