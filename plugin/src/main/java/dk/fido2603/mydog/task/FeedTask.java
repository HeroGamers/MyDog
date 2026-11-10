package dk.fido2603.mydog.task;

import java.util.Map;
import java.util.function.Supplier;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;

/** Hurt dogs near their owner eat wolf food from the owner's inventory (dogs.auto-feed). */
public final class FeedTask implements Runnable {
    public static final long PERIOD_TICKS = 20L * 5L;

    /**
     * Hunger points of wolf foods; a wolf heals twice that, like vanilla feeding. Wolf food not listed (e.g. from a
     * newer Minecraft) counts as 2.
     */
    private static final Map<Material, Integer> NUTRITION = Map.ofEntries(
            Map.entry(Material.ROTTEN_FLESH, 4), Map.entry(Material.BEEF, 3), Map.entry(Material.COOKED_BEEF, 8),
            Map.entry(Material.PORKCHOP, 3), Map.entry(Material.COOKED_PORKCHOP, 8), Map.entry(Material.CHICKEN, 2),
            Map.entry(Material.COOKED_CHICKEN, 6), Map.entry(Material.MUTTON, 2), Map.entry(Material.COOKED_MUTTON, 6),
            Map.entry(Material.RABBIT, 3), Map.entry(Material.COOKED_RABBIT, 5), Map.entry(Material.RABBIT_STEW, 10));

    private final Server server;
    private final DogService dogService;
    private final Messages messages;
    private final Supplier<Settings> settings;

    public FeedTask(Server server, DogService dogService, Messages messages, Supplier<Settings> settings) {
        this.server = server;
        this.dogService = dogService;
        this.messages = messages;
        this.settings = settings;
    }

    @Override
    public void run() {
        Settings.AutoFeed config = settings.get().dogs().autoFeed();
        if (!config.enabled()) {
            return;
        }
        for (Player owner : server.getOnlinePlayers()) {
            for (Dog dog : dogService.aliveDogsOf(owner.getUniqueId())) {
                dogService.wolfOf(dog).ifPresent(wolf -> {
                    if (wolf.getWorld().equals(owner.getWorld())
                            && wolf.getLocation().distanceSquared(owner.getLocation()) <= config.range() * config.range()
                            && wolf.getHealth() < dogService.maxHealth(dog) * config.below()) {
                        feed(dog, wolf, owner);
                    }
                });
            }
        }
    }

    private void feed(Dog dog, Wolf wolf, Player owner) {
        PlayerInventory inventory = owner.getInventory();
        int slot = foodSlot(inventory);
        if (slot < 0) {
            return;
        }
        ItemStack food = inventory.getItem(slot);
        Material type = food.getType();
        if (owner.getGameMode() != GameMode.CREATIVE) {
            food.setAmount(food.getAmount() - 1);
        }
        double heal = NUTRITION.getOrDefault(type, 2) * 2.0;
        wolf.setHealth(Math.min(wolf.getHealth() + heal, dogService.maxHealth(dog)));
        wolf.getWorld().playSound(wolf.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0F, 1.0F);
        messages.send(owner, "dog.auto-fed", Messages.dog(dog),
                Messages.text("food", Messages.displayName(type.getKey().toString())));
    }

    /** The slot to eat from: rotten flesh first, so the good food is kept for the owner. -1 if there is none. */
    private static int foodSlot(PlayerInventory inventory) {
        int first = -1;
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || !Tag.ITEMS_WOLF_FOOD.isTagged(item.getType())) {
                continue;
            }
            if (item.getType() == Material.ROTTEN_FLESH) {
                return i;
            }
            if (first < 0) {
                first = i;
            }
        }
        return first;
    }
}
