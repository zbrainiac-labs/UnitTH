/*
 * MIT License - see LICENSE.txt
 */
package unitth.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class UnitTHConfigTest {

	private static Properties props(String... kv) {
		Properties p = new Properties();
		for (int i = 0; i < kv.length; i += 2) {
			p.setProperty(kv[i], kv[i + 1]);
		}
		return p;
	}

	@Test
	void defaults() {
		UnitTHConfig c = UnitTHConfig.load(null, null, new Properties());
		assertThat(c).isEqualTo(UnitTHConfig.defaults());
		assertThat(c.rootFolder()).isEqualTo("report.th");
		assertThat(c.xmlReportFilter()).isEqualTo("TEST-");
		assertThat(c.jsonReportFilter()).isEqualTo("TEST_");
		assertThat(c.reportFormat()).isEqualTo("json");
		assertThat(c.maxRuns()).isEqualTo(100);
		assertThat(c.debug()).isFalse();
	}

	@Test
	void runFileOverridesDefaults() {
		UnitTHConfig c = UnitTHConfig.load(props(UnitTHConfig.PROP_REPORT_DIR, "out",
				UnitTHConfig.PROP_USE_ABS_PATHS, "yes"), null, new Properties());
		assertThat(c.rootFolder()).isEqualTo("out");
		assertThat(c.useAbsPaths()).isTrue();
	}

	@Test
	void systemPropertiesOverrideFile() {
		UnitTHConfig c = UnitTHConfig.load(props(UnitTHConfig.PROP_REPORT_FORMAT, "xml"), null,
				props(UnitTHConfig.PROP_REPORT_FORMAT, "both", UnitTHConfig.PROP_DEBUG, "TRUE"));
		assertThat(c.reportFormat()).isEqualTo("both");
		assertThat(c.debug()).isTrue();
	}

	@Test
	void runFileShadowsHomeFile() {
		UnitTHConfig c = UnitTHConfig.load(props(UnitTHConfig.PROP_REPORT_DIR, "run"),
				props(UnitTHConfig.PROP_REPORT_DIR, "home", UnitTHConfig.PROP_XML_REPORT_FILTER, "H_"),
				new Properties());
		assertThat(c.rootFolder()).isEqualTo("run");
		assertThat(c.xmlReportFilter()).as("home file ignored when run file exists").isEqualTo("TEST-");
	}

	@Test
	void homeFileUsedWithoutRunFile() {
		UnitTHConfig c = UnitTHConfig.load(null, props(UnitTHConfig.PROP_REPORT_DIR, "home"), new Properties());
		assertThat(c.rootFolder()).isEqualTo("home");
	}

	@Test
	void emptyFileValueFallsBackToDefault() {
		UnitTHConfig c = UnitTHConfig.load(props(UnitTHConfig.PROP_REPORT_DIR, ""), null, new Properties());
		assertThat(c.rootFolder()).isEqualTo("report.th");
	}

	@Test
	void invalidBooleanIsFalse() {
		UnitTHConfig c = UnitTHConfig.load(null, null, props(UnitTHConfig.PROP_GEN_EXECTIMEGRAPHS, "maybe"));
		assertThat(c.generateExecTimeGraphs()).isFalse();
	}

	@Test
	void witherAndInputArgsAreImmutable() {
		UnitTHConfig base = UnitTHConfig.defaults();
		UnitTHConfig c = base.withMaxRuns(5).withInputArgs(List.of("a", "b"));
		assertThat(base.maxRuns()).isEqualTo(100);
		assertThat(c.maxRuns()).isEqualTo(5);
		assertThat(c.inputArgs()).containsExactly("a", "b");
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> c.inputArgs().add("x"))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
