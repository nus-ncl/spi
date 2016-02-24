package net.deterlab.testbed.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ExperimentAspect;

import net.deterlab.testbed.topology.TopologyDescription;

/**
 * This class allows any aspect handed to it to be stored, removed or realized
 * with no effect (other than storage or removal).
 * @author ISI DETER team
 * @version 1.0
 */
public class DefaultAspect implements Aspect {
    /** The aspect type this instance processes. */
    protected String type;

    /**
     * Set the type of this DefaultAspect.
     * @param t the type string
     */
    public DefaultAspect(String t) {
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
     * Add a new instance of this aspect to the experiment. Just copies the
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
	List<ExperimentAspect> rv = new ArrayList<ExperimentAspect>();
	rv.add(inputAspect);
	return rv;
    }
    /**
     * Request a change to this aspect to the experiment.  This Aspect
     * overwrites an existing entry with the new data.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param inputAspect the requested aspect addition
     * @return a collection of aspects to be overwritten in the experiment
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> changeAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect inputAspect)
	    throws DeterFault {
	List<ExperimentAspect> rlist = new ArrayList<ExperimentAspect>();
	List<ExperimentAspect> rv = new ArrayList<ExperimentAspect>();
	List<ExperimentAspect> asps = null;
	ExperimentAspect req = new ExperimentAspect();

	req.setName(inputAspect.getName());
	req.setType(inputAspect.getType());
	req.setSubType(inputAspect.getSubType());
	rlist.add(req);

	asps = exp.getAspects(rlist, false);

	if (asps.size() == 0)
	    throw new DeterFault(DeterFault.request, "No such aspect");
	else if ( asps.size() > 1)
	    throw new DeterFault(DeterFault.internal,
		    "More than one such aspect!?");

	// Overwrite with the new aspect
	rv.add(inputAspect);
	return rv;
    }
    
    /**
     * Remove an instance of this aspect from the experiment.  The removeAspect
     * parameter includes the name and type of the aspect to remove - other
     * fields are not guaranteed to be valid.  If aspect details are needed,
     * they can be requested from the experiment.  Implicit in the removal
     * request is the removal of all sub-aspects with the same type and name.
     * Unless this routine throws a DeterFault, the aspect and subaspect are
     * queued for removal.  Actual removal isn't done until finalizeTransaction
     * is called, so the plugin can veto the removal there as well.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param removeAspect the requested aspect removal, only name and type are
     *	    necessarily valid.
     * @return a collection of aspects to be removed from the experiment
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> removeAspect(
	    ImmutableExperimentDB exp, long transactionID,
	    ExperimentAspect removeAspect) throws DeterFault {
	List<ExperimentAspect> rlist = new ArrayList<ExperimentAspect>();
	List<ExperimentAspect> rv = new ArrayList<ExperimentAspect>();
	List<ExperimentAspect> asps = null;
	ExperimentAspect req = new ExperimentAspect();

	req.setName(removeAspect.getName());
	req.setType(getType());
	req.setSubType(removeAspect.getSubType());
	rlist.add(req);

	asps = exp.getAspects(rlist, false);

	if (asps.size() == 0)
	    throw new DeterFault(DeterFault.request, "No such aspect");

	for (ExperimentAspect ea : exp.getAspects(Arrays.asList(req), false)) {
		// Remove all matching aspects
		rv.add(ea);
	}
	
	return rv;
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
	return null;
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
    }

    /**
     * Finalize the transaction.  Unless the plugin throws a DeterFault, the
     * transaction to date will be carried out.  After this returns, the
     * transactionID can be reused.  The plugin should release any resources it
     * has been using to validate the transaction.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @throws DeterFault if the transaction cannot be started
     */
    public void finalizeTransaction(ImmutableExperimentDB exp,
	    long transactionID) throws DeterFault { }
}
