package dk.fido2603.mydog.utils.versioning;

import org.bukkit.plugin.Plugin;

// From DogOnFire's Versioning in Werewolf
// https://github.com/DogOnFire/Werewolf

public class TesterFactory
{

	@SuppressWarnings("rawtypes")
	public static Tester getNewTester(Plugin plugin)
	{
		if (plugin == null)
		{
			return new Tester<Plugin>()
			{

				@Override
				public boolean isEnabled(Plugin t)
				{
					return false;
				}
			};
		}
		else
		{
			return new Tester<Plugin>()
			{

				@Override
				public boolean isEnabled(Plugin t)
				{
					return t.isEnabled();
				}
			};
		}
	}

	@SuppressWarnings("rawtypes")
	public static Tester getDefaultTester()
	{
		return new Tester()
		{

			@Override
			public boolean isEnabled(Object t)
			{
				return true;
			}
		};
	}

}