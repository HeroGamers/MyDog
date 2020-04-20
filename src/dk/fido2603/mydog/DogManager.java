package dk.fido2603.mydog;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.LevelFactory.Level;
import dk.fido2603.mydog.utils.ColorUtils;

public class DogManager
{
	private MyDog				plugin				= null;
	private FileConfiguration	dogsConfig			= null;
	private File				dogsConfigFile		= null;
	private Random				random				= new Random();
	private long				lastSaveTime		= 0L;

	DogManager(MyDog plugin)
	{
		this.plugin = plugin;
	}

	public class Dog
	{
		private UUID dogId;
		private UUID dogOwnerId;
		private Integer dogIdentifier;
		private String dogName;
		private Integer level;
		private Integer experience;
		private Date birthday;
		private Location location;
		private DyeColor collarColor;
		private ChatColor nameColor;

		private String pattern = "dd-MM-yyyy HH:mm";
		private DateFormat formatter = new SimpleDateFormat(pattern);

		// For new dogs
		public Dog(Wolf dog, Player dogOwner, Integer dogUID)
		{
			// The UUID of the Dog
			this.dogId = dog.getUniqueId();

			// The UUID of the Dog's owner (Player)
			this.dogOwnerId = dogOwner.getUniqueId();
			dogsConfig.set(dogId.toString() + ".Owner", dogOwnerId.toString());

			// Generate an ID for the Dog
			this.dogIdentifier = dogUID;
			dogsConfig.set(dogId.toString() + ".ID", dogIdentifier);

			// Generate a new name for the Dog
			this.dogName = newDogName();
			dogsConfig.set(dogId.toString() + ".Name", dogName);

			// Generate a random Collar Color and set the Dog's Color
			if (plugin.randomCollarColor)
			{
				dog.setCollarColor(ColorUtils.randomDyeColor());
			}
			this.collarColor = dog.getCollarColor();
			this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);
			dogsConfig.set(dogId.toString() + ".NameChatColor", nameColor.name());

			// Save the Dog's last seen location
			this.location = dog.getLocation();
			dogsConfig.set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
			dogsConfig.set(dogId.toString() + ".LastSeen.X", location.getX());
			dogsConfig.set(dogId.toString() + ".LastSeen.Y", location.getY());
			dogsConfig.set(dogId.toString() + ".LastSeen.Z", location.getZ());

			// Give the Dog a level
			if (plugin.useLevels)
			{
				this.level = 1;
				dogsConfig.set(dogId.toString() + ".Level.Level", level);
				this.experience = 0;
				dogsConfig.set(dogId.toString() + ".Level.Experience", experience);
			}

			// Set the current time as the Dog's birthday
			this.birthday = new Date();
			dogsConfig.set(dogId.toString() + ".Birthday", formatter.format(birthday));

			saveTimed();
		}

		// For new, already-tamed dogs with old data
		public Dog(Wolf dog, Player dogOwner, String customName, DyeColor collarColorImport, Integer dogUID)
		{
			// The UUID of the Dog
			this.dogId = dog.getUniqueId();

			// The UUID of the Dog's owner (Player)
			this.dogOwnerId = dogOwner.getUniqueId();
			dogsConfig.set(dogId.toString() + ".Owner", dogOwnerId.toString());

			// Generate an ID for the Dog
			this.dogIdentifier = dogUID;
			dogsConfig.set(dogId.toString() + ".ID", dogIdentifier);

			// Generate a new name for the Dog
			if (customName == null || customName.isEmpty())
			{
				this.dogName = newDogName();
			}
			else
			{
				this.dogName = customName;
			}
			dogsConfig.set(dogId.toString() + ".Name", dogName);

			// Generate a random Collar Color and set the Dog's Color
			if (collarColorImport == null)
			{
				if (plugin.randomCollarColor)
				{
					dog.setCollarColor(ColorUtils.randomDyeColor());
				}
			}

			this.collarColor = dog.getCollarColor();
			this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);
			dogsConfig.set(dogId.toString() + ".NameChatColor", nameColor.name());

			// Save the Dog's last seen location
			this.location = dog.getLocation();
			dogsConfig.set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
			dogsConfig.set(dogId.toString() + ".LastSeen.X", location.getX());
			dogsConfig.set(dogId.toString() + ".LastSeen.Y", location.getY());
			dogsConfig.set(dogId.toString() + ".LastSeen.Z", location.getZ());

			// Give the Dog a level
			if (plugin.useLevels)
			{
				this.level = 1;
				dogsConfig.set(dogId.toString() + ".Level.Level", level);
				this.experience = 0;
				dogsConfig.set(dogId.toString() + ".Level.Experience", experience);
			}

			// Set the current time as the Dog's birthday
			this.birthday = new Date();
			dogsConfig.set(dogId.toString() + ".Birthday", formatter.format(birthday));

			saveTimed();
		}

		// For old, already created, dogs
		public Dog(Wolf dog)
		{
			if (dogsConfig.contains(dog.getUniqueId().toString()))
			{
				this.dogId = dog.getUniqueId();
				this.dogOwnerId = dog.getOwner().getUniqueId();
				this.dogIdentifier = getIdentifier();
				this.birthday = getBirthday();
				this.level = getLevel();
				this.experience = getExperience();
				this.dogName = getDogName();
				this.nameColor = getDogColor();
			}
		}

		// For old, already created, dogs
		public Dog(UUID dogUUID, UUID playerUUID)
		{
			if (dogsConfig.contains(dogUUID.toString()))
			{
				this.dogId = dogUUID;
				this.dogOwnerId = playerUUID;
				this.dogIdentifier = getIdentifier();
				this.birthday = getBirthday();
				this.level = getLevel();
				this.experience = getExperience();
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

			if (plugin.getServer().getEntity(dogId) == null || !plugin.getServer().getEntity(dogId).isValid())
			{
				if (dogsConfig.getString(dogId.toString() + ".NameChatColor") != null)
				{
					return ChatColor.valueOf(dogsConfig.getString(dogId.toString() + ".NameChatColor"));
				}
				return ChatColor.WHITE;
			}

			return ColorUtils.getChatColorFromDyeColor(((Wolf) plugin.getServer().getEntity(dogId)).getCollarColor());
		}

		public boolean setDogColor(DyeColor color)
		{
			if (color == null)
			{
				return false;
			}
			if (dogsConfig.contains(dogId.toString()))
			{
				ChatColor chatColor = ColorUtils.getChatColorFromDyeColor(color);

				this.nameColor = chatColor;
				dogsConfig.set(dogId.toString() + ".NameChatColor", nameColor.name());

				Wolf dog = (Wolf) plugin.getServer().getEntity(dogId);

				if (dog == null)
				{
					plugin.logDebug("Dog is null");
					return false;
				}

				if (dogName == null)
				{
					this.dogName = getDogName();
				}


				if (plugin.useLevels && plugin.showLevelsInNametag) {
					plugin.logDebug("Setting customName to: " + nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]");
					dog.setCustomName(nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]");
				}
				else {
					plugin.logDebug("Setting customName to: " + nameColor + dogName);
					dog.setCustomName(nameColor + dogName);
				}

				if (plugin.onlyShowNametagOnHover)
				{
					dog.setCustomNameVisible(false);
				}
				else
				{
					dog.setCustomNameVisible(true);
				}
			}
			saveTimed();
			return true;
		}

		public boolean setDogName(String name)
		{
			if (name == null || name.isEmpty())
			{
				return false;
			}
			if (dogsConfig.contains(dogId.toString()))
			{
				this.dogName = name;
				dogsConfig.set(dogId.toString() + ".Name", dogName);

				Wolf dog = (Wolf) plugin.getServer().getEntity(dogId);

				if (dog == null)
				{
					plugin.logDebug("Dog is null");
					return false;
				}

				if (dogName.isEmpty())
				{
					this.dogName = getDogName();
				}

				if (plugin.useLevels && plugin.showLevelsInNametag) {
					plugin.logDebug("Setting customName to: " + nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]");
					dog.setCustomName(nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]");
				}
				else {
					plugin.logDebug("Setting customName to: " + nameColor + dogName);
					dog.setCustomName(nameColor + dogName);
				}
				if (plugin.onlyShowNametagOnHover)
				{
					dog.setCustomNameVisible(false);
				}
				else
				{
					dog.setCustomNameVisible(true);
				}
			}
			saveTimed();
			return true;
		}

		public Location getDogLocation()
		{
			if (plugin.getServer().getEntity(dogId) == null || !plugin.getServer().getEntity(dogId).isValid())
			{
				if (dogsConfig.getString(dogId.toString() + ".LastSeen.World") != null)
				{
					return new Location(plugin.getServer().getWorld(dogsConfig.getString(dogId.toString() + ".LastSeen.World")), dogsConfig.getInt(dogId.toString() + ".LastSeen.X"), dogsConfig.getInt(dogId.toString() + ".LastSeen.Y"), dogsConfig.getInt(dogId.toString() + ".LastSeen.Z"));
				}
				return null;
			}

			this.location = plugin.getServer().getEntity(dogId).getLocation();
			dogsConfig.set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
			dogsConfig.set(dogId.toString() + ".LastSeen.X", location.getX());
			dogsConfig.set(dogId.toString() + ".LastSeen.Y", location.getY());
			dogsConfig.set(dogId.toString() + ".LastSeen.Z", location.getZ());

			saveTimed();
			return location;
		}

		public boolean saveDogLocation()
		{
			if (plugin.getServer().getEntity(dogId) == null || !plugin.getServer().getEntity(dogId).isValid())
			{
				return false;
			}

			this.location = plugin.getServer().getEntity(dogId).getLocation();
			dogsConfig.set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
			dogsConfig.set(dogId.toString() + ".LastSeen.X", location.getX());
			dogsConfig.set(dogId.toString() + ".LastSeen.Y", location.getY());
			dogsConfig.set(dogId.toString() + ".LastSeen.Z", location.getZ());

			saveTimed();
			return true;
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

			return dogsConfig.getInt(dogId.toString() + ".Level.Level", 1);
		}

		public void setLevel(Integer lvl)
		{
			if (!level.equals(lvl))
			{
				this.level = lvl;
			}

			dogsConfig.set(dogId.toString() + ".Level.Level", lvl);
		}

		public Integer getExperience()
		{
			if (experience != null)
			{
				return experience;
			}

			return dogsConfig.getInt(dogId.toString() + ".Level.Experience", 0);
		}

		public void giveExperience(Integer exp)
		{
			Integer currentExp = getExperience();
			setExperience(currentExp + exp);
		}

		public void setExperience(Integer exp)
		{
			this.experience = exp;
			dogsConfig.set(dogId.toString() + ".Level.Experience", exp);

			Integer levelBefore = level;
			Integer newLevel = 1;
			Map<Integer, Level> levels = plugin.dogLevels;

			for (Integer levelInt : levels.keySet())
			{
				Integer levelExp = levels.get(levelInt).exp;
				// Amount of exp must be higher or equals to exp to level
				// The level must be higher than the level the Dog had before
				// The new level variable must be smaller than the level checking against
				if (exp >= levelExp && levelInt > levelBefore && newLevel < levelInt)
				{
					plugin.logDebug("Iterating through levels... Possible new level: " + levelInt);
					newLevel = levelInt;
				}
			}

			if (newLevel != 1)
			{
				plugin.logDebug("Setting new level to level: " + newLevel + "! Old level: " + this.level);
				this.level = newLevel;
			}

			if (levelBefore < level)
			{
				setLevel(level);
				handleNewLevel(this);
			}
			
			saveTimed();
		}

		public boolean setDogCustomName()
		{
			plugin.logDebug("Setting custom name... dogId: " + dogId);
			if (plugin.getServer().getEntity(dogId) == null || !plugin.getServer().getEntity(dogId).isValid() || !(plugin.getServer().getEntity(dogId) instanceof Wolf))
			{
				plugin.logDebug("Retuning false!");
				return false;
			}

			Wolf dog = (Wolf) plugin.getServer().getEntity(dogId);

			if (dogsConfig.contains(dogId.toString()))
			{
				if (plugin.useLevels && plugin.showLevelsInNametag) {
					plugin.logDebug("Setting customName to: " + nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]");
					dog.setCustomName(nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]");
				}
				else {
					plugin.logDebug("Setting customName to: " + nameColor + dogName);
					dog.setCustomName(nameColor + dogName);
				}
				if (plugin.onlyShowNametagOnHover)
				{
					dog.setCustomNameVisible(false);
				}
				else
				{
					dog.setCustomNameVisible(true);
				}
				plugin.logDebug("Returning true!");
				return true;
			}
			plugin.logDebug("Retuning false!");
			return false;
		}

		public Integer getIdentifier()
		{
			if (dogIdentifier != null)
			{
				return dogIdentifier;
			}

			return dogsConfig.getInt(dogId.toString() + ".ID", -1);
		}

		public boolean setIdentifier(Integer id)
		{
			// If the ID is already used, return false
			for (String dogIdString : dogsConfig.getKeys(false))
			{
				if (dogsConfig.getString(dogIdString + ".ID").equals(id.toString()) && dogsConfig.getString(dogIdString + ".Owner").contains(dogOwnerId.toString()))
				{
					return false;
				}
			}
			// Otherwise, apply the new ID
			this.dogIdentifier = id;
			dogsConfig.set(dogId.toString() + ".ID", dogIdentifier);
			return true;
		}

		public boolean setHealth()
		{
			Wolf wolf = (Wolf) plugin.getServer().getEntity(dogId);

			if (wolf == null)
			{
				plugin.logDebug("Failed to set Dog health, Wolf entity is null!");
				return false;
			}

			Integer dogsLevel = getLevel();
			if (dogsLevel == null || dogsLevel < 1)
			{
				plugin.logDebug("Level was under 1 or null, setting level to 1");
				dogsLevel = 1;
			}

			Level level = plugin.dogLevels.get(dogsLevel);
			if (level == null)
			{
				plugin.logDebug("Level object is null, returning!");
				return false;
			}

			double health = level.health;
			if (health < 10.0)
			{
				health = 10.0;
			}

			AttributeInstance wolfMaxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			plugin.logDebug("Dog Maxhealth Before: " + wolfMaxHealth.getValue());
			// wolfMaxHealth.setBaseValue((wolfMaxHealth.getValue()/(0.5*(getLevel()-1)))*(0.5*getLevel()));
			wolfMaxHealth.setBaseValue(health);
			wolf.setHealth(wolfMaxHealth.getValue());
			plugin.logDebug("Dog Maxhealth After: " + wolfMaxHealth.getValue());

			return true;
		}

//		public boolean setSpeed()
//		{
//			Wolf wolf = (Wolf) plugin.getServer().getEntity(dogId);
//
//			if (wolf == null)
//			{
//				plugin.logDebug("Failed to set Dog speed, Wolf entity is null!");
//				return false;
//			}
//
//			Integer dogsLevel = getLevel();
//			if (dogsLevel == null || dogsLevel < 1)
//			{
//				plugin.logDebug("Level was under 1 or null, setting level to 1");
//				dogsLevel = 1;
//			}
//
//			Level level = plugin.dogLevels.get(dogsLevel);
//			if (level == null)
//			{
//				plugin.logDebug("Level object is null, returning!");
//				return false;
//			}
//
//			double speed = level.speed;
//			if (speed < 1.0)
//			{
//				speed = 1.0;
//			}
//
//			AttributeInstance wolfSpeed = wolf.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
//			plugin.logDebug("Dog Maxhealth Before: " + wolfSpeed.getValue());
//			// wolfMaxHealth.setBaseValue((wolfMaxHealth.getValue()/(0.5*(getLevel()-1)))*(0.5*getLevel()));
//			wolfSpeed.setBaseValue(speed);
//			wolf.setSpeed(wolfSpeed.getValue()); // Needs working on
//			plugin.logDebug("Dog Maxhealth After: " + wolfSpeed.getValue());
//
//			return true;
//		}

		public boolean setDamage()
		{
			Wolf wolf = (Wolf) plugin.getServer().getEntity(dogId);

			if (wolf == null)
			{
				plugin.logDebug("Failed to set Dog damage, Wolf entity is null!");
				return false;
			}

			Integer dogsLevel = getLevel();
			if (dogsLevel == null || dogsLevel < 1)
			{
				plugin.logDebug("Level was under 1 or null, setting level to 1");
				dogsLevel = 1;
			}

			Level level = plugin.dogLevels.get(dogsLevel);
			if (level == null)
			{
				plugin.logDebug("Level object is null, returning!");
				return false;
			}

			double damage = level.damage;
			if (damage < 1.0)
			{
				damage = 1.0;
			}

			AttributeInstance wolfDamage = wolf.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
			plugin.logDebug("Dog Damage Before: " + wolfDamage.getValue());
			// wolfDamage.setBaseValue((wolfDamage.getValue()/(0.5*(getLevel()-1)))*(0.5*getLevel()));
			wolfDamage.setBaseValue(damage);
			plugin.logDebug("Dog Damage After: " + wolfDamage.getValue());
			return true;
		}

		public boolean updateWolf()
		{
			if (!plugin.useLevels)
			{
				plugin.logDebug("Not updating wolf, levels are disabled!");
				return true;
			}

			return (setHealth() && setDamage());
		}
	}

	public void load()
	{
		if (this.dogsConfigFile == null)
		{
			this.dogsConfigFile = new File(this.plugin.getDataFolder(), "dogs.yml");
		}
		this.dogsConfig = YamlConfiguration.loadConfiguration(this.dogsConfigFile);
		this.plugin.log("Loaded " + this.dogsConfig.getKeys(false).size() + " dogs.");
	}

	public void save()
	{
		this.lastSaveTime = System.currentTimeMillis();
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

	public void saveTimed()
	{
		if (plugin.instantSave) {
			save();
			return;
		}

		if (System.currentTimeMillis() - this.lastSaveTime < 180000L)
		{
			return;
		}

		save();
	}

	public boolean isDog(UUID dogId)
	{
		return dogsConfig.contains(dogId.toString());
	}

	public void removeDog(UUID dogId)
	{
		if (dogsConfig.contains(dogId.toString()))
		{
			dogsConfig.set(dogId.toString(), null);
			saveTimed();
		}
	}

	public Integer dogsOwned(Player player)
	{
		return dogsOwned(player.getUniqueId());
	}

	public Integer dogsOwned(UUID playerId)
	{
		Integer dogs = 0;
		for (String dogUUID : dogsConfig.getKeys(false))
		{
			plugin.logDebug(dogUUID);
			UUID ownerId = UUID.fromString(dogsConfig.getString(dogUUID + ".Owner"));
			if (ownerId.equals(playerId))
			{
				dogs++;
			}
		}
		return dogs;
	}

	public Dog newDog(Wolf dog, Player dogOwner) {
		Integer dogID = generateNewId(dogOwner.getUniqueId());
		return new Dog(dog, dogOwner, dogID);
	}

	public Dog newDog(Wolf dog, Player dogOwner, String customName, DyeColor collarColor) {
		Integer dogID = generateNewId(dogOwner.getUniqueId());
		return new Dog(dog, dogOwner, customName, collarColor, dogID);
	}

	public Dog getDog(UUID dogId)
	{
		if (dogsConfig.contains(dogId.toString()))
		{
			return new Dog(dogId, UUID.fromString(dogsConfig.getString(dogId.toString() + ".Owner")));
		}
		return null;
	}

	public Dog getDog(String dogIdentifier, UUID ownerId)
	{
		for (String dogIdString : dogsConfig.getKeys(false))
		{
			if (dogsConfig.getString(dogIdString + ".ID").equals(dogIdentifier) && dogsConfig.getString(dogIdString + ".Owner").contains(ownerId.toString()))
			{
				UUID dogId = UUID.fromString(dogIdString);
				return new Dog(dogId, UUID.fromString(dogsConfig.getString(dogId.toString() + ".Owner")));
			}
		}

		return null;
	}

	public List<Dog> getDogs()
	{
		List<Dog> dogs = new ArrayList<Dog>();

		for (String dogIdString : dogsConfig.getKeys(false))
		{
			UUID dogId = UUID.fromString(dogIdString);
			dogs.add(new Dog((Wolf) Objects.requireNonNull(plugin.getServer().getEntity(dogId))));
		}

		return dogs;
	}

	public List<Dog> getDogs(UUID ownerId)
	{
		List<Dog> dogs = new ArrayList<Dog>();

		for (String dogIdString : dogsConfig.getKeys(false))
		{
			if (dogsConfig.getString(dogIdString + ".Owner").contains(ownerId.toString()))
			{
				UUID dogId = UUID.fromString(dogIdString);
				dogs.add(new Dog(dogId, ownerId));
			}
		}

		return dogs;
	}

	public String newDogName()
	{
		int dogNameNumber = random.nextInt(plugin.dogNames.size());
		return plugin.dogNames.get(dogNameNumber);
	}

	public boolean setNewId(Dog dog, Integer id)
	{
		// If another dog is already using the ID
		if (!dog.setIdentifier(id))
		{
			Dog dog2 = getDog(id.toString(), dog.getOwnerId());
			if (dog2.setIdentifier(generateNewId(dog.getOwnerId())))
			{
				if (dog.setIdentifier(id))
				{
					return true;
				}
			}
		}
		else
		{
			return true;
		}
		return false;
	}

	private Integer generateNewId(UUID dogOwnerId)
	{
		Integer id = 1;
		List<Dog> dogs = MyDog.getDogManager().getDogs(dogOwnerId);

		if (!dogs.isEmpty())
		{
			plugin.logDebug("Running new generator for ID");

			while (true)
			{
				plugin.logDebug("Running loop - Dogs size: " + dogs.size());
				Boolean isUsed = false;
				for (Dog dog : dogs)
				{
					plugin.logDebug("Current dog: " + dog.getDogName() + " - " + dog.getIdentifier() + " ID to search: " + id);
					if (dog.getIdentifier().equals(id))
					{
						plugin.logDebug("ID already used - ID: " + id);
						isUsed = true;
						break;
					}
				}
				if (!isUsed)
				{
					plugin.logDebug("Found a free ID: " + id);
					break;
				}
				id++;
			}
			plugin.logDebug("ok");
		}
		else
		{
			plugin.logDebug("Dogs list is empty!");
			id = 1;
		}

		plugin.logDebug("Returning ID: " + id);
		return id;
	}

	public void handleNewLevel(Dog dog)
	{
		plugin.logDebug("Dog levelup! Level before: " + (dog.getLevel()-1) + " - Level now: " + dog.getLevel());
		UUID ownerId = dog.getOwnerId();
		Player owner = plugin.getServer().getPlayer(ownerId);
		if (owner.isOnline())
		{
			String levelupString = plugin.levelUpString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName()).replace("{level}", dog.level.toString());
			/*owner.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.DARK_PURPLE + "Your dog, "
					+ dog.getDogColor() + dog.getDogName() + ChatColor.DARK_PURPLE + ", just leveled up to " + ChatColor.LIGHT_PURPLE + "Level " + dog.level + ChatColor.DARK_PURPLE + "!");*/
			owner.sendMessage(ChatColor.translateAlternateColorCodes('&', levelupString));

			MyDog.getParticleUtils().newLevelUpParticle(plugin.getServer().getEntity(dog.getDogId()));
			Sound sound = null;
			if (plugin.levelUpSound == null)
			{
				plugin.logDebug("Couldn't load the levelup sound, took Howl!");
				sound = Sound.ENTITY_WOLF_HOWL;
			}
			else
			{
				sound = Sound.valueOf(plugin.levelUpSound);
			}

			dog.setDogName(dog.dogName);

			owner.playSound(owner.getLocation(), sound, 3.0F, 1.0F);
		}

		dog.updateWolf();
	}
}