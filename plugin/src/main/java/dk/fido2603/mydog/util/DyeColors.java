package dk.fido2603.mydog.util;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.DyeColor;
import org.bukkit.Material;

public final class DyeColors {
    private DyeColors() {
    }

    /** The chat color closest to a collar color. Chat has fewer colors, so some collars share one. */
    public static NamedTextColor nameColor(DyeColor dye) {
        return switch (dye) {
            case BLACK, BROWN -> NamedTextColor.BLACK;
            case BLUE -> NamedTextColor.DARK_BLUE;
            case CYAN -> NamedTextColor.BLUE;
            case GRAY -> NamedTextColor.DARK_GRAY;
            case GREEN -> NamedTextColor.DARK_GREEN;
            case LIGHT_BLUE -> NamedTextColor.AQUA;
            case LIGHT_GRAY -> NamedTextColor.GRAY;
            case LIME -> NamedTextColor.GREEN;
            case MAGENTA -> NamedTextColor.LIGHT_PURPLE;
            case ORANGE -> NamedTextColor.GOLD;
            case PINK -> NamedTextColor.RED;
            case PURPLE -> NamedTextColor.DARK_PURPLE;
            case RED -> NamedTextColor.DARK_RED;
            case YELLOW -> NamedTextColor.YELLOW;
            case WHITE -> NamedTextColor.WHITE;
        };
    }

    public static DyeColor random() {
        DyeColor[] colors = DyeColor.values();
        return colors[ThreadLocalRandom.current().nextInt(colors.length)];
    }

    /** The dye color of a dye item (RED_DYE gives RED), or {@code null} for any other item. */
    public static DyeColor ofDye(Material material) {
        String name = material.name();
        if (!name.endsWith("_DYE")) {
            return null;
        }
        try {
            return DyeColor.valueOf(name.substring(0, name.length() - "_DYE".length()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
