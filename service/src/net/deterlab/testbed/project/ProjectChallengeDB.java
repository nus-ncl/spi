package net.deterlab.testbed.project;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.db.DBObject;
import net.deterlab.testbed.db.SharedConnection;

/**
 * This manages the challenges issued to users.  A challenge is issued whenever
 * one user adds another to a project using the api.Projects.addUser() call -
 * which is what most users use.  The user being added needs to confirm because
 * some amount of visibility is being granted to their data via this exchange.
 * @author DETER team
 * @version 1.0
 */
public class ProjectChallengeDB extends DBObject {
    /** Random number generator to pick IDs */
    static private Random rng = new Random();

    static public class Contents {
	/** The uid to add */
	private String uid;
	/** The project to add it to */
	private String projectid;
	/** The permissions for the user */
	private Set<String> perms;

	/**
	 * Create a new contents object
	 * @param u the uid
	 * @param pi the projectid
	 * @param p the premissions
	 */
	public Contents(String u, String pi, Collection<String> p) {
	    uid = u;
	    projectid = pi;
	    perms = new HashSet<String>(p);
	}
	/**
	 * Return the uid
	 * @return the uid
	 */
	public String getUid() { return uid; }
	/**
	 * Return the projectid
	 * @return the projectid
	 */
	public String getProjectId() { return projectid; }
	/**
	 * Return the permissions
	 * @return the permissions
	 */
	public Set<String> getPermissions() { return perms; }
    }


    /**
     * Create a projectchallange
     * @throws DeterFault if the testbed is misconfigured
     */
    public ProjectChallengeDB() throws DeterFault {
	super();
    }

    /**
     * Create a projectchallange that shares a DB connection
     * @param sc the shared connection
     * @throws DeterFault if the testbed is misconfigured
     */
    public ProjectChallengeDB(SharedConnection sc) throws DeterFault {
	super(sc);
    } 

    /**
     * Synch the index pool with the challenge DB, e.g. after a deletion
     * @throws DeterFault on a poorly configured DB
     */
    protected void cleanIndexPool() throws DeterFault {
	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM projectchallengeidx " +
		    "WHERE idx NOT IN (SELECT idx FROM projectchallenge)");
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Remove expried challenges from the DB, called by each public routine
     * @throws DeterFault on a poorly configured DB
     */
    protected void expireChallenges() throws DeterFault {
	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM projectchallenge WHERE expires < NOW()");
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Remove all challenges that would allow uid to be added to projectid.
     * Leaving projectid null removes all uid challenges across any project,
     * leaving uid null removes all for the project.  Both null is an error.
     * @param uid the uid to revoke (if null revoke all challenges for this
     * project)
     * @param projectid the projectid to revoke (if null revoke all uid
     * challenges)
     * @throws DeterFault on DB error
     */
    public void clearChallenges(String uid, String projectid) 
	throws DeterFault {
	PreparedStatement p = null;

	if ( projectid == null && uid == null)
	    throw new DeterFault(DeterFault.request, 
		    "Missing uid and projectid clearing challenges");
	try {
	    expireChallenges();
	    if ( uid != null && projectid != null ) {
		p = getPreparedStatement(
			"DELETE FROM projectchallenge " + 
			"WHERE uidx=(SELECT idx FROM users WHERE uid=?) " + 
			"AND pidx=(SELECT idx FROM projects " + 
			    "WHERE projectid=?)");
		p.setString(1, uid);
		p.setString(2, projectid);
	    }
	    else if (uid == null ) {
		p = getPreparedStatement(
			"DELETE FROM projectchallenge " + 
			"WHERE pidx=(SELECT idx FROM projects " + 
			    "WHERE projectid=?)");
		p.setString(1, projectid);
	    }
	    else {
		p = getPreparedStatement(
			"DELETE FROM projectchallenge " +
			"WHERE uidx=(SELECT idx FROM users WHERE uid=?)");
		p.setString(1, uid);
	    }
	    p.executeUpdate();
	    cleanIndexPool();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }


    /**
     * Remove all challenges for this project.
     * @param projectid the projectid to revoke
     * @throws DeterFault on DB error
     */
    public void clearChallenges(String projectid) 
	throws DeterFault { clearChallenges(null, projectid); }


    /**
     * Add a challenge to the database that will add uid to projectid with
     * permissions perms when responded to.  The challenge expires in 2 days.
     * @param uid the user to add
     * @param projectid the destination project
     * @param perms the permissions
     * @return the challengeID created
     * @throws DeterFault if the challenge cannot be created
     */
    public long addChallenge(String uid, String projectid,
	    Collection<String> perms) throws DeterFault {
	long id = rng.nextLong();

	if ( uid == null || projectid == null) 
	    throw new DeterFault(DeterFault.request, 
		    "Missing uid or projectid");
	try {
	    boolean added = false;
	    int retries = 0;

	    expireChallenges();

	    PreparedStatement p = getPreparedStatement(
		    "INSERT INTO projectchallengeidx (idx) VALUES (?)");
	    while (!added) {
		try {
		    p.setLong(1,id);
		    p.executeUpdate();
		    added = true;
		}
		catch (SQLIntegrityConstraintViolationException e) {
		    id = rng.nextLong();
		    if ( retries++ > 1000)
			throw new DeterFault(DeterFault.internal,
				"Cannot get unique id for challenge!?");
		}
	    }

	    // If the permission string requested is valid for both projects
	    // and circles, the LIMIT clause in the nested query for permission
	    // indices effectively picks one randomly.  Since permissions in
	    // challenges are exported as strings, this causes no ambiguity.
	    p = getPreparedStatement(
		    "INSERT INTO projectchallenge " +
			"(idx, uidx, pidx, expires, permidx) "+
		    "VALUES (?, (SELECT idx FROM users WHERE uid=?), " +
			"(SELECT idx FROM projects WHERE projectid = ?), ?, "+
			"(SELECT idx FROM permissions WHERE name=? " +
			"AND (valid_for='project' OR valid_for='circle' "+
			    ") LIMIT 1))");
	    p.setLong(1, id);
	    p.setString(2, uid);
	    p.setString(3, projectid);
	    // Exprires in 2 days
	    p.setTimestamp(4, new Timestamp(System.currentTimeMillis() + 
			2 * 24 * 3600 * 1000));
	    if ( perms.isEmpty()) {
		p.setString(5, null);
		p.executeUpdate();
	    }
	    else {
		for (String perm: perms){
		    p.setString(5, perm);
		    p.executeUpdate();
		}
	    }
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	return id;
    }

    /**
     * Return the contents of the challenge with the given ID, if any.
     * @param challengeId the ID to find
     * @return the contents
     * @throws DeterFault if there is a DB error
     */
    public Contents getChallenge(long challengeId) throws DeterFault {
	Contents rv = null;
	try {
	    expireChallenges();
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid, projectid, name " +
		    "FROM projectchallenge AS ch " +
			"LEFT JOIN projects AS c ON c.idx = pidx " +
			"LEFT JOIN users AS u ON u.idx = uidx " +
			"LEFT JOIN permissions AS p ON p.idx = permidx " +
			"WHERE ch.idx= ?");
	    p.setLong(1, challengeId);
	    ResultSet r = p.executeQuery();
	    String uid = null;
	    String pid = null;
	    int rows = 0;
	    Set<String> perms = new HashSet<String>();
	    while (r.next()) {
		rows++;
		uid = r.getString(1);
		pid = r.getString(2);
		if (r.getString(3) != null)
		    perms.add(r.getString(3));
	    }
	    p = getPreparedStatement(
		    "DELETE FROM projectchallenge WHERE idx=?");
	    p.setLong(1, challengeId);
	    p.executeUpdate();
	    cleanIndexPool();
	    if (rows > 0)
		rv = new Contents(uid, pid, perms);
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
}
