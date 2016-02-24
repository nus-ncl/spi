package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An object nested inside objects that support the services/status/operations
 * interfaces.
 * @author DeterTeam
 * @version 1.0
 */
public class OperationalObject extends TopologyObject {
    /** A local name, if assigned */
    private List<String> localnames;
    /** Current status, if assigned */
    private String status;
    /** Services provided by this substrate */
    private List<Service> services;
    /** Supported operations */
    private List<String> ops;

    /**
     * Basic initializer
     */
    public OperationalObject() {
	super();
	localnames = new ArrayList<String>();
	status = null;
	services = new ArrayList<Service>();
	ops = new ArrayList<String>();
    }

    /**
     * Copy constructor
     * @param o the object to copy
     */
    public OperationalObject(OperationalObject o) {
	this(o.getLocalnames(), o.getStatus(), o.getServices(), 
		o.getOperations());
    }

    /**
     * Initialize from parts
     * @param ln the local names (nay be null or empty)
     * @param s the status (may be null)
     * @param ser the services (may be null or empty)
     * @param o the operations (may be null or empty)
     */
    public OperationalObject(Collection<String> ln, String s, 
	    Collection<Service> ser, Collection<String> o) {
	localnames = (ln != null) ? 
	    new ArrayList<String>(ln): new ArrayList<String>();
	status = s;
	ops = ( o != null ) ?
	    new ArrayList<String>(o) : new ArrayList<String>();
	services = new ArrayList<Service>();
	if (ser != null ) 
	    for (Service ss: ser)
		services.add(new Service(ss));
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof OperationalObject))
	    throw new IsomorphismException("Not an OperationalObject", o);
	OperationalObject oo = (OperationalObject) o;
	if ( !localnames.equals(oo.getLocalnames()))
	    throw new IsomorphismException("Different localnames", o);
	if ( !ops.equals(oo.getOperations()))
	    throw new IsomorphismException("Different operations", o);
	if ( !equalObjs(status, oo.getStatus()))
	    throw new IsomorphismException("Different statuses", o);
	try {
	    sameLists(services, oo.getServices());
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

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
     * Get the valid localnames.  Modifying these will modify the localnames in
     * the object.
     * @return the operations
     */
    public List<String> getLocalnames() { return localnames; }

    /**
     * Get the valid operations.  Modifying these will modify the operations in
     * the object.
     * @return the operations
     */
    public List<String> getOperations() { return ops; }

    /**
     * Get the services.  Modifying these will modify the services
     * the object.
     * @return the services
     */
    public List<Service> getServices() { return services; }

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

	if ( ename != null ) p.println("<" + ename + ">");
	for (String ln: localnames)
	    p.println("<localname>" + ln + "</localname>");
	if ( status != null )
	    p.println("<status>" + status + "</status>");
	for (String op: ops) 
	    p.println("<operation>" + op + "</operation>");
	for (Service s: services)
	    s.writeXML(p, "service");
	if ( ename != null ) p.println("</" + ename + ">");
	p.flush();
    }
}
