package dk.fido2603.mydog.listeners;

import dk.fido2603.mydog.DogManager.Dog;
import dk.fido2603.mydog.MyDog;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener
{
	private MyDog	plugin	= null;

	public DamageListener(MyDog p)
	{
		this.plugin = p;
	}

	@EventHandler
	public void onWolfEntityDamage(EntityDamageEvent event)
	{
		if (event.getEntity().getType() != EntityType.WOLF || !(MyDog.getDogManager().isDog(event.getEntity().getUniqueId())))
		{
			return;
		}
		
		// TODO something with a Dog's equipped armor to lower damage caused
	}

	@EventHandler
	public void onEntityDeath(EntityDamageEvent event)
	{
		if (event instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
			if (damageEvent.getDamager() instanceof Wolf && damageEvent.getEntity() instanceof LivingEntity)
			{
				if (!(MyDog.getDogManager().isDog(damageEvent.getDamager().getUniqueId())) || (damageEvent.getFinalDamage() < ((LivingEntity) damageEvent.getEntity()).getHealth()))
				{
					return;
				}

				plugin.logDebug("Dog has killed " + event.getEntityType());
				if (plugin.useLevels)
				{
					Integer gainedExp;
					switch (event.getEntityType())
					{
					case BAT:
					case OCELOT:
					case CAT:
					case PARROT:
					case COD:
					case CHICKEN:
					case FOX:
					case SILVERFISH:
					case PUFFERFISH:
					case RABBIT:
					case SALMON:
					case SHULKER:
					case SLIME:
					case SNOWMAN:
					case SPIDER:
					case ZOMBIE:
					case SKELETON:
					case SQUID:
					case TROPICAL_FISH:
					case TURTLE:
						gainedExp = 5;
						break;
					case COW:
					case MUSHROOM_COW:
					case PIG:
					case SHEEP:
					case WOLF:
					case WANDERING_TRADER:
					case VILLAGER:
					case STRAY:
						gainedExp = 8;
						break;
					case CAVE_SPIDER:
					case PANDA:
					case CREEPER:
					case DROWNED:
					case DOLPHIN:
					case WITHER_SKELETON:
					case DONKEY:
					case WITCH:
					case SKELETON_HORSE:
					case VEX:
					case VINDICATOR:
					case HORSE:
					case TRADER_LLAMA:
					case HUSK:
					case LLAMA:
					case MULE:
					case ZOMBIE_HORSE:
					case ZOMBIE_VILLAGER:
					case PIG_ZOMBIE:
					case POLAR_BEAR:
						gainedExp = 20;
						break;
					case ENDERMITE:
					case GUARDIAN:
					case MAGMA_CUBE:
					case PHANTOM:
					case PILLAGER:
					case ENDERMAN:
						gainedExp = 40;
						break;
					case BLAZE:
					case EVOKER:
					case GHAST:
					case ILLUSIONER:
						gainedExp = 60;
						break;
					case IRON_GOLEM:
					case PLAYER:
					case RAVAGER:
						gainedExp = 70;
						break;
					case ELDER_GUARDIAN:
					case GIANT:
						gainedExp = 90;
						break;
					case WITHER:
					case ENDER_DRAGON:
						gainedExp = 200;
						break;
					default:
						gainedExp = 0;
						break;
					}

					// Give the Dog the experience
					Dog dog = MyDog.getDogManager().getDog(damageEvent.getDamager().getUniqueId());
					plugin.logDebug("Giving " + dog.getDogName() + " " + gainedExp + " experience!");
					dog.giveExperience(gainedExp);
				}
			}
		}
	}
}
