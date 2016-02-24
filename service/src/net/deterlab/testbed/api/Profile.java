package net.deterlab.testbed.api;

/**
 * The representation of a profile in the API.  The ID field is generic, and it
 * is otherwise a collection of Attribute objects.
 * 
 * @author the DETER Team
 * @version 1.0
 * @see Attribute
 */
public class Profile extends ApiObject {
    /** The identifier to which this profile belongs */
    private String id;
    /** The attributes associated with this profile */
    private Attribute[] attrs;

    /**
     * Create a Profile with no uid and empty attributes.
     */
    public Profile() {
	id = null;
	attrs = null;
    }

    /**
     * Create a Profile with the given id and attributes.
     * @param i the id
     * @param a the attributes
     */
    public Profile(String i, Attribute[] a) {
	id = i;
	attrs = a;
    }

    /**
     * Create a Profile with the given id and empty attributes.
     * @param i the id
     */
    public Profile(String i) {
	this(i, null);
    }

    /**
     * Return the (generic) identifier.  
     * @return the identifier 
     */
    public String getId() { return id; }
    /**
     * Set the (generic) identifier
     * @param i the identifier 
     */
    public void setId(String i) { id = i; }

    /**
     * Return the attributes
     * @return the attributes
     * @see Attribute
     */
    public Attribute[] getAttributes() { return attrs; }

    /**
     * Set the attributes
     * @param a the array of new attributes
     * @see Attribute
     */
    public void setAttributes(Attribute[] a) { attrs = a; }
}
