/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import unitth.core.TestItemUtils;

public class TestPackage extends TestItem {

	/** All unique tests modules in this package. */
	private int noTestModulesRec = 0;
	/** All unique tests with error verdict in all sub packages and test classes. */
	private int noErrors = 0;
	/** All unique tests with fail verdict in all sub packages and test classes. */
	private int noFailures = 0;
	/** All unique tests with passed verdict in all sub packages and test classes. */
	private int noPassed = 0;
	/** All unique tests with ignored verdict in all sub packages and test classes. */
	private int noIgnored = 0;
	/** All unique tests packages in this test package. */
	private int noTestPackagesRec = 0;
	/** All unique tests cases in this package's test modules. */
	private int noTestCases = 0;
	/** The run index for this instance of <code>TestPackage</code>. */
	private int runIdx = 0;

	/** This is a container for all test classes part of this package. */
	private HashMap<String, TestModule> testModules = null;
	
	public String toString() {
		String ret = "\n--+-- Test package --+--\n";
		ret += name + "\n" + "Run date: " + runDate + "\n" + "Execution time: "
				+ executionTime + "\n" + "Test cases: " + noTestCases + "\n"
				+ "Errors: " + noErrors + "\n" 
				+ "Failures: " + noFailures + "\n"
				+ "Ignored: " + noIgnored + "\n"
				+ "Pass: " + getNoPassed() + "\n"
		        + "Pass %: " + passPct + "%";
		if (testModules.size() > 0) {
			ret += "\n";
		}

		Collection<TestModule> c = testModules.values();
		Iterator<TestModule> iter = c.iterator();
		ret += "Test modules: "; 
		while (iter.hasNext()) {
			TestModule tm = iter.next();
			ret += tm.toString() + "\n";
		}
		return ret;
	}
	
	/**
	 * Ctor, simply instantiates the object containers.
	 */
	public TestPackage() {
		runDate = "";
		testModules = new HashMap<String, TestModule>();
	}
	
	/**
	 * This methods adds a test module to the current TestPackage. The key
	 * is the name of the <code>TestModule</code>.
	 * @param tm The <code>TestModule</code> to be added.
	 */
	public void addTestModule(TestModule tm) {
		// Is the package already initialized, first add?
		if (name.equals("")) {
			name = tm.getPackageName();
		}
		if (testModules != null && tm != null) {
			testModules.put(tm.getName(), tm);
		}
	}
		
	public int getNoTestModulesThis() {
		return testModules.size();
	}
	
	public int getNoTestCases() {
		return noTestCases;
	}
	
	public int getNoTestPackagesRec() {
		return noTestPackagesRec;
	}
	
	public int getNoTestModulesRec() {
		return noTestModulesRec;
	}

	public void setNoTestModules(int noTestModules) {
		this.noTestModulesRec = noTestModules;
	}
	
	public int getNoErrors() {
		return noErrors;
	}
	
	public void setNoErrors(int noErrors) {
		this.noErrors = noErrors;
	}
	
	public void setNoIgnored(int noIgnored) {
		this.noIgnored = noIgnored;
	}
	
	public int getNoFailures() {
		return noFailures;
	}
	
	public void setNoFailures(int noFailures) {
		this.noFailures = noFailures;
	}
	
	public int getNoPassed() {
		return noPassed;
	}
	
	public void setNoPassed(int noPassed) {
		this.noPassed = noPassed;
	}
	
	public void setNoTestCases(int noTestCases) {
		this.noTestCases = noTestCases;
	}
	
	public int getRunIdx() {
		return runIdx;
	}
	
	public void setRunIdx(int runIdx) {
		this.runIdx = runIdx;
	}
	
	public double getPassPctDouble() {
		return passPct;
	}
	

	public void setPassPct(double passPct) {
		this.passPct = passPct;
	}
	
	public String getRunDate() {
		return runDate;
	}
	
	public void setRunDate(String runDate) {
		this.runDate = TestItemUtils.fixDateFormat(runDate);
	}
	
	/**
	 * This method runs through all the test cases and packages for this 
	 * package. All the stats are calculated once and only once.
	 */
	public void calcStats() {

		// Iterate through all modules in this package.
		Collection<TestModule> cm = testModules.values();
		Iterator<TestModule> iterm = cm.iterator();

		while (iterm.hasNext()) {
			TestModule tm = iterm.next();
			tm.calcStats();
			
			// Summarize, recursively
			noErrors += tm.getNoErrors();
			noIgnored += tm.getNoIgnored();
			noFailures += tm.getNoFailures();
			noPassed += tm.getNoPassed();
			noTestCases += tm.getNoTestCases();
			this.executionTime += tm.getExecutionTimeDouble();
			
			String timeStampHolder = tm.getExecutionDate();
			if (runDate == null || runDate.isEmpty() || 0 < runDate.compareToIgnoreCase(timeStampHolder)) {
				runDate = timeStampHolder;
			}
		}
		
		// Final calculations
		noTestModulesRec += testModules.size();
		
		// Division by ZERO
		if (0 > noPassed || 0 == noTestCases) {
			noPassed = 0;
			passPct = 0.0;
		} else {
		    passPct = ((double) noPassed / (double) noTestCases) * 100;
		}
	}
	
	public String getExecutionDate() {
		return runDate;
	}
	
	public HashMap<String, TestModule> getTestModules() {
		return testModules;
	}
	
	public TestModule getTestModule(String name) {
		return testModules.get(name);
	}
	
	/**
	 * Returns the number of non passing test cases in a test module.
	 * 
	 * @return The number of non passing test cases in a test module.
	 */
	public int getNoNonPassing() {
		return noErrors+noFailures;
	}
	
	public void addFrom(TestPackage tp) {
		noErrors += tp.getNoErrors();
		noFailures += tp.getNoFailures();
		noPassed += tp.getNoPassed();
		noIgnored += tp.getNoIgnored();
		noTestCases += tp.getNoTestCases();
		this.executionTime += tp.getExecutionTimeDouble();
		
		noTestModulesRec += tp.testModules.size();
		
		// Division by ZERO
		if (0 > noPassed || 0 == noTestCases) {
			noPassed = 0;
			passPct = 0.0;
		} else {
			passPct = ((double) noPassed / (double) noTestCases) * 100;
		}
	}

	public int getNoIgnored() {
	    return noIgnored;
	}	
}

/* eof */
