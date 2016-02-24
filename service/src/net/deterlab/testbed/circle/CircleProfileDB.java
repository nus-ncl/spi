package net.deterlab.testbed.circle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.profile.ProfileDB;

/**
 * A circle profile: the meta data stored by the testbed that identifies the
 * circle.
 * @author the DETER Team
 * @version 1.0
 */
public class CircleProfileDB extends ProfileDB {
    /**
     * A CircleAttribute stored in a field in a table of a database
     * @author DETER Team
     * @version 1.0
     */
    public class CircleAttributeDB extends AttributeDB {
	/**
	 * Initialize a CircleAttributeDB.
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
	public CircleAttributeDB(String n, String t, boolean opt,
		String a, String d, String f, String fd, int o, int l) {
	    super(n, t, opt, a, d, f, fd, o, l);
	}

	/**
	 * Initialize a CircleAttributeDB.
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
	public CircleAttributeDB(String n, String v, String t, boolean opt,
		String a, String d, String f, String fd, int o, int l) {
	    super(n, v, t, opt, a, d, f, fd, o, l);
	}

	/**
	 * Initialize a CircleAttributeDB from some other form of Attribute
	 * @param a the attribute
	 */
	public CircleAttributeDB(Attribute a) {
	    this(a.getName(), a.getValue(), a.getDataType(), a.getOptional(),
		    a.getAccess(), a.getDescription(), a.getFormat(),
		    a.getFormatDescription(), a.getOrderingHint(),
		    a.getLengthHint());
	}

	/**
	 * Create a new CircleAttributeDB
	 */
	public CircleAttributeDB() { super(); }

	/**
	 * Return the table containing the attribute schema for this attribute.
	 * @return the table containing the attribute schema for this attribute
	 */
	public String getSchemaTable() { return "circleattribute"; }

	/**
	 * Return the table containing the value schema for this attribute.
	 * @return the table containing the value schema for this attribute
	 */
	public String getValuesTable() { return "circleattributevalue"; }

	/**
	 * Return the table containing the identity resolver for this attribute.
	 * @return the table containing the identity resolver schema for this
	 * attribute
	 */
	public String getIdJoinTable() { return "circles"; }

	/**
	 * Return the column in the schema that holds the index of the joining
	 * id
	 * @return the column in the schema that holds the index of the joining
	 * id
	 */
	public String getIdJoinColumn() { return "cidx"; }

	/**
	 * Return the column in the identifier table that holds the index of
	 * the joining id
	 * @return the column in the identifier table that holds the index of
	 * the joining id
	 */
	public String getIdJoinKey() { return "circleid"; }
    }
    /**
     * Create a CircleProfile with no uid and empty attributes.
     * @throws DeterFault if there is an underlying database error
     */
    public CircleProfileDB() throws DeterFault{
	super();
    }

    /**
     * Create an CircleProfile with the given uid and empty attributes.
     * @param u the circleID
     * @throws DeterFault if there is an underlying database error
     */
    public CircleProfileDB(String u) throws DeterFault{
	super(u);
    }

    /**
     * Create a CircleProfile with no uid and empty attributes connected to a
     * shared DB connection.
     * @param sc sc the shared connection
     * @throws DeterFault if there is an underlying database error
     */
    public CircleProfileDB(SharedConnection sc) throws DeterFault{
	super(sc);
    }

    /**
     * Create an CircleProfile with the given uid and empty attributes.
     * @param u the circleID
     * @param sc sc the shared connection
     * @throws DeterFault if there is an underlying database error
     */
    public CircleProfileDB(String u, SharedConnection sc) throws DeterFault{
	super(u, sc);
    }
    /**
     * Get an instance of an attribute for this profile.
     * @return an instance of an attribute for this profile.
     */
    public AttributeDB getAttribute() { return new CircleAttributeDB(); }

    /**
     * Get an instance of an attribute for this profile, initialized from an
     * Attribute.
     * @param a the Attribute to use as a template
     * @return an instance of an attribute for this profile.
     */
    public AttributeDB getAttribute(Attribute a) {
	return new CircleAttributeDB(a);
    }

    /**
     * Set dummy values for a profile that was created internally.  Circles are
     * created and linked to users and projects internally, and those
     * internally created profiles will need to be filled in.  Accordingly, if
     * non optional profile attributes are defined for circles that do not have
     * good defaults, this will break.
     * @param p the profile to initialize
     * @param defString the string to insert into string fields
     * @return the required field names
     */
    static public Collection<String> fillDefaultProfile(ProfileDB p , 
	    String defString ) {
	List<String> req = new ArrayList<String>();

	for (Attribute a: p.getAttributes()) {
	    if ( a.getOptional() ) continue;

	    String dt = a.getDataType().toUpperCase();
	    req.add(a.getName());

	    if (dt.equals("STRING")) a.setValue(defString);
	    else if (dt.equals("INT") || dt.equals("FLOAT")) a.setValue("0");
	    else a.setValue("");
	}
	return req;
    }
    /**
     * Return the profile type in all lower case.
     * @return the profile type in all lower case.
     */
    public String getType() { return "circle"; }
    /**
     * Return the profile type with the first letter in caps.
     * @return the profile type with the first letter in caps.
     */
    public String getTypeCaps() { return "Circle"; }

}
