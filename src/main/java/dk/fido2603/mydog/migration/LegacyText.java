package dk.fido2603.mydog.migration;

import java.util.Locale;
import java.util.Map;

/**
 * Converts the old {@code &}-code messages with {@code {placeholders}} to MiniMessage.
 */
final class LegacyText {
    private LegacyText() {
    }

    private static final Map<Character, String> COLORS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"));

    private static final Map<Character, String> DECORATIONS = Map.of(
            'k', "obfuscated", 'l', "bold", 'm', "strikethrough", 'n', "underlined", 'o', "italic");

    /** Old placeholder to new tag. {dogNameColor} is dropped because the new <dog_name> already carries the color. */
    private static final Map<String, String> PLACEHOLDERS = Map.of(
            "{chatPrefix}", "<prefix>",
            "{dogNameColor}", "",
            "{dogName}", "<dog_name>",
            "{level}", "<level>",
            "{time}", "<time>",
            "{deadDogLevelString}", "<level_text>",
            "{health}", "<health>");

    static String toMiniMessage(String legacy) {
        // Text that looks like a tag must stay text
        String text = legacy.replace("\\", "\\\\").replace("<", "\\<");
        for (Map.Entry<String, String> placeholder : PLACEHOLDERS.entrySet()) {
            text = text.replace(placeholder.getKey(), placeholder.getValue());
        }

        StringBuilder out = new StringBuilder();
        // In legacy text a color code clears bold, italic etc. MiniMessage tags nest instead, so a color that follows a
        // decoration needs a <reset> first to look the same.
        boolean decorated = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c != '&' && c != '§') || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }

            // &#rrggbb hex colors
            if (text.charAt(i + 1) == '#' && i + 8 <= text.length() && isHex(text.substring(i + 2, i + 8))) {
                if (decorated) {
                    out.append("<reset>");
                    decorated = false;
                }
                out.append("<#").append(text, i + 2, i + 8).append('>');
                i += 7;
                continue;
            }

            char code = Character.toLowerCase(text.charAt(i + 1));
            if (COLORS.containsKey(code)) {
                if (decorated) {
                    out.append("<reset>");
                    decorated = false;
                }
                out.append('<').append(COLORS.get(code)).append('>');
            } else if (DECORATIONS.containsKey(code)) {
                out.append('<').append(DECORATIONS.get(code)).append('>');
                decorated = true;
            } else if (code == 'r') {
                out.append("<reset>");
                decorated = false;
            } else {
                out.append(c);
                continue;
            }
            i++;
        }
        return out.toString();
    }

    private static boolean isHex(String s) {
        return s.toLowerCase(Locale.ROOT).chars().allMatch(ch -> Character.digit(ch, 16) >= 0);
    }
}
