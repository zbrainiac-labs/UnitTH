/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import unitth.core.CustomStringLengthComparator;
import unitth.core.RunHistory;
import unitth.core.TestItemUtils;
import unitth.core.UnitTHConfig;
import unitth.core.UnitTHException;

/**
 * This class is main place holder for all the statistics that have been read
 * from the parsed runs.
 * <p>
 * It contains a list for <code>TestRun</code> objects, <code>TestCase</code>
 * and <code>TestModule</code> summaries.
 * <p>
 * The class also has a set of counters for test history statistics.
 * 
 */
public class TestHistory extends RunHistory implements TrendSource {

	private TreeSet<TestRun> history = null;
	private TreeSet<TestRun> sortedHistory = null;
	private int noUniqueTestCases = 0;
	private int noIgnoredTestCases = 0;
	private double worstRun = 100.0;
	private double bestRun = 0.0;
	private double averagePassRate = 0.0;
	private int largestNumberOfTcs = 0;
	private int largestNumberOfFailures = 0; // In a test run
	private int largestNumberOfErrors = 0; // In a test run
	private int largestNumberOfNonPass = 0; // In a test run
	private double largestExecutionTime = 0.0;

	/*
	 * This <code>TreeMap</code> stores a number of summaries for all the test
	 * modules found in the JUnit reports. These summaries are created while
	 * running through all the save statistics. The reason for using a summary
	 * object is to avoid having to parse all the stored statistics every time
	 * some statistics regarding a module needs to be fetched.
	 */
	private TreeMap<String, TestModuleSummary> testModuleSummaries = new TreeMap<String, TestModuleSummary>();

	/*
	 * This <code>TreeMap</code> stores a number of summaries for all the test
	 * modules found in the JUnit reports. These summaries are created while
	 * running through all the saved statistics. The reason for using a summary
	 * object is to avoid having to parse all the stored statistics every time
	 * some statistics regarding a module needs to be fetched.
	 */
	private TreeMap<String, TestCaseSummary> testCaseSummaries = new TreeMap<String, TestCaseSummary>();

	/*
	 * This <code>TreeMap</code> stores a number of summaries for all the test
	 * packages found in the JUnit reports. These summaries are created while
	 * running through all the saved statistics. The reason for using a summary
	 * object is to avoid having to parse all the stored statistics every time
	 * some statistics regarding a module needs to be fetched.
	 */
	private TreeMap<String, TestPackageSummary> testPackageSummaries = new TreeMap<String, TestPackageSummary>();

	/*
	 * This is the attribute that stores the time stamp for the last parsed
	 * module.
	 */
	private String lastRunStr = "0";

	/*
	 * Simple counters for some of the test history statistics.
	 */
	private int noExecutedTestCases = 0;
	private int noPassedTestCases = 0;
	private int noFailedTestCases = 0;
	private int noErrorTestCases = 0;
	private double execTimeSum = 0.0;

	/**
	 * Constructor that creates the empty history.
	 */
	public TestHistory(UnitTHConfig config) {
		this.config = config;
		history = new TreeSet<TestRun>();
	}

	private final UnitTHConfig config;

	public UnitTHConfig getConfig() {
		return config;
	}

	/**
	 * Adds a parsed test run to the history
	 * 
	 * @param run
	 *            The test run to add.
	 * @throws UnitTHException
	 */
	public void addTestRun(TestRun run) throws UnitTHException {
		if (null != history) {
			history.add(run);
		} else {
			throw new UnitTHException("The history TreeSet was null!");
		}
	}

	/**
	 * Returns the complete history.
	 * 
	 * @return The complete history.
	 */
	public TreeSet<TestRun> getRuns() {
		return history;
	}

	/**
	 * Returns the <code>TestCaseSummary</code> collection.
	 * 
	 * @return The <code>TreeMap<String, TestCaseSummary</code> collection.
	 */
	public TreeMap<String, TestCaseSummary> getTestCaseSummaries() {
		return testCaseSummaries;
	}

	/**
	 * @param moduleName
	 *            The name of the module which is used as a key in the list of
	 *            test case summaries.
	 * @return The looked for test case summary or NULL.
	 */
	public TestCaseSummary getTestCaseSummary(String testCaseName) {
		return testCaseSummaries.get(testCaseName);
	}

	/**
	 * This method returns all test module summaries.
	 * 
	 * @return All test module summaries.
	 */
	public TreeMap<String, TestModuleSummary> getTestModuleSummaries() {
		return testModuleSummaries;
	}

	/**
	 * Sets a time stamp for the lastly executed run.
	 * 
	 * @param s
	 *            The stringified time stamp of the run that was executed last.
	 */
	public void setLastRunStr(String s) {
		lastRunStr = s;
	}

	/**
	 * @param moduleName
	 *            The name/key of the module to get from the collection of test
	 *            module summaries.
	 * @return The looked for test module summary
	 */
	public TestModuleSummary getTestModuleSummary(String moduleName) {
		return testModuleSummaries.get(moduleName);
	}

	public TestPackageSummary getTestPackageSummary(String packageName) {
		return testPackageSummaries.get(packageName);
	}

	public TreeMap<String, TestPackageSummary> getTestPackageSummaries() {
		return testPackageSummaries;
	}

	/**
	 * Returns the time stamp of the last executed run.
	 * 
	 * @return The time stamp of the last executed run.
	 */
	public String getLastRun() {
		return lastRunStr;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String buf = "";
		for (TestRun run : history) {
			buf += run.toString() + "\n";
		}
		return buf;
	}

	/**
	 * Returns all keys in the test module summary collection.
	 * 
	 * @return An array of all the keys in the test module summary collection.
	 */
	public Object[] getUniqueModules() {
		return testModuleSummaries.keySet().toArray();
	}

	/**
	 * Returns all keys in the test module summary collection.
	 * 
	 * @return An array of all the keys in the test module summary collection.
	 */
	public Object[] getUniquePackages() {
		return testPackageSummaries.keySet().toArray();
	}

	/**
	 * Returns all keys in the test case summary collection.
	 * 
	 * @return An array of all the keys in the test case summary collection.
	 */
	public String[] getUniqueTestCases() {
		return testCaseSummaries.keySet().toArray(new String[0]);
	}

	/**
	 * Returns the number of parsed test runs.
	 * 
	 * @return Returns the number of parsed test runs.
	 */
	public int getNoRuns() {
		return history.size();
	}

	/**
	 * Returns the number of unique test modules.
	 * 
	 * @return Returns the number of unique test modules.
	 */
	public int getNoUniqueModules() {
		return testModuleSummaries.size();
	}

	/**
	 * Returns the number of unique test modules.
	 * 
	 * @return Returns the number of unique test modules.
	 */
	public int getNoPackages() {
		return testPackageSummaries.size();
	}

	/**
	 * Returns the number of unique test cases.
	 * 
	 * @return Returns the number of unique test cases.
	 */
	public int getNoUniqueTestCases() {
		return noUniqueTestCases;
	}

	/**
	 * Returns the number of ignored test cases.
	 * 
	 * @return Returns the number of ignored test cases.
	 */
	public int getNoIgnoredTestCases() {
		return noIgnoredTestCases;
	}
	
	/**
	 * Returns the pass rate of the best run.
	 * 
	 * @return The pass rate of the best run.
	 */
	public String getBestRun() {
		return TestItemUtils.passPctToString(bestRun);
	}

	/**
	 * Returns the pass rate of the worst run.
	 * 
	 * @return The pass rate of the worst run.
	 */
	public String getWorstRun() {
		return TestItemUtils.passPctToString(worstRun);
	}

	/**
	 * Returns the average pass rate.
	 * 
	 * @return The average pass rate.
	 */
	public String getAvePassPct() {
		return TestItemUtils.passPctToString(averagePassRate);
	}

	/**
	 * Returns the largest number of test cases in any run.
	 * 
	 * @return The largest number of test cases in any run.
	 */
	public int getLargestNumberOfTcs() {
		return largestNumberOfTcs;
	}

	/**
	 * Returns the largest number of failures in any run.
	 * 
	 * @return The largest number of failures in any run.
	 */
	public int getLargestNumberOfNonPass() {
		return largestNumberOfNonPass;
	}

	/**
	 * Returns the largest number of failures in any run.
	 * 
	 * @return The largest number of failures in any run.
	 */
	public int getLargestNumberOfFailures() {
		return largestNumberOfFailures;
	}

	/**
	 * Returns the largest number of failures in any run.
	 * 
	 * @return The largest number of failures in any run.
	 */
	public int getLargestNumberOfErrors() {
		return largestNumberOfErrors;
	}

	/**
	 * This method runs through all stored test runs and generates all the
	 * statistics down to the lowest level through calls to the equivalent
	 * method in the <code>TestRun</code> objects.
	 */
	public void calcStats() {

		// First count the number of unique test cases
		// they have been executed.
		double sumOfPassRates = 0.0;
		sortedHistory = new TreeSet<TestRun>();

		for (TestRun tr : history) {
			tr.calcStats();

			double passRateHolder = tr.getPassPctDouble();

			// Get worst and best pass rates
			if (passRateHolder > bestRun) {
				bestRun = passRateHolder;
			}
			if (passRateHolder < worstRun) {
				worstRun = passRateHolder;
			}
			sumOfPassRates += passRateHolder;

			// Largest number of test cases
			int noTestCasesHolder = tr.getNoTestCases();
			if (noTestCasesHolder > largestNumberOfTcs) {
				largestNumberOfTcs = noTestCasesHolder;
			}

			int noFailuresHolder = tr.getNoFailures();
			if (noFailuresHolder > largestNumberOfFailures) {
				largestNumberOfFailures = noFailuresHolder;
			}

			int noErrorsHolder = tr.getNoErrors();
			if (noErrorsHolder > largestNumberOfErrors) {
				largestNumberOfErrors = noErrorsHolder;
			}

			// Needed for the calculation of the negative graph.
			if (noFailuresHolder + noErrorsHolder > largestNumberOfNonPass) {
				largestNumberOfNonPass = noFailuresHolder + noErrorsHolder;
			}

			// Get the last run date, the formats are in YYYY-MM-DD
			String timeStampHolder = tr.getExecutionDate();
			if (timeStampHolder==null) {
				System.err
				.println("The test run execution date is 'null', this can be an indication that there were no results to be parsed for this run.");
				continue;
			}
			if (0 > lastRunStr.compareToIgnoreCase(timeStampHolder)) {
				setLastRunStr(timeStampHolder);
			}

			// Overall summary
			noExecutedTestCases += tr.getNoTestCases();
			noPassedTestCases += tr.getNoPassed();
			noFailedTestCases += tr.getNoFailures();
			noErrorTestCases += tr.getNoErrors();
			noIgnoredTestCases += tr.getNoIgnored();
			execTimeSum += tr.getExecutionTimeDouble();
			sortedHistory.add(tr);
		}

		// Second loop starts here. This time we'll put the test runs
		// back into a new collection sorted by run date. This time
		// also setting the run index for all modules in the run.
		int index = sortedHistory.size();
		for (TestRun tr : sortedHistory) {

			tr.setRunIdx(index);

			//
			// PACKAGES
			//
			Collection<TestPackage> c0 = tr.getTestPackages().values();
			Iterator<TestPackage> iter0 = c0.iterator();

			while (iter0.hasNext()) {

				TestPackage tp = iter0.next();
				tp.setRunIdx(index);

				try {
					if (testPackageSummaries.containsKey(tp.getName())) {
						testPackageSummaries.get(tp.getName()).increment(tp);
					} else {
						testPackageSummaries.put(tp.getName(),
								new TestPackageSummary(tp));
					}
					// We need to set the number of total test runs here since
					// it is needed when
					// calculating test module and test case trends.
					if (history.size() == index) {
						testPackageSummaries.get(tp.getName())
								.setTotalNumberOfTestRuns(index);
					}
				} catch (UnitTHException jthe) {
					System.err
							.print("Trying to add, package stats to a list created for another package name. ");
					System.err.println(jthe);
				}

				//
				// MODULES
				//
				Collection<TestModule> c1 = tp.getTestModules().values();
				Iterator<TestModule> iter1 = c1.iterator();
				while (iter1.hasNext()) {

					TestModule tm = iter1.next();

					// Before we are adding the test module to the test summary
					tm.setRunIdx(index);

					try {
						if (testModuleSummaries.containsKey(tm.getName())) {
							testModuleSummaries.get(tm.getName()).increment(tm);
						} else {
							testModuleSummaries.put(tm.getName(),
									new TestModuleSummary(tm));
						}
						// We need to set the number of total test runs here
						// since it is needed when
						// calculating test module and test case trends.
						if (history.size() == index) {
							testModuleSummaries.get(tm.getName())
									.setTotalNumberOfTestRuns(index);
						}
					} catch (UnitTHException jthe) {
						System.err
								.print("Trying to add, module stats to a list created for another module name. ");
						System.err.println(jthe);
					}

					//
					// TEST CASES
					//
					Collection<TestCase> c2 = tm.getTestCases().values();
					Iterator<TestCase> iter2 = c2.iterator();
					while (iter2.hasNext()) {
						TestCase tc = iter2.next();
						try {
							if (testCaseSummaries.containsKey(tc.getFullName())) {
								testCaseSummaries.get(tc.getFullName())
										.increment(tc, tr.getRunIdx());
							} else {
								TestCaseSummary tcs = new TestCaseSummary(tc,
										tr.getRunIdx());
								testCaseSummaries.put(tc.getFullName(), tcs);
							}
						} catch (UnitTHException jthe) {
							System.err
									.print("Trying to add, test case stats to a list created for another test case name. ");
							System.err.println(jthe);
						}
					}
				}
			}

			// Largest execution time, we need to get the full test run
			// execution time.
			double largestExecutionTimeHolder = tr.getExecutionTimeDouble();
			if (largestExecutionTimeHolder > largestExecutionTime) {
				largestExecutionTime = largestExecutionTimeHolder;
			}

			// Create the relative part for this test run here to avoid
			// having to generate it in all places where it is used. But only
			// if the flag use absolute paths are set to false which is the
			// default.
			if (!config.useAbsPaths()) {
				tr.setRelativePathFromOutputDir(config.rootFolder(),
					config.reportPath());
			} else {
				tr.setAbsolutePath(config.rootFolder(), config.reportPath());
			}
			index--;
		}
		noUniqueTestCases = testCaseSummaries.size();
		averagePassRate = sumOfPassRates / (sortedHistory.size());

		//
		// Test packages are stored flat - lets fake a tree structure
		//
		// 1) Get a list of sorted keys.
		// 2) For every package where there is a sub match in name we increment.
		//
		String[] packageNames = testPackageSummaries.keySet().toArray(
				new String[0]);
		Arrays.sort(packageNames, new CustomStringLengthComparator());

		for (String s1 : packageNames) {
			for (String s2 : packageNames) {
				// We do not want to increment the same
				if (!s1.equalsIgnoreCase(s2)) {
					// Trim s1 one level!
					if (s1.contains(".")) {
						String parent = s1.substring(0, s1.lastIndexOf("."));
						if (parent.equalsIgnoreCase(s2)) {
							testPackageSummaries.get(s2).addFrom(
									testPackageSummaries.get(s1));
						}
					}
				}
			}
		}

		history = null;
		history = sortedHistory;
	}

	/**
	 * Return the number of executed test cases in all runs.
	 * 
	 * @return The number of executed test cases in all runs.
	 */
	public int getNoExecutedTestCases() {
		return noExecutedTestCases;
	}

	/**
	 * Return the number of passed test cases in all runs.
	 * 
	 * @return The number of passed test cases in all runs.
	 */
	public int getNoPassedTestCases() {
		return noPassedTestCases;
	}

	/**
	 * Return the number of failed test cases in all runs.
	 * 
	 * @return The number of failed test cases in all runs.
	 */
	public int getNoFailedTestCases() {
		return noFailedTestCases;
	}

	/**
	 * Return the number of error test cases in all runs.
	 * 
	 * @return The number of error test cases in all runs.
	 */
	public int getNoErrorTestCases() {
		return noErrorTestCases;
	}

	/**
	 * Return the total execution run for all test runs, summarized.
	 * 
	 * @return The total execution run for all test runs, summarized.
	 */
	public String getTotalExecutionTime() {
		return TestItemUtils.executionTimeToString(execTimeSum);
	}

	/**
	 * Return the average pass rate over all test runs.
	 * 
	 * @return The average pass rate over all test runs.
	 */
	public double getAvePassPctDouble() {
		return averagePassRate;
	}

	/**
	 * This method returns a test run based on its index.
	 * 
	 * @param idx
	 *            The index of the test run to fetch.
	 * @return The looked for test run.
	 */
	public TestRun getTestRunByIdx(int idx) {
		for (TestRun tr : history) {
			if (tr.getRunIdx() == idx) {
				return tr;
			}
		}
		return null;
	}

	/**
	 * Returns the pass rate difference between the last run and second last
	 * run.
	 * 
	 * @return Returns the pass rate difference.
	 */
	public String getPrTrendLastRun() {
		int runs = history.size();
		if (runs < 2) {
			return "NA";
		} else {
			return getPrDiff(runs, 1);
		}
	}

	/**
	 * Returns the pass rate difference between the last run and five last runs.
	 * 
	 * @return Returns the pass rate difference.
	 */
	public String getPrTrendLast5Runs() {
		int runs = history.size();
		if (runs < 5) {
			return "NA";
		} else {
			return getPrDiff(runs, 4);
		}
	}

	/**
	 * Returns the pass rate difference between the last run and 10 last runs.
	 * run.
	 * 
	 * @return Returns the pass rate difference.
	 */
	public String getPrTrendLast10Runs() {
		int runs = history.size();
		if (runs < 10) {
			return "NA";
		} else {
			return getPrDiff(runs, 9);
		}
	}

	/*
	 * @param runs The number of runs in the history since we need an offset to
	 * count from.
	 * 
	 * @param idx The index of the run to match with.
	 * 
	 * @return The pass rate difference as string.
	 */
	private String getPrDiff(int runs, int idx) {
		double diff = Double.parseDouble(getTestRunByIdx(runs).getPassPct())
				- Double.parseDouble(getTestRunByIdx(runs - idx).getPassPct());
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
	private TestRun getTrendTestRun(long trendInterval) {
		long currentTime = System.currentTimeMillis();
		long breakPoint = currentTime - trendInterval;
		TestRun matchingTr = null;
		long matchingTrTimeStamp = 0;
		for (TestRun tr : history) {
			long runTimeStamp = tr.getRunDateAsLong();
			if (breakPoint > runTimeStamp) {
				if ((breakPoint - matchingTrTimeStamp) > (breakPoint - runTimeStamp)) {
					matchingTr = tr;
					matchingTrTimeStamp = runTimeStamp;
				}
			}
		}
		return matchingTr;
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
		TestRun matchingTr = getTrendTestRun(trendInterval);
		if (null != matchingTr) {
			int runs = history.size();
			double diff = Double
					.parseDouble(getTestRunByIdx(runs).getPassPct())
					- Double.parseDouble(matchingTr.getPassPct());

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
		TestRun matchingTr = getTrendTestRun(trendInterval);
		if (null != matchingTr) {
			int runs = history.size();
			int diff = getTestRunByIdx(runs).getNoTestCases()
					- matchingTr.getNoTestCases();

			String ret = Integer.toString(diff);
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
	public String getFnTrend(long trendInterval) {
		TestRun matchingTr = getTrendTestRun(trendInterval);
		if (null != matchingTr) {
			int runs = history.size();
			int diff = getTestRunByIdx(runs).getNoNonPassing()
					+ -matchingTr.getNoNonPassing();

			String ret = Integer.toString(diff);
			if (0 < diff) {
				ret = "+" + ret;
			}
			return ret;
		} else {
			return "NA";
		}
	}

	/**
	 * Returns the largest execution time in all test runs.
	 * 
	 * @return Returns the largest execution time in all test runs.
	 */
	public double getLargestExecutionTime() {
		return largestExecutionTime;
	}

	/**
	 * Checks if the passed package has any packages that are potential
	 * subpackages based on the package names.
	 * 
	 * @param packageName
	 *            of the package to find subpackages for.
	 * @return true or false
	 */
	public boolean hasSubPackageSummaries(String packageName) {
		Collection<TestPackageSummary> c = testPackageSummaries.values();
		Iterator<TestPackageSummary> iter = c.iterator();
		while (iter.hasNext()) {
			TestPackageSummary tps = iter.next();
			if (null != tps) {
//				if (tps.getName().contains(packageName + ".")
//						&& tps.getName().startsWith(packageName + ".")) {
				if (tps.getName().startsWith(packageName + ".")) {
					return true;
				}
			}
		}
		return false;
	}
}
