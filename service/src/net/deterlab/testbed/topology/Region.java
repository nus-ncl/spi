package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A region is an Element standing in for the Topology represented by a
 * fragment.  Regions and fragments can recurse.  The fragment's elements and
 * substrates are renamed by a NameMap.
 * @author DeterTeam
 * @version 1.0
 */
public class Region extends Element {
    /** level of expansion 0 means it will not expand any more and will
     * disappear.*/
    private int level;
    /** The name of the fragment into which the region expands.  */
    private String fragName;
    /** Used to construct unique names */
    private static String defaultPrefix = "r-%d";

    /**
     * Basic initializer
     */
    public Region() {
	super();
	level = 0;
	fragName = null;
	setPrefix(defaultPrefix);
    }

    /**
     * Initialize a Region from parts
     * @param n the name
     * @param le the level
     * @param fn the fragment name
     * @param ifs the interfaces
     * @param a the atrributes (may be null or empty)
     */
    public Region(String n, int le, String fn, Collection<Interface> ifs, 
	    Map<String, String> a) {
	super(n, ifs, a);
	setPrefix(defaultPrefix);
	level = le;
	fragName = fn;
    }

    /**
     * Copy the given Region.  The result is disconnected.
     * @param r the region to copy
     */
    public Region(Region r) {
	this(r.getName(), r.getLevel(), r.getFragmentName(), null, 
		r.getAttributes());
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Region))
	    throw new IsomorphismException("Not a Region", o);
	super.sameAs(o);
	Region r = (Region) o;
	if (level != r.getLevel())
	    throw new IsomorphismException("Different levels", o);
	if (!equalObjs(fragName, r.getFragmentName()))
	    throw new IsomorphismException("Different fragment names", o);
    }

    /**
     * Return a disconnected clone of this Region.  Interfaces are not copied.
     * @return a disconnected clone of this Region. 
     */
    public Region clone() {
	return new Region(this);
    }
    /**
     * Get the level
     * @return the level
     */
    public int getLevel() { return level; }

    /**
     * Set the level
     * @param le the new level
     */
    public void setLevel(int le) { level = le; }

    /**
     * Get the fragment name
     * @return the fragment name
     */
    public String getFragmentName() { return fragName; }

    /**
     * Set the fragment name
     * @param fn the new fragment name
     */
    public void setFragmentName(String fn) { fragName = fn; }

    /**
     * The name of the inner XML element for the usual representation of this
     * element.
     * @return the element name
     */
    public String getXMLElement() { return "region"; }

    /**
     * Expand this region into the given Fragment
     * @param f the fragment
     * @param nm a name map to translate the fragment names, if present
     * @param w the world to add the elements to.
     * @param outMap the output NameMap containing the mappings made, if
     * null, no map is generated
     * @return a collection of the elements and substrates added.
     * @throws TopologyException if there is aproblem creating the topology
     */
    public Collection<ConnectedObject> expand(Fragment f, NameMap nm,
	    Topology w, NameMap outMap) throws TopologyException {
	if ( fragName == null ) 
	    throw new TopologyException("No fragment name for region");
	if ( !fragName.equals(f.getName()))
	    throw new TopologyException("Fragment name for region " + 
		    "does not match name of fragment");
	Topology t = f.clone();
	Map<String, String> interfaceMap = f.getInterfaceMap();
	Map<String, String> names = (nm != null) ?
	    nm.getMap() : new HashMap<String, String>();
	Map<String, String> out = (outMap != null) ?  outMap.getMap() : null;
	List<ConnectedObject> rv = new ArrayList<ConnectedObject>();
	String path = getAttribute("path");
	if (path == null ) path = "/";
	String newPath = path + getName() + "/";


	for ( Interface i : new ArrayList<Interface>(getInterfaces())) {
	    String ifname = i.getName();

	    i.disconnect();
	    if (interfaceMap.containsKey(ifname)) {
		Element e = t.getElement(interfaceMap.get(ifname));
		if ( e == null ) 
		    throw new TopologyException("No element named " + 
			    interfaceMap.get(ifname) + " in fragment!?");
		String ipath = i.getAttribute("interface_path");
		if (ipath == null) ipath = ifname;
		else ipath = ipath + "/" + ifname;

		// Force a unique name.
		i.setName(null);
		i.setAttribute("interface_path", ipath);
		i.connect(e, null);
	    }
	}
	// Set region levels in the new expansion, remove regions if we're at
	// level 0.
	int myLevel = getLevel();
	for (Element e: new ArrayList<Element>(t.getElements())) {

	    if ( !(e instanceof Region)) continue;
	    Region r = (Region) e;

	    if ( myLevel > 1)  r.setLevel(myLevel-1); 
	    else {
		for ( Interface i : new ArrayList<Interface>(r.getInterfaces()))
		    i.disconnectAll();
		t.removeElement(e);
		continue;
	    }
	}
	// All connected - into the world.  If the substrate name is in the
	// name map, use that name and throw an exeception if it is
	// inconsistent.  If no names are given (or this name is not in the
	// map) let the topology pick a unique name.  In either case, if an
	// output name map is requested, fill it in.  In addition, add a
	// pathname attribute.
	for (Substrate s: t.getSubstrates()) {
	    // Don't add any substrates to the world that were only connected
	    // to a deleted Region.
	    if (s.getInterfaces().size() < 2) continue;
	    String oldName = s.getName();

	    s.setAttribute("path", newPath);
	    if (names.containsKey(s.getName())) {
		s.setName(names.get(s.getName()));
		w.addSubstrate(s, false);
	    }
	    else {
		w.addSubstrate(s, true);
	    }
	    rv.add(s);
	    if ( out != null ) out.put(oldName, s.getName());

	}

	// Names and paths are treated the same way as the substrate loop.
	for (Element e: t.getElements())  {
	    String oldName = e.getName();

	    e.setAttribute("path", newPath);
	    if (names.containsKey(e.getName())) {
		e.setName(names.get(e.getName()));
		w.addElement(e, false);
	    }else {
		w.addElement(e, true);
	    }
	    rv.add(e);
	    if ( out != null ) out.put(oldName, e.getName());
	}
	// Finally, the region can leave the world topology - it's been
	// replaced
	w.removeElement(this);
	return rv;
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
	super.writeXML(p, null);
	p.println("<level>" + level + "</level>");
	p.println("<fragname>" + fragName + "</fragname>");
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
