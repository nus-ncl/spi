package net.deterlab.testbed.experiment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.ChangeResult;
import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ExperimentAspect;
import net.deterlab.testbed.db.ACLObject;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.topology.TopologyDescription;

/**
 * Experiment information in the Database
 * @author DETER team
 * @version 1.1
 */

public class ExperimentDB extends ACLObject {
    /** The name of the experiment */
    private String eid;

    /**
     * This is an experiment aspect including both its database and filesystem
     * storage.  This acts as a DBObject, but piggybacks on the shared
     * connection of the enclosing ExperimentDB.
     * @author DETER Team
     * @version 1.0
     */
    public class ExperimentAspectDB {
	/** Aspect Type */
	protected String type;
	/** Aspect SubType */
	protected String subType;
	/** Aspect Name */
	protected String name;
	/** This aspect's representation - null until loaded */
	protected byte[] data;
	/** Path to this aspect's representation in the file system, if any */
	protected String path;
	/** Reference to this aspect's representation if it's not in the file
	 * system. */
	protected String ref;

	/**
	 * Create an empty ExperimentAspectDB
	 */
	public ExperimentAspectDB() {
	    type = null;
	    subType = null;
	    name = null;
	    data = null;
	    path = null;
	    ref = null;
	}

	/**
	 * Create an ExperimentAspectDB with the naming parameters set.
	 * @param t the type
	 * @param st the subtype
	 * @param n the name
	 */
	public ExperimentAspectDB(String t, String st, String n) {
	    this();
	    type = t;
	    subType = st;
	    name = n;
	}

	/**
	 * Return the type.
	 * @return the type.
	 */
	public String getType() { return type; }
	/**
	 * Set the type
	 * @param t the new type
	 */
	public void setType(String t) { type = t; }
	/**
	 * Return the subtype.
	 * @return the subtype.
	 */
	public String getSubType() { return subType; }
	/**
	 * Set the subtype
	 * @param t the new subtype
	 */
	public void setSubType(String t) { subType = t; }
	/**
	 * Return the name.
	 * @return the name.
	 */
	public String getName() { return name; }
	/**
	 * Set the name
	 * @param n the new name
	 */
	public void setName(String n) { name = n; }
	/**
	 * Return the data reference (if any).
	 * @return the data reference (if any).
	 */
	public String getReference() { return ref; }
	/**
	 * Set the data reference
	 * @param r the new data reference
	 */
	public void setReference(String r) { ref = r; }
	/**
	 * Return the path (if any).
	 * @return the path (if any).
	 */
	public String getPath() { return path; }
	/**
	 * Set the path
	 * @param p the new path
	 */
	public void setPath(String p) { path = p; }
	/**
	 * Return the data (if any).
	 * @return the data (if any).
	 */
	public byte[] getData() { return data; }
	/**
	 * Set the data
	 * @param d the new data
	 */
	public void setData(byte[] d) { data = d; }

	/**
	 * Write the bytes to a file
	 * @param b the bytes to write
	 * @param file the file to write
	 * @throws IOException if there is a problem writing the file
	 */
	private void writeBytesToFile(byte[] b, File file) throws IOException {
	    FileOutputStream p = new FileOutputStream(file);
	    p.write(b);
	    p.flush();
	    p.close();
	}

	/**
	 * Read the bytes from a file
	 * @param file the file to read
	 * @return the bytes in the file
	 * @throws IOException if there is a problem reading the file
	 */
	private byte[] readBytesFromFile(File file) throws IOException {
	    ByteArrayOutputStream rv = new ByteArrayOutputStream();
	    FileInputStream f = new FileInputStream(file);
	    byte[] buf = new byte[16 * 1024];
	    int r = 0;

	    while ( (r = f.read(buf)) != -1)
		rv.write(buf, 0, r);
	    f.close();
	    rv.close();
	    return rv.toByteArray();
	}

	/**
	 * Convert the members of the aspect into a pathname (from root), create
	 * the path if missing and ensure no other aspect is written there.  If
	 * create is true, an fault is thrown if the file already exists.
	 * @param root the root directory for the path
	 * @param create if true throw a fault if the file exists
	 * @throws IOException if there is an IO problem
	 * @throws DeterFault on all other problems
	 */
	private File makeAspectFile(File root, boolean create)
		throws IOException, DeterFault {
	    File dest = new File(root, getType());

	    if (getSubType() != null)
		dest = new File(dest, getSubType());

	    if ( !dest.exists()) {
		if ( !dest.mkdirs())
		    throw new IOException("Cannot make aspect directory " +
			    dest);
	    }
	    if (getName() == null || getName().isEmpty())
		setName("canonical");
	    // Convert the name (which may have slashes) to a filename
	    String fn = (getName() != null) ? getName() : "default";
	    fn = fn.replaceAll("/", "_");

	    dest = new File(dest, fn);
	    if ( create && dest.exists())
		throw new DeterFault(DeterFault.request,
			"duplicate aspect (multiple unnamed aspects? " +
			"Or inconsistent filesystem/DB pair)");
	    return dest;
	}

	/**
	 * Convert the members of the aspect into a pathname (from root), and
	 * make sure the file exists.
	 * @param root the root directory for the path
	 * @throws IOException if there is an IO problem
	 * @throws DeterFault on all other problems
	 */
	private File getAspectFile(File root)
		throws IOException, DeterFault {
	    File dest = new File(root, getType());

	    if (getSubType() != null)
		dest = new File(dest, getSubType());

	    // Convert the name (which may have slashes) to a filename
	    String fn = (getName() != null) ? getName() : "default";
	    fn = fn.replaceAll("/", "_");

	    dest = new File(dest, fn);
	    if ( !dest.exists())
		throw new DeterFault(DeterFault.request, "no aspect file");
	    return dest;
	}

	/**
	 * Set the members of this aspect from the DB and optionally from the
	 * file system.  If getData is true, and there is data in the file
	 * system, retrieve it as well as the DB-stored elements (everything
	 * else).
	 * @param getData if true, load the data from the filesystem
	 * @throws DeterFault on error.
	 */
	public void load(boolean getData) throws DeterFault {
	    try {
		StringBuilder query = new StringBuilder(
			"SELECT path, ref " +
			"FROM experimentaspects " +
			"WHERE eidx=(SELECT idx FROM experiments WHERE eid=?) "+
			"AND type=? AND name=? ");

		if (getSubType() == null) query.append("AND subtype is NULL");
		else query.append("AND subtype =?");

		PreparedStatement p = getPreparedStatement(query.toString());
		p.setString(1, getEid());
		p.setString(2, getType());
		p.setString(3, getName());
		if ( getSubType() != null )
		    p.setString(4, getSubType());

		ResultSet r = p.executeQuery();
		int rows = 0;
		while (r.next())
		    if (rows++ == 0) {
			setPath(r.getString(1));
			setReference(r.getString(2));
		    }
		    else throw new DeterFault(DeterFault.internal,
			    "Aspect has multiple definitions");
		if ( rows == 0 )
		    throw new DeterFault(DeterFault.internal,
			    "Aspect has no definition");
		if ( !getData) return;
		setData(readBytesFromFile(
			    getAspectFile(getComponentDirectory())));
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal,
			"SQL error retrieving aspect: " +e.getMessage());
	    }
	    catch (IOException e) {
		throw new DeterFault(DeterFault.internal,
			"Error reading aspect file: " + e.getMessage());
	    }
	}

	/**
	 * Store the members in the DB and the filesystem,  The aspect is
	 * overwritten if it exists.
	 * @param putData if true write data to the file system
	 * @param create if true throw an exception if the aspect or the
	 * underlying file exists.
	 * @throws DeterFault on errors
	 */
	public void save(boolean putData, boolean create)
		throws DeterFault {
	    try{
		if (getType() == null )
		    throw new DeterFault(DeterFault.request, "Untyped aspect");

		String dataRef=getReference();
		byte[] data = getData();
		File path = (getPath() != null) ? new File(getPath()) : null;

		if (dataRef == null && data == null)
		    throw new DeterFault(DeterFault.request,
			    "Aspect has no data or reference");

		if (dataRef == null) {
		    if ( path == null)
			path = makeAspectFile(getComponentDirectory(), create);
		    if (putData)
			writeBytesToFile(data, path);
		}
		String where =
			"WHERE eidx=(SELECT idx FROM experiments WHERE eid=?) "+
			"AND type=? AND name=? ";
		if (getSubType() != null )
		    where += "AND subtype = ?";
		else
		    where += "AND subtype IS NULL";

		PreparedStatement p = getPreparedStatement(
			"SELECT idx FROM experimentaspects "+ where);
		p.setString(1, getEid());
		p.setString(2, getType());
		if (getSubType() != null )  {
		    p.setString(3, getSubType());
		    p.setString(4, getName());
		}
		else {
		    p.setString(3, getName());
		}
		ResultSet r = p.executeQuery();
		String q = null;
		int rows = 0;

		while (r.next())
		    rows++;

		// Save the aspect in the DB.  Insert if not present, update if
		// so.
		if (rows == 0) {
		    q = "INSERT INTO experimentaspects " +
			    "(path, ref, eidx, " +
				"type, name, subtype) " +
			    "VALUES (" +
				"?, ?, " +
				"(SELECT idx FROM experiments WHERE eid=?), " +
				"?, ?, ?)";
		}
		else if (rows == 1) {
		    if ( create )
			throw new DeterFault(DeterFault.request,
				"Aspect exists");
		    q = "UPDATE experimentaspects " +
			    "SET path=?, ref=? " + where;
		}
		else {
		    throw new DeterFault(DeterFault.internal,
			    "More than one definition for aspect");
		}

		p = getPreparedStatement(q);
		p.setString(1, (path != null) ? path.toString() : null);
		p.setString(2, dataRef);
		p.setString(3, getEid());
		p.setString(4, getType());
		p.setString(5, getName());
		// If we are inserting or there is a given subtype, put subtype
		// in.  Inserting a NULL works where checking for equality does
		// not.
		if ( getSubType() != null || rows == 0)
		    p.setString(6, getSubType());
		p.executeUpdate();
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal,
			"SQL error saving aspect: " +e.getMessage());
	    }
	    catch (IOException e) {
		throw new DeterFault(DeterFault.internal,
			"Error saving aspect file: " + e.getMessage());
	    }
	}

	/**
	 * Remove the Aspect from the DB and filesystem.
	 * @throws DeterFault on errors
	 */
	public void remove() throws DeterFault {
	    if (getEid() == null )
	    	throw new DeterFault(DeterFault.request, 
	    			"Aspect not in a named experiment??");
	    if (getName() == null )
	    	throw new DeterFault(DeterFault.request, "Unnamed aspect");
	    if (getType() == null )
	    	throw new DeterFault(DeterFault.request, "Untyped aspect");

	    PreparedStatement p = null;
	    try {
		if ( getSubType() == null ) {
		    p = getPreparedStatement(
			    "SELECT path FROM experimentaspects "+
			    "WHERE eidx="+
				"(SELECT idx FROM experiments WHERE eid=?) "+
			    "AND type=? AND name=? AND path IS NOT NULL");
		    p.setString(1, getEid());
		    p.setString(2, getType());
		    p.setString(3, getName());
		}
		else {
		    p = getPreparedStatement(
			    "SELECT path FROM experimentaspects "+
			    "WHERE eidx="+
				"(SELECT idx FROM experiments WHERE eid=?) "+
			    "AND type=? AND name=? AND subtype= ? " +
			    "AND path IS NOT NULL");
		    p.setString(1, getEid());
		    p.setString(2, getType());
		    p.setString(3, getName());
		    p.setString(4, getSubType());
		}
		ResultSet r = p.executeQuery();
		p.close();

		/* Delete the files associated with this aspect */
		while (r.next()) {
		    File path = new File(r.getString(1));
		    path.delete();
		}

		/* Delete this aspect from the DB */
		if ( getSubType() == null ) {
		    p = getPreparedStatement(
			    "DELETE FROM experimentaspects " +
			    "WHERE eidx="+
				"(SELECT idx FROM experiments WHERE eid=?) "+
			    "AND type=? AND name=?");
		    p.setString(1, getEid());
		    p.setString(2, getType());
		    p.setString(3, getName());
		}
		else {
		    p = getPreparedStatement(
			    "DELETE FROM experimentaspects " +
			    "WHERE eidx="+
				"(SELECT idx FROM experiments WHERE eid=?) "+
			    "AND type=? AND name=? AND subtype = ?");
		    p.setString(1, getEid());
		    p.setString(2, getType());
		    p.setString(3, getName());
		    p.setString(4, getSubType());
		}
		p.executeUpdate();
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal,
			"SQL error removing aspect: " +e.getMessage());
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
	 * Return this ExperimentAspectDB as an ExperimentAspect, suitable for
	 * passing out through the API.
	 * @return this ExperimentAspectDB as an ExperimentAspect
	 */
	public ExperimentAspect export() {
	    ExperimentAspect rv = new ExperimentAspect();
	    rv.setType(getType());
	    rv.setSubType(getSubType());
	    rv.setName(getName());
	    rv.setData(getData());
	    rv.setDataReference(getReference());
	    return rv;
	}
    }

    /**
     * Base constructor.  Needed to allow nameless ExperimentDBs and to keep
     * one call to the superclass constructor.
     * @throws DeterFault if there is a DB setup error.
     */
    protected ExperimentDB(SharedConnection sc) throws DeterFault {
	super("experiment", "experimentperms", "eidx", "experiments",
		"eid", sc);
    }

    /**
     * Create a nameless ExperimentDB.
     * @throws DeterFault if there is a DB setup error.
     */
    public ExperimentDB() throws DeterFault {
	this((SharedConnection) null);
    }

    /**
     * Create an experimentDB with the given eid.
     * @param e the eid
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error.
     */
    public ExperimentDB(String e) throws DeterFault {
	this(e, null);
    }

    /**
     * Create an experimentDB with the given eid that shares a DB connection
     * @param e the eid
     * @param sc the shared connection
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error.
     */
    public ExperimentDB(String e, SharedConnection sc) throws DeterFault {
	this(sc);
	try {
	    setEid(e);
	}
	catch (DeterFault df) {
	    forceClose();
	    throw df;
	}
    }

    /**
     * Let the ACLObject class know the ID
     * @return the eid
     */
    protected String getID() { return getEid(); }

    /**
     * Return the eid.
     * @return the eid.
     */
    public String getEid() { return eid; }

    /**
     * Set a new eid
     * @param e the eid.
     * @throws DeterFault if the name is badly formatted
     */
    public void setEid(String e) throws DeterFault {
	checkScopedName(e);
	eid = e;
    }

    /**
     * Return true if the experiment exists.
     * @return true if the experiment exists.
     * @throws DeterFault if there is a DB error
     */
    public boolean isValid() throws DeterFault {
	PreparedStatement p = null;
	int count = 0;

	if ( getEid() == null ) return false;

	try {
	    p = getPreparedStatement(
		    "SELECT count(*) FROM experiments WHERE eid=?");
	    p.setString(1, getEid());
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

    /**
     * Remove the contents of the directory root and then the directory itself.
     * No errors are reported.
     * @param root the directory to remove.
     */
    private void cleanDir(File root) {
	if ( root.isDirectory()) {
	    File[] contents = root.listFiles();
	    for (File c : contents != null ? contents : new File[0]) 
		cleanDir(c);
	}
	root.delete();
    }

    /**
     * Functor to call an Aspect method in the context of processAspects,
     * below.
     */
    static private abstract class AspectOperation {
	/**
	 * Call an operation on an Aspect.
	 * @param asp the Aspect
	 * @param e the experiment on which the operation is carried out
	 * @param tid transaction ID under which the operation proceeds
	 * @param a the input ExperimentAspect
	 * @return the vetted ExperimentAspects
	 * @throws DeterFault on trouble
	 */
	public abstract Collection<ExperimentAspect> call(Aspect asp,
		ImmutableExperimentDB e, long tid, ExperimentAspect a)
	    throws DeterFault;
    }

    /**
     * Call the given operation on the array of ExperimentAspects (Aspect
     * descriptors, basically).  A transaction surrounds the operations .The
     * factory, experiment and tranasaction ID are all passed through to the
     * function.  This encapsulates a fairly intricate pair of loops to
     * reasonably deal with the possibilities of errors in the Aspects during
     * the transaction.
     * @param aspects the ExperimentAspects to process
     * @param aspectFactory a factory to generate Aspect objects
     * @param me the ImmutableExperiment on which the loops operate
     * @param fcn the operation to carry out
     * @return a Collection of aspects on which the operation will really be
     *	    carried out.
     * @throws DeterFault on errors. The routine is careful to clean up Aspects
     *	    on a thrown fault.
     */
    private Collection<ExperimentAspect> processAspects(
	    ExperimentAspect[] aspects, AspectFactory aspectFactory,
	    ImmutableExperimentDB me, AspectOperation fcn)
	throws DeterFault {

	List<ExperimentAspect> asps = new ArrayList<ExperimentAspect>();
	Set<String> started = new HashSet<String>();
	DeterFault aspectException = null;
	long tid = aspectFactory.getTransactionID();

	// Walk through the ExperimentAspects passed in, and call fcn on them
	// via Aspect objects.  If an exception is generated anywhere in the
	// loop we still have to finalize any Aspects.  The routine throws the
	// first generated exception, if any.
	try {
	    for (ExperimentAspect a: aspects) {
		String t = a.getType();
		Aspect asp = aspectFactory.getInstance(t);

		if (asp == null )
		    throw new DeterFault(DeterFault.request,
			    "Cannot process aspect " + t);

		if ( !started.contains(t)) {
		    asp.beginTransaction(me, tid);
		    started.add(t);
		}
		asps.addAll(fcn.call(asp, me, tid, a));
	    }
	}
	catch (DeterFault df) {
	    aspectException = df;
	}
	finally {
	    // Tell all the generators that we are done, so they can give
	    // errors if necessary.  Note that we call all the
	    // finalizeTransaction routines, even if one fails.
	    for (String t: started ) {
		try {
		    Aspect asp = aspectFactory.getInstance(t);
		    asp.finalizeTransaction(me, tid);
		}
		catch (DeterFault df) {
		    if ( aspectException == null ) aspectException = df;
		}
	    }
	    aspectFactory.releaseTransactionID(tid);
	}
	if ( aspectException != null ) throw aspectException;
	return asps;
    }

    /**
     * Store this experimentDB in the database, including the aspects and
     * credentials.
     * @param owner the experiment owner
     * @param aspects the aspects
     * @param inAcl the access control list
     * @throws DeterFault on errors
     */
    public void create(String owner, ExperimentAspect[] aspects,
	    Collection<AccessMember> inAcl) throws DeterFault {
	Config config = new Config();
	PreparedStatement p = null;
	List<AccessMember> acl = new ArrayList<AccessMember>(inAcl);
	Collection<ExperimentAspect> asps = null;
	ImmutableExperimentDB me = new ImmutableExperimentDB(this);
	AspectFactory aspectFactory = new AspectFactory();

	if ( owner == null )
	    throw new DeterFault(DeterFault.request, "No owner given");

	if (aspects == null)
	    throw new DeterFault(DeterFault.request, "No aspects given");

	if ( inAcl == null )
	    throw new DeterFault(DeterFault.request, "No acl given");

	if ( getEid() == null ) 
	    throw new DeterFault(DeterFault.request,
		    "Cannot create experiment with no name");

	if ( config.getExperimentRoot() == null) 
	    throw new DeterFault(DeterFault.internal,
		    "no experiment root directory");

	/*if ( aspectFactory == null )
	    throw new DeterFault(DeterFault.internal,
		    "Cannot create aspectFactory?!");*/

	asps = processAspects(aspects, aspectFactory, me,
		new AspectOperation() {
		    public Collection<ExperimentAspect> call(Aspect asp,
			    ImmutableExperimentDB e, long tid,
			    ExperimentAspect a) throws DeterFault {
			return asp.addAspect(e, tid, a);
		    }
		});

	try {
	    File root = new File(config.getExperimentRoot(), getEid());
	    p = getPreparedStatement(
		    "INSERT INTO experiments (eid, owneridx, compdir) " +
		    "VALUES(?, (SELECT idx FROM users WHERE uid=?), ?)");
	    p.setString(1, getEid());
	    p.setString(2, owner);
	    p.setString(3, root.getAbsolutePath());
	    p.executeUpdate();
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    // Creating this record can only really violate the unique eid
	    // constraint.
	    throw new DeterFault(DeterFault.request, "Experiment exists");
	}
	catch (SQLException e) {
	    // Some other SQL problem.  Punt.
	    throw new DeterFault(DeterFault.internal,
		    "Database error creating experiment: " + e);
	}

	// Really create the rest of the experiment.  From here on out, if
	// there are problems we wipe the partial experiment.
	try {
	    // Assign permissions
	    for (AccessMember m : acl )
		assignPermissions(m);

	    // Add aspects
	    for (ExperimentAspect a : asps) {
		ExperimentAspectDB adb = new ExperimentAspectDB(a.getType(),
			a.getSubType(), a.getName());

		adb.setData(a.getData());
		adb.setReference(a.getDataReference());
		adb.save(true, true);
	    }
	}
	catch (DeterFault df) {
	    // Writing an ExperimentDB failed.  Remove the database entries
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
     * Return the base directory for files associated with this experiment.
     * It will only be null if the DB entries are messed up.
     * @return the base directory for files associated with this experiment.
     * @throws DeterFault on errors
     */
    public File getComponentDirectory() throws DeterFault {
	if ( getEid() == null ) 
	    throw new DeterFault(DeterFault.request,
		    "Cannot get configuration directory for " +
		    "experiment with no name");

	PreparedStatement p = null;
	String rv = null;
	try {
	    p = getPreparedStatement(
		    "SELECT compdir FROM experiments WHERE eid = ?");
	    p.setString(1, getEid());
	    ResultSet r = p.executeQuery();

	    int count = 0;
	    while (r.next()) {
		rv = r.getString(1);
		if ( count++ > 0 ) 
		    throw new DeterFault(DeterFault.internal,
			    "More than one match for eid? " + getEid());
	    }
	    return (rv != null) ? new File(rv) : null;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error getting config. directory: " + e);
	}
    }

    /**
     * Get the experiment's owner
     * @return the experiment's owner
     * @throws DeterFault if there is an error
     */
    public String getOwner() throws DeterFault {
	String rv = null;

	if ( getEid() == null )
	    throw new DeterFault(DeterFault.internal,
		    "getOwner failed. Experiment does not have a name");

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM experiments LEFT JOIN users AS u " +
			"ON u.idx = owneridx " +
		    "WHERE eid=?");
	    p.setString(1, getEid());
	    ResultSet r = p.executeQuery();
	    int rows = 0;
	    while (r.next())
		if (rows++ == 0) rv= r.getString(1);
		else throw new DeterFault(DeterFault.internal,
			"More than one owner for experiment??");

	    if (rv == null)
		throw new DeterFault(DeterFault.internal,
			"No owner for experiment??");
	     return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Set the experiment's owner to the new uid.
     * @param o the new onwer's uid
     * @throws DeterFault if there is a DB problem
     */
    public void setOwner(String o) throws DeterFault {

	if ( getEid() == null )
	    throw new DeterFault(DeterFault.internal,
		    "setOwner failed. Experiment does not have a name");
	if ( o  == null )
	    throw new DeterFault(DeterFault.request,
		    "setOwner failed. No owner provided");
	String oldOwner = getOwner();
	try {
	    PreparedStatement p = getPreparedStatement(
		    "UPDATE experiments " +
		    "SET owneridx = (SELECT idx FROM users WHERE uid = ?) " +
		    "WHERE eid=?");
	    p.setString(1, o);
	    p.setString(2, getEid());
	    p.executeUpdate();
	    updateOwnerCredentials(oldOwner, o);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Get the aspects as ExperimentAspects.
     * @param getData if true load the full data into the returned aspects
     * @return the aspects
     * @throws DeterFault if there is an error
     */
    public List<ExperimentAspect> getAspects(boolean getData)
	    throws DeterFault {
	List<ExperimentAspect> rv = new ArrayList<ExperimentAspect>();

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT type, subtype, name FROM experimentaspects "+
		    "WHERE eidx=(SELECT idx FROM experiments WHERE eid=?)");
	    p.setString(1, getEid());
	    ResultSet r = p.executeQuery();

	    while (r.next()) {
		ExperimentAspectDB edb = new ExperimentAspectDB(
			r.getString(1), r.getString(2), r.getString(3));
		edb.load(getData);
		rv.add(edb.export());
	    }
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "SQLException gathering aspects: " + e.getMessage());
	}
    }

    /**
     * Get some aspects as ExperimentAspectDBs (used internally by other aspect
     * manipulators).  The aspects to fetch are encoded as ExperimentAspects
     * with names, types and subtypes to match.  A null name or type matches
     * any name or type.  A null subtype denotes an aspect without a subtype
     * and the subtype "*" matches any subtype including aspects without
     * subtypes.
     * @param aspects ExperimentAspects encoding the aspects to return.
     * @param getData if true load the full data into the returned aspects
     * @return the aspects
     * @throws DeterFault if there is an error
     */
    protected List<ExperimentAspectDB> gatherAspects(
	    Collection<ExperimentAspect> aspects, boolean getData)
	throws DeterFault {
	PreparedStatement p = null;
	List<ExperimentAspectDB> rv = new ArrayList<ExperimentAspectDB>();
	String [] tables = new String[] {
	    "all_fields", "name_type", "name_type_star", "name", "type_subtype",
	    "type", "type_star",
	};
	String [] tableDefs = new String[] {
	    "CREATE TEMPORARY TABLE all_fields " +
		"(name varchar(1024), type varchar(256), "+
		"subtype varchar(256))",
	    "CREATE TEMPORARY TABLE name_type " +
		"(name varchar(1024), type varchar(256))",
	    "CREATE TEMPORARY TABLE name_type_star " +
		"(name varchar(1024), type varchar(256))",
	    "CREATE TEMPORARY TABLE name (name varchar(1024))",
	    "CREATE TEMPORARY TABLE type_subtype " +
		"(type varchar(256), subtype varchar(256))",
	    "CREATE TEMPORARY TABLE type (type varchar(256))",
	    "CREATE TEMPORARY TABLE type_star (type varchar(256))",
	};


	try {

	    // Create tempory tables that encode the aspects to find
	    for (String tDef : tableDefs) {
		p = getPreparedStatement(tDef);
		p.executeUpdate();
	    }

	    // Put each query aspect into the proper temporary table
	    for (ExperimentAspect a : aspects) {
		String name = a.getName();
		String type = a.getType();
		String subType  = a.getSubType();


		if ( name != null && type != null && subType != null ) {
		    if ( subType.equals("*")) {
			p = getPreparedStatement(
				"INSERT INTO name_type_star " +
				    "(name, type) VALUES (?, ?)");
			p.setString(1, name);
			p.setString(2, type);
		    } else {
			p = getPreparedStatement(
				"INSERT INTO all_fields " +
				    "(name, type, subtype) VALUES (?, ?, ?)");
			p.setString(1, name);
			p.setString(2, type);
			p.setString(3, subType);
		    }
		} else if (name != null && type != null ) {
			p = getPreparedStatement(
				"INSERT INTO name_type " +
				    "(name, type) VALUES (?, ?)");
			p.setString(1, name);
			p.setString(2, type);
		} else if ( name != null &&  type == null ) {
			p = getPreparedStatement(
				"INSERT INTO name " +
				    "(name) VALUES (?)");
			p.setString(1, name);
		} else if ( name == null && type != null ) {
		    if ( subType == null ) {
			p = getPreparedStatement(
				"INSERT INTO type (type) VALUES (?)");
			p.setString(1, type);
		    } else if ( subType.equals("*") ) {
			p = getPreparedStatement(
				"INSERT INTO type_star (type) VALUES (?)");
			p.setString(1, type);
		    } else {
			p = getPreparedStatement(
				"INSERT INTO type_subtype " +
				    "(type, subtype) VALUES (?, ?)");
			p.setString(1, type);
			p.setString(2, subType);
		    }
		} else {
		    throw new DeterFault(DeterFault.request,
			    "Bad search aspect");
		}
		p.executeUpdate();
		p.close();
	    }

	    // The actual aspect query
	    p = getPreparedStatement(
		    "SELECT type, subtype, name FROM experimentaspects "+
			"WHERE eidx=(SELECT idx FROM experiments WHERE eid=?) "+
			"AND " +
			"(((name, type, subtype) IN " +
			    "(SELECT name, type, subtype FROM all_fields)) OR "+
			"((name, type) IN "+
			    "(SELECT name, type FROM name_type) "+
			    "AND subtype is NULL) OR "+
			"((name, type) IN " +
			    "(SELECT name, type FROM name_type_star)) OR "+
			"((name) IN " +
			    "(SELECT name FROM name)) OR "+
			"((type, subtype) IN " +
			    "(SELECT type, subtype FROM type_subtype)) OR "+
			"((type) IN " +
			    "(SELECT type FROM type) "+
			    "AND subtype IS NULL) OR "+
			"((type) IN "+
			    "(SELECT type FROM type_star)))");
	    p.setString(1, getEid());
	    ResultSet r = p.executeQuery();

	    // Gather data if requested
	    while (r.next()) {
		ExperimentAspectDB edb = new ExperimentAspectDB(
			r.getString(1), r.getString(2), r.getString(3));
		edb.load(getData);
		rv.add(edb);
	    }
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "SQLException gathering aspects: " + e.getMessage());
	}
	finally {
	    // Drop temporaries no matter what
	    for ( String t : tables ) {
		try {
		    p = getPreparedStatement("DROP TABLE " + t);
		    p.executeUpdate();
		}
		catch (SQLException ignored) { }
	    }
	}
    }

    /**
     * Get some aspects as ExperimentAspects.  The aspects to fetch are encoded
     * as ExperimentAspects with names, types and subtypes to match.  A null
     * name or type matches any name or type.  A null subtype denotes an aspect
     * without a subtype and the subtype "*" matches any subtype including
     * aspects without subtypes.
     * @param aspects ExperimentAspects encoding the aspects to return.
     * @param getData if true load the full data into the returned aspects
     * @return the aspects
     * @throws DeterFault if there is an error
     */
    public List<ExperimentAspect> getAspects(
	    Collection<ExperimentAspect> aspects, boolean getData)
	throws DeterFault {

	List<ExperimentAspect> rv = new ArrayList<ExperimentAspect>();

	for (ExperimentAspectDB edb: gatherAspects(aspects, getData))
	    rv.add(edb.export());
	return rv;
    }

    /**
     * Remove a set of aspects.  The aspects to remove are encoded
     * as ExperimentAspects with names, types and subtypes to match.  A null
     * name or type matches any name or type.  A null subtype denotes an aspect
     * without a subtype and the subtype "*" matches any subtype including
     * aspects without subtypes.
     * @param aspects ExperimentAspects encoding the aspects to remove.
     * @return the aspects that were successfully removed.
     * @throws DeterFault if there is an error
     */
    public List<ChangeResult> removeAspects(
	    Collection<ExperimentAspect> aspects) throws DeterFault {
	List<ChangeResult> rv = new ArrayList<ChangeResult>();
	Collection<ExperimentAspect> asps = null;
	ImmutableExperimentDB me = new ImmutableExperimentDB(this);
	AspectFactory aspectFactory = new AspectFactory();

	asps = processAspects(aspects.toArray(new ExperimentAspect[0]),
		aspectFactory, me,
		new AspectOperation() {
		    public Collection<ExperimentAspect> call(Aspect asp,
			    ImmutableExperimentDB e, long tid,
			    ExperimentAspect a) throws DeterFault {
			return asp.removeAspect(e, tid, a);
		    }
		});
	// Gather up the aspects and remove them.  Successes are returned as
	// the rv.  After the aspects cleared them above, all should succeed.
	for (ExperimentAspectDB edb: gatherAspects(asps, false)) {
	    try {
		//ExperimentAspect e = edb.export();
		edb.remove();
		rv.add(new ChangeResult(edb.getName(), null, true));
	    }
	    catch (DeterFault df) {
		rv.add(new ChangeResult(edb.getName(), df.getDetailMessage(),
			    false));
	    }
	}
	return rv;
    }

    /**
     * Add a set of aspects.  The aspects to add are passed as
     * as ExperimentAspects.  Adding some aspects may result in subaspects
     * appearing as well.
     * @param aspects ExperimentAspects to add
     * @return the aspects that were successfully added.
     * @throws DeterFault if there is an error
     */
    public List<ChangeResult> addAspects(
	    Collection<ExperimentAspect> aspects) throws DeterFault {
	List<ChangeResult> rv = new ArrayList<ChangeResult>();
	Collection<ExperimentAspect> asps = null;
	ImmutableExperimentDB me = new ImmutableExperimentDB(this);
	AspectFactory aspectFactory = new AspectFactory();

	asps = processAspects(aspects.toArray(new ExperimentAspect[0]),
		aspectFactory, me,
		new AspectOperation() {
		    public Collection<ExperimentAspect> call(Aspect asp,
			    ImmutableExperimentDB e, long tid,
			    ExperimentAspect a) throws DeterFault {
			return asp.addAspect(e, tid, a);
		    }
		});

	// Gather up the aspects and add them.  Successes are returned as
	// the rv.  After the aspects cleared them above, all should succeed.
	for (ExperimentAspect a : asps) {
	    ExperimentAspectDB adb = new ExperimentAspectDB(a.getType(),
		    a.getSubType(), a.getName());

	    adb.setData(a.getData());
	    adb.setReference(a.getDataReference());
	    try {
		adb.save(true, true);
		rv.add(new ChangeResult(a.getName(), null, true));
	    }
	    catch (DeterFault df) {
		rv.add(new ChangeResult(a.getName(), df.getDetailMessage(),
			    false));
	    }
	}
	return rv;
    }

    /**
     * Change a set of aspects.  The aspects to add are passed as
     * as ExperimentAspects that contain the Aspect-specific incremental update
     * instructions in the Data or DataReference field.  Changes may may result
     * in subaspects changing as well.
     * @param aspects ExperimentAspects to change
     * @return the aspects that were successfully changed
     * @throws DeterFault if there is an error
     */
    public List<ChangeResult> changeAspects(
	    Collection<ExperimentAspect> aspects) throws DeterFault {
	List<ChangeResult> rv = new ArrayList<ChangeResult>();
	Collection<ExperimentAspect> asps = null;
	ImmutableExperimentDB me = new ImmutableExperimentDB(this);
	AspectFactory aspectFactory = new AspectFactory();

	asps = processAspects(aspects.toArray(new ExperimentAspect[0]),
		aspectFactory, me,
		new AspectOperation() {
		    public Collection<ExperimentAspect> call(Aspect asp,
			    ImmutableExperimentDB e, long tid,
			    ExperimentAspect a) throws DeterFault {
			return asp.changeAspect(e, tid, a);
		    }
		});

	// Gather up the aspects and overwrite them.  Successes are returned as
	// the rv.  After the aspects cleared them above, all should succeed.
	for (ExperimentAspect a : asps) {
	    ExperimentAspectDB adb = new ExperimentAspectDB(a.getType(),
		    a.getSubType(), a.getName());

	    adb.setData(a.getData());
	    adb.setReference(a.getDataReference());
	    try {
		adb.save(true, false);
		rv.add(new ChangeResult(a.getName(), null, true));
	    }
	    catch (DeterFault df) {
		rv.add(new ChangeResult(a.getName(), df.getDetailMessage(),
			    false));
	    }
	}
	return rv;
    }

    /**
     * Operator that collects a TopologyDescription from the aspects of this
     * experiments.
     */
    static private class RealizeOperation extends AspectOperation {
	/** The description being collected. */
	TopologyDescription td;

	/**
	 * New Realize operation.
	 * @param top the initial description
	 */
	public RealizeOperation(TopologyDescription top) { td = top; }

	/**
	 * Return the current collected description.
	 * @return the current collected description.
	 */
	public TopologyDescription getRealizationDescription() { return td; }

	/**
	 * Call realize aspect on an aspect.  If the aspect updates the
	 * collected description, update the collected description and omit the
	 * aspect from the return value.  If the aspect does not update
	 * anything, add it to the return value.  Consensus is achieved when
	 * all the aspects do not change the description.
	 * @param asp the Aspect
	 * @param e the experiment on which the operation is carried out
	 * @param tid transaction ID under which the operation proceeds
	 * @param a the input ExperimentAspect
	 * @return the vetted ExperimentAspects
	 * @throws DeterFault on trouble
	 */
	public Collection<ExperimentAspect> call(Aspect asp,
		ImmutableExperimentDB e, long tid, ExperimentAspect a)
	    throws DeterFault {
	    TopologyDescription tdd = null;
	    List<ExperimentAspect> rv = new ArrayList<>();

	    if ( (tdd = asp.realizeAspect(e, tid, a, td)) != null) {
		td = tdd;
		return rv;
	    }
	    else {
		rv.add(a);
		return rv;
	    }
	}
    }

    /**
     * Gather the realization topology from the aspects.
     * @return the consensus description
     * @throws DeterFault if the routine cannot achieve consensus
     */
    public TopologyDescription realizeAspects() throws DeterFault {
	Collection<ExperimentAspect> aspects = getAspects(false);
	Collection<ExperimentAspect> asps = new ArrayList<>();
	ImmutableExperimentDB me = new ImmutableExperimentDB(this);
	AspectFactory aspectFactory = new AspectFactory();
	TopologyDescription rv = null;
	RealizeOperation relOp = new RealizeOperation(rv);
	int tries = 0;
	final int maxTries = 5;

	while ( asps.size() < aspects.size() ) {
	    if ( tries > maxTries )
		throw new DeterFault(DeterFault.request,
			"Cannot realize experiment - " +
			"too many attempts w/o progress");

	    asps = processAspects(aspects.toArray(new ExperimentAspect[0]),
		    aspectFactory, me, relOp);
	    rv = relOp.getRealizationDescription();
	    tries++;
	}
	return rv;
    }

    /**
     * Release resources from the associated aspects.  The releaseAspect method
     * is called on each, before the resources actually are reclaimed.
     * @throws DeterFault if there is an error
     */
    public void releaseAspects() throws DeterFault {
	Collection<ExperimentAspect> aspects = getAspects(false);
	ImmutableExperimentDB me = new ImmutableExperimentDB(this);
	AspectFactory aspectFactory = new AspectFactory();

	processAspects(aspects.toArray(new ExperimentAspect[0]),
		aspectFactory, me,
		new AspectOperation() {
		    public Collection<ExperimentAspect> call(Aspect asp,
			    ImmutableExperimentDB e, long tid,
			    ExperimentAspect a) throws DeterFault {
			asp.releaseAspect(e, tid, a);
			return new ArrayList<>();
		    }
		});
    }

    /**
     * Remove the experiment from the database and remove config files stored
     * for it.  Errors from the database cause faults to be thrown.
     * @throws DeterFault on error
     */
    public void remove() throws DeterFault {
	PreparedStatement p = null;
	File root = null;

	if ( getEid() == null ) 
	    throw new DeterFault(DeterFault.request,
		    "Cannot remove experiment with no name");

	// get the root if possible, but if it fails, keep going.
	try { root = getComponentDirectory(); }
	catch (DeterFault ignored) { }

	if ( root != null) cleanDir(root);

	// Get all of the credentials out of the DB and cache
	removeCredentials();
	try {
	    p = getPreparedStatement("DELETE FROM librarymembers " +
		    "WHERE eidx=(SELECT idx FROM experiments WHERE eid=?)");
	    p.setString(1, getEid());
	    p.executeUpdate();
	    p = getPreparedStatement( "DELETE FROM experimentaspects " +
		    "WHERE eidx=(SELECT idx FROM experiments WHERE eid=?)");
	    p.setString(1, getEid());
	    p.executeUpdate();
	    p = getPreparedStatement( "DELETE FROM experimentperms " +
		    "WHERE eidx=(SELECT idx FROM experiments WHERE eid=?)");
	    p.setString(1, getEid());
	    p.executeUpdate();
	    p = getPreparedStatement( "DELETE FROM experiments WHERE eid=?");
	    p.setString(1, getEid());
	    int ndel = p.executeUpdate();

	    if ( ndel == 0 )
		throw new DeterFault(DeterFault.request, "No such experiment");
	    else if ( ndel > 1 )
		throw new DeterFault(DeterFault.internal,
			"Multiple experiments deleted??");

	    if ( root != null && root.exists() )
		throw new DeterFault(DeterFault.internal,
			"Could not completely delete configuration directory");
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error removing experiment: " + e);
	}
    }

    /**
     * Return ExperimentDBs readable by the given user, that match regex and
     * that are in library.  Any of the parameters can be null.  All the
     * returned ExperimentDB's share the given connection.
     * @param uid the user to match
     * @param lib the library to match
     * @param regex the name regex
     * @param offset the index of the first experiment description to return
     * @param count the number of descriptions to return
     * @param sc the shared connection to use
     * @return a collection of ExperimentDBs
     * @throws DeterFault on error
     */
    static public List<ExperimentDB> getExperiments(String uid,
	    String lib, String regex, int offset, int count,
	    SharedConnection sc) throws DeterFault {
	StringBuilder query = new StringBuilder("SELECT eid FROM experiments ");
	Connection c = null;
	boolean whereAdded = false;
	int sqlIdx = 1;
	List<ExperimentDB> rv = new ArrayList<ExperimentDB>();

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
		// Pretty complex.  Get the experiment indices from
		// experimentperms entries where the permission index matches
		// EXPERIMENT_READ and the cidx is a circle that has uid as a
		// member.
		query.append(
			"(idx IN "+
			    "(SELECT DISTINCT eidx FROM experimentperms " +
			    "WHERE permidx=(" +
				"SELECT idx FROM permissions "+
				"WHERE name='READ_EXPERIMENT' and " +
				    "valid_for='experiment') " +
			    "AND cidx IN (" +
				"SELECT DISTINCT cidx FROM circleusers " +
				"WHERE uidx=(SELECT idx FROM users " +
				    "WHERE uid=?))"+
			") OR owneridx=(SELECT idx FROM users WHERE uid=?)) ");
	    }
	    if ( lib != null ) {
		if ( whereAdded ) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		query.append(
			"idx IN (SELECT eidx FROM librarymembers " +
			    "WHERE lidx=" +
				"(SELECT idx FROM libraries WHERE libid=?)) ");
	    }

	    if ( regex != null ) {
		if (whereAdded) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		// Nice and simple.
		query.append("eid REGEXP ? ");
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
	    if ( lib != null) p.setString(sqlIdx++, lib);
	    if ( regex != null) p.setString(sqlIdx++, regex);
	    if ( count  != -1 ) {
		p.setInt(sqlIdx++, count);
		p.setInt(sqlIdx++, offset);
	    }

	    ResultSet r = p.executeQuery();
	    while (r.next())
		rv.add(new ExperimentDB(r.getString(1), sc));
	    sc.close();
	    return rv;
	}
	catch (SQLException e) {
	    for (ExperimentDB exp : rv)
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
