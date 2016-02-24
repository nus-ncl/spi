package net.deterlab.testbed.experiment;

/**
 * Process a visualization aspect of an experiment
 * Right now, this class has no specific functionality
 * @author ISI DETER team
 * @version 1.0
 */
public class VisualizationAspect extends DefaultAspect {
	/** Type of this Aspect */
    static private String TYPE = "visualization";
    
    /**
     * Simple Constructor
     */
    public VisualizationAspect() {
    	super(TYPE);
    }
    
    /**
     * Aspects all take strings in their constructors, but this is ignored.
     * @param ignored nominally the Aspect type. Ignored
     */
    public VisualizationAspect(String ignored) { this(); }
    
}
