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
 * Add aspects to an existing experiment.
 * @author DETER Team
 * @version 1.0
 */
public class AddExperimentAspects extends Utility {

    /** a circle specification from the command line: circlename(PERMS).  The
     * circle name is further parsed by validCircle below */
    static private Pattern splitCircle = Pattern.compile("(.*)\\((.*)\\)");

    /** valid cirle names are owner:name */
    static private Pattern validCircle = Pattern.compile("^[^:]+:[^:]+$");

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

    /**
     * Print a usage message and exit; is a message is given print that as a
     * warning first.
     * @param s a warning message (may be null)
     */
    static void usage(String s) {
	if ( s != null) warn(s);
	fatal("AddExperimentAspect [--aspect type,[subtype,]name,URL ...] " +
		"eid");
    }

    /**
     * Parse the aspect arguments and call AddExperimentAspects.  Display
     * results.
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	try {

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();

	    ListOption aspectParam = new ListOption("aspect");
	    List<String> argv = new ArrayList<String>();
	    List<ExperimentsStub.ExperimentAspect> aspects =
		new ArrayList<ExperimentsStub.ExperimentAspect>();

	    Option[] options = new Option[] { aspectParam };
	    Option.parseArgs(args, options, argv);

	    if ( argv.size()!= 1) usage(null);

	    String eid = argv.get(0);


	    // This is the GetProfileDescription call
	    ExperimentsStub stub =
		new ExperimentsStub(getServiceUrl("Experiments"));
	    ExperimentsStub.AddExperimentAspects addReq = 
		new ExperimentsStub.AddExperimentAspects();

	    addReq.setEid(eid);

	    for ( String s : aspectParam.getValue()) 
		aspects.add(parseAspect(s));
	    addReq.setAspects(aspects.toArray(
			new ExperimentsStub.ExperimentAspect[0]));

	    ExperimentsStub.AddExperimentAspectsResponse addAspectsResp = 
		stub.addExperimentAspects(addReq);
	    ExperimentsStub.ChangeResult[] resp =
		    addAspectsResp.get_return();

	    if ( resp == null ) {
		System.err.println("Nothing added");
		System.exit(20);
	    }

	    for (ExperimentsStub.ChangeResult ra : resp) {
		System.out.print(ra.getName());
		if ( ra.getSuccess()) System.out.println(" succeeded");
		else System.out.println(" failed: " + ra.getReason());
	    }


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
