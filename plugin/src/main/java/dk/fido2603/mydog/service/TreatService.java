package dk.fido2603.mydog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.config.StatBoost;
import dk.fido2603.mydog.config.TreatKind;
import dk.fido2603.mydog.message.Messages;

/**
 * Dog treats: items used on a dog to change its breed or personality ({@link Look}), or to permanently raise one of
 * its stats ({@link Boost}). A treat is recognized by a tag on the item ({@code mydog:treat}), never by its name, so
 * renaming one doesn't break it and lookalikes don't work.
 */
public final class TreatService {
    public sealed interface Treat permits Look, Boost {}

    /** Changes the dog's breed or personality to {@code variant}. */
    public record Look(TreatKind kind, NamespacedKey variant) implements Treat {}

    /** Raises one of the dog's stats. */
    public record Boost(StatBoost stat) implements Treat {}

    private static final String BOOST = "boost";

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
        String key;
        TagResolver[] tags;
        String tag;
        switch (treat) {
            case Look look -> {
                key = "treat." + look.kind().id();
                tags = new TagResolver[]{Messages.text(look.kind().id(), Messages.displayName(look.variant().toString()))};
                tag = look.kind().id() + "/" + look.variant();
            }
            case Boost boost -> {
                Settings.Boost config = settings.get().boosts().stats().get(boost.stat());
                key = "treat.boost";
                tags = new TagResolver[]{
                        Messages.component("stat", messages.component("boost." + boost.stat().id())),
                        Messages.text("amount", config != null ? boostAmount(boost.stat(), config.perTreat()) : "?"),
                        Messages.text("max", config != null ? boostAmount(boost.stat(), config.max()) : "?")};
                tag = BOOST + "/" + boost.stat().id();
            }
        }
        meta.setItemName(Messages.legacy(messages.component(key + ".name", tags)));
        meta.setLore(messages.lines(key + ".lore", tags).stream().map(Messages::legacy).toList());
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(treatTag, PersistentDataType.STRING, tag);
        item.setItemMeta(meta);
        return item;
    }

    /** A boost as shown to players: "+2" for health and damage, "+2%" for speed. */
    public static String boostAmount(StatBoost stat, double amount) {
        String number = amount == Math.rint(amount) ? Long.toString((long) amount) : Double.toString(amount);
        return "+" + number + (stat == StatBoost.SPEED ? "%" : "");
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
        String kindId = value.substring(0, value.indexOf('/'));
        String rest = value.substring(value.indexOf('/') + 1);
        if (kindId.equals(BOOST)) {
            StatBoost stat = StatBoost.byId(rest);
            return stat == null ? Optional.empty() : Optional.of(new Boost(stat));
        }
        TreatKind kind = TreatKind.byId(kindId);
        NamespacedKey variant = NamespacedKey.fromString(rest);
        return kind == null || variant == null ? Optional.empty() : Optional.of(new Look(kind, variant));
    }

    /** Whether this treat is set up in config.yml (and turned on), crafted or not. */
    public boolean isConfigured(Treat treat) {
        Settings current = settings.get();
        return switch (treat) {
            case Look look -> current.breeds().treats() && current.breeds().recipe(look.kind(), look.variant()).isPresent();
            case Boost boost -> current.boosts().get(boost.stat()).isPresent();
        };
    }

    /** (Re)registers the crafting recipes from config.yml. Called on enable and on every reload. */
    public void registerRecipes() {
        for (NamespacedKey key : registeredRecipes) {
            plugin.getServer().removeRecipe(key);
        }
        registeredRecipes.clear();

        Settings current = settings.get();
        if (current.breeds().treats()) {
            for (Settings.TreatRecipe recipe : current.breeds().recipes()) {
                if (recipe.craftable()) {
                    register("treat_" + recipe.kind().id() + "_" + recipe.variant().getNamespace() + "_" + recipe.variant().getKey(),
                            new Look(recipe.kind(), recipe.variant()), recipe.shape(), recipe.ingredients());
                }
            }
        }
        if (current.boosts().enabled()) {
            for (Settings.Boost boost : current.boosts().stats().values()) {
                if (boost.craftable()) {
                    register("treat_boost_" + boost.stat().id(), new Boost(boost.stat()), boost.shape(), boost.ingredients());
                }
            }
        }
    }

    private void register(String id, Treat treat, List<String> shape, Map<Character, Material> ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        ShapedRecipe shaped = new ShapedRecipe(key, create(treat, 1));
        shaped.shape(shape.toArray(String[]::new));
        for (Map.Entry<Character, Material> ingredient : ingredients.entrySet()) {
            if (shape.stream().anyMatch(row -> row.indexOf(ingredient.getKey()) >= 0)) {
                shaped.setIngredient(ingredient.getKey(), ingredient.getValue());
            }
        }
        if (plugin.getServer().addRecipe(shaped)) {
            registeredRecipes.add(key);
        }
    }
}
