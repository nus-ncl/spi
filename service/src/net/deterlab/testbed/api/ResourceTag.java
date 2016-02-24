
package net.deterlab.testbed.api;

/**
 * A name/value pair attached to a resource or facet.  This is a way to attach
 * semantics to resources and facets.
 * @author the DETER Team
 * @version 1.0
 */
public class ResourceTag extends ApiObject {
    /** The ID of the facet */
    private String name;
    /** The type of the aspect */
    private String value;

    /**
     * Create an empty description
     */
    public ResourceTag() { 
	name = null;
	value = null;
    }

    public ResourceTag(String n, String v) {
	this();
	name = n;
	value = v;
    }
    /**
     * Return the name
     * @return the ID
     */
    public String getName() { return name; }
    /**
     * Set the name
     * @param n the new name
     */
    public void setName(String n) { name = n; }
    /**
     * Return the value
     * @return the value
     */
    public String getValue() { return value; }
    /**
     * Set the value
     * @param v the new value
     */
    public void setValue(String v) { value = v; }
}
