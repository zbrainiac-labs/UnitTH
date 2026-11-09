/*
 * MIT License - see LICENSE.txt
 */
package unitth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import unitth.core.UnitTH;
import unitth.core.UnitTHConfig;

/**
 * End-to-end regression: runs the full pipeline in-process on the bundled
 * sample reports and checks the generated HTML.
 */
class ReportE2EIT {

	static final Path SAMPLES = Paths.get("src/test/resources/junitsamplereport");
	static final Path GOLDEN = Paths.get("src/test/resources/golden");
	static final boolean UPDATE_GOLDEN = Boolean.getBoolean("unitth.updateGolden");

	private static final Pattern RUNS_KPI = Pattern.compile("kpi-value accent\">(\\d+)");
	private static final Pattern RUN_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}&nbsp;\\d{2}:\\d{2}:\\d{2}");
	private static final Pattern PACKAGE_LINK = Pattern.compile("th-package-[^\"]*\\.html");

	static Stream<Arguments> scenarios() {
		return Stream.of(
				Arguments.of("xml", "xml", false, 9, 2, 1, 9),
				Arguments.of("json", "json", false, 3, 3, 3, 3),
				Arguments.of("both", "both", false, 12, 5, 4, 12),
				Arguments.of("ctrf", "json", true, 3, 3, 3, 3));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("scenarios")
	void generatesExpectedReport(String scenario, String format, boolean ctrf, int runs,
			int modules, int packages, int dates, @TempDir Path out) throws IOException {
		assertThat(generate(out, format, ctrf, UnitTHConfig.DEFAULT_MAX_RUNS)).isEqualTo(UnitTH.OK);

		String index = read(out.resolve("index.html"));
		assertThat(runsKpi(index)).as("runs").isEqualTo(runs);
		assertThat(list(out, "th-module-")).as("modules").hasSize(modules);
		assertThat(list(out, "th-package-")).as("packages").hasSize(packages);
		assertThat(uniqueDates(index)).as("unique run dates").hasSize(dates);

		assertThat(out.resolve("css/style.css")).exists();
		assertThat(out.resolve("js/tablesort.js")).exists();

		// One expandable project per package, listing exactly the runs of its package page
		String[] projects = index.split("<details class=\"project\">");
		assertThat(projects.length - 1).as("projects").isEqualTo(packages);
		for (int i = 1; i < projects.length; i++) {
			String block = projects[i].substring(0, projects[i].indexOf("</details>"));
			Matcher page = PACKAGE_LINK.matcher(block);
			assertThat(page.find()).as("project link").isTrue();
			int runRows = block.split("<tr>", -1).length - 2; // minus text before the first row and the header
			assertThat(runRows).as("runs of " + page.group())
					.isEqualTo(tableRows(read(out.resolve(page.group())), "run_table")).isPositive();
		}
		assertThat(index.contains("href=\"#modules\"")).as("modules section").isEqualTo(modules != packages);

		for (Path pkg : list(out, "th-package-")) {
			String html = read(pkg);
			assertThat(tableRows(html, "testcase_table")).as("test case rows in " + pkg.getFileName()).isPositive();
			assertThat(tableRows(html, "spread_table")).as("spread rows in " + pkg.getFileName()).isPositive();
			assertThat(html).doesNotContain(">null<");
		}

		for (Path page : list(out, "")) {
			if (page.toString().endsWith(".html")) {
				assertThat(read(page)).as("NaN in " + page.getFileName()).doesNotContain("NaN");
			}
		}

		assertGolden(scenario, metrics(out));
	}

	@Test
	void ctrfPackagesComeFromEnvironmentProjectName(@TempDir Path out) throws IOException {
		generate(out, "json", true, UnitTHConfig.DEFAULT_MAX_RUNS);
		assertThat(list(out, "th-package-")).extracting(p -> p.getFileName().toString())
				.contains("th-package-zbrainiac-labs_SyntheticRetailBank.html",
						"th-package-zbrainiac-labs_sharing_any_objects.html")
				.doesNotContain("th-package-not-in-package.html");
	}

	@Test
	void runDatesComeFromFolderNames(@TempDir Path out) throws IOException {
		generate(out, "json", false, UnitTHConfig.DEFAULT_MAX_RUNS);
		assertThat(uniqueDates(read(out.resolve("index.html")))).containsExactly(
				"2026-04-29&nbsp;13:17:07", "2026-04-29&nbsp;13:46:37", "2026-04-30&nbsp;04:33:35");
	}

	@Test
	void maxRunsKeepsMostRecentRuns(@TempDir Path out) throws IOException {
		generate(out, "json", false, 2);
		String index = read(out.resolve("index.html"));
		assertThat(runsKpi(index)).isEqualTo(2);
		assertThat(uniqueDates(index)).containsExactly(
				"2026-04-29&nbsp;13:46:37", "2026-04-30&nbsp;04:33:35");
	}

	@Test
	void missingFolderFails(@TempDir Path out) {
		UnitTH th = new UnitTH(config(out, "json", UnitTHConfig.DEFAULT_MAX_RUNS));
		assertThat(th.run(out.resolve("does-not-exist").toString())).isEqualTo(UnitTH.FAILED);
	}

	// --- helpers -----------------------------------------------------------

	static UnitTHConfig config(Path out, String format, int maxRuns) {
		Properties p = new Properties();
		p.setProperty(UnitTHConfig.PROP_REPORT_DIR, out.toString());
		p.setProperty(UnitTHConfig.PROP_REPORT_FORMAT, format);
		p.setProperty(UnitTHConfig.PROP_XML_REPORT_FILTER, "TEST_");
		p.setProperty(UnitTHConfig.PROP_JSON_REPORT_FILTER, "TEST_");
		p.setProperty(UnitTHConfig.PROP_GEN_EXECTIMEGRAPHS, "true");
		return UnitTHConfig.load(null, null, p).withMaxRuns(maxRuns);
	}

	static int generate(Path out, String format, boolean ctrf, int maxRuns) throws IOException {
		List<String> args = new ArrayList<>();
		if (ctrf) {
			args.add("--ctrf");
		}
		try (Stream<Path> s = Files.list(SAMPLES)) {
			s.filter(Files::isDirectory).map(Path::toString).sorted().forEach(args::add);
		}
		return new UnitTH(config(out, format, maxRuns)).run(args.toArray(new String[0]));
	}

	static String read(Path p) throws IOException {
		return Files.readString(p, StandardCharsets.UTF_8);
	}

	static List<Path> list(Path dir, String prefix) throws IOException {
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(p -> p.getFileName().toString().startsWith(prefix)).sorted().toList();
		}
	}

	static int runsKpi(String index) {
		Matcher m = RUNS_KPI.matcher(index);
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	static TreeSet<String> uniqueDates(String html) {
		TreeSet<String> dates = new TreeSet<>();
		Matcher m = RUN_DATE.matcher(html);
		while (m.find()) {
			dates.add(m.group());
		}
		return dates;
	}

	/** Counts body rows of the first table with the given id. */
	static int tableRows(String html, String tableId) {
		int start = html.indexOf("id=\"" + tableId + "\"");
		if (start < 0) {
			return 0;
		}
		int body = html.indexOf("<tbody>", start);
		int end = html.indexOf("</tbody>", body);
		if (body < 0 || end < 0) {
			return 0;
		}
		String rows = html.substring(body, end);
		int count = 0;
		for (int i = rows.indexOf("<tr"); i >= 0; i = rows.indexOf("<tr", i + 1)) {
			count++;
		}
		return count;
	}

	/** Stable, timestamp-free summary of the report used as golden snapshot. */
	static String metrics(Path out) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("runs=").append(runsKpi(read(out.resolve("index.html")))).append('\n');
		for (Path p : list(out, "th-")) {
			String html = read(p);
			sb.append(p.getFileName()).append(" testcases=").append(tableRows(html, "testcase_table"))
					.append(" spread=").append(tableRows(html, "spread_table")).append('\n');
		}
		return sb.toString();
	}

	static void assertGolden(String scenario, String actual) throws IOException {
		Path file = GOLDEN.resolve(scenario + ".txt");
		if (UPDATE_GOLDEN || !Files.exists(file)) {
			Files.createDirectories(GOLDEN);
			Files.writeString(file, actual, StandardCharsets.UTF_8);
			return;
		}
		assertThat(actual).as("golden metrics for " + scenario + " (regenerate with -Dunitth.updateGolden=true)")
				.isEqualTo(read(file));
	}
}
