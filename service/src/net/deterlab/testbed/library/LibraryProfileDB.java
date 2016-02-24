package net.deterlab.testbed.library;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.db.SharedConnection;

import net.deterlab.testbed.profile.ProfileDB;

/**
 * A library profile: the meta data stored by the testbed that identifies the
 * user.  @author the DETER Team
 * @version 1.0
 */
public class LibraryProfileDB extends ProfileDB {

    /**
     * A UserAttribute stored in a field in a table of a database
     * @author DETER Team
     * @version 1.0
     */
    public class LibraryAttributeDB extends AttributeDB {
	/**
	 * Initialize a LibraryAttributeDB.
	 * @param n the attribute name
	 * @param t the attribute data type
	 * @param opt true if the attribute is optional
	 * @param a the access permissions (see constants)
	 * @param d the description of the attribute
	 * @param f the regular expression defining the format (may be null)
	 * @param fd the natural language description of the format (may be null)
	 * @param o the ordering hint
	 * @param l the length hint
	 */
	public LibraryAttributeDB(String n, String t, boolean opt, String a,
		String d, String f, String fd, int o, int l) {
	    super(n, t, opt, a, d, f, fd, o, l);
	}

	/**
	 * Initialize a LibraryAttributeDB.
	 * @param n the attribute name
	 * @param v the attribute value
	 * @param t the attribute data type
	 * @param opt true if the attribute is optional
	 * @param a the access permissions (see constants)
	 * @param d the description of the attribute
	 * @param f the regular expression defining the format (may be null)
	 * @param fd the natural language description of the format (may be null)
	 * @param o the ordering hint
	 * @param l the length hint
	 */
	public LibraryAttributeDB(String n, String v, String t, boolean opt,
		String a, String d, String f, String fd, int o, int l) {
	    super(n, v, t, opt, a, d, f, fd, o, l);
	}

	/**
	 * Initialize a LibraryAttributeDB from some other form of Attribute
	 * @param a the attribute
	 */
	public LibraryAttributeDB(Attribute a) {
	    this(a.getName(), a.getValue(), a.getDataType(), a.getOptional(),
		    a.getAccess(), a.getDescription(), a.getFormat(),
		    a.getFormatDescription(), a.getOrderingHint(),
		    a.getLengthHint());
	}

	/**
	 * Create a new LibraryAttributeDB
	 */
	public LibraryAttributeDB() { super(); }

	/**
	 * Return the table containing the attribute schema for this attribute.
	 * @return the table containing the attribute schema for this attribute
	 */
	public String getSchemaTable() { return "libraryattribute"; }

	/**
	 * Return the table containing the value schema for this attribute.
	 * @return the table containing the value schema for this attribute
	 */
	public String getValuesTable() { return "libraryattributevalue"; }

	/**
	 * Return the table containing the identity resolver for this attribute.
	 * @return the table containing the identity resolver schema for this
	 * attribute
	 */
	public String getIdJoinTable() { return "libraries"; }

	/**
	 * Return the column in the schema that holds the index of the joining
	 * id
	 * @return the column in the schema that holds the index of the joining
	 * id
	 */
	public String getIdJoinColumn() { return "lidx"; }

	/**
	 * Return the column in the identifier table that holds the index of the
	 * joining id
	 * @return the column in the identifier table that holds the index of
	 * the joining id
	 */
	public String getIdJoinKey() { return "libid"; }
    }
    /**
     * Create a LibraryProfile with no libID and empty attributes.
     * @throws DeterFault if there is an underlying database error
     */
    public LibraryProfileDB() throws DeterFault {
	super();
    }

    /**
     * Create an LibraryProfile with the given libID and empty attributes.
     * @param e the eid
     * @throws DeterFault if there is an underlying database error
     */
    public LibraryProfileDB(String e) throws DeterFault {
	super();
    }

    /**
     * Create a LibraryProfile with no libID and empty attributes connected to
     * a shared database connection.
     * @param sc the shared connection
     * @throws DeterFault if there is an underlying database error
     */
    public LibraryProfileDB(SharedConnection sc) throws DeterFault {
	super(sc);
    }

    /**
     * Create an LibraryProfile with the given libID and empty attributes
     * connected to a shared DB connection.
     * @param n the libID
     * @param sc the shared connection
     * @throws DeterFault if there is an underlying database error
     */
    public LibraryProfileDB(String n, SharedConnection sc)
	    throws DeterFault {
	super(n, sc);
    }

    /**
     * Get an instance of an attribute for this profile.
     * @return an instance of an attribute for this profile.
     */
    public AttributeDB getAttribute() { return new LibraryAttributeDB(); }

    /**
     * Get an instance of an attribute for this profile, initialized from an
     * Attribute.
     * @param a the Attribute to use as a template
     * @return an instance of an attribute for this profile.
     */
    public AttributeDB getAttribute(Attribute a) {
	return new LibraryAttributeDB(a);
    }

    /**
     * Return the profile type in all lower case.
     * @return the profile type in all lower case.
     */
    public String getType() { return "library"; }
    /**
     * Return the profile type with the first letter in caps.
     * @return the profile type with the first letter in caps.
     */
    public String getTypeCaps() { return "Library"; }
}
