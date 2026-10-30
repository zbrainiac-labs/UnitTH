/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import unitth.core.UnitTHException;

class TestModelTest {

	@ParameterizedTest
	@CsvSource({
			"zbrainiac-labs_project-one.SQLValidation, zbrainiac-labs_project-one",
			"a.b.c, a.b",
			"NoDotsHere, not-in-package" })
	void moduleNameToPackageName(String moduleName, String expected) {
		TestModule m = new TestModule();
		m.setName(moduleName);
		assertThat(m.getPackageName()).isEqualTo(expected);
	}

	@Test
	void unnamedItemIsNotInPackage() {
		assertThat(new TestItem().getPackageName()).isEqualTo("not-in-package");
	}

	private static TestCase tc(String module, String name, TestCaseVerdict v, String time) {
		TestCase tc = new TestCase();
		tc.setName(name);
		tc.setClassName(module);
		tc.setModuleName(module);
		tc.setVerdict(v);
		tc.setExecutionTime(time);
		return tc;
	}

	@Test
	void fullNameCombinesModuleClassAndName() {
		assertThat(tc("p.M", "t1", TestCaseVerdict.e_PASS, "1").getFullName()).isEqualTo("p.M::p.M::t1");
	}

	@Test
	void summaryPackageComesFromDottedClassName() {
		TestCaseSummary s = new TestCaseSummary(tc("org_repo.SQLValidation", "t", TestCaseVerdict.e_PASS, "1"), 1);
		assertThat(s.getPackageName()).isEqualTo("org_repo");
	}

	@Test
	void summaryCountsVerdictsAndSpread() throws UnitTHException {
		TestCaseSummary s = new TestCaseSummary(tc("p.M", "t", TestCaseVerdict.e_PASS, "1.0"), 1);
		s.increment(tc("p.M", "t", TestCaseVerdict.e_FAIL, "2.0"), 2);
		s.increment(tc("p.M", "t", TestCaseVerdict.e_IGNORED, "0"), 3);
		s.increment(tc("p.M", "t", TestCaseVerdict.e_ERROR, "0"), 5);

		assertThat(s.getNoRuns()).isEqualTo(4);
		assertThat(s.getNoPassed()).isEqualTo(1);
		assertThat(s.getNoFailures()).isEqualTo(1);
		assertThat(s.getNoIgnored()).isEqualTo(1);
		assertThat(s.getNoErrors()).isEqualTo(1);
		assertThat(s.getSpreadAt(2)).isEqualTo(TestCaseVerdict.e_FAIL);
		assertThat(s.getSpreadAt(5)).isEqualTo(TestCaseVerdict.e_ERROR);
		assertThat(s.getSpread()).doesNotContainKey(4);
	}

	@Test
	void summaryRejectsOtherTestCase() {
		TestCaseSummary s = new TestCaseSummary(tc("p.M", "t", TestCaseVerdict.e_PASS, "1"), 1);
		assertThatThrownBy(() -> s.increment(tc("p.M", "other", TestCaseVerdict.e_PASS, "1"), 2))
				.isInstanceOf(UnitTHException.class);
	}
}
