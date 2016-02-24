package net.deterlab.testbed.project;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.profile.ProfileDB;

/**
 * A project profile: the meta data stored by the testbed that identifies the
 * project.
 * @author the DETER Team
 * @version 1.0
 */
public class ProjectProfileDB extends ProfileDB {
    /**
     * A ProjectAttribute stored in a field in a table of a database
     * @author DETER Team
     * @version 1.0
     */
    public class ProjectAttributeDB extends AttributeDB {
	/**
	 * Initialize a ProjectAttributeDB.
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
	public ProjectAttributeDB(String n, String t, boolean opt, String a,
		String d, String f, String fd, int o, int l) {
	    super(n, t, opt, a, d, f, fd, o, l);
	}

	/**
	 * Initialize a ProjectAttributeDB.
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
	public ProjectAttributeDB(String n, String v, String t, boolean opt,
		String a, String d, String f, String fd, int o, int l) {
	    super(n, v, t, opt, a, d, f, fd, o, l);
	}

	/**
	 * Initialize a ProjectAttributeDB from some other form of Attribute
	 * @param a the attribute
	 */
	public ProjectAttributeDB(Attribute a) {
	    this(a.getName(), a.getValue(), a.getDataType(), a.getOptional(),
		    a.getAccess(), a.getDescription(), a.getFormat(),
		    a.getFormatDescription(), a.getOrderingHint(),
		    a.getLengthHint());
	}

	/**
	 * Create a new ProjectAttributeDB
	 */
	public ProjectAttributeDB() { super(); }

	/**
	 * Return the table containing the attribute schema for this attribute.
	 * @return the table containing the attribute schema for this attribute
	 */
	public String getSchemaTable() { return "projectattribute"; }

	/**
	 * Return the table containing the value schema for this attribute.
	 * @return the table containing the value schema for this attribute
	 */
	public String getValuesTable() { return "projectattributevalue"; }

	/**
	 * Return the table containing the identity resolver for this attribute.
	 * @return the table containing the identity resolver schema for this
	 * attribute
	 */
	public String getIdJoinTable() { return "projects"; }

	/**
	 * Return the column in the schema that holds the index of the joining
	 * id
	 * @return the column in the schema that holds the index of the joining
	 * id
	 */
	public String getIdJoinColumn() { return "pidx"; }

	/**
	 * Return the column in the identifier table that holds the index of the
	 * joining id
	 * @return the column in the identifier table that holds the index of
	 * the joining id
	 */
	public String getIdJoinKey() { return "projectid"; }
    }
    /**
     * Create a ProjectProfile with no uid and empty attributes.
     * @throws DeterFault if there is an underlying database error
     */
    public ProjectProfileDB() throws DeterFault {
	super();
    }

    /**
     * Create an ProjectProfile with the given uid and empty attributes.
     * @param p the projectID
     * @throws DeterFault if there is an underlying database error
     */
    public ProjectProfileDB(String p) throws DeterFault {
	super(p);
    }

    /**
     * Create a ProjectProfile with no uid and empty attributes and a shared DB
     * connection.
     * @param sc the shared connection
     * @throws DeterFault if there is an underlying database error
     */
    public ProjectProfileDB(SharedConnection sc) throws DeterFault {
	super(sc);
    }

    /**
     * Create an ProjectProfile with the given uid and empty attributes and a
     * shared database conncetion.
     * @param p the projectID
     * @param sc the shared connection
     * @throws DeterFault if there is an underlying database error
     */
    public ProjectProfileDB(String p, SharedConnection sc) throws DeterFault {
	super(p);
    }

    /**
     * Get an instance of an attribute for this profile.
     * @return an instance of an attribute for this profile.
     */
    public AttributeDB getAttribute() { return new ProjectAttributeDB(); }

    /**
     * Get an instance of an attribute for this profile, initialized from an
     * Attribute.
     * @param a the Attribute to use as a template
     * @return an instance of an attribute for this profile.
     */
    public AttributeDB getAttribute(Attribute a) {
	return new ProjectAttributeDB(a);
    }

    /**
     * Return the profile type in all lower case.
     * @return the profile type in all lower case.
     */
    public String getType() { return "project"; }
    /**
     * Return the profile type with the first letter in caps.
     * @return the profile type with the first letter in caps.
     */
    public String getTypeCaps() { return "Project"; }
}
