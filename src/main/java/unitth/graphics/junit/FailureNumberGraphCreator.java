/*
 * MIT License - see LICENSE.txt
 */
package unitth.graphics.junit;

import java.awt.Color;

import unitth.junit.TestHistory;
import unitth.junit.TestModule;
import unitth.junit.TestPackage;
import unitth.junit.TestRun;

public class FailureNumberGraphCreator extends NumberGraphCreator {

	/**
	 * CTOR, sets the failure number graph specifics attributes, like
	 * out put file names and coloring of the generated graph.
	 * 
	 * @param history The test history to get the data for the graph from.
	 */
	public FailureNumberGraphCreator(TestHistory history) {
		super(history);
		mainImage = "fn.png";
		filePrefix = "mfn-";
		color = Color.RED;
	}

	/* (non-Javadoc)
	 * @see unitth.graphics.NumberGraphCreator#getGraphNumbers(unitth.core.TestRun)
	 */
	protected int getGraphNumbers(TestRun tr) {
		return tr.getNoFailures()+tr.getNoErrors();
	}

	/* (non-Javadoc)
	 * @see unitth.graphics.NumberGraphCreator#getGraphNumbers(unitth.core.TestModule)
	 */
	protected int getGraphNumbers(TestModule tm) {
		return tm.getNoFailures()+tm.getNoErrors();
	}

	/* (non-Javadoc)
	 * @see unitth.graphics.NumberGraphCreator#getGraphNumbers(unitth.core.TestModule)
	 */
	protected int getGraphNumbers(TestPackage tp) {
		return tp.getNoFailures()+tp.getNoErrors();
	}
}
