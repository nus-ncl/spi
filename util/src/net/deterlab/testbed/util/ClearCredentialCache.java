package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.AdminDeterFault;
import net.deterlab.testbed.client.AdminStub;

/**
 * Remove a circle and its profile.
 * @author DETER Team
 * @version 1.0
 */
public class ClearCredentialCache extends Utility {

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
	    AdminStub.ClearCredentialCache req =
		new AdminStub.ClearCredentialCache();

	    stub.clearCredentialCache(req);

	} catch (AdminDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
