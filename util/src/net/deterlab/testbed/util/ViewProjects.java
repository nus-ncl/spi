package net.deterlab.testbed.util;

import java.util.Arrays;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ProjectsDeterFault;
import net.deterlab.testbed.client.ProjectsStub;

import org.apache.axis2.AxisFault;

/**
 * View a list of projects
 * @author DETER Team
 * @version 1.0
 */
public class ViewProjects extends Utility {

    static public void usage() {
	fatal("USage: ViewProjects user [regex] ");
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


	    if ( args.length > 2)
		usage();

	    String uid = (args.length > 0 ) ? args[0] : null;
	    String regex = (args.length > 1) ? args[1] : null;

	    ProjectsStub stub = new ProjectsStub(getServiceUrl("Projects"));
	    ProjectsStub.ViewProjects req = 
		new ProjectsStub.ViewProjects();

	    if ( uid != null) req.setUid(uid);
	    if ( regex != null) req.setRegex(regex);

	    ProjectsStub.ViewProjectsResponse resp = 
		stub.viewProjects(req);

	    ProjectsStub.ProjectDescription[] projects= resp.get_return();

	    if (projects == null ) 
		fatal("No projects found for " + uid );

	    for (ProjectsStub.ProjectDescription c : projects) {
		System.out.println("Project: " + c.getProjectId() + 
			"  Owner: " + c.getOwner() + " approved: " + 
			c.getApproved());
		for (ProjectsStub.Member m : c.getMembers()) 
		    System.err.println("\t"+m.getUid() + " " + 
			    joinStrings(m.getPermissions(), ", "));
	    }

	} catch (ProjectsDeterFault e) {
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
