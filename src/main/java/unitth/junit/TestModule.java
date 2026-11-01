/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import unitth.core.TestItemUtils;

/**
 * This class implements the functionality for the test module object. It stores
 * all the statistics parsed for a modules in a unique run.
 * 
 */
/**
 *
 */
public class TestModule extends TestItem implements Comparable<TestModule>, RunCounts {

	private HashMap<String, TestCase> testCases = null;
	private int noTestCases = 0;
	private int noFailures = 0;
	private int noErrors = 0;
	private int noPassed = 0;
	private int noIgnored = 0;
	private int runIdx = 0;
	private boolean isIgnored = false; // If the whole class is @Ignore

	/**
	 * CTOR, initializes the container holding all the test cases.
	 */
	public TestModule() {
		testCases = new HashMap<String, TestCase>();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((null == obj) || !(obj instanceof TestModule)) {
			return false;
		}
		TestModule tm = (TestModule) obj;
		if (runDate.equals(tm.getExecutionDate()) && name.equals(tm.name)
				&& executionTime == tm.getExecutionTimeDouble()
				&& passPct == tm.getPassPctDouble()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return name.hashCode() + runDate.hashCode()
				+ (int) (executionTime * 10000);
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(TestModule tm) {
		if (tm!=null && tm.name != null && !tm.name.equalsIgnoreCase("") && !name.equalsIgnoreCase("")) {
			return name.compareTo(tm.name);
		}
		return -1;
	}

	/**
	 * This method sets the execution run date from a long time stamp.
	 * 
	 * @see unitth.core.TestItemUtils#fixDateFormat
	 * @param timeStamp
	 */
	public void setDate(long timeStamp) {
		runDate = TestItemUtils.fixDateFormat(timeStamp);
	}

	/**
	 * This method sets the execution run date from the parsed string. The time
	 * stamp is first formatted to remove some special characters like CEST, CET
	 * or T.
	 * 
	 * @see unitth.core.TestItemUtils#fixDateFormat
	 * @param timeStamp
	 */
	public void setDate(String timeStamp) {
		runDate = TestItemUtils.fixDateFormat(timeStamp);
	}

	/**
	 * Restores an already formatted run date, e.g. from the run store.
	 * 
	 * @param formatted
	 *            The run date as returned by <code>getExecutionDate</code>.
	 */
	public void setFormattedDate(String formatted) {
		runDate = formatted;
	}

	/**
	 * Sets the number of test cases after first converting the string to an
	 * integer.
	 * 
	 * @param s
	 *            The number of test cases as parsed string
	 */
	public void setNoTestCases(String s) {
		try {
			noTestCases = Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			noTestCases = 0;
		}
	}

	/**
	 * Sets the number of failed test cases after first converting the string to
	 * an integer.
	 * 
	 * @param s
	 *            The number of failed test cases as parsed string
	 */
	public void setNoFailures(String s) {
		try {
			noFailures = Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			noFailures = 0;
		}
	}

	/**
	 * Sets the number of error test cases after first converting the string to
	 * an integer.
	 * 
	 * @param s
	 *            The number of error test cases as parsed string
	 */
	public void setNoErrors(String s) {
		try {
			noErrors = Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			noErrors = 0;
		}
	}

	/**
	 * Adds a test case to the list of test cases.
	 * 
	 * @param tc
	 *            The test case to add to the list of test cases.
	 */
	public void addTestCase(TestCase tc) {
		testCases.put(tc.getName(), tc);
	}

	/**
	 * Returns the number of test cases in a test module.
	 * 
	 * @return The number of test cases in a test module.
	 */
	public int getNoTestCases() {
		return noTestCases;
	}

	/**
	 * Returns the number of failed test cases in a test module.
	 * 
	 * @return The number of failed test cases in a test module.
	 */
	public int getNoFailures() {
		return noFailures;
	}

	/**
	 * Returns the number of error test cases in a test module.
	 * 
	 * @return The number of error test cases in a test module.
	 */
	public int getNoErrors() {
		return noErrors;
	}

	/**
	 * Returns the number of non passing test cases in a test module.
	 * 
	 * @return The number of non passing test cases in a test module.
	 */
	public int getNoNonPassing() {
		return noErrors+noFailures;
	}
	
	/**
	 * Returns the formatted date execution stamp.
	 * 
	 * @return The date of execution stamp.
	 */
	public String getExecutionDate() {
		return runDate;
	}

	/**
	 * Returns the collection of test cases.
	 * 
	 * @return The collection of test cases.
	 */
	public HashMap<String, TestCase> getTestCases() {
		return testCases;
	}

	/**
	 * Returns a test case from the test modules list of test cases based on a
	 * test case name.
	 * 
	 * @param name
	 *            The name of the test case to find.
	 * @return The test case in the collection that matches by name.
	 */
	public TestCase getTestCaseByName(String name) {
		return testCases.get(name);
	}

	/**
	 * Returns the number of passed test cases.
	 * 
	 * @return The number of passed test cases.
	 */
	public int getNoPassed() {
		return noPassed;
	}

	/**
	 * Returns the pass percentage as double.
	 * 
	 * @return The pass percentage as double.
	 */
	public double getPassPctDouble() {
		return (double) noPassed / (double) (noPassed + noFailures + noErrors)
                * 100.0;
	}
	
	@Override
	public String getPassPct() {
        return TestItemUtils.passPctToString(getPassPctDouble());
    }

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String ret = "\n--+-- Test module --+--\n";
		ret += name + "\n" + "Run date: " + runDate + "\n" + "Execution time: "
				+ executionTime + "\n" + "Test cases: " + noTestCases + "\n"
				+ "Errors: " + noErrors + "\n" + "Failures: " + noFailures
				+ "\n" + "Pass: " + getNoPassed();
		if (testCases.size() > 0) {
			ret += "\n";
		}

		Collection<TestCase> c = testCases.values();
		Iterator<TestCase> iter = c.iterator();
		while (iter.hasNext()) {
			TestCase tc = iter.next();
			ret += "Test case: " + tc.toString() + "\n";
		}
		return ret;
	}

	/**
	 * This method runs through all the test cases added to a test module. For
	 * each test case run through some statistics for this test module gets
	 * calculated.
	 */
	public void calcStats() {
	    if (!isIgnored) { 
	        noPassed = noTestCases - noFailures - noErrors;
	    }
	    
	    /*
		if (noPassed<=0 || 0==noTestCases) {
			noPassed = 0;
			passPct = 0.0;
		} else {
			passPct = ((double) noPassed / (double) noTestCases) * 100;
		}
	    */		
		if (runDate == null || runDate.isEmpty()) { 
			// FIXME, is this really the correct approach. How can
			// we properly handle the case where test runs are broken
			// and there are no time stamps in the results.
			runDate = UNKNOWN_TIME_STAMP;
		}
	}
	
	/**
	 * Sets the run index for this module.
	 * 
	 * @param idx The run index to set.
	 */
	public void setRunIdx(int idx) {
		runIdx = idx;
	}

	/**
	 * Returns the run index of this module.
	 * 
	 * @return The run index of this module.
	 */
	public int getRunIdx() {
		return runIdx;
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
	 * Sets the fact that the entire test class has been ignored using the @Ignore annotation.
	 */
	public void setAsIgnored() {
		isIgnored = true;
	}
	
	/**
	 * Returns the fact that the entire test class has been ignored using the @Ignore annotation.
	 * @return true/false
	 */
	public boolean getIsIgnored() {
		return isIgnored;
	}

	public int getNoIgnored() {
	    return noIgnored;
	}

    public void setNoIgnored(String s) {
        try {
            noIgnored = Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            noIgnored = 0;
        }
    }
}

/* eof */
