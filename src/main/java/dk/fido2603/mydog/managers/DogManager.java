package dk.fido2603.mydog.managers;

import java.io.File;
import java.sql.*;
import java.util.*;

import dk.fido2603.mydog.MyDog;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.objects.Dog;
import org.intellij.lang.annotations.Language;

public class DogManager
{
	private MyDog 				plugin				= null;
	private Connection			dogsDB				= null;
	private Random				random				= new Random();
	private long				lastSaveTime		= 0L;

	private @Language("SQL")String[] 			setupSQL 			= new String[] {
			"CREATE TABLE IF NOT EXISTS player(\n"
					+ "uuid     TEXT PRIMARY KEY\n"
					+");",

			"CREATE TABLE IF NOT EXISTS dog(\n"
					+ "uuid     TEXT PRIMARY KEY,\n"
					+ "id       INTEGER NOT NULL,\n"
					+ "name     TEXT NOT NULL,\n"
					+ "color    TEXT NOT NULL,\n"
					+ "level    INTEGER NOT NULL,\n"
					+ "exp      INTEGER NOT NULL,\n"
					+ "creation TEXT NOT NULL,\n"
					+ "owner    TEXT NOT NULL,\n"
					+ "FOREIGN KEY(owner) REFERENCES player(uuid)\n"
					+");",

			"CREATE TABLE IF NOT EXISTS location(\n"
					+ "uuid     TEXT PRIMARY KEY,\n"
					+ "world    TEXT NOT NULL,\n"
					+ "x        REAL NOT NULL,\n"
					+ "y        REAL NOT NULL,\n"
					+ "z        REAL NOT NULL,\n"
					+ "FOREIGN KEY(uuid) REFERENCES dog(uuid)\n"
					+");"
	};

	public DogManager(MyDog plugin)
	{
		this.plugin = plugin;
	}

	public void load()
	{
		// connect to the database
		if (this.dogsDB == null) {
			try {
				this.dogsDB = DriverManager.getConnection("jdbc:sqlite:"+(new File(this.plugin.getDataFolder(), "MyDog.db")).toURI());
			}
			catch (SQLException e) {
				plugin.log("Could not load/connect to SQLite database! - " + e.getMessage());
			}

			if (this.dogsDB != null) {
				// debug
				try {
					DatabaseMetaData dbMeta = this.dogsDB.getMetaData();
					plugin.logDebug("DB driver name: " + dbMeta.getDriverName());
				}
				catch (SQLException e) {
					plugin.logDebug("Could not get database metadata! - " + e.getMessage());
				}

				// run setup SQL
				try {
					Statement stmt = this.dogsDB.createStatement();
					for (String sql : this.setupSQL) {
						stmt.execute(sql);
					}
				}
				catch (SQLException e) {
					plugin.logDebug("Could not run setup SQL! - " + e.getMessage());
				}


			}
		}

		// check if old configuration file exists
		File dogsConfigFile = new File(this.plugin.getDataFolder(), "dogs.yml");
		if (dogsConfigFile.exists())
		{
			FileConfiguration dogsConfig = YamlConfiguration.loadConfiguration(dogsConfigFile);
			int size = dogsConfig.getKeys(false).size();
			this.plugin.log("Loaded " + size + " dogs from an old configuration file.");
			if (size > 0) {
				// todo: convert old dogs
			}
		}
	}

	public boolean isDog(UUID dogId)
	{
		try {
			PreparedStatement pstmt = this.dogsDB.prepareStatement("SELECT EXISTS(SELECT uuid FROM dog WHERE uuid = ? LIMIT 1)");
			pstmt.setString(1, dogId.toString());
			return (pstmt.executeQuery().getInt(1) == 1);
		}
		catch (SQLException e) {
			plugin.logDebug(e.getMessage());
		}
		return false;
	}

	public void removeDog(UUID dogId)
	{
		try {
			PreparedStatement pstmt = this.dogsDB.prepareStatement("DELETE FROM dog WHERE uuid = ?");
			pstmt.setString(1, dogId.toString());
			pstmt.executeUpdate();
		}
		catch (SQLException e) {
			plugin.logDebug(e.getMessage());
		}
	}

	public int dogsOwned(Player player)
	{
		return dogsOwned(player.getUniqueId());
	}

	public int dogsOwned(UUID playerId)
	{
		return getDogs(playerId).size();
	}

	public Dog newDog(Wolf dog, Player dogOwner) {
		int dogID = generateNewId(dogOwner.getUniqueId());
		return new Dog(dog, dogOwner, dogID);
	}

	public Dog newDog(Wolf dog, Player dogOwner, String customName, DyeColor collarColor) {
		int dogID = generateNewId(dogOwner.getUniqueId());
		return new Dog(dog, dogOwner, customName, collarColor, dogID);
	}

	public Dog getDog(UUID dogId)
	{
		try {
			PreparedStatement pstmt = this.dogsDB.prepareStatement("SELECT (uuid, owner) FROM dog WHERE uuid = ? LIMIT 1");
			pstmt.setString(1, dogId.toString());
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				return new Dog(UUID.fromString(rs.getString("uuid")), UUID.fromString(rs.getString("owner")));
			}
		}
		catch (SQLException e) {
			plugin.logDebug(e.getMessage());
		}
		return null;
	}

	public Dog getDog(int dogIdentifier, UUID ownerId)
	{
		try {
			PreparedStatement pstmt = this.dogsDB.prepareStatement("SELECT uuid FROM dog WHERE id = ? AND owner = ? LIMIT 1");
			pstmt.setInt(1, dogIdentifier);
			pstmt.setString(2, ownerId.toString());
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				return new Dog(UUID.fromString(rs.getString("uuid")), ownerId);
			}
		}
		catch (SQLException e) {
			plugin.logDebug(e.getMessage());
		}
		return null;
	}

	public List<Dog> getDogs()
	{
		List<Dog> dogs = new ArrayList<>();

		try {
			PreparedStatement pstmt = this.dogsDB.prepareStatement("SELECT uuid FROM dog");
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				dogs.add(new Dog((Wolf) Objects.requireNonNull(plugin.getServer().getEntity(UUID.fromString(rs.getString("uuid"))))));
			}
		}
		catch (SQLException e) {
			plugin.logDebug(e.getMessage());
		}

		return dogs;
	}

	public List<Dog> getDogs(UUID ownerId)
	{
		List<Dog> dogs = new ArrayList<>();

		try {
			PreparedStatement pstmt = this.dogsDB.prepareStatement("SELECT uuid FROM dog WHERE owner = ?");
			pstmt.setString(1, ownerId.toString());
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				dogs.add(new Dog(UUID.fromString(rs.getString("uuid")), ownerId));
			}
		}
		catch (SQLException e) {
			plugin.logDebug(e.getMessage());
		}

		return dogs;
	}

	public String newDogName()
	{
		int dogNameNumber = random.nextInt(plugin.dogNames.size());
		return plugin.dogNames.get(dogNameNumber);
	}

	public boolean setNewId(Dog dog, int id)
	{
		// If another dog is already using the ID
		if (!dog.setIdentifier(id))
		{
			Dog dog2 = getDog(id, dog.getOwnerId());
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

	private int generateNewId(UUID dogOwnerId)
	{
		int id = 1;
		List<Dog> dogs = MyDog.getDogManager().getDogs(dogOwnerId);

		if (!dogs.isEmpty())
		{
			plugin.logDebug("Running new generator for ID");

			while (true)
			{
				plugin.logDebug("Running loop - Dogs size: " + dogs.size());
				boolean isUsed = false;
				for (Dog dog : dogs)
				{
					plugin.logDebug("Current dog: " + dog.getDogName() + " - " + dog.getIdentifier() + " ID to search: " + id);
					if (dog.getIdentifier() == id)
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
			String levelupString = plugin.levelUpString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName()).replace("{level}", Integer.toString(dog.getLevel()));
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

			dog.setDogName(dog.getDogName());

			owner.playSound(owner.getLocation(), sound, 3.0F, 1.0F);
		}

		dog.updateWolf();
	}
}