package dk.fido2603.mydog;

import java.text.DecimalFormat;
import java.util.*;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.util.StringUtil;

import dk.fido2603.mydog.DogManager.Dog;
import dk.fido2603.mydog.LevelFactory.Level;

public class Commands
{
	private MyDog	plugin = null;

	Commands(MyDog p)
	{
		this.plugin = p;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player = null;

		if ((sender instanceof Player))
		{
			player = (Player) sender;
		}


		if ((cmd.getName().equalsIgnoreCase("mydog")) || (cmd.getName().equalsIgnoreCase("md")) || (cmd.getName().equalsIgnoreCase("dog")) || (cmd.getName().equalsIgnoreCase("dogs")))
		{
			if ((args.length == 0) && (player != null))
			{
				commandHelp(sender);
				return true;
			}
			else if (args.length == 1)
			{
				if (args[0].equalsIgnoreCase("reload"))
				{
					if (player == null) {
						plugin.reloadSettings();
						this.plugin.log("Reloaded the configurations.");

						return true;
					}
					
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.reload")))
					{
						return false;
					}

					this.plugin.reloadSettings();
					sender.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ":" + ChatColor.AQUA + " Reloaded the configurations.");
					return true;
				}
				if (args[0].equalsIgnoreCase("save"))
				{
					if (player == null) {
						plugin.saveSettings();
						this.plugin.log("Saved the configurations.");

						return true;
					}
					
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.save")))
					{
						return false;
					}

					this.plugin.saveSettings();
					sender.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ":" + ChatColor.AQUA + " Saved the configurations.");
					return true;
				}
				if ((args[0].equalsIgnoreCase("help")) && (player != null))
				{
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.help")))
					{
						return false;
					}

					commandList(sender);

					return true;
				}
				if (((args[0].equalsIgnoreCase("dogs")) || (args[0].equalsIgnoreCase("list"))) && (player != null))
				{
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.dogs")))
					{
						return false;
					}

					commandDogList(sender);

					return true;
				}
			}
			else if ((args.length == 2) && (player != null))
			{
				if ((args[0].equalsIgnoreCase("putdown")) || (args[0].equalsIgnoreCase("kill")))
				{
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.putdown")))
					{
						return false;
					}

					commandDogPutdown(sender, args[1]);

					return true;
				}
				if ((args[0].equalsIgnoreCase("free")) || (args[0].equalsIgnoreCase("setfree")))
				{
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.free")))
					{
						return false;
					}

					commandDogFree(sender, args[1]);

					return true;
				}
				if ((args[0].equalsIgnoreCase("stats")) || (args[0].equalsIgnoreCase("info")))
				{
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.stats")))
					{
						return false;
					}

					commandDogStats(sender, args[1]);

					return true;
				}
				if ((args[0].equalsIgnoreCase("comehere")))
				{
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.stats")))
					{
						return false;
					}

					commandDogComehere(sender, args[1]);

					return true;
				}
			}
			else if ((args.length >= 3) && (player != null))
			{
				if ((args[0].equalsIgnoreCase("setid")) || (args[0].equalsIgnoreCase("changeid")))
				{
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.setid")))
					{
						return false;
					}

					commandDogSetId(sender, args);

					return true;
				}

				if ((args[0].equalsIgnoreCase("rename")))
				{
					if ((!player.isOp()) && (!MyDog.getPermissionsManager().hasPermission(player, "mydog.rename")))
					{
						return false;
					}

					commandDogRename(sender, args);

					return true;
				}
				sender.sendMessage(ChatColor.RED + "Too many arguments! Check /mydog help");
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + "Not a MyDog Command! Check /mydog help");
				return true;
			}
		}
		return true;
	}
	
	private boolean commandHelp(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "------------------ " + plugin.getDescription().getFullName() + " ------------------");
		sender.sendMessage(ChatColor.AQUA + "By Fido2603");
		sender.sendMessage(ChatColor.AQUA + "");
		Integer dogsOwned = MyDog.getDogManager().dogsOwned((Player) sender);
		String dogs = " dogs!";
		if (dogsOwned == 1)
		{
			dogs = " dog!";
		}
		sender.sendMessage(ChatColor.AQUA + "You currently own " + ChatColor.WHITE + dogsOwned + ChatColor.AQUA + dogs);
		sender.sendMessage(ChatColor.AQUA + "");
		sender.sendMessage(ChatColor.AQUA + "Use " + ChatColor.WHITE + "/mydog help" + ChatColor.AQUA + " for a list of commands!");

		return true;
	}

	private boolean commandList(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "------------------ " + this.plugin.getDescription().getFullName() + " ------------------");
		sender.sendMessage(ChatColor.AQUA + "/mydog" + ChatColor.WHITE + " - Basic info");
		if (sender instanceof Player)
		{
			Player player = (Player) sender;

			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.help")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog help" + ChatColor.WHITE + " - This command");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.reload")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog reload" + ChatColor.WHITE + " - Reloads the MyDog system");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.save")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog save" + ChatColor.WHITE + " - Saves the current changes to the MyDog system");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.dogs")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog list" + ChatColor.WHITE + " - View a list with your current Dogs");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.putdown")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog putdown <id>" + ChatColor.WHITE + " - Get rid of a Dog you own");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.free")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog free <id>" + ChatColor.WHITE + " - Set one of your Dogs free!");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.comehere")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog comehere <id>" + ChatColor.WHITE + " - Forces your Dog to teleport to your location");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.stats")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog info <id>" + ChatColor.WHITE + " - Gets stats and other info about a Dog you own");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.rename")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog rename <id> <name>" + ChatColor.WHITE + " - Renames a Dog you own");
			}
			if ((sender.isOp()) || (MyDog.getPermissionsManager().hasPermission(player, "mydog.setid")))
			{
				sender.sendMessage(ChatColor.AQUA + "/mydog setid <id> <newid>" + ChatColor.WHITE + " - Assigns a custom ID to a Dog you own");
			}
		}

		return true;
	}

	private boolean commandDogList(CommandSender sender)
	{
		// Sort the dogs after their ID (identifier)
		TreeMap<Integer, Dog> dogsSorted = new TreeMap();
		for (Dog dog : MyDog.getDogManager().getDogs(((Player) sender).getUniqueId()))
		{
			dogsSorted.put(dog.getIdentifier(), dog);
		}

		sender.sendMessage(ChatColor.YELLOW + "------------------ " + this.plugin.getDescription().getFullName() + " ------------------");
		for (Map.Entry entry : dogsSorted.entrySet())
		{
			Wolf wolf = (Wolf) plugin.getServer().getEntity(((Dog) entry.getValue()).getDogId());
			String healthString = "";
			if (wolf != null)
			{
				double maxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				double health = wolf.getHealth();
				healthString = " " + ChatColor.BLUE + "(HP: " + health + "/" + maxHealth + ")";
			}

			sender.sendMessage(ChatColor.AQUA + "#" + ((Dog) entry.getValue()).getIdentifier() + ChatColor.WHITE + " - " + ChatColor.AQUA + ((Dog) entry.getValue()).getDogName() + healthString);
		}
		return true;
	}

	private boolean commandDogRename(CommandSender sender, String[] args)
	{
		String dogIdentifier = args[1];
		String name = args[2];

		for (int i=3; i<args.length; i++)
		{
			name += " " + args[i];
		}

		Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
		if (dog == null)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
			return false;
		}

		if (name.isEmpty() || name.length() > 16)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please choose a name between 1 and 16 characters for your Dog!");
			return false;
		}

		if (!dog.setDogName(name))
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "An error occured! Could not set Dog name!");
			return false;
		}

		sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + "From now on, I will call you " + dog.getDogColor() + dog.getDogName() + ChatColor.RESET + ChatColor.AQUA + "!");

		return true;
	}

	private boolean commandDogSetId(CommandSender sender, String[] args)
	{
		String dogIdentifier = args[1];
		String new_id = args[2];

		Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
		if (dog == null)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
			return false;
		}

		if (new_id.isEmpty() || new_id.length() > 10)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Please choose an identifier between 1 and 10 figures for your Dog!");
			return false;
		}

		int id;
		try
		{
			id = Integer.parseInt(new_id);
		}
		catch (NumberFormatException e)
		{
			plugin.logDebug("Error while trying to format ID from string: " + e);
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Your Dog's identifier can only consist of numbers!");
			return false;
		}

		if (!MyDog.getDogManager().setNewId(dog, id))
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "An error occured! Could not set new Dog ID!");
			return false;
		}

		sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + "New Dog ID successfully set!");

		return true;
	}

	private boolean commandDogPutdown(CommandSender sender, String dogIdentifier)
	{
		Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
		if (dog == null)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
			return false;
		}

		Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
		
		if (wolf == null)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Aww bonkers! Seems like your Dog cannot be found... Is it loaded?");
			return false;
		}

		sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + "I'm sorry my friend...");
		wolf.damage(wolf.getHealth(), (Player) sender);

		return true;
	}

	private boolean commandDogFree(CommandSender sender, String dogIdentifier)
	{
		Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
		if (dog == null)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
			return false;
		}

		sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.AQUA + "It was great having you here, " + dog.getDogColor() + dog.getDogName() + ChatColor.RESET + ChatColor.AQUA + "...");

		Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
		
		if (wolf != null)
		{
			wolf.setCustomName("");
			wolf.setCustomNameVisible(false);
			if (wolf.isSitting())
			{
				wolf.setSitting(false);
			}
			wolf.setTamed(false);
		}

		MyDog.getDogManager().removeDog(dog.getDogId());

		return true;
	}

	private boolean commandDogStats(CommandSender sender, String dogIdentifier)
	{
		Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
		if (dog == null)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
			return false;
		}

		Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
		DecimalFormat df = new DecimalFormat("#.#");

		sender.sendMessage(ChatColor.YELLOW + "------------------ " + this.plugin.getDescription().getFullName() + " ------------------");
		
		sender.sendMessage(ChatColor.AQUA + "Name: " + dog.getDogColor() + dog.getDogName());

		if (plugin.useLevels)
		{
			sender.sendMessage(ChatColor.AQUA + "Level: " + ChatColor.WHITE + dog.getLevel());

			// Calculate and make experience string
			String experienceString = "";
			double exp = dog.getExperience();
			double maxExp = 0;
			
			Map<Integer, Level> levels = plugin.dogLevels;

			for (Integer levelInt : levels.keySet())
			{
				Integer levelExp = levels.get(levelInt).exp;

				// If experience is under the experience needed to level up
				if (exp < levelExp)
				{
					// If there is a level under the current one, check if the exp is over or equals to the value of that levelup
					if (levels.containsKey((levelInt-1)) && exp >= levels.get((levelInt-1)).exp)
					{
						maxExp = levelExp;
						break;
					}
					// Exp is under needed, and there is no level under. Lowest level found. User is at lowest level then

					maxExp = levelExp;
					break;
				}
				else if (exp > levelExp && !levels.containsKey((levelInt+1))) // Highest level
				{
					if (levels.get((levelInt-1)) == null)
					{
						plugin.logDebug("Something went wrong! Last level, there is no level under! Return!");
						return false;
					}
					maxExp = levelExp;
				}
			}

			plugin.logDebug("Exp: " + exp + " - MaxExp: " + maxExp);
			if (maxExp != 0)
			{
				double percent = (exp/maxExp)*100;
				plugin.logDebug("Current percent: " + percent);
				
				experienceString = calculatePercentString(percent) + ChatColor.AQUA + "" + ChatColor.BOLD + " [" + ChatColor.DARK_AQUA + df.format(exp) + 
						ChatColor.AQUA + "" + ChatColor.BOLD + "/" + ChatColor.RESET + ChatColor.AQUA + df.format(maxExp) + ChatColor.AQUA + "" + ChatColor.BOLD + "]";
				sender.sendMessage(ChatColor.AQUA + "Experience: " + experienceString);
			}
		}

		if (wolf != null)
		{
			// Health graphics
			double maxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double health = wolf.getHealth();
			
			double percent = (health/maxHealth)*100;
			
			String healthString = calculatePercentString(percent);

			sender.sendMessage(ChatColor.AQUA + "Health: " + healthString + ChatColor.AQUA + "" + ChatColor.BOLD + " [" + ChatColor.DARK_AQUA + df.format(health) + 
					ChatColor.AQUA + "" + ChatColor.BOLD + "/" + ChatColor.RESET + ChatColor.AQUA + df.format(maxHealth) + ChatColor.AQUA + "" + ChatColor.BOLD + "]");

			sender.sendMessage(ChatColor.AQUA + "Damage: " + ChatColor.WHITE + wolf.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue() + " HP");
		}

		Location dogLoc = dog.getDogLocation();
		if (dogLoc != null)
		{
			sender.sendMessage(ChatColor.AQUA + "Last Seen at: " + ChatColor.DARK_AQUA + "World: " + ChatColor.WHITE + dogLoc.getWorld().getName() + 
					ChatColor.DARK_AQUA + " X: " + ChatColor.WHITE + df.format(dogLoc.getX()) + ChatColor.DARK_AQUA + " Y: " + ChatColor.WHITE + df.format(dogLoc.getY()) + 
					ChatColor.DARK_AQUA + " Z: " + ChatColor.WHITE + df.format(dogLoc.getZ()));
		}

		return true;
	}

	private String calculatePercentString(double percent)
	{
		String percentString = "==========";

		if (percent >= 10.0 && percent < 19.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=" + ChatColor.AQUA + "=========";
		}
		else if (percent >= 20.0 && percent < 29.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "==" + ChatColor.AQUA + "========";
		}
		else if (percent >= 30.0 && percent < 39.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "===" + ChatColor.AQUA + "=======";
		}
		else if (percent >= 40.0 && percent < 49.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "====" + ChatColor.AQUA + "=====";
		}
		else if (percent >= 50.0 && percent < 59.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=====" + ChatColor.AQUA + "=====";
		}
		else if (percent >= 60.0 && percent < 69.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "======" + ChatColor.AQUA + "====";
		}
		else if (percent >= 70.0 && percent < 79.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=======" + ChatColor.AQUA + "===";
		}
		else if (percent >= 80.0 && percent < 89.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "========" + ChatColor.AQUA + "==";
		}
		else if (percent >= 90.0 && percent < 99.9)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=========" + ChatColor.AQUA + "=";
		}
		else if (percent >= 100.0)
		{
			percentString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "==========";
		}

		return ChatColor.AQUA + "" + ChatColor.BOLD + "[" + percentString + ChatColor.AQUA + "" + ChatColor.BOLD + "]";
	}

	private boolean commandDogComehere(CommandSender sender, String dogIdentifier)
	{
		Dog dog = MyDog.getDogManager().getDog(dogIdentifier, ((Player) sender).getUniqueId());
		if (dog == null)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + 
					ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
			return false;
		}

		Wolf wolf = null;

		Location dogLocation = dog.getDogLocation();
		Boolean useLocation = false;
		if (dogLocation == null)
		{
			wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
			if (wolf == null)
			{
				sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + 
						ChatColor.RED + "Aww bonkers! Seems like your Dog cannot be found... Sorry about that!");
				return false;
			}
		}
		else
		{
			if (dogLocation.getChunk().load(false))
			{
				plugin.logDebug("Loaded the chunk sucessfully, no generate!");
			}
			else if (dogLocation.getChunk().load(true))
			{
				plugin.logDebug("Loaded the chunk sucessfully, generated!");
			}
			else
			{
				sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + 
						ChatColor.RED + "Aww bonkers! Seems like your Dog is at a location that cannot be loaded right now!");
				return false;
			}
			useLocation = true;
		}

		wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
		
		if (wolf == null)
		{
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

		if (useLocation)
		{
			dogLocation.getChunk().unload(true);
			plugin.logDebug("Unloaded the chunk sucessfully!");
		}

		return true;
	}

	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		Validate.notNull(sender, "Sender cannot be null");
		Validate.notNull(args, "Arguments cannot be null");
		Validate.notNull(alias, "Alias cannot be null");

		List<String> result = new ArrayList<String>();

		Player player = null;
		if (sender instanceof Player)
		{
			player = (Player) sender;
		}

		if (args.length == 1 && (cmd.getName().equalsIgnoreCase("mydog") || cmd.getName().equalsIgnoreCase("dog") || cmd.getName().equalsIgnoreCase("dogs") || cmd.getName().equalsIgnoreCase("md")))
		{
			List<String> arg1 = new ArrayList<String>();
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.help")))
			{
				arg1.add("help");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.reload")))
			{
				arg1.add("reload");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.save")))
			{
				arg1.add("save");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.dogs")))
			{
				arg1.add("dogs");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.putdown")))
			{
				arg1.add("putdown");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.comehere")))
			{
				arg1.add("comehere");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.stats")))
			{
				arg1.add("info");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.free")))
			{
				arg1.add("free");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.rename")))
			{
				arg1.add("rename");
			}
			if (player == null || (player.isOp() || MyDog.getPermissionsManager().hasPermission(player, "mydog.setid")))
			{
				arg1.add("setid");
			}
			Iterable<String> FIRST_ARGUMENTS = arg1;
			StringUtil.copyPartialMatches(args[0], FIRST_ARGUMENTS, result);
		}
		else if (args.length == 2)
		{
			List<String> arg2 = new ArrayList<String>();

			if (player != null && (args[0].equalsIgnoreCase("putdown") || args[0].equalsIgnoreCase("comehere") || args[0].equalsIgnoreCase("free") || args[0].equalsIgnoreCase("setfree") || args[0].equalsIgnoreCase("rename") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("setid")  || args[0].equalsIgnoreCase("stats")))
			{
				List<Dog> dogs = MyDog.getDogManager().getDogs(player.getUniqueId());
				for (Dog dog : dogs)
				{
					arg2.add(dog.getIdentifier().toString());
				}
			}

			Iterable<String> SECOND_ARGUMENTS = arg2;
			StringUtil.copyPartialMatches(args[1], SECOND_ARGUMENTS, result);
		}
		else if (args.length == 3)
		{
			List<String> arg3 = new ArrayList<String>();

			if (args[0].equalsIgnoreCase("rename"))
			{
				arg3.add("<name>");
			}
			else if (args[0].equalsIgnoreCase("setid"))
			{
				arg3.add("<custom_id>");
			}

			Iterable<String> SECOND_ARGUMENTS = arg3;
			StringUtil.copyPartialMatches(args[2], SECOND_ARGUMENTS, result);
		}

		Collections.sort(result);
		return result;
	}
}