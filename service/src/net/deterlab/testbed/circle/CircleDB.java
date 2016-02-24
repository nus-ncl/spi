package net.deterlab.testbed.circle;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.Member;
import net.deterlab.testbed.db.PolicyObject;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.policy.CredentialStoreDB;
import net.deterlab.testbed.policy.PolicyFile;

/**
 * Interface to Circle information
 * @author DETER team
 * @version 1.1
 */
public class CircleDB  extends PolicyObject {
    /** The name of the circle */
    private String name;

    /** String for the token indicating all valid permissions */
    static public String ALL_PERMS = "ALL_PERMS";

    /**
     * Create an empty CircleDB
     * @throws DeterFault if the DB setup fails
     */
    public CircleDB() throws DeterFault {
	this(null, null);
    }

    /**
     * Create a new DB interface to the circle named n
     * @param n the name of the circle
     * @throws DeterFault if the name is improperly formatted or DB setup fails
     */
    public CircleDB(String n) throws DeterFault {
	this(n, null);
    }

    /**
     * Create a new DB interface to the circle named n using shared connection
     * sc.
     * @param n the name of the circle
     * @param sc the shared connection
     * @throws DeterFault if the name is improperly formatted or DB setup fails
     */
    public CircleDB(String n, SharedConnection sc) throws DeterFault {
	super("circle", sc);
	try {
	    if ( n!= null )
		checkScopedName(n);
	}
	catch (DeterFault df) {
	    forceClose();
	    throw df;
	}
	name = n;
    }

    /**
     * Let the PolicyObject class know the name
     * @return the name
     */
    protected String getID() { return getName(); }
    
    /**
     * Return the circle name
     * @return the circle name
     */
    public String getName() { return name; }
    /**
     * Set the circle name
     * @param n the new name
     * @throws DeterFault if the name is improperly formatted
     */
    public void setName(String n) throws DeterFault {
	checkScopedName(n); name = n;
    }

//    /**
//     * Get all valid permissions for a circle
//     * @return all valid permissions for a circle
//     * @throws DeterFault if there is a DB problem
//     */
//    public Set<String> getValidPerms() throws DeterFault {
//	Set<String> rv = new HashSet<String>();
//	try {
//	    PreparedStatement p= getPreparedStatement(
//		    "SELECT name FROM permissions WHERE valid_for = 'circle'");
//	    ResultSet r = p.executeQuery();
//
//	    while (r.next())
//		rv.add(r.getString(1));
//	    return rv;
//	}
//	catch (SQLException e) {
//	    throw new DeterFault(DeterFault.internal,
//		    "Cannot get valid circle permissions: " + e);
//	}
//    }

//    /**
//     * Expand special tokens like ALL_PERMS and then validate the collection of
//     * permissions. Return them as a set if valid, throw a fault if not.
//     * @param perms the permissions to check
//     * @return the permissions in a set
//     * @throws DeterFault if the perms are invalid or there's a DB problem
//     */
//    public Set<String> validatePerms(Collection<String> perms)
//	    throws DeterFault {
//	Set<String> pSet = new HashSet<String>(perms);
//	try {
//	    if (pSet.contains(ALL_PERMS)) {
//		pSet.remove(ALL_PERMS);
//		pSet.addAll(getValidPerms());
//	    }
//
//	    if ( pSet.isEmpty()) return pSet;
//
//	    StringBuilder sb = new StringBuilder(
//		    "SELECT count(*) FROM permissions ");
//	    for (int i = 0; i < pSet.size(); i++) {
//		if ( i == 0 ) sb.append("WHERE (name =? ");
//		else sb.append("OR name = ? ");
//	    }
//	    sb.append(") AND valid_for = 'circle'");
//	    int idx = 1;
//
//	    PreparedStatement p = getPreparedStatement(sb.toString());
//	    for (String perm: pSet)
//		p.setString(idx++, perm);
//	    ResultSet r = p.executeQuery();
//	    int rows = 0;
//	    int count = 0;
//	    while (r.next()) {
//		if (rows ++ > 0 )
//		    throw new DeterFault(DeterFault.internal,
//			"Wrong number of rows in validatePerms?");
//		count = r.getInt(1);
//	    }
//	    if ( count != pSet.size())
//		throw new DeterFault(DeterFault.request, "Invalid permission");
//	    return pSet;
//	}
//	catch (SQLException e) {
//	    throw new DeterFault(DeterFault.internal,
//		    "Cannot validate circle permissions: " + e);
//	}
//    }

    /**
     * Remove and regenerate the credentials for this user in this circle
     * @param uid the user to reset
     * @throws DeterFault if something is wrong internally
     */
    public void updateUserCredentials(String uid)
	    throws DeterFault {

	if (uid == null ) return;

	Config config = new Config();
	String files = config.getProperty("circleUserPolicy");
	CredentialStoreDB cdb = null;

	try {
	    cdb = new CredentialStoreDB(getSharedConnection());
	    for ( String fn : files.split(",")) {
		PolicyFile userPolicy = new PolicyFile(new File(fn));
		userPolicy.updateCredentials(cdb, getName(), uid,
			getPerms(uid), null);
	    }
	    cdb.close();
	}
	catch (DeterFault df) {
	    if (cdb != null) cdb.forceClose();
	    throw df;
	}
    }

//    /**
//     * Remove the user credentials from the old owner (if any) and create an
//     * owner credential for the new one.
//     * @param oldOwner the old owner (may be null)
//     * @param newOwner the new owner
//     * @throws DeterFault if something is wrong internally
//     */
//    public void updateOwnerCredentials(String oldOwner, String newOwner)
//	    throws DeterFault {
//
//	if (newOwner == null ) return;
//
//	Config config = new Config();
//	String files = config.getProperty("circleOwnerPolicy");
//	CredentialStoreDB cdb = null;
//
//	try {
//	    cdb = new CredentialStoreDB(getSharedConnection());
//	    for ( String fn : files.split(",")) {
//		PolicyFile userPolicy = new PolicyFile(new File(fn));
//		userPolicy.updateCredentials(cdb, getName(), newOwner,
//			null, null);
//		if ( oldOwner != null)
//		    userPolicy.removeCredentials(cdb, getName(), oldOwner);
//	    }
//	    cdb.close();
//	}
//	catch (DeterFault df) {
//	    if ( cdb != null ) cdb.forceClose();
//	    throw df;
//	}
//    }

    /**
     * Remove user credentials for uid. If uid is null, remove all the
     * credentials for this circle.
     * @param uid the user to clear
     * @throws DeterFault if something is wrong internally
     */
    public void removeUserCredentials(String uid)
	    throws DeterFault {
	Config config = new Config();
	String files = config.getProperty((uid != null) ?
		"circleUserRemovePolicy" : "circleRemovePolicy");
	CredentialStoreDB cdb = null;

	try {
	    cdb = new CredentialStoreDB(getSharedConnection());
	    for ( String fn : files.split(",")) {
		PolicyFile policy = new PolicyFile(new File(fn));
		policy.removeCredentials(cdb, getName(), uid);
	    }
	    cdb.close();
	}
	catch (DeterFault df) {
	    if (cdb != null ) cdb.forceClose();
	    throw df;
	}
    }

    /**
     * Insert the circle policy.  This will need to be made less ad hoc.
     * @throws DeterFault if something is wrong internally
     */
    public void updateCirclePolicy() throws DeterFault {
    	updatePolicyCredentials();
    }

    /**
     * Look the circle up in the DB and make sure that it exists.
     * @return true if the circle is valid
     * @throws DeterFault if something is wrong internally
     */
    public boolean isValid() throws DeterFault{
	if ( getName() == null ) return false;

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT idx FROM circles " + 
			"WHERE circleid =?");
	    p.setString(1, name);
	    ResultSet r = p.executeQuery();

	    // Cryptic, but we want r.next() to return true exactly once.  If
	    // there is no index, the first call will return false, if there is
	    // more than one the second call will return true;
	    return r.next() && !r.next();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Return true if uid is a member of this circle
     * @param uid the user ID to test
     * @return true if uid is a member of this circle
     * @throws DeterFault if there is a problem
     */
    public boolean isMember(String uid) throws DeterFault {
	if ( getName() == null || uid == null ) return false;

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT COUNT(*) FROM circleusers " +
			"WHERE uidx=(SELECT idx FROM users WHERE uid=?) "+ 
			"AND cidx=(SELECT idx FROM circles WHERE circleid=?)");
	    p.setString(1, uid);
	    p.setString(2, getName());

	    ResultSet r = p.executeQuery();
	    int rows = 0;
	    boolean rv = false;
	    while (r.next()) {
		if (rows++ > 0)
		    throw new DeterFault(DeterFault.internal,
			    "Strange circleusers??");
		if ( r.getInt(1) > 0 ) rv = true;
	    }
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
    /**
     * Return true if this circle is linked to a project.  Such circles will
     * have their membership operations handled by the project.
     * @return true if the circle is linked to a project
     * @throws DeterFault if there are DB problems.
     */
    public boolean isLinked() throws DeterFault {

	if ( getName() == null) return false;

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT idx FROM projects " +
		    "WHERE linkedidx=" +
			"(SELECT idx FROM circles WHERE circleid=?)");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();
	    // Cryptic, see isValid().
	    return r.next() && !r.next();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Return the members of the circle with the given permission
     * @param perm the permission value
     * @return the members of the circle
     * @throws DeterFault if something is wrong internally
     */
    public List<String> getUidsByPerm(String perm) throws DeterFault {
	List<String> rv = new ArrayList<String>();

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "getMembers failed. Circle does not have a name");
	if ( perm == null )
	    throw new DeterFault(DeterFault.internal,
		    "getMembers failed. Null query permission");
	try {
	    // Looking for a user with a permission, go right to circleperms
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM circleperms " +
			"LEFT JOIN users AS u ON uidx=u.idx " +
		    "WHERE cidx=" +
			"(SELECT idx FROM circles WHERE circleid=?) " +
			"AND permidx=" +
			    "(SELECT idx FROM permissions " +
				"WHERE name = ? AND valid_for='circle')");
	    p.setString(1, getName());
	    p.setString(2, perm);
	    ResultSet r = p.executeQuery();
	    while (r.next())
		rv.add(r.getString(1));
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
    /**
     * Return the members of the circle
     * @return the members of the circle
     * @throws DeterFault if something is wrong internally
     */
    public List<Member> getMembers() throws DeterFault {
	List<Member> m = new ArrayList<Member>();

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "getMembers failed. Circle does not have a name");

	try {
	    Map<String, Set<String>> users = new HashMap<String, Set<String>>();

	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM circleusers LEFT JOIN users AS u " +
			"ON uidx = u.idx " +
		    "WHERE cidx=(SELECT idx FROM circles WHERE circleid=?)");
	    p.setString(1, getName());
	    ResultSet r= p.executeQuery();
	    while (r.next())
		users.put(r.getString(1), new HashSet<String>());

	    p = getPreparedStatement(
		    "SELECT uid, name FROM circleperms " +
			"LEFT JOIN permissions AS p ON permidx = p.idx " +
			"LEFT JOIN users AS u ON uidx = u.idx " +
			"WHERE cidx=(SELECT idx FROM circles " +
			    "WHERE circleid=?)");
	    p.setString(1, getName());
	    r = p.executeQuery();

	    while (r.next()) {
		String uid = r.getString(1);

		if ( !users.containsKey(uid) )
		    throw new DeterFault(DeterFault.internal,
			    "circleusers and circleperms out of sync!");
		users.get(uid).add(r.getString(2));
	    }

	    for (String uid: users.keySet())
		m.add(new Member(uid, users.get(uid).toArray(new String[0])));
	    return m;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Return the permissions of this user in the circle
     * @return the permissions of this user in the circle
     * @param uid the user to get
     * @return the permissions
     * @throws DeterFault if something is wrong internally
     */
    public Set<String> getPerms(String uid) throws DeterFault {
	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "getPerms failed. Circle does not have a name");

	if ( uid == null )
	    throw new DeterFault(DeterFault.internal, 
		    "getPerms failed. no uid");

	try {
	    if ( !isMember(uid))
		throw new DeterFault(DeterFault.request,
			"getPerms failed. Cannot find uid " + uid);

	    Set<String> rv = new HashSet<String>();
	    PreparedStatement p = getPreparedStatement(
		    "SELECT name FROM circleperms " +
			"LEFT JOIN permissions AS p ON permidx = p.idx " +
			"WHERE cidx=(SELECT idx FROM circles " +
			    "WHERE circleid=?) "+
			"AND uidx = (SELECT idx FROM users WHERE uid = ?)");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    ResultSet r = p.executeQuery();
	    while (r.next())
		rv.add(r.getString(1));
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Get the circle's owner
     * @return the circle's owner
     * @throws DeterFault if there is an error
     */
    public String getOwner() throws DeterFault {
	String rv = null;

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal,
		    "getOwner failed. Circle does not have a name");

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM circles LEFT JOIN users AS u " +
			"ON u.idx = owneridx " +
		    "WHERE circleid=?");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();
	    if (!r.next() ) 
		throw new DeterFault(DeterFault.internal,
			"No owner for circle??");
	    rv = r.getString(1);
	    if (r.next() ) 
		throw new DeterFault(DeterFault.internal,
			"More than one owner for circle??");
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Set the circle's owner to the new uid.  The uid must be a member of the
     * cricle
     * @param o the new onwer's uid
     * @throws DeterFault if the uid is not a member or there is a DB problem
     */
    public void setOwner(String o) throws DeterFault {

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "setOwner failed. Circle does not have a name");
	if ( o  == null ) 
	    throw new DeterFault(DeterFault.request, 
		    "setOwner failed. No owner provided");
	if ( !isMember(o) )
		throw new DeterFault(DeterFault.internal, 
			"setOwner failed. New owner is not a member");
	String oldOwner = getOwner();
	try {
	    PreparedStatement p = getPreparedStatement(
		    "UPDATE circles " +
		    "SET owneridx = (SELECT idx FROM users WHERE uid = ?) " +
		    "WHERE circleid=?");
	    p.setString(1, o);
	    p.setString(2, getName());
	    p.executeUpdate();
	    updateOwnerCredentials(oldOwner, o);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Add the given member to this circle with the given permissions.  If the
     * member is already in the group, throw an DeterFault.
     * @param uid user to add
     * @param perms permissions
     * @throws DeterFault on error
     */
    public void addUser(String uid, Set<String> perms)
	    throws DeterFault {
	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "addUser failed. Circle does not have a name");

	try {

	    if (isMember(uid))
		throw new DeterFault(DeterFault.request,
			uid + " is already a member of " + getName());

	    Set<String> valid = getValidPerms();
	    PreparedStatement p = getPreparedStatement(
		    "INSERT INTO circleusers (cidx, uidx) " +
			"VALUES ((SELECT idx FROM circles WHERE circleid=?), "+
			    "(SELECT idx FROM users WHERE uid=?))");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    p.executeUpdate();

	    p = getPreparedStatement(
		    "INSERT INTO circleperms (cidx, uidx, permidx) " +
			"VALUES ((SELECT idx FROM circles WHERE circleid=?), "+
			    "(SELECT idx FROM users WHERE uid=?), " +
			    "(SELECT idx FROM permissions WHERE name=? " +
				"AND valid_for='circle'))");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    for ( String perm : perms) {
		if (!valid.contains(perm)) continue;
		p.setString(3, perm);
		p.executeUpdate();
	    }
	    updateUserCredentials(uid);
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    // Constraint exceptions happen because the subqueries fail to find
	    // indices for a user or circle.  Usually this is a bad or
	    // duplicate username.
	    throw new DeterFault(DeterFault.request,
		    "Bad or duplicate uid (or bad circleid)");
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
    /**
     * Change the given user's permissions.
     * @param uid user to modify
     * @param perms permissions
     * @throws DeterFault on error
     */
    public void setPermissions(String uid, Set<String> perms)
	throws DeterFault {

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "addUser failed. Circle does not have a name");
	try {
	    Set<String> valid = getValidPerms();

	    if ( !isMember(uid))
		throw new DeterFault(DeterFault.request,
			"No such user in circle");

	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM circleperms " +
		    "WHERE cidx=(SELECT idx FROM circles where circleid = ?) " +
		    "AND uidx=(SELECT idx FROM users where uid = ?)");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    p.executeUpdate();

	    p = getPreparedStatement(
		    "INSERT INTO circleperms (cidx, uidx, permidx) " +
			"VALUES ((SELECT idx FROM circles WHERE circleid=?), "+
			    "(SELECT idx FROM users WHERE uid=?), " +
			    "(SELECT idx FROM permissions WHERE name=? "
				+ "AND valid_for='circle'))");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    for ( String perm : perms) {
		if (!valid.contains(perm)) continue;
		p.setString(3, perm);
		p.executeUpdate();
	    }
	    updateUserCredentials(uid);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Remove the given user from this circle, if they are in it.
     * @param uid the uid to remove
     * @throws DeterFault if there is a problem
     */
    public void removeUser(String uid) throws DeterFault {
	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "removeUser failed. Circle does not have a name");
	if ( uid == null)
	    throw new DeterFault(DeterFault.internal,
		    "removeUser failed. no uid given");
	try {
	    String owner = getOwner();

	    if ( uid.equals(owner))
		throw new DeterFault(DeterFault.request, "Cannot remove owner");
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM circleusers WHERE cidx=" +
			"(SELECT idx FROM circles WHERE circleid = ?) " +
			"AND uidx=(SELECT idx FROM users WHERE uid=?)");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    if ( p.executeUpdate() < 1) 
		throw new DeterFault(DeterFault.request,
			"Could not remove user - not present");
	    p = getPreparedStatement(
		    "DELETE FROM circleperms WHERE cidx=" +
			"(SELECT idx FROM circles WHERE circleid = ?) " +
			"AND uidx=(SELECT idx FROM users WHERE uid=?)");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    p.executeUpdate();
	    removeUserCredentials(uid);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Create the circle.  The owner is made a member with all permissions.
     * @param owner the new circle's owner
     * @throws DeterFault if there is a problem
     */
    public void create(String owner) throws DeterFault {
	PreparedStatement p = null;

	if ( getName() == null)
	    throw new DeterFault(DeterFault.internal, 
		    "create failed. Circle does not have a name");
	try {
	    try {
		p = getPreparedStatement(
			"INSERT INTO circles (circleid, owneridx) VALUES " +
			"(?, (SELECT idx FROM users WHERE uid=?))");
		p.setString(1, getName());
		p.setString(2, owner);
		p.executeUpdate();
	    }
	    catch (SQLIntegrityConstraintViolationException e) {
		throw new DeterFault(DeterFault.request, "Owner is invalid");
	    }

	    p = getPreparedStatement(
		    "INSERT INTO circleusers (cidx, uidx) " +
			"VALUES ((SELECT idx FROM circles WHERE circleid=?), "+
			"(SELECT idx FROM users WHERE uid=?))");
	    p.setString(1, getName());
	    p.setString(2, owner);
	    p.executeUpdate();

	    // The INSERT .. SELECT adds all valid permissions in one DB call
	    p = getPreparedStatement(
		    "INSERT INTO circleperms (cidx, uidx, permidx) " +
			"SELECT (SELECT idx FROM circles WHERE circleid=?), "+
			"(SELECT idx FROM users WHERE uid=?), idx " +
			    "FROM permissions WHERE valid_for='circle'");
	    p.setString(1, getName());
	    p.setString(2, owner);
	    p.executeUpdate();
	    
	    updatePolicyCredentials();
	    updateOwnerCredentials(null, owner);
	    updateUserCredentials(owner);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Completely remove this circle.  The profile must be empty when this is
     * called - it must have had removeAll() called.  All challenges must be
     * removed as well.
     * @throws DeterFault if something goes wrong
     */
    public void remove() throws DeterFault {
	if ( getName() == null)
	    throw new DeterFault(DeterFault.internal, 
		    "remove failed. Circle does not have a name");

	// Clear credentials first as they refer to the circle
	removeUserCredentials(null);
	try {
	    // Remove the users
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM circleusers " +
		    "WHERE cidx=(SELECT idx FROM circles WHERE circleid=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    p = getPreparedStatement(
		    "DELETE FROM circleperms " +
		    "WHERE cidx=(SELECT idx FROM circles WHERE circleid=?)");
	    p.setString(1, getName());
	    p.executeUpdate();
	    // Credentials
	    removeUserCredentials(null);

	    // Now get out of any experiment and library ACLs (the call above
	    // removes the ABAC permissions)
	    p = getPreparedStatement(
		    "DELETE FROM experimentperms " +
		    "WHERE cidx=(SELECT idx FROM circles WHERE circleid=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    p = getPreparedStatement(
		    "DELETE FROM libraryperms " +
		    "WHERE cidx=(SELECT idx FROM circles WHERE circleid=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    // and then the circle
	    p = getPreparedStatement("DELETE FROM circles WHERE circleid =?");
	    p.setString(1, getName());
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Get a list of all Circles that the uid is a member of.  The method
     * returns a list of CircleDBs that share a connection. If regex is given,
     * only circleids that match it will be returned.  If a shared connection
     * is given it will be used.
     * @param uid the uid to look for (if given)
     * @param regex the regex to use (if given)
     * @param sc the shared connection
     * @return a list of CircleDBs that match
     * @throws DeterFault on errors
     */
    static public List<CircleDB> getCircles(String uid, String regex,
	    SharedConnection sc) throws DeterFault {
	Connection c = null;
	PreparedStatement p = null;
	List<CircleDB> circles = new ArrayList<CircleDB>();

	if ( sc == null ) sc = new SharedConnection();

	try {
	    sc.open();
	    c = sc.getConnection();
	    if ( uid != null ) {
		if ( regex != null ) {
		    p = c.prepareStatement(
			    "SELECT circleid FROM " +
				"circleusers AS u LEFT JOIN circles AS c " +
				    "ON cidx = c.idx " +
			    "WHERE u.uidx=(SELECT idx FROM users " +
				    "WHERE uid =?) "+
				"AND circleid REGEXP ?");
		    p.setString(1, uid);
		    p.setString(2, regex);
		}
		else {
		    p = c.prepareStatement(
			    "SELECT circleid FROM " +
				"circleusers AS u LEFT JOIN circles AS c " +
				    "ON cidx = c.idx " +
			    "WHERE u.uidx=" +
				"(SELECT idx FROM users WHERE uid =?)");
		    p.setString(1, uid);
		}
	    }
	    else {
		if ( regex != null ) {
		    p = c.prepareStatement(
			    "SELECT circleid FROM " +
				"circleusers AS u LEFT JOIN circles AS c " +
				    "ON cidx = c.idx " +
			    "WHERE circleid REGEXP ?");
		    p.setString(1, regex);
		}
		else {
		    p = c.prepareStatement(
			    "SELECT circleid FROM " +
				"circleusers AS u LEFT JOIN circles AS c " +
				    "ON cidx = c.idx");
		}
	    }
	    ResultSet r = p.executeQuery();
	    while (r.next())
		circles.add(new CircleDB(r.getString(1), sc));
	    sc.close();
	    return circles;
	}
	catch (SQLException e) {
	    try {
		sc.close();
	    } catch (DeterFault ignored) { }
	    for (CircleDB circ : circles)
		circ.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
}
