/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import unitth.core.Log;
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
			+ HTMLPACKAGEOUTPUTFILE_PREFIX + fileSafe(packageName)
			+ HTMLPACKAGEOUTPUTFILE_SUFFIX;
		try (BufferedWriter out = createFile(fileName)) {
			startSortablePage(out, "UnitTH - " + packageName, "run_table", "package_table", "module_table", "testcase_table", "spread_table");
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
			Log.error("Error writing " + fileName, e);
		}
	}

	private void generateBreadcrumb(BufferedWriter out, String packageName) throws IOException {
		out.write(t(2) + "<nav class=\"breadcrumb\">" + c_LF);
		out.write(t(3) + "<a href=\"index.html\">\u2190 Dashboard</a>" + c_LF);
		out.write(t(3) + "<span class=\"separator\">/</span>" + c_LF);
		out.write(t(3) + "<span class=\"current\">" + esc(packageName) + "</span>" + c_LF);
		out.write(t(2) + "</nav>" + c_LF);
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
		buf.write(t(2) + "<h2>" + esc(tps.getName()) + "</h2>" + c_LF);

		buf.write(t(2) + "<div class=\"kpi-strip\">" + c_LF);
		writeKpi(buf, "Runs", String.valueOf(tps.getNoRuns()), "", "accent");
		writeKpi(buf, "Modules", String.valueOf(tps.getNoUniqueTestModules()), "", "");
		writeKpi(buf, "Test Cases", String.valueOf(tps.getNoUniqueTestCases()), "", "");
		double avgPct = tps.getPassPctDouble();
		String pctColor = avgPct >= 99.0 ? "green" : avgPct >= 80.0 ? "accent" : "red";
		writeKpi(buf, "Avg Pass Rate", pctText(tps.getPassPct()), "Best: " + pctText(tps.getBestRun()) + " / Worst: " + pctText(tps.getWorstRun()), pctColor);
		buf.write(t(2) + "</div>" + c_LF);

		buf.write(t(2) + "<section class=\"dashboard\">" + c_LF);
		buf.write(t(3) + "<div class=\"dashboard-grid\">" + c_LF);

		String labels = buildRunLabelsJson();
		String prData = buildPackagePassRateDataJson(tps.getName());
		String prFail = buildPackageFailDataJson(tps.getName());
		String tcData = buildPackageTcCountDataJson(tps.getName());
		String fnData = buildPackageFailCountDataJson(tps.getName());
		String etData = buildPackageExecTimeDataJson(tps.getName());
		String chartId = fileSafe(tps.getName()).replace('.', '_');

		writeDashboardCharts(buf, chartId, new ChartSeries(labels, prData, prFail, tcData, etData, fnData), getExecutionTimeUnit(tps));

		buf.write(t(3) + "</div>" + c_LF);
		buf.write(t(2) + "</section>" + c_LF);
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
				buf.write(t(5) + "<td class=\"text-right\">" + pctText(tp.getPassPct()) + "</td>" + c_LF);
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
				writeModuleRow(buf, tms);
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
				writePackageRow(buf, tps);
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
				writeTestCaseCounts(buf, tcs, tcs.getClassPlusTestCaseName(packageName));
				buf.write(t(5) + "<td class=\"mono\">" + tcs.getExecutionTime() + "</td>" + c_LF);
				buf.write(t(5) + "<td class=\"text-right\">" + pctText(tcs.getPassPct()) + "</td>" + c_LF);
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
				writeTestCaseCounts(buf, tcs, tcs.getClassPlusTestCaseName(packageName));
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
