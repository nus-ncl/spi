package net.deterlab.testbed.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.UserChallenge;
import net.deterlab.testbed.db.DBObject;
import net.deterlab.testbed.db.SharedConnection;

/**
 * A class that validates users.
 * @author the DETER Team
 * @version 1.0
 */
public abstract class UserValidatorDB extends DBObject {

    /**
     * Make a UserValidatorDB
     * @throws DeterFault if the DB connection fails
     */
    public UserValidatorDB() throws DeterFault {
	super();
    }

    /**
     * Make a UserValidatorDB that shares a DB connection
     * @param sc the shared connection
     * @throws DeterFault if the DB connection fails
     */
    public UserValidatorDB(SharedConnection sc) throws DeterFault {
	super(sc);
    }

    /**
     * Remove challenges of this type that have expired from the database.
     * @throws DeterFault if there are database or configuration problems
     */
    protected void clearExpired() throws DeterFault {
	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM userchallenge " +
		    "WHERE validity < NOW() AND type = ?");
	    p.setString(1, getName());
	    p.executeUpdate();
	}
	catch (SQLException e) { 
	    throw new DeterFault(DeterFault.internal, 
		    "Update Failed in clearExpired!? " + e.getMessage());
	}
    }

    /**
     * Return the number of open challenges of this type for the given uid.
     * Used to throttle requests.
     * @param uid the user ID to check
     * @return the number of open challenges of this type for the given uid.
     * @throws DeterFault if there are database or configuration problems
     */
    protected int countOpenChallenges(String uid) throws DeterFault {
	int rv = 0;
	try {
	    PreparedStatement p = getPreparedStatement("SELECT COUNT(*) " +
		    "FROM userchallenge INNER JOIN users ON uidx = idx " +
		    "WHERE type = ? and uid=?");
	    p.setString(1, getName());
	    p.setString(2, uid);
	    ResultSet r = p.executeQuery();
	    if (r == null) return 0;
	    if ( !r.next()) return 0;

	    rv = r.getInt("COUNT(*)");
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Query Failed in countOpenChallenges!? " + e.getMessage());
	}
    }

    /**
     * Remove the challenge with the given identifier.
     * @param id the ID to remove
     * @throws DeterFault if there are database or configuration problems
     */
    protected void clearID(long id) throws DeterFault {
	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM userchallenge " +
		    "WHERE challengeid = ?");
	    p.setLong(1, id);
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Update Failed in clearID!? " + e.getMessage());
	}
    }


    /**
     * Return the name of this class (the type in a request)
     * @return the name of this class (the type in a request)
     */
    public abstract String getName();

    /**
     * Return the challenge to issue this user.  As a side effect, put the
     * challenge into the database.
     * @param u the loaded user profile of the user to validate
     * @return the challenge to issue
     * @throws DeterFault if the challenge cannot be issued (DB problems or too
     * many outstanding challenges)
     */
    public abstract UserChallenge issueChallenge(UserDB u)
	throws DeterFault;

    /**
     * Return the uid validated or null
     * @param response the user-submitted response
     * @param uc the challenge being responded to
     * @return the uid validated 
     * @throws DeterFault if validation fails.
     */
    public abstract String validateChallenge(byte[] response, 
	    UserChallengeDB uc) throws DeterFault;
}
