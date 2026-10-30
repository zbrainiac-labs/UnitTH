/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import unitth.core.TestItemUtils;
import unitth.core.UnitTHException;

/**
 * Purpose, summarize a set of historical packages
 *
 * Identified by name = test.func1.func2
 * Identified by name = test.func1
 * Identified by name = test
 * 
 * Will generate three packages, only the ones that includes test
 * cases will be part of the out put.
 * 
 * Same output as for the modules but filtered and summarized
 * on the package name.
 */
public class TestPackageSummary extends TestItemSummary {

	private TreeSet<String> noUniqueTestModules = null;
	private TreeSet<String> noUniqueTestCases = null;
	
	protected int noExecuted = 0;
	protected int largestNoOfTestCases = 0;
	protected int largestNumberOfFailures = 0; 
	protected int largestNumberOfErrors = 0; 
	protected int largestNumberOfNonPass = 0; 
	protected double largestExecutionTime = 0.0;
	
	/** Quick check for presence of test cases. */ 
	public boolean containsTestModules = false;
	
	/** Contains the spread that indicates what executions where
	 *  this package has been included.
	 */
	private HashMap<Integer, TestPackage> spread = null;
	
	public TestPackageSummary(TestPackage tp) {
		
		noUniqueTestModules = new  TreeSet<String>();
		noUniqueTestCases = new  TreeSet<String>();
		
		noRuns = 1;
		name = tp.getName();
		noPassed = tp.getNoPassed();
		noFailed = tp.getNoFailures();
		noErrors = tp.getNoErrors();
		noIgnored = tp.getNoIgnored();
		noExecuted = tp.getNoTestCases();
		
		largestNoOfTestCases = noExecuted;
		largestNumberOfFailures = noFailed; 
		largestNumberOfErrors = noErrors; 
		largestNumberOfNonPass = noErrors+noFailed; 
		sumExecutionTime = tp.getExecutionTimeDouble();
		largestExecutionTime = sumExecutionTime;
		
		if (tp.getNoTestModulesThis() > 0) {
			containsTestModules = true;
		}
		
		double passPctPlaceHolder = tp.getPassPctDouble();
		if (passPctPlaceHolder > bestRun) {
			bestRun = passPctPlaceHolder;
		}
		if (passPctPlaceHolder < worstRun) {
			worstRun = passPctPlaceHolder;
		}
		spread = new HashMap<Integer, TestPackage>();
		spread.put(tp.getRunIdx(), tp);
		addTestModuleNames(tp);
	}
	
	/**
	 * This method increments all the statistics in this summary for each new
	 * test module that gets added.
	 * @param tm
	 *            The test module who's information to add to the summary.
	 * @throws UnitTHException
	 */
	public void increment(TestPackage tp) throws UnitTHException {
		
		if (name.equals(tp.getName())) {
			noRuns++;
			noExecuted += tp.getNoTestCases();
			noPassed += tp.getNoPassed();
			noFailed += tp.getNoFailures();
			noErrors += tp.getNoErrors();
			noIgnored += tp.getNoIgnored();
			sumExecutionTime += tp.getExecutionTimeDouble();
		
			if (tp.getNoTestModulesThis() > 0) {
				containsTestModules = true;
			}
			
			// Largest number of test cases
			if (tp.getNoTestCases() > largestNoOfTestCases) {
				largestNoOfTestCases = tp.getNoTestCases();
			}
			
			// Largest number of failed test cases
			int noFailuresHolder = tp.getNoFailures(); 
			if (noFailuresHolder > largestNumberOfFailures) {
				largestNumberOfFailures = noFailuresHolder;
			}

			// Largest number of error test cases
			int noErrorsHolder = tp.getNoErrors(); 
			if (noErrorsHolder > largestNumberOfErrors) {
				largestNumberOfErrors = noErrorsHolder;
			}

			// Needed for the calculation of the negative graph. 
			if (noFailuresHolder+noErrorsHolder > largestNumberOfNonPass) {
				largestNumberOfNonPass = noFailuresHolder+noErrorsHolder; 
			}
			
			// Largest execution time
			if (tp.getExecutionTimeDouble() > largestExecutionTime) {
				largestExecutionTime = tp.getExecutionTimeDouble();
			}

			double passPctPlaceHolder = tp.getPassPctDouble();
			if (passPctPlaceHolder > bestRun) {
				bestRun = passPctPlaceHolder;
			}
			if (passPctPlaceHolder < worstRun) {
				worstRun = passPctPlaceHolder;
			}
			spread.put(tp.getRunIdx(), tp);
			addTestModuleNames(tp);
			addTestCaseNames(tp);
		} else {
			throw new UnitTHException(
					"This module has a different class name than expected, "+tp.getName()+" !");
		}
	}
	
	/**
	 * Returns the number of executed test cases in this class.
	 * @return Returns the number of executed test cases in this class.
	 */
	public int getNoTestCases() {
		return this.noExecuted;
	}
	
	/**
	 * Does this package contain any test modules?
	 * @return Does this package contain any test modules?
	 */
	public boolean containsTestModules() {
		return this.containsTestModules;	
	}
	
	public double getLargestExecutionTime() {
		return largestExecutionTime;
	}
	
	public int getLargestNumberOfNonPass() {
		return largestNumberOfNonPass;
	}
	
	public int getLargestNumberOfTcs() {
		return largestNoOfTestCases;
	}
	
	public int getNoUniqueTestCases() {
		return noUniqueTestCases.size();
	}
	
	public int getNoUniqueTestModules() {
		return noUniqueTestModules.size();
	}
	
	/*
	 * Adds unique test case names to a list.
	 * @param tm The test module who's test case names to add to the unique test
	 * case names list.
	 */
	private void addTestModuleNames(TestPackage tp) {
		Collection<TestModule> c = tp.getTestModules().values();
		Iterator<TestModule> iter = c.iterator();
		while (iter.hasNext()) {
			TestModule tm = iter.next();
			noUniqueTestModules.add(tm.getName());
		}
	}
	
	private void addTestCaseNames(TestPackage tp) {
		Collection<TestModule> c = tp.getTestModules().values();
		Iterator<TestModule> iter = c.iterator();
		while (iter.hasNext()) {
			TestModule tm = iter.next();
			
			Collection<TestCase> c2 = tm.getTestCases().values();
			Iterator<TestCase> iter2 = c2.iterator();
			while (iter2.hasNext()) {
				TestCase tc = iter2.next();
				noUniqueTestCases.add(tc.getFullName());
			}
		}
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
		TestPackage matchingTp = getTrendTestRun(trendInterval);

		if (null != matchingTp) {
			double diff = Double.parseDouble(spread.get(totalNumberOfTestRuns)
					.getPassPct())
					- Double.parseDouble(matchingTp.getPassPct());

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
		TestPackage matchingTp = getTrendTestRun(trendInterval);
		if (null != matchingTp) {
			TestPackage tpAtEnd = spread.get(totalNumberOfTestRuns);
			if (null != tpAtEnd) {
				int diff = spread.get(totalNumberOfTestRuns).getNoTestCases()
						- matchingTp.getNoTestCases();

				String ret = Integer.toString(diff);
				if (0 < diff) {
					ret = "+" + ret;
				}
				return ret;
			}
		}
		return "NA";
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
		TestPackage matchingTp = getTrendTestRun(trendInterval);
		if (null != matchingTp) {
			TestPackage tpAtEnd = spread.get(totalNumberOfTestRuns);
			if (null != tpAtEnd) {
				int diff = spread.get(totalNumberOfTestRuns).getNoNonPassing()
						- matchingTp.getNoNonPassing();

				String ret = Integer.toString(diff);
				if (0 < diff) {
					ret = "+" + ret;
				}
				return ret;
			}
		}
		return "NA";
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
	private TestPackage getTrendTestRun(long trendInterval) {
		long currentTime = System.currentTimeMillis();
		long breakPoint = currentTime - trendInterval;

		// It might be necessary to run through all runs in case there might be
		// an
		// occasion where the modules are not sorted.

		for (int i = totalNumberOfTestRuns - 1; i > 0; i--) {
			TestPackage tp = spread.get(i);

			if (tp != null) {
				long runTimeStamp = tp.getRunDateAsLong();

				if (breakPoint > runTimeStamp) {
					return tp;
				}
			}
		}
		return null;
	}
	
	/*
	 * Returns the diff in pass rate between the last module run and the last
	 * executed module run before that.
	 * 
	 * @param runs The number of runs in the history since we need an offset to
	 * count from.
	 * @param idx The index of the run to match with.
	 * @return The pass rate difference as string.
	 */
	protected String getPrDiff(int runs, int idx) {
		TestPackage tpLast = spread.get(runs); // Latest
		TestPackage tpFirst = null;
		
		if (null == tpLast) {
			return "NA";
		}
		
		// Loop from the start until a run is found and then increase
		// the counter
		int ctr = 0;
		for (int i=1; ctr<=spread.size(); i++) {
			if ((runs-i)<0) {
				break;
			}
			tpFirst = spread.get(runs - i);
			if (null != tpFirst) {
				ctr++;
				if (ctr==idx) {
					break;
				}
			}
		}
		if (null == tpFirst) {
			return "NA";
		}

		double diff = Double.parseDouble(tpLast.getPassPct())
				- Double.parseDouble(tpFirst.getPassPct());
		String ret = TestItemUtils.passPctToString(diff) + "%";
		if (0 < diff) {
			ret = "+" + ret;
		}
		return ret;
	}
	
	/**
	 * Used for incrementing the flat summary structure so that it look s like a tree.
	 * @param tp TestPackage to get the contents from.
	 */
	public void addFrom(TestPackageSummary tps) {
		addTestModuleNames(tps);
		addTestCaseNames(tps);
		
		if (tps.getNoUniqueTestModules() > 0) {
			containsTestModules = true;
		}
			
		// Largest number of test cases
		if (tps.getNoTestCases() > largestNoOfTestCases) {
			largestNoOfTestCases = tps.getNoTestCases();
		}
			
		// Largest number of failed test cases
		int noFailuresHolder = tps.getNoFailures(); 
		if (noFailuresHolder > largestNumberOfFailures) {
			largestNumberOfFailures = noFailuresHolder;
		}

		// Largest number of error test cases
		int noErrorsHolder = tps.getNoErrors(); 
		if (noErrorsHolder > largestNumberOfErrors) {
			largestNumberOfErrors = noErrorsHolder;
		}

		// Needed for the calculation of the negative graph. 
		if (noFailuresHolder+noErrorsHolder > largestNumberOfNonPass) {
			largestNumberOfNonPass = noFailuresHolder+noErrorsHolder; 
		}
		
		// Largest execution time
		if (tps.getExecutionTimeDouble() > largestExecutionTime) {
			largestExecutionTime = tps.getExecutionTimeDouble();
		}
	}
	
	private void addTestModuleNames(TestPackageSummary tps) {
		noUniqueTestModules.addAll(tps.noUniqueTestModules);
	}
	
	private void addTestCaseNames(TestPackageSummary tps) {
		noUniqueTestCases.addAll(tps.noUniqueTestCases);
	}
}
