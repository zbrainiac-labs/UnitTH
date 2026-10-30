/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

import unitth.core.Log;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import unitth.core.CustomStringLengthComparator;
import unitth.core.TestItemUtils;
import unitth.core.UnitTHConfig;

/**
 * This class implements the functionality for the test run object. It stores
 * all the statistics parsed for an executed run.
 * 
 */
public class TestRun extends TestItem implements Comparable<TestRun> {

	/* runDate (lowest module time stamp) is inherited from TestItem. */
	/* The <code>HashMap</code> holding all the test packages in this test run. */
	private HashMap<String, TestPackage> testPackages;

	private int noTestModules = 0;
	/*
	 * The number of test cases in this test run. This is the sum of all
	 * executed test cases in all runs.
	 */
	private int noTestCases = 0;
	/*
	 * The number of error test cases in this test run. This is the sum of all
	 * executed test cases that have ended up with an error verdict in all runs.
	 */
	private int noErrors = 0;
	/*
	 * The number of failed test cases in this test run. This is the sum of all
	 * executed test cases that have ended up with a failed verdict in all runs.
	 */
	private int noFailures = 0;
	/*
	 * The number of passed test cases in this test run. This is the sum of all
	 * executed test cases that have ended up with a pass verdict in all runs.
	 */
	private int noPassed = 0;
	/*
	 * The number of ignored test cases in this test run. This is the sum of all
	 * executed test cases that have ended up with a ignored verdict in all runs.
	 */
	private int noIgnored = 0;
	/*
	 * This is the relative run path of this test run. This is the location
	 * where all the files belonging to this run can be found.
	 */
	private String runPath = "";
	/*
	 * This is the relative run path of this test run, relative to the report
	 * output folder.
	 */
	private String relativePath = "";
	/*
	 * This String contains the absolute path to the index.html file in the
	 * JUnit reports.
	 */
	private String absolutePath = "";
	/* The run index of this test run. */
	private int runIdx = 0;
	/* Used when creating the relative path for this test run. */
	private final String INDEX_HTML = "index.html";

	private double largestExecutionTime = 0.0;

	/**
	 * CTOR, initializes the list of test modules belonging to this run.
	 */
	public TestRun(UnitTHConfig config) {
		this.config = config;
		testPackages = new HashMap<String, TestPackage>();
	}

	private final UnitTHConfig config;

	/**
	 * Does a comparison based on the run stamp which should be unique.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(TestRun tr) {
		if (tr != null && tr.runDate != null) {
			int v1 = tr.runDate.compareTo(runDate);
			int v2 = 0;

			if (!runPath.equalsIgnoreCase("")
					&& !tr.runPath.equalsIgnoreCase("")) {
				v2 = tr.runPath.compareTo(runPath);
			}

			if (0 == v1) {
				return v2;
			} else {
				return v1;
			}

		}
		return -1;
	}

	/**
	 * Returns the run date for this test run.
	 * 
	 * @return The run date of this test run.
	 */
	public String getExecutionDate() {
		return runDate;
	}

	/**
	 * Returns the list of test modules in this test run.
	 * 
	 * @return The modules associated to this run.
	 */
	public HashMap<String, TestModule> getTestModules() {

		HashMap<String, TestModule> tms = new HashMap<String, TestModule>();

		Collection<TestPackage> c = testPackages.values();
		Iterator<TestPackage> iter = c.iterator();

		while (iter.hasNext()) {
			TestPackage tp = iter.next();
			tms.putAll(tp.getTestModules());
		}
		return tms;
	}

	/**
	 * Returns a test module in this run based on its name.
	 * 
	 * @param name
	 *            The name of the test module to get.
	 * @return The looked for test module.
	 */
	public TestModule getTestModule(String packageName, String name) {
		TestPackage tp = testPackages.get(packageName);
		if (null != tp) {
			return tp.getTestModule(name);
		}
		return null; // Return of null is expected and handled.
	}

	/**
	 * Returns the list of test packages in this test run.
	 * 
	 * @return The packages associated to this run.
	 */
	public HashMap<String, TestPackage> getTestPackages() {
		return testPackages;
	}

	/**
	 * Returns a test package in this run based on its name.
	 * 
	 * @param name
	 *            The name of the test package to get.
	 * @return The looked for test package.
	 */
	public TestPackage getTestPackage(String name) {
		return testPackages.get(name);
	}

	/**
	 * Returns the number of test cases executed in this test run.
	 * 
	 * @return The number of test cases executed in this test run.
	 */
	public int getNoTestCases() {
		return noTestCases;
	}

	/**
	 * Returns the number of error test cases in this test run.
	 * 
	 * @return The number of error test cases in this test run.
	 */
	public int getNoErrors() {
		return noErrors;
	}

	/**
	 * Returns the number of failed test cases in this test run.
	 * 
	 * @return The number of failed test cases in this test run.
	 */
	public int getNoFailures() {
		return noFailures;
	}

	/**
	 * Returns the number of passed test cases in this test run.
	 * 
	 * @return The number of passed test cases in this test run.
	 */
	public int getNoPassed() {
		return noPassed;
	}

	/**
	 * Returns the number of non passed test cases in this test run.
	 * 
	 * @return The number of non passed test cases in this test run.
	 */
	public int getNoNonPassing() {
		return noFailures + noErrors;
	}

	/**
	 * Returns the number of test modules that was executed in this test run.
	 * 
	 * @return The number of test modules.
	 */
	public int getNoTestModules() {
		return noTestModules;
	}

	/**
	 * Returns the relative path to this parsed test run, i.e. the path to where
	 * all the parsed xml files where.
	 * 
	 * @return The relative path to the files belonging to this test run.
	 */
	public String getRunPath() {
		return runPath;
	}

	/**
	 * Returns the stringified pass percentage. Three decimals.
	 * 
	 * @return The stringified pass percentage.
	 */
	public String getPassPct() {
		return TestItemUtils.passPctToString(passPct);
	}

	/**
	 * Returns the pass percentage as double. Three decimals.
	 * 
	 * @return The pass percentage.
	 */
	public double getPassPctDouble() {
		return passPct;
	}

	/**
	 * Returns the index of this run. This index is relative to the time stamp
	 * of the the test run.
	 * 
	 * @return The run index for this run.
	 */
	public int getRunIdx() {
		return runIdx;
	}

	/**
	 * This method calculates a long value from a stringified date. The method
	 * is primarily used for calculating trends based on time stamps.
	 * 
	 * @return The run date converted to long.
	 */
	public long getRunDateAsLong() {
		return TestItemUtils.runDateToLong(runDate);
	}

	/**
	 * This method does all the statistics calculations for this run. It calls
	 * the corresponding method for each of the test modules in this test run.
	 * 
	 * @see unitth.core.TestModule#calcStats()
	 */
	public void calcStats() {
		Collection<TestPackage> c = testPackages.values();
		Iterator<TestPackage> iter = c.iterator();

		while (iter.hasNext()) {
			TestPackage tp = iter.next();
			tp.calcStats();

			noTestCases += tp.getNoTestCases();
			noErrors += tp.getNoErrors();
			noFailures += tp.getNoFailures();
			noIgnored += tp.getNoIgnored();
			noTestModules += tp.getNoTestModulesThis();
			executionTime += tp.getExecutionTimeDouble();

			// If null or the new string is less than the set time stamp
			// Get the last run date, the formats are in YYYY-MM-DD
			String timeStampHolder = tp.getExecutionDate();
			if (null == runDate || runDate.isEmpty()
					|| 0 < runDate.compareToIgnoreCase(timeStampHolder)) {
				runDate = timeStampHolder;
			}

			// Largest execution time
			double largestExecutionTimeHolder = tp.getExecutionTimeDouble();
			if (largestExecutionTimeHolder > largestExecutionTime) {
				largestExecutionTime = largestExecutionTimeHolder;
			}
		}
		noPassed = noTestCases - noErrors - noFailures;
		if (0 > noPassed || 0 == noTestCases) {
			noPassed = 0;
			passPct = 0.0;
		} else {
			passPct = ((double) noPassed / (double) noTestCases) * 100;
		}

		//
		// Test packages are stored flat - lets fake a tree structure
		//
		// 1) Get a list of sorted keys.
		// 2) For every package where there is a sub match in name we increment.
		//
		String[] packageNames = testPackages.keySet().toArray(new String[0]);
		Arrays.sort(packageNames, new CustomStringLengthComparator());
		
		for (String s1 : packageNames) {
			for (String s2 : packageNames) {
				// We do not want to increment the same
				if (!s1.equalsIgnoreCase(s2)) {
					
					// Trim s1 one level!
					if (s1.contains(".")) {
						String parent = s1.substring(0, s1.lastIndexOf("."));
						if (parent.equalsIgnoreCase(s2)) {
							testPackages.get(s2).addFrom(testPackages.get(s1));
						}
					}
				}
			}
		}
		
		// All set, what has been parsed...
		if (config.debug()) {
			System.out.println("Parsed: tcs " + noTestCases + " e " + noErrors
					+ " f " + noFailures + " et " + executionTime + " rd "
					+ runDate);
		}
	}

	/**
	 * Sets the relative run path for this test run. This is where all the files
	 * belonging to a test run can be found.
	 * 
	 * @param path
	 *            The relative path as a string.
	 */
	public void setRunPath(String path) {
		File finput = new File(path);
		File foutput = new File(config.rootFolder());
		String sinput = finput.toURI().normalize().getPath();
		String soutput = foutput.toURI().normalize().getPath();
		runPath = calcRelativePath(sinput, soutput);
	}

	/**
	 * Returns the relative calculated path for any JUnit HTML report found.
	 * 
	 * @return The relative calculated path for any JUnit HTML report found.
	 */
	public String getRelativePath() {
		return relativePath;
	}

	/**
	 * The input needs to be absolute paths.
	 */
	public String calcRelativePath(String a, String b) {
		String ret = "";

		// Remove trailing '/'
		if (a.endsWith("/")) {
			a = a.substring(0, a.length() - 1);
		}
		if (b.endsWith("/")) {
			b = b.substring(0, b.length() - 1);
		}

		int shortest = (a.length() > b.length()) ? b.length() : a.length();

		int idxOfLastSlash = 0;
		for (int i = 1; i <= shortest; i++) { // i = 0 or i = 1 ????
			if (false == a.regionMatches(true, 0, b, 0, i + 1)) {
				if (idxOfLastSlash > 0) {
					a = a.substring(idxOfLastSlash + 1);
					b = b.substring(idxOfLastSlash + 1);
				}
				break;
			} else if (i == (shortest - 1)) {

				// Which case works where?
				a = a.substring(i + 1);
				b = b.substring(i + 1);

				// a = a.substring(i);
				// b = b.substring(i);
				break;
			}
			if (a.charAt(i) == '/') {
				idxOfLastSlash = i;
			}
		}

		// Tricky with short strings, check if the first char i an '/'
		if (b.startsWith("/")) {
			b = b.substring(1);
		}

		// Now calculate the number of '/'. The '/' are the result from the
		// normalization of the URI. Each '/' indicates at least two levels.
		int count = 1;
		for (char c : b.toCharArray()) {
			if ('/' == c) {
				count++;
			}
		}

		for (int i = 0; i < count; i++) {
			if (a.equals("")) {
				ret += "..";
			} else {
				ret += "../";
			}
		}
		ret += a;
		return ret;
	}

	/**
	 * Calculates any relative path to a JUnit HTML report if it exists. The
	 * relative path is as link in the generated HTML history.
	 * 
	 * @param destDir
	 *            The test history out put directory.
	 * @param junitHtmlReportPath
	 *            The relative path to the JUnit HTML reports from the root
	 *            folder of any parsed run.
	 */
	public void setRelativePathFromOutputDir(String destDir,
			String junitHtmlReportPath) {

		File theIndexFile = new File(destDir + File.separator + runPath
				+ File.separator + junitHtmlReportPath + File.separator
				+ INDEX_HTML);

		if (!theIndexFile.exists()) {
			if (config.debug()) {
				System.err
						.println("index.html file ("
								+ theIndexFile
								+ ") could not be found, no HTML report link will be created.");
			}
			return;
		}

		try {
			// There is a need for normalization of the paths so we need
			// URI objects before the final string to be manipulated.
			URI indexUri = theIndexFile.toURI();
			URI destUri = null;

			// First determine if the given output path is absolute or not
			if (new File(destDir).isAbsolute()) {
				destUri = new File(destDir).toURI();
			} else {
				destUri = new File(System.getProperty("user.dir")
						+ File.separator + destDir).toURI();
			}

			// Remove all "/./", "/../" and '\' -> '/'
			String indexPath = indexUri.normalize().getPath();
			String reportPath = destUri.normalize().getPath();

			relativePath = calcRelativePath(indexPath, reportPath);
		} catch (Exception e) {
			Log.error("Could not calculate the relative report path", e);
		}
	}

	public void setAbsolutePath(String destDir,
			String junitHtmlReportPath) {
		File theIndexFile = new File(destDir + File.separator + runPath
				+ File.separator + junitHtmlReportPath + File.separator
				+ INDEX_HTML);
		
		if (!theIndexFile.exists()) {
			if (config.debug()) {
				System.err
						.println("index.html file ("
								+ theIndexFile
								+ ") could not be found, no HTML report link will be created.");
			}
			return;
		}
		URI indexUri = theIndexFile.toURI();
		absolutePath = indexUri.normalize().getPath();
	}
	
	/**
	 * Sets the run index for this test run.
	 * 
	 * @param idx
	 *            The index to set.
	 */
	public void setRunIdx(int idx) {
		runIdx = idx;
		try {
			setName("Run-" + Integer.toString(idx));
		} catch (NumberFormatException nfe) {
			setName("Run-N");
		}
	}
	
	/**
	 * Adds a test package to this test run.
	 * 
	 * @param tp
	 *            The test package to add.
	 */
	public void addTestPackage(TestPackage tp) {
		if (testPackages != null && tp != null) {
			testPackages.put(tp.getName(), tp);
		}
	}

	/**
	 * Sets the run time stamp after formatting for this test run.
	 * 
	 * @param timeStamp
	 *            The time stamp string to format and set.
	 */
	public void setDate(String timeStamp) {
		runDate = TestItemUtils.fixDateFormat(timeStamp);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String buf = "-+- Test run -+-\n";
		buf += "Execution timestamp: " + runDate + "\n";
		buf += "No test packages: " + testPackages.size() + "\n";
		buf += "No test modules: " + noTestModules + "\n";
		buf += "No test cases: " + noTestCases + "\n";
		buf += "No errors: " + noErrors + "\n";
		buf += "No failures: " + noFailures + "\n";
		buf += "No passes: " + noPassed;
		if (testPackages.size() > 0) {
			buf += "\n";
		}

		Collection<TestPackage> c = testPackages.values();
		Iterator<TestPackage> iter = c.iterator();
		while (iter.hasNext()) {
			buf += iter.next().toString() + "\n";
		}
		return buf;
	}

	/**
	 * Returns the largest execution time in all test runs.
	 * 
	 * @return Returns the largest execution time in all test runs.
	 */
	public double getLargestExecutionTime() {
		return largestExecutionTime;
	}

	public void addTestPackages(HashMap<String, TestPackage> tps) {
		testPackages = tps;
	}
	
	public String getAbsolutePath() {
		return absolutePath;
	}
	
	public String getChosenPath() {
		if (config.useAbsPaths()) {
			return absolutePath;
		} else {
			return runPath;
		}
	}

	public int getNoIgnored() {
	    return noIgnored;
	}
}

/* eof */
