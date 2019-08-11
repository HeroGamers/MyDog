package dk.fido2603.mydog;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import dk.fido2603.mydog.utils.ColorUtils;

public class DogManager
{
	private MyDog				plugin				= null;
	private FileConfiguration	dogsConfig			= null;
	private File				dogsConfigFile		= null;
	private Random				random				= new Random();

	DogManager(MyDog plugin)
	{
		this.plugin = plugin;
	}

	public class Dog
	{
		UUID dogId;
		UUID dogOwnerId;
		String dogName;
		Integer level;
		Date birthday;
		Location location;
		DyeColor collarColor;
		ChatColor nameColor;

		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		// For new dogs
		public Dog(Wolf dog, Player dogOwner)
		{
			this.dogId = dog.getUniqueId();

			this.dogOwnerId = dogOwner.getUniqueId();
			dogsConfig.set(dogId.toString() + ".Owner", dogOwnerId.toString());

			this.dogName = newDogName();
			dogsConfig.set(dogId.toString() + ".Name", dogName);

			if (plugin.randomCollarColor)
			{
				dog.setCollarColor(ColorUtils.randomDyeColor());
			}
			this.collarColor = dog.getCollarColor();
			this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);
			dogsConfig.set(dogId.toString() + ".NameChatColor", nameColor.name());

			this.location = dog.getLocation();
			dogsConfig.set(dogId.toString() + ".LastSeen", location.toString());

			if (plugin.useLevels)
			{
				this.level = 1;
				dogsConfig.set(dogId.toString() + ".Level", level);
			}

			this.birthday = new Date();
			dogsConfig.set(dogId.toString() + ".Birthday", formatter.format(birthday));
		}

		// For old, already created, dogs
		public Dog(Wolf dog)
		{
			if (dogsConfig.contains(dog.getUniqueId().toString()))
			{
				this.dogId = dog.getUniqueId();
				this.dogOwnerId = dog.getOwner().getUniqueId();
				this.birthday = getBirthday();
				this.level = getLevel();
				this.dogName = getDogName();
				this.nameColor = getDogColor();
			}
		}

		public String getDogName()
		{
			if (dogName != null)
			{
				return dogName;
			}

			return dogsConfig.getString(dogId.toString() + ".Name", "UNKNOWN DOGNAME");
		}

		public Wolf getWolf()
		{
			if (!plugin.getServer().getEntity(dogId).isValid() || !(plugin.getServer().getEntity(dogId) instanceof Wolf))
			{
				return null;
			}
			
			return (Wolf) plugin.getServer().getEntity(dogId);
		}

		public Date getBirthday()
		{
			if (birthday != null)
			{
				return birthday;
			}

			try
			{
				return formatter.parse(dogsConfig.getString(dogId.toString() + ".Birthday"));
			}
			catch (ParseException e)
			{
				e.printStackTrace();
				return null;
			}
		}

		public ChatColor getDogColor()
		{
			if (nameColor != null)
			{
				return nameColor;
			}

			if (!plugin.getServer().getEntity(dogId).isValid())
			{
				if (dogsConfig.getString(dogId.toString() + ".NameChatColor") != null)
				{
					return ChatColor.valueOf(dogsConfig.getString(dogId.toString() + ".NameChatColor"));
				}
				return ChatColor.WHITE;
			}

			return ColorUtils.getChatColorFromDyeColor(((Wolf) plugin.getServer().getEntity(dogId)).getCollarColor());
		}

		public Location getDogLocation()
		{
			if (!plugin.getServer().getEntity(dogId).isValid())
			{
				return null;
			}

			this.location = plugin.getServer().getEntity(dogId).getLocation();
			dogsConfig.set(dogId.toString() + ".LastSeen", location.toString());
			
			return location;
		}

		public Location getLastDogLocation()
		{
			return location;
		}

		public UUID getDogId()
		{
			return dogId;
		}

		public UUID getOwnerId()
		{
			return dogOwnerId;
		}

		public Integer getLevel()
		{
			if (level != null)
			{
				return level;
			}

			return dogsConfig.getInt(dogId.toString() + ".Level", 0);
		}

		public boolean setDogCustomName()
		{
			plugin.logDebug("Setting custom name...");
			if (!plugin.getServer().getEntity(dogId).isValid() || !(plugin.getServer().getEntity(dogId) instanceof Wolf))
			{
				plugin.logDebug("Retuning false!");
				return false;
			}

			Wolf dog = (Wolf) plugin.getServer().getEntity(dogId);

			if (dogsConfig.contains(dogId.toString()))
			{
				plugin.logDebug("Setting customName to: " + nameColor + dogName);
				dog.setCustomName(nameColor + dogName);
				dog.setCustomNameVisible(true);
				plugin.logDebug("Returning true!");
				return true;
			}
			plugin.logDebug("Retuning false!");
			return false;
		}
	}

	public void load()
	{
		if (this.dogsConfigFile == null)
		{
			this.dogsConfigFile = new File(this.plugin.getDataFolder(), "dogs.yml");
		}
		this.dogsConfig = YamlConfiguration.loadConfiguration(this.dogsConfigFile);
		if (this.dogsConfig == null)
		{
			this.plugin.log("Error loading dogs.yml! This plugin will NOT work.");
			return;
		}
		this.plugin.log("Loaded " + this.dogsConfig.getKeys(false).size() + " dogs.");
	}

	public void save()
	{
		if ((this.dogsConfig == null) || (this.dogsConfigFile == null))
		{
			return;
		}
		try
		{
			this.dogsConfig.save(this.dogsConfigFile);
		}
		catch (Exception ex)
		{
			this.plugin.log("Could not save config to " + this.dogsConfigFile + ": " + ex.getMessage());
		}
	}

	public boolean isDog(UUID dogId)
	{
		if (dogsConfig.contains(dogId.toString())) { return true; }
		return false;
	}

	public void removeDog(UUID dogId)
	{
		if (dogsConfig.contains(dogId.toString()))
		{
			dogsConfig.set(dogId.toString(), null);
		}
	}

	public Dog newDog(Wolf dog, Player dogOwner) {
		return new Dog(dog, dogOwner);
	}

	public Dog getDog(UUID dogId)
	{
		if (dogsConfig.contains(dogId.toString()))
		{
			return new Dog((Wolf) plugin.getServer().getEntity(dogId));
		}
		return null;
	}

	public String newDogName()
	{
		int dogNameNumber = random.nextInt(plugin.dogNames.size());
		return plugin.dogNames.get(dogNameNumber);
	}
}