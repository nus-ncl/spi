package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * A collection of resources instantiated on a testbed
 * @author DeterTeam
 * @version 1.0
 */
public class Segment extends Testbed implements Cloneable {
    /** Used to generate unique names */
    private static String defaultPrefix = "seg-%d";
    /**
     * An ID is an identifier for a principal, service, or object.  This type
     * is currently polymorphic o allow different implementations of type,
     * though running code primarily uses localnames and fedids.
     * @author DETER team
     * @version 1.0
     */
    static class ID extends TopologyObject {
	/** A UUID */
	private byte[] uuid;
	/** a fedid */
	private byte[] fedid;
	/** A URI */
	private String uri;
	/** a localname */
	private String localname;
	/** A kerberos user name */
	private String kerberosName;

	/**
	 * An empty ID
	 */
	public ID() { 
	    super();
	    uuid = null;
	    fedid = null;
	    uri = null;
	    localname = null;
	    kerberosName = null;
	}

	/**
	 * Build an ID from parts.  Unspecified parts can be null.
	 * @param u a uuid 
	 * @param i a fedid (ABAC identity)
	 * @param ur a URI
	 * @param ln a localname
	 * @param k a Kerberos user name
	 */
	public ID(byte[] u, byte[] i, String ur, String ln, String k) {
	    super();
	    uuid = u;
	    fedid = i;
	    uri = ur;
	    localname = ln;
	    kerberosName = k;
	}

	/**
	 * Clone an identity
	 * @param i the id to clone;
	 */
	public ID(ID i) {
	    this(i.getUUID(), i.getFedid(), i.getURI(), i.getLocalname(), 
		    i.getKerberosName());
	}

	/**
	 * See if this object and the given one match in isomporphic topologies.
	 * If not throw an exception that tracks the point of inconsistency.
	 * @param o the object to check
	 * @throws IsomorphismException if there is a mismatch
	 */
	public void sameAs(TopologyObject o) throws IsomorphismException {
	    if ( !(o instanceof ID))
		throw new IsomorphismException("Not an ID", o);
	    ID i = (ID) o;
	    if ( !equalByteArrays(uuid, i.getUUID()))
		throw new IsomorphismException("Different UUIDs", o);
	    if ( !equalByteArrays(fedid, i.getFedid()))
		throw new IsomorphismException("Different fedids", o);
	    if ( !equalObjs(uri, i.getURI()))
		throw new IsomorphismException("Different URIs", o);
	    if ( !equalObjs(localname, i.getLocalname()))
		throw new IsomorphismException("Different localnames", o);
	    if ( !equalObjs(kerberosName, i.getKerberosName()))
		throw new IsomorphismException("Different kerbersoNames", o);
	}

	/**
	 * Get the UUID.
	 * @return the UUID
	 */
	public byte[] getUUID() { return uuid; }

	/**
	 * Set the UUID.
	 * @param u the new UUID
	 */
	public void setUUID(byte[] u) { uuid = u; }

	/**
	 * Get the Fedid.
	 * @return the Fedid
	 */
	public byte[] getFedid() { return fedid; }

	/**
	 * Set the Fedid.
	 * @param i the new Fedid
	 */
	public void setFedid(byte[] i) { fedid = i; }

	/**
	 * Get the URI.
	 * @return the URI
	 */
	public String getURI() { return uri; }

	/**
	 * Set the URI.
	 * @param u the new URI
	 */
	public void setURI(String u) { uri = u; }

	/**
	 * Get the Localname.
	 * @return the Localname
	 */
	public String getLocalname() { return localname; }

	/**
	 * Set the Localname.
	 * @param n the new Localname
	 */
	public void setLocalname(String n) { localname = n; }

	/**
	 * Get the KerberosName.
	 * @return the KerberosName
	 */
	public String getKerberosName() { return kerberosName; }

	/**
	 * Set the KerberosName.
	 * @param k the new KerberosName
	 */
	public void setKerberosName(String k) { kerberosName = k; }

	/**
	 * Output this object's XML representation. If ename is given, surround
	 * the output with an element with that name. 
	 * @param w the writer for output
	 * @param ename the name of the element enclosing this object (may be
	 * null)
	 * @throws IOException on a writing error.
	 */
	public void writeXML(Writer w, String ename) throws IOException {
	    // Coerce w to a PrintWriter if possible.  Otherwise hook a
	    // PrintWriter to it.
	    PrintWriter p = (w instanceof PrintWriter) ? 
		(PrintWriter) w : new PrintWriter(w);

	    p.println("<" + ename + ">");
	    if ( uuid != null )
		p.println("<uuid>" + uuid + "</uuid>");
	    if ( fedid != null )
		p.println("<fedid>" + fedid + "</fedid>");
	    if ( uri != null )
		p.println("<uri>" + uri + "</uri>");
	    if ( localname != null )
		p.println("<localname>" + localname + "</localname>");
	    if ( kerberosName != null )
		p.println("<kerberosUsername>" + kerberosName + 
			"</kerberosUsername>");
	    p.println("</" + ename + ">");
	    p.flush();
	}
    }

    /** The allocation ID */
    private ID id;

    /**
     * Basic initializer
     */
    public Segment() {
	super();
	id = null;
	setPrefix(defaultPrefix);
    }

    /**
     * Initialize a testbed from parts
     * @param i the ID
     * @param u the URI
     * @param n the name
     * @param t the type
     * @param ifs the interfaces
     * @param ln the local names (nay be null or empty)
     * @param s the status (may be null)
     * @param ser the services (may be null or empty)
     * @param o the operations (may be null or empty)
     * @param a the atrributes (may be null or empty)
     */
    public Segment(ID i, String n, String u, String t,
	    Collection<Interface> ifs, Collection<String> ln,
	    String s, Collection<Service> ser, Collection<String> o,
	    Map<String, String> a) {
	super(n, u, t, ifs, ln, s, ser, o, a);
	id = (i != null ) ? new ID(i) : null;
	setPrefix(defaultPrefix);
    }

    /**
     * Construct a segment from the given one.  The constructed Segment is
     * disconnected - it has no interfaces.
     * @param s the segment to copy
     */
    public Segment(Segment s) {
	this(s.getID(), s.getName(), s.getURI(), s.getType(), null, 
		s.getOperationalData().getLocalnames(),
		s.getOperationalData().getStatus(),
		s.getOperationalData().getServices(),
		s.getOperationalData().getOperations(),
		s.getAttributes());
    }
    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Segment))
	    throw new IsomorphismException("Not a Segment", o);
	super.sameAs(o);
	Segment s = (Segment) o;

	if ( id != null) {
	    if (s.getID() != null)
		throw new IsomorphismException("Unexpected ID", o);
	    else return;
	}
	try {
	    id.sameAs(s.getID());
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

    /**
     * Return a disconnected copy of this segment.  No interfaces are present
     * in the clone.
     * @return a disconnected copy of this segment.
     */
    public Segment clone() {
	return new Segment(this);
    }

    /**
     * get the ID
     * @return the ID
     */
    public ID getID() { return id; } 

    /**
     * Set the ID
     * @param i the new ID
     */
    public void setID(ID i) { id = (i != null ) ? new ID(i) : null; }

    /**
     * The name of the inner XML element for the usual representation of this
     * element.
     * @return the element name
     */
    public String getXMLElement() { return "segment"; }

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
	if ( id != null) 
	    id.writeXML(p, "id");
	super.writeXML(p, null);
	if ( ename != null ) p.println("</" + ename + ">");
	p.flush();
    }
}
