package dk.fido2603.mydog.utils;

import java.util.concurrent.TimeUnit;

public class TimeUtils
{
	// From my other plugin, Semi-Hardcore
	public static String parseMillisToUFString(long millis) {
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		String d = "days";
		String h = "hours";
		String m = "minutes";
		
		if (days > 0) {
			// For the grammar <3
			if (days == 1) {
				d = "day";
			}
			if ((hours - TimeUnit.DAYS.toHours(days)) > 0) {
				// For the grammar <3
				if ((hours - TimeUnit.DAYS.toHours(days)) == 1) {
					h = "hour";
				}
				if ((minutes - TimeUnit.HOURS.toMinutes(hours)) > 0) {
					// For the grammar <3
					if ((minutes - TimeUnit.HOURS.toMinutes(hours)) == 1) {
						m = "minute";
					}
					// Send the return string
					return String.format("%d %s, %d %s and %d %s", days, d, hours - TimeUnit.DAYS.toHours(days), h, minutes - TimeUnit.HOURS.toMinutes(hours), m);
				}
				// Send the return string
				return String.format("%d %s, %d %s", days, d, hours - TimeUnit.DAYS.toHours(days), h);
			}
			// Send the return string
			return String.format("%d %s", days, d);
		}
		if (hours > 0) {
			// For the grammar <3
			if ((hours - TimeUnit.DAYS.toHours(days)) == 1) {
				h = "hour";
			}
			if ((minutes - TimeUnit.HOURS.toMinutes(hours)) > 0) {
				// For the grammar <3
				if ((minutes - TimeUnit.HOURS.toMinutes(hours)) == 1) {
					m = "minute";
				}
				// Send the return string
				return String.format("%d %s and %d %s", hours, h, minutes - TimeUnit.HOURS.toMinutes(hours), m);
			}
			// Send the return string
			return String.format("%d %s", hours, h);
		}
		// For the grammar <3
		if ((minutes - TimeUnit.HOURS.toMinutes(hours)) == 1) {
			m = "minute";
		}
		// Send the return string
		return String.format("%d %s", minutes, m);
	}
}