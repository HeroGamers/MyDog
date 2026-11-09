package dk.fido2603.mydog.service;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Permission checks. Operators and the console are allowed everything, even nodes that default to false. */
public final class Permissions {
    public static final String TELEPORT = "mydog.teleport";
    public static final String PET = "mydog.pet";
    public static final String PET_OTHERS = "mydog.pet.others";
    public static final String TRADE = "mydog.trade";
    public static final String LIMIT_PREFIX = "mydog.limit.";

    private Permissions() {
    }

    public static boolean has(CommandSender sender, String node) {
        return !(sender instanceof Player) || sender.isOp() || sender.hasPermission(node);
    }
}
