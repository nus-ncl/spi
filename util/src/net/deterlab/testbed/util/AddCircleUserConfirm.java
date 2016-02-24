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
public class AddCircleUserConfirm extends Utility {

    static public void usage() {
	fatal("USage: AddCircleUserConfirm ID");
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


	    if ( args.length < 1) 
		usage();

	    // Pass the parame to addUsersNoConfirm directly and print results
	    CirclesStub stub = new CirclesStub(getServiceUrl("Circles"));
	    CirclesStub.AddUserConfirm req = new CirclesStub.AddUserConfirm();

	    req.setChallengeId(Long.parseLong(args[0]));
	    CirclesStub.AddUserConfirmResponse resp = stub.addUserConfirm(req);
	    System.out.println("Confirmed");

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
