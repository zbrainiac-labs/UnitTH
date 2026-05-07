/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import unitth.core.ReportParser;
import unitth.core.UnitTH;
import unitth.core.UnitTHException;
import unitth.jenkins.JenkinsReportParser;

/**
 * This class is responsible for handling all the parsing of all JUnit files. It
 * extends the <code>DefaultHandler</code> and the methods needed for parsing
 * the XML in the JUnit reports.
 * 
 */
public class JUnitReportParser extends ReportParser {

	/** The SAX parser factory used for reading in the order files. */
	private static SAXParserFactory saxFactory = SAXParserFactory.newInstance();
	/** The SAXParser instance used for all the parsing. */
	private SAXParser saxp = null;

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

	private int currentRunIdx = 0;

	/* Currently parsed element holders. */
	private TestRun currentTestRun = null;
	private TestModule currentTestModule = null;
	private TestCase currentTestCase = null;
	private TestHistory history = null;
	private HashMap<String, TestPackage> parsedTestPackages = null;

	/**
	 * The ReportParser Ctor, creates a new parser and initializes the test
	 * history to put all the parsed contents into.
	 */
	public JUnitReportParser() {
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

			if (UnitTH.c_DBG) {
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
			if (null != currentTestCase) {
				currentTestCase.setVerdict(TestCaseVerdict.e_IGNORED);
			}
			
			// Entire class has been ignored
            if (currentTestCase.getName().equals(currentTestModule.getName())) {
				currentTestModule.setAsIgnored();
				currentTestModule.setNoTestCases("0"); // TODO, valid fix or not
			}
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

		if (file.getName().endsWith(".json") || file.getName().endsWith(".JSON")) {
			parseJsonFile(file);
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
				String folderDate = parseFolderDate(file.getParentFile().getName());
				if (folderDate != null) {
					currentTestModule.setDate(folderDate);
				} else {
					long fileLastModified = file.lastModified();
					currentTestModule.setDate(fileLastModified);
				}
			}

			addToPackage(currentTestModule);
			if (UnitTH.c_DBG) {
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

	private static String parseFolderDate(String folderName) {
		SimpleDateFormat[] formats = {
			new SimpleDateFormat("yyyy-MM-dd'T'HHmmss'Z'", Locale.US),
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
			new SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US),
			new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
		};
		for (SimpleDateFormat fmt : formats) {
			fmt.setLenient(false);
			try {
				Date d = fmt.parse(folderName);
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(d);
			} catch (Exception e) { /* try next */ }
		}
		return null;
	}

	private void parseJsonFile(File file) {
		try {
			StringBuilder sb = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			}
			String json = sb.toString();

			String testsArray = extractTestsArray(json);
			if (testsArray == null) {
				System.err.println("No tests array found in: " + file.getName());
				return;
			}

			String folderDate = parseFolderDate(file.getParentFile().getName());

			java.util.LinkedHashMap<String, ArrayList<String>> suiteMap = new java.util.LinkedHashMap<>();
			ArrayList<String> testObjects = splitJsonObjects(testsArray);
			for (String testJson : testObjects) {
				String suite = extractJsonString(testJson, "\"suite\"", null);
				if (suite == null) suite = "SQLValidation";
				suiteMap.computeIfAbsent(suite, k -> new ArrayList<>()).add(testJson);
			}

			for (java.util.Map.Entry<String, ArrayList<String>> entry : suiteMap.entrySet()) {
				String suiteName = entry.getKey();
				ArrayList<String> tests = entry.getValue();

				currentTestModule = new TestModule();
				int lastUnderscore = suiteName.lastIndexOf('_');
				String moduleName = lastUnderscore > 0
						? suiteName.substring(0, lastUnderscore) + "." + suiteName.substring(lastUnderscore + 1)
						: "default." + suiteName;
				currentTestModule.setName(moduleName);

				int totalTests = 0;
				int failCount = 0;
				int skipCount = 0;
				long totalDurationMs = 0;

				for (String testJson : tests) {
					TestCase tc = new TestCase();
					String name = extractJsonString(testJson, "\"name\"", null);
					String status = extractJsonString(testJson, "\"status\"", null);
					long duration = extractJsonLong(testJson, "\"duration\"");

					tc.setName(name != null ? name : "Unnamed");
					tc.setClassName(suiteName);
					tc.setModuleName(suiteName);
					tc.setExecutionTime(String.valueOf(duration / 1000.0));
					totalDurationMs += duration;
					totalTests++;

					if ("failed".equals(status)) {
						tc.setVerdict(TestCaseVerdict.e_FAIL);
						failCount++;
					} else if ("skipped".equals(status)) {
						tc.setVerdict(TestCaseVerdict.e_IGNORED);
						skipCount++;
					} else {
						tc.setVerdict(TestCaseVerdict.e_PASS);
					}
					currentTestModule.addTestCase(tc);
				}

				currentTestModule.setNoTestCases(String.valueOf(totalTests));
				currentTestModule.setNoFailures(String.valueOf(failCount));
				currentTestModule.setNoErrors("0");
				currentTestModule.setNoIgnored(String.valueOf(skipCount));
				currentTestModule.setExecutionTime(String.valueOf(totalDurationMs / 1000.0));

				if (folderDate != null) {
					currentTestModule.setDate(folderDate);
				} else {
					currentTestModule.setDate(file.lastModified());
				}

				currentTestModule.calcStats();
				addToPackage(currentTestModule);
				currentTestModule = null;
			}
		} catch (Exception e) {
			System.err.println("Error parsing JSON file '" + file.getAbsolutePath() + "': " + e.getMessage());
		}
	}

	private String extractJsonString(String json, String key, String context) {
		int keyIdx = -1;
		if (context != null) {
			int ctxIdx = json.indexOf("\"" + context + "\"");
			if (ctxIdx >= 0) {
				keyIdx = json.indexOf(key, ctxIdx);
			}
		}
		if (keyIdx < 0) keyIdx = json.indexOf(key);
		if (keyIdx < 0) return null;
		int colonIdx = json.indexOf(':', keyIdx + key.length());
		if (colonIdx < 0) return null;
		int startQuote = json.indexOf('"', colonIdx + 1);
		if (startQuote < 0) return null;
		int endQuote = json.indexOf('"', startQuote + 1);
		while (endQuote > 0 && json.charAt(endQuote - 1) == '\\') {
			endQuote = json.indexOf('"', endQuote + 1);
		}
		if (endQuote < 0) return null;
		return json.substring(startQuote + 1, endQuote);
	}

	private int extractJsonInt(String json, String key, String context) {
		int keyIdx = -1;
		if (context != null) {
			int ctxIdx = json.indexOf("\"" + context + "\"");
			if (ctxIdx >= 0) keyIdx = json.indexOf(key, ctxIdx);
		}
		if (keyIdx < 0) keyIdx = json.indexOf(key);
		if (keyIdx < 0) return 0;
		int colonIdx = json.indexOf(':', keyIdx + key.length());
		if (colonIdx < 0) return 0;
		int start = colonIdx + 1;
		while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
		int end = start;
		while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
		if (start == end) return 0;
		try { return Integer.parseInt(json.substring(start, end)); } catch (NumberFormatException e) { return 0; }
	}

	private long extractJsonLong(String json, String key) {
		int keyIdx = json.indexOf(key);
		if (keyIdx < 0) return 0;
		int colonIdx = json.indexOf(':', keyIdx + key.length());
		if (colonIdx < 0) return 0;
		int start = colonIdx + 1;
		while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
		int end = start;
		while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
		if (start == end) return 0;
		try { return Long.parseLong(json.substring(start, end)); } catch (NumberFormatException e) { return 0; }
	}

	private String extractTestsArray(String json) {
		int idx = json.indexOf("\"tests\"");
		while (idx >= 0) {
			int colonIdx = json.indexOf(':', idx + 7);
			if (colonIdx < 0) return null;
			int bracketIdx = -1;
			for (int i = colonIdx + 1; i < json.length(); i++) {
				char c = json.charAt(i);
				if (c == ' ' || c == '\t' || c == '\n' || c == '\r') continue;
				if (c == '[') { bracketIdx = i; }
				break;
			}
			if (bracketIdx >= 0) {
				int depth = 1;
				int end = bracketIdx + 1;
				while (end < json.length() && depth > 0) {
					char c = json.charAt(end);
					if (c == '[') depth++;
					else if (c == ']') depth--;
					end++;
				}
				return json.substring(bracketIdx + 1, end - 1);
			}
			idx = json.indexOf("\"tests\"", idx + 7);
		}
		return null;
	}

	private ArrayList<String> splitJsonObjects(String arrayContent) {
		ArrayList<String> objects = new ArrayList<>();
		int depth = 0;
		int start = -1;
		for (int i = 0; i < arrayContent.length(); i++) {
			char c = arrayContent.charAt(i);
			if (c == '{') { if (depth == 0) start = i; depth++; }
			else if (c == '}') { depth--; if (depth == 0 && start >= 0) { objects.add(arrayContent.substring(start, i + 1)); start = -1; } }
		}
		return objects;
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
