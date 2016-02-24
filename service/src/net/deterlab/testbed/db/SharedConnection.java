package net.deterlab.testbed.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;

/**
 * A connection shared among DBObjects.  This exports a fairly minimal supset
 * of the Connection interface.  There's a little reference counting in
 * here as well as some error handling.
 * @author DETER Team
 * @version 1.0
 */
public class SharedConnection {
    /** reference count */
    private int refs;
    /** The underlying shared connection. */
    private Connection c;
    /** The DETER config that tells where the DB is. */
    private Config config;

    /**
     * Get a new SharedConnection, already open.
     * @throws DeterFault if the underlying connection fails to initialize
     */
    public SharedConnection() throws DeterFault {
	refs = 0;
	c = null;
	config = new Config();
    }

    /**
     * If the underlying connection doesn't exist, make it.
     */
    protected void connect() throws DeterFault {
	try {
	    if ( c != null ) {
		// Yep, this can throw an exception
		if ( c.isClosed()) c = null;
		else return;
	    }
	    c = DriverManager.getConnection(config.getDeterDbUrl());
	}
	catch (SQLException e) {
	    c = null;
	    throw new DeterFault(DeterFault.internal, "DB error: " +e);
	}
    }

    /**
     * Start sharing this connection.
     * @throws DeterFault if the underlying connection was closed and could not
     * be reestablished.
     */
    public void open() throws DeterFault { refs++; connect(); }

    /**
     * Stop sharing this connection - close the underlying connection if the
     * count has gone below 1.
     * @throws DeterFault if the underlying connection fails to close
     */
    public void close() throws DeterFault {
	if ( --refs == 0 ) {
	    try {
		c.close();
		c = null;
	    }
	    catch (SQLException e) {
		c = null;
		throw new DeterFault(DeterFault.internal,
			"Error closing DB connection: " + e);
	    }
	}
    }

    /**
     * Close the underlying connection w/o checking the refs and ignore errors.
     * Should only be called when the connection will no longer be used.
     */
    public void forceClose() {
	try {
	    if (c != null ) c.close();
	    c = null;
	}
	catch (SQLException e) {
	    c = null;
	}
    }

    /**
     * Get the underlying connection for SQL use.
     * @return a Connection (connected)
     * @throws DeterFault if a new connection is attempted and fails.
     */
    public Connection getConnection() throws DeterFault {
	connect();
	return c;
    }

    /**
     * If this SharedConnection has been finalized and the connection is still
     * around, close it up.
     */
    protected void finalize() { 
	try {
	    if ( c != null ) c.close();
	}
	catch (SQLException ignored) { }
    }
}
