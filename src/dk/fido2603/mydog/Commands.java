package dk.fido2603.mydog;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.DogManager.Dog;

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
					
					if ((!player.isOp()) && (!player.hasPermission("mydog.reload")))
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
					
					if ((!player.isOp()) && (!player.hasPermission("mydog.save")))
					{
						return false;
					}

					this.plugin.saveSettings();
					sender.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ":" + ChatColor.AQUA + " Saved the configurations.");
					return true;
				}
				if ((args[0].equalsIgnoreCase("help")) && (player != null))
				{
					if ((!player.isOp()) && (!player.hasPermission("mydog.list")))
					{
						return false;
					}

					commandList(sender);

					return true;
				}
				if (((args[0].equalsIgnoreCase("dogs")) || (args[0].equalsIgnoreCase("list"))) && (player != null))
				{
					if ((!player.isOp()) && (!player.hasPermission("mydog.dogs")))
					{
						return false;
					}

					commandDogList(sender);

					return true;
				}
			}
			else if ((args.length == 2) && (player != null))
			{
				if ((args[0].equalsIgnoreCase("putdown")) && (player != null))
				{
					if ((!player.isOp()) && (!player.hasPermission("mydog.putdown")))
					{
						return false;
					}

					commandDogPutdown(sender, args[1]);

					return true;
				}
				if (((args[0].equalsIgnoreCase("stats")) || (args[0].equalsIgnoreCase("info"))) && (player != null))
				{
					if ((!player.isOp()) && (!player.hasPermission("mydog.stats")))
					{
						return false;
					}

					commandDogStats(sender, args[1]);

					return true;
				}
			}
			else if (player != null)
			{
				if (args.length > 3)
				{
					sender.sendMessage(ChatColor.RED + "Too many arguments! Check /mydog help");
					return true;
				}
			}
		}
		return true;
	}
	
	private boolean commandHelp(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "------------------ " + plugin.getDescription().getFullName() + " ------------------");
		sender.sendMessage(ChatColor.AQUA + "By Fido2603");
		sender.sendMessage(ChatColor.AQUA + "");
		sender.sendMessage(ChatColor.AQUA + "You currently have " + ChatColor.WHITE + MyDog.getDogManager().dogsOwned((Player) sender) + ChatColor.AQUA + " dogs!");
		sender.sendMessage(ChatColor.AQUA + "");
		sender.sendMessage(ChatColor.AQUA + "Use " + ChatColor.WHITE + "/mydog help" + ChatColor.AQUA + " for a list of commands!");

		return true;
	}

	private boolean commandList(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "------------------ " + this.plugin.getDescription().getFullName() + " ------------------");
		sender.sendMessage(ChatColor.AQUA + "/mydog" + ChatColor.WHITE + " - Basic info");
		if ((sender.isOp()) || (sender.hasPermission("mydog.list")))
		{
			sender.sendMessage(ChatColor.AQUA + "/mydog help" + ChatColor.WHITE + " - This command");
		}
		if ((sender.isOp()) || (sender.hasPermission("mydog.reload")))
		{
			sender.sendMessage(ChatColor.AQUA + "/mydog reload" + ChatColor.WHITE + " - Reloads the MyDog system");
		}
		if ((sender.isOp()) || (sender.hasPermission("mydog.save")))
		{
			sender.sendMessage(ChatColor.AQUA + "/mydog save" + ChatColor.WHITE + " - Saves the current changes to the MyDog system");
		}
		if ((sender.isOp()) || (sender.hasPermission("mydog.dogs")))
		{
			sender.sendMessage(ChatColor.AQUA + "/mydog dogs" + ChatColor.WHITE + " - View a list with your current Dogs");
		}
		if ((sender.isOp()) || (sender.hasPermission("mydog.putdown")))
		{
			sender.sendMessage(ChatColor.AQUA + "/mydog putdown <id>" + ChatColor.WHITE + " - Kill your Dog");
		}
		if ((sender.isOp()) || (sender.hasPermission("mydog.stats")))
		{
			sender.sendMessage(ChatColor.AQUA + "/mydog info <id>" + ChatColor.WHITE + " - Gets stats about a Dog");
		}

		return true;
	}

	private boolean commandDogList(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "------------------ " + this.plugin.getDescription().getFullName() + " ------------------");
		for (Dog dog : MyDog.getDogManager().getDogs(((Player) sender).getUniqueId()))
		{
			Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
			String healthString = "";
			if (wolf != null)
			{
				double maxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				double health = wolf.getHealth();
				healthString = " " + ChatColor.BLUE + "(HP: " + health + "/" + maxHealth + ")";
			}

			sender.sendMessage(ChatColor.AQUA + "#" + dog.getIdentifier() + ChatColor.WHITE + " - " + ChatColor.AQUA + dog.getDogName() + healthString);
		}
		return true;
	}

	private boolean commandDogPutdown(CommandSender sender, String dogIdentifier)
	{
		Dog dog = MyDog.getDogManager().getDog(dogIdentifier);
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

	private boolean commandDogStats(CommandSender sender, String dogIdentifier)
	{
		Dog dog = MyDog.getDogManager().getDog(dogIdentifier);
		if (dog == null)
		{
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Could not find a Dog with that ID! Check /mydog dogs");
			return false;
		}

		Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());

		sender.sendMessage(ChatColor.YELLOW + "------------------ " + this.plugin.getDescription().getFullName() + " ------------------");
		
		sender.sendMessage(ChatColor.AQUA + "Name: " + dog.getDogColor() + dog.getDogName());
		sender.sendMessage(ChatColor.AQUA + "Level: " + ChatColor.WHITE + dog.getLevel());

		if (wolf != null)
		{
			// Health graphics
			double maxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double health = wolf.getHealth();
			
			double percent = (health/maxHealth)*100;
			String healthString = "==========";

			if (percent > 0 && percent <= 10)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=" + ChatColor.AQUA + "=========";
			}
			else if (percent > 10 && percent <= 20)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "==" + ChatColor.AQUA + "========";
			}
			else if (percent > 20 && percent <= 30)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "===" + ChatColor.AQUA + "=======";
			}
			else if (percent > 30 && percent <= 40)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "====" + ChatColor.AQUA + "=====";
			}
			else if (percent > 40 && percent <= 50)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=====" + ChatColor.AQUA + "=====";
			}
			else if (percent > 50 && percent <= 60)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "======" + ChatColor.AQUA + "====";
			}
			else if (percent > 60 && percent <= 70)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=======" + ChatColor.AQUA + "===";
			}
			else if (percent > 70 && percent <= 80)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "========" + ChatColor.AQUA + "==";
			}
			else if (percent > 80 && percent <= 90)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=========" + ChatColor.AQUA + "=";
			}
			else if (percent > 90 && percent <= 100)
			{
				healthString = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "==========";
			}

			sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[" + healthString + ChatColor.AQUA + "" + ChatColor.BOLD + "] [" + ChatColor.DARK_AQUA + health + ChatColor.AQUA + "" + ChatColor.BOLD + "/" + ChatColor.RESET + ChatColor.AQUA + maxHealth + ChatColor.AQUA + "" + ChatColor.BOLD + "]");

			sender.sendMessage(ChatColor.AQUA + "Damage: " + ChatColor.WHITE + wolf.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue() + " HP");
		}

		Location dogLoc = dog.getDogLocation();
		if (dogLoc != null)
		{
			sender.sendMessage(ChatColor.AQUA + "Last Seen at: " + ChatColor.WHITE + "World: " + dogLoc.getWorld().getName() + " - X: " + dogLoc.getX() + " Y: " + dogLoc.getY() + " Z: " + dogLoc.getZ());
		}

		return true;
	}

	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}
}