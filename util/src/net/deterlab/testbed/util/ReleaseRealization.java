package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.RealizationsDeterFault;
import net.deterlab.testbed.client.RealizationsStub;

/**
 * Release a realization and deallocate its resources.
 * @author DETER Team
 * @version 1.0
 */
public class ReleaseRealization extends Utility {

    static public void usage() {
	fatal("Usage: ReleaseRealization realizationname");
    }

    /**
     * Call releaseRealization.
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

	    RealizationsStub stub = new RealizationsStub(
		    getServiceUrl("Realizations"));
	    RealizationsStub.ReleaseRealization req =
		new RealizationsStub.ReleaseRealization();

	    req.setName(name);
	    RealizationsStub.ReleaseRealizationResponse releaseResp = 
		stub.releaseRealization(req);
	    dumpRealizationDescription(releaseResp.get_return(), System.out);

	} catch (RealizationsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
