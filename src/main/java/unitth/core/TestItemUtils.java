/*
 * MIT License - see LICENSE.txt
 */

package unitth.core;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

	/**
	 * 
	 * @param dateStamp
	 *            Must be in the following format dd/MM/yyyy hh:mm:ss.
	 * @return
	 */
	public static String fixFitNesseDateFormat(String dateStamp) {
		Date date = null;

		// Avoid problems if using the method for already fixed date strings.
		if (dateStamp.contains("&nbsp;")) {
			dateStamp = dateStamp.replace("&nbsp;", " ");
		}

		// Trying format 1
		try {
			DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
			date = formatter.parse(dateStamp);
			DateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
					Locale.US);
			String ret = formatter2.format(date);
			ret = ret.replace(" ", "&nbsp;");
			return ret;
		} catch (ParseException e) { /* continue */
		} catch (Exception e) {
			return null;
		}

		// Trying format 2
		try {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
			date = formatter.parse(dateStamp);
			DateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
					Locale.US);
			String ret = formatter2.format(date);
			ret = ret.replace(" ", "&nbsp;");
			return ret;
		} catch (ParseException e) { 
			return null;
		}
	}

	public static double calculateExecutionTimeDiff(String startDate,
			String endDate) {
		
		Date start = null;
		Date end = null;
		startDate = startDate.replace("&nbsp;", " ");
		endDate = endDate.replace("&nbsp;", " ");
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		try {
			start = formatter.parse(startDate);
			end = formatter.parse(endDate);
		} catch (ParseException dfe) {
			Log.error("Could not parse date", dfe);
			return 0.0;
		}
		
		long timeDiff = (end.getTime() - start.getTime()) / 1000; // Milli seconds / 1000
		return (double)timeDiff;  // seconds
	}

	/**
	 * 
	 * @param path
	 * @return
	 */
	public static String getTestSuiteFolderFromPath(String path) {
		File f = new File(path);
		URI fUri = f.toURI();
		String normalizedPath = fUri.normalize().getPath();
		if (normalizedPath.charAt(normalizedPath.length() - 1) == '/') {
			normalizedPath = normalizedPath.substring(0, normalizedPath
					.length() - 1);
		}

		int i = normalizedPath.lastIndexOf("/");
		if (i == -1) {
			return normalizedPath;
		}
		return normalizedPath.substring(i + 1, normalizedPath.length());
	}
	
	/**
	 * Ripps the test suite part from a full FitNesse test case name.
	 * @param name The full test case name.
	 * @return The test suite part of the full FitNesse test case name.
	 * If null or empty the string undef is returned.
	 */
	public static String getFitNesseTestSuiteFromTestCaseName(String name) {
		if (null == name || name.equals("") || name.equalsIgnoreCase("undef")) {
			return "undef";
		}
		return name.substring(0, name.lastIndexOf("."));
	}
	
	public static String getShortestCommonality(String a, String b) {
		// They are equal
		if (a.equalsIgnoreCase(b)) {
			return a;
		}
		
		// They have nothing in common
		if (!a.regionMatches(true, 0, b, 0, 1)) {
			return "root.undef";
		}
		
		// Partly shared
		int shortest = a.length() > b.length() ? b.length() : a.length();
		for (int i=1; i<shortest+1; i++) {
			if (!a.regionMatches(true, 0, b, 0, i)) {
				// Find the last dot with an index lower than i, 
				// since it is the last matching character.
				int idx = a.lastIndexOf(".", i-1);
				// Could be a mismatch from the start.
				if (!a.regionMatches(true, 0, b, 0, idx)) {
					return "root.undef";
				}
				return a.substring(0, idx);
			}
		}
		return a.substring(0, shortest);
	}
}

/* eof */
