package dk.fido2603.mydog.command;

import java.util.function.Supplier;

import org.bukkit.Server;

import dk.fido2603.mydog.MyDog;
import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.service.DogService;
import dk.fido2603.mydog.service.TradeService;

/** What subcommands need from the rest of the plugin. */
public record CommandServices(MyDog plugin, Server server, DogService dogs, TradeService trades, Messages messages,
                              Supplier<Settings> settings) {
}
