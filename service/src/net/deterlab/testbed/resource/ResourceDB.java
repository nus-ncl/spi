package net.deterlab.testbed.resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.ChangeResult;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ResourceFacet;
import net.deterlab.testbed.api.ResourceTag;
import net.deterlab.testbed.db.ACLObject;
import net.deterlab.testbed.db.SharedConnection;

/**
 * Resource information in the Database
 * @author DETER team
 * @version 1.1
 */

public class ResourceDB extends ACLObject {
    /** The name of the resource */
    private String name;
    /** type */
    private String type;
    /** True if the resource persists after the experiment realizations
     * containing it go away. */
    private boolean persist;
    /** The  description*/
    private String description;
    /** Optional data attached to this resource. */
    private byte[] data;
    /** facets attached to this resource */
    private List<ResourceFacetDB> facets;
    /** tags/attributes */
    private Map<String, String> tags;

    /**
     * This is an experiment aspect including both its database and filesystem
     * storage.  This acts as a DBObject, but piggybacks on the shared
     * connection of the enclosing ResourceDB.
     * @author DETER Team
     * @version 1.0
     */
    public class ResourceFacetDB {
	/** name scoped by resource */
	private String name;
	/** type */
	private String type;
	/** value */
	private double value;
	/** Units */
	private String units;
	/** tags/attributes */
	private Map<String, String> tags;

	/**
	 * Create an empty ResourceAspectDB
	 */
	public ResourceFacetDB() {
	    name = null;
	    type = null;
	    value = 0.0;
	    units = null;
	    tags = new HashMap<>();
	}

	/**
	 * Create an ResourceFacetDB with the naming parameters set.
	 * @param n the name
	 * @param t the type
	 * @param v the value
	 * @param u the units
	 */
	public ResourceFacetDB(String n, String t, double v, String u) {
	    this();
	    name = n;
	    type = t;
	    value = v;
	    units = u;
	}

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
	 * Return the value.
	 * @return the value.
	 */
	public double  getValue() { return value; }
	/**
	 * Set the value
	 * @param v the new value
	 */
	public void setValue(double v) { value = v; }
	/**
	 * Return the units.
	 * @return the units.
	 */
	public String getUnits() { return units; }
	/**
	 * Set the units
	 * @param u the new units
	 */
	public void setUnits(String u) { units = u; }

	/**
	 * Return the tag with the given name.
	 * @param n the name to look up
	 * @return the named tag
	 */
	public String getTag(String n) { return tags.get(n); }
	/**
	 * Set the tag with the given name.  If v is null, delete the tag.
	 * @param n the name to assign/delete
	 * @param v the new value
	 */
	public void setTag(String n, String v) {
	    if ( v == null ) tags.remove(n);
	    else tags.put(n, v);
	}
	/**
	 * Set the members of this facet from the DB and tags.
	 * @throws DeterFault on error.
	 */
	public void load() throws DeterFault {
	    try {
		StringBuilder query = new StringBuilder(
			"SELECT name, type, value, units " +
			"FROM facets " +
			"WHERE ridx=(SELECT idx FROM resources WHERE name=?) "+
			    "AND name=?");

		PreparedStatement p = getPreparedStatement(query.toString());
		p.setString(1, ResourceDB.this.getName());
		p.setString(2, getName());

		ResultSet r = p.executeQuery();
		int rows = 0;

		while (r.next()) {
		    if (++rows > 1)
			throw new DeterFault(DeterFault.internal,
				"Facet has multiple definitions");
		    setName(r.getString(1));
		    setType(r.getString(2));
		    setValue(r.getDouble(3));
		    setUnits(r.getString(4));
		}
		if ( rows == 0 )
		    throw new DeterFault(DeterFault.internal,
			    "Facet has no definition");
		p = getPreparedStatement(
			"SELECT name, value FROM facettags " +
			    "WHERE fidx=" +
			    "(SELECT idx FROM facets WHERE name=? AND "+
			    "ridx=(SELECT idx FROM resources WHERE name=?))");
		p.setString(1, getName());
		p.setString(2, ResourceDB.this.getName());
		for (r = p.executeQuery(); r.next(); )
		    setTag(r.getString(1), r.getString(2));

	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal,
			"SQL error retrieving aspect: " +e.getMessage());
	    }
	}

	/**
	 * Return the number of instances of a facet with this name.  This
	 * should only return 0 or 1, but if the world is messier, this will
	 * let you know.
	 * @param n the name to check; If null check this facet.
	 * @return the number of facets with this name
	 * @throws DeterFault if the DB fails us
	 * @throws SQLException if the DB fails us
	 */
	private int facetCount(String n) throws SQLException, DeterFault {
	    PreparedStatement p = null;

	    if ( n == null ) n = getName();

	    p = getPreparedStatement(
		"SELECT COUNT(*) FROM facets " +
		    "WHERE ridx=" +
			"(SELECT idx FROM resources WHERE name=?) " +
		    "AND name=?");
	    p.setString(1, ResourceDB.this.getName());
	    p.setString(2, n);

	    ResultSet r = p.executeQuery();
	    if ( !r.next()) return 0;
	    return r.getInt(1);
	}

	/**
	 * Disambiguate a facet name.  Append an integer to it until the facet
	 * name is unique among this resource's facets.
	 * @param n the name to disambiguate
	 * @return the new name
	 * @throws SQLException if the underlying DB fails us.
	 */
	private String disambiguate(String n) throws SQLException,DeterFault {
	    String nn = n;
	    int i = 0;

	    // As an old C geek, I couldnt resist writing this as a for loop.
	    // Initialize the name to the suggested one, count instances of it,
	    // and each time the loop iterates append the next integer to it.
	    for (nn = n; facetCount(nn) > 0; nn=String.format("%s%d", n, i++))
		;
	    return nn;
	}

	/**
	 * Store the facet.  If create is given, the facet name is
	 * disambiguated to avoid conflicts.  If not, an existing facet with
	 * the same name is overwritten if present.  Unnamed facets are
	 * initially named by their type and potentially disambiguated further.
	 * @param create if true create the facet
	 * @throws DeterFault on errors
	 */
	public void save(boolean create)
		throws DeterFault {
	    try {
		int rows = 0;
		String q = null;
		PreparedStatement p = null;

		if (getType() == null )
		    throw new DeterFault(DeterFault.request, "Untyped facet");
		// Unnamed facets are named for their type.
		if ( getName() == null ) setName(getType());

		if ( create )
		    setName(disambiguate(getName()));
		else
		    rows = facetCount(getName());


		// Save the aspect in the DB.  Insert if not present, update if
		// so.
		if (rows == 0) {
		    q = "INSERT INTO facets " +
			    "(value, units, type, name, ridx) " +
			    "VALUES ( ?, ?, ?, ?, " +
				"(SELECT idx FROM resources WHERE name=?))";
		}
		else if (rows == 1) {
		    if ( create )
			throw new DeterFault(DeterFault.request,
				"facet exists: How'd this happen?");
		    q = "UPDATE facets " +
			    "SET value=?, units=?, type=?, name=? " +
			    "WHERE ridx=" +
				"(SELECT idx FROM resources WHERE name=?) "+
				"AND idx=?";
		}
		else {
		    throw new DeterFault(DeterFault.internal,
			    "More than one definition for aspect");
		}

		p = getPreparedStatement(q);
		p.setDouble(1, getValue());
		p.setString(2, getUnits());
		p.setString(3, getType());
		p.setString(4, getName());
		p.setString(5, ResourceDB.this.getName());
		p.executeUpdate();

		for (Map.Entry<String, String> t : tags.entrySet()) {
		    p = getPreparedStatement(
			    "INSERT INTO facettags (name, value, fidx) " +
			    "VALUES (?, ?, " +
				"(SELECT idx FROM facets WHERE name=? AND "+
				"ridx=(SELECT idx FROM resources " +
				    "WHERE name=?)))");
		    p.setString(1, t.getKey());
		    p.setString(2, t.getValue());
		    p.setString(3, getName());
		    p.setString(4, ResourceDB.this.getName());
		    p.executeUpdate();
		}

	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal,
			"SQL error saving Facet: " +e.getMessage());
	    }
	}

	/**
	 * Remove the facet from the DB and filesystem.
	 * @throws DeterFault on errors
	 */
	public void remove() throws DeterFault {
	    PreparedStatement p = null;

	    if (getName() == null )
		throw new DeterFault(DeterFault.request, "Unnamed facet");
	    if (getType() == null )
		throw new DeterFault(DeterFault.request, "Untyped facet");

	    try {
		p = getPreparedStatement(
			"DELETE FROM facettags " +
			"WHERE fidx IN " +
			"(SELECT idx FROM facets WHERE name=? AND ridx="+
			    "(SELECT idx FROM resources WHERE name = ?))");
		p.setString(1, getName());
		p.setString(2, ResourceDB.this.getName());
		p.executeUpdate();

		p = getPreparedStatement(
			"DELETE FROM facets " +
			"WHERE ridx="+
			    "(SELECT idx FROM resources WHERE name=?) "+
			"AND name=?");
		p.setString(1, ResourceDB.this.getName());
		p.setString(2, getName());
		p.executeUpdate();
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal,
			"SQL error removing facet: " +e.getMessage());
	    }
	}

	/**
	 * Return this ResourceFacetDB as an ResourceFacet, suitable for
	 * passing out through the API.
	 * @return this ResourceFacetDB as an ResourceFacet
	 */
	public ResourceFacet export() {
	    ResourceFacet rv = new ResourceFacet();
	    List<ResourceTag> t = new ArrayList<>();
	    rv.setName(getName());
	    rv.setType(getType());
	    rv.setUnits(getUnits());
	    rv.setValue(getValue());
	    for (Map.Entry<String, String> e : tags.entrySet()) {
		ResourceTag rt = new ResourceTag();

		rt.setName(e.getKey());
		rt.setValue(e.getValue());
		t.add(rt);
	    }
	    rv.setTags(t);
	    return rv;
	}
    }

    /**
     * Base constructor.  Needed to allow nameless ResourceDBs and to keep
     * one call to the superclass constructor.
     * @throws DeterFault if there is a DB setup error.
     */
    protected ResourceDB(SharedConnection sc) throws DeterFault {
	super("resource", "resourceperms", "ridx", "resources",
		"name", sc);
	name = null;
	type = null;
	description = null;
	data = null;
	facets = new ArrayList<>();
	tags = new HashMap<>();
    }

    /**
     * Create a nameless ResourceDB.
     * @throws DeterFault if there is a DB setup error.
     */
    public ResourceDB() throws DeterFault {
	this((SharedConnection) null);
    }

    /**
     * Create an ResourceDB with the given eid.
     * @param n the name
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error.
     */
    public ResourceDB(String n) throws DeterFault {
	this(n, null);
    }

    /**
     * Create an ResourceDB with the given eid that shares a DB connection
     * @param n the name
     * @param sc the shared connection
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error or naming issue
     */
    public ResourceDB(String n, SharedConnection sc) throws DeterFault {
	this(sc);
	setName(n);
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
     * @throws DeterFault if the name is not scoped or the user cannot
     * create in its namespace.
     */
    public void setName(String n) throws DeterFault {
	checkScopedName(n);
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
     * Return the persistence.
     * @return the persistence.
     */
    public boolean getPersist() { return persist; }

    /**
     * Set the persistence
     * @param p the new persistence
     */
    public void setPersist(boolean p) { persist = p; }

    /**
     * Return the description.
     * @return the description.
     */
    public String getDescription() { return description; }

    /**
     * Set a new description
     * @param d the description.
     */
    public void setDescription(String d) { description = d; }

    /**
     * Return the data.
     * @return the data.
     */
    public byte[] getData() { return data; }

    /**
     * Set a new data
     * @param d the data.
     */
    public void setData(byte[] d) { data = d; }

    /**
     * Export tags.
     * @return a List of ResourceTags
     */
    public ResourceTag[] exportTags() {
	ResourceTag[] rv = new ResourceTag[tags.size()];
	int i = 0;

	for (Map.Entry<String, String> t : tags.entrySet())
	    rv[i++] = new ResourceTag(t.getKey(), t.getValue());
	return rv;
    }

    /**
     * Return true if the resource exists.
     * @return true if the resource exists.
     * @throws DeterFault if there is a DB error
     */
    public boolean isValid() throws DeterFault {
	PreparedStatement p = null;
	int count = 0;

	if ( getName() == null ) return false;

	try {
	    p = getPreparedStatement(
		    "SELECT count(*) FROM resources WHERE name=?");
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
     * Store this resourceDB in the database, including the aspects and
     * credentials.
     * @param facets the resource facets
     * @param inAcl the access control list
     * @param tgs the tags to attach
     * @throws DeterFault on errors
     */
    public void create(ResourceFacet[] facets, Collection<AccessMember> inAcl,
	    ResourceTag[] tgs) throws DeterFault {
	PreparedStatement p = null;

	if (facets == null)
	    throw new DeterFault(DeterFault.request, "No facets given");

	if ( inAcl == null )
	    throw new DeterFault(DeterFault.request, "No acl given");

	if ( tgs == null )
	    throw new DeterFault(DeterFault.request, "No tags given");

	if ( getName() == null ) 
	    throw new DeterFault(DeterFault.request,
		    "Cannot create resource with no name");

	try {
	    p = getPreparedStatement(
		    "INSERT INTO resources " +
			"(name, type, persist, description, data) " +
			"VALUES(?, ?, ?, ?, ?) " +
			"ON DUPLICATE KEY UPDATE type=?, persist=?, " +
			    "description = ?, data=?");
	    p.setString(1, getName());
	    p.setString(2, getType());
	    p.setBoolean(3, getPersist());
	    p.setString(4, getDescription());
	    p.setBytes(5, getData());
	    p.setString(6, getType());
	    p.setBoolean(7, getPersist());
	    p.setString(8, getDescription());
	    p.setBytes(9, getData());
	    p.executeUpdate();
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    // Creating this record can only really violate the unique eid
	    // constraint.
	    System.err.println(e);
	    throw new DeterFault(DeterFault.request, "Resource exists" + p );
	}
	catch (SQLException e) {
	    // Some other SQL problem.  Punt.
	    throw new DeterFault(DeterFault.internal,
		    "Database error creating resource: " + e);
	}

	// Really create the rest of the resource.  From here on out, if
	// there are problems we wipe the partial resource.
	try {
	    // Assign permissions
	    for (AccessMember m : inAcl )
		assignPermissions(m);

	    // Add tags
	    p = getPreparedStatement(
		    "INSERT INTO resourcetags (ridx, name, value) " +
			"VALUES (" +
			    "(SELECT idx FROM resources WHERE name=?), "+
			    "?, ?)");
	    p.setString(1, getName());
	    for (ResourceTag t: tgs) {
		String name = t.getName();
		String value = t.getValue();

		p.setString(2, name);
		p.setString(3, value);
		p.executeUpdate();
		tags.put(name, value);
	    }


	    // Add facets
	    for (ResourceFacet f : facets) {
		ResourceFacetDB fdb = new ResourceFacetDB(f.getName(),
			f.getType(), f.getValue(), f.getUnits());
		ResourceTag[]tags = f.getTags();

		for (ResourceTag t : (tags != null) ? tags : new ResourceTag[0])
		    fdb.setTag(t.getName(), t.getValue());

		fdb.save(true);
	    }
	}
	catch (SQLException se) {
	    // Something went wrong in the DB manipulations. Remove the
	    // database entries (ACLs and definition).
	    try { remove(); }
	    catch (DeterFault ignored) { }
	    throw new DeterFault(DeterFault.internal, se.getMessage());
	}
	catch (DeterFault df) {
	    // Writing the Resource failed.  Remove the database entries
	    // (ACLs and definition).
	    try { remove(); }
	    catch (DeterFault ignored) { }
	    throw df;
	}
	try {
	    updatePolicyCredentials();
	    updateCircleCredentials();
	}
	catch (Exception e) {
	    e.printStackTrace();
	    throw e;
	}
    }

    /**
     * Get the facets as ResourceFacetDBs.
     * @return the facets as ResourceFacetDBs.
     * @throws DeterFault if there is an error
     */
    public List<ResourceFacetDB> getFacets() throws DeterFault {
	return getFacets(null);
    }

    /**
     * Get the facets of this resource with the given names as ResourceFacetDBs.
     * @param names the names to gather
     * @return the facets as ResourceFacetDBs.
     * @throws DeterFault if there is an error
     */
    public List<ResourceFacetDB> getFacets(List<String> names)
	throws DeterFault {
	List<ResourceFacetDB> rv = new ArrayList<>();
	String query =
	    "SELECT name FROM facets "+
		"WHERE ridx=(SELECT idx FROM resources WHERE name=?)";

	if (names != null && names.size() > 0) {
	    StringBuilder sb = new StringBuilder(query);
	    boolean first = true;

	    sb.append(" AND name IN (");
	    for (String i : names ) {
		if (!first) sb.append(",");
		sb.append(i);
		first = false;
	    }
	    sb.append(")");
	    query = sb.toString();
	}

	try {
	    PreparedStatement p = getPreparedStatement(query);
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();

	    while (r.next()) {
		ResourceFacetDB rdb = new ResourceFacetDB();

		rdb.setName(r.getString(1));
		rdb.load();
		rv.add(rdb);
	    }
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "SQLException gathering facets: " + e.getMessage());
	}
    }

    /**
     * Remove a set of facets. 
     * @param facets ResourceFacets encoding the facets to remove.
     * @return the facets that were successfully removed.
     * @throws DeterFault if there is an error
     */
    public List<ChangeResult> removeFacets(Collection<ResourceFacet> facets)
	    throws DeterFault {
	List<String> names = new ArrayList<>();
	List<ChangeResult> rv = new ArrayList<>();

	for (ResourceFacet f : facets)
	    names.add(f.getName());

	// Gather up the facets and remove them.  Successes are returned as
	// the rv.
	for (ResourceFacetDB rf: getFacets(names)) {
	    try {
		rf.load();
		rf.remove();
		rv.add(new ChangeResult(rf.getName(), null, true));
	    }
	    catch (DeterFault df) {
		rv.add(new ChangeResult((rf != null) ? rf.getName():"No Name?",
			    df.getDetailMessage(), false));
	    }
	}
	return rv;
    }

    /**
     * Add a set of facets.  The facets to add are passed as
     * as ResourceFacets.
     * @param facets the ResourceFacets to add
     * @return the facets that were successfully added.
     * @throws DeterFault if there is an error
     */
    public List<ChangeResult> addFacets(
	    Collection<ResourceFacet> facets) throws DeterFault {
	List<ChangeResult> rv = new ArrayList<ChangeResult>();

	// Gather up the facets and add them.
	for (ResourceFacet f : facets) {
	    ResourceFacetDB rdb = new ResourceFacetDB(f.getName(), f.getType(),
		    f.getValue(), f.getUnits());
	    ResourceTag[] tags = f.getTags();

	    for (ResourceTag t: (tags != null) ? tags : new ResourceTag[0])
		rdb.setTag(t.getName(), t.getValue());

	    try {
		rdb.save(true);
		rv.add(new ChangeResult(f.getName(), null, true));
	    }
	    catch (DeterFault df) {
		rv.add(new ChangeResult(f.getName(), df.getDetailMessage(),
			    false));
	    }
	}
	return rv;
    }

    /**
     * Return the tag with the given name.
     * @param n the name to look up
     * @return the named tag
     */
    public String getTag(String n) { return tags.get(n); }
    /**
     * Set the tag with the given name.  If v is null, delete the tag.
     * @param n the name to assign/delete
     * @param v the new value
     */
    public void setTag(String n, String v) {
	if ( v == null ) tags.remove(n);
	else tags.put(n, v);
    }

    /**
     * Load the resource's content from the DB
     * @throws DeterFault on errors
     */

    public void load() throws DeterFault {
	PreparedStatement p = null;
	try {
	    p = getPreparedStatement(
		    "SELECT name, type, persist, description, data " +
		    "FROM resources WHERE name=?");
	    p.setString(1, getName());
	    ResultSet r = p.executeQuery();
	    int rows = 0;

	    while (r.next()) {
		if ( ++rows > 1) throw new DeterFault(DeterFault.internal,
			"More than one definition for resource");
		setName(r.getString(1));
		setType(r.getString(2));
		setPersist(r.getBoolean(3));
		setDescription(r.getString(4));
		setData(r.getBytes(5));
	    }
	    if (rows == 0 )
		throw new DeterFault(DeterFault.request,
			"No such resource " + getName());
	    facets.clear();
	    facets = getFacets();

	    p = getPreparedStatement(
		    "SELECT name, value FROM resourcetags " +
		    "WHERE ridx = (SELECT idx FROM resources WHERE name =?)");
	    p.setString(1, getName());
	    r = p.executeQuery();

	    while (r.next())
		tags.put(r.getString(1), r.getString(2));
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
     * Store the resource into the DB.
     * @throws DeterFault on errors
     */
    public void save() throws DeterFault {
	try {
	    String q = null;
	    PreparedStatement p = null;

	    if (getName() == null )
		throw new DeterFault(DeterFault.request, "Untyped resource");

	    // Save the resource in the DB.  Only update it.
	    q = "UPDATE resources " +
		    "SET name=?, type=?, persist=?, description=?, data=? " +
		"WHERE name=?";

	    p = getPreparedStatement(q);
	    p.setString(1, getName());
	    p.setString(2, getType());
	    p.setBoolean(3, getPersist());
	    p.setString(4, getDescription());
	    p.setBytes(5, getData());
	    p.setString(6, getName());
	    p.executeUpdate();

	    p = getPreparedStatement(
		    "REPLACE INTO resourcetags (name, value, ridx) " +
		    "VALUES (?, ?, "+
		    "(SELECT idx FROM resources WHERE name=?)) ");
	    p.setString(3, getName());

	    for (Map.Entry<String, String> t : tags.entrySet()) {
		p.setString(1, t.getKey());
		p.setString(2, t.getValue());
		p.executeUpdate();
	    }
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "SQL error saving Resource: " +e.getMessage());
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
		    "Cannot remove experiment with no name");

	// Get all of the credentials out of the DB and cache
	removeCredentials();
	try {
	    p = getPreparedStatement("DELETE FROM facettags " +
		    "WHERE fidx=(SELECT idx FROM facets " +
			"WHERE ridx=(SELECT idx FROM resources WHERE name=?))");
	    p.setString(1, getName());
	    p.executeUpdate();
	    p = getPreparedStatement("DELETE FROM facets " +
		    "WHERE ridx=(SELECT idx FROM resources WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();
	    p = getPreparedStatement("DELETE FROM resourceperms " +
		    "WHERE ridx=(SELECT idx FROM resources WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();
	    p = getPreparedStatement("DELETE FROM resourcetags " +
		    "WHERE ridx=(SELECT idx FROM resources WHERE name=?)");
	    p.setString(1, getName());
	    p.executeUpdate();
	    p = getPreparedStatement( "DELETE FROM resources WHERE name=?");
	    p.setString(1, getName());
	    int ndel = p.executeUpdate();

	    if ( ndel == 0 )
		throw new DeterFault(DeterFault.request, "No such Resource");
	    else if ( ndel > 1 )
		throw new DeterFault(DeterFault.internal,
			"Multiple Resources deleted??");

	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Database error removing resource: " + e);
	}
    }

    /**
     * Return ResourceDBs readable by the given user, that match regex.
     * of type ty.  Any of the parameters can be null.  All the
     * returned ResourceDB's share the given connection.
     * @param uid the user to match
     * @param type the type of resource to find
     * @param regex the name regex
     * @param realization get resources bound to this realization (optional)
     * @param persist true if only persistent resources are requested (optional)
     * @param tags tags that must be present
     * @param offset the index of the first experiment description to return
     * @param count the number of descriptions to return (-1 for all)
     * @param sc the shared connection to use
     * @return a collection of ResourceDBs
     * @throws DeterFault on error
     */
    static public List<ResourceDB> getResources(String uid,
	    String type, String regex, String realization, Boolean persist,
	    Collection<ResourceTag> tags, int offset, int count,
	    SharedConnection sc) throws DeterFault {
	StringBuilder query = new StringBuilder("SELECT name FROM resources ");
	Connection c = null;
	List<ResourceDB> rv = new ArrayList<>();
	boolean whereAdded = false;
	int sqlIdx = 1;
	boolean needRealizationName = false;

	if ( sc == null) sc = new SharedConnection();
	try {
	    sc.open();
	    c = sc.getConnection();
	    // XXX: uid?
	    if ( type != null ) {
		if ( whereAdded ) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		query.append("type = ? ");
	    }

	    if ( regex != null ) {
		if (whereAdded) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		// Nice and simple.
		query.append("name REGEXP ? ");
	    }

	    if ( realization != null && !realization.equals("any")) {
		if (whereAdded) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}

		if ( realization.equals("none")) {
		    query.append(
			    "( (idx NOT IN (SELECT outeridx "+
			    "FROM realizationcontainment)) AND " +
			    "(idx NOT IN (SELECT inneridx "+
			    "FROM realizationcontainment)) AND " +
			    "(idx NOT IN (SELECT residx "+
			    "FROM realizationtopology)) ) ");
		}
		else {
		    needRealizationName = true;
		    // One??
		    query.append("( (idx IN (SELECT outeridx " +
			    "FROM realizationcontainment " +
			    "WHERE ridx=" +
				"(SELECT idx FROM realizations "+
				    "WHERE name = ?))) ");
		    query.append("OR (idx IN (SELECT inneridx " +
			    "FROM realizationcontainment " +
			    "WHERE ridx=" +
				"(SELECT idx FROM realizations " +
				    "WHERE name = ?))) ");
		    query.append("OR (idx IN (SELECT residx " +
			    "FROM realizationtopology " +
			    "WHERE ridx=" +
				"(SELECT idx FROM realizations " +
				    "WHERE name = ?)))) ");
		}

	    }

	    if ( persist != null ) {
		if (whereAdded) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		query.append("persist=? ");
	    }

	    for (ResourceTag rt : tags ) {
		if (whereAdded) query.append("AND ");
		else {
		    query.append("WHERE ");
		    whereAdded = true;
		}
		query.append("idx IN (SELECT ridx FROM resourcetags " +
			"WHERE name=? AND value=?) ");
	    }

	    if ( count != -1 ) {
		if ( offset < 0) offset = 0;
		query.append("ORDER BY idx LIMIT ? OFFSET ? ");
	    }
	    PreparedStatement p = c.prepareStatement(query.toString());

	    if ( type != null) p.setString(sqlIdx++, type);
	    if ( regex != null) p.setString(sqlIdx++, regex);
	    if ( needRealizationName ) {
		p.setString(sqlIdx++, realization);
		p.setString(sqlIdx++, realization);
		p.setString(sqlIdx++, realization);
	    }
	    if ( persist != null ) p.setBoolean(sqlIdx++, persist);
	    for (ResourceTag rt : tags ) {
		p.setString(sqlIdx++, rt.getName());
		p.setString(sqlIdx++, rt.getValue());
	    }
	    if ( count  != -1 ) {
		p.setInt(sqlIdx++, count);
		p.setInt(sqlIdx++, offset);
	    }

	    ResultSet r = p.executeQuery();
	    while (r.next()) {
		ResourceDB rdb = new ResourceDB(r.getString(1), sc);
		rdb.load();
		rv.add(rdb);
	    }
	    sc.close();
	    return rv;
	}
	catch (SQLException e) {
	    for (ResourceDB r : rv)
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
