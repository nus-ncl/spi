package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A TopologyObject with interfaces
 * @author DeterTeam
 * @version 1.0
 */
public class ConnectedObject extends AttributedObject {
    /** The interfaces connected to this element */
    private Set<Interface> interfaces;
    /** Set of interface names in use.  Only of interest in connected objects
     * with uniqueInterfaces set.  Resist the urge to combine this and the
     * interfaces into a Map - that will fail on objects with multiple
     * interfaces with the same name - Substrates. */
    private Set<String> names;

    /** if true, this object requires all infterace names to be unique, and the
     * object will enforce it. */
    private boolean uniqueInterfaces;
    /**
     * If true this object will serialize interfaces */
    private boolean serializeInterfaces;

    /** The prefix for generated names. */
    private String prefix;

    /** The default prefix for all objects */
    private static String defaultPrefix = "obj-%d";
    /**
     * Basic initializer
     */
    public ConnectedObject() {
	super();
	interfaces = new HashSet<Interface>();
	names = new HashSet<String>();
	uniqueInterfaces = true;
	serializeInterfaces = true;
	prefix = defaultPrefix;
    }

    /**
     * Initialize an element's interfaces and attributes.  Connect the
     * interfaces to this element.  Note that duplicate interfaces are silently
     * dropped by this constructor.
     * @param ifs the interfaces - not copied
     * @param a the atrributes (may be null or empty)
     * @param ua true if interfaces must have unique names
     * @param si true if interfaces should be serialized by writeXML
     */
    public ConnectedObject(Collection<Interface> ifs, Map<String, String> a, 
	    boolean ua, boolean si) {
	super(a);
	interfaces = new HashSet<Interface>();
	names = new HashSet<String>();
	uniqueInterfaces = ua;
	serializeInterfaces = si;
	setPrefix(defaultPrefix);
	if ( ifs == null ) return;
	for (Interface i: ifs) 
	    addInterface(i);
    }
    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof ConnectedObject))
	    throw new IsomorphismException("Not a ConnectedObject", o);
	super.sameAs(o);
	ConnectedObject c = (ConnectedObject) o;

	if ( uniqueInterfaces != c.getUniqueInterfaces())
	    throw new IsomorphismException(
		    "Different notions of interface uniqueness", o);

	Set<Interface> otherIfs = c.getInterfaces();

	if ( interfaces.size() != otherIfs.size())
	    throw new IsomorphismException("Different number of interfaces", o);

	// Rather than call sameSets, this uses some knowledge of interfaces -
	// they have names that must match - to be more efficient.  We index
	// all of this object's interfaces by name and confirm that all of
	// the comparison objects interfaces are congruent to one of them.
	Map<String, List<Interface>> nameToIf =
	    new HashMap<String, List<Interface>>();

	for (Interface i: interfaces) {
	    if ( !nameToIf.containsKey(i.getName()))
		nameToIf.put(i.getName(), new ArrayList<Interface>());
	    nameToIf.get(i.getName()).add(i);
	}
	try {
	    for (Interface i: otherIfs) {
		if (!nameToIf.containsKey(i.getName()))
		    throw new IsomorphismException("No match for interface", i);
		boolean foundit = false;
		for (Interface j: nameToIf.get(i.getName())) {
		    try {
			i.sameAs(j);
			foundit = true;
			break;
		    }
		    catch (IsomorphismException ignored) {}
		}
		if (!foundit)
		    throw new IsomorphismException("No match for interface", i);
	    }
	}
	catch (IsomorphismException e) {
	    e.addObjectTop(o);
	    throw e;
	}
    }

    /**
     * Return this element's interfaces.  Changing these changes the Element's
     * interfaces.
     * @return the interfaces
     */
    public Set<Interface> getInterfaces() {
	return interfaces;
    }

    /**
     * Returns true if the object enforces unique names on interfaces
     * @return true if the object enforces unique names on interfaces
     */
    public boolean getUniqueInterfaces() { return uniqueInterfaces; }

    /**
     * Set or unset the unique interface requirement.
     * @param v the new requirement.
     */
    public void setUniqueInterfaces(boolean v) { uniqueInterfaces = v; }


    /**
     * Returns true if the object serializes interfaces to XML
     * @return true if the object serializes interfaces to XML
     */
    public boolean getSerializeInterfaces() { return serializeInterfaces; }

    /**
     * Set or unset the serialize interface requirement.
     * @param v the new requirement.
     */
    public void setSerializeInterfaces(boolean v) { serializeInterfaces = v; }

    /**
     * Get the prefix used to generate unique names
     * @return the prefix used to generate unique names
     */
    public String getPrefix() { return prefix; }

    /**
     * Set the prefix used to generate unique names.  The prefix must contain a
     * %d string that is replaced with integers.  If not present, a "-%d" is
     * appended.  If a null value is set, the default is used.
     * @param pre the new prefix
     */
    public void setPrefix(String pre) {
	prefix = (pre != null ) ? pre : defaultPrefix;
	if ( prefix.indexOf("%d") == -1) 
	    prefix = prefix + "-%d";
    }

    /**
     * Add an interface to this Object, making the name unique if
     * uniqueInterfaces is set.  If the name is made unique, the change is also
     * made to the interface object.  If the name is null, pick a unique one.
     * @param inf the interface to add
     */
    public void addInterface(Interface inf) {
	if ( interfaces.contains(inf)) return;
	if ( uniqueInterfaces ) {
	    int i =0;
	    String prefix = "inf-%d";

	    if (inf.getName() == null )
		inf.setName(String.format(prefix, 0));

	    while ( names.contains(inf.getName()))
		inf.setName(String.format(prefix, i++));
	}
	interfaces.add(inf);
	names.add(inf.getName());
    }

    /**
     * Remove an interface from this Object
     * @param i the interface to remove
     * @return true if the interface was present
     */
    public boolean removeInterface(Interface i) {

	if ( !interfaces.remove(i) ) return false;
	// An interface was removed.  If the removed interface's name was
	// unique on this object, remove the name from names.  We look in case
	// the uniqueInterfaces flag might change.
	String iname = i.getName();
	if ( iname == null ) return true;

	for ( Interface ii: interfaces)
	    if ( iname.equals(ii.getName())) return true;

	names.remove(iname);
	return true;
    }

    /**
     * The name of the inner XML element for the usual representation of this
     * element.
     * @return the element name
     */
    public String getXMLElement() { return null; }

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

	if ( ename != null) p.println("<" + ename + ">");
	if ( serializeInterfaces) {
	    for (Interface i: interfaces)
		i.writeXML(p, "interface");
	}
	super.writeXML(p, "attribute");
	if ( ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
