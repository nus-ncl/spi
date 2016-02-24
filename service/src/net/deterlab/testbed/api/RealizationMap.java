package net.deterlab.testbed.api;

/**
 * This represents the resource to topology mapping in a realization.
 * @author the DETER Team
 * @version 1.0
 */
public class RealizationMap extends ApiObject {
    /** The name of the containing resource*/
    private String resource;
    /** The name of the containing resource*/
    private String topologyName;

    /**
     * Create an empty description
     */
    public RealizationMap() { 
	resource = null;
	topologyName = null;
    }

    /**
     * Create a new map with the given values
     * @param r the resource name
     * @param t the topology name
     */
    public RealizationMap(String r, String t) {
	this();
	resource = r;
	topologyName = t;
    }
    /**
     * Return the name of the resource name
     * @return the name of the resource name
     */
    public String getResource() { return resource; }
    /**
     * Set the name of the resource name
     * @param n the new name of the resource name
     */
    public void setResource(String n) { resource = n; }
    /**
     * Return the name of the topology name
     * @return the name of the topology name
     */
    public String getTopologyName() { return topologyName; }
    /**
     * Set the name of the topology name
     * @param n the new name of the topology name
     */
    public void setTopologyName(String n) { topologyName = n; }
}
