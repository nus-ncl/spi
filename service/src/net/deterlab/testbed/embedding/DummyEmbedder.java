package net.deterlab.testbed.embedding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.RealizationDescription;
import net.deterlab.testbed.api.ResourceFacet;
import net.deterlab.testbed.api.ResourceTag;
import net.deterlab.testbed.realization.RealizationDB;
import net.deterlab.testbed.resource.ResourceDB;
import net.deterlab.testbed.topology.Element;
import net.deterlab.testbed.topology.TopologyDescription;

/**
 * The interface for classes that embed topologies into testbeds.
 * @author the DETER Team
 * @version 1.1
 */
public class DummyEmbedder implements Embedder {
    /**
     * Data about an interface used in allocation.
     */
    private static class IfData {
	/** Name of the interface */
	public String name;
	/** Name of the peer interface */
	public String peer;
	/** Number of vlans allowed on this interface */
	public int count;

	/**
	 * Create a new interface record.
	 * @param n name of the interface
	 * @param p name of the peer
	 * @param c the count of vlans on this interface
	 */
	public IfData(String n, String p, int c) {
	    name = n;
	    peer = p;
	    count = c;
	}
    }

    /**
     * This class implements a task that changes a realization's state once to
     * the given new state.
     */
    private class RealizationTask extends TimerTask {
	/** The realization to manipulate */
	private String realizationName;
	/** New status to assign */
	private String newStatus;

	/**
	 * Create a new state-adjusting task
	 * @param r the realization to manipulate
	 * @param ns the new state
	 */
	public RealizationTask(String r, String ns) {
	    realizationName = r;
	    newStatus = ns;
	}

	/**
	 * Move the realization to the new state
	 */
	public void run() {
	    try {
		RealizationDB nr = new RealizationDB();

		nr.setName(realizationName);
		nr.load();
		nr.setStatus(newStatus);
		nr.save();
	    }
	    catch (DeterFault ignored ) { /* Defensive Driving */}
	}
    }

    /** A scheduled transition */
    class Transition {
	/** Task to run */
	public RealizationTask rt;
	/** Scheduling delay */
	public long delay;

	/**
	 * Init in place
	 * @param r task to run
	 * @param d scheduling delay
	 */
	public Transition(RealizationTask r, long d) { rt = r; delay = d; }
    }

    // XXX: get one instance out there.
    /** Next vlan number to assign */
    static private int nextVlan = 0;
    /** Timer to update realization states */
    static private Timer timer = new Timer();
    /** Timer task that's updating each realization state */
    static private Map<String, List<RealizationTask> > tasks  = new HashMap<>();

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
	    boolean sendNotifications) throws DeterFault {
	RealizationDB nr = new RealizationDB();
	RealizationDescription rd = new RealizationDescription();
	List<ResourceDB> availableNodes = ResourceDB.getResources(uid,
		"testnode", null, "none", true, new ArrayList<ResourceTag>(),
		-1, -1, null);
	Map<String, List<IfData>> availableInterfaces = new HashMap<>();
	Map<String, String> vmToPnode = new HashMap<>();
	ResourceDB outer = null;
	String rName = null;
	int vms = 0;
	final char destChar = '!';

	nr.create(eid, cid, td, Arrays.asList(acl), uid);
	nr.setEmbedderName(getClass().getName());
	nr.save();
	rName = nr.getName();
	rd.setName(rName);
	rd.setExperiment(nr.getExperimentID());
	rd.setCircle(nr.getCircleID());
	rd.setStatus(nr.getStatus());
	rd.setACL(acl);

	for (Element e : td.getElements()) {
	    if ( outer == null || vms++ >= 4 ) {
		// Need a new resource on which to allocate
		if (availableNodes.size() == 0 )
		    throw new DeterFault(DeterFault.request,
			    "Insufficient Resources");
		if ( outer != null ) outer.close();
		outer = availableNodes.remove(0);
		vms = 0;
	    }
	    // Allocate and assign the new VM
	    ResourceDB vmResource = new ResourceDB("system:" +
		    rName.replace(':', destChar) + destChar +
		    e.getName().replace(':', destChar));
	    vmResource.create(new ResourceFacet[0],
		    new ArrayList<AccessMember>(), new ResourceTag[0]);
	    vmResource.setType("Qemu VM");
	    vmResource.setPersist(false);
	    vmResource.save();
	    vmToPnode.put(e.getName(), outer.getName());

	    // Update the realization
	    nr.addContainmentEntry(outer.getName(), vmResource.getName());
	    nr.addMappingEntry(e.getName(), vmResource.getName());
	    vmResource.close();
	}

	// Save the realization and return the description
	nr.setStatus("Allocated");
	nr.save();
	rd.setStatus(nr.getStatus());
	rd.setContainment(nr.getContainment());
	rd.setMapping(nr.getMapping());

	if ( !tasks.containsKey(rName))
	    tasks.put(rName, new ArrayList<RealizationTask>());

	for (Transition t : new Transition[] {
	    new Transition(new RealizationTask(rName, "Initializing"), 5000L),
	    new Transition(new RealizationTask(rName, "Active"), 30000L), }) {
	    tasks.get(rName).add(t.rt);
	    timer.schedule(t.rt, t.delay);
	}

	nr.close();
	return rd;
    }

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
	throws DeterFault {
	List<RealizationDB> rList = RealizationDB.getRealizations(null,
		"^" + name + "$", -1, -1, null);
	List<ResourceDB> resources = ResourceDB.getResources(null, null, null,
		name, false, new ArrayList<ResourceTag>(), -1, -1, null);
	RealizationDB rdb = null;
	RealizationDescription rd = new RealizationDescription();
	List<RealizationTask> rTasks = null;

	if ( rList.size() == 0 )
	    throw new DeterFault(DeterFault.request,
		    "No such realization: " + name);
	if ( rList.size() > 1 )
	    throw new DeterFault(DeterFault.internal,
		    "Multiple realizations (!?)): " + name);

	rdb = rList.get(0);
	rdb.load();
	rdb.setStatus("Releasing");

	// Order is important.  Get the allocated virtual resources, disconnect
	// them, then delete them.
	resources = ResourceDB.getResources(null, null, null, name, false,
		new ArrayList<ResourceTag>(), -1, -1, null);
	rdb.setMapping(new HashMap<String, Set<String>>());
	rdb.setContainment(new HashMap<String, Set<String>>());
	rdb.save();

	// Cancel tasks
	if ( (rTasks = tasks.get(name)) != null ) {
	    for (RealizationTask rt : rTasks)
		rt.cancel();
	    tasks.remove(name);
	}

	for (ResourceDB r : resources) {
	    r.remove();
	    r.close();
	}

	rdb.setStatus("Empty");
	rdb.save();

	rd.setName(rdb.getName());
	rd.setExperiment(rdb.getExperimentID());
	rd.setCircle(rdb.getCircleID());
	rd.setStatus(rdb.getStatus());
	rd.setACL(rdb.getACL());
	rd.setContainment(rdb.getContainment());
	rd.setMapping(rdb.getMapping());
	rdb.remove();
	rdb.close();
	return rd;
    }
}
