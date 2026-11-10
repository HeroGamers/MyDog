package dk.fido2603.mydog.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

class LegacyTextTest {
    @Test
    void convertsColorsAndPlaceholders() {
        assertEquals("<gold><bold>[<prefix>] <reset><gold>Good doggo, <dog_name><gold>!",
                LegacyText.toMiniMessage("&6&l[{chatPrefix}] &r&6Good doggo, {dogNameColor}{dogName}&6!"));
    }

    @Test
    void colorAfterDecorationResetsIt() {
        // In legacy text "&l...&c" turns bold off; MiniMessage needs an explicit <reset> for that
        assertEquals("<bold>a<reset><red>b", LegacyText.toMiniMessage("&la&cb"));
    }

    @Test
    void convertsHexColors() {
        assertEquals("<#12ab34>x", LegacyText.toMiniMessage("&#12ab34x"));
    }

    @Test
    void escapesTextThatLooksLikeTags() {
        assertEquals("\\<3 dogs", LegacyText.toMiniMessage("<3 dogs"));
    }

    @Test
    void leavesUnknownCodesAlone() {
        assertEquals("Fish & chips", LegacyText.toMiniMessage("Fish & chips"));
    }

    /** The converted message must look exactly like the old one did, apart from how the codes are written. */
    @Test
    void rendersLikeTheOldMessage() {
        String old = "&c&l[MyDog] &r&cYour dog, Fido&c, just passed away... Fido&c lived for 2 days, and got to &4Level 3&c.";
        String converted = LegacyText.toMiniMessage(old);

        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        assertEquals(
                legacy.serialize(legacy.deserialize(old)),
                legacy.serialize(MiniMessage.miniMessage().deserialize(converted, TagResolver.empty())));
    }

    @Test
    void placeholdersStillWorkAfterConversion() {
        String converted = LegacyText.toMiniMessage("&6Who's a good doggo?! {dogNameColor}{dogName}&6 is!");
        String rendered = LegacyComponentSerializer.legacyAmpersand().serialize(MiniMessage.miniMessage().deserialize(converted,
                Placeholder.unparsed("dog_name", "Fido")));
        assertEquals("&6Who's a good doggo?! Fido is!", rendered);
    }
}
