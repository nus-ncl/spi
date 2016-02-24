package net.deterlab.testbed.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.deterlab.testbed.api.ApiObject;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.NotificationFlag;
import net.deterlab.testbed.api.UserNotification;
import net.deterlab.testbed.db.DBObject;
import net.deterlab.testbed.db.SharedConnection;

public class NotificationStoreDB extends DBObject {
    /** Notification has been read */
    static final int READ = 1 << 0;
    /** Notification is urgent */
    static final int URGENT = 1 << 1;

    /**
     * Create a notification store
     * @throws DeterFault if there are problems setting up the database
     */
    public NotificationStoreDB() throws DeterFault {
	super();
    }
    /**
     * Create a notification store that shares a DB connection.
     * @param sc the shared connection
     * @throws DeterFault if there are problems setting up the database
     */
    public NotificationStoreDB(SharedConnection sc) throws DeterFault {
	super(sc);
    }

    /**
     * Return the integer representation of the given flag.
     * @param flag the flag to check
     * @return the integer representation
     * @throws DeterFault if the flag is invalid
     */
    static public int validateFlag(NotificationFlag flag) throws DeterFault {
	if (flag == null || flag.getTag() == null )
	    throw new DeterFault(DeterFault.request, "Null Notification Flag");
	String TAG = flag.getTag().toUpperCase();

	if (TAG.equals(NotificationFlag.READ_TAG))
	    return flag.getIsSet() ? READ : 0;
	else if (TAG.equals(NotificationFlag.URGENT_TAG))
	    return flag.getIsSet() ? URGENT : 0;
	else throw new DeterFault(DeterFault.request,
		"Bad Notification Flag " + flag);
    }

    /**
     * Return the integer representation of the given flags.
     * @param flags the flags to check
     * @return the integer representation
     * @throws DeterFault if any flag is invalid
     */
    static public int validateFlags(NotificationFlag[] flags)
	throws DeterFault {
	int rv = 0;
	for (NotificationFlag f: flags )
	    rv |= validateFlag(f);
	return rv;
    }

    /**
     * Return a set of flags that encode the given integer.
     * @param f the integer
     * @return the flags
     */
    static public NotificationFlag[] intToFlags(int f) {
	NotificationFlag[] rv = new NotificationFlag[2];

	rv[0] = new NotificationFlag(NotificationFlag.URGENT_TAG,
		(f & URGENT) == URGENT);
	rv[1] = new NotificationFlag(NotificationFlag.READ_TAG,
		(f & READ) == READ);
	return rv;
    }

    /**
     * Return the mask bits for a flag.
     * @param f the flag to convert
     * @return the mask bits for a flag
     * @throws DeterFault if the flag is invalid
     */
    static public int makeMask(NotificationFlag f) throws DeterFault {
	return validateFlag(new NotificationFlag(f.getTag(), true));
    }

    /**
     * Return the mask bits for a set of flags.  Applications generally do not
     * need to do this.
     * @param flags the flags to convert
     * @return the mask bits for a set of flags
     * @throws DeterFault if any flag is invalid
     */
    static public int makeMask(NotificationFlag[] flags) throws DeterFault {
	int rv = 0;
	for (NotificationFlag f: flags )
	    rv |= makeMask(f);
	return rv;
    }

    /**
     * This places a new notification with the given body into the database and
     * sets the ID and creation time.  
     * @param text the content of the notification
     * @return the id of the notofication
     * @throws DeterFault if there is a creation problem
     */
    public long create(String text) throws DeterFault  {
	long id = -1;

	try {
	    PreparedStatement p = getPreparedStatement(
		    "INSERT INTO notification " +
		    "(body, created) VALUES(?, ?)",
		    Statement.RETURN_GENERATED_KEYS);
	    p.setString(1, text);
	    p.setTimestamp(2, new Timestamp(new Date().getTime()));
	    p.executeUpdate();
	    ResultSet r = p.getGeneratedKeys();
	    if ( !r.next()) 
		throw new DeterFault(DeterFault.internal, 
			"Failed to get generated key??");
	    id = r.getLong(1);
	    if ( r.next()) 
		throw new DeterFault(DeterFault.internal, 
			"More than one generated key??");
	    return id;

	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Database error saving notification: " + e.getMessage());
	}
    }

    /**
     * Deliver this notification to the collection of users with the given set
     * of flags.  This is also used to update the flags of different delivery
     * instances, e.g., to mark messages read.
     * @param users the users to revceive the updates
     * @param f the new flags
     * @param id the message to deliver
     * @throws DeterFault if there are database errors
     */
    public void deliver(Collection<String> users, int f, long id) 
	    throws DeterFault {
	if ( users == null ) return;
	if ( id == -1 ) 
	    throw new DeterFault(DeterFault.internal,
		    "Cannot deliver notification with id == -1");

	Set<String> insert = new HashSet<String>(users);
	StringBuilder ids = new StringBuilder();

	try {
	    /* If this notification has been sent to some users, we want to
	     * update them. */
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid, uidx FROM usernotification " +
		    "LEFT JOIN users on uidx = idx " +
		    "WHERE nidx = ?");
	    p.setLong(1, id);
	    ResultSet r = p.executeQuery();
	    while (r.next()) {
		String rcpt = r.getString("uid");
		if (insert.contains(rcpt)) {
		    insert.remove(rcpt);
		    if (ids.length() > 0 ) ids.append(",");
		    ids.append(r.getInt(2));
		}
	    }
	    p.close();

	    // Insert new sends
	    p = getPreparedStatement("INSERT INTO usernotification " +
		    "(nidx, uidx, flags) VALUES "+ 
		    "(?, (SELECT idx FROM users WHERE uid = ?), ?)");
	    for (String uid: insert) {
		p.setLong(1, id);
		p.setString(2, uid);
		p.setInt(3, f);
		p.executeUpdate();
	    }
	    p.close();

	    // No messages to update? all done.
	    if ( ids.length() == 0 ) return ;
	    // This is safe to construct because both the notification index
	    // (id) and the list of user indexes (ids) are from the DB.  Plus
	    // there's no way to construct the set.
	    p = getPreparedStatement(
		    "UPDATE usernotification SET flags = " + f +
		    "WHERE nidx = " + id + " AND uidx IN (" + 
		    ids.toString() + ")");
	    p.executeUpdate();
	    p.close();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Database error loading notification: " + e.getMessage());
	}
    }

    /**
     * Remove all notification deliveries from a user in preparation for
     * deleting the user.  This should only be done immediately prior to
     * removing a user.
     * @param uid the user to undeliver.
     * @throws DeterFault if there are database errors
     */
    public void undeliverAll(String uid) throws DeterFault {
	if ( uid == null ) return ;
	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM usernotification WHERE uidx=" +
			"(SELECT idx FROM users WHERE uid=?)");
	    p.setString(1, uid);
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Database error undelivering notification: " + 
		    e.getMessage());
	}
    }

    /**
     * Change the flags of the given ids sent to user.
     * @param uid the recipient
     * @param ids an array of ids
     * @param flags the new flags
     * @param mask the flags that are significant
     * @throws DeterFault on error
     */
    public void mark(String uid, long[] ids, int flags, int mask) 
	    throws DeterFault {
	if ( uid == null || ids == null || ids.length == 0) return;

	StringBuilder idset = new StringBuilder();
	// Make a list of the ids that the user has asked to mark.
	for (long i: ids ) {
	    if ( idset.length() > 0 ) idset.append(",");
	    idset.append(i);
	}
	try {
	    // This is sort of a festival of stuff going on in SQL
	    // the SET clause sets only the bits in the DB flags field that 
	    //	    are set in both the flags parameter and the mask parameter
	    //	    and clears the bits in the flags DB field that are unset in
	    //	    the flags parameter and set in the mask.
	    // the WHERE uidx clause gets the index of uid.  uid has come from
	    //	    a user and needs to be passed through the prepareStatement
	    //	    interface so bad things are escaped.
	    // the WHERE nidx subquery sifts out indices that are not around in
	    //	    the DB.  They're just longs so it's safe to print them into
	    //	    the query and sets cannot be ?-ed.
	    PreparedStatement p = getPreparedStatement(
		    "UPDATE usernotification " +
		    "SET flags = ((flags & ~ " + mask + ") | ( " +
			flags + " & " + mask + ")) " +
		    "WHERE uidx = (SELECT idx FROM users WHERE uid = ?) AND " +
			"nidx IN (SELECT idx FROM notification WHERE idx IN " +
			    "(" + idset.toString() + "))");
	    p.setString(1, uid);
	    p.executeUpdate();
	    p.close();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Database error loading notification: " + e.getMessage());
	}
    }


    /**
     * Gather a list of notifications sent to this user between the given
     * timestamps (inclusive) and with the given flags set.  Only flags set in
     * the mask are considered.
     * @param uid the user
     * @param from the first time to return messages for (may be null)
     * @param to the last time to return messages for (may be null)
     * @param flags return messages with flags in this state
     * @param mask the flags to consider (mask == 0 and flags are ignored)
     * @return a list of relevant UserNotificationDBs
     * @throws DeterFault if ther is a database problem
     */
    public List<UserNotification> get(String uid, String from, 
	    String to, int flags, int mask) throws DeterFault {
	int match = (flags & mask);
	List<UserNotification> rv = new ArrayList<UserNotification>();
	String q = "SELECT nidx, body, created, flags FROM usernotification " + 
	    "LEFT JOIN notification ON nidx = idx " +
	    "WHERE uidx = (SELECT idx FROM users WHERE uid = ?) ";

	if ( uid == null ) return rv;
	if ( mask != 0 ) 
	    q += "AND (flags & ?) = ? ";
	if (from != null ) q += "AND created >= ? ";
	if (to != null ) q += "AND created <= ? ";

	try {
	    PreparedStatement p = getPreparedStatement(q);
	    int field = 1;
	    p.setString(field++, uid);
	    if ( mask != 0 ) {
		p.setInt(field++, mask);
		p.setInt(field++, match);
	    }
	    if (from != null ) 
		p.setTimestamp(field++, new Timestamp(
			    ApiObject.stringToDate(from).getTime()));
	    if (to != null ) 
		p.setTimestamp(field++, new Timestamp(
			    ApiObject.stringToDate(to).getTime()));
	    ResultSet r = p.executeQuery();
	    while (r.next()) {
		UserNotification n = new UserNotification();

		n.setId(r.getLong("nidx"));
		n.setBody(r.getString("body"));
		n.setSent(ApiObject.dateToString(r.getTimestamp("created")));
		n.setFlags(intToFlags(r.getInt("flags")));
		rv.add(n);
	    }
	    p.close();
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Database error loading notification: " + e.getMessage());
	}
    }
}
