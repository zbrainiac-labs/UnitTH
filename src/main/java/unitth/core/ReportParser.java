/*
 * MIT License - see LICENSE.txt
 */
package unitth.core;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.helpers.DefaultHandler;

import unitth.junit.TestHistory;
import unitth.junit.TestModule;
import unitth.junit.TestPackage;
import unitth.junit.TestRun;

/**
 * Common run/file bookkeeping of all report parsers. Subclasses parse one file
 * at a time into the packages of the current run. It extends
 * <code>DefaultHandler</code> so SAX based parsers can be their own handler.
 */
public abstract class ReportParser extends DefaultHandler {

	protected final UnitTHConfig config;
	protected final TestHistory history;
	protected TestRun currentTestRun = null;
	protected HashMap<String, TestPackage> parsedTestPackages = null;

	protected ReportParser(UnitTHConfig config) {
		this.config = config;
		this.history = new TestHistory(config);
	}

	public UnitTHConfig getConfig() {
		return config;
	}

	/** Parses one file into the packages of the current run. */
	protected abstract void parseFile(File file);

	/**
	 * Parses all runs; each inner list holds the files of one run folder.
	 *
	 * @return The filled <code>TestHistory</code>.
	 */
	public TestHistory parseRuns(List<? extends List<File>> testRunFiles, List<String> dirs) {
		for (int i = 0; i < testRunFiles.size(); i++) {
			history.addTestRun(parseRun(testRunFiles.get(i), dirs.get(i)));
		}
		return history;
	}

	/** Parses the files of one run folder into a new run. */
	public TestRun parseRun(List<File> files, String dir) {
		currentTestRun = new TestRun(config);
		parsedTestPackages = new HashMap<>();
		for (File f : files) {
			parseFile(f);
		}
		TestRun run = currentTestRun;
		run.setRunPath(dir);
		run.addTestPackages(parsedTestPackages);
		currentTestRun = null;
		parsedTestPackages = null;
		return run;
	}

	/** Adds a parsed module to its package in the current run. */
	public void addToPackage(TestModule tm) {
		addToPackage(parsedTestPackages, tm);
	}

	public static void addToPackage(Map<String, TestPackage> packages, TestModule tm) {
		packages.computeIfAbsent(tm.getPackageName(), name -> {
			TestPackage tp = new TestPackage();
			tp.setName(name);
			return tp;
		}).addTestModule(tm);
	}
}
