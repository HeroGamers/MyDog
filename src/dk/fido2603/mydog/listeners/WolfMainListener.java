package dk.fido2603.mydog.listeners;

import dk.fido2603.mydog.DogManager.Dog;
import net.md_5.bungee.api.ChatColor;
import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.utils.TimeUtils;

import java.util.Date;

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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

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

		owner.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.GOLD + "Congratulations with your new dog, "
				+ dog.getDogColor() + dog.getDogName() + ChatColor.GOLD + "!");
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

			String levelText = ".";
			if (plugin.useLevels)
			{
				levelText = ", and got to " + ChatColor.DARK_RED + "Level " + dog.getLevel() + ChatColor.RED + ".";
			}

			owner.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Your dog, " + dog.getDogColor() + dog.getDogName() + ChatColor.RED + 
					", just passed away... " + dog.getDogColor() + dog.getDogName() + ChatColor.RED + " lived for " + time + levelText);
		}
	
		MyDog.getDogManager().removeDog(event.getEntity().getUniqueId());
	}

	@EventHandler
	public void onWolfSit(PlayerInteractAtEntityEvent event)
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChunkUnload(ChunkUnloadEvent event)
	{
		if (event.getChunk().getEntities() == null)
		{
			return;
		}

		Entity[] entities = event.getChunk().getEntities();
		for (Entity e : entities)
		{
			if (e != null && e.getType().equals(EntityType.WOLF))
			{
				Wolf dog = (Wolf) e;
				if (MyDog.getDogManager().isDog(dog.getUniqueId()) && !dog.isSitting())
				{
					Player player = (Player) dog.getOwner();
					if (player != null && player.isOnline() && player.hasPermission("mydog.teleport"))
					{
						Location loc = player.getLocation();
						if (!isSafeLocation(loc))
						{
							plugin.logDebug("Whoops, seems like our player isn't at a safe location, let's find a good spot for the doggo...");
							loc = searchSafeLocation(loc);
							if (loc == null)
							{
								plugin.logDebug("Did not find a safe place to teleport a wolf! Keeping wolf at unloaded chunks!");
								player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[" + plugin.getChatPrefix() + "] " + ChatColor.RESET + ChatColor.RED + "Hello! Looks like you just teleported away from your Dog(s)! " +
										"They can sadly not find a safe place to stay, so they are staying behind for now :( They will be waiting for you where you left them...");
								return;
							}
						}
						plugin.logDebug("It's a safe location, teleporting!");
						plugin.logDebug("Teleported a dog to a player! Chunk-unload!");
						dog.teleport(loc);
					}
				}
			}
		}
	}

	public Location searchSafeLocation(Location loc)
	{
		Integer y;
		for (y = 255; y > 1; y--)
		{
			loc.setY(y);
			plugin.logDebug("Current location = X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ());
			if (isSafeLocation(loc))
			{
				return loc;
			}
		}
		Integer x;
		for (x = 1; x == 5; x++)
		{
			loc.setX(loc.getX()+x);
			for (y = 255; y > 1; y--)
			{
				loc.setY(y);
				plugin.logDebug("Current location = X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ());
				if (isSafeLocation(loc))
				{
					return loc;
				}
			}
		}
		Integer z;
		for (z = 1; z == 5; z++)
		{
			loc.setZ(loc.getZ()+z);
			for (x = 1; x == 5; x++)
			{
				loc.setX(loc.getX()+x);
				for (y = 255; y > 1; y--)
				{
					loc.setY(y);
					plugin.logDebug("Current location = X: " + loc.getX() + " Y: " + loc.getY() + " Z: " + loc.getZ());
					if (isSafeLocation(loc))
					{
						return loc;
					}
				}
			}
		}
		
		return null;
	}

	// Method stolen from the Spigot Forums @ BillyGalbreath (https://www.spigotmc.org/threads/safely-teleport-players.83205/), and modified slightly
	public boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        plugin.logDebug("Feet: " + feet.getType().toString());
        if (feet.getType() != Material.AIR && feet.getLocation().add(0, 1, 0).getBlock().getType() != Material.AIR) {
            return false; // not transparent (will suffocate)
        }
        Block head = feet.getRelative(BlockFace.UP);
        plugin.logDebug("Head: " + head.getType().toString());
        if (head.getType() != Material.AIR) {
            return false; // not transparent (will suffocate)
        }
        Block ground = feet.getRelative(BlockFace.DOWN);
        plugin.logDebug("Ground: " + ground.getType().toString());
        if (!ground.getType().isSolid()) {
            return false; // not solid
        }
        return true;
    }
}
