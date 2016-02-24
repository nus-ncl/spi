package net.deterlab.testbed.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parse a topdl v2.0 file into the classes used to manipulate it.  It
 * contains abunch of parsing state and is called from the standard SAX
 * parser at 3 places: when an XML element starts (&lt;element&gt;) when
 * one ends (&lt;element/&gt;) and whenever characters inside an element
 * are encountered.  It mostly initializes fields as elements are exited,
 * collecting other parsed fields into objects that are finally attached to
 * the topology itself.
 */
public class TopdlParser extends DefaultHandler {
    /** The name of the outermost element, "experiment" by default */
    protected String topName;

    // Topology parameters
    /** The current topology being built */
    protected TopologyDescription topo;

    /** A map of substrates */
    Map<String, Substrate> nameToSubstrate;

    // Many elements have a name
    /** the most recent name element parsed */
    protected String name;
    // Many statuses as well
    protected String status;

    // CPU parameters
    /** current cpu type */
    protected String type;
    /** current number of cpus */
    protected int ncpus;

    // Operatingsystem parameters
    /** Current OS version */
    protected String version;
    /** Current OS distro */
    protected String distribution;
    /** Current OS distro version */
    protected String distributionversion;
    /** Local names of this node */
    protected List<String> localnames;

    /** True if in a Computer testbed or substrate that has local names */
    protected boolean collectLocalnames;


    // Software parameters
    /** Current software installation point */
    protected String install;
    /** Current software location */
    protected String location;

    // Service Parameters
    protected List<String> importers;
    protected List<Service.Param> serviceParams;
    protected String description;

    //ServiceParam parameters
    protected String serviceParamName;
    protected String serviceParamType;

    // Storage parameters
    /** Current storage amount */
    protected double amount;
    /** Current storage persistence */
    protected String persistence;

    // Interface Parameters
    /** Substrate the current interface is attached to */
    protected Substrate ifsub;
    /** Current interface capacity */
    protected Capacity cap;
    /** Current interface latency */
    protected Latency lat;

    // Capacity parameters
    /** Current capacity rate (bandwidth) */
    protected double rate;
    /** Current capacity kind (max, peak)*/
    protected String kind;

    // Latency parameters (shares kind)
    /** Current latency time */
    protected double time;

    // Testbed parameters (shares interfaces, type)
    /** Current URI */
    protected String uri;

    // Segment parameters (shares interfaces, type, uri)
    /** Current segment ID */
    protected Segment.ID id;
    // IDType parameters
    /** Current ID uuid (if any) */
    protected byte[] uuid;
    /** Current ID fedid (if any) */
    protected byte[] fedid;
    /** Current ID uri (if any) */
    protected String id_uri;
    /** Current ID localname (if any) */
    protected String localname;
    /** Current ID kerberosUsername (if any) */
    protected String kerberosUsername;
    /** True when we are parsing an ID (so URIs are stored in id_uri) */
    protected boolean inID;

    // Region parameters (that aren't covered by others)
    /** This region's level */
    protected int level;
    /** This region's fragment */
    protected String fragName;

    // ifmap parameters (inside Fragments)
    /** The name of the interface inside the fragment */
    protected String inner;
    /** The name of the interface outside the fragment */
    protected String outer;

    // Fragment parameters
    /** The current interface map */
    protected Map<String, String> ifmap;

    // NameMap parameters
    /** pathname for the current name map */
    protected String pathname;
    /** Name mapping for the current name map */
    protected Map<String, String> nameMapping;

    // Attribute parameters
    /** Current attribute name */
    protected String aname;
    /** Current attribute value */
    protected String aval;

    /** Elements seen so far */
    protected List<Element> elements;
    /** Substrates seen so far */
    protected List<Substrate> subs;
    /** Fragments seen so far */
    protected List<Fragment> frags;
    /** Attributes seen so far */
    protected Map<String, String> attrs;
    /** CPUs seen so far */
    protected List<CPU> cpus;
    /** Operating Systems seen so far */
    protected List<OperatingSystem> oses;
    /** Software seen so far */
    protected List<Software> software;
    /** Storage seen so far */
    protected List<Storage> storage;
    /** Interfaces seen so far */
    protected List<Interface> interfaces;
    /** Services seen so far */
    protected List<Service> services;
    /** Operations seen so far */
    protected List<String> operations;
    /** Name maps seen so far */
    protected List<NameMap> nameMaps;

    /**
     * Attributes appear in many elements, some of which can appear inside
     * the definition of others.  We keep a stack of live attributes so
     * that a sub-element does not overwrite a super-element's attributes.
     * attrElements is a set of element names that should start a new set
     * of recorded attributes, and attrStack keeps the contexts stacked. 
     */
    protected Set<String> attrElements;
    /**
     * The stack of attribute contexts.
     */
    protected Stack<Map<String, String> > attrStack;
    /**
     * Analogous to attrElements for names.
     */
    protected Set<String> nameElements;
    /**
     * Analogous to attrStack for names.
     */
    protected Stack<String> nameStack;
    /**
     * Analogous to attrStack for status
     */
    protected Stack<String> statusStack;
    /**
     * Analogous to attrStack for elements (!) needed for fragments.
     */
    protected Stack<List<Element>> elementsStack;
    /**
     * Analogous to attrStack for substrates (!) needed for fragments.
     */
    protected Stack<List<Substrate>> substratesStack;
    /**
     * Analogous to attrStack for substrates/name map (!) needed for fragments.
     */
    protected Stack< Map<String, Substrate>> nameToSubstrateStack;

    /**

    /**
     * Analogous to attrElements for names.
     */
    protected Set<String> statusElements;
    /**
     * These elements collect localnames
     */
    protected Set<String> localnameElements;
    /**
     * These names have substrates and elements inside them
     */
    protected Set<String> topoElements;
    /**
     * The current buffer of characters collected.
     */
    protected char[] c;

    /**
     * Print parsing info if true
     */
    protected boolean debug;

    /**
     * Initialize the internal data structures.  Top is the outer element
     * name.  If d is true, debugging info is printed.
     * @param top a String containing the outer element name
     * @param d a boolean, if true pring debugging info
     */
    public TopdlParser(String top, boolean d) {
	debug = d;
	topName = top;
	topo = null;
	nameToSubstrate = new HashMap<String, Substrate>();

	localnames = new ArrayList<String>();
	collectLocalnames = false;

	type = null;
	name = null;
	ncpus = 1;

	version = distribution = distributionversion = null;
	description = null;

	install = location = null;

	amount = 0.0;
	persistence = null;

	ifsub = null;
	cap = null;
	lat = null;

	rate = 0.0;
	kind = null;

	time = 0.0;

	uri = null;

	id = null;

	uuid = fedid = null;
	id_uri = localname = kerberosUsername = null;
	inID = false;

	level = 1;
	fragName = null;

	inner = null;
	outer = null;
	ifmap = new HashMap<String, String>();

	pathname = null;
	nameMapping = new HashMap<String, String>();

	aname = aval = null;
	importers = new ArrayList<String>();;
	serviceParams = new ArrayList<Service.Param>();

	elements = new ArrayList<Element>();
	frags = new ArrayList<Fragment>();
	cpus = new ArrayList<CPU>();
	subs = new ArrayList<Substrate>();
	oses = new ArrayList<OperatingSystem>();
	software = new ArrayList<Software>();
	storage = new ArrayList<Storage>();
	interfaces = new ArrayList<Interface>();
	services = new ArrayList<Service>();
	serviceParams = new ArrayList<Service.Param>();
	attrs = new HashMap<String, String>();
	operations = new ArrayList<String>();
	nameMaps = new ArrayList<NameMap>();

	attrElements = new TreeSet<String>();
	for (String e : new String[] {
	    topName, "computer", "cpu", "os", "software", "storage",
	    "interface", "segment", "testbed", "other", "substrates",
	    "region", "fragments", "namemaps"
	}) attrElements.add(e);

	attrStack = new Stack<Map<String, String>>();

	nameElements = new TreeSet<String>();
	for (String e : new String[] {
	    "computer", "os", "interface", "substrates", "service",
	    "param", "other",  "segment", "testbed", "region", "fragments"
	}) nameElements.add(e);
	nameStack = new Stack<String>();

	statusElements = new TreeSet<String>();
	for (String e : new String[] {
	    "computer", "testbed", "substrates", "service", "segment",
	}) statusElements.add(e);
	statusStack = new Stack<String>();

	localnameElements = new TreeSet<String>();
	for (String e : new String[] {
	    "computer", "testbed", "substrates", "segment",
	}) localnameElements.add(e);

	elementsStack = new Stack<List<Element>>();
	substratesStack = new Stack<List<Substrate>>();
	nameToSubstrateStack = new Stack<Map<String, Substrate>>();
	topoElements = new TreeSet<String>();
	for (String e: new String[] {top, "fragments" })
	    topoElements.add(e);

	c = new char[0];
    }

    /**
     * Called when an element begins.  qn is the element and a has
     * attributes assigned in the element.  This starts new name or
     * attribute contexts, if necessary as well as noting the start of an
     * id (which is a lightweight uri context).  The parameter definitions
     * below are from org.xml.sax.helpers.DefaultHandler
     * @param u - The Namespace URI, or the empty string if the element has
     * no Namespace URI or if Namespace processing is not being performed.
     * @param l - The local name (without prefix), or the empty string if
     * Namespace processing is not being performed.
     * @param qn - The qualified name (with prefix), or the empty string if
     * qualified names are not available.
     * @param a - The attributes attached to the element. If there are no
     * attributes, it shall be an empty Attributes object. 
     */
    public void startElement(String u, String l, String qn, Attributes a) 
	    throws SAXException {

	if (debug) System.err.println("<" + qn + ">");
	c = new char[0];
	if ( attrElements.contains(qn) ) {
	    attrStack.push(attrs);
	    attrs = new HashMap<String, String>();
	}
	if ( nameElements.contains(qn) ) {
	    nameStack.push(name);
	    name = null;
	}
	if ( statusElements.contains(qn) ) {
	    statusStack.push(status);
	    status = null;
	}
	if ( localnameElements.contains(qn) ) {
	    collectLocalnames = true;
	}
	if ( topoElements.contains(qn) ) {
	    elementsStack.push(elements);
	    substratesStack.push(subs);
	    nameToSubstrateStack.push(nameToSubstrate);
	    elements = new ArrayList<Element>();
	    subs = new ArrayList<Substrate>();
	    nameToSubstrate = new HashMap<String, Substrate>();
	}
	if (qn.equals("id")) inID = true;
	if (qn.equals("cpu")) {
	    String n = a.getValue("count");
	    if ( n != null) ncpus = Integer.valueOf(n);
	    else ncpus = 1;
	}
    }

    /**
     * Collect the data from each element and stash it where any containing
     * elent can find it when it is time to construct that thing.  Clear
     * any fields used.  This is long but straightforward parsing.
     * @param u - The Namespace URI, or the empty string if the element has
     * no Namespace URI or if Namespace processing is not being performed.
     * @param l - The local name (without prefix), or the empty string if
     * Namespace processing is not being performed.
     * @param qn - The qualified name (with prefix), or the empty string if
     * qualified names are not available.
     */
    public void endElement(String u, String l, String qn) 
	    throws SAXException {

	try{
	    if (debug) System.err.println("<" + qn + "/>");

	    // Each branch parses the data from the given element, e.g,
	    // "computer" parses the fields collected for </computer>.  In that
	    // case the data stashed by trips through here for
	    // <operatingsystem>, <name>, etc.
	    //
	    // The vector.toArray(new Type[vector.size()], idiom just returns
	    // the contents of the vector as a java array of the vector's type.
	    if (qn.equals(topName)) {
		topo = new TopologyDescription("2.0", subs, elements, frags, 
			nameMaps, attrs);
		elements = elementsStack.pop();
		subs = substratesStack.pop();
		attrs = attrStack.pop();
		frags = new ArrayList<Fragment>();
		nameMaps = new ArrayList<NameMap>();
		nameToSubstrate = nameToSubstrateStack.pop();
	    }
	    else if (qn.equals("elements")) {
	    }
	    else if (qn.equals("computer")) {
		elements.add(
			new Computer(name, interfaces, cpus, oses, software,
			    storage, localnames, status, services, operations,
			    attrs)
		);

		name = nameStack.pop();
		cpus = new ArrayList<CPU>();
		oses = new ArrayList<OperatingSystem>();
		software = new ArrayList<Software>();
		storage = new ArrayList<Storage>();
		interfaces = new ArrayList<Interface>();
		attrs = attrStack.pop();
		localnames = new ArrayList<String>();
		status = statusStack.pop();
		services = new ArrayList<Service>();
		operations = new ArrayList<String>();
		collectLocalnames = false;
	    }
	    else if (qn.equals("cpu")) {
		cpus.add(new CPU(type, ncpus, attrs));
		type = null;
		attrs = attrStack.pop();
		ncpus = 1;
	    }
	    else if (qn.equals("type")) { type = new String(c).trim(); }
	    else if (qn.equals("os")) {
		oses.add(new OperatingSystem(name, version, 
			distribution, distributionversion, attrs));
		name = nameStack.pop();
		version = distribution = distributionversion = null;
		attrs = attrStack.pop();
	    }
	    else if ( qn.equals("version")) { version = new String(c).trim(); }
	    else if ( qn.equals("distribution")) {
		distribution = new String(c).trim();
	    }
	    else if ( qn.equals("distributionversion")) {
		distributionversion = new String(c).trim();
	    }
	    else if (qn.equals("software")) {
		software.add(new Software(location, install, 
			    attrs));
		location = install = null;
		attrs = attrStack.pop();
	    }
	    else if ( qn.equals("location")) { 
		location = new String(c).trim();
	    }
	    else if ( qn.equals("install")) { install = new String(c).trim(); }
	    else if (qn.equals("storage")) {
		storage.add(new Storage(amount, persistence, attrs));
		amount = 0.0;
		persistence = null;
		attrs = attrStack.pop();
	    }
	    else if ( qn.equals("amount")) { 
		amount = Double.valueOf(new String(c));
	    }
	    else if ( qn.equals("persistence")) { 
		persistence = new String(c).trim();
	    }
	    else if (qn.equals("interface")) {
		interfaces.add(new Interface(ifsub, name, 
			    cap, lat, attrs));
		ifsub = null;
		name = nameStack.pop();
		cap = null;
		lat = null;
		attrs = attrStack.pop();
	    }
	    else if (qn.equals("substrate")) {
		// XXX error message/exception
		Substrate s = nameToSubstrate.get(new String(c).trim());
		ifsub = s;
	    }
	    else if (qn.equals("capacity")) {
		cap = new Capacity(rate, kind);
		rate = 0.0;
		kind = null;
	    }
	    else if ( qn.equals("rate")) { 
		rate = Double.valueOf(new String(c));
	    }
	    else if ( qn.equals("kind")) { 
		kind = new String(c).toLowerCase();
	    }
	    else if (qn.equals("latency")) {
		lat = new Latency(time, kind);
		time = 0.0;
		kind = null;
	    }
	    else if ( qn.equals("time")) { 
		time = Double.valueOf(new String(c));
	    }
	    else if (qn.equals("testbed")) {
		elements.add(
			new Testbed(name, uri, type, interfaces, localnames,
			    status, services, operations, attrs)
		);
		name = nameStack.pop();
		uri = type = null;
		interfaces = new ArrayList<Interface>();
		attrs = attrStack.pop();
		localnames = new ArrayList<String>();
		status = statusStack.pop();
		services = new ArrayList<Service>();
		operations = new ArrayList<String>();
		collectLocalnames = false;
	    }
	    else if (qn.equals("uri")) { 
		if (inID) id_uri = new String(c).trim();
		else uri = new String(c).trim();
	    }
	    else if (qn.equals("segment")) {
		elements.add(
			new Segment(id, name, uri, type, interfaces,
			    localnames, status, services, operations, attrs)
		);
		name = nameStack.pop();
		id = null;
		type = uri = null;
		interfaces = new ArrayList<Interface>();
		localnames = new ArrayList<String>();
		status = statusStack.pop();
		services = new ArrayList<Service>();
		operations = new ArrayList<String>();
		collectLocalnames = false;
		attrs = attrStack.pop();
	    }
	    else if (qn.equals("id")) { 
		id = new Segment.ID(uuid, fedid, id_uri, localname, 
			kerberosUsername);
		uuid = null;
		fedid = null;
		id_uri = null;
		localname = null;
		kerberosUsername = null;
		inID = false;
	    }
	    else if (qn.equals("uuid")) {
		uuid = new String(c).trim().getBytes();
	    }
	    else if (qn.equals("fedid")) {
		fedid = new String(c).trim().getBytes();
	    }
	    else if (qn.equals("localname")) {
		if (collectLocalnames) localnames.add(new String(c).trim());
		else localname = new String(c).trim();
	    }
	    else if (qn.equals("kerberosUsername")) { 
		kerberosUsername = new String(c).trim();
	    }
	    else if (qn.equals("description")) { 
		description = new String(c).trim();
	    }
	    else if (qn.equals("other")) { 
		elements.add(new OtherElement(name, interfaces, attrs));
		name = nameStack.pop();
		interfaces = new ArrayList<Interface>();
		attrs = attrStack.pop();
	    }
	    else if (qn.equals("substrates")) {
		Substrate sub = new Substrate(name, cap, lat, localnames,
			status, services, operations, attrs);
		subs.add(sub);
		nameToSubstrate.put(name, sub);
		name = nameStack.pop();
		cap = null;
		lat = null;
		attrs = attrStack.pop();
		localnames = new ArrayList<String>();
		status = statusStack.pop();
		services = new ArrayList<Service>();
		operations = new ArrayList<String>();
		collectLocalnames = false;
	    }
	    else if (qn.equals("attribute")) {
		if ( aname != null && aval != null ) {
		    attrs.put(aname, aval);
		    aname = aval = null;
		}
		else { aname = new String(c).trim(); }
	    }
	    else if ( qn.equals("value")) { aval = new String(c).trim(); }
	    else if ( qn.equals("name")) { name = new String(c).trim(); }
	    else if ( qn.equals("param") ) {
		serviceParams.add(new Service.Param(name, type));
		name = nameStack.pop();
		type = null;
	    }
	    else if (qn.equals("service")) {
		services.add(
			new Service(name, importers, serviceParams,
			    description, status));
		name = nameStack.pop();
		importers = new ArrayList<String>();
		serviceParams = new ArrayList<Service.Param>();
		description = null;
		status = statusStack.pop();
	    }
	    else if (qn.equals("region")) {
		elements.add(
			new Region(name, level, fragName, interfaces, attrs));
		name = nameStack.pop();
		level = 1;
		fragName = null;
		interfaces = new ArrayList<Interface>();
		attrs = attrStack.pop();
	    }
	    else if (qn.equals("level")) { 
		level = Integer.parseInt(new String(c));
	    }
	    else if (qn.equals("fragname")) {
		fragName = new String(c).trim();
	    }
	    else if (qn.equals("fragments")) {
		frags.add(new Fragment(name, subs, elements, ifmap, attrs));
		name = nameStack.pop();
		elements = elementsStack.pop();
		subs = substratesStack.pop();
		attrs = attrStack.pop();
		nameToSubstrate = nameToSubstrateStack.pop();
		ifmap = new HashMap<String, String>();
	    }
	    else if (qn.equals("inner")) { inner = new String(c).trim(); }
	    else if (qn.equals("outer")) { outer = new String(c).trim(); }
	    else if (qn.equals("ifmap")) { 
		ifmap.put(outer, inner);
		inner = null;
		outer = null;
	    }
	    else if (qn.equals("pathname")) { pathname = new String(c).trim(); }
	    else if (qn.equals("namemap")) { 
		nameMapping.put(outer, inner);
		inner = null;
		outer = null;
	    }
	    else if (qn.equals("namemaps")) { 
		nameMaps.add(new NameMap(pathname, nameMapping, attrs));
		pathname = null;
		nameMapping = new HashMap<String, String>();
		attrs = attrStack.pop();
	    }
	    // Always clear any accumulated characters
	    c = new char[0];
	}
	catch (TopologyException e) {
	    throw new SAXException(e);
	}
    }

    /** 
     * Collect text.
     */
    public void characters(char[] ch, int s, int l) {
	char[] nc = new char[c.length + l];
	System.arraycopy(c, 0, nc, 0, c.length);
	System.arraycopy(ch, s, nc, c.length, l);
	c = nc;
    }
    /**
     * Return the parsed topology
     * @return the parsed topology
     */
    public TopologyDescription getTopology() { return topo; }
}
