package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.LibrariesStub;

/**
 * Add experiments to an existing library.
 * @author DETER Team
 * @version 1.0
 */
public class AddLibraryExperiments extends Utility {

    /**
     * Print a usage message and exit; is a message is given print that as a
     * warning first.
     * @param s a warning message (may be null)
     */
    static void usage(String s) {
	if ( s != null) warn(s);
	fatal("AddLibraryExperiments libid eid [...]");
    }
	
    /**
     * Collect terms and call addLibraryExperiments.  Parameters are the
     * library id and one or more experiment ids
     * @param args the library id and one or more experiment ids
     */
    static public void main(String[] args) {
	try {

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();

	    if ( args.length <  2) usage(null);

	    String libid = args[0];
	    String[] eids = new String[args.length-1];

	    for ( int i = 1; i < args.length; i++)
		eids[i-1] = args[i];

	    LibrariesStub stub = new LibrariesStub(getServiceUrl("Libraries"));
	    LibrariesStub.AddLibraryExperiments req = 
		new LibrariesStub.AddLibraryExperiments();

	    req.setLibid(libid);
	    req.setEids(eids);

	    LibrariesStub.AddLibraryExperimentsResponse resp = 
		stub.addLibraryExperiments(req);
	    LibrariesStub.ChangeResult[] results = resp.get_return();

	    for (LibrariesStub.ChangeResult r : results) {
		System.out.print(r.getName());
		if ( r.getSuccess()) System.out.println(" succeeded");
		else System.out.println(" failed: " + r.getReason());
	    }
	}
	catch (LibrariesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
