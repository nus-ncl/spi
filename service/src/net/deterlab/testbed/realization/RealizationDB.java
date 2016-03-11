package net.deterlab.testbed.realization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.db.ACLObject;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.topology.TopologyDescription;
import net.deterlab.testbed.topology.TopologyException;

/**
 * Realization information in the Database
 * @author DETER team
 * @version 1.1
 */

public class RealizationDB extends ACLObject {
    /** The name of the realization */
    private String name;
    /** type */
    private File topoFile;
    /** The  circle realized under */
    private String cid;
    /** The experiment being realized */
    private String eid;
    /** The current realization status */
    private String status;
    /** The creator uid */
    private String creator;
    /** The topologyDescription stored in topofile. */
    private TopologyDescription topo;
    /** The embedder used to create the experimenter */
    String embedder;
    /** The resource to resource mappings.  Maps outer to inner*/
    private Map<String, Set<String>> contains;
    /** The topology element to resource(s) map */
    private Map<String, Set<String>> toTopo;

    /**
     * Base constructor.  
     * @param sc a DB connection to share
     * @throws DeterFault if there is a DB setup error.
     */
    public RealizationDB(SharedConnection sc) throws DeterFault {
	super("realization", "realizationperms", "ridx", "realizations",
		"name", sc);
	name = null;
	topoFile = null;
	cid = null;
	eid = null;
	creator = null;
	status = null;
	topo = null;
	embedder = null;
	contains = new HashMap<>();
	toTopo = new HashMap<>();
    }

    /**
     * Create a nameless RealizationDB.
     * @throws DeterFault if there is a DB setup error.
     */
    public RealizationDB() throws DeterFault {
	this((SharedConnection) null);
    }


    /**
     * Let the ACLObject class know the ID
     * @return the ID
     */
    protected String getID() { return getName(); }

    /**
     * Return the name.
     * @return the name.
     */
    public String getName() { return name; }

    /**
     * Set the name
     * @param n the new name
     * create in its namespace.
     */
    public void setName(String n) { name = n; }

    /**
     * Return the type.
     * @return the type.
     */
    public File getTopoFile() { return topoFile; }

    /**
     * Return the circle id
     * @return the circle id
     */
    public String getCircleID() { return cid; } 

    /**
     * Return the experiment ID.
     * @return the experiment ID.
     */
    public String getExperimentID() { return eid; }

    /**
     * Return the uid of the creator.
     * @return the uid of the creator.
     */
    public String getCreator() { return creator; }

    /**
     * Set the uid of the owner
     * @param uid the new uid
     */
    public void setCreator(String uid) { creator = uid; }

    /**
     * Return the status.
     * @return the status.
     */
    public String getStatus() { return status; }

    /**
     * Set the status
     * @param s the new status
     */
    public void setStatus(String s) { status = s; }

    /**
     * Return the embedder name.
     * @return the embedder name.
     */
    public String getEmbedderName() { return embedder; }

    /**
     * Set the embedder name
     * @param e the new embedder name
     */
    public void setEmbedderName(String e) { embedder = e; }

    /**
     * Return the topology element-to resources mapping
     * @return the mapping
     */
    public Map<String, Set<String>> getMapping() { return toTopo; }

    /**
     * Set the topology element to resources mapping.
     * @param m the new mapping
     */
    public void setMapping(Map<String, Set<String>> m) {
	toTopo = new HashMap<String, Set<String>>(m);
    }

    /**
     * Add an entry to the topology element to resources map
     * @param e the element to map
     * @param r the resource to map
     */
    public void addMappingEntry(String e, String r) {
	if ( !toTopo.containsKey(e))
	    toTopo.put(e, new HashSet<String>());
	toTopo.get(e).add(r);
    }

    /**
     * Return the resource containment mapping
     * @return the mapping
     */
    public Map<String, Set<String>> getContainment() { return contains; }

    /**
     * Set the resource contaiment mapping.
     * @param m the new mapping
     */
    public void setContainment(Map<String, Set<String>> m) {
	contains = new HashMap<String, Set<String>>(m);
    }

    /**
     * Add an entry to the resource containment
     * @param outer the outer resource
     * @param inner the inner resource
     */
    public void addContainmentEntry(String outer, String inner) {
	if ( !contains.containsKey(outer))
	    contains.put(outer, new HashSet<String>());
	contains.get(outer).add(inner);
    }

    /**
     * Return the topology associated with this realization.
     * @return the topology associated with this realization.
     * @throws DeterFault if a topology muct be generated and there is an eror
     * doing so.
     */
    public TopologyDescription getTopology() throws DeterFault {
	if ( topo != null ) return topo;
	else if (topoFile != null) {
	    try {
		return topo = TopologyDescription.xmlToTopology(
			new FileInputStream(topoFile), "topology", true);
	    }
	    catch (TopologyException te) {
		throw new DeterFault(DeterFault.internal,
			"Bad topology: " + te.getMessage());
	    }
	    catch (IOException ie) {
		throw new DeterFault(DeterFault.internal,
			"Cannot recover topology: " + ie.getMessage());
	    }
	}
	else throw new DeterFault(DeterFault.internal,
		"Cannot recover topology - no topolog or file in object");
    }

    /**
     * Return true if the realization exists.
     * @return true if the realization exists.
     * @throws DeterFault if there is a DB error
     */
    public boolean isValid() throws DeterFault {
	PreparedStatement p = null;
	int count = 0;

	if ( getName() == null ) return false;

	try {
	    p = getPreparedStatement(
		    "SELECT count(*) FROM realizations WHERE name=?");
	    p.setString(1, getName());
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
     * Create a realization of the given experiment in the given circle on the
     * topology.  The experiment must have been validated and the aspect system
     * used to create the topology correctly.  Name is picked and resources
     * that are bound to this may be created.
     * @param eName the experiment
     * @param cName the circle
     * @param top the topology to realize, after aspects have made their
     * additions.
     * @param inAcl the permissions to this realization
     * @param uid the owner of the realization - not reassignable
     * @throws DeterFault on errors
     */
    public void create(String eName, String cName, TopologyDescription top,
	    Collection<AccessMember> inAcl, String uid) throws DeterFault {
	Config config = new Config();
	File tdir = new File(config.getProperty("realization_root"));
	PreparedStatement p = null;
	boolean created = false;
	String name = String.format("%s-%s", eName, cName);
	int tries = 0;
	final int lim = 10;

	if (inAcl == null )
	    throw new DeterFault(DeterFault.request, "No ACL to create");

	p = getPreparedStatement(
		"INSERT INTO realizations (name, eidx, cidx, creator) " +
		"VALUES(?, " +
		    "(SELECT idx FROM experiments WHERE eid=?), " +
		    "(SELECT idx FROM circles WHERE circleid=?), ?)");
	while ( !created && tries++ < lim ) {
	    try {
		p.setString(1, name);
		p.setString(2, eName);
		p.setString(3, cName);
		p.setString(4, uid);
		p.executeUpdate();
		created = true;
		setName(name);
		cid = cName;
		eid = eName;
		setCreator(uid);
	    }
	    catch (SQLIntegrityConstraintViolationException e) {
		// Creating this record can only really violate the unique eid
		// constraint.
		if ( tries >= lim ) 
		    throw new DeterFault(DeterFault.request,
			    "Cannot find unique realization name.");
	    }
	    catch (SQLException e) {
		// Some other SQL problem.  Punt.
		throw new DeterFault(DeterFault.internal,
			"Database error creating realization: " + e);
	    }

	    // Try the next name.  Note we fall through to here when
	    // p.executeUpdate() throws an SQLIntegrityException
	    name = String.format("%s-%s-%d", eName, cName,
		    System.currentTimeMillis());
	}
	if (!created)
	    throw new DeterFault(DeterFault.request,
		    "Cannot create realization.");

	// Really create the rest of the realization.  From here on out, if
	// there are problems we wipe the partial resource.
	try {
	    // Load the status from the DB
	    p = getPreparedStatement(
		    "SELECT status FROM realizations WHERE name=?");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();
	    if (!r.next())
		throw new DeterFault(DeterFault.internal,
			"Realization create initally failed!?");
	    status = r.getString(1);
	    if (r.next())
		throw new DeterFault(DeterFault.internal,
			"Realization create initally failed: " +
			"multiple inserts!?");

	    // Assign permissions
	    for (AccessMember m : inAcl )
		assignPermissions(m);

	    if ( top != null) {
		topoFile = File.createTempFile("topo", ".xml", tdir);
		top.writeXML(
			new OutputStreamWriter(new FileOutputStream(topoFile)),
			"experiment");

		p = getPreparedStatement(
			"UPDATE realizations SET topofile=? WHERE name=?");
		p.setString(1, topoFile.getCanonicalPath());
		p.setString(2, getName());
		p.executeUpdate();
	    }

	}
	catch (SQLException se) {
	    // Something went wrong in the DB manipulations. Remove the
	    // database entries (ACLs and definition).
	    try { remove(); }
	    catch (DeterFault ignored) { }
	    throw new DeterFault(DeterFault.internal, se.getMessage());
	}
	catch (IOException ie) {
	    try { remove(); }
	    catch (DeterFault ignored) { }

	    throw new DeterFault(DeterFault.internal,
		    "Cannot save topology: " + ie.getMessage());
	}
	catch (DeterFault df) {
	    // Writing the Realization failed.  Remove the database entries
	    // (ACLs and definition).
	    try { remove(); }
	    catch (DeterFault ignored) { }
	    throw df;
	}

	try {
	    updatePolicyCredentials(uid);
	    updateCircleCredentials();
	    updateOwnerCredentials(null, uid);
	}
	catch (Exception e) {
	    e.printStackTrace();
	    throw e;
	}
    }

    /**
     * Load the realization's content from the DB
     * @throws DeterFault on errors
     */
    public void load() throws DeterFault {
	PreparedStatement p = null;
	try {
	    p = getPreparedStatement(
		    "SELECT circleid, eid, status, topofile, creator, " +
			"embedder " +
		    "FROM realizations AS r LEFT JOIN circles AS c "+
			"ON r.cidx=c.idx " +
		    "LEFT JOIN experiments AS e " +
			"ON r.eidx = e.idx " +
		    "WHERE name=?");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();
	    int rows = 0;

	    while (r.next()) {
		if ( ++rows > 1) throw new DeterFault(DeterFault.internal,
			"More than one definition for realization");
		cid = r.getString(1);
		eid = r.getString(2);
		status = r.getString(3);
		topoFile = (r.getString(4) != null) ?
		    new File(r.getString(4)) : null;
		setCreator(r.getString(5));
		setEmbedderName(r.getString(6));
	    }
	    if (rows == 0 )
		throw new DeterFault(DeterFault.request,
			"No such realization " + getName());

	    p = getPreparedStatement("SELECT o.name, i.name " +
		    "FROM realizationcontainment " +
			"LEFT JOIN resources AS o " +
			    "ON o.idx = outeridx " +
			"LEFT JOIN resources AS i " +
			    "ON i.idx = inneridx " +
		    "WHERE ridx=(SELECT idx FROM realizations WHERE name=?)");
	    p.setString(1, name);
	    r = p.executeQuery();
	    while (r.next()) {
		String outer = r.getString("o.name");
		String inner = r.getString("i.name");

		if ( !contains.containsKey(outer))
		    contains.put(outer, new HashSet<String>());
		contains.get(outer).add(inner);
	    }

	    p = getPreparedStatement("SELECT res.name, ename "+
		    "FROM realizationtopology " +
			"LEFT JOIN resources AS res ON res.idx=residx "+
		    "WHERE ridx=(SELECT idx FROM realizations WHERE name=?)");
	    p.setString(1, name);
	    r = p.executeQuery();
	    while (r.next()) {
		String ename = r.getString("ename");
		String resName = r.getString("res.name");

		if ( !toTopo.containsKey(ename))
		    toTopo.put(ename, new HashSet<String>());
		toTopo.get(ename).add(resName);
	    }
	}
	catch (SQLException e) {
	    if ( getName() != null) {
		// Try to clean up any partial insertion
		try { remove(); } catch (Exception ignored ) { }
	    }
	    throw new DeterFault(DeterFault.internal,
		    "Database error removing resource: " + e);
	}
    }
    /**
     * Store the realization into the DB.
     * @throws DeterFault on errors
     */
    public void save() throws DeterFault {
	PreparedStatement p = null;

	try {

	    if (getName() == null )
		throw new DeterFault(DeterFault.request, "Untyped facet");

	    // Save the realization in the DB.

	    p = getPreparedStatement("UPDATE realizations " +
		    "SET cidx=(SELECT idx from circles WHERE circleid=?), "+
			"eidx=(SELECT idx from experiments WHERE eid=?), "+
			"status=?, " +
			"topofile=?, " +
			"embedder=? "+
		    "WHERE name=?");
	    p.setString(1, cid);
	    p.setString(2, eid);
	    p.setString(3, status);
	    try {
		p.setString(4, (topoFile != null) ?
			topoFile.getCanonicalPath() : null);
	    }
	    catch (IOException ie) {
		p.setString(4, (topoFile != null) ? topoFile.getPath() : null);
	    }
	    p.setString(5, getEmbedderName());
	    p.setString(6, getName());
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "SQL error saving realization: " +e.getMessage());
	}
	// Now save the internals.  If this stuff fails, remove it.
	try {
	    // Remove any containment and topo mapping already there
	    p = getPreparedStatement(
		    "DELETE FROM realizationcontainment " +
		    "WHERE ridx=(SELECT idx FROM realizations WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    p = getPreparedStatement(
		    "DELETE FROM realizationtopology " +
		    "WHERE ridx=(SELECT idx FROM realizations WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    p = getPreparedStatement(
		    "INSERT INTO realizationcontainment " +
			"(ridx, outeridx, inneridx) " +
			"VALUES (" +
			    "(SELECT idx FROM realizations WHERE name=?), " +
			    "(SELECT idx FROM resources WHERE name=?), " +
			    "(SELECT idx FROM resources WHERE name=?))");
	    p.setString(1, getName());
	    for (Map.Entry<String, Set<String>> c: contains.entrySet()) {
		p.setString(2, c.getKey());
		for (String inner : c.getValue()) {
		    p.setString(3, inner);
		    p.executeUpdate();
		}
	    }

	    p = getPreparedStatement(
		    "INSERT INTO realizationtopology " +
			"(ridx, residx, ename) " +
			"VALUES (" +
			    "(SELECT idx FROM realizations WHERE name=?), " +
			    "(SELECT idx FROM resources WHERE name=?), ?)");
	    p.setString(1, getName());
	    for (Map.Entry<String, Set<String>> c: toTopo.entrySet()) {
		p.setString(3, c.getKey());
		for (String resName : c.getValue()) {
		    p.setString(2, resName);
		    p.executeUpdate();
		}
	    }
	}
	catch (SQLException e) {
	    try { remove(); } catch (DeterFault ignored) {}
	    throw new DeterFault(DeterFault.internal,
		    "Could not save realization " + getName());
	}
    }


    /**
     * Remove the experiment from the database and remove config files stored
     * for it.  Errors from the database cause faults to be thrown.
     * @throws DeterFault on error
     */
    public void remove() throws DeterFault {
	PreparedStatement p = null;

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.request,
		    "Cannot remove realization with no name");

	// Get all of the credentials out of the DB and cache
	removeCredentials();
	try {
	    p = getPreparedStatement("SELECT topofile FROM realizations " +
		    "WHERE name=?");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();

	    while (r.next()) {
		String tfn = null;
		File f = null;

		if ( (tfn = r.getString(1)) == null ) continue;
		f = new File(tfn);
		f.delete();
	    }

	    p = getPreparedStatement("DELETE FROM realizationperms " +
		    "WHERE ridx=(SELECT idx FROM realizations WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    p = getPreparedStatement("DELETE FROM realizationtopology " +
		    "WHERE ridx=(SELECT idx FROM realizations WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    p = getPreparedStatement("DELETE FROM realizationcontainment " +
		    "WHERE ridx=(SELECT idx FROM realizations WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();

	    p = getPreparedStatement("DELETE FROM realizations WHERE name=?");
	    p.setString(1, getName());
	    int ndel = p.executeUpdate();

	    if ( ndel == 0 )
		throw new DeterFault(DeterFault.request, "No such realization");
	    else if ( ndel > 1 )
		throw new DeterFault(DeterFault.internal,
			"Multiple realizations deleted??");

	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error removing realization: " + e);
	}
    }

    /**
     * Return RealizationDBs readable by the given user, that match regex.
     * Any of the parameters can be null.  All the returned RealizationDB's
     * share the given connection.
     * @param uid the user to match
     * @param regex the name regex
     * @param offset the index of the first experiment description to return
     * @param count the number of descriptions to return (-1 for all)
     * @param sc the shared connection to use
     * @return a collection of RealizationDBs
     * @throws DeterFault on error
     */
    static public List<RealizationDB> getRealizations(String uid,
	    String regex, int offset, int count,
	    SharedConnection sc)
	throws DeterFault {
	StringBuilder query =
	    new StringBuilder("SELECT name FROM realizations ");
	Connection c = null;
	List<RealizationDB> rv = new ArrayList<>();
	boolean whereAdded = false;
	int sqlIdx = 1;

	if ( sc == null) sc = new SharedConnection();
	try {
	    sc.open();
	    c = sc.getConnection();
	    // XXX: uid?
	    if ( regex != null ) {
		if (whereAdded) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		// Nice and simple.
		query.append("name REGEXP ? ");
	    }

	    if ( count != -1 ) {
		if ( offset < 0) offset = 0;
		query.append("ORDER BY idx LIMIT ? OFFSET ? ");
	    }
	    PreparedStatement p = c.prepareStatement(query.toString());

	    if ( regex != null) p.setString(sqlIdx++, regex);
	    if ( count  != -1 ) {
		p.setInt(sqlIdx++, count);
		p.setInt(sqlIdx++, offset);
	    }

	    ResultSet r = p.executeQuery();
	    while (r.next()) {
		RealizationDB rdb = new RealizationDB(sc);
		rdb.setName(r.getString(1));
		rdb.load();
		rv.add(rdb);
	    }
	    sc.close();
	    return rv;
	}
	catch (SQLException e) {
	    for (RealizationDB r : rv)
		r.forceClose();
	    try {
		// Forceclose might affect others
		sc.close();
	    }
	    catch (DeterFault ignored) { }
	    throw new DeterFault(DeterFault.internal, "SQL Exception: " +e);
	}
    }
}
