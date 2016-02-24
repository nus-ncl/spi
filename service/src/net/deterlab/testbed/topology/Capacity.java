package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

/**
 * The capacity of a substrate.  The rate is in kb/s
 * @author DETER team
 * @version 1.0
 */
public class Capacity extends TopologyObject {
    /** Substrate rate (kb/s) */
    double rate;
    /** The sort of rate: max or average */
    String kind;

    /**
     * Basic constructor
     */
    public Capacity() {
	super();
	rate = 0.0;
	kind = null;
    }

    /**
     * Constructor with both values
     * @param r the rate in kb/s
     * @param k the kind of capacity: max or average
     */
    public Capacity(double r, String k) {
	super();
	rate = r;
	kind = k;
    }

    /**
     * Copy constructor
     * @param c the capacity to clone
     */
    public Capacity(Capacity c) {
	super();
	rate = c.rate;
	kind = c.kind;
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Capacity))
	    throw new IsomorphismException("Not a Capacity", o);
	Capacity c = (Capacity) o;
	if ( getRate() != c.getRate())
	    throw new IsomorphismException("Different rates", o);
	if ( !equalObjs(kind, c.getKind()))
	    throw new IsomorphismException("Different kinds", o);
    }

    /**
     * Get the rate
     * @return the rate
     */
    public double getRate() { return rate; }

    /**
     * Set the rate
     * @param r the new rate
     */
    public void setRate(double r) { rate = r; }

    /**
     * Get the kind
     * @return the kind
     */
    public String getKind() { return kind; }

    /**
     * Set the kind
     * @param k the new kind
     */
    public void setKind(String k) { kind = k; }

    /**
     * Output this object's XML.  
     * @param w the writer for output
     * @param ename the name of the element enclosing this object
     * @throws IOException on a writing error.
     */
    public void writeXML(Writer w, String ename) throws IOException {
	// Coerce w to a PrintWriter if possible.  Otherwise hook a
	// PrintWriter to it.
	PrintWriter p = (w instanceof PrintWriter) ? 
	    (PrintWriter) w : new PrintWriter(w);
	if (ename != null) p.println("<" + ename + ">");
	p.println("<rate>" + rate + "</rate>");
	p.println("<kind>" + kind + "</kind>");
	if (ename != null) p.println("</" + ename + ">");
    }
}
