/*
 * MIT License - see LICENSE.txt
 */
package unitth.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import unitth.core.Log;
import unitth.core.UnitTH;

/** Error paths must be reported and must not abort the whole report. */
class ErrorPathTest {

	private static final String CTRF = "{\"results\": {\"environment\": {\"projectName\": \"o_r_S\"},"
			+ " \"tests\": [{\"name\": \"t\", \"status\": \"passed\", \"duration\": 1}]}}";

	@Test
	void logWritesWithoutThrowing() {
		assertThatCode(() -> {
			Log.error("plain message");
			Log.error("with cause", new IllegalStateException("boom"));
		}).doesNotThrowAnyException();
	}

	@Test
	void malformedXmlIsSkippedAndOtherRunsStillReported(@TempDir Path tmp) throws Exception {
		Path bad = Files.createDirectories(tmp.resolve("in/2026-04-27T100000Z"));
		Files.writeString(bad.resolve("TEST_bad.xml"), "<testsuite name=\"x\"><testcase");
		Path good = Files.createDirectories(tmp.resolve("in/2026-04-27T110000Z"));
		Files.writeString(good.resolve("TEST_good.json"), CTRF);

		UnitTH th = new UnitTH(ReportE2EIT.config(tmp.resolve("out"), "both", 100));
		assertThat(th.run(bad.toString(), good.toString())).isEqualTo(UnitTH.OK);
		assertThat(th.getHistory().getNoExecutedTestCases()).isEqualTo(1);
	}

	@Test
	void unwritablePageIsReportedAndOtherPagesAreWritten(@TempDir Path tmp) throws Exception {
		Path run = Files.createDirectories(tmp.resolve("in/2026-04-27T110000Z"));
		Files.writeString(run.resolve("TEST_x.json"), CTRF);
		Path out = tmp.resolve("out");
		// A directory where index.html and the package page should go makes those writes fail.
		Files.createDirectories(out.resolve("index.html"));
		Files.createDirectories(out.resolve("th-package-o_r.html"));
		Files.createDirectories(out.resolve("th-module-o_r.S.html"));

		UnitTH th = new UnitTH(ReportE2EIT.config(out, "json", 100));
		assertThatCode(() -> th.run("--ctrf", run.toString())).doesNotThrowAnyException();
		assertThat(out.resolve("css/style.css")).exists();
	}

	@Test
	void reportFolderThatIsAFileIsReported(@TempDir Path tmp) throws Exception {
		Path run = Files.createDirectories(tmp.resolve("in/2026-04-27T110000Z"));
		Files.writeString(run.resolve("TEST_x.json"), CTRF);
		Path out = Files.writeString(tmp.resolve("out"), "not a folder");

		UnitTH th = new UnitTH(ReportE2EIT.config(out, "json", 100));
		assertThatCode(() -> th.run("--ctrf", run.toString())).doesNotThrowAnyException();
		assertThat(out).isRegularFile();
	}
}
