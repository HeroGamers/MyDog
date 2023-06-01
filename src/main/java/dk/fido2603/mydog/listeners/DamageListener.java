package dk.fido2603.mydog.listeners;

import dk.fido2603.mydog.MyDog;

import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.objects.LevelFactory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {
    private final MyDog plugin;

    public DamageListener(MyDog p) {
        this.plugin = p;
    }

    @EventHandler
    public void onWolfEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity().getType() != EntityType.WOLF || !(MyDog.getDogManager().isDog(e.getEntity().getUniqueId()))) {
            return;
        }
        EntityType type = e.getDamager().getType();
        if (type == EntityType.PLAYER) {
            Dog wolf = MyDog.getDogManager().getDog(e.getEntity().getUniqueId());
            if (wolf.getOwnerId().equals(e.getDamager().getUniqueId())) {
                e.setCancelled(true);
            }
        }
        if (type == EntityType.ARROW && !plugin.allowArrowDamage) {
            e.setCancelled(true);
        }

        // TODO something with a Dog's equipped armor to lower damage caused
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWolfEntityDamage2(EntityDamageByEntityEvent e) {
        if (e.getDamager().getType() != EntityType.WOLF || !(MyDog.getDogManager().isDog(e.getDamager().getUniqueId())) || plugin.lifesteal == 0.0D) {
            return;
        }

        if (e.getFinalDamage() > 0) {
            double healthPoints = e.getFinalDamage() * plugin.lifesteal;

            if (healthPoints > 0) {
                plugin.logDebug("Lifesteal event, dog stole " + healthPoints + " health!");

                Wolf wolf = (Wolf) e.getDamager();
                Dog dog = MyDog.getDogManager().getDog(wolf.getUniqueId());

                int dogsLevel = dog.getLevel();
                if (dogsLevel < 1) {
                    plugin.logDebug("Level was under 1, setting level to 1");
                    dogsLevel = 1;
                }

                LevelFactory.Level level = plugin.dogLevels.get(dogsLevel);
                if (level == null) {
                    plugin.logDebug("Level object is null, returning!");
                    return;
                }

                double health = level.health;
                if (health < 10.0) {
                    health = 10.0;
                }

                AttributeInstance wolfMaxHealth = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);

                if (wolfMaxHealth != null && wolfMaxHealth.getValue() != health) {
                    wolfMaxHealth.setBaseValue(health);
                }

                if (wolf.getHealth() < health) {
                    // If health would overflow, just set full health
                    wolf.setHealth(Math.min(wolf.getHealth() + healthPoints, health));
                    plugin.logDebug("Gave the dog, " + dog.getDogName() + ", " + healthPoints + " in health.");
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            if (damageEvent.getDamager() instanceof Wolf && damageEvent.getEntity() instanceof LivingEntity) {
                if (!(MyDog.getDogManager().isDog(damageEvent.getDamager().getUniqueId())) || (damageEvent.getFinalDamage() < ((LivingEntity) damageEvent.getEntity()).getHealth())) {
                    return;
                }

                plugin.logDebug("Dog has killed " + event.getEntityType() + " with a final blow dealing " + event.getFinalDamage() + " HP!");
                if (plugin.useLevels) {
                    int gainedExp;
                    switch (event.getEntityType()) {
                        case BAT:
                        case AXOLOTL:
                        case BEE:
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
                        case GLOW_SQUID:
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
                        case STRIDER:
                        case DONKEY:
                        case GOAT:
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
                        case PIGLIN:
                        case ZOMBIFIED_PIGLIN:
                        case POLAR_BEAR:
                            gainedExp = 20;
                            break;
                        case ENDERMITE:
                        case GUARDIAN:
                        case MAGMA_CUBE:
                        case PIGLIN_BRUTE:
                        case PHANTOM:
                        case PILLAGER:
                        case ENDERMAN:
                            gainedExp = 40;
                            break;
                        case BLAZE:
                        case EVOKER:
                        case GHAST:
                        case HOGLIN:
                        case ILLUSIONER:
                            gainedExp = 60;
                            break;
                        case IRON_GOLEM:
                        case RAVAGER:
                            gainedExp = 70;
                            break;
                        case PLAYER:
                            if (plugin.allowPlayerKillExp) {
                                gainedExp = 70;
                            } else {
                                gainedExp = 0;
                            }
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
