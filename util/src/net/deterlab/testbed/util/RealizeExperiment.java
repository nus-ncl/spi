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
 * Realize an existing experiment.
 * @author DETER Team
 * @version 1.0
 */
public class RealizeExperiment extends Utility {
    /** a circle specification from the command line: circlename(PERMS).  The
     * circle name is further parsed by validCircle below */
    static private Pattern splitCircle = Pattern.compile("(.*)\\((.*)\\)");

    /** valid cirle names are owner:name */
    static private Pattern validCircle = Pattern.compile("^[^:]+:[^:]+$");
    /**
     * Print a usage message and exit; is a message is given print that as a
     * warning first.
     * @param s a warning message (may be null)
     */
    static void usage(String s) {
	if ( s != null) warn(s);
	fatal("RealizeExperiment experiment circle user " +
		"[--circle circleid(perms) ... ]");
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
     * Realize the experiment and print the realization description.
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	try {

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();

	    List<String> argv = new ArrayList<String>();
	    ListOption circles = new ListOption("circle");

	    Option[] options = new Option[] { circles };
	    Option.parseArgs(args, options, argv);

	    if ( argv.size()!= 3) usage(null);

	    String eid = argv.get(0);
	    String cid = argv.get(1);
	    String uid = argv.get(2);


	    ExperimentsStub stub =
		new ExperimentsStub(getServiceUrl("Experiments"));
	    ExperimentsStub.RealizeExperiment realizeReq = 
		new ExperimentsStub.RealizeExperiment();
	    List<ExperimentsStub.AccessMember> acl =
		new ArrayList<ExperimentsStub.AccessMember>();

	    realizeReq.setEid(eid);
	    realizeReq.setCid(cid);
	    realizeReq.setUid(uid);
	    for ( String s : circles.getValue()) {
		ExperimentsStub.AccessMember a = parseCircle(s);
		if ( a == null )
		    throw new Option.OptionException("Bad circle: " +s);
		acl.add(a);
	    }
	    realizeReq.setAcl(acl.toArray(new ExperimentsStub.AccessMember[0]));

	    ExperimentsStub.RealizeExperimentResponse realizeResp = 
		stub.realizeExperiment(realizeReq);
	    dumpRealizationDescription(realizeResp.get_return(), System.out);
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
