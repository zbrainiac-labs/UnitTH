/*
 * MIT License - see LICENSE.txt
 */
package unitth.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Immutable run configuration. Built once from system properties and
 * unitth.properties, then passed to every component that needs it.
 */
public record UnitTHConfig(
		String rootFolder,
		String reportPath,
		boolean generateExecTimeGraphs,
		boolean useAbsPaths,
		String xmlReportFilter,
		String jsonReportFilter,
		String reportFormat,
		boolean debug,
		int maxRuns,
		String storeFile,
		List<String> inputArgs) {

	public static final String PROPS_FILENAME = "unitth.properties";
	public static final String PROP_USE_ABS_PATHS = "unitth.use.absolute.paths";
	public static final String PROP_XML_REPORT_FILTER = "unitth.xml.report.filter";
	public static final String PROP_JSON_REPORT_FILTER = "unitth.json.report.filter";
	public static final String PROP_REPORT_DIR = "unitth.report.dir";
	public static final String PROP_HTML_REPORT_PATH = "unitth.html.report.path";
	public static final String PROP_GEN_EXECTIMEGRAPHS = "unitth.generate.exectimegraphs";
	public static final String PROP_REPORT_FORMAT = "unitth.report.format";
	public static final String PROP_DEBUG = "unitth.debug";
	public static final String PROP_STORE_FILE = "unitth.store.file";
	public static final String PROP_MAX_RUNS = "unitth.max.runs";

	public static final int DEFAULT_MAX_RUNS = 100;

	public UnitTHConfig {
		inputArgs = inputArgs == null ? List.of() : List.copyOf(inputArgs);
	}

	public static UnitTHConfig defaults() {
		return new UnitTHConfig("report.th", ".", false, false, "TEST-", "TEST_",
				"json", false, DEFAULT_MAX_RUNS, null, List.of());
	}

	/**
	 * Resolves the configuration. System properties win; otherwise the
	 * working-folder properties file is used if present, else the home-folder
	 * file. Either file may be null.
	 */
	public static UnitTHConfig load(Properties runFile, Properties homeFile, Properties sys) {
		Properties file = runFile != null ? runFile : homeFile;
		UnitTHConfig d = defaults();
		return new UnitTHConfig(
				str(PROP_REPORT_DIR, sys, file, d.rootFolder()),
				str(PROP_HTML_REPORT_PATH, sys, file, d.reportPath()),
				bool(PROP_GEN_EXECTIMEGRAPHS, sys, file, d.generateExecTimeGraphs()),
				bool(PROP_USE_ABS_PATHS, sys, file, d.useAbsPaths()),
				str(PROP_XML_REPORT_FILTER, sys, file, d.xmlReportFilter()),
				str(PROP_JSON_REPORT_FILTER, sys, file, d.jsonReportFilter()),
				str(PROP_REPORT_FORMAT, sys, file, d.reportFormat()),
				"true".equalsIgnoreCase(sys.getProperty(PROP_DEBUG)),
				intValue(str(PROP_MAX_RUNS, sys, file, null), d.maxRuns()),
				str(PROP_STORE_FILE, sys, file, d.storeFile()),
				List.of());
	}

	/** Loads from the JVM system properties and the standard property file locations. */
	public static UnitTHConfig fromEnvironment() {
		Properties sys = System.getProperties();
		return load(readFile(new File(sys.getProperty("user.dir"), PROPS_FILENAME)),
				readFile(new File(sys.getProperty("user.home"), PROPS_FILENAME)), sys);
	}

	public UnitTHConfig withMaxRuns(int max) {
		return new UnitTHConfig(rootFolder, reportPath, generateExecTimeGraphs, useAbsPaths,
				xmlReportFilter, jsonReportFilter, reportFormat, debug, max, storeFile, inputArgs);
	}

	public UnitTHConfig withStoreFile(String store) {
		return new UnitTHConfig(rootFolder, reportPath, generateExecTimeGraphs, useAbsPaths,
				xmlReportFilter, jsonReportFilter, reportFormat, debug, maxRuns, store, inputArgs);
	}

	public UnitTHConfig withInputArgs(List<String> args) {
		return new UnitTHConfig(rootFolder, reportPath, generateExecTimeGraphs, useAbsPaths,
				xmlReportFilter, jsonReportFilter, reportFormat, debug, maxRuns, storeFile, args);
	}

	private static int intValue(String v, int def) {
		if (v == null) {
			return def;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			Log.warn("Ignoring invalid number '" + v + "', using " + def);
			return def;
		}
	}

	static Properties readFile(File f) {
		if (!f.exists()) {
			return null;
		}
		Properties p = new Properties();
		try (InputStream in = new FileInputStream(f)) {
			p.load(in);
		} catch (IOException ioe) {
			Log.warn("Problems parsing the properties file... using default settings instead.");
		}
		return p;
	}

	private static String str(String key, Properties sys, Properties file, String def) {
		String v = sys.getProperty(key);
		if (v != null) {
			return v;
		}
		if (file != null) {
			v = file.getProperty(key);
			if (v != null && !v.isEmpty()) {
				return v;
			}
		}
		return def;
	}

	private static boolean bool(String key, Properties sys, Properties file, boolean def) {
		String v = sys.getProperty(key);
		if (v == null && file != null) {
			v = file.getProperty(key);
		}
		if (v == null) {
			return def;
		}
		return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes");
	}
}
