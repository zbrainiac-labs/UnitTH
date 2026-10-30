/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

/**
 * Trend KPIs over fixed time windows. Implementors provide the calculation
 * for an arbitrary interval; the 1/3/7-day accessors used by the HTML
 * generators are shared here.
 */
public interface TrendSource {

	long DAY_MS = 24L * 60 * 60 * 1000;

	/** Change in number of test cases since the newest run older than the interval. */
	String getTcTrend(long trendInterval);

	/** Change in number of non-passing test cases since the newest run older than the interval. */
	String getFnTrend(long trendInterval);

	/** Change in pass rate since the newest run older than the interval. */
	String getPrTrend(long trendInterval);

	default String getTcTrendLastDay() {
		return getTcTrend(DAY_MS);
	}

	default String getTcTrendLast3Days() {
		return getTcTrend(3 * DAY_MS);
	}

	default String getTcTrendLast7Days() {
		return getTcTrend(7 * DAY_MS);
	}

	default String getFnTrendLastDay() {
		return getFnTrend(DAY_MS);
	}

	default String getFnTrendLast3Days() {
		return getFnTrend(3 * DAY_MS);
	}

	default String getFnTrendLast7Days() {
		return getFnTrend(7 * DAY_MS);
	}

	default String getPrTrendLast3Days() {
		return getPrTrend(3 * DAY_MS);
	}

	default String getPrTrendLast7Days() {
		return getPrTrend(7 * DAY_MS);
	}
}
