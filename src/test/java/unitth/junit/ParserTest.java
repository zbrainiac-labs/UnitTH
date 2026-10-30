/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import unitth.core.UnitTHConfig;

class ParserTest {

	@TempDir
	Path tmp;

	private final UnitTHConfig config = UnitTHConfig.defaults();

	private File write(String folder, String file, String content) throws IOException {
		Path dir = Files.createDirectories(tmp.resolve(folder));
		return Files.writeString(dir.resolve(file), content).toFile();
	}

	private static String ctrf(String projectName, String testsJson) {
		String env = projectName == null ? "" : "\"environment\": {\"projectName\": \"" + projectName + "\"},";
		return "{\"results\": {" + env + "\"tests\": [" + testsJson + "]}}";
	}

	private static String test(String name, String status, long durationMs, String suite) {
		return "{\"name\": \"" + name + "\", \"status\": \"" + status + "\", \"duration\": " + durationMs
				+ (suite == null ? "" : ", \"suite\": \"" + suite + "\"") + "}";
	}

	private TestHistory parse(unitth.core.ReportParser parser, File... files) {
		ArrayList<ArrayList<File>> runs = new ArrayList<>();
		ArrayList<String> dirs = new ArrayList<>();
		for (File f : files) {
			runs.add(new ArrayList<>(List.of(f)));
			dirs.add(f.getParent());
		}
		TestHistory h = parser.parseRuns(runs, dirs);
		h.calcStats();
		return h;
	}

	@Test
	void ctrfUsesEnvironmentProjectNameForPackage() throws IOException {
		File f = write("2026-04-27T182826Z", "TEST_a.json", ctrf("org_repo_SQLValidation",
				test("t1", "passed", 1500, "SQLValidation") + "," + test("t2", "failed", 500, "SQLValidation")
						+ "," + test("t3", "skipped", 0, "SQLValidation")));

		TestHistory h = parse(new CTRFReportParser(config), f);

		assertThat(h.getTestPackageSummaries()).containsOnlyKeys("org_repo");
		assertThat(h.getTestModuleSummaries()).containsOnlyKeys("org_repo.SQLValidation");
		TestModule m = h.getRuns().first().getTestPackages().get("org_repo").getTestModules().values().iterator().next();
		assertThat(m.getNoTestCases()).isEqualTo(3);
		assertThat(m.getNoFailures()).isEqualTo(1);
		assertThat(m.getNoIgnored()).isEqualTo(1);
		assertThat(m.getExecutionTimeDouble()).isEqualTo(2.0);
		assertThat(m.getTestCaseByName("t1").getVerdict()).isEqualTo(TestCaseVerdict.e_PASS);
		assertThat(m.getTestCaseByName("t2").getVerdict()).isEqualTo(TestCaseVerdict.e_FAIL);
		assertThat(m.getTestCaseByName("t3").getVerdict()).isEqualTo(TestCaseVerdict.e_IGNORED);
	}

	@Test
	void ctrfFallsBackToSuiteThenDefault() throws IOException {
		File a = write("2026-04-27T110000Z", "TEST_a.json", ctrf(null, test("t1", "passed", 1, "own_Suite")));
		File b = write("2026-04-27T120000Z", "TEST_b.json", ctrf(null, test("t1", "passed", 1, null)));

		TestHistory h = parse(new CTRFReportParser(config), a, b);

		assertThat(h.getTestModuleSummaries()).containsOnlyKeys("own.Suite", "default.SQLValidation");
	}

	@Test
	void ctrfRunDateComesFromFolderName() throws IOException {
		File f = write("2026-04-27T182826Z", "TEST_a.json", ctrf("o_r_S", test("t", "passed", 1, null)));
		TestHistory h = parse(new CTRFReportParser(config), f);
		assertThat(h.getRuns().first().getExecutionDate()).isEqualTo("2026-04-27&nbsp;18:28:26");
	}

	@Test
	void ctrfMalformedFileDoesNotThrow() throws IOException {
		File bad = write("2026-04-27T182826Z", "TEST_bad.json", "{ not json");
		TestHistory h = parse(new CTRFReportParser(config), bad);
		assertThat(h.getNoExecutedTestCases()).isZero();
	}

	@Test
	void defaultParserUsesProjectNameForJsonToo() throws IOException {
		File f = write("2026-04-27T182826Z", "TEST_a.json", ctrf("org_repo_SQLValidation",
				test("t1", "passed", 1, "SQLValidation")));

		TestHistory h = parse(new JUnitReportParser(config), f);

		assertThat(h.getTestPackageSummaries()).containsOnlyKeys("org_repo");
		assertThat(h.getTestModuleSummaries()).containsOnlyKeys("org_repo.SQLValidation");
		assertThat(h.getRuns().first().getExecutionDate()).isEqualTo("2026-04-27&nbsp;18:28:26");
	}

	@Test
	void junitXmlCountsVerdicts() throws IOException {
		File f = write("2026-04-27T110025Z", "TEST_x.xml", """
				<?xml version="1.0" encoding="UTF-8"?>
				<testsuite name="org.Suite" tests="3" failures="1" errors="0" skipped="1" time="3.0"
				           timestamp="2026-04-27T11:00:25">
				  <testcase name="ok" classname="org.Suite" time="1.0"/>
				  <testcase name="bad" classname="org.Suite" time="1.0"><failure message="x"/></testcase>
				  <testcase name="skip" classname="org.Suite" time="1.0"><skipped/></testcase>
				</testsuite>
				""");

		TestHistory h = parse(new JUnitReportParser(config), f);

		assertThat(h.getTestPackageSummaries()).containsOnlyKeys("org");
		assertThat(h.getNoExecutedTestCases()).isEqualTo(3);
		TestModule m = h.getRuns().first().getTestPackages().get("org").getTestModules().values().iterator().next();
		assertThat(m.getNoFailures()).isEqualTo(1);
		assertThat(m.getNoIgnored()).isEqualTo(1);
		assertThat(m.getTestCaseByName("bad").getVerdict()).isEqualTo(TestCaseVerdict.e_FAIL);
	}

	@Test
	void junitXmlSuiteLevelSkippedDoesNotCrash() throws IOException {
		File f = write("2026-04-27T110025Z", "TEST_s.xml", """
				<?xml version="1.0" encoding="UTF-8"?>
				<testsuite name="org.Suite" tests="1" failures="0" errors="0" skipped="1" time="1.0">
				  <skipped/>
				  <testcase name="t" classname="org.Suite" time="1.0"><skipped/></testcase>
				</testsuite>
				""");

		TestHistory h = parse(new JUnitReportParser(config), f);

		TestModule m = h.getRuns().first().getTestPackages().get("org").getTestModules().values().iterator().next();
		assertThat(m.getTestCaseByName("t").getVerdict()).isEqualTo(TestCaseVerdict.e_IGNORED);
	}

	@Test
	void historyAggregatesUniqueTestCasesAcrossRuns() throws IOException {
		File r1 = write("2026-04-27T110000Z", "TEST_1.json", ctrf("o_r_S",
				test("t1", "passed", 1, null) + "," + test("t2", "failed", 1, null)));
		File r2 = write("2026-04-27T120000Z", "TEST_2.json", ctrf("o_r_S",
				test("t1", "passed", 1, null) + "," + test("t2", "passed", 1, null)));

		TestHistory h = parse(new CTRFReportParser(config), r1, r2);

		assertThat(h.getNoRuns()).isEqualTo(2);
		assertThat(h.getNoUniqueTestCases()).isEqualTo(2);
		TestCaseSummary t2 = h.getTestCaseSummary("o_r.S::o_r.S::t2");
		assertThat(t2.getNoRuns()).isEqualTo(2);
		assertThat(t2.getNoPassed()).isEqualTo(1);
		assertThat(t2.getNoFailures()).isEqualTo(1);
		assertThat(h.getTestPackageSummary("o_r").getPassPctDouble()).isEqualTo(75.0);
	}
}
