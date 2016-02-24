package net.deterlab.testbed.db;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.circle.CirclePerms;
import net.deterlab.testbed.policy.CredentialStoreDB;
import net.deterlab.testbed.policy.PolicyFile;

/**
 * Class that collects ACL operations for multiple classes
 * @author DETER team
 * @version 1.1
 */

public abstract class ACLObject extends PolicyObject {
    /** The table containing the ACL for this object - the table mapping object
     * to circle to permission */
    private String table;
    /** The name of the column in the ACL table that links to the object table
     * that contains identifiers */
    private String linkIndex;
    /** The name of the table that contains object identifiers */
    private String objectTable;
    /** The name of the column that contains object identifiers */
    private String objectIDColumn;


    /**
     * Construct an ACLObject.
     * @param t the object type
     * @param tbl the table containing the ACL for this object - the table
     *	    mapping object to circle to permission.  
     * @param li name of the column in the ACL table that links to the object
     *	    table that contains identifiers 
     * @param ot the name of the table that contains object identifiers
     * @param otc the name of the column that contains object identifiers 
     * @throws DeterFault if the underlying DBObject fails to initialize
     */
    public ACLObject(String t, String tbl, String li, String ot, String otc)
	throws DeterFault {
	super(t);
	table = tbl;
	linkIndex = li;
	objectTable = ot;
	objectIDColumn = otc;
    }

    /**
     * Construct an ACLObject with a SharedConnection.
     * @param t the object type
     * @param tbl the table containing the ACL for this object - the table
     *	    mapping object to circle to permission.  
     * @param li name of the column in the ACL table that links to the object
     *	    table that contains identifiers 
     * @param ot the name of the table that contains object identifiers
     * @param otc the name of the column that contains object identifiers 
     * @param sc the SharedConnection
     * @throws DeterFault if the underlying DBObject fails to initialize
     */
    public ACLObject(String t, String tbl, String li, String ot, String otc,
	    SharedConnection sc) throws DeterFault {
	super(t, sc);
	table = tbl;
	linkIndex = li;
	objectTable = ot;
	objectIDColumn = otc;
    }

    /**
     * Return the permissions each circle has to this object
     * @return the permissions each circle has to this object
     * @throws DeterFault on DB error
     */
    protected Collection<CirclePerms> getPerms() throws DeterFault {
	PreparedStatement p = null;
	Map<String, CirclePerms> circles = new HashMap<String, CirclePerms>();

	if ( getID() == null ) return circles.values();

	try {
	    p = getPreparedStatement(
		    "SELECT circleid, name FROM " + table +" AS e " +
			"INNER JOIN circles AS c ON e.cidx = c.idx " +
			"INNER JOIN permissions AS p ON e.permidx = p.idx " +
			"WHERE " + linkIndex +
			    "=(SELECT idx FROM " + objectTable +
			    " WHERE " + objectIDColumn + "=?)");
	    p.setString(1, getID());
	    ResultSet r = p.executeQuery();
	    while ( r.next()) {
		String cid = r.getString(1);
		if ( !circles.containsKey(cid))
		    circles.put(cid, new CirclePerms(cid));
		circles.get(cid).addPermission(r.getString(2));
	    }

	    return circles.values();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error checking name: " + e);
	}
    }

    /**
     * Get the access control list.  Internally this translates from getPerms
     * to a list of AccessMembers.
     * @return the access control list.
     * @throws DeterFault if there is an error
     */
    public List<AccessMember> getACL() throws DeterFault {
	List<AccessMember> rv = new ArrayList<AccessMember>();

	for (CirclePerms cp : getPerms() )
	    rv.add(new AccessMember(cp.getCircleId(),
			cp.getPerms().toArray(new String[0])));
	return rv;
    }

    /**
     * Remove and regenerate the credentials for all the circles in this
     * object's access control list.
     * @throws DeterFault if something is wrong internally
     */
    public void updateCircleCredentials() throws DeterFault {
	CredentialStoreDB cdb = null;
	Config config = new Config();
	String files = config.getProperty(getPolicyType() + "CirclePolicy");

	if ( files == null)
	    throw new DeterFault(DeterFault.internal,
		    "No " + getPolicyType() + "CirclePolicy file?");

	try {
	    cdb = new CredentialStoreDB(getSharedConnection());
	    for ( CirclePerms cp : getPerms()) {
		for ( String fn : files.split(",")) {
		    PolicyFile circlePolicy = new PolicyFile(new File(fn));
		    circlePolicy.updateCredentials(cdb, getID(),
			    cp.getCircleId(), cp.getPerms(), null);
		}
	    }
	    cdb.close();
	}
	catch (DeterFault df) {
	    if (cdb != null) cdb.forceClose();
	    throw df;
	}
    }

    /**
     * Update the access control list for this object with the values in m.
     * If that circle is already in the ACL, remove it and replace the entries.
     * Otherwise add it.  If there are no permissions, remove the entry.
     * @param m the access info to change
     * @throws DeterFault on errors.
     */
    public void assignPermissions(AccessMember m) throws DeterFault {
	Set<String> perms = null;
	String[] memberPerms = m.getPermissions();
	try {

	    // If there are permissions to validate, do so before deleting
	    // anything.
	    if (memberPerms != null)
		perms = validatePerms(Arrays.asList(memberPerms));

	    // Delete any existing permissions
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM " + table + " " +
		    "WHERE " + linkIndex +
			"=(SELECT idx FROM " + objectTable + 
			    "  WHERE " + objectIDColumn + "=?) " +
			"AND " +
			"cidx=(SELECT idx FROM circles WHERE circleid =?)");
	    p.setString(1, getID());
	    p.setString(2, m.getCircleId());
	    p.executeUpdate();

	    // No permissions means delete the entry, which is done.
	    if ( perms == null || perms.isEmpty()) return;

	    // Add new permissions back in. It would be faster to build one
	    // statement and execute it, but an ACL modification is not time
	    // critical and this allows better error messages.
	    p = getPreparedStatement("INSERT INTO " + table + " " +
		    "(" + linkIndex +", cidx, permidx) " +
		    "VALUES (" +
			"(SELECT idx FROM " + objectTable + " " +
			    "WHERE " + objectIDColumn +"=?)," +
			"(SELECT idx FROM circles WHERE circleid=?)," +
			"(SELECT idx FROM permissions WHERE name=?))"
		);
	    p.setString(1, getID());
	    p.setString(2, m.getCircleId());
	    for ( String perm : perms) {
		p.setString(3, perm);
		p.executeUpdate();
	    }
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    throw new DeterFault(DeterFault.request,
		    "Badly formed permissions request for circle: " +
		    m.getCircleId());
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Unexpected SQL error: " + e);
	}
	finally {
	    updateCircleCredentials();
	}
    }
}
