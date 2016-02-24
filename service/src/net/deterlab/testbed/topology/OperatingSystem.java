
package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.Map;

/**
 * The OS requirements of a computer or other element with an OS.
 * @author DeterTeam
 * @version 1.0
 */
public class OperatingSystem extends AttributedObject {
    /** The OS name */
    String name;
    /** The verison */
    String version;
    /** The distribution name */
    String distro;
    /** The distribution version */
    String distroVersion;

    /**
     * Basic initializer
     */
    public OperatingSystem() {
	super();
	name = null;
	version = null;
	distro = null;
	distroVersion = null;
    }

    /**
     * Construct from parts
     * @param n the name
     * @param v the version
     * @param d the distribution name
     * @param dv the distribution version
     * @param a the attached attributes
     */
    public OperatingSystem(String n, String v, String d, String dv,
	    Map<String, String> a) {
	super(a);
	name = n;
	version = v;
	distro = d;
	distroVersion = dv;
    }

    /**
     * Copy constructor
     * @param o the object to copy
     */
    public OperatingSystem(OperatingSystem o) {
	this(o.getName(), o.getVersion(), o.getDistribution(), 
		o.getDistributionVersion(), o.getAttributes());
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof OperatingSystem))
	    throw new IsomorphismException("Not an OperatingSystem", o);
	super.sameAs(o);
	OperatingSystem os = (OperatingSystem) o;

	if ( !equalObjs(name, os.getName()))
	    throw new IsomorphismException("Different names", o);
	if ( !equalObjs(version, os.getVersion()))
	    throw new IsomorphismException("Different versions", o);
	if ( !equalObjs(distro, os.getDistribution()))
	    throw new IsomorphismException("Different distribution names", o);
	if ( !equalObjs(distroVersion, os.getDistributionVersion()))
	    throw new IsomorphismException("Different distribution versions",o);
    }

    /**
     * Get the name
     * @return the name
     */
    public String getName() { return name; }

    /**
     * Set the name
     * @param n the new name
     */
    public void setName(String n) { name = n; }

    /**
     * Get the version
     * @return the version
     */
    public String getVersion() { return version; }

    /**
     * Set the version
     * @param v the new version
     */
    public void setVersion(String v) { version = v; }


    /**
     * Get the distribution
     * @return the distribution
     */
    public String getDistribution() { return distro; }

    /**
     * Set the distribution
     * @param d the new distribution
     */
    public void setDistribution(String d) { distro = d; }

    /**
     * Get the version
     * @return the version
     */
    public String getDistributionVersion() { return distroVersion; }

    /**
     * Set the version
     * @param v the new version
     */
    public void setDistributionVersion(String v) { distroVersion = v; }

    /**
     * Output this object's XML representation. If ename is given, surround the
     * output with an element with that name. 
     * @param w the writer for output
     * @param ename the name of the element enclosing this object (may be null)
     * @throws IOException on a writing error.
     */
    public void writeXML(Writer w, String ename) throws IOException {

	// Coerce w to a PrintWriter if possible.  Otherwise hook a PrintWriter
	// to it.
	PrintWriter p = (w instanceof PrintWriter) ? 
	    (PrintWriter) w : new PrintWriter(w);

	if (ename != null) p.println("<" + ename + ">");
	if ( name != null )
	    p.println("<name>" + name + "</name>");
	if ( version != null )
	    p.println("<version>" + version + "</version>");
	if ( distro != null ) 
	    p.println("<distribution>" + distro + "</distribution>");
	if ( distroVersion != null ) 
	    p.println("<distributionversion>" + distroVersion + 
		    "</distributionversion>");
	super.writeXML(p, "attribute");
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
