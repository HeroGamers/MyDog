package dk.fido2603.mydog.config;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The levels from {@code levels.table}: how much experience each level needs and the stats it gives.
 */
public final class LevelTable {
    /** Dogs never get less max health than this, whatever the table says. */
    private static final double MIN_HEALTH = 5.0;
    /** Dogs never deal less damage than this, whatever the table says. */
    private static final double MIN_DAMAGE = 1.0;

    public record Level(int level, int experience, double health, double damage) {
        public Level {
            health = Math.max(health, MIN_HEALTH);
            damage = Math.max(damage, MIN_DAMAGE);
        }
    }

    private final NavigableMap<Integer, Level> levels;

    public LevelTable(Map<Integer, Level> levels) {
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("The level table needs at least one level");
        }
        this.levels = Collections.unmodifiableNavigableMap(new TreeMap<>(levels));
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
