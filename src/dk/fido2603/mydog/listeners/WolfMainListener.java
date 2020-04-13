package dk.fido2603.mydog.listeners;

import dk.fido2603.mydog.DogManager.Dog;
import dk.fido2603.mydog.LevelFactory.Level;
import net.md_5.bungee.api.ChatColor;
import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.utils.TimeUtils;

import java.util.Date;
import java.util.HashMap;

import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class WolfMainListener implements Listener
{
	private MyDog	plugin	= null;

	public WolfMainListener(MyDog p)
	{
		this.plugin = p;
	}

	@EventHandler
	public void onEntityTameEvent(EntityTameEvent event)
	{
		if (event.getEntity().getType() != EntityType.WOLF || !(event.getOwner() instanceof Player))
		{
			return;
		}

		Wolf wolf = (Wolf) event.getEntity();
		Player owner = (Player) event.getOwner();

		Dog dog = MyDog.getDogManager().newDog(wolf, owner);
		plugin.logDebug("New dog! Name: " + dog.getDogName() + " - DogId: " + dog.getDogId() + " - Owner: " + plugin.getServer().getPlayer(dog.getOwnerId()).getName() + " - OwnerId: " + dog.getOwnerId());
		Location dogLocation = dog.getDogLocation();
		plugin.logDebug("Dog Location = X: " + dogLocation.getX() + " Y: " + dogLocation.getY() + " Z: " + dogLocation.getZ());
		
		if (!dog.setDogCustomName())
		{
			event.setCancelled(true);
			return;
		}

		/*owner.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.GOLD + "Congratulations with your new dog, "
		+ dog.getDogColor() + dog.getDogName() + ChatColor.GOLD + "!");*/
		String newDogString = plugin.newDogString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName());
		owner.sendMessage(ChatColor.translateAlternateColorCodes('&', newDogString));
	}

	@EventHandler
	public void onEntityDeathEvent(EntityDeathEvent event)
	{
		if (event.getEntity().getType() != EntityType.WOLF || !MyDog.getDogManager().isDog(event.getEntity().getUniqueId()))
		{
			return;
		}

		Dog dog = MyDog.getDogManager().getDog(event.getEntity().getUniqueId());
		Player owner = plugin.getServer().getPlayer(dog.getOwnerId());
		
		if (owner.isOnline())
		{
			Date dogBirthday = dog.getBirthday();
			Date today = new Date();
			long diff = Math.abs(today.getTime() - dogBirthday.getTime());
			String time = TimeUtils.parseMillisToUFString(diff);

			String levelText = "";
			if (plugin.useLevels)
			{
				//levelText = ", and got to " + ChatColor.DARK_RED + "Level " + dog.getLevel() + ChatColor.RED + ".";
				levelText = plugin.deadDogLevelString.replace("{level}", dog.getLevel().toString());
			}

			/*owner.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Your dog, " + dog.getDogColor() + dog.getDogName() + ChatColor.RED + 
					", just passed away... " + dog.getDogColor() + dog.getDogName() + ChatColor.RED + " lived for " + time + levelText);*/
			String deadDogString = plugin.deadDogString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName()).replace("{time}", time).replace("{deadDogLevelString}", levelText);
			owner.sendMessage(ChatColor.translateAlternateColorCodes('&', deadDogString));
		}
	
		MyDog.getDogManager().removeDog(event.getEntity().getUniqueId());
	}

	@EventHandler
	public void onWolfSit(PlayerInteractEntityEvent event)
	{
		Entity entity = event.getRightClicked();
		if (!(entity instanceof Sittable) || !(entity instanceof Wolf))
		{
			return;
		}

		if (MyDog.getDogManager().isDog(entity.getUniqueId()))
		{
			Dog dog = MyDog.getDogManager().getDog(entity.getUniqueId());

			if (!((Sittable) entity).isSitting())
			{
				plugin.logDebug("Saved dog location!");
				dog.getDogLocation();
			}
		}
	}

	@EventHandler
	public void onWolfPlayerInteract(PlayerInteractEntityEvent event)
	{
		Entity entity = event.getRightClicked();
		if (!(entity instanceof Wolf))
		{
			return;
		}

		if (!MyDog.getDogManager().isDog(entity.getUniqueId()))
		{
			// Make the wolf into a dog, if it's tamed
			Wolf wolf = (Wolf) entity;
			if (wolf.isValid() && wolf.isTamed() && wolf.getOwner() != null && wolf.getOwner() instanceof Player)
			{
				Player owner = (Player) wolf.getOwner();
				Dog dog = null;

				if ((wolf.getCustomName() == null || wolf.getCustomName().isEmpty()) && wolf.getCollarColor() == DyeColor.RED)
				{
					dog = MyDog.getDogManager().newDog(wolf, owner);
				}
				else if (wolf.getCustomName() == null || wolf.getCustomName().isEmpty())
				{
					dog = MyDog.getDogManager().newDog(wolf, owner, null, wolf.getCollarColor());
				}
				else if ((wolf.getCustomName() != null && !wolf.getCustomName().isEmpty()) && wolf.getCollarColor() == DyeColor.RED)
				{
					dog = MyDog.getDogManager().newDog(wolf, owner, wolf.getCustomName(), null);
				}
				else if (wolf.getCustomName() != null && !wolf.getCustomName().isEmpty())
				{
					dog = MyDog.getDogManager().newDog(wolf, owner, wolf.getCustomName(), wolf.getCollarColor());
				}
				else
				{
					plugin.logDebug("New already-tamed dog creation failed!");
					return;
				}
				plugin.logDebug("New already-tamed dog! Name: " + dog.getDogName() + " - DogId: " + dog.getDogId() + " - Owner: " + plugin.getServer().getPlayer(dog.getOwnerId()).getName() + " - OwnerId: " + dog.getOwnerId());
				Location dogLocation = dog.getDogLocation();
				plugin.logDebug("Dog Location = X: " + dogLocation.getX() + " Y: " + dogLocation.getY() + " Z: " + dogLocation.getZ());
				
				if (!dog.setDogCustomName())
				{
					event.setCancelled(true);
					return;
				}

				String newDogString = plugin.newDogString.replace("{chatPrefix}", plugin.getChatPrefix()).replace("{dogNameColor}", "&" + dog.getDogColor().getChar()).replace("{dogName}", dog.getDogName());
				owner.sendMessage(ChatColor.translateAlternateColorCodes('&', newDogString));
			}
		}

		Dog dog = MyDog.getDogManager().getDog(entity.getUniqueId());

		if (dog == null)
		{
			plugin.logDebug("Dog is null, returning!");
			return;
		}

		EquipmentSlot hand = event.getHand();
		Player player = event.getPlayer();
		Wolf wolf = (Wolf) entity;
		ItemStack item = null;

		if (hand.equals(EquipmentSlot.HAND))
		{
			item = player.getEquipment().getItemInMainHand();
		}
		else if (hand.equals(EquipmentSlot.OFF_HAND))
		{
			item = player.getEquipment().getItemInOffHand();
		}
		else
		{
			plugin.logDebug("No item in hand, returning!");
			return;
		}

		if (item.getType() == Material.AIR)
		{
			plugin.logDebug("Item is null, retuning!");
			return;
		}

		DyeColor dc = null;
		switch (item.getType())
		{
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

		if (dc == null)
		{
			// Check if the player has a name_tag equipped
			if (item.getType().equals(Material.NAME_TAG) && item.getItemMeta().hasDisplayName())
			{
				if (!plugin.allowNametagRename)
				{
					plugin.logDebug("NametagRename is disabled, cancelling interact event!");
					event.setCancelled(true);
					return;
				}

				dog.setDogName(item.getItemMeta().getDisplayName());
				plugin.logDebug("Set the Dog's name to: " + item.getItemMeta().getDisplayName());
				if (player.getGameMode() != GameMode.CREATIVE)
				{
					item.setAmount(item.getAmount()-1);
				}
				event.setCancelled(true);
				return;
			}
			plugin.logDebug("Item is not a name-tag!");

			// Check for food
			double healthPoints = 0.0;
			switch (item.getType())
			{
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
				break;
			}

			if (healthPoints != 0.0)
			{
				plugin.logDebug("Item is food!");
				Integer dogsLevel = dog.getLevel();
				if (dogsLevel == null || dogsLevel < 1)
				{
					plugin.logDebug("Level was under 1 or null, setting level to 1");
					dogsLevel = 1;
				}

				Level level = plugin.dogLevels.get(dogsLevel);
				if (level == null)
				{
					plugin.logDebug("Level object is null, returning!");
					return;
				}

				double health = level.health;
				if (health < 10.0)
				{
					health = 10.0;
				}

				AttributeInstance wolfMaxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);

				if (wolfMaxHealth.getValue() != health)
				{
					wolfMaxHealth.setBaseValue(health);
				}

				if (wolf.getHealth() >= 20.0 && wolf.getHealth() < health)
				{
					if (wolf.getHealth() + healthPoints > health)
					{
						wolf.setHealth(health);
					}
					else
					{
						wolf.setHealth(wolf.getHealth() + healthPoints);
					}
					plugin.logDebug("Gave the dog, " + dog.getDogName() + ", " + healthPoints + " in health.");
					if (player.getGameMode() != GameMode.CREATIVE)
					{
						item.setAmount(item.getAmount()-1);
					}
					event.setCancelled(true);
				}
				return;
			}
			plugin.logDebug("Item is not food!");
			return;
		}
		else
		{
			// Set collar color

			if (wolf.getCollarColor().equals(dc))
			{
				plugin.logDebug("Collar color is the same as dye color, returning!");
				return;
			}

			dog.setDogColor(dc);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBreedEvent(EntityBreedEvent event)
	{
		if (event.getEntity() == null || !(event.getEntity() instanceof Wolf) || /*(!MyDog.getDogManager().isDog(event.getMother().getUniqueId())) || (!MyDog.getDogManager().isDog(event.getFather().getUniqueId())) ||*/ !(event.getBreeder() instanceof Player))
		{
			plugin.logDebug("Entity breed return!");
			return;
		}

		Wolf wolf = (Wolf) event.getEntity();
		Player ownerFind = null;

		if (wolf.getOwner() != null)
		{
			ownerFind = (Player) wolf.getOwner();
		}
		else
		{
			ownerFind = (Player) event.getBreeder();
		}

		if (ownerFind == null)
		{
			plugin.logDebug("Dog owner is null, returning!");
			return;
		}

		final Player owner = ownerFind;

		// Make the task for getting the doggo, we want it to load in first...
		BukkitRunnable newDogBreed = new BukkitRunnable()
		{
			@Override
			public void run() {
				plugin.logDebug("Running newDogBreed BukkitRunnable...");
				Dog dog = MyDog.getDogManager().newDog(wolf, owner);
				plugin.logDebug("New dog! Name: " + dog.getDogName() + " - DogId: " + dog.getDogId() + " - Owner: " + plugin.getServer().getPlayer(dog.getOwnerId()).getName() + " - OwnerId: " + dog.getOwnerId());
				Location dogLocation = dog.getDogLocation();
				plugin.logDebug("Dog Location = X: " + dogLocation.getX() + " Y: " + dogLocation.getY() + " Z: " + dogLocation.getZ());
				
				if (!dog.setDogCustomName())
				{
					plugin.logDebug("Could not set custom dog name, cancelling event!");
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

	// Load dogs
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChunkLoad(ChunkLoadEvent event)
	{
		Entity[] entities = event.getChunk().getEntities();
		for (Entity e : entities)
		{
			if (e != null && e.getType().equals(EntityType.WOLF))
			{
				String customName = "UNKNOWN NAME";
				if (e.getCustomName() != null)
				{
					customName = e.getCustomName();
				}
				plugin.logDebug("There is a wolf in the loaded chunk! Name: " + customName);

				Wolf dog = (Wolf) e;
				if (MyDog.getDogManager().isDog(dog.getUniqueId()))
				{
					plugin.logDebug("Updated loaded wolf with health and damage!");
					MyDog.getDogManager().getDog(dog.getUniqueId()).updateWolf();
				}
			}
		}
	}

	// If the player is teleporting, this would be used in regions that might be loaded already by other players, or spawn regions
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if (!plugin.automaticTeleportation)
		{
			return;
		}

		Player player = event.getPlayer();

		// Do the teleport task 3 ticks after the player has teleported
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				Location safeLocation = null;

				// Check whether the player has any dogs
				for (Dog dog : MyDog.getDogManager().getDogs(player.getUniqueId()))
				{
					Wolf wolf = (Wolf) plugin.getServer().getEntity(dog.getDogId());
					if (wolf != null && !wolf.isSilent())
					{
						HashMap<Boolean, Location> teleportResult = teleportTameable(wolf, safeLocation, event.getTo());

						// If the first entity didn't find a safe location
						Boolean triedTeleporting = (Boolean) teleportResult.keySet().toArray()[0];
						safeLocation = (Location) teleportResult.values().toArray()[0];

						if (triedTeleporting && safeLocation == null)
						{
							return;
						}
						else if (triedTeleporting && safeLocation != null)
						{
							plugin.logDebug("Teleported a dog successfully, playerteleport!");
						}
					}
				}
			}
		}.runTaskLater(this.plugin, 3);
	}

	// If a chunk is unloading, check if there are any tameables inside it
	@EventHandler(priority = EventPriority.LOWEST)
	public void onChunkUnload(ChunkUnloadEvent event)
	{
		if (!plugin.automaticTeleportation)
		{
			return;
		}

		if (event.getChunk().getEntities() == null)
		{
			return;
		}

		Location safeLocation = null;

		Entity[] entities = event.getChunk().getEntities();
		for (Entity e : entities)
		{
			// All tameables
			if (e != null && e instanceof Sittable && e instanceof Tameable && !((Sittable)e).isSitting())
			{
				HashMap<Boolean, Location> teleportResult = teleportTameable(e, safeLocation, null);

				// If the first entity didn't find a safe location
				Boolean triedTeleporting = (Boolean) teleportResult.keySet().toArray()[0];
				safeLocation = (Location) teleportResult.values().toArray()[0];

				if (triedTeleporting && safeLocation == null)
				{
					return;
				}
				else if (triedTeleporting && safeLocation != null)
				{
					plugin.logDebug("Teleported a dog successfully, chunkunload!");
				}
			}
		}
	}

	public HashMap<Boolean, Location> teleportTameable(Entity e, Location safeLocation, Location searchLocation)
	{
		// We use this map to store whether it even tried searching for a location, and then to return with a safe location, so we don't have to search again
		HashMap<Boolean, Location> teleportResult = new HashMap<>();

		Tameable tameableEntity = (Tameable) e;

		if (tameableEntity.getOwner() instanceof Player)
		{
			Sittable sittingEntity = (Sittable) e;
			Player player = (Player) tameableEntity.getOwner();

			if (player != null && player.isOnline() && MyDog.getPermissionsManager().hasPermission(player, "mydog.teleport"))
			{
				// If it's a dog, or if the config allows all tameables to teleport
				Boolean isDog = (e.getType().equals(EntityType.WOLF) && MyDog.getDogManager().isDog(tameableEntity.getUniqueId()));
				if (isDog || plugin.teleportAllTameables)
				{
					// If the tameable is sitting, or is in another world
					if (!sittingEntity.isSitting() || (!tameableEntity.getWorld().equals(player.getWorld()) && plugin.teleportOnWorldChange))
					{
						// If dog, save location of the dog
						if (isDog)
						{
							MyDog.getDogManager().getDog(tameableEntity.getUniqueId()).saveDogLocation();
						}

						// Begin teleport procedure!!
						if (safeLocation == null) {
							// If a search location is provided, like a player that is starting to teleport
							if (searchLocation == null)
							{
								searchLocation = player.getLocation();
							}
							if (!isSafeLocation(searchLocation))
							{
								plugin.logDebug("Whoops, seems like our player isn't at a safe location, let's find a good spot for the tameable...");
								searchLocation = searchSafeLocation(searchLocation);
								if (searchLocation == null)
								{
									plugin.logDebug("Did not find a safe place to teleport the tameable! Keeping tameable at unloaded chunks!");
									player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.cannotTeleportTameableString.replace("{chatPrefix}", plugin.getChatPrefix())));
									teleportResult.put(true, null);
									return teleportResult;
								}
							}
							safeLocation = searchLocation;
						}

						plugin.logDebug("It's a safe location, teleporting!");
						tameableEntity.teleport(safeLocation);
						if (sittingEntity.isSitting())
						{
							sittingEntity.setSitting(false);
						}

						// Return the found location
						teleportResult.put(true, safeLocation);
						return teleportResult;
					}
				}
			}
		}
		teleportResult.put(false, safeLocation);
		return teleportResult;
	}

	public Location searchSafeLocation(Location loc)
	{
		if (plugin.expandedSearch)
		{
			double y;
			double x;
			double z;
			plugin.logDebug("Starting safe location search!");
			for (z = 0; z <= 2; z++)
			{
				loc.setZ(loc.getZ()+z);
				//plugin.logDebug("Setting 1 Current location = X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ());
				for (x = 0; x <= 2; x++)
				{
					loc.setX(loc.getX()+x);
					//plugin.logDebug("Setting 2 Current location = X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ());
					for (y = 255; y > 1; y--)
					{
						loc.setY(y);
						//plugin.logDebug("Setting 3 Current location = X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ());
						if (isSafeLocation(loc))
						{
							//plugin.logDebug("is safe");
							return loc;
						}
						//plugin.logDebug("not safe");
					}
				}
			}
		}
		else
		{
			double y;
			for (y = 255; y > 1; y--)
			{
				loc.setY(y);
				//plugin.logDebug("Current location = X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ());
				if (isSafeLocation(loc))
				{
					//plugin.logDebug("is safe");
					return loc;
				}
				//plugin.logDebug("not safe");
			}
		}

		return null;
	}

	public boolean isSafeLocation(Location location) {
		Block feet = location.getBlock();
		Block ground = feet.getRelative(BlockFace.DOWN);
		//plugin.logDebug("Feet: " + feet.getType().toString());
		//plugin.logDebug("Ground: " + ground.getType().toString());

		return (isTransparent(feet.getType()) && (ground.getType().isSolid() || ground.getType() == Material.WATER));
    }

	public boolean isTransparent(Material materialType)
	{
		switch (materialType)
		{
		case AIR:
		case GRASS:
		case OAK_SAPLING:
		case SPRUCE_SAPLING:
		case JUNGLE_SAPLING:
		case BIRCH_SAPLING:
		case ACACIA_SAPLING:
		case DARK_OAK_SAPLING:
		case DEAD_BUSH:
		case VINE:
		case LILY_PAD:
		case LILAC:
		case ROSE_BUSH:
		case TALL_GRASS:
		case PEONY:
		case OAK_SIGN:
		case SPRUCE_SIGN:
		case BIRCH_SIGN:
		case JUNGLE_SIGN:
		case ACACIA_SIGN:
		case DARK_OAK_SIGN:
		case SUNFLOWER:
		case WHITE_CARPET:
		case ORANGE_CARPET:
		case MAGENTA_CARPET:
		case LIGHT_BLUE_CARPET:
		case YELLOW_CARPET:
		case LIME_CARPET:
		case PINK_CARPET:
		case GRAY_CARPET:
		case LIGHT_GRAY_CARPET:
		case CYAN_CARPET:
		case PURPLE_CARPET:
		case BLUE_CARPET:
		case BROWN_CARPET:
		case GREEN_CARPET:
		case RED_CARPET:
		case BLACK_CARPET:
		case DANDELION:
		case POPPY:
		case BLUE_ORCHID:
		case ALLIUM:
		case AZURE_BLUET:
		case RED_TULIP:
		case ORANGE_TULIP:
		case WHITE_TULIP:
		case PINK_TULIP:
		case OXEYE_DAISY:
		case CORNFLOWER:
		case LILY_OF_THE_VALLEY:
		case BROWN_MUSHROOM:
		case RED_MUSHROOM:
		case TORCH:
		case REDSTONE_TORCH:
		case SNOW:
		case LARGE_FERN:
		case FERN:
		case BAMBOO:
		case SUGAR_CANE:
		case WHEAT:
		case TRIPWIRE:
		case PUMPKIN_STEM:
		case MELON_STEM:
		case NETHER_WART:
		case BEETROOTS:
			return true;
		default:
			return false;
		}
	}
}
