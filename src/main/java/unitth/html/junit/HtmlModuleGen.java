/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import unitth.core.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import unitth.junit.TestCaseSummary;
import unitth.junit.TestHistory;
import unitth.junit.TestModuleSummary;

public class HtmlModuleGen extends HtmlGen {

	public HtmlModuleGen(TestHistory history, boolean generateExecTimeGraphs) {
		super(history, generateExecTimeGraphs);
	}

	@Override
	public void generateHtmlHistory() {
		for (Object moduleName : th.getUniqueModules()) {
			generateModuleRunPage((String) moduleName);
		}
	}

	private void generateModuleRunPage(String moduleName) {
		String fileName = destDir + File.separator + HTMLMODULEOUTPUTFILE_PREFIX + fileSafe(moduleName) + HTML_SUFFIX;
		try (BufferedWriter out = createFile(fileName)) {
			startSortablePage(out, "UnitTH - " + moduleName, "run_table", "testcase_table", "spread_table");
			startMain(out);
			writeBreadcrumb(out, moduleName);
			writeNav(out, "runs", "Runs", ANCHOR_TESTCASES, TEST_CASES, "spread", "Verdict Spread");
			generateSummary(out, th.getTestModuleSummary(moduleName));

			writeSection(out, "runs", "Module Runs", null, "Click on a run to open its HTML test report.");
			writeRunTable(out, tr -> tr.getTestModules().get(moduleName));

			List<TestCaseSummary> cases = th.getTestCaseSummaries().values().stream()
				.filter(tcs -> tcs.getClassName().equals(moduleName) || tcs.getModuleName().equals(moduleName)).toList();
			writeSection(out, ANCHOR_TESTCASES, TEST_CASES, null, "Average pass rate and execution times for all test cases in this module.");
			writeTestCaseTable(out, cases, TestCaseSummary::getTestCaseName, 3);
			writeSpreadTable(out, cases, TestCaseSummary::getTestCaseName, 3);
			endMain(out, "");
		} catch (IOException e) {
			Log.error("Error writing " + fileName, e);
		}
	}

	private void generateSummary(BufferedWriter buf, TestModuleSummary tms) throws IOException {
		buf.write(t(2) + "<h1>Module</h1>" + LF);
		buf.write(t(2) + "<h2>" + esc(tms.getName()) + "</h2>" + LF);
		buf.write(t(2) + "<div class=\"kpi-strip\">" + LF);
		writeKpi(buf, "Runs", String.valueOf(tms.getNoRuns()), "", "accent");
		writeKpi(buf, TEST_CASES, String.valueOf(tms.getNoUniqueTestCases()), "", "");
		writePassRateKpi(buf, tms.getPassPctDouble(), tms.getPassPct(), tms.getBestRun(), tms.getWorstRun());
		buf.write(t(2) + DIV_END + LF);
		String chartId = fileSafe(tms.getName()).replace('.', '_');
		writeDashboardCharts(buf, k -> "chart_" + k + "_" + chartId,
			chartSeries(tr -> tr.getTestModule(tms.getPackageName(), tms.getName())), getExecutionTimeUnit(tms));
	}
}
