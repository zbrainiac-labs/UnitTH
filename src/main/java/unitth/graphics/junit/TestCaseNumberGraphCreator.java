/*
 * MIT License - see LICENSE.txt
 */
package unitth.graphics.junit;

import java.awt.Color;

import unitth.junit.TestHistory;
import unitth.junit.TestModule;
import unitth.junit.TestPackage;
import unitth.junit.TestRun;

public class TestCaseNumberGraphCreator extends NumberGraphCreator {

	public TestCaseNumberGraphCreator(TestHistory history) {
		super(history);
		mainImage = "tc.png";
		filePrefix = "mtc-";
		color = Color.GREEN;
	}

	/* (non-Javadoc)
	 * @see unitth.graphics.NumberGraphCreator#getGraphNumbers(unitth.core.TestRun)
	 */
	protected int getGraphNumbers(TestRun tr) {
		return tr.getNoTestCases();
	}

	/* (non-Javadoc)
	 * @see unitth.graphics.NumberGraphCreator#getGraphNumbers(unitth.core.TestModule)
	 */
	protected int getGraphNumbers(TestModule tm) {
		return tm.getNoTestCases();
	}
	
	/* (non-Javadoc)
	 * @see unitth.graphics.NumberGraphCreator#getGraphNumbers(unitth.core.TestModule)
	 */
	protected int getGraphNumbers(TestPackage tp) {
		return tp.getNoTestCases();
	}
}
