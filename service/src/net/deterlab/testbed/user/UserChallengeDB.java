package net.deterlab.testbed.user;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;

import javax.sql.rowset.serial.SerialBlob;

import net.deterlab.testbed.api.ApiObject;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.UserChallenge;
import net.deterlab.testbed.db.DBObject;
import net.deterlab.testbed.db.SharedConnection;

/**
 * A UserChallenge stored in a table of a database
 * @author DETER Team
 * @version 1.0
 */
public class UserChallengeDB extends DBObject {

    /** Random number generator to pick IDs */
    static protected Random rng = new Random();

    /**  The kind of challege being issued */
    protected String type;
    /** Bytes in the challenge*/
    protected byte[] data;
    /** Valid until */
    protected String validity;
    /** challenge valid until */
    protected long challengeID;
    /** The userID associated with this challenge */
    protected String uid;

    /**
     * Construct an empty UserChallengeDB
     * @throws DeterFault if the DB connection fails
     */
    public UserChallengeDB() throws DeterFault {
	this(null, null, null, (String) null, -1, null);
    }
    /**
     * Construct an empty UserChallengeDB connected to a shared DB conncetion
     * @param sc the shared connection
     * @throws DeterFault if the DB connection fails
     */
    public UserChallengeDB(SharedConnection sc) throws DeterFault {
	this(null, null, null, (String) null, -1, sc);
    }
    /**
     * Construct a full UserChallengeDB
     * @param u the user attached to this challenge
     * @param t the challenge type
     * @param d the data to carry out the challenge
     * @param v the validity time, as a string
     * @param i the challenge ID
     * @throws DeterFault if the DB connection fails
     */
    public UserChallengeDB(String u, String t, byte[] d, String v, long i)
	    throws DeterFault {
	this(u, t, d, v, i, null);
    }
    /**
     * Construct a full UserChallengeDB
     * @param u the user attached to this challenge
     * @param t the challenge type
     * @param d the data to carry out the challenge
     * @param v the validity time, as a Date
     * @param i the challenge ID
     * @throws DeterFault if the DB connection fails
     */
    public UserChallengeDB(String u, String t, byte[] d, Date v, long i)
	    throws DeterFault {
	this(u, t, d, v, i, null);
    }
    /**
     * Construct a full UserChallengeDB
     * @param u the user attached to this challenge
     * @param t the challenge type
     * @param d the data to carry out the challenge
     * @param v the validity time, as a string
     * @param i the challenge ID
     * @param sc the DB connection to share
     * @throws DeterFault if the DB connection fails
     */
    public UserChallengeDB(String u, String t, byte[] d, String v, long i,
	    SharedConnection sc) throws DeterFault {
	super(sc);
	type = t;
	data = d;
	validity = v;
	challengeID = i;
	uid = u;
    }
    /**
     * Construct a full UserChallengeDB
     * @param u the user attached to this challenge
     * @param t the challenge type
     * @param d the data to carry out the challenge
     * @param v the validity time, as a Date
     * @param i the challenge ID
     * @param sc the DB connection to share
     * @throws DeterFault if the DB connection fails
     */
    public UserChallengeDB(String u, String t, byte[] d, Date v, long i,
	    SharedConnection sc) throws DeterFault {
	this(u, t, d, (String) null, i, sc);
	validity = ApiObject.dateToString(v);
    }
    /**
     * Construct a full UserChallengeDB
     * @param u the user attached to this challenge
     * @param t the challenge type
     * @param d the data to carry out the challenge
     * @param v the validity time, as a Date
     * @param sc the DB connection to share
     * @throws DeterFault if the DB connection fails
     */
    public UserChallengeDB(String u, String t, byte[] d, Date v,
	    SharedConnection sc) throws DeterFault {
	this(u, t, d, (String) null, -1, sc);
	validity = ApiObject.dateToString(v);
    }
    /**
     * Get the challenge type
     * @return the challenge type
     */
    public String getType() { return type; }
    /**
     * Set the challenge type
     * @param t the new challenge type
     */
    public void setType(String t) { type = t; }
    /**
     * Get the challenge data
     * @return the challenge data
     */
    public byte[] getData() { return data; }
    /**
     * Set the challenge data
     * @param d the new challenge data
     */
    public void setData(byte[] d) { data = d; }
    /**
     * Get the time the challenge is valid until.  It is a string of 4-digits
     * of year, 2 of month, 2 of day, a constant T then 2 digits of hour (24
     * hour), 2 of minute and 2 of second followed by a 'Z', in GMT.  In java a
     * format string of yyyyMMdd'T'HHmmss'Z will format it.
     * @return the valid time
     */
    public String getValidity() { return validity; }
    /**
     * Set the time the challenge is valid until.  It is a string of 4-digits
     * of year, 2 of month, 2 of day, a constant T then 2 digits of hour (24
     * hour), 2 of minute and 2 of second followed by a Z, in GMT.  In java a
     * format string of yyyyMMdd'T'HHmmss'Z' will format it.
     * @param v the new validity
     */
    public void setValidity(String v) { validity=v; }
    /**
     * Get the challenge ID
     * @return the challenge ID
     */
    public long getChallengeID() { return challengeID; }
    /**
     * Set the challenge data
     * @param i the new challenge data
     */
    public void setChallengeID(long i) { challengeID = i; }

    /**
     * Return the uid
     * @return the uid
     */
    public String getUid() { return uid; }

    /**
     * Set the uid
     * @param u the new uid
     */
    public void setUid(String u) { uid = u; }

    /**
     * Save a this entry into the DB.  An unique ID is picked in here.
     * @throws DeterFault on database error 
     */
    public void save() throws DeterFault {
	PreparedStatement p = null;
	long id = rng.nextLong();
	Date ts = ApiObject.stringToDate(getValidity());
	boolean added = false;
	byte[] d = getData();
	int retries = 0;

	try {

	    if ( ts == null ) {
		ts = new Date();
		setValidity(ApiObject.dateToString(ts));
	    }

	    p = getPreparedStatement("INSERT INTO userchallenge " +
		    " (uidx, data, validity, challengeid, type) "+ 
		    "VALUES ((SELECT idx FROM users WHERE uid = ?), " +
			"?, ?, ?, ?)");
	    p.setString(1, uid);
	    p.setBlob(2, (d != null) ? new SerialBlob(d) : null);
	    p.setTimestamp(3, new Timestamp(ts.getTime()));
	    p.setString(5, getType());


	    // Add the new challenge to the database.  If the update throws
	    // that exception, the id has collided with an existing challenge
	    // (a very unlikely event).  On a collision, linear search forward
	    // for 1000 entries.  If that fails, throw out a DeterFault.
	    while (!added) {
		try {
		    p.setLong(4, id);
		    p.executeUpdate();
		    added = true;
		}
		catch (SQLIntegrityConstraintViolationException e) {
		    if ( retries ++ > 1000) 
			throw new DeterFault(DeterFault.internal,
				"Tried 1000 challenge IDs and failed");
		    id++;
		}
	    }
	    setChallengeID(id);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Unexpected Exception" + e);
	}
    }

    /**
     * Load this challenge from the database.  Existing values are overweitten.
     * @param id the challengeID to load
     * @throws DeterFault if the challenge cannot be loaded
     */
    public void load(long id) throws DeterFault {
	PreparedStatement p = null;
	ResultSet r = null;
	int i = 0;

	try {
	    int n = 0;

	    p = getPreparedStatement("SELECT uid, data, validity, type " +
		    "FROM userchallenge INNER JOIN users ON idx = uidx " +
		    "WHERE challengeid = ?");
	    p.setLong(1, id);
	    if ( ( r = p.executeQuery()) == null ) 
		throw new DeterFault(DeterFault.internal, "Null result set??");

	    while (r.next()) {
		if ( ++i > 1 ) 
		    throw new DeterFault(DeterFault.internal, 
			    "More than one challenge with ID " + id + "!?");

		Blob b = r.getBlob("data");

		setData( (b != null ) ? b.getBytes(1, (int) b.length()) : null);
		uid = r.getString("uid");
		setValidity(ApiObject.dateToString(r.getDate("validity")));
		setType(r.getString("type"));
		setChallengeID(id);
	    }
	    if ( i != 1 ) 
		throw new DeterFault(DeterFault.request, 
			"no challenge with ID " + id);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Unexpected Exception" + e);
	}
    }

    /**
     * Export this UserChallengeDB as a basic UserChallenge, suitable for
     * transmission.
     * @return the exportable challenge
     */
    public UserChallenge export() {
	return new UserChallenge(getType(), getData(), getValidity(), 
		getChallengeID());
    }
}

