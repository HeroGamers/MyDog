package dk.fido2603.mydog;

import dk.fido2603.mydog.listeners.WolfMainListener_1_18;
import dk.fido2603.mydog.objects.LevelFactory;
import dk.fido2603.mydog.objects.LevelFactory.Level;
import dk.fido2603.mydog.listeners.DamageListener;
import dk.fido2603.mydog.listeners.WolfMainListener;
import dk.fido2603.mydog.managers.CommandManager;
import dk.fido2603.mydog.managers.DogManager;
import dk.fido2603.mydog.managers.PermissionsManager;
import dk.fido2603.mydog.managers.TeleportationManager;
import dk.fido2603.mydog.tasks.AttackModeTask;
import dk.fido2603.mydog.tasks.DistanceTask;
import dk.fido2603.mydog.utils.ParticleUtils;
import dk.fido2603.mydog.utils.versioning.Version;
import dk.fido2603.mydog.utils.versioning.VersionFactory;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

import java.util.*;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/*           THIS PLUGIN IS HEAVILY INSPIRED BY           */
/*                   DOGONFIRE'S MYHORSE                  */
/*														  */
/*          https://github.com/DogOnFire/MyHorse          */
public class MyDog extends JavaPlugin {
    public static MyDog instance;
    public static boolean pluginEnabled = false;

    public boolean vaultEnabled = false;

    public static Server server = null;
    public boolean debug = false;
    public boolean instantSave = false;
    public boolean automaticTeleportation = true;
    public boolean teleportOnWorldChange = true;
    public boolean teleportAllTameables = false;
    public boolean experimentalTeleport = false;
    private boolean playerDistanceCheck = true;
    public boolean expandedSearch = false;
    public boolean onlyShowNametagOnHover = false;
    public boolean showLevelsInNametag = true;

    public boolean allowPlayerKillExp = true;
    public boolean allowNametagRename = true;
    public boolean allowRevival = true;
    public int revivalPrice = 200;
    public boolean revivalUsingPlayerExp = true;
    public boolean allowArrowDamage = false;

    public String levelUpSound = "ENTITY_WOLF_HOWL";
    public String levelUpString = "&5&l[{chatPrefix}] &r&5Your dog, {dogNameColor}{dogName}&5, just leveled up to &dLevel {level}&5!";
    public String cannotTeleportTameableString = "&c&l[{chatPrefix}] &r&cHello! Looks like you just teleported away from your Pet(s)! " +
            "They can sadly not find a safe place to stay, so they are staying behind for now :( They will be waiting for you where you left them...";
    public String newDogString = "&6&l[{chatPrefix}] &r&6Congratulations with your new dog, {dogNameColor}{dogName}&6!";
    public String deadDogString = "&c&l[{chatPrefix}] &r&cYour dog, {dogNameColor}{dogName}&c, just passed away... {dogNameColor}{dogName}&c lived for {time}{deadDogLevelString}.";
    public String deadDogLevelString = ", and got to &4Level {level}&c";
    public String commandComehereString = "&6&l[{chatPrefix}] &r&6Come here! Good doggo, {dogNameColor}{dogName}&6!";

    private static MyDog plugin;
    private static FileConfiguration config = null;
    private static PermissionsManager permissionsManager = null;
    private static DogManager dogManager = null;
    private static TeleportationManager teleportationManager = null;
    private static LevelFactory levelFactory = null;
    private static ParticleUtils particleUtils = null;

    public boolean randomCollarColor = true;
    public boolean useLevels = true;
    public List<String> dogNames = Arrays.asList(
            "Fido", "Queen", "King", "Doggy", "Charlie", "Max", "Milo", "Ollie", "Toby", "Teddy", "Molly", "Rosie", "Bella",
            "Abby", "Addie", "Alexis", "Alice", "Allie", "Alyssa", "Amber", "Angel", "Anna", "Annie", "Ariel", "Ashley",
            "Aspen", "Athena", "Autumn", "Ava", "Avery", "Baby", "Bailey", "Basil", "Bean", "Bella", "Belle", "Betsy",
            "Betty", "Bianca", "Birdie", "Biscuit", "Blondie", "Blossom", "Bonnie", "Brandy", "Brooklyn", "Brownie", "Buffy",
            "Callie", "Camilla", "Candy", "Carla", "Carly", "Carmela", "Casey", "Cassie", "Chance", "Chanel", "Chloe",
            "Cinnamon", "Cleo", "Coco", "Cookie", "Cricket", "Daisy", "Dakota", "Dana", "Daphne", "Darla", "Darlene",
            "Delia", "Delilah", "Destiny", "Diamond", "Diva", "Dixie", "Dolly", "Duchess", "Eden", "Edie", "Ella", "Ellie",
            "Elsa", "Emma", "Emmy", "Eva", "Faith", "Fanny", "Fern", "Fiona", "Foxy", "Gabby", "Gemma", "Georgia", "Gia",
            "Gidget", "Gigi", "Ginger", "Goldie", "Grace", "Gracie", "Greta", "Gypsy", "Hailey", "Hannah", "Harley", "Harper",
            "Hazel", "Heidi", "Hershey", "Holly", "Honey", "Hope", "Ibby", "Inez", "Isabella", "Ivy", "Izzy", "Jackie", "Jada",
            "Jade", "Jasmine", "Jenna", "Jersey", "Jessie", "Jill", "Josie", "Julia", "Juliet", "Juno", "Kali", "Kallie",
            "Karma", "Kate", "Katie", "Kayla", "Kelsey", "Khloe", "Kiki", "Kira", "Koko", "Kona", "Lacy", "Lady", "Layla",
            "Leia", "Lena", "Lexi", "Libby", "Liberty", "Lily", "Lizzy", "Lola", "London", "Lucky", "Lulu", "Luna", "Mabel",
            "Mackenzie", "Macy", "Maddie", "Madison", "Maggie", "Maisy", "Mandy", "Marley", "Matilda", "Mattie", "Maya",
            "Mia", "Mika", "Mila", "Miley", "Millie", "Mimi", "Minnie", "Missy", "Misty", "Mitzi", "Mocha", "Molly", "Morgan",
            "Moxie", "Muffin", "Mya", "Nala", "Nell", "Nellie", "Nikki", "Nina", "Noel", "Nola", "Nori", "Olive", "Olivia",
            "Oreo", "Paisley", "Pandora", "Paris", "Peaches", "Peanut", "Pearl", "Pebbles", "Penny", "Pepper", "Phoebe",
            "Piper", "Pippa", "Pixie", "Polly", "Poppy", "Precious", "Princess", "Priscilla", "Raven", "Reese", "Riley",
            "Rose", "Rosie", "Roxy", "Ruby", "Sadie", "Sage", "Sally", "Sam", "Samantha", "Sammie", "Sandy", "Sasha",
            "Sassy", "Savannah", "Scarlet", "Shadow", "Sheba", "Shelby", "Shiloh", "Sierra", "Sissy", "Sky", "Smokey",
            "Snickers", "Sophia", "Sophie", "Star", "Stella", "Sugar", "Suki", "Summer", "Sunny", "Sweetie", "Sydney",
            "Tasha", "Tessa", "Tilly", "Tootsie", "Trixie", "Violet", "Willow", "Winnie", "Xena", "Zelda", "Zoe", "Abe",
            "Abbott", "Ace", "Aero", "Aiden", "AJ", "Albert", "Alden", "Alex", "Alfie", "Alvin", "Amos", "Andy", "Angus",
            "Apollo", "Archie", "Aries", "Artie", "Ash", "Austin", "Axel", "Bailey", "Bandit", "Barkley", "Barney", "Baron",
            "Baxter", "Bear", "Beau", "Benji", "Benny", "Bentley", "Billy", "Bingo", "Blake", "Blaze", "Blue", "Bo", "Boomer",
            "Brady", "Brody", "Brownie", "Bruce", "Bruno", "Brutus", "Bubba", "Buck", "Buddy", "Buster", "Butch", "Buzz",
            "Cain", "Captain", "Carter", "Cash", "Casper", "Champ", "Chance", "Charlie", "Chase", "Chester", "Chewy", "Chico",
            "Chief", "Chip", "CJ", "Clifford", "Clyde", "Coco", "Cody", "Colby", "Cooper", "Copper", "Damien", "Dane", "Dante",
            "Denver", "Dexter", "Diego", "Diesel", "Dodge", "Drew", "Duke", "Dylan", "Eddie", "Eli", "Elmer", "Emmett", "Evan",
            "Felix", "Finn", "Fisher", "Flash", "Frankie", "Freddy", "Fritz", "Gage", "George", "Gizmo", "Goose", "Gordie",
            "Griffin", "Gunner", "Gus", "Hank", "Harley", "Harvey", "Hawkeye", "Henry", "Hoss", "Huck", "Hunter", "Iggy",
            "Ivan", "Jack", "Jackson", "Jake", "Jasper", "Jax", "Jesse", "Joey", "Johnny", "Judge", "Kane", "King", "Kobe",
            "Koda", "Lenny", "Leo", "Leroy", "Levi", "Lewis", "Logan", "Loki", "Louie", "Lucky", "Luke", "Marley", "Marty",
            "Maverick", "Max", "Maximus", "Mickey", "Miles", "Milo", "Moe", "Moose", "Morris", "Murphy", "Ned", "Nelson",
            "Nero", "Nico", "Noah", "Norm", "Oakley", "Odie", "Odin", "Oliver", "Ollie", "Oreo", "Oscar", "Otis", "Otto",
            "Ozzy", "Pablo", "Parker", "Peanut", "Pepper", "Petey", "Porter", "Prince", "Quincy", "Radar", "Ralph", "Rambo",
            "Ranger", "Rascal", "Rebel", "Reese", "Reggie", "Remy", "Rex", "Ricky", "Rider", "Riley", "Ringo", "Rocco",
            "Rockwell", "Rocky", "Romeo", "Rosco", "Rudy", "Rufus", "Rusty", "Sam", "Sammy", "Samson", "Sarge", "Sawyer",
            "Scooby", "Scooter", "Scout", "Scrappy", "Shadow", "Shamus", "Shiloh", "Simba", "Simon", "Smoky", "Snoopy",
            "Sparky", "Spencer", "Spike", "Spot", "Stanley", "Stewie", "Storm", "Taco", "Tank", "Taz", "Teddy", "Tesla",
            "Theo", "Thor", "Titus", "TJ", "Toby", "Trapper", "Tripp", "Tucker", "Tyler", "Tyson", "Vince", "Vinnie",
            "Wally", "Walter", "Watson", "Willy", "Winston", "Woody", "Wrigley", "Wyatt", "Yogi", "Yoshi", "Yukon",
            "Zane", "Zeus", "Ziggy");

    public Map<Integer, Level> dogLevels = new HashMap<Integer, Level>();

    private static Economy economy = null;
    private CommandManager commands = null;
    private String chatPrefix = "MyDog";
    public String serverName = "Your Server";

    public static MyDog instance() {
        return instance;
    }

    public static PermissionsManager getPermissionsManager() {
        return permissionsManager;
    }

    public static DogManager getDogManager() {
        return dogManager;
    }

    public static TeleportationManager getTeleportationManager() {
        return teleportationManager;
    }

    public static LevelFactory getLevelFactory() {
        return levelFactory;
    }

    public static ParticleUtils getParticleUtils() {
        return particleUtils;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public String getChatPrefix() {
        return chatPrefix;
    }

    public void sendInfo(Player player, String message) {
        if (player == null) {
            log(message);
        } else {
            player.sendMessage(message);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public void onDisable() {
        saveSettings();
        reloadSettings();

        pluginEnabled = false;
    }

    @Override
    public void onEnable() {
        WolfMainListener tameListener = null;
        WolfMainListener_1_18 tameListener_1_18 = null;
        DamageListener damageListener = null;

        plugin = this;
        instance = this;
        server = getServer();
        config = getConfig();

        this.commands = new CommandManager(this);

        pluginEnabled = true;

        tameListener = new WolfMainListener(this);
        tameListener_1_18 = new WolfMainListener_1_18(this);
        damageListener = new DamageListener(this);
        dogManager = new DogManager(this);
        teleportationManager = new TeleportationManager(this);
        levelFactory = new LevelFactory(this);

        PluginManager pm = getServer().getPluginManager();

        // Check for Vault
        if (pm.getPlugin("Vault") != null && pm.getPlugin("Vault").isEnabled()) {
            log("Vault detected.");
            setupEconomy();
            RegisteredServiceProvider<Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(Permission.class);
            RegisteredServiceProvider<Chat> chatProvider = plugin.getServer().getServicesManager().getRegistration(Chat.class);
            if (permissionProvider == null || chatProvider == null) {
                plugin.log("A permission provider or a chat provider was not found! Will not enable the vault integration!");
            } else {
                this.vaultEnabled = true;
            }
        } else {
            log("Vault not found.");
        }

        permissionsManager = new PermissionsManager(this);
        particleUtils = new ParticleUtils(this);

        reloadSettings();
        saveSettings();

        permissionsManager.load();
        Version version = VersionFactory.getServerVersion();

        getServer().getPluginManager().registerEvents(tameListener, this);
        if (version.isCompatible("1.18")) {
            getServer().getPluginManager().registerEvents(tameListener_1_18, this);
        }
        getServer().getPluginManager().registerEvents(damageListener, this);

        // The dog distance checker, might take some extra powerrr. Checks every ~30 seconds. Starts after 1,5 minutes.
        if (playerDistanceCheck && automaticTeleportation) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new DistanceTask(this), 20L * 60L, 20L * 10L);
        }

        // Attack mode / angry checker
        // keeps the dog on a target
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new AttackModeTask(this), 20L * 30L, 20L * 2L);
    }

    public void log(String message) {
        plugin.getLogger().info(message);
    }

    public void logDebug(String message) {
        if (this.debug) {
            plugin.getLogger().info("[Debug] " + message);
        }
    }

    public void reloadSettings() {
        reloadConfig();
        loadSettings();
    }

    public void loadSettings() {
        config = getConfig();

        this.debug = config.getBoolean("Settings.Debug", false);
        this.serverName = config.getString("Settings.ServerName", "Your Server");
        this.chatPrefix = config.getString("Settings.ChatPrefix", "MyDog");
        this.instantSave = config.getBoolean("Settings.InstantSaveConfig", false);
        this.automaticTeleportation = config.getBoolean("Settings.AutomaticTeleportation", true);
        this.teleportAllTameables = config.getBoolean("Settings.TeleportAllTameables", false);
        this.experimentalTeleport = config.getBoolean("Settings.EnableExperimentalTeleport", false);
        this.playerDistanceCheck = config.getBoolean("Settings.PlayerDistanceCheck", true);
        this.expandedSearch = config.getBoolean("Settings.ExpandedSearch", false);
        this.randomCollarColor = config.getBoolean("DogSettings.RandomCollarColor", true);
        this.useLevels = config.getBoolean("DogSettings.UseLevels", true);
        this.teleportOnWorldChange = config.getBoolean("DogSettings.TeleportOnWorldChange", true);
        this.onlyShowNametagOnHover = config.getBoolean("DogSettings.OnlyShowNametagOnHover", false);
        this.showLevelsInNametag = config.getBoolean("DogSettings.ShowLevelsInNametag", true);
        this.allowPlayerKillExp = config.getBoolean("DogSettings.AllowPlayerKillExp", true);
        this.allowNametagRename = config.getBoolean("DogSettings.AllowNametagRename", true);
        this.allowRevival = config.getBoolean("DogSettings.AllowRevival", true);
        this.revivalPrice = config.getInt("DogSettings.RevivalPricePerLevel", 200);
        this.revivalUsingPlayerExp = config.getBoolean("DogSettings.RevivalUsingPlayerExp", false);
        this.allowArrowDamage = config.getBoolean("DogSettings.AllowArrowDamage", false);
        if (config.contains("DogSettings.DogNames") && !config.getStringList("DogSettings.DogNames").isEmpty()) {
            this.dogNames = config.getStringList("DogSettings.DogNames");
        }

        // Levels
        if (config.getConfigurationSection("DogSettings.Levels") != null) {
            this.dogLevels.clear();
            for (String level : config.getConfigurationSection("DogSettings.Levels").getKeys(false)) {
                if (config.getConfigurationSection("DogSettings.Levels." + level) != null) {
                    int exp = config.getInt("DogSettings.Levels." + level + ".Experience");
                    double health = config.getInt("DogSettings.Levels." + level + ".Health");
                    double damage = config.getInt("DogSettings.Levels." + level + ".Damage");

                    this.dogLevels.put(Integer.parseInt(level), getLevelFactory().newLevel(Integer.parseInt(level), exp, health, damage));
                }
            }
        } else {
            // Put levels into the hashmap
            // Level format - [level, experience]
            this.dogLevels.clear();
            this.dogLevels.put(1, getLevelFactory().newLevel(1, 0, 20, 4));
            this.dogLevels.put(2, getLevelFactory().newLevel(2, 10, 21, 5));
            this.dogLevels.put(3, getLevelFactory().newLevel(3, 100, 22, 6));
            this.dogLevels.put(4, getLevelFactory().newLevel(4, 200, 23, 7));
            this.dogLevels.put(5, getLevelFactory().newLevel(5, 500, 24, 8));
            this.dogLevels.put(6, getLevelFactory().newLevel(6, 1000, 26, 11));
            this.dogLevels.put(7, getLevelFactory().newLevel(7, 2000, 29, 13));
            this.dogLevels.put(8, getLevelFactory().newLevel(8, 3000, 31, 15));
            this.dogLevels.put(9, getLevelFactory().newLevel(9, 4000, 33, 17));
            this.dogLevels.put(10, getLevelFactory().newLevel(10, 5000, 36, 20));
        }

        // Messages and sounds
        this.levelUpSound = config.getString("PlayerInteraction.LevelUpSound", "ENTITY_WOLF_HOWL");
        this.levelUpString = config.getString("PlayerInteraction.LevelUpString", "&5&l[{chatPrefix}] &r&5Your dog, {dogNameColor}{dogName}&5, just leveled up to &dLevel {level}&5!");
        this.cannotTeleportTameableString = config.getString("PlayerInteraction.CannotTeleportTameableString", "&c&l[{chatPrefix}] &r&cHello! Looks like you just teleported away from your Pet(s)! " +
                "They can sadly not find a safe place to stay, so they are staying behind for now :( They will be waiting for you where you left them...");
        this.newDogString = config.getString("PlayerInteraction.NewDogString", "&6&l[{chatPrefix}] &r&6Congratulations with your new dog, {dogNameColor}{dogName}&6!");
        this.deadDogString = config.getString("PlayerInteraction.DeadDogString", "&c&l[{chatPrefix}] &r&cYour dog, {dogNameColor}{dogName}&c, just passed away... {dogNameColor}{dogName}&c lived for {time}{deadDogLevelString}.");
        this.deadDogLevelString = config.getString("PlayerInteraction.DeadDogLevelString", ", and got to &4Level {level}&c");
        this.commandComehereString = config.getString("PlayerInteraction.CommandComehereString", "&6&l[{chatPrefix}] &r&6Come here! Good doggo, {dogNameColor}{dogName}&6!");

        dogManager.load();
    }

    public void saveSettings() {
        config.set("Settings.ServerName", this.serverName);
        config.set("Settings.Debug", Boolean.valueOf(this.debug));
        config.set("Settings.ChatPrefix", this.chatPrefix);
        config.set("Settings.InstantSaveConfig", Boolean.valueOf(this.instantSave));
        config.set("Settings.AutomaticTeleportation", Boolean.valueOf(this.automaticTeleportation));
        config.set("Settings.ExpandedSearch", Boolean.valueOf(this.expandedSearch));
        config.set("Settings.EnableExperimentalTeleport", Boolean.valueOf(this.experimentalTeleport));
        config.set("Settings.PlayerDistanceCheck", Boolean.valueOf(this.playerDistanceCheck));
        config.set("DogSettings.RandomCollarColor", this.randomCollarColor);
        config.set("DogSettings.UseLevels", this.useLevels);
        config.set("DogSettings.TeleportOnWorldChange", this.teleportOnWorldChange);
        config.set("Settings.TeleportAllTameables", this.teleportAllTameables);
        config.set("DogSettings.OnlyShowNametagOnHover", this.onlyShowNametagOnHover);
        config.set("DogSettings.ShowLevelsInNametag", this.showLevelsInNametag);
        config.set("DogSettings.AllowPlayerKillExp", this.allowPlayerKillExp);
        config.set("DogSettings.AllowNametagRename", this.allowNametagRename);
        config.set("DogSettings.DogNames", this.dogNames);
        config.set("DogSettings.AllowRevival", this.allowRevival);
        config.set("DogSettings.RevivalPricePerLevel", this.revivalPrice);
        config.set("DogSettings.RevivalUsingPlayerExp", this.revivalUsingPlayerExp);
        config.set("DogSettings.AllowArrowDamage", this.allowArrowDamage);

        // Levels
        for (Integer level : this.dogLevels.keySet()) {
            Level levelObject = this.dogLevels.get(level);
            config.set("DogSettings.Levels." + level + ".Experience", levelObject.exp);
            config.set("DogSettings.Levels." + level + ".Health", levelObject.health);
            config.set("DogSettings.Levels." + level + ".Damage", levelObject.damage);
        }

        // Messages and sounds
        config.set("PlayerInteraction.LevelUpSound", this.levelUpSound);
        config.set("PlayerInteraction.LevelUpString", this.levelUpString);
        config.set("PlayerInteraction.CannotTeleportTameableString", this.cannotTeleportTameableString);
        config.set("PlayerInteraction.NewDogString", this.newDogString);
        config.set("PlayerInteraction.DeadDogString", this.deadDogString);
        config.set("PlayerInteraction.DeadDogLevelString", this.deadDogLevelString);
        config.set("PlayerInteraction.CommandComehereString", this.commandComehereString);

        saveConfig();
        dogManager.save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return this.commands.onCommand(sender, cmd, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return this.commands.onTabComplete(sender, cmd, alias, args);
    }
}