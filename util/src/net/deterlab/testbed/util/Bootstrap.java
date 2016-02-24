package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.AdminDeterFault;
import net.deterlab.testbed.client.AdminStub;

/**
 * Remove a circle and its profile.
 * @author DETER Team
 * @version 1.0
 */
public class Bootstrap extends Utility {

    static public void usage() {
	fatal("Usage: Bootstrap");
    }

    /**
     * Call bootstrap and print the bootstrapped user
     * @param args Ignored
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();

	    AdminStub stub = new AdminStub(getServiceUrl("Admin"));
	    AdminStub.Bootstrap req = new AdminStub.Bootstrap();

	    AdminStub.BootstrapResponse resp = stub.bootstrap(req);
	    AdminStub.BootstrapUser bu = resp.get_return();
	    System.err.println(bu.getUid() + " " + bu.getPassword());

	} catch (AdminDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
