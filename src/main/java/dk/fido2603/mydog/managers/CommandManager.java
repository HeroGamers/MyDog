package dk.fido2603.mydog.managers;

import java.text.DecimalFormat;
import java.util.*;

import dk.fido2603.mydog.MyDog;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.util.StringUtil;

import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.objects.LevelFactory.Level;

public class CommandManager {
    private final MyDog plugin;

    public CommandManager(MyDog p) {
        this.plugin = p;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!plugin.isEnabled()) {
            return false;
        }

        Player player = null;

        if ((sender instanceof Player)) {
            player = (Player) sender;
        }

        if ((cmd.getName().equalsIgnoreCase("mydog")) || (cmd.getName().equalsIgnoreCase("md")) || (cmd.getName().equalsIgnoreCase("dog")) || (cmd.getName().equalsIgnoreCase("dogs"))) {
            if ((args.length == 0) && (player != null)) {
                commandHelp(sender);
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (player == null) {
                        plugin.reloadSettings();
                        this.plugin.log("Reloaded the configurations.");

                        return true;
                    }

                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.reload"))) {
                        return false;
                    }

                    this.plugin.reloadSettings();
                    sender.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ":" + ChatColor.AQUA + " Reloaded the configurations.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("save")) {
                    if (player == null) {
                        plugin.saveSettings();
                        this.plugin.log("Saved the configurations.");

                        return true;
                    }

                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.save"))) {
                        return false;
                    }

                    this.plugin.saveSettings();
                    sender.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ":" + ChatColor.AQUA + " Saved the configurations.");
                    return true;
                }
                if ((args[0].equalsIgnoreCase("help")) && (player != null)) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.help"))) {
                        return false;
                    }

                    commandList(sender);
                    return true;
                }
                if (((args[0].equalsIgnoreCase("dogs")) || (args[0].equalsIgnoreCase("list"))) && (player != null)) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.dogs"))) {
                        return false;
                    }

                    commandDogList(sender);
                    return true;
                }
                if (args[0].equalsIgnoreCase("dead") && (player != null)) {
                    if (!plugin.allowRevival || ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.dead")))) {
                        return false;
                    }

                    commandDogDead(sender);
                    return true;
                }
                if (args[0].equalsIgnoreCase("tradeaccept") && (player != null)) {
                    if (((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.trade")))) {
                        return false;
                    }

                    commandTradeAccept(sender);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("tradedeny") || args[0].equalsIgnoreCase("tradedecline")) && (player != null)) {
                    if (((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.trade")))) {
                        return false;
                    }

                    commandTradeDeny(sender);
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Not a MyDog Command! Check /mydog help");
            } else if ((args.length == 2) && (player != null)) {
                if ((args[0].equalsIgnoreCase("putdown")) || (args[0].equalsIgnoreCase("kill"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.putdown"))) {
                        return false;
                    }
                    int dogIdentifier;
                    try {
                        dogIdentifier = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                        return true;
                    }

                    commandDogPutdown(sender, dogIdentifier);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("free")) || (args[0].equalsIgnoreCase("setfree")) || (args[0].equalsIgnoreCase("release"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.free"))) {
                        return false;
                    }

                    int dogIdentifier;
                    try {
                        dogIdentifier = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                        return true;
                    }

                    commandDogFree(sender, dogIdentifier);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("stats")) || (args[0].equalsIgnoreCase("info"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.stats"))) {
                        return false;
                    }
                    int dogIdentifier;
                    try {
                        dogIdentifier = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                        return true;
                    }

                    commandDogStats(sender, dogIdentifier);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("comehere"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.comehere"))) {
                        return false;
                    }

                    int dogIdentifier;
                    if (args[1].equalsIgnoreCase("all")) {
                        dogIdentifier = -1;
                    } else {
                        try {
                            dogIdentifier = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                            return true;
                        }
                    }

                    commandDogComehere(sender, dogIdentifier);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("sit"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.sit"))) {
                        return false;
                    }

                    int dogIdentifier;
                    if (args[1].equalsIgnoreCase("all")) {
                        dogIdentifier = -1;
                    } else {
                        try {
                            dogIdentifier = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                            return true;
                        }
                    }

                    commandDogStandSit(sender, dogIdentifier, true);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("stand"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.sit"))) {
                        return false;
                    }

                    int dogIdentifier;
                    if (args[1].equalsIgnoreCase("all")) {
                        dogIdentifier = -1;
                    } else {
                        try {
                            dogIdentifier = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                            return true;
                        }
                    }

                    commandDogStandSit(sender, dogIdentifier, false);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("attack"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.togglemode"))) {
                        return false;
                    }

                    int dogIdentifier;
                    if (args[1].equalsIgnoreCase("all")) {
                        dogIdentifier = -1;
                    } else {
                        try {
                            dogIdentifier = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                            return true;
                        }
                    }

                    commandDogDefendAttack(sender, dogIdentifier, true);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("defend"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.togglemode"))) {
                        return false;
                    }

                    int dogIdentifier;
                    if (args[1].equalsIgnoreCase("all")) {
                        dogIdentifier = -1;
                    } else {
                        try {
                            dogIdentifier = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                            return true;
                        }
                    }

                    commandDogDefendAttack(sender, dogIdentifier, false);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("revive"))) {
                    if (!plugin.allowRevival || ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.revive")))) {
                        return false;
                    }

                    //check is args1 is a number
                    int dogIdentifier;
                    try {
                        dogIdentifier = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                        return true;
                    }

                    commandReviveDog(player, sender, dogIdentifier);
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Not a MyDog Command! Check /mydog help");
            } else if ((args.length == 3) && (player != null)) {
                if ((args[0].equalsIgnoreCase("editlevel") || args[0].equalsIgnoreCase("setlevel"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.editlevel"))) {
                        return false;
                    }

                    //check is args1 is a number
                    int dogIdentifier;
                    int dogLevel;
                    try {
                        dogIdentifier = Integer.parseInt(args[1]);
                        dogLevel = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                        return true;
                    }

                    commandEditLevel(sender, dogIdentifier, dogLevel);
                    return true;
                }
                if ((args[0].equalsIgnoreCase("setid")) || (args[0].equalsIgnoreCase("changeid"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.setid"))) {
                        return false;
                    }

                    commandDogSetId(sender, args);
                    return true;
                }

                if ((args[0].equalsIgnoreCase("rename"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.rename"))) {
                        return false;
                    }

                    commandDogRename(sender, args);
                    return true;
                }

                if ((args[0].equalsIgnoreCase("trade"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.trade"))) {
                        return false;
                    }

                    //check is args1 is a number
                    int dogIdentifier;
                    String recipientName = args[2];
                    try {
                        dogIdentifier = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                        return true;
                    }

                    Player recipient = plugin.getServer().getPlayer(recipientName);
                    if (recipient == null) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a player with that name!");
                        return true;
                    }
                    if (recipient.getUniqueId().equals(((Player) sender).getUniqueId())) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "You cannot send a trade request to yourself!");
                        return true;
                    }

                    commandTrade(sender, dogIdentifier, recipient, 0.0D);
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Not a MyDog Command! Check /mydog help");
            } else if ((args.length == 4) && (player != null)) {
                if ((args[0].equalsIgnoreCase("trade"))) {
                    if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.trade"))) {
                        return false;
                    }

                    //check is args1 is a number
                    int dogIdentifier;
                    String recipientName = args[2];
                    double price;
                    try {
                        dogIdentifier = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
                        return true;
                    }
                    try {
                        price = Double.parseDouble(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid price!");
                        return true;
                    }

                    if (price < 0.0D) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid price!");
                        return true;
                    }

                    Player recipient = plugin.getServer().getPlayer(recipientName);
                    if (recipient == null) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a player with that name!");
                        return true;
                    }
                    if (recipient.getUniqueId().equals(((Player) sender).getUniqueId())) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "You cannot send a trade request to yourself!");
                        return true;
                    }

                    commandTrade(sender, dogIdentifier, recipient, price);
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Not a MyDog Command! Check /mydog help");
            } else {
                sender.sendMessage(ChatColor.RED + "Too many arguments! Check /mydog help");
            }
        }
        return true;
    }

    private boolean commandHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "---------------- " + plugin.getDescription().getFullName() + " ----------------");
        sender.sendMessage(ChatColor.AQUA + "By HeroGamers (Fido2603)");
        sender.sendMessage(ChatColor.AQUA + "");
        int dogsOwned = MyDog.getDogManager().dogsOwned((Player) sender);
        String dogs = " dogs!";
        if (dogsOwned == 1) {
            dogs = " dog!";
        }
        sender.sendMessage(ChatColor.AQUA + "You currently own " + ChatColor.WHITE + dogsOwned + ChatColor.AQUA + dogs);
        sender.sendMessage(ChatColor.AQUA + "");
        sender.sendMessage(ChatColor.AQUA + "Use " + ChatColor.WHITE + "/mydog help" + ChatColor.AQUA + " for a list of commands!");

        return true;
    }

    private boolean commandList(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "---------------- " + this.plugin.getDescription().getFullName() + " ----------------");
        sender.sendMessage(ChatColor.AQUA + "/mydog" + ChatColor.WHITE + " - Basic info");
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.help"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog help" + ChatColor.WHITE + " - This command");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.reload"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog reload" + ChatColor.WHITE + " - Reloads the MyDog system");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.save"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog save" + ChatColor.WHITE + " - Saves the current changes to the MyDog system");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.dogs"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog list" + ChatColor.WHITE + " - View a list with your current Dogs");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.putdown"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog putdown <id>" + ChatColor.WHITE + " - Kills a dog you own");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.free"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog release <id>" + ChatColor.WHITE + " - Set one of your Dogs free!");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.comehere"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog comehere <id>" + ChatColor.WHITE + " - Forces your Dog to teleport to your location");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.sit"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog sit <id | all>" + ChatColor.WHITE + " - Tells your dog(s) to sit and keep their position(s)");
                sender.sendMessage(ChatColor.AQUA + "/mydog stand <id | all>" + ChatColor.WHITE + " - Tells your dog(s) to stand up and roam free");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.togglemode"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog attack <id | all>" + ChatColor.WHITE + " - Tells your dog(s) to attack any mobs nearby");
                sender.sendMessage(ChatColor.AQUA + "/mydog defend <id | all>" + ChatColor.WHITE + " - Tells your dog(s) to defend you");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.trade"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog trade <id> <player> [price]" + ChatColor.WHITE + " - Send a trade request to another player");
                sender.sendMessage(ChatColor.AQUA + "/mydog tradeaccept" + ChatColor.WHITE + " - Accept a trade");
                sender.sendMessage(ChatColor.AQUA + "/mydog tradedecline" + ChatColor.WHITE + " - Decline a trade");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.stats"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog info <id>" + ChatColor.WHITE + " - Gets stats and other info about a Dog you own");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.rename"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog rename <id> <name>" + ChatColor.WHITE + " - Renames a Dog you own");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.setid"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog setid <id> <newid>" + ChatColor.WHITE + " - Assigns a custom ID to a Dog you own");
            }
            if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.editlevel"))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog setlevel <id> <level>" + ChatColor.WHITE + " - Sets a dog's level");
            }
            if (plugin.allowRevival && ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.dead")))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog dead" + ChatColor.WHITE + " - List of Dogs that have gone to another world");
            }
            if (plugin.allowRevival && ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.revive")))) {
                sender.sendMessage(ChatColor.AQUA + "/mydog revive <id>" + ChatColor.WHITE + " - Resurrects a Dog for a fee");
            }
        }

        return true;
    }

    private boolean commandDogList(CommandSender sender) {
        // Sort the dogs after their ID (identifier)
        TreeMap<Integer, Dog> dogsSorted = new TreeMap<>();
        for (Dog dog : MyDog.getDogManager().getAliveDogs(((Player) sender).getUniqueId())) {
            dogsSorted.put(dog.getIdentifier(), dog);
        }
        DecimalFormat df = new DecimalFormat("#.#");

        sender.sendMessage(ChatColor.YELLOW + "---------------- " + this.plugin.getDescription().getFullName() + " ----------------");
        for (Map.Entry<Integer, Dog> entry : dogsSorted.entrySet()) {
            Wolf wolf = (Wolf) plugin.getServer().getEntity(((Dog) entry.getValue()).getDogId());
            String healthString = "";
            if (wolf != null) {
                double health = wolf.getHealth();
                AttributeInstance maxHealthInstance = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHealth;
                if (maxHealthInstance != null) {
                    maxHealth = maxHealthInstance.getValue();
                }
                else {
                    maxHealth = health;
                }
                healthString = " " + ChatColor.BLUE + "(HP: " + df.format(health) + "/" + df.format(maxHealth) + ")";
            }

            sender.sendMessage(ChatColor.AQUA + "#" + ((Dog) entry.getValue()).getIdentifier() + ChatColor.WHITE + " - " + ChatColor.AQUA + ((Dog) entry.getValue()).getDogName() + healthString);
        }
        return true;
    }

    private boolean commandDogDead(CommandSender sender) {
        // Sort the dogs after their ID (identifier)
        TreeMap<Integer, Dog> dogsSorted = new TreeMap<>();
        for (Dog dog : MyDog.getDogManager().getDeadDogs(((Player) sender).getUniqueId())) {
            dogsSorted.put(dog.getIdentifier(), dog);
        }

        sender.sendMessage(ChatColor.YELLOW + "---------------- " + this.plugin.getDescription().getFullName() + " ----------------");
        for (Map.Entry<Integer, Dog> entry : dogsSorted.entrySet()) {
            sender.sendMessage(ChatColor.AQUA + "#" + ((Dog) entry.getValue()).getIdentifier() + ChatColor.WHITE + " - " + ChatColor.AQUA + ((Dog) entry.getValue()).getDogName() + ChatColor.WHITE + " LVL " + ((Dog) entry.getValue()).getLevel() + " " + ChatColor.GREEN + " $" + ((Dog) entry.getValue()).getRevivalPrice());
        }
        return true;
    }

    private boolean commandDogRename(CommandSender sender, String[] args) {
        int dogIdentifier;
        try {
            dogIdentifier = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please enter a valid ID! Check /mydog dogs");
            return false;
        }

        String name = args[2];

        for (int i = 3; i < args.length; i++) {
            name += " " + args[i];
        }

        Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
        if (dog == null) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
            return false;
        }

        if (name.isEmpty() || name.length() > 16) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please choose a name between 1 and 16 characters for your Dog!");
            return false;
        }

        if (!dog.setDogName(name)) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "An error occured! Could not set Dog name!");
            return false;
        }

        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + "From now on, I will call you " + dog.getDogColor() + dog.getDogName() + ChatColor.RESET + ChatColor.AQUA + "!");

        return true;
    }

    private boolean commandDogSetId(CommandSender sender, String[] args) {
        int dogIdentifier;
        try {
            dogIdentifier = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Invalid ID! Type /mydog dogs to find the ID.");
            return false;
        }
        String new_id = args[2];

        Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
        if (dog == null) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
            return false;
        }

        if (new_id.isEmpty() || new_id.length() > 10) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please choose an identifier between 1 and 10 figures for your Dog!");
            return false;
        }

        int id;
        try {
            id = Integer.parseInt(new_id);
        } catch (NumberFormatException e) {
            plugin.logDebug("Error while trying to format ID from string: " + e);
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Your Dog's identifier can only consist of numbers!");
            return false;
        }

        if (!MyDog.getDogManager().setNewId(dog, id)) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "An error occurred! Could not set new Dog ID!");
            return false;
        }

        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + "New Dog ID successfully set!");

        return true;
    }

    private boolean commandDogPutdown(CommandSender sender, int dogIdentifier) {
        Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
        if (dog == null) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
            return false;
        }

        Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());

        if (wolf == null) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Aww bonkers! Seems like your Dog cannot be found... Is it loaded?");
            return false;
        }

        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + "I'm sorry my friend...");
        wolf.setHealth(0);

        return true;
    }

    private boolean commandDogFree(CommandSender sender, int dogIdentifier) {
        Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());

        if (dog == null) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
            return false;
        }

        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + "It was great having you here, " + dog.getDogColor() + dog.getDogName() + ChatColor.RESET + ChatColor.AQUA + "...");

        Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());

        if (wolf != null) {
            wolf.setCustomName("");
            wolf.setCustomNameVisible(false);
            if (wolf.isSitting()) {
                wolf.setSitting(false);
            }
            wolf.setTamed(false);
        }

        MyDog.getDogManager().removeDog(dog.getDogId());

        return true;
    }

    private boolean commandDogDefendAttack(CommandSender sender, int dogIdentifier, boolean toAttack) {
        List<Dog> dogs = new ArrayList<>();

        String mode = "Defend me";
        if (toAttack) {
            mode = "Attack";
        }

        if (dogIdentifier == -1) {
            dogs = MyDog.getDogManager().getAliveDogs(((Player) sender).getUniqueId());
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + mode + ", my dogs!");
        } else {
            dogs.add(MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId()));
            if (dogs.get(0) != null) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + mode + ", " + dogs.get(0).getDogColor() + dogs.get(0).getDogName() + ChatColor.RESET + ChatColor.AQUA + "!");
            }
        }

        for (Dog dog : dogs) {
            if (dog == null) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
                return false;
            }

            dog.setAngryMode(toAttack);
        }

        return true;
    }

    private boolean commandDogStandSit(CommandSender sender, int dogIdentifier, boolean toSit) {
        List<Dog> dogs = new ArrayList<>();

        String mode = "Stand";
        if (toSit) {
            mode = "Sit";
        }

        if (dogIdentifier == -1) {
            dogs = MyDog.getDogManager().getAliveDogs(((Player) sender).getUniqueId());
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + mode + ", my dogs!");
        } else {
            dogs.add(MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId()));
            if (dogs.get(0) != null) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + mode + ", " + dogs.get(0).getDogColor() + dogs.get(0).getDogName() + ChatColor.RESET + ChatColor.AQUA + "!");
            }
        }

        for (Dog dog : dogs) {
            if (dog == null) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
                return false;
            }

            dog.sit(toSit);
        }

        return true;
    }

    private boolean commandDogStats(CommandSender sender, int dogIdentifier) {
        Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
        if (dog == null) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
            return false;
        }

        Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
        DecimalFormat df = new DecimalFormat("#.#");

        sender.sendMessage(ChatColor.YELLOW + "---------------- " + this.plugin.getDescription().getFullName() + " ----------------");

        sender.sendMessage(ChatColor.AQUA + "Name: " + dog.getDogColor() + dog.getDogName());

        if (plugin.useLevels) {
            sender.sendMessage(ChatColor.AQUA + "Level: " + ChatColor.WHITE + dog.getLevel());

            // Calculate and make experience string
            String experienceString;
            double exp = dog.getExperience();
            double maxExp = 0;

            Map<Integer, Level> levels = plugin.dogLevels;

            for (Integer levelInt : levels.keySet()) {
                int levelExp = levels.get(levelInt).exp;

                // If experience is under the experience needed to level up
                if (exp < levelExp) {
                    // If there is a level under the current one, check if the exp is over or equals to the value of that levelup
                    if (levels.containsKey((levelInt - 1)) && exp >= levels.get((levelInt - 1)).exp) {
                        maxExp = levelExp;
                        break;
                    }
                    // Exp is under needed, and there is no level under. Lowest level found. User is at lowest level then

                    maxExp = levelExp;
                    break;
                } else if (exp > levelExp && !levels.containsKey((levelInt + 1))) // Highest level
                {
                    if (levels.get((levelInt - 1)) == null) {
                        plugin.logDebug("Something went wrong! Last level, there is no level under! Return!");
                        return false;
                    }
                    maxExp = levelExp;
                }
            }

            plugin.logDebug("Exp: " + exp + " - MaxExp: " + maxExp);
            if (maxExp != 0) {
                double percent = (exp / maxExp) * 100;
                plugin.logDebug("Current percent: " + percent);

                experienceString = calculatePercentString(percent) + ChatColor.AQUA + "" + ChatColor.BOLD + " [" + ChatColor.DARK_AQUA + df.format(exp) +
                        ChatColor.AQUA + "" + ChatColor.BOLD + "/" + ChatColor.RESET + ChatColor.AQUA + df.format(maxExp) + ChatColor.AQUA + "" + ChatColor.BOLD + "]";
                sender.sendMessage(ChatColor.AQUA + "Experience: " + experienceString);
            }
        }

        if (wolf != null) {
            // Health graphics
            double health = wolf.getHealth();
            AttributeInstance maxHealthInstance = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double maxHealth;
            if (maxHealthInstance != null) {
                maxHealth = maxHealthInstance.getValue();
            }
            else {
                maxHealth = health;
            }

            double percent = (health / maxHealth) * 100;

            String healthString = calculatePercentString(percent);

            sender.sendMessage(ChatColor.AQUA + "Health: " + healthString + ChatColor.AQUA + "" + ChatColor.BOLD + " [" + ChatColor.DARK_AQUA + df.format(health) +
                    ChatColor.AQUA + "" + ChatColor.BOLD + "/" + ChatColor.RESET + ChatColor.AQUA + df.format(maxHealth) + ChatColor.AQUA + "" + ChatColor.BOLD + "]");

            AttributeInstance attackDamage = wolf.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            sender.sendMessage(ChatColor.AQUA + "Damage: " + ChatColor.WHITE + (attackDamage != null ? attackDamage.getValue() : 0) + " HP");
        }

        Location dogLoc = dog.getDogLocation();
        if (dogLoc != null) {
            sender.sendMessage(ChatColor.AQUA + "Last Seen at: " + ChatColor.DARK_AQUA + "World: " + ChatColor.WHITE + (dogLoc.getWorld() != null ? dogLoc.getWorld().getName() : "Unknown World") +
                    ChatColor.DARK_AQUA + " X: " + ChatColor.WHITE + df.format(dogLoc.getX()) + ChatColor.DARK_AQUA + " Y: " + ChatColor.WHITE + df.format(dogLoc.getY()) +
                    ChatColor.DARK_AQUA + " Z: " + ChatColor.WHITE + df.format(dogLoc.getZ()));
        }

        return true;
    }

    private String calculatePercentString(double percent) {
        String percentString = "==========";

        if (percent >= 10.0 && percent < 19.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=" + ChatColor.AQUA + "=========";
        } else if (percent >= 20.0 && percent < 29.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "==" + ChatColor.AQUA + "========";
        } else if (percent >= 30.0 && percent < 39.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "===" + ChatColor.AQUA + "=======";
        } else if (percent >= 40.0 && percent < 49.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "====" + ChatColor.AQUA + "=====";
        } else if (percent >= 50.0 && percent < 59.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=====" + ChatColor.AQUA + "=====";
        } else if (percent >= 60.0 && percent < 69.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "======" + ChatColor.AQUA + "====";
        } else if (percent >= 70.0 && percent < 79.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=======" + ChatColor.AQUA + "===";
        } else if (percent >= 80.0 && percent < 89.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "========" + ChatColor.AQUA + "==";
        } else if (percent >= 90.0 && percent < 99.9) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=========" + ChatColor.AQUA + "=";
        } else if (percent >= 100.0) {
            percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "==========";
        }

        return ChatColor.AQUA + "" + ChatColor.BOLD + "[" + percentString + ChatColor.AQUA + "" + ChatColor.BOLD + "]";
    }

    private boolean commandReviveDog(Player player, CommandSender sender, int dogIdentifier) {
        List<Dog> dogs = MyDog.getDogManager().getDeadDogs(player.getUniqueId());
        Dog deadDog = null;
        for (Dog dog : dogs) {
            if (dog.getIdentifier() == dogIdentifier) {
                deadDog = dog;
                break;
            }
        }
        if (deadDog == null) {
            sender.sendMessage(ChatColor.RED + "You don't have a dead dog with that ID.");
            return false;
        }

        int revivalPrice = deadDog.getRevivalPrice();
        if (!plugin.revivalUsingPlayerExp) {
            if (MyDog.getEconomy() != null) {
                if (!MyDog.getEconomy().has(player, revivalPrice)) {
                    sender.sendMessage(ChatColor.RED + "You don't have enough funds to resurrect the dog.");
                    return false;
                }
                MyDog.getEconomy().withdrawPlayer(player, revivalPrice);
            }
        } else {
            if (player.getLevel() < revivalPrice) {
                sender.sendMessage(ChatColor.RED + "You don't have enough power to resurrect the dog. You need " + ChatColor.GOLD + (revivalPrice - player.getLevel()) + ChatColor.RED + " more levels!");
                return false;
            }
            player.setLevel(player.getLevel() - revivalPrice);
        }

        Wolf wolf = player.getWorld().spawn(player.getLocation(), Wolf.class);
        wolf.setOwner(player);
        wolf.setSitting(true);
        wolf.setTamed(true);

        deadDog.setUUID(wolf.getUniqueId());
        deadDog.setDead(false);
        deadDog.updateWolf();

        sender.sendMessage(ChatColor.GREEN + "Your dog has been resurrected.");
        return true;
    }

    private boolean commandDogComehere(CommandSender sender, int dogIdentifier) {
        List<Dog> dogs = new ArrayList<>();

        if (dogIdentifier == -1) {
            dogs = MyDog.getDogManager().getAliveDogs(((Player) sender).getUniqueId());
        } else {
            dogs.add(MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId()));
        }

        for (Dog dog : dogs) {
            if (dog == null) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " +
                        ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
                return false;
            }

            Wolf wolf = null;

            Location dogLocation = dog.getDogLocation();
            boolean useLocation = false;
            if (dogLocation == null) {
                wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
                if (wolf == null) {
                    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET +
                            ChatColor.RED + "Aww bonkers! Seems like your Dog cannot be found... Sorry about that!");
                    return false;
                }
            } else {
                if (dogLocation.getChunk().load(false)) {
                    plugin.logDebug("Loaded the chunk sucessfully, no generate!");
                } else if (dogLocation.getChunk().load(true)) {
                    plugin.logDebug("Loaded the chunk sucessfully, generated!");
                } else {
                    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET +
                            ChatColor.RED + "Aww bonkers! Seems like your Dog is at a location that cannot be loaded right now!");
                    return false;
                }
                useLocation = true;
            }

            wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());

            if (wolf == null) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET +
                        ChatColor.RED + "Aww bonkers! Seems like your Dog cannot be found...");
                plugin.logDebug("Could not find Dog, even though chunks should be loaded...");
                return false;
            }

            Location playerLoc = ((Player) sender).getLocation();
            wolf.teleport(playerLoc);
            wolf.setSitting(false);

            String comehereString = plugin.commandComehereString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', comehereString));
		/*sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET +
				ChatColor.GOLD + "Come here! Good doggo, " + dog.getDogName() + "!");*/

            dog.updateWolf();

            if (useLocation) {
                dogLocation.getChunk().unload(true);
                plugin.logDebug("Unloaded the chunk sucessfully!");
            }
        }

        return true;
    }

    private boolean commandTradeAccept(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = ((Player) sender);

        DogManager dogManager = MyDog.getDogManager();
        if (!dogManager.hasTrade(player.getUniqueId())) {
            sender.sendMessage(ChatColor.GOLD + "You don't have any pending trades!");
            return false;
        }

        double tradePrice = dogManager.getTradePrice(player.getUniqueId());
        if (tradePrice > 0.0D) {
            if (MyDog.getEconomy() == null) {
                sender.sendMessage(ChatColor.GOLD + "No economy provider found, you cannot trade dogs for cash!");
                return false;
            }

            if (!MyDog.getEconomy().has(player, tradePrice)) {
                sender.sendMessage(ChatColor.GOLD + "You don't have enough money to trade for that dog!");
                return false;
            }
        }

        if (!dogManager.canTameMoreDogs(player)) {
            sender.sendMessage(ChatColor.GOLD + "You cannot own any more dogs!");
            return false;
        }

        Dog dog = dogManager.getTradeDog(player.getUniqueId());
        Wolf wolf = null;

        if (dog != null) {
            wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
        }

        if (wolf == null || !wolf.isValid() || wolf.isDead()) {
            sender.sendMessage(ChatColor.GOLD + "The dog you are trying to accept cannot be found! Is it dead?");
            return false;
        }

        dogManager.acceptTrade(player);
        sender.sendMessage(ChatColor.GOLD + "You successfully accepted the trade request!");
        Player tradeSender = plugin.getServer().getPlayer(dog.getOwnerId());
        if (tradeSender != null) {
            tradeSender.sendMessage(ChatColor.GOLD + "Dog trade request got accepted by recipient!");
        }
        return true;
    }

    private boolean commandTradeDeny(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = ((Player) sender);

        DogManager dogManager = MyDog.getDogManager();
        if (!dogManager.hasTrade(player.getUniqueId())) {
            sender.sendMessage(ChatColor.GOLD + "You don't have any pending trades!");
            return false;
        }

        Dog dog = dogManager.getTradeDog(player.getUniqueId());

        dogManager.denyTrade(player);
        sender.sendMessage(ChatColor.GOLD + "You successfully denied the trade request!");
        if (dog != null) {
            Player tradeSender = plugin.getServer().getPlayer(dog.getOwnerId());
            if (tradeSender != null) {
                tradeSender.sendMessage(ChatColor.GOLD + "Dog trade request got denied by recipient!");
            }
        }
        return true;
    }

    private boolean commandTrade(CommandSender sender, int dogIdentifier, Player receiver, double price) {
        DogManager dogManager = MyDog.getDogManager();

        Dog dog = dogManager.getDog(dogIdentifier, ((Player) sender).getUniqueId());
        if (dog == null) {
            sender.sendMessage(ChatColor.GOLD + "Could not find a dog with this ID!");
            return false;
        }
        if (dog.isDead()) {
            sender.sendMessage(ChatColor.GOLD + "You cannot trade dead dogs!");
            return false;
        }

        if (price > 0.0D) {
            if (MyDog.getEconomy() == null) {
                sender.sendMessage(ChatColor.GOLD + "No economy provider found, you cannot trade dogs for cash!");
                return false;
            }

            if (!MyDog.getEconomy().has(receiver, price)) {
                sender.sendMessage(ChatColor.GOLD + "It doesn't look like the receiver will be able to.. afford that..");
                return false;
            }
        }

        if (dogManager.hasTrade(receiver.getUniqueId())) {
            sender.sendMessage(ChatColor.GOLD + "The receiver already has a trade pending!");
            return false;
        }

        if (!receiver.isOp() && !MyDog.getPermissionsManager().hasPermission(receiver, "mydog.trade")) {
            sender.sendMessage(ChatColor.GOLD + "The receiver doesn't have permissions to trade!");
            return false;
        }

        if (!dogManager.canTameMoreDogs(receiver)) {
            sender.sendMessage(ChatColor.GOLD + "The receiver has reached their dog limit!");
            return false;
        }

        if (dogManager.handleNewTrade(dog, receiver, price)) {
            DecimalFormat df = new DecimalFormat("#.#");
            receiver.sendMessage(ChatColor.GOLD + "You have received a dog trade request from " + ChatColor.AQUA +
                    ((Player) sender).getDisplayName() + ChatColor.GOLD + "!\n" + ChatColor.AQUA +
                    ((Player) sender).getDisplayName() + ChatColor.GOLD + " is offering " + dog.getDogColor() +
                    dog.getDogName() + ChatColor.GOLD + (plugin.useLevels ? (" (" + ChatColor.AQUA + "Level " +
                    dog.getLevel() + ChatColor.GOLD + ")") : "") + " for " + ChatColor.AQUA +
                    ((price > 0.0D ? (df.format(price) + "$") : "free")) + ChatColor.GOLD + "!\n\n" +
                    ChatColor.GOLD + "Accept the trade request with " + ChatColor.AQUA + "/md tradeaccept\n" +
                    ChatColor.GOLD + "Decline the trade request with " + ChatColor.AQUA + "/md tradedecline\n" +
                    ChatColor.GOLD + "Request expires in 30 seconds!");
            sender.sendMessage(ChatColor.GOLD + "Trade request successfully sent!");
        }
        return true;
    }

    private boolean commandEditLevel(CommandSender sender, int dogIdentifier, int dogLevel) {
        Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
        if (dog == null) {
            sender.sendMessage(ChatColor.GOLD + "Could not find a dog with this ID!");
            return false;
        }
        if (dogLevel < 1 || dogLevel > 100) {
            sender.sendMessage(ChatColor.GOLD + "Level must be between 0 and 100!");
        }
        dog.setLevel(dogLevel);
        dog.updateWolf();
        MyDog.getDogManager().handleNewLevel(dog);
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (sender == null || alias == null || args == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 1 && (cmd.getName().equalsIgnoreCase("mydog") || cmd.getName().equalsIgnoreCase("dog") || cmd.getName().equalsIgnoreCase("dogs") || cmd.getName().equalsIgnoreCase("md"))) {
            List<String> arg1 = new ArrayList<String>();
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.help"))) {
                arg1.add("help");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.reload"))) {
                arg1.add("reload");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.save"))) {
                arg1.add("save");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.list"))) {
                arg1.add("list");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.dogs"))) {
                arg1.add("dogs");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.putdown"))) {
                arg1.add("putdown");
                arg1.add("kill");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.comehere"))) {
                arg1.add("comehere");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.stats"))) {
                arg1.add("info");
                arg1.add("stats");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.free"))) {
                arg1.add("free");
                arg1.add("release");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.rename"))) {
                arg1.add("rename");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.setid"))) {
                arg1.add("setid");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.sit"))) {
                arg1.add("sit");
                arg1.add("stand");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.togglemode"))) {
                arg1.add("attack");
                arg1.add("defend");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.trade"))) {
                arg1.add("trade");
                arg1.add("tradeaccept");
                arg1.add("tradedecline");
                arg1.add("tradedeny");
            }
            if (plugin.allowRevival && (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.dead")))) {
                arg1.add("dead");
            }
            if (plugin.allowRevival && (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.revive")))) {
                arg1.add("revive");
            }
            if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.editlevel"))) {
                arg1.add("setlevel");
            }
            Iterable<String> FIRST_ARGUMENTS = arg1;
            StringUtil.copyPartialMatches(args[0], FIRST_ARGUMENTS, result);
        } else if (args.length == 2) {
            List<String> arg2 = new ArrayList<>();

            if (player != null && (args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("release") || args[0].equalsIgnoreCase("free") || args[0].equalsIgnoreCase("setfree") || args[0].equalsIgnoreCase("setid"))) {
                List<Dog> dogs = MyDog.getDogManager().getDogs(player.getUniqueId());
                for (Dog dog : dogs) {
                    arg2.add(Integer.toString(dog.getIdentifier()));
                }
            }

            if (player != null && (args[0].equalsIgnoreCase("putdown") || args[0].equalsIgnoreCase("rename") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("trade"))) {
                List<Dog> dogs = MyDog.getDogManager().getAliveDogs(player.getUniqueId());
                for (Dog dog : dogs) {
                    arg2.add(Integer.toString(dog.getIdentifier()));
                }
            }

            if (player != null && ((plugin.allowRevival && args[0].equalsIgnoreCase("revive")))) {
                List<Dog> dogs = MyDog.getDogManager().getDeadDogs(player.getUniqueId());
                for (Dog dog : dogs) {
                    arg2.add(Integer.toString(dog.getIdentifier()));
                }
            }

            if (player != null && (args[0].equalsIgnoreCase("sit") || args[0].equalsIgnoreCase("stand") || args[0].equalsIgnoreCase("comehere") || args[0].equalsIgnoreCase("attack") || args[0].equalsIgnoreCase("defend"))) {
                arg2.add("all");

                List<Dog> dogs = MyDog.getDogManager().getAliveDogs(player.getUniqueId());
                for (Dog dog : dogs) {
                    arg2.add(Integer.toString(dog.getIdentifier()));
                }
            }

            Iterable<String> SECOND_ARGUMENTS = arg2;
            StringUtil.copyPartialMatches(args[1], SECOND_ARGUMENTS, result);
        } else if (args.length == 3) {
            List<String> arg3 = new ArrayList<String>();

            if (args[0].equalsIgnoreCase("rename")) {
                arg3.add("<name>");
            } else if (args[0].equalsIgnoreCase("setid")) {
                arg3.add("<custom_id>");
            } else if (args[0].equalsIgnoreCase("setlevel")) {
                arg3.add("<level>");
            } else if (args[0].equalsIgnoreCase("trade")) {
                // idk how, uhm, good this might be
                Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
                for (Player playerObj : players) {
                    if (playerObj != null) {
                        arg3.add(playerObj.getName());
                    }
                }
            }

            Iterable<String> THIRD_ARGUMENTS = arg3;
            StringUtil.copyPartialMatches(args[2], THIRD_ARGUMENTS, result);
        } else if (args.length == 4) {
            List<String> arg4 = new ArrayList<String>();

            if (args[0].equalsIgnoreCase("trade")) {
                arg4.add("[price]");
            }

            Iterable<String> FOURTH_ARGUMENTS = arg4;
            StringUtil.copyPartialMatches(args[3], FOURTH_ARGUMENTS, result);
        }

        Collections.sort(result);
        return result;
    }
}