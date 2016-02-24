
package net.deterlab.testbed.api;

/**
 * An aspect of a Experiment as passed through the SPI calls. An aspect is
 * some data that makes up the experiment, identified by type, subtype, and a
 * name.  That triple must be unique (including a null subtype), and different
 * types of aspect are free to impose their own meaning on the various
 * components.  The data portion of an aspect is either local in the Data
 * field, or stored remotely ay the URI in DataReference.
 * <p>
 * This class is the interface representation of an Aspect.  An Aspect
 * implementation is defined by the <a href="../experiment/Aspect.html">Aspect
 * interface</a>.
 *
 * @author the DETER Team
 * @version 1.0
 * @see net.deterlab.testbed.experiment.Aspect
 */
public class ExperimentAspect extends ApiObject {
    /** The type of the aspect */
    protected String type;
    /** The subtype of the aspect */
    protected String subType;
    /** The subtype of the aspect (scoped by type) */
    protected String name;
    /**  Data about the aspect */
    protected byte[] data;
    /** Reference to data located elsewhere */
    protected String dataRef;

    /**
     * Create an empty description
     */
    public ExperimentAspect() { 
	type = null;
	name = null;
	data = null;
	dataRef = null;
    }
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
     * Return the subtype
     * @return the subtype
     */
    public String getSubType() { return subType; }
    /**
     * Set the subtype
     * @param t the new subtype
     */
    public void setSubType(String t) { subType = t; }
    /**
     * Return the name
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Set the name
     * @param n the new name
     */
    public void setName(String n) { name = n; }
    /**
     * Return the data
     * @return the data
     */
    public byte[] getData() { return data; }
    /**
     * Set the data
     * @param d the new data
     */
    public void setData(byte[] d) { data = d; }
    /**
     * Return the data reference (A URI)
     * @return the data reference (A URI)
     */
    public String getDataReference() { return dataRef; }
    /**
     * Set the data reference (A URI)
     * @param dr the new data reference (A URI)
     */
    public void setDataReference(String dr) { dataRef = dr; }
}
