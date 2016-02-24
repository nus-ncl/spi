package net.deterlab.testbed.experiment;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ExperimentAspect;
import net.deterlab.testbed.topology.TopologyDescription;

/**
 * Process an orchestration aspect of an experiment
 * @author ISI DETER team
 * @version 1.0
 */
public class OrchestrationAspect implements Aspect {
	/** Type of this Aspect */
    static private String TYPE = "orchestration";
    
    /** Logger **/
    final static private Logger log = Logger.getLogger(OrchestrationAspect.class.getName());
    
    /**
     * Simple Constructor
     */
    public OrchestrationAspect() {
    }
    
    /**
     * Aspects all take strings in their constructors, but this is ignored.
     * @param ignored nominally the Aspect type. Ignored
     */
    public OrchestrationAspect(String ignored) { this(); }
    
    /**
     * Return the type of aspect this class processes.
     * @return the type of aspect this class processes.
     */
    public String getType() { return TYPE; }
    
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

    try {
    	byte[] inputData = inputAspect.getData();
		File aalTempFile = File.createTempFile("tmp", ".aal");
		Files.write(Paths.get(aalTempFile.getAbsolutePath()), inputData);
    
		String command = String.format("/usr/local/bin/magi_orchestrator.py "
				+ "-f %s -j", aalTempFile.getAbsolutePath());
		
		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
		int exitStatus = p.exitValue();
    
		if (exitStatus != 0){
			throw new DeterFault(DeterFault.request, "Invalid format");
		}
    }catch (IOException | InterruptedException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Cannot parse AAL!?:" + e.getMessage());
	}
    
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

    	for (ExperimentAspect ea : asps) {
    		// Remove all matching aspects
    		rv.add(ea);
    	}
    	
    	return rv;
        }

    /**
     * Realize this aspect.  The plugin can make changes to the input
     * realization description that represents the realized experiment, for
     * example to add elements that are necessary for the aspect.  If the
     * realization is unacceptable, the plugin can throw a DeterFault to
     * preempt it.  If the aspect changes the realization, it should return
     * the modified or a new topology description. and additional calls to
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
    public TopologyDescription realizeAspect(ImmutableExperimentDB exp, long transactionID,
	    ExperimentAspect realizeAspect, TopologyDescription realTop)
	throws DeterFault {
    
    String eid = exp.getEid();
    log.debug("Realizing orchestration aspect for experiment: " + eid);

    final Semaphore expOrchLock;
    if (orchestrationLocks.containsKey(eid)){
    	expOrchLock = orchestrationLocks.get(eid);
    } else{
    	expOrchLock = new Semaphore(1);
    	orchestrationLocks.put(eid, expOrchLock);
    }
    
    Boolean lockAcquireFlag = false;
    try{
    	lockAcquireFlag = 
    			expOrchLock.tryAcquire(0, TimeUnit.NANOSECONDS);
    }catch(InterruptedException interruptedException){
    	lockAcquireFlag = false;
    }
    
    if (lockAcquireFlag) {
    	boolean isProcessRunning = false;
    	try{
        	List<ExperimentAspect> rlist = new ArrayList<ExperimentAspect>();
        	List<ExperimentAspect> asps = null;
        	
        	ExperimentAspect req = new ExperimentAspect();
        	req.setName(realizeAspect.getName());
        	req.setType(getType());
        	req.setSubType(realizeAspect.getSubType());
        	rlist.add(req);

        	asps = exp.getAspects(rlist, true);

        	if (asps.size() == 0)
        	    throw new DeterFault(DeterFault.request, "No such aspect");
        	
        	if (asps.size() > 1)
        	    throw new DeterFault(DeterFault.request, 
        	    		"Multiple aspects with same name");
        	
        	// Extracting project and experiment name from eid
        	String project = exp.getEid().split(":")[0];
    		String experiment = exp.getEid().replace(':', '-');
    		
        	// Copying orchestration procedure to Deter Ops
        	byte[] inputData = asps.get(0).getData();
    		File aalTempFile = File.createTempFile("tmp", ".aal");
    		Files.write(Paths.get(aalTempFile.getAbsolutePath()), inputData);
    		
    		String command;
    		ProcessBuilder pb;
    		
    		command = String.format(
    				"/usr/local/bin/magi_orchestrator.py "
    				+ "-p %s -e %s -f %s -o /tmp/%s_%s_orch.log", 
    				project, experiment, aalTempFile, project, experiment);
    		
    		log.debug("Running orchestration cmd: " + command);
    		
    		pb = new ProcessBuilder(command.split(" "));
    		File log = new File(String.format(
    				"/tmp/%s_%s_orch.log", project, experiment));
    		pb.redirectErrorStream(true);
    		pb.redirectOutput(Redirect.appendTo(log));
    		final Process pr = pb.start();
    		
    		// Wait for a second to check  for any exceptions
    		if (waitFor(pr, 1, TimeUnit.SECONDS) && (pr.exitValue() != 0)){
    			throw new DeterFault(DeterFault.internal, 
    					"Could not orchestrate experiment");
    		}
    		
    		isProcessRunning = true;
    				
    		Thread t1 = new Thread(new Runnable() {
    		     public void run() {
    		    	 try {
						pr.waitFor();
					} catch (InterruptedException e) {
						// Can't do anything useful with the exception
					} finally {
			        	expOrchLock.release();
			        }
    		     } 
    		});  
    		t1.start();
    		
    		return null;
    		
        } catch (IOException ie) {
    	    throw new DeterFault(DeterFault.internal,
    		    "Could not orchestrate experiment. "
    	    		+"IO error!?: " + ie.getMessage());
    	} catch (InterruptedException ie) {
    		throw new DeterFault(DeterFault.internal,
    			"Command execution interuptted error!?: " + ie.getMessage());
	    } finally {
	    	if (!isProcessRunning)
	    		expOrchLock.release();
	    }
    } else {
        // Already being orchestrated
        log.debug("Another instanace of orchestrator "
        		+ "already running for the experiment");
    	return null;
    }
    }
    
    Map<String, Semaphore> orchestrationLocks = 
    		new HashMap<String, Semaphore>();
    
    // This has been copied from Java 8 java.lang.Process.waitFor()
    // to be able to have a similar function when working with 
    // earlier version of Java.
    public boolean waitFor(Process p, long timeout, TimeUnit unit)
            throws InterruptedException
    {
        long startTime = System.nanoTime();
        long rem = unit.toNanos(timeout);

        do {
            try {
                p.exitValue();
                return true;
            } catch(IllegalThreadStateException ex) {
                if (rem > 0)
                    Thread.sleep(
                        Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
            }
            rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
        } while (rem > 0);
        return false;
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
