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

import net.deterlab.testbed.util.gui.EditProfileDialog;

/**
 * Create a new project and profile.  Pop up a dialog that displays an empty
 * profile and * allow editing of fields that can be edited.  When OK is
 * pressed, submit the request.
 * @author DETER Team
 * @version 1.0
 */
public class CreateProject extends Utility {

    static public void usage() {
	fatal("Usage: CreateProject uid projectname");
    }

    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then pop up a dialog and allow editing.  Make sure that the
     * required fields are initialized and send the request
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();


	    String uid = (args.length > 0 ) ? args[0] : null;
	    String name = (args.length > 1 ) ? args[1] : null;

	    if ( name == null || uid == null ) 
		usage();

	    // The calls to remote procedures all take this form.
	    // * Create a request object
	    // * Fill in parameters to the request object
	    // * Initialize a response object with the result of the call
	    // * Extract and work with the data.
	    //
	    // This is the GetProfileDescription call
	    ProjectsStub stub = new ProjectsStub(getServiceUrl("Projects"));
	    ProjectsStub.GetProfileDescription descReq = 
		new ProjectsStub.GetProfileDescription();
	    ProjectsStub.GetProfileDescriptionResponse descResp = 
		stub.getProfileDescription(descReq);
	    ProjectsStub.Profile up = descResp.get_return();
	    ProjectsStub.Attribute[] profile = up.getAttributes();

	    // Edit profile array
	    EditProfileDialog d = new EditProfileDialog(name, profile);
	    boolean allThere = false;

	    // For a new user, required read-only fields need to be filled in.
	    for (ProjectsStub.Attribute a: profile) 
		if (!a.getOptional() && 
			a.getAccess()!= Attribute.READ_WRITE)
		    a.setAccess(Attribute.READ_WRITE);

	    // Keep asking the user for profile information until all required
	    // fields are there.
	    while (!allThere) {
		StringBuilder missing = new StringBuilder();

		d.setVisible(true);
		if (d.isCancelled()) {
		    d.dispose();
		    return;
		}

		allThere = true;
		for (ProjectsStub.Attribute a: profile) {
		    if (a.getOptional()) continue;
		    if (a.getValue() == null || a.getValue().length() == 0) {
			if ( missing.length() > 0 )
			    missing.append(", ");
			missing.append(a.getName());
			allThere = false;
		    }
		}
		if (!allThere)
		    JOptionPane.showMessageDialog(null, 
			    "Required fields " + missing.toString() + 
			    " missing");
	    }
	    d.dispose();
	    ProjectsStub.CreateProject createReq = 
		new ProjectsStub.CreateProject();
	    createReq.setProjectid(name);
	    createReq.setOwner(uid);
	    createReq.setProfile(profile);

	    ProjectsStub.CreateProjectResponse createResp = 
		stub.createProject(createReq);

	} catch (ProjectsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
