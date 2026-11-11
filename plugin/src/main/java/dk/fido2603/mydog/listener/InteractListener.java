package dk.fido2603.mydog.listener;

import java.util.Optional;
import java.util.function.Supplier;

import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dk.fido2603.mydog.api.event.DogTameEvent;
import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.util.DyeColors;

/**
 * Right-clicking a dog: petting, dyeing the collar and renaming with a name tag. Feeding is left to vanilla, which
 * heals tamed wolves up to their max health (set from the dog's level).
 */
public final class InteractListener implements Listener {
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
        dogService.syncOwner(dog, wolf);
        dogService.updateLastSeen(dog, wolf);

        // Sneak-clicking pets the dog, unless the player is holding wolf food. Only the main hand counts, so the
        // off-hand event that follows doesn't pet the dog twice.
        if (player.isSneaking() && event.getHand() == EquipmentSlot.HAND && !Tag.ITEMS_WOLF_FOOD.isTagged(item.getType())) {
            dogService.pet(dog, player);
            event.setCancelled(true);
            return;
        }

        if (event.getHand() == EquipmentSlot.HAND && !DogService.isOwnerOrCoOwner(dog, player.getUniqueId())) {
            messages.send(player, "dog.owner-info", Messages.dog(dog));
        }

        DyeColor dye = DyeColors.ofDye(item.getType());
        if (dye != null && DogService.canControl(dog, player) && wolf.getCollarColor() != dye) {
            // Vanilla dyes the collar; MyDog only follows with the name color
            dogService.setCollarColor(dog, dye);
            return;
        }

        if (renameWithNameTag(dog, item, player)) {
            event.setCancelled(true);
        }
    }

    /** Wolves tamed before MyDog was installed become dogs when their owner right-clicks them. */
    @SuppressWarnings("deprecation") // Entity#getCustomName(): the String version is the one Spigot has
    private void importTamedWolf(Wolf wolf, Player player) {
        if (!wolf.isTamed() || !(wolf.getOwner() instanceof Player owner) || !owner.getUniqueId().equals(player.getUniqueId())
                || !dogService.canBecomeDog(wolf)) {
            return;
        }
        if (!dogService.canOwnMore(player)) {
            messages.send(player, "tame.limit-reached");
            return;
        }
        // A red collar is the default, so treat it as "no collar color chosen"
        DyeColor collar = wolf.getCollarColor() == DyeColor.RED ? null : wolf.getCollarColor();
        dogService.register(wolf, player, wolf.getCustomName(), collar, DogTameEvent.Cause.EXISTING_WOLF)
                .ifPresent(dog -> messages.send(player, "tame.new-dog", Messages.dog(dog)));
    }

    @SuppressWarnings("deprecation") // ItemMeta#getDisplayName(): the String version is the one Spigot has
    private boolean renameWithNameTag(Dog dog, ItemStack item, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (item.getType() != Material.NAME_TAG || meta == null || !meta.hasDisplayName()) {
            return false;
        }
        // Without this the name tag would rename the wolf the vanilla way, which MyDog's nametag then overwrites
        if (!settings.get().dogs().allowNametagRename() || !DogService.canControl(dog, player)) {
            return true;
        }
        if (dogService.rename(dog, meta.getDisplayName(), player)) {
            consumeOne(item, player);
        }
        return true;
    }

    private static void consumeOne(ItemStack item, Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

}
