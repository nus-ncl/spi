package net.deterlab.testbed.api;

/**
 * This represents the resource conatinments in a realization.
 * @author the DETER Team
 * @version 1.0
 */
public class RealizationContainment extends ApiObject {
    /** The name of the containing resource*/
    private String outer;
    /** The name of the containing resource*/
    private String inner;

    /**
     * Create an empty description
     */
    public RealizationContainment() { 
	outer = null;
	inner = null;
    }

    public RealizationContainment(String n, String v) {
	this();
	outer = n;
	inner = v;
    }
    /**
     * Return the name of the containing resource
     * @return the name of the containing resource
     */
    public String getOuter() { return outer; }
    /**
     * Set the name of the containing resource
     * @param n the new name of the containing resource
     */
    public void setOuter(String n) { outer = n; }
    /**
     * Return the name of the contained resource
     * @return the name of the contained resource
     */
    public String getInner() { return inner; }
    /**
     * Set the name of the contained resource
     * @param n the new name of the contained resource
     */
    public void setInner(String n) { inner = n; }
}
