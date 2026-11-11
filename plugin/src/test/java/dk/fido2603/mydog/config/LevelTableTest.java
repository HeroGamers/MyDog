package dk.fido2603.mydog.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

class LevelTableTest {
    private final LevelTable table = new LevelTable(Map.of(
            1, new LevelTable.Level(1, 0, 20, 4),
            2, new LevelTable.Level(2, 10, 21, 5),
            3, new LevelTable.Level(3, 100, 22, 6)));

    @Test
    void levelForExperience() {
        assertEquals(1, table.levelFor(9, 1));
        assertEquals(2, table.levelFor(10, 1));
        assertEquals(3, table.levelFor(5000, 1));
    }

    @Test
    void dogsNeverLoseLevels() {
        // A dog set to level 3 by an admin keeps it, whatever its experience
        assertEquals(3, table.levelFor(0, 3));
    }

    @Test
    void nextThreshold() {
        assertEquals(10, table.nextThreshold(0));
        assertEquals(100, table.nextThreshold(10));
        assertEquals(100, table.nextThreshold(500));
    }

    @Test
    void levelsMissingFromTheTableUseTheOneBelow() {
        assertEquals(3, table.get(7).level());
    }

    @Test
    void effectsCarryOverToHigherLevels() {
        NamespacedKey speed = NamespacedKey.minecraft("speed");
        NamespacedKey regeneration = NamespacedKey.minecraft("regeneration");
        LevelTable withEffects = new LevelTable(Map.of(
                1, new LevelTable.Level(1, 0, 40, 4),
                2, new LevelTable.Level(2, 10, 42, 5, null, Map.of(speed, 1)),
                3, new LevelTable.Level(3, 100, 44, 6, null, Map.of(speed, 2, regeneration, 1)),
                4, new LevelTable.Level(4, 200, 46, 7, null, Map.of(speed, 0))));

        assertEquals(Map.of(), withEffects.effectsAt(1));
        assertEquals(Map.of(speed, 1), withEffects.effectsAt(2));
        assertEquals(Map.of(speed, 2, regeneration, 1), withEffects.effectsAt(3));
        // :0 takes speed away again, regeneration carries over
        assertEquals(Map.of(regeneration, 1), withEffects.effectsAt(4));
        assertEquals(Set.of(speed, regeneration), withEffects.allEffects());
    }

    @Test
    void scaledTableSpreadsStats() {
        LevelTable scaled = LevelTable.scaled(5, 1000, 2.0, 40, 80, 4, 20);
        assertTrue(scaled.scaled());
        assertFalse(table.scaled());
        assertEquals(1, scaled.minLevel());
        assertEquals(5, scaled.maxLevel());
        assertEquals(new LevelTable.Level(1, 0, 40, 4), scaled.get(1));
        assertEquals(new LevelTable.Level(5, 1000, 80, 20), scaled.get(5));
        // Halfway: stats halfway, experience (0.5)^2 of the max with curve 2
        assertEquals(new LevelTable.Level(3, 250, 60, 12), scaled.get(3));
    }

    @Test
    void statsHaveAFloor() {
        LevelTable weak = new LevelTable(Map.of(1, new LevelTable.Level(1, 0, 1, 0)));
        assertEquals(5.0, weak.get(1).health());
        assertEquals(1.0, weak.get(1).damage());
    }

    @Test
    void armorProficiencyIsAPercentage() {
        assertEquals(0.0, new LevelTable.Level(1, 0, 40, 4).armorProficiency());
        assertEquals(100.0, new LevelTable.Level(1, 0, 40, 4, null, Map.of(), 150).armorProficiency());
        assertEquals(0.0, new LevelTable.Level(1, 0, 40, 4, null, Map.of(), -10).armorProficiency());
    }
}
