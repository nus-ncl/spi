package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * An abstract resource collection on which experiments (or sub-sets thereof)
 * can be realized.
 * @author DeterTeam
 * @version 1.0
 */
public class Testbed extends Element implements Cloneable {
    /** The URI */
    private String uri;
    /** The testbed type */
    private String type;
    /** The operational data */
    private OperationalObject oo;

    /** Used to generate unique names */
    private static String defaultPrefix = "seg-%d";

    /**
     * Basic initializer
     */
    public Testbed() {
	super();
	setPrefix(defaultPrefix);
	uri = null;
	type = null;
	oo = new OperationalObject();
    }

    /**
     * Initialize a testbed from parts
     * @param n the testbed name
     * @param u the URI
     * @param t the type
     * @param ifs the interfaces
     * @param ln the local names (nay be null or empty)
     * @param s the status (may be null)
     * @param ser the services (may be null or empty)
     * @param o the operations (may be null or empty)
     * @param a the atrributes (may be null or empty)
     */
    public Testbed(String n, String u, String t, Collection<Interface> ifs,
	    Collection<String> ln, String s, Collection<Service> ser, 
	    Collection<String> o, Map<String, String> a) {
	super(n, ifs, a);
	setPrefix(defaultPrefix);
	uri = u;
	type = t;
	oo = new OperationalObject(ln, s, ser, o);
    }
    /**
     * Construct a testbed from the given one.  The constructed testbed is
     * disconnected - it has no interfaces.
     * @param t the testbed to copy
     */
    public Testbed(Testbed t) {
	this(t.getName(), t.getURI(), t.getType(), null, 
		t.getOperationalData().getLocalnames(),
		t.getOperationalData().getStatus(),
		t.getOperationalData().getServices(),
		t.getOperationalData().getOperations(),
		t.getAttributes());
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Testbed))
	    throw new IsomorphismException("Not a Testbed", o);
	super.sameAs(o);
	Testbed t = (Testbed) o;
	if ( !equalObjs(uri, t.getURI()))
	    throw new IsomorphismException("Differenr URIs", o);
	if ( !equalObjs(type, t.getType()))
	    throw new IsomorphismException("Differenr types", o);
	try {
	    if ( oo == null ) {
		if (t.getOperationalData() != null)
		    throw new IsomorphismException(
			    "Unexpected OperationalObject");
	    }
	    else oo.sameAs(t.getOperationalData());
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

    /**
     * Return a disconnected copy of this testbed.  No interfaces are present
     * in the clone.
     * @return a disconnected copy of this testbed.
     */
    public Testbed clone() {
	return new Testbed(this);
    }


    /**
     * Get the uri
     * @return the uri
     */
    public String getURI() { return uri; }

    /**
     * Set the uri
     * @param u the new uri
     */
    public void setURI(String u) { uri = u; }

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
     * Get the operational data
     * @return the operational data
     */
    public OperationalObject getOperationalData() { return oo; }

    /**
     * The name of the inner XML element for the usual representation of this
     * element.
     * @return the element name
     */
    public String getXMLElement() { return "testbed"; }

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
	p.println("<uri>" + uri + "</uri>");
	p.println("<type>" + type + "</type>");
	super.writeXML(p, null);
	oo.writeXML(p, null);
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
