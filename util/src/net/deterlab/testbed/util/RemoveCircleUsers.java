package net.deterlab.testbed.util;

import java.util.Arrays;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.CirclesStub;

import org.apache.axis2.AxisFault;

/**
 * Add the users to the circle with the given permissions.  Print the results.
 * @author DETER Team
 * @version 1.0
 */
public class RemoveCircleUsers extends Utility {

    static public void usage() {
	fatal("Usage: ReomveCircleUsers circle user ... ");
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

	    // Pass the parame to addUsersNoConfirm directly and print results
	    CirclesStub stub = new CirclesStub(getServiceUrl("Circles"));
	    CirclesStub.RemoveUsers req = 
		new CirclesStub.RemoveUsers();

	    req.setCircleid(args[0]);
	    req.setUids(Arrays.copyOfRange(args, 1, args.length));

	    CirclesStub.RemoveUsersResponse resp = 
		stub.removeUsers(req);

	    for (CirclesStub.ChangeResult r : resp.get_return())
		if ( r.getSuccess()) 
		    System.out.println(r.getName() + " removed");
		else
		    System.out.println(r.getName() + " failed: " + 
			    r.getReason());


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
