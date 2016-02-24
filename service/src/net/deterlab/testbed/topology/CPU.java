package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.Map;

/**
 * The CPU requirements of a computer or other entity.  The type is a string
 * for simplicity.  We may make this more complex later.
 * @author DeterTeam
 * @version 1.0
 */
public class CPU extends AttributedObject {
    /** The CPU type */
    String type;
    /** Number of copies */
    int count;
    /**
     * Basic initializer
     */
    public CPU() {
	super();
	type = null;
	count = 0;
    }

    /**
     * Construct a CPU from parts
     * @param t the type
     * @param c the count
     * @param a attached attributes
     */
    public CPU(String t, int c, Map<String, String> a) {
	super(a);
	type = t;
	count = c;
    }

    /**
     * Convenience a CPU with only a type
     * @param t the type
     */
    public CPU(String t) {
	this(t, 1, null);
    }

    /**
     * Copy constructor
     * @param c the object to copy
     */
    public CPU(CPU c) {
	this(c.getType(), c.getCount(), c.getAttributes());
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof CPU))
	    throw new IsomorphismException("Not a CPU", o);
	CPU c = (CPU) o;
	if ( getCount() != c.getCount())
	    throw new IsomorphismException("Different CPU counts", o);
	if (!equalObjs(type, c.getType()))
	    throw new IsomorphismException("Different CPU types", o);
    }
    /**
     * Get the type
     * @return the type
     */
    public String getType() { return type; }

    /**
     * Set the type
     * @param t the new type
     */
    public void setType(String t) { type = t; }

    /**
     * Get the count
     * @return the count
     */
    public int getCount() { return count; }

    /**
     * Set the count
     * @param c the new count
     */
    public void setCount(int c) { count = c; }
    /**
     * Output this object's XML representation. If ename is given, surround the
     * output with an element with that name. 
     * @param w the writer for output
     * @param ename the name of the element enclosing this object (may be null)
     * @throws IOException on a writing error.
     */
    public void writeXML(Writer w, String ename) throws IOException {
	if ( count < 1 ) return;

	// Coerce w to a PrintWriter if possible.  Otherwise hook a PrintWriter
	// to it.
	PrintWriter p = (w instanceof PrintWriter) ? 
	    (PrintWriter) w : new PrintWriter(w);

	if (ename != null) p.println("<" + ename + ">");
	p.println("<count>" + count + "</count>");
	p.println("<type>" + type + "</type>");
	super.writeXML(p, "attribute");
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
