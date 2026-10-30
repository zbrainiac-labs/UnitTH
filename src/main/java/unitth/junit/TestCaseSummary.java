/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.util.HashMap;

import unitth.core.UnitTHException;

/**
 * This class represents a summary for a <code>TestCase</code> objects execution
 * in all runs. The class contains a <code>HashMap</code> where the test case
 * execution spread is stored. The key is the run id integer.
 * 
 */
public class TestCaseSummary extends TestItemSummary {

	private String storedModuleName = "UNDEF";
	private String className = "UNDEF";
	private String tcName = "UNDEF";

	/**
	 * Container for the test case spread. The key is the test run id and the
	 * verdict can be any of the following.
	 * 
	 * <code>TestCaseVerdict.e_PASS</code> <code>TestCaseVerdict.e_FAIL</code>
	 * <code>TestCaseVerdict.e_ERROR</code> <code>TestCaseVerdict.e_NORUN</code>
	 */
	private HashMap<Integer, TestCaseVerdict> spread = null;

    /**
     * Ctor, takes test case as inparameter and starts calculating stats for the
     * unique test case.
     * 
     * @param tc
     *            The test case to add to the test case summary
     * @param idx
     *            The index of the run where the added test case instance was
     *            run
     */
    public TestCaseSummary(TestCase tc, int idx) {
	name = tc.getFullName();
	tcName = tc.getName();
	className = tc.getClassName();
	storedModuleName = tc.getModuleName();
	noRuns = 1;
	if (tc.getVerdict() == TestCaseVerdict.e_PASS) {
	    noPassed = 1;
	} else if (tc.getVerdict() == TestCaseVerdict.e_FAIL) {
	    noFailed = 1;
	} else if (tc.getVerdict() == TestCaseVerdict.e_ERROR) {
	    noErrors = 1;
	} else if (tc.getVerdict() == TestCaseVerdict.e_IGNORED) {
	    noIgnored = 1;
	}
	sumExecutionTime = tc.getExecutionTimeDouble();
	spread = new HashMap<Integer, TestCaseVerdict>();
	addRunIdx(idx, tc.getVerdict());
    }

	/**
	 * Increments all the statistics for this unique test case.
	 * 
	 * @param tc
	 *            The test case to add to the test case summary
	 * @param idx
	 *            The index of the run where the added test case instance was
	 *            run
	 * @throws UnitTHException
	 */
    public void increment(TestCase tc, int idx) throws UnitTHException {
	if (name.equals(tc.getFullName())) {
	    noRuns++;
	    if (tc.getVerdict() == TestCaseVerdict.e_PASS) {
		noPassed++;
	    } else if (tc.getVerdict() == TestCaseVerdict.e_FAIL) {
		noFailed++;
	    } else if (tc.getVerdict() == TestCaseVerdict.e_ERROR) {
		noErrors++;
	    } else if (tc.getVerdict() == TestCaseVerdict.e_IGNORED) {
		noIgnored++;
	    }

	    sumExecutionTime += tc.getExecutionTimeDouble();
	    addRunIdx(idx, tc.getVerdict());
	} else {
	    throw new UnitTHException(
		    "This test case has a different class name than expected!");
	}
    }

	/**
	 * This method adds a verdict to specific index in the spread collection.
	 * 
	 * @param idx
	 *            The index where to set the verdict.
	 * @param verdict
	 *            The verdict to set.
	 */
	private void addRunIdx(int idx, TestCaseVerdict verdict) {
		spread.put(idx, verdict);
	}

	/**
	 * Returns the test class name.
	 * 
	 * @return The test class name.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns the test case name.
	 * 
	 * @return The test case name.
	 */
	public String getTestCaseName() {
		return tcName;
	}

	/**
	 * Returns the module tied to this test case summary.
	 * 
	 * @return The module name.
	 */
	public String getModuleName() {
		return storedModuleName;
	}

	/**
	 * Returns the <code>HashMap</code> object representing the test case
	 * verdict spread.
	 * 
	 * @return The test case verdict spread.
	 */
	public HashMap<Integer, TestCaseVerdict> getSpread() {
		return spread;
	}

	/**
	 * Returns the test case verdict at a specific index in the spread.
	 * 
	 * @param idx
	 *            The index where to get the test case verdict.
	 * @return The looked for test case verdict or null.
	 */
	public TestCaseVerdict getSpreadAt(int idx) {
		return spread.get(idx);
	}

	/**
	 * This method returns the package name as ripped from the class name.
	 * 
	 * @return The stripped package name.
	 */
	public String getPackageName() {
		if (name == null) {
			return "package-not-defined";
		}

		int liof = className.lastIndexOf(".");
		if (-1 == liof) {
			return "not-in-package";
		}
		return className.substring(0, liof);
	}

	//
	// TODO, VX.XNever used. Find a better for inheritance here.
	//
	public String getFnTrend(long trendInterval) {
		return "";
	}

	public String getTcTrend(long trendInterval) {
		return "";
	}

	public String getPrTrend(long trendInterval) {
		return "";
	}

	protected String getPrDiff(int runs, int idx) {
		return "";
	}

	public double getLargestExecutionTime() {
		return 69.69;
	}

	public int getLargestNumberOfTcs() {
		return 69;
	}

	public String getClassPlusTestCaseName(String packageName) {
		// Ripping everything but the last part of the
		String strippedClassName = "UNDEF";
		if (this.className.startsWith(packageName) && !packageName.equals("")
				&& !packageName.equals("UNDEF")) {
			if (className.equalsIgnoreCase(packageName)) {
				return this.tcName;
			} else {
				strippedClassName = this.className.replaceFirst(packageName
						+ ".", "");
				return strippedClassName + "." + this.tcName;
			}
		} else {
			return this.tcName;
		}
	}
}

/* eof */
