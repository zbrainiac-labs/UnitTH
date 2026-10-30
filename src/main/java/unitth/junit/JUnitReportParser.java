/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.io.File;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import unitth.core.UnitTHConfig;
import unitth.core.UnitTHException;
import unitth.jenkins.JenkinsReportParser;

/**
 * This class is responsible for handling all the parsing of all JUnit files. It
 * extends the <code>DefaultHandler</code> and the methods needed for parsing
 * the XML in the JUnit reports.
 * 
 */
public class JUnitReportParser extends SaxReportParser {

	private final String c_XML_TAG_TESTSUITE = "testsuite";
	private final String c_XML_TAG_TESTCASE = "testcase";
	private final String c_XML_TAG_FAILURE = "failure";
	private final String c_XML_TAG_FAILURES = "failures";
	private final String c_XML_TAG_SKIPPED = "skipped";
	private final String c_XML_TAG_ERROR = "error";
	private final String c_XML_TAG_TESTS = "tests";
	private final String c_XML_TAG_TIME = "time";
	private final String c_XML_TAG_NAME = "name";
	private final String c_XML_TAG_PROPERTY = "property";
	private final String c_XML_TAG_TIMESTAMP = "timestamp";
	private final String c_XML_TAG_LAUNCH_TIMESTAMP = "launch.timestamp";
	private final String c_XML_TAG_PROPERTY_VALUE = "value";
	private final String c_XML_TAG_PROPERTY_NAME = "name";
	private final String c_XML_TAG_IGNORE = "skipped";

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

		if (c_XML_TAG_TESTSUITE.equals(eName)) {

			currentTestModule = new TestModule();
			if (attrs.getIndex(c_XML_TAG_TIMESTAMP) != -1) {
				currentTestModule.setDate(attrs.getValue(c_XML_TAG_TIMESTAMP));
			}

			String c_XML_TAG_ERRORS = "errors";
			currentTestModule.setNoErrors(attrs.getValue(c_XML_TAG_ERRORS));
			currentTestModule.setNoFailures(attrs.getValue(c_XML_TAG_FAILURES));
			currentTestModule.setNoIgnored(attrs.getValue(c_XML_TAG_SKIPPED));
			currentTestModule.setNoTestCases(attrs.getValue(c_XML_TAG_TESTS)); // Will be overwritten if the class has been ignored
			currentTestModule.setExecutionTime(attrs.getValue(c_XML_TAG_TIME));
			currentTestModule.setName(attrs.getValue(c_XML_TAG_NAME));

			if (config.debug()) {
				System.out.println("The attributes of the testsuite element.");
				System.out.println("errors: " + attrs.getValue("errors"));
				System.out.println("failures: " + attrs.getValue("failures"));
				System.out.println("name: " + attrs.getValue("name"));
				System.out.println("tests: " + attrs.getValue("tests"));
				System.out.println("time: " + attrs.getValue("time"));
				System.out.println("timestamp: " + attrs.getValue("timestamp")
						+ "\n");
			}
		} else if (c_XML_TAG_PROPERTY.equals(eName)) {
			String str = attrs.getValue(c_XML_TAG_PROPERTY_NAME);
			if (null != str) {
				if (str.equals(c_XML_TAG_LAUNCH_TIMESTAMP)
						&& null == currentTestModule.getExecutionDate()) {
					currentTestModule.setDate(attrs
							.getValue(c_XML_TAG_PROPERTY_VALUE));
				}
			}
		} else if (c_XML_TAG_TESTCASE.equals(eName)) {
			if (!attrs.getValue("name").equalsIgnoreCase("initializationError")) {
				currentTestCase = new TestCase();
				currentTestCase.setName(attrs.getValue("name"));
				// If a class name has not been provided for the test case the
				// test
				// module name can be used instead.
				currentTestCase
						.setClassName(attrs.getValue("classname") != null ? attrs
								.getValue("classname")
								: currentTestModule.getName());
				currentTestCase.setModuleName(currentTestModule.getName());
				currentTestCase.setExecutionTime(attrs.getValue("time"));
			}
		} else if (c_XML_TAG_FAILURE.equals(eName)) {
			currentTestCase.setVerdict(TestCaseVerdict.e_FAIL);
		} else if (c_XML_TAG_ERROR.equals(eName)) {
			currentTestCase.setVerdict(TestCaseVerdict.e_ERROR);
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
		if (c_XML_TAG_TESTSUITE.equals(eName)) {
			// void
		} else if (c_XML_TAG_TESTCASE.equals(eName)) {
			if (null != currentTestCase) {
				currentTestModule.addTestCase(currentTestCase);
			}
		} else if (c_XML_TAG_IGNORE.equals(eName)) {
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
				System.err.println("The file '"+file.getAbsolutePath()+"' did not get parsed sucessfully. Expected a JUnit report XML-file.");
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

			addToPackage(currentTestModule);
			if (config.debug()) {
				System.out.println("Module: " + currentTestModule.getName()
						+ " added to run: " + currentTestModule.toString());
			}

			// currentTestRun.addTestPackage(parsedTestPackages);
			currentTestModule = null;
		} catch (UnitTHException jte) {
			// If we end up here the test module will never be added
			System.err
					.println("Test module never added to test run since the parsed file ("
							+ file.getAbsolutePath() + ") contained errors.");
			System.err
			.println(jte.getMessage());
		}
	}
}

/* eof */
