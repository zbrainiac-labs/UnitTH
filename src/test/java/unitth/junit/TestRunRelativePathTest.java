/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import unitth.core.UnitTHConfig;

/** Characterization of {@link TestRun#calcRelativePath}, recorded before its refactoring. */
class TestRunRelativePathTest {

	@ParameterizedTest
	@CsvSource({
		"/a/b/c, /a/b/d, ../c",
		"/a/b/c/, /a/x/y/z, ../../../b/c",
		"/root/proj/reports, /root/proj/out, ../reports",
		"/x/report, /x/report/html, ..",
		"/same/dir, /same/dir, ..",
		"/a, /b, ..//a",
		"/data/runs/2026-01-01, /data/html/th, ../../runs/2026-01-01",
		"abc, abd, ../abc"
	})
	void relativePath(String from, String to, String expected) {
		assertThat(new TestRun(UnitTHConfig.defaults()).calcRelativePath(from, to)).isEqualTo(expected);
	}
}
