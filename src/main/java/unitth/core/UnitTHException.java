/*
 * MIT License - see LICENSE.txt
 */

package unitth.core;

/**
 * This is the exception class for JUnitTH specific exceptions.
 * 
 */
public class UnitTHException extends Exception {

	static final long serialVersionUID = 3L;

	String msg = "undef";

	/**
	 * Ctor for the UnitTH exception, takes a message as in parameter.
	 * 
	 * @param msg
	 *            An message for this exception.
	 */
	// FIXME, there needs to be support for handling multiple messages.
	public UnitTHException(String... msg) {
		if (null != msg[0]) {
			this.msg = msg[0];
		}
	}
	
	public String getMessage() {
		return msg;
	}
}

/* eof */
