/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.ToIntFunction;

import unitth.core.Log;
import unitth.core.TestItemUtils;
import unitth.core.UnitTHConfig;
import unitth.core.UnitTHException;

/**
 * This class is main place holder for all the statistics that have been read
 * from the parsed runs.
 * <p>
 * It contains a list for <code>TestRun</code> objects, <code>TestCase</code>,
 * <code>TestModule</code> and <code>TestPackage</code> summaries.
 */
public class TestHistory implements TrendSource {

	private final UnitTHConfig config;
	private TreeSet<TestRun> history = new TreeSet<>();
	private final Map<Integer, TestRun> runsByIdx = new HashMap<>();
	private int noUniqueTestCases = 0;
	private int noIgnoredTestCases = 0;
	private double worstRun = 100.0;
	private double bestRun = 0.0;
	private double averagePassRate = 0.0;
	private double largestExecutionTime = 0.0;

	/* Summaries over all runs, keyed by name. */
	private final TreeMap<String, TestModuleSummary> testModuleSummaries = new TreeMap<>();
	private final TreeMap<String, TestCaseSummary> testCaseSummaries = new TreeMap<>();
	private final TreeMap<String, TestPackageSummary> testPackageSummaries = new TreeMap<>();

	/* Time stamp of the newest run. */
	private String lastRunStr = "0";

	private int noExecutedTestCases = 0;
	private int noPassedTestCases = 0;
	private int noFailedTestCases = 0;
	private int noErrorTestCases = 0;
	private double execTimeSum = 0.0;

	public TestHistory(UnitTHConfig config) {
		this.config = config;
	}

	public UnitTHConfig getConfig() {
		return config;
	}

	/**
	 * Adds a parsed test run to the history
	 * 
	 * @param run
	 *            The test run to add.
	 */
	public void addTestRun(TestRun run) {
		history.add(run);
	}

	/** @return The runs, newest first after <code>calcStats</code>. */
	public TreeSet<TestRun> getRuns() {
		return history;
	}

	public TreeMap<String, TestCaseSummary> getTestCaseSummaries() {
		return testCaseSummaries;
	}

	public TestCaseSummary getTestCaseSummary(String testCaseName) {
		return testCaseSummaries.get(testCaseName);
	}

	public TreeMap<String, TestModuleSummary> getTestModuleSummaries() {
		return testModuleSummaries;
	}

	public TestModuleSummary getTestModuleSummary(String moduleName) {
		return testModuleSummaries.get(moduleName);
	}

	public TestPackageSummary getTestPackageSummary(String packageName) {
		return testPackageSummaries.get(packageName);
	}

	public TreeMap<String, TestPackageSummary> getTestPackageSummaries() {
		return testPackageSummaries;
	}

	/** @return The time stamp of the newest run. */
	public String getLastRun() {
		return lastRunStr;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (TestRun run : history) {
			buf.append(run).append('\n');
		}
		return buf.toString();
	}

	public Object[] getUniqueModules() {
		return testModuleSummaries.keySet().toArray();
	}

	public Object[] getUniquePackages() {
		return testPackageSummaries.keySet().toArray();
	}

	public int getNoRuns() {
		return history.size();
	}

	public int getNoUniqueModules() {
		return testModuleSummaries.size();
	}

	public int getNoPackages() {
		return testPackageSummaries.size();
	}

	public int getNoUniqueTestCases() {
		return noUniqueTestCases;
	}

	public int getNoIgnoredTestCases() {
		return noIgnoredTestCases;
	}

	public String getBestRun() {
		return TestItemUtils.passPctToString(bestRun);
	}

	public String getWorstRun() {
		return TestItemUtils.passPctToString(worstRun);
	}

	public String getAvePassPct() {
		return TestItemUtils.passPctToString(averagePassRate);
	}

	public double getAvePassPctDouble() {
		return averagePassRate;
	}

	/**
	 * Calculates the statistics of every run, then indexes the runs by date
	 * (newest = highest index) and builds the package, module and test case
	 * summaries.
	 */
	public void calcStats() {
		double sumOfPassRates = 0.0;
		TreeSet<TestRun> sortedHistory = new TreeSet<>();

		for (TestRun tr : history) {
			tr.calcStats();
			double passRate = tr.getPassPctDouble();
			if (passRate > bestRun) {
				bestRun = passRate;
			}
			if (passRate < worstRun) {
				worstRun = passRate;
			}
			sumOfPassRates += passRate;

			String timeStamp = tr.getExecutionDate();
			if (timeStamp == null) {
				Log.warn("The test run execution date is 'null', this can be an indication that there were no results to be parsed for this run.");
				continue;
			}
			if (0 > lastRunStr.compareToIgnoreCase(timeStamp)) {
				lastRunStr = timeStamp;
			}
			noExecutedTestCases += tr.getNoTestCases();
			noPassedTestCases += tr.getNoPassed();
			noFailedTestCases += tr.getNoFailures();
			noErrorTestCases += tr.getNoErrors();
			noIgnoredTestCases += tr.getNoIgnored();
			execTimeSum += tr.getExecutionTimeDouble();
			sortedHistory.add(tr);
		}

		int newest = history.size();
		int index = sortedHistory.size();
		for (TestRun tr : sortedHistory) {
			tr.setRunIdx(index);
			runsByIdx.put(index, tr);
			for (TestPackage tp : tr.getTestPackages().values()) {
				tp.setRunIdx(index);
				addPackage(tp, index == newest);
				for (TestModule tm : tp.getTestModules().values()) {
					tm.setRunIdx(index);
					addModule(tm, index == newest);
					for (TestCase tc : tm.getTestCases().values()) {
						addTestCase(tc, index);
					}
				}
			}
			largestExecutionTime = Math.max(largestExecutionTime, tr.getExecutionTimeDouble());
			if (!config.useAbsPaths()) {
				tr.setRelativePathFromOutputDir(config.rootFolder(), config.reportPath());
			} else {
				tr.setAbsolutePath(config.rootFolder(), config.reportPath());
			}
			index--;
		}
		noUniqueTestCases = testCaseSummaries.size();
		averagePassRate = sumOfPassRates / sortedHistory.size();

		TestItemUtils.foldIntoParents(testPackageSummaries, TestPackageSummary::addFrom);
		history = sortedHistory;
	}

	private void addPackage(TestPackage tp, boolean inNewestRun) {
		TestPackageSummary tps = testPackageSummaries.get(tp.getName());
		if (tps == null) {
			tps = new TestPackageSummary(tp);
			testPackageSummaries.put(tp.getName(), tps);
		} else {
			tps.increment(tp);
		}
		// Trends are calculated from the newest run, so only its items get the total.
		if (inNewestRun) {
			tps.setTotalNumberOfTestRuns(tp.getRunIdx());
		}
	}

	private void addModule(TestModule tm, boolean inNewestRun) {
		TestModuleSummary tms = testModuleSummaries.get(tm.getName());
		if (tms == null) {
			tms = new TestModuleSummary(tm);
			testModuleSummaries.put(tm.getName(), tms);
		} else {
			tms.increment(tm);
		}
		if (inNewestRun) {
			tms.setTotalNumberOfTestRuns(tm.getRunIdx());
		}
	}

	private void addTestCase(TestCase tc, int runIdx) {
		TestCaseSummary tcs = testCaseSummaries.get(tc.getFullName());
		if (tcs == null) {
			testCaseSummaries.put(tc.getFullName(), new TestCaseSummary(tc, runIdx));
			return;
		}
		try {
			tcs.increment(tc, runIdx);
		} catch (UnitTHException e) {
			Log.error("Trying to add test case stats to a list created for another test case name", e);
		}
	}

	public int getNoExecutedTestCases() {
		return noExecutedTestCases;
	}

	public int getNoPassedTestCases() {
		return noPassedTestCases;
	}

	public int getNoFailedTestCases() {
		return noFailedTestCases;
	}

	public int getNoErrorTestCases() {
		return noErrorTestCases;
	}

	/** @return The summed execution time of all runs. */
	public String getTotalExecutionTime() {
		return TestItemUtils.executionTimeToString(execTimeSum);
	}

	/**
	 * Returns a test run based on its index, 1 = oldest.
	 * 
	 * @param idx
	 *            The index of the test run to fetch.
	 * @return The looked for test run or null.
	 */
	public TestRun getTestRunByIdx(int idx) {
		return runsByIdx.get(idx);
	}

	/* The run older than the interval that is closest to it. */
	private TestRun getTrendTestRun(long trendInterval) {
		long breakPoint = System.currentTimeMillis() - trendInterval;
		TestRun matchingTr = null;
		long matchingTrTimeStamp = 0;
		for (TestRun tr : history) {
			long runTimeStamp = tr.getRunDateAsLong();
			if (breakPoint > runTimeStamp && runTimeStamp > matchingTrTimeStamp) {
				matchingTr = tr;
				matchingTrTimeStamp = runTimeStamp;
			}
		}
		return matchingTr;
	}

	public String getPrTrend(long trendInterval) {
		TestRun then = getTrendTestRun(trendInterval);
		if (then == null) {
			return "NA";
		}
		TestRun now = getTestRunByIdx(history.size());
		return TestItemUtils.signedPct(Double.parseDouble(now.getPassPct()) - Double.parseDouble(then.getPassPct()));
	}

	private String countTrend(long trendInterval, ToIntFunction<TestRun> count) {
		TestRun then = getTrendTestRun(trendInterval);
		if (then == null) {
			return "NA";
		}
		return TestItemUtils.signed(count.applyAsInt(getTestRunByIdx(history.size())) - count.applyAsInt(then));
	}

	public String getTcTrend(long trendInterval) {
		return countTrend(trendInterval, TestRun::getNoTestCases);
	}

	public String getFnTrend(long trendInterval) {
		return countTrend(trendInterval, TestRun::getNoNonPassing);
	}

	/** @return The largest execution time of any run. */
	public double getLargestExecutionTime() {
		return largestExecutionTime;
	}

	/**
	 * Checks if the passed package has sub packages based on the package
	 * names.
	 * 
	 * @param packageName
	 *            of the package to find subpackages for.
	 * @return true or false
	 */
	public boolean hasSubPackageSummaries(String packageName) {
		String prefix = packageName + ".";
		return testPackageSummaries.keySet().stream().anyMatch(n -> n.startsWith(prefix));
	}
}
