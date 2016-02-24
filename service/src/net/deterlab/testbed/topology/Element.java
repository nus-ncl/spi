package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.Collection;
import java.util.Map;

/**
 * An element in the topology - something that connects to substrates using
 * interfaces.  Almost a typedef to ConnectedObject.  The distinction is so
 * that Substrates are Connected Objects, but not Elements.
 * @author DeterTeam
 * @version 1.0
 */
public class Element extends ConnectedObject implements Cloneable {
    /** The element's (unique) name */
    private String name;
    /**
     * Basic initializer
     */
    public Element() {
	super();
	name = null;
	setUniqueInterfaces(true);
	setSerializeInterfaces(true);
    }

    /**
     * Initialize an element's interfaces and attributes.  Connect the
     * interfaces to this element.  Note that interfaces with the same name are
     * dripped silently.
     * @param n the name
     * @param ifs the interfaces - not copied
     * @param a the atrributes (may be null or empty)
     */
    public Element(String n, Collection<Interface> ifs, Map<String, String> a) {
	super(ifs, a, true, true);
	name = n;
	for (Interface i: getInterfaces()) 
	    i.connect(this, (Substrate) null);
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Element))
	    throw new IsomorphismException("Not an Element", o);
	super.sameAs(o);
	Element e = (Element) o;
	if ( !equalObjs(name, e.getName()))
	    throw new IsomorphismException("Different names", o);
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
     * Clone this thing.  Subclasses must override.  This returns null.
     * @return null
     */
    public Element clone() { return null; }

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

	if ( ename != null) p.println("<" + ename + ">");
	p.println("<name>" + name + "</name>");
	super.writeXML(p, null);
	if ( ename != null) p.println("</" + ename + ">");
	p.flush();
    }

}
