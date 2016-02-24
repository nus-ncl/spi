package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * A communication medium in a topology.
 * @author DeterTeam
 * @version 1.0
 */
public class Substrate extends ConnectedObject {
    /** The substrate name */
    private String name;
    /** The substrate capacity (may be null) */
    private Capacity cap;
    /** The substrate latency (may be null) */
    private Latency lat;
    /** The operations part of a substrate */
    private OperationalObject oo;

    /** The prefix for default names of Substrates */
    private static String defaultPrefix = "sub-%d";

    /**
     * Basic initializer
     */
    public Substrate() {
	super();
	setUniqueInterfaces(false);
	setSerializeInterfaces(false);
	name = null;
	cap = null;
	lat = null;
	oo = new OperationalObject();
	setPrefix(defaultPrefix);
    }

    /**
     * Copy constructor
     * @param s the object to copy
     */
    public Substrate(Substrate s) {
	this(s.getName(), s.getCapacity(), s.getLatency(), 
		s.getOperationalData().getLocalnames(),
		s.getOperationalData().getStatus(),
		s.getOperationalData().getServices(),
		s.getOperationalData().getOperations(), 
		s.getAttributes());
    }

    /**
     * Initialize a substrate from parts
     * @param n the name
     * @param c the capacity (may be null)
     * @param l the latency (may be null)
     * @param ln the local names (nay be null or empty)
     * @param s the status (may be null)
     * @param ser the services (may be null or empty)
     * @param o the operations (may be null or empty)
     * @param a the atrributes (may be null or empty)
     */
    public Substrate(String n, Capacity c, Latency l, Collection<String> ln,
	    String s, Collection<Service> ser, Collection<String> o, 
	    Map<String, String> a) {
	super(null, a, false, false);
	name = n;
	cap =  (c != null ) ? new Capacity(c) : null;
	lat =  (l != null ) ? new Latency(l) : null;
	oo = new OperationalObject(ln, s, ser, o);
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Substrate))
	    throw new IsomorphismException("Not a Substrate", o);
	super.sameAs(o);
	Substrate s = (Substrate) o;
	if ( !equalObjs(name, s.getName()))
	    throw new IsomorphismException("Different names", o);

	try {
	    if (cap == null ) {
		if (s.getCapacity() != null)
		    throw new IsomorphismException("Unexpected Capacity");
	    }
	    else cap.sameAs(s.getCapacity());

	    if (lat == null ) {
		if (s.getLatency() != null)
		    throw new IsomorphismException("Unexpected Latency");
	    }
	    else lat.sameAs(s.getLatency());

	    if (oo == null ) {
		if (s.getOperationalData() != null)
		    throw new IsomorphismException(
			    "Unexpected OperationalObject");
	    }
	    else oo.sameAs(s.getOperationalData());
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

    /**
     * Return a disconnected copy of this Substrate.  
     * @return a disconnected copy of this Substrate.
     */
    public Substrate clone() {
	return new Substrate(this);
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
     * Get the capacity
     * @return the capacity (may be null)
     */
    public Capacity getCapacity() { return cap ; }

    /**
     * Set the capacity
     * @param c a new capacity
     */
    public void setCapacity(Capacity c) { cap = c; }

    /**
     * Set the capacity
     * @param r the new rate
     * @param k the new kind
     */
    public void setCapacity(double r, String k) { cap = new Capacity(r, k); }

    /**
     * Get the latency
     * @return the latency (may be null)
     */
    public Latency getLatency() { return lat ; }

    /**
     * Set the latency
     * @param c a new latency
     */
    public void setLatency(Latency c) { lat = c; }

    /**
     * Set the latency
     * @param t the new time
     * @param k the new kind
     */
    public void setLatency(double t, String k) { lat = new Latency(t, k); }

    /**
     * Get the operational data
     * @return the operational data
     */
    public OperationalObject getOperationalData() { return oo; }

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
	if ( cap != null ) cap.writeXML(p, "capacity");
	if ( lat != null ) lat.writeXML(p, "latency");
	super.writeXML(p, null);
	oo.writeXML(p, null);
	if ( ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
