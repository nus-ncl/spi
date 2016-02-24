package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.CirclesStub;

import org.apache.axis2.AxisFault;

/**
 * Change a circle's owner
 * @author DETER Team
 * @version 1.0
 */
public class SetCircleOwner extends Utility {

    static public void usage() {
	fatal("Usage: SetCircleOwner circleid user ");
    }

    /**
     * Collect the users, call add and display results.
     * @param args the circleid permissions and users
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();


	    if ( args.length < 2) 
		usage();

	    String circleid = args[0];
	    String owner = args[1];

	    CirclesStub stub = new CirclesStub(getServiceUrl("Circles"));
	    CirclesStub.SetOwner req = 
		new CirclesStub.SetOwner();

	    req.setCircleid(circleid);
	    req.setUid(owner);

	    stub.setOwner(req);
	    System.out.println("Owner changed");

	} catch (CirclesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
