
package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.Map;

/**
 * A piece of software and where to install it on an element.  Some
 * distribution types have the install location embedded in the format,
 * and for them the installation location is empty.  
 * @author DeterTeam
 * @version 1.0
 */
public class Software extends AttributedObject {
    /** The location from which to get the software */
    String location;
    /** The location to install the software in the experiment */
    String install;
    /**
     * Basic initializer
     */
    public Software() {
	super();
	location = null;
	install = null;
    }

    /**
     * Construct from parts
     * @param loc the location
     * @param i the install location
     * @param a the attached attributes
     */
    public Software(String loc, String i, Map<String, String> a) {
	super(a);
	location = loc;
	install = i;
    }

    /**
     * Copy constructor
     * @param s the object to copy
     */
    public Software(Software s) {
	this(s.getLocation(), s.getInstall(), s.getAttributes());
    }
    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Software))
	    throw new IsomorphismException("Not a Software", o);
	super.sameAs(o);
	Software s = (Software) o;
	if ( !equalObjs(location, s.getLocation()))
	    throw new IsomorphismException("Different locations", o);
	if ( !equalObjs(install, s.getInstall()))
	    throw new IsomorphismException("Different install points", o);
    }

    /**
     * Get the location
     * @return the location
     */
    public String getLocation() { return location; }

    /**
     * Set the location
     * @param loc the new location
     */
    public void setLocation(String loc) { location = loc; }

    /**
     * Get the install
     * @return the install
     */
    public String getInstall() { return install; }

    /**
     * Set the install
     * @param i the new install
     */
    public void setInstall(String i) { install = i; }

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
	p.println("<location>" + location + "</location>");
	p.println("<install>" + install + "</install>");
	super.writeXML(p, "attribute");
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
