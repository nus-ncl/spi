package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

/**
 * An interface to at least one communication substrate.  Details like address
 * assignement are in attributes.
 * @author DeterTeam
 * @version 1.0
 */
public class Interface extends AttributedObject implements Cloneable {
    /** The substrate to which this interface is attached.  This is the
     * actual substrate, not clones */
    Substrate substrate;
    /** The Element to which this is attached */
    Element elem;
    /** The interface name */
    private String name;
    /** The interface capacity (may be null) */
    private Capacity cap;
    /** The interface latency (may be null) */
    private Latency lat;
    /**
     * Basic initializer
     */
    public Interface() {
	super();
	substrate = null;
	elem = null;
	name = null;
	cap = null;
	lat = null;
    }

    /**
     * Copy constructor.  Return an *unconnected* copy of the given interface.
     * @param i the object to copy
     */
    public Interface(Interface i) {
	this(null, i.getName(), i.getCapacity(), i.getLatency(),
		i.getAttributes());
    }

    /**
     * Initialize an interface from parts.  Calls connect on the collection of
     * substrates, if any.  Does not connect to an Element.
     * @param s the substrates
     * @param n the name
     * @param c the capacity (may be null)
     * @param l the latency (may be null)
     * @param a the atrributes (may be null or empty)
     */
    public Interface(Substrate s, String n, Capacity c, Latency l,
	    Map<String, String> a) {
	super(a);
	name = n;
	elem = null;
	cap =  (c != null ) ? new Capacity(c) : null;
	lat =  (l != null ) ? new Latency(l) : null;
	substrate = s;
	connect(null, s);
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Interface))
	    throw new IsomorphismException("Not an Interface", o);
	super.sameAs(o);

	Interface i = (Interface) o;

	if ( !equalObjs(name, i.getName()))
	    throw new IsomorphismException("Different Names", o);

	Element otherE = i.getElement();

	if ( (elem == null && otherE != null) ||
		(elem != null && otherE == null))
	    throw new IsomorphismException("Different element attachments", o);

	if ( elem != null && otherE != null)
	    if ( !equalObjs(elem.getName(), otherE.getName()))
		throw new IsomorphismException(
			"Connected to different elements", o);
	// The only other choice is elem and otherE both null, which is OK.

	Substrate otherS = i.getSubstrate();

	if ( (substrate == null && otherS != null) ||
		(substrate != null && otherS == null))
	    throw new IsomorphismException(
		    "Different substrate attachments", o);

	if ( substrate != null && otherS != null)
	    if ( !equalObjs(substrate.getName(), otherS.getName()))
		throw new IsomorphismException(
			"Connected to different substrates", o);
	// The only other choice is substrate and otherS both null, which is
	// OK.

	try {
	    if ( cap != null ) cap.sameAs(i.getCapacity());
	    else {
		if ( i.getCapacity() != null)
		    throw new IsomorphismException("Capacity Missing");
	    }
	    if ( lat != null ) lat.sameAs(i.getLatency());
	    else {
		if ( i.getLatency() != null)
		    throw new IsomorphismException("Latency Missing");
	    }
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

    /**
     * Return an unconnected clone of this interface.
     * @return an unconnected clone of this interface.
     */
    public Interface clone() {
	return new Interface(this);
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
     * Return the element to which this is attached.  Use connect to change it.
     * @return the element to which this is attached.
     */
    public Element getElement() { return elem; }

    /**
     * Get the substrate. 
     * @return the substrate
     */
    public Substrate getSubstrate() { return substrate; }

    /**
     * Set the substrate. 
     * @param s the new substrate
     */
    public void setSubstrate(Substrate s) { substrate = s; connect(null, s); }

    /**
     * Connect an interface to a collection of substrates and an element.
     * These arre new connections to substrates - existing connections will
     * remain, including element to interface connections. To move an
     * interface, disconnect it and reconnect it.
     * @param e the element (if null, nothing is changed)
     * @param sub the substrate (may be null)
     */
    public void connect(Element e, Substrate sub) {
	if ( e != null ) {
	    elem = e;
	    e.addInterface(this);
	}
	if ( sub == null ) return;
	sub.addInterface(this);
	substrate = sub;
    }

    /**
     * Disconnect an interface from a of substrate.  Note: to
     * disconnect from the only substrate disconnect(getSubstrate()) will work.
     * @param sub the substrate (may be null)
     */
    public void disconnect(Substrate sub) {
	if ( sub == null ) return;
	sub.removeInterface(this);
	substrate = null;
    }

    /**
     * Disconnect from the current element.
     */
    public void disconnect() {
	if (elem == null ) return;
	elem.removeInterface(this);
    }

    /**
     * Disconnect from the element and all substrates.
     */
    public void disconnectAll() {
	disconnect();
	disconnect(getSubstrate());
    }

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
	p.println("<substrate>" + substrate.getName() + "</substrate>");
	p.println("<name>" + name + "</name>");
	if ( cap != null ) cap.writeXML(p, "capacity");
	if ( lat != null ) lat.writeXML(p, "latency");
	super.writeXML(p, "attribute");
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
