package net.deterlab.testbed.api;

/**
 * The results of a request to bootstrap the system.
 * @author the DETER Team
 * @version 1.1
 */
public class BootstrapUser extends ApiObject {
    /** The userid created */
    protected String uid;
    /** The userid created */
    protected String passwd;

    /**
     * Create an empty user
     */
    public BootstrapUser() { }
    /**
     * Create and initialize a result.
     * @param u the user ID
     * @param p the password
     */
    public BootstrapUser(String u, String p) {
	uid = u;
	passwd = p;
    }

    /**
     * Return the user ID
     * @return the user ID
     */
    public String getUid() { return uid; }
    /**
     * Set the user ID
     * @param u the new user ID
     */
    public void setUid(String u) { uid = u; }

    /**
     * Return the password
     * @return the password
     */
    public String getPassword() { return passwd; }
    /**
     * Set the password
     * @param p the new password
     */
    public void setPassword(String p) { passwd = p; }
}
