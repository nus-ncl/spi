package net.deterlab.testbed.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A facet of a Resource as passed through the SPI calls. A facet is a
 * characterization of part of the resource as: computation, communication,
 * storage, sensing, and articulation.  The facets are named by ID, an integer
 * scoped by the containing resource's URI.  Each contains a value (quantity),
 * units, and a set of name value pairs - tags.
 * @author the DETER Team
 * @version 1.0
 */
public class ResourceFacet extends ApiObject {
    /** The Name of the facet */
    private String name;
    /** The type of the aspect */
    private String type;
    /** The subtype of the aspect */
    private double value;
    /** The subtype of the aspect (scoped by type) */
    private String units;
    /** Tags on this facet */
    private List<ResourceTag> tags;

    /**
     * Create an empty description
     */
    public ResourceFacet() { 
	name = null;
	type = null;
	value = 0.0;
	units = null;
	tags = new ArrayList<>();
    }
    /**
     * Return the Name
     * @return the Name
     */
    public String getName() { return name; }
    /**
     * Set the name
     * @param n the new name
     */
    public void setName(String n) { name = n; }
    /**
     * Return the type
     * @return the type
     */
    public String getType() { return type; }
    /**
     * Set the type
     * @param t the new type
     */
    public void setType(String t) { type = t; }
    /**
     * Return the value
     * @return the value
     */
    public double getValue() { return value; }
    /**
     * Set the value
     * @param v the new value
     */
    public void setValue(double v) { value = v; }
    /**
     * Return the units
     * @return the units
     */
    public String getUnits() { return units; }
    /**
     * Set the units
     * @param u the new units
     */
    public void setUnits(String u) { units = u; }

    /**
     * Return the tags as an array
     * @return the tags as an array
     */
    public ResourceTag[] getTags() { return tags.toArray(new ResourceTag[0]); }

    /**
     * Set tags from a tag array
     * @param t the new tags
     */
    public void setTags(ResourceTag[] t) {
	tags.clear();
	for (ResourceTag r : t)
	    tags.add(r);
    }

    /**
     * Set tags from a tag collection
     * @param t the new tags
     */
    public void setTags(Collection<ResourceTag> t) {
	tags.clear();
	tags.addAll(t);
    }
}
