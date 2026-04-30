/*
 * MIT License - see LICENSE.txt
 */
package unitth.core;

import java.io.File;
import java.util.ArrayList;

import org.xml.sax.helpers.DefaultHandler;

import unitth.junit.TestHistory;

public abstract class ReportParser extends DefaultHandler {

	public abstract TestHistory parseRuns(ArrayList<ArrayList<File>> testRunFiles,
			ArrayList<String> dirs);
}
