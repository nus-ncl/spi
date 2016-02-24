package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

/**
 * Log the user out of to the DETER API.  Such a login results in an X.509
 * certificate being stored in the default user ID file that other utilities
 * can use.  Main calls logout with that certificate.
 * @author DETER team
 * @version 1.0
 */
public class Logout extends Utility {
    /**
     * Exit with a usage message
     */
    static public void usage() {
	fatal("Usage: Logout ");
    }

    /**
     * Login as the userid in the first argument.
     * @param args the command line arguments
     */
    static public void main(String[] args) {
	// Load certs
	loadTrust();
	loadID();
	// Carry logout
	try {
	    logout();
	} catch (DeterFault df) {
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	    fatal("unexpected exception");
	}

    }
}
