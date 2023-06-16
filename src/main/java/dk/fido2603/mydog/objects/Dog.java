package dk.fido2603.mydog.objects;

import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Dog {
    private static final String ANGRY_MODE = ChatColor.GRAY + "[" + ChatColor.RED + "⚔" + ChatColor.GRAY + "]";
    private static final String DEFENCE_MODE = ChatColor.GRAY + "[" + ChatColor.GREEN + "⛨" + ChatColor.GRAY + "]";
    private static final List<Sound> PETTING_SOUNDS = Arrays.asList(Sound.ENTITY_WOLF_PANT, Sound.ENTITY_WOLF_AMBIENT);

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

    private final String pattern = "dd-MM-yyyy HH:mm";
    private final DateFormat formatter = new SimpleDateFormat(pattern);
    private final static Random random = new Random();

    // For new dogs
    public Dog(Wolf wolf, Player dogOwner, int dogUID, int level) {
        this(wolf, dogOwner, null, null, dogUID, level);
    }

    // For new, already-tamed dogs with old data
    public Dog(Wolf wolf, Player dogOwner, String customName, DyeColor collarColorImport, int dogUID, Integer level) {
        // The UUID of the Dog
        this.dogId = wolf.getUniqueId();

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
        if (collarColorImport == null && MyDog.instance().randomCollarColor) {
            wolf.setCollarColor(ColorUtils.randomDyeColor());
        }

        this.collarColor = wolf.getCollarColor();
        this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);

        // Save the Dog's last seen location
        this.location = wolf.getLocation();

        // Give the Dog a level
        if (MyDog.instance().useLevels) {
            if (level == null || level == 0) {
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
    public Dog(Wolf wolf) {
        this(wolf.getUniqueId(), Objects.requireNonNull(wolf.getOwner()).getUniqueId());
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
        Entity wolfEntity = MyDog.instance().getServer().getEntity(dogId);
        if (wolfEntity == null || !wolfEntity.isValid() || !(wolfEntity instanceof Wolf)) {
            return null;
        }

        return (Wolf) wolfEntity;
    }

    /**
     * Reassigns the dog to another UUID.
     * NOTE: Deletes the old dog from the data!
     *
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

        if (MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".Birthday") == null) {
            setBirthday(new Date());
        }

        try {
            return formatter.parse(MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".Birthday"));
        } catch (ParseException e) {
            MyDog.instance().log("Failed to parse dog birthday.");
            return null;
        }
    }

    public ChatColor getDogColor() {
        if (nameColor != null) {
            return nameColor;
        }

        Wolf wolf = getWolf();
        if (wolf == null) {
            if (MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".NameChatColor") != null) {
                return ChatColor.valueOf(MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".NameChatColor"));
            }
            return ChatColor.WHITE;
        }

        return ColorUtils.getChatColorFromDyeColor(wolf.getCollarColor());
    }

    public boolean setDogColor(DyeColor color) {
        if (color == null) {
            return false;
        }
        if (MyDog.getDogManager().getDogsConfig().contains(dogId.toString())) {
            this.nameColor = ColorUtils.getChatColorFromDyeColor(color);
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());

            if (dogName == null) {
                this.dogName = getDogName();
            }

            return setDogCustomName();
        }
        MyDog.getDogManager().saveTimed();
        return true;
    }

    public void pet(Player player) {
        if (player == null || (!player.isOp() && !MyDog.getPermissionsManager().hasPermission(player, "mydog.pet"))) {
            return;
        }

        if (!player.getUniqueId().equals(dogOwnerId) && !MyDog.getPermissionsManager().hasPermission(player, "mydog.pet.others")) {
            return;
        }

        Wolf wolf = getWolf();
        if (wolf == null) {
            return;
        }

        MyDog.instance().logDebug("Petting dog.");
        String pettingString = MyDog.instance().pettingString.replace("{dogNameColor}", "&" + getDogColor().getChar()).replace("{dogName}", getDogName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', pettingString));

        if (random.nextInt(10) == 1) {
            wolf.playEffect(EntityEffect.WOLF_SHAKE);
            pettingString = MyDog.instance().pettingSplashString.replace("{dogNameColor}", "&" + getDogColor().getChar()).replace("{dogName}", getDogName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', pettingString));
        }
        MyDog.getParticleUtils().newPettingParticle(wolf);
        Sound sound = PETTING_SOUNDS.get(random.nextInt(PETTING_SOUNDS.size()));
        player.playSound(player.getLocation(), sound, 3.0F, 1.0F);
    }

    public void toggleMode() {
        setAngryMode(!isAngry());
    }

    public void setAngryMode(boolean angry) {
        MyDog.instance().logDebug("Toggling dog mode.");
        this.setIsAngry(angry);
        setDogCustomName();
        MyDog.getDogManager().saveTimed();
    }

    public void sit(boolean sit) {
        Wolf wolf = getWolf();
        if (wolf == null) {
            return;
        }

        wolf.setSitting(sit);
        getDogLocation();
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

            setDogCustomName();
        }
        MyDog.getDogManager().saveTimed();
        return true;
    }

    public Location getDogLocation() {
        Wolf wolf = getWolf();
        if (wolf == null) {
            if (MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".LastSeen.World") != null) {
                String lastSeenWorld = MyDog.getDogManager().getDogsConfig().getString(dogId.toString() + ".LastSeen.World");
                if (lastSeenWorld == null) {
                    return null;
                }
                return new Location(MyDog.instance().getServer().getWorld(lastSeenWorld), MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".LastSeen.X"), MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".LastSeen.Y"), MyDog.getDogManager().getDogsConfig().getInt(dogId.toString() + ".LastSeen.Z"));
            }
            return null;
        }

        saveDogLocation();
        return this.location;
    }

    public boolean saveDogLocation() {
        Wolf wolf = getWolf();
        if (wolf == null) {
            return false;
        }

        this.location = wolf.getLocation();
        if (this.location.getWorld() != null) {
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".LastSeen.World", location.getWorld().getName());
        }
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

    public boolean setOwner(Player player) {
        if (player == null) {
            return false;
        }

        Wolf wolf = getWolf();

        if (wolf == null) {
            return false;
        }

        if (dogOwnerId.equals(player.getUniqueId())) {
            // Same owner
            return false;
        }

        if (MyDog.getDogManager().getDogsConfig().contains(dogId.toString())) {
            this.dogOwnerId = player.getUniqueId();
            this.dogIdentifier = MyDog.getDogManager().generateNewId(player.getUniqueId());
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".Owner", dogOwnerId);
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".ID", dogIdentifier);
            wolf.setOwner(player);
        }

        MyDog.getDogManager().saveTimed();
        return true;
    }

    public int getRevivalPrice() {
        return level * MyDog.instance().revivalPrice;
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

        for (Map.Entry<Integer, LevelFactory.Level> levelSet : levels.entrySet()) {
            int levelInt = levelSet.getKey();
            int levelExp = levelSet.getValue().exp;
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

    public String getDogCustomName() {
        if (MyDog.getDogManager().getDogsConfig().contains(dogId.toString())) {
            if (MyDog.instance().useLevels && MyDog.instance().showLevelsInNametag) {
                return (nameColor + dogName + ChatColor.GRAY + " [" + ChatColor.GOLD + "" + this.level + ChatColor.GRAY + "]" + (isAngry() ? ANGRY_MODE : DEFENCE_MODE));
            }
            else {
                return nameColor + dogName;
            }
        }
        return null;
    }

    public boolean setDogCustomName() {
        MyDog.instance().logDebug("Setting custom name... dogId: " + dogId);
        Wolf wolf = getWolf();
        if (wolf == null) {
            MyDog.instance().logDebug("Retuning false!");
            return false;
        }

        if (MyDog.getDogManager().getDogsConfig().contains(dogId.toString())) {
            String customName = getDogCustomName();
            MyDog.instance().logDebug("Setting customName to: " + customName);
            wolf.setCustomName(customName);

            wolf.setCustomNameVisible(!MyDog.instance().onlyShowNametagOnHover);
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
            if (MyDog.getDogManager().getDogsConfig().getString(dogIdString + ".ID") == null) continue;
            if (MyDog.getDogManager().getDogsConfig().getString(dogIdString + ".Owner") == null) continue;

            if (Integer.toString(id).equals(MyDog.getDogManager().getDogsConfig().getString(dogIdString + ".ID"))
                    && dogOwnerId.toString().equals(MyDog.getDogManager().getDogsConfig().getString(dogIdString + ".Owner"))) {
                return false;
            }
        }
        // Otherwise, apply the new ID
        this.dogIdentifier = id;
        MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".ID", dogIdentifier);
        return true;
    }

    public boolean setHealth() {
        Wolf wolf = getWolf();

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
        if (wolfMaxHealth == null) {
            return false;
        }

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
        Wolf wolf = getWolf();

        if (wolf == null) {
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
        if (wolfDamage == null) {
            return false;
        }
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
            } else {
                if (MyDog.instance().randomCollarColor) {
                    this.collarColor = ColorUtils.randomDyeColor();
                }
            }

            if (collarColor != null) {
                this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);
                MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());
            }
        } else {
            if (nameColor == null) {
                this.nameColor = ColorUtils.getChatColorFromDyeColor(collarColor);
            }
            MyDog.getDogManager().getDogsConfig().set(dogId.toString() + ".NameChatColor", nameColor.name());
        }

        // Save the Dog's last seen location
        this.location = getDogLocation();

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
