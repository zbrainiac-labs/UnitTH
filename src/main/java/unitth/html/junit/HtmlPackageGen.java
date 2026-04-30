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
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackage;
import unitth.junit.TestPackageSummary;
import unitth.junit.TestRun;

public class HtmlPackageGen extends HtmlGen {

	public HtmlPackageGen(TestHistory history, boolean generateExecTimeGraphs) {
		super(history, generateExecTimeGraphs);
	}

	public void generateHtmlHistory() {
		generatePackageRunPages();
	}

	protected void generatePackageRunPages() {
		Object[] packageNames = th.getUniquePackages();
		for (Object packageName : packageNames) {
			generatePackageRunPage((String) packageName);
		}
	}

	private void generatePackageRunPage(String packageName) {
		String fileName = destDir + File.separator
			+ HTMLPACKAGEOUTPUTFILE_PREFIX + packageName
			+ HTMLPACKAGEOUTPUTFILE_SUFFIX;
		try {
			BufferedWriter out = createFile(fileName);
			startPagePackage(out, "UnitTH - " + packageName);
			startBody(out);
			generateHeader(out);
			out.write(t(1) + "<main class=\"container\">" + c_LF);
			generateBreadcrumb(out, packageName);
			generatePackageAnchors(out, packageName);
			generatePackagePackageSummary(out, th.getTestPackageSummary(packageName));
			generatePackageRunSection(out, packageName);
			if (th.hasSubPackageSummaries(packageName) == true) {
				generatePackageSubPackageSection(out, packageName);
			}
			generatePackageModuleSection(out, packageName);
			generatePackageTestCaseSection(out, packageName);
			generateTestCaseSpreadInfo(out, false);
			generatePackageTestCaseSpreadList(out, packageName);
			out.write(t(1) + "</main>" + c_LF);
			generateFooter(out);
			endBody(out);
			endPage(out);
		} catch (IOException e) {
			System.out.println("Error writing " + fileName);
			e.printStackTrace();
		}
	}

	private void generateBreadcrumb(BufferedWriter out, String packageName) throws IOException {
		out.write(t(2) + "<nav class=\"breadcrumb\">" + c_LF);
		out.write(t(3) + "<a href=\"index.html\">\u2190 Dashboard</a>" + c_LF);
		out.write(t(3) + "<span class=\"separator\">/</span>" + c_LF);
		out.write(t(3) + "<span class=\"current\">" + packageName + "</span>" + c_LF);
		out.write(t(2) + "</nav>" + c_LF);
	}

	protected void startPagePackage(BufferedWriter buf, String pageTitle) throws IOException {
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
		buf.write(t(3) + "['run_table','package_table','module_table','testcase_table','spread_table'].forEach(function(id) {" + c_LF);
		buf.write(t(4) + "var t = new TableSort; t.init(id);" + c_LF);
		buf.write(t(3) + "});" + c_LF);
		buf.write(t(2) + "};" + c_LF);
		buf.write(t(1) + "</script>" + c_LF);
		buf.write("</head>" + c_LF);
	}

	private void generatePackageAnchors(BufferedWriter out, String packageName) throws IOException {
		out.write(t(2) + "<nav class=\"nav-bar\">" + c_LF);
		out.write(t(3) + "<a href=\"#runs\">Runs</a>" + c_LF);
		if (th.hasSubPackageSummaries(packageName) == true) {
			out.write(t(3) + "<a href=\"#subpackages\">Sub Packages</a>" + c_LF);
		}
		out.write(t(3) + "<a href=\"#testmodules\">Modules</a>" + c_LF);
		out.write(t(3) + "<a href=\"#testcases\">Test Cases</a>" + c_LF);
		out.write(t(3) + "<a href=\"#spread\">Verdict Spread</a>" + c_LF);
		out.write(t(2) + "</nav>" + c_LF);
	}

	private void generatePackagePackageSummary(BufferedWriter buf, TestPackageSummary tps) throws IOException {
		buf.write(t(2) + "<h1>Package</h1>" + c_LF);
		buf.write(t(2) + "<h2>" + tps.getName() + "</h2>" + c_LF);

		buf.write(t(2) + "<div class=\"kpi-strip\">" + c_LF);
		writeKpi(buf, "Runs", String.valueOf(tps.getNoRuns()), "", "accent");
		writeKpi(buf, "Modules", String.valueOf(tps.getNoUniqueTestModules()), "", "");
		writeKpi(buf, "Test Cases", String.valueOf(tps.getNoUniqueTestCases()), "", "");
		double avgPct = tps.getPassPctDouble();
		String pctColor = avgPct >= 99.0 ? "green" : avgPct >= 80.0 ? "accent" : "red";
		writeKpi(buf, "Avg Pass Rate", tps.getPassPct() + "%", "Best: " + tps.getBestRun() + "% / Worst: " + tps.getWorstRun() + "%", pctColor);
		buf.write(t(2) + "</div>" + c_LF);

		buf.write(t(2) + "<section class=\"dashboard\">" + c_LF);
		buf.write(t(3) + "<div class=\"dashboard-grid\">" + c_LF);

		String labels = buildRunLabelsJson();
		String prData = buildPackagePassRateDataJson(tps.getName());
		String prFail = buildPackageFailDataJson(tps.getName());
		String tcData = buildPackageTcCountDataJson(tps.getName());
		String fnData = buildPackageFailCountDataJson(tps.getName());
		String etData = buildPackageExecTimeDataJson(tps.getName());
		String chartId = tps.getName().replace('.', '_');

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
			buf.write(t(5) + "<h3 class=\"card-header\">Execution Times (" + getExecutionTimeUnit(tps) + ")</h3>" + c_LF);
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

	private void generatePackageRunSection(BufferedWriter buf, String packageName) throws IOException {
		buf.write(t(2) + "<span id=\"runs\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Package Runs</h3>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Click on a run to open its HTML test report.</p>" + c_LF);
		generatePackageRunList(buf, packageName);
	}

	private void generatePackageSubPackageSection(BufferedWriter buf, String packageName) throws IOException {
		buf.write(t(2) + "<span id=\"subpackages\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Sub Packages</h3>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		generatePackagePackageList(buf, packageName);
	}

	private void generatePackageModuleSection(BufferedWriter buf, String packageName) throws IOException {
		buf.write(t(2) + "<span id=\"testmodules\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Modules</h3>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		generatePackageModuleList(buf, packageName);
	}

	private void generatePackageTestCaseSection(BufferedWriter buf, String packageName) throws IOException {
		buf.write(t(2) + "<span id=\"testcases\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Test Cases</h3>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Average pass rate and execution times for all test cases in this package.</p>" + c_LF);
		generatePackageTestCaseList(buf, packageName);
	}

	private void generatePackageRunList(BufferedWriter buf, String packageName) throws IOException {
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
			TestPackage tp = tr.getTestPackages().get(packageName);
			if (null != tp) {
				String idxStr = HtmlGenUtils.calculateRunIdxString(th.getNoRuns(), idx);
				String runName = getHtmlReportLink(tr, "Run-" + idxStr);
				buf.write(t(4) + "<tr>" + c_LF);
				buf.write(t(5) + "<td>" + runName + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tp.getNoTestCases() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tp.getNoPassed() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tp.getNoErrors() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tp.getNoFailures() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tp.getNoIgnored() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tp.getExecutionDate() + "</td>" + c_LF);
				buf.write(t(5) + "<td class=\"mono\">" + tp.getExecutionTime() + "</td>" + c_LF);
				buf.write(t(5) + "<td class=\"text-right\">" + tp.getPassPct() + "%</td>" + c_LF);
				buf.write(t(5) + "<td class=\"graphBar\">" + pctBarHtml(tp.getPassPctDouble(), tp.getPassPct()) + "</td>" + c_LF);
				buf.write(t(4) + "</tr>" + c_LF);
			}
			idx--;
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generatePackageModuleList(BufferedWriter buf, String packageName) throws IOException {
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
			if (null != tms && tms.getPackageName().contains(packageName)) {
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
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generatePackagePackageList(BufferedWriter buf, String packageName) throws IOException {
		buf.write(t(2) + "<table id=\"package_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		buf.write(t(4) + "<tr>" + c_LF);
		buf.write(t(5) + "<th>Package Name</th>" + c_LF);
		buf.write(t(5) + "<th>Runs</th>" + c_LF);
		buf.write(t(5) + "<th>Classes</th>" + c_LF);
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
			if (null != tps && tps.getName().contains(packageName + ".")) {
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
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generatePackageTestCaseList(BufferedWriter buf, String packageName) throws IOException {
		buf.write(t(2) + "<table id=\"testcase_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		generateTestCaseHeader(buf);
		buf.write(t(3) + "</thead>" + c_LF);
		buf.write(t(3) + "<tbody>" + c_LF);
		for (TestCaseSummary tcs : th.getTestCaseSummaries().values()) {
			if (null != tcs && tcs.getPackageName().contains(packageName)) {
				buf.write(t(4) + "<tr>" + c_LF);
				buf.write(t(5) + "<td class=\"text-left\">" + tcs.getClassPlusTestCaseName(packageName) + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoRuns() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoPassed() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoErrors() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoFailures() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoIgnored() + "</td>" + c_LF);
				buf.write(t(5) + "<td class=\"mono\">" + tcs.getExecutionTime() + "</td>" + c_LF);
				buf.write(t(5) + "<td class=\"text-right\">" + tcs.getPassPct() + "%</td>" + c_LF);
				buf.write(t(5) + "<td class=\"graphBar\">" + pctBarHtml(tcs.getPassPctDouble(), tcs.getPassPct()) + "</td>" + c_LF);
				buf.write(t(4) + "</tr>" + c_LF);
			}
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}

	private void generatePackageTestCaseSpreadList(BufferedWriter buf, String packageName) throws IOException {
		buf.write(t(2) + "<table id=\"spread_table\" class=\"sortable\">" + c_LF);
		buf.write(t(3) + "<thead>" + c_LF);
		generateTestCaseSpreadHeader(buf);
		buf.write(t(3) + "</thead>" + c_LF);
		buf.write(t(3) + "<tbody>" + c_LF);
		for (TestCaseSummary tcs : th.getTestCaseSummaries().values()) {
			if (null != tcs && tcs.getPackageName().contains(packageName)) {
				buf.write(t(4) + "<tr>" + c_LF);
				buf.write(t(5) + "<td class=\"text-left\">" + tcs.getClassPlusTestCaseName(packageName) + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoRuns() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoPassed() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoErrors() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoFailures() + "</td>" + c_LF);
				buf.write(t(5) + "<td>" + tcs.getNoIgnored() + "</td>" + c_LF);
				buf.write(t(5) + "<td class=\"spread-cell\">" + c_LF);
				buf.write(t(6) + "<div class=\"spread-bar\">" + c_LF);
				generateSpreadBar(buf, tcs);
				buf.write(t(6) + "</div>" + c_LF);
				buf.write(t(5) + "</td>" + c_LF);
				buf.write(t(4) + "</tr>" + c_LF);
			}
		}
		buf.write(t(3) + "</tbody>" + c_LF);
		buf.write(t(2) + "</table>" + c_LF);
	}
}
