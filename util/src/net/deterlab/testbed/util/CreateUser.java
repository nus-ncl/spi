package net.deterlab.testbed.util;

import java.io.File;
import java.io.IOException;

import java.security.GeneralSecurityException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.activation.DataHandler;

import javax.swing.JOptionPane;

import net.deterlab.abac.ABACException;
import net.deterlab.abac.Identity;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersDeterFault;
import net.deterlab.testbed.client.UsersStub;

import net.deterlab.testbed.util.gui.EditProfileDialog;
import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ParamOption;

/**
 * Create a new user profile.  Pop up a dialog that displays an empty profile
 * and * allow editing of fields that can be edited.  When OK is pressed,
 * submit the request.
 * @author DETER Team
 * @version 1.0
 */
public class CreateUser extends Utility {

    /**
     * This Comparator sorts UsersStub.Attributes by OrderingHint
     * @author DETER team
     * @version 1.0
     */
    static public class AttributeComparator implements 
	Comparator<UsersStub.Attribute> {
	    /**
	     * Constructor.
	     */
	    public AttributeComparator() { }
	    /**
	     * Compares its two arguments for order.
	     * @param a Attribute to compare 
	     * @param b Attribute to compare 
	     * @return a negative integer, zero, or a positive integer as the
	     * first argument is less than, equal to, or greater than the
	     * second. 
	     */
	    public int compare(UsersStub.Attribute a, 
		    UsersStub.Attribute b) {
		return a.getOrderingHint() - b.getOrderingHint();
	    }
	    /**
	     * Indicates whether some other object is "equal to" this
	     * comparator. 
	     * @param o the object to test
	     * @return true if o refers to this
	     */
	    public boolean equals(Object o) {
		if ( o == null ) return false;
		if ( !(o instanceof AttributeComparator ) ) return false;
		AttributeComparator a = (AttributeComparator) o;
		return (this == o);
	    }
	}
    /**
     * Exit with a usage message
     */
    static public void usage() {
	fatal("Usage: CreateUser [--pem file] [--keystore store] " +
		"[--keypass pass] [user]");
    }

    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then pop up a dialog and allow editing.  Make sure that the
     * required fields are initialized and send the request
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	Identity i = null;
	List<String> users = new ArrayList<String>();
	ParamOption pem = new ParamOption("pem");
	ParamOption store = new ParamOption("keystore", getUserIDFilename());
	ParamOption pass = new ParamOption("keypass", getUserIDPassword());

	try {
	    Option.parseArgs(args, new Option[] { pem, store, pass }, users);
	}
	catch (Option.OptionException e) {
	    usage();
	}

	try {

	    // Set trusted certificates.
	    loadTrust();
	    if (users.size() > 1) usage();

	    String uid = (users.size() > 0) ? users.get(0) : null;
	    String pemfile = pem.getValue();
	    String keystore = store.getValue();
	    String keypass = pass.getValue();

	    // The calls to remote procedures all take this form.
	    // * Create a request object
	    // * Fill in parameters to the request object
	    // * Initialize a response object with the result of the call
	    // * Extract and work with the data.
	    //
	    // This is the GetProfileDescription call
	    UsersStub stub = new UsersStub(getServiceUrl("Users"));
	    UsersStub.GetProfileDescription descReq = 
		new UsersStub.GetProfileDescription();
	    UsersStub.GetProfileDescriptionResponse descResp = 
		stub.getProfileDescription(descReq);
	    UsersStub.Profile up = descResp.get_return();
	    UsersStub.Attribute[] profile = up.getAttributes();
	    Arrays.sort(profile, new AttributeComparator());

	    // Edit profile array
	    EditProfileDialog d = new EditProfileDialog(uid, profile);
	    boolean allThere = false;

	    // For a new user, required read-only fields need to be filled in.
	    for (UsersStub.Attribute a: profile) 
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
		for (UsersStub.Attribute a: profile) {
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
	    UsersStub.CreateUser createReq = 
		new UsersStub.CreateUser();
	    createReq.setUid(uid);
	    createReq.setProfile(profile);
	    createReq.setUrlPrefix("challenge=");

	    UsersStub.CreateUserResponse createResp = 
		stub.createUser(createReq);
	    UsersStub.CreateUserResult result = createResp.get_return();

	    System.out.println("Created user " + result.getUid());
	    // If an identity came back, store it.
	    try {
		DataHandler dh = result.getIdentity();
		if ( dh != null ) {
		    i = loadIdentity(getBytes(dh));
		}
		if ( i == null)
		    throw new ABACException("Cannot decode identity");

		/* Store to pem if we have a pem filename */
		if ( pemfile != null )
		    identityToPem(i, new File(pemfile));

		/* Store to keystore if we have a keystore filename */
		if (keystore != null ) {
		    char[] p = (keypass != null) ? keypass.toCharArray() : null;
		    identityToKeyStore(i, new File(keystore), p);
		}
	    }
	    catch (IOException e) {
		fatal("File system error: " +e.getMessage());
	    }
	    catch (ABACException e ) {
		fatal("Identity error: " +e.getMessage());
	    }
	    catch (GeneralSecurityException e) {
		fatal("Keystore error: " +e.getMessage());
	    }

	} catch (UsersDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
