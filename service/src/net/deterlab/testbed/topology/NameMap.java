package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * A map that tells how to rename the members of a fragment when it replaces a
 * given Region.
 * @author DeterTeam
 * @version 1.0
 */
public class NameMap extends AttributedObject {
    /** Pathname to which this map applies*/
    private String pathname;
    /** Map from fragment name to global name */
    private Map<String, String> names;

    /**
     * Basic initializer
     */
    public NameMap() {
	super();
	pathname = null;
	names = new HashMap<String, String>();
    }

    /**
     * Initialize an element's interfaces and attributes.  Connect the
     * interfaces to this element.
     * @param pn the pathname
     * @param nm the interface map
     * @param a the atrributes (may be null or empty)
     */
    public NameMap(String pn,  Map<String, String> nm, Map<String, String> a) {
	super(a);
	pathname = pn;
	names = (nm != null) ?  new HashMap<String, String>(nm) : 
	    new HashMap<String, String>();
    }

    /**
     * Copy constructor
     * @param nm the map to copy
     */
    public NameMap(NameMap nm) {
	super(nm);
	pathname = nm.getPathName();
	Map<String, String> m = nm.getMap();
	names = (m != null) ?
	    new HashMap<String, String>(m) : new HashMap<String, String>();
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof NameMap))
	    throw new IsomorphismException("Not a NameMap", o);
	super.sameAs(o);
	NameMap m = (NameMap) o;

	if (!equalObjs(pathname, m.getPathName()))
	    throw new IsomorphismException("Different Pathnames", o);

	if ( !names.equals(m.getMap()))
	    throw new IsomorphismException("Different Maps", o);
    }

    /**
     * Get the pathname
     * @return the pathname
     */
    public String getPathName() { return pathname; }

    /**
     * Set the pathname
     * @param pn the new pathname
     */
    public void setPathName(String pn) { pathname = pn; }

    /**
     * Return the interface map.  Changes to it are persistent.
     * @return the interface map.
     */
    public Map<String, String> getMap() { return names; }

    /**
     * Return a copy of this NameMap
     * @return a copy of this NameMap
     */
    public NameMap clone() {
	return new NameMap(this);
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
	p.println("<pathname>" + pathname + "</pathname>");
	for (Map.Entry<String, String> ent : names.entrySet()) {
	    String iname = ent.getValue();
	    String oname = ent.getKey();

	    if (  iname == null || oname == null) {
		p.println("<!-- Bad interface for " + 
			((oname != null) ? oname : iname) + " -->");
		continue;
	    }
	    p.println("<namemap>");
	    p.println("<inner>" + iname + "</inner>");
	    p.println("<outer>" + oname + "</outer>");
	    p.println("</namemap>");
	}
	super.writeXML(p, "attribute");
	p.println("</" + ename + ">");
	p.flush();
    }

     
}
