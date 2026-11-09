/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.io.File;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import unitth.core.UnitTHConfig;
import unitth.core.Log;
import unitth.core.UnitTHException;

/**
 * This class is responsible for handling all the parsing of all JUnit files. It
 * extends the <code>DefaultHandler</code> and the methods needed for parsing
 * the XML in the JUnit reports.
 * 
 */
public class JUnitReportParser extends SaxReportParser {

	private static final String TAG_TESTSUITE = "testsuite";
	private static final String TAG_TESTCASE = "testcase";
	private static final String TAG_FAILURE = "failure";
	private static final String TAG_FAILURES = "failures";
	private static final String TAG_SKIPPED = "skipped";
	private static final String TAG_ERROR = "error";
	private static final String TAG_ERRORS = "errors";
	private static final String TAG_TESTS = "tests";
	private static final String TAG_TIME = "time";
	private static final String TAG_NAME = "name";
	private static final String TAG_PROPERTY = "property";
	private static final String TAG_TIMESTAMP = "timestamp";
	private static final String TAG_LAUNCH_TIMESTAMP = "launch.timestamp";
	private static final String TAG_PROPERTY_VALUE = "value";

	/* Currently parsed element holders. */
	private TestModule currentTestModule = null;
	private TestCase currentTestCase = null;

	/* CTRF JSON files in a run folder are handled by the CTRF parser. */
	private final CTRFReportParser ctrfParser = new CTRFReportParser(config);

	/**
	 * The ReportParser Ctor, creates a new parser and initializes the test
	 * history to put all the parsed contents into.
	 */
	public JUnitReportParser(UnitTHConfig config) {
		super(config);
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

		String eName = sName; // element name

		// We do not know which name was used, simple or qualified
		if ("".equals(eName)) {
			eName = qName; // not namespace-aware
		}

		if (TAG_TESTSUITE.equals(eName)) {
			startTestSuite(attrs);
		} else if (TAG_PROPERTY.equals(eName)) {
			String str = attrs.getValue(TAG_NAME);
			if (TAG_LAUNCH_TIMESTAMP.equals(str) && null == currentTestModule.getExecutionDate()) {
				currentTestModule.setDate(attrs.getValue(TAG_PROPERTY_VALUE));
			}
		} else if (TAG_TESTCASE.equals(eName)) {
			startTestCase(attrs);
		} else if (TAG_FAILURE.equals(eName)) {
			currentTestCase.setVerdict(TestCaseVerdict.e_FAIL);
		} else if (TAG_ERROR.equals(eName)) {
			currentTestCase.setVerdict(TestCaseVerdict.e_ERROR);
		}
	}

	private void startTestSuite(Attributes attrs) {
		currentTestModule = new TestModule();
		if (attrs.getIndex(TAG_TIMESTAMP) != -1) {
			currentTestModule.setDate(attrs.getValue(TAG_TIMESTAMP));
		}

		currentTestModule.setNoErrors(attrs.getValue(TAG_ERRORS));
		currentTestModule.setNoFailures(attrs.getValue(TAG_FAILURES));
		currentTestModule.setNoIgnored(attrs.getValue(TAG_SKIPPED));
		currentTestModule.setNoTestCases(attrs.getValue(TAG_TESTS)); // Will be overwritten if the class has been ignored
		currentTestModule.setExecutionTime(attrs.getValue(TAG_TIME));
		currentTestModule.setName(attrs.getValue(TAG_NAME));

		if (config.debug()) {
			Log.out("The attributes of the testsuite element.");
			for (String tag : new String[] { TAG_ERRORS, TAG_FAILURES, TAG_NAME, TAG_TESTS, TAG_TIME, TAG_TIMESTAMP }) {
				Log.out(tag + ": " + attrs.getValue(tag));
			}
		}
	}

	private void startTestCase(Attributes attrs) {
		if (attrs.getValue(TAG_NAME).equalsIgnoreCase("initializationError")) {
			return;
		}
		currentTestCase = new TestCase();
		currentTestCase.setName(attrs.getValue(TAG_NAME));
		// If a class name has not been provided for the test case the
		// test module name can be used instead.
		String className = attrs.getValue("classname");
		currentTestCase.setClassName(className != null ? className : currentTestModule.getName());
		currentTestCase.setModuleName(currentTestModule.getName());
		currentTestCase.setExecutionTime(attrs.getValue(TAG_TIME));
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
		if (TAG_TESTSUITE.equals(eName)) {
			// void
		} else if (TAG_TESTCASE.equals(eName)) {
			if (null != currentTestCase) {
				currentTestModule.addTestCase(currentTestCase);
			}
		} else if (TAG_SKIPPED.equals(eName)) {
			if (null == currentTestCase) {
				// <skipped/> outside a <testcase>: nothing to mark
				return;
			}
			currentTestCase.setVerdict(TestCaseVerdict.e_IGNORED);

			// Entire class has been ignored
			if (null != currentTestModule
					&& currentTestCase.getName().equals(currentTestModule.getName())) {
				currentTestModule.setAsIgnored();
				currentTestModule.setNoTestCases("0"); // TODO, valid fix or not
			}
		}
	}

	/**
	 * Parses a file and adds the statistics to a <code>TestPackage</code>.
	 * 
	 * @param file
	 *            The file to be parsed.
	 */
	@Override
	protected void parseFile(File file) {

		if (config.debug()) {
			System.out.println("Parsing file: " + file.getName());
		}

		if (file.getName().endsWith(".json") || file.getName().endsWith(".JSON")) {
			ctrfParser.parseFile(file, parsedTestPackages);
			return;
		}

		try {
			parse(file);

			// Check if anything has been parsed at all.
			if (currentTestModule==null) {
				Log.error("The file '" + file.getAbsolutePath() + "' did not get parsed sucessfully. Expected a JUnit report XML-file.");
				return;
			}
			
			// If there are no time stamps included in the report format the
			// last modification time of the parsed file will be used.
			if (null == currentTestModule.getExecutionDate()) {
				String folderDate = CTRFReportParser.parseFolderDate(file.getParentFile().getName());
				if (folderDate != null) {
					currentTestModule.setDate(folderDate);
				} else {
					long fileLastModified = file.lastModified();
					currentTestModule.setDate(fileLastModified);
				}
			}

			if (config.skipCommentTests()) {
				currentTestModule.removeCommentTests();
			}
			addToPackage(currentTestModule);
			if (config.debug()) {
				System.out.println("Module: " + currentTestModule.getName()
						+ " added to run: " + currentTestModule.toString());
			}

			currentTestModule = null;
		} catch (UnitTHException jte) {
			// If we end up here the test module will never be added
			Log.error("Test module never added to test run since the parsed file ("
					+ file.getAbsolutePath() + ") contained errors: " + jte.getMessage());
		}
	}
}

/* eof */
