package dk.fido2603.mydog.objects;

import dk.fido2603.mydog.MyDog;
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

public class Dog {
    private static final String ANGRY_MODE = ChatColor.GRAY + "[" + ChatColor.RED + "⚔" + ChatColor.GRAY + "]";
    private static final String DEFENCE_MODE = ChatColor.GRAY + "[" + ChatColor.GREEN + "⛨" + ChatColor.GRAY + "]";

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
    private Boolean isDead;
    private Boolean isAngry;

    private String pattern = "dd-MM-yyyy HH:mm";
    private DateFormat formatter = new SimpleDateFormat(pattern);

    // For new dogs
    public Dog(Wolf dog, Player dogOwner, int dogUID, int level) {
        this(dog, dogOwner, null, null, dogUID, level);
    }

    // For new, already-tamed dogs with old data
    public Dog(Wolf dog, Player dogOwner, String customName, DyeColor collarColorImport, int dogUID, Integer level) {
        // The UUID of the Dog
        this.dogId = dog.getUniqueId();

        // The UUID of the Dog's owner (Player)
        this.dogOwnerId = dogOwner.getUniqueId();

        // Generate an ID for the Dog
        this.dogIdentifier = dogUID;

        // Generate a new name for the Dog
        if (customName == null || customName.isEmpty()) {
            this.dogName = MyDog.getDogManager().newDogName();
        } else {
            this.dogName = customName;
        }

        // Generate a random Collar Color and set the Dog's Color
        if (collarColorImport == null) {
            if (MyDog.instance().randomCollarColor) {
                dog.setCollarColor(ColorUtils.randomDyeColor());
            }
        }

        this.collarColor = dog.getCollarColor();
        this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);

        // Save the Dog's last seen location
        this.location = dog.getLocation();

        // Give the Dog a level
        if (MyDog.instance().useLevels) {
            if (level == 0) {
                this.level = 1;
            } else {
                this.level = level;
            }
            this.experience = 0;
        }

        // Set the current time as the Dog's birthday
        this.birthday = new Date();

        // Sync the data to the database
        syncDog();
    }

    // For old, already created, dogs
    public Dog(Wolf dog) {
        this(dog.getUniqueId(), dog.getOwner().getUniqueId());
    }

    // For old, already created, dogs
    public Dog(UUID dogUUID, UUID playerUUID) {
        if (MyDog.getDogManager().getDogsConfig().contains(dogUUID.toString())) {
            this.dogId = dogUUID;
            this.dogOwnerId = playerUUID;
            this.dogIdentifier = getIdentifier();
            this.birthday = getBirthday();
            this.level = getLevel();
            this.experience = getExperience();
            this.dogName = getDogName();
            this.nameColor = getDogColor();
            this.isDead = isDead();
            this.isAngry = isAngry();
            this.location = getDogLocation();

            syncDog();
        }
    }

    public String getDogName() {
        if (dogName != null) {
            return dogName;
        }

        return MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".Name", "UNKNOWN DOGNAME");
    }

    public Wolf getWolf() {
        if (MyDog.instance().getServer().getEntity(dogId) == null || !MyDog.instance().getServer().getEntity(dogId).isValid() || !(MyDog.instance().getServer().getEntity(dogId) instanceof Wolf)) {
            return null;
        }

        return (Wolf) MyDog.instance().getServer().getEntity(dogId);
    }

    /**
     * Reassigns the dog to another UUID.
     * NOTE: Deletes the old dog from the data!
     * @param dogID the new UUID
     */
    public void setUUID(UUID dogID) {
        UUID oldID = this.dogId;
        this.dogId = dogID;

        syncDog();

        MyDog.getDogManager().removeDog(oldID);
    }

    public void setIsAngry(boolean isAngry) {
        this.isAngry = isAngry;
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Angry", this.isAngry);

        MyDog.getDogManager().saveTimed();
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Birthday", formatter.format(this.birthday));

        MyDog.getDogManager().saveTimed();
    }

    public Date getBirthday() {
        if (birthday != null) {
            return birthday;
        }

        try {
            return formatter.parse(MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".Birthday"));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ChatColor getDogColor() {
        if (nameColor != null) {
            return nameColor;
        }

        if (MyDog.instance().getServer().getEntity(dogId) == null || !MyDog.instance().getServer().getEntity(dogId).isValid()) {
            if (MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".NameChatColor") != null) {
                return ChatColor.valueOf(MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".NameChatColor"));
            }
            return ChatColor.WHITE;
        }

        return ColorUtils.getChatColorFromDyeColor(((Wolf) MyDog.instance().getServer().getEntity(dogId)).getCollarColor());
    }

    public boolean setDogColor(DyeColor color) {
        if (color == null) {
            return false;
        }
        if (MyDog.getDogManager().getDogsConfig().contains(dogId.toString())) {
            ChatColor chatColor = ColorUtils.getChatColorFromDyeColor(color);

            this.nameColor = chatColor;
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());

            Wolf dog = (Wolf) MyDog.instance().getServer().getEntity(dogId);

            if (dog == null) {
                MyDog.instance().logDebug("Dog is null");
                return false;
            }

            if (dogName == null) {
                this.dogName = getDogName();
            }


            if (MyDog.instance().useLevels && MyDog.instance().showLevelsInNametag) {
                String dogNamePlate = nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]" + (isAngry() ? ANGRY_MODE : DEFENCE_MODE);
                MyDog.instance().logDebug("Setting customName to: " + dogNamePlate);
                dog.setCustomName(dogNamePlate);
            } else {
                MyDog.instance().logDebug("Setting customName to: " + nameColor + dogName);
                dog.setCustomName(nameColor + dogName);
            }

            if (MyDog.instance().onlyShowNametagOnHover) {
                dog.setCustomNameVisible(false);
            } else {
                dog.setCustomNameVisible(true);
            }
        }
        MyDog.getDogManager().saveTimed();
        return true;
    }

    public void toggleMode() {
        Wolf dog = (Wolf) MyDog.instance().getServer().getEntity(dogId);
        if (dog == null) {
            return;
        }
        this.setIsAngry(!isAngry());
        dog.setCustomName(nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]" + (isAngry() ? ANGRY_MODE : DEFENCE_MODE));
        dog.setCustomNameVisible(!MyDog.instance().onlyShowNametagOnHover);
        MyDog.getDogManager().saveTimed();
    }

    public boolean isDead() {
        if (isDead != null) {
            return isDead;
        }
        return MyDog.getDogManager().getDogsConfig().getBoolean(dogId.toString() + ".Dead", false);
    }

    public void setDead(boolean dead) {
        isDead = dead;
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Dead", isDead);

        MyDog.getDogManager().saveTimed();
    }

    public boolean isAngry() {
        if (isAngry != null) {
            return isAngry;
        }

        return MyDog.getDogManager().getDogsConfig().getBoolean(dogId.toString() + ".Angry", false);
    }

    public boolean setDogName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (MyDog.getDogManager().getDogsConfig().contains(dogId.toString())) {
            this.dogName = name;
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Name", dogName);

            Wolf dog = (Wolf) MyDog.instance().getServer().getEntity(dogId);

            if (dog == null) {
                MyDog.instance().logDebug("Dog is null");
                return false;
            }

            if (dogName.isEmpty()) {
                this.dogName = getDogName();
            }

            if (MyDog.instance().useLevels && MyDog.instance().showLevelsInNametag) {
                MyDog.instance().logDebug("Setting customName to: " + nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]" + (isAngry() ? ANGRY_MODE : DEFENCE_MODE));
                dog.setCustomName(nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]" + (isAngry() ? ANGRY_MODE : DEFENCE_MODE));
            } else {
                MyDog.instance().logDebug("Setting customName to: " + nameColor + dogName);
                dog.setCustomName(nameColor + dogName);
            }
            if (MyDog.instance().onlyShowNametagOnHover) {
                dog.setCustomNameVisible(false);
            } else {
                dog.setCustomNameVisible(true);
            }
        }
        MyDog.getDogManager().saveTimed();
        return true;
    }

    public Location getDogLocation() {
        if (MyDog.instance().getServer().getEntity(dogId) == null || !MyDog.instance().getServer().getEntity(dogId).isValid()) {
            if (MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".LastSeen.World") != null) {
                return new Location(MyDog.instance().getServer().getWorld(MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".LastSeen.World")), MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".LastSeen.X"), MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".LastSeen.Y"), MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".LastSeen.Z"));
            }
            return null;
        }

        this.location = MyDog.instance().getServer().getEntity(dogId).getLocation();
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.X", location.getX());
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.Y", location.getY());
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.Z", location.getZ());

        MyDog.getDogManager().saveTimed();
        return location;
    }

    public boolean saveDogLocation() {
        if (MyDog.instance().getServer().getEntity(dogId) == null || !MyDog.instance().getServer().getEntity(dogId).isValid()) {
            return false;
        }

        this.location = MyDog.instance().getServer().getEntity(dogId).getLocation();
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.X", location.getX());
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.Y", location.getY());
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.Z", location.getZ());

        MyDog.getDogManager().saveTimed();
        return true;
    }

    public Location getLastDogLocation() {
        return location;
    }

    public UUID getDogId() {
        return dogId;
    }

    public UUID getOwnerId() {
        return dogOwnerId;
    }

    public int getRevivalPrice() {
        return (int) (level * MyDog.instance().revivalPrice);
    }

    public int getLevel() {
        if (level != 0) {
            return level;
        }
        return MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".Level.Level", 1);
    }

    public void setLevel(int lvl) {
        if (level != lvl) {
            this.level = lvl;
        }

        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Level.Level", lvl);
    }

    public int getExperience() {
        if (experience != 0) {
            return experience;
        }
        return MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".Level.Experience", 0);
    }

    public void giveExperience(int exp) {
        int currentExp = getExperience();
        setExperience(currentExp + exp);
    }

    public void setExperience(int exp) {
        this.experience = exp;
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Level.Experience", exp);

        int levelBefore = level;
        int newLevel = 1;
        Map<Integer, LevelFactory.Level> levels = MyDog.instance().dogLevels;

        for (Integer levelInt : levels.keySet()) {
            int levelExp = levels.get(levelInt).exp;
            // Amount of exp must be higher or equals to exp to level
            // The level must be higher than the level the Dog had before
            // The new level variable must be smaller than the level checking against
            if (exp >= levelExp && levelInt > levelBefore && newLevel < levelInt) {
                MyDog.instance().logDebug("Iterating through levels... Possible new level: " + levelInt);
                newLevel = levelInt;
            }
        }

        if (newLevel != 1) {
            MyDog.instance().logDebug("Setting new level to level: " + newLevel + "! Old level: " + this.level);
            this.level = newLevel;
        }

        if (levelBefore < level) {
            setLevel(level);
            MyDog.getDogManager().handleNewLevel(this);
        }

        MyDog.getDogManager().saveTimed();
    }

    public boolean setDogCustomName() {
        MyDog.instance().logDebug("Setting custom name... dogId: " + dogId);
        if (MyDog.instance().getServer().getEntity(dogId) == null || !MyDog.instance().getServer().getEntity(dogId).isValid() || !(MyDog.instance().getServer().getEntity(dogId) instanceof Wolf)) {
            MyDog.instance().logDebug("Retuning false!");
            return false;
        }

        Wolf dog = (Wolf) MyDog.instance().getServer().getEntity(dogId);

        if (MyDog.getDogManager().getDogsConfig().contains(dogId.toString())) {
            if (MyDog.instance().useLevels && MyDog.instance().showLevelsInNametag) {
                MyDog.instance().logDebug("Setting customName to: " + nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]" + (isAngry() ? ANGRY_MODE : DEFENCE_MODE));
                dog.setCustomName(nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]" + (isAngry() ? ANGRY_MODE : DEFENCE_MODE));
            } else {
                MyDog.instance().logDebug("Setting customName to: " + nameColor + dogName);
                dog.setCustomName(nameColor + dogName);
            }
            if (MyDog.instance().onlyShowNametagOnHover) {
                dog.setCustomNameVisible(false);
            } else {
                dog.setCustomNameVisible(true);
            }
            MyDog.instance().logDebug("Returning true!");
            return true;
        }
        MyDog.instance().logDebug("Retuning false!");
        return false;
    }

    public int getIdentifier() {
        if (dogIdentifier > 0) {
            return dogIdentifier;
        }
        return MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".ID", -1);
    }

    public boolean setIdentifier(int id) {
        // If the ID is already used, return false
        for (String dogIdString : MyDog.getDogManager().getDogsConfig().getKeys(false)) {
            if (MyDog.getDogManager().getDogsConfig().getString(dogIdString + ".ID").equals(Integer.toString(id)) && MyDog.getDogManager().getDogsConfig().getString(dogIdString + ".Owner").contains(dogOwnerId.toString())) {
                return false;
            }
        }
        // Otherwise, apply the new ID
        this.dogIdentifier = id;
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".ID", dogIdentifier);
        return true;
    }

    public boolean setHealth() {
        Wolf wolf = (Wolf) MyDog.instance().getServer().getEntity(dogId);

        if (wolf == null) {
            MyDog.instance().logDebug("Failed to set Dog health, Wolf entity is null!");
            return false;
        }

        int dogsLevel = getLevel();
        if (dogsLevel < 1) {
            MyDog.instance().logDebug("Level was under 1, setting level to 1");
            dogsLevel = 1;
        }

        LevelFactory.Level level = MyDog.instance().dogLevels.get(dogsLevel);
        if (level == null) {
            MyDog.instance().logDebug("Level object is null, returning!");
            return false;
        }

        double health = level.health;
        if (health < 5D) {
            health = 5D;
        }

        AttributeInstance wolfMaxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        MyDog.instance().logDebug("Dog Maxhealth Before: " + wolfMaxHealth.getValue());
        // wolfMaxHealth.setBaseValue((wolfMaxHealth.getValue()/(0.5*(getLevel()-1)))*(0.5*getLevel()));
        wolfMaxHealth.setBaseValue(health);
        wolf.setHealth(wolfMaxHealth.getValue());
        MyDog.instance().logDebug("Dog Maxhealth After: " + wolfMaxHealth.getValue());

        return true;
    }

//		public boolean setSpeed()
//		{
//			Wolf wolf = (Wolf) MyDog.instance().getServer().getEntity(dogId);
//
//			if (wolf == null)
//			{
//				MyDog.instance().logDebug("Failed to set Dog speed, Wolf entity is null!");
//				return false;
//			}
//
//			int dogsLevel = getLevel();
//			if (dogsLevel < 1)
//			{
//				MyDog.instance().logDebug("Level was under 1, setting level to 1");
//				dogsLevel = 1;
//			}
//
//			Level level = MyDog.instance().dogLevels.get(dogsLevel);
//			if (level == null)
//			{
//				MyDog.instance().logDebug("Level object is null, returning!");
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
//			MyDog.instance().logDebug("Dog Maxhealth Before: " + wolfSpeed.getValue());
//			// wolfMaxHealth.setBaseValue((wolfMaxHealth.getValue()/(0.5*(getLevel()-1)))*(0.5*getLevel()));
//			wolfSpeed.setBaseValue(speed);
//			wolf.setSpeed(wolfSpeed.getValue()); // Needs working on
//			MyDog.instance().logDebug("Dog Maxhealth After: " + wolfSpeed.getValue());
//
//			return true;
//		}

    public boolean setDamage() {
        Wolf wolf = (Wolf) MyDog.instance().getServer().getEntity(dogId);

        if (wolf == null || !wolf.isValid()) {
            MyDog.instance().logDebug("Failed to set Dog damage, Wolf entity is null or invalid!");
            return false;
        }

        int dogsLevel = getLevel();
        if (dogsLevel < 1) {
            MyDog.instance().logDebug("Level was under 1, setting level to 1");
            dogsLevel = 1;
        }

        LevelFactory.Level level = MyDog.instance().dogLevels.get(dogsLevel);
        if (level == null) {
            MyDog.instance().logDebug("Level object is null, returning!");
            return false;
        }

        double damage = level.damage;
        if (damage < 1.0D) {
            damage = 1.0D;
        }

        AttributeInstance wolfDamage = wolf.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        MyDog.instance().logDebug("Dog Damage Before: " + wolfDamage.getValue());
        wolfDamage.setBaseValue(damage);
        MyDog.instance().logDebug("Dog Damage After: " + wolfDamage.getValue());
        return true;
    }

    public void syncDog() {
        Wolf wolf = getWolf();

        // The UUID of the Dog's owner (Player)
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Owner", dogOwnerId.toString());
        // ID for the Dog
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".ID", dogIdentifier);
        // Name for the Dog
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Name", dogName);

        // Generate a random Collar Color and set the Dog's Color
        if (collarColor == null) {
            if (wolf != null) {
                this.collarColor = wolf.getCollarColor();
            }
            else {
                if (MyDog.instance().randomCollarColor) {
                    this.collarColor = ColorUtils.randomDyeColor();
                }
            }

            if (collarColor != null) {
                this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);
                MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());
            }
        }
        else {
            if (nameColor == null) {
                this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);
            }
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());
        }

        // Save the Dog's last seen location
        if (this.location == null) {
            this.location = getDogLocation();
        }
        else {
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.X", location.getX());
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.Y", location.getY());
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.Z", location.getZ());
        }

        // Give the Dog a level
        if (MyDog.instance().useLevels) {
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Level.Level", level);
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Level.Experience", experience);
        }

        // Set the Dog's birthday
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Birthday", formatter.format(birthday));

        // Set angry
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Angry", (isAngry != null) ? isAngry : false);
        // Set dead to false (I hope LOL)
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Dead", (isDead != null) ? isDead : false);

        MyDog.getDogManager().saveTimed();
    }

    public boolean updateWolf() {
        boolean customNameSet = setDogCustomName();

        if (!MyDog.instance().useLevels) {
            MyDog.instance().logDebug("Not updating health and damage for dog, levels are disabled!");
            return customNameSet;
        }

        return customNameSet && (setHealth() && setDamage());
    }
}
