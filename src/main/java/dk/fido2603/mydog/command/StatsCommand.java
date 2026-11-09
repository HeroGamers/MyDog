package dk.fido2603.mydog.command;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.meta.Damageable;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.service.DogService;

/** Shows a dog's level, experience, health, damage and where it was last seen. */
public final class StatsCommand extends SubCommand {
    private static final int BAR_SEGMENTS = 10;

    public StatsCommand(CommandServices services) {
        super(services, "stats", "mydog.stats", "<id>", true, "info");
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 1) {
            throw new UsageException();
        }
        Dog dog = ownLivingDog((Player) sender, args.getFirst());
        Optional<Wolf> wolf = services.dogs().wolfOf(dog);
        Messages messages = services.messages();
        DecimalFormat format = new DecimalFormat("#.#");

        messages.send(sender, "command.header");
        messages.send(sender, "command.stats.name", Messages.dog(dog));
        if (dog.getAppearance() != null) {
            messages.send(sender, "command.stats.breed", Messages.dog(dog));
        }

        Settings.Levels levels = services.settings().get().levels();
        if (levels.enabled()) {
            messages.send(sender, "command.stats.level", Messages.dog(dog));
            int next = levels.table().nextThreshold(dog.getExperience());
            if (next > 0) {
                messages.send(sender, "command.stats.experience", Messages.dog(dog),
                        Messages.component("bar", bar(dog.getExperience() * 100.0 / next)),
                        Messages.text("current", dog.getExperience()),
                        Messages.text("next", next));
            }
        }

        if (wolf.isPresent()) {
            double health = wolf.get().getHealth();
            double maxHealth = ListCommand.maxHealth(wolf.get());
            messages.send(sender, "command.stats.health", Messages.dog(dog),
                    Messages.component("bar", bar(health * 100.0 / maxHealth)),
                    Messages.text("current", format.format(health)),
                    Messages.text("max", format.format(maxHealth)));

            AttributeInstance damage = wolf.get().getAttribute(Attribute.ATTACK_DAMAGE);
            messages.send(sender, "command.stats.damage", Messages.dog(dog),
                    Messages.text("damage", format.format(damage != null ? damage.getValue() : 0.0)));

            DogService.armorOf(wolf.get()).ifPresent(armor -> {
                int maxDurability = armor.getItemMeta() instanceof Damageable damageable && damageable.hasMaxDamage()
                        ? damageable.getMaxDamage()
                        : armor.getType().getMaxDurability();
                int used = armor.getItemMeta() instanceof Damageable damageable ? damageable.getDamage() : 0;
                messages.send(sender, "command.stats.armor", Messages.dog(dog),
                        Messages.component("bar", bar(maxDurability > 0 ? (maxDurability - used) * 100.0 / maxDurability : 100.0)),
                        Messages.text("durability", maxDurability - used),
                        Messages.text("max", maxDurability));
            });
        }

        Dog.LastSeen lastSeen = wolf.map(w -> Dog.LastSeen.of(w.getLocation())).orElse(dog.getLastSeen());
        if (lastSeen != null) {
            messages.send(sender, "command.stats.last-seen", Messages.dog(dog),
                    Messages.text("world", lastSeen.world() != null ? lastSeen.world() : "?"),
                    Messages.text("x", format.format(lastSeen.x())),
                    Messages.text("y", format.format(lastSeen.y())),
                    Messages.text("z", format.format(lastSeen.z())));
        }
    }

    /** A bar of ten "=", the filled part in bold dark aqua. */
    private static Component bar(double percent) {
        int filled = (int) Math.clamp(Math.floor(percent / 10.0), 0, BAR_SEGMENTS);
        return Component.text()
                .append(Component.text("[", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("=".repeat(filled), NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.text("=".repeat(BAR_SEGMENTS - filled), NamedTextColor.AQUA))
                .append(Component.text("]", NamedTextColor.AQUA, TextDecoration.BOLD))
                .build();
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return args.size() == 1 ? livingDogIds(sender) : List.of();
    }
}
