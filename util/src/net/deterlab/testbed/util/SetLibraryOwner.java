package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.LibrariesStub;

import org.apache.axis2.AxisFault;

/**
 * Change an experiment's owner
 * @author DETER Team
 * @version 1.0
 */
public class SetLibraryOwner extends Utility {

    static public void usage() {
	fatal("Usage: SetLibraryOwner eid user ");
    }

    /**
     * Set the owner.  Params are the library id and new owner
     * @param args the the library id and new owner
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();


	    if ( args.length < 2) 
		usage();

	    String libid = args[0];
	    String owner = args[1];

	    LibrariesStub stub =
		new LibrariesStub(getServiceUrl("Libraries"));
	    LibrariesStub.SetOwner req = new LibrariesStub.SetOwner();

	    req.setLibid(libid);
	    req.setUid(owner);

	    stub.setOwner(req);
	    System.out.println("Owner changed");

	} catch (LibrariesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
