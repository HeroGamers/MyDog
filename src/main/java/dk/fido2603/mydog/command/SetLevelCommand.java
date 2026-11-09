package dk.fido2603.mydog.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dk.fido2603.mydog.config.LevelTable;
import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;

/** Sets the level of one of the player's own dogs. Its experience is left as it is. */
public final class SetLevelCommand extends SubCommand {
    public SetLevelCommand(CommandServices services) {
        super(services, "setlevel", "mydog.editlevel", "<id> <level>", true, "editlevel");
    }

    @Override
    public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException {
        if (args.size() != 2) {
            throw new UsageException();
        }
        Settings.Levels levels = services.settings().get().levels();
        if (!levels.enabled()) {
            throw new CommandException("command.setlevel.levels-disabled");
        }
        Dog dog = livingDog((Player) sender, args.getFirst(), Access.OWNER);
        LevelTable table = levels.table();
        int level;
        try {
            level = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            level = 0;
        }
        if (level < 1 || level > table.maxLevel()) {
            throw new CommandException("command.setlevel.out-of-range", Messages.text("max", table.maxLevel()));
        }
        services.dogs().setLevel(dog, level);
        services.messages().send(sender, "command.setlevel.done", Messages.dog(dog));
    }

    @Override
    public List<String> complete(CommandSender sender, List<String> args) {
        return args.size() == 1 ? livingDogIds(sender, false) : List.of();
    }
}
