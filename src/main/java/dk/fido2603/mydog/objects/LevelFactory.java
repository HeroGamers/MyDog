package dk.fido2603.mydog.objects;

import dk.fido2603.mydog.MyDog;

public class LevelFactory {
    private final MyDog plugin;

    public LevelFactory(MyDog plugin) {
        this.plugin = plugin;
    }

    public class Level {
        public int level;
        public int exp;
        public double health;
        public double damage;

        public Level(int levelInt, int expNeeded, double healthStat, double damageStat) {
            this.level = levelInt;
            this.exp = expNeeded;
            this.health = healthStat;
            this.damage = damageStat;
            plugin.logDebug("New level created! Level: " + level + " Exp: " + exp + " Health: " + health + " Damage: " + damage);
        }
    }

    public Level newLevel(int level, int exp, double health, double damage) {
        return new Level(level, exp, health, damage);
    }
}