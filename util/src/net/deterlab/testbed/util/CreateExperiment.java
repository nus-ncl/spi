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

import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;

import net.deterlab.testbed.util.gui.EditProfileDialog;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ListOption;
import net.deterlab.testbed.util.option.ParamOption;

/**
 * Create a new experiment.  Pop up a dialog that displays an empty profile
 * and allow editing of fields that can be edited.  When OK is pressed,
 * submit the request.  Aspects, the owner, and access rights are passed in on
 * the command line.
 * @author DETER Team
 * @version 1.0
 */
public class CreateExperiment extends Utility {

    /** a circle specification from the command line: circlename(PERMS).  The
     * circle name is further parsed by validCircle below */
    static private Pattern splitCircle = Pattern.compile("(.*)\\((.*)\\)");

    /** valid cirle names are owner:name */
    static private Pattern validCircle = Pattern.compile("^[^:]+:[^:]+$");

    /**
     * This Comparator sorts ExperimentsStub.Attributes by OrderingHint
     * @author DETER team
     * @version 1.0
     */
    static public class AttributeComparator implements 
	Comparator<ExperimentsStub.Attribute> {
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
	    public int compare(ExperimentsStub.Attribute a, 
		    ExperimentsStub.Attribute b) {
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
     * @return the constructed ExperimentsStub.AccessMember
     * @throws OptionException on errors
     */
    static ExperimentsStub.AccessMember parseCircle(String s)
	    throws Option.OptionException {
	Matcher m = splitCircle.matcher(s);
	ExperimentsStub.AccessMember rv = new ExperimentsStub.AccessMember();

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
     * Parse an aspect specification string into an ExperimentAspect. The
     * parsing is primarily splitting on commas.  Make the best choice about
     * the data source: namely see if it is a pathname or file:// URL and read
     * that data, otherwise pass it as a URL.  An unreadable file URL (or
     * pathname) triggers an exception.  Throw an OptionException if the string
     * is confusing or an IOException if there are problems reading.
     * @param s the specification string
     * @return the constructed ExperimentsSub.ExperimentAspect
     * @throws IOException if a file is unreadable
     * @throws OptionException if the specification is badly formed.
     */
    static private ExperimentsStub.ExperimentAspect parseAspect(String s)
	    throws Option.OptionException, IOException {
	String[] parts = s.split(",");
	String type = null;
	String subtype = null;
	String name = null;
	String path = null;
	URL u = null;
	String p = null;
	ExperimentsStub.ExperimentAspect rv =
	    new ExperimentsStub.ExperimentAspect();

	if ( parts.length < 3 || parts.length > 4)
	    throw new Option.OptionException("Bad aspect: " + s);
	else if (parts.length == 3 ) {
	    rv.setType(parts[0]);
	    rv.setName(parts[1]);
	    path = parts[2];
	}
	else {
	    rv.setType(parts[0]);
	    rv.setSubType(parts[1]);
	    rv.setName(parts[2]);
	    path = parts[3];
	}

	// If path is a well formed file URL, extract the path from the URL.
	// Otherwise path will be unchanged and u will be null.
	try {
	    u = new URL(path);
	    p = u.getProtocol();

	    if ( p != null && p.equals("file") && u.getPath() != null)
		path = u.getPath();
	}
	catch (MalformedURLException ignored) { }

	// Try to read the pathname and fall back to interpreting it as a URL
	// if that is a valid plan.  Otherwise throw an error.
	File f = new File(path);

	if (f.isFile())
	    rv.setData(putBytes(readFile(f)));
	else if (u != null && p != null && !p.equals("file"))
	    rv.setDataReference(u.toExternalForm());
	else
	    throw new Option.OptionException("Can't find data for '" + s +"'");

	return rv;
    }

    static void profilePopup(String eid, ExperimentsStub.Attribute[] profile) {
	// Edit profile array
	boolean allThere = false;

	// Keep asking the user for profile information until all required
	// fields are there.
	while (!allThere) {
	    StringBuilder missing = new StringBuilder();
	    EditProfileDialog d = new EditProfileDialog(eid, profile);

	    d.setVisible(true);
	    if (d.isCancelled()) {
		d.dispose();
		return;
	    }

	    allThere = true;
	    for (ExperimentsStub.Attribute a: profile) {
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
	    d.dispose();
	}
    }

    /**
     * Print a usage message and exit; is a message is given print that as a
     * warning first.
     * @param s a warning message (may be null)
     */
    static void usage(String s) {
	if ( s != null) warn(s);
	fatal("CreateExperiment [--aspect type,[subtype,]name,URL ...] "+
		"[--circle circleid(perms) ... ]\n\teid owner");
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
	    ListOption aspectParam = new ListOption("aspect");
	    ListOption attrParams = new ListOption("attr");
	    List<String> argv = new ArrayList<String>();
	    List<ExperimentsStub.AccessMember> acl =
		new ArrayList<ExperimentsStub.AccessMember>();
	    List<ExperimentsStub.ExperimentAspect> aspects =
		new ArrayList<ExperimentsStub.ExperimentAspect>();

	    Option[] options = new Option[] {
		aspectParam, circles, attrParams };
	    Option.parseArgs(args, options, argv);

	    if ( argv.size()!= 2) usage(null);

	    String eid = argv.get(0);
	    String owner = argv.get(1);


	    // The calls to remote procedures all take this form.
	    // * Create a request object
	    // * Fill in parameters to the request object
	    // * Initialize a response object with the result of the call
	    // * Extract and work with the data.
	    //
	    // This is the GetProfileDescription call
	    ExperimentsStub stub =
		new ExperimentsStub(getServiceUrl("Experiments"));
	     ExperimentsStub.GetProfileDescription descReq =
		new ExperimentsStub.GetProfileDescription();
	    ExperimentsStub.GetProfileDescriptionResponse descResp = 
		stub.getProfileDescription(descReq);
	    ExperimentsStub.Profile up = descResp.get_return();
	    ExperimentsStub.Attribute[] profile = up.getAttributes();
	    Arrays.sort(profile, new AttributeComparator());

	    // For a new experiment, required read-only fields need to be
	    // filled in.
	    for (ExperimentsStub.Attribute a: profile) 
		if (!a.getOptional() && 
			a.getAccess()!= Attribute.READ_WRITE)
		    a.setAccess(Attribute.READ_WRITE);

	    Collection<String> attrList = attrParams.getValue();

	    if ( attrList.size() > 0 ) {
		// Profile is on the command line.  Fill in the fields and hope
		// for the best.  If not, popup a menu and get them.
		for (String attr : attrParams.getValue()) {
		    String[] s = attr.split("=", 2);
		    if (s.length != 2 || s[0] == null) continue;

		    for (ExperimentsStub.Attribute aa : profile)
			if (s[0].equals(aa.getName()) ) {
			    aa.setValue(s[1]);
			    break;
			}
		}
	    }
	    else profilePopup(eid, profile);

	    ExperimentsStub.CreateExperiment createReq = 
		new ExperimentsStub.CreateExperiment();

	    createReq.setEid(eid);
	    createReq.setOwner(owner);

	    for ( String s : aspectParam.getValue()) 
		aspects.add(parseAspect(s));
	    createReq.setAspects(aspects.toArray(
			new ExperimentsStub.ExperimentAspect[0]));

	    for ( String s : circles.getValue()) {
		ExperimentsStub.AccessMember a = parseCircle(s);
		if ( a == null )
		    throw new Option.OptionException("Bad circle: " +s);
		acl.add(a);
	    }

	    createReq.setAccessLists(
		    acl.toArray(new ExperimentsStub.AccessMember[0]));
	    createReq.setProfile(profile);

	    ExperimentsStub.CreateExperimentResponse createResp = 
		stub.createExperiment(createReq);
	    System.out.println("CreateExperiment returned " + 
		    createResp.get_return());

	}
	catch (Option.OptionException e) {
	    usage("Option parsing exception: " + e);
	}
	catch (IOException e) {
	    fatal("Error reading file: " + e);
	}
	catch (ExperimentsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
