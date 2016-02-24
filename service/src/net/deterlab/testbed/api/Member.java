package net.deterlab.testbed.api;

/**
 * The members of a Project or Circle as returned by the API.  Each opbject
 * includes the user ID and the permissions that user is assigned in the
 * project or circle.
 *
 * @version 1.0
 * @author DETER Team
 */
public class Member {
    /** User id */
    private String uid;
    /** Permissions */
    private String[] permissions;
    /**
     * Create an empty Member
     */
    public Member() {
	uid = null;
	permissions = null;
    }

    /**
     * Create a member with the given id and permissions
     * @param u the userid
     * @param perms the permissions
     */
    public Member(String u, String[] perms) {
	uid = u;
	permissions = perms;
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
     * Return the permissions.
     * @return the permissions
     */
    public String[] getPermissions() { return permissions; }
    /**
     * Set the permissions
     * @param p the new permissions
     */
    public void setPermissions(String[] p) { permissions = p; }
}
