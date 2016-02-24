package net.deterlab.testbed.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.JOptionPane;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.LibrariesStub;

import net.deterlab.testbed.util.gui.EditProfileDialog;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ListOption;
import net.deterlab.testbed.util.option.ParamOption;

/**
 * Create a new library.  Pop up a dialog that displays an empty profile
 * and allow editing of fields that can be edited.  When OK is pressed,
 * submit the request.  Experiments, the owner, and access rights are passed in
 * on the command line.
 * @author DETER Team
 * @version 1.0
 */
public class CreateLibrary extends Utility {

    /** a circle specification from the command line: circlename(PERMS).  The
     * circle name is further parsed by validCircle below */
    static private Pattern splitCircle = Pattern.compile("(.*)\\((.*)\\)");

    /** valid cirle names are owner:name */
    static private Pattern validCircle = Pattern.compile("^[^:]+:[^:]+$");

    /**
     * This Comparator sorts LibrariesStub.Attributes by OrderingHint
     * @author DETER team
     * @version 1.0
     */
    static public class AttributeComparator implements 
	Comparator<LibrariesStub.Attribute> {
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
	    public int compare(LibrariesStub.Attribute a, 
		    LibrariesStub.Attribute b) {
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
     * Parse a string of the form given by the splitCircle regexp into parts
     * and build an AccessMember object from it.  If there are problems, throw
     * an OptionException.
     * @param s the circle specification string
     * @return the constructed LibrariesStub.AccessMember
     * @throws OptionException on errors
     */
    static LibrariesStub.AccessMember parseCircle(String s)
	    throws Option.OptionException {
	Matcher m = splitCircle.matcher(s);
	LibrariesStub.AccessMember rv = new LibrariesStub.AccessMember();

	if (!m.matches()) throw new Option.OptionException("Bad circle " + s);
	String circleId = m.group(1);
	Matcher valid = validCircle.matcher(circleId);

	if ( !valid.matches())
	    throw new Option.OptionException("Bad circleId " + s);

	String[] perms = m.group(2).split("\\s*,\\s*");

	rv.setCircleId(circleId);
	rv.setPermissions(perms);
	return rv;
    }

    /**
     * Print a usage message and exit; is a message is given print that as a
     * warning first.
     * @param s a warning message (may be null)
     */
    static void usage(String s) {
	if ( s != null) warn(s);
	fatal("CreateLibrary [--circle circleid(perms) ... ]\n\t"+
		"libid owner [experiment ...]");
    }
	
    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then pop up a dialog and allow editing.  Make sure that the
     * required fields are initialized and send the request
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	try {

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();

	    ListOption circles = new ListOption("circle");
	    List<String> argv = new ArrayList<String>();
	    List<LibrariesStub.AccessMember> acl =
		new ArrayList<LibrariesStub.AccessMember>();
	    List<String> exps = new ArrayList<String>();

	    Option[] options = new Option[] { circles };
	    Option.parseArgs(args, options, argv);

	    if ( argv.size() <  2) usage(null);

	    String libid = argv.get(0);
	    String owner = argv.get(1);

	    for (int i = 2; i < argv.size(); i++)
		exps.add(argv.get(i));


	    // The calls to remote procedures all take this form.
	    // * Create a request object
	    // * Fill in parameters to the request object
	    // * Initialize a response object with the result of the call
	    // * Extract and work with the data.
	    //
	    // This is the GetProfileDescription call
	    LibrariesStub stub = new LibrariesStub(getServiceUrl("Libraries"));
	     LibrariesStub.GetProfileDescription descReq =
		new LibrariesStub.GetProfileDescription();
	    LibrariesStub.GetProfileDescriptionResponse descResp = 
		stub.getProfileDescription(descReq);
	    LibrariesStub.Profile up = descResp.get_return();
	    LibrariesStub.Attribute[] profile = up.getAttributes();

	    if ( profile == null )
		fatal("Empty profile!?");
	    Arrays.sort(profile, new AttributeComparator());

	    // Edit profile array
	    EditProfileDialog d = new EditProfileDialog(libid, profile);
	    boolean allThere = false;

	    // For a new experiment, required read-only fields need to be
	    // filled in.
	    for (LibrariesStub.Attribute a: profile)
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
		for (LibrariesStub.Attribute a: profile) {
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

	    LibrariesStub.CreateLibrary createReq = 
		new LibrariesStub.CreateLibrary();

	    createReq.setLibid(libid);
	    createReq.setOwner(owner);
	    createReq.setEids(exps.toArray(new String[0]));

	    for ( String s : circles.getValue()) {
		LibrariesStub.AccessMember a = parseCircle(s);
		if ( a == null )
		    throw new Option.OptionException("Bad circle: " +s);
		acl.add(a);
	    }

	    createReq.setAccessLists(
		    acl.toArray(new LibrariesStub.AccessMember[0]));
	    createReq.setProfile(profile);

	    LibrariesStub.CreateLibraryResponse createResp = 
		stub.createLibrary(createReq);
	    System.out.println("CreateLibrary returned " + 
		    createResp.get_return());

	}
	catch (Option.OptionException e) {
	    usage("Option parsing exception: " + e);
	}
	catch (IOException e) {
	    fatal("Error reading file: " + e);
	}
	catch (LibrariesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
