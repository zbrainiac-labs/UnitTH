/*
 * MIT License - see LICENSE.txt
 */

/**
 * This class is responsible for creating various graphs that 
 * will be displayed in the history report. 
 * 
 */
package unitth.graphics.junit;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import unitth.core.UnitTH;
import unitth.junit.TestHistory;

/**
 * This class is responsible for drawing all graphs to be shown in the generated
 * history reports. The different types of graphs are: <br>
 * <ul>
 * <li>Pass rates</li>
 * <li>Test case numbers</li>
 * <li>Execution times</li>
 * </ul>
 * 
 */
public class GraphCreator {

	protected static int TRUE_WIDTH = 300;
	protected TestHistory history = null;
	protected int imgHeight = 160;
	protected int imgWidth = 600;
	protected int tcImgHeight = 180;
	protected int c_OF = 20;
	protected int c_xOF = 3;
	protected final static int YAXIS_WIDTH = 2;
	protected final static int XAXIS_WIDTH = 2;
	
	protected  boolean isFitNesse = false;
	
	/**
	 * The CTOR sets the history and calculates the width of the images to draw
	 * depending on the number of parsed runs.
	 * 
	 * @param history
	 *            The run history to base all graphics on.
	 */
	public GraphCreator(TestHistory history) {
		this.history = history;
	}
	
	/*
	 * This method created a transparent object to be used when drawing the
	 * transparent polygon in the test case graphs.
	 * 
	 * @param alpha
	 *            The float number between 0.1 and 1 representing the
	 *            transparency level.
	 * @return The <code>AlphaComposite</code> object to use when drawing.
	 */
	protected AlphaComposite makeComposite(float alpha) {
		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, alpha));
	}

	/*
	 * This method saves a graphics/<code>BufferedImage</code> object to a
	 * png file.
	 * 
	 * @param name
	 *            The name if the output image.
	 * @param img
	 *            The image to save.
	 */
	protected void saveResultToImage(String name, BufferedImage img) {
		File outputfile = new File(UnitTH.rootFolder + File.separator
				+ UnitTH.IMAGE_DIR + File.separator + name);
		try {
			outputfile.createNewFile();
			ImageIO.write(img, "png", outputfile);
		} catch (Exception e) {
			System.err.println("Tried to create file: '"+outputfile.getAbsolutePath()+"'.");
			e.printStackTrace(System.err);
		}
	}
}
