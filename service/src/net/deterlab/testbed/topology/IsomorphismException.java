package net.deterlab.testbed.topology;

import java.lang.reflect.Method;

import java.io.PrintStream;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * A sameAs has failed.  This gives a message and a reference to the
 * TopologyElements that caused the problem.
 * @author DeterTeam
 * @version 1.0
 */
public class IsomorphismException extends Exception {
    Deque<TopologyObject> objs;

    /**
     * Constructs without a detail message
     */
    public IsomorphismException() { 
	super(); 
	objs = new ArrayDeque<TopologyObject>();
    }

    /**
     * Constructs with a detail message
     * @param m the detail message
     */
    public IsomorphismException(String m) { 
	super(m);
	objs = new ArrayDeque<TopologyObject>();
    }

    /**
     * Constructs with a detail message and an object to start the Deque
     * @param m the detail message
     * @param t the object causing the fault
     */
    public IsomorphismException(String m, TopologyObject t) { 
	super(m);
	objs = new ArrayDeque<TopologyObject>();
	objs.add(t);
    }

    /**
     * Constructs with a detail message and a cause
     * @param m the detail message
     * @param c the cause
     */
    public IsomorphismException(String m, Throwable c) { 
	super(m, c);
	objs = new ArrayDeque<TopologyObject>();
    }

    /**
     * Constructs with a cause
     * @param c the cause
     */
    public IsomorphismException(Throwable c) { 
	super(c);
	objs = new ArrayDeque<TopologyObject>();
    }

    /**
     * Get the Deque of objects - the highest level is first.
     * @return the objects
     */
    public Deque<TopologyObject> getObjects() { return objs; }

    /**
     * Add a higher level object to the deque
     * @param o the new object
     */
    public void addObjectTop(TopologyObject o) {
	objs.addFirst(o);
    }

    /**
     * Print debuggiung information about where the isomorphism was found to
     * fail.
     * @param p the PrintStream for output
     */
    public void printTrace(PrintStream p) {
	Iterator<TopologyObject> i = objs.iterator();

	while (i.hasNext()) {
	    TopologyObject t = i.next();
	    String name = null;

	    // If t has a getName method, call it to get a name for more
	    // readable output.
	    try {
		Class<?> c = t.getClass();
		Method getName = c.getMethod("getName", new Class<?>[0]);
		if ( getName != null)
		    name = (String) getName.invoke(t, new Object[0]);
	    }
	    catch (Exception ignored) { }

	    if ( name != null ) p.println(t+ " " + name);
	    else p.println(t);
	}
    }
}
