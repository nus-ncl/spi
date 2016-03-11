package net.deterlab.testbed.project;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.Member;
import net.deterlab.testbed.circle.CircleChallengeDB;
import net.deterlab.testbed.circle.CircleDB;
import net.deterlab.testbed.circle.CircleProfileDB;
import net.deterlab.testbed.db.PolicyObject;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.policy.CredentialStoreDB;
import net.deterlab.testbed.policy.PolicyFile;
/**
 * Interface to Project information
 * @author DETER team
 * @version 1.0
 */
public class ProjectDB extends PolicyObject {
    /** Is this project approved? */
    static private final int APPROVED = 1<<0;
    /** The name of the project */
    private String name;

    /** valid project names have no spaces or colons */
    static private Pattern validName = Pattern.compile("^[^\\s:]+$");

    /** String for the token indicating all valid permissions */
    static public String ALL_PERMS = "ALL_PERMS";

    /**
     * Confirm that this name is valid or null.
     * @param n the candidate
     * @throws DeterFault is the name is invalid
     */
    protected static void checkName(String n) throws DeterFault {
	if ( n == null) return;
	if ( !validName.matcher(n).matches())
	    throw new DeterFault(DeterFault.request,
		    "Bad project name (lexical) " + n);
    }

    /**
     * Create an empty ProjectDB
     * @throws DeterFault if there is a DB initialization error
     */
    public ProjectDB() throws DeterFault {
    this(null, null);
    }

    /**
     * Create a new DB interface to the project named n
     * @param n the name of the project
     * @throws DeterFault if the name is improperly formatted
     */
    public ProjectDB(String n) throws DeterFault { this(n, null); }
    
    /**
     * Create a new DB interface to the project named n and
     * sharing connection sc.
     * @param n the name of the project
     * @param sc the shared connection
     * @throws DeterFault if the name is improperly formatted
     */
    public ProjectDB(String n, SharedConnection sc) throws DeterFault {
    super("project", sc);;
	try {
	    checkName(n);
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
     * Return the project name
     * @return the project name
     */
    public String getName() { return name; }
    /**
     * Set the project name
     * @param n the new name
     * @throws DeterFault if the name is improperly formatted
     */
    public void setName(String n) throws DeterFault { checkName(n); name = n; }

    /**
     * Expand special tokens like ALL_PERMS and then validate the collection of
     * permissions. Return them as a set if valid, throw a fault if not. This
     * passes and expands to all valid permissions for a project AND its linked
     * circle.
     * @param perms the permissions to check
     * @return the permissions in a set
     * @throws DeterFault if the perms are invalid or there's a DB problem
     */
    public Set<String> validatePerms(Collection<String> perms)
	    throws DeterFault {
	Set<String> pSet = new HashSet<String>(perms);
	try {
	    // Expand ALL_PERMS into all valid permissions for circles or
	    // projects.  Using a Set collapses the duplicates that are present
	    // in both.
	    if (pSet.contains(ALL_PERMS)) {
		pSet.remove(ALL_PERMS);
		pSet.addAll(getValidPerms());
		CircleDB c = null;
		try {
		    c = new CircleDB(null, getSharedConnection());
		    pSet.addAll(c.getValidPerms());
		    c.close();
		}
		catch (DeterFault df) {
		    if ( c != null) c.forceClose();
		    throw df;
		}
	    }

	    if ( pSet.isEmpty()) return pSet;

	    // Count the number of distinct names that match names in pSet note
	    // that the DICTINCT collapses permissions that apply to both
	    // circles and projects into one entry, as the Set inclusion did
	    // above.  Constructing the DB statement is a two-phase operation:
	    // build a string with the right number of WHERE name=? clauses and
	    // then fill them.
	    StringBuilder sb = new StringBuilder(
		    "SELECT count(DISTINCT name) FROM permissions ");
	    for (int i = 0; i < pSet.size(); i++) {
		if ( i == 0 ) sb.append("WHERE (name =? ");
		else sb.append("OR name = ? ");
	    }
	    sb.append(") AND (valid_for = 'circle' OR valid_for='project')");
	    int idx = 1;

	    PreparedStatement p = getPreparedStatement(sb.toString());
	    for (String perm: pSet)
		p.setString(idx++, perm);
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
		    "Cannot validate circle permissions: " + e);
	}
    }

    /**
     * Remove and regenerate the credentials for this user in this project
     * @param uid the user to reset
     * @throws DeterFault if something is wrong internally
     */
    public void updateUserCredentials(String uid)
	    throws DeterFault {
	String owner = getOwner();

	if (uid == null ) return;
	if (owner == null ) return;

	Config config = new Config();
	CredentialStoreDB cdb = null;
	String files = null;

	if ( isApproved()) 
	    files = config.getProperty((owner.equals(uid)) ? 
		    "projectOwnerApprovedPolicy":"projectUserApprovedPolicy");
	else
	    files = config.getProperty((owner.equals(uid)) ? 
		    "projectOwnerPolicy":"projectUserPolicy");

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
	    if ( cdb != null ) cdb.forceClose();
	    throw df;
	}
    }

    /**
     * Remove user credentials for uid. If uid is null, remove all the
     * credentials for this project.
     * @param uid the user to clear
     * @throws DeterFault if something is wrong internally
     */
    public void removeUserCredentials(String uid)
	    throws DeterFault {
	Config config = new Config();
	String files = config.getProperty((uid != null) ?
		"projectUserRemovePolicy" : "projectRemovePolicy");
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
	    if ( cdb != null ) cdb.forceClose();
	    throw df;
	}
    }

    /**
     * Look the project up in the DB and make sure that it exists.
     * @return true if the project is valid
     * @throws DeterFault if something is wrong internally
     */
    public boolean isValid() throws DeterFault{
	if ( getName() == null ) return false;
	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT COUNT(*) FROM projects " +
			"WHERE projectid =?");
	    p.setString(1, name);
	    ResultSet r = p.executeQuery();
	    int rows = 0;
	    boolean rv = false;
	    while (r.next()) {
		if (rows++ > 0)
		    throw new DeterFault(DeterFault.internal,
			    "More than one count in isValid??");
		if ( r.getInt(1) > 0) rv = true;
	    }
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Retrieve a CircleDB pointing to the linked circle for this project.
     * @return a CircleDB pointing to the linked circle for this project.
     * @throws DeterFault if something goes wrong
     */
    public CircleDB getLinkedCircle() throws DeterFault {
	if ( getName() == null ) return null;

	try {

	    String cname = null;
	    PreparedStatement p = getPreparedStatement(
		    "SELECT circleid FROM circles " +
		    "WHERE idx=(SELECT linkedidx FROM projects " + 
			"WHERE projectid=?)");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();
	    if ( !r.next() )
		throw new DeterFault(DeterFault.internal, 
			"Project " + getName() + " has no linked circle");
	    cname = r.getString(1);
	    if ( r.next() )
		throw new DeterFault(DeterFault.internal, 
			"Project " + getName() + " has many linked circles");

	    // If this CircleDB throws an exception, its connection will be
	    // closed so we don't catch the exception in this method.
	    if (cname != null )
		return new CircleDB(cname, getSharedConnection());
	    else
		return null;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Look the project up in the DB and make sure that it exists and has the
     * live flag set.
     * @param flags the flags to check
     * @param mask the relevant bits in the flags parameter (only set bits in
     * mask are checked)
     * @return true if the given flags have the same state
     * @throws DeterFault if something is wrong internally
     */
    public boolean checkFlags(int flags, int mask) throws DeterFault {

	if ( getName() == null ) return false;

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT idx FROM projects " + 
			"WHERE projectid =? AND (flags & ?) = ?");
	    p.setString(1, name);
	    p.setInt(2, mask);
	    p.setInt(3, flags & mask);
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
     * Set the project's flags to the given value, constrained by mask.
     * @param flags the flags to set (or unset)
     * @param mask the relevant bits in the flags parameter (only set bits in
     * mask are changed)
     * @throws DeterFault if something is wrong internally
     */
    public void setFlags(int flags, int mask) throws DeterFault {

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.request, "Project has no name set");

	try {
	    PreparedStatement p = getPreparedStatement(
		    "UPDATE projects SET flags=((flags & ?) | ?) " + 
			"WHERE projectid =?");
	    p.setInt(1, ~mask);
	    p.setInt(2, (flags & mask));
	    p.setString(3, name);
	    p.executeUpdate();
	    for (Member m: getMembers())
		updateUserCredentials(m.getUid());
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }


    /**
     * Look the project up in the DB and make sure that it exists and has the
     * live flag set.
     * @return true if the project is live
     * @throws DeterFault if something is wrong internally
     */
    public boolean isApproved() throws DeterFault { 
	return checkFlags(APPROVED, APPROVED);
    }

    /**
     * Set the project's liveness flag to the given value.
     * @param v the new liveness value
     * @throws DeterFault if something is wrong internally
     */
    public void setApproval(boolean v) throws DeterFault { 
	if ( isApproved() ) return;
	setFlags(v ? APPROVED : 0, APPROVED);
	if ( v) {
	    String n = getName();
	    String cid = n + ":" + n;
	    String owner = getOwner();

	    try {
		addLinkedCircle(cid, owner);
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal,
			"Error linking circle: " + e);
	    }
	}
    }

    /**
     * Return true if uid is a member of this project
     * @param uid the user ID to test for membership
     * @return true if uid is a member of this project
     * @throws DeterFault if there is a problem
     */
    public boolean isMember(String uid) throws DeterFault {
	if ( getName() == null || uid == null ) return false;

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT COUNT(*) FROM projectusers " +
			"WHERE uidx=(SELECT idx FROM users WHERE uid=?) "+ 
			"AND pidx=(SELECT idx FROM projects " +
			    "WHERE projectid=?)");
	    p.setString(1, uid);
	    p.setString(2, getName());
	    ResultSet r = p.executeQuery();
	    int rows = 0;
	    boolean rv = false;
	    while (r.next()) {
		if (rows++ > 0)
		    throw new DeterFault(DeterFault.internal,
			    "More than one count in isMember??");
		if ( r.getInt(1) > 0) rv = true;
	    }
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }


    /**
     * Return the names of members of the project with the given permission
     * @param perm the permission values
     * @return the members of the project
     * @throws DeterFault if something is wrong internally
     */
    public List<String> getUidsByPerm(String perm) throws DeterFault {

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "getMembers failed. Project does not have a name");

	try {
	    List<String> rv = new ArrayList<String>();
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM projectperms LEFT JOIN users " +
			"ON  uidx = idx " +
			"WHERE pidx=" +
			    "(SELECT idx FROM projects WHERE projectid=?) " +
			    "AND permidx=(SELECT idx FROM permissions " +
				"WHERE name=? AND valid_for='project')");
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
     * Return the members of the project
     * @return the members of the project
     * @throws DeterFault if something is wrong internally
     */
    public List<Member> getMembers() throws DeterFault {

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "getMembers failed. Project does not have a name");

	try {
	    Map<String, Set<String>> users = new HashMap<String, Set<String>>();
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM projectusers LEFT JOIN users AS u " +
			"ON uidx = u.idx " +
		    "WHERE pidx=(SELECT idx FROM projects WHERE projectid=?)");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();
	    while (r.next())
		users.put(r.getString(1), new HashSet<String>());

	    p = getPreparedStatement(
		    "SELECT uid, name FROM projectperms LEFT JOIN users AS u "+
			"ON  uidx = u.idx " +
		    "LEFT JOIN permissions AS p " +
			"ON permidx = p.idx " +
		    "WHERE pidx=(SELECT idx FROM projects WHERE projectid=?)");
	    p.setString(1, getName());
	    r = p.executeQuery();

	    while (r.next()) {
		String uid = r.getString(1);

		if ( !users.containsKey(uid))
		    throw new DeterFault(DeterFault.internal,
			    "projectperms and projectusers are out of sync");
		users.get(uid).add(r.getString(2));
	    }
	    List<Member> m = new ArrayList<Member>();
	    for (String uid: users.keySet())
		m.add(new Member(uid, users.get(uid).toArray(new String[0])));
	    return m;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }


    /**
     * Return the permissions of this user in the project
     * @return the permissions of this user in the project
     * @param uid the user to get
     * @return the permissions
     * @throws DeterFault if something is wrong internally
     */
    public Set<String> getPerms(String uid) throws DeterFault {

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "getPerms failed. Project does not have a name");

	if ( uid == null )
	    throw new DeterFault(DeterFault.request,
		    "getPerms failed. no uid");

	if (!isMember(uid))
	    throw new DeterFault(DeterFault.request,
		    "getPerms failed. Cannot find uid " + uid);
	try {
	    Set<String> rv = new HashSet<String>();
	    PreparedStatement p = getPreparedStatement(
		    "SELECT name FROM projectperms LEFT JOIN users AS u " +
			"ON  uidx = u.idx " +
		    "LEFT JOIN permissions AS p "+
			"ON permidx = p.idx " +
		    "WHERE pidx=" +
			"(SELECT idx FROM projects WHERE projectid=?) "+
		    "AND uidx=" +
			"(SELECT idx FROM users WHERE uid=?)");
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
     * Get the project's owner
     * @return the project's owner
     * @throws DeterFault if there is an error
     */
    public String getOwner() throws DeterFault {

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "getOwner failed. Project does not have a name");

	try {
	    String rv = null;
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM projects LEFT JOIN users AS u " +
			"ON u.idx = owneridx " +
		    "WHERE projectid=?");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();
	    if (!r.next() ) 
		throw new DeterFault(DeterFault.internal,
			"No owner for project??");
	    rv = r.getString(1);
	    if (r.next() ) 
		throw new DeterFault(DeterFault.internal,
			"More than one owner? for project??");
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Set the project's owner to the new uid.  The uid must be a member of the
     * project
     * @param o the new onwer's uid
     * @throws DeterFault if the uid is not a member or there is a DB problem
     */
    public void setOwner(String o) throws DeterFault {
	CircleDB linked = null;

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal,
		    "setOwner failed. Project does not have a name");
	if ( o  == null ) 
	    throw new DeterFault(DeterFault.request,
		    "setOwner failed. No owner provided");

	if ( !isMember(o) )
		throw new DeterFault(DeterFault.request,
			"setOwner failed. New owner is not a member");

	try {
	    String oldOwner = getOwner();
	    PreparedStatement p = getPreparedStatement(
		    "UPDATE projects " +
		    "SET owneridx = (SELECT idx FROM users WHERE uid = ?) " +
		    "WHERE projectid=?");
	    p.setString(1, o);
	    p.setString(2, getName());
	    p.executeUpdate();

	    // Now change linked circle
	    linked = getLinkedCircle();
	    if ( linked == null ) 
		throw new DeterFault(DeterFault.internal, 
			"Could not find linked circle for " + getName());
	    linked.setOwner(o);
	    linked.close();
	    linked = null;
	    updateUserCredentials(oldOwner);
	    updateUserCredentials(o);
	}
	catch (SQLException e) {
	    //if ( linked != null ) linked.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	catch (DeterFault df) {
	    if ( linked != null ) linked.forceClose();
	    throw df;
	}
    }

    /**
     * Add the given member to this project with the given permissions.  If the
     * member is already in the group, throw a DeterFault.  This adds the user
     * to the linked circle with the same permissions.
     * @param uid user to add
     * @param perms permissions
     * @throws DeterFault on error
     */
    public void addUser(String uid, Set<String> perms) throws DeterFault {
	CircleDB linked = null;

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "addUser failed. Project does not have a name");
	if ( uid == null )
	    throw new DeterFault(DeterFault.internal, 
		    "addUser failed. No user given");
	if (perms == null)
	    throw new DeterFault(DeterFault.internal, 
		    "addUser failed. Null permissions given");
	if ( isMember(uid))
	    throw new DeterFault(DeterFault.request, 
		    "User already in project");
	// Try an update first, and if that fails, an insert
	try {
	    PreparedStatement p = getPreparedStatement(
		    "INSERT INTO projectusers (pidx, uidx) " +
			"VALUES (" +
			    "(SELECT idx FROM projects WHERE projectid=?), "+
			    "(SELECT idx FROM users WHERE uid=?))");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    p.executeUpdate();
	    if (!perms.isEmpty()) {
		Set<String> valid = getValidPerms();
		p = getPreparedStatement(
			"INSERT INTO projectperms (pidx, uidx, permidx) "+
			"VALUES ((SELECT idx FROM projects WHERE projectid=?), "+
			"(SELECT idx FROM users WHERE uid=?), "+
			"(SELECT idx FROM permissions WHERE name=? " +
			    "AND valid_for='project'))");
		p.setString(1, getName());
		p.setString(2, uid);
		for (String perm : perms) {
		    if (!valid.contains(perm)) continue;
		    p.setString(3, perm);
		    p.executeUpdate();
		}
	    }

	    // Now add to linked circle
	    linked = getLinkedCircle();
	    if ( linked == null ) 
		throw new DeterFault(DeterFault.internal, 
			"Could not find linked circle for " + getName());
	    linked.addUser(uid, perms);
	    linked.close();
	    linked = null;
	    updateUserCredentials(uid);
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    // Constraint exceptions happen because the subqueries fail to find
	    // indices for a user or project.  Usually this is a bad username.
	    //if (linked != null ) linked.forceClose();
	    throw new DeterFault(DeterFault.request, "Bad uid (or projectid)");
	}
	catch (SQLException e) {
	    //if (linked != null ) linked.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	catch (DeterFault df) {
	    if (linked != null ) linked.forceClose();
	    throw df;
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
	CircleDB linked = null;
	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal,
		    "setPermissions failed. Project does not have a name");
	if ( uid == null )
	    throw new DeterFault(DeterFault.request,
		    "setPermissions failed. No user given");
	if (perms == null)
	    throw new DeterFault(DeterFault.request,
		    "setPermissions failed. Null permissions given");
	if ( !isMember(uid))
	    throw new DeterFault(DeterFault.request,
		    "setPermissions failed User not in project");
	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM projectperms " +
		    "WHERE pidx=(SELECT idx FROM projects WHERE projectid=?) "+
		    "AND uidx=(SELECT idx FROM users WHERE uid=?)");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    p.executeUpdate();
	    if (!perms.isEmpty()) {
		Set<String> valid = getValidPerms();
		p = getPreparedStatement(
			"INSERT INTO projectperms (pidx, uidx, permidx) "+
			"VALUES ((SELECT idx FROM projects WHERE projectid=?), "+
			"(SELECT idx FROM users WHERE uid=?), "+
			"(SELECT idx FROM permissions WHERE name=? " +
			    "AND valid_for='project'))");
		p.setString(1, getName());
		p.setString(2, uid);
		for (String perm : perms) {
		    if (!valid.contains(perm)) continue;
		    p.setString(3, perm);
		    p.executeUpdate();
		}
	    }

	    // Now modify in linked circle
	    linked = getLinkedCircle();
	    if ( linked == null ) 
		throw new DeterFault(DeterFault.internal, 
			"Could not find linked circle for " + getName());
	    linked.setPermissions(uid, perms);
	    linked.close();
	    linked = null;
	    updateUserCredentials(uid);
	}
	catch (SQLException e) {
	    //if (linked != null ) linked.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	catch (DeterFault df) {
	    if (linked != null ) linked.forceClose();
	    throw df;
	}
    }

    /**
     * Remove the given user from this project, if they are in it.
     * @param uid the uid to remove
     * @throws DeterFault if there is a problem
     */
    public void removeUser(String uid) throws DeterFault {
	CircleDB linked = null;

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "removeUser failed. Project does not have a name");
	if ( uid == null)
	    throw new DeterFault(DeterFault.request,
		    "removeUser failed. No uid given");
	try {
	    String owner = getOwner();

	    if ( uid.equals(owner))
		throw new DeterFault(DeterFault.request, "Cannot remove owner");

	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM projectusers WHERE pidx=" +
			"(SELECT idx FROM projects WHERE projectid = ?) " +
			"AND uidx=(SELECT idx FROM users WHERE uid=?)");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    if ( p.executeUpdate() < 1) 
		throw new DeterFault(DeterFault.request,
			"Could not remove user - not present");

	    p = getPreparedStatement(
		    "DELETE FROM projectperms WHERE pidx=" +
			"(SELECT idx FROM projects WHERE projectid = ?) " +
			"AND uidx=(SELECT idx FROM users WHERE uid=?)");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    p.executeUpdate();

	    // Now remove from linked circle
	    linked = getLinkedCircle();
	    if ( linked == null ) 
		throw new DeterFault(DeterFault.internal, 
			"Could not find linked circle for " + getName());
	    linked.removeUser(uid);
	    linked.close();
	    linked = null;
	    removeUserCredentials(uid);
	}
	catch (SQLException e) {
	    //if (linked != null) linked.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	catch (DeterFault df) {
	    if (linked != null) linked.forceClose();
	    throw df;
	}
    }

    /**
     * Create a circle with the given identifier, a single member (owner) with
     * all permissions and link it to this project.
     * @param circleid the ID to create
     * @param owner the circle owner and only member
     * @throws DeterFault on DETER errors
     * @throws SQLException on direct DB errors - making the link
     */
    private void addLinkedCircle(String circleid, String owner)
	throws DeterFault, SQLException {
	PreparedStatement p = null;
	CircleDB linked = new CircleDB(circleid, getSharedConnection());
	linked.create(owner);
	linked.close();
	linked = null;

	// Make a dummy profile and attach it
	CircleProfileDB profile = null;
	try {
	    profile = new CircleProfileDB(getSharedConnection());
	    profile.loadAll();

	    Collection<String> req = CircleProfileDB.fillDefaultProfile(
		    profile, "Circle linked to project " + getName());
	    profile.setId(circleid);
	    profile.save(req);
	    profile.close();
	}
	catch (DeterFault df) {
	    if (profile != null ) profile.forceClose();
	    throw df;
	}

	// Link it up.
	p = getPreparedStatement(
		"UPDATE projects " +
		"SET linkedidx=" +
		    "(SELECT idx FROM circles WHERE circleid=?) " +
		"WHERE projectid=?");
	p.setString(1, circleid);
	p.setString(2, getName());
	p.executeUpdate();
	updateUserCredentials(owner);
    }

    /**
     * Create the project.  The owner passed in is made a member
     * with all permissions.  A linked circle is created, owned by the same
     * user and containing that user.
     * @param owner the owner of the new project
     * @throws DeterFault if there is a problem
     */
    public void create(String owner) throws DeterFault {
	PreparedStatement p = null;
	//CircleDB linked = null;

	try {
	    if ( getName() == null)
		throw new DeterFault(DeterFault.internal, 
			"create failed. Project does not have a name");

	    //String circleid = getName() + ":" + getName();
	    try {
		p = getPreparedStatement(
			"INSERT INTO scopenames (name, type) "+
			    "VALUES (?, 'project')");
		p.setString(1, getName());
		p.executeUpdate();
		p.close();
	    }
	    catch (SQLIntegrityConstraintViolationException e) {
		throw new DeterFault(DeterFault.request,
			"Project name conflict on " + getName());
	    }

	    try {
		p = getPreparedStatement(
			"INSERT INTO rawprojects (nameidx, owneridx) " +
			"VALUES ((SELECT idx FROM scopenames WHERE name=?), " +
			    "(SELECT idx FROM users WHERE uid=?))");
		p.setString(1, getName());
		p.setString(2, owner);
		p.executeUpdate();
		p.close();
	    }
	    catch (SQLIntegrityConstraintViolationException e) {
		// Bad owner - remove the scopename
		try {
		    p = getPreparedStatement(
			    "DELETE FROM  scopenames WHERE name=?");
		    p.setString(1, getName());
		    p.execute();
		    p.close();
		}
		catch (SQLException ignored) { }
		throw new DeterFault(DeterFault.request,
			"No such owner " + owner);
	    }

	    p = getPreparedStatement(
		    "INSERT INTO projectusers (pidx, uidx) " +
			"VALUES ((SELECT idx FROM projects WHERE projectid=?),"+
			"(SELECT idx FROM users WHERE uid=?))");
	    p.setString(1, getName());
	    p.setString(2, owner);
	    p.executeUpdate();
	    // The INSERT .. SELECT adds all valid permissions in one DB call
	    p = getPreparedStatement(
		    "INSERT INTO projectperms (pidx, uidx, permidx) " +
			"SELECT (SELECT idx FROM projects WHERE projectid=?), "+
			"(SELECT idx FROM users WHERE uid=?), idx " +
			    "FROM permissions WHERE valid_for='project'");
	    p.setString(1, getName());
	    p.setString(2, owner);
	    p.executeUpdate();

	    // addLinkedCircle(circleid, owner);

	    updatePolicyCredentials();
	}
	catch (SQLException e) {
	    //if ( linked != null ) linked.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	catch (DeterFault df) {
	    //if ( linked != null ) linked.forceClose();
	    throw df;
	}finally {
    	try{
    		p.close();
    	} catch (SQLException e) {
    		/* exception ignored */
    		/* Don't know a better approach */
    	}
    }
    }
    /**
     * Completely remove this project.  The project's profile must be empty
     * when this is called - it must have had removeAll() called.  All
     * project challenges must be  removed as well.  However, this routine
     * removes the profile and challenges for the linked circle.
     * @throws DeterFault if something goes wrong
     */
    public void remove() throws DeterFault {
	CircleDB circle = null;
	CircleProfileDB profile = null;
	CircleChallengeDB challenges = null;

	if ( getName() == null)
	    throw new DeterFault(DeterFault.internal, 
		    "remove failed. Project does not have a name");
	try {
	    // Get the linked circle here, because the code below disconnects
	    // it.
	    circle = getLinkedCircle();
	    String circleid = circle.getName();

	    profile = new CircleProfileDB(circleid, getSharedConnection());
	    challenges = new CircleChallengeDB(getSharedConnection());

	    challenges.clearChallenges(circleid);
	    challenges.close();
	    profile.loadAll();
	    profile.removeAll();
	    profile.close();
	    profile = null;
	    // Remove the users
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM projectusers " +
		    "WHERE pidx=(SELECT idx FROM projects WHERE projectid=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    p = getPreparedStatement(
		    "DELETE FROM projectperms " +
		    "WHERE pidx=(SELECT idx FROM projects WHERE projectid=?)");
	    p.setString(1, getName());
	    p.executeUpdate();
	    // Credentials
	    removeUserCredentials(null);
	    // and then the project
	    p = getPreparedStatement("DELETE FROM rawprojects " +
		    "WHERE nameidx=(SELECT idx FROM scopenames WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();
	    // Remove the circle after the project because the linking between
	    // project and circle must be severed (by the SQL above) first.
	    circle.remove();
	    circle.close();
	    circle = null;
	    // Remove the scopename
	    p = getPreparedStatement("DELETE FROM scopenames WHERE name=?");
	    p.setString(1, getName());
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    if ( circle != null ) circle.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	catch (DeterFault df) {
	    if ( circle != null ) circle.forceClose();
	    if ( profile != null ) profile.forceClose();
	    if ( challenges != null ) challenges.forceClose();
	    throw df;
	}
    }

    /**
     * Get a list of all Projects that the uid is a member of.  Regex and uid
     * both constrain the search. The shared connection is used if present.
     * @param uid the uid to look for if any
     * @param regex to search with
     * @param sc the shared connection to use
     * @return a list of ProjectDBs that match
     * @throws DeterFault on errors
     */
    static public List<ProjectDB> getProjects(String uid, String regex,
	    SharedConnection sc) throws DeterFault {
	Connection c = null;
	PreparedStatement p = null;
	List<ProjectDB> projects = new ArrayList<ProjectDB>();
	if ( sc == null ) sc = new SharedConnection();

	try {
	    sc.open();
	    c = sc.getConnection();
	    if ( uid != null ) {
		if ( regex != null ) {
		    p = c.prepareStatement(
			    "SELECT projectid FROM " +
				"projectusers AS u LEFT JOIN projects AS p " +
				    "ON pidx = p.idx " +
			    "WHERE u.uidx=(SELECT idx FROM users " +
				"WHERE uid =?) AND projectid REGEXP ?");
		    p.setString(1, uid);
		    p.setString(2, regex);
		}
		else {
		    p = c.prepareStatement(
			    "SELECT projectid FROM " +
				"projectusers AS u LEFT JOIN projects AS p " +
				    "ON pidx = p.idx " +
			    "WHERE u.uidx=(SELECT idx FROM users WHERE uid =?)");
		    p.setString(1, uid);
		}
	    }
	    else {
		if ( regex != null ) {
		    p = c.prepareStatement(
			    "SELECT projectid FROM " +
				"projectusers AS u LEFT JOIN projects AS p " +
				    "ON pidx = p.idx " +
			    "WHERE AND projectid REGEXP ?");
		    p.setString(1, regex);
		}
		else {
		    p = c.prepareStatement(
			    "SELECT projectid FROM " +
				"projectusers AS u LEFT JOIN projects AS p " +
				    "ON pidx = p.idx");
		}
	    }
	    ResultSet r = p.executeQuery();

	    // Build the lists of projects as ProjectDBs that share a
	    // connection.
	    while (r.next())
		projects.add(new ProjectDB(r.getString(1), sc));
	    sc.close();
	    return projects;
	}
	catch (SQLException e) {
	    try {
		sc.close();
	    } catch (DeterFault ignored) {}
	    for (ProjectDB proj: projects)
		proj.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
}
