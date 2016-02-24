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
public class JoinCircle extends Utility {

    static public void usage() {
	fatal("Usage: JoinCircle circle user ");
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
	    CirclesStub.JoinCircle req = new CirclesStub.JoinCircle();

	    req.setCircleid(args[0]);
	    req.setUid(args[1]);
	    req.setUrlPrefix("http://www.isi.edu/~faber");

	    stub.joinCircle(req);
	    System.out.println("Join request sent");

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
