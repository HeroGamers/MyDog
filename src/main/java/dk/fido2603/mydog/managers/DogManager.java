package dk.fido2603.mydog.managers;

import java.io.File;
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
import org.bukkit.scheduler.BukkitRunnable;

public class DogManager {
    private final MyDog plugin;
    private FileConfiguration dogsConfig = null;
    private File dogsConfigFile = null;
    private final Random random = new Random();
    private long lastSaveTime = 0L;

    private final HashMap<UUID, HashMap<String, Object>> dogTrades = new HashMap<>();

    public DogManager(MyDog plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (this.dogsConfigFile == null) {
            this.dogsConfigFile = new File(this.plugin.getDataFolder(), "dogs.yml");
        }
        this.dogsConfig = YamlConfiguration.loadConfiguration(this.dogsConfigFile);
        this.plugin.log("Loaded " + this.dogsConfig.getKeys(false).size() + " dogs.");
    }

    public void save() {
        this.lastSaveTime = System.currentTimeMillis();
        if ((this.dogsConfig == null) || (this.dogsConfigFile == null)) {
            return;
        }
        try {
            this.dogsConfig.save(this.dogsConfigFile);
        } catch (Exception ex) {
            this.plugin.log("Could not save config to " + this.dogsConfigFile + ": " + ex.getMessage());
        }
    }

    public void saveTimed() {
        if (plugin.instantSave) {
            save();
            return;
        }

        if (System.currentTimeMillis() - this.lastSaveTime < 180000L) {
            return;
        }

        save();
    }

    public FileConfiguration getDogsConfig() {
        return dogsConfig;
    }

    public boolean isDog(UUID dogId) {
        if (!(plugin.getServer().getEntity(dogId) instanceof Wolf)) {
            return false;
        }
        return dogsConfig.contains(dogId.toString());
    }

    public void removeDog(UUID dogId) {
        if (dogsConfig.contains(dogId.toString())) {
            dogsConfig.set(dogId.toString(), null);
            saveTimed();
        }
    }

    public boolean isUUIDDeadDog(UUID uuid) {
        return dogsConfig.contains(uuid.toString()) && dogsConfig.getBoolean(uuid + ".isDead");
    }

    public void dogDied(UUID dogId) {
        if (plugin.allowRevival) {
            Dog dog = getDog(dogId);
            if (dog != null) {
                dog.setDead(true);
            }
        } else {
            removeDog(dogId);
        }
    }

    public int dogsOwned(Player player) {
        return dogsOwned(player.getUniqueId());
    }

    public int dogsOwned(UUID playerId) {
        int dogs = 0;
        for (String dogUUID : dogsConfig.getKeys(false)) {
            plugin.logDebug(dogUUID);
            String ownerIdString = dogsConfig.getString(dogUUID + ".Owner");
            if (ownerIdString != null) {
                UUID ownerId = UUID.fromString(ownerIdString);
                if (ownerId.equals(playerId)) {
                    dogs++;
                }
            }
        }
        return dogs;
    }

//    public void changeDogUUID(UUID oldDogID, UUID newDogID) {
//        Dog oldDog = getDog(oldDogID);
//        Dog newDog = null;
//        if (oldDog != null) {
//
//        }
//    }

    public boolean canTameMoreDogs(Player player) {
        if (player.isOp()) {
            return true;
        }

        int currentlyOwned = getDogs(player.getUniqueId()).size();
        for (int i = 0; i <= currentlyOwned; i++) {
            if (MyDog.getPermissionsManager().hasPermission(player, "mydog.limit." + i)) {
                return false;
            }
        }
        return true;
    }

    public Dog newDog(Wolf dog, Player dogOwner) {
        int dogID = generateNewId(dogOwner.getUniqueId());
        return new Dog(dog, dogOwner, dogID, 1);
    }

    public Dog newDog(Wolf dog, Player dogOwner, String customName, DyeColor collarColor) {
        int dogID = generateNewId(dogOwner.getUniqueId());
        return new Dog(dog, dogOwner, customName, collarColor, dogID, null);
    }

    public UUID getDogOwnerId(UUID dogId) {
        if (!dogsConfig.contains(dogId.toString())) {
            return null;
        }
        String ownerIdString = dogsConfig.getString(dogId + ".Owner");
        if (ownerIdString == null) {
            return null;
        }
        return UUID.fromString(ownerIdString);
    }

    public Dog getDog(UUID dogId) {
        if (isDog(dogId)) {
            return new Dog(dogId, getDogOwnerId(dogId));
        }
        return null;
    }

    public Dog getDog(int dogIdentifier, UUID ownerId) {
        for (String dogIdString : dogsConfig.getKeys(false)) {
            if (Objects.equals(dogsConfig.getString(dogIdString + ".ID"), Integer.toString(dogIdentifier)) && ownerId.equals(getDogOwnerId(UUID.fromString(dogIdString)))) {
                UUID dogId = UUID.fromString(dogIdString);
                return new Dog(dogId, ownerId);
            }
        }

        return null;
    }

    public List<Dog> getDogs() {
        List<Dog> dogs = new ArrayList<>();

        for (String dogIdString : dogsConfig.getKeys(false)) {
            UUID dogId = UUID.fromString(dogIdString);
            dogs.add(new Dog((Wolf) Objects.requireNonNull(plugin.getServer().getEntity(dogId))));
        }

        return dogs;
    }

    public List<Dog> getDogs(UUID ownerId) {
        List<Dog> dogs = new ArrayList<>();

        for (String dogIdString : dogsConfig.getKeys(false)) {
            if (ownerId.equals(getDogOwnerId(UUID.fromString(dogIdString)))) {
                UUID dogId = UUID.fromString(dogIdString);
                dogs.add(new Dog(dogId, ownerId));
            }
        }

        return dogs;
    }

    public List<Dog> getAliveDogs(UUID ownerId) {
        List<Dog> dogs = new ArrayList<>();
        for (Dog dog : getDogs(ownerId)) {
            if (!dog.isDead()) {
                dogs.add(dog);
            }
        }
        return dogs;
    }

    public List<Dog> getDeadDogs(UUID ownerId) {
        List<Dog> dogs = new ArrayList<>();
        for (Dog dog : getDogs(ownerId)) {
            if (dog.isDead()) {
                dogs.add(dog);
            }
        }
        return dogs;
    }

    public boolean handleNewTrade(Dog dog, Player receiver, double price) {
        if (dogTrades.containsKey(receiver.getUniqueId())) {
            return false;
        }

        if (!canTameMoreDogs(receiver)) {
            return false;
        }

        HashMap<String, Object> dogTrade = new HashMap<>();
        dogTrade.put("time", System.currentTimeMillis());
        dogTrade.put("dog", dog.getDogId());
        dogTrade.put("price", price);

        dogTrades.put(receiver.getUniqueId(), dogTrade);

        // Delete the trade after 30 seconds if it still exists
        new BukkitRunnable() {
            @Override
            public void run() {
                if (hasTrade(receiver.getUniqueId())) {
                    dogTrades.remove(receiver.getUniqueId());
                    receiver.sendMessage(ChatColor.GOLD + "Dog trade expired!");

                    Player sender = plugin.getServer().getPlayer(dog.getOwnerId());
                    if (sender != null) {
                        sender.sendMessage(ChatColor.GOLD + "Recipient didn't answer to dog trade!");
                    }
                }
            }
        }.runTaskLaterAsynchronously(plugin, 20L * 30L);

        return true;
    }

    public boolean hasTrade(UUID recipient) {
        return dogTrades.containsKey(recipient);
    }

    public HashMap<String, Object> getTrade(UUID recipient) {
        if (!hasTrade(recipient)) {
            return new HashMap<>();
        }

        return dogTrades.get(recipient);
    }

    public Dog getTradeDog(UUID recipient) {
        HashMap<String, Object> trade = getTrade(recipient);
        if (trade == null || trade.isEmpty()) {
            return null;
        }

        return getDog((UUID) trade.get("dog"));
    }

    public Double getTradePrice(UUID recipient) {
        if (!hasTrade(recipient)) {
            return null;
        }

        return (double) dogTrades.get(recipient).get("price");
    }

    public boolean acceptTrade(Player accepter) {
        HashMap<String, Object> dogTrade = getTrade(accepter.getUniqueId());

        if (dogTrade == null || dogTrade.isEmpty()) {
            return false;
        }

        Dog dog = getDog((UUID) dogTrade.get("dog"));

        if (dog == null) {
            return false;
        }

        double price = (double) dogTrade.get("price");
        if (price > 0.0D) {
            if (MyDog.getEconomy() == null) {
                plugin.log("No economy provider, failed to trade dogs!");
                return false;
            }

            if (!MyDog.getEconomy().has(accepter, price)) {
                return false;
            }
            MyDog.getEconomy().withdrawPlayer(accepter, price);
        }

        dog.setOwner(accepter);

        dogTrades.remove(accepter.getUniqueId());
        return true;
    }

    public boolean denyTrade(Player denier) {
        if (!hasTrade(denier.getUniqueId())) {
            return false;
        }

        dogTrades.remove(denier.getUniqueId());
        return true;
    }

    public String newDogName() {
        int dogNameNumber = random.nextInt(plugin.dogNames.size());
        return plugin.dogNames.get(dogNameNumber);
    }

    public boolean setNewId(Dog dog, int id) {
        // If another dog is already using the ID
        if (!dog.setIdentifier(id)) {
            Dog dog2 = getDog(id, dog.getOwnerId());
            if (dog2.setIdentifier(generateNewId(dog.getOwnerId()))) {
                return dog.setIdentifier(id);
            }
        } else {
            return true;
        }
        return false;
    }

    public int generateNewId(UUID dogOwnerId) {
        int id = 1;
        List<Dog> dogs = MyDog.getDogManager().getDogs(dogOwnerId);

        if (!dogs.isEmpty()) {
            plugin.logDebug("Running new generator for ID");

            while (true) {
                plugin.logDebug("Running loop - Dogs size: " + dogs.size());
                boolean isUsed = false;
                for (Dog dog : dogs) {
                    plugin.logDebug("Current dog: " + dog.getDogName() + " - " + dog.getIdentifier() + " ID to search: " + id);
                    if (dog.getIdentifier() == id) {
                        plugin.logDebug("ID already used - ID: " + id);
                        isUsed = true;
                        break;
                    }
                }
                if (!isUsed) {
                    plugin.logDebug("Found a free ID: " + id);
                    break;
                }
                id++;
            }
            plugin.logDebug("ok");
        } else {
            plugin.logDebug("Dogs list is empty!");
        }

        plugin.logDebug("Returning ID: " + id);
        return id;
    }

    public void handleNewLevel(Dog dog) {
        plugin.logDebug("Dog levelup! Level before: " + (dog.getLevel() - 1) + " - Level now: " + dog.getLevel());
        UUID ownerId = dog.getOwnerId();
        Player owner = plugin.getServer().getPlayer(ownerId);
        if (owner != null && owner.isOnline()) {
            String levelupString = plugin.levelUpString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName()).replace("{level}", Integer.toString(dog.getLevel()));
			/*owner.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.DARK_PURPLE + "Your dog, "
					+ dog.getDogColor() + dog.getDogName() + ChatColor.DARK_PURPLE + ", just leveled up to " + ChatColor.LIGHT_PURPLE + "Level " + dog.level + ChatColor.DARK_PURPLE + "!");*/
            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', levelupString));

            MyDog.getParticleUtils().newLevelUpParticle(plugin.getServer().getEntity(dog.getDogId()));
            Sound sound;
            if (plugin.levelUpSound == null) {
                plugin.logDebug("Couldn't load the levelup sound, took Howl!");
                sound = Sound.ENTITY_WOLF_HOWL;
            } else {
                sound = Sound.valueOf(plugin.levelUpSound);
            }

            owner.playSound(owner.getLocation(), sound, 3.0F, 1.0F);
        }

        dog.updateWolf();
    }
}