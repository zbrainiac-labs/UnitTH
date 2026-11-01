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
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import unitth.html.junit.HtmlMainGen;
import unitth.html.junit.HtmlModuleGen;
import unitth.html.junit.HtmlPackageGen;
import unitth.jenkins.JenkinsReportParser;
import unitth.junit.CTRFReportParser;
import unitth.junit.JUnitReportParser;
import unitth.junit.TestHistory;
import unitth.store.JsonRunStore;
import unitth.store.RunId;

public class UnitTH {
	public static final String VERSION_NUMBER = "3.0";

	private static final String HELP_TEXT = "\n"
			+ " UnitTH - Unit Test History\n"
			+ " MIT License\n\n"
			+ " Unit Test (report) history generator - UnitTH v"
			+ VERSION_NUMBER
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
			+ "\n"
			+ "> java -jar unitth.jar --store history.json --ctrf reports/*\n"
			+ "  Keeps parsed runs in history.json; later invocations only parse new\n"
			+ "  run folders and the raw reports of stored runs may be deleted.\n"
			;

	private static final String USAGE_TEXT = "\n"
			+ "UnitTH - Unit Test History\n"
			+ "MIT License\n\n"
			+ "   Test (report) history generator - UnitTH v"
			+ VERSION_NUMBER
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
			+ "\n"
			+ "Run store (incremental history, keeps the newest unitth.max.runs runs, default 100):\n"
			+ "Usage: java -jar unitth.jar --store <file.json> [--ingest-only] [-c|--ctrf|-j|--jenkins] <folders>\n"
			+ "  --store <file>   read/write the JSON run store (or -Dunitth.store.file=<file>)\n"
			+ "  --ingest-only    only add new runs to the store, do not generate the report\n"
			+ "  --store-dir <d>  report on all run stores in <d> (ingest still goes to --store)\n"
			+ "  lock: -Dunitth.store.lock.timeout=<s> (default 60); on timeout the ingest is skipped\n"
			;

	private static final String FLAG_HELP_LONG = "--help";
	private static final String FLAG_HELP_SHORT = "-h";
	private static final String FLAG_JENKINS_SHORT = "-j";
	private static final String FLAG_JENKINS_LONG = "--jenkins";
	private static final String FLAG_CTRF_SHORT = "-c";
	private static final String FLAG_CTRF_LONG = "--ctrf";
	private static final String FLAG_VERSION_SHORT = "-v";
	private static final String FLAG_VERSION_LONG = "--version";
	private static final String FLAG_STORE = "--store";
	private static final String FLAG_INGEST_ONLY = "--ingest-only";
	private static final String FLAG_STORE_DIR = "--store-dir";
	/** Seconds to wait for the run store lock before skipping the ingest. */
	public static final String PROP_LOCK_TIMEOUT = "unitth.store.lock.timeout";
	private static final long DEFAULT_LOCK_TIMEOUT = 60;
	private static final String RUN_STORE = "Run store '";

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
	private boolean ingestOnly = false;
	private String storeDir = null;

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		UnitTH thg = new UnitTH(UnitTHConfig.fromEnvironment());
		thg.run(args);
		if (thg.config.debug()) {
			Log.out(thg.toString());
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
		List<String> rest = new ArrayList<>(Arrays.asList(args));
		if (!parseOptions(rest)) {
			return FAILED;
		}
		if (rest.isEmpty() && config.storeFile() == null && storeDir == null) {
			Log.out(USAGE_TEXT);
			return OK;
		}
		String first = rest.isEmpty() ? "" : rest.get(0);
		if (first.equals(FLAG_HELP_LONG) || first.equals(FLAG_HELP_SHORT)) {
			Log.out(HELP_TEXT);
			return OK;
		}
		if (first.equals(FLAG_VERSION_SHORT) || first.equals(FLAG_VERSION_LONG)) {
			Log.out("UnitTH version: " + VERSION_NUMBER);
			return OK;
		}
		long timeStamp = System.currentTimeMillis();
		printProperties();

		String[] folders;
		try {
			folders = selectParser(rest);
		} catch (UnitTHException e) {
			Log.error(e.getMessage());
			return FAILED;
		}
		if (!parseAndCalculate(folders)) {
			return FAILED;
		}
		if (ingestOnly) {
			return OK;
		}
		unpackAndGenerate();

		if (!(parser instanceof JenkinsReportParser) && theHistory.getNoExecutedTestCases() < 1) {
			Log.error("No test cases has been parsed, skipping report generation.");
			return FAILED;
		}

		long diff = System.currentTimeMillis() - timeStamp;
		Log.out("\nA test history report has been generated in "
				+ diff + " ms at '" + config.rootFolder() + "'.");
		return OK;
	}

	/* Consumes the leading --store/--store-dir/--ingest-only options; false on invalid use. */
	private boolean parseOptions(List<String> rest) {
		ingestOnly = false;
		storeDir = null;
		while (!rest.isEmpty() && List.of(FLAG_STORE, FLAG_STORE_DIR, FLAG_INGEST_ONLY).contains(rest.get(0))) {
			String flag = rest.remove(0);
			if (flag.equals(FLAG_INGEST_ONLY)) {
				ingestOnly = true;
			} else if (rest.isEmpty()) {
				Log.error("The " + flag + " option requires an argument.");
				return false;
			} else if (flag.equals(FLAG_STORE)) {
				config = config.withStoreFile(rest.remove(0));
			} else {
				storeDir = rest.remove(0);
			}
		}
		if (ingestOnly && config.storeFile() == null) {
			Log.error("The " + FLAG_INGEST_ONLY + " option requires a run store (" + FLAG_STORE + " <file>).");
			return false;
		}
		return true;
	}

	/* Chooses the parser from the mode flag and returns the run folders. */
	private String[] selectParser(List<String> rest) throws UnitTHException {
		String first = rest.isEmpty() ? "" : rest.get(0);
		if (first.equals(FLAG_JENKINS_SHORT) || first.equals(FLAG_JENKINS_LONG)) {
			if (rest.size() < 2) {
				throw new UnitTHException("The Jenkins flags (-j --jenkins) require one additional argument pointing to the Jenkins builds folder.");
			}
			parser = new JenkinsReportParser(config);
			return jenkinsBuildFolders(rest.get(1));
		}
		if (first.equals(FLAG_CTRF_SHORT) || first.equals(FLAG_CTRF_LONG)) {
			parser = new CTRFReportParser(config);
			return rest.subList(1, rest.size()).toArray(new String[0]);
		}
		parser = new JUnitReportParser(config);
		return rest.toArray(new String[0]);
	}

	private String[] jenkinsBuildFolders(String path) {
		File jenkinsBuildDir = new File(path);
		ArrayList<String> buildFolders = new ArrayList<>();
		File[] builds = jenkinsBuildDir.listFiles();
		if (builds != null) {
			for (File build : builds) {
				if (build.isDirectory()) {
					buildFolders.add(build.getAbsolutePath());
				}
			}
		} else {
			Log.error("Jenkins build reports folder '" + jenkinsBuildDir.getAbsolutePath()
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
					&& name.startsWith(jsonFilter) && !JsonRunStore.isStoreFile(new File(dir, name).toPath());
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
		dirs = new ArrayList<>();
		testRuns = new ArrayList<>();

		int effectiveStart = Math.max(0, args.length - config.maxRuns());
		FilenameFilter filter = createReportFileFilter();
		for (int i = effectiveStart; i < args.length; i++) {
			File d = new File(args[i]);
			if (!d.exists()) {
				throw new UnitTHException("Folder '" + d.getAbsoluteFile()
						+ "' given as input argument does not exist.");
			}
			File[] files = d.isDirectory() ? d.listFiles(filter) : null;
			ArrayList<File> testRunFiles = files == null ? new ArrayList<>()
					: Arrays.stream(files).filter(File::isFile).collect(Collectors.toCollection(ArrayList::new));
			if (!testRunFiles.isEmpty()) {
				dirs.add(args[i]);
				testRuns.add(testRunFiles);
			}
		}
	}

	public boolean parseAndCalculate(String... args) {
		try {
			collectAllFiles(args);
		} catch (UnitTHException uthe) {
			Log.error(uthe.getMessage());
			return false;
		}

		if (config.storeFile() != null || storeDir != null) {
			theHistory = storeHistory();
			if (theHistory == null) {
				return false;
			}
		} else if (!testRuns.isEmpty()) {
			theHistory = parser.parseRuns(testRuns, dirs);
		} else {
			Log.error("There are no files to parse! Check the input parameters and properties.");
			Log.out(USAGE_TEXT);
			return false;
		}
		if (ingestOnly) {
			return true;
		}
		theHistory.calcStats();
		if (config.debug()) {
			Log.out(theHistory.toString());
		}
		return true;
	}

	private enum Ingest { ADDED, KNOWN, FAILED }

	/* Parses and adds the i-th collected run unless its content is already known. */
	private Ingest ingestRun(JsonRunStore store, int i) {
		try {
			String id = RunId.of(testRuns.get(i));
			if (store.isKnown(id)) {
				return Ingest.KNOWN;
			}
			store.add(id, dirs.get(i), parser.parseRun(testRuns.get(i), dirs.get(i)));
			return Ingest.ADDED;
		} catch (IOException e) {
			Log.error("Could not read run folder '" + dirs.get(i) + "', it is retried next time", e);
			return Ingest.FAILED;
		}
	}

	/*
	 * Ingests the collected runs into the --store file (if any), then returns
	 * the history of the stored runs, of all stores in --store-dir if given,
	 * or null on errors.
	 */
	private TestHistory storeHistory() {
		if (config.storeFile() == null) {
			if (!testRuns.isEmpty()) {
				Log.error("Run folders need a run store to ingest into (" + FLAG_STORE + " <file>).");
				return null;
			}
		} else if (!ingestIntoStore(Path.of(config.storeFile()))) {
			return null;
		}
		try {
			List<JsonRunStore> stores = storeDir != null ? JsonRunStore.loadAll(Path.of(storeDir))
					: List.of(JsonRunStore.load(Path.of(config.storeFile())));
			TestHistory history = new TestHistory(config);
			for (JsonRunStore store : stores) {
				store.toRuns(config).forEach(history::addTestRun);
			}
			if (history.getNoRuns() == 0) {
				Log.error("The run store is empty and there are no new runs to parse.");
				return null;
			}
			return history;
		} catch (IOException e) {
			Log.error("Run stores could not be read", e);
			return null;
		}
	}

	/*
	 * Adds the collected runs that are not yet in the store and trims it to
	 * maxRuns, holding the store lock. A lock timeout skips the ingest with a
	 * warning; false only if the store could not be used at all.
	 */
	private boolean ingestIntoStore(Path storePath) {
		Duration timeout = Duration.ofSeconds(Long.getLong(PROP_LOCK_TIMEOUT, DEFAULT_LOCK_TIMEOUT));
		try {
			String summary = JsonRunStore.locked(storePath, timeout, () -> ingestLocked(storePath));
			Log.out(summary);
			return true;
		} catch (JsonRunStore.LockTimeoutException e) {
			Log.warn(e.getMessage() + "; " + testRuns.size() + " run folder(s) were not stored");
			Log.out(storeLine(storePath) + " ingest=skipped");
			return true;
		} catch (IOException e) {
			Log.error(RUN_STORE + storePath + "' could not be used", e);
			Log.out(storeLine(storePath) + " ingest=failed");
			return false;
		}
	}

	private String storeLine(Path storePath) {
		return RUN_STORE + storePath + "': found=" + testRuns.size();
	}

	private String ingestLocked(Path storePath) throws IOException {
		JsonRunStore store = JsonRunStore.load(storePath);
		int added = 0;
		int known = 0;
		int failed = 0;
		for (int i = 0; i < testRuns.size(); i++) {
			switch (ingestRun(store, i)) {
			case ADDED -> added++;
			case KNOWN -> known++;
			default -> failed++;
			}
		}
		int dropped = store.trim(config.maxRuns());
		store.save();
		return storeLine(storePath) + " added=" + added
				+ " known=" + known + " failed=" + failed + " dropped=" + dropped + " stored=" + store.size()
				+ " ingest=" + (failed == 0 ? "ok" : "failed");
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
		Log.out("Using the following properties");
		Log.out(" " + UnitTHConfig.PROP_HTML_REPORT_PATH + " = " + config.reportPath());
		Log.out(" " + UnitTHConfig.PROP_REPORT_DIR + " = " + config.rootFolder());
		Log.out(" " + UnitTHConfig.PROP_XML_REPORT_FILTER + " = " + config.xmlReportFilter());
		Log.out(" " + UnitTHConfig.PROP_GEN_EXECTIMEGRAPHS + " = " + config.generateExecTimeGraphs());
		Log.out(" " + UnitTHConfig.PROP_USE_ABS_PATHS + " = " + config.useAbsPaths());
		Log.out(" " + UnitTHConfig.PROP_REPORT_FORMAT + " = " + config.reportFormat());
		if (storeDir != null) {
			Log.out(" " + FLAG_STORE_DIR + " = " + storeDir);
		}
		if (config.storeFile() != null) {
			Log.out(" " + UnitTHConfig.PROP_STORE_FILE + " = " + config.storeFile());
			Log.out(" " + UnitTHConfig.PROP_MAX_RUNS + " = " + config.maxRuns());
		}
	}
}
