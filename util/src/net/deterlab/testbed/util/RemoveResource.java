package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ResourcesDeterFault;
import net.deterlab.testbed.client.ResourcesStub;

/**
 * Remove a project and its profile.
 * @author DETER Team
 * @version 1.0
 */
public class RemoveResource extends Utility {

    static public void usage() {
	fatal("Usage: RemoveResource resourcename");
    }

    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then pop up a dialog and allow editing.  Make sure that the
     * required fields are initialized and send the request
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

	    ResourcesStub stub = new ResourcesStub(getServiceUrl("Resources"));
	    ResourcesStub.RemoveResource req =
		new ResourcesStub.RemoveResource();

	    req.setName(name);
	    stub.removeResource(req);

	} catch (ResourcesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
