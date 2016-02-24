package net.deterlab.testbed.api;

/**
 * A request to change the value of a Profile Attribute.  The request includes
 * the name of the attribute to assign a value to, the new value.  The request
 * also contains a detete flag.  If that flag is set, the value of that
 * attribute is removed from the given profile.  This class is only used to
 * modify values, not schemae.
 *
 * @author the DETER Team
 * @version 1.0
 */
public class ChangeAttribute extends ApiObject {
    /** Name of the attribute to change */
    protected String name;
    /** New value for an attribute */
    protected String value;
    /** True if this is a deletion request*/
    protected boolean deleteMe;

    /**
     * Construct an empty ChangeAttribute
     */
    public ChangeAttribute() { }

    /**
     * Get the name of the attribute to change
     * @return the name to change
     */
    public String getName() { return name; }
    /**
     * Set the name of the attribute to change
     * @param n the new name to change
     */
    public void setName(String n) { name = n; }
    /**
     * Get the string value requested
     * @return the string value requested
     */
    public String getValue() { return value; }
    /**
     * Set the string value requested
     * @param v the string value requested
     */
    public void setValue(String v) { value = v; }
    /**
     * Return true if this is a request to delete the given parameter entirely.
     * @return true if this is a request to delete the given parameter entirely.
     */
    public boolean getDelete() { return deleteMe; }
    /**
     * Set delete flag.  When set, this request asks to delete the field.
     * @param b the new value of the delete flag
     */
    public void setDelete(boolean b) { deleteMe = b; }
}
