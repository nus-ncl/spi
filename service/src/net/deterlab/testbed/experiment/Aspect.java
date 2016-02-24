package net.deterlab.testbed.experiment;

import java.util.Collection;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ExperimentAspect;

import net.deterlab.testbed.topology.TopologyDescription;

/**
 * This is the interface that all Aspect plugins implement.
 * @author ISI DETER team
 * @version 1.0
 */
public interface Aspect  {

    /**
     * Return the type of aspect this class processes.
     * @return the type of aspect this class processes.
     */
    public String getType();
    /**
     * Begin an transaction on the experiment.  One or more instances of this
     * aspect are being added, removed, or realized in an experiment.  Calls to
     * the other interfaces will be made with this transactionID and then
     * finalized.  The plugin may cache information to do consistency checking
     * on other operations.  In principle a transactionID can be reused after
     * finalizeTransaction is called on it.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @throws DeterFault if the transaction cannot be started
     */
    public void beginTransaction(ImmutableExperimentDB exp, long transactionID)
	throws DeterFault;

    /**
     * Add a new instance of this aspect to the experiment.  The input
     * aspect is the raw data pulled from the request. This routine may reparse
     * and reformat that data as requires, as well as adding additional
     * sub-aspects to the return value.  The SPI will only accept new aspects
     * of the same type and name as the input aspect, but other fields may be
     * manipulated.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param inputAspect the requested aspect addition
     * @return a collection of aspects to be added to the experiment
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> addAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect inputAspect)
	throws DeterFault;
    /**
     * Request a change to this aspect to the experiment.  The input aspect is
     * the raw data pulled from the request and the Data or DataReference is a
     * request to manipulate that data in a format that the aspect expects.
     * This routine may reparse and reformat that data as requires, as well as
     * adding additional sub-aspects to the return value.  The SPI will only
     * accept new aspects of the same type and name as the input aspect, but
     * other fields may be manipulated.  The Collection returned from here will
     * overwrite the existing aspects (or add them if new) when the transaction
     * is finalized.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param inputAspect the requested aspect addition
     * @return a collection of aspects to be overwritten in the experiment
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> changeAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect inputAspect) throws DeterFault;
    /**
     * Remove an instance of this aspect from the experiment.  The removeAspect
     * parameter includes the name and type of the aspect to remove - other
     * fields are not guaranteed to be valid.  If aspect details are needed,
     * they can be requested from the experiment.  The aspect may choose to add
     * subaspects to the list of aspects to delete.  The return value includes
     * all the aspects to remove.  Unless this routine throws a DeterFault, the
     * aspects are queued for removal.  Actual removal isn't done until
     * finalizeTransaction is called, so the plugin can veto the removal there
     * as well.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param removeAspect the requested aspect removal, only name and type are
     *	    necessarily valid.
     * @return a collection of aspects to be removed from the experiment
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> removeAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect removeAspect)
	throws DeterFault;

    /**
     * Realize this aspect.  The plugin can make changes to the input
     * realization description that represents the realized experiment, for
     * example to add elements that are necessary for the aspect.  If the
     * realization is unacceptable, the plugin can throw a DeterFault to
     * preempt it.  If the aspect changes the realization, it should return
     * the modified or a new topology deacription. and additional calls to
     * realize may happen.  If the aspect makes no changes, it should return
     * null.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param realizeAspect the requested aspect realization, only name and
     *	    type are necessarily valid.
     * @param realTop the realization description being collaborated on
     * @return a changed TopologyDescription or null if no changes are made.
     * @throws DeterFault if the aspect realization is unacceptable
     */
    public TopologyDescription realizeAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect realizeAspect,
	    TopologyDescription realTop) throws DeterFault;

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
	throws DeterFault;

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
	    long transactionID) throws DeterFault;
}
