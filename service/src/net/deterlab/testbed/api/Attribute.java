package net.deterlab.testbed.api;

/**
 * A field in a Profile, including the schema information (field metadata) and
 * the value contained in the field represented as a string.  Any of the
 * abstractions with profiles attached use this same definition of an
 * attribute.
 * @author The DETER Team
 * @version 1.0
 */
public class Attribute extends ApiObject implements Cloneable {
    /** Access constant: attribute can be read and written */
    public static final String READ_WRITE = "READ_WRITE";
    /** Access constant: attribute can only be read */
    public static final String READ_ONLY = "READ_ONLY";
    /** Access constant: attribute can only be written */
    public static final String WRITE_ONLY = "WRITE_ONLY";
    /** Access constant: users can neither read nor write - internal only */
    public static final String NO_ACCESS = "NO_ACCESS";
    /** Name of the attribute */
    private String name;
    /** Value of an attribute. */
    private String value;
    /** The type of data encoded */
    private String dataType;
    /** True if the attribute is optional */
    private boolean optional;
    /** The access a user has to this attribute */
    private String access;
    /** Human-readable description of the attribute */
    private String description;
    /** format of the field as a regular expression */
    private String format;
    /** Description of the format in natural language */
    private String formatDescription;
    /** Ordering hint */
    private int order;
    /** Field length hint */
    private int len;


    /**
     * Initialize an empty attribute
     */
    public Attribute() {
	name = null;
	value = null;
	dataType = null;
	optional = true;
	access = NO_ACCESS;
	description = null;
	format = null;
	formatDescription = null;
	order = 0;
	len = 0;
    }

    /**
     * Initialize an attribute with no value.
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
    public Attribute(String n, String t, boolean opt, String a, String d,
	    String f, String fd, int o, int l) {
	name = n;
	value = null;
	dataType = t;
	optional = opt;
	access = a;
	description = d;
	format = f;
	formatDescription = fd;
	order = o;
	len=l;
    }

    /**
     * Initialize an attribute with a string value.
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
    public Attribute(String n, String v, String t, boolean opt, String a, 
	    String d, String f, String fd, int o, int l) {
	this(n, t, opt, a, d, f, fd, o, l);
	value = v;
    }
    /**
     * Create a copy of this attribute.
     * @return a copy of this attribute
     */
    public Object clone() throws CloneNotSupportedException { 
	return super.clone();
    }

    /**
     * Return the attribute name
     * @return the attribute name
     */
    public String getName() { return name; }

    /**
     * Set the attribute name.
     * @param n the new name
     */
    public void setName(String n) { name = n; }

    /**
     * Assign the attribute a string value.
     * @param v the value to assign
     */
    public void setValue(String v) { value = v; }
    /**
     * Get the value of the attribute as a string.  This will return null for
     * binary-valued attributes.
     * @return the string value or null
     */
    public String getValue() { return value; }
    /**
     * Return the data type of the attribute
     * @return the data type of the attribute
     */
    public String getDataType() { return dataType; }

    /**
     * Set the attribute's data type
     * @param d the new type.
     */
    public void setDataType(String d) { dataType = d; }
    /**
     * Return true if the attribute is optional
     * @return true if the attribute is optional
     */
    public boolean getOptional() { return optional; }

    /**
     * Set the optional status of the attribute
     * @param b the new status
     */
    public void setOptional(boolean b) { optional = b; }
    /**
     * Return the user access to this attribute.  One of:
     * READ_WRITE
     * READ_ONLY
     * WRITE_ONLY
     * NO_ACCESS
     * @return the user access to this attribute.
     */
    public String getAccess() { return access; }

    /**
     * Set the user access to this attribute.  One of
     * READ_WRITE
     * READ_ONLY
     * WRITE_ONLY
     * NO_ACCESS
     * @param a the new access
     */
    public void setAccess(String a) { access = a; }
    /**
     * Return the human-readable description of this element.
     * @return the human-readable description of this element.
     */
    public String getDescription() { return description; }
    /**
     * Set the human-readable description of this element.
     * @param d the human-readable description of this element.
     */
    public void setDescription(String d) { description = d; }

    /**
     * Return the attribute format, a regular expression
     * @return the attribute format, a regular expression
     */
    public String getFormat() { return format; }

    /**
     * Set the attribute format, a regular expression
     * @param f the attribute format, a regular expression
     */
    public void setFormat(String f) { format = f; }

    /**
     * Return the format description in natural language
     * @return the format description in natural language
     */
    public String getFormatDescription() { return formatDescription; }

    /**
     * Set the format description in natural language
     * @param f the format description in natural language
     */
    public void setFormatDescription(String f) { formatDescription = f; }

    /**
     * Return an integer indicating the preferred display order of this
     * attribute.
     * @return the ordering hint
     */
    public int getOrderingHint() { return order; }

    /**
     * Set an integer indicating the preferred display order of this
     * attribute.
     * @param o the ordering hint
     */
    public void setOrderingHint(int o) { order = o; }

    /**
     * Return an integer indicating an estimate of the number of characters in
     * the field.
     * @return the length hint
     */
    public int getLengthHint() { return len; }

    /**
     * Set the estimate of the number of characters in the field.
     * @param l the length hint
     */
    public void setLengthHint(int l) { len = l; }

    /**
     * Convert the string value of access stored in the DB into the integer
     * stored in the object.
     * @param access the string representation
     * @return the integer representation
     */
    static public boolean validateAccess(String access) {
	if (access.equals("READ_WRITE")) return true;
	else if (access.equals("READ_ONLY")) return true;
	else if (access.equals("WRITE_ONLY")) return true;
	else if (access.equals("NO_ACCESS")) return true;
	else return false;
    }

    /**
     * Return an Attribute version of this object, suitable for web services
     * export.  If the attribute is WRITE_ONLY, the value will be cleared.
     * Subclasses must override this.
     * @return an Attribute version of this object for web services export.
     */
    public Attribute export() { 
	Attribute rv = null;
	try {
	    rv = (Attribute) clone(); 
	} catch (CloneNotSupportedException ignored) { 
	    return null;
	}
	if (WRITE_ONLY.equals(getAccess()) || 
		NO_ACCESS.equals(getAccess())) rv.setValue(null);
	return rv;
    }

    /**
     * Return a string representation of the attribute.
     * @return a string representation of the attribute
     */
    public String toString() {
	return "Name: " + getName() + " Value: " + getValue();
    }
}
