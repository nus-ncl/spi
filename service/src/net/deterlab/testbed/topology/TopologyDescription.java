package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * A description of a topology including the topology fragments used to expand
 * regions, and the name maps that indicate how to name elements and substrates
 * added from fragments to avoid name collisions.
 * @author DeterTeam
 * @version 1.0
 */
public class TopologyDescription extends Topology {
    /** The substrate name */
    private String version;
    /** The Fragments */
    private Map<String, Fragment> frags;
    /** The name maps */
    private Map<String, NameMap> nameMaps;



    /**
     * Basic initializer
     */
    public TopologyDescription() {
	super();
	version = "2.0";
	frags = new HashMap<String, Fragment>();
	nameMaps = new HashMap<String, NameMap>();
    }

    /**
     * Build a TopologyDescription from parts
     * @param v the version
     * @param subs the substrates
     * @param elems the elements
     * @param fragments the fragments
     * @param nmaps the name maps
     * @param attrs the attributes
     * @throws TopologyException if the topology is inconsistent
     */
    public TopologyDescription(String v, Collection<Substrate> subs,
	    Collection<Element> elems, Collection<Fragment> fragments,
	    Collection<NameMap> nmaps, Map<String, String> attrs)
	throws TopologyException {
	super(subs, elems, attrs);
	version = v;
	frags = new HashMap<String, Fragment>();
	nameMaps = new HashMap<String, NameMap>();
	if ( fragments != null ) 
	    for ( Fragment f : fragments)
		addFragment(f);
	if (nmaps != null ) 
	    for (NameMap n : nmaps) 
		addNameMap(n);
    }

    /**
     *  Copy constructor: makes a deep clone of this TopologyDescription - new
     *  substrates and elements, interconnected by interface copies that point
     *  inside the new topology as well as (deep) copies of the fragments and
     *  name maps.
     *  @param td the description to clone
     *  @throws TopologyException if the topology to clone is inconsistent
     */
    public TopologyDescription(TopologyDescription td)
	    throws TopologyException {
	super(td);
	version = td.getVersion();
	frags = new HashMap<String, Fragment>();
	nameMaps = new HashMap<String, NameMap>();

	for (Fragment f: td.getFragments())
	    frags.put(f.getName(), f);

	for (NameMap n: td.getNameMaps())
	    nameMaps.put(n.getPathName(), n);
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof TopologyDescription))
	    throw new IsomorphismException("Not a TopologyDescription");
	super.sameAs(o);
	TopologyDescription t = (TopologyDescription) o;
	try {
	    sameMaps(frags, t.frags);
	    sameMaps(nameMaps, t.nameMaps);
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

    /**
     * Get the version
     * @return the version
     */
    public String getVersion() { return version; }

    /**
     * Set the version
     * @param v the new version
     */
    public void setVersion(String v) { version = v; }


    /**
     * Return a list of fragments.  Changes to the individual fragments are
     * propagated, but the list is not - e.g., sorting it will not affect the
     * TopologyDescription.
     * @return the fragments
     */
    public List<Fragment> getFragments() { 
	return new ArrayList<Fragment>(frags.values()); }

    /**
     * Add a fragment
     * @param f the fragment to add
     * @throws TopologyException if there is a problem.
     */
    public void addFragment(Fragment f) throws TopologyException {
	if ( frags.containsKey(f.getName()))
	    throw new TopologyException("Duplicate fragment name " + 
		    f.getName());
	frags.put(f.getName(), f);
    }

    /**
     * Remove a fragment
     * @param f the fragment to remove
     * @return true if the fragment was present
     */
    public boolean removeFragment(Fragment f) {
	return frags.remove(f.getName()) != null;
    }

    /**
     * Get a fragment by name
     * @param name the name to retrieve
     * @return the fragment, or null 
     */
    public Fragment getFragment(String name) {
	return frags.get(name);
    }


    /**
     * Return a list of NameMaps.  Changes to the individual NameMaps are
     * propagated, but the list is not - e.g., sorting it will not affect the
     * TopologyDescription.
     * @return the NameMaps
     */
    public List<NameMap> getNameMaps() { 
	return new ArrayList<NameMap>(nameMaps.values()); }

    /**
     * Add a name map
     * @param nm the name map to add
     * @throws TopologyException if there is a problem.
     */
    public void addNameMap(NameMap nm) throws TopologyException {
	if ( nameMaps.containsKey(nm.getPathName()))
	    throw new TopologyException("Duplicate map pathname " + 
		    nm.getPathName());
	nameMaps.put(nm.getPathName(), nm);
    }

    /**
     * Remove a name map
     * @param nm the name map to remove
     * @return true if the name map was present
     */
    public boolean removeNameMap(NameMap nm) {
	return nameMaps.remove(nm.getPathName()) != null;
    }

    /**
     * Get a name map by name
     * @param name the name to retrieve
     * @return the name map, or null 
     */
    public NameMap getNameMap(String name) {
	return nameMaps.get(name);
    }

    /**
     *  Return a deep clone of this TopologyDescription - new substrates and
     *  elements, interconnected by interface copies that point inside the new
     *  topology as well as (deep) copies of the fragments and name maps.
     *  @return a copy of this TopologyDescription
     */
    public TopologyDescription clone() {
	try {
	    return new TopologyDescription(this);
	}
	catch (TopologyException e) {
	    // clone returns null on errors.
	    return null;
	}
    }

    /**
     * Expand the topology as far as possible, adding name maps as necessary.
     * Discard unnecessary fragments and name maps.  If expandThis is true, do
     * the expansion necessary in place and leave the expanded topology in this
     * TopologyDescription.
     * @param expandThis true to expand this topology/false to only update Maps
     * and Fragments
     * @throws TopologyException if the expansion is inconsistent.
     */
    public void validate(boolean expandThis) throws TopologyException {
	Set<Fragment> usedFrags = new HashSet<Fragment>();
	Set<NameMap> usedMaps = new HashSet<NameMap>();
	Topology t = expandThis ? this : clone();
	Queue<Element> elems = new ArrayDeque<Element>(t.getElements());

	while ( !elems.isEmpty() ) {
	    Element e = elems.remove();

	    if ( !(e instanceof Region) ) continue;
	    Region r = (Region) e;
	    Fragment f = getFragment(r.getFragmentName());
	    String pathname = r.getAttribute("path");

	    if ( pathname == null ) pathname = "/" + r.getName();
	    else pathname = pathname + r.getName();

	    NameMap inMap = getNameMap(pathname);
	    NameMap outMap = new NameMap(pathname, null, null);

	    if ( f == null ) 
		throw new TopologyException("Unknown fragment " +
			r.getFragmentName());

	    Collection<ConnectedObject> nobjs = r.expand(f, inMap, t, outMap);
	    for (ConnectedObject o: nobjs)
		if ( o instanceof Element) elems.add((Element)o);

	    usedFrags.add(f);
	    usedMaps.add(outMap);
	}
	// all done,  Install the new data.  Topology is unchanged.
	frags = new HashMap<String, Fragment>();
	nameMaps = new HashMap<String, NameMap>();
	for (Fragment f : usedFrags) 
	    frags.put(f.getName(), f);

	for (NameMap m: usedMaps)
	    nameMaps.put(m.getPathName(), m);

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
	p.println("<version>" + version + "</version>");
	super.writeXML(p, null);
	for (Fragment f : frags.values()) 
	    f.writeXML(p, "fragments");
	for (NameMap m: nameMaps.values())
	    m.writeXML(p, "namemaps");
	p.println("</" + ename + ">");
	p.flush();
    }

    /**
     * Create a TopologyDescription from an XML InputSource.
     * @param s the input stream
     * @param topName the name of the outermost element containing the topology
     * @param debug true for debug output to System.err
     * @return the TopologyDescription encoded in the XML
     * @throws TopologyException if the topology is inconsistent or incorrect
     * @throws IOException if the XML cannot be loaded or parsed
     */
    static public TopologyDescription xmlToTopology(InputSource s,
	    String topName, boolean debug)
	throws TopologyException, IOException {

	TopdlParser h = new TopdlParser(topName != null ? 
		topName: "experiment", debug);
	try {
	    XMLReader xr = 
		SAXParserFactory.newInstance().newSAXParser().getXMLReader();
	    xr.setContentHandler(h);
	    xr.parse(s);
	}
	catch (SAXException e) {
	    Throwable cause = e.getCause();
	    if ( cause != null && cause instanceof TopologyException)
		throw (TopologyException) cause;
	    else
		throw new IOException(e.getMessage());
	}
	catch (ParserConfigurationException e) {
	    throw new IOException(e.getMessage());
	}
	return h.getTopology();
    }

    /**
     * Create a TopologyDescription from an XML InputStream.
     * @param s the input stream
     * @param topName the name of the outermost element containing the topology
     * @param debug true for debug output to System.err
     * @return the TopologyDescription encoded in the XML
     * @throws TopologyException if the topology is inconsistent or incorrect
     * @throws IOException if the XML cannot be loaded or parsed
     */
    static public TopologyDescription xmlToTopology(InputStream s,
	    String topName, boolean debug)
	throws TopologyException, IOException {
	return xmlToTopology(new InputSource(s), topName, debug);
    }

    /**
     * Create a TopologyDescription from an XML Reader.
     * @param r the Reader
     * @param topName the name of the outermost element containing the topology
     * @param debug true for debug output to System.err
     * @return the TopologyDescription encoded in the XML
     * @throws TopologyException if the topology is inconsistent or incorrect
     * @throws IOException if the XML cannot be loaded or parsed
     */
    static public TopologyDescription xmlToTopology(Reader r,
	    String topName, boolean debug)
	throws TopologyException, IOException {
	return xmlToTopology(new InputSource(r), topName, debug);
    }
}
