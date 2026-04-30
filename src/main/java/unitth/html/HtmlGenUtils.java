/*
 * MIT License - see LICENSE.txt
 */
package unitth.html;

import unitth.core.UnitTH;

public class HtmlGenUtils {
	public static String createArgsDivTag() {
		String ret = "<div class=\"hidden\">\n";
		ret += "Properties\n"
				+ "-+--------------------------------------------------+-\n";
		ret += "unitth.generate.exectimegraphs="
				+ UnitTH.generateExecTimeGraphs + "\n";
		ret += "unitth.html.report.path=" + UnitTH.reportPath + "\n";
		ret += "unitth.report.dir=" + UnitTH.rootFolder + "\n";
		ret += "unitth.xml.report.filter=" + UnitTH.xmlReportFilter + "\n";
		ret += "unitth.use.absolute.paths=" + UnitTH.useAbsPaths + "\n";
		ret += "\nInput arguments\n"
				+ "-+--------------------------------------------------+-\n";

		if (UnitTH.inputArgs != null) {
			for (String s : UnitTH.inputArgs) {
				ret += s + "\n";
			}
		}
		ret += "\n</div>";
		return ret;
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
