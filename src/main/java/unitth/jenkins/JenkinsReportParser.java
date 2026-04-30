/*
 * MIT License - see LICENSE.txt
 */
package unitth.jenkins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import unitth.core.ReportParser;
import unitth.core.UnitTH;
import unitth.core.UnitTHException;
import unitth.junit.TestCase;
import unitth.junit.TestCaseVerdict;
import unitth.junit.TestHistory;
import unitth.junit.TestModule;
import unitth.junit.TestPackage;
import unitth.junit.TestRun;

/**
 * This class is responsible for handling all the parsing of all Jenkins report files. It
 * extends the <code>DefaultHandler</code> and the methods needed for parsing
 * the XML in the Jenkins reports.
 * 
 */
public class JenkinsReportParser extends ReportParser {

	/** The SAX parser factory used for reading in the report files. */
	private static SAXParserFactory saxFactory = SAXParserFactory.newInstance();
	/** The SAXParser instance used for all the parsing. */
	private SAXParser saxp = null;

	private final String c_XML_TAG_TESTSUITE = "name";
	private final String c_XML_TAG_TESTSUITE_END = "suite";
	private final String c_XML_TAG_TESTCASE_NAME = "testName";
	private final String c_XML_TAG_TESTCASE = "case";
	private final String c_XML_TAG_CLASSNAME = "className";
	private final String c_XML_TAG_VERDICT = "failedSince";
	private final String c_XML_TAG_DURATION = "duration";
	private final String c_XML_TAG_LAUNCH_TIMESTAMP = "timestamp";

	private int currentRunIdx = 0;
	private boolean inTestCase = false;

	/* Currently parsed element holders. */
	private String currentElement = "";
	private TestRun currentTestRun = null;
	private TestModule currentTestModule = null;
	private TestCase currentTestCase = null;
	private TestHistory history = null;
	private HashMap<String, TestPackage> parsedTestPackages = null;

	private StringBuffer moduleName = null;
	private StringBuffer moduleDuration = null;
	private StringBuffer moduleTimestamp = null;
	private StringBuffer tcName = null;
	private StringBuffer tcClass = null;
	private StringBuffer tcVerdict = null;
	private StringBuffer tcDuration = null;
	
	/**
	 * The ReportParser Ctor, creates a new parser and initializes the test
	 * history to put all the parsed contents into.
	 */
	public JenkinsReportParser() {
		try {
			saxp = saxFactory.newSAXParser();
			history = new TestHistory();
		} catch (SAXException t) {
			System.err.println("Could not create SAX parser... "+t.getMessage());
			return;
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.println("Unknown exception... "+t.getMessage());
			return;
		}
	}

	public void characters(char ch[], int start, int length)
			throws SAXException {
		
		if (currentElement.equals(c_XML_TAG_TESTSUITE)) {
			moduleName.append(new String(ch, start, length));
		} else if (currentElement.equals(c_XML_TAG_DURATION)) {
			if (inTestCase) {
				tcDuration.append(new String(ch, start, length));
			} else {
				moduleDuration.append(new String(ch, start, length));
			}
		} else if (currentElement.equals(c_XML_TAG_LAUNCH_TIMESTAMP)) {
			moduleTimestamp.append(new String(ch, start, length));
		} else if (currentElement.equals(c_XML_TAG_TESTCASE_NAME)) {
			tcName.append(new String(ch, start, length));
		} else if (currentElement.equals(c_XML_TAG_CLASSNAME)) {
			tcClass.append(new String(ch, start, length));
		} else if (currentElement.equals(c_XML_TAG_VERDICT)) {
			tcVerdict.append(new String(ch, start, length));
		}
	}
	
	/**
	 * This method only bothers about the few elements that are needed for the
	 * collection of statistics, Testsuite, Testcase.
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String sName, String qName,
			Attributes attrs) throws SAXException {

		currentElement = sName; // element name

		// We do not know which name was used, simple or qualified
		if ("".equals(currentElement)) {
			currentElement = qName; // not namespace-aware
		}
		if (currentElement.equals(c_XML_TAG_TESTCASE)) {
			inTestCase = true;
			currentTestCase = new TestCase();
			
			tcName = new StringBuffer();
			tcClass = new StringBuffer();
			tcVerdict = new StringBuffer();
			tcDuration = new StringBuffer();
		}
		
		if (currentElement.equals(c_XML_TAG_TESTSUITE)) {
			currentTestModule = new TestModule();
			
			// Creating buffers for the characters calls to fill in.
			moduleName = new StringBuffer();
			moduleDuration = new StringBuffer();
			moduleTimestamp = new StringBuffer();
		}
	}

	/**                            
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String sName, String qName)
			throws SAXException {

		String eName = sName; // element name

		// We do not know which name was used, simple or qualified
		if ("".equals(eName)) {
			eName = qName; // not namespace-aware
		}
		
		if (c_XML_TAG_TESTSUITE_END.equals(eName)) {
			currentTestModule.setName(moduleName.toString().trim());
			currentTestModule.setDate(moduleTimestamp.toString().trim());
			currentTestModule.setExecutionTime(moduleDuration.toString().trim());
			
			String packageName = moduleName.toString().trim();
			if (packageName.contains(".")) {
				packageName = packageName.substring(0, packageName.lastIndexOf("."));
			}
			
			TestPackage tp = null;
			if (!parsedTestPackages.containsKey(packageName)) {
				tp = new TestPackage();
				tp.setName(packageName);
				parsedTestPackages.put(packageName, tp);
			}
			
			addToPackage(currentTestModule);
			//currentTestModule.calcStats();
			currentTestRun.addTestPackages(parsedTestPackages);
			currentTestModule = null;
		} 
		
		
		else if (c_XML_TAG_TESTCASE.equals(eName)) {
			currentTestCase.setName(tcName.toString().trim());
			currentTestCase.setClassName(tcClass.toString().trim());
			currentTestCase.setModuleName(currentTestModule.getName());
			currentTestCase.setExecutionTime(tcDuration.toString().trim());
			if (tcVerdict.toString().trim().equals("0")) {
				currentTestCase.setVerdict(TestCaseVerdict.e_PASS);
			} else {
				currentTestCase.setVerdict(TestCaseVerdict.e_FAIL);
				int fails = currentTestModule.getNoFailures()+1; 
				currentTestModule.setNoFailures(Integer.toString(fails));
			}
			int no = currentTestModule.getNoTestCases()+1;
			currentTestModule.setNoTestCases(Integer.toString(no));
			currentTestModule.addTestCase(currentTestCase);
			inTestCase = false;
		}
	}

	/**
	 * This method calls the SAXParser parse method.
	 * 
	 * @param file
	 *            The file to be parsed.
	 * @throws UnitTHException
	 */
	private void parse(File file) throws UnitTHException {
		try {
			saxp.parse(file, this);
		} catch (IOException ioe) {
			// ioe.printStackTrace();
			System.err.println("IO errors occured...");
			throw new UnitTHException("IO errors occured...");
		} catch (SAXException saxe) {
			throw new UnitTHException("SAXException ..."+saxe.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unknown exception...");
			throw new UnitTHException("Unknown exception...");
		}
	}

	/**
	 * Initiates the parsing of all runs. The method also sets an index for the
	 * run.
	 * 
	 * @param testRunFiles
	 *            A list of files to be parsed.
	 * @param dirs
	 *            A list of folders where the files to e parsed can be found.
	 * @return The filled <code>RunHistory</code> data structure.
	 */
	public TestHistory parseRuns(ArrayList<ArrayList<File>> testRunFiles,
			ArrayList<String> dirs) {

		currentRunIdx = 1; // Counter for the directories list.
		for (ArrayList<File> files : testRunFiles) {
			try {
				currentTestRun = new TestRun();
				parsedTestPackages = new HashMap<String, TestPackage>();
				// Populate the current test run object and return
				// the relative path to the parsed files
				parseFiles(files);
				// -1 since the runs are in an array starting at ZERO
				currentTestRun.setRunPath(dirs.get(currentRunIdx - 1));
				currentTestRun.addTestPackages(parsedTestPackages);
				history.addTestRun(currentTestRun);
				currentTestRun = null;
				parsedTestPackages = null;
			} catch (UnitTHException jthe) {
				jthe.printStackTrace();
			}
			currentRunIdx++;
		}
		return history;
	}

	/**
	 * Calls parseFile for each of the files in a run.
	 * 
	 * @see JenkinsReportParser.parseFile
	 * 
	 * @param files
	 *            The files to be parsed
	 */
	private void parseFiles(ArrayList<File> files) {
		for (File f : files) {
			parseFile(f);
		}
	}

	/**
	 * Parses a file and adds the statistics to a <code>TestPackage</code>.
	 * 
	 * @param file
	 *            The file to be parsed.
	 */
	private void parseFile(File file) {

		if (UnitTH.c_DBG) {
			System.out.println("Parsing file: " + file.getName());
		}
		try {
			parse(file);

			// If there are no time stamps included in the report format the
			// last modification time of the parsed file will be used.
		} catch (UnitTHException jte) {
			System.err.println(jte.getMessage());
		}
	}

	public void addToPackage(TestModule tm) {

		String packageName = tm.getPackageName();

		TestPackage tp = parsedTestPackages.get(packageName);

		// Package does not exist, creating it.
		if (tp == null) {
			tp = new TestPackage();
			tp.setName(packageName);
			parsedTestPackages.put(packageName, tp);
		}
		tp.addTestModule(tm);
	}
}

/* eof */
