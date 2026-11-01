/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import unitth.core.Log;
import unitth.core.ReportParser;
import unitth.core.UnitTHConfig;
import unitth.core.UnitTHException;

/**
 * Base of the SAX based report parsers; the parser is its own SAX handler.
 */
public abstract class SaxReportParser extends ReportParser {

	private SAXParser saxp = null;

	protected SaxReportParser(UnitTHConfig config) {
		super(config);
		try {
			saxp = newSecureFactory().newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			Log.error("Could not create SAX parser", e);
		}
	}

	/** SAX factory with DTD/external entity loading disabled (XXE hardening). */
	private static SAXParserFactory newSecureFactory()
			throws ParserConfigurationException, SAXException {
		SAXParserFactory f = SAXParserFactory.newInstance();
		f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		f.setFeature("http://xml.org/sax/features/external-general-entities", false);
		f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return f;
	}

	/** Runs the SAX parser on a file with this handler. */
	protected void parse(File file) throws UnitTHException {
		if (saxp == null) {
			throw new UnitTHException("No SAX parser available");
		}
		try {
			saxp.parse(file, this);
		} catch (IOException ioe) {
			throw new UnitTHException("IO errors occured... " + ioe.getMessage());
		} catch (SAXException saxe) {
			throw new UnitTHException("SAXException ..." + saxe.getMessage());
		} catch (RuntimeException e) {
			Log.error("Unexpected error while parsing", e);
			throw new UnitTHException("Unknown exception...");
		}
	}
}
