package net.deterlab.testbed.api;

import org.apache.axis2.AxisFault;

/**
 * A fault thrown from a failed call to a DETERlab service, it characterizes
 * the error broadly as well as providing a more detailed message that can be
 * provided to a UI.  The fault has three fields, a numeric error code for
 * programmatic classification, an error message which is a 1-to-1 mapping from
 * numeric code to a user-comprehensible message suitable for display.  In
 * addition therer is a detail message that gives additional information that
 * can be displayed to a user about specific causes of this error.
 * <p>
 * Successful oprtations never throw a DeterFault.  Some operations do report
 * partial successes through <a href="ChangeResult.html">ChangeResult</a>s.
 * <p>
 * The numeric error codes and their corresponding messages (in italics) are:
 *<dl>
 *<dt>ACCESS</dt><dd><i>Access Denied</i>.  User does not have teh right to carry this operation out on this object.  Repeated requests are unlikely to change the outcome.</dd>
 *<dt>REQUEST</dt><dd><i>Badly Formed Request</i>.  The user input had a pad parameter.  Correcting the problem can resolve the issue.</dd>
 *<dt>INTERNAL</dt><dd><i>Internal Error</i>. The server failed in an unexpected way.  Changes to user input are unlikely to resolve the issue.  Report the problem.</dd>
 *<dt>PASSWORD</dt><dd><i>Expored Password</i>. Only operations relevant to a password reset can be successfully carried out.</dd>
 *<dt>LOGIN</dt><dd><i>Not Logged In (or login expired)</i>.  The user must log in to carry out this operation.</dd>
 *<dt>UNIMPLEMENTED</dt><dd><i>Unimplemented feature.</i> Though legal and perhaps invoked properly, this operation is unimplemented.  Send money to developers.</dd>
 *</dl>
 *
 * @author DETER team
 * @version 1.0
 */
public class DeterFault extends Exception {
    /** The DETER specific error code, see constants */
    protected int errorCode;
    /** A human-readable general error message */
    protected String errorMessage;
    /** A human-readable error message that defines the specific cause of the
     * fault */
    protected String detailMessage;

    /** Error code: No Error - should never be returned */
    static public final int none = 0;
    /** Error code: Access Denied */
    static public final int access = 1;
    /** Error code: Badly Formed Request -  more info in the detail */
    static public final int request = 2;
    /** Error code: Internal Error - more info in the detail, but probably not
     * fixable by client*/
    static public final int internal = 3;
    /** Error code: Expired password - client can basically only reset their
     * password to a new value */
    static public final int password = 4;
    /** Error code: user is not logged in for a call that requires a valid
     * login. */
    static public final int login = 5;
    /** Error code: Feature is unimplemented.  */
    static public final int unimplemented = 6;
    /**
     * Default error strings for each code.  Keep these in sync with the
     * constants.
     */
    static protected final String[] defaultMessage = new String[] {
	"No Error", "Access Denied", "Badly Formed Request", "Internal Error",
	    "Expired Password", "Not Logged In (or login expired)",
	    "Unimplemented feature"
    };


    /**
     * Create a new Fault with the given code, message and detail string.  If
     * msg is null and code is known, a default error message string is used.
     * @param code the error code, one of the constants defined in this class.
     * @param msg the error message, e.g., "Bad Request"
     * @param detail the specific detail information, e.g., "Missing username"
     */
    public DeterFault(int code, String msg, String detail) {
	super();
	errorCode = code;
	if ( msg == null ) {
	    if ( code > 0 && code < defaultMessage.length ) 
		errorMessage = defaultMessage[code];
	    else 
		errorMessage = "Unknown code";
	} else errorMessage = msg;
	detailMessage = detail;
    }

    /**
     * Create a new Fault with the given code and detail string and an error
     * message derived from the code.
     * @param code the error code, one of the constants defined in this class.
     * @param detail the specific detail information, e.g., "Missing username"
     */
    public DeterFault(int code, String detail) { this(code, null, detail); }

    /**
     * Return the error code.
     * @return the error code.
     */
    public int getErrorCode() { return errorCode; }

    /**
     * Set the error code.
     * @param c the new error code.
     */
    public void setErrorCode(int c) { errorCode = c; }

    /**
     * Return the standard error message.
     * @return the error message.
     */
    public String getErrorMessage() { return errorMessage; }

    /**
     * Set the error message.
     * @param m the new error message.
     */
    public void setErrorMessage(String m) { errorMessage = m; }

    /**
     * Return the detail message.
     * @return the detail message.
     */
    public String getDetailMessage() { return detailMessage; }

    /**
     * Set the detail message.
     * @param m the new detail message.
     */
    public void setDetailMessage(String m) { detailMessage = m; }

    /**
     * Return a string representation of this fault
     * @return a string representation of this fault
     */
    public String toString() {
	StringBuilder rv = new StringBuilder();

	rv.append(getErrorMessage());
	rv.append(" (");
	rv.append(getErrorCode());
	rv.append("): ");
	rv.append(getDetailMessage());
	return rv.toString();
    }
}
