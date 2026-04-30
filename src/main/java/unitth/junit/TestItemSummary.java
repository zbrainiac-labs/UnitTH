/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

import unitth.core.TestItemUtils;

/**
 * This class is the super class for the container classes <code>
 * TestModuleSummary and TestCaseSummary</code>
 * . It keeps statistics regarding multiple executions of a run to avoid having
 * to go through all the stored statistics of the parsed test runs.
 * 
 */
// TODO, VX.X Find a better solution than using an abstract class here since all
// classes inheriting from TestItemSummary does not need all methods.
public abstract class TestItemSummary {

	protected int noPassed = 0;
	protected int noFailed = 0;
	protected int noErrors = 0;
	protected int noIgnored = 0;
	protected int noRuns = 1;
	protected String name = "UNDEF";
	protected double sumExecutionTime = 0.0;
	protected double sumExecutionTimePassed = 0.0;
	protected int totalNumberOfTestRuns = 0;
	protected double worstRun = 100.0;
	protected double bestRun = 0.0;

	/**
	 * CTOR
	 */
	public TestItemSummary() {
	}

	/**
	 * Returns the number of runs a test module or a test case have been
	 * executed.
	 * 
	 * @return The number of runs a test module or a test case have been
	 *         executed.
	 */
	public int getNoRuns() {
		return noRuns;
	}

	/**
	 * Returns the name of the test item in this specific summary instance. s
	 * 
	 * @return The name of the test module or test case stored in this instance.
	 */
	public String getName() {
		if (name == null) {
			return "module-not-defined";
		}

		return name;
	}

	/**
	 * Returns the number of passed test cases in a test module or in the case
	 * of a <code>TestCaseSummary</code>the number of times a specific test case
	 * have passed.
	 * 
	 * @return The number of passed test cases.
	 */
	public int getNoPassed() {
		return noPassed;
	}

	/**
	 * Returns the number of failed test cases in a test module or in the case
	 * of a <code>TestCaseSummary</code>the number of times a specific test case
	 * have failed.
	 * 
	 * @return The number of failed test cases.
	 */
	public int getNoFailures() {
		return noFailed;
	}

	/**
	 * Returns the number of error test cases in a test module or in the case of
	 * a <code>TestCaseSummary</code>the number of times a specific test case
	 * have ended up with an error verdict.
	 * 
	 * @return The number of error test cases.
	 */
	public int getNoErrors() {
		return noErrors;
	}
	
	public int getNoIgnored() {
	    return noIgnored;
	}

	/**
	 * Returns the stringified execution time. 3 decimals.
	 * 
	 * @return The stringified execution time. 3 decimals.
	 */
	public String getExecutionTime() {
		return String.format("%1.3f", sumExecutionTime / (double) noRuns);
	}

	/**
	 * Returns the execution time as double.
	 * 
	 * @return The execution time as double.
	 */
	public double getExecutionTimeDouble() {
		return sumExecutionTime / (double) noRuns;
	}

	/**
	 * Returns the pass percentage as string.
	 * 
	 * @return The pass percentage as string.
	 */
	public String getPassPct() {
		return TestItemUtils.passPctToString(getPassPctDouble());
	}

	/**
	 * Returns the pass percentage as double.
	 * 
	 * @return The pass percentage as double.
	 */
	public double getPassPctDouble() {
		return (double) noPassed / (double) (noPassed + noFailed + noErrors)
				* 100.0;
	}

	/**
	 * Returns the sum of execution time for all passes test cases.
	 * 
	 * @return The sum of execution time for all passes test cases.
	 */
	public double getExecutionTimePassedDouble() {
		return sumExecutionTimePassed;
	}

	/**
	 * Sets the total number of test runs that this summary object has been
	 * executed.
	 * 
	 * @param total
	 *            The number of runs to set.
	 */
	public void setTotalNumberOfTestRuns(int total) {
		this.totalNumberOfTestRuns = total;
	}

	/**
	 * Returns the total number of times this test object has been executed.
	 * 
	 * @return The number of executions for this test summary.
	 */
	public int getTotalNumberOfTestRuns() {
		return this.totalNumberOfTestRuns;
	}

	public abstract double getLargestExecutionTime();

	public abstract int getLargestNumberOfTcs();

	/**
	 * This method returns the package name as ripped from the class name.
	 * 
	 * @return The stripped package name.
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
	 * Returns the pass rate for the best run.
	 * 
	 * @return The pass rate of the best run.
	 */
	public String getBestRun() {
		return String.format("%1.2f", bestRun);
	}

	/**
	 * Returns the pass rate for the worst run.
	 * 
	 * @return The pass rate of the worst run.
	 */
	public String getWorstRun() {
		return String.format("%1.2f", worstRun);
	}

	/**
	 * This method returns the test case trend for the last day.
	 * 
	 * @return The change in number of test cases.
	 */
	public String getTcTrendLastDay() {
		return getTcTrend(1 * 24 * 60 * 60 * 1000);
	}

	/**
	 * This method returns the test case trend for the last 3 days.
	 * 
	 * @return The change in number of test cases.
	 */
	public String getTcTrendLast3Days() {
		return getTcTrend(3 * 24 * 60 * 60 * 1000);
	}

	/**
	 * This method returns the test case trend for the last 7 days.
	 * 
	 * @return The change in number of test cases.
	 */
	public String getTcTrendLast7Days() {
		return getTcTrend(7 * 24 * 60 * 60 * 1000);
	}

	/**
	 * This method returns the failure trend for the last day.
	 * 
	 * @return The change in number of failure.
	 */
	public String getFnTrendLastDay() {
		return getFnTrend(1 * 24 * 60 * 60 * 1000);
	}

	/**
	 * This method returns the failure trend for the last 3 days.
	 * 
	 * @return The change in number of failures.
	 */
	public String getFnTrendLast3Days() {
		return getFnTrend(3 * 24 * 60 * 60 * 1000);
	}

	/**
	 * This method returns the failure trend for the last 7 days.
	 * 
	 * @return The change in number of failures.
	 */
	public String getFnTrendLast7Days() {
		return getFnTrend(7 * 24 * 60 * 60 * 1000);
	}

	/**
	 * This method returns the pass rate trend for the last 3 days.
	 * 
	 * @return The pass rate trend as a formatted String.
	 */
	public String getPrTrendLast3Days() {
		return getPrTrend(3 * 24 * 60 * 60 * 1000);
	}

	/**
	 * This method returns the pass rate trend for the last 7 days.
	 * 
	 * @return The pass rate trend as a formatted String.
	 */
	public String getPrTrendLast7Days() {
		return getPrTrend(7 * 24 * 60 * 60 * 1000);
	}

	/**
	 * Returns the pass rate difference between the last run and second last
	 * run.
	 * 
	 * @return Returns the pass rate difference.
	 */
	public String getPrTrendLastRun() {
		if (totalNumberOfTestRuns < 2) {
			return "NA";
		} else {
			return getPrDiff(totalNumberOfTestRuns, 1);
		}
	}

	/**
	 * Returns the pass rate difference between the last run and five last runs.
	 * 
	 * @return Returns the pass rate difference.
	 */
	public String getPrTrendLast5Runs() {
		if (totalNumberOfTestRuns < 5) {
			return "NA";
		} else {
			return getPrDiff(totalNumberOfTestRuns, 4);
		}
	}

	/**
	 * Returns the pass rate difference between the last run and 10 last runs.
	 * run.
	 * 
	 * @return Returns the pass rate difference.
	 */
	public String getPrTrendLast10Runs() {
		if (totalNumberOfTestRuns < 10) {
			return "NA";
		} else {
			return getPrDiff(totalNumberOfTestRuns, 9);// FIXME, bug here, gets stucked in a loop.
		}
	}

	// Good solution?
	protected abstract String getFnTrend(long trendInterval);

	protected abstract String getTcTrend(long trendInterval);

	protected abstract String getPrTrend(long trendInterval);

	protected abstract String getPrDiff(int runs, int idx);
}

/* eof */
