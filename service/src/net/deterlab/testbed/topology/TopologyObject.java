package net.deterlab.testbed.topology;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The base object for things in a topology.  It is mostly a place for routines
 * used to compare topologies for isomorphism.
 * @author DeterTeam
 * @version 1.0
 */
public abstract class TopologyObject {
    /**
     * Basic initializer
     */
    public TopologyObject() { }

    /**
     * Copy constructor
     * @param to the object to copy
     */
    public TopologyObject(TopologyObject to) { }

    /**
     * This checks to see if the two topology objects are the same and are at
     * the same place in a topology - it's seeing if the two objects have the
     * same parameters and enighbors, basically. It's used in confirming two
     * topologies are the same.  The exceptions track the mismatch on the
     * parameter topology.
     * @param o the Object to compare
     * @throws IsomorphismException if the objects differ, including info about
     * how.
     */
    public abstract void sameAs(TopologyObject o) throws IsomorphismException;

    /**
     * Output this object's XML representation. If ename is given, surround the
     * output with an element with that name.  That allows the same signature
     * to let a superclass add its elements to an output stream without an
     * enclosure and emebedded elements to enclose themselves.
     * @param w the writer for output
     * @param ename the name of the element enclosing this object (may be null)
     * @throws IOException on a writing error.
     */
     public abstract void writeXML(Writer w, String ename) throws IOException;

     /**
      * Compare lists of TopologyElements pairwise for isomorphism
      * @param a a list
      * @param b a list
      * @throws IsomorphismException if an entry is not same as the other (the
      * b-side is reported)
      */
     protected static void sameLists(List<? extends TopologyObject> a,
	     List<? extends TopologyObject> b) throws IsomorphismException {
	 if ( a.size() != b.size())
	     throw new IsomorphismException("Different List lengths");
	 final int lim = a.size();

	 for (int i = 0; i < lim; i++) {
	     TopologyObject aa = a.get(i);
	     TopologyObject bb = b.get(i);

	     if ( aa == null ) {
		 if ( bb == null) continue;
		 else throw new IsomorphismException("Unmatched null in list");
	     }
	     aa.sameAs(bb);
	 }
     }

     /**
      * Compare sets of TopologyElements pairwise for isomorphism.  This is a
      * general, n^2 implementation.
      * @param a a set
      * @param b a set
      * @throws IsomorphismException if an entry is not same as the other (the
      * b-side is reported)
      */
     protected static void sameSets(Set<? extends TopologyObject> a,
	     Set<? extends TopologyObject> b) throws IsomorphismException {
	 for (TopologyObject bb : b) {
	     boolean foundit = false;
	     for (TopologyObject aa: a) {
		 try {
		     bb.sameAs(aa);
		     foundit = true;
		     break;
		 }
		 catch (IsomorphismException ignored) { }
	     }
	     if ( !foundit)
		 throw new IsomorphismException("No match in set", bb);
	 }
     }


    /**
     * Compare two byte arrays for equality.
     * @param a an array
     * @param b an array
     * @return true if the arrays have the same contents or are both null
     */
    protected boolean equalByteArrays(byte[] a, byte[] b) {
	if ( a == null ) return b == null;
	if (b == null) return false;
	if ( a.length != b.length ) return false;
	for ( int i = 0; i < a.length ; i++ )
	    if (a[i] != b[i] ) return false;
	return true;
    }

    /**
     * Compare two maps that map Strings to TopologyObjects for isomorphism.
     * The same keys must be present in each map and map to isomorphic (sameAs)
     * objects.
     * @param a a map
     * @param b a map
     * @throws IsomorphismException if the maps differ
     */
    protected void sameMaps(Map<String, ? extends TopologyObject> a,
	    Map<String, ? extends TopologyObject> b)
	throws IsomorphismException {

	if ( a.size() != b.size())
	    throw new IsomorphismException("Different sized maps");

	for (Map.Entry<String, ? extends TopologyObject> ent: b.entrySet()) {
	    if ( ent.getValue() == null ) {
		if ( a.get(ent.getKey()) != null )
		    throw new IsomorphismException(
			    "Mismatched null map entries");
		else continue;
	    }
	    ent.getValue().sameAs(a.get(ent.getKey()));
	}
    }


    /**
     * Utility for comparisons objects where one or the other may be null.  A
     * littel ncier than typing the ? expression in if statements, for example.
     * @param s1 an object to compare (may be null)
     * @param s2 an object to compare (may be null)
     * @return true if s1 and s1 are equal or both null
     */
    protected static boolean equalObjs(Object s1, Object s2) {
	return (s1 != null) ? s1.equals(s2) : s2 == null;
    }
}
