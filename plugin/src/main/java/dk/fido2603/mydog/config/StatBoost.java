package dk.fido2603.mydog.config;

/**
 * Stats that stat treats raise permanently (up to a cap), on top of what the dog's level gives. Health and damage
 * are in points; speed is in percent of the wolf's base speed.
 */
public enum StatBoost {
    HEALTH("health"),
    DAMAGE("damage"),
    SPEED("speed");

    private final String id;

    StatBoost(String id) {
        this.id = id;
    }

    /** As written in config.yml, dogs.yml, messages.yml and commands. */
    public String id() {
        return id;
    }

    public static StatBoost byId(String id) {
        for (StatBoost stat : values()) {
            if (stat.id.equalsIgnoreCase(id)) {
                return stat;
            }
        }
        return null;
    }
}
