/*
 * MIT License - see LICENSE.txt
 */

package unitth.junit;

/**
 * This class represents a single unique test case in any module in any run. Its
 * unique identifier is the module name and test case name. Apart from the name
 * the class only contain attributes for the test status
 * <code>TestCaseVerdict</code> and the time it took to execute the test case.
 * 
 */
public class TestCase extends TestItem {

	private TestCaseVerdict verdict;
	private String className;
	private String moduleName;

	/**
	 * Ctor, sets the initial verdict to pass
	 */
	public TestCase() {
		verdict = TestCaseVerdict.e_PASS;
	}

	/**
	 * Returns the name of the test class.
	 * 
	 * @return The name of the test class.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns the module name of the test class.
	 * 
	 * @return The module name of the test class.
	 */
	public String getModuleName() {
		return moduleName;
	}
	
	/**
	 * Returns the full name of the test case in the following format
	 * <module name>::<class name>::<test case name>
	 * 
	 * @return The full name of the test case.
	 */
	public String getFullName() {
		return moduleName + "::" + className + "::" + name;
	}
	
	/**
	 * Returns the verdict of the test case.
	 * 
	 * @return The verdict for this test case.
	 */
	public TestCaseVerdict getVerdict() {
		return verdict;
	}

	/**
	 * Sets the name of the test class for this test case.
	 * 
	 * @param s
	 *            The test class name.
	 */
	public void setClassName(String s) {
		className = s;
	}

	/**
	 * Sets the name of the test module for this test case.
	 * 
	 * @param s
	 *            The test class name.
	 */
	public void setModuleName(String s) {
		moduleName = s;
	}

	/**
	 * Sets the verdict of this test case.
	 * 
	 * @param v
	 *            The verdict to assign to this test case.
	 */
	public void setVerdict(TestCaseVerdict v) {
		verdict = v;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return name + " " + verdict + " " + executionTime;
	}
}

/* eof */
