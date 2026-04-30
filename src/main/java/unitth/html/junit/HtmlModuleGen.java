/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import unitth.core.UnitTH;
import unitth.html.HtmlGenUtils;
import unitth.junit.TestCaseSummary;
import unitth.junit.TestHistory;
import unitth.junit.TestModule;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestRun;

public class HtmlModuleGen extends HtmlGen {

	public HtmlModuleGen(TestHistory history, boolean generateExecTimeGraphs) {
		super(history, generateExecTimeGraphs);
	}

	public void generateHtmlHistory() {
		generateModuleRunPages();
	}

	protected void generateModuleRunPages() {
		Object[] moduleNames = th.getUniqueModules();
		for (Object moduleName : moduleNames) {
			generateModuleRunPage((String) moduleName);
		}
	}

	private void generateModuleRunPage(String moduleName) {
		String fileName = destDir + File.separator
			+ HTMLMODULEOUTPUTFILE_PREFIX + moduleName
			+ HTMLMODULEOUTPUTFILE_SUFFIX;
		try {
			BufferedWriter out = createFile(fileName);
			startPageModule(out, "UnitTH - " + moduleName);
			startBody(out);
			generateHeader(out);
			out.write(t(1) + "<main class=\"container\">" + c_LF);
			generateBreadcrumb(out, moduleName);
			generateModuleAnchors(out);
			generateModuleModuleSummary(out, th.getTestModuleSummary(moduleName));
			generateModuleRunSection(out, moduleName);
			generateModuleTestCaseSection(out, moduleName);
			generateTestCaseSpreadInfo(out, true);
			generateModuleTestCaseSpreadList(out, moduleName);
			out.write(t(1) + "</main>" + c_LF);
			generateFooter(out);
			endBody(out);
			endPage(out);
		} catch (IOException e) {
			System.out.println("Error writing " + fileName);
			e.printStackTrace();
		}
	}

	private void generateBreadcrumb(BufferedWriter out, String moduleName) throws IOException {
		out.write(t(2) + "<nav class=\"breadcrumb\">" + c_LF);
		out.write(t(3) + "<a href=\"index.html\">\u2190 Dashboard</a>" + c_LF);
		out.write(t(3) + "<span class=\"separator\">/</span>" + c_LF);
		out.write(t(3) + "<span class=\"current\">" + moduleName + "</span>" + c_LF);
		out.write(t(2) + "</nav>" + c_LF);
	}

	private void generateModuleAnchors(BufferedWriter out) throws IOException {
		out.write(t(2) + "<nav class=\"nav-bar\">" + c_LF);
		out.write(t(3) + "<a href=\"#runs\">Runs</a>" + c_LF);
		out.write(t(3) + "<a href=\"#testcases\">Test Cases</a>" + c_LF);
		out.write(t(3) + "<a href=\"#spread\">Verdict Spread</a>" + c_LF);
		out.write(t(2) + "</nav>" + c_LF);
	}

	private void generateModuleModuleSummary(BufferedWriter buf, TestModuleSummary tms) throws IOException {
		buf.write(t(2) + "<h1>Module</h1>" + c_LF);
		buf.write(t(2) + "<h2>" + tms.getName() + "</h2>" + c_LF);

		buf.write(t(2) + "<div class=\"kpi-strip\">" + c_LF);
		writeKpi(buf, "Runs", String.valueOf(tms.getNoRuns()), "", "accent");
		writeKpi(buf, "Test Cases", String.valueOf(tms.getNoUniqueTestCases()), "", "");
		double avgPct = tms.getPassPctDouble();
		String pctColor = avgPct >= 99.0 ? "green" : avgPct >= 80.0 ? "accent" : "red";
		writeKpi(buf, "Avg Pass Rate", tms.getPassPct() + "%", "Best: " + tms.getBestRun() + "% / Worst: " + tms.getWorstRun() + "%", pctColor);
		buf.write(t(2) + "</div>" + c_LF);

		buf.write(t(2) + "<section class=\"dashboard\">" + c_LF);
		buf.write(t(3) + "<div class=\"dashboard-grid\">" + c_LF);

		String labels = buildRunLabelsJson();
		String prData = buildModulePassRateDataJson(tms.getPackageName(), tms.getName());
		String prFail = buildModuleFailDataJson(tms.getPackageName(), tms.getName());
		String tcData = buildModuleTcCountDataJson(tms.getPackageName(), tms.getName());
		String fnData = buildModuleFailCountDataJson(tms.getPackageName(), tms.getName());
		String etData = buildModuleExecTimeDataJson(tms.getPackageName(), tms.getName());
		String chartId = tms.getName().replace('.', '_');

		buf.write(t(4) + "<div class=\"card\">" + c_LF);
		buf.write(t(5) + "<h3 class=\"card-header\">Pass Rates</h3>" + c_LF);
		writeChartCanvas(buf, "chart_pr_" + chartId, 160);
		writeBarChart(buf, "chart_pr_" + chartId, "Pass %", labels, prData,
			"rgba(16,185,129,0.65)", "rgba(52,211,153,0.9)", prFail, 100);
		buf.write(t(5) + "<h3 class=\"card-header\">Test Case Count</h3>" + c_LF);
		writeChartCanvas(buf, "chart_tc_" + chartId, 100);
		writeAreaChart(buf, "chart_tc_" + chartId, "Test Cases", labels, tcData,
			"rgba(16,185,129,0.15)", "rgba(52,211,153,0.9)");
		buf.write(t(4) + "</div>" + c_LF);

		buf.write(t(4) + "<div class=\"card\">" + c_LF);
		if (generateExecTimeGraphs) {
			buf.write(t(5) + "<h3 class=\"card-header\">Execution Times (" + getExecutionTimeUnit(tms) + ")</h3>" + c_LF);
			writeChartCanvas(buf, "chart_et_" + chartId, 120);
			writeAreaChart(buf, "chart_et_" + chartId, "Exec Time", labels, etData,
				"rgba(59,130,246,0.15)", "rgba(59,130,246,0.9)");
		}
		buf.write(t(5) + "<h3 class=\"card-header\">Non-Passing Test Cases</h3>" + c_LF);
		writeChartCanvas(buf, "chart_fn_" + chartId, 100);
		writeAreaChart(buf, "chart_fn_" + chartId, "Failures", labels, fnData,
			"rgba(239,68,68,0.15)", "rgba(239,68,68,0.9)");
		buf.write(t(4) + "</div>" + c_LF);

		buf.write(t(3) + "</div>" + c_LF);
		buf.write(t(2) + "</section>" + c_LF);
	}

	private void writeKpi(BufferedWriter buf, String label, String value, String sub, String colorClass) throws IOException {
		buf.write(t(3) + "<div class=\"kpi-card\">" + c_LF);
		buf.write(t(4) + "<div class=\"kpi-label\">" + label + "</div>" + c_LF);
		String cls = colorClass.isEmpty() ? "kpi-value" : "kpi-value " + colorClass;
		buf.write(t(4) + "<div class=\"" + cls + "\">" + value + "</div>" + c_LF);
		if (!sub.isEmpty()) {
			buf.write(t(4) + "<div class=\"kpi-sub\">" + sub + "</div>" + c_LF);
		}
		buf.write(t(3) + "</div>" + c_LF);
	}

	private void generateModuleRunSection(BufferedWriter buf, String moduleName) throws IOException {
		buf.write(t(2) + "<span id=\"runs\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Module Runs</h3>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Click on a run to open its HTML test report.</p>" + c_LF);
		generateModuleModuleList(buf, moduleName);
	}

	private void generateModuleTestCaseSection(BufferedWriter buf, String moduleName) throws IOException {
		buf.write(t(2) + "<span id=\"testcases\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Test Cases</h3>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Average pass rate and execution times for all test cases in this module.</p>" + c_LF);
		generateModuleTestCaseList(buf, moduleName);
	}

	private void generateModuleModuleList(BufferedWriter buf, String moduleName) throws IOException {
		buf.write(t(2) + "<table id=\"run_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		buf.write(t(4) + "<tr>" + c_LF);
		buf.write(t(5) + "<th>Run</th>" + c_LF);
		buf.write(t(5) + "<th>Tests</th>" + c_LF);
		buf.write(t(5) + "<th>Pass</th>" + c_LF);
		buf.write(t(5) + "<th>Error</th>" + c_LF);
		buf.write(t(5) + "<th>Fail</th>" + c_LF);
		buf.write(t(5) + "<th>Ignored</th>" + c_LF);
		buf.write(t(5) + "<th>Exec Date</th>" + c_LF);
		buf.write(t(5) + "<th>Exec Time</th>" + c_LF);
		buf.write(t(5) + "<th colspan=\"2\">Total</th>" + c_LF);
		buf.write(t(4) + "</tr>" + c_LF);
		buf.write(t(3) + "</thead>" + c_LF);
		buf.write(t(3) + "<tbody>" + c_LF);
		int idx = th.getRuns().size();
		for (TestRun tr : th.getRuns()) {
			TestModule tm = tr.getTestModules().get(moduleName);
			if (null != tm) {
				String idxStr = HtmlGenUtils.calculateRunIdxString(th.getNoRuns(), idx);
				String runName = getHtmlReportLink(tr, "Run-" + idxStr);
				buf.write(t(4) + "<tr>" + c_LF);
				buf.write(t(5) + "<td>" + runName + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tm.getNoTestCases() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tm.getNoPassed() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tm.getNoErrors() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tm.getNoFailures() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tm.getNoIgnored() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tm.getExecutionDate() + "</td>" + c_LF);
				buf.write(t(5) + "<td class=\"mono\">" + tm.getExecutionTime() + "</td>" + c_LF);
				buf.write(t(5) + "<td class=\"text-right\">" + tm.getPassPct() + "%</td>" + c_LF);
				buf.write(t(5) + "<td class=\"graphBar\">" + pctBarHtml(tm.getPassPctDouble(), tm.getPassPct()) + "</td>" + c_LF);
				buf.write(t(4) + "</tr>" + c_LF);
			}
			idx--;
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generateModuleTestCaseList(BufferedWriter buf, String moduleName) throws IOException {
		buf.write(t(2) + "<table id=\"testcase_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		generateTestCaseHeader(buf);
		buf.write(t(3) + "</thead>" + c_LF);
		buf.write(t(3) + "<tbody>" + c_LF);
		for (TestCaseSummary tcs : th.getTestCaseSummaries().values()) {
			if (null != tcs && (tcs.getClassName().equals(moduleName) || tcs.getModuleName().equals(moduleName))) {
				generateTestCaseItem(buf, tcs);
			}
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generateModuleTestCaseSpreadList(BufferedWriter buf, String moduleName) throws IOException {
		buf.write(t(2) + "<table id=\"spread_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		generateTestCaseSpreadHeader(buf);
		buf.write(t(3) + "</thead>" + c_LF);
		buf.write(t(3) + "<tbody>" + c_LF);
		for (TestCaseSummary tcs : th.getTestCaseSummaries().values()) {
			if (null != tcs && (tcs.getClassName().equals(moduleName) || tcs.getModuleName().equals(moduleName))) {
				generateTestCaseSpreadItem(buf, tcs);
			}
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	protected void startPageModule(BufferedWriter buf, String pageTitle) throws IOException {
		buf.write("<!DOCTYPE html>" + c_LF);
		buf.write("<html lang=\"en\">" + c_LF);
		buf.write("<head>" + c_LF);
		buf.write(t(1) + "<meta charset=\"UTF-8\">" + c_LF);
		buf.write(t(1) + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + c_LF);
		buf.write(t(1) + "<title>" + pageTitle + "</title>" + c_LF);
		buf.write(t(1) + "<link rel=\"stylesheet\" href=\"css/style.css\">" + c_LF);
		buf.write(t(1) + "<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js\"></script>" + c_LF);
		buf.write(t(1) + "<script src=\"" + UnitTH.JS_DIR + "/" + UnitTH.SORT_SCRIPT + "\"></script>" + c_LF);
		buf.write(t(1) + "<script>" + c_LF);
		buf.write(t(2) + "window.onload = function() {" + c_LF);
		buf.write(t(3) + "['run_table','testcase_table','spread_table'].forEach(function(id) {" + c_LF);
		buf.write(t(4) + "var t = new TableSort; t.init(id);" + c_LF);
		buf.write(t(3) + "});" + c_LF);
		buf.write(t(2) + "};" + c_LF);
		buf.write(t(1) + "</script>" + c_LF);
		buf.write("</head>" + c_LF);
	}
}
