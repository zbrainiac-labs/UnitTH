/*
 * MIT License - see LICENSE.txt
 */
package unitth.graphics.junit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import unitth.core.TestItemUtils;
import unitth.junit.TestHistory;
import unitth.junit.TestItemSummary;
import unitth.junit.TestModule;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestPackage;
import unitth.junit.TestPackageSummary;
import unitth.junit.TestRun;

/**
 * This class is responsible for creating graphs representing the execution
 * times for different test runs and test module runs.
 */
public class ExecutionTimeGraphCreator extends GraphCreator {

	private static int ET_TRUE_WIDTH = TRUE_WIDTH;
	private final static int ET_TRUE_HEIGHT = 160;
	private final static int ET_LEFT_OFFSET = 50;
	private static int ET_WIDTH = ET_TRUE_WIDTH + ET_LEFT_OFFSET;
	private final static int ET_TOP_OFFSET = 10;
	private final static int ET_BOTTOM_OFFSET = 5;
	private final static int ET_HEIGHT = ET_TRUE_HEIGHT + ET_TOP_OFFSET
			+ ET_BOTTOM_OFFSET;
	private final static int ET_YAXIS_LOCATION = ET_LEFT_OFFSET;
	private final static int ET_XAXIS_LOCATION = ET_HEIGHT - ET_BOTTOM_OFFSET;
	private static final int ET_STEP_WIDTH = 4;

	private static final String ET_PREFIX = "etg-";
	private static final String MAIN_EXEC_TIME_IMAGE = "et.png";

	private BufferedImage execTimesImg = null;

	/**
	 * CTOR, sets the history and the initial dimensions of the graph to draw.
	 * 
	 * @param history
	 */
	public ExecutionTimeGraphCreator(TestHistory history) {
		super(history);

		// Make sure the image width is enough relative to the number of runs
		if (null != history) {
			int minWidth = history.getNoRuns() * (ET_STEP_WIDTH)
					+ ET_LEFT_OFFSET;
			if (ET_TRUE_WIDTH < minWidth) {
				ET_TRUE_WIDTH = minWidth + 100;
				ET_WIDTH = ET_TRUE_WIDTH + ET_LEFT_OFFSET;
			}
		}
	}

	/**
	 * Creates the execution time graph for the main page.
	 * 
	 * @param draw
	 *            Boolean indicating if the graph shall be drawn or not.
	 */
	public void drawExecutionTimes(boolean draw) {
		if (draw) {
			execTimesImg = new BufferedImage(ET_WIDTH, ET_HEIGHT,
					BufferedImage.TYPE_INT_BGR);
			Graphics2D g2 = execTimesImg.createGraphics();
			drawETDiagramBase(g2, null);
			populateExecutionTimeGraph(g2);
			saveResultToImage(MAIN_EXEC_TIME_IMAGE, execTimesImg);
			execTimesImg = null;
		}
	}

	/**
	 * Creates the execution time graph for the main page.
	 * 
	 * @param draw
	 *            Boolean indicating if the graph shall be drawn or not.
	 */
	public void drawExecutionTimes(TestPackageSummary tps, boolean draw) {
		if (draw) {
			execTimesImg = new BufferedImage(ET_WIDTH, ET_HEIGHT,
					BufferedImage.TYPE_INT_BGR);
			Graphics2D g2 = execTimesImg.createGraphics();
			drawETDiagramBase(g2, tps);
			populateExecutionTimeGraph(g2, "", tps.getName(), true,
					(double) determineExecTimeGraphScale(tps) / 1000.0);
			saveResultToImage(ET_PREFIX + tps.getName().replace('.', '-')
					+ ".png", execTimesImg);
			execTimesImg = null;
		}
	}

	/**
	 * Creates the execution time graph for the main page.
	 * 
	 * @param draw
	 *            Boolean indicating if the graph shall be drawn or not.
	 */
	public void drawExecutionTimes(TestModuleSummary tms, boolean draw) {
		if (draw) {
			execTimesImg = new BufferedImage(ET_WIDTH, ET_HEIGHT,
					BufferedImage.TYPE_INT_BGR);
			Graphics2D g2 = execTimesImg.createGraphics();
			drawETDiagramBase(g2, tms);
			populateExecutionTimeGraph(g2, tms.getName(), tms.getPackageName(),
					false, (double) determineExecTimeGraphScale(tms) / 1000.0);
			saveResultToImage(ET_PREFIX + tms.getName().replace('.', '-')
					+ ".png", execTimesImg);
			execTimesImg = null;
		}
	}

	/**
	 * This method paints the background lines on the test case graphs. The
	 * lines represents, x/y-axises and the scale depending on the number of
	 * test cases.
	 * 
	 * @param g2
	 *            The graphics object o paint on.
	 */
	// private void drawETDiagramBase(Graphics2D g2, TestPackageSummary tps) {
	private void drawETDiagramBase(Graphics2D g2, TestItemSummary tps) {
		// White background
		g2.setPaint(Color.WHITE);
		g2.fill(new Rectangle2D.Double(0, 0, ET_WIDTH, ET_HEIGHT));

		int xStartPoint = ET_LEFT_OFFSET - 2;
		int xEndPoint = ET_TRUE_WIDTH + 2;
		int yL50 = ET_XAXIS_LOCATION - (ET_TRUE_HEIGHT / 2);
		int yL100 = ET_XAXIS_LOCATION - ET_TRUE_HEIGHT;
		Line2D.Double tcsHalfLine = new Line2D.Double(xStartPoint, yL50,
				xEndPoint, yL50);
		Line2D.Double tcsUpperLine = new Line2D.Double(xStartPoint, yL100,
				xEndPoint, yL100);

		g2.setPaint(Color.LIGHT_GRAY);
		g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND, 10.0f, new float[] { 3.0f }, 0.0f));
		g2.draw(tcsHalfLine);
		g2.draw(tcsUpperLine);

		// X and Y axises
		g2.setStroke(new BasicStroke((float) YAXIS_WIDTH));
		// Place Y-axis at the left offset, going down to the X-axis+2pxs
		g2.draw(new Line2D.Double(ET_YAXIS_LOCATION, 0, ET_YAXIS_LOCATION,
				ET_HEIGHT - 2));
		// Place X-axis at the bottom offset, starting at the Y-axis -3pxs
		g2.draw(new Line2D.Double(ET_YAXIS_LOCATION - 3, ET_XAXIS_LOCATION,
				ET_TRUE_WIDTH + 3, ET_XAXIS_LOCATION));

		// Labels
		int scale = determineExecTimeGraphScale(tps);
		g2.setPaint(Color.BLACK);
		g2.setFont(new Font("Verdana", Font.PLAIN, 9));

		String topLabel = formatLabel((double) scale / 1000.0);
		String bottomLabel = formatLabel((double) scale / 2000.0);

		g2.drawString(topLabel, 0, yL100 + 4);
		g2.drawString(bottomLabel, 0, yL50 + 4);
	}

	/**
	 * Formats a double value to a nice string.
	 * 
	 * @param lable
	 *            The double label to format to a nice string.
	 * @return The nicely formatted label.
	 */
	public String formatLabel(double label) {
		if (label <= 1.0) {
			return TestItemUtils.executionTimeToString(label);
		} else if (label > 1.0 && label <= 120.0) {
			String ret = String.format(Locale.US, "%1.3f", label);
			return ret.replaceAll(" ", "&nbsp;");
		} else if (label > 120.0 && label <= 7200.0) { // minutes
			String ret = null;
			if (label % 60.0 != 0.0) {
				if (label % 30.0 == 0) {
					ret = String.format(Locale.US, "%1.2f",
							((int) label / 60) + 0.3);
				} else {
					ret = String.format(Locale.US, "%1.2f", label / 60.0);
				}
			} else {
				ret = String.format(Locale.US, "%1.2f", label / 60.0);
			}
			return ret.replaceAll(" ", "&nbsp;");
		} else {
			// String ret = String.format(Locale.US, "%1.2f", label / 3600.0);
			String ret = null;
			if (label % 3600.0 != 0.0) {
				if (label % 1800.0 == 0) {
					ret = String.format(Locale.US, "%1.2f",
							((int) label / 3600) + 0.3);
				} else {
					ret = String.format(Locale.US, "%1.2f", label / 3600.0);
				}
			} else {
				ret = String.format(Locale.US, "%1.2f", label / 3600.0);
			}
			return ret.replaceAll(" ", "&nbsp;");
		}
	}

	/**
	 * This method calculates the Y-axis scale to use ion the test case numbers
	 * graph for a unique module.
	 * 
	 * @param tms
	 *            The test module where to get the test case number s from.
	 * @return Returns and integer representing the Y-axis scale.
	 */
	public int determineExecTimeGraphScale(TestItemSummary tis) {
		double largestExecutionTime = 0;
		if (null == tis) {
			largestExecutionTime = history.getLargestExecutionTime();
		} else {
			largestExecutionTime = tis.getLargestExecutionTime();
		}
		return determineScale(largestExecutionTime);
	}

	/**
	 * This method returns a scale depending on the largest execution time.
	 * There scale returned is divided into categories depending on the actual
	 * number execution times. The categories are. <br>
	 * <code>
	 * Scales ms, sec, minutes, hours
	 * ------
	 * 50
	 * 100
	 * 250
	 * 500
	 * 1000
	 * ...+1000
	 * 30000
	 * 60000
	 * 2min
	 * 5min
	 * 10min
	 * 20min
	 * 30min
	 * 60min
	 * 90min
	 * ...+30min
	 * </code>
	 * 
	 * @param largestExecutionTime
	 *            The largest execution time to base the scale on.
	 * @return The Y-axis scale.
	 */
	public int determineScale(double largestExecutionTime) {
		if (largestExecutionTime <= 0.050) {
			return 50;
		} else if (largestExecutionTime > 0.050
				&& largestExecutionTime <= 0.100) {
			return 100;
		} else if (largestExecutionTime > 0.100
				&& largestExecutionTime <= 0.250) {
			return 250;
		} else if (largestExecutionTime > 0.250
				&& largestExecutionTime <= 0.500) {
			return 500;
		} else if (largestExecutionTime > 0.500
				&& largestExecutionTime <= 1.000) {
			return 1000;
		} else if (largestExecutionTime > 1.000
				&& largestExecutionTime <= 30.000) { // 1-30 sec
			if (largestExecutionTime % 1.0 == 0.0) {
				return (int) largestExecutionTime * 1000;
				// or down?
			} else {
				return ((int) largestExecutionTime + 1) * 1000;
			}
		} else if (largestExecutionTime > 30.000
				&& largestExecutionTime <= 60.000) { // 30sec -> 1min
			return 60000;
		} else if (largestExecutionTime > 60.000
				&& largestExecutionTime <= 120.000) { // 1min -> 2min
			return 120000;
		} else if (largestExecutionTime > 120.000
				&& largestExecutionTime <= 300.000) { // 2min -> 5min
			return 300000;
		} else if (largestExecutionTime > 300.000
				&& largestExecutionTime <= 600.000) { // 5min -> 10min
			return 600000;
		} else if (largestExecutionTime > 600.000
				&& largestExecutionTime <= 1200.000) { // 10min -> 20min
			return 1200000;
		} else if (largestExecutionTime > 1200.000
				&& largestExecutionTime <= 1800.000) { // 20min -> 30min
			return 1800000;
		} else if (largestExecutionTime > 1800.000) { // 30min+
			if (largestExecutionTime % 1800.0 == 0) {
				return (int) largestExecutionTime * 1000;
			} else {
				return ((int) (largestExecutionTime / 1800) + 1) * 1800 * 1000;
			}
		} else {
			return 0; // Will in turn throw an arithmetic exception.
		}
	}

	/**
	 * This method is call when the overall test case graph is to be populated.
	 * 
	 * @param g2
	 *            The test case graphics object to be populated.
	 */
	private void populateExecutionTimeGraph(Graphics2D g2) {

		if (null == history.getRuns() || null == history) {
			return;
		}

		int[] xCords = new int[history.getNoRuns() + 3];
		int[] yCords = new int[history.getNoRuns() + 3];

		double scale = (double) determineExecTimeGraphScale(null) / 1000.0;

		// This is the first point
		xCords[0] = ET_LEFT_OFFSET + YAXIS_WIDTH;
		yCords[0] = ET_TRUE_HEIGHT + ET_TOP_OFFSET;

		// We want the bars to be printed in reverse order so we create an
		// array the Java 1.5 style.
		TestRun[] testModules = history.getRuns().toArray(
				new TestRun[history.getRuns().size()]);

		// Loop through all the x-cords and spit out the line
		int j = 1;
		for (int i = testModules.length - 1; i >= 0; i--, j++) {
			yCords[j] = (ET_TRUE_HEIGHT
					- (int) (((testModules[i].getExecutionTimeDouble() / scale)) * (double) ET_TRUE_HEIGHT) + ET_TOP_OFFSET);

			// Compensate for any miss-calculations
			int diff = yCords[j] - ET_XAXIS_LOCATION;
			if (diff >= 0) {
				yCords[j] -= (diff + 2); // +2 to compensate for the axis
				// thickness.
			}

			xCords[j] = xCords[j - 1] + ET_STEP_WIDTH;
		}

		// Add the last two coordinates so that we get an closed polygon to fill
		xCords[j] = xCords[j - 1];
		yCords[j] = yCords[0] + 1; // Compensate +1 for the BasicStroke width
		xCords[j + 1] = xCords[0] + 1; // Compensate +1 for the BasicStroke
		// width
		yCords[j + 1] = yCords[0] + 1; // Compensate +1 for the BasicStroke
		// width

		populateExecutionTimeGraphDraw(g2, xCords, yCords);
	}

	/**
	 * This method is call when a test module test case graph is to be
	 * populated.
	 * 
	 * @param g2
	 *            The test case graphics object to be populated.
	 */
	private void populateExecutionTimeGraph(Graphics2D g2, String name,
			String packageName, boolean isPackage, double scale) {

		int[] xCords = new int[history.getNoRuns() + 3];
		int[] yCords = new int[history.getNoRuns() + 3];

		xCords[0] = ET_LEFT_OFFSET + XAXIS_WIDTH;
		yCords[0] = ET_XAXIS_LOCATION - YAXIS_WIDTH;

		// We want the bars to be printed in reverse order so we create an
		// array the Java 1.5 style.
		TestRun[] testRuns = history.getRuns().toArray(
				new TestRun[history.getRuns().size()]);

		// Loop through all the x-cords and spit out the line
		int j = 1;
		for (int i = testRuns.length - 1; i >= 0; i--, j++) {

			// Get the pass rate from the specific module. This needs a
			// check first since the module might not be present in the run.
			double executionTime = 0.0;
			boolean notInRun = false;
			if (true == isPackage) {
				TestPackage tp = testRuns[i].getTestPackage(packageName);
				if (null != tp) {
					executionTime = tp.getExecutionTimeDouble();
				} else {
					notInRun = true;
				}
			} else {
				TestModule tm = testRuns[i].getTestModule(packageName, name);
				if (null != tm) {
					executionTime = tm.getExecutionTimeDouble();
				} else {
					notInRun = true;
				}
			}

			if (false == notInRun) {
				yCords[j] = (ET_TRUE_HEIGHT
						- (int) (((executionTime / scale)) * (double) ET_TRUE_HEIGHT) + ET_TOP_OFFSET);
			} else {
				yCords[j] = ET_XAXIS_LOCATION - YAXIS_WIDTH;
			}

			// Compensate for any miss-calculations
			int diff = yCords[j] - ET_XAXIS_LOCATION;
			if (diff >= 0) {
				yCords[j] -= (diff + 2); // +2 to compensate for the axis
											// thickness.
			}

			// Get the previous coordinate and add the step width
			xCords[j] = xCords[j - 1] + ET_STEP_WIDTH;
		}

		// Add the last two coordinates so that we get an closed polygon to fill
		xCords[j] = xCords[j - 1];
		yCords[j] = yCords[0] + 1;

		// Compensate +1 for the BasicStroke width
		xCords[j + 1] = xCords[0] + 1;
		// Compensate +1 for the BasicStroke width
		yCords[j + 1] = yCords[0] + 1;
		// Compensate +1 for the BasicStroke width
		populateExecutionTimeGraphDraw(g2, xCords, yCords);
	}

	/**
	 * This method performs the actual drawing of the test case numbers graph.
	 * It draws the transparent polygon and the line representing the actual
	 * curve.
	 * 
	 * @param g2
	 *            The graphics object to draw on.
	 * @param xCords
	 *            The line's X-coordinates
	 * @param yCords
	 *            The line's Y-coordinates
	 */
	private void populateExecutionTimeGraphDraw(Graphics2D g2, int xCords[],
			int yCords[]) {
		// Draw polygon and fill it
		g2.setStroke(new BasicStroke(1.4f));
		g2.setComposite(makeComposite(3 * 0.1F));
		g2.setPaint(Color.BLUE);
		g2.fillPolygon(xCords, yCords, xCords.length);

		// Draw the graph, excluding the last two dots.
		g2.setComposite(makeComposite(1F));
		g2.drawPolyline(xCords, yCords, xCords.length - 2);
	}
}

/* eof */
