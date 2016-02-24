
package net.deterlab.testbed.api;

/**
 * The results of a request to change several parameters in the SPI, e.g., ACLs
 * or permissions or adding or removing users from circles or projects.  The
 * class includes the name of the attribute for which the change was attempted
 * and a flag indicating if the change succeeded (success).  If getSuccess()
 * returns false, the return value of getReason() contains information intended
 * for users describing why the request failed.
 * @author the DETER Team
 * @version 1.1
 */
public class ChangeResult extends ApiObject {
    /** The target of the request */
    protected String name;
    /** Reason for failure, null on success */
    protected String reason;
    /** True if the request scucceeded */
    protected boolean success;

    /**
     * Create an empty result
     */
    public ChangeResult() { }
    /**
     * Create and initialize a result.
     * @param n the name of the attribute
     * @param r reason for failure if any
     * @param s success or failure
     */
    public ChangeResult(String n, String r, boolean s) {
	name = n;
	reason = r;
	success = s;
    }

    /**
     * Return the attribute name
     * @return the attribute name
     */
    public String getName() { return name; }
    /**
     * Set the attribute name
     * @param n the new attribute name
     */
    public void setName(String n) { name = n; }
    /**
     * Return the failure reason.  This will be empty if success it true.
     * @return the failure reason
     */
    public String getReason() { return reason; }
    /**
     * Set the failure reason
     * @param v the new failure reason
     */
    public void setReason(String v) { reason = v; }
    /**
     * Return the success flag
     * @return the success flag
     */
    public boolean getSuccess() { return success; }
    /**
     * Set the success flag 
     * @param b the new success flag value
     */
    public void setSuccess(boolean b) { success = b; }
}
