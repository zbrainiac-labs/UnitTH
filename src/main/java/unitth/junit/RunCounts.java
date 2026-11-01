/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

/**
 * Per-run counters shared by <code>TestRun</code>, <code>TestPackage</code>
 * and <code>TestModule</code>, so summaries, charts and run tables can treat
 * them alike.
 */
public interface RunCounts {

	String getName();

	int getNoTestCases();

	int getNoPassed();

	int getNoFailures();

	int getNoErrors();

	int getNoIgnored();

	int getNoNonPassing();

	String getPassPct();

	double getPassPctDouble();

	String getExecutionTime();

	double getExecutionTimeDouble();

	String getExecutionDate();

	long getRunDateAsLong();

	int getRunIdx();
}
