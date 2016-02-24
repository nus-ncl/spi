package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.LibrariesStub;

/**
 * Remove a library and its profile.
 * @author DETER Team
 * @version 1.0
 */
public class RemoveLibrary extends Utility {

    static public void usage() {
	fatal("Usage: RemoveLibrary libraryname");
    }

    /**
     * Remove a library,
     * @param args the library to remove is the first and only parameter.
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();

	    String name = (args.length > 0 ) ? args[0] : null;

	    if ( name == null ) 
		usage();

	    LibrariesStub stub =
		new LibrariesStub(getServiceUrl("Libraries"));
	    LibrariesStub.RemoveLibrary req =
		new LibrariesStub.RemoveLibrary();

	    req.setLibid(name);
	    stub.removeLibrary(req);

	} catch (LibrariesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
