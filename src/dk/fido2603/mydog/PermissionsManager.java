package dk.fido2603.mydog;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class PermissionsManager
{
	private String		pluginName		= "null";
	private MyDog		plugin;
	private Permission	vaultPermission	= null;
	private static Chat	vaultChat		= null;

	public PermissionsManager(MyDog p)
	{
		this.plugin = p;

		if (p.vaultEnabled)
		{
			RegisteredServiceProvider<Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(Permission.class);
			vaultPermission = permissionProvider.getProvider();

			RegisteredServiceProvider<Chat> chatProvider = plugin.getServer().getServicesManager().getRegistration(Chat.class);
			vaultChat = chatProvider.getProvider();
		}
	}

	public void load()
	{
		// Nothing
	}

	public Plugin getPlugin()
	{
		return this.plugin;
	}

	public String getPermissionPluginName()
	{
		return this.pluginName;
	}

	public boolean hasPermission(Player player, String node)
	{
		this.plugin.logDebug("Checking for perm.. Player: " + player.getName() + " - Node: " + node);
		if (this.plugin.vaultEnabled)
		{
			this.plugin.logDebug("Vault is enabled, checking there...");
			return vaultPermission.has(player, node);
		}
		else
		{
			// We check the permissions the other way, using the defaults given
			// through plugin.yml
			this.plugin.logDebug("No vault. Checking the old way..");
			List<org.bukkit.permissions.Permission> permissions = this.plugin.getDescription().getPermissions();

			for (org.bukkit.permissions.Permission permission : permissions)
			{
				//this.plugin.logDebug("Current permission: " + permission.getName());
				Map<String, Boolean> permissionChildren = permission.getChildren();

				if ((permissionChildren.containsKey(node) && permissionChildren.get(node).booleanValue() == true) && permission.getName().contains(node))
				{
					this.plugin.logDebug("Heyy, it was the correct perm finally... Perm: " + permission.getName() + " - Node: " + node);
					this.plugin.logDebug("Current permissionDefault: " + permission.getDefault().toString());
					if (permission.getDefault() == PermissionDefault.TRUE)
					{
						this.plugin.logDebug("It was default true! Returning true!");
						return true;
					}
					else if (permission.getDefault() == PermissionDefault.OP)
					{
						this.plugin.logDebug("It was for operators!");
						if (player.isOp())
						{
							this.plugin.logDebug("Player is an operator! Returning true!");
							return true;
						}
						this.plugin.logDebug("Player is not an operator!");
					}
					else if (permission.getDefault() == PermissionDefault.NOT_OP)
					{
						this.plugin.logDebug("It was for non-operators!");
						if (!player.isOp())
						{
							this.plugin.logDebug("Player is not an operator! Returning true!");
							return true;
						}
						this.plugin.logDebug("Player is an operator!");
					}
					break;
				}
			}
		}
		this.plugin.logDebug("Returning false!");
		return false;
	}

	public boolean isGroup(String groupName)
	{
		if (this.plugin.vaultEnabled)
		{
			for (String str : vaultPermission.getGroups())
			{
				if (str.contains(groupName))
					return true;
			}
		}
		return false;
	}

	public String getGroup(String playerName)
	{
		if (this.plugin.vaultEnabled)
		{
			return vaultPermission.getPrimaryGroup(plugin.getServer().getPlayer(playerName));
		}
		return "";
	}

	public String getPrefix(String playerName)
	{
		if (this.plugin.vaultEnabled)
		{
			Player player = plugin.getServer().getPlayer(playerName);
			return vaultChat.getPlayerPrefix(player);
		}
		return "";
	}

	public void setGroup(String playerName, String groupName)
	{
		if (this.plugin.vaultEnabled)
		{
			Player player = plugin.getServer().getPlayer(playerName);
			vaultPermission.playerAddGroup(player, groupName);
		}
	}
}