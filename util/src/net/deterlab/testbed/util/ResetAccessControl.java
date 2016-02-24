package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.AdminDeterFault;
import net.deterlab.testbed.client.AdminStub;

/**
 * Remove a circle and its profile.
 * @author DETER Team
 * @version 1.0
 */
public class ResetAccessControl extends Utility {

    static public void usage() {
	fatal("Usage: ResetAccessControl");
    }

    /**
     * Call bootstrap and print the bootstrapped user
     * @param args Ignored
     */
    static public void main(String[] args) {
	try {


	    // Set trusted certificates.
	    loadTrust();
	    loadID();

	    AdminStub stub = new AdminStub(getServiceUrl("Admin"));
	    // This takes a while: set a long timeout
	    stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(
		    5*60*1000);
	    AdminStub.ResetAccessControl req =
		new AdminStub.ResetAccessControl();

	    stub.resetAccessControl(req);

	} catch (AdminDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
