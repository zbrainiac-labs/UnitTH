/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

import unitth.core.Log;
import unitth.core.TestItemUtils;

/**
 * This is the super class for all test item classes. It provides some common
 * functionality and variables.
 * 
 */
public class TestItem {

	protected static final String UNKNOWN_TIME_STAMP = "unknown, broken run";
	
	protected String name = "";
	protected double executionTime = 0.000;
	protected String runDate = null;
	protected double passPct = 0.0;

	/**
	 * This method sets the execution time parsed from a string.
	 * 
	 * @param s
	 *            The string to parse into a double.
	 */
	public void setExecutionTime(String s) {
		try {
			executionTime = Double.parseDouble(s);
		} catch (NumberFormatException nfe) {
			Log.warn("Could not parse the execution time " + s + " setting the execution time to 0.000");
			executionTime = 0.000;
		}
	}

	/**
	 * This method returns the execution time as a string.
	 * 
	 * @return The stringified execution time.
	 */
	public String getExecutionTime() {
		return TestItemUtils.executionTimeToString(executionTime);
	}

	/**
	 * This method returns the execution time as a double.
	 * 
	 * @return The execution time.
	 */
	public double getExecutionTimeDouble() {
		return executionTime;
	}

	/**
	 * Returns the name of the test item, qualified test case name, test module
	 * name or simply the string Run+index for the test run objects.
	 * 
	 * @return The name of the test item.
	 */
	public String getName() {
		if (name == null) {
			return "module-not-defined";
		}

		return name;
	}

	/**
	 * Sets the test item name.
	 * 
	 * @param s
	 *            The name to be set.
	 */
	public void setName(String s) {
		name = s;
	}
	
	/**
	 * Strips the last part of the qualified name to get the package name.
	 * @return The package part ripped from the full name.
	 */
	public String getPackageName() {
		if (name == null) {
			return "package-not-defined";
		}

		int liof = name.lastIndexOf(".");
		if (-1 == liof) {
			return "not-in-package";
		}
		return name.substring(0, liof);
	}
	
	/**
	 * This method calculates a long value from a stringified date. The method
	 * is primarily used for calculating trends based on time stamps.
	 * 
	 * @return The run date converted to long.
	 */
	public long getRunDateAsLong() {
		return TestItemUtils.runDateToLong(runDate);
	}
	
	/**
	 * Returns the stringified pass percentage.
	 * 
	 * @return The pass percentage as string.
	 */
	public String getPassPct() {
		return TestItemUtils.passPctToString(passPct);
	}
}

/* eof */
