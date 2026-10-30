/*
 * MIT License - see LICENSE.txt
 */
package unitth.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import unitth.html.junit.HtmlMainGen;
import unitth.html.junit.HtmlModuleGen;
import unitth.html.junit.HtmlPackageGen;
import unitth.jenkins.JenkinsReportParser;
import unitth.junit.CTRFReportParser;
import unitth.junit.JUnitReportParser;
import unitth.junit.TestHistory;

public class UnitTH {
	public final static String versionNumber = "3.0";

	public final static String NOT_IN_PACKAGE = "not-in-package";

	private static final String c_HELP_TEXT = "\n"
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

	private static final String c_USAGE_TEXT = "\n"
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

	private static final String c_FLAG_HELP_LONG = "--help";
	private static final String c_FLAG_HELP_SHORT = "-h";
	private static final String c_JENKINS_REPORTS_SHORT = "-j";
	private static final String c_JENKINS_REPORTS_LONG = "--jenkins";
	private static final String c_CTRF_REPORTS_SHORT = "-c";
	private static final String c_CTRF_REPORTS_LONG = "--ctrf";
	public static final String c_VERSION_SHORT = "-v";
	public static final String c_VERSION_LONG = "--version";

	public static final String CSS_DIR = "css";
	public static final String CSS_FILE = "style.css";
	public static final String JS_DIR = "js";
	public static final String SORT_SCRIPT = "tablesort.js";

	/** Exit/status codes returned by {@link #run(String...)}. */
	public static final int OK = 0;
	public static final int FAILED = 1;

	private final UnitTHConfig baseConfig;
	private UnitTHConfig config;

	protected ArrayList<ArrayList<File>> testRuns = null;
	protected ArrayList<String> dirs = null;
	protected ReportParser parser = null;
	protected TestHistory theHistory = null;

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		UnitTH thg = new UnitTH(UnitTHConfig.fromEnvironment());
		thg.run(args);
		if (thg.config.debug()) {
			System.out.println(thg.toString());
		}
	}

	public UnitTH(UnitTHConfig config) {
		this.baseConfig = config;
		this.config = config;
	}

	public UnitTHConfig getConfig() {
		return config;
	}

	public TestHistory getHistory() {
		return theHistory;
	}

	/**
	 * Parses the given report folders and generates the HTML history.
	 *
	 * @return {@link #OK} if a report was generated (or help/version was
	 *         printed), {@link #FAILED} otherwise.
	 */
	public int run(String... args) {
		config = baseConfig.withInputArgs(Arrays.asList(args));
		if (args.length == 0) {
			System.out.println(c_USAGE_TEXT);
			return OK;
		}
		if (args[0].equals(c_FLAG_HELP_LONG) || args[0].equals(c_FLAG_HELP_SHORT)) {
			System.out.println(c_HELP_TEXT);
			return OK;
		}
		if (args[0].equals(c_VERSION_SHORT) || args[0].equals(c_VERSION_LONG)) {
			System.out.println("UnitTH version: " + versionNumber);
			return OK;
		}
		long timeStamp = System.currentTimeMillis();
		printProperties();

		String[] folders;
		if (args[0].equals(c_JENKINS_REPORTS_SHORT) || args[0].equals(c_JENKINS_REPORTS_LONG)) {
			if (args.length < 2) {
				wrongJenkinsInvocation();
				return FAILED;
			}
			parser = new JenkinsReportParser(config);
			folders = jenkinsBuildFolders(args[1]);
		} else if (args[0].equals(c_CTRF_REPORTS_SHORT) || args[0].equals(c_CTRF_REPORTS_LONG)) {
			parser = new CTRFReportParser(config);
			folders = Arrays.copyOfRange(args, 1, args.length);
		} else {
			parser = new JUnitReportParser(config);
			folders = args;
		}

		if (!parseAndCalculate(folders)) {
			return FAILED;
		}
		unpackAndGenerate();

		if (!(parser instanceof JenkinsReportParser) && theHistory.getNoExecutedTestCases() < 1) {
			System.err.println("No test cases has been parsed, skipping report generation.");
			return FAILED;
		}

		long diff = System.currentTimeMillis() - timeStamp;
		System.out.println("\nA test history report has been generated in "
				+ diff + " ms at '" + config.rootFolder() + "'.");
		return OK;
	}

	private String[] jenkinsBuildFolders(String path) {
		File jenkinsBuildDir = new File(path);
		ArrayList<String> buildFolders = new ArrayList<String>();
		if (jenkinsBuildDir.exists()) {
			File[] builds = jenkinsBuildDir.listFiles();
			for (File build : builds) {
				if (build.isDirectory()) {
					buildFolders.add(build.getAbsolutePath());
				}
			}
		} else {
			System.err.println("Jenkins build reports folder '" + jenkinsBuildDir.getAbsolutePath()
					+ "' does not exists, please check the input arguments '-j <folder path>'.");
		}
		return buildFolders.toArray(new String[0]);
	}

	private FilenameFilter createReportFileFilter() {
		final boolean ctrfOnly = parser instanceof CTRFReportParser;
		final String xmlFilter = config.xmlReportFilter();
		final String jsonFilter = config.jsonReportFilter();
		final String format = config.reportFormat();
		return (dir, name) -> {
			boolean isXml = (name.endsWith(".xml") || name.endsWith(".XML"))
					&& name.startsWith(xmlFilter);
			boolean isJson = (name.endsWith(".json") || name.endsWith(".JSON"))
					&& name.startsWith(jsonFilter);
			if (ctrfOnly || "json".equalsIgnoreCase(format)) {
				return isJson;
			}
			if ("xml".equalsIgnoreCase(format)) {
				return isXml;
			}
			return isXml || isJson;
		};
	}

	private void unpackAndGenerate() {
		try {
			new File(config.rootFolder()).mkdir();
			extractFilesFromJar();
		} catch (Exception e) {
			Log.error("Could not create the report folder or extract static files", e);
			return;
		}
		if (null != theHistory) {
			boolean graphs = config.generateExecTimeGraphs();
			new HtmlMainGen(theHistory, graphs).generateHtmlHistory();
			new HtmlPackageGen(theHistory, graphs).generateHtmlHistory();
			new HtmlModuleGen(theHistory, graphs).generateHtmlHistory();
		}
	}

	private void collectAllFiles(String... args) throws UnitTHException {
		dirs = new ArrayList<String>();
		testRuns = new ArrayList<ArrayList<File>>();

		int effectiveStart = Math.max(0, args.length - config.maxRuns());
		FilenameFilter filter = createReportFileFilter();
		for (int i = effectiveStart; i < args.length; i++) {
			File d = new File(args[i]);
			if (!d.exists()) {
				throw new UnitTHException("Folder '" + d.getAbsoluteFile()
						+ "' given as input argument does not exist.");
			}
			if (d.isDirectory()) {
				ArrayList<File> testRunFiles = new ArrayList<File>();
				for (File f : d.listFiles(filter)) {
					if (f.isFile()) {
						testRunFiles.add(f);
					}
				}
				if (testRunFiles.size() > 0) {
					dirs.add(args[i]);
					testRuns.add(testRunFiles);
				}
			}
		}
	}

	public boolean parseAndCalculate(String... args) {
		try {
			collectAllFiles(args);
		} catch (UnitTHException uthe) {
			System.err.println(uthe.getMessage());
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
			if (config.debug()) {
				System.out.println(theHistory.toString());
			}
			return true;
		}
		return false;
	}

	private void extractFilesFromJar() throws IOException, UnitTHException {
		String root = config.rootFolder();
		getFileFromJar("/" + CSS_DIR + "/" + CSS_FILE,
				root + File.separator + CSS_DIR + File.separator + CSS_FILE, CSS_DIR);
		getFileFromJar("/javascript/" + SORT_SCRIPT,
				root + File.separator + JS_DIR + File.separator + SORT_SCRIPT, JS_DIR);
	}

	public void getFileFromJar(String fileName, String outputName, String dest)
			throws IOException, UnitTHException {
		try (InputStream in = UnitTH.class.getResourceAsStream(fileName)) {
			if (null == in) {
				throw new UnitTHException("Could not extract '" + fileName
						+ "' from jar file (target dir: '" + dest + "').");
			}
			new File(config.rootFolder(), dest).mkdirs();
			try (OutputStream out = new FileOutputStream(outputName)) {
				in.transferTo(out);
			}
		}
	}

	private void printProperties() {
		System.out.println("Using the following properties");
		System.out.println(" " + UnitTHConfig.PROP_HTML_REPORT_PATH + " = " + config.reportPath());
		System.out.println(" " + UnitTHConfig.PROP_REPORT_DIR + " = " + config.rootFolder());
		System.out.println(" " + UnitTHConfig.PROP_XML_REPORT_FILTER + " = " + config.xmlReportFilter());
		System.out.println(" " + UnitTHConfig.PROP_GEN_EXECTIMEGRAPHS + " = " + config.generateExecTimeGraphs());
		System.out.println(" " + UnitTHConfig.PROP_USE_ABS_PATHS + " = " + config.useAbsPaths());
		System.out.println(" " + UnitTHConfig.PROP_REPORT_FORMAT + " = " + config.reportFormat());
	}

	private void wrongJenkinsInvocation() {
		System.err.println("The Jenkins flags (-j --jenkins) require one additional argument pointing to the Jenkins builds folder.");
	}
}
