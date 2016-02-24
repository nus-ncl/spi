package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

/**
 * The capacity of a substrate.  The rate is in kb/s
 * @author DETER team
 * @version 1.0
 */
public class Latency extends TopologyObject {
    /** Substrate latency (ms) */
    double time;
    /** The sort of latency: max or average */
    String kind;

    /**
     * Basic constructor
     */
    public Latency() {
	time = 0.0;
	kind = null;
    }

    /**
     * Constructor with both values
     * @param t the latency in ms
     * @param k the kind of capacity: max or average
     */
    public Latency(double t, String k) {
	time = t;
	kind = k;
    }

    /**
     * Copy constructor
     * @param l the latency to clone
     */
    public Latency(Latency l) {
	time = l.time;
	kind = l.kind;
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Latency))
	    throw new IsomorphismException("Not a Latency", o);
	Latency lat = (Latency) o;

	if ( time != lat.getTime())
	    throw new IsomorphismException("Different times", o);

	if (!equalObjs(kind, lat.getKind()))
	    throw new IsomorphismException("Different kinds", o);
    }

    /**
     * Get the time
     * @return the time
     */
    public double getTime() { return time; }

    /**
     * Set the time
     * @param t the new time
     */
    public void setTime(double t) { time = t; }

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
     * Output this object's XML representation. If ename is given, surround the
     * output with an element with that name. 
     * @param w the writer for output
     * @param ename the name of the element enclosing this object (may be null)
     * @throws IOException on a writing error.
     */
    public void writeXML(Writer w, String ename) throws IOException {
	// Coerce w to a PrintWriter if possible.  Otherwise hook a
	// PrintWriter to it.
	PrintWriter p = (w instanceof PrintWriter) ? 
	    (PrintWriter) w : new PrintWriter(w);
	if (ename != null) p.println("<" + ename + ">");
	p.println("<time>" + time + "</time>");
	p.println("<kind>" + kind + "</kind>");
	if (ename != null) p.println("</" + ename + ">");
    }
}

