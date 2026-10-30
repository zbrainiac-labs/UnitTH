/*
 * MIT License - see LICENSE.txt
 */
package unitth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import unitth.core.UnitTH;

class FileCollectionTest {

	private static final String CTRF = "{\"results\": {\"environment\": {\"projectName\": \"o_r_S\"},"
			+ " \"tests\": [{\"name\": \"t\", \"status\": \"passed\", \"duration\": 1}]}}";

	@Test
	void skipsEmptyFoldersAndNonMatchingFiles(@TempDir Path tmp) throws Exception {
		Path out = tmp.resolve("out");
		Path empty = Files.createDirectories(tmp.resolve("in/2026-04-27T100000Z"));
		Path wrongPrefix = Files.createDirectories(tmp.resolve("in/2026-04-27T110000Z"));
		Files.writeString(wrongPrefix.resolve("OTHER_x.json"), CTRF);
		Path good = Files.createDirectories(tmp.resolve("in/2026-04-27T120000Z"));
		Files.writeString(good.resolve("TEST_x.json"), CTRF);
		Files.writeString(good.resolve("TEST_x.txt"), "ignored");

		UnitTH th = new UnitTH(ReportE2EIT.config(out, "json", 100));
		assertThat(th.run("--ctrf", empty.toString(), wrongPrefix.toString(), good.toString()))
				.isEqualTo(UnitTH.OK);
		assertThat(th.getHistory().getNoRuns()).isEqualTo(1);
		assertThat(th.getHistory().getNoExecutedTestCases()).isEqualTo(1);
	}

	@Test
	void hostileNamesAreEscapedAndFileNamesAreSafe(@TempDir Path tmp) throws Exception {
		String ctrf = "{\"results\": {\"environment\": {\"projectName\": \"evil/../x<b>_S\"},"
				+ " \"tests\": [{\"name\": \"<script>alert(1)</script> >= 90\", \"status\": \"passed\", \"duration\": 1}]}}";
		Path run = Files.createDirectories(tmp.resolve("in/2026-04-27T120000Z"));
		Files.writeString(run.resolve("TEST_x.json"), ctrf);
		Path out = tmp.resolve("out");

		assertThat(new UnitTH(ReportE2EIT.config(out, "json", 100)).run("--ctrf", run.toString()))
				.isEqualTo(UnitTH.OK);

		assertThat(ReportE2EIT.list(out, "th-package-")).extracting(p -> p.getFileName().toString())
				.containsExactly("th-package-evil_.._x_b_.html");
		for (Path page : ReportE2EIT.list(out, "")) {
			if (page.toString().endsWith(".html")) {
				String html = ReportE2EIT.read(page);
				assertThat(html).as(page.getFileName().toString())
						.doesNotContain("<script>alert(1)")
						.doesNotContain("x<b>");
			}
		}
		assertThat(ReportE2EIT.read(out.resolve("th-package-evil_.._x_b_.html")))
				.contains("&lt;script&gt;alert(1)&lt;/script&gt; &gt;= 90");
		assertThat(out.toFile().list()).as("nothing written outside the report folder")
				.allMatch(n -> !n.contains("/"));
		assertThat(tmp.resolve("x<b>_S")).doesNotExist();
	}

	@Test
	void noMatchingFilesFails(@TempDir Path tmp) throws Exception {
		Path empty = Files.createDirectories(tmp.resolve("in/2026-04-27T100000Z"));
		UnitTH th = new UnitTH(ReportE2EIT.config(tmp.resolve("out"), "json", 100));
		assertThat(th.run(empty.toString())).isEqualTo(UnitTH.FAILED);
	}
}
