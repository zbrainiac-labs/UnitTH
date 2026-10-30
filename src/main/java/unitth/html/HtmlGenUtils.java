/*
 * MIT License - see LICENSE.txt
 */
package unitth.html;

import unitth.core.UnitTHConfig;

public class HtmlGenUtils {

	/** Escapes text for use in HTML element content and attribute values. */
	public static String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '&': sb.append("&amp;"); break;
			case '<': sb.append("&lt;"); break;
			case '>': sb.append("&gt;"); break;
			case '"': sb.append("&quot;"); break;
			case '\'': sb.append("&#39;"); break;
			default: sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Maps a package/module name to a safe file name / element id fragment:
	 * anything outside [A-Za-z0-9._-] becomes '_'. Leading dots are replaced so
	 * the name can never be ".." or hidden.
	 */
	public static String fileSafe(String s) {
		if (s == null || s.isEmpty()) {
			return "_";
		}
		String safe = s.replaceAll("[^A-Za-z0-9._-]", "_");
		return safe.startsWith(".") ? "_" + safe.substring(1) : safe;
	}

	public static String createArgsDivTag(UnitTHConfig config) {
		StringBuilder ret = new StringBuilder("<div class=\"hidden\">\n");
		ret.append("Properties\n")
				.append("-+--------------------------------------------------+-\n");
		ret.append("unitth.generate.exectimegraphs=")
				.append(config.generateExecTimeGraphs()).append("\n");
		ret.append("unitth.html.report.path=").append(escapeHtml(config.reportPath())).append("\n");
		ret.append("unitth.report.dir=").append(escapeHtml(config.rootFolder())).append("\n");
		ret.append("unitth.xml.report.filter=").append(escapeHtml(config.xmlReportFilter())).append("\n");
		ret.append("unitth.use.absolute.paths=").append(config.useAbsPaths()).append("\n");
		ret.append("\nInput arguments\n")
				.append("-+--------------------------------------------------+-\n");

		for (String s : config.inputArgs()) {
			ret.append(escapeHtml(s)).append("\n");
		}
		ret.append("\n</div>");
		return ret.toString();
	}

	public static String calculateRunIdxString(int noRuns, int idx) {
		String noRunsStr = String.valueOf(noRuns);
		String idxStr = String.valueOf(idx);

		int diff = noRunsStr.length() - idxStr.length();
		String retStr = "";
		for (int i = 0; i < diff; i++) {
			retStr += "0";
		}
		return retStr + idxStr;
	}
}
