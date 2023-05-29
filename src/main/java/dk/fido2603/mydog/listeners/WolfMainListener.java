package dk.fido2603.mydog.listeners;

import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.objects.LevelFactory.Level;
import net.md_5.bungee.api.ChatColor;
import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.utils.TimeUtils;

import java.util.*;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class WolfMainListener implements Listener {
    private MyDog plugin = null;

    public WolfMainListener(MyDog p) {
        this.plugin = p;
    }

    @EventHandler
    public void onEntityTameEvent(EntityTameEvent event) {
        if (event.getEntity().getType() != EntityType.WOLF || !(event.getOwner() instanceof Player)) {
            return;
        }

        Player owner = (Player) event.getOwner();

        if (!MyDog.getDogManager().canTameMoreDogs(owner)) {
            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.tameLimitString.replace("{chatPrefix}", plugin.getChatPrefix())));
            event.setCancelled(true);
            return;
        }

        Wolf wolf = (Wolf) event.getEntity();

        // Make the task for getting the doggo, we want it to load in first...
        BukkitRunnable newTamedDog = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.logDebug("Running newTamedDog BukkitRunnable...");
                Dog dog = MyDog.getDogManager().newDog(wolf, owner);
                plugin.logDebug("New dog! Name: " + dog.getDogName() + " - DogId: " + dog.getDogId() + " - Owner: " + plugin.getServer().getPlayer(dog.getOwnerId()).getName() + " - OwnerId: " + dog.getOwnerId());
                Location dogLocation = dog.getDogLocation();
                plugin.logDebug("Dog Location = X: " + dogLocation.getX() + " Y: " + dogLocation.getY() + " Z: " + dogLocation.getZ());

                if (!dog.updateWolf()) {
                    plugin.logDebug("Could not set custom dog name, health and attack, cancelling event!");
                    event.setCancelled(true);
                    return;
                }
                plugin.logDebug("Finished setting custom dog name! Tame successful!");

				/*owner.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.GOLD + "Congratulations with your new dog, "
		+ dog.getDogColor() + dog.getDogName() + ChatColor.GOLD + "!");*/
                String newDogString = plugin.newDogString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName());
                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', newDogString));
            }
        };

        // Run the DoggoMaker task
        newTamedDog.runTaskLater(plugin, 2);
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        if (event.getEntity().getType() != EntityType.WOLF || !MyDog.getDogManager().isDog(event.getEntity().getUniqueId())) {
            return;
        }

        Dog dog = MyDog.getDogManager().getDog(event.getEntity().getUniqueId());
        Player owner = plugin.getServer().getPlayer(dog.getOwnerId());

        if (owner != null && owner.isOnline()) {
            Date dogBirthday = dog.getBirthday();
            Date today = new Date();
            long diff = Math.abs(today.getTime() - dogBirthday.getTime());
            String time = TimeUtils.parseMillisToUFString(diff);

            String levelText = "";
            if (plugin.useLevels) {
                //levelText = ", and got to " + ChatColor.DARK_RED + "Level " + dog.getLevel() + ChatColor.RED + ".";
                levelText = plugin.deadDogLevelString.replace("{level}", Integer.toString(dog.getLevel()));
            }

			/*owner.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Your dog, " + dog.getDogColor() + dog.getDogName() + ChatColor.RED + 
					", just passed away... " + dog.getDogColor() + dog.getDogName() + ChatColor.RED + " lived for " + time + levelText);*/
            String deadDogString = plugin.deadDogString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName()).replace("{time}", time).replace("{deadDogLevelString}", levelText);
            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', deadDogString));
        }

        MyDog.getDogManager().dogDied(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onWolfPlayerInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Wolf)) {
            return;
        }

        Dog dog = MyDog.getDogManager().getDog(entity.getUniqueId());

        if (dog == null) {
            plugin.logDebug("Dog is null!");
        }

        EquipmentSlot hand = event.getHand();
        Player player = event.getPlayer();
        Wolf wolf = (Wolf) entity;
        ItemStack item = null;

        if (hand.equals(EquipmentSlot.HAND)) {
            item = player.getEquipment().getItemInMainHand();
        } else if (hand.equals(EquipmentSlot.OFF_HAND)) {
            item = player.getEquipment().getItemInOffHand();
        } else {
            plugin.logDebug("No item in hand.");
        }

        saveDogLocation(dog, wolf);

        // 0 = OK
        // 1 = Failed
        // 2 = Cancel event
        if (onWolfPlayerInteractAlreadyTamed(dog, wolf, player) == 2) {
            plugin.logDebug("AlreadyTamed event: Cancelled");
            event.setCancelled(true);
            return;
        }

        if (onWolfPlayerInteractPet(dog, item, player) == 2) {
            plugin.logDebug("Petting event: Cancelled");
            event.setCancelled(true);
            return;
        }

        if (onWolfPlayerInteractChangeColor(dog, wolf, item, player) == 2) {
            plugin.logDebug("ChangeColor event: Cancelled");
            event.setCancelled(true);
            return;
        }

        if (onWolfPlayerInteractFeed(dog, wolf, item, player) == 2) {
            plugin.logDebug("Feed event: Cancelled");
            event.setCancelled(true);
            return;
        }

        if (onWolfPlayerInteractRename(dog, item, player) == 2) {
            plugin.logDebug("Rename event: Cancelled");
            event.setCancelled(true);
        }
    }

    public void saveDogLocation(Dog dog, Wolf wolf) {
        if (dog != null && wolf != null) {
            plugin.logDebug("Saved dog location!");
            dog.getDogLocation();
        }
    }

    public int onWolfPlayerInteractAlreadyTamed(Dog dog, Wolf wolf, Player player) {
        if (dog != null || player == null) {
            return 0; // OK
        }

        // Make the wolf into a dog, if it's tamed
        if (wolf.isValid() && wolf.isTamed() && wolf.getOwner() != null && wolf.getOwner() instanceof Player) {
            Player owner = (Player) wolf.getOwner();

            if (!owner.getUniqueId().equals(player.getUniqueId())) {
                return 1; // Error
            }

            if (!MyDog.getDogManager().canTameMoreDogs(owner)) {
                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.tameLimitString.replace("{chatPrefix}", plugin.getChatPrefix())));
                return 1; // Error
            }

            if ((wolf.getCustomName() == null || wolf.getCustomName().isEmpty()) && wolf.getCollarColor() == DyeColor.RED) {
                dog = MyDog.getDogManager().newDog(wolf, owner);
            } else if (wolf.getCustomName() == null || wolf.getCustomName().isEmpty()) {
                dog = MyDog.getDogManager().newDog(wolf, owner, null, wolf.getCollarColor());
            } else if ((wolf.getCustomName() != null && !wolf.getCustomName().isEmpty()) && wolf.getCollarColor() == DyeColor.RED) {
                dog = MyDog.getDogManager().newDog(wolf, owner, wolf.getCustomName(), null);
            } else if (wolf.getCustomName() != null && !wolf.getCustomName().isEmpty()) {
                dog = MyDog.getDogManager().newDog(wolf, owner, wolf.getCustomName(), wolf.getCollarColor());
            } else {
                plugin.logDebug("New already-tamed dog creation failed!");
                return 1; // Error
            }
            plugin.logDebug("New already-tamed dog! Name: " + dog.getDogName() + " - DogId: " + dog.getDogId() + " - Owner: " + plugin.getServer().getPlayer(dog.getOwnerId()).getName() + " - OwnerId: " + dog.getOwnerId());
            Location dogLocation = dog.getDogLocation();
            plugin.logDebug("Dog Location = X: " + dogLocation.getX() + " Y: " + dogLocation.getY() + " Z: " + dogLocation.getZ());

            if (!dog.updateWolf()) {
                return 1; // Error
            }

            String newDogString = plugin.newDogString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName());
            owner.sendMessage(ChatColor.translateAlternateColorCodes('&', newDogString));
        }
        return 0; // OK
    }

    public int onWolfPlayerInteractFeed(Dog dog, Wolf wolf, ItemStack item, Player player) {
        if (dog == null || item == null) {
            return 0; // OK
        }

        // Check for food
        double healthPoints = 0.0;
        switch (item.getType()) {
            case CHICKEN:
            case COOKED_CHICKEN:
                healthPoints = 1.0;
                break;
            case PORKCHOP:
            case COOKED_PORKCHOP:
            case BEEF:
            case COOKED_BEEF:
            case MUTTON:
            case COOKED_MUTTON:
            case RABBIT:
            case COOKED_RABBIT:
            case ROTTEN_FLESH:
                healthPoints = 2.0;
                break;
            default:
                if (player.isSneaking()) {
                    dog.pet(player);
                    return 2; // Cancel event
                }
                break;
        }

        if (healthPoints != 0.0) {
            plugin.logDebug("Item is food!");
            int dogsLevel = dog.getLevel();
            if (dogsLevel < 1) {
                plugin.logDebug("Level was under 1, setting level to 1");
                dogsLevel = 1;
            }

            Level level = plugin.dogLevels.get(dogsLevel);
            if (level == null) {
                plugin.logDebug("Level object is null, returning!");
                return 1; // Error
            }

            double health = level.health;
            if (health < 10.0) {
                health = 10.0;
            }

            AttributeInstance wolfMaxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);

            if (wolfMaxHealth.getValue() != health) {
                wolfMaxHealth.setBaseValue(health);
            }

            if (wolf.getHealth() >= 20.0 && wolf.getHealth() < health) {
                if (wolf.getHealth() + healthPoints > health) {
                    wolf.setHealth(health);
                } else {
                    wolf.setHealth(wolf.getHealth() + healthPoints);
                }
                plugin.logDebug("Gave the dog, " + dog.getDogName() + ", " + healthPoints + " in health.");
                if (player.getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
                return 2; // Cancel event
            }
        }
        return 0; // OK
    }

    public int onWolfPlayerInteractChangeColor(Dog dog, Wolf wolf, ItemStack item, Player player) {
        if (dog == null || wolf == null || item == null || player == null) {
            return 0; // OK
        }

        if (!dog.getOwnerId().equals(player.getUniqueId())) {
            return 1; // Error
        }

        DyeColor dc = null;
        switch (item.getType()) {
            case BLACK_DYE:
                dc = DyeColor.BLACK;
                break;
            case BLUE_DYE:
                dc = DyeColor.BLUE;
                break;
            case BROWN_DYE:
                dc = DyeColor.BROWN;
                break;
            case CYAN_DYE:
                dc = DyeColor.CYAN;
                break;
            case GRAY_DYE:
                dc = DyeColor.GRAY;
                break;
            case GREEN_DYE:
                dc = DyeColor.GREEN;
                break;
            case LIGHT_BLUE_DYE:
                dc = DyeColor.LIGHT_BLUE;
                break;
            case LIGHT_GRAY_DYE:
                dc = DyeColor.LIGHT_GRAY;
                break;
            case LIME_DYE:
                dc = DyeColor.LIME;
                break;
            case MAGENTA_DYE:
                dc = DyeColor.MAGENTA;
                break;
            case ORANGE_DYE:
                dc = DyeColor.ORANGE;
                break;
            case PINK_DYE:
                dc = DyeColor.PINK;
                break;
            case PURPLE_DYE:
                dc = DyeColor.PURPLE;
                break;
            case RED_DYE:
                dc = DyeColor.RED;
                break;
            case YELLOW_DYE:
                dc = DyeColor.YELLOW;
                break;
            case WHITE_DYE:
                dc = DyeColor.WHITE;
                break;
            default:
                break;
        }

        if (dc != null) {
            // Set collar color

            if (wolf.getCollarColor().equals(dc)) {
                plugin.logDebug("Collar color is the same as dye color, returning!");
                return 0; // OK
            }

            dog.setDogColor(dc);
        }
        return 0; // OK
    }

    public int onWolfPlayerInteractPet(Dog dog, ItemStack item, Player player) {
        if (dog == null || item == null || player == null) {
            return 0; // OK
        }

        if (item.getType().equals(Material.AIR)) {
            if (player.isSneaking()) {
                dog.pet(player);
                return 2; // Cancel event
            }
        }
        return 0; // OK
    }

    public int onWolfPlayerInteractRename(Dog dog, ItemStack item, Player player) {
        if (dog == null || item == null || player == null) {
            return 0; // OK
        }

        // Check if the player has a name_tag equipped
        if (item.getType().equals(Material.NAME_TAG) && item.getItemMeta().hasDisplayName()) {
            if (!plugin.allowNametagRename || !dog.getOwnerId().equals(player.getUniqueId())) {
                plugin.logDebug("NametagRename is disabled or not owner trying to rename dog!");
                return 2; // Cancel event
            }

            dog.setDogName(item.getItemMeta().getDisplayName());
            plugin.logDebug("Set the Dog's name to: " + item.getItemMeta().getDisplayName());
            if (player.getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
            return 2; // Cancel event
        }
        return 0; // OK
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreedEvent(EntityBreedEvent event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Wolf) || /*(!MyDog.getDogManager().isDog(event.getMother().getUniqueId())) || (!MyDog.getDogManager().isDog(event.getFather().getUniqueId())) ||*/ !(event.getBreeder() instanceof Player)) {
            plugin.logDebug("Entity breed return!");
            return;
        }

        Wolf wolf = (Wolf) event.getEntity();
        Player ownerFind = null;

        if (wolf.getOwner() != null) {
            ownerFind = (Player) wolf.getOwner();
        } else {
            ownerFind = (Player) event.getBreeder();
        }

        if (ownerFind == null) {
            plugin.logDebug("Dog owner is null, returning!");
            return;
        }

        final Player owner = ownerFind;

        // Make the task for getting the doggo, we want it to load in first...
        BukkitRunnable newDogBreed = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.logDebug("Running newDogBreed BukkitRunnable...");
                Dog dog = MyDog.getDogManager().newDog(wolf, owner);
                plugin.logDebug("New dog! Name: " + dog.getDogName() + " - DogId: " + dog.getDogId() + " - Owner: " + plugin.getServer().getPlayer(dog.getOwnerId()).getName() + " - OwnerId: " + dog.getOwnerId());
                Location dogLocation = dog.getDogLocation();
                plugin.logDebug("Dog Location = X: " + dogLocation.getX() + " Y: " + dogLocation.getY() + " Z: " + dogLocation.getZ());

                if (!dog.updateWolf()) {
                    plugin.logDebug("Could not set custom dog name, health and attack, cancelling event!");
                    event.setCancelled(true);
                    return;
                }
                plugin.logDebug("Finished setting custom dog name! Breed sucessfull!");

				/*owner.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.GOLD + "Congratulations with your new dog, "
						+ dog.getDogColor() + dog.getDogName() + ChatColor.GOLD + "!");*/
                String newDogString = plugin.newDogString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName());
                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', newDogString));
            }
        };

        // Run the DoggoMaker task
        newDogBreed.runTaskLater(plugin, 2);
    }

    // package-private
    static void checkForDogs(Entity[] entities) {
        for (Entity e : entities) {
            if (e != null && e.getType().equals(EntityType.WOLF)) {
                String customName = "UNKNOWN NAME";
                if (e.getCustomName() != null) {
                    customName = e.getCustomName();
                }
                MyDog.instance().logDebug("There is a wolf in the loaded chunk! Name: " + customName);

                Wolf dog = (Wolf) e;
                if (MyDog.getDogManager().isDog(dog.getUniqueId())) {
                    MyDog.instance().logDebug("Updated loaded wolf with health and damage!");
                    MyDog.getDogManager().getDog(dog.getUniqueId()).updateWolf();
                }
            }
        }
    }


//	@EventHandler(priority = EventPriority.HIGHEST)
//	public void onChunkPopulate(ChunkPopulateEvent event)
//	{
//		checkChunkDogs(event.getChunk());
//	}

    // Load dogs
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        Entity[] entities = event.getChunk().getEntities();
        if (entities.length != 0) {
            checkForDogs(entities);
        }
    }

    // If the player is teleporting, this would be used in regions that might be loaded already by other players, or spawn regions
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.automaticTeleportation) {
            return;
        }

        Player player = event.getPlayer();

        List<Entity> entities = new ArrayList<>();

        // Check whether the player has any dogs
        for (Dog dog : MyDog.getDogManager().getAliveDogs(player.getUniqueId())) {
            Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
            if (wolf != null && !wolf.isSilent()) {
                entities.add(wolf);
            }
        }

        if (plugin.experimentalTeleport) {
            MyDog.getTeleportationManager().teleportEntities(entities, event.getTo(), "PlayerTeleport");
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    MyDog.getTeleportationManager().doTeleportEntities(entities, event.getTo());
                }
            }.runTaskLater(this.plugin, 3);
        }

    }

    // If a chunk is unloading, check if there are any tameables inside it
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!plugin.automaticTeleportation) {
            return;
        }

        List<Entity> entities = Arrays.asList(event.getChunk().getEntities());

        if (plugin.experimentalTeleport) {
            MyDog.getTeleportationManager().teleportEntities(entities, null, "ChunkUnload");
        } else {
            MyDog.getTeleportationManager().doTeleportEntities(entities, null);
        }
    }

    // When a dog enters a portal, deny the teleport, since it's annoying
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortalTeleport(EntityPortalEvent event) {
        if (!plugin.automaticTeleportation) {
            return;
        }

        Entity entity = event.getEntity();

        if (MyDog.getDogManager().isDog(entity.getUniqueId())) {
            // Make sure that the portal isn't the end portal... pretty sure there's lava under that lol
            if (event.getTo() != null && event.getTo().getWorld() != null) {
                if (event.getTo().getWorld().getEnvironment() != World.Environment.THE_END) {
                    MyDog.instance().logDebug("Stopped a dog from teleporting through a portal.");
                    event.setCancelled(true);
                }
            }
        }
    }
}
