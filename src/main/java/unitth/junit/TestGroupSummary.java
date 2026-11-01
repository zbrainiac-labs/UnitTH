/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.util.HashMap;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.ToIntFunction;

import unitth.core.TestItemUtils;

/**
 * Summary of a test module or test package over all runs. The spread maps the
 * run index to the item instance of that run.
 *
 * @param <T> <code>TestModule</code> or <code>TestPackage</code>
 */
public abstract class TestGroupSummary<T extends RunCounts> extends TestItemSummary implements TrendSource {

	protected final HashMap<Integer, T> spread = new HashMap<>();
	protected final TreeSet<String> uniqueTestCases = new TreeSet<>();
	protected int noExecuted = 0;
	protected double largestExecutionTime = 0.0;
	private double worstRun = 100.0;
	private double bestRun = 0.0;
	private int totalNumberOfTestRuns = 0;

	protected TestGroupSummary(T first) {
		name = first.getName();
		noRuns = 0;
	}

	/** Number of test cases of this run that count as executed. */
	protected abstract int executed(T item);

	/** Adds the statistics of one more run of this item. */
	protected void add(T item) {
		noRuns++;
		noExecuted += executed(item);
		noPassed += item.getNoPassed();
		noFailed += item.getNoFailures();
		noErrors += item.getNoErrors();
		noIgnored += item.getNoIgnored();
		sumExecutionTime += item.getExecutionTimeDouble();
		largestExecutionTime = Math.max(largestExecutionTime, item.getExecutionTimeDouble());
		// Comparisons rather than Math.max/min so a NaN pass rate is ignored.
		double pct = item.getPassPctDouble();
		if (pct > bestRun) {
			bestRun = pct;
		}
		if (pct < worstRun) {
			worstRun = pct;
		}
		spread.put(item.getRunIdx(), item);
	}

	/** @return Returns the number of executed test cases. */
	public int getNoTestCases() {
		return noExecuted;
	}

	/** @return Returns the number of unique test cases. */
	public int getNoUniqueTestCases() {
		return uniqueTestCases.size();
	}

	public double getLargestExecutionTime() {
		return largestExecutionTime;
	}

	public String getBestRun() {
		return String.format(Locale.US, "%1.2f", bestRun);
	}

	public String getWorstRun() {
		return String.format(Locale.US, "%1.2f", worstRun);
	}

	/**
	 * Sets the total number of test runs, needed for trend calculation. Only
	 * set for items that are part of the newest run.
	 */
	public void setTotalNumberOfTestRuns(int total) {
		this.totalNumberOfTestRuns = total;
	}

	/* Newest run of this item that is older than the trend interval. */
	private T getTrendTestRun(long trendInterval) {
		long breakPoint = System.currentTimeMillis() - trendInterval;
		for (int i = totalNumberOfTestRuns - 1; i > 0; i--) {
			T item = spread.get(i);
			if (item != null && breakPoint > item.getRunDateAsLong()) {
				return item;
			}
		}
		return null;
	}

	private String countTrend(long trendInterval, ToIntFunction<T> count) {
		T then = getTrendTestRun(trendInterval);
		T now = spread.get(totalNumberOfTestRuns);
		if (then == null || now == null) {
			return "NA";
		}
		return TestItemUtils.signed(count.applyAsInt(now) - count.applyAsInt(then));
	}

	public String getPrTrend(long trendInterval) {
		T then = getTrendTestRun(trendInterval);
		T now = spread.get(totalNumberOfTestRuns);
		if (then == null || now == null) {
			return "NA";
		}
		return TestItemUtils.signedPct(Double.parseDouble(now.getPassPct()) - Double.parseDouble(then.getPassPct()));
	}

	public String getTcTrend(long trendInterval) {
		return countTrend(trendInterval, RunCounts::getNoTestCases);
	}

	public String getFnTrend(long trendInterval) {
		return countTrend(trendInterval, RunCounts::getNoNonPassing);
	}
}
