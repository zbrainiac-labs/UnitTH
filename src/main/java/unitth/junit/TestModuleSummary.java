/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

/**
 * Summary of a <code>TestModule</code> over all runs.
 */
public class TestModuleSummary extends TestGroupSummary<TestModule> {

	public TestModuleSummary(TestModule tm) {
		super(tm);
		increment(tm);
	}

	/**
	 * Adds the statistics of one more run of this module.
	 * 
	 * @param tm
	 *            The test module run to add.
	 */
	public void increment(TestModule tm) {
		add(tm);
		for (TestCase tc : tm.getTestCases().values()) {
			uniqueTestCases.add(tc.getName());
		}
	}

	@Override
	protected int executed(TestModule tm) {
		return tm.getIsIgnored() ? 0 : tm.getNoTestCases();
	}
}

/* eof */
