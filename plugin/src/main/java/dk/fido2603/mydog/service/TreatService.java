package dk.fido2603.mydog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.config.TreatKind;
import dk.fido2603.mydog.message.Messages;

/**
 * Dog treats: items that change the breed or personality of the dog they are used on. A treat is recognized by a tag
 * on the item ({@code mydog:treat}), never by its name, so renaming one doesn't break it and lookalikes don't work.
 */
public final class TreatService {
    public record Treat(TreatKind kind, NamespacedKey variant) {}

    private final Plugin plugin;
    private final Messages messages;
    private final Supplier<Settings> settings;
    private final NamespacedKey treatTag;
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    public TreatService(Plugin plugin, Messages messages, Supplier<Settings> settings) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
        this.treatTag = new NamespacedKey(plugin, "treat");
    }

    // ItemMeta#setItemName(String) and #setLore(List<String>) are deprecated on Paper, but they are what Spigot has
    @SuppressWarnings("deprecation")
    public ItemStack create(Treat treat, int amount) {
        ItemStack item = new ItemStack(Material.BONE, amount);
        ItemMeta meta = item.getItemMeta();
        String kind = treat.kind().id();
        var name = Messages.text(kind, Messages.displayName(treat.variant().toString()));
        meta.setItemName(Messages.legacy(messages.component("treat." + kind + ".name", name)));
        meta.setLore(messages.lines("treat." + kind + ".lore", name).stream().map(Messages::legacy).toList());
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(treatTag, PersistentDataType.STRING, kind + "/" + treat.variant());
        item.setItemMeta(meta);
        return item;
    }

    /** The treat an item is, if it is one. */
    public Optional<Treat> treatOf(ItemStack item) {
        if (item == null || item.getType() != Material.BONE || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String value = item.getItemMeta().getPersistentDataContainer().get(treatTag, PersistentDataType.STRING);
        if (value == null || !value.contains("/")) {
            return Optional.empty();
        }
        TreatKind kind = TreatKind.byId(value.substring(0, value.indexOf('/')));
        NamespacedKey variant = NamespacedKey.fromString(value.substring(value.indexOf('/') + 1));
        return kind == null || variant == null ? Optional.empty() : Optional.of(new Treat(kind, variant));
    }

    /** Whether this treat is set up in config.yml (and treats are on), crafted or not. */
    public boolean isConfigured(Treat treat) {
        Settings.Breeds breeds = settings.get().breeds();
        return breeds.treats() && breeds.recipe(treat.kind(), treat.variant()).isPresent();
    }

    /** (Re)registers the crafting recipes from config.yml. Called on enable and on every reload. */
    public void registerRecipes() {
        for (NamespacedKey key : registeredRecipes) {
            plugin.getServer().removeRecipe(key);
        }
        registeredRecipes.clear();

        Settings.Breeds breeds = settings.get().breeds();
        if (!breeds.treats()) {
            return;
        }
        for (Settings.TreatRecipe recipe : breeds.recipes()) {
            if (!recipe.craftable()) {
                continue;
            }
            NamespacedKey key = new NamespacedKey(plugin,
                    "treat_" + recipe.kind().id() + "_" + recipe.variant().getNamespace() + "_" + recipe.variant().getKey());
            ShapedRecipe shaped = new ShapedRecipe(key, create(new Treat(recipe.kind(), recipe.variant()), 1));
            shaped.shape(recipe.shape().toArray(String[]::new));
            for (Map.Entry<Character, Material> ingredient : recipe.ingredients().entrySet()) {
                if (recipe.shape().stream().anyMatch(row -> row.indexOf(ingredient.getKey()) >= 0)) {
                    shaped.setIngredient(ingredient.getKey(), ingredient.getValue());
                }
            }
            if (plugin.getServer().addRecipe(shaped)) {
                registeredRecipes.add(key);
            }
        }
    }
}
