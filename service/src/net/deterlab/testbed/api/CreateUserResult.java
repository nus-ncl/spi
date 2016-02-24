package net.deterlab.testbed.api;

/**
 * Result of a successful call to createUser on the Users service.  Contains
 * the uid chosen and an X.509 certificate logged in as that user for 30
 * minutes.
 *
 * @version 1.0
 * @author DETER Team
 */
public class CreateUserResult {
    /** User id */
    private String uid;
    /** Permissions */
    private byte[] identity;
    /**
     * Create an empty CreateUserResult
     */
    public CreateUserResult() {
	uid = null;
	identity = null;
    }

    /**
     * Create a UserResult with the given uid and X.509 identity
     * @param u the userid
     * @param id the identity
     */
    public CreateUserResult(String u, byte[] id) {
	uid = u;
	identity = id;
    }

    /**
     * Return the uid.
     * @return the uid
     */
    public String getUid() { return uid; }
    /**
     * Set the uid
     * @param u the new uid
     */
    public void setUid(String u) { uid = u; }
    /**
     * Return the X.509 identity.
     * @return the X.509 identity
     */
    public byte[] getIdentity() { return identity; }
    /**
     * Set the X.509 identity
     * @param id the new X.509 identity
     */
    public void setIdentity(byte[] id) { identity = id; }
}
