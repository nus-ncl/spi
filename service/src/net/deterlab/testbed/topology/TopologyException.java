package net.deterlab.testbed.topology;

/**
 * Problem with topology.  Behaves exactly like an Exception.
 * @author DeterTeam
 * @version 1.0
 */
public class TopologyException extends Exception {
    /**
     * Constructs without a detail message
     */
    public TopologyException() { super(); }

    /**
     * Constructs with a detail message
     * @param m the detail message
     */
    public TopologyException(String m) { super(m); }

    /**
     * Constructs with a detail message and a cause
     * @param m the detail message
     * @param c the cause
     */
    public TopologyException(String m, Throwable c) { super(m, c); }

    /**
     * Constructs with a cause
     * @param c the cause
     */
    public TopologyException(Throwable c) { super(c); }
}
