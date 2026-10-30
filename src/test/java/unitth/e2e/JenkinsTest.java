/*
 * MIT License - see LICENSE.txt
 */
package unitth.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import unitth.core.UnitTH;
import unitth.core.UnitTHConfig;

/** Jenkins mode (-j) on a synthetic builds folder with junitResult.xml files. */
class JenkinsTest {

	private static String junitResult(String timestamp, boolean failSecond) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<result>
				  <suites>
				    <suite>
				      <name>org.Suite</name>
				      <duration>1.5</duration>
				      <timestamp>%s</timestamp>
				      <cases>
				        <case>
				          <duration>0.5</duration>
				          <className>org.Suite</className>
				          <testName>first</testName>
				          <failedSince>0</failedSince>
				        </case>
				        <case>
				          <duration>1.0</duration>
				          <className>org.Suite</className>
				          <testName>second</testName>
				          <failedSince>%s</failedSince>
				        </case>
				      </cases>
				    </suite>
				  </suites>
				</result>
				""".formatted(timestamp, failSecond ? "2" : "0");
	}

	@Test
	void parsesJenkinsBuildFolders(@TempDir Path tmp) throws Exception {
		Path builds = tmp.resolve("builds");
		Files.writeString(Files.createDirectories(builds.resolve("1")).resolve("junitResult.xml"),
				junitResult("2026-04-27T11:00:25", false));
		Files.writeString(Files.createDirectories(builds.resolve("2")).resolve("junitResult.xml"),
				junitResult("2026-04-28T11:00:25", true));

		Properties p = new Properties();
		p.setProperty(UnitTHConfig.PROP_REPORT_DIR, tmp.resolve("out").toString());
		p.setProperty(UnitTHConfig.PROP_REPORT_FORMAT, "xml");
		p.setProperty(UnitTHConfig.PROP_XML_REPORT_FILTER, "junitResult");
		UnitTH th = new UnitTH(UnitTHConfig.load(null, null, p));

		assertThat(th.run("-j", builds.toString())).isEqualTo(UnitTH.OK);
		assertThat(th.getHistory().getNoRuns()).isEqualTo(2);
		assertThat(th.getHistory().getTestPackageSummaries()).containsKey("org");
		assertThat(tmp.resolve("out/index.html")).exists();
	}

	@Test
	void missingBuildsFolderFails(@TempDir Path tmp) {
		UnitTH th = new UnitTH(ReportE2EIT.config(tmp.resolve("out"), "xml", 100));
		assertThat(th.run("-j", tmp.resolve("nope").toString())).isEqualTo(UnitTH.FAILED);
	}
}
