package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;

import org.apache.axis2.AxisFault;

/**
 * Change an experiment's owner
 * @author DETER Team
 * @version 1.0
 */
public class SetExperimentOwner extends Utility {

    static public void usage() {
	fatal("Usage: SetExperimentOwner eid user ");
    }

    /**
     * Set the owner.  Params are the experiment id and new owner
     * @param args the the experiment id and new owner
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();


	    if ( args.length < 2) 
		usage();

	    String eid = args[0];
	    String owner = args[1];

	    ExperimentsStub stub =
		new ExperimentsStub(getServiceUrl("Experiments"));
	    ExperimentsStub.SetOwner req = new ExperimentsStub.SetOwner();

	    req.setEid(eid);
	    req.setUid(owner);

	    stub.setOwner(req);
	    System.out.println("Owner changed");

	} catch (ExperimentsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
