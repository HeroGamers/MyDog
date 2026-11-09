package dk.fido2603.mydog.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

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
    void statsHaveAFloor() {
        LevelTable weak = new LevelTable(Map.of(1, new LevelTable.Level(1, 0, 1, 0)));
        assertEquals(5.0, weak.get(1).health());
        assertEquals(1.0, weak.get(1).damage());
    }
}
