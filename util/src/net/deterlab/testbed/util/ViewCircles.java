package net.deterlab.testbed.util;

import java.util.Arrays;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.CirclesStub;

import org.apache.axis2.AxisFault;

/**
 * View a list of circles
 * @author DETER Team
 * @version 1.0
 */
public class ViewCircles extends Utility {

    static public void usage() {
	fatal("USage: ViewCircles [user] [regex] ");
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


	    if ( args.length > 2)
		usage();

	    String uid = (args.length > 0) ? args[0] : null;
	    String regex = (args.length > 1) ? args[1] : null;

	    CirclesStub stub = new CirclesStub(getServiceUrl("Circles"));
	    CirclesStub.ViewCircles req = 
		new CirclesStub.ViewCircles();

	    if (uid != null) req.setUid(uid);
	    if (regex != null) req.setRegex(regex);

	    CirclesStub.ViewCirclesResponse resp = 
		stub.viewCircles(req);

	    CirclesStub.CircleDescription[] circles= resp.get_return();

	    if (circles == null ) 
		fatal("No circles found for " + uid );

	    for (CirclesStub.CircleDescription c : circles) {
		System.out.println("Circle: " + c.getCircleId() + 
			"  Owner: " + c.getOwner());
		for (CirclesStub.Member m : c.getMembers()) 
		    System.err.println("\t"+m.getUid() + " " + 
			    joinStrings(m.getPermissions(), ", "));
	    }

	} catch (CirclesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (NumberFormatException e) {
	    fatal("permissions is not an integer");
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
