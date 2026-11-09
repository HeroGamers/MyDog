package dk.fido2603.mydog.listener;

import java.util.Optional;
import java.util.function.Supplier;

import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.util.DyeColors;

/** Right-clicking a dog: petting, dyeing the collar, feeding and renaming with a name tag. */
public final class InteractListener implements Listener {
    /** Vanilla heals wolves up to 20 health by itself; MyDog only takes over above that. */
    private static final double VANILLA_MAX_HEALTH = 20.0;

    private final DogService dogService;
    private final Messages messages;
    private final Supplier<Settings> settings;

    public InteractListener(DogService dogService, Messages messages, Supplier<Settings> settings) {
        this.dogService = dogService;
        this.messages = messages;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Wolf wolf)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());

        Optional<Dog> found = dogService.dogOf(wolf);
        if (found.isEmpty()) {
            importTamedWolf(wolf, player);
            return;
        }
        Dog dog = found.get();
        dogService.updateLastSeen(dog, wolf);

        // Sneak-clicking pets the dog, unless the player is holding food. Only the main hand counts, so the
        // off-hand event that follows doesn't pet the dog twice.
        if (player.isSneaking() && event.getHand() == EquipmentSlot.HAND && healthFrom(item) == 0.0) {
            dogService.pet(dog, player);
            event.setCancelled(true);
            return;
        }

        DyeColor dye = DyeColors.ofDye(item.getType());
        if (dye != null && dog.getOwnerId().equals(player.getUniqueId()) && wolf.getCollarColor() != dye) {
            // Vanilla dyes the collar; MyDog only follows with the name color
            dogService.setCollarColor(dog, dye);
            return;
        }

        if (feed(dog, wolf, item, player) || renameWithNameTag(dog, item, player)) {
            event.setCancelled(true);
        }
    }

    /** Wolves tamed before MyDog was installed become dogs when their owner right-clicks them. */
    @SuppressWarnings("deprecation") // Entity#getCustomName(): the String version is the one Spigot has
    private void importTamedWolf(Wolf wolf, Player player) {
        if (!wolf.isTamed() || !(wolf.getOwner() instanceof Player owner) || !owner.getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        if (!dogService.canOwnMore(player)) {
            messages.send(player, "tame.limit-reached");
            return;
        }
        // A red collar is the default, so treat it as "no collar color chosen"
        DyeColor collar = wolf.getCollarColor() == DyeColor.RED ? null : wolf.getCollarColor();
        Dog dog = dogService.register(wolf, player, wolf.getCustomName(), collar);
        messages.send(player, "tame.new-dog", Messages.dog(dog));
    }

    /** Feeding a dog above 20 health heals it up to its level's max health. */
    private boolean feed(Dog dog, Wolf wolf, ItemStack item, Player player) {
        double healthPoints = healthFrom(item);
        if (healthPoints == 0.0 || !settings.get().levels().enabled()) {
            return false;
        }
        double maxHealth = dogService.maxHealth(dog);
        AttributeInstance maxHealthAttribute = wolf.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null && maxHealthAttribute.getBaseValue() != maxHealth) {
            maxHealthAttribute.setBaseValue(maxHealth);
        }
        if (wolf.getHealth() < VANILLA_MAX_HEALTH || wolf.getHealth() >= maxHealth) {
            return false;
        }
        wolf.setHealth(Math.min(wolf.getHealth() + healthPoints, maxHealth));
        consumeOne(item, player);
        return true;
    }

    @SuppressWarnings("deprecation") // ItemMeta#getDisplayName(): the String version is the one Spigot has
    private boolean renameWithNameTag(Dog dog, ItemStack item, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (item.getType() != org.bukkit.Material.NAME_TAG || meta == null || !meta.hasDisplayName()) {
            return false;
        }
        // Without this the name tag would rename the wolf the vanilla way, which MyDog's nametag then overwrites
        if (!settings.get().dogs().allowNametagRename() || !dog.getOwnerId().equals(player.getUniqueId())) {
            return true;
        }
        dogService.rename(dog, meta.getDisplayName());
        consumeOne(item, player);
        return true;
    }

    private static void consumeOne(ItemStack item, Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    /** How much a food item heals a dog, or 0 for anything that isn't dog food. */
    private static double healthFrom(ItemStack item) {
        return switch (item.getType()) {
            case CHICKEN, COOKED_CHICKEN -> 1.0;
            case PORKCHOP, COOKED_PORKCHOP, BEEF, COOKED_BEEF, MUTTON, COOKED_MUTTON, RABBIT, COOKED_RABBIT,
                 ROTTEN_FLESH -> 2.0;
            default -> 0.0;
        };
    }
}
