/*
 * MIT License - see LICENSE.txt
 */
package unitth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import unitth.core.UnitTH;
import unitth.junit.TestHistory;
import unitth.junit.TestGroupSummary;

/**
 * Trend KPIs compare the newest run with the newest run that is older than
 * 1/3/7 days. Run folders are dated relative to "now" so the windows hit.
 */
class TrendTest {

	private static final DateTimeFormatter FOLDER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss'Z'")
			.withZone(ZoneOffset.UTC);

	private static String ctrf(String tests) {
		return "{\"results\": {\"environment\": {\"projectName\": \"o_r_S\"}, \"tests\": [" + tests + "]}}";
	}

	private static String test(String name, String status) {
		return "{\"name\": \"" + name + "\", \"status\": \"" + status + "\", \"duration\": 1}";
	}

	private static Path run(Path root, Instant when, String json) throws Exception {
		Path dir = Files.createDirectories(root.resolve(FOLDER.format(when)));
		Files.writeString(dir.resolve("TEST_r.json"), json);
		return dir;
	}

	@Test
	void trendsCompareNewestRunWithOlderWindow(@TempDir Path tmp) throws Exception {
		Instant now = Instant.now();
		Path old = run(tmp.resolve("in"), now.minus(10, ChronoUnit.DAYS), ctrf(test("a", "passed")));
		Path recent = run(tmp.resolve("in"), now.minus(1, ChronoUnit.HOURS),
				ctrf(test("a", "passed") + "," + test("b", "failed")));

		UnitTH th = new UnitTH(ReportE2EIT.config(tmp.resolve("out"), "json", 100));
		assertThat(th.run("--ctrf", old.toString(), recent.toString())).isEqualTo(UnitTH.OK);
		TestHistory h = th.getHistory();

		for (String tc : new String[] { h.getTcTrendLastDay(), h.getTcTrendLast3Days(), h.getTcTrendLast7Days() }) {
			assertThat(tc).isEqualTo("+1");
		}
		for (String fn : new String[] { h.getFnTrendLastDay(), h.getFnTrendLast3Days(), h.getFnTrendLast7Days() }) {
			assertThat(fn).isEqualTo("+1");
		}
		assertThat(h.getPrTrendLast3Days()).endsWith("%").startsWith("-50");
		assertThat(h.getPrTrendLast7Days()).endsWith("%").startsWith("-50");

		for (TestGroupSummary<?> s : new TestGroupSummary<?>[] { h.getTestPackageSummary("o_r"),
				h.getTestModuleSummary("o_r.S") }) {
			assertThat(s.getTcTrendLastDay()).isEqualTo("+1");
			assertThat(s.getTcTrendLast3Days()).isEqualTo("+1");
			assertThat(s.getTcTrendLast7Days()).isEqualTo("+1");
			assertThat(s.getFnTrendLastDay()).isEqualTo("+1");
			assertThat(s.getFnTrendLast3Days()).isEqualTo("+1");
			assertThat(s.getFnTrendLast7Days()).isEqualTo("+1");
			assertThat(s.getPrTrendLast3Days()).startsWith("-50");
			assertThat(s.getPrTrendLast7Days()).startsWith("-50");
		}
	}

	@Test
	void trendsWithoutOlderRunAreNotAvailable(@TempDir Path tmp) throws Exception {
		Path recent = run(tmp.resolve("in"), Instant.now().minus(1, ChronoUnit.HOURS), ctrf(test("a", "passed")));
		UnitTH th = new UnitTH(ReportE2EIT.config(tmp.resolve("out"), "json", 100));
		assertThat(th.run("--ctrf", recent.toString())).isEqualTo(UnitTH.OK);
		assertThat(th.getHistory().getTcTrendLast7Days()).isEqualTo("NA");
		assertThat(th.getHistory().getPrTrendLast7Days()).isEqualTo("NA");
	}
}
