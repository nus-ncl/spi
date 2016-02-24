package net.deterlab.testbed.experiment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ExperimentAspect;
import net.deterlab.testbed.topology.TopologyDescription;

/**
 * This class allows any aspect handed to it to be stored, removed or realized
 * with no effect (other than storage or removal).
 * @author ISI DETER team
 * @version 1.0
 */
public class EmbedderAspect extends DefaultAspect {
    /** The aspect type this instance processes. */
    protected String type;

    /**
     * Set the type of this EmbedderAspect.
     * @param t the type string
     */
    public EmbedderAspect(String t) {
	super(t);
    }

    /**
     * Realize this aspect.  The plugin can make changes to the input topology
     * that represents the realized experiment, for example to add elements
     * that are necessary for the aspect.  If the realization is unacceptable,
     * the plugin can throw a DeterFault to preempt it.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param realizeAspect the requested aspect realization, only name and
     *	    type are necessarily valid.
     * @param realTop the realization topology
     * @return a changed TopologyDescription or null if no changes are made.
     * @throws DeterFault if the aspect realization is unacceptable
     */
    public TopologyDescription realizeAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect realizeAspect,
	    TopologyDescription realTop) throws DeterFault {
	boolean changed = false;

	if ( getType() == null )
	    throw new DeterFault(DeterFault.internal, "No type in aspect");

	if (getType().equals(realizeAspect.getType())) {
	    List<ExperimentAspect> aspects = exp.getAspects(
		    Arrays.asList(new ExperimentAspect[] {realizeAspect}),
		    true);
	    for (ExperimentAspect a : aspects) {
		try {
		    Properties props = new Properties();

		    props.loadFromXML(new ByteArrayInputStream(a.getData()));
		    for (String p : props.stringPropertyNames()) {
			String v = null;

			if ( p == null ) continue;
			if ( (v = props.getProperty(p))== null ) continue;
			if ( realTop.getAttribute(p) == null) {
			    realTop.setAttribute(p, v);
			    changed = true;
			}
		    }
		}
		catch (IOException ie) {
		    throw new DeterFault(DeterFault.internal,
			    "IO error!?: " + ie.getMessage());
		}
	    }
	}
	return (changed) ? realTop : null;
    }
}
