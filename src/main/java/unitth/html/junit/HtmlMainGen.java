/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import unitth.core.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import unitth.html.HtmlGenUtils;
import unitth.junit.TestHistory;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackageSummary;
import unitth.junit.TestRun;

public class HtmlMainGen extends HtmlGen {

	public HtmlMainGen(TestHistory history, boolean generateExecTimeGraphs) {
		super(history, generateExecTimeGraphs);
	}

	@Override
	public void generateHtmlHistory() {
		String fileName = destDir + File.separator + HTML_MAIN;
		try (BufferedWriter out = createFile(fileName)) {
			startSortablePage(out, "UnitTestHistory - Test History", "run_table", "package_table", "module_table");
			startMain(out);
			writeNav(out, "runs", "Runs", "packages", "Packages", "modules", MODULES, "totals", "Totals");
			generateKpiStrip(out);
			writeDashboardCharts(out, k -> "chart_main_" + k, chartSeries(tr -> tr), getExecutionTimeUnit(null));
			generateRunSection(out);
			generatePackageSection(out);
			generateModuleSection(out);
			generateTotals(out);
			endMain(out, HtmlGenUtils.createArgsDivTag(th.getConfig()));
		} catch (IOException e) {
			Log.error("Error writing " + fileName, e);
		}
	}

	private void generateKpiStrip(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<div class=\"kpi-strip\">" + LF);
		writeKpi(buf, "Total Runs", String.valueOf(th.getNoRuns()), "", "accent");
		writeKpi(buf, TEST_CASES, String.valueOf(th.getNoUniqueTestCases()), th.getNoIgnoredTestCases() + " ignored", "");
		writeKpi(buf, MODULES, String.valueOf(th.getNoUniqueModules()), th.getNoPackages() + " package(s)", "");
		writePassRateKpi(buf, th.getAvePassPctDouble(), th.getAvePassPct(), th.getBestRun(), th.getWorstRun());
		String[] parts = th.getLastRun().split("&nbsp;");
		String lastRun = parts.length == 2 ? parts[0] + "<br>" + parts[1] : th.getLastRun();
		writeKpi(buf, "Last Run", lastRun, "", "");
		buf.write(t(2) + DIV_END + LF);
	}

	private void generateRunSection(BufferedWriter buf) throws IOException {
		writeSection(buf, "runs", "Runs", th.getNoRuns() + " runs", "Click on a run to open its HTML test report.");
		startTable(buf, "run_table", 4, "Run", "Path", "Tests", PASS, ERROR, FAIL, IGNORED, "Exec Date", EXEC_TIME, TOTAL);
		int idx = th.getNoRuns();
		for (TestRun tr : th.getRuns()) {
			writeRunRow(buf, tr, tr, idx, true);
			idx--;
		}
		endTable(buf);
	}

	private void generatePackageSection(BufferedWriter buf) throws IOException {
		writeSection(buf, "packages", "Packages", th.getNoPackages() + " packages",
			"Statistics per test package. Click the package name for details.");
		startTable(buf, "package_table", 4, withNameColumns("Package Name", "Runs", MODULES));
		for (TestPackageSummary tps : th.getTestPackageSummaries().values()) {
			writePackageRow(buf, tps);
		}
		endTable(buf);
	}

	private void generateModuleSection(BufferedWriter buf) throws IOException {
		writeSection(buf, "modules", MODULES, th.getNoUniqueModules() + " modules",
			"Average pass rate per test module. Click the module name for details.");
		startTable(buf, "module_table", 4, withNameColumns("Module Name", "Runs"));
		for (TestModuleSummary tms : th.getTestModuleSummaries().values()) {
			writeModuleRow(buf, tms);
		}
		endTable(buf);
	}

	/** Leading name columns followed by the shared summary count headers. */
	static String[] withNameColumns(String... first) {
		String[] all = new String[first.length + SUMMARY_COUNT_HEADERS.length];
		System.arraycopy(first, 0, all, 0, first.length);
		System.arraycopy(SUMMARY_COUNT_HEADERS, 0, all, first.length, SUMMARY_COUNT_HEADERS.length);
		return all;
	}

	private void generateTotals(BufferedWriter buf) throws IOException {
		writeSection(buf, "totals", "Execution Totals", null, "Aggregate of all executed tests across all runs.");
		startTable(buf, null, 4, "Run", "Tests", PASS, ERROR, FAIL, IGNORED, EXEC_TIME, TOTAL);
		buf.write(t(4) + "<tr>" + LF);
		buf.write(t(5) + "<td><strong>TOTALS</strong>" + TD_END);
		buf.write(t(5) + "<td>" + th.getNoExecutedTestCases() + TD_END);
		buf.write(t(5) + "<td>" + th.getNoPassedTestCases() + TD_END);
		buf.write(t(5) + "<td>" + th.getNoErrorTestCases() + TD_END);
		buf.write(t(5) + "<td>" + th.getNoFailedTestCases() + TD_END);
		buf.write(t(5) + "<td>" + th.getNoIgnoredTestCases() + TD_END);
		buf.write(t(5) + TD_MONO + th.getTotalExecutionTime() + TD_END);
		buf.write(t(5) + "<td class=\"text-right\"><strong>" + pctText(th.getAvePassPct()) + "</strong>" + TD_END);
		buf.write(t(5) + "<td class=\"graphBar\">" + pctBarHtml(th.getAvePassPctDouble(), th.getAvePassPct()) + TD_END);
		buf.write(t(4) + TR_END + LF);
		endTable(buf);
	}
}
