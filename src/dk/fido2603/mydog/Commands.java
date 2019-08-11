package dk.fido2603.mydog;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
			}
			else if ((args.length == 2) && (player != null))
			{
				
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
	
	private void commandList(CommandSender sender)
	{
		
	}

	private void commandHelp(CommandSender sender)
	{
		
	}

	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}
}