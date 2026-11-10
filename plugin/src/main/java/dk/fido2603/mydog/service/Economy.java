package dk.fido2603.mydog.service;

import java.util.Optional;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Money, through Vault. Vault is optional, so its classes are only touched after checking that it is installed, and
 * the economy is looked up on every use because economy plugins often register after MyDog has started.
 */
public interface Economy {
    boolean has(OfflinePlayer player, double amount);

    boolean withdraw(OfflinePlayer player, double amount);

    void deposit(OfflinePlayer player, double amount);

    /** The economy, if Vault and an economy plugin are installed. */
    static Optional<Economy> find(Server server) {
        if (server.getPluginManager().getPlugin("Vault") == null) {
            return Optional.empty();
        }
        return VaultEconomy.find(server);
    }

    final class VaultEconomy implements Economy {
        private final net.milkbowl.vault.economy.Economy vault;

        private VaultEconomy(net.milkbowl.vault.economy.Economy vault) {
            this.vault = vault;
        }

        static Optional<Economy> find(Server server) {
            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> provider =
                    server.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            return provider == null ? Optional.empty() : Optional.of(new VaultEconomy(provider.getProvider()));
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return vault.has(player, amount);
        }

        @Override
        public boolean withdraw(OfflinePlayer player, double amount) {
            return vault.withdrawPlayer(player, amount).transactionSuccess();
        }

        @Override
        public void deposit(OfflinePlayer player, double amount) {
            vault.depositPlayer(player, amount);
        }
    }
}
