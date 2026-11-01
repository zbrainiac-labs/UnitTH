/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

import java.util.Locale;

import unitth.core.TestItemUtils;

/**
 * Super class of the summaries kept over all parsed runs, so statistics do not
 * have to be recalculated from the stored runs on every access.
 */
public abstract class TestItemSummary {

	protected int noPassed = 0;
	protected int noFailed = 0;
	protected int noErrors = 0;
	protected int noIgnored = 0;
	protected int noRuns = 1;
	protected String name = "UNDEF";
	protected double sumExecutionTime = 0.0;

	/**
	 * Returns the number of runs a test module or a test case have been
	 * executed.
	 * 
	 * @return The number of runs.
	 */
	public int getNoRuns() {
		return noRuns;
	}

	/**
	 * Returns the name of the test item in this summary.
	 * 
	 * @return The name of the summarised item.
	 */
	public String getName() {
		if (name == null) {
			return "module-not-defined";
		}
		return name;
	}

	public int getNoPassed() {
		return noPassed;
	}

	public int getNoFailures() {
		return noFailed;
	}

	public int getNoErrors() {
		return noErrors;
	}

	public int getNoIgnored() {
		return noIgnored;
	}

	/**
	 * Returns the average execution time per run, 3 decimals.
	 * 
	 * @return The stringified execution time.
	 */
	public String getExecutionTime() {
		return String.format(Locale.US, "%1.3f", sumExecutionTime / noRuns);
	}

	public double getExecutionTimeDouble() {
		return sumExecutionTime / noRuns;
	}

	public String getPassPct() {
		return TestItemUtils.passPctToString(getPassPctDouble());
	}

	public double getPassPctDouble() {
		return (double) noPassed / (double) (noPassed + noFailed + noErrors) * 100.0;
	}

	/**
	 * This method returns the package name as ripped from the name.
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
}

/* eof */
