
package net.deterlab.testbed.topology;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;

import java.util.Map;

/**
 * The storage requirements of a computer or other entity.  The amount and its
 * persistance.  Amounts are in megabytes.  Should media be required, an
 * attribute can be used.
 * @author DeterTeam
 * @version 1.0
 */
public class Storage extends AttributedObject {
    /** The amount of storage in MB */
    double amount;
    /** The persistence "temporary" or "permanent" */
    String persistence;
    /**
     * Basic initializer
     */
    public Storage() {
	super();
	amount = 0.0;
	persistence = null;
    }

    /**
     * Construct from parts
     * @param amt the amount
     * @param p the persistence 
     * @param a the attached attributes
     */
    public Storage(double amt, String p, Map<String, String> a) {
	super(a);
	amount = amt;
	persistence = p;
    }

    /**
     * Copy constructor
     * @param s the object to copy
     */
    public Storage(Storage s) {
	this(s.getAmount(), s.getPersistence(), s.getAttributes());
    }
    /**
     * See if this object and the given one match in isomporphic topologies.
     * If not throw an exception that tracks the point of inconsistency.
     * @param o the object to check
     * @throws IsomorphismException if there is a mismatch
     */
    public void sameAs(TopologyObject o) throws IsomorphismException {
	if ( !(o instanceof Storage))
	    throw new IsomorphismException("Not a Storage", o);
	super.sameAs(o);
	Storage s = (Storage) o;
	if ( amount != s.getAmount() )
	    throw new IsomorphismException("Different amounts", o);
	if (!equalObjs(persistence, s.getPersistence()))
	    throw new IsomorphismException("Different persistence", o);
    }

    /**
     * Get the amount
     * @return the amount
     */
    public double getAmount() { return amount; }

    /**
     * Set the amount
     * @param a the new amount
     */
    public void setAmount(double a) { amount = a; }

    /**
     * Get the persistence
     * @return the persistence
     */
    public String getPersistence() { return persistence; }

    /**
     * Set the persistence
     * @param p the new persistence
     */
    public void setPersistence(String p) { persistence = p; }

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
	p.println("<amount>" + amount + "</amount>");
	p.println("<persistence>" + persistence + "</persistence>");
	super.writeXML(p, "attribute");
	if (ename != null) p.println("</" + ename + ">");
	p.flush();
    }
}
