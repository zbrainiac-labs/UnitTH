/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import unitth.core.UnitTH;
import unitth.html.HtmlGenUtils;
import unitth.junit.RunCounts;
import unitth.junit.TestCaseSummary;
import unitth.junit.TestCaseVerdict;
import unitth.junit.TestGroupSummary;
import unitth.junit.TestHistory;
import unitth.junit.TestItemSummary;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackageSummary;
import unitth.junit.TestRun;

public abstract class HtmlGen {

	protected static final String LF = "\r\n";
	protected static final String TD_END = "</td>" + LF;
	protected static final String TD_TEXT_LEFT = "<td class=\"text-left\">";
	protected static final String TR_END = "</tr>";
	protected static final String DIV_END = "</div>";
	protected static final String A_HREF = "<a href=\"";
	protected static final String SCRIPT_OPEN = "<script>";
	protected static final String SCRIPT_CLOSE = "</script>";
	protected static final String NOT_AVAILABLE = "N/A";
	/** Header cell spanning the pass rate text and bar columns. */
	protected static final String TOTAL = "Total";
	protected static final String SPAN_END = "</span>";
	protected static final String TD_MONO = "<td class=\"mono\">";
	protected static final String PASS = "Pass";
	protected static final String ERROR = "Error";
	protected static final String FAIL = "Fail";
	protected static final String IGNORED = "Ignored";
	protected static final String EXEC_TIME = "Exec Time";
	protected static final String TEST_CASES = "Test Cases";
	protected static final String MODULES = "Modules";
	protected static final String ANCHOR_TESTCASES = "testcases";
	protected static final String ANCHOR_TESTMODULES = "testmodules";

	protected static final String HTML_MAIN = "index.html";
	protected static final String HTMLMODULEOUTPUTFILE_PREFIX = "th-module-";
	protected static final String HTMLPACKAGEOUTPUTFILE_PREFIX = "th-package-";
	protected static final String HTML_SUFFIX = ".html";

	/** Chart.js data series of a page, already serialised as JS arrays. */
	protected record ChartSeries(String labels, String passRate, String passFail, String testCases,
			String execTime, String failures) {
	}

	protected final TestHistory th;
	protected final boolean generateExecTimeGraphs;
	protected final String destDir;

	protected HtmlGen(TestHistory history, boolean generateExecTimeGraphs) {
		th = history;
		this.generateExecTimeGraphs = generateExecTimeGraphs;
		destDir = history.getConfig().rootFolder();
	}

	/** Writes the page(s) of this generator. */
	public abstract void generateHtmlHistory();

	protected static String t(int noTabs) {
		return "  ".repeat(noTabs);
	}

	protected BufferedWriter createFile(String fileName) throws IOException {
		return Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8);
	}

	/** Page head with Chart.js and table sorting for the given table ids. */
	protected void startSortablePage(BufferedWriter buf, String pageTitle, String... tableIds) throws IOException {
		buf.write("<!DOCTYPE html>" + LF);
		buf.write("<html lang=\"en\">" + LF);
		buf.write("<head>" + LF);
		buf.write(t(1) + "<meta charset=\"UTF-8\">" + LF);
		buf.write(t(1) + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + LF);
		buf.write(t(1) + "<title>" + esc(pageTitle) + "</title>" + LF);
		buf.write(t(1) + "<link rel=\"stylesheet\" href=\"css/style.css\">" + LF);
		buf.write(t(1) + "<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js\"></script>" + LF);
		buf.write(t(1) + "<script src=\"" + UnitTH.JS_DIR + "/" + UnitTH.SORT_SCRIPT + "\"></script>" + LF);
		buf.write(t(1) + SCRIPT_OPEN + LF);
		buf.write(t(2) + "window.onload = function() {" + LF);
		StringBuilder ids = new StringBuilder();
		for (String id : tableIds) {
			ids.append(ids.isEmpty() ? "" : ",").append('\'').append(id).append('\'');
		}
		buf.write(t(3) + "[" + ids + "].forEach(function(id) {" + LF);
		buf.write(t(4) + "var t = new TableSort; t.init(id);" + LF);
		buf.write(t(3) + "});" + LF);
		buf.write(t(2) + "};" + LF);
		buf.write(t(1) + SCRIPT_CLOSE + LF);
		buf.write("</head>" + LF);
	}

	/** Opening of a page body: header and main container. */
	protected void startMain(BufferedWriter buf) throws IOException {
		buf.write("<body>" + LF);
		generateHeader(buf);
		buf.write(t(1) + "<main class=\"container\">" + LF);
	}

	/** Closing of a page body; <code>beforeFooter</code> is written after the main container. */
	protected void endMain(BufferedWriter buf, String beforeFooter) throws IOException {
		buf.write(t(1) + "</main>" + LF);
		buf.write(beforeFooter);
		buf.write(t(1) + "<footer class=\"footer\">" + LF);
		buf.write(t(2) + "Generated by <a href=\"https://github.com/zbrainiac-labs/UnitTH\">UnitTestHistory v" + UnitTH.VERSION_NUMBER + "</a>" + LF);
		buf.write(t(1) + "</footer>" + LF);
		buf.write("</body>" + LF);
		buf.write("</html>" + LF);
		buf.flush();
	}

	private void generateHeader(BufferedWriter buf) throws IOException {
		java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance(
			java.text.DateFormat.MEDIUM, java.text.DateFormat.MEDIUM);
		String generationTime = df.format(new java.util.Date());
		buf.write(t(1) + "<header class=\"site-header\">" + LF);
		buf.write(t(2) + "<div class=\"header-brand\">" + LF);
		buf.write(t(3) + "<svg class=\"header-logo\" viewBox=\"0 0 32 32\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">" + LF);
		buf.write(t(4) + "<rect width=\"32\" height=\"32\" rx=\"8\" fill=\"url(#logo-grad)\"/>" + LF);
		buf.write(t(4) + "<path d=\"M8 16L13 21L24 10\" stroke=\"white\" stroke-width=\"3\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>" + LF);
		buf.write(t(4) + "<defs><linearGradient id=\"logo-grad\" x1=\"0\" y1=\"0\" x2=\"32\" y2=\"32\"><stop stop-color=\"#6366f1\"/><stop offset=\"1\" stop-color=\"#a78bfa\"/></linearGradient></defs>" + LF);
		buf.write(t(3) + "</svg>" + LF);
		buf.write(t(3) + "<span class=\"header-title\">UnitTestHistory v" + UnitTH.VERSION_NUMBER + SPAN_END + LF);
		buf.write(t(2) + DIV_END + LF);
		buf.write(t(2) + "<span class=\"header-date\">Report generated: " + generationTime + SPAN_END + LF);
		buf.write(t(1) + "</header>" + LF);
	}

	/** Breadcrumb back to the dashboard. */
	protected void writeBreadcrumb(BufferedWriter out, String current) throws IOException {
		out.write(t(2) + "<nav class=\"breadcrumb\">" + LF);
		out.write(t(3) + "<a href=\"index.html\">\u2190 Dashboard</a>" + LF);
		out.write(t(3) + "<span class=\"separator\">/</span>" + LF);
		out.write(t(3) + "<span class=\"current\">" + esc(current) + SPAN_END + LF);
		out.write(t(2) + "</nav>" + LF);
	}

	/** Navigation bar; <code>links</code> alternates anchor id and label. */
	protected void writeNav(BufferedWriter out, String... links) throws IOException {
		out.write(t(2) + "<nav class=\"nav-bar\">" + LF);
		for (int i = 0; i < links.length; i += 2) {
			out.write(t(3) + "<a href=\"#" + links[i] + "\">" + links[i + 1] + "</a>" + LF);
		}
		out.write(t(2) + "</nav>" + LF);
	}

	/** Section anchor and heading with optional badge and description (null = none). */
	protected void writeSection(BufferedWriter buf, String anchor, String title, String badge, String desc) throws IOException {
		buf.write(t(2) + "<span id=\"" + anchor + "\" class=\"section-anchor\"></span>" + LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + LF);
		buf.write(t(3) + "<h3>" + title + "</h3>" + LF);
		if (badge != null) {
			buf.write(t(3) + "<span class=\"section-badge\">" + badge + SPAN_END + LF);
		}
		buf.write(t(2) + DIV_END + LF);
		if (desc != null) {
			buf.write(t(2) + "<p class=\"section-desc\">" + desc + "</p>" + LF);
		}
	}

	/**
	 * Opens a table up to and including <code>tbody</code>. The header row is
	 * indented by <code>rowIndent</code>; a {@link #TOTAL} header spans two
	 * columns. A null id gives a plain, unsorted table.
	 */
	protected void startTable(BufferedWriter buf, String id, int rowIndent, String... headers) throws IOException {
		buf.write(t(2) + (id == null ? "<table>" : "<table id=\"" + id + "\" class=\"sortable\">") + LF);
		buf.write(t(3) + "<thead>" + LF);
		buf.write(t(rowIndent) + "<tr>" + LF);
		for (String h : headers) {
			buf.write(t(rowIndent + 1) + (TOTAL.equals(h) ? "<th colspan=\"2\">" : "<th>") + h + "</th>" + LF);
		}
		buf.write(t(rowIndent) + TR_END + LF);
		buf.write(t(3) + "</thead>" + LF);
		buf.write(t(3) + "<tbody>" + LF);
	}

	protected void endTable(BufferedWriter buf) throws IOException {
		buf.write(t(3) + "</tbody>" + LF);
		buf.write(t(2) + "</table>" + LF);
	}

	protected void writeKpi(BufferedWriter buf, String label, String value, String sub, String colorClass) throws IOException {
		buf.write(t(3) + "<div class=\"kpi-card\">" + LF);
		buf.write(t(4) + "<div class=\"kpi-label\">" + label + DIV_END + LF);
		String cls = colorClass.isEmpty() ? "kpi-value" : "kpi-value " + colorClass;
		buf.write(t(4) + "<div class=\"" + cls + "\">" + value + DIV_END + LF);
		if (!sub.isEmpty()) {
			buf.write(t(4) + "<div class=\"kpi-sub\">" + sub + DIV_END + LF);
		}
		buf.write(t(3) + DIV_END + LF);
	}

	/** Average pass rate KPI coloured by threshold. */
	protected void writePassRateKpi(BufferedWriter buf, double avgPct, String pct, String best, String worst) throws IOException {
		String color = "red";
		if (avgPct >= 99.0) {
			color = "green";
		} else if (avgPct >= 80.0) {
			color = "accent";
		}
		writeKpi(buf, "Avg Pass Rate", pctText(pct), "Best: " + pctText(best) + " / Worst: " + pctText(worst), color);
	}

	/**
	 * Pass rate / test count / exec time / non-passing chart cards.
	 * <code>canvasId</code> maps a chart kind (pr, tc, et, fn) to its canvas id.
	 */
	protected void writeDashboardCharts(BufferedWriter buf, UnaryOperator<String> canvasId, ChartSeries s, String etUnit) throws IOException {
		buf.write(t(2) + "<section class=\"dashboard\">" + LF);
		buf.write(t(3) + "<div class=\"dashboard-grid\">" + LF);
		buf.write(t(4) + "<div class=\"card\">" + LF);
		buf.write(t(5) + "<h3 class=\"card-header\">Pass Rates</h3>" + LF);
		writeChartCanvas(buf, canvasId.apply("pr"), 160);
		writeBarChart(buf, canvasId.apply("pr"), s.labels(), s.passRate(), s.passFail());
		buf.write(t(5) + "<h3 class=\"card-header\">Test Case Count</h3>" + LF);
		writeChartCanvas(buf, canvasId.apply("tc"), 100);
		writeAreaChart(buf, canvasId.apply("tc"), TEST_CASES, s.labels(), s.testCases(),
			"rgba(16,185,129,0.15)", "rgba(52,211,153,0.9)");
		buf.write(t(4) + DIV_END + LF);

		buf.write(t(4) + "<div class=\"card\">" + LF);
		if (generateExecTimeGraphs) {
			buf.write(t(5) + "<h3 class=\"card-header\">Execution Times (" + etUnit + ")</h3>" + LF);
			writeChartCanvas(buf, canvasId.apply("et"), 120);
			writeAreaChart(buf, canvasId.apply("et"), EXEC_TIME, s.labels(), s.execTime(),
				"rgba(59,130,246,0.15)", "rgba(59,130,246,0.9)");
		}
		buf.write(t(5) + "<h3 class=\"card-header\">Non-Passing Test Cases</h3>" + LF);
		writeChartCanvas(buf, canvasId.apply("fn"), 100);
		writeAreaChart(buf, canvasId.apply("fn"), "Failures", s.labels(), s.failures(),
			"rgba(239,68,68,0.15)", "rgba(239,68,68,0.9)");
		buf.write(t(4) + DIV_END + LF);
		buf.write(t(3) + DIV_END + LF);
		buf.write(t(2) + "</section>" + LF);
	}

	/** One run table row; the path column is only shown on the main page. */
	protected void writeRunRow(BufferedWriter buf, TestRun tr, RunCounts item, int idx, boolean withPath) throws IOException {
		String runName = getHtmlReportLink(tr, "Run-" + HtmlGenUtils.calculateRunIdxString(th.getNoRuns(), idx));
		buf.write(t(4) + "<tr>" + LF);
		buf.write(t(5) + "<td>" + runName + TD_END);
		if (withPath) {
			buf.write(t(5) + "<td class=\"text-left mono\">" + tr.getChosenPath() + TD_END);
		}
		buf.write(t(5) + "<td>" + item.getNoTestCases() + TD_END);
		buf.write(t(5) + "<td>" + item.getNoPassed() + TD_END);
		buf.write(t(5) + "<td>" + item.getNoErrors() + TD_END);
		buf.write(t(5) + "<td>" + item.getNoFailures() + TD_END);
		buf.write(t(5) + "<td>" + item.getNoIgnored() + TD_END);
		buf.write(t(5) + "<td>" + item.getExecutionDate() + TD_END);
		buf.write(t(5) + TD_MONO + item.getExecutionTime() + TD_END);
		writePctCells(buf, 5, item.getPassPctDouble(), item.getPassPct());
		buf.write(t(4) + TR_END + LF);
	}

	/** Run table of a package or module page: runs that contain the item. */
	protected void writeRunTable(BufferedWriter buf, Function<TestRun, ? extends RunCounts> pick) throws IOException {
		writeRunTable(buf, "run_table", pick);
	}

	/** Run table with the given id (null = not sortable), only runs that contain the item. */
	protected void writeRunTable(BufferedWriter buf, String id, Function<TestRun, ? extends RunCounts> pick) throws IOException {
		startTable(buf, id, 4, "Run", "Tests", PASS, ERROR, FAIL, IGNORED, "Exec Date", EXEC_TIME, TOTAL);
		int idx = th.getNoRuns();
		for (TestRun tr : th.getRuns()) {
			RunCounts item = pick.apply(tr);
			if (item != null) {
				writeRunRow(buf, tr, item, idx, false);
			}
			idx--;
		}
		endTable(buf);
	}

	private void writePctCells(BufferedWriter buf, int indent, double pctDouble, String pct) throws IOException {
		buf.write(t(indent) + "<td class=\"text-right\">" + pctText(pct) + TD_END);
		buf.write(t(indent) + "<td class=\"graphBar\">" + pctBarHtml(pctDouble, pct) + TD_END);
	}

	protected static final String[] SUMMARY_COUNT_HEADERS = { "Tests", "Executed", PASS, ERROR, FAIL, IGNORED, EXEC_TIME, TOTAL };

	/** True if every package has exactly one module, so module views repeat the package views. */
	protected boolean modulesMirrorPackages() {
		return th.getTestPackageSummaries().values().stream().allMatch(tps -> tps.getNoUniqueTestModules() == 1);
	}

	/** One row of a package table, linking to the package page. */
	protected void writePackageRow(BufferedWriter buf, TestPackageSummary tps) throws IOException {
		String link = A_HREF + HTMLPACKAGEOUTPUTFILE_PREFIX + fileSafe(tps.getName()) + HTML_SUFFIX + "\">" + esc(tps.getName()) + "</a>";
		buf.write(t(4) + "<tr>" + LF);
		buf.write(t(5) + TD_TEXT_LEFT + link + TD_END);
		buf.write(t(5) + "<td>" + tps.getNoRuns() + TD_END);
		buf.write(t(5) + "<td>" + tps.getNoUniqueTestModules() + TD_END);
		writeSummaryTotals(buf, tps);
	}

	/** One row of a module table, linking to the module page. */
	protected void writeModuleRow(BufferedWriter buf, TestModuleSummary tms) throws IOException {
		String link = A_HREF + HTMLMODULEOUTPUTFILE_PREFIX + fileSafe(tms.getName()) + HTML_SUFFIX + "\">" + esc(tms.getName()) + "</a>";
		buf.write(t(4) + "<tr>" + LF);
		buf.write(t(5) + TD_TEXT_LEFT + link + TD_END);
		buf.write(t(5) + "<td>" + tms.getNoRuns() + TD_END);
		writeSummaryTotals(buf, tms);
	}

	private void writeSummaryTotals(BufferedWriter buf, TestGroupSummary<?> s) throws IOException {
		buf.write(t(5) + "<td>" + s.getNoUniqueTestCases() + TD_END);
		buf.write(t(5) + "<td>" + s.getNoTestCases() + TD_END);
		writeCounts(buf, 5, s);
		buf.write(t(5) + TD_MONO + s.getExecutionTime() + TD_END);
		writePctCells(buf, 5, s.getPassPctDouble(), s.getPassPct());
		buf.write(t(4) + TR_END + LF);
	}

	private void writeCounts(BufferedWriter buf, int indent, TestItemSummary s) throws IOException {
		buf.write(t(indent) + "<td>" + s.getNoPassed() + TD_END);
		buf.write(t(indent) + "<td>" + s.getNoErrors() + TD_END);
		buf.write(t(indent) + "<td>" + s.getNoFailures() + TD_END);
		buf.write(t(indent) + "<td>" + s.getNoIgnored() + TD_END);
	}

	/** Test case table, rows indented by <code>rowIndent</code>; the row cells one deeper. */
	protected void writeTestCaseTable(BufferedWriter buf, Iterable<TestCaseSummary> cases,
			Function<TestCaseSummary, String> name, int rowIndent) throws IOException {
		startTable(buf, "testcase_table", 3, "Test case name", "Runs", PASS, ERROR, FAIL, IGNORED, "Ave ET", TOTAL);
		for (TestCaseSummary tcs : cases) {
			writeTestCaseCounts(buf, tcs, name.apply(tcs), rowIndent);
			buf.write(t(rowIndent + 1) + TD_MONO + tcs.getExecutionTime() + TD_END);
			writePctCells(buf, rowIndent + 1, tcs.getPassPctDouble(), tcs.getPassPct());
			buf.write(t(rowIndent) + TR_END + LF);
		}
		endTable(buf);
	}

	/** Verdict spread section and table, rows indented by <code>rowIndent</code>. */
	protected void writeSpreadTable(BufferedWriter buf, Iterable<TestCaseSummary> cases,
			Function<TestCaseSummary, String> name, int rowIndent) throws IOException {
		writeSection(buf, "spread", "Test case verdict spread", null, "Test case verdicts in order of execution. "
			+ "<span class=\"legend-pass\">Pass</span> "
			+ "<span class=\"legend-norun\">No-run</span> "
			+ "<span class=\"legend-fail\">Fail/Error</span>");
		startTable(buf, "spread_table", 3, "Test case name", "Runs", PASS, ERROR, FAIL, IGNORED, "Spread");
		for (TestCaseSummary tcs : cases) {
			writeTestCaseCounts(buf, tcs, name.apply(tcs), rowIndent);
			buf.write(t(rowIndent + 1) + "<td class=\"spread-cell\">" + LF);
			buf.write(t(rowIndent + 2) + "<div class=\"spread-bar\">" + LF);
			writeSpreadBar(buf, tcs);
			buf.write(t(rowIndent + 2) + DIV_END + LF);
			buf.write(t(rowIndent + 1) + TD_END);
			buf.write(t(rowIndent) + TR_END + LF);
		}
		endTable(buf);
	}

	private void writeTestCaseCounts(BufferedWriter buf, TestCaseSummary tcs, String name, int rowIndent) throws IOException {
		buf.write(t(rowIndent) + "<tr>" + LF);
		buf.write(t(rowIndent + 1) + TD_TEXT_LEFT + esc(name) + TD_END);
		buf.write(t(rowIndent + 1) + "<td>" + tcs.getNoRuns() + TD_END);
		writeCounts(buf, rowIndent + 1, tcs);
	}

	private void writeSpreadBar(BufferedWriter buf, TestCaseSummary tcs) throws IOException {
		int noRuns = th.getNoRuns();
		for (int i = 1; i <= noRuns; i++) {
			TestCaseVerdict tcv = tcs.getSpreadAt(i);
			String cssClass = "spread-norun";
			if (null != tcv) {
				if (TestCaseVerdict.e_PASS == tcv) cssClass = "spread-pass";
				else if (TestCaseVerdict.e_FAIL == tcv) cssClass = "spread-fail";
				else if (TestCaseVerdict.e_ERROR == tcv) cssClass = "spread-error";
				else if (TestCaseVerdict.e_IGNORED == tcv) cssClass = "spread-ignored";
			}
			String link = getHtmlReportLink(th.getTestRunByIdx(i), "<span class=\"" + cssClass + "\" title=\"Run-" + i + "\"></span>");
			buf.write(t(6) + link + LF);
		}
	}

	protected String getHtmlReportLink(TestRun tr, String linkText) {
		String link = th.getConfig().useAbsPaths() ? tr.getAbsolutePath() : tr.getRelativePath();
		if (link.equals("")) {
			return linkText;
		}
		return A_HREF + esc(link) + "\">" + linkText + "</a>";
	}

	/** Unit for the execution time chart, from the largest time of the item (null = whole history). */
	public String getExecutionTimeUnit(TestGroupSummary<?> summary) {
		double largest = null == summary ? th.getLargestExecutionTime() : summary.getLargestExecutionTime();
		if (largest <= 1.0) {
			return "ms";
		} else if (largest <= 120.0) {
			return "sec";
		} else if (largest <= 3600.0 * 2.0) {
			return "minutes";
		}
		return "hours";
	}

	private void writeChartCanvas(BufferedWriter buf, String canvasId, int height) throws IOException {
		buf.write(t(5) + "<div class=\"chart-container\"><canvas id=\"" + canvasId + "\" height=\"" + height + "\"></canvas></div>" + LF);
	}

	/* Pass rate bar chart with the fail percentage stacked as a second dataset. */
	private void writeBarChart(BufferedWriter buf, String canvasId, String labelsJson, String dataJson,
			String failDataJson) throws IOException {
		buf.write(t(5) + SCRIPT_OPEN + LF);
		buf.write(t(5) + "new Chart(document.getElementById('" + canvasId + "'),{" + LF);
		buf.write(t(6) + "type:'bar'," + LF);
		buf.write(t(6) + "data:{labels:" + labelsJson + ",datasets:[" + LF);
		buf.write(t(7) + "{label:'Fail',data:" + failDataJson + ",backgroundColor:'rgba(239,68,68,0.35)',borderColor:'rgba(239,68,68,0.8)',borderWidth:1,borderRadius:1,barPercentage:0.9,categoryPercentage:0.95}," + LF);
		buf.write(t(7) + "{label:'Pass %',data:" + dataJson + ",backgroundColor:'rgba(16,185,129,0.65)',borderColor:'rgba(52,211,153,0.9)',borderWidth:1,borderRadius:1,barPercentage:0.9,categoryPercentage:0.95}" + LF);
		buf.write(t(6) + "]}," + LF);
		buf.write(t(6) + "options:{responsive:true,maintainAspectRatio:false,animation:{duration:600},interaction:{mode:'index',intersect:false}," + LF);
		buf.write(t(7) + "plugins:{legend:{display:true,labels:{color:'#94a3b8',font:{family:'Inter',size:11}}},tooltip:{backgroundColor:'#1e293b',titleColor:'#f1f5f9',bodyColor:'#94a3b8',borderColor:'rgba(99,102,241,0.3)',borderWidth:1,cornerRadius:8,padding:10}}," + LF);
		buf.write(t(7) + "scales:{x:{display:false},y:{display:true,max:100.0,grid:{color:'rgba(255,255,255,0.05)'},ticks:{color:'#64748b',font:{family:'Inter',size:10}},border:{color:'rgba(255,255,255,0.08)'}}}" + LF);
		buf.write(t(6) + "}" + LF);
		buf.write(t(5) + "});" + LF);
		buf.write(t(5) + SCRIPT_CLOSE + LF);
	}

	private void writeAreaChart(BufferedWriter buf, String canvasId, String label,
			String labelsJson, String dataJson, String bgColor, String borderColor) throws IOException {
		buf.write(t(5) + SCRIPT_OPEN + LF);
		buf.write(t(5) + "new Chart(document.getElementById('" + canvasId + "'),{" + LF);
		buf.write(t(6) + "type:'line'," + LF);
		buf.write(t(6) + "data:{labels:" + labelsJson + ",datasets:[{" + LF);
		buf.write(t(7) + "label:'" + label + "',data:" + dataJson + ",fill:true," + LF);
		buf.write(t(7) + "backgroundColor:'" + bgColor + "',borderColor:'" + borderColor + "',borderWidth:2," + LF);
		buf.write(t(7) + "pointRadius:0,pointHoverRadius:4,pointHoverBackgroundColor:'" + borderColor + "',tension:0.3" + LF);
		buf.write(t(6) + "}]}," + LF);
		buf.write(t(6) + "options:{responsive:true,maintainAspectRatio:false,animation:{duration:600},interaction:{mode:'index',intersect:false}," + LF);
		buf.write(t(7) + "plugins:{legend:{display:false},tooltip:{backgroundColor:'#1e293b',titleColor:'#f1f5f9',bodyColor:'#94a3b8',borderColor:'rgba(99,102,241,0.3)',borderWidth:1,cornerRadius:8,padding:10}}," + LF);
		buf.write(t(7) + "scales:{x:{display:false},y:{display:true,beginAtZero:true,grid:{color:'rgba(255,255,255,0.05)'},ticks:{color:'#64748b',font:{family:'Inter',size:10}},border:{color:'rgba(255,255,255,0.08)'}}}" + LF);
		buf.write(t(6) + "}" + LF);
		buf.write(t(5) + "});" + LF);
		buf.write(t(5) + SCRIPT_CLOSE + LF);
	}

	/**
	 * Chart data of the item <code>pick</code> returns per run (null = not in
	 * that run, plotted as 0), oldest run first.
	 */
	protected ChartSeries chartSeries(Function<TestRun, ? extends RunCounts> pick) {
		return new ChartSeries(runLabels(),
			series(pick, i -> jsPct(i.getPassPctDouble())),
			series(pick, i -> String.format(Locale.US, "%.1f", 100.0 - i.getPassPctDouble())),
			series(pick, i -> String.valueOf(i.getNoTestCases())),
			series(pick, i -> String.format(Locale.US, "%.3f", i.getExecutionTimeDouble())),
			series(pick, i -> String.valueOf(i.getNoFailures() + i.getNoErrors())));
	}

	private String runLabels() {
		int n = th.getNoRuns();
		StringBuilder sb = new StringBuilder("[");
		for (int r = 1; r <= n; r++) {
			sb.append(r > 1 ? "," : "").append("'R").append(r).append('\'');
		}
		return sb.append(']').toString();
	}

	private String series(Function<TestRun, ? extends RunCounts> pick, Function<RunCounts, String> fmt) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			RunCounts item = pick.apply(runs[i]);
			sb.append(i < runs.length - 1 ? "," : "").append(item == null ? "0" : fmt.apply(item));
		}
		return sb.append(']').toString();
	}

	protected String pctBarHtml(double pctDouble, String pctStr) {
		if (Double.isNaN(pctDouble)) {
			return "<div class=\"progress-bar\">"
				+ "<div class=\"progress-fill progress-nan\" style=\"width:100%\">"
				+ "<span class=\"progress-label\">" + NOT_AVAILABLE + SPAN_END
				+ "</div></div>";
		}
		int w = (int) pctDouble;
		return "<div class=\"progress-bar\">"
			+ "<div class=\"progress-fill progress-pass\" style=\"width:" + w + "%\">"
			+ "<span class=\"progress-label\">" + pctStr + "%</span>"
			+ "</div></div>";
	}

	/** HTML-escapes a name taken from the parsed reports. */
	protected static String esc(String s) {
		return HtmlGenUtils.escapeHtml(s);
	}

	/** Safe file name / element id for a package or module name. */
	protected static String fileSafe(String s) {
		return HtmlGenUtils.fileSafe(s);
	}

	/** Formats a pass percentage for display; no executed tests (NaN) shows as N/A. */
	protected static String pctText(String pct) {
		return pct == null || pct.contains("NaN") ? NOT_AVAILABLE : pct + "%";
	}

	/** Formats a pass percentage for chart data; NaN becomes a gap. */
	protected static String jsPct(double pct) {
		return Double.isNaN(pct) ? "null" : String.format(Locale.US, "%.1f", pct);
	}
}
