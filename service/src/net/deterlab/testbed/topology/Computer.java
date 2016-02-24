package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A general purpose computer as part of a topology.
 * @author DeterTeam
 * @version 1.0
 */
public class Computer extends Element implements Cloneable {
    /** CPU info, if any */
    private List<CPU> cpu;
    /** OS info, if any */
    private List<OperatingSystem> os;
    /** software info, if any */
    private List<Software> software;
    /** Storage info, if any */
    private List<Storage> storage;
    /** Operational data */
    private OperationalObject oo;

    /** The prefix for default names of Computers */
    private static String defaultPrefix = "n-%d";

    /**
     * Basic initializer
     */
    public Computer() {
	super();
	cpu = new ArrayList<CPU>();
	os = new ArrayList<OperatingSystem>();
	software = new ArrayList<Software>();
	storage = new ArrayList<Storage>();
	oo = new OperationalObject();
	setPrefix(defaultPrefix);
    }

    /**
     * Copy constructor - does not copy interfaces
     * @param c the computer to copy
     */
    public Computer(Computer c) {
	this(c.getName(), null, c.getCPU(), c.getOS(), c.getSoftware(),
		c.getStorage(), c.getOperationalData().getLocalnames(),
		c.getOperationalData().getStatus(),
		c.getOperationalData().getServices(),
		c.getOperationalData().getOperations(),
		c.getAttributes());
    }

    /**
     * Initialize a computer from parts
     * @param n the name
     * @param ifs the interfaces
     * @param c the CPU info if any
     * @param osys the operating system info, if any
     * @param sw software info if any
     * @param st storage info if any
     * @param ln the local names (nay be null or empty)
     * @param s the status (may be null)
     * @param ser the services (may be null or empty)
     * @param o the operations (may be null or empty)
     * @param a the atrributes (may be null or empty)
     */
    public Computer(String n, Collection<Interface> ifs,
	    Collection<CPU> c, Collection<OperatingSystem> osys,
	    Collection<Software> sw, Collection<Storage> st, 
	    Collection<String> ln,
	    String s, Collection<Service> ser, Collection<String> o, 
	    Map<String, String> a) {
	super(n, ifs, a);
	setPrefix(defaultPrefix);
	cpu = new ArrayList<CPU>();
	if ( c != null )
	    for (CPU cc: c)
		cpu.add(new CPU(cc));
	os = new ArrayList<OperatingSystem>();
	if ( osys != null) 
	    for ( OperatingSystem x: osys) 
		os.add(new OperatingSystem(x));

	software = new ArrayList<Software>();
	if (sw != null) 
	    for (Software so: sw) 
		software.add(new Software(so));

	storage = new ArrayList<Storage>();
	if (st != null) 
	    for (Storage so: st) 
		storage.add(new Storage(so));
	oo = new OperationalObject(ln, s, ser, o);
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Computer))
	    throw new IsomorphismException("Not a Computer", o);
	super.sameAs(o);
	Computer c = (Computer) o;
	try {
	    sameLists(cpu, c.getCPU());
	    sameLists(os, c.getOS());
	    sameLists(software, c.getSoftware());
	    sameLists(storage, c.getStorage());
	    oo.sameAs(c.getOperationalData());
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

    /**
     * Return a disconnected clone of this Computer.  No interfaces are copied.
     * @return a disconnected clone of this Computer.
     */
    public Element clone() {
	return new Computer(this);
    }

    /**
     * Get the cpu info.  Modifying these will modify the cpu info in
     * the object.
     * @return the operations
     */
    public List<CPU> getCPU() { return cpu; }

    /**
     * Get the operating system info.  Modifying these will modify the
     * operating system info in the object.
     * @return the operations
     */
    public List<OperatingSystem> getOS() { return os; }

    /**
     * Get the software info.  Modifying these will modify the software info in
     * the object.
     * @return the operations
     */
    public List<Software> getSoftware() { return software; }

    /**
     * Get the storage info.  Modifying these will modify the storage info in
     * the object.
     * @return the operations
     */
    public List<Storage> getStorage() { return storage; }

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
    public String getXMLElement() { return "computer"; }

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
	for ( CPU c: cpu)
	    c.writeXML(p, "cpu");
	for ( OperatingSystem o: os)
	    o.writeXML(p, "os");
	for ( Software s: software)
	    s.writeXML(p, "software");
	for ( Storage s: storage)
	    s.writeXML(p, "storage");
	oo.writeXML(p, null);
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
