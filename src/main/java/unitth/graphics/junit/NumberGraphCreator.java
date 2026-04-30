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

import unitth.core.UnitTHException;
import unitth.junit.TestHistory;
import unitth.junit.TestItemSummary;
import unitth.junit.TestModule;
import unitth.junit.TestPackage;
import unitth.junit.TestPackageSummary;
import unitth.junit.TestRun;

public abstract class NumberGraphCreator extends GraphCreator {

	protected static int NG_TRUE_WIDTH = TRUE_WIDTH;
	protected final static int NG_TRUE_HEIGHT = 80;
	protected final static int NG_LEFT_OFFSET = 30;
	protected static int NG_WIDTH = NG_TRUE_WIDTH + NG_LEFT_OFFSET;
	protected final static int NG_TOP_OFFSET = 5;
	protected final static int NG_BOTTOM_OFFSET = 5;
	protected final static int NG_HEIGHT = NG_TRUE_HEIGHT + NG_TOP_OFFSET
			+ NG_BOTTOM_OFFSET;
	protected final static int NG_YAXIS_LOCATION = NG_LEFT_OFFSET;
	protected final static int NG_XAXIS_LOCATION = NG_HEIGHT - NG_BOTTOM_OFFSET;
	protected static final int NG_STEP_WIDTH = 4;

	private BufferedImage testCaseImg = null;

	protected String mainImage = "";
	protected String filePrefix = "";
	protected Color color = Color.BLUE;

	public NumberGraphCreator(TestHistory history) {
		super(history);

		// Make sure the image width is enough relative to the number of runs
		if (null != history) {
			int minWidth = history.getNoRuns() * (NG_STEP_WIDTH)
					+ NG_LEFT_OFFSET;
			if (NG_TRUE_WIDTH < minWidth) {
				NG_TRUE_WIDTH = minWidth + 100;
				NG_WIDTH = NG_TRUE_WIDTH + NG_LEFT_OFFSET;
			}
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
	private void drawDiagramBase(Graphics2D g2, TestItemSummary tis) {
		// White background
		g2.setPaint(Color.WHITE);
		g2.fill(new Rectangle2D.Double(0, 0, NG_WIDTH, NG_HEIGHT));

		int xStartPoint = NG_LEFT_OFFSET - 2;
		int xEndPoint = NG_TRUE_WIDTH + 2;
		int yL50 = NG_XAXIS_LOCATION - (NG_TRUE_HEIGHT / 2);
		int yL100 = NG_XAXIS_LOCATION - NG_TRUE_HEIGHT;
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
		g2.draw(new Line2D.Double(NG_YAXIS_LOCATION, 0, NG_YAXIS_LOCATION,
				NG_HEIGHT - 2));
		// Place X-axis at the bottom offset, starting at the Y-axis -3pxs
		g2.draw(new Line2D.Double(NG_YAXIS_LOCATION - 3, NG_XAXIS_LOCATION,
				NG_TRUE_WIDTH + 3, NG_XAXIS_LOCATION));

		// Labels
		int scale = determineGraphScale(tis);
		g2.setPaint(Color.BLACK);
		g2.setFont(new Font("Verdana", Font.PLAIN, 9));
		g2.drawString(Integer.toString(scale), 0, yL100 + 4);
		if (25 != scale) {
			g2.drawString(Integer.toString(scale / 2), 0, yL50 + 4);
		}
	}

	/**
	 * This method creates the graphics object to draw the test case numbers on.
	 * Then it in order calls methods for drawing the background and population
	 * of the graph before saving it to a file. <br>
	 * The method is only called when to draw the overall test case numbers
	 * graph. There is a corresponding method to be called for drawing the test
	 * case numbers graphs for each module.
	 */
	public void drawNumbers() {
		testCaseImg = new BufferedImage(NG_WIDTH, NG_HEIGHT,
				BufferedImage.TYPE_INT_BGR);
		Graphics2D g2 = testCaseImg.createGraphics();
		drawDiagramBase(g2, null);
		populateNumbersGraph(g2);
		saveResultToImage(mainImage, testCaseImg);
		testCaseImg = null;
	}

	/**
	 * This method creates the graphics object to draw the test case numbers on.
	 * Then it in order calls methods for drawing the background and population
	 * of the graph before saving it to a file. <br>
	 * The method is only called when to draw the test case numbers graphs for
	 * unique test modules. There is a corresponding method to be called for
	 * drawing the test case numbers for the overall history.
	 */
	public void drawNumbers(TestItemSummary tis) {
		testCaseImg = new BufferedImage(NG_WIDTH, NG_HEIGHT,
				BufferedImage.TYPE_INT_BGR);
		Graphics2D g2 = testCaseImg.createGraphics();

		try {
			drawDiagramBase(g2, tis);
			populateNumbersGraph(g2, tis);
		} catch (UnitTHException uthe) {
			System.out.println(uthe.getMessage());
			uthe.printStackTrace();
		}
		saveResultToImage(
				filePrefix + tis.getName().replace('.', '-') + ".png",
				testCaseImg);
		testCaseImg = null;
	}

	/**
	 * This method is call when the overall test case graph is to be populated.
	 * 
	 * @param g2
	 *            The test case graphics object to be populated.
	 */
	private void populateNumbersGraph(Graphics2D g2) {

		if (null == history.getRuns() || null == history) {
			return;
		}

		int[] xCords = new int[history.getNoRuns() + 3];
		int[] yCords = new int[history.getNoRuns() + 3];

		int scale = determineGraphScale(null);

		// This is the first point
		xCords[0] = NG_LEFT_OFFSET + YAXIS_WIDTH;
		yCords[0] = (NG_TRUE_HEIGHT - (getGraphNumbers(history.getRuns()
				.first()) / scale)
				* NG_TRUE_HEIGHT)
				+ NG_TOP_OFFSET - YAXIS_WIDTH;

		// We want the bars to be printed in reverse order so we create an
		// array the Java 1.5 style.
		TestRun[] testModules = history.getRuns().toArray(
				new TestRun[history.getRuns().size()]);

		// Loop through all the x-cords and spit out the line
		int j = 1;
		for (int i = testModules.length - 1; i >= 0; i--, j++) {
			yCords[j] = (NG_TRUE_HEIGHT
					- (int) (((getGraphNumbers(testModules[i]) / (double) scale)) * (double) NG_TRUE_HEIGHT) + NG_TOP_OFFSET);

			// Compensate for any miss-calculations
			int diff = yCords[j] - NG_XAXIS_LOCATION;
			if (diff >= 0) {
				yCords[j] -= (diff + 2); // +2 to compensate for the axis
											// thickness.
			}

			xCords[j] = xCords[j - 1] + NG_STEP_WIDTH;
		}

		// Add the last two coordinates so that we get an closed polygon to fill
		xCords[j] = xCords[j - 1];
		yCords[j] = yCords[0] + 1; // Compensate +1 for the BasicStroke width
		xCords[j + 1] = xCords[0] + 1; // Compensate +1 for the BasicStroke
		// width
		yCords[j + 1] = yCords[0] + 1; // Compensate +1 for the BasicStroke
		// width

		populateNumbersGraphDraw(g2, xCords, yCords);
	}

	/**
	 * This method is call when a test module test case graph is to be
	 * populated.
	 * 
	 * @param g2
	 *            The test case graphics object to be populated.
	 */
	private void populateNumbersGraph(Graphics2D g2, TestItemSummary tis)
			throws UnitTHException {

		if (null == tis) {
			return;
		}

		int[] xCords = new int[history.getNoRuns() + 3];
		int[] yCords = new int[history.getNoRuns() + 3];

		int scale = determineGraphScale(tis);

		xCords[0] = NG_LEFT_OFFSET + YAXIS_WIDTH;
		yCords[0] = NG_XAXIS_LOCATION - YAXIS_WIDTH;

		// We want the bars to be printed in reverse order so we create an
		// array the Java 1.5 style.
		TestRun[] testRuns = history.getRuns().toArray(
				new TestRun[history.getRuns().size()]);

		// Loop through all the x-cords and spit out the line
		int j = 1;
		for (int i = testRuns.length - 1; i >= 0; i--, j++) {
			int getNumbers = 0;
			boolean notInRun = false;
			if (true == (tis instanceof TestPackageSummary)) {
				TestPackage tp = testRuns[i].getTestPackage(tis.getName());
				if (null != tp) {
					getNumbers = getGraphNumbers(tp);
				} else {
					notInRun = true;
				}
			} else {
				TestModule tm = testRuns[i].getTestModule(tis.getPackageName(),
						tis.getName());
				if (null != tm) {
					getNumbers = getGraphNumbers(tm);
				} else {
					notInRun = true;
				}
			}

			if (false == notInRun) {
				yCords[j] = (NG_TRUE_HEIGHT
						- (int) (((getNumbers / (double) scale)) * (double) NG_TRUE_HEIGHT) + NG_TOP_OFFSET);
			} else {
				yCords[j] = NG_XAXIS_LOCATION - YAXIS_WIDTH;
			}

			// Compensate for any miss-calculations
			int diff = yCords[j] - NG_XAXIS_LOCATION;
			if (diff >= 0) {
				yCords[j] -= (diff + 2); // +2 to compensate for the axis
											// thickness.
			}

			// Get the previous coordinate and add the step width
			xCords[j] = xCords[j - 1] + NG_STEP_WIDTH;
		}

		// Add the last two coordinates so that we get an closed polygon to fill
		xCords[j] = xCords[j - 1];
		yCords[j] = yCords[0] + 1; // Compensate +1 for the BasicStroke width
		xCords[j + 1] = xCords[0] + 1; // Compensate +1 for the BasicStroke
		// width
		yCords[j + 1] = yCords[0] + 1; // Compensate +1 for the BasicStroke
		// width

		populateNumbersGraphDraw(g2, xCords, yCords);
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
	private void populateNumbersGraphDraw(Graphics2D g2, int xCords[],
			int yCords[]) {
		// Draw polygon and fill it
		g2.setStroke(new BasicStroke(1.2f));
		g2.setComposite(makeComposite(3 * 0.1F));
		g2.setPaint(color);
		g2.fillPolygon(xCords, yCords, xCords.length);

		// Draw the graph, excluding the last two dots.
		g2.setComposite(makeComposite(1F));
		g2.drawPolyline(xCords, yCords, xCords.length - 2);
	}

	/**
	 * This method calculates the Y-axis scale to use ion the test case numbers
	 * graph for a unique module.
	 * 
	 * @param tms
	 *            The test module where to get the test case number s from.
	 * @return Returns and integer representing the Y-axis scale.
	 */
	// public abstract int determineGraphScale(TestModuleSummary tms);

	/**
	 * This method calculates the Y-axis scale to use ion the test case numbers
	 * graph for a unique module.
	 * 
	 * @param tps
	 *            The test module where to get the test case number s from.
	 * @return Returns and integer representing the Y-axis scale.
	 */
	public int determineGraphScale(TestItemSummary tis) {
		int largestNumberOfTcs = 0;
		if (null == tis) {
			largestNumberOfTcs = history.getLargestNumberOfTcs();
		} else {
			largestNumberOfTcs = tis.getLargestNumberOfTcs();
		}
		return determineScale(largestNumberOfTcs);
	}

	/**
	 * This method returns a scale depending on the largest number of test cases
	 * in a run or in the over all history. <br>
	 * There scale returned is divided into categories depending on the actual
	 * number of test cases. The categories are. <br>
	 * <code>
	 * Scales
	 * ------
	 * 25
	 * 50
	 * 100
	 * 200
	 * ...
	 * 1000
	 * 2000
	 * ...
	 * </code>
	 * 
	 * @param largestNumberOfTcs
	 *            The largest number of test cases to base the graph scale on.
	 * @return The Y-axis scale.
	 */
	public int determineScale(int largestNumberOfTcs) { // 0-25
		if (largestNumberOfTcs <= 10) {
			return 10;
		} else if (largestNumberOfTcs > 10 && largestNumberOfTcs <= 25) {
			return 25;
		} else if (largestNumberOfTcs > 25 && largestNumberOfTcs <= 50) { // 26-50
			return 50;
		} else if (largestNumberOfTcs > 50 && largestNumberOfTcs <= 100) { // 51-100
			return 100;
		} else if (largestNumberOfTcs > 100 && largestNumberOfTcs <= 1000) { // 101-999
			if (largestNumberOfTcs % 100 == 0) {
				return largestNumberOfTcs;
			} else {
				return largestNumberOfTcs / 100 * 100 + 100;
			}
		} else if (largestNumberOfTcs > 1000) { // 1000->
			if (largestNumberOfTcs % 1000 == 0) {
				return largestNumberOfTcs;
			} else {
				return largestNumberOfTcs / 1000 * 1000 + 1000;
			}
		} else {
			return 0; // Will in turn throw an arithmetic exception.
		}
	}

	/**
	 * This method shall be overridden by the subclasses that draw their graphs
	 * based on integer values. The method shall return a selectable value
	 * depending on the purpose of the sub class. As an example failure numbers
	 * graph returns the sum of failing and error test cases for passed test
	 * run.
	 * 
	 * @param tr
	 *            The test run to get chosen data from.
	 * @return Returns the selected data.
	 */
	protected abstract int getGraphNumbers(TestRun tr);

	/**
	 * This method shall be overridden by the subclasses that draw their graphs
	 * based on integer values. The method shall return a selectable value
	 * depending on the purpose of the sub class. As an example failure numbers
	 * graph returns the sum of failing and error test cases for passed test
	 * module.
	 * 
	 * @param tr
	 *            The test package to get chosen data from.
	 * @return Returns the selected data.
	 */
	protected abstract int getGraphNumbers(TestPackage tm);

	/**
	 * This method shall be overridden by the subclasses that draw their graphs
	 * based on integer values. The method shall return a selectable value
	 * depending on the purpose of the sub class. As an example failure numbers
	 * graph returns the sum of failing and error test cases for passed test
	 * module.
	 * 
	 * @param tr
	 *            The test run to get chosen data from.
	 * @return Returns the selected data.
	 */
	protected abstract int getGraphNumbers(TestModule tm);
}
