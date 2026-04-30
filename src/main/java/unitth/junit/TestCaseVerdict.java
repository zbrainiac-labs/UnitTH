/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

/**
 * This is the enum that represents the different final states for a
 * <code>TestCase</code> object.
 * 
 */
public enum TestCaseVerdict {
	e_PASS, e_FAIL, e_ERROR, e_NORUN, e_IGNORED;

	/**
	 * @see java.lang.Enum#toString()
	 */
	public String toString() {
		switch (this) {
		case e_PASS:
			return "PASS";
		case e_FAIL:
			return "FAIL";
		case e_ERROR:
			return "ERROR";
		case e_IGNORED:
			return "IGNORED";
		default:
			return "NORUN";
		}
	}
}
