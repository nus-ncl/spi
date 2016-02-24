package net.deterlab.testbed.experiment;

import java.util.Collection;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ExperimentAspect;

import net.deterlab.testbed.topology.TopologyDescription;

/**
 * This Aspect is unimplemented and attempts to manipulate it result in
 * exceptions.
 * @author ISI DETER team
 * @version 1.0
 */
public class UnimplementedAspect implements Aspect {
    /** The aspect type this instance processes. */
    protected String type;

    /**
     * Set the type of this DefaultAspect.
     * @param t the type string
     */
    public UnimplementedAspect(String t) {
	type = t;
    }

    /**
     * Return the type of aspect this class processes.
     * @return the type of aspect this class processes.
     */
    public String getType() { return type; }
    /**
     * Begin an transaction on the experiment.  Nothing to do.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @throws DeterFault if the transaction cannot be started
     */
    public void beginTransaction(ImmutableExperimentDB exp, long transactionID)
	throws DeterFault { }

    /**
     * Reject an attempt to add this unimplemented aspect.
     * input aspect out.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param inputAspect the requested aspect addition
     * @return a collection of aspects to be added to the experiment
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> addAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect inputAspect)
	    throws DeterFault {
	throw new DeterFault(DeterFault.unimplemented, "Aspect " + getType() +
		" is unimplemented");
    }
    /**
     * Reject an attempt to change this unimplemented aspect.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param inputAspect the requested aspect addition
     * @return a collection of aspects to be overwritten in the experiment
     * @throws DeterFault if the aspect change is unacceptable (which it always
     *	is)
     */
    public Collection<ExperimentAspect> changeAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect inputAspect)
	    throws DeterFault {
	throw new DeterFault(DeterFault.unimplemented, "Aspect " + getType() +
		" is unimplemented");
    }
    /**
     * Reject an attempt to remove this aspect (which should be un-addable...)
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param removeAspect the requested aspect removal, only name and type are
     *	    necessarily valid.
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> removeAspect(
	    ImmutableExperimentDB exp, long transactionID,
	    ExperimentAspect removeAspect) throws DeterFault {
	throw new DeterFault(DeterFault.unimplemented, "Aspect " + getType() +
		" is unimplemented");
    }

    /**
     * Reject an attempt to realize this aspect (which should be un-addable...)
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
	throw new DeterFault(DeterFault.unimplemented, "Aspect " + getType() +
		" is unimplemented");
    }

    /**
     * Resources for the current realization are being released.  The plugin
     * should adjust its internal state in any way necessary.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param releaseAspect the requested aspect realization, only name and
     *	    type are necessarily valid.
     * @throws DeterFault on catastrophic problems
     */
    public void releaseAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect releaseAspect)
	throws DeterFault {
	throw new DeterFault(DeterFault.unimplemented, "Aspect " + getType() +
		" is unimplemented");
    }

    /**
     * Finalize the transaction.  Nothing to do.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @throws DeterFault if the transaction cannot be started
     */
    public void finalizeTransaction(ImmutableExperimentDB exp,
	    long transactionID) throws DeterFault { }
}
