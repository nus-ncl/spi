package net.deterlab.testbed.db;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.policy.CredentialStoreDB;
import net.deterlab.testbed.policy.PolicyFile;

/**
 * Class that collects ACL operations for multiple classes
 * @author DETER team
 * @version 1.1
 */

public abstract class PolicyObject extends DBObject {
    /** String for the token indicating all valid permissions */
    static public String ALL_PERMS = "ALL_PERMS";

    /** The type of this object, used to identify policy files */
    private String type;

    /**
     * Construct an PolicyObject.
     * @param t the object type
     * @throws DeterFault if the underlying DBObject fails to initialize
     */
    public PolicyObject(String t) throws DeterFault {
	super();
	type = t;
    }

    /**
     * Construct an PolicyObject with a SharedConnection.
     * @param t the object type
     * @param sc the SharedConnection
     * @throws DeterFault if the underlying DBObject fails to initialize
     */
    public PolicyObject(String t, SharedConnection sc) throws DeterFault {
	super(sc);
	type = t;
    }

    /**
     * Return the type of this object
     * @return the type of this object
     */
    protected String getPolicyType() { return type; }

    /**
     * Return the object's identifier. Subclasses must override.
     * @return the ID.
     */
    protected abstract String getID();

    /**
     * Get all valid permissions for an object
     * @return all valid permissions for an object
     * @throws DeterFault if there is a DB problem
     */
    public Set<String> getValidPerms() throws DeterFault {
	Set<String> rv = new HashSet<String>();
	try {
	    PreparedStatement p= getPreparedStatement(
		    "SELECT name FROM permissions " +
		    "WHERE valid_for = ?");
	    p.setString(1, type);
	    ResultSet r = p.executeQuery();

	    while (r.next())
		rv.add(r.getString(1));
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Cannot get valid " + type + " permissions: " + e);
	}
    }

    /**
     * Expand special tokens like ALL_PERMS and then validate the collection of
     * permissions. Return them as a set if valid, throw a fault if not.
     * @param perms the permissions to check
     * @return the permissions in a set
     * @throws DeterFault if the perms are invalid or there's a DB problem
     */
    public Set<String> validatePerms(Collection<String> perms)
	    throws DeterFault {
	Set<String> pSet = new HashSet<String>(perms);
	try {
	    if (pSet.contains(ALL_PERMS)) {
		pSet.remove(ALL_PERMS);
		pSet.addAll(getValidPerms());
	    }

	    if ( pSet.isEmpty()) return pSet;

	    StringBuilder sb = new StringBuilder(
		    "SELECT count(*) FROM permissions ");
	    for (int i = 0; i < pSet.size(); i++) {
		if ( i == 0 ) sb.append("WHERE (name =? ");
		else sb.append("OR name = ? ");
	    }
	    sb.append(") AND valid_for=?");
	    int idx = 1;

	    PreparedStatement p = getPreparedStatement(sb.toString());
	    for (String perm: pSet)
		p.setString(idx++, perm);
	    p.setString(idx++, type);
	    ResultSet r = p.executeQuery();
	    int rows = 0;
	    int count = 0;
	    while (r.next()) {
		if (rows ++ > 0 )
		    throw new DeterFault(DeterFault.internal,
			"Wrong number of rows in validatePerms?");
		count = r.getInt(1);
	    }
	    if ( count != pSet.size())
		throw new DeterFault(DeterFault.request, "Invalid permission");
	    return pSet;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Cannot validate " + type + " permissions: " + e);
	}
    }

    /**
     * Clear any old policy for this object and insert the current one.
     * @param uid user id if needed
     * @throws DeterFault if something is wrong internally
     */
    public void updatePolicyCredentials(String uid) throws DeterFault {
	Config config = new Config();
	String files = config.getProperty( type + "Policy");
	CredentialStoreDB cdb = null;

	if ( files == null)
	    throw new DeterFault(DeterFault.internal,
		    "No " + type + "Policy file?");

	try {
	    cdb = new CredentialStoreDB(getSharedConnection());
	    for (String fn: files.split(",") ) {
		PolicyFile policy = new PolicyFile(new File(fn));
		// Permissions are the valid permissions here.
		policy.updateCredentials(cdb, getID(), uid,
			getValidPerms(), null);
	    }
	    cdb.close();
	}
	catch (DeterFault df) {
	    if ( cdb != null ) cdb.forceClose();
	    throw df;
	}
    }


    /**
     * Clear any old policy for this object and insert the current one.
     * @throws DeterFault if something is wrong internally
     */
    public void updatePolicyCredentials() throws DeterFault {
	updatePolicyCredentials(null);
    }

    /**
     * Remove all credentials attached to this object
     * @throws DeterFault if something is wrong internally
     */
    public void removeCredentials() throws DeterFault {
	Config config = new Config();
	String files = config.getProperty(type + "RemovePolicy");
	CredentialStoreDB cdb = null;

	if ( files == null)
	    throw new DeterFault(DeterFault.internal,
		    "No " + type + "RemovePolicy file?");

	try {
	    cdb = new CredentialStoreDB(getSharedConnection());
	    for (String fn: files.split(",") ) {
		PolicyFile objectPolicy = new PolicyFile(new File(fn));
		objectPolicy.removeCredentials(cdb, getID(), null);
	    }
	    cdb.close();
	}
	catch (DeterFault df) {
	    if ( cdb != null ) cdb.forceClose();
	    throw df;
	}
    }

    /**
     * Update credentials when onwership changes.
     * @param oldOwner the old owner - may be null
     * @param newOwner the new owner
     * @throws DeterFault if something is wrong internally
     */
    public void updateOwnerCredentials(String oldOwner, String newOwner)
	    throws DeterFault {
	Config config = new Config();
	String files = config.getProperty(type + "OwnerPolicy");
	CredentialStoreDB cdb = null;

	// If there's no new owner or the ownership is the same, punt
	if (newOwner == null || newOwner.equals(oldOwner)) return;

	if ( files == null)
	    throw new DeterFault(DeterFault.internal,
		    "No " + type + "OwnerPolicy file?");

	try {
	    cdb = new CredentialStoreDB(getSharedConnection());
	    for (String fn: files.split(",") ) {
		PolicyFile ownerPolicy = new PolicyFile(new File(fn));
		// Permissions are the valid permissions here.
		ownerPolicy.updateCredentials(cdb, getID(), newOwner,
			null, null);
		if (oldOwner != null )
		    ownerPolicy.removeCredentials(cdb, getID(), oldOwner);
	    }
	    cdb.close();
	}
	catch (DeterFault df) {
	    if (cdb != null) cdb.forceClose();
	    throw df;
	}
    }
}
