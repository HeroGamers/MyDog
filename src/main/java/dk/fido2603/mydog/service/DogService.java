package dk.fido2603.mydog.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;

import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import dk.fido2603.mydog.config.DefaultDogNames;
import dk.fido2603.mydog.config.LevelTable;
import dk.fido2603.mydog.config.Settings;
import dk.fido2603.mydog.config.TreatKind;
import dk.fido2603.mydog.message.Messages;
import dk.fido2603.mydog.objects.Dog;
import dk.fido2603.mydog.storage.DogRepository;
import dk.fido2603.mydog.util.DebugLog;
import dk.fido2603.mydog.util.DyeColors;

/**
 * Everything that changes a dog. Each method keeps the saved data and the wolf entity (nametag, health, damage) in step.
 */
public final class DogService {
    /** Removes leftovers of an old MyDog nametag from a name: the level ("[3]") and the mode ("[⚔]"). */
    private static final Pattern NAMETAG_EXTRAS = Pattern.compile("\\[\\d+]|\\[[⚔⛨]]");
    private static final Pattern LEGACY_CODES = Pattern.compile("(?i)[§&]([0-9A-FK-ORX]|#[0-9A-F]{6})");
    private static final List<Sound> PETTING_SOUNDS = List.of(Sound.ENTITY_WOLF_PANT, Sound.ENTITY_WOLF_AMBIENT);

    private final Server server;
    private final DogRepository dogs;
    private final Supplier<Settings> settings;
    private final Messages messages;
    private final Effects effects;
    private final DebugLog debug;
    private final PluginMobs pluginMobs;
    /** Calmed dogs, until when (epoch millis). Kept in memory only. */
    private final Map<UUID, Long> calmUntil = new HashMap<>();

    public DogService(Server server, DogRepository dogs, Supplier<Settings> settings, Messages messages, Effects effects,
                      DebugLog debug, PluginMobs pluginMobs) {
        this.server = server;
        this.dogs = dogs;
        this.settings = settings;
        this.messages = messages;
        this.effects = effects;
        this.debug = debug;
        this.pluginMobs = pluginMobs;
    }

    // Lookups

    public Optional<Dog> find(UUID dogId) {
        return dogs.find(dogId);
    }

    /** Whether the player is the dog's owner or one of its co-owners. */
    public static boolean isOwnerOrCoOwner(Dog dog, UUID player) {
        return dog.getOwnerId().equals(player) || dog.getCoOwners().contains(player);
    }

    /** Whether the player may look after the dog: its owner, a co-owner, or an admin with mydog.admin.others. */
    public static boolean canControl(Dog dog, Player player) {
        return isOwnerOrCoOwner(dog, player.getUniqueId()) || Permissions.has(player, Permissions.ADMIN_OTHERS);
    }

    /** Living dogs other players have shared with this player, sorted by owner and ID. */
    public List<Dog> sharedWith(UUID player) {
        return dogs.all().stream()
                .filter(dog -> !dog.isDead() && dog.getCoOwners().contains(player))
                .sorted(Comparator.comparing((Dog dog) -> dog.getOwnerId().toString()).thenComparingInt(Dog::getIdentifier))
                .toList();
    }

    /** The UUID of a player who owns dogs, by name (online players first). */
    public Optional<UUID> ownerByName(String name) {
        Player online = server.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online.getUniqueId());
        }
        return dogs.all().stream().map(Dog::getOwnerId).distinct()
                .filter(id -> name.equalsIgnoreCase(server.getOfflinePlayer(id).getName()))
                .findFirst();
    }

    public void share(Dog dog, UUID player) {
        dog.addCoOwner(player);
        dogs.markChanged(dog);
    }

    public void unshare(Dog dog, UUID player) {
        dog.removeCoOwner(player);
        dogs.markChanged(dog);
    }

    /** The dog for an entity, if the entity is a MyDog dog. */
    public Optional<Dog> dogOf(Entity entity) {
        return entity instanceof Wolf ? dogs.find(entity.getUniqueId()) : Optional.empty();
    }

    /** The dog's wolf, if it is loaded. */
    public Optional<Wolf> wolfOf(Dog dog) {
        return server.getEntity(dog.getDogId()) instanceof Wolf wolf && wolf.isValid() ? Optional.of(wolf) : Optional.empty();
    }

    public List<Dog> dogsOf(UUID ownerId) {
        return dogs.findByOwner(ownerId);
    }

    public List<Dog> aliveDogsOf(UUID ownerId) {
        return dogs.findByOwner(ownerId).stream().filter(dog -> !dog.isDead()).toList();
    }

    public List<Dog> deadDogsOf(UUID ownerId) {
        return dogs.findByOwner(ownerId).stream().filter(Dog::isDead).toList();
    }

    public Optional<Dog> find(UUID ownerId, int identifier) {
        return dogs.findByOwnerAndIdentifier(ownerId, identifier);
    }

    /**
     * Whether a player may get another dog. A {@code mydog.limit.<n>} permission caps a player at n dogs, dead ones
     * included; operators have no limit.
     */
    public boolean canOwnMore(Player player) {
        if (player.isOp()) {
            return true;
        }
        int owned = dogs.findByOwner(player.getUniqueId()).size();
        for (int limit = 0; limit <= owned; limit++) {
            if (player.hasPermission(Permissions.LIMIT_PREFIX + limit)) {
                return false;
            }
        }
        return true;
    }

    // Creating dogs

    /** Whether a tamed wolf may become a dog: not when it is another plugin's mob, like a MythicMobs mount or an NPC. */
    public boolean canBecomeDog(Wolf wolf) {
        return !pluginMobs.belongsToOtherPlugin(wolf);
    }

    /**
     * Turns a tamed wolf into a dog. A wolf that already has a name or a non-default collar (when importing wolves
     * that were tamed before MyDog) keeps them.
     */
    public Dog register(Wolf wolf, Player owner, String existingName, DyeColor existingCollar) {
        Settings.Dogs config = settings.get().dogs();
        if (existingCollar == null && config.randomCollarColor()) {
            wolf.setCollarColor(DyeColors.random());
        }

        String name = existingName != null ? cleanName(existingName) : "";
        Dog dog = new Dog(wolf.getUniqueId(), owner.getUniqueId(), nextFreeIdentifier(owner.getUniqueId()),
                name.isEmpty() ? randomName() : name, DyeColors.nameColor(wolf.getCollarColor()), 1, 0, Instant.now(),
                false, false, Dog.LastSeen.of(wolf.getLocation()));
        dogs.markChanged(dog);
        updateWolf(dog, wolf);
        debug.log(() -> "New dog " + dog.getDogName() + " (" + dog.getDogId() + ") for " + owner.getName());
        return dog;
    }

    public String randomName() {
        List<String> names = settings.get().dogs().names();
        if (names.isEmpty()) {
            names = DefaultDogNames.NAMES;
        }
        return names.get(ThreadLocalRandom.current().nextInt(names.size()));
    }

    /** A name without color codes or the level and mode an old nametag added. */
    public static String cleanName(String name) {
        return NAMETAG_EXTRAS.matcher(LEGACY_CODES.matcher(name).replaceAll("")).replaceAll("").trim();
    }

    /** The lowest ID (from 1 up) that none of the owner's dogs uses. */
    public int nextFreeIdentifier(UUID ownerId) {
        List<Dog> owned = dogs.findByOwner(ownerId);
        int id = 1;
        while (true) {
            int candidate = id;
            if (owned.stream().noneMatch(dog -> dog.getIdentifier() == candidate)) {
                return id;
            }
            id++;
        }
    }

    // The wolf entity

    /** Applies the dog's nametag, and the health and damage of its level, to its wolf if loaded. */
    public void updateWolf(Dog dog) {
        wolfOf(dog).ifPresent(wolf -> updateWolf(dog, wolf));
    }

    public void updateWolf(Dog dog, Wolf wolf) {
        applyNametag(dog, wolf);
        rememberAppearance(dog, wolf, wolf.getCollarColor());

        Settings.Levels levels = settings.get().levels();
        if (!levels.enabled()) {
            return;
        }
        LevelTable.Level stats = levels.table().get(dog.getLevel());
        AttributeInstance maxHealth = wolf.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(stats.health());
            wolf.setHealth(maxHealth.getValue());
        }
        AttributeInstance damage = wolf.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(stats.damage());
        }
        applyEffects(dog, wolf, levels.table());
    }

    /**
     * Gives the wolf the potion effects of its level, permanently and without particles. Permanent effects it no
     * longer qualifies for are taken off; effects from potions run out as usual.
     */
    private static void applyEffects(Dog dog, Wolf wolf, LevelTable table) {
        Map<NamespacedKey, Integer> wanted = table.effectsAt(dog.getLevel());
        for (NamespacedKey key : table.allEffects()) {
            PotionEffectType type = Registry.EFFECT.get(key);
            if (type == null) {
                continue;
            }
            PotionEffect current = wolf.getPotionEffect(type);
            Integer level = wanted.get(key);
            if (level == null) {
                if (current != null && current.isInfinite()) {
                    wolf.removePotionEffect(type);
                }
            } else if (current == null || !current.isInfinite() || current.getAmplifier() != level - 1) {
                wolf.removePotionEffect(type);
                wolf.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, level - 1, true, false, false));
            }
        }
    }

    /** Saves how the wolf looks, so it can be revived looking the same. */
    private void rememberAppearance(Dog dog, Wolf wolf, DyeColor collar) {
        Dog.Appearance appearance = new Dog.Appearance(collar, wolf.getVariant().getKey().toString(),
                wolf.getSoundVariant().getKey().toString());
        if (!appearance.equals(dog.getAppearance())) {
            dog.setAppearance(appearance);
            dogs.markChanged(dog);
        }
    }

    /** Gives a (new) wolf the looks the dog had. Parts that aren't known, or no longer exist, are left as they are. */
    private void applyAppearance(Dog dog, Wolf wolf) {
        Dog.Appearance appearance = dog.getAppearance();
        if (appearance == null) {
            return;
        }
        if (appearance.collar() != null) {
            wolf.setCollarColor(appearance.collar());
        }
        for (TreatKind kind : TreatKind.values()) {
            NamespacedKey key = current(dog, kind);
            if (key != null) {
                kind.apply(wolf, key);
            }
        }
    }

    // Calming

    /** Makes the dog drop its target and ignore new ones for {@code dogs.calm-seconds}. */
    public void calm(Dog dog) {
        calmUntil.put(dog.getDogId(), System.currentTimeMillis() + settings.get().dogs().calmSeconds() * 1000L);
        wolfOf(dog).ifPresent(wolf -> {
            wolf.setTarget(null);
            wolf.setAngry(false);
        });
    }

    public boolean isCalm(UUID dogId) {
        Long until = calmUntil.get(dogId);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            calmUntil.remove(dogId);
            return false;
        }
        return true;
    }

    // Breeds and personalities

    /**
     * Whether the dog may pick a breed (or personality) for free: it was born before Minecraft had them, still has
     * the look every old wolf got (Pale / Classic), and its owner hasn't used the choice yet.
     */
    public boolean hasFreeChoice(Dog dog, TreatKind kind) {
        Settings.Breeds breeds = settings.get().breeds();
        Dog.Appearance appearance = dog.getAppearance();
        if (!breeds.freeChoice() || appearance == null || dog.hasUsedFreeChoice(kind)) {
            return false;
        }
        Instant cutoff = kind == TreatKind.BREED ? breeds.breedCutoff() : breeds.personalityCutoff();
        String current = kind == TreatKind.BREED ? appearance.variant() : appearance.soundVariant();
        return dog.getBirthday().isBefore(cutoff) && kind.legacyDefault().toString().equals(current);
    }

    /** The dog's breed or personality as last seen, or null if MyDog hasn't seen its wolf yet. */
    public static NamespacedKey current(Dog dog, TreatKind kind) {
        Dog.Appearance appearance = dog.getAppearance();
        String key = appearance == null ? null : kind == TreatKind.BREED ? appearance.variant() : appearance.soundVariant();
        return key == null ? null : NamespacedKey.fromString(key);
    }

    /** Gives the dog's wolf a new breed or personality. Returns false if it doesn't exist on this server. */
    public boolean changeLook(Dog dog, Wolf wolf, TreatKind kind, NamespacedKey key) {
        if (!kind.apply(wolf, key)) {
            return false;
        }
        rememberAppearance(dog, wolf, wolf.getCollarColor());
        effects.levelUp(wolf);
        return true;
    }

    public void useFreeChoice(Dog dog, TreatKind kind) {
        dog.setFreeChoiceUsed(kind);
        dogs.markChanged(dog);
    }

    /** The wolf armor the wolf is wearing, if any. */
    public static Optional<ItemStack> armorOf(Wolf wolf) {
        EntityEquipment equipment = wolf.getEquipment();
        ItemStack armor = equipment != null ? equipment.getItem(EquipmentSlot.BODY) : null;
        return armor == null || armor.getType().isAir() ? Optional.empty() : Optional.of(armor);
    }

    /** The max health a dog's level gives it. */
    public double maxHealth(Dog dog) {
        return settings.get().levels().table().get(dog.getLevel()).health();
    }

    // Entity#setCustomName(String) is deprecated on Paper in favor of components, but it is the only call Spigot has
    @SuppressWarnings("deprecation")
    private void applyNametag(Dog dog, Wolf wolf) {
        wolf.setCustomName(Messages.legacy(nametag(dog)));
        wolf.setCustomNameVisible(!settings.get().dogs().nametagOnlyOnHover());
    }

    private Component nametag(Dog dog) {
        Settings current = settings.get();
        if (!current.levels().enabled() || !current.dogs().nametagShowLevel()) {
            return messages.component("nametag.plain", Messages.dog(dog));
        }
        Component mode = messages.component(dog.isAngry() ? "nametag.attack-mode" : "nametag.defend-mode");
        return messages.component("nametag.with-level", Messages.dog(dog), Messages.component("mode", mode));
    }

    /** Records where the dog's wolf is now. */
    public void updateLastSeen(Dog dog, Wolf wolf) {
        dog.setLastSeen(Dog.LastSeen.of(wolf.getLocation()));
        dogs.markChanged(dog);
    }

    // Changes players make

    public void rename(Dog dog, String name) {
        dog.setDogName(cleanName(name));
        dogs.markChanged(dog);
        updateWolf(dog);
    }

    public void setCollarColor(Dog dog, DyeColor color) {
        dog.setColor(DyeColors.nameColor(color));
        dogs.markChanged(dog);
        wolfOf(dog).ifPresent(wolf -> {
            applyNametag(dog, wolf);
            // Called before vanilla dyes the collar, so record the new color rather than reading the wolf's
            rememberAppearance(dog, wolf, color);
        });
    }

    public void setAttackMode(Dog dog, boolean attack) {
        dog.setAngry(attack);
        dogs.markChanged(dog);
        wolfOf(dog).ifPresent(wolf -> applyNametag(dog, wolf));
    }

    public void sit(Dog dog, boolean sit) {
        wolfOf(dog).ifPresent(wolf -> {
            wolf.setSitting(sit);
            updateLastSeen(dog, wolf);
        });
    }

    /**
     * Gives a dog a new ID. If another of the owner's dogs already has it, that dog gets the lowest free ID instead.
     */
    public void changeIdentifier(Dog dog, int identifier) {
        if (dog.getIdentifier() == identifier) {
            return;
        }
        dogs.findByOwnerAndIdentifier(dog.getOwnerId(), identifier).ifPresent(other -> {
            other.setIdentifier(nextFreeIdentifier(dog.getOwnerId()));
            dogs.markChanged(other);
        });
        dog.setIdentifier(identifier);
        dogs.markChanged(dog);
    }

    /** Hands a dog over to a new owner (after a trade). It gets the new owner's lowest free ID. */
    public void transfer(Dog dog, Player newOwner) {
        dog.setOwnerId(newOwner.getUniqueId());
        // Co-owners were the old owner's friends
        dog.clearCoOwners();
        dog.setIdentifier(nextFreeIdentifier(newOwner.getUniqueId()));
        dogs.markChanged(dog);
        wolfOf(dog).ifPresent(wolf -> {
            wolf.setOwner(newOwner);
            applyNametag(dog, wolf);
        });
    }

    /** Lets a dog go: the wolf becomes wild again and MyDog forgets about it. */
    @SuppressWarnings("deprecation") // see applyNametag
    public void release(Dog dog) {
        wolfOf(dog).ifPresent(wolf -> {
            // Only tamed wolves wear armor, so it comes off before the wolf goes wild
            armorOf(wolf).ifPresent(armor -> {
                wolf.getWorld().dropItemNaturally(wolf.getLocation(), armor);
                wolf.getEquipment().setItem(EquipmentSlot.BODY, null);
            });
            wolf.setCustomName(null);
            wolf.setCustomNameVisible(false);
            wolf.setSitting(false);
            wolf.setTamed(false);
        });
        // A stored dog's armor goes back to its owner
        Player owner = server.getPlayer(dog.getOwnerId());
        if (dog.isStored() && dog.getStored().armor() != null && owner != null) {
            for (ItemStack leftover : owner.getInventory().addItem(dog.getStored().armor()).values()) {
                owner.getWorld().dropItemNaturally(owner.getLocation(), leftover);
            }
        }
        dogs.delete(dog.getDogId());
    }

    public void pet(Dog dog, Player player) {
        if (!Permissions.has(player, Permissions.PET)
                || (!isOwnerOrCoOwner(dog, player.getUniqueId()) && !Permissions.has(player, Permissions.PET_OTHERS))) {
            return;
        }
        Optional<Wolf> wolf = wolfOf(dog);
        if (wolf.isEmpty()) {
            return;
        }

        messages.send(player, "dog.petting", Messages.dog(dog));
        if (ThreadLocalRandom.current().nextInt(10) == 1) {
            wolf.get().playEffect(EntityEffect.WOLF_SHAKE);
            messages.send(player, "dog.petting-splash", Messages.dog(dog));
        }
        effects.petting(wolf.get());
        Sound sound = PETTING_SOUNDS.get(ThreadLocalRandom.current().nextInt(PETTING_SOUNDS.size()));
        player.playSound(player.getLocation(), sound, 3.0F, 1.0F);
    }

    // Levels

    public void giveExperience(Dog dog, int experience) {
        Settings.Levels levels = settings.get().levels();
        if (!levels.enabled() || experience <= 0) {
            return;
        }
        dog.setExperience(dog.getExperience() + experience);
        int newLevel = levels.table().levelFor(dog.getExperience(), dog.getLevel());
        dogs.markChanged(dog);
        debug.log(() -> dog.getDogName() + " got " + experience + " experience, now " + dog.getExperience());

        if (newLevel > dog.getLevel()) {
            setLevel(dog, newLevel);
        }
    }

    /** Sets the level, updates the wolf's stats and tells the owner. */
    public void setLevel(Dog dog, int level) {
        dog.setLevel(level);
        dogs.markChanged(dog);
        updateWolf(dog);

        Player owner = server.getPlayer(dog.getOwnerId());
        if (owner == null) {
            return;
        }
        messages.send(owner, "dog.level-up", Messages.dog(dog));
        wolfOf(dog).ifPresent(effects::levelUp);
        owner.playSound(owner.getLocation(), settings.get().levels().levelUpSound(), 3.0F, 1.0F);
    }

    // Death and revival

    /** Called when a dog's wolf dies. Keeps the dog as dead when revival is on, otherwise deletes it. */
    public void died(Dog dog) {
        if (settings.get().dogs().revival().enabled()) {
            dog.setDead(true);
            dogs.markChanged(dog);
        } else {
            dogs.delete(dog.getDogId());
        }
    }

    public int revivalPrice(Dog dog) {
        return dog.getLevel() * settings.get().dogs().revival().pricePerLevel();
    }

    /** Brings a dead dog back as a new wolf, sitting at the owner's feet. Payment is up to the caller. */
    public void revive(Dog dog, Player at) {
        dog.setDead(false);
        spawnWolf(dog, at, true);
    }

    // Storing

    /** Puts the dog away: its wolf is removed, and its health and armor are kept for when it is summoned. */
    public void store(Dog dog, Wolf wolf) {
        dog.setStored(new Dog.Stored(wolf.getHealth(), armorOf(wolf).map(ItemStack::clone).orElse(null)));
        dog.setLastSeen(Dog.LastSeen.of(wolf.getLocation()));
        dogs.markChanged(dog);
        wolf.remove();
    }

    /** Brings a stored dog back next to the player summoning it, as it was when it was stored. */
    public void summon(Dog dog, Player at) {
        Dog.Stored stored = dog.getStored();
        dog.setStored(null);
        Wolf wolf = spawnWolf(dog, at, false);
        if (stored == null) {
            return;
        }
        if (stored.armor() != null) {
            wolf.getEquipment().setItem(EquipmentSlot.BODY, stored.armor());
        }
        AttributeInstance maxHealth = wolf.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : stored.health();
        wolf.setHealth(Math.clamp(stored.health(), 1.0, max));
    }

    /**
     * Spawns a new wolf for the dog next to a player (its owner, a co-owner or an admin), with its look, nametag and
     * stats, and moves the dog to it. The wolf always belongs to the dog's owner.
     */
    private Wolf spawnWolf(Dog dog, Player at, boolean sitting) {
        Wolf wolf = at.getWorld().spawn(at.getLocation(), Wolf.class, spawned -> {
            spawned.setTamed(true);
            spawned.setOwner(server.getOfflinePlayer(dog.getOwnerId()));
            spawned.setSitting(sitting);
            applyAppearance(dog, spawned);
        });
        dogs.changeDogId(dog, wolf.getUniqueId());
        dog.setLastSeen(Dog.LastSeen.of(wolf.getLocation()));
        dogs.markChanged(dog);
        updateWolf(dog, wolf);
        return wolf;
    }
}
