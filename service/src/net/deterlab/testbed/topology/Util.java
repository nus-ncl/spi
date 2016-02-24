package net.deterlab.testbed.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of utilities for manipulating topologies.
 * @author DeterTeam
 * @version 1.0
 */
public class Util {
    /**
     * Interface for choosing Elements to go into a new region
     */
    public interface RegionPredicate {
	/**
	 * Returns true if e is inside the new region
	 * @param e the element to test
	 * @return true if e is inside the new region
	 */
	public boolean insideRegion(Element e);
    }

    /**
     * A pair: a new region and the fragment it is connected to.
     * @author DeterTeam
     * @version 1.0
     */
    static public class NewRegion {
	/** The region */
	private Region region;
	/** The fragment */
	private Fragment fragment;

	/**
	 * Make a NewProject object
	 * @param r the region
	 * @param f the Fragment
	 */
	public NewRegion(Region r, Fragment f) {
	    region = r;
	    fragment = f;
	}

	/**
	 * Return the region
	 * @return the region
	 */
	public Region getRegion() { return region; }

	/**
	 * Return the fragment
	 * @return the fragment
	 */
	public Fragment getFragment() { return fragment; }
    }

    /**
     * Return true if the substrate connects only elements inside the region
     * defined by in.
     * @param s the substrate to test
     * @param rp the region defining predicate
     * @return true if the substrate connects only elements inside the region
     * defined by in.
     */
    static private boolean substrateInside(Substrate s, RegionPredicate rp) {
	for (Interface i: s.getInterfaces()) {
	    Element e = i.getElement();
	    if ( e != null && !rp.insideRegion(e)) return false;
	}
	return true;
    }

    /**
     * Return the index of the region name in the path, taking the path as a
     * sequence of components (0-based).  If the regin name is not there, or
     * the path is not there, return -1;
     * @param rname the name of the region to look for
     * @param es the object to check
     * @return true if the object is in the region (according to the path)
     */
    static protected int regionIndex(String rname, AttributedObject es) {
	if (es == null) return -1;

	String path = es.getAttribute("path");

	if (path == null || rname == null) return -1;

	int i = 0;
	for ( String c : path.split("/")) 
	    if ( rname.equals(c) ) return i;
	    else i++;

	return -1;
    }

    /**
     * Return true if the given TopologyElement includes rname in its path
     * attribute.  Objects without a path attribute are outside all regions.
     * @param rname the name of the region to look for
     * @param es the object to check
     * @return true if the object is in the region (according to the path)
     */
    static protected boolean inRegion(String rname, AttributedObject es) {
	return regionIndex(rname, es) != -1;
    }

    /**
     * Recombine the path elements into a single string.  Include elements 0
     * through lim.
     * @param path the path components to put together
     * @param lim the first item in path to ignore
     * @return the combined string
     */
    static protected String makePath(String[] path, int lim) {
	StringBuilder sb = new StringBuilder();
	if (lim > path.length) lim = path.length;
	for ( int i = 0; i < lim; i++) {
	    sb.append("/");
	    sb.append(path[i]);
	}
	return sb.toString();
    }

    /**
     * Remove the interfaces attached from the given region name and return
     * them.  Readjust the interface path and rename the interfaces
     * appropriately as well.
     * @param rname the name of teh region to roll back to
     * @param e the element to edit
     * @return a disconnected list of interfaces to be attached to the region.
     */
    static protected List<Interface> getRegionInterfaces(String rname,
	    Element e) {
	List<Interface> rv = new ArrayList<Interface>();
	int idx = regionIndex(rname, e);

	if (idx == -1) return rv;
	String pathAttr = e.getAttribute("path");
	String[] path = (pathAttr != null) ? pathAttr.split("/") : null;

	if ( path == null ) return rv;
	int pi = path.length - idx;
	for (Interface i: new ArrayList<Interface>(e.getInterfaces())) {
	    if (inRegion(rname, i.getSubstrate())) continue;
	    String ipathAttr = i.getAttribute("interface_path");
	    String[] ipath = (ipathAttr != null) ? ipathAttr.split("/") : null;

	    if (ipath == null ) continue;

	    int ii = ipath.length- pi;
	    if (ii < 0 ) continue;

	    // disconnect i from e, rename it for insertion to the region
	    // and store it.
	    i.disconnect();
	    i.setName(ipath[ii]);
	    i.setAttribute("interface_path", makePath(ipath, ii));
	    rv.add(i);
	}
	return rv;
    }

    /**
     * Remove the elements and substrates that were expanded from this region
     * and children and replace them with the region.
     * @param t the Topology to adjust
     * @param r the region to collapse
     * @throws TopologyException if the collapsed topology is inconsistent
     */
    static public void collapseExistingRegion(Topology t, Region r) 
	    throws TopologyException {
	List<Substrate> inSubs = new ArrayList<Substrate>();
	List<Element> inElems = new ArrayList<Element>();
	List<Interface> inInts = new ArrayList<Interface>();
	String rname = r.getName();

	for (Element e: t.getElements())
	    if ( inRegion(rname, e)) inElems.add(e);
	for (Substrate s : t.getSubstrates())
	    if ( inRegion(rname, s)) inSubs.add(s);


	// All the stuff in inSubs and inElems is going to be removed, but
	// first we gather up the interfaces that need to be connected to this
	// region.
	for ( Element e : inElems ) {
	    inInts.addAll(getRegionInterfaces(rname, e));
	    t.removeElement(e);
	}
	for (Substrate s: inSubs) 
	    t.removeSubstrate(s);

	for (Interface i : inInts) 
	    i.connect(r, null);
	t.addElement(r);
    }

    /**
     * Gather a Topology that would be clipped out of the topology by
     * collapseNewRegion on the same predicate.  This allows callers to confirm
     * that the new region will be reasonable, etc.  The given topology is not
     * altered by this call.  Call this to confirm features of the region being
     * created.
     * @param t the Topology to manipulate
     * @param rp the RegionComparator that defines the region
     * @return a topology containing the removed elements and substrates
     * @throws TopologyException if the collapsed topology is inconsistent
     */
    static public Topology gatherRegionTopology(Topology t, RegionPredicate rp)
	    throws TopologyException {
	Set<Substrate> inSubs = new HashSet<Substrate>();
	List<Element> inElems = new ArrayList<Element>();
	List<Interface> inInts = new ArrayList<Interface>();
	Map<String, Substrate> nameToSub = new HashMap<String, Substrate>();

	// Create copies of all the substrates in the region and make an index
	// to find them by name
	for (Substrate s : t.getSubstrates())
	    if ( substrateInside(s, rp)) {
		Substrate ss = s.clone();
		nameToSub.put(s.getName(), s);
		inSubs.add(ss);
	    }
	// Create copies of the elements in the proposed region and connect
	// them to the duplicated substrates.
	for (Element e: t.getElements())
	    if ( rp.insideRegion(e)) {
		Map<String, String> ifToSub = new HashMap<String, String>();
		Element ne = e.clone();

		for (Interface i : e.getInterfaces()) {
		    String sn = i.getSubstrate().getName();
		    if ( nameToSub.containsKey(sn))
			ifToSub.put(i.getName(), i.getSubstrate().getName());
		}

		// the clone of the element has created interfaces with the
		// same names connected only to the element.  This loop
		// connects the interfaces of the clone to the clones of the
		// substrates with the same name.
		for (Interface i :
			new ArrayList<Interface>(ne.getInterfaces())) {
		    String iname = i.getName();

		    if (ifToSub.containsKey(iname))
			i.connect(null, nameToSub.get(ifToSub.get(iname)));
		    else
			i.disconnect();
		}
		inElems.add(ne);
	    }
	return new Topology(inSubs, inElems, null);
    }

    /**
     * Create and collapse a new region.  Elements for which inside returns
     * true are part of the region, as are any susbtrates that only have
     * elements inside the region connected to them.  This will assign level to
     * the new region that can invalidate regions containing recursive regions.
     * Use gather fragment to be sure that the region being created has
     * appropriate properties.
     * @param t the Topology to manipulate
     * @param rname the name of the new region
     * @param level the level of the new region
     * @param fname the name of the new fragment to create
     * @param rp the RegionComparator that defines the region
     * @return a fragment containing the removed elements and substrates
     * @throws TopologyException if the collapsed topology is inconsistent
     */
    static public NewRegion collapseNewRegion(Topology t, String rname,
	    int level, String fname, RegionPredicate rp)
	    throws TopologyException {
	Set<Substrate> inSubs = new HashSet<Substrate>();
	List<Element> inElems = new ArrayList<Element>();
	List<Interface> inInts = new ArrayList<Interface>();
	Map<String, String> fmap = new HashMap<String, String>();
	Region r = new Region(rname, level, fname, null, null);

	for (Element e: t.getElements())
	    if ( rp.insideRegion(e)) inElems.add(e);
	for (Substrate s : t.getSubstrates())
	    if ( substrateInside(s, rp)) inSubs.add(s);

	t.addElement(r);

	// Find the interfaces that connect outside the new region, disconnect
	// them from their current elements and connect them to the new region
	// element.  When this loop exits inElems will only have interfaces
	// that connect to inSubs.
	for ( Element e : inElems ) {
	    // Copy the interface list as it may change
	    for (Interface i: new ArrayList<Interface>(e.getInterfaces())) {
		if (inSubs.contains(i.getSubstrate())) continue;
		i.setName(null);
		i.disconnect();
		// Sets the interface name
		i.connect(r, null);
		fmap.put(i.getName(), e.getName());
	    }
	}

	// Pick up the elements and substrates in the region, remove them from
	// the topology and install them in a fragment.
	for (Substrate s: inSubs)
	    t.removeSubstrate(s);
	for (Element e : inElems)
	    t.removeElement(e);

	return new NewRegion(r,
		new Fragment(fname, inSubs, inElems, fmap, null));

    }
}
