package dk.fido2603.mydog.listener;

import java.util.Optional;
import java.util.function.Supplier;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.config.TreatKind;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.service.TreatService;

/** Using dog treats, keeping them out of other recipes, and reminding owners of free breed/personality choices. */
public final class TreatListener implements Listener {
    /** Sent a moment after joining, so it isn't lost among other join messages. */
    private static final long REMINDER_DELAY_TICKS = 20L * 3L;

    private final Plugin plugin;
    private final DogService dogService;
    private final TreatService treats;
    private final Messages messages;
    private final Supplier<Settings> settings;

    public TreatListener(Plugin plugin, DogService dogService, TreatService treats, Messages messages,
                         Supplier<Settings> settings) {
        this.plugin = plugin;
        this.dogService = dogService;
        this.treats = treats;
        this.messages = messages;
        this.settings = settings;
    }

    /** Runs before InteractListener, so a treat is never used as a taming bone or a name tag check. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onUseTreat(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Wolf wolf)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        Optional<TreatService.Treat> found = treats.treatOf(item);
        if (found.isEmpty()) {
            return;
        }
        // Treats are bones; without this they would tame wild wolves
        event.setCancelled(true);
        TreatService.Treat treat = found.get();

        Optional<Dog> dog = dogService.dogOf(wolf);
        if (dog.isEmpty() || !DogService.canControl(dog.get(), player)) {
            messages.send(player, "treat.not-your-dog");
            return;
        }
        if (!treats.isConfigured(treat)) {
            messages.send(player, "treat.disabled");
            return;
        }
        boolean used = switch (treat) {
            case TreatService.Look look -> useLook(player, dog.get(), wolf, look);
            case TreatService.Boost boost -> useBoost(player, dog.get(), boost);
        };
        if (used && player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private boolean useLook(Player player, Dog dog, Wolf wolf, TreatService.Look look) {
        String kind = look.kind().id();
        if (look.variant().equals(look.kind().of(wolf))) {
            messages.send(player, "treat." + kind + ".same", Messages.dog(dog));
            return false;
        }
        if (!dogService.changeLook(dog, wolf, look.kind(), look.variant())) {
            messages.send(player, "treat.disabled");
            return false;
        }
        messages.send(player, "treat." + kind + ".used", Messages.dog(dog));
        return true;
    }

    private boolean useBoost(Player player, Dog dog, TreatService.Boost boost) {
        Settings.Boost config = settings.get().boosts().stats().get(boost.stat());
        var stat = Messages.component("stat", messages.component("boost." + boost.stat().id()));
        var max = Messages.text("max", TreatService.boostAmount(boost.stat(), config.max()));
        double added = dogService.addBoost(dog, config);
        if (added <= 0) {
            messages.send(player, "treat.boost.maxed", Messages.dog(dog), stat, max);
            return false;
        }
        messages.send(player, "treat.boost.used", Messages.dog(dog), stat, max,
                Messages.text("amount", TreatService.boostAmount(boost.stat(), added)),
                Messages.text("total", TreatService.boostAmount(boost.stat(), dog.getBoost(boost.stat()))));
        return true;
    }

    /** Treats can't be crafted into anything (a treat is a bone, so it would make bone meal). */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (treats.treatOf(ingredient).isPresent()) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (Dog dog : dogService.aliveDogsOf(player.getUniqueId())) {
                for (TreatKind kind : TreatKind.values()) {
                    if (dogService.hasFreeChoice(dog, kind)) {
                        messages.send(player, "command." + kind.id() + ".free-choice-reminder", Messages.dog(dog));
                    }
                }
            }
        }, REMINDER_DELAY_TICKS);
    }
}
