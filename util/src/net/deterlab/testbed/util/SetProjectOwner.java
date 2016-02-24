package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ProjectsDeterFault;
import net.deterlab.testbed.client.ProjectsStub;

import org.apache.axis2.AxisFault;

/**
 * Change a project's owner
 * @author DETER Team
 * @version 1.0
 */
public class SetProjectOwner extends Utility {

    static public void usage() {
	fatal("Usage: SetProjectOwner projectid user ");
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

	    String projectid = args[0];
	    String owner = args[1];

	    ProjectsStub stub = new ProjectsStub(getServiceUrl("Projects"));
	    ProjectsStub.SetOwner req = 
		new ProjectsStub.SetOwner();

	    req.setProjectid(projectid);
	    req.setUid(owner);

	    stub.setOwner(req);
	    System.out.println("Owner changed");

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
