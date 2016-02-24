
package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A service parameter from a substrate or element
 * @author DeterTeam
 * @version 1.0
 */
public class Service extends TopologyObject {

    /**
     * A parameter to a service, including name and type information.
     * @author DETER team
     * @version 1.0
     */
    static public class Param extends TopologyObject {
	/** Substrate rate (kb/s) */
	String name;
	/** The sort of rate: max or average */
	String type;

	/**
	 * Basic constructor
	 */
	public Param() {
	    super();
	    name = null;
	    type = null;
	}

	/**
	 * Constructor with both values
	 * @param n the rate in kb/s
	 * @param t the kind of capacity: max or average
	 */
	public Param(String n, String t) {
	    super();
	    name = n;
	    type = t;
	}

	/**
	 * Copy constructor
	 * @param p the Param to clone
	 */
	public Param(Param p) {
	    super();
	    name = p.name;
	    type = p.type;
	}
	/**
	 * See if this object and the given one match in isomporphic topologies.
	 * If not throw an exception that tracks the point of inconsistency.
	 * @param o the object to check
	 * @throws IsomorphismException if there is a mismatch
	 */
	public void sameAs(TopologyObject o) throws IsomorphismException {
	    if ( !(o instanceof Param))
		throw new IsomorphismException("Not a Param", o);
	    Param p = (Param) o;
	    if (!equalObjs(name, p.getName()))
		throw new IsomorphismException("Different names", o);
	    if (!equalObjs(type, p.getType()))
		throw new IsomorphismException("Different names", o);
	}

	/**
	 * Get the name
	 * @return the name
	 */
	public String getName() { return name; }

	/**
	 * Set the rate
	 * @param n the new rate
	 */
	public void setName(String n) { name = n; }

	/**
	 * Get the type
	 * @return the type
	 */
	public String getType() { return type; }

	/**
	 * Set the rate
	 * @param t the new rate
	 */
	public void setType(String t) { type = t; }

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
	    if (ename != null) p.println("<" + ename + ">");
	    p.println("<name>" + name + "</name>");
	    p.println("<type>" + type + "</type>");
	    if (ename != null) p.println("</" + ename + ">");
	}
    }

    /** The service name */
    private String name;
    /** Supported operations */
    private List<String> importers;
    /** Parameters to the service */
    private List<Param> params;
    /** A description of the service */
    private String description;
    /** Current status of the service */
    private String status;

    /**
     * Basic initializer
     */
    public Service() {
	super();
	name = null;
	importers = new ArrayList<String>();
	params = new ArrayList<Param>();
	description = null;
	status = null;
    }

    /**
     * Copy constructor
     * @param s the object to copy
     */
    public Service(Service s) {
	this(s.getName(), s.getImporters(), s.getParams(), 
		s.getDescription(), s.getStatus());
    }

    /**
     * Construct the service from parts
     * @param n the name
     * @param i the importers (copied)
     * @param p the parameters (copied)
     * @param d the description
     * @param s the status
     */
    public Service(String n, Collection<String> i, Collection<Param> p, 
	    String d, String s) {
	super();
	name = n;
	importers = (i != null) ? 
	    new ArrayList<String>(i) : new ArrayList<String>();
	params = new ArrayList<Param>();
	if ( p !=  null ) 
	    for (Param pa : p)
		params.add(new Param(pa));
	description = d;
	status = s;
    }
    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Service))
	    throw new IsomorphismException("Not a Service", o);
	Service s = (Service) o;
	if (!equalObjs(name, s.getName()))
	    throw new IsomorphismException("Different Names", o);
	if (!equalObjs(status, s.getStatus()))
	    throw new IsomorphismException("Different Statuses", o);
	if (!equalObjs(description, s.getDescription()))
	    throw new IsomorphismException("Different Descriptions", o);
	if ( !importers.equals(s.getImporters()))
	    throw new IsomorphismException("Different Importers", o);
	try {
	    sameLists(params, s.getParams());
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
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
     * Get the status
     * @return the status
     */
    public String getStatus() { return status; }

    /**
     * Set the status
     * @param s the new status
     */
    public void setStatus(String s) { status = s; }

    /**
     * Get the importers.  Modifying these will modify the importers in
     * the object.
     * @return the operations
     */
    public List<String> getImporters() { return importers; }

    /**
     * Get the parameters.  Modifying these will modify the parameters in
     * the object.
     * @return the operations
     */
    public List<Param> getParams() { return params; }
    /**
     * Get the description
     * @return the description
     */
    public String getDescription() { return description; }

    /**
     * Set the description
     * @param d the new description
     */
    public void setDescription(String d) { description = d; }

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
	p.println("<name>" + name + "</name>");
	for (String i: importers) 
	    p.println("<importer>" + i + "</importer>");
	for (Param pa: params)
	    pa.writeXML(p, "param");
	if ( description != null )
	    p.println("<description>" + description + "</description>");
	if ( status != null )
	    p.println("<status>" + status + "</status>");
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
