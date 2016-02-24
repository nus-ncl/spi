package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A Topology fragment that can be associated with a region.
 * @author DeterTeam
 * @version 1.0
 */
public class Fragment extends Topology {
    /** Name of this fragment */
    private String name;
    /** Map from region interface to local element name */
    private Map<String, String> ifMap;

    /**
     * Basic initializer
     */
    public Fragment() {
	super();
	name = null;
	ifMap = new HashMap<String, String>();
    }

    /**
     * Initialize an element's interfaces and attributes.  Connect the
     * interfaces to this element.
     * @param n the name
     * @param e the elements
     * @param s the substrates
     * @param ifm the interface map
     * @param a the atrributes (may be null or empty)
     * @throws TopologyException if the subs or elements are inconsistent
     */
    public Fragment(String n,  Collection<Substrate> s, Collection<Element> e,
	    Map<String, String> ifm,  Map<String, String> a) 
	throws TopologyException {
	super(s, e, a);
	name = n;
	ifMap = (ifm != null) ?  new HashMap<String, String>(ifm) : 
	    new HashMap<String, String>();
    }
    /**
     * Copy constructor - new substrates, elements, and ifmap,
     * interconnected by interface copies that point inside the new topology.
     * @param f the Fragment to copy
     * @throws TopologyException if the given Fragment is inconsistent
     */
    public Fragment(Fragment f) throws TopologyException {
	super(f);
	name = f.getName();
	Map<String, String> m = f.getInterfaceMap();
	ifMap = (m != null) ?
	    new HashMap<String, String>(m): new HashMap<String, String>();
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Fragment))
	    throw new IsomorphismException("Not a Fragment", o);
	super.sameAs(o);
	Fragment f = (Fragment) o;

	if ( !equalObjs(name, f.getName()))
	    throw new IsomorphismException("Different names", o);

	if ( !ifMap.equals(f.getInterfaceMap()))
	    throw new IsomorphismException("Different interface maps", o);
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
     * Return the interface map.  Changes to it are persistent.
     * @return the interface map.
     */
    public Map<String, String> getInterfaceMap() { return ifMap; }

    /**
     * Clone this fragment: new substrates, elements, and ifmap, interconnected
     * by interface copies that point inside the new topology.
     * @return a deep copy of this fragment
     */
    public Fragment clone() {
	try {
	    return new Fragment(this);
	}
	catch (TopologyException e) {
	    // Clone interface returns null on error.
	    return null;
	}
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

	p.println("<" + ename + ">");
	p.println("<name>" + name + "</name>");
	super.writeXML(p, "topology");
	for (Map.Entry<String, String> ent : ifMap.entrySet()) {
	    String iname = ent.getValue();
	    String oname = ent.getKey();

	    if (  iname == null || oname == null) {
		p.println("<!-- Bad interface for " + 
			((oname != null) ? oname : iname) + " -->");
		continue;
	    }
	    p.println("<ifmap>");
	    p.println("<inner>" + iname + "</inner>");
	    p.println("<outer>" + oname + "</outer>");
	    p.println("</ifmap>");
	}
	p.println("</" + ename + ">");
	p.flush();
    }

     
}
