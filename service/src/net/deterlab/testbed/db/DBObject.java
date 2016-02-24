package net.deterlab.testbed.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

import net.deterlab.testbed.api.DeterFault;

/**
 * A base class than manages a connection to the Deter Database and provides
 * methods to re-establish connections and allocate PreparedStatements.
 * Subclasses will build the DB interacions from these.
 * @author DETER team 
 * @version 1.0
 */
public class DBObject {
    /** SharedConnection to the database */
    private SharedConnection c;

    /** valid scoped names are owner:name */
    static private Pattern validScopedName = Pattern.compile("^[^:]+:[^:]+$");

    /**
     * Create a new DBObject that shares sc.  Note that this object does its
     * best to have a connection.  if the passed connection is null, this
     * contstructor allocates one.
     * @param sc the connection to share
     * @throws DeterFault if sc was closed completely and cannot be opened.
     */
    public DBObject(SharedConnection sc) throws DeterFault {
	c = (sc != null) ? sc : new SharedConnection();
	c.open();
    }

    /**
     * Initialize a new database object.  A new SharedConnection is allocated.
     * @throws DeterFault if the DB connection cannot be made (e.g., DETER
     * config is wrong)
     */
    public DBObject() throws DeterFault {
	this(new SharedConnection());
    }

    /**
     * Confirm that this name is a lexically valid scoped name: name:name with
     * exactly one colon(:).
     * @param n the candidate name
     * @throws DeterFault if the name is invalid
     */
    protected static void checkScopedName(String n) throws DeterFault {
	if ( n == null || !validScopedName.matcher(n).matches())
	    throw new DeterFault(DeterFault.request,
		    "Bad scoped name (lexical) " + n);
    }


    /**
     * Close the underlying SharedConnection.
     * @throws DeterFault if there's a problem closing (!)
     */
    public void close() throws DeterFault {
	try {
	    if ( c != null ) {
		c.close();
		c = null;
	    }
	} catch (DeterFault e) {
	    c = null;
	    throw e;
	}
    }

    /**
     * Close the DB connection, ignoring exceptions.
     */
    public void forceClose() {
	try {
	    close();
	}
	catch (DeterFault ignored) { }
    }

    /**
     * Get a prepared statement from the DB connection.  If there is no
     * connection, make it.
     * @param sqlString the SQL in the prepared statement
     * @return the prepared statement.
     * @throws DeterFault if the new DB connection cannot be made (e.g., DETER
     * config is wrong) or prepared statement blows up.
     */
    public PreparedStatement getPreparedStatement(String sqlString) 
	throws DeterFault {

	try {
	    if ( c == null )
		c = new SharedConnection();

	    Connection cc = c.getConnection();
	    return cc.prepareStatement(sqlString);
	}
	catch (SQLException e) {
	    forceClose();
	    throw new DeterFault(DeterFault.internal,
		    "Error preparing DB statement " + e);
	}
	catch (DeterFault e) {
	    forceClose();
	    throw e;
	}
    }

    /**
     * Get a prepared statement from the DB connection.  If there is no
     * connection, make it.
     * @param sqlString the SQL in the prepared statement
     * @param sqlFlags the flags for the prepared statement
     * @return the prepared statement.
     * @throws DeterFault if the new DB connection cannot be made (e.g., DETER
     * config is wrong) or prepared statement blows up.
     */
    public PreparedStatement getPreparedStatement(String sqlString,
	    int sqlFlags) throws DeterFault {

	try {
	    if ( c == null )
		c = new SharedConnection();

	    Connection cc = c.getConnection();
	    return cc.prepareStatement(sqlString, sqlFlags);
	}
	catch (SQLException e) {
	    forceClose();
	    throw new DeterFault(DeterFault.internal,
		    "Error preparing DB statement " + e);
	}
	catch (DeterFault e) {
	    forceClose();
	    throw e;
	}
    }

    /**
     * Return the underlying shared connection, useful for sharing it.
     * @return the underlying shared connection
     */
    public SharedConnection getSharedConnection() {
	return c;
    }
}
