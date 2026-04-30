/*
 * MIT License - see LICENSE.txt
 */
package unitth.html.junit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import unitth.core.UnitTH;
import unitth.html.HtmlGenUtils;
import unitth.junit.TestCaseSummary;
import unitth.junit.TestCaseVerdict;
import unitth.junit.TestHistory;
import unitth.junit.TestItemSummary;
import unitth.junit.TestModule;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackage;
import unitth.junit.TestPackageSummary;
import unitth.junit.TestRun;

public class HtmlGen {

	protected static final String c_LF = "\r\n";
	protected static final String c_TAB = "  ";

	protected final String CSSDIR = "css";
	protected final String HTML_MAIN = "index.html";
	protected final String HTMLMODULEOUTPUTFILE_PREFIX = "th-module-";
	protected final String HTMLMODULEOUTPUTFILE_SUFFIX = ".html";
	protected final String HTMLPACKAGEOUTPUTFILE_PREFIX = "th-package-";
	protected final String HTMLPACKAGEOUTPUTFILE_SUFFIX = ".html";
	protected final String IMAGE_SUFFIX = ".png";
	protected final String IMAGE_PASS_PREFIX = "mpr-";
	protected final String IMAGE_TC_PREFIX = "mtc-";
	protected final String IMAGE_FN_PREFIX = "mfn-";
	protected final String IMAGE_ET_PREFIX = "etg-";

	protected TestHistory th = null;
	protected boolean generateExecTimeGraphs = false;

	protected String destDir;
	protected String cssDir;

	public HtmlGen(TestHistory history, boolean generateExecTimeGraphs) {
		th = history;
		this.generateExecTimeGraphs = generateExecTimeGraphs;
		destDir = UnitTH.rootFolder;
		cssDir = destDir + File.separator + CSSDIR;
	}

	protected String t(int noTabs) {
		String ret = "";
		for (int i = 0; i < noTabs; i++) {
			ret += c_TAB;
		}
		return ret;
	}

	public void setDestDir(String dDir) { this.destDir = dDir; }
	public void setCssDir(String cDir) { this.cssDir = cDir; }
	public void generateHtmlHistory() { }
	public String getDestDir() { return destDir; }
	public String getCssDir() { return cssDir; }

	protected boolean closeFile(BufferedWriter writer) {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Could not close the buffer to the output file.");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	protected BufferedWriter createFile(String fileName) {
		File outFile = null;
		BufferedWriter bwout = null;
		try {
			outFile = new File(fileName);
			outFile.createNewFile();
			bwout = new BufferedWriter(new FileWriter(outFile));
		} catch (NullPointerException e) {
			System.out.println("Could not create the output file.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not create the output file: " + outFile.getName());
			e.printStackTrace();
		}
		return bwout;
	}

	protected void startPage(BufferedWriter buf, String pageTitle) throws IOException {
		buf.write("<!DOCTYPE html>" + c_LF);
		buf.write("<html lang=\"en\">" + c_LF);
		buf.write("<head>" + c_LF);
		buf.write(t(1) + "<meta charset=\"UTF-8\">" + c_LF);
		buf.write(t(1) + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + c_LF);
		buf.write(t(1) + "<title>" + pageTitle + "</title>" + c_LF);
		buf.write(t(1) + "<link rel=\"stylesheet\" href=\"css/style.css\">" + c_LF);
		buf.write(t(1) + "<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js\"></script>" + c_LF);
		buf.write("</head>" + c_LF);
	}

	protected void generateHeader(BufferedWriter buf) throws IOException {
		java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance(
			java.text.DateFormat.MEDIUM, java.text.DateFormat.MEDIUM);
		String generationTime = df.format(new java.util.Date());
		buf.write(t(1) + "<header class=\"site-header\">" + c_LF);
		buf.write(t(2) + "<div class=\"header-brand\">" + c_LF);
		buf.write(t(3) + "<svg class=\"header-logo\" viewBox=\"0 0 32 32\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">" + c_LF);
		buf.write(t(4) + "<rect width=\"32\" height=\"32\" rx=\"8\" fill=\"url(#logo-grad)\"/>" + c_LF);
		buf.write(t(4) + "<path d=\"M8 16L13 21L24 10\" stroke=\"white\" stroke-width=\"3\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>" + c_LF);
		buf.write(t(4) + "<defs><linearGradient id=\"logo-grad\" x1=\"0\" y1=\"0\" x2=\"32\" y2=\"32\"><stop stop-color=\"#6366f1\"/><stop offset=\"1\" stop-color=\"#a78bfa\"/></linearGradient></defs>" + c_LF);
		buf.write(t(3) + "</svg>" + c_LF);
		buf.write(t(3) + "<span class=\"header-title\">UnitTestHistory v" + UnitTH.versionNumber + "</span>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<span class=\"header-date\">Report generated: " + generationTime + "</span>" + c_LF);
		buf.write(t(1) + "</header>" + c_LF);
	}

	protected void generateFooter(BufferedWriter buf) throws IOException {
		buf.write(t(1) + "<footer class=\"footer\">" + c_LF);
		buf.write(t(2) + "Generated by <a href=\"https://github.com/zbrainiac-labs/UnitTH\">UnitTestHistory v" + UnitTH.versionNumber + "</a>" + c_LF);
		buf.write(t(1) + "</footer>" + c_LF);
	}

	protected void startBody(BufferedWriter buf) throws IOException {
		buf.write("<body>" + c_LF);
	}

	protected void endBody(BufferedWriter buf) throws IOException {
		buf.write("</body>" + c_LF);
	}

	protected void endPage(BufferedWriter buf) throws IOException {
		buf.write("</html>" + c_LF);
		buf.flush();
	}

	protected String getHtmlReportLink(TestRun tr, String linkText) {
		String link = null;
		if (UnitTH.useAbsPaths == true) {
			link = tr.getAbsolutePath();
		} else {
			link = tr.getRelativePath();
		}
		if (link.equals("")) {
			return linkText;
		} else {
			return "<a href=\"" + link + "\">" + linkText + "</a>";
		}
	}

	public String getExecutionTimeUnit(TestItemSummary tis) {
		double largest = 0.0;
		if (null == tis) {
			largest = th.getLargestExecutionTime();
		} else {
			largest = tis.getLargestExecutionTime();
		}
		if (largest <= 1.0) {
			return "ms";
		} else if (largest <= 120.0) {
			return "sec";
		} else if (largest <= 3600.0 * 2.0) {
			return "minutes";
		} else {
			return "hours";
		}
	}

	protected void generateMainPage() { }
	protected void generateModuleRunPages() { }

	protected void writeChartCanvas(BufferedWriter buf, String canvasId, int height) throws IOException {
		buf.write(t(5) + "<div class=\"chart-container\"><canvas id=\"" + canvasId + "\" height=\"" + height + "\"></canvas></div>" + c_LF);
	}

	protected void writeBarChart(BufferedWriter buf, String canvasId, String label,
			String labelsJson, String dataJson, String color, String borderColor,
			String failDataJson, double maxVal) throws IOException {
		buf.write(t(5) + "<script>" + c_LF);
		buf.write(t(5) + "new Chart(document.getElementById('" + canvasId + "'),{" + c_LF);
		buf.write(t(6) + "type:'bar'," + c_LF);
		buf.write(t(6) + "data:{labels:" + labelsJson + ",datasets:[" + c_LF);
		if (failDataJson != null) {
			buf.write(t(7) + "{label:'Fail',data:" + failDataJson + ",backgroundColor:'rgba(239,68,68,0.35)',borderColor:'rgba(239,68,68,0.8)',borderWidth:1,borderRadius:1,barPercentage:0.9,categoryPercentage:0.95}," + c_LF);
		}
		buf.write(t(7) + "{label:'" + label + "',data:" + dataJson + ",backgroundColor:'" + color + "',borderColor:'" + borderColor + "',borderWidth:1,borderRadius:1,barPercentage:0.9,categoryPercentage:0.95}" + c_LF);
		buf.write(t(6) + "]}," + c_LF);
		buf.write(t(6) + "options:{responsive:true,maintainAspectRatio:false,animation:{duration:600},interaction:{mode:'index',intersect:false}," + c_LF);
		buf.write(t(7) + "plugins:{legend:{display:" + (failDataJson != null ? "true" : "false") + ",labels:{color:'#94a3b8',font:{family:'Inter',size:11}}},tooltip:{backgroundColor:'#1e293b',titleColor:'#f1f5f9',bodyColor:'#94a3b8',borderColor:'rgba(99,102,241,0.3)',borderWidth:1,cornerRadius:8,padding:10}}," + c_LF);
		buf.write(t(7) + "scales:{x:{display:false},y:{display:true," + (maxVal > 0 ? "max:" + maxVal + "," : "") + "grid:{color:'rgba(255,255,255,0.05)'},ticks:{color:'#64748b',font:{family:'Inter',size:10}},border:{color:'rgba(255,255,255,0.08)'}}}" + c_LF);
		buf.write(t(6) + "}" + c_LF);
		buf.write(t(5) + "});" + c_LF);
		buf.write(t(5) + "</script>" + c_LF);
	}

	protected void writeAreaChart(BufferedWriter buf, String canvasId, String label,
			String labelsJson, String dataJson, String bgColor, String borderColor) throws IOException {
		buf.write(t(5) + "<script>" + c_LF);
		buf.write(t(5) + "new Chart(document.getElementById('" + canvasId + "'),{" + c_LF);
		buf.write(t(6) + "type:'line'," + c_LF);
		buf.write(t(6) + "data:{labels:" + labelsJson + ",datasets:[{" + c_LF);
		buf.write(t(7) + "label:'" + label + "',data:" + dataJson + ",fill:true," + c_LF);
		buf.write(t(7) + "backgroundColor:'" + bgColor + "',borderColor:'" + borderColor + "',borderWidth:2," + c_LF);
		buf.write(t(7) + "pointRadius:0,pointHoverRadius:4,pointHoverBackgroundColor:'" + borderColor + "',tension:0.3" + c_LF);
		buf.write(t(6) + "}]}," + c_LF);
		buf.write(t(6) + "options:{responsive:true,maintainAspectRatio:false,animation:{duration:600},interaction:{mode:'index',intersect:false}," + c_LF);
		buf.write(t(7) + "plugins:{legend:{display:false},tooltip:{backgroundColor:'#1e293b',titleColor:'#f1f5f9',bodyColor:'#94a3b8',borderColor:'rgba(99,102,241,0.3)',borderWidth:1,cornerRadius:8,padding:10}}," + c_LF);
		buf.write(t(7) + "scales:{x:{display:false},y:{display:true,beginAtZero:true,grid:{color:'rgba(255,255,255,0.05)'},ticks:{color:'#64748b',font:{family:'Inter',size:10}},border:{color:'rgba(255,255,255,0.08)'}}}" + c_LF);
		buf.write(t(6) + "}" + c_LF);
		buf.write(t(5) + "});" + c_LF);
		buf.write(t(5) + "</script>" + c_LF);
	}

	protected String buildRunLabelsJson() {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			sb.append("'R").append(runs.length - i).append("'");
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildPassRateDataJson() {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			sb.append(String.format("%.1f", runs[i].getPassPctDouble()));
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildPassRateFailDataJson() {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			double failPct = 100.0 - runs[i].getPassPctDouble();
			sb.append(String.format("%.1f", failPct));
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildTestCaseCountDataJson() {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			sb.append(runs[i].getNoTestCases());
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildFailureCountDataJson() {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			sb.append(runs[i].getNoFailures() + runs[i].getNoErrors());
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildExecTimeDataJson() {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			sb.append(String.format("%.3f", runs[i].getExecutionTimeDouble()));
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildPackagePassRateDataJson(String packageName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestPackage tp = runs[i].getTestPackage(packageName);
			sb.append(tp != null ? String.format("%.1f", tp.getPassPctDouble()) : "0");
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildPackageFailDataJson(String packageName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestPackage tp = runs[i].getTestPackage(packageName);
			if (tp != null) {
				double failPct = 100.0 - tp.getPassPctDouble();
				sb.append(String.format("%.1f", failPct));
			} else { sb.append("0"); }
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildPackageTcCountDataJson(String packageName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestPackage tp = runs[i].getTestPackage(packageName);
			sb.append(tp != null ? tp.getNoTestCases() : "0");
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildPackageFailCountDataJson(String packageName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestPackage tp = runs[i].getTestPackage(packageName);
			sb.append(tp != null ? (tp.getNoFailures() + tp.getNoErrors()) : "0");
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildPackageExecTimeDataJson(String packageName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestPackage tp = runs[i].getTestPackage(packageName);
			sb.append(tp != null ? String.format("%.3f", tp.getExecutionTimeDouble()) : "0");
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildModulePassRateDataJson(String packageName, String moduleName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestModule tm = runs[i].getTestModule(packageName, moduleName);
			sb.append(tm != null ? String.format("%.1f", tm.getPassPctDouble()) : "0");
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildModuleFailDataJson(String packageName, String moduleName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestModule tm = runs[i].getTestModule(packageName, moduleName);
			if (tm != null) {
				double failPct = 100.0 - tm.getPassPctDouble();
				sb.append(String.format("%.1f", failPct));
			} else { sb.append("0"); }
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildModuleTcCountDataJson(String packageName, String moduleName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestModule tm = runs[i].getTestModule(packageName, moduleName);
			sb.append(tm != null ? tm.getNoTestCases() : "0");
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildModuleFailCountDataJson(String packageName, String moduleName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestModule tm = runs[i].getTestModule(packageName, moduleName);
			sb.append(tm != null ? (tm.getNoFailures() + tm.getNoErrors()) : "0");
		}
		sb.append("]");
		return sb.toString();
	}

	protected String buildModuleExecTimeDataJson(String packageName, String moduleName) {
		TestRun[] runs = th.getRuns().toArray(new TestRun[0]);
		StringBuilder sb = new StringBuilder("[");
		for (int i = runs.length - 1; i >= 0; i--) {
			if (sb.length() > 1) sb.append(",");
			TestModule tm = runs[i].getTestModule(packageName, moduleName);
			sb.append(tm != null ? String.format("%.3f", tm.getExecutionTimeDouble()) : "0");
		}
		sb.append("]");
		return sb.toString();
	}

	protected void generatePctBar(BufferedWriter buf, Double pct) throws IOException {
		if (pct.toString().equals("NaN")) {
			buf.write(t(5) + "<td class=\"graphBar\"><div class=\"progress-bar\"><div class=\"progress-fill progress-nan\" style=\"width:100%\"><span class=\"progress-label\">N/A</span></div></div></td>" + c_LF);
			return;
		}
		int greenWidth = pct.intValue();
		String label = String.format("%.0f%%", pct);
		buf.write(t(5) + "<td class=\"graphBar\"><div class=\"progress-bar\">"
			+ "<div class=\"progress-fill progress-pass\" style=\"width:" + greenWidth + "%\">"
			+ "<span class=\"progress-label\">" + label + "</span>"
			+ "</div></div></td>" + c_LF);
	}

	protected String pctBarHtml(double pctDouble, String pctStr) {
		int w = (int) pctDouble;
		return "<div class=\"progress-bar\">"
			+ "<div class=\"progress-fill progress-pass\" style=\"width:" + w + "%\">"
			+ "<span class=\"progress-label\">" + pctStr + "%</span>"
			+ "</div></div>";
	}

	protected void generateTestCaseHeader(BufferedWriter buf) throws IOException {
		buf.write(t(3) + "<tr>" + c_LF);
		buf.write(t(4) + "<th>Test case name</th>" + c_LF);
		buf.write(t(4) + "<th>Runs</th>" + c_LF);
		buf.write(t(4) + "<th>Pass</th>" + c_LF);
		buf.write(t(4) + "<th>Error</th>" + c_LF);
		buf.write(t(4) + "<th>Fail</th>" + c_LF);
		buf.write(t(4) + "<th>Ignored</th>" + c_LF);
		buf.write(t(4) + "<th>Ave ET</th>" + c_LF);
		buf.write(t(4) + "<th colspan=\"2\">Total</th>" + c_LF);
		buf.write(t(3) + "</tr>" + c_LF);
	}

	protected void generateTestCaseItem(BufferedWriter buf, TestCaseSummary tcs) throws IOException {
		buf.write(t(3) + "<tr>" + c_LF);
		buf.write(t(4) + "<td class=\"text-left\">" + tcs.getTestCaseName() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoRuns() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoPassed() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoErrors() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoFailures() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoIgnored() + "</td>" + c_LF);
		buf.write(t(4) + "<td class=\"mono\">" + tcs.getExecutionTime() + "</td>" + c_LF);
		buf.write(t(4) + "<td class=\"text-right\">" + tcs.getPassPct() + "%</td>" + c_LF);
		buf.write(t(4) + "<td class=\"graphBar\">" + pctBarHtml(tcs.getPassPctDouble(), tcs.getPassPct()) + "</td>" + c_LF);
		buf.write(t(3) + "</tr>" + c_LF);
	}

	protected void generateTestCaseSpreadInfo(BufferedWriter buf, boolean isModule) throws IOException {
		buf.write(t(2) + "<span id=\"spread\" class=\"section-anchor\"></span>" + c_LF);
		buf.write(t(2) + "<div class=\"section-heading\">" + c_LF);
		buf.write(t(3) + "<h3>Test case verdict spread</h3>" + c_LF);
		buf.write(t(2) + "</div>" + c_LF);
		buf.write(t(2) + "<p class=\"section-desc\">Test case verdicts in order of execution. "
			+ "<span class=\"legend-pass\">Pass</span> "
			+ "<span class=\"legend-norun\">No-run</span> "
			+ "<span class=\"legend-fail\">Fail/Error</span></p>" + c_LF);
	}

	protected void generateTestCaseSpreadHeader(BufferedWriter buf) throws IOException {
		buf.write(t(3) + "<tr>" + c_LF);
		buf.write(t(4) + "<th>Test case name</th>" + c_LF);
		buf.write(t(4) + "<th>Runs</th>" + c_LF);
		buf.write(t(4) + "<th>Pass</th>" + c_LF);
		buf.write(t(4) + "<th>Error</th>" + c_LF);
		buf.write(t(4) + "<th>Fail</th>" + c_LF);
		buf.write(t(4) + "<th>Ignored</th>" + c_LF);
		buf.write(t(4) + "<th>Spread</th>" + c_LF);
		buf.write(t(3) + "</tr>" + c_LF);
	}

	protected void generateTestCaseSpreadItem(BufferedWriter buf, TestCaseSummary tcs) throws IOException {
		buf.write(t(3) + "<tr>" + c_LF);
		buf.write(t(4) + "<td class=\"text-left\">" + tcs.getTestCaseName() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoRuns() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoPassed() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoErrors() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoFailures() + "</td>" + c_LF);
		buf.write(t(4) + "<td>" + tcs.getNoIgnored() + "</td>" + c_LF);
		buf.write(t(4) + "<td class=\"spread-cell\">" + c_LF);
		buf.write(t(5) + "<div class=\"spread-bar\">" + c_LF);
		generateSpreadBar(buf, tcs);
		buf.write(t(5) + "</div>" + c_LF);
		buf.write(t(4) + "</td>" + c_LF);
		buf.write(t(3) + "</tr>" + c_LF);
	}

	protected void generateSpreadBar(BufferedWriter buf, TestCaseSummary tcs) throws IOException {
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
			buf.write(t(6) + link + c_LF);
		}
	}
}
