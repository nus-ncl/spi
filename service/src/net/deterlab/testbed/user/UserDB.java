package net.deterlab.testbed.user;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.circle.CircleChallengeDB;
import net.deterlab.testbed.circle.CircleDB;
import net.deterlab.testbed.circle.CircleProfileDB;
import net.deterlab.testbed.db.DBObject;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.policy.CredentialStoreDB;
import net.deterlab.testbed.policy.PolicyFile;
import net.deterlab.testbed.project.ProjectChallengeDB;

/**
 * Basic user data read and written to the deter database
 * @author DETER team
 * @version 1.1
 */
public class UserDB extends DBObject {
    /** The user identifier */
    protected String uid;
    /** The user's password hash */
    protected PasswordHash hash;
    /** Password Expiration */
    protected Date expires;


    // This is messy, but the PasswordHash subclasses do not add themselves to
    // the PasswordHash abstract class until the first time they are
    // referenced.  This static makes sure that when the first UserDB is
    // instantiated it instantiates all the PasswordHash subclasses that future
    // Users may need. As other hashes are added, they need to be added here as
    // well.
    private static PasswordHash[] touchEm = new PasswordHash[] { 
	new CryptPasswordHash(null),
    };

    /**
     * A collection of permissions valid for the per-user circle.  A user can
     * do anything to their circle but add or delete other users.
     */
    private static Set<String> perUserCirclePerms;
    static {
	perUserCirclePerms = new HashSet<String>();
	CircleDB c = null;
	try {
	    c = new CircleDB();
	    for (String p: c.getValidPerms())
		if ( p != null && !p.equals("ADD_USER") &&
			!p.equals("REMOVE_USER"))
		    perUserCirclePerms.add(p);
	    c.close();
	}
	catch (DeterFault df) {
	    if ( c != null ) c.forceClose();
	}
    }


    /**
     * Create a user.
     * @param u the uid
     * @throws DeterFault on an initialization error
     */
    public UserDB(String u) throws DeterFault {
	super();
	uid = u;
	hash = null;
	expires = null;
    }

    /**
     * Create a user with a shared connection
     * @param u the uid
     * @param sc a shared DB connection
     * @throws DeterFault on an initialization error
     */
    public UserDB(String u, SharedConnection sc) throws DeterFault {
	super(sc);
	uid = u;
	hash = null;
	expires = null;
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
     * Return the password hash
     * @return the password hash
     */
    public PasswordHash getPasswordHash() { return hash; }
    /**
     * Set the password hash
     * @param h the new password hash
     */
    public void setPasswordHash(PasswordHash h) { hash =h; }

    /**
     * Return the password expiration
     * @return the password expiration
     */
    public Date getExpiration() { return expires; }

    /**
     * Set the password expiration
     * @param d the new expiration
     */
    public void setExpiration(Date d) { expires = d; }

    /**
     * Validate the password.  Make sure that a password is assigned and not
     * expired.
     * @throws DeterFault if the password is not valid
     */
    public void checkPassword() throws DeterFault {
	Date exp = getExpiration();

	if ( getPasswordHash() == null )
	    throw new DeterFault(DeterFault.password,
		    "No password assigned yet");
	if (exp == null )
	    throw new DeterFault(DeterFault.password,
		    "No expiration assigned");
	if ( exp.before(new Date()))
	    throw new DeterFault(DeterFault.password,
		    "Password expired on " + exp);
    }

    /**
     * Look the user up in the DB and make sure that it exists.
     * @return true if the project is valid
     * @throws DeterFault if something is wrong internally
     */
    public boolean isValid() throws DeterFault {
	boolean rv = false;

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT idx FROM users WHERE uid=?");
	    p.setString(1, getUid());
	    ResultSet r = p.executeQuery();

	    // Is there only one value in the result set?
	    rv = r.next() && !r.next();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error reading user: " + e);
	}
	return rv;
    }


    /**
     * Load the user from the Database.  If the uid has not been set, silently
     * return.
     * @throws DeterFault if there is difficulty reading the user
     */
    public void load() throws DeterFault {
	boolean loadedOne = false;

	if ( getUid() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "Attempt to load user without uid");
	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT password, hashtype, passwordexpires FROM users " +
		    "WHERE uid = ?");
	    p.setString(1, getUid());
	    ResultSet r = p.executeQuery();
	    while (r.next()) {
		if (loadedOne) 
		    throw new DeterFault(DeterFault.internal,
			    "Multiple users with uid " + getUid());
		else
		    loadedOne = true;
		setPasswordHash(PasswordHash.getInstance(
			    r.getString("hashtype"),
			    r.getString("password"))
			);
		setExpiration(r.getTimestamp("passwordexpires"));
	    }
	    if ( !loadedOne ) 
		throw new DeterFault(DeterFault.request,
			"No such user " + getUid());
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error reading user: " + e);
	}
    }

    /**
     * Save or update this user into the DB.
     * @throws DeterFault if there is a database error, or the user is
     * incompletely specified or does not exits
     */
    public void save() throws DeterFault {
	String uid = getUid();
	PasswordHash ph = getPasswordHash();

	if ( uid == null || ph == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "Attempt to save incomplete user");
	try {
	    PreparedStatement u = getPreparedStatement(
		    "UPDATE users SET  password=?, hashtype=?, " +
			"passwordexpires=? WHERE uid=?");
	    u.setString(1, ph.getValue());
	    u.setString(2, ph.getType());
	    u.setTimestamp(3, new Timestamp(getExpiration().getTime()));
	    u.setString(4, uid);

	    int rows = u.executeUpdate();
	    if ( rows == 1 ) return;
	    else if (rows == 0 )
		throw new DeterFault(DeterFault.request,
			"No such user " + uid);
	    else
		throw new DeterFault(DeterFault.request,
			"More than one user " + uid);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error reading user: " + e);
	}
    }

    /**
     * Insert the user policy.
     * @throws DeterFault if something is wrong internally
     */
    public void updateUserPolicy() throws DeterFault {
	Config config = new Config();
	String files = config.getProperty("userPolicy");
	CredentialStoreDB cdb = null;

	try {
	    cdb = new CredentialStoreDB(getSharedConnection());
	    for ( String fn : files.split(",")) {
		PolicyFile userPolicy = new PolicyFile(new File(fn));
		userPolicy.updateCredentials(cdb, null, uid, null, null);
	    }
	    cdb.close();
	}
	catch (DeterFault df) {
	    if ( cdb != null ) cdb.forceClose();
	    throw df;
	}
    }

    /**
     * Create this user in the DB.  Also creates a singleton circle with this
     * user as the only member and initializes the user's credentials.
     * @throws DeterFault if there is a database error, or the user is
     * incompletely specified.
     */
    public void create() throws DeterFault {
	String uid = getUid();
	PasswordHash ph = getPasswordHash();
	CircleDB circle = null;

	if ( uid == null || ph == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "Attempt to save incomplete user");
	try {
	    Config config = new Config();
	    String worldName = config.getWorldCircle();
	    PreparedStatement p = null;
	    try {
		p = getPreparedStatement(
			"INSERT INTO scopenames (name, type) " +
			    "VALUES (?, 'user')");
		p.setString(1, uid);
		p.executeUpdate();
	    }
	    catch (SQLIntegrityConstraintViolationException e) {
		throw new DeterFault(DeterFault.request, "User exists: " + uid);
	    }
	    p = getPreparedStatement(
		    "INSERT INTO rawusers (password, hashtype, " +
			"passwordexpires, nameidx) " +
			"VALUES (?, ?, ?, " +
			    "(SELECT idx FROM scopenames WHERE name=?))");

	    p.setString(1, ph.getValue());
	    p.setString(2, ph.getType());
	    p.setTimestamp(3, new Timestamp(getExpiration().getTime()));
	    p.setString(4, uid);
	    p.executeUpdate();
	    updateUserPolicy();

	    String circleid = uid + ":" + uid;
	    circle = new CircleDB(circleid, getSharedConnection());
	    circle.create(uid);
	    // Let this user do anything except add or delete themselves from
	    // their circle.
	    circle.setPermissions(uid, perUserCirclePerms);
	    circle.close();
	    if ( worldName != null ) {
		circle = new CircleDB(worldName, getSharedConnection());
		if ( circle.isValid())
		    circle.addUser(uid, new TreeSet<String>());
		circle.close();
	    }
	    circle = null;
	    // Make a dummy profile and attach it
	    CircleProfileDB profile = null;
	    try {
		profile = new CircleProfileDB(getSharedConnection());
		profile.loadAll();

		Collection<String> req = CircleProfileDB.fillDefaultProfile(
			profile,
			"Circle linked to user " + uid);
		profile.setId(circleid);
		profile.save(req);
		profile.close();
	    }
	    catch (DeterFault df) {
		if (profile != null) profile.forceClose();
		throw df;
	    }
	}
	catch (SQLException e) {
	    //if ( circle != null ) circle.forceClose();
	    throw new DeterFault(DeterFault.internal, 
		    "Database error reading user: " + e);
	}
	catch (DeterFault df) {
	    if ( circle != null ) circle.forceClose();
	    throw df;
	}
    }
    /**
     * Remove this user from the DB.  This can fail if the profile and other
     * dependencies have not been removed first.  Removes the single user
     * circle and notification delivery links.
     * @throws DeterFault if there is a database error, or the user is
     * not in the DB
     */
    public void remove() throws DeterFault {
	CircleDB circle = null;
	CircleProfileDB profile = null;
	CircleChallengeDB challenges = null;
	ProjectChallengeDB pchallenges = null;
	CredentialStoreDB cdb = null;
	NotificationStoreDB not = null;

	if ( uid == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "Attempt to remove user w/o uid");
	try {
	    String circleid = uid + ":" + uid;
	    circle = new CircleDB(circleid, getSharedConnection());
	    profile = new CircleProfileDB(circleid, getSharedConnection());
	    challenges = new CircleChallengeDB(getSharedConnection());
	    pchallenges = new ProjectChallengeDB(getSharedConnection());
	    not = new NotificationStoreDB(getSharedConnection());
	    cdb = new CredentialStoreDB(getSharedConnection());
	    Config config = new Config();

	    // remove any credentials for this user
	    String files = config.getProperty("userRemovePolicy");

	    for ( String fn : files.split(",")) {
		PolicyFile userPolicy = new PolicyFile(new File(fn));
		userPolicy.removeCredentials(cdb, null, uid);
	    }
	    // log any connections out
	    cdb.unbindUid(uid);
	    cdb.close();
	    challenges.clearChallenges(uid, null);
	    challenges.close();
	    pchallenges.clearChallenges(uid, null);
	    pchallenges.close();
	    not.undeliverAll(uid);
	    not.close();
	    profile.loadAll();
	    profile.removeAll();
	    profile.close();
	    circle.remove();
	    circle.close();
	    circle = null;

	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM rawusers WHERE nameidx=" +
			"(SELECT idx FROM scopenames WHERE name= ?)");
	    p.setString(1,uid);
	    p.executeUpdate();
	    p = getPreparedStatement(
		    "DELETE FROM scopenames WHERE name=?");
	    p.setString(1,uid);
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    if ( not != null ) not.forceClose();
	    if ( cdb != null) cdb.forceClose();
	    //if ( circle != null) circle.forceClose();
	    if ( profile != null) profile.forceClose();
	    if ( challenges != null) challenges.forceClose();
	    if ( pchallenges != null) pchallenges.forceClose();
	    throw new DeterFault(DeterFault.internal, 
		    "Database error removing user: " + e);
	}
	catch (DeterFault df) {
	    if ( not != null ) not.forceClose();
	    if ( circle != null) circle.forceClose();
	    if ( profile != null) profile.forceClose();
	    if ( challenges != null) challenges.forceClose();
	    if ( pchallenges != null) pchallenges.forceClose();
	    throw df;
	}
    }
    /**
     * Get a list of all users as UserDB objects that all share a database
     * connection.  If regex is given, only uids that match it
     * will be returned.
     * @param regex return user IDs matching this regualr expression
     * @param conn a shared database connection (may be null)
     * @return a list of UserDBs that match
     * @throws DeterFault on errors
     */
    static public List<UserDB> getUsers(String regex, SharedConnection conn)
	    throws DeterFault {
	PreparedStatement p = null;
	List<UserDB> users = new ArrayList<UserDB>();

	if (conn == null) conn = new SharedConnection();
	conn.open();

	try {
	    Connection c = conn.getConnection();
	    if ( regex != null ) {
		p = c.prepareStatement(
			"SELECT uid FROM users WHERE uid REGEXP ?");
		p.setString(1, regex);
	    }
	    else {
		p = c.prepareStatement("SELECT uid FROM users");
	    }
	    ResultSet r = p.executeQuery();

	    while (r.next())
		users.add(new UserDB(r.getString(1), conn));

	    conn.close();
	    return users;
	}
	catch (SQLException e) {
	    try {
		conn.close();
	    }
	    catch (DeterFault ignored) { }

	    for (UserDB u: users )
		u.forceClose();
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Set the uid to a unique user/project id derived from base and return the
     * value picked.  The code just adds integers until there is no conflicts.
     * @param base the string to start from
     * @return the unique ID
     * @throws DeterFault if something very odd goes wrong.
     */
    public String getUniqueId(String base) throws DeterFault {
	Set<String> used = new TreeSet<String>();
	if ( base == null ) base = "user";
	String regex = base + ".*";
	String fmt = base + "%d";
	String rv = base;

	try {
	    // Get the used names that start with base
	    PreparedStatement p = getPreparedStatement(
		    "SELECT name FROM scopenames WHERE name REGEXP ?");
	    p.setString(1, regex);
	    ResultSet r = p.executeQuery();

	    while (r.next())
		used.add(r.getString(1));
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	// Keep trying new integers until there's an open space. In a world of
	// collisions, a random pick may be better.
	for (int i = 0; used.contains(rv); i++)
	    rv = String.format(fmt, i);
	setUid(rv);
	return rv;
    }
}
