package net.deterlab.testbed.embedding;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.RealizationDescription;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.topology.TopologyDescription;

/**
 * The interface for classes that embed topologies into testbeds.
 * @author the DETER Team
 * @version 1.1
 */
public interface Embedder {
    /**
     * Begin realizing an experiment on the testbed in the given circle.  This
     * actually starts the realization process.  Users will need to poll the
     * realization to determine when the realization is complete.
     * @param uid user id realizing the experiment
     * @param eid experiment ID to realize
     * @param cid the circle in which to realize the experiment
     * @param acl the initial access control list
     * @param td a description of the topology to create
     * @param sendNotifications if true send notifications when state changes
     * @return a description of the realization.
     * @throws DeterFault on errors
     */
    public RealizationDescription startRealization(String uid, String eid,
	    String cid, AccessMember[] acl, TopologyDescription td,
	    boolean sendNotifications) throws DeterFault;

    /**
     * Terminate the realization, whether in process or complete.  Release
     * resources and cancel the process.  Status remains live, and the
     * realization can be restarted.
     * @param uid the user calling
     * @param name the realization to terminate
     * @return current realization description
     * @throws DeterFault on errors
     */
    public RealizationDescription terminateRealization(String uid, String name)
	throws DeterFault;

}
