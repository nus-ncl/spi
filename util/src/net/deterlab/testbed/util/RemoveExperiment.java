package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;

/**
 * Remove an experiment and its profile.
 * @author DETER Team
 * @version 1.0
 */
public class RemoveExperiment extends Utility {

    static public void usage() {
	fatal("Usage: RemoveExperiment experimentname");
    }

    /**
     * Remove the experiment.  The only parameter is the experiment name to
     * delete.
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();

	    String name = (args.length > 0 ) ? args[0] : null;

	    if ( name == null ) 
		usage();

	    ExperimentsStub stub =
		new ExperimentsStub(getServiceUrl("Experiments"));
	    ExperimentsStub.RemoveExperiment req =
		new ExperimentsStub.RemoveExperiment();

	    req.setEid(name);
	    stub.removeExperiment(req);

	} catch (ExperimentsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
