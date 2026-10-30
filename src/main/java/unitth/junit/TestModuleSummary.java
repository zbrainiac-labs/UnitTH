/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import unitth.core.TestItemUtils;
import unitth.core.UnitTHException;

/**
 * This class represents a summary for a <code>TestModule</code> objects
 * execution in all runs. The class contains a <code>HashMap</code> where the
 * test module execution spread is stored. The key is the run id integer. The
 * reasoning behind the set of summary objects are to avoid having to parse all
 * stored statistics in the RunHistory instance as soon as some statistics needs
 * to be retrieved.
 * 
 */
public class TestModuleSummary extends TestItemSummary {

	private TreeSet<String> noUniqueTestCases = null;
	private int noExecuted = 0;

	private HashMap<Integer, TestModule> spread = null;

	/* Needed to be able to determine the scale of the test case numbers graph. */
	private int largestNumberOfTestCases = 0;
	/* Needed to be able to determine the scale of the failure numbers graph. */
	private int largestNumberOfFailures = 0;
	/* Needed to be able to determine the scale of the failure numbers graph. */
	private int largestNumberOfErrors = 0;
	/* Needed to be able to determine the scale of the failure numbers graph. */
	private int largestNumberOfNonPass = 0;
	/* Needed to be able to determine the scale of the execution time graph. */
	private double largestExecutionTime = 0.0;

	/**
	 * CTOR, this constructor initializes all attributes based on the first test
	 * module added to this summary.
	 * 
	 * @param tm
	 *            The test module to add to the summary.
	 */
	public TestModuleSummary(TestModule tm) {
		name = tm.getName();
		if (!tm.getIsIgnored()) {
            noExecuted = tm.getNoTestCases();
        }
		noPassed = tm.getNoPassed();
		noFailed = tm.getNoFailures();
		noIgnored = tm.getNoIgnored();
		noErrors = tm.getNoErrors();
		
		sumExecutionTime = tm.getExecutionTimeDouble();
		
		largestNumberOfTestCases = noExecuted;
		largestNumberOfFailures = noFailed;
		largestNumberOfErrors = noErrors; 
		largestNumberOfNonPass = noFailed+noErrors;
		
		largestExecutionTime = tm.getExecutionTimeDouble();
		noUniqueTestCases = new TreeSet<String>();
		addTestCaseNames(tm);

		double passPctPlaceHolder = tm.getPassPctDouble();
		if (passPctPlaceHolder > bestRun) {
			bestRun = passPctPlaceHolder;
		}
		if (passPctPlaceHolder < worstRun) {
			worstRun = passPctPlaceHolder;
		}

		spread = new HashMap<Integer, TestModule>();
		spread.put(tm.getRunIdx(), tm);
	}

	/**
	 * This method increments all the statistics in this summary for each new
	 * test module that gets added.
	 * 
	 * @param tm
	 *            The test module who's information to add to the summary.
	 * @throws UnitTHException
	 */
	public void increment(TestModule tm) throws UnitTHException {
		if (name.equals(tm.getName())) {
			noRuns++;
			if (!tm.getIsIgnored()) {
			    noExecuted += tm.getNoTestCases();
			}
			noPassed += tm.getNoPassed();
			noFailed += tm.getNoFailures();
			noIgnored += tm.getNoIgnored();
			noErrors += tm.getNoErrors();
			sumExecutionTime += tm.getExecutionTimeDouble();
			addTestCaseNames(tm);

			// Largest number of test cases
			if (tm.getNoTestCases() > largestNumberOfTestCases) {
				largestNumberOfTestCases = tm.getNoTestCases();
			}
			
			// Largest number of failed test cases
			int noFailuresHolder = tm.getNoFailures(); 
			if (noFailuresHolder > largestNumberOfFailures) {
				largestNumberOfFailures = noFailuresHolder;
			}

			// Largest number of error test cases
			int noErrorsHolder = tm.getNoErrors(); 
			if (noErrorsHolder > largestNumberOfErrors) {
				largestNumberOfErrors = noErrorsHolder;
			}

			// Needed for the calculation of the negative graph. 
			if (noFailuresHolder+noErrorsHolder > largestNumberOfNonPass) {
				largestNumberOfNonPass = noFailuresHolder+noErrorsHolder; 
			}
			
			// Largest execution time
			if (tm.getExecutionTimeDouble() > largestExecutionTime) {
				largestExecutionTime = tm.getExecutionTimeDouble();
			}

			double passPctPlaceHolder = tm.getPassPctDouble();
			if (passPctPlaceHolder > bestRun) {
				bestRun = passPctPlaceHolder;
			}
			if (passPctPlaceHolder < worstRun) {
				worstRun = passPctPlaceHolder;
			}
			spread.put(tm.getRunIdx(), tm);
		} else {
			throw new UnitTHException(
					"This module has a different class name than expected!");
		}
	}

	/*
	 * Adds unique test case names to a list.
	 * 
	 * @param tm The test module who's test case names to add to the unique test
	 * case names list.
	 */
	private void addTestCaseNames(TestModule tm) {
		Collection<TestCase> c = tm.getTestCases().values();
		Iterator<TestCase> iter = c.iterator();
		while (iter.hasNext()) {
			TestCase tc = iter.next();
			noUniqueTestCases.add(tc.getName());
		}
	}

	/**
	 * @return Returns the number of executed test cases.
	 */
	public int getNoTestCases() {
		return noExecuted;
	}

	/**
	 * @return Returns the number of unique test cases executed in the module.
	 */
	public int getNoUniqueTestCases() {
		return noUniqueTestCases.size();
	}

	/**
	 * Returns the largest number of test cases in any test modules.
	 * 
	 * @return The largest number of test cases in any test modules.
	 */
	public int getLargestNumberOfTcs() {
		return largestNumberOfTestCases;
	}

	/**
	 * Returns the largest number of failures in any test modules.
	 * 
	 * @return The largest number of failures in any test modules.
	 */
	public int getLargestNumberOfFailures() {
		return largestNumberOfFailures;
	}

	/**
	 * Returns the largest number of errors in any test modules.
	 * 
	 * @return The largest number of errors in any test modules.
	 */
	public int getLargestNumberOfErrors() {
		return largestNumberOfErrors;
	}
	
	/**
	 * Returns the largest number of errors in any test modules.
	 * 
	 * @return The largest number of errors in any test modules.
	 */
	public int getLargestNumberOfNonPass() {
		return largestNumberOfNonPass;
	}
	
	/**
	 * Returns the largest execution time in all test runs.
	 * 
	 * @return Returns the largest execution time in all test runs.
	 */
	public double getLargestExecutionTime() {
		return largestExecutionTime;
	}

	/*
	 * Returns the diff in pass rate between the last module run and the last
	 * executed module run before that.
	 * 
	 * @param runs The number of runs in the history since we need an offset to
	 * count from.
	 * 
	 * @param idx The index of the run to match with.
	 * 
	 * @return The pass rate difference as string.
	 */
	protected String getPrDiff(int runs, int idx) {
		TestModule tmLast = spread.get(runs);
		TestModule tmFirst = null;

		// Do not return any diff if this module was not part of the last run.
		if (null == tmLast) {
			return "NA";
		}

		// Loop from the start until a run is found and then increase
		// the counter
		int ctr = 0;
		for (int i=1; ctr<=spread.size(); i++) {
			if ((runs-i)<0) {
				break;
			}
			tmFirst = spread.get(runs - i);
			if (null != tmFirst) {
				ctr++;
				if (ctr==idx) {
					break;
				}
			}
		}
		if (null == tmFirst) {
			return "NA";
		}
		
		double diff = Double.parseDouble(tmLast.getPassPct())
				- Double.parseDouble(tmFirst.getPassPct());
		String ret = TestItemUtils.passPctToString(diff) + "%";
		if (0 < diff) {
			ret = "+" + ret;
		}
		return ret;
	}

	/*
	 * This method calculates from which test run the trend shall be counted
	 * for. The calculation is based on a time stamp taken at the point of
	 * report generation.
	 * 
	 * @param trendInterval The interval in milliseconds. The interval is the
	 * point in time from where to calculate the trend.
	 * 
	 * @return The test run to calculate the trend from.
	 */
	private TestModule getTrendTestRun(long trendInterval) {
		long currentTime = System.currentTimeMillis();
		long breakPoint = currentTime - trendInterval;

		// It might be necessary to run through all runs in case there might be
		// an
		// occasion where the modules are not sorted.

		for (int i = totalNumberOfTestRuns - 1; i > 0; i--) {
			TestModule tm = spread.get(i);

			if (tm != null) {
				long runTimeStamp = tm.getRunDateAsLong();

				if (breakPoint > runTimeStamp) {
					return tm;
				}
			}
		}
		return null;
	}

	/*
	 * This method calculates the pass rate trend based on a time stamp taken at
	 * the point of report generation.
	 * 
	 * @param trendInterval The interval in milliseconds. The interval is the
	 * point in time from where to calculate the trend.
	 * 
	 * @return The change in pass rate.
	 */
	public String getPrTrend(long trendInterval) {
		TestModule matchingTm = getTrendTestRun(trendInterval);

		if (null != matchingTm) {
			double diff = Double.parseDouble(spread.get(totalNumberOfTestRuns)
					.getPassPct())
					- Double.parseDouble(matchingTm.getPassPct());

			String ret = TestItemUtils.passPctToString(diff) + "%";
			if (0 < diff) {
				ret = "+" + ret;
			}
			return ret;
		} else {
			return "NA";
		}
	}

	/*
	 * This method calculates the test case trend based on a time stamp taken at
	 * the point of report generation.
	 * 
	 * @param trendInterval The interval in milliseconds. The interval is the
	 * point in time from where to calculate the trend.
	 * 
	 * @return The change in number of test cases.
	 */
	public String getTcTrend(long trendInterval) {
		TestModule matchingTm = getTrendTestRun(trendInterval);
		if (null != matchingTm) {
			TestModule tmAtEnd = spread.get(totalNumberOfTestRuns);
			if (null != tmAtEnd) {
				int diff = spread.get(totalNumberOfTestRuns).getNoTestCases()
						- matchingTm.getNoTestCases();

				String ret = Integer.toString(diff);
				if (0 < diff) {
					ret = "+" + ret;
				}
				return ret;
			}
		}
		return "NA";
	}

	/*
	 * This method calculates the test case trend based on a time stamp taken at
	 * the point of report generation.
	 * 
	 * @param trendInterval The interval in milliseconds. The interval is the
	 * point in time from where to calculate the trend.
	 * 
	 * @return The change in number of test cases.
	 */
	public String getFnTrend(long trendInterval) {
		TestModule matchingTm = getTrendTestRun(trendInterval);
		if (null != matchingTm) {
			TestModule tmAtEnd = spread.get(totalNumberOfTestRuns);
			if (null != tmAtEnd) {
				int diff = spread.get(totalNumberOfTestRuns).getNoNonPassing()
						- matchingTm.getNoNonPassing();

				String ret = Integer.toString(diff);
				if (0 < diff) {
					ret = "+" + ret;
				}
				return ret;
			}
		}
		return "NA";
	}
}

/* eof */
