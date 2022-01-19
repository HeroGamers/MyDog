package dk.fido2603.mydog.objects;

import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.managers.DogManager;
import dk.fido2603.mydog.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class Dog
{
    private UUID dogId;
    private UUID dogOwnerId;
    private int dogIdentifier;
    private String dogName;
    private int level;
    private int experience;
    private Date birthday;
    private Location location;
    private DyeColor collarColor;
    private ChatColor nameColor;

    private static DogManager dogManager;
    private static MyDog plugin;

    private String pattern = "dd-MM-yyyy HH:mm";
    private DateFormat formatter = new SimpleDateFormat(pattern);

    // For new dogs
    public Dog(Wolf dog, Player dogOwner, int dogUID)
    {
        if (dogManager == null) {
            dogManager = MyDog.getDogManager();
        }
        if (plugin == null) {
            plugin = MyDog.instance();
        }
        
        // The UUID of the Dog
        this.dogId = dog.getUniqueId();

        // The UUID of the Dog's owner (Player)
        this.dogOwnerId = dogOwner.getUniqueId();
        dogManager.getDogsConfig().set(dogId.toString() + ".Owner", dogOwnerId.toString());

        // Generate an ID for the Dog
        this.dogIdentifier = dogUID;
        dogManager.getDogsConfig().set(dogId.toString() + ".ID", dogIdentifier);

        // Generate a new name for the Dog
        this.dogName = dogManager.newDogName();
        dogManager.getDogsConfig().set(dogId.toString() + ".Name", dogName);

        // Generate a random Collar Color and set the Dog's Color
        if (plugin.randomCollarColor)
        {
            dog.setCollarColor(ColorUtils.randomDyeColor());
        }
        this.collarColor = dog.getCollarColor();
        this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);
        dogManager.getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());

        // Save the Dog's last seen location
        this.location = dog.getLocation();
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.X", location.getX());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.Y", location.getY());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.Z", location.getZ());

        // Give the Dog a level
        if (plugin.useLevels)
        {
            this.level = 1;
            dogManager.getDogsConfig().set(dogId.toString() + ".Level.Level", level);
            this.experience = 0;
            dogManager.getDogsConfig().set(dogId.toString() + ".Level.Experience", experience);
        }

        // Set the current time as the Dog's birthday
        this.birthday = new Date();
        dogManager.getDogsConfig().set(dogId.toString() + ".Birthday", formatter.format(birthday));

        dogManager.saveTimed();
    }

    // For new, already-tamed dogs with old data
    public Dog(Wolf dog, Player dogOwner, String customName, DyeColor collarColorImport, int dogUID)
    {
        if (dogManager == null) {
            dogManager = MyDog.getDogManager();
        }
        if (plugin == null) {
            plugin = MyDog.instance();
        }
        
        // The UUID of the Dog
        this.dogId = dog.getUniqueId();

        // The UUID of the Dog's owner (Player)
        this.dogOwnerId = dogOwner.getUniqueId();
        dogManager.getDogsConfig().set(dogId.toString() + ".Owner", dogOwnerId.toString());

        // Generate an ID for the Dog
        this.dogIdentifier = dogUID;
        dogManager.getDogsConfig().set(dogId.toString() + ".ID", dogIdentifier);

        // Generate a new name for the Dog
        if (customName == null || customName.isEmpty())
        {
            this.dogName = dogManager.newDogName();
        }
        else
        {
            this.dogName = customName;
        }
        dogManager.getDogsConfig().set(dogId.toString() + ".Name", dogName);

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
        dogManager.getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());

        // Save the Dog's last seen location
        this.location = dog.getLocation();
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.X", location.getX());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.Y", location.getY());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.Z", location.getZ());

        // Give the Dog a level
        if (plugin.useLevels)
        {
            this.level = 1;
            dogManager.getDogsConfig().set(dogId.toString() + ".Level.Level", level);
            this.experience = 0;
            dogManager.getDogsConfig().set(dogId.toString() + ".Level.Experience", experience);
        }

        // Set the current time as the Dog's birthday
        this.birthday = new Date();
        dogManager.getDogsConfig().set(dogId.toString() + ".Birthday", formatter.format(birthday));

        dogManager.saveTimed();
    }

    // For old, already created, dogs
    public Dog(Wolf dog)
    {
        if (dogManager.getDogsConfig().contains(dog.getUniqueId().toString()))
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
        if (dogManager.getDogsConfig().contains(dogUUID.toString()))
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

        return dogManager.getDogsConfig().getString(dogId.toString() + ".Name", "UNKNOWN DOGNAME");
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
            return formatter.parse(dogManager.getDogsConfig().getString(dogId.toString() + ".Birthday"));
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
            if (dogManager.getDogsConfig().getString(dogId.toString() + ".NameChatColor") != null)
            {
                return ChatColor.valueOf(dogManager.getDogsConfig().getString(dogId.toString() + ".NameChatColor"));
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
        if (dogManager.getDogsConfig().contains(dogId.toString()))
        {
            ChatColor chatColor = ColorUtils.getChatColorFromDyeColor(color);

            this.nameColor = chatColor;
            dogManager.getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());

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
        dogManager.saveTimed();
        return true;
    }

    public boolean setDogName(String name)
    {
        if (name == null || name.isEmpty())
        {
            return false;
        }
        if (dogManager.getDogsConfig().contains(dogId.toString()))
        {
            this.dogName = name;
            dogManager.getDogsConfig().set(dogId.toString() + ".Name", dogName);

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
        dogManager.saveTimed();
        return true;
    }

    public Location getDogLocation()
    {
        if (plugin.getServer().getEntity(dogId) == null || !plugin.getServer().getEntity(dogId).isValid())
        {
            if (dogManager.getDogsConfig().getString(dogId.toString() + ".LastSeen.World") != null)
            {
                return new Location(plugin.getServer().getWorld(dogManager.getDogsConfig().getString(dogId.toString() + ".LastSeen.World")), dogManager.getDogsConfig().getInt(dogId.toString() + ".LastSeen.X"), dogManager.getDogsConfig().getInt(dogId.toString() + ".LastSeen.Y"), dogManager.getDogsConfig().getInt(dogId.toString() + ".LastSeen.Z"));
            }
            return null;
        }

        this.location = plugin.getServer().getEntity(dogId).getLocation();
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.X", location.getX());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.Y", location.getY());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.Z", location.getZ());

        dogManager.saveTimed();
        return location;
    }

    public boolean saveDogLocation()
    {
        if (plugin.getServer().getEntity(dogId) == null || !plugin.getServer().getEntity(dogId).isValid())
        {
            return false;
        }

        this.location = plugin.getServer().getEntity(dogId).getLocation();
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.X", location.getX());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.Y", location.getY());
        dogManager.getDogsConfig().set(dogId.toString() + ".LastSeen.Z", location.getZ());

        dogManager.saveTimed();
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

    public int getLevel()
    {
        if (level != 0)
        {
            return level;
        }

        return dogManager.getDogsConfig().getInt(dogId.toString() + ".Level.Level", 1);
    }

    public void setLevel(int lvl)
    {
        if (level != lvl)
        {
            this.level = lvl;
        }

        dogManager.getDogsConfig().set(dogId.toString() + ".Level.Level", lvl);
    }

    public int getExperience()
    {
        if (experience != 0)
        {
            return experience;
        }

        return dogManager.getDogsConfig().getInt(dogId.toString() + ".Level.Experience", 0);
    }

    public void giveExperience(int exp)
    {
        int currentExp = getExperience();
        setExperience(currentExp + exp);
    }

    public void setExperience(int exp)
    {
        this.experience = exp;
        dogManager.getDogsConfig().set(dogId.toString() + ".Level.Experience", exp);

        int levelBefore = level;
        int newLevel = 1;
        Map<Integer, LevelFactory.Level> levels = plugin.dogLevels;

        for (Integer levelInt : levels.keySet())
        {
            int levelExp = levels.get(levelInt).exp;
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
            MyDog.getDogManager().handleNewLevel(this);
        }

        MyDog.getDogManager().saveTimed();
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

        if (dogManager.getDogsConfig().contains(dogId.toString()))
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

    public int getIdentifier()
    {
        if (dogIdentifier > 0)
        {
            return dogIdentifier;
        }

        return dogManager.getDogsConfig().getInt(dogId.toString() + ".ID", -1);
    }

    public boolean setIdentifier(int id)
    {
        // If the ID is already used, return false
        for (String dogIdString : dogManager.getDogsConfig().getKeys(false))
        {
            if (dogManager.getDogsConfig().getString(dogIdString + ".ID").equals(Integer.toString(id)) && dogManager.getDogsConfig().getString(dogIdString + ".Owner").contains(dogOwnerId.toString()))
            {
                return false;
            }
        }
        // Otherwise, apply the new ID
        this.dogIdentifier = id;
        dogManager.getDogsConfig().set(dogId.toString() + ".ID", dogIdentifier);
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

        int dogsLevel = getLevel();
        if (dogsLevel < 1)
        {
            plugin.logDebug("Level was under 1, setting level to 1");
            dogsLevel = 1;
        }

        LevelFactory.Level level = plugin.dogLevels.get(dogsLevel);
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
//			int dogsLevel = getLevel();
//			if (dogsLevel < 1)
//			{
//				plugin.logDebug("Level was under 1, setting level to 1");
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

        int dogsLevel = getLevel();
        if (dogsLevel < 1)
        {
            plugin.logDebug("Level was under 1, setting level to 1");
            dogsLevel = 1;
        }

        LevelFactory.Level level = plugin.dogLevels.get(dogsLevel);
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
