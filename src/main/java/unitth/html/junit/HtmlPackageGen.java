/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import unitth.core.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import unitth.junit.TestCaseSummary;
import unitth.junit.TestHistory;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackageSummary;

public class HtmlPackageGen extends HtmlGen {

	public HtmlPackageGen(TestHistory history, boolean generateExecTimeGraphs) {
		super(history, generateExecTimeGraphs);
	}

	@Override
	public void generateHtmlHistory() {
		for (Object packageName : th.getUniquePackages()) {
			generatePackageRunPage((String) packageName);
		}
	}

	private void generatePackageRunPage(String packageName) {
		String fileName = destDir + File.separator + HTMLPACKAGEOUTPUTFILE_PREFIX + fileSafe(packageName) + HTML_SUFFIX;
		boolean hasSubPackages = th.hasSubPackageSummaries(packageName);
		boolean withModules = !modulesMirrorPackages();
		try (BufferedWriter out = createFile(fileName)) {
			startSortablePage(out, "UnitTH - " + packageName, "run_table", "package_table", "module_table", "testcase_table", "spread_table");
			startMain(out);
			writeBreadcrumb(out, packageName);
			List<String> nav = new ArrayList<>(List.of("runs", "Runs"));
			if (hasSubPackages) {
				nav.addAll(List.of("subpackages", "Sub Packages"));
			}
			if (withModules) {
				nav.addAll(List.of(ANCHOR_TESTMODULES, MODULES));
			}
			nav.addAll(List.of(ANCHOR_TESTCASES, TEST_CASES, "spread", "Verdict Spread"));
			writeNav(out, nav.toArray(String[]::new));
			generateSummary(out, th.getTestPackageSummary(packageName));

			writeSection(out, "runs", "Package Runs", null, "Click on a run to open its HTML test report.");
			writeRunTable(out, tr -> tr.getTestPackages().get(packageName));

			if (hasSubPackages) {
				writeSection(out, "subpackages", "Sub Packages", null, null);
				startTable(out, "package_table", 4, HtmlMainGen.withNameColumns("Package Name", "Runs", "Classes"));
				for (TestPackageSummary tps : th.getTestPackageSummaries().values()) {
					if (tps.getName().contains(packageName + ".")) {
						writePackageRow(out, tps);
					}
				}
				endTable(out);
			}

			if (withModules) {
				writeSection(out, ANCHOR_TESTMODULES, MODULES, null, null);
				startTable(out, "module_table", 4, HtmlMainGen.withNameColumns("Module Name", "Runs"));
				for (TestModuleSummary tms : th.getTestModuleSummaries().values()) {
					if (tms.getPackageName().contains(packageName)) {
						writeModuleRow(out, tms);
					}
				}
				endTable(out);
			}

			List<TestCaseSummary> cases = th.getTestCaseSummaries().values().stream()
				.filter(tcs -> tcs.getPackageName().contains(packageName)).toList();
			writeSection(out, ANCHOR_TESTCASES, TEST_CASES, null, "Average pass rate and execution times for all test cases in this package.");
			writeTestCaseTable(out, cases, tcs -> tcs.getClassPlusTestCaseName(packageName), 4);
			writeSpreadTable(out, cases, tcs -> tcs.getClassPlusTestCaseName(packageName), 4);
			endMain(out, "");
		} catch (IOException e) {
			Log.error("Error writing " + fileName, e);
		}
	}

	private void generateSummary(BufferedWriter buf, TestPackageSummary tps) throws IOException {
		buf.write(t(2) + "<h1>Package</h1>" + LF);
		buf.write(t(2) + "<h2>" + esc(tps.getName()) + "</h2>" + LF);
		buf.write(t(2) + "<div class=\"kpi-strip\">" + LF);
		writeKpi(buf, "Runs", String.valueOf(tps.getNoRuns()), "", "accent");
		writeKpi(buf, MODULES, String.valueOf(tps.getNoUniqueTestModules()), "", "");
		writeKpi(buf, TEST_CASES, String.valueOf(tps.getNoUniqueTestCases()), "", "");
		writePassRateKpi(buf, tps.getPassPctDouble(), tps.getPassPct(), tps.getBestRun(), tps.getWorstRun());
		buf.write(t(2) + DIV_END + LF);
		String chartId = fileSafe(tps.getName()).replace('.', '_');
		writeDashboardCharts(buf, k -> "chart_" + k + "_" + chartId,
			chartSeries(tr -> tr.getTestPackage(tps.getName())), getExecutionTimeUnit(tps));
	}
}
