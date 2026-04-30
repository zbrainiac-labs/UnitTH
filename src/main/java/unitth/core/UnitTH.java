/*
 * MIT License - see LICENSE.txt
 */
package unitth.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import unitth.html.junit.HtmlMainGen;
import unitth.html.junit.HtmlModuleGen;
import unitth.html.junit.HtmlPackageGen;
import unitth.jenkins.JenkinsReportParser;
import unitth.junit.CTRFReportParser;
import unitth.junit.JUnitReportParser;
import unitth.junit.TestHistory;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackageSummary;

public class UnitTH {
	public final static String versionNumber = "3.0";

	public static boolean c_DBG = false;


	private final static String PROPS_FILENAME = "unitth.properties";
	private final static String PROP_USE_ABS_PATHS = "unitth.use.absolute.paths";
	private final static String PROP_XML_REPORT_FILTER = "unitth.xml.report.filter";
	private final static String PROP_REPORT_DIR = "unitth.report.dir";
	private final static String PROP_HTML_REPORT_PATH = "unitth.html.report.path";
	private final static String PROP_GEN_EXECTIMEGRAPHS = "unitth.generate.exectimegraphs";
	private final static String PROP_REPORT_FORMAT = "unitth.report.format";

	public final static String NOT_IN_PACKAGE = "not-in-package";

	private static String c_HELP_TEXT = "\n"
			+ " UnitTH - Unit Test History\n"
			+ " MIT License\n\n"
			+ " Unit Test (report) history generator - UnitTH v"
			+ versionNumber
			+ "\n"
			+ "------------------------------------------------------\n"
			+ "This program is to be used for multiple generated\n"
			+ "test runs and test reports. The folders where\n"
			+ "the test results and generated reports are\n"
			+ "placed counts as input to this test history\n"
			+ "generator.\n"
			+ "\n"
			+ "Input and output properties shall be set using any of these three\n"
			+ "methods (given in the priority order understood by the application,\n"
			+ "ie. system parameters overrides local properties).\n\n"
			+ " 1) as system parameters in build scripts\n"
			+ " 2) as D flags in Ant or Maven invocation\n"
			+ " 3) in unitth.properties in the working folder\n"
			+ " 4) in unitth.properties in the $HOME folder\n"
			+ "\n"
			+ "Example:\n"
			+ "\n"
			+ "> java -jar unitth.jar report_1 report_2 report_3\n"
			+ "\n"
			+ "> java -jar unitth.jar report_2009-*\n" + "\n"
			+ "\n"
			+ "> java -jar unitth.jar -j jenkinsInstallation/builds/unitth-job/builds\n" + "\n"
			;

	private static String c_USAGE_TEXT = "\n"
			+ "UnitTH - Unit Test History\n"
			+ "MIT License\n\n"
			+ "   Test (report) history generator - UnitTH v"
			+ versionNumber
			+ "\n"
			+ "-----------------------------------------------------\n"
			+ "This program parses test results and test reports\n"
			+ "to generate a historical summary of results.\n"
			+ "\n"
			+ "Usage: java -jar unitth.jar --help\n"
			+ "\n"
			+ "Usage: java -jar unitth.jar <folder including generated XML and HTML results and report files> * N\n"
			+ "Example: java -jar unitth.jar report.th/report-0*\n"
			+ "\n"
			+ "Integration with Jenkins and Hudson can be done by using the -j or --jenkins flags. Note that the XML report file filter needs to be set to match junitResult.xml\n"
			+ "\n"
			+ "Usage: java -jar unitth.jar <-j|--jenkins> <Path to folder where all Jenkins reports have been generated. Usually <jenkins installation>/jobs/<job name>/builds\n"
			+ "Example: java -jar unitth.jar -j ../builds/unitth-job/builds\n"
			;

	private static String c_FLAG_HELP_LONG = "--help";
	private static String c_FLAG_HELP_SHORT = "-h";
	private static String c_JENKINS_REPORTS_SHORT = "-j";
	private static String c_JENKINS_REPORTS_LONG = "--jenkins";
	private static String c_CTRF_REPORTS_SHORT = "-c";
	private static String c_CTRF_REPORTS_LONG = "--ctrf";
	protected final int c_FILE_START_IDX = 0;
	public static final String c_VERSION_SHORT = "-v";
	public static final String c_VERSION_LONG = "--version";

	public static String[] inputArgs = null;
	private int startIndex = c_FILE_START_IDX;

	public static String rootFolder = "report.th";
	public static String reportPath = ".";
	public static boolean generateExecTimeGraphs = false;
	public static boolean useAbsPaths = false;
	public static String xmlReportFilter = "TEST-";
	public static String jsonReportFilter = "TEST_";
	public static String reportFormat = "json";

	public static final String IMAGE_DIR = "images";
	public static final String CSS_DIR = "css";
	public static final String CSS_FILE = "style.css";
	public static final String JS_DIR = "js";
	public static final String SORT_SCRIPT = "tablesort.js";

	protected ArrayList<ArrayList<File>> testRuns = null;
	protected ArrayList<String> dirs = null;
	protected ReportParser parser = null;
	private FilenameFilter filter = null;
	protected RunHistory theHistory = null;
	public Properties properties = null;

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);

		if (null != System.getProperty("unitth.debug")) {
			if (System.getProperty("unitth.debug").equalsIgnoreCase("true")) {
				c_DBG = true;
			}
		}

		inputArgs = args;
		UnitTH thg = new UnitTH(args);
		if (c_DBG) {
			System.out.println(thg.toString());
		}
	}

	public UnitTH() {
	}

	public UnitTH(String... args) {
		doTheStuff(args);
	}

	private void createJUnitXMLFilter() {
		filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (parser instanceof CTRFReportParser) {
					return (name.endsWith(".json") || name.endsWith(".JSON"))
							&& name.startsWith(jsonReportFilter);
				}
				boolean isXml = (name.endsWith(".xml") || name.endsWith(".XML"))
						&& name.startsWith(xmlReportFilter);
				boolean isJson = (name.endsWith(".json") || name.endsWith(".JSON"))
						&& name.startsWith(jsonReportFilter);
				if ("xml".equalsIgnoreCase(reportFormat)) {
					return isXml;
				}
				if ("json".equalsIgnoreCase(reportFormat)) {
					return isJson;
				}
				return isXml || isJson;
			}
		};
	}

	private void doTheStuff(String... args) {
		if (args.length == 0) {
			System.out.println(c_USAGE_TEXT);
			return;
		}
		if (args[0].equals(c_FLAG_HELP_LONG)
				|| args[0].equals(c_FLAG_HELP_SHORT)) {
			System.out.println(c_HELP_TEXT);
			return;
		}
		if (args[0].equals(c_VERSION_SHORT) || args[0].equals(c_VERSION_LONG)) {
			System.out.println("UnitTH version: " + versionNumber);
			return;
		}
		long timeStamp = System.currentTimeMillis();
		properties = new Properties();
		readProperties();

		if (args[0].equals(c_JENKINS_REPORTS_SHORT) || args[0].equals(c_JENKINS_REPORTS_LONG)) {
			if (args.length < 2) {
				wrongJenkinsInvocation();
			}
			parser = new JenkinsReportParser();

			File jenkinsBuildDir = new File(args[c_FILE_START_IDX+1]);
			File[] builds;
			ArrayList<String> buildFolders = new ArrayList<String>();
			if (jenkinsBuildDir.exists()) {
				builds = jenkinsBuildDir.listFiles();
				for (int i=0; i<builds.length; i++) {
					if (builds[i].isDirectory()) {
						buildFolders.add(builds[i].getAbsolutePath());
					}
				}
			} else {
				System.err.println("Jenkins build reports folder '"+jenkinsBuildDir.getAbsolutePath()+"' does not exists, please check the input arguments '-j <folder path>'.");
			}

			if (parseAndCalculate(buildFolders.toArray(new String[0]))) {
				unpackAndGenerate();
				long diff = System.currentTimeMillis() - timeStamp;
				System.out.println("\nA test history report has been generated in "
						+ diff + " ms at '" + rootFolder + "'.");
			}
		}
		else if (args[0].equals(c_CTRF_REPORTS_SHORT) || args[0].equals(c_CTRF_REPORTS_LONG)) {
			parser = new CTRFReportParser();
			String[] ctrfArgs = new String[args.length - 1];
			System.arraycopy(args, 1, ctrfArgs, 0, args.length - 1);
			if (parseAndCalculate(ctrfArgs)) {
				unpackAndGenerate();

				if (((unitth.junit.TestHistory)theHistory).getNoExecutedTestCases() < 1) {
					System.err.println("No test cases has been parsed, skipping report generation.");
					return;
				}

				long diff = System.currentTimeMillis() - timeStamp;
				System.out.println("\nA test history report has been generated in "
						+ diff + " ms at '" + rootFolder + "'.");
			}
		}
		else {
			parser = new JUnitReportParser();
			if (parseAndCalculate(args)) {
				unpackAndGenerate();

			if (((unitth.junit.TestHistory)theHistory).getNoExecutedTestCases() < 1) {
					System.err.println("No test cases has been parsed, skipping report generation.");
					return;
				}

				long diff = System.currentTimeMillis() - timeStamp;
				System.out.println("\nA test history report has been generated in "
						+ diff + " ms at '" + rootFolder + "'.");
			}
		}
	}

	private void unpackAndGenerate() {
		try {
			createDir(rootFolder);
			extractFilesFromJar();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (null != theHistory) {
			HtmlMainGen htmlMainGen = new HtmlMainGen((TestHistory) theHistory,
					generateExecTimeGraphs);
			htmlMainGen.generateHtmlHistory();
			HtmlPackageGen htmlPackageGen = new HtmlPackageGen(
					(TestHistory) theHistory, generateExecTimeGraphs);
			htmlPackageGen.generateHtmlHistory();
			HtmlModuleGen htmlModuleGen = new HtmlModuleGen(
					(TestHistory) theHistory, generateExecTimeGraphs);
			htmlModuleGen.generateHtmlHistory();
		}
	}

	private void collectAllFiles(String... args) throws Exception {
		dirs = new ArrayList<String>();
		testRuns = new ArrayList<ArrayList<File>>();

		try {
			for (int i = startIndex; i < args.length; i++) {
				File d = new File(args[i]);
				if (!d.exists()) {
					throw new UnitTHException("Folder '"+d.getAbsoluteFile()+"' given as input argument does not exist.");
				}

				if (d.isDirectory()) {
					ArrayList<File> testRunFiles = new ArrayList<File>();
					createJUnitXMLFilter();
					File[] ff = d.listFiles(filter);
					for (File f : ff) {
						if (fileSanityCheck(f)) {
							testRunFiles.add(f);
						}
					}
					if (testRunFiles.size() > 0) {
						dirs.add(args[i]);
						testRuns.add(testRunFiles);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public boolean parseAndCalculate(String... args) {
		try {
			collectAllFiles(args);
		} catch(UnitTHException uthe) {
			System.err.println(uthe.getMessage());
			return false;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return false;
		}

		if (0 < testRuns.size()) {
			theHistory = parser.parseRuns(testRuns, dirs);
		} else {
			System.err.println("There are no files to parse! Check the input parameters and properties.");
			System.out.println(c_USAGE_TEXT);
			return false;
		}

		if (null != theHistory) {
			theHistory.calcStats();
			if (c_DBG) {
				System.out.println(theHistory.toString());
			}
			return true;
		} else {
			return false;
		}
	}

	private void createDir(String dir) {
		File outputDir = new File(dir);
		try {
			outputDir.mkdir();
		} catch (SecurityException se) {
			se.printStackTrace();
		}
	}

	private void createDir(String parent, String dir) {
		File outputDir = new File(parent, dir);
		try {
			outputDir.mkdirs();
		} catch (SecurityException se) {
			se.printStackTrace();
		}
	}

	private void extractFilesFromJar() throws IOException, UnitTHException {
		try {
			String outputName = rootFolder + File.separator + "css"
					+ File.separator + CSS_FILE;
			getFileFromJar("/"+CSS_DIR+"/" + CSS_FILE, outputName, CSS_DIR);

			outputName = rootFolder + File.separator + JS_DIR + File.separator
					+ SORT_SCRIPT;
			getFileFromJar("/javascript/" + SORT_SCRIPT, outputName, JS_DIR);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw ioe;
		} catch (UnitTHException uthe) {
			uthe.printStackTrace();
			throw uthe;
		}
	}

	public void getFileFromJar(String fileName, String outputName, String dest)
			throws IOException, UnitTHException {

		InputStream in = this.getClass().getResourceAsStream(fileName);
		if (null == in) {
			throw new UnitTHException("Could not extract '" + fileName
					+ "' from jar file (target dir: '"+dest+"').");
		}
		createDir(rootFolder, dest);
		OutputStream out = new FileOutputStream(outputName);
		int c;
		while ((c = in.read()) != -1) {
			out.write(c);
		}
		in.close();
		out.close();
	}

	private boolean fileSanityCheck(File inFile) {
		return inFile.isFile();
	}

	private void getExecutionTimeProp(File runProps, File homeProps) {
		if (null != System.getProperty(PROP_GEN_EXECTIMEGRAPHS)) {
			setGenerateExecTimeGraphs(System.getProperty(PROP_GEN_EXECTIMEGRAPHS));
			return;
		}
		try {
			if (runProps.exists()) {
				properties.load(new FileInputStream(runProps));
				if (null != properties.getProperty(PROP_GEN_EXECTIMEGRAPHS)) {
					setGenerateExecTimeGraphs(properties.getProperty(PROP_GEN_EXECTIMEGRAPHS));
				}
				return;
			}
			if (homeProps.exists()) {
				properties.load(new FileInputStream(homeProps));
				if (null != properties.getProperty(PROP_GEN_EXECTIMEGRAPHS)) {
					setGenerateExecTimeGraphs(properties.getProperty(PROP_GEN_EXECTIMEGRAPHS));
				}
				return;
			}
		} catch (IOException ioe) {
			System.err.println("Problems parsing the properties file... using default settings instead.");
		}
	}

	private void getJunitReportPathProp(File runProps, File homeProps) {
		if (null != System.getProperty(PROP_HTML_REPORT_PATH)) {
			reportPath = System.getProperty(PROP_HTML_REPORT_PATH);
			return;
		}
		try {
			if (runProps.exists()) {
				properties.load(new FileInputStream(runProps));
				String propHolder = properties.getProperty(PROP_HTML_REPORT_PATH);
				if (null != propHolder && !propHolder.equals("")) {
					reportPath = propHolder;
				}
				return;
			}
			if (homeProps.exists()) {
				properties.load(new FileInputStream(homeProps));
				String propHolder = properties.getProperty(PROP_HTML_REPORT_PATH);
				if (null != propHolder && !propHolder.equals("")) {
					reportPath = propHolder;
				}
				return;
			}
		} catch (IOException ioe) {
			System.err.println("Problems parsing the properties file... using default settings instead.");
		}
	}

	private void getReportDirProp(File runProps, File homeProps) {
		if (null != System.getProperty(PROP_REPORT_DIR)) {
			rootFolder = System.getProperty(PROP_REPORT_DIR);
			return;
		}
		try {
			if (runProps.exists()) {
				properties.load(new FileInputStream(runProps));
				String propHolder = properties.getProperty(PROP_REPORT_DIR);
				if (null != propHolder && !propHolder.equals("")) {
					rootFolder = propHolder;
				}
				return;
			}
			if (homeProps.exists()) {
				properties.load(new FileInputStream(homeProps));
				String propHolder = properties.getProperty(PROP_REPORT_DIR);
				if (null != propHolder && !propHolder.equals("")) {
					rootFolder = propHolder;
				}
				return;
			}
		} catch (IOException ioe) {
			System.err.println("Problems parsing the properties file... using default settings instead.");
		}
	}

	private void getXmlFilterProp(File runProps, File homeProps) {
		if (null != System.getProperty(PROP_XML_REPORT_FILTER)) {
			xmlReportFilter = System.getProperty(PROP_XML_REPORT_FILTER);
			return;
		}
		try {
			if (runProps.exists()) {
				properties.load(new FileInputStream(runProps));
				String propHolder = properties.getProperty(PROP_XML_REPORT_FILTER);
				if (null != propHolder && !propHolder.equals("")) {
					xmlReportFilter = propHolder;
				}
				return;
			}
			if (homeProps.exists()) {
				properties.load(new FileInputStream(homeProps));
				String propHolder = properties.getProperty(PROP_XML_REPORT_FILTER);
				if (null != propHolder && !propHolder.equals("")) {
					xmlReportFilter = propHolder;
				}
				return;
			}
		} catch (IOException ioe) {
			System.err.println("Problems parsing the properties file... using default settings instead.");
		}
	}

	private void getAbsPathsProp(File runProps, File homeProps) {
		if (null != System.getProperty(PROP_USE_ABS_PATHS)) {
			setUseAbsPaths(System.getProperty(PROP_USE_ABS_PATHS));
			return;
		}
		try {
			if (runProps.exists()) {
				properties.load(new FileInputStream(runProps));
				if (null != properties.getProperty(PROP_USE_ABS_PATHS)) {
					setUseAbsPaths(properties.getProperty(PROP_USE_ABS_PATHS));
				}
				return;
			}
			if (homeProps.exists()) {
				properties.load(new FileInputStream(homeProps));
				if (null != properties.getProperty(PROP_USE_ABS_PATHS)) {
					setUseAbsPaths(properties.getProperty(PROP_USE_ABS_PATHS));
				}
				return;
			}
		} catch (IOException ioe) {
			System.err.println("Problems parsing the properties file... using default settings instead.");
		}
	}

	private void setUseAbsPaths(String value) {
		useAbsPaths = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes");
	}

	private void setGenerateExecTimeGraphs(String value) {
		generateExecTimeGraphs = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes");
	}

	public void readProperties() {
		File runPropertiesFile = new File(System.getProperty("user.dir")
				+ File.separator + PROPS_FILENAME);
		File homePropertiesFile = new File(System.getProperty("user.home")
				+ File.separator + PROPS_FILENAME);

		getExecutionTimeProp(runPropertiesFile, homePropertiesFile);
		getJunitReportPathProp(runPropertiesFile, homePropertiesFile);
		getReportDirProp(runPropertiesFile, homePropertiesFile);
		getXmlFilterProp(runPropertiesFile, homePropertiesFile);
		getAbsPathsProp(runPropertiesFile, homePropertiesFile);
		getReportFormatProp(runPropertiesFile, homePropertiesFile);

		properties.setProperty(PROP_HTML_REPORT_PATH, reportPath);
		properties.setProperty(PROP_REPORT_DIR, rootFolder);
		properties.setProperty(PROP_XML_REPORT_FILTER, xmlReportFilter);
		properties.setProperty(PROP_GEN_EXECTIMEGRAPHS, Boolean.toString(generateExecTimeGraphs));
		properties.setProperty(PROP_USE_ABS_PATHS, Boolean.toString(useAbsPaths));
		properties.setProperty(PROP_REPORT_FORMAT, reportFormat);

		System.out.println("Using the following properties");
		System.out.println(" "+PROP_HTML_REPORT_PATH+" = " + reportPath);
		System.out.println(" "+PROP_REPORT_DIR+" = " + rootFolder);
		System.out.println(" "+PROP_XML_REPORT_FILTER+" = " + xmlReportFilter);
		System.out.println(" "+PROP_GEN_EXECTIMEGRAPHS+" = " + generateExecTimeGraphs);
		System.out.println(" "+PROP_USE_ABS_PATHS+" = " + useAbsPaths);
		System.out.println(" "+PROP_REPORT_FORMAT+" = " + reportFormat);
	}

	private void getReportFormatProp(File runProps, File homeProps) {
		if (null != System.getProperty(PROP_REPORT_FORMAT)) {
			reportFormat = System.getProperty(PROP_REPORT_FORMAT);
			return;
		}
		try {
			if (runProps.exists()) {
				properties.load(new FileInputStream(runProps));
				String propHolder = properties.getProperty(PROP_REPORT_FORMAT);
				if (null != propHolder && !propHolder.equals("")) {
					reportFormat = propHolder;
				}
				return;
			}
			if (homeProps.exists()) {
				properties.load(new FileInputStream(homeProps));
				String propHolder = properties.getProperty(PROP_REPORT_FORMAT);
				if (null != propHolder && !propHolder.equals("")) {
					reportFormat = propHolder;
				}
				return;
			}
		} catch (IOException ioe) {
			System.err.println("Problems parsing the properties file... using default settings instead.");
		}
	}

	private void wrongJenkinsInvocation() {
		System.err.println("The Jenkins flags (-j --jenkins) require one additional argument pointing to the Jenkins builds folder.");
	}
}
