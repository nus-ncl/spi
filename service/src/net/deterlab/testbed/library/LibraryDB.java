package net.deterlab.testbed.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.db.ACLObject;
import net.deterlab.testbed.db.SharedConnection;

/**
 * Library information in the Database
 * @author DETER team
 * @version 1.1
 */

public class LibraryDB extends ACLObject {
    /** The name of the library */
    private String libid;

    /**
     * Base constructor, needed to allow nameless libraries and keep one call
     * to the superclass constructor.
     * @throws DeterFault if there is a DB setup  error.
     */

    protected LibraryDB(SharedConnection sc) throws DeterFault{
	super("library", "libraryperms", "lidx", "libraries", "libid", sc);
    }

    /**
     * Create a nameless LibraryDB
     * @throws DeterFault if there is a DB setup  error.
     */
    public LibraryDB() throws DeterFault {
	this((SharedConnection) null);
    }

    /**
     * Create an LibraryDB with the given libid.
     * @param lib the libid
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error.
     */
    public LibraryDB(String lib) throws DeterFault {
	this(lib, null);
    }

    /**
     * Create an LibraryDB with the given libid that shares a DB connection
     * @param lib the libid
     * @param sc the shared connection
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error.
     */
    public LibraryDB(String lib, SharedConnection sc) throws DeterFault {
	this(sc);
	try {
	    setLibid(lib);
	}
	catch (DeterFault df) {
	    forceClose();
	    throw df;
	}
    }

    /**
     * Return the identifier to ACLObject routines
     * @return the identifier to ACLObject routines
     */
    protected String getID() { return getLibid(); }

    /**
     * Return the libid.
     * @return the libid.
     */
    public String getLibid() { return libid; }

    /**
     * Set a new libid
     * @param lib the libid.
     * @throws DeterFault if the name is badly formatted
     */
    public void setLibid(String lib) throws DeterFault {
	checkScopedName(lib);
	libid = lib;
    }

    /**
     * Return true if the library exists.
     * @return true if the library exists.
     * @throws DeterFault if there is a DB error
     */
    public boolean isValid() throws DeterFault {
	PreparedStatement p = null;
	int count = 0;

	if ( getLibid() == null ) return false;

	try {
	    p = getPreparedStatement(
		    "SELECT count(*) FROM libraries WHERE libid=?");
	    p.setString(1, getLibid());
	    ResultSet r = p.executeQuery();
	    int rows = 0;
	    while ( r.next()) {
		if ( rows++ != 0) return false;
		count = r.getInt(1);
	    }
	    return count == 1;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error checking name: " + e);
	}
    }

    protected void validateExperimentNames(Set<String> eids) throws DeterFault {
	// We could do this more efficiently from a DB prespective, but it's
	// nice to know which experiment was bad.
	try {
	    for (String eid: eids) {
		PreparedStatement p = getPreparedStatement(
			"SELECT idx FROM experiments WHERE eid=?");
		p.setString(1, eid);
		ResultSet r = p.executeQuery();
		if ( !r.next() )
		    throw new DeterFault(DeterFault.request,
			    "No such experiment " + eid);
		if ( r.next())
		    throw new DeterFault(DeterFault.internal,
			    "Multiple definitions for experiment " + eid);
	    }
	}
	catch (SQLException e) {
	    // Some other SQL problem.  Punt.
	    throw new DeterFault(DeterFault.internal,
		    "Database error checking eids: " + e);
	}
    }

    /**
     * Store this LibraryDB in the database, including the  credentials.
     * @param owner the experiment owner
     * @param experiments the initial experiments
     * @param inAcl the access control list
     * @throws DeterFault on errors
     */
    public void create(String owner, String[] experiments,
	    Collection<AccessMember> inAcl) throws DeterFault {
	PreparedStatement p = null;
	List<AccessMember> acl = new ArrayList<AccessMember>(inAcl);
	Set<String> eids = new HashSet<String>();

	if ( owner == null )
	    throw new DeterFault(DeterFault.request, "No owner given");

	if (experiments == null)
	    throw new DeterFault(DeterFault.internal,
		    "Null experiments in LibraryDB.create");

	if ( acl == null )
	    throw new DeterFault(DeterFault.request, "No acl given");

	if ( getLibid() == null ) 
	    throw new DeterFault(DeterFault.request,
		    "Cannot create library with no name");

	// The set eliminates duplicate entries.
	for (String eid : experiments)
	    eids.add(eid);

	validateExperimentNames(eids);

	try {
	    p = getPreparedStatement(
		    "INSERT INTO libraries (libid, owneridx) " +
		    "VALUES(?, (SELECT idx FROM users WHERE uid=?))");
	    p.setString(1, getLibid());
	    p.setString(2, owner);
	    p.executeUpdate();
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    // Creating this record can only really violate the unique libid
	    // constraint.
	    throw new DeterFault(DeterFault.request, "Library exists");
	}
	catch (SQLException e) {
	    // Some other SQL problem.  Punt.
	    throw new DeterFault(DeterFault.internal,
		    "Database error creating library: " + e);
	}

	// Really create the rest of the library.  From here on out, if
	// there are problems we wipe the partial library.
	try {
	    // Assign permissions
	    for (AccessMember m : acl )
		assignPermissions(m);

	    // Now add the initial experiments
	    for (String eid : eids) 
		addExperiment(eid);
	}
	catch (DeterFault df) {
	    // Something failed. Remove the database entries
	    // (ACLs and definition).
	    try { remove(); }
	    catch (DeterFault ignored) { }
	    throw df;
	}
	updatePolicyCredentials();
	updateCircleCredentials();
	updateOwnerCredentials(null, owner);
    }

    /**
     * Put this experiment into the library.
     * @param eid the ID to add
     * @throws DeterFault if the experiment could not be added.
     */
    public void addExperiment(String eid) throws DeterFault {
	try {
	    PreparedStatement p = getPreparedStatement(
		    "INSERT INTO librarymembers (lidx, eidx) " +
			"VALUES (" +
			    "(SELECT idx FROM libraries WHERE libid=?)," +
			    "(SELECT idx FROM experiments WHERE eid=?))");
	    p.setString(1,getLibid());
	    p.setString(2, eid);
	    p.executeUpdate();
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    throw new DeterFault(DeterFault.request, "Bad experiment: " + eid);
	}
	catch (SQLException e) {
	    // Some other SQL problem.  Punt.
	    throw new DeterFault(DeterFault.internal,
		    "Database error adding experiment: " + e);
	}
    }

    /**
     * Remove this experiment from the library.
     * @param eid the ID to add
     * @throws DeterFault if the experiment could not be added.
     */
    public void removeExperiment(String eid) throws DeterFault {
	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM librarymembers " +
			"WHERE " +
			    "lidx=(SELECT idx FROM libraries WHERE libid=?)" +
			    " AND " +
			    "eidx=(SELECT idx FROM experiments WHERE eid=?)");
	    p.setString(1,getLibid());
	    p.setString(2, eid);
	    int deleted = p.executeUpdate();

	    if ( deleted < 1)
		throw new DeterFault(DeterFault.request,
			"No such experiment in library");
	    else if ( deleted > 1)
		throw new DeterFault(DeterFault.internal,
			"Experiment in library multiple times!?");
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    throw new DeterFault(DeterFault.request, "Bad experiment: " + eid);
	}
	catch (SQLException e) {
	    // Some other SQL problem.  Punt.
	    throw new DeterFault(DeterFault.internal,
		    "Database error removing experiment: " + e);
	}
    }

    /**
     * Return the eids associated with this library.
     * @return the eids associated with this library.
     * @throws DeterFault on DB error
     */
    public List<String> getExperiments() throws DeterFault {
	List<String> rv = new ArrayList<String>();
	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT eid " +
			"FROM librarymembers LEFT JOIN " +
			    "experiments ON eidx = idx " +
			"WHERE lidx=(SELECT idx FROM libraries WHERE libid=?)");
	    p.setString(1, getLibid());
	    ResultSet r = p.executeQuery();

	    while ( r.next())
		rv.add(r.getString(1));
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, "Database error: " + e);
	}
	return rv;
    }


    /**
     * Get the library's owner
     * @return the library's owner
     * @throws DeterFault if there is an error
     */
    public String getOwner() throws DeterFault {
	String rv = null;

	if ( getLibid() == null )
	    throw new DeterFault(DeterFault.internal,
		    "getOwner failed. Library does not have a name");

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM libraries LEFT JOIN users AS u " +
			"ON u.idx = owneridx " +
		    "WHERE libid=?");
	    p.setString(1, getLibid());
	    ResultSet r = p.executeQuery();
	    int rows = 0;
	    while (r.next())
		if (rows++ == 0) rv= r.getString(1);
		else throw new DeterFault(DeterFault.internal,
			"More than one owner for library??");

	    if (rv == null)
		throw new DeterFault(DeterFault.internal,
			"No owner for library??");
	     return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Set the library's owner to the new uid.
     * @param o the new onwer's uid
     * @throws DeterFault if there is a DB problem
     */
    public void setOwner(String o) throws DeterFault {

	if ( getLibid() == null )
	    throw new DeterFault(DeterFault.internal,
		    "setOwner failed. Library does not have a name");
	if ( o  == null )
	    throw new DeterFault(DeterFault.request,
		    "setOwner failed. No owner provided");
	String oldOwner = getOwner();
	String newOwnerCircle = o + ":" + o;
	try {
	    PreparedStatement p = getPreparedStatement(
		    "UPDATE libraries " +
		    "SET owneridx = (SELECT idx FROM users WHERE uid = ?) " +
		    "WHERE libid=?");
	    p.setString(1, o);
	    p.setString(2, getLibid());
	    p.executeUpdate();
	    updateOwnerCredentials(oldOwner, o);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Remove the library from the database and remove config files stored
     * for it.  Errors from the database cause faults to be thrown.
     * @throws DeterFault on error
     */
    public void remove() throws DeterFault {
	PreparedStatement p = null;

	if ( getLibid() == null ) 
	    throw new DeterFault(DeterFault.request,
		    "Cannot remove library with no name");

	// Get all of the credentials out of the DB and cache
	removeCredentials();
	try {
	    p = getPreparedStatement( "DELETE FROM librarymembers " +
		    "WHERE lidx=(SELECT idx FROM libraries WHERE libid=?)");
	    p.setString(1, getLibid());
	    p.executeUpdate();
	    p = getPreparedStatement( "DELETE FROM libraryperms " +
		    "WHERE lidx=(SELECT idx FROM libraries WHERE libid=?)");
	    p.setString(1, getLibid());
	    p.executeUpdate();
	    p = getPreparedStatement( "DELETE FROM libraries WHERE libid=?");
	    p.setString(1, getLibid());
	    int ndel = p.executeUpdate();

	    if ( ndel == 0 )
		throw new DeterFault(DeterFault.request, "No such library");
	    else if ( ndel > 1 )
		throw new DeterFault(DeterFault.internal,
			"Multiple libraries deleted??");
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error removing library: " + e);
	}
    }

    /**
     * Return LibraryDBs readable by the given user, that match regex. Any of
     * the parameters can be null.  All the returned LibraryDB's share the
     * given connection.
     * @param uid the user to match
     * @param regex the name regex
     * @param offset the index of the first library description to return
     * @param count the number of library descriptions to return
     * @param sc the shared connection to use
     * @return a collection of LibraryDBs
     * @throws DeterFault on error
     */
    static public List<LibraryDB> getLibraries(String uid,
	    String regex, int offset, int count, SharedConnection sc)
	throws DeterFault {
	StringBuilder query = new StringBuilder("SELECT libid FROM libraries ");
	Connection c = null;
	boolean whereAdded = false;
	int sqlIdx = 1;
	List<LibraryDB> rv = new ArrayList<LibraryDB>();

	if ( sc == null) sc = new SharedConnection();

	try {
	    sc.open();
	    c = sc.getConnection();
	    if ( uid != null ) {
		if (whereAdded) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		// Pretty complex.  Get the library indices from
		// libraryperms entries where the permission index matches
		// LIBRARY_READ and the cidx is a circle that has uid as a
		// member.
		query.append(
			"(idx IN "+
			    "(SELECT DISTINCT lidx FROM libraryperms " +
			    "WHERE permidx=(" +
				"SELECT idx FROM permissions "+
				"WHERE name='READ_LIBRARY' and " +
				    "valid_for='library') " +
			    "AND cidx IN (" +
				"SELECT DISTINCT cidx FROM circleusers " +
				"WHERE uidx=(SELECT idx FROM users " +
				    "WHERE uid=?))"+
			") OR owneridx=(SELECT idx FROM users WHERE uid=?)) ");
	    }

	    if ( regex != null ) {
		if (whereAdded) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		// Nice and simple.
		query.append("libid REGEXP ? ");
	    }

	    if ( count != -1 ) {
		if ( offset < 0) offset = 0;
		query.append("ORDER BY idx LIMIT ? OFFSET ? ");
	    }
	    PreparedStatement p = c.prepareStatement(query.toString());

	    if ( uid != null) {
		p.setString(sqlIdx++, uid);
		p.setString(sqlIdx++, uid);
	    }
	    if ( regex != null) p.setString(sqlIdx++, regex);
	    if ( count  != -1 ) {
		p.setInt(sqlIdx++, count);
		p.setInt(sqlIdx++, offset);
	    }

	    ResultSet r = p.executeQuery();
	    while (r.next())
		rv.add(new LibraryDB(r.getString(1), sc));
	    sc.close();
	    return rv;
	}
	catch (SQLException e) {
	    for (LibraryDB exp : rv)
		exp.forceClose();
	    try {
		// Forceclose might affect others
		sc.close();
	    }
	    catch (DeterFault ignored) { }
	    throw new DeterFault(DeterFault.internal, "SQL Exception: " +e);
	}
    }
}
