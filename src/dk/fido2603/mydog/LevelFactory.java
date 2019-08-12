package dk.fido2603.mydog;

public class LevelFactory
{
	private MyDog				plugin				= null;

	LevelFactory(MyDog plugin)
	{
		this.plugin = plugin;
	}

	public class Level
	{
		public Integer level;
		public Integer exp;
		public double health;
		public double damage;

		public Level(Integer levelInt, Integer expNeeded, double healthStat, double damageStat)
		{
			this.level = levelInt;
			this.exp = expNeeded;
			this.health = healthStat;
			this.damage = damageStat;
			plugin.logDebug("New level created! Level: " + level + " Exp: " + exp + " Health: " + health + " Damage: " + damage);
		}
	}

	public Level newLevel(Integer level, Integer exp, double health, double damage) {
		return new Level(level, exp, health, damage);
	}
}