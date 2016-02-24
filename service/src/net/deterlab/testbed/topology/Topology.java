package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of connected Elements and Substrates.  This is a base for
 * TopologyDescriptions and Fragments.
 * @author DeterTeam
 * @version 1.0
 */
public class Topology extends AttributedObject implements Cloneable {
    /** Elements, indexed by name*/
    private Map<String, Element> elements;
    /** Substrates, indexed by name*/
    private Map<String, Substrate> substrates;

    /**
     * Basic initializer
     */
    public Topology() {
	super();
	substrates = new HashMap<String, Substrate>();
	elements = new HashMap<String, Element>();
    }

    /**
     * Initialize a substrate from parts
     * @param subs the substrates
     * @param elems the elements
     * @param a the attributes
     * @throws TopologyException if the subs or elements are inconsistent
     */
    public Topology(Collection<Substrate> subs, Collection<Element> elems,
	    Map<String, String> a) throws TopologyException{
	super(a);
	substrates = new HashMap<String, Substrate>();
	elements = new HashMap<String, Element>();

	if ( subs != null) 
	    for (Substrate s : subs) 
		addSubstrate(s);
	if ( elems != null) 
	    for (Element e: elems)
		addElement(e);
    }

    /**
     * Copy constructor - new substrates and elements,
     * interconnected by interface copies that point inside the new topology.
     * @param t the Topology to copy
     * @throws TopologyException if the given Topology is inconsistent
     */
    public Topology(Topology t) throws TopologyException {
	this(null, null, t.getAttributes());

	for (Substrate s: t.getSubstrates()) {
	    Substrate ns = s.clone();

	    addSubstrate(ns);
	}
	for (Element e: t.getElements()) {
	    Element ne = e.clone();
	    for (Interface i : e.getInterfaces()) {
		Interface ni = i.clone();
		Substrate ss = i.getSubstrate();

		if (ss == null ) continue;

		Substrate dest = getSubstrate(ss.getName());
		if ( dest != null ) ni.connect(ne, dest);
	    }
	    addElement(ne);
	}
    }

    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Topology))
	    throw new IsomorphismException("Not a Topology", o);
	super.sameAs(o);
	Topology t = (Topology) o;

	try {
	    sameMaps(substrates, t.substrates);
	    sameMaps(elements, t.elements);
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

    /**
     * Add a substrate to the topology.  If the substrate name is null, pick a
     * unique one.
     * @param s the new substrate
     * @throws TopologyException if the name is a duplicate.
     */
    public void addSubstrate(Substrate s) throws TopologyException {
	addSubstrate(s, false);
    }
    /**
     * Add a substrate to the topology, making the substrate name unique if
     * another instance of that name exists and makeUnique is set.  This will
     * change the name in the substrate.  If the substrate name is null, pick a
     * unique one.
     * @param s the new substrate
     * @param makeUnique if true, use the name as a prefix
     * @throws TopologyException if the name is a duplicate and makeUnique is
     * false
     */
    public void addSubstrate(Substrate s, boolean makeUnique) 
	    throws TopologyException {
	int i =0;

	String prefix = null;
	if ( s.getName() != null ) {
	    s.setPrefix(s.getName());
	    prefix = s.getPrefix();
	}
	else {
	    makeUnique = true;
	    prefix = s.getPrefix();
	    s.setName(String.format(prefix, 0));
	}

	while ( substrates.containsKey(s.getName()) ) {
	    if ( !makeUnique ) 
		throw new TopologyException("duplicate Substrate name " + 
		    s.getName());
	    s.setName(String.format(prefix, i++));
	}
	substrates.put(s.getName(), s);
    }


    /**
     * Remove the given substrate from this topology.  Interfaces must be
     * removed from the various elements as well.
     * @param s the substrate to remove
     * @return true if the substrate was present
     */
    public boolean removeSubstrate(Substrate s) {
	return substrates.remove(s.getName()) != null;
    }

    /**
     * Add an element to the topology.
     * @param e the new element
     * @throws TopologyException if the name is a duplicate.
     */
    public void addElement(Element e) throws TopologyException {
	addElement(e, false);
    }

    /**
     * Add an element to the topology, making the element name unique if
     * another instance of that name exists and makeUnique is set.  This will
     * change the name in the element.  If the element name is null, always
     * pick a unique one.
     * @param e the new element
     * @param makeUnique if true, use the name as a prefix
     * @throws TopologyException if the name is a duplicate and makeUnique is
     * false
     */
    public void addElement(Element e, boolean makeUnique) 
	    throws TopologyException {
	int i =0;
	String prefix = null;

	if (e.getName() == null ) {
	    makeUnique = true;
	    prefix = e.getPrefix();
	    e.setName(String.format(prefix, 0));
	}
	else {
	    e.setPrefix(e.getName());
	    prefix = e.getPrefix();
	}

	while ( elements.containsKey(e.getName()) ) {
	    if ( !makeUnique ) 
		throw new TopologyException("duplicate Element name " + 
		    e.getName());
	    e.setName(String.format(prefix, i++));
	}
	elements.put(e.getName(), e);
    }

    /**
     * Remove the given element from this topology.  Interfaces must be
     * removed from the various elements as well.  If the element name is null,
     * always pick a unique one.
     * @param e the element to remove
     * @return true if the element was present
     */
    public boolean removeElement(Element e) {
	return elements.remove(e.getName()) != null;
    }

    /**
     * Return a clone of this topology, new substrates and elements,
     * interconnected by interface copies that point inside the new topology.
     * @return a clone of this topology.
     */
    public Topology clone() {
	try {
	    return new Topology(this);
	}
	catch (TopologyException e) {
	    // Can't really fool with the clone signature, so return null on
	    // error.
	    return null;
	}
    }
    /**
     * Return the elements.  Changes here will be not reflected in the toploogy,
     * use addElement and removeElement to keep indices up to date.
     * @return the elements. 
     */
    public Collection<Element> getElements() { return elements.values(); }

    /**
     * Return the substrates.  Changes here will be noy reflected in the
     * toploogy.  Use addSubstrate and remvoeSubstrate to keep indices correct.
     * @return the elements. 
     */
    public Collection<Substrate> getSubstrates() { return substrates.values(); }

    /**
     * Return the substrate with the given name, if present.
     * @param name the name to look for
     * @return the substrate
     */
    public Substrate getSubstrate(String name) {
	return substrates.get(name);
    }

    /**
     * Return the element with the given name, if present.
     * @param name the name to look for
     * @return the element
     */
    public Element getElement(String name) {
	return elements.get(name);
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
	for (Substrate s: substrates.values())
	    s.writeXML(p, "substrates");
	for (Element e: elements.values()) {
	    p.println("<elements>");
	    e.writeXML(p, e.getXMLElement());
	    p.println("</elements>");
	}
	super.writeXML(p, "attribute");
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
