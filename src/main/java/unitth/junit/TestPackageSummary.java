/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.util.TreeSet;

/**
 * Summary of a <code>TestPackage</code> over all runs. Packages are stored
 * flat; <code>addFrom</code> folds sub package statistics into their parent
 * so the result looks like a tree.
 */
public class TestPackageSummary extends TestGroupSummary<TestPackage> {

	private final TreeSet<String> uniqueTestModules = new TreeSet<>();

	public TestPackageSummary(TestPackage tp) {
		super(tp);
		increment(tp);
	}

	/**
	 * Adds the statistics of one more run of this package.
	 * 
	 * @param tp
	 *            The test package run to add.
	 */
	public void increment(TestPackage tp) {
		add(tp);
		addTestModuleNames(tp);
		for (TestModule tm : tp.getTestModules().values()) {
			for (TestCase tc : tm.getTestCases().values()) {
				uniqueTestCases.add(tc.getFullName());
			}
		}
	}

	@Override
	protected int executed(TestPackage tp) {
		return tp.getNoTestCases();
	}

	private void addTestModuleNames(TestPackage tp) {
		uniqueTestModules.addAll(tp.getTestModules().keySet());
	}

	public int getNoUniqueTestModules() {
		return uniqueTestModules.size();
	}

	/**
	 * Folds a direct sub package into this one: unique names and the largest
	 * execution time.
	 * 
	 * @param tps
	 *            The sub package summary.
	 */
	public void addFrom(TestPackageSummary tps) {
		uniqueTestModules.addAll(tps.uniqueTestModules);
		uniqueTestCases.addAll(tps.uniqueTestCases);
		largestExecutionTime = Math.max(largestExecutionTime, tps.getExecutionTimeDouble());
	}
}
