package dk.fido2603.mydog.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.NamespacedKey;

/**
 * The levels from {@code levels.table}: how much experience each level needs and the stats it gives.
 */
public final class LevelTable {
    /** Dogs never get less max health than this, whatever the table says. */
    private static final double MIN_HEALTH = 5.0;
    /** Dogs never deal less damage than this, whatever the table says. */
    private static final double MIN_DAMAGE = 1.0;

    /**
     * @param playerDamage damage dealt to players instead of {@code damage}, or null to use {@code damage}
     * @param effects      potion effects (by key) and their level (1 = I) from this level up; 0 removes an effect
     *                     an earlier level gave
     * @param armorProficiency how much less wear (in percent, 0 to 100) wolf armor takes on dogs of this level
     */
    public record Level(int level, int experience, double health, double damage, Double playerDamage,
                        Map<NamespacedKey, Integer> effects, double armorProficiency) {
        public Level {
            effects = Map.copyOf(effects);
            health = Math.max(health, MIN_HEALTH);
            damage = Math.max(damage, MIN_DAMAGE);
            if (playerDamage != null) {
                playerDamage = Math.max(playerDamage, 0.0);
            }
            armorProficiency = Math.clamp(armorProficiency, 0.0, 100.0);
        }

        public Level(int level, int experience, double health, double damage, Double playerDamage,
                     Map<NamespacedKey, Integer> effects) {
            this(level, experience, health, damage, playerDamage, effects, 0.0);
        }

        public Level(int level, int experience, double health, double damage) {
            this(level, experience, health, damage, null, Map.of());
        }
    }

    private final NavigableMap<Integer, Level> levels;

    public LevelTable(Map<Integer, Level> levels) {
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("The level table needs at least one level");
        }
        this.levels = Collections.unmodifiableNavigableMap(new TreeMap<>(levels));
    }

    /**
     * Builds levels 1 to {@code maxLevel} with health and damage spread evenly between min and max, and the
     * experience for each level following {@code curve}: 1 makes every level the same step, higher values make the
     * first levels quick and the last ones much harder.
     */
    public static LevelTable scaled(int maxLevel, int maxExperience, double curve, double minHealth, double maxHealth,
                                    double minDamage, double maxDamage) {
        Map<Integer, Level> levels = new HashMap<>();
        for (int level = 1; level <= maxLevel; level++) {
            double t = maxLevel == 1 ? 0.0 : (level - 1) / (double) (maxLevel - 1);
            int experience = (int) Math.round(maxExperience * Math.pow(t, curve));
            double health = Math.round(minHealth + (maxHealth - minHealth) * t);
            double damage = Math.round((minDamage + (maxDamage - minDamage) * t) * 10.0) / 10.0;
            levels.put(level, new Level(level, experience, health, damage));
        }
        return new LevelTable(levels);
    }

    public int minLevel() {
        return levels.firstKey();
    }

    public int maxLevel() {
        return levels.lastKey();
    }

    /** The stats for a level, using the closest level below it when that exact level isn't in the table. */
    public Level get(int level) {
        Map.Entry<Integer, Level> entry = levels.floorEntry(level);
        return entry != null ? entry.getValue() : levels.firstEntry().getValue();
    }

    /**
     * The potion effects (key to effect level, 1 = I) a dog at this level has: everything the levels up to it give,
     * where a higher level overrides a lower one and 0 removes an effect.
     */
    public Map<NamespacedKey, Integer> effectsAt(int level) {
        Map<NamespacedKey, Integer> effects = new HashMap<>();
        for (Level step : levels.headMap(level, true).values()) {
            effects.putAll(step.effects());
        }
        effects.values().removeIf(amplifier -> amplifier <= 0);
        return effects;
    }

    /** Every effect any level gives, so effects a dog no longer has can be taken away. */
    public Set<NamespacedKey> allEffects() {
        Set<NamespacedKey> all = new HashSet<>();
        for (Level step : levels.values()) {
            all.addAll(step.effects().keySet());
        }
        return all;
    }

    /** The level a dog with this much experience should be at. Dogs never lose levels. */
    public int levelFor(int experience, int currentLevel) {
        int level = currentLevel;
        for (Level candidate : levels.values()) {
            if (candidate.experience() <= experience && candidate.level() > level) {
                level = candidate.level();
            }
        }
        return level;
    }

    /** The experience needed for the next level up, or the top level's experience once a dog is past it. */
    public int nextThreshold(int experience) {
        for (Level level : levels.values()) {
            if (level.experience() > experience) {
                return level.experience();
            }
        }
        return levels.lastEntry().getValue().experience();
    }
}
