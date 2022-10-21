package dk.fido2603.mydog.utils;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;

public class ColorUtils {
    private static Random random = new Random();

    public static ChatColor getChatColorFromDyeColor(DyeColor dyeColor) {
        switch (dyeColor) {
            case BLACK:
                return ChatColor.BLACK;
            case BLUE:
                return ChatColor.DARK_BLUE;
            case BROWN:
                return ChatColor.BLACK;
            case CYAN:
                return ChatColor.BLUE;
            case GRAY:
                return ChatColor.DARK_GRAY;
            case GREEN:
                return ChatColor.DARK_GREEN;
            case LIGHT_BLUE:
                return ChatColor.AQUA;
            case LIGHT_GRAY:
                return ChatColor.GRAY;
            case LIME:
                return ChatColor.GREEN;
            case MAGENTA:
                return ChatColor.LIGHT_PURPLE;
            case ORANGE:
                return ChatColor.GOLD;
            case PINK:
                return ChatColor.RED;
            case PURPLE:
                return ChatColor.DARK_PURPLE;
            case RED:
                return ChatColor.DARK_RED;
            case YELLOW:
                return ChatColor.YELLOW;
            case WHITE:
                return ChatColor.WHITE;
            default:
                return ChatColor.WHITE;
        }
    }

    public static DyeColor randomDyeColor() {
        return DyeColor.values()[random.nextInt(DyeColor.values().length)];
    }
}