package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

/**
 * A TopologyObject that can have attributes attached.
 * @author DeterTeam
 * @version 1.0
 */
public class AttributedObject extends TopologyObject {
    /** Attributes attached to this object */
    private Map<String, String> attrs;
    /**
     * Basic initializer
     */
    public AttributedObject() {
	super();
	attrs = new HashMap<String, String>();
    }

    /**
     * Copy constructor
     * @param to the object to copy
     */
    public AttributedObject(AttributedObject to) {
	super(to);
	attrs = new HashMap<String, String>(to.attrs);
    }

    /**
     * Initialize the attributes from a map
     * @param a map of key/value pairs from which to initialize the attributes
     */
    public AttributedObject(Map<String, String> a) {
	super();
	attrs = (a != null) ?
	    new HashMap<String, String>(a): new HashMap<String, String>();
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof AttributedObject))
	    throw new IsomorphismException("Not an AttributedObject", o);
	AttributedObject t = (AttributedObject) o;
	if (!attrs.equals(t.getAttributes()))
	    throw new IsomorphismException("Mismatched Attributes", o);
    }

    /**
     * Get the string associated with the given attribute name.
     * @param name the given attribute name
     * @return the value
     */
    public String getAttribute(String name) {
	return attrs.get(name);
    }

    /**
     * Set the string associated with the given attribute name.
     * @param name the given attribute name
     * @param value the value
     */
    public void setAttribute(String name, String value) {
	attrs.put(name, value);
    }

    /**
     * Remove the given attribute from this object.
     * @param name the given attribute name
     */
    public void removeAttribute(String name) {
	attrs.remove(name);
    }

    /**
     * Return the attribute map: Note the package scope.
     * @return the attribute map
     */
    Map<String, String> getAttributes() { return attrs; }

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

	for (Map.Entry<String, String> e : attrs.entrySet())  {
	    if (ename != null) p.println("<" + ename + ">");
	    p.println("<attribute>" + e.getKey() + "</attribute>");
	    p.println("<value>" + e.getValue() + "</value>");
	    if (ename != null) p.println("</" + ename + ">");
	}
	p.flush();
    }
}
