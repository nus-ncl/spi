package net.deterlab.testbed.circle;

import java.util.HashSet;
import java.util.Set;

/**
 * A struct to collect circles with rights to an object access-controlled by
 * circles  and the string representation of those rights.
 */
public class CirclePerms {
    /** The circle ID */
    String cid;
    /** The permissions */
    Set<String> perms;

    /**
     * Empty CirclePerms
     */
    public CirclePerms() {
	this(null, new HashSet<String>());
    }

    /**
     * Make a CirclePerms with only a name
     * @param n the name
     */
    public CirclePerms(String n) {
	this(n, new HashSet<String>());
    }

    /**
     * Make one from parts
     * @param c the circle id
     * @param p the permissions
     */
    public CirclePerms(String c, Set<String> p) { cid = c; perms = p; }
    /**
     * Get the circle ID
     * @return the circle ID
     */
    public String getCircleId() { return cid; }
    /**
     * Get the permissions
     * @return the permissions
     */
    public Set<String> getPerms() { return perms; }

    /**
     * Add a permission string
     * @param perm the string to add
     */
    public void addPermission(String perm) {
	perms.add(perm);
    }
}

