package dk.fido2603.mydog.message;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Every message key the code uses must exist in the bundled messages.yml, or players see "missing message". */
class MessageKeysTest {
    private static final Pattern KEY = Pattern.compile("\"((?:command|dog|tame|teleport|nametag|time|treat)\\.[a-z0-9.-]+)\"");

    /** Keys built at runtime, which the scan can't see. */
    private static final List<String> DYNAMIC_KEYS = List.of(
            "command.sit.all", "command.sit.one", "command.stand.all", "command.stand.one",
            "command.attack.all", "command.attack.one", "command.defend.all", "command.defend.one",
            "command.help.calm",
            "command.help.help", "command.help.reload", "command.help.save", "command.help.list",
            "command.help.putdown", "command.help.release", "command.help.comehere", "command.help.sit",
            "command.help.stand", "command.help.attack", "command.help.defend", "command.help.trade",
            "command.help.tradeaccept", "command.help.tradedecline", "command.help.stats", "command.help.rename",
            "command.help.setid", "command.help.setlevel", "command.help.dead", "command.help.revive",
            "time.minute", "time.minutes", "time.hour", "time.hours", "time.day", "time.days",
            "command.help.breed", "command.help.personality", "command.help.givetreat",
            "command.breed.info", "command.breed.free-choice", "command.breed.no-free-choice", "command.breed.unknown",
            "command.breed.done", "command.breed.free-choice-reminder",
            "command.personality.info", "command.personality.free-choice", "command.personality.no-free-choice",
            "command.personality.unknown", "command.personality.done", "command.personality.free-choice-reminder",
            "treat.breed.name", "treat.breed.lore", "treat.breed.same", "treat.breed.used",
            "treat.personality.name", "treat.personality.lore", "treat.personality.same", "treat.personality.used");

    @Test
    void everyUsedKeyExists() throws IOException {
        YamlConfiguration messages = bundled("messages.yml");
        // Config keys look the same (teleport.enabled), so they are told apart by being in config.yml
        YamlConfiguration config = bundled("config.yml");

        TreeSet<String> used = new TreeSet<>(DYNAMIC_KEYS);
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher matcher = KEY.matcher(Files.readString(file));
                while (matcher.find()) {
                    used.add(matcher.group(1));
                }
            }
        }

        List<String> missing = new ArrayList<>();
        for (String key : used) {
            // "time.separator"-style prefixes passed to helpers end with a dot; those aren't keys
            if (!key.endsWith(".") && !messages.contains(key) && !config.contains(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "Missing from messages.yml: " + missing);
    }

    private YamlConfiguration bundled(String name) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(name)), StandardCharsets.UTF_8));
    }
}
