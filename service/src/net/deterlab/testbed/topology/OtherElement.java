package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * An element described only by attributes.
 * @author DeterTeam
 * @version 1.0
 */
public class OtherElement extends Element implements Cloneable {
    /**
     * Basic initializer
     */
    public OtherElement() {
	super();
    }

    /**
     * Initialize an OtherElement from parts
     * @param n the name of the element
     * @param ifs the interfaces
     * @param a the atrributes (may be null or empty)
     */
    public OtherElement(String n, Collection<Interface> ifs,
	    Map<String, String> a) {
	super(n, ifs, a);
    }


    /**
     * Construct a copy of the given OtherElement.  It is disconnected - no
     * interfaces are copied.
     * @param o the element to copy
     */
    public OtherElement(OtherElement o) {
	this(o.getName(), null, o.getAttributes());
    }

    /**
     * Return a disconnected clone of this OtherElement.  No interfaces are
     * copied (or present) in the clone.
     * @return a disconnected clone of this OtherElement
     */
    public OtherElement clone() {
	return new OtherElement(this);
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof OtherElement))
	    throw new IsomorphismException("Not an OtherElement", o);
	super.sameAs(o);
    }

    /**
     * The name of the inner XML element for the usual representation of this
     * element.
     * @return the element name
     */
    public String getXMLElement() { return "other"; }

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
	super.writeXML(p, null);
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
