/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import unitth.core.Log;
import unitth.core.TestItemUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import unitth.html.HtmlGenUtils;
import unitth.junit.RunCounts;
import unitth.junit.TestHistory;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackage;
import unitth.junit.TestPackageSummary;

public class HtmlMainGen extends HtmlGen {

	private static final String PROJECTS = "Projects";
	private static final String ANCHOR_PROJECTS = "projects";
	private static final String ANCHOR_TOTALS = "totals";

	public HtmlMainGen(TestHistory history, boolean generateExecTimeGraphs) {
		super(history, generateExecTimeGraphs);
	}

	@Override
	public void generateHtmlHistory() {
		String fileName = destDir + File.separator + HTML_MAIN;
		boolean withModules = !modulesMirrorPackages();
		try (BufferedWriter out = createFile(fileName)) {
			startSortablePage(out, "UnitTestHistory - Test History", "module_table");
			startMain(out);
			if (withModules) {
				writeNav(out, ANCHOR_PROJECTS, PROJECTS, "modules", MODULES, ANCHOR_TOTALS, "Totals");
			} else {
				writeNav(out, ANCHOR_PROJECTS, PROJECTS, ANCHOR_TOTALS, "Totals");
			}
			generateKpiStrip(out, withModules);
			writeDashboardCharts(out, k -> "chart_main_" + k, chartSeries(tr -> tr), getExecutionTimeUnit(null));
			generateProjectSection(out);
			if (withModules) {
				generateModuleSection(out);
			}
			generateTotals(out);
			endMain(out, HtmlGenUtils.createArgsDivTag(th.getConfig()));
		} catch (IOException e) {
			Log.error("Error writing " + fileName, e);
		}
	}

	private void generateKpiStrip(BufferedWriter buf, boolean withModules) throws IOException {
		buf.write(t(2) + "<div class=\"kpi-strip\">" + LF);
		writeKpi(buf, "Total Runs", String.valueOf(th.getNoRuns()), "", "accent");
		writeKpi(buf, TEST_CASES, String.valueOf(th.getNoUniqueTestCases()), th.getNoIgnoredTestCases() + " ignored", "");
		if (withModules) {
			writeKpi(buf, MODULES, String.valueOf(th.getNoUniqueModules()), th.getNoPackages() + " package(s)", "");
		} else {
			writeKpi(buf, PROJECTS, String.valueOf(th.getNoPackages()), "", "");
		}
		writePassRateKpi(buf, th.getAvePassPctDouble(), th.getAvePassPct(), th.getBestRun(), th.getWorstRun());
		String[] parts = th.getLastRun().split("&nbsp;");
		String lastRun = parts.length == 2 ? parts[0] + "<br>" + parts[1] : th.getLastRun();
		writeKpi(buf, "Last Run", lastRun, "", "");
		buf.write(t(2) + DIV_END + LF);
	}

	/*
	 * One expandable block per project (package): the summary shows the latest
	 * run, opening it lists all runs of the project, newest first.
	 */
	private void generateProjectSection(BufferedWriter buf) throws IOException {
		writeSection(buf, ANCHOR_PROJECTS, PROJECTS, th.getNoPackages() + " projects, " + th.getNoRuns() + " runs",
			"Click a project to list its runs; the project name opens its test cases.");
		buf.write(t(2) + "<div class=\"projects\">" + LF);
		buf.write(t(3) + "<button type=\"button\" class=\"expand-all\" onclick=\"toggleProjects(this)\">Expand all</button>" + LF);
		List<TestPackageSummary> projects = new ArrayList<>(th.getTestPackageSummaries().values());
		projects.sort(Comparator.comparing((TestPackageSummary p) -> p.getName().startsWith("Unknown"))
				.thenComparing(p -> p.getName().toLowerCase(Locale.ROOT)));
		for (TestPackageSummary tps : projects) {
			writeProject(buf, tps);
		}
		buf.write(t(2) + DIV_END + LF);
		buf.write(t(2) + SCRIPT_OPEN + "function toggleProjects(b){const open=b.textContent==='Expand all';"
				+ "document.querySelectorAll('details.project').forEach(d=>d.open=open);"
				+ "b.textContent=open?'Collapse all':'Expand all';}" + SCRIPT_CLOSE + LF);
	}

	private void writeProject(BufferedWriter buf, TestPackageSummary tps) throws IOException {
		String name = tps.getName();
		List<TestPackage> runs = th.getRuns().stream()
				.map(tr -> tr.getTestPackages().get(name)).filter(Objects::nonNull).toList();
		TestPackage latest = runs.isEmpty() ? null : runs.get(0);
		String link = A_HREF + HTMLPACKAGEOUTPUTFILE_PREFIX + fileSafe(name) + HTML_SUFFIX + "\">" + esc(name) + "</a>";

		buf.write(t(3) + "<details class=\"project\">" + LF);
		buf.write(t(4) + "<summary>" + LF);
		buf.write(t(5) + "<span class=\"project-name\">" + link + SPAN_END + LF);
		buf.write(t(5) + "<span class=\"project-runs\">" + runs.size() + (runs.size() == 1 ? " run" : " runs") + SPAN_END + LF);
		buf.write(t(5) + "<span class=\"project-date mono\">" + (latest == null ? NOT_AVAILABLE : latest.getExecutionDate()) + SPAN_END + LF);
		if (latest != null) {
			buf.write(t(5) + "<span class=\"project-pct\">" + pctText(latest.getPassPct()) + SPAN_END + LF);
			buf.write(t(5) + "<span class=\"project-bar graphBar\">" + pctBarHtml(latest.getPassPctDouble(), latest.getPassPct()) + SPAN_END + LF);
			buf.write(t(5) + "<span class=\"project-trend mono\" title=\"Pass rate change since the previous run\">"
					+ trend(latest, runs.size() > 1 ? runs.get(1) : null) + SPAN_END + LF);
		}
		buf.write(t(4) + "</summary>" + LF);
		writeRunTable(buf, null, tr -> tr.getTestPackages().get(name));
		buf.write(t(3) + "</details>" + LF);
	}

	/* Pass rate change between two runs of a project, "" if not comparable. */
	private static String trend(RunCounts now, RunCounts before) {
		if (before == null || Double.isNaN(now.getPassPctDouble()) || Double.isNaN(before.getPassPctDouble())) {
			return "";
		}
		double delta = now.getPassPctDouble() - before.getPassPctDouble();
		if (Math.abs(delta) < 0.005) {
			return "&plusmn;0";
		}
		return TestItemUtils.signedPct(delta);
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
		writeSection(buf, ANCHOR_TOTALS, "Execution Totals", null, "Aggregate of all executed tests across all runs.");
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
