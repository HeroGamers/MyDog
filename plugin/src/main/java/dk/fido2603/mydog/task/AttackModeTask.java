package dk.fido2603.mydog.task;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.bukkit.Server;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.service.PluginMobs;

/**
 * Points dogs in attack mode at a target near their owner: first whatever is attacking the owner, then the closest
 * mob the attack-mode settings allow.
 */
public final class AttackModeTask implements Runnable {
    public static final long PERIOD_TICKS = 20L * 2L;
    /** Dogs further than this from their owner are left alone. */
    private static final double MAX_DOG_DISTANCE = 20.0;
    /** How far around the owner to look for targets. */
    private static final double SEARCH_RADIUS = 13.0;

    private final Server server;
    private final DogService dogService;
    private final PluginMobs pluginMobs;
    private final Supplier<Settings> settings;

    public AttackModeTask(Server server, DogService dogService, PluginMobs pluginMobs, Supplier<Settings> settings) {
        this.server = server;
        this.dogService = dogService;
        this.pluginMobs = pluginMobs;
        this.settings = settings;
    }

    @Override
    public void run() {
        Settings.AttackMode config = settings.get().attackMode();
        for (Player player : server.getOnlinePlayers()) {
            LivingEntity target = null;
            boolean searched = false;

            for (Dog dog : dogService.aliveDogsOf(player.getUniqueId())) {
                if (!dog.isAngry() || dogService.isCalm(dog.getDogId())) {
                    continue;
                }
                Wolf wolf = dogService.wolfOf(dog).orElse(null);
                if (wolf == null || wolf.isSitting() || wolf.getTarget() != null || !wolf.getWorld().equals(player.getWorld())
                        || wolf.getLocation().distanceSquared(player.getLocation()) > MAX_DOG_DISTANCE * MAX_DOG_DISTANCE) {
                    continue;
                }
                if (!searched) {
                    target = findTarget(player, config);
                    searched = true;
                }
                if (target != null) {
                    wolf.setTarget(target);
                }
            }
        }
    }

    private LivingEntity findTarget(Player owner, Settings.AttackMode config) {
        if (config.defendOwnerFirst()) {
            LivingEntity attacker = closest(owner, entity -> entity instanceof Mob mob && owner.equals(mob.getTarget())
                    && allowed(entity, config));
            if (attacker != null) {
                return attacker;
            }
        }
        return closest(owner, entity -> allowed(entity, config)
                && (config.onlyListed() ? config.targets().contains(entity.getType()) : entity instanceof Enemy));
    }

    /**
     * Never: players, other players' pets, NPCs, ignored mob types, MythicMobs (if set), and mobs the owner can't see
     * (behind walls or underground, where dogs pile up trying to reach them).
     */
    private boolean allowed(Entity entity, Settings.AttackMode config) {
        return entity instanceof LivingEntity living && !living.isDead() && !(entity instanceof Player)
                && !(entity instanceof Tameable pet && pet.isTamed())
                && !config.ignore().contains(entity.getType())
                && !pluginMobs.isNpc(entity)
                && !(config.ignoreMythicMobs() && pluginMobs.isMythicMob(entity));
    }

    private static LivingEntity closest(Player owner, Predicate<Entity> filter) {
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : owner.getNearbyEntities(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
            if (!filter.test(entity) || !owner.hasLineOfSight(entity)) {
                continue;
            }
            double distance = entity.getLocation().distanceSquared(owner.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = (LivingEntity) entity;
            }
        }
        return closest;
    }
}
