package dk.fido2603.mydog.listeners;

import dk.fido2603.mydog.DogManager.Dog;
import net.md_5.bungee.api.ChatColor;
import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.utils.TimeUtils;

import java.util.Date;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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

		EquipmentSlot hand = event.getHand();
		Player player = event.getPlayer();
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
			if (!item.getType().equals(Material.NAME_TAG) || !item.getItemMeta().hasDisplayName())
			{
				plugin.logDebug("Item is not a name-tag, returning!");
				return;
			}

			if (MyDog.getDogManager().isDog(entity.getUniqueId()))
			{
				if (!plugin.allowNametagRename)
				{
					plugin.logDebug("NametagRename is disabled, cancelling interact event!");
					event.setCancelled(true);
					return;
				}
				Dog dog = MyDog.getDogManager().getDog(entity.getUniqueId());

				if (dog == null)
				{
					plugin.logDebug("Dog is null, returning!");
					return;
				}

				dog.setDogName(item.getItemMeta().getDisplayName());
				plugin.logDebug("Set the Dog's name to: " + item.getItemMeta().getDisplayName());
				item.setAmount(0);
				event.setCancelled(true);
			}
			return;
		}

		if (MyDog.getDogManager().isDog(entity.getUniqueId()))
		{
			Dog dog = MyDog.getDogManager().getDog(entity.getUniqueId());

			if (dog == null)
			{
				plugin.logDebug("Dog is null, returning!");
				return;
			}

			Wolf wolf = (Wolf) entity;

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

		Player owner = (Player) event.getBreeder();
		if (owner == null)
		{
			plugin.logDebug("Dog owner is null, returning!");
			return;
		}

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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChunkUnload(ChunkUnloadEvent event)
	{
		if (event.getChunk().getEntities() == null)
		{
			return;
		}

		Location safeLocation = null;
		Boolean isSafe = true;

		Entity[] entities = event.getChunk().getEntities();
		for (Entity e : entities)
		{
			if (isSafe && e != null && e.getType().equals(EntityType.WOLF))
			{
				Wolf dog = (Wolf) e;
				if (dog.getOwner() == null || !(dog.getOwner() instanceof Player))
				{
					plugin.logDebug("Owner is null or not instance of Player!");
					return;
				}
				Player player = (Player) dog.getOwner();
				if (MyDog.getDogManager().isDog(dog.getUniqueId()) && player != null && player.isOnline() && (!dog.isSitting() || !dog.getWorld().equals(player.getWorld())))
				{
					MyDog.getDogManager().getDog(dog.getUniqueId()).saveDogLocation();
					if (MyDog.getPermissionsManager().hasPermission(player, "mydog.teleport"))
					{
						Location loc = null;
						if (safeLocation == null)
						{
							loc = player.getLocation();
							if (!isSafeLocation(loc))
							{
								plugin.logDebug("Whoops, seems like our player isn't at a safe location, let's find a good spot for the doggo...");
								loc = searchSafeLocation(loc);
								if (loc == null)
								{
									plugin.logDebug("Did not find a safe place to teleport a wolf! Keeping wolf at unloaded chunks!");
									/*player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Hello! Looks like you just teleported away from your Dog(s)! " +
											"They can sadly not find a safe place to stay, so they are staying behind for now :( They will be waiting for you where you left them...");*/
									player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.cannotTeleportWolfString));
									isSafe = false;
									return;
								}
							}
						}
						else
						{
							loc = safeLocation;
						}

						plugin.logDebug("It's a safe location, teleporting!");
						plugin.logDebug("Teleported a dog to a player! Chunk-unload!");
						dog.teleport(loc);
						if (dog.isSitting())
						{
							dog.setSitting(false);
						}
					}
				}
			}
		}
	}

	public Location searchSafeLocation(Location loc)
	{
		double y;
		for (y = 255; y > 1; y--)
		{
			loc.setY(y);
			plugin.logDebug("Current location = X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ());
			if (isSafeLocation(loc))
			{
				plugin.logDebug("Is safe location");
				return loc;
			}
			plugin.logDebug("Not safe location");
		}
		
		return null;
	}

	public boolean isSafeLocation(Location location) {
		Block feet = location.getBlock();
		Block ground = feet.getRelative(BlockFace.DOWN);
		plugin.logDebug("Feet: " + feet.getType().toString());
		plugin.logDebug("Ground: " + ground.getType().toString());

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
