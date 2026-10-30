/*
 * MIT License - see LICENSE.txt
 */
package unitth.core;

import java.io.File;
import java.util.ArrayList;

import org.xml.sax.helpers.DefaultHandler;

import unitth.junit.TestHistory;

public abstract class ReportParser extends DefaultHandler {

	protected final UnitTHConfig config;

	protected ReportParser(UnitTHConfig config) {
		this.config = config;
	}

	public UnitTHConfig getConfig() {
		return config;
	}

	public abstract TestHistory parseRuns(ArrayList<ArrayList<File>> testRunFiles,
			ArrayList<String> dirs);
}
