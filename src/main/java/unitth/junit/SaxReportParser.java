/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import unitth.core.Log;
import unitth.core.ReportParser;
import unitth.core.UnitTHConfig;
import unitth.core.UnitTHException;

/**
 * Common run/file bookkeeping for the SAX based report parsers. Subclasses
 * implement the SAX callbacks and {@link #parseFile(File)}.
 */
public abstract class SaxReportParser extends ReportParser {

	private SAXParser saxp = null;

	protected final TestHistory history;
	protected TestRun currentTestRun = null;
	protected HashMap<String, TestPackage> parsedTestPackages = null;

	protected SaxReportParser(UnitTHConfig config) {
		super(config);
		history = new TestHistory(config);
		try {
			saxp = newSecureFactory().newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			Log.error("Could not create SAX parser", e);
		}
	}

	/** SAX factory with DTD/external entity loading disabled (XXE hardening). */
	private static SAXParserFactory newSecureFactory()
			throws ParserConfigurationException, SAXException {
		SAXParserFactory f = SAXParserFactory.newInstance();
		f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		f.setFeature("http://xml.org/sax/features/external-general-entities", false);
		f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return f;
	}

	/** Parses one file in the current run. */
	protected abstract void parseFile(File file);

	/** Runs the SAX parser on a file with this handler. */
	protected void parse(File file) throws UnitTHException {
		if (saxp == null) {
			throw new UnitTHException("No SAX parser available");
		}
		try {
			saxp.parse(file, this);
		} catch (IOException ioe) {
			throw new UnitTHException("IO errors occured... " + ioe.getMessage());
		} catch (SAXException saxe) {
			throw new UnitTHException("SAXException ..." + saxe.getMessage());
		} catch (RuntimeException e) {
			Log.error("Unexpected error while parsing", e);
			throw new UnitTHException("Unknown exception...");
		}
	}

	/**
	 * Parses all runs; each inner list holds the files of one run folder.
	 *
	 * @return The filled <code>TestHistory</code>.
	 */
	public TestHistory parseRuns(ArrayList<ArrayList<File>> testRunFiles,
			ArrayList<String> dirs) {
		for (int i = 0; i < testRunFiles.size(); i++) {
			currentTestRun = new TestRun(config);
			parsedTestPackages = new HashMap<String, TestPackage>();
			for (File f : testRunFiles.get(i)) {
				parseFile(f);
			}
			currentTestRun.setRunPath(dirs.get(i));
			currentTestRun.addTestPackages(parsedTestPackages);
			try {
				history.addTestRun(currentTestRun);
			} catch (UnitTHException jthe) {
				Log.error("Test run skipped", jthe);
			}
			currentTestRun = null;
			parsedTestPackages = null;
		}
		return history;
	}

	public void addToPackage(TestModule tm) {
		parsedTestPackages.computeIfAbsent(tm.getPackageName(), name -> {
			TestPackage tp = new TestPackage();
			tp.setName(name);
			return tp;
		}).addTestModule(tm);
	}
}
