package dk.fido2603.mydog.listener;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.tag.DamageTypeTags;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;

/**
 * Wolf armor on dogs: optionally unbreakable, and the owner is warned when it is nearly worn out or breaks. Bukkit has
 * no event for wolf armor losing durability, so the armor is compared right before and a tick after a dog is hurt.
 */
public final class ArmorListener implements Listener {
    private final Plugin plugin;
    private final DogService dogService;
    private final Messages messages;
    private final Supplier<Settings> settings;

    public ArmorListener(Plugin plugin, DogService dogService, Messages messages, Supplier<Settings> settings) {
        this.plugin = plugin;
        this.dogService = dogService;
        this.messages = messages;
        this.settings = settings;
    }

    /**
     * Armor proficiency: wolf armor takes the whole hit (rounded up) as wear instead of the dog, so a dog at a level
     * with armor proficiency gets a smaller hit. It is rounded at random, so small hits are spared on average too.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmoredDogHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf) || DogService.armorOf(wolf).isEmpty()
                || DamageTypeTags.BYPASSES_WOLF_ARMOR.isTagged(event.getDamageSource().getDamageType())) {
            return;
        }
        Settings current = settings.get();
        Optional<Dog> dog = current.levels().enabled() && !current.dogs().armor().unbreakable()
                ? dogService.dogOf(wolf)
                : Optional.empty();
        if (dog.isEmpty()) {
            return;
        }
        double proficiency = current.levels().table().get(dog.get().getLevel()).armorProficiency();
        if (proficiency <= 0.0) {
            return;
        }
        int wear = (int) Math.ceil(event.getDamage());
        double kept = wear * (1.0 - proficiency / 100.0);
        int reduced = (int) Math.floor(kept) + (ThreadLocalRandom.current().nextDouble() < kept - Math.floor(kept) ? 1 : 0);
        if (reduced < wear) {
            event.setDamage(reduced);
        }
    }

    /** Runs before the damage is dealt, so an unbreakable flag is in place before the armor would lose durability. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDogHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) {
            return;
        }
        Optional<ItemStack> armor = DogService.armorOf(wolf);
        Optional<Dog> dog = armor.isPresent() ? dogService.dogOf(wolf) : Optional.empty();
        if (dog.isEmpty()) {
            return;
        }
        Settings.Armor config = settings.get().dogs().armor();
        if (config.unbreakable()) {
            makeUnbreakable(wolf, armor.get());
            return;
        }

        int before = durabilityLeft(armor.get());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!wolf.isValid()) {
                return;
            }
            Player owner = plugin.getServer().getPlayer(dog.get().getOwnerId());
            if (owner == null) {
                return;
            }
            Optional<ItemStack> after = DogService.armorOf(wolf);
            if (after.isEmpty()) {
                messages.send(owner, "dog.armor-broke", Messages.dog(dog.get()));
            } else {
                int left = durabilityLeft(after.get());
                if (before > config.warnBelow() && left <= config.warnBelow()) {
                    messages.send(owner, "dog.armor-worn", Messages.dog(dog.get()), Messages.text("durability", left),
                            Messages.text("max", after.get().getType().getMaxDurability()));
                }
            }
        });
    }

    /** Marks the armor unbreakable. It stays that way when it comes off; wolf armor has no other use. */
    private static void makeUnbreakable(Wolf wolf, ItemStack armor) {
        ItemMeta meta = armor.getItemMeta();
        if (meta == null || meta.isUnbreakable()) {
            return;
        }
        meta.setUnbreakable(true);
        armor.setItemMeta(meta);
        wolf.getEquipment().setItem(EquipmentSlot.BODY, armor);
    }

    private static int durabilityLeft(ItemStack armor) {
        int max = armor.getType().getMaxDurability();
        return armor.getItemMeta() instanceof Damageable damageable ? max - damageable.getDamage() : max;
    }
}
