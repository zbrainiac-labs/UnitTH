/*
 * MIT License - see LICENSE.txt
 */

package unitth.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Contains core functionality that can be applied to most of the test item
 * classes in UnitUnitTH
 * 
 * 
 */
public class TestItemUtils {

	/**
	 * This method converts the execution times for any of the test objects
	 * <code>TestCase TestModule TestRun</code>. If the execution time is larger
	 * than 120 seconds the number of minutes will be printed. If the execution
	 * time is larger than 60 minutes the number of hours will be printed.
	 * 
	 * @param executionTime
	 *            The time to convert.
	 * @return Returns a formatted string representing the execution time.
	 */
	public static String executionTimeToString(double executionTime) {
		int minutes = 0;

		if (executionTime < 120.0) { // lt 2 minutes
			String ret = String.format(Locale.US, "%1.3f", executionTime);
			return ret.replaceAll(" ", "&nbsp;");
		} else if (executionTime < 60.0 * 60.0 * 2.0) { // lt 2 hours, 7200 sec
			if ((int) executionTime % 60 < 30) {
				minutes = (int) executionTime / 60;
			} else {
				minutes = ((int) executionTime / 60) + 1;
			}
			String ret = String.format(Locale.US, "%1.3f (~%dmin)",
					executionTime, minutes);
			return ret.replaceAll(" ", "&nbsp;");
		} else { // Above 2 hours
			int hours = (int) executionTime / 3600;
			if ((int) executionTime % 60 < 30) {
				minutes = ((int) executionTime - (hours * 3600)) / 60;
			} else {
				minutes = (((int) executionTime - (hours * 3600)) / 60) + 1;
				if (60 == minutes) {
					hours++;
					minutes = 0;
				}
			}

			String ret = String.format(Locale.US, "%1.3f (~%dh %dmin)",
					executionTime, hours, minutes);
			return ret.replaceAll(" ", "&nbsp;");
		}
	}

	/**
	 * This method formats a pass percentage double value to a String with a set
	 * number of decimals.
	 * 
	 * @param passPct
	 *            The double representing the pass percentage for any of the
	 *            test items.
	 * @return A formatted string that represents the pass percentage
	 */
	public static String passPctToString(double passPct) {
		return String.format(Locale.US, "%1.2f", passPct);
	}

	/** Formats a count difference with a leading + when positive. */
	public static String signed(int diff) {
		return (diff > 0 ? "+" : "") + diff;
	}

	/** Formats a pass rate difference with a leading + when positive and a trailing %. */
	public static String signedPct(double diff) {
		return (diff > 0 ? "+" : "") + passPctToString(diff) + "%";
	}

	/**
	 * This method converts any parsed time stamps so that they all look the
	 * same in all reports.
	 * 
	 * @param timeStamp
	 *            The time stamp to format.
	 * @return The formatted time stamp as String.
	 */
	public static String fixDateFormat(String timeStamp) {
		String ret = timeStamp;
		if (timeStamp != null) {
			if (timeStamp.contains(" CET")) {
				// Remove the CET in any time stamps like
				// "2008-11-12 22:30:17 CET"
				ret = timeStamp.replace(" CET", "");
			} else if (timeStamp.contains(" CEST")) {
				// CEST must be looked for before the check for T
				// Remove the CEST in any time stamps like
				// "2008-11-12 22:30:17 CEST"
				ret = timeStamp.replace(" CEST", "");
			} else if (timeStamp.contains("T")) {
				// Remove the T in any time stamps like "2008-11-12T22:30:17"
				ret = timeStamp.replace("T", " ");
			}
			ret = ret.replace(" ", "&nbsp;");
		} else {
			ret = "";
		}
		return ret;
	}

	/**
	 * This method converts any long time stamps so that they all look the same
	 * in all reports.
	 * 
	 * @param timeStamp
	 *            The time stamp to convert to a date.
	 * @return The formatted time stamp as String.
	 */
	public static String fixDateFormat(long timeStamp) {
		// 2009-01-28 22:55:51
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
				Locale.US);
		return fixDateFormat(sdf.format(new Date(timeStamp)));
	}

	/**
	 * Packages are stored flat by dotted name. This folds every package into
	 * its direct parent, deepest first, so grandchildren reach the root.
	 * Names are matched case-insensitively.
	 *
	 * @param packages
	 *            Packages keyed by dotted name.
	 * @param addChildToParent
	 *            Called with (parent, child).
	 */
	public static <V> void foldIntoParents(Map<String, V> packages, BiConsumer<V, V> addChildToParent) {
		String[] names = packages.keySet().toArray(new String[0]);
		Arrays.sort(names, Comparator.comparingInt(String::length).reversed());
		for (String child : names) {
			int dot = child.lastIndexOf('.');
			if (dot < 0) {
				continue;
			}
			String parent = child.substring(0, dot);
			for (String candidate : names) {
				if (parent.equalsIgnoreCase(candidate) && !candidate.equalsIgnoreCase(child)) {
					addChildToParent.accept(packages.get(candidate), packages.get(child));
				}
			}
		}
	}

	/**
	 * Converts a <code>String</code> object run date to a long value.
	 * 
	 * @param runDate
	 *            The date to convert.
	 * @return The date as long.
	 */
	public static long runDateToLong(String runDate) {
		try {
			// 2009-01-13 16:33:37
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String rd = runDate;
			rd = rd.replaceAll("&nbsp;", " ");
			Date date = formatter.parse(rd);
			return date.getTime();
		} catch (Exception e) {
			// Could not convert this date properly
			return 0;
		}
	}
}

/* eof */
