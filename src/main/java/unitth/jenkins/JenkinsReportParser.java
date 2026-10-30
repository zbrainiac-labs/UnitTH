/*
 * MIT License - see LICENSE.txt
 */
package unitth.jenkins;

import java.io.File;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import unitth.core.UnitTHConfig;
import unitth.core.UnitTHException;
import unitth.junit.TestCase;
import unitth.junit.TestCaseVerdict;
import unitth.junit.TestHistory;
import unitth.junit.TestModule;
import unitth.junit.TestPackage;
import unitth.junit.SaxReportParser;

/**
 * This class is responsible for handling all the parsing of all Jenkins report files. It
 * extends the <code>DefaultHandler</code> and the methods needed for parsing
 * the XML in the Jenkins reports.
 * 
 */
public class JenkinsReportParser extends SaxReportParser {

	private final String c_XML_TAG_TESTSUITE = "name";
	private final String c_XML_TAG_TESTSUITE_END = "suite";
	private final String c_XML_TAG_TESTCASE_NAME = "testName";
	private final String c_XML_TAG_TESTCASE = "case";
	private final String c_XML_TAG_CLASSNAME = "className";
	private final String c_XML_TAG_VERDICT = "failedSince";
	private final String c_XML_TAG_DURATION = "duration";
	private final String c_XML_TAG_LAUNCH_TIMESTAMP = "timestamp";

	private boolean inTestCase = false;

	/* Currently parsed element holders. */
	private String currentElement = "";
	private TestModule currentTestModule = null;
	private TestCase currentTestCase = null;

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
	public JenkinsReportParser(UnitTHConfig config) {
		super(config);
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
		try {
			parse(file);

			// If there are no time stamps included in the report format the
			// last modification time of the parsed file will be used.
		} catch (UnitTHException jte) {
			System.err.println(jte.getMessage());
		}
	}
}

/* eof */
