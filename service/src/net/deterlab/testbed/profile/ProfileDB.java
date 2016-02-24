package net.deterlab.testbed.profile;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.Profile;
import net.deterlab.testbed.db.DBObject;
import net.deterlab.testbed.db.SharedConnection;

/**
 * A profile: the meta data stored by the testbed that identifies the
 * associated object.
 * @author the DETER Team
 * @version 1.0
 */
abstract public class ProfileDB extends DBObject {
    abstract public class AttributeDB extends Attribute {

	/**
	 * Initialize an empty AttributeDB
	 */
	public AttributeDB() { }

	/**
	 * Initialize a UserAttributeDB.
	 * @param n the attribute name
	 * @param t the attribute data type
	 * @param opt true if the attribute is optional
	 * @param a the access permissions (see constants)
	 * @param d the description of the attribute
	 * @param f the regular expression defining the format (may be null)
	 * @param fd the natural language description of the format (may be
	 * null)
	 * @param o the ordering hint
	 * @param l the length hint
	 */
	public AttributeDB(String n, String t, boolean opt, String a, String d,
		String f, String fd, int o, int l) {
	    super(n, t, opt, a, d, f, fd, o, l);
	}

	/**
	 * Initialize a UserAttributeDB.
	 * @param n the attribute name
	 * @param v the attribute value
	 * @param t the attribute data type
	 * @param opt true if the attribute is optional
	 * @param a the access permissions (see constants)
	 * @param d the description of the attribute
	 * @param f the regular expression defining the format (may be null)
	 * @param fd the natural language description of the format (may be
	 * null)
	 * @param o the ordering hint
	 * @param l the length hint
	 */
	public AttributeDB(String n, String v, String t, boolean opt, String a,
		String d, String f, String fd, int o, int l) {
	    super(n, v, t, opt, a, d, f, fd, o, l);
	}

	/**
	 * Initialize a UserAttributeDB from some other form of UserAttribute
	 * @param a the attribute
	 */
	public AttributeDB(Attribute a) {
	    this(a.getName(), a.getValue(), a.getDataType(), a.getOptional(),
		    a.getAccess(), a.getDescription(), a.getFormat(),
		    a.getFormatDescription(), a.getOrderingHint(),
		    a.getLengthHint());
	}

	/**
	 * Return the table containing the attribute schema for this attribute.
	 * @return the table containing the attribute schema for this attribute
	 */
	abstract public String getSchemaTable();

	/**
	 * Return the table containing the value schema for this attribute.
	 * @return the table containing the value schema for this attribute
	 */
	abstract public String getValuesTable();

	/**
	 * Return the table containing the identity resolver for this attribute.
	 * @return the table containing the identity resolver schema for this
	 * attribute
	 */
	abstract public String getIdJoinTable();

	/**
	 * Return the column in the schema that holds the index of the joining
	 * id
	 * @return the column in the schema that holds the index of the joining
	 * id
	 */
	abstract public String getIdJoinColumn();

	/**
	 * Return the column in the identifier table that holds the index of
	 * the joining id
	 * @return the column in the identifier table that holds the index of
	 * the joining id
	 */
	abstract public String getIdJoinKey();

	/**
	 * Return the column in the schema that holds the name of the attribute
	 * @return the column in the schema that holds the name of the attribute
	 */
	public String getNameColumn() { return "name"; }

	/**
	 * Return the column in the values that holds the value of the attribute
	 * @return the column in the values that holds the value of the
	 * attribute
	 */
	public String getValueColumn() { return "value"; }

	/**
	 * Return the column in the schema that holds the type of the attribute
	 * @return the column in the schema that holds the type of the attribute
	 */
	public String getDataTypeColumn() { return "datatype"; }

	/**
	 * Return the column in the schema that holds the optional flag of the
	 * attribute
	 * @return the column in the schema that holds the optional flag of the
	 * attribute
	 */
	public String getOptionalColumn() { return "optional"; }

	/**
	 * Return the column in the schema that holds the access of the
	 * attribute
	 * @return the column in the schema that holds the access of the
	 * attribute
	 */
	public String getAccessColumn() { return "access"; }

	/**
	 * Return the column in the schema that holds the description of the
	 * attribute
	 * @return the column in the schema that holds the description of the
	 * attribute
	 */
	public String getDescriptionColumn() { return "description"; }

	/**
	 * Return the column in the schema that holds the format of the
	 * attribute
	 * @return the column in the schema that holds the format of the
	 * attribute
	 */
	public String getFormatColumn() { return "format"; }

	/**
	 * Return the column in the schema that holds the format description of
	 * the attribute
	 * @return the column in the schema that holds the format description of
	 * the attribute
	 */
	public String getFormatDescriptionColumn() {
	    return "formatdescription";
	}

	/**
	 * Return the column in the schema that holds the suggested ordering
	 * info
	 * @return the column in the schema that holds the suggested ordering
	 * info
	 */
	// "order" is an sql keyword, so "sequence"
	public String getOrderingHintColumn() { return "sequence"; }

	/**
	 * Return the column in the schema that holds the suggested length info
	 * @return the column in the schema that holds the suggested length info
	 */
	public String getLengthHintColumn() { return "length"; }


	/**
	 * Remove this value from the database for the given user.  The
	 * connection should point to the correct database.  This removes the
	 * assignment of the value to this user, not the schema. Note the
	 * package scope.
	 * @param c the open database connection
	 * @param id the ID to remove
	 * @throws DeterFault primarily on database problems
	 */
	void remove(String id) throws DeterFault {
	    PreparedStatement del = null;
	    try {
		del = getPreparedStatement(
			"DELETE FROM " + getValuesTable() +
			" WHERE " + getIdJoinColumn()  +
			    "=(SELECT idx FROM " + getIdJoinTable() +
				" WHERE " + getIdJoinKey() + "=?) " +
			    "AND aidx=" +
				"(SELECT idx FROM " + getSchemaTable() +
				" WHERE " + getNameColumn() + "=?)");
		del.setString(1, id);
		del.setString(2, getName());
		del.executeUpdate();
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal, e.toString());
	    }
	}

	/**
	 * Save this value to the database for the given id.  The connection
	 * should point to the correct database.  Note the package scope.
	 * @param id the identifier
	 * @throws DeterFault primarily on database problems
	 */
	void save(String id) throws DeterFault {

	    PreparedStatement detect = null;
	    PreparedStatement assign = null;
	    try {
		detect = getPreparedStatement(
			"SELECT " + getValueColumn() +" FROM " +
			    getValuesTable() +
			" WHERE " + getIdJoinColumn() + "=(SELECT idx FROM " +
			    getIdJoinTable() + " WHERE " +
				getIdJoinKey() + "=?) "+
			    "AND aidx=(SELECT idx FROM " + getSchemaTable() +
			    " WHERE "+ getNameColumn()+ "=?)");

		/* Both of these strings take the same parameters in the same
		 * order - value, user, name */
		/* Use this string to update an existing assignment */
		String update =
			"UPDATE " +getValuesTable() +" SET " +
			    getValueColumn() +" = ? " +
			"WHERE " + getIdJoinColumn() + "=(SELECT idx FROM " +
			    getIdJoinTable() + " WHERE " +
				getIdJoinKey() +"=?) " +
			    "AND aidx=(SELECT idx FROM " + getSchemaTable() +
			    " WHERE " + getNameColumn() + "=?)";
		/* Use this string to insert a new assignment */
		String insert =
			"INSERT " + getValuesTable() +" (" + getValueColumn()+
			    ", " + getIdJoinColumn() + ", aidx) " +
			"VALUES (?, (SELECT idx FROM "+ getIdJoinTable() +
			    " WHERE " + getIdJoinKey() + "= ?), " +
			" (SELECT idx from " + getSchemaTable() + " WHERE " +
			    getNameColumn() + "=?))";
		ResultSet r = null;

		detect.setString(1, id);
		detect.setString(2, getName());
		r = detect.executeQuery();

		/* If r.next() returns true, there was a previous assignment
		 * and the prepared statement will update; otherwise it will
		 * insert. Same parameters either way, so the code is straight
		 * line from here. */
		assign = getPreparedStatement((r.next()) ? update : insert);
		detect.close();
		detect = null;

		assign.setString(1, getValue());
		assign.setString(2, id);
		assign.setString(3, getName());
		assign.executeUpdate();

		assign.close();
		assign = null;
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal, e.toString());
	    }
	}

	/**
	 * Create this attribute if it does not exist.
	 * @param def if the attribute is not optional, set that vaule for all
	 * users.
	 * @throws DeterFault if the attribute exists or if there is a problem
	 * creating it
	 */
	public void create(String def) throws DeterFault {
	    PreparedStatement p = null;
	    if ( !getOptional() && def == null )
		throw new DeterFault(DeterFault.request,
			"Cannot add required attribute without default value");

	    if ( getName() == null )
		throw new DeterFault(DeterFault.request,
			"Cannot create attribute without a name");

	    if ( !validateAccess(getAccess()))
		throw new DeterFault(DeterFault.request,
			"Cannot create attribute: unknown access");

	    try {
		p = getPreparedStatement("SELECT idx FROM " +
			getSchemaTable() + " WHERE " +
			getNameColumn() + " = ?");
		p.setString(1, getName());

		ResultSet r = p.executeQuery();

		// r.next() == true if the query above found a row
		if ( r.next())
		    throw new DeterFault(DeterFault.request,
			    "Attribute " + getName() +
			    " exists cannot create it");

		// If the ordering hint is unset, make this the last attribute
		if ( getOrderingHint() == 0 ) {
		    p = getPreparedStatement(
			    "SELECT MAX(" + getOrderingHintColumn() +
				") FROM " + getSchemaTable());
		    r = p.executeQuery();
		    if ( r.next()) setOrderingHint(r.getInt(1)+100);
		    else setOrderingHint(100); // Nothing in there!?
		}

		p = getPreparedStatement(
			"INSERT INTO " + getSchemaTable() + " (" +
			    getNameColumn() + ", " +
			    getDataTypeColumn() + ", " +
			    getOptionalColumn() + ", " +
			    getAccessColumn() + ", " +
			    getDescriptionColumn() + ", " +
			    getFormatColumn() + ", " +
			    getFormatDescriptionColumn() + ", " +
			    getOrderingHintColumn() + ", " +
			    getLengthHintColumn() +
			    ")  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		p.setString(1, getName());
		p.setString(2, getDataType());
		p.setInt(3, getOptional() ? 1 : 0);
		p.setString(4, getAccess());
		p.setString(5, getDescription());
		p.setString(6, getFormat());
		p.setString(7, getFormatDescription());
		p.setInt(8, getOrderingHint());
		p.setInt(9, getLengthHint());

		p.executeUpdate();

		// If there is a default value for the attribute, assign it
		if ( def == null  ) return;

		/* Use INSERT SELECT to add the default value for all users.
		 * The select finds the index assigned by the insert above,
		 * then inserts an enter for each netry in the join table (e.g,
		 * users or projects) and assigns the default value above to
		 * it.
		 */
		p = getPreparedStatement(
			"INSERT into " + getValuesTable() + "(aidx, " +
			    getIdJoinColumn() + ", " +
			    getValueColumn() + ") " +
			"SELECT (SELECT idx from " + getSchemaTable() +
			    " WHERE " + getNameColumn() + " = ? ), idx, ? " +
			" FROM " + getIdJoinTable());
		p.setString(1, getName());
		p.setString(2, def);
		p.executeUpdate();
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal, e.toString());
	    }
	}

	/**
	 * Modify the schema of this attribute - set the schema to the current
	 * schema values.
	 * @throws DeterFault if the attribute does not exist or if there is a
	 * problem modifying it
	 */
	public void modifySchema() throws DeterFault {
	    PreparedStatement p = null;
	    try {
		p = getPreparedStatement("SELECT idx FROM " +
			getSchemaTable() + " WHERE " +
			getNameColumn() + " = ?");
		p.setString(1, getName());

		ResultSet r = p.executeQuery();

		// r.next() == true if the query above found a row
		if ( !r.next())
		    throw new DeterFault(DeterFault.request,
			    "Attribute " + getName() + " does not exist. "
			    + "cannot modify it");

		p = getPreparedStatement(
			"UPDATE " + getSchemaTable() + " " +
			    "SET " + getDataTypeColumn() + "=? " +
			    ", " + getOptionalColumn() + "=? " +
			    ", " + getAccessColumn() + "=? " +
			    ", " + getDescriptionColumn() + "=? " +
			    ", " + getFormatColumn() + "=? " +
			    ", " + getFormatDescriptionColumn() + "=? " +
			    ", " + getOrderingHintColumn() + "=? " +
			    ", " + getLengthHintColumn() + "=? WHERE name=?" );
		p.setString(1, getDataType());
		p.setInt(2, getOptional() ? 1 : 0);
		p.setString(3, getAccess());
		p.setString(4, getDescription());
		p.setString(5, getFormat());
		p.setString(6, getFormatDescription());
		p.setInt(7, getOrderingHint());
		p.setInt(8, getLengthHint());
		p.setString(9, getName());

		p.executeUpdate();
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal, e.toString());
	    }
	}
	/**
	 * Remove this attribute from all users and the attribute schema.  Be
	 * careful.  Only the attribute name needs to be set.
	 * @throws DeterFault if the attribute does not exist or if there is a
	 * problem creating it
	 */
	public void removeFromSchema() throws DeterFault {
	    PreparedStatement p = null;
	    try {
		p = getPreparedStatement("SELECT idx FROM " +
			getSchemaTable() + " WHERE " +
			getNameColumn() + " = ?");
		p.setString(1, getName());

		ResultSet r = p.executeQuery();

		// r.next() == true if the query above found a row
		if ( !r.next())
		    throw new DeterFault(DeterFault.request,
			    "Attribute " + getName() +
			    " does not exist cannot remove it");

		// Remove the value from all users
		p = getPreparedStatement(
			"DELETE FROM " + getValuesTable() + " WHERE aidx=(" +
			    "SELECT idx FROM " + getSchemaTable() + " WHERE " +
			    getNameColumn() + "=?)");
		p.setString(1, getName());
		p.executeUpdate();

		// And remove it from the schema
		p = getPreparedStatement(
			"DELETE FROM " + getSchemaTable() +
			" WHERE " + getNameColumn() + " =?");
		p.setString(1, getName());
		p.executeUpdate();
	    }
	    catch (SQLException e) {
		throw new DeterFault(DeterFault.internal, e.toString());
	    }
	}

	/**
	 * Return an Attribute version of this object, suitable for web
	 * services export.  If the attribute is WRITE_ONLY or NO_ACCESS, the
	 * value will be cleared.
	 * @return this class as an Attribute
	 */
	public Attribute export() {
	    Attribute rv = new Attribute();

	    rv.setName(getName());
	    if (getAccess() == WRITE_ONLY || getAccess() == NO_ACCESS)
		rv.setValue(null);
	    else
		rv.setValue(getValue());

	    rv.setDataType(getDataType());
	    rv.setAccess(getAccess());
	    rv.setOptional(getOptional());
	    rv.setDescription(getDescription());
	    rv.setFormat(getFormat());
	    rv.setFormatDescription(getFormatDescription());
	    rv.setOrderingHint(getOrderingHint());
	    rv.setLengthHint(getLengthHint());

	    return rv;
	}
    }
    /** The id associated with this profile */
    private String id;
    /** The attributes attached to this profile */
    protected Map<String, AttributeDB> dattrs;

    /**
     * Create a ProfileDB with no id and empty attributes.
     * @throws DeterFault if there is a problem with the shared DB connection
     */
    public ProfileDB() throws DeterFault { this(null, null); }

    /**
     * Create a ProfileDB with the given identifier and underlying attribute
     * class using a SharedConnection
     * @param i the identifier string
     * @param sc a shared database connection
     * @throws DeterFault if there is a problem with the shared DB connection
     */
    public ProfileDB(String i, SharedConnection sc)
	    throws DeterFault {
	super(sc);
	id = i;
	dattrs = new TreeMap<String, AttributeDB>();
    }

    /**
     * Create a ProfileDB with the given identifier and underlying attribute
     * class
     * @param i the identifier string
     * @throws DeterFault if there is a problem with the shared DB connection
     */
    public ProfileDB(String i) throws DeterFault {
	this(i, null);
	id = i;
	dattrs = new TreeMap<String, AttributeDB>();
    }

    /**
     * Create a ProfileDB with the given underlying attribute class that shares
     * a DB connection.
     * @param sc the shared connection
     * @throws DeterFault if there is a problem with the shared DB connection
     */
    public ProfileDB(SharedConnection sc) throws DeterFault {
	this(null, sc);
    }

    /**
     * Return the identifier associated with the profile.
     * @return the identifier associated with the profile
     */
    public String getId() { return id; }

    /**
     * Set the identifier associated with the profile.
     * @param i the identifier associated with the profile
     */
    public void setId(String i) { id = i; }

    /**
     * Get an instance of an attribute for this profile.
     * @return an instance of an attribute for this profile.
     */
    abstract public AttributeDB getAttribute();

    /**
     * Get an instance of an attribute for this profile, initialized from an
     * Attribute.
     * @param a the Attribute to use as a template
     * @return an instance of an attribute for this profile.
     */
    abstract public AttributeDB getAttribute(Attribute a);

    /**
     * Load all attributes defined for this user from the database.  If no user
     * is attached to the profile, load empty versions of all possible
     * attributes.
     * @throws DeterFault on errors
     */
    public void loadAll() throws DeterFault { load(null); }

    /**
     * Load the named attributes from the database.
     * @param names the collection of names to read
     * @throws DeterFault on errors
     */
    public void load(Collection<String> names) throws DeterFault {
	PreparedStatement p = null;
	ResultSet r = null;
	Vector<AttributeDB> v = new Vector<AttributeDB>();
	Set<String> toLoad = 
	    (names != null ) ? new TreeSet<String>(names) : null;
	int nAttrs = 0;
	AttributeDB contents = getAttribute();

	try {
	    // If there is no id set, gather up all the attribute specs without
	    // any data.  If the id is set, pull in just the set value for
	    // this user (some optional fields may not appear at all.  Both
	    // queries return the same columns.
	    if ( getId() != null ) { 
		p = getPreparedStatement(
			"SELECT " +  
			    contents.getNameColumn() + ", " +
			    contents.getValueColumn() + ", " +
			    contents.getDataTypeColumn() + ", " +
			    contents.getOptionalColumn() + ", " +
			    contents.getAccessColumn() + ", " +
			    contents.getDescriptionColumn() + ", " +
			    contents.getFormatColumn() + ", " +
			    contents.getFormatDescriptionColumn() + ", " +
			    contents.getOrderingHintColumn() + ", " +
			    contents.getLengthHintColumn() + 
			" FROM " + contents.getValuesTable() + " as av " + 
			    "INNER JOIN " + contents.getSchemaTable() + 
				" as v ON v.idx = av.aidx " + 
			"WHERE av." + contents.getIdJoinColumn() + 
			    "=(select idx from " + 
			    contents.getIdJoinTable() +
			    " where " + contents.getIdJoinKey() +" = ?)");
		p.setString(1, getId());
	    }
	    else {
		p = getPreparedStatement(
			"SELECT " + 
			    contents.getNameColumn() + ", NULL as value, " +
			    contents.getDataTypeColumn() + ", " +
			    contents.getOptionalColumn() + ", " +
			    contents.getAccessColumn() + ", " +
			    contents.getDescriptionColumn() + ", " +
			    contents.getFormatColumn() + ", " +
			    contents.getFormatDescriptionColumn() + ", " +
			    contents.getOrderingHintColumn() + ", " +
			    contents.getLengthHintColumn() + 
			" FROM " + contents.getSchemaTable());
	    }
	    r = p.executeQuery();
	    // Convert each row to a AttributeDB
	    while (r.next()) {
		String name = r.getString(contents.getNameColumn());

		nAttrs++;
		// If only certain named attributes are to be loaded, and this
		// isn't one of them, skip it. (NB: we count it as found
		// whether we export it.
		if ( toLoad != null && !toLoad.contains(name))
		    continue;

		AttributeDB toAdd = getAttribute();

		toAdd.setName(r.getString(toAdd.getNameColumn()));
		toAdd.setValue(r.getString(toAdd.getValueColumn()));
		toAdd.setDataType(
			r.getString(toAdd.getDataTypeColumn()).toLowerCase());
		toAdd.setOptional(r.getInt(toAdd.getOptionalColumn())> 0);
		toAdd.setAccess(r.getString(toAdd.getAccessColumn()));
		toAdd.setDescription(r.getString(toAdd.getDescriptionColumn()));
		toAdd.setFormat(r.getString(toAdd.getFormatColumn()));
		toAdd.setFormatDescription(
			r.getString(toAdd.getFormatDescriptionColumn()));
		toAdd.setOrderingHint(r.getInt(toAdd.getOrderingHintColumn()));
		toAdd.setLengthHint(r.getInt(toAdd.getLengthHintColumn()));
		v.add(toAdd);
	    }
	    // All valid users must have at least one attribute - e.g. an
	    // e-mail address.  If we asked for a user and found no attributes,
	    // throw this out.
	    if ( nAttrs == 0 && getId() != null ) 
		throw new DeterFault(DeterFault.request, 
			"No profile for id " + getId());
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
	// Put the new attributes into the map
	for (AttributeDB a: v ) 
	    addAttribute(a);
    }


    /**
     * Save the named attributes to the database.
     * @param names the collection of names to Save.  If names is null, this
     * becomes saveAll.
     * @throws DeterFault on errors
     */
    public void save(Collection<String> names) throws DeterFault {
	Set<String> toSave = 
	    (names != null ) ? new TreeSet<String>(names) : null;

	if ( getId() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "Cannot save unbound profile (no id)");
	for (Attribute a: dattrs.values() ) {
	    // If this attribute cannot write itself, ignore it
	    if ( !(a instanceof AttributeDB ))
		continue;

	    // If only certain names are to be written and this attribute
	    // isn't in the set, ignore it.
	    if ( toSave != null && !toSave.contains(a.getName()))
		continue;

	    AttributeDB dba = (AttributeDB) a;
	    dba.save(getId());
	}
    }
    /**
     * Save all attributes from the database.
     * @throws DeterFault on errors
     */
    public void saveAll() throws DeterFault { save(null); }

    /**
     * Delete the named attributes from the database.  This deletes the
     * atrtributes regardless of their optional status or access.
     * @param names the collection of names to remove. If null, this becomes
     * removeAll.
     * @throws DeterFault on errors
     */
    public void remove(Collection<String> names) throws DeterFault {
	Set<String> toDel = 
	    (names != null ) ? new TreeSet<String>(names) : null;

	if ( getId() == null ) 
	    throw new DeterFault(DeterFault.internal, 
		    "Cannot remove attributes from unbound profile (no id)");
	for (Attribute a: dattrs.values() ) {
	    // If this attribute cannot write itself, ignore it
	    if ( !(a instanceof AttributeDB ))
		continue;

	    // If only certain names are to be removed and this attribute
	    // isn't in the set, ignore it.
	    if ( toDel != null && !toDel.contains(a.getName()))
		continue;

	    AttributeDB dba = (AttributeDB) a;
	    dba.remove(getId());
	}
    }

    /**
     * Remove all the profile's attributes from the database.  Unless you are
     * deleting a profile, you probably don't want to do this.
     * @throws DeterFault on errors
     */
    public void removeAll() throws DeterFault { remove(null); }

    /**
     * Return the named attribute, if any valid attribute exists with that name.
     * @param aname the name to find
     * @return the named attribute or null
     */
    public AttributeDB lookupAttribute(String aname) {
	return dattrs.get(aname);
    }

    /**
     * Add an attribute to this profile. 
     * @param a the attribute to add
     */
    public void addAttribute(AttributeDB a) {
	dattrs.put(a.getName(), a);
    }

    /**
     * Return all the attributes as a Collection.
     * @return all the attributes as a Collection
     */
    public Collection<AttributeDB> getAttributes() { return dattrs.values(); }

    /**
     * Return an exportable version of this profile. 
     * @return an exportable version of this profile.
     */
    public Profile export() {
	Profile p = new Profile();
	Vector<Attribute> exportAttrs = new Vector<Attribute>();

	for ( Attribute ua: getAttributes())
	    exportAttrs.add(ua.export());
	p.setId(getId());
	p.setAttributes(exportAttrs.toArray(new Attribute[0]));
	return p;
    }

    /**
     * Return the profile type in all lower case.
     * @return the profile type in all lower case.
     */
    public abstract String getType();
    /**
     * Return the profile type with the first letter in caps.
     * @return the profile type with the first letter in caps.
     */
    public abstract String getTypeCaps();
}
