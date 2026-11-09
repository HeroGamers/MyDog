package dk.fido2603.mydog.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DisplayNameTest {
    @Test
    void turnsKeysIntoNames() {
        assertEquals("Snowy", Messages.displayName("minecraft:snowy"));
        assertEquals("Some Thing", Messages.displayName("minecraft:some_thing"));
        assertEquals("Puglin", Messages.displayName("puglin"));
        assertEquals("?", Messages.displayName(null));
    }
}
