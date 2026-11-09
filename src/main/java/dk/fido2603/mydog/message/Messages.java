package dk.fido2603.mydog.message;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import dk.fido2603.mydog.migration.VersionedYaml;
import dk.fido2603.mydog.objects.Dog;

/**
 * The texts in messages.yml. They are MiniMessage, rendered to legacy (§) text before sending, since Spigot has no
 * Adventure support of its own. Keys missing from the server's file fall back to the bundled messages.yml.
 */
public final class Messages {
    public static final int MESSAGES_VERSION = 1;

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    /** §x§r§r§g§g§b§b hex colors are understood by both Spigot and Paper. */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final Plugin plugin;
    private final String pluginName;
    private YamlConfiguration messages = new YamlConfiguration();
    private TagResolver globals = TagResolver.empty();

    public Messages(Plugin plugin, String pluginName) {
        this.plugin = plugin;
        this.pluginName = pluginName;
    }

    public void load() throws VersionedYaml.LoadException {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration loaded = new VersionedYaml("messages-version", MESSAGES_VERSION, 1, List.of()).load(file, plugin.getLogger());

        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read the bundled messages.yml: " + e.getMessage());
        }

        messages = loaded;
        globals = TagResolver.resolver(
                Placeholder.parsed("prefix", messages.getString("prefix", "MyDog")),
                Placeholder.unparsed("plugin", pluginName));
    }

    /** The lines of a message. A list gives one line per entry; an empty message gives no lines. */
    public List<Component> lines(String key, TagResolver... tags) {
        List<String> raw;
        if (messages.isList(key)) {
            raw = messages.getStringList(key);
        } else {
            String value = messages.getString(key);
            raw = value == null ? List.of("<red>[MyDog: missing message " + key + "]") : List.of(value);
        }

        TagResolver resolver = TagResolver.resolver(globals, TagResolver.resolver(tags));
        List<Component> lines = new ArrayList<>(raw.size());
        for (String line : raw) {
            if (raw.size() == 1 && line.isEmpty()) {
                return List.of();
            }
            lines.add(MINI_MESSAGE.deserialize(line, resolver));
        }
        return lines;
    }

    /** A one-line message, for use inside other messages or nametags. */
    public Component component(String key, TagResolver... tags) {
        return Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), lines(key, tags));
    }

    public void send(CommandSender to, String key, TagResolver... tags) {
        for (Component line : lines(key, tags)) {
            to.sendMessage(legacy(line));
        }
    }

    public static String legacy(Component component) {
        return LEGACY.serialize(component);
    }

    /** {@code <dog_name>}, {@code <id>}, {@code <level>} and {@code <owner>} for a dog. */
    public static TagResolver dog(Dog dog) {
        return TagResolver.resolver(
                Placeholder.component("dog_name", dogName(dog)),
                Placeholder.unparsed("id", Integer.toString(dog.getIdentifier())),
                Placeholder.unparsed("level", Integer.toString(dog.getLevel())),
                // Only looked up when a message actually uses <owner>
                TagResolver.resolver("owner", (arguments, context) -> Tag.selfClosingInserting(Component.text(ownerName(dog)))));
    }

    private static String ownerName(Dog dog) {
        String name = Bukkit.getOfflinePlayer(dog.getOwnerId()).getName();
        return name != null ? name : "?";
    }

    public static Component dogName(Dog dog) {
        return Component.text(dog.getDogName(), dog.getColor());
    }

    public static TagResolver text(String name, Object value) {
        return Placeholder.unparsed(name, String.valueOf(value));
    }

    public static TagResolver component(String name, Component value) {
        return Placeholder.component(name, value);
    }

    /** A duration like "2 days, 3 hours and 5 minutes", leaving out parts that are zero. */
    public Component duration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        List<Component> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(unit("day", days));
        }
        if (hours > 0) {
            parts.add(unit("hour", hours));
        }
        if (minutes > 0 || parts.isEmpty()) {
            parts.add(unit("minute", minutes));
        }

        Component result = parts.getFirst();
        for (int i = 1; i < parts.size(); i++) {
            String separator = i == parts.size() - 1 ? "time.last-separator" : "time.separator";
            result = result.append(component(separator)).append(parts.get(i));
        }
        return result;
    }

    private Component unit(String unit, long amount) {
        return component(amount == 1 ? "time." + unit : "time." + unit + "s", text("amount", amount));
    }
}
