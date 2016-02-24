package net.deterlab.testbed.circle;

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
 * one user adds another to a circle using the api.Circles.addUser() call -
 * which is what most users use.  The user being added needs to confirm because
 * some amount of visibility is being granted to their data via this exchange.
 * @author DETER team
 * @version 1.0
 */
public class CircleChallengeDB extends DBObject {
    /** Random number generator to pick IDs */
    static private Random rng = new Random();

    static public class Contents {
	/** The uid to add */
	private String uid;
	/** The circle to add it to */
	private String circleid;
	/** The permissions for the user */
	private Set<String> perms;

	/**
	 * Create a new contents object
	 * @param u the uid
	 * @param c the circleid
	 * @param p the permissions
	 */
	public Contents(String u, String c, Collection<String> p) {
	    uid = u;
	    circleid = c;
	    perms = new HashSet<String>(p);
	}
	/**
	 * Return the uid
	 * @return the uid
	 */
	public String getUid() { return uid; }
	/**
	 * Return the circleid
	 * @return the circleid
	 */
	public String getCircleId() { return circleid; }
	/**
	 * Return the permissions
	 * @return the permissions
	 */
	public Collection<String> getPermissions() { return perms; }
    }


    /**
     * Create a circlechallange
     * @throws DeterFault if the testbed is misconfigured
     */
    public CircleChallengeDB() throws DeterFault {
	super();
    } 

    /**
     * Create a circlechallange that shares a DB connection
     * @param sc the shared connection
     * @throws DeterFault if the testbed is misconfigured
     */
    public CircleChallengeDB(SharedConnection sc) throws DeterFault {
	super(sc);
    } 

    /**
     * Synch the index pool with the challenge DB, e.g. after a deletion
     * @throws DeterFault on a poorly configured DB
     */
    protected void cleanIndexPool() throws DeterFault {
	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM circlechallengeidx " +
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
		    "DELETE FROM circlechallenge WHERE expires < NOW()");
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Remove all challenges that would allow uid to be added to circleid.  If
     * uid is null remove all challenges for this circle, if circleid is null
     * remove all chalenges for this user.  If both are null throw an
     * exception.
     * @param uid the uid to revoke. If uid is null, remove all the challenges
     * for this circle.
     * @param circleid the cirlceid to revoke
     * @throws DeterFault on DB error
     */
    public void clearChallenges(String uid, String circleid) 
	throws DeterFault {
	PreparedStatement p = null;

	if ( circleid == null && uid == null)
	    throw new DeterFault(DeterFault.request, 
		    "Missing circleid and uid");
	try {
	    expireChallenges();
	    if ( uid != null && circleid != null) {
		p = getPreparedStatement(
			"DELETE FROM circlechallenge " + 
			"WHERE uidx=(SELECT idx FROM users WHERE uid=?) " + 
			"AND cidx=(SELECT idx FROM circles WHERE circleid=?)");
		p.setString(1, uid);
		p.setString(2, circleid);
	    }
	    else if ( circleid != null ) {
		p = getPreparedStatement(
			"DELETE FROM circlechallenge " + 
			"WHERE cidx=" +
			    "(SELECT idx FROM circles WHERE circleid=?)");
		p.setString(1, circleid);
	    }
	    else {
		p = getPreparedStatement(
			"DELETE FROM circlechallenge " +
			"WHERE uidx=" +
			    "(SELECT idx FROM users WHERE uid=?)");
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
     * Remove all challenges outstanding for circleid
     * @param circleid the cirlceid to revoke
     * @throws DeterFault on DB error
     */
    public void clearChallenges(String circleid) throws DeterFault {
	clearChallenges(null, circleid);
    }

    /**
     * Add a challenge to the database that will add uid to circleid with
     * permissions perms when responded to.  The challenge expires in 2 days.
     * @param uid the user to add
     * @param circleid the destination circle
     * @param perms the permissions
     * @return the challengeID created
     * @throws DeterFault if the challenge cannot be created
     */
    public long addChallenge(String uid, String circleid,
	    Collection<String> perms) throws DeterFault {
	long id = rng.nextLong();

	if ( uid == null || circleid == null) 
	    throw new DeterFault(DeterFault.request, 
		    "Missing uid or circleid");
	try {
	    boolean added = false;
	    int retries = 0;

	    expireChallenges();

	    PreparedStatement p = getPreparedStatement(
		    "INSERT INTO circlechallengeidx (idx) VALUES (?)");
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
	    p = getPreparedStatement(
		    "INSERT INTO circlechallenge " +
			"(idx, uidx, cidx, expires, permidx) "+
		    "VALUES (?, (SELECT idx FROM users WHERE uid=?), " +
			"(SELECT idx FROM circles WHERE circleid = ?), ?, "+
			"(SELECT idx FROM permissions WHERE name=? " +
			    "AND valid_for='circle'))");
	    p.setLong(1, id);
	    p.setString(2, uid);
	    p.setString(3, circleid);
	    // Exprires in 2 days
	    p.setTimestamp(4, new Timestamp(System.currentTimeMillis() + 
			2 * 24 * 3600 * 1000));
	    if (perms.isEmpty()) {
		p.setString(5, null);
		p.executeUpdate();
	    }
	    else {
		for ( String perm: perms) {
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
		    "SELECT uid, circleid, name FROM circlechallenge AS ch " +
			"LEFT JOIN circles AS c ON c.idx = cidx " +
			"LEFT JOIN users AS u ON u.idx = uidx " +
			"LEFT JOIN permissions AS p ON p.idx = permidx " +
			"WHERE ch.idx= ?");
	    p.setLong(1, challengeId);
	    ResultSet r = p.executeQuery();
	    int rows = 0;
	    String uid = null;
	    String cid = null;
	    Set<String> perms = new HashSet<String>();
	    while ( r.next())  {
		rows++;
		uid = r.getString(1);
		cid = r.getString(2);
		if (r.getString(3) != null)
		    perms.add(r.getString("name"));
	    }
	    p = getPreparedStatement("DELETE FROM circlechallenge WHERE idx=?");
	    p.setLong(1, challengeId);
	    p.executeUpdate();
	    cleanIndexPool();
	    if (rows > 0)
		rv = new Contents(uid, cid, perms);
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
}
