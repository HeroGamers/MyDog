package dk.fido2603.mydog.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.service.Permissions;

/** Damage to and from dogs: protection, lifesteal, experience for kills and low-health warnings. */
public final class CombatListener implements Listener {
    private final Server server;
    private final DogService dogService;
    private final Messages messages;
    private final Supplier<Settings> settings;
    private final Map<UUID, Long> lastLowHealthWarning = new HashMap<>();

    public CombatListener(Server server, DogService dogService, Messages messages, Supplier<Settings> settings) {
        this.server = server;
        this.dogService = dogService;
        this.messages = messages;
        this.settings = settings;
    }

    /**
     * Owners can't hurt their own dog. With protection on, other players can't either (unless they have
     * mydog.harm.others), and arrows can be turned off.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDogDamagedByEntity(EntityDamageByEntityEvent event) {
        Optional<Dog> dog = dogService.dogOf(event.getEntity());
        if (dog.isEmpty()) {
            return;
        }
        Settings.Dogs config = settings.get().dogs();
        // The player behind the damage, also when it comes from an arrow, trident, ...
        if (event.getDamageSource().getCausingEntity() instanceof Player player) {
            boolean owner = DogService.isOwnerOrCoOwner(dog.get(), player.getUniqueId());
            if (owner || (config.protectFromOtherPlayers() && !Permissions.has(player, Permissions.HARM_OTHERS))) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getDamager().getType() == EntityType.ARROW && !config.arrowDamage()) {
            event.setCancelled(true);
        }
    }

    /** Levels can deal a different amount of damage to players, so packs of strong dogs don't rule PvP. */
    @EventHandler(ignoreCancelled = true)
    public void onDogHitsPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Wolf wolf)) {
            return;
        }
        Settings.Levels levels = settings.get().levels();
        if (!levels.enabled()) {
            return;
        }
        dogService.dogOf(wolf).ifPresent(dog -> {
            Double playerDamage = levels.table().get(dog.getLevel()).playerDamage();
            if (playerDamage != null) {
                event.setDamage(playerDamage);
            }
        });
    }

    /** Invulnerable dogs only die from /kill and falling out of the world. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInvulnerableDogDamaged(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (settings.get().dogs().invulnerable() && cause != EntityDamageEvent.DamageCause.KILL
                && cause != EntityDamageEvent.DamageCause.VOID && dogService.dogOf(event.getEntity()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /** Calmed dogs don't pick new targets, whatever the reason (attack mode, defending the owner, ...). */
    @EventHandler(ignoreCancelled = true)
    public void onDogTargets(EntityTargetEvent event) {
        if (event.getTarget() != null && event.getEntity() instanceof Wolf wolf && dogService.isCalm(wolf.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** Packs of dogs easily end up in one spot, so cramming damage can be turned off. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDogCrammed(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.CRAMMING && !settings.get().dogs().crammingDamage()
                && dogService.dogOf(event.getEntity()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /** Dogs heal a share of the damage they deal. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDogDealsDamage(EntityDamageByEntityEvent event) {
        double lifesteal = settings.get().dogs().lifesteal();
        if (lifesteal <= 0.0 || event.getFinalDamage() <= 0.0 || !(event.getDamager() instanceof Wolf wolf)) {
            return;
        }
        if (dogService.dogOf(wolf).isEmpty()) {
            return;
        }
        // The wolf's real max health: its level's, plus any health treats
        AttributeInstance maxHealthAttribute = wolf.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : wolf.getHealth();
        if (wolf.getHealth() < maxHealth) {
            wolf.setHealth(Math.min(wolf.getHealth() + event.getFinalDamage() * lifesteal, maxHealth));
        }
    }

    /** Dogs get experience for what they kill. */
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getDamageSource().getCausingEntity() instanceof Wolf wolf)) {
            return;
        }
        Settings.Levels levels = settings.get().levels();
        dogService.dogOf(wolf).ifPresent(dog ->
                dogService.giveExperience(dog, levels.experienceFor(event.getEntity()), event.getEntity()));
    }

    /** Tells the owner when their dog gets low on health. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDogDamaged(EntityDamageEvent event) {
        Settings.LowHealthWarning warning = settings.get().dogs().lowHealthWarning();
        if (!warning.enabled() || !(event.getEntity() instanceof Wolf wolf)) {
            return;
        }
        Optional<Dog> found = dogService.dogOf(wolf);
        if (found.isEmpty()) {
            return;
        }
        Dog dog = found.get();

        double healthLeft = wolf.getHealth() - event.getFinalDamage();
        if (healthLeft <= 0.0 || healthLeft > dogService.maxHealth(dog) * warning.threshold()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLowHealthWarning.getOrDefault(dog.getDogId(), 0L) < warning.cooldownMillis()) {
            return;
        }
        lastLowHealthWarning.put(dog.getDogId(), now);

        Player owner = server.getPlayer(dog.getOwnerId());
        if (owner != null) {
            messages.send(owner, "dog.low-health", Messages.dog(dog), Messages.text("health", Math.round(healthLeft)));
        }
    }

    /** Forget warning cooldowns of dogs that died, so the map doesn't keep growing. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDogDeath(EntityDeathEvent event) {
        lastLowHealthWarning.remove(event.getEntity().getUniqueId());
    }
}
