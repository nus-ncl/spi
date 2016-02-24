package net.deterlab.testbed.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ProjectsDeterFault;
import net.deterlab.testbed.client.ProjectsStub;

import net.deterlab.testbed.util.option.BooleanOption;
import net.deterlab.testbed.util.option.Option;

/**
 * Remove a project and its profile.
 * @author DETER Team
 * @version 1.0
 */
public class ApproveProject extends Utility {

    static public void usage() {
	fatal("Usage: RemoveProject projectname [--no-approve|--approve]");
    }

    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then pop up a dialog and allow editing.  Make sure that the
     * required fields are initialized and send the request
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	BooleanOption approve = new BooleanOption("approve", true);
	List<String> argv = new ArrayList<String>();
	try {


	    Option.parseArgs(args, new Option[] { approve }, argv);
	    String name = (argv.size() > 0 ) ? argv.get(0) : null;
	    boolean app = approve.getValue();

	    // Set trusted certificates.
	    loadTrust();
	    loadID();


	    if ( name == null ) 
		usage();

	    ProjectsStub stub = new ProjectsStub(getServiceUrl("Projects"));
	    ProjectsStub.ApproveProject req = new ProjectsStub.ApproveProject();

	    req.setProjectid(name);
	    req.setApproved(app);
	    stub.approveProject(req);
	} catch (Option.OptionException e) {
	    fatal(e.getMessage());
	} catch (ProjectsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
