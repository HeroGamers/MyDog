package dk.fido2603.mydog.listener;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.plugin.Plugin;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;

/** New dogs (taming and breeding) and dogs dying. */
public final class TamingListener implements Listener {
    /** New wolves are registered a moment later, once the server has finished setting them up. */
    private static final long REGISTER_DELAY_TICKS = 2L;

    private final Plugin plugin;
    private final DogService dogService;
    private final Messages messages;
    private final Supplier<Settings> settings;

    public TamingListener(Plugin plugin, DogService dogService, Messages messages, Supplier<Settings> settings) {
        this.plugin = plugin;
        this.dogService = dogService;
        this.messages = messages;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf) || !(event.getOwner() instanceof Player owner)) {
            return;
        }
        if (!dogService.canOwnMore(owner)) {
            messages.send(owner, "tame.limit-reached");
            event.setCancelled(true);
            return;
        }
        registerLater(wolf, owner);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Wolf puppy) || !(event.getBreeder() instanceof Player breeder)) {
            return;
        }
        Player owner = puppy.getOwner() instanceof Player puppyOwner ? puppyOwner : breeder;
        if (dogService.canOwnMore(owner)) {
            registerLater(puppy, owner);
        }
    }

    private void registerLater(Wolf wolf, Player owner) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!wolf.isValid() || !wolf.isTamed() || dogService.dogOf(wolf).isPresent()) {
                return;
            }
            Dog dog = dogService.register(wolf, owner, null, null);
            if (owner.isOnline()) {
                messages.send(owner, "tame.new-dog", Messages.dog(dog));
            }
        }, REGISTER_DELAY_TICKS);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Optional<Dog> found = dogService.dogOf(event.getEntity());
        if (found.isEmpty()) {
            return;
        }
        Dog dog = found.get();

        Player owner = plugin.getServer().getPlayer(dog.getOwnerId());
        if (owner != null) {
            Component lifetime = messages.duration(Duration.between(dog.getBirthday(), Instant.now()).abs());
            Component levelText = settings.get().levels().enabled()
                    ? messages.component("dog.died-level", Messages.dog(dog))
                    : Component.empty();
            messages.send(owner, "dog.died", Messages.dog(dog),
                    Messages.component("time", lifetime), Messages.component("level_text", levelText));
        }
        dogService.died(dog);
    }
}
