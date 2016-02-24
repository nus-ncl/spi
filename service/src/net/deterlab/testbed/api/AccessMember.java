package net.deterlab.testbed.api;

/**
 * Member of an access control list - a circle and the permissions granted it.
 * @author DETER team
 * @version 1.0
 */
public class AccessMember {
    /** Circle id */
    private String circleId;
    /** Permissions */
    private String[] permissions;
    /**
     * Create an empty Member
     */
    public AccessMember() {
	circleId = null;
	permissions = null;
    }

    /**
     * Create a member with the given id and permissions
     * @param cid the circle id
     * @param perms the permissions
     */
    public AccessMember(String cid, String[] perms) {
	circleId = cid;
	permissions = perms;
    }

    /**
     * Return the uid.
     * @return the uid
     */
    public String getCircleId() { return circleId; }
    /**
     * Set the uid
     * @param u the new uid
     */
    public void setCircleId(String u) { circleId = u; }
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
