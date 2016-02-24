package net.deterlab.testbed.util;

import java.util.Arrays;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ProjectsDeterFault;
import net.deterlab.testbed.client.ProjectsStub;

import org.apache.axis2.AxisFault;

/**
 * Add the users to the project with the given permissions.  Print the results.
 * @author DETER Team
 * @version 1.0
 */
public class RemoveProjectUsers extends Utility {

    static public void usage() {
	fatal("Usage: ReomveProjectUsers project user ... ");
    }

    /**
     * Collect the users, call add and display results.
     * @param args the projectid permissions and users
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();


	    if ( args.length < 2) 
		usage();

	    // Pass the parame to addUsersNoConfirm directly and print results
	    ProjectsStub stub = new ProjectsStub(getServiceUrl("Projects"));
	    ProjectsStub.RemoveUsers req = 
		new ProjectsStub.RemoveUsers();

	    req.setProjectid(args[0]);
	    req.setUids(Arrays.copyOfRange(args, 1, args.length));

	    ProjectsStub.RemoveUsersResponse resp = 
		stub.removeUsers(req);

	    for (ProjectsStub.ChangeResult r : resp.get_return())
		if ( r.getSuccess()) 
		    System.out.println(r.getName() + " removed");
		else
		    System.out.println(r.getName() + " failed: " + 
			    r.getReason());


	} catch (ProjectsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
