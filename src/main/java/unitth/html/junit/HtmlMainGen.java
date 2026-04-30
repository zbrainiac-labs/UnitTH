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
import unitth.junit.TestHistory;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackageSummary;
import unitth.junit.TestRun;

public class HtmlMainGen extends HtmlGen {

	public HtmlMainGen(TestHistory history, boolean generateExecTimeGraphs) {
		super(history, generateExecTimeGraphs);
	}

	public void generateHtmlHistory() {
		generateMainPage();
	}

	protected void generateMainPage() {
		String fileName = destDir + File.separator + HTML_MAIN;
		try {
			BufferedWriter bwout = createFile(fileName);
			startPageMain(bwout, "UnitTestHistory - Test History");
			startBody(bwout);
			generateHeader(bwout);
			bwout.write(t(1) + "<main class=\"container\">" + c_LF);
			generateMainAnchors(bwout);
			generateKpiStrip(bwout);
			generateMainRunSummary(bwout);
			generateMainRunSection(bwout);
			generateMainPackageSection(bwout);
			generateMainModuleSection(bwout);
			generateMainModuleTotals(bwout);
			bwout.write(t(1) + "</main>" + c_LF);
			bwout.write(HtmlGenUtils.createArgsDivTag());
			generateFooter(bwout);
			endBody(bwout);
			endPage(bwout);
		} catch (IOException e) {
			System.out.println("Error writing " + fileName);
			e.printStackTrace();
		}
	}

	private void generateMainAnchors(BufferedWriter out) throws IOException {
		out.write(t(2) + "<nav class=\"nav-bar\">" + c_LF);
		out.write(t(3) + "<a href=\"#runs\">Runs</a>" + c_LF);
		out.write(t(3) + "<a href=\"#packages\">Packages</a>" + c_LF);
		out.write(t(3) + "<a href=\"#modules\">Modules</a>" + c_LF);
		out.write(t(3) + "<a href=\"#totals\">Totals</a>" + c_LF);
		out.write(t(2) + "</nav>" + c_LF);
	}

	private void generateKpiStrip(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<div class=\"kpi-strip\">" + c_LF);
		writeKpi(buf, "Total Runs", String.valueOf(th.getNoRuns()), "", "accent");
		writeKpi(buf, "Test Cases", String.valueOf(th.getNoUniqueTestCases()), th.getNoIgnoredTestCases() + " ignored", "");
		writeKpi(buf, "Modules", String.valueOf(th.getNoUniqueModules()), th.getNoPackages() + " package(s)", "");
		double avgPct = th.getAvePassPctDouble();
		String pctColor = avgPct >= 99.0 ? "green" : avgPct >= 80.0 ? "accent" : "red";
		writeKpi(buf, "Avg Pass Rate", th.getAvePassPct() + "%", "Best: " + th.getBestRun() + "% / Worst: " + th.getWorstRun() + "%", pctColor);
		String[] parts = th.getLastRun().split("&nbsp;");
		String lastRun = parts.length == 2 ? parts[0] + "<br>" + parts[1] : th.getLastRun();
		writeKpi(buf, "Last Run", lastRun, "", "");
		buf.write(t(2) + "</div>" + c_LF);
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

	private void generateMainRunSummary(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<section class=\"dashboard\">" + c_LF);
		buf.write(t(3) + "<div class=\"dashboard-grid\">" + c_LF);
		String labels = buildRunLabelsJson();
		buf.write(t(4) + "<div class=\"card\">" + c_LF);
		buf.write(t(5) + "<h3 class=\"card-header\">Pass Rates</h3>" + c_LF);
		writeChartCanvas(buf, "chart_main_pr", 160);
		writeBarChart(buf, "chart_main_pr", "Pass %", labels, buildPassRateDataJson(),
			"rgba(16,185,129,0.65)", "rgba(52,211,153,0.9)", buildPassRateFailDataJson(), 100);
		buf.write(t(5) + "<h3 class=\"card-header\">Test Case Count</h3>" + c_LF);
		writeChartCanvas(buf, "chart_main_tc", 100);
		writeAreaChart(buf, "chart_main_tc", "Test Cases", labels, buildTestCaseCountDataJson(),
			"rgba(16,185,129,0.15)", "rgba(52,211,153,0.9)");
		buf.write(t(4) + "</div>" + c_LF);

		buf.write(t(4) + "<div class=\"card\">" + c_LF);
		if (generateExecTimeGraphs) {
			buf.write(t(5) + "<h3 class=\"card-header\">Execution Times (" + getExecutionTimeUnit(null) + ")</h3>" + c_LF);
			writeChartCanvas(buf, "chart_main_et", 120);
			writeAreaChart(buf, "chart_main_et", "Exec Time", labels, buildExecTimeDataJson(),
				"rgba(59,130,246,0.15)", "rgba(59,130,246,0.9)");
		}
		buf.write(t(5) + "<h3 class=\"card-header\">Non-Passing Test Cases</h3>" + c_LF);
		writeChartCanvas(buf, "chart_main_fn", 100);
		writeAreaChart(buf, "chart_main_fn", "Failures", labels, buildFailureCountDataJson(),
			"rgba(239,68,68,0.15)", "rgba(239,68,68,0.9)");
		buf.write(t(4) + "</div>" + c_LF);

		buf.write(t(3) + "</div>" + c_LF);
		buf.write(t(2) + "</section>" + c_LF);
	}

	private void generateMainRunSection(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<span id=\"runs\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Runs</h3>" + c_LF);
		buf.write(t(3) + "<span class=\"section-badge\">" + th.getNoRuns() + " runs</span>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Click on a run to open its HTML test report.</p>" + c_LF);
		generateMainRunList(buf);
	}

	private void generateMainPackageSection(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<span id=\"packages\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Packages</h3>" + c_LF);
		buf.write(t(3) + "<span class=\"section-badge\">" + th.getNoPackages() + " packages</span>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Statistics per test package. Click the package name for details.</p>" + c_LF);
		generateMainPackageList(buf);
	}

	private void generateMainModuleSection(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<span id=\"modules\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Modules</h3>" + c_LF);
		buf.write(t(3) + "<span class=\"section-badge\">" + th.getNoUniqueModules() + " modules</span>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Average pass rate per test module. Click the module name for details.</p>" + c_LF);
		generateMainModuleList(buf);
	}

	private void generateMainRunList(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<table id=\"run_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		buf.write(t(4) + "<tr>" + c_LF);
		buf.write(t(5) + "<th>Run</th>" + c_LF);
		buf.write(t(5) + "<th>Path</th>" + c_LF);
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
		int idx = th.getNoRuns();
		for (TestRun tr : th.getRuns()) {
			String idxStr = HtmlGenUtils.calculateRunIdxString(th.getNoRuns(), idx);
			String runName = getHtmlReportLink(tr, "Run-" + idxStr);
			buf.write(t(4) + "<tr>" + c_LF);
			buf.write(t(5) + "<td>" + runName + "</td>" + c_LF);
			buf.write(t(5) + "<td class=\"text-left mono\">" + tr.getChosenPath() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tr.getNoTestCases() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tr.getNoPassed() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tr.getNoErrors() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tr.getNoFailures() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tr.getNoIgnored() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tr.getExecutionDate() + "</td>" + c_LF);
			buf.write(t(5) + "<td class=\"mono\">" + tr.getExecutionTime() + "</td>" + c_LF);
			buf.write(t(5) + "<td class=\"text-right\">" + tr.getPassPct() + "%</td>" + c_LF);
			buf.write(t(5) + "<td class=\"graphBar\">" + pctBarHtml(tr.getPassPctDouble(), tr.getPassPct()) + "</td>" + c_LF);
			buf.write(t(4) + "</tr>" + c_LF);
			idx--;
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generateMainPackageList(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<table id=\"package_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		buf.write(t(4) + "<tr>" + c_LF);
		buf.write(t(5) + "<th>Package Name</th>" + c_LF);
		buf.write(t(5) + "<th>Runs</th>" + c_LF);
		buf.write(t(5) + "<th>Modules</th>" + c_LF);
		buf.write(t(5) + "<th>Tests</th>" + c_LF);
		buf.write(t(5) + "<th>Executed</th>" + c_LF);
		buf.write(t(5) + "<th>Pass</th>" + c_LF);
		buf.write(t(5) + "<th>Error</th>" + c_LF);
		buf.write(t(5) + "<th>Fail</th>" + c_LF);
		buf.write(t(5) + "<th>Ignored</th>" + c_LF);
		buf.write(t(5) + "<th>Exec Time</th>" + c_LF);
		buf.write(t(5) + "<th colspan=\"2\">Total</th>" + c_LF);
		buf.write(t(4) + "</tr>" + c_LF);
		buf.write(t(3) + "</thead>" + c_LF);
		buf.write(t(3) + "<tbody>" + c_LF);
		for (TestPackageSummary tps : th.getTestPackageSummaries().values()) {
			if (null == tps) continue;
			String link = "<a href=\"" + HTMLPACKAGEOUTPUTFILE_PREFIX + tps.getName() + HTMLPACKAGEOUTPUTFILE_SUFFIX + "\">" + tps.getName() + "</a>";
			buf.write(t(4) + "<tr>" + c_LF);
			buf.write(t(5) + "<td class=\"text-left\">" + link + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tps.getNoRuns() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tps.getNoUniqueTestModules() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tps.getNoUniqueTestCases() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tps.getNoTestCases() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tps.getNoPassed() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tps.getNoErrors() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tps.getNoFailures() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tps.getNoIgnored() + "</td>" + c_LF);
			buf.write(t(5) + "<td class=\"mono\">" + tps.getExecutionTime() + "</td>" + c_LF);
			buf.write(t(5) + "<td class=\"text-right\">" + tps.getPassPct() + "%</td>" + c_LF);
			buf.write(t(5) + "<td class=\"graphBar\">" + pctBarHtml(tps.getPassPctDouble(), tps.getPassPct()) + "</td>" + c_LF);
			buf.write(t(4) + "</tr>" + c_LF);
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generateMainModuleList(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<table id=\"module_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		buf.write(t(4) + "<tr>" + c_LF);
		buf.write(t(5) + "<th>Module Name</th>" + c_LF);
		buf.write(t(5) + "<th>Runs</th>" + c_LF);
		buf.write(t(5) + "<th>Tests</th>" + c_LF);
		buf.write(t(5) + "<th>Executed</th>" + c_LF);
		buf.write(t(5) + "<th>Pass</th>" + c_LF);
		buf.write(t(5) + "<th>Error</th>" + c_LF);
		buf.write(t(5) + "<th>Fail</th>" + c_LF);
		buf.write(t(5) + "<th>Ignored</th>" + c_LF);
		buf.write(t(5) + "<th>Exec Time</th>" + c_LF);
		buf.write(t(5) + "<th colspan=\"2\">Total</th>" + c_LF);
		buf.write(t(4) + "</tr>" + c_LF);
		buf.write(t(3) + "</thead>" + c_LF);
		buf.write(t(3) + "<tbody>" + c_LF);
		for (TestModuleSummary tms : th.getTestModuleSummaries().values()) {
			if (null == tms) continue;
			String link = "<a href=\"" + HTMLMODULEOUTPUTFILE_PREFIX + tms.getName() + HTMLMODULEOUTPUTFILE_SUFFIX + "\">" + tms.getName() + "</a>";
			buf.write(t(4) + "<tr>" + c_LF);
			buf.write(t(5) + "<td class=\"text-left\">" + link + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tms.getNoRuns() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tms.getNoUniqueTestCases() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tms.getNoTestCases() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tms.getNoPassed() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tms.getNoErrors() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tms.getNoFailures() + "</td>" + c_LF);
			buf.write(t(5) + "<td>" + tms.getNoIgnored() + "</td>" + c_LF);
			buf.write(t(5) + "<td class=\"mono\">" + tms.getExecutionTime() + "</td>" + c_LF);
			buf.write(t(5) + "<td class=\"text-right\">" + tms.getPassPct() + "%</td>" + c_LF);
			buf.write(t(5) + "<td class=\"graphBar\">" + pctBarHtml(tms.getPassPctDouble(), tms.getPassPct()) + "</td>" + c_LF);
			buf.write(t(4) + "</tr>" + c_LF);
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generateMainModuleTotals(BufferedWriter buf) throws IOException {
		buf.write(t(2) + "<span id=\"totals\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Execution Totals</h3>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Aggregate of all executed tests across all runs.</p>" + c_LF);
		buf.write(t(2) + "<table>" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		buf.write(t(4) + "<tr>" + c_LF);
		buf.write(t(5) + "<th>Run</th>" + c_LF);
		buf.write(t(5) + "<th>Tests</th>" + c_LF);
		buf.write(t(5) + "<th>Pass</th>" + c_LF);
		buf.write(t(5) + "<th>Error</th>" + c_LF);
		buf.write(t(5) + "<th>Fail</th>" + c_LF);
		buf.write(t(5) + "<th>Ignored</th>" + c_LF);
		buf.write(t(5) + "<th>Exec Time</th>" + c_LF);
		buf.write(t(5) + "<th colspan=\"2\">Total</th>" + c_LF);
		buf.write(t(4) + "</tr>" + c_LF);
		buf.write(t(3) + "</thead>" + c_LF);
		buf.write(t(3) + "<tbody>" + c_LF);
		buf.write(t(4) + "<tr>" + c_LF);
		buf.write(t(5) + "<td><strong>TOTALS</strong></td>" + c_LF);
		buf.write(t(5) + "<td>" + th.getNoExecutedTestCases() + "</td>" + c_LF);
		buf.write(t(5) + "<td>" + th.getNoPassedTestCases() + "</td>" + c_LF);
		buf.write(t(5) + "<td>" + th.getNoErrorTestCases() + "</td>" + c_LF);
		buf.write(t(5) + "<td>" + th.getNoFailedTestCases() + "</td>" + c_LF);
		buf.write(t(5) + "<td>" + th.getNoIgnoredTestCases() + "</td>" + c_LF);
		buf.write(t(5) + "<td class=\"mono\">" + th.getTotalExecutionTime() + "</td>" + c_LF);
		buf.write(t(5) + "<td class=\"text-right\"><strong>" + th.getAvePassPct() + "%</strong></td>" + c_LF);
		buf.write(t(5) + "<td class=\"graphBar\">" + pctBarHtml(th.getAvePassPctDouble(), th.getAvePassPct()) + "</td>" + c_LF);
		buf.write(t(4) + "</tr>" + c_LF);
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	protected void startPageMain(BufferedWriter buf, String pageTitle) throws IOException {
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
		buf.write(t(3) + "['run_table','package_table','module_table'].forEach(function(id) {" + c_LF);
		buf.write(t(4) + "var t = new TableSort; t.init(id);" + c_LF);
		buf.write(t(3) + "});" + c_LF);
		buf.write(t(2) + "};" + c_LF);
		buf.write(t(1) + "</script>" + c_LF);
		buf.write("</head>" + c_LF);
	}
}
